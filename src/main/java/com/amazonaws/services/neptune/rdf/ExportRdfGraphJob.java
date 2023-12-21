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

import com.amazonaws.services.neptune.rdf.io.RdfTargetConfig;
import com.amazonaws.services.neptune.util.CheckedActivity;
import com.amazonaws.services.neptune.util.Timer;

import java.util.Collections;
import java.util.List;

public class ExportRdfGraphJob implements ExportRdfJob {

    private final NeptuneSparqlClient client;
    private final RdfTargetConfig targetConfig;
    private final List<String> namedGraphs;

    public ExportRdfGraphJob(NeptuneSparqlClient client, RdfTargetConfig targetConfig) {
        this(client, targetConfig, Collections.emptyList());
    }

    public ExportRdfGraphJob(NeptuneSparqlClient client, RdfTargetConfig targetConfig, List<String> namedGraphs) {
        this.client = client;
        this.targetConfig = targetConfig;
        this.namedGraphs = namedGraphs;
    }

    @Override
    public void execute() throws Exception {
        Timer.timedActivity("exporting RDF as " + targetConfig.format().description(),
                (CheckedActivity.Runnable) () -> {
                    System.err.println("Creating statement files");
                    if(namedGraphs == null || namedGraphs.isEmpty()) {
                        client.executeCompleteExport(targetConfig);
                    } else {
                        for(String namedGraph : namedGraphs) {
                            client.executeNamedGraphExport(targetConfig, namedGraph);
                        }
                    }
                });
    }
}
