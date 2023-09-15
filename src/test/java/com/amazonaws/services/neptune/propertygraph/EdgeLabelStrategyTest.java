package com.amazonaws.services.neptune.propertygraph;

import com.amazonaws.services.neptune.propertygraph.io.result.PGEdgeResult;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy.edgeLabelsOnly;
import static com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy.edgeAndVertexLabels;
import static org.junit.Assert.assertEquals;

public class EdgeLabelStrategyTest {

    private final GraphTraversalSource gmodern;
    private final Map<String, Object> inputMap;
    private final PGEdgeResult pgEdgeResult;
    private final List<String> fromLabels;
    private final List<String> toLabels;

    public EdgeLabelStrategyTest() {
        gmodern = TinkerFactory.createModern().traversal();

        inputMap = new HashMap<>();
        inputMap.put("~label", "TestLabel");
        inputMap.put("~from", "FromID");
        inputMap.put("~to", "ToID");
        fromLabels = new ArrayList<>();
        toLabels = new ArrayList<>();
        fromLabels.add("FromLabel");
        toLabels.add("ToLabels");
        inputMap.put("~fromLabels", fromLabels);
        inputMap.put("~toLabels", toLabels);

        pgEdgeResult = new PGEdgeResult(inputMap);
    }

    //Edge Labels Only

    @Test
    public void shouldGetEdgeLabelsFromModernGraph() {
        Collection<Label> labels = edgeLabelsOnly.getLabels(gmodern);

        Collection<Label> expected = new HashSet<>();
        expected.add(new Label("knows"));
        expected.add(new Label("created"));

        assertEquals(expected, labels);
    }

    @Test
    public void shouldGetEdgeLabelForMap() {
        assertEquals(new Label("TestLabel"), edgeLabelsOnly.getLabelFor(inputMap));
    }

    @Test
    public void shouldGetEdgeLabelForPgEdgeResult() {
        assertEquals(new Label("TestLabel"), edgeLabelsOnly.getLabelFor(pgEdgeResult));
    }

    // Edge and Vertex Labels

    @Test
    public void shouldGetEdgeAndVertexLabelsFromModernGraph() {
        Collection<Label> labels = edgeAndVertexLabels.getLabels(gmodern);

        Collection<Label> expected = new HashSet<>();
        expected.add(new Label("(person)-knows-(person)"));
        expected.add(new Label("(person)-created-(software)"));

        assertEquals(expected, labels);
    }

    @Test
    public void shouldGetEdgeAndVertexLabelForMap() {
        assertEquals(new Label("TestLabel", fromLabels, toLabels), edgeAndVertexLabels.getLabelFor(inputMap));
    }

    @Test
    public void shouldGetEdgeAndVertexLabelForPgEdgeResult() {
        assertEquals(new Label("TestLabel", fromLabels, toLabels), edgeAndVertexLabels.getLabelFor(pgEdgeResult));
    }

}
