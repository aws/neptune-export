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
import com.amazonaws.services.neptune.propertygraph.io.GraphElementHandler;
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import com.amazonaws.services.neptune.propertygraph.schema.GraphSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodesClientTest {

    private NodesClient client;
    private GraphTraversalSource graphTraversalSource;
    private ExportStats mockStats;
    private FeatureToggles mockFeatures;

    @Before
    public void setup() {
        graphTraversalSource = TinkerFactory.createModern().traversal();
        mockStats = mock(ExportStats.class);
        mockFeatures = mock(FeatureToggles.class);
        when(mockFeatures.containsFeature(Mockito.any())).thenReturn(false);

        client = new NodesClient(graphTraversalSource, false, mockStats, mockFeatures);
    }

    @Test
    public void testQueryForSchema() throws JsonProcessingException {
        GraphSchema schema = new GraphSchema();
        client.queryForSchema(
                new GraphElementHandler<Map<?, Object>>() {
                    @Override
                    public void handle(Map<?, Object> properties, boolean allowTokens) throws IOException {
                        schema.update(GraphElementType.nodes, properties, allowTokens);
                    }
                    @Override
                    public void close() {}
                },
                Range.ALL, new AllLabels(NodeLabelStrategy.nodeLabelsOnly), GremlinFilters.EMPTY);

        JsonNode expectedSchema = new ObjectMapper().readTree(
                "{\n" +
                        "  \"nodes\" : [ {\n" +
                        "    \"label\" : \"software\",\n" +
                        "    \"properties\" : [ {\n" +
                        "      \"property\" : \"name\",\n" +
                        "      \"dataType\" : \"String\",\n" +
                        "      \"isMultiValue\" : false,\n" +
                        "      \"isNullable\" : false,\n" +
                        "      \"allTypes\" : [ \"String\" ]\n" +
                        "    }, {\n" +
                        "      \"property\" : \"lang\",\n" +
                        "      \"dataType\" : \"String\",\n" +
                        "      \"isMultiValue\" : false,\n" +
                        "      \"isNullable\" : false,\n" +
                        "      \"allTypes\" : [ \"String\" ]\n" +
                        "    } ]\n" +
                        "  }, {\n" +
                        "    \"label\" : \"person\",\n" +
                        "    \"properties\" : [ {\n" +
                        "      \"property\" : \"name\",\n" +
                        "      \"dataType\" : \"String\",\n" +
                        "      \"isMultiValue\" : false,\n" +
                        "      \"isNullable\" : false,\n" +
                        "      \"allTypes\" : [ \"String\" ]\n" +
                        "    }, {\n" +
                        "      \"property\" : \"age\",\n" +
                        "      \"dataType\" : \"Integer\",\n" +
                        "      \"isMultiValue\" : false,\n" +
                        "      \"isNullable\" : false,\n" +
                        "      \"allTypes\" : [ \"Integer\" ]\n" +
                        "    } ]\n" +
                        "  } ]\n" +
                        "}"
        );
        assertEquals(expectedSchema, schema.toJson(false));
    }

    @Test
    public void testQueryForValues() {
        List<String> ids = new ArrayList<>();
        GraphElementHandler<PGResult> handler = new GraphElementHandler<PGResult>() {
            @Override
            public void handle(PGResult element, boolean allowTokens) throws IOException {
                ids.add(element.getId());
                assertFalse(allowTokens);
            }
            @Override
            public void close() throws Exception {}
        };
        client.queryForValues(handler, Range.ALL, new AllLabels(NodeLabelStrategy.nodeLabelsOnly),
                GremlinFilters.EMPTY, new GraphElementSchemas());

        assertEquals(Arrays.asList("1","2","3","4","5","6"), ids);
    }

}
