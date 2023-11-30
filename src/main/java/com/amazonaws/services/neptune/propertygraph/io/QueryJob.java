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

package com.amazonaws.services.neptune.propertygraph.io;

import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.io.Status;
import com.amazonaws.services.neptune.io.StatusOutputFormat;
import com.amazonaws.services.neptune.propertygraph.AllLabels;
import com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.LabelsFilter;
import com.amazonaws.services.neptune.propertygraph.NamedQuery;
import com.amazonaws.services.neptune.cluster.ConcurrencyConfig;
import com.amazonaws.services.neptune.propertygraph.NeptuneGremlinClient;
import com.amazonaws.services.neptune.propertygraph.NodeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.schema.ExportSpecification;
import com.amazonaws.services.neptune.propertygraph.schema.FileSpecificLabelSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import com.amazonaws.services.neptune.propertygraph.schema.GraphSchema;
import com.amazonaws.services.neptune.propertygraph.schema.MasterLabelSchemas;
import com.amazonaws.services.neptune.util.CheckedActivity;
import com.amazonaws.services.neptune.util.Timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryJob {
    private final Queue<NamedQuery> queries;
    private final NeptuneGremlinClient.QueryClient queryClient;
    private final ConcurrencyConfig concurrencyConfig;
    private final PropertyGraphTargetConfig targetConfig;
    private final boolean twoPassAnalysis;
    private final Long timeoutMillis;
    private final Collection<ExportSpecification> exportSpecifications;
    private final FeatureToggles featureToggles;
    private final boolean structuredOutput;

    public QueryJob(Collection<NamedQuery> queries,
                    NeptuneGremlinClient.QueryClient queryClient,
                    ConcurrencyConfig concurrencyConfig,
                    PropertyGraphTargetConfig targetConfig,
                    boolean twoPassAnalysis,
                    Long timeoutMillis,
                    Collection<ExportSpecification> exportSpecifications,
                    FeatureToggles featureToggles,
                    boolean structuredOutput){
        this.queries = new ConcurrentLinkedQueue<>(queries);
        this.queryClient = queryClient;
        this.concurrencyConfig = concurrencyConfig;
        this.targetConfig = targetConfig;
        this.twoPassAnalysis = twoPassAnalysis;
        this.timeoutMillis = timeoutMillis;
        this.exportSpecifications = exportSpecifications;
        this.featureToggles = featureToggles;
        this.structuredOutput = structuredOutput;
    }

    public GraphSchema execute() throws Exception {
        Map<GraphElementType, GraphElementSchemas> graphElementSchemas = Timer.timedActivity("exporting results from queries",
                (CheckedActivity.Callable<Map<GraphElementType, GraphElementSchemas>>) this::export);

        return new GraphSchema(graphElementSchemas);
    }

    private Map<GraphElementType, GraphElementSchemas> export() throws ExecutionException, InterruptedException {

        System.err.println("Writing query results to " + targetConfig.output().name() + " as " + targetConfig.format().description());

        Status status = new Status(StatusOutputFormat.Description, "query results");

        ExecutorService taskExecutor = Executors.newFixedThreadPool(concurrencyConfig.concurrency());

        Collection<Future<Map<GraphElementType, FileSpecificLabelSchemas>>> futures = new ArrayList<>();

        Collection<FileSpecificLabelSchemas> nodesFileSpecificLabelSchemas = new ArrayList<>();
        Collection<FileSpecificLabelSchemas> edgesFileSpecificLabelSchemas = new ArrayList<>();

        LabelsFilter nodeLabelFilter = new AllLabels(NodeLabelStrategy.nodeLabelsOnly);
        LabelsFilter edgeLabelFilter = new AllLabels(EdgeLabelStrategy.edgeLabelsOnly);

        for(ExportSpecification exportSpecification : exportSpecifications) {
            if (exportSpecification.getGraphElementType() == GraphElementType.nodes) {
                nodeLabelFilter = exportSpecification.getLabelsFilter();
            }
            else {
                edgeLabelFilter = exportSpecification.getLabelsFilter();
            }
        }

        AtomicInteger fileIndex = new AtomicInteger();

        for (int index = 1; index <= concurrencyConfig.concurrency(); index++) {
            QueryTask queryTask = new QueryTask(
                    queries,
                    queryClient,
                    targetConfig,
                    twoPassAnalysis,
                    timeoutMillis,
                    status,
                    fileIndex,
                    structuredOutput,
                    nodeLabelFilter,
                    edgeLabelFilter,
                    exportSpecifications.iterator().next().getExportStats());
            futures.add(taskExecutor.submit(queryTask));
        }

        taskExecutor.shutdown();

        try {
            taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        for (Future<Map<GraphElementType, FileSpecificLabelSchemas>> future : futures) {
            if (future.isCancelled()) {
                throw new IllegalStateException("Unable to complete job because at least one task was cancelled");
            }
            if (!future.isDone()) {
                throw new IllegalStateException("Unable to complete job because at least one task has not completed");
            }
            Map<GraphElementType, FileSpecificLabelSchemas> result = future.get();
            nodesFileSpecificLabelSchemas.add(result.get(GraphElementType.nodes));
            edgesFileSpecificLabelSchemas.add(result.get(GraphElementType.edges));
        }

        RewriteCommand rewriteCommand = targetConfig.createRewriteCommand(concurrencyConfig, featureToggles);
        Map<GraphElementType, GraphElementSchemas> graphElementSchemas = new HashMap<>();

        for(ExportSpecification exportSpecification : exportSpecifications) {
            MasterLabelSchemas masterLabelSchemas = exportSpecification.createMasterLabelSchemas(
                    exportSpecification.getGraphElementType().equals(GraphElementType.nodes) ?
                            nodesFileSpecificLabelSchemas : edgesFileSpecificLabelSchemas
            );
            try {
                graphElementSchemas.put(exportSpecification.getGraphElementType(), rewriteCommand.execute(masterLabelSchemas).toGraphElementSchemas());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return graphElementSchemas;
    }
}
