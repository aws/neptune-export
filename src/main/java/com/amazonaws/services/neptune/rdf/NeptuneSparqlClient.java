/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.rdf;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.neptune.auth.NeptuneSigV4SignerException;
import com.amazonaws.services.neptune.cluster.ConnectionConfig;
import com.amazonaws.services.neptune.export.FeatureToggle;
import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.io.OutputWriter;
import com.amazonaws.services.neptune.rdf.io.NeptuneExportSparqlRepository;
import com.amazonaws.services.neptune.rdf.io.RdfTargetConfig;
import com.amazonaws.services.neptune.util.EnvironmentVariableUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.base.AbstractRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class NeptuneSparqlClient implements AutoCloseable {

    private static final ParserConfig PARSER_CONFIG = new ParserConfig().addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);

    public static NeptuneSparqlClient create(ConnectionConfig config, FeatureToggles featureToggles) {

        String serviceRegion = config.useIamAuth() ? EnvironmentVariableUtils.getMandatoryEnv("SERVICE_REGION") : null;
        AWSCredentialsProvider credentialsProvider = config.useIamAuth() ? config.getCredentialsProvider() : null;

        return new NeptuneSparqlClient(
                config.endpoints().stream()
                        .map(e -> {
                                    try {
                                        return updateParser(new NeptuneExportSparqlRepository(
                                                sparqlEndpoint(e, config.port()),
                                                credentialsProvider,
                                                serviceRegion,
                                                config));
                                    } catch (NeptuneSigV4SignerException e1) {
                                        throw new RuntimeException(e1);
                                    }
                                }
                        )
                        .peek(AbstractRepository::init)
                        .collect(Collectors.toList()),
                featureToggles, config);
    }

    private static SPARQLRepository updateParser(SPARQLRepository repository) {

        HttpClientSessionManager sessionManager = repository.getHttpClientSessionManager();
        repository.setHttpClientSessionManager(new HttpClientSessionManager() {
            @Override
            public HttpClient getHttpClient() {
                return sessionManager.getHttpClient();
            }

            @Override
            public SPARQLProtocolSession createSPARQLProtocolSession(String s, String s1) {
                SPARQLProtocolSession session = sessionManager.createSPARQLProtocolSession(s, s1);
                session.setParserConfig(PARSER_CONFIG);
                session.setPreferredTupleQueryResultFormat(TupleQueryResultFormat.JSON);

                return session;
            }

            @Override
            public RDF4JProtocolSession createRDF4JProtocolSession(String s) {
                return sessionManager.createRDF4JProtocolSession(s);
            }

            @Override
            public void shutDown() {
                sessionManager.shutDown();
            }
        });
        return repository;
    }

    private static String sparqlEndpoint(String endpoint, int port) {
        return String.format("https://%s:%s", endpoint, port);
    }

    private final List<SPARQLRepository> repositories;
    private final Random random = new Random(DateTime.now().getMillis());
    private final FeatureToggles featureToggles;
    private final ConnectionConfig connectionConfig;

    private NeptuneSparqlClient(List<SPARQLRepository> repositories, FeatureToggles featureToggles, ConnectionConfig connectionConfig) {
        this.repositories = repositories;
        this.featureToggles = featureToggles;
        this.connectionConfig = connectionConfig;
    }

    public void executeTupleQuery(String sparql, RdfTargetConfig targetConfig) throws IOException {
        SPARQLRepository repository = chooseRepository();
        ValueFactory factory = repository.getValueFactory();

        try (RepositoryConnection connection = repository.getConnection();
             OutputWriter outputWriter = targetConfig.createOutputWriter()) {

            RDFWriter writer = targetConfig.createRDFWriter(outputWriter, featureToggles);

            connection.prepareTupleQuery(sparql).evaluate(new TupleQueryHandler(writer, factory));

        } catch (Exception e) {
            if (repository instanceof NeptuneExportSparqlRepository) {
                throw new AmazonServiceException(((NeptuneExportSparqlRepository) repository).getErrorMessageFromTrailers(), e);
            }
            else {
                throw new RuntimeException(e);
            }
        }
    }

    public void executeGraphQuery(String sparql, RdfTargetConfig targetConfig) throws IOException {
        SPARQLRepository repository = chooseRepository();

        try (RepositoryConnection connection = repository.getConnection();
             OutputWriter outputWriter = targetConfig.createOutputWriter()) {

            RDFWriter writer = targetConfig.createRDFWriter(outputWriter, featureToggles);

            connection.prepareGraphQuery(sparql).evaluate(new GraphQueryHandler(writer));

        } catch (Exception e) {
            if (repository instanceof NeptuneExportSparqlRepository) {
                throw new AmazonServiceException(((NeptuneExportSparqlRepository) repository).getErrorMessageFromTrailers(), e);
            }
            else {
                throw new RuntimeException(e);
            }
        }
    }

    public void executeCompleteExport(RdfTargetConfig targetConfig) throws IOException {
        if(featureToggles.containsFeature(FeatureToggle.No_GSP)) {
            executeTupleQuery("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }", targetConfig);
        } else {
            executeGSPExport(targetConfig, "default");
        }
    }

    public void executeNamedGraphExport(RdfTargetConfig targetConfig, String namedGraph) throws IOException {
        if(featureToggles.containsFeature(FeatureToggle.No_GSP)) {
            executeTupleQuery(String.format("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } FILTER(?g = <%s>) .}", namedGraph), targetConfig);
        } else {
            executeGSPExport(targetConfig, "graph="+namedGraph);
        }
    }

    void executeGSPExport(RdfTargetConfig targetConfig, String graph) throws IOException {
        HttpClient httpClient = chooseRepository().getHttpClient();
        HttpUriRequest request = new HttpGet(getGSPEndpoint(graph));
        request.addHeader("Accept", "application/n-triples");
        request.addHeader("te", "trailers");

        org.apache.http.HttpResponse response = httpClient.execute(request);
        InputStream responseBody = response.getEntity().getContent();
        RDFParser rdfParser = Rio.createParser(RDFFormat.NTRIPLES);

        try (OutputWriter outputWriter = targetConfig.createOutputWriter()) {
            RDFWriter writer = targetConfig.createRDFWriter(outputWriter, featureToggles);
            rdfParser.setRDFHandler(writer);

            try {
                rdfParser.parse(responseBody);
            } catch(RDFParseException e) {
                String serverErrorMessage = getErrorMessageFromTrailers(response);
                if (StringUtils.isNotEmpty(serverErrorMessage)) {
                    throw new AmazonServiceException(serverErrorMessage, e);
                } else {
                    throw new RuntimeException("Failed to parse RDF response. This may indicate malformed data in the " +
                            "results, or a corrupted response from the server potentially due to query execution failure.", e);
                }
            } finally {
                responseBody.close();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    SPARQLRepository chooseRepository() {
        return repositories.get(random.nextInt(repositories.size()));
    }

    @Override
    public void close() {
        repositories.forEach(AbstractRepository::shutDown);
    }

    private String getGSPEndpoint(String graphName) {
        return String.format("https://%s:%s/sparql/gsp/?%s",
                connectionConfig.endpoints().iterator().next(),
                connectionConfig.port(),
                graphName);
    }

    /**
     * Attempts to extract error messages from trailing headers from the most recent response received by 'repository'.
     * If no trailers are found an empty String is returned.
     */
    static String getErrorMessageFromTrailers(org.apache.http.HttpResponse response) {
        if (response == null) {
            return "";
        }
        InputStream responseInStream;
        try {
            responseInStream = response.getEntity().getContent();
        } catch (IOException e) {
            return "";
        }
        // HTTPClient 4.5.13 provides no methods for accessing trailers from a wrapped stream requiring the use of
        // reflection to break encapsulation. This bug is being tracked in https://issues.apache.org/jira/browse/HTTPCLIENT-2263.
        while (!(responseInStream instanceof ChunkedInputStream)){
            Field wrappedStreamField;
            try {
                wrappedStreamField = responseInStream.getClass().getDeclaredField("wrappedStream");
                wrappedStreamField.setAccessible(true);
                responseInStream = (InputStream) wrappedStreamField.get(responseInStream);
                wrappedStreamField.setAccessible(false);
            } catch (Exception e) {
                return "";
            }
        }
        // Consume remaining response, ensures trailers have been consumed and are available
        EntityUtils.consumeQuietly(response.getEntity());

        Header[] trailers = ((ChunkedInputStream) responseInStream).getFooters();
        StringBuilder messageBuilder = new StringBuilder();
        for (Header trailer : trailers) {
            try {
                messageBuilder.append(URLDecoder.decode(trailer.toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                messageBuilder.append(trailer);
            }
            messageBuilder.append('\n');
        }
        return messageBuilder.toString();
    }

}
