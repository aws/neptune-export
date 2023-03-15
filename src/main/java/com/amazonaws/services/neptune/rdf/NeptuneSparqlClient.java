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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.neptune.auth.NeptuneSigV4SignerException;
import com.amazonaws.services.neptune.cluster.ConnectionConfig;
import com.amazonaws.services.neptune.io.OutputWriter;
import com.amazonaws.services.neptune.rdf.io.NeptuneExportSparqlRepository;
import com.amazonaws.services.neptune.rdf.io.RdfTargetConfig;
import com.amazonaws.services.neptune.util.EnvironmentVariableUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.impl.io.ChunkedInputStream;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.base.AbstractRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class NeptuneSparqlClient implements AutoCloseable {

    private static final ParserConfig PARSER_CONFIG = new ParserConfig().addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);

    public static NeptuneSparqlClient create(ConnectionConfig config) {

        String serviceRegion = config.useIamAuth() ? EnvironmentVariableUtils.getMandatoryEnv("SERVICE_REGION") : null;
        AWSCredentialsProvider credentialsProvider = config.useIamAuth() ? new DefaultAWSCredentialsProviderChain() : null;

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
                        .collect(Collectors.toList()));
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

    private NeptuneSparqlClient(List<SPARQLRepository> repositories) {
        this.repositories = repositories;
    }

    public void executeTupleQuery(String sparql, RdfTargetConfig targetConfig) throws IOException {
        SPARQLRepository repository = chooseRepository();
        ValueFactory factory = repository.getValueFactory();

        try (RepositoryConnection connection = repository.getConnection();
             OutputWriter outputWriter = targetConfig.createOutputWriter()) {

            RDFWriter writer = targetConfig.createRDFWriter(outputWriter);

            connection.prepareTupleQuery(sparql).evaluate(new TupleQueryHandler(writer, factory));

        } catch (Exception e) {
            throw new RuntimeException(getErrorMessageFromTrailers(repository), e);
        }
    }

    public void executeGraphQuery(String sparql, RdfTargetConfig targetConfig) throws IOException {
        SPARQLRepository repository = chooseRepository();

        try (RepositoryConnection connection = repository.getConnection();
             OutputWriter outputWriter = targetConfig.createOutputWriter()) {

            RDFWriter writer = targetConfig.createRDFWriter(outputWriter);

            connection.prepareGraphQuery(sparql).evaluate(new GraphQueryHandler(writer));

        } catch (Exception e) {
            throw new RuntimeException(getErrorMessageFromTrailers(repository), e);
        }
    }


    private SPARQLRepository chooseRepository() {
        return repositories.get(random.nextInt(repositories.size()));
    }

    /**
     * Attempts to extract error messages from trailing headers from the most recent response received by 'repository'.
     * If no trailers are found an empty String is returned.
     */
    private String getErrorMessageFromTrailers(SPARQLRepository repository) {
        if(repository instanceof NeptuneExportSparqlRepository) {
            InputStream responseInStream = (InputStream) ((NeptuneExportSparqlRepository) repository).lastContext.getAttribute("raw-response-inputstream");
            ChunkedInputStream chunkedInStream;
            if (responseInStream instanceof ChunkedInputStream) {
                chunkedInStream = (ChunkedInputStream) responseInStream;
            }
            else if (responseInStream instanceof EofSensorInputStream) {
                // HTTPClient 4.5.13 provides no methods for accessing trailers from a wrapped stream requiring the use
                // reflection to break encapsulation. This bug is being tracked in https://issues.apache.org/jira/browse/HTTPCLIENT-2263.
                try {
                    Method getWrappedStream = EofSensorInputStream.class.getDeclaredMethod("getWrappedStream");
                    getWrappedStream.setAccessible(true);
                    chunkedInStream = (ChunkedInputStream) getWrappedStream.invoke(responseInStream);
                    getWrappedStream.setAccessible(false);
                } catch (Exception e) {
                    return "";
                }
            }
            else {
                return "";
            }
            Header[] trailers = chunkedInStream.getFooters();
            String message = "";
            for (Header trailer : trailers) {
                try {
                    message += URLDecoder.decode(trailer.toString(), "UTF-8") + "\n";
                } catch (UnsupportedEncodingException e) {
                    message += trailer + "\n";
                }
            }
            return message;
        }
        return "";
    }

    @Override
    public void close() {
        repositories.forEach(AbstractRepository::shutDown);
    }

}
