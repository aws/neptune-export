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
import com.amazonaws.services.neptune.propertygraph.NodeLabelStrategy;
import com.amazonaws.services.neptune.propertygraph.NodesClient;
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

public class NodeWriterTest {

    private NodesClient client;

    @Before
    public void setup() {
        GraphTraversalSource graphTraversalSource = TinkerFactory.createModern().traversal();
        ExportStats mockStats = mock(ExportStats.class);
        FeatureToggles mockFeatures = mock(FeatureToggles.class);
        when(mockFeatures.containsFeature(Mockito.any())).thenReturn(false);

        client = new NodesClient(graphTraversalSource, false, mockStats, mockFeatures);
    }

    @Test
    public void testHandle() throws IOException {
        PGResult nodeResult = getPGNodeResult("1");
        PropertyGraphStringPrinter pgPrinter = new PropertyGraphStringPrinter();
        NodeWriter nodeWriter = new NodeWriter(pgPrinter);

        nodeWriter.handle(nodeResult, true);

        String expected = "Start Row\n" +
                "Node[1, Labels{person, }] Properties{name:[marko], age:[29], } \n";

        assertEquals(expected, pgPrinter.getOutput());
    }

    private PGResult getPGNodeResult(String id) {
        final PGResult[] result = {null};
        GraphElementHandler<PGResult> handler = new GraphElementHandler<PGResult>() {
            @Override
            public void handle(PGResult element, boolean allowTokens) throws IOException {
                if(element.getId().equals(id)) {
                    result[0] = element;
                }
            }
            @Override
            public void close() throws Exception {}
        };
        client.queryForValues(handler, Range.ALL, new AllLabels(NodeLabelStrategy.nodeLabelsOnly),
                GremlinFilters.EMPTY, new GraphElementSchemas());
        return result[0];
    }

}
