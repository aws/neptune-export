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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EdgesClientTest {

    private EdgesClient client;
    private GraphTraversalSource graphTraversalSource;
    private ExportStats mockStats;
    private FeatureToggles mockFeatures;

    @Before
    public void setup() {
        graphTraversalSource = TinkerFactory.createModern().traversal();
        mockStats = mock(ExportStats.class);
        mockFeatures = mock(FeatureToggles.class);
        when(mockFeatures.containsFeature(Mockito.any())).thenReturn(false);

        client = new EdgesClient(graphTraversalSource, false, mockStats, mockFeatures);
    }

    @Test
    public void testQueryForSchema() throws JsonProcessingException {
        GraphSchema schema = new GraphSchema();
        client.queryForSchema(
                new GraphElementHandler<Map<?, Object>>() {
                    @Override
                    public void handle(Map<?, Object> properties, boolean allowTokens) throws IOException {
                        schema.update(GraphElementType.edges, properties, allowTokens);
                    }
                    @Override
                    public void close() {}
                },
                Range.ALL, new AllLabels(EdgeLabelStrategy.edgeLabelsOnly), GremlinFilters.EMPTY);

        JsonNode expectedSchema = new ObjectMapper().readTree(
                "{\n" +
                        "  \"edges\" : [ {\n" +
                        "    \"label\" : \"knows\",\n" +
                        "    \"properties\" : [ {\n" +
                        "      \"property\" : \"weight\",\n" +
                        "      \"dataType\" : \"Double\",\n" +
                        "      \"isMultiValue\" : false,\n" +
                        "      \"isNullable\" : false,\n" +
                        "      \"allTypes\" : [ \"Double\" ]\n" +
                        "    } ]\n" +
                        "  }, {\n" +
                        "    \"label\" : \"created\",\n" +
                        "    \"properties\" : [ {\n" +
                        "      \"property\" : \"weight\",\n" +
                        "      \"dataType\" : \"Double\",\n" +
                        "      \"isMultiValue\" : false,\n" +
                        "      \"isNullable\" : false,\n" +
                        "      \"allTypes\" : [ \"Double\" ]\n" +
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
        client.queryForValues(handler, Range.ALL, new AllLabels(EdgeLabelStrategy.edgeLabelsOnly),
                GremlinFilters.EMPTY, new GraphElementSchemas());

        assertEquals(Arrays.asList("7","8","9","10","11","12"), ids);
    }

}
