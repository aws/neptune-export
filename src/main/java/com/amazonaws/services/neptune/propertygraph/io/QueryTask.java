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

import com.amazonaws.services.neptune.io.Directories;
import com.amazonaws.services.neptune.io.Status;
import com.amazonaws.services.neptune.propertygraph.AllLabels;
import com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.ExportStats;
import com.amazonaws.services.neptune.propertygraph.GraphClient;
import com.amazonaws.services.neptune.propertygraph.GremlinFilters;
import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.LabelStrategy;
import com.amazonaws.services.neptune.propertygraph.LabelsFilter;
import com.amazonaws.services.neptune.propertygraph.NamedQuery;
import com.amazonaws.services.neptune.propertygraph.NeptuneGremlinClient;
import com.amazonaws.services.neptune.propertygraph.NodeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.NodesClient;
import com.amazonaws.services.neptune.propertygraph.Range;
import com.amazonaws.services.neptune.propertygraph.RangeConfig;
import com.amazonaws.services.neptune.propertygraph.StatsContainer;
import com.amazonaws.services.neptune.propertygraph.io.result.PGEdgeResult;
import com.amazonaws.services.neptune.propertygraph.io.result.QueriesEdgeResult;
import com.amazonaws.services.neptune.propertygraph.io.result.QueriesNodeResult;
import com.amazonaws.services.neptune.propertygraph.schema.FileSpecificLabelSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import com.amazonaws.services.neptune.propertygraph.schema.LabelSchema;
import com.amazonaws.services.neptune.util.Activity;
import com.amazonaws.services.neptune.util.CheckedActivity;
import com.amazonaws.services.neptune.util.Timer;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryTask implements Callable<Map<GraphElementType, FileSpecificLabelSchemas>> {

    private static final Logger logger = LoggerFactory.getLogger(QueryTask.class);

    private final Queue<NamedQuery> queries;
    private final NeptuneGremlinClient.QueryClient queryClient;
    private final PropertyGraphTargetConfig targetConfig;
    private final boolean twoPassAnalysis;
    private final Long timeoutMillis;
    private final Status status;
    private final AtomicInteger index;
    private final boolean structuredOutput;
    private final LabelsFilter nodeLabelFilter;
    private final LabelsFilter edgeLabelFilter;
    private final ExportStats exportStats;

    public QueryTask(Queue<NamedQuery> queries,
                     NeptuneGremlinClient.QueryClient queryClient,
                     PropertyGraphTargetConfig targetConfig,
                     boolean twoPassAnalysis,
                     Long timeoutMillis,
                     Status status,
                     AtomicInteger index,
                     boolean structuredOutput,
                     LabelsFilter nodeLabelFilter,
                     LabelsFilter edgeLabelFilter,
                     ExportStats exportStats) {

        this.queries = queries;
        this.queryClient = queryClient;
        this.targetConfig = targetConfig;
        this.twoPassAnalysis = twoPassAnalysis;
        this.timeoutMillis = timeoutMillis;
        this.status = status;
        this.index = index;
        this.structuredOutput = structuredOutput;
        this.nodeLabelFilter = nodeLabelFilter;
        this.edgeLabelFilter = edgeLabelFilter;
        this.exportStats = exportStats;
    }

    @Override
    public Map<GraphElementType, FileSpecificLabelSchemas> call() throws Exception {

        QueriesWriterFactory writerFactory = new QueriesWriterFactory();
        Map<Label, LabelWriter<Map<?, ?>>> labelWriters = new HashMap<>();

        Map<GraphElementType, FileSpecificLabelSchemas> fileSpecificLabelSchemasMap = new HashMap<>();
        fileSpecificLabelSchemasMap.put(GraphElementType.nodes, new FileSpecificLabelSchemas());
        fileSpecificLabelSchemasMap.put(GraphElementType.edges, new FileSpecificLabelSchemas());

        try {

            while (status.allowContinue()) {

                try {

                    NamedQuery namedQuery = queries.poll();

                    if (!(namedQuery == null)) {

                        final GraphElementSchemas graphElementSchemas = new GraphElementSchemas();

                        if (twoPassAnalysis) {
                            Timer.timedActivity(String.format("generating schema for query [%s]", namedQuery.query()),
                                    (Activity.Runnable) () -> updateSchema(namedQuery, graphElementSchemas));
                        }

                        Timer.timedActivity(String.format("executing query [%s]", namedQuery.query()),
                                (CheckedActivity.Runnable) () ->
                                        executeQuery(namedQuery, writerFactory, labelWriters, graphElementSchemas, fileSpecificLabelSchemasMap));

                    } else {
                        status.halt();
                    }

                } catch (IllegalStateException e) {
                    logger.warn("Unexpected result value. {}. Proceeding with next query.", e.getMessage());
                }
            }

        } finally {
            for (LabelWriter<Map<?, ?>> labelWriter : labelWriters.values()) {
                try {
                    labelWriter.close();
                } catch (Exception e) {
                    logger.warn("Error closing label writer: {}.", e.getMessage());
                }
            }
        }

        return fileSpecificLabelSchemasMap;

    }

    private void updateSchema(NamedQuery namedQuery, GraphElementSchemas graphElementSchemas) {
        ResultSet firstPassResults = queryClient.submit(namedQuery.query(), timeoutMillis);

        firstPassResults.stream().
                map(r -> castToMap(r.getObject())).
                forEach(r -> {
                    graphElementSchemas.update(new Label(namedQuery.name()), r, true);
                });
    }

    private void executeQuery(NamedQuery namedQuery,
                              QueriesWriterFactory writerFactory,
                              Map<Label, LabelWriter<Map<?, ?>>> labelWriters,
                              GraphElementSchemas graphElementSchemas,
                              Map<GraphElementType, FileSpecificLabelSchemas> fileSpecificLabelSchemasMap) {

        ResultSet results = queryClient.submit(namedQuery.query(), timeoutMillis);

        GraphElementHandler<Map<?, ?>> handler;

        if(structuredOutput) {
            handler = new QueriesResultWrapperHandler(
                    new CountingHandler<QueriesNodeResult>(
                        new ExportPGTaskHandler<QueriesNodeResult>(
                                fileSpecificLabelSchemasMap.get(GraphElementType.nodes),
                                graphElementSchemas,
                                targetConfig,
                                (WriterFactory<QueriesNodeResult>) GraphElementType.nodes.writerFactory(),
                                new LabelWriters<>(new AtomicInteger(), 0),
                                new ExportStatsWrapper(exportStats, GraphElementType.nodes),
                                status,
                                index,
                                nodeLabelFilter)
                    ),
                    new CountingHandler<QueriesEdgeResult>(
                            new ExportPGTaskHandler<QueriesEdgeResult>(
                                    fileSpecificLabelSchemasMap.get(GraphElementType.edges),
                                    graphElementSchemas,
                                    targetConfig,
                                    (WriterFactory<QueriesEdgeResult>) GraphElementType.edges.writerFactory(),
                                    new LabelWriters<>(new AtomicInteger(), 0),
                                    new ExportStatsWrapper(exportStats, GraphElementType.edges),
                                    status,
                                    index,
                                    edgeLabelFilter)
                    )
            );
        }
        else {
            ResultsHandler resultsHandler = new ResultsHandler(
                    new Label(namedQuery.name()),
                    labelWriters,
                    writerFactory,
                    graphElementSchemas);

            handler = new StatusHandler(resultsHandler, status);
        }

        results.stream().
                map(r -> castToMap(r.getObject())).
                forEach(r -> {
                    try {
                        handler.handle(r, true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private HashMap<?, ?> castToMap(Object o) {
        if (Map.class.isAssignableFrom(o.getClass())) {
            return (HashMap<?, ?>) o;
        }

        throw new IllegalStateException("Expected Map, found " + o.getClass().getSimpleName());
    }

    private class ResultsHandler implements GraphElementHandler<Map<?, ?>> {

        private final Label label;
        private final Map<Label, LabelWriter<Map<?, ?>>> labelWriters;
        private final QueriesWriterFactory writerFactory;
        private final GraphElementSchemas graphElementSchemas;

        private ResultsHandler(Label label,
                               Map<Label, LabelWriter<Map<?, ?>>> labelWriters,
                               QueriesWriterFactory writerFactory,
                               GraphElementSchemas graphElementSchemas) {
            this.label = label;
            this.labelWriters = labelWriters;
            this.writerFactory = writerFactory;

            this.graphElementSchemas = graphElementSchemas;
        }

        private void createWriter(Map<?, ?> properties, boolean allowStructuralElements) {
            try {

                if (!graphElementSchemas.hasSchemaFor(label)) {
                    graphElementSchemas.update(label, properties, allowStructuralElements);
                }

                LabelSchema labelSchema = graphElementSchemas.getSchemaFor(label);
                PropertyGraphPrinter propertyGraphPrinter =
                        writerFactory.createPrinter(Directories.fileName(label.fullyQualifiedLabel(), index), labelSchema, targetConfig);

                labelWriters.put(label, writerFactory.createLabelWriter(propertyGraphPrinter, label));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void handle(Map<?, ?> properties, boolean allowTokens) throws IOException {

            if (!labelWriters.containsKey(label)) {
                createWriter(properties, allowTokens);
            }

            labelWriters.get(label).handle(properties, allowTokens);
        }

        @Override
        public void close() throws Exception {
            // Do nothing
        }
    }

    private static class StatusHandler implements GraphElementHandler<Map<?, ?>> {

        private final GraphElementHandler<Map<?, ?>> parent;
        private final Status status;

        private StatusHandler(GraphElementHandler<Map<?, ?>> parent, Status status) {
            this.parent = parent;
            this.status = status;
        }

        @Override
        public void handle(Map<?, ?> input, boolean allowTokens) throws IOException {
            parent.handle(input, allowTokens);
            status.update();
        }

        @Override
        public void close() throws Exception {
            parent.close();
        }
    }

    private static class QueriesResultWrapperHandler implements GraphElementHandler<Map<?, ?>> {

        private final GraphElementHandler<QueriesNodeResult> nodeParent;
        private final GraphElementHandler<QueriesEdgeResult> edgeParent;

        private QueriesResultWrapperHandler(GraphElementHandler<QueriesNodeResult> nodeParent, GraphElementHandler<QueriesEdgeResult> edgeParent) {
            this.nodeParent = nodeParent;
            this.edgeParent = edgeParent;
        }

        @Override
        public void handle(Map<?, ?> input, boolean allowTokens) throws IOException {
            if(isEdge(input)) {
                edgeParent.handle(getQueriesEdgeResult(input), allowTokens);
            }
            else {
                nodeParent.handle(getQueriesNodeResult(input), allowTokens);
            }
        }

        @Override
        public void close() throws Exception {
            nodeParent.close();
        }

        private boolean isEdge(Map<?, ?> input) {
            return input.containsKey(Direction.IN) && input.containsKey(Direction.OUT);
        }

        private QueriesNodeResult getQueriesNodeResult(Map<?, ?> map) {
            return new QueriesNodeResult(map);
        }

        private QueriesEdgeResult getQueriesEdgeResult(Map<?, ?> map) {
            return new QueriesEdgeResult(map);
        }
    }

    private class ExportStatsWrapper implements StatsContainer {
        private final ExportStats exportStats;
        private final GraphElementType graphElementType;

        public ExportStatsWrapper(ExportStats exportStats, GraphElementType graphElementType) {
            this.exportStats = exportStats;
            this.graphElementType = graphElementType;
        }

        @Override
        public void updateStats(Label label) {
            if(graphElementType.equals(GraphElementType.nodes)) {
                exportStats.incrementNodeStats(label);
            } else {
                exportStats.incrementEdgeStats(label);
            }
        }
    }
}
