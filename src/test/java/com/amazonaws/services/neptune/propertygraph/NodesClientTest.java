package com.amazonaws.services.neptune.propertygraph;

import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.propertygraph.io.GraphElementHandler;
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
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
                Range.ALL, new AllLabels(mock(LabelStrategy.class)), GremlinFilters.EMPTY);

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

}
