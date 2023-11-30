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
