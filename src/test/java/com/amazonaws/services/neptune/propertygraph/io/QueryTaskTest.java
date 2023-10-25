package com.amazonaws.services.neptune.propertygraph.io;

import com.amazonaws.services.neptune.io.Status;
import com.amazonaws.services.neptune.io.StatusOutputFormat;
import com.amazonaws.services.neptune.propertygraph.AllLabels;
import com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.NamedQuery;
import com.amazonaws.services.neptune.propertygraph.NeptuneGremlinClient;
import com.amazonaws.services.neptune.propertygraph.NodeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.schema.FileSpecificLabelSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Test;

import java.io.IOException;
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

    private NeptuneGremlinClient.QueryClient getMockClient(GraphTraversal traversal) {
        NeptuneGremlinClient.QueryClient mockClient = mock(NeptuneGremlinClient.QueryClient.class);
        ResultSet results = mock(ResultSet.class);

        when(results.stream()).thenReturn(traversal.toStream().map(r -> new Result(r)));
        when(mockClient.submit(any(), any())).thenReturn(results);

        return mockClient;
    }

    private QueryTask createQueryTask(GraphTraversal traversal, boolean structuredOutput) throws IOException {
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
                new AllLabels(EdgeLabelStrategy.edgeLabelsOnly)
        );
    }

}
