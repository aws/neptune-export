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

import com.amazonaws.services.neptune.propertygraph.io.result.QueriesNodeResult;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class QueriesNodeResultTest {

    private final GraphTraversalSource gModern = TinkerFactory.createModern().traversal();

    /**
     * Wrap TinkerPop's modern graph g.V(1) in a QueriesNodeResult and asserts correct results
     */
    @Test
    public void testStandardNodeElementMap() {
        QueriesNodeResult modernV1 = new QueriesNodeResult(gModern.V("1").elementMap().next());

        assertEquals(GraphElementType.nodes, modernV1.getGraphElementType());
        assertEquals(Collections.singletonList("person"), modernV1.getLabel());
        assertEquals("1", modernV1.getId());

        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "marko");
        properties.put("age", 29);
        assertEquals(properties, modernV1.getProperties());
        assertThrows(IllegalStateException.class, () -> {modernV1.getFrom();});
        assertThrows(IllegalStateException.class, () -> {modernV1.getTo();});
        assertThrows(IllegalStateException.class, () -> {modernV1.getFromLabels();});
        assertThrows(IllegalStateException.class, () -> {modernV1.getToLabels();});
    }

    @Test
    public void testNodeWithNoProperties() {
        Map v1 = gModern.V("1").elementMap().next();
        v1.remove("name");
        v1.remove("age");
        QueriesNodeResult queriesNodeResult = new QueriesNodeResult(v1);

        assertEquals(new HashMap<String, Object>(), queriesNodeResult.getProperties());
    }

}
