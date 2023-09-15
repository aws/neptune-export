package com.amazonaws.services.neptune.propertygraph;

import com.amazonaws.services.neptune.propertygraph.io.result.ExportPGNodeResult;
import com.amazonaws.services.neptune.propertygraph.io.result.PGEdgeResult;
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy.edgeAndVertexLabels;
import static com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy.edgeLabelsOnly;
import static com.amazonaws.services.neptune.propertygraph.NodeLabelStrategy.nodeLabelsOnly;
import static org.junit.Assert.assertEquals;

public class NodeLabelStrategyTest {

    private final GraphTraversalSource gmodern;
    private final Map<String, Object> inputMap;
    private final PGResult pgNodeResult;
    private final List<String> labels;

    public NodeLabelStrategyTest() {
        gmodern = TinkerFactory.createModern().traversal();

        labels = new ArrayList<>();
        labels.add("TestLabel");

        inputMap = new HashMap<>();
        inputMap.put("~label", labels);

        pgNodeResult = new ExportPGNodeResult(inputMap);
    }

    //Node Labels Only

    @Test
    public void shouldGetLabelsFromModernGraph() {
        Collection<Label> labels = nodeLabelsOnly.getLabels(gmodern);

        Collection<Label> expected = new HashSet<>();
        expected.add(new Label("person"));
        expected.add(new Label("software"));

        assertEquals(expected, labels);
    }

    @Test
    public void shouldGetLabelForMap() {
        assertEquals(new Label(labels), nodeLabelsOnly.getLabelFor(inputMap));
    }

    @Test
    public void shouldGetLabelForPgEdgeResult() {
        assertEquals(new Label(labels), nodeLabelsOnly.getLabelFor(pgNodeResult));
    }
}
