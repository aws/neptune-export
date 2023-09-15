package com.amazonaws.services.neptune.propertygraph.io;

import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.propertygraph.AllLabels;
import com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.EdgesClient;
import com.amazonaws.services.neptune.propertygraph.ExportStats;
import com.amazonaws.services.neptune.propertygraph.GremlinFilters;
import com.amazonaws.services.neptune.propertygraph.Range;
import com.amazonaws.services.neptune.propertygraph.io.result.PGEdgeResult;
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EdgeWriterTest {

    private EdgesClient client;

    @Before
    public void setup() {
        GraphTraversalSource graphTraversalSource = TinkerFactory.createModern().traversal();
        ExportStats mockStats = mock(ExportStats.class);
        FeatureToggles mockFeatures = mock(FeatureToggles.class);
        when(mockFeatures.containsFeature(Mockito.any())).thenReturn(false);

        client = new EdgesClient(graphTraversalSource, false, mockStats, mockFeatures);
    }

    @Test
    public void testHandle() throws IOException {
        PGEdgeResult edgeResult = getPGEdgeResult("7");
        PropertyGraphStringPrinter pgPrinter = new PropertyGraphStringPrinter();
        EdgeWriter edgeWriter = new EdgeWriter(pgPrinter, EdgeLabelStrategy.edgeLabelsOnly.getLabelFor(edgeResult));

        edgeWriter.handle(edgeResult, true);

        String expected = "Start Row\n" +
                "Edge[7, knows, 1, 2] Properties{weight:0.5, } \n";

        assertEquals(expected, pgPrinter.getOutput());
    }

    private PGEdgeResult getPGEdgeResult(String id) {
        final PGEdgeResult[] result = {null};
        GraphElementHandler<PGResult> handler = new GraphElementHandler<PGResult>() {
            @Override
            public void handle(PGResult element, boolean allowTokens) throws IOException {
                if(element.getId().equals(id)) {
                    result[0] = (PGEdgeResult) element;
                }
            }
            @Override
            public void close() throws Exception {}
        };
        client.queryForValues(handler, Range.ALL, new AllLabels(EdgeLabelStrategy.edgeLabelsOnly),
                GremlinFilters.EMPTY, new GraphElementSchemas());
        return result[0];
    }

}
