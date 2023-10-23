package com.amazonaws.services.neptune.propertygraph.io;

import com.amazonaws.services.neptune.propertygraph.io.result.QueriesEdgeResult;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class QueriesEdgeResultTest {

    private final GraphTraversalSource gModern = TinkerFactory.createModern().traversal();

    /**
     * Wrap TinkerPop's modern graph g.E(9) in a QueriesEdgeResult and asserts correct results
     */
    @Test
    public void testStandardEdgeElementMap() {
        QueriesEdgeResult modernE9 = new QueriesEdgeResult(gModern.E("9").elementMap().next());

        assertEquals(GraphElementType.edges, modernE9.getGraphElementType());
        assertEquals(Collections.singletonList("created"), modernE9.getLabel());
        assertEquals("9", modernE9.getId());

        Map<String, Object> properties = new HashMap<>();
        properties.put("weight", 0.4);
        assertEquals(properties, modernE9.getProperties());
        assertEquals("1", modernE9.getFrom());
        assertEquals("3", modernE9.getTo());
        assertEquals(Collections.singletonList("person"), modernE9.getFromLabels());
        assertEquals(Collections.singletonList("software"), modernE9.getToLabels());
    }

    @Test
    public void testEdgeWithNoProperties() {
        Map e9 = gModern.E("9").elementMap().next();
        e9.remove("weight");
        QueriesEdgeResult queriesEdgeResult = new QueriesEdgeResult(e9);

        assertEquals(new HashMap<String, Object>(), queriesEdgeResult.getProperties());
    }

}
