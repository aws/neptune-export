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
import com.amazonaws.services.neptune.propertygraph.AllLabels;
import com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.EdgesClient;
import com.amazonaws.services.neptune.propertygraph.ExportStats;
import com.amazonaws.services.neptune.propertygraph.GremlinFilters;
import com.amazonaws.services.neptune.propertygraph.LabelStrategy;
import com.amazonaws.services.neptune.propertygraph.LabelsFilter;
import com.amazonaws.services.neptune.propertygraph.Range;
import com.amazonaws.services.neptune.propertygraph.io.result.PGEdgeResult;
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;
import com.amazonaws.services.neptune.propertygraph.io.result.QueriesEdgeResult;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
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
    private GraphTraversalSource gModern;

    @Before
    public void setup() {
        gModern = TinkerFactory.createModern().traversal();
        ExportStats mockStats = mock(ExportStats.class);
        FeatureToggles mockFeatures = mock(FeatureToggles.class);
        when(mockFeatures.containsFeature(Mockito.any())).thenReturn(false);

        client = new EdgesClient(gModern, false, mockStats, mockFeatures);
    }

    @Test
    public void shouldHandlePGEdgeResultWithEdgeLabelsOnly() throws IOException {
        PGEdgeResult edgeResult = getPGEdgeResult("7", new AllLabels(EdgeLabelStrategy.edgeLabelsOnly));
        PropertyGraphStringPrinter pgPrinter = new PropertyGraphStringPrinter();
        EdgeWriter edgeWriter = new EdgeWriter(pgPrinter, EdgeLabelStrategy.edgeLabelsOnly.getLabelFor(edgeResult));

        edgeWriter.handle(edgeResult, true);

        String expected = "Start Row\n" +
                "Edge[7, knows, 1, 2] Properties{weight:0.5, } \n";

        assertEquals(expected, pgPrinter.getOutput());
    }

    @Test
    public void shouldHandlePGEdgeResultWithEdgeAndVertexLabels() throws IOException {
        PGEdgeResult edgeResult = getPGEdgeResult("7", new AllLabels(EdgeLabelStrategy.edgeAndVertexLabels));
        PropertyGraphStringPrinter pgPrinter = new PropertyGraphStringPrinter();
        EdgeWriter edgeWriter = new EdgeWriter(pgPrinter, EdgeLabelStrategy.edgeAndVertexLabels.getLabelFor(edgeResult));

        edgeWriter.handle(edgeResult, true);

        String expected = "Start Row\n" +
                "Edge[7, knows, 1, 2, fromLabels{person, }, toLabels{person, }] Properties{weight:0.5, } \n";

        assertEquals(expected, pgPrinter.getOutput());
    }

    @Test
    public void shouldHandleQueriesEdgeResultWithEdgeLabelsOnly() throws IOException {
        QueriesEdgeResult edgeResult = getQueriesEdgeResult("7");
        PropertyGraphStringPrinter pgPrinter = new PropertyGraphStringPrinter();
        EdgeWriter edgeWriter = new EdgeWriter(pgPrinter, EdgeLabelStrategy.edgeLabelsOnly.getLabelFor(edgeResult));

        edgeWriter.handle(edgeResult, true);

        String expected = "Start Row\n" +
                "Edge[7, knows, 1, 2] Properties{weight:0.5, } \n";

        assertEquals(expected, pgPrinter.getOutput());
    }

    @Test
    public void shouldHandleQueriesEdgeResultWithEdgeAndVertexLabels() throws IOException {
        QueriesEdgeResult edgeResult = getQueriesEdgeResult("7");
        PropertyGraphStringPrinter pgPrinter = new PropertyGraphStringPrinter();
        EdgeWriter edgeWriter = new EdgeWriter(pgPrinter, EdgeLabelStrategy.edgeAndVertexLabels.getLabelFor(edgeResult));

        edgeWriter.handle(edgeResult, true);

        String expected = "Start Row\n" +
                "Edge[7, knows, 1, 2, fromLabels{person, }, toLabels{person, }] Properties{weight:0.5, } \n";

        assertEquals(expected, pgPrinter.getOutput());
    }

    private PGEdgeResult getPGEdgeResult(String id, LabelsFilter labelsFilter) {
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
        client.queryForValues(handler, Range.ALL, labelsFilter,
                GremlinFilters.EMPTY, new GraphElementSchemas());
        return result[0];
    }

    private QueriesEdgeResult getQueriesEdgeResult(String id) {
        return new QueriesEdgeResult(gModern.E(id).elementMap().next());
    }

}
