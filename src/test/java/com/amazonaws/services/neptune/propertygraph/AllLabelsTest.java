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

import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.propertygraph.io.result.ExportPGNodeResult;
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
public class AllLabelsTest {

    @Test
    public void shouldGetLabelForPGResult() {
        LabelStrategy labelStrategy = NodeLabelStrategy.nodeLabelsOnly;
        AllLabels allLabels = new AllLabels(labelStrategy);

        Map<String, Object> input = new HashMap<>();
        List<String> labels = new ArrayList<>();
        labels.add("TestLabel");
        input.put("~label", labels);
        PGResult pgResult = new ExportPGNodeResult(input);

        Label label = allLabels.getLabelFor(pgResult);

        assertEquals(new Label(labels), label);
    }

    @Test
    public void shouldGetLabelForInputMap() {
        LabelStrategy labelStrategy = NodeLabelStrategy.nodeLabelsOnly;
        AllLabels allLabels = new AllLabels(labelStrategy);

        Map<String, Object> input = new HashMap<>();
        List<String> labels = new ArrayList<>();
        labels.add("TestLabel");
        input.put("~label", labels);

        Label label = allLabels.getLabelFor(input);

        assertEquals(new Label(labels), label);
    }

    @Test
    public void shouldNotAddAnyLabelFiltersWhenApplied() {
        AllLabels allLabels = new AllLabels(NodeLabelStrategy.nodeLabelsOnly);

        AnonymousTraversalSource<GraphTraversalSource> traversalSource = AnonymousTraversalSource.traversal();
        GraphTraversalSource g = traversalSource.withGraph(EmptyGraph.instance());

        GraphTraversal<? extends Element, ?> traversal =
                allLabels.apply(g.V(), new FeatureToggles(Collections.emptyList()), GraphElementType.nodes);

        assertEquals("__.V()",
                GremlinQueryDebugger.queryAsString(traversal));
    }

    @Test
    public void shouldNotAddAnyLabelFiltersWhenAppliedForEdges() {
        AllLabels allLabels = new AllLabels(EdgeLabelStrategy.edgeLabelsOnly);

        AnonymousTraversalSource<GraphTraversalSource> traversalSource = AnonymousTraversalSource.traversal();
        GraphTraversalSource g = traversalSource.withGraph(EmptyGraph.instance());

        GraphTraversal<? extends Element, ?> traversal =
                allLabels.apply(g.E(), new FeatureToggles(Collections.emptyList()), GraphElementType.nodes);

        assertEquals("__.E()",
                GremlinQueryDebugger.queryAsString(traversal));
    }

    @Test
    public void getPropertiesForLabelsTest() {
        AllLabels allLabels = new AllLabels(NodeLabelStrategy.nodeLabelsOnly);

        GraphElementSchemas graphElementSchemas = new GraphElementSchemas();
        Label label = new Label("test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("Test Prop int", 1);
        properties.put("Test Prop String", "String");
        properties.put("Test Prop Array", new int[]{1, 2});

        graphElementSchemas.update(label, properties, false);

        String[] propertyLabels = allLabels.getPropertiesForLabels(graphElementSchemas);
        assertEquals(new String[]{"Test Prop String", "Test Prop Array", "Test Prop int"}, propertyLabels);
    }

}
