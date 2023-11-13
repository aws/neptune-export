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

import com.amazonaws.services.neptune.cluster.ConnectionConfig;
import com.amazonaws.services.neptune.cluster.HttpResponse;
import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.io.OutputWriter;
import com.amazonaws.services.neptune.rdf.io.RdfTargetConfig;
import com.amazonaws.services.neptune.util.CheckedActivity;
import com.amazonaws.services.neptune.util.Timer;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import java.io.InputStream;
import java.util.Collections;

public class ExportRdfGraphJob implements ExportRdfJob {

    private final NeptuneSparqlClient client;
    private final RdfTargetConfig targetConfig;
    private final ConnectionConfig connectionConfig;

    public ExportRdfGraphJob(NeptuneSparqlClient client, RdfTargetConfig targetConfig, ConnectionConfig connectionConfig) {
        this.client = client;
        this.targetConfig = targetConfig;
        this.connectionConfig = connectionConfig;
    }

    @Override
    public void execute() throws Exception {
        Timer.timedActivity("exporting RDF as " + targetConfig.format().description(),
                (CheckedActivity.Runnable) () -> {
                    System.err.println("Creating statement files");
//                    client.executeTupleQuery("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }", targetConfig);
                    HttpClient httpClient = client.chooseRepository().getHttpClient();
                    HttpUriRequest request = new HttpGet(getGSPEndpoint("default"));
                    request.addHeader("Content-Type", "application/n-quads");

                    org.apache.http.HttpResponse response = httpClient.execute(request);
                    InputStream responseBody = response.getEntity().getContent();

                    RDFParser rdfParser = Rio.createParser(RDFFormat.NQUADS);
                    OutputWriter outputWriter = targetConfig.createOutputWriter();
                    RDFWriter writer = targetConfig.createRDFWriter(outputWriter, new FeatureToggles(Collections.emptyList()));
                    rdfParser.setRDFHandler(writer);

                    try {
                        rdfParser.parse(responseBody);
                    }
                    catch (Exception e) {

                    }
                    finally {
                        responseBody.close();
                    }
                });
    }

    private String getGSPEndpoint(String graphName) {
        return String.format("https://%s:%s/sparql/gsp/?%s",
                connectionConfig.endpoints().iterator().next(),
                connectionConfig.port(),
                graphName);
    }
}
