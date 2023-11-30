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
import com.amazonaws.services.neptune.propertygraph.ExportStats;
import com.amazonaws.services.neptune.propertygraph.GremlinFilters;
import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.NamedQuery;
import com.amazonaws.services.neptune.propertygraph.NeptuneGremlinClient;
import com.amazonaws.services.neptune.propertygraph.NodeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.schema.ExportSpecification;
import com.amazonaws.services.neptune.propertygraph.schema.FileSpecificLabelSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import com.amazonaws.services.neptune.propertygraph.schema.GraphSchema;
import com.amazonaws.services.neptune.propertygraph.schema.MasterLabelSchemas;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryTaskTest {

    private final GraphTraversalSource gModern = TinkerFactory.createModern().traversal();

    @Test
    public void gDotEShouldOnlyCreateEdges() throws Exception {
        QueryTask qt = createQueryTask(gModern.E().elementMap(), true);

        Map<GraphElementType, FileSpecificLabelSchemas> results = qt.call();

        FileSpecificLabelSchemas nodeSchemas = results.get(GraphElementType.nodes);
        FileSpecificLabelSchemas edgeSchemas = results.get(GraphElementType.edges);

        assertEquals(0, nodeSchemas.labels().size());
        assertEquals(2, edgeSchemas.labels().size());
        assertTrue(edgeSchemas.hasSchemasForLabel(new Label("knows")));
        assertTrue(edgeSchemas.hasSchemasForLabel(new Label("created")));
    }

    @Test
    public void gDotVShouldOnlyCreateNodes() throws Exception {
        QueryTask qt = createQueryTask(gModern.V().elementMap(), true);

        Map<GraphElementType, FileSpecificLabelSchemas> results = qt.call();

        FileSpecificLabelSchemas nodeSchemas = results.get(GraphElementType.nodes);
        FileSpecificLabelSchemas edgeSchemas = results.get(GraphElementType.edges);

        assertEquals(2, nodeSchemas.labels().size());
        assertEquals(0, edgeSchemas.labels().size());
        assertTrue(nodeSchemas.hasSchemasForLabel(new Label("person")));
        assertTrue(nodeSchemas.hasSchemasForLabel(new Label("software")));
    }

    @Test
    public void shouldSeparateEdgesAndNodes() throws Exception {
        QueryTask qt = createQueryTask(gModern.V().union(__.hasLabel("person"), __.outE().hasLabel("created")).elementMap(), true);

        Map<GraphElementType, FileSpecificLabelSchemas> results = qt.call();

        FileSpecificLabelSchemas nodeSchemas = results.get(GraphElementType.nodes);
        FileSpecificLabelSchemas edgeSchemas = results.get(GraphElementType.edges);

        assertEquals(1, nodeSchemas.labels().size());
        assertEquals(1, edgeSchemas.labels().size());
        assertTrue(nodeSchemas.hasSchemasForLabel(new Label("person")));
        assertTrue(edgeSchemas.hasSchemasForLabel(new Label("created")));
    }

    @Test
    public void shouldUpdateExportStats() throws Exception {
        ExportStats exportStats = new ExportStats();
        QueryTask qt = createQueryTask(gModern.V().union(__.elementMap(), __.outE().elementMap()), true, exportStats);

        Map<GraphElementType, FileSpecificLabelSchemas> results = qt.call();

        Map<GraphElementType, GraphElementSchemas> graphElementSchemas = new HashMap<>();
        Collection<ExportSpecification> exportSpecifications = createExportSpecifications(exportStats);

        for(ExportSpecification exportSpecification : exportSpecifications) {
            MasterLabelSchemas masterLabelSchemas = exportSpecification.createMasterLabelSchemas(
                    Collections.singletonList(results.get(exportSpecification.getGraphElementType()))
            );
            try {
                graphElementSchemas.put(exportSpecification.getGraphElementType(), masterLabelSchemas.toGraphElementSchemas());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        JsonNode statsResults = exportStats.toJson(new GraphSchema(graphElementSchemas)).findPath("stats");
        assertEquals(6, statsResults.findValue("nodes").intValue());
        assertEquals(6, statsResults.findValue("edges").intValue());
        //NOTE: Property stats are normally recorded through the PropertyGraphPrinter, which is mocked out in this test.
        assertEquals(0, statsResults.findValue("properties").intValue());

        JsonNode nodeDetails = statsResults.findPath("details").findPath("nodes");
        assertTrue(nodeDetails.isArray());
        assertEquals(2, nodeDetails.size());
        assertEquals("software", nodeDetails.get(0).get("description").textValue());
        assertEquals(2, nodeDetails.get(0).get("count").intValue());
        assertEquals("person", nodeDetails.get(1).get("description").textValue());
        assertEquals(4, nodeDetails.get(1).get("count").intValue());

        JsonNode edgeDetails = statsResults.findPath("details").findPath("edges");
        assertTrue(edgeDetails.isArray());
        assertEquals(2, edgeDetails.size());
        assertEquals("knows", edgeDetails.get(0).get("description").textValue());
        assertEquals(2, edgeDetails.get(0).get("count").intValue());
        assertEquals("created", edgeDetails.get(1).get("description").textValue());
        assertEquals(4, edgeDetails.get(1).get("count").intValue());
    }

    private NeptuneGremlinClient.QueryClient getMockClient(GraphTraversal traversal) {
        NeptuneGremlinClient.QueryClient mockClient = mock(NeptuneGremlinClient.QueryClient.class);
        ResultSet results = mock(ResultSet.class);

        when(results.stream()).thenReturn(traversal.toStream().map(r -> new Result(r)));
        when(mockClient.submit(any(), any())).thenReturn(results);

        return mockClient;
    }

    private QueryTask createQueryTask(GraphTraversal traversal, boolean structuredOutput) throws IOException {
        return createQueryTask(traversal, structuredOutput, new ExportStats());
    }

    private QueryTask createQueryTask(GraphTraversal traversal, boolean structuredOutput, ExportStats exportStats) throws IOException {
        Queue<NamedQuery> mockQueries = new LinkedList<>();
        mockQueries.add(mock(NamedQuery.class));

        PropertyGraphTargetConfig targetConfig = mock(PropertyGraphTargetConfig.class);
        when(targetConfig.createPrinterForEdges(any(), any())).thenReturn(mock(PropertyGraphPrinter.class));
        when(targetConfig.createPrinterForNodes(any(), any())).thenReturn(mock(PropertyGraphPrinter.class));

        return  new QueryTask(mockQueries,
                getMockClient(traversal),
                targetConfig,
                false,
                10000L,
                new Status(StatusOutputFormat.Description, "query results test"),
                new AtomicInteger(),
                structuredOutput,
                new AllLabels(NodeLabelStrategy.nodeLabelsOnly),
                new AllLabels(EdgeLabelStrategy.edgeLabelsOnly),
                exportStats
        );
    }

    private Collection<ExportSpecification> createExportSpecifications(ExportStats exportStats) {
        Collection<ExportSpecification> exportSpecifications = new ArrayList<>();
        exportSpecifications.add(new ExportSpecification(
                GraphElementType.nodes,
                new AllLabels(NodeLabelStrategy.nodeLabelsOnly),
                GremlinFilters.EMPTY,
                exportStats,
                false,
                new FeatureToggles(Collections.EMPTY_SET)
        ));
        exportSpecifications.add(new ExportSpecification(
                GraphElementType.edges,
                new AllLabels(NodeLabelStrategy.nodeLabelsOnly),
                GremlinFilters.EMPTY,
                exportStats,
                false,
                new FeatureToggles(Collections.EMPTY_SET)
        ));
        return exportSpecifications;
    }

}
