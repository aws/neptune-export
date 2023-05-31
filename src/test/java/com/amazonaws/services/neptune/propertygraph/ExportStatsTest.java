package com.amazonaws.services.neptune.propertygraph;

import com.amazonaws.services.neptune.propertygraph.schema.GraphSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExportStatsTest {


    @Test
    public void testExportStats() throws JsonProcessingException {
        ExportStats stats = new ExportStats();
        ObjectNode neptuneExportNode = JsonNodeFactory.instance.objectNode();
        GraphSchema schema = GraphSchema.fromJson(new ObjectMapper().readTree(
                "{\"nodes\":[" +
                            "{\"label\":\"node1\"," +
                                "\"properties\":[" +
                                    "{\"property\":\"prop1\",\"dataType\":\"String\",\"isMultiValue\":false,\"isNullable\":false,\"allTypes\":[\"String\"]}," +
                                    "{\"property\":\"prop2\",\"dataType\":\"Double\",\"isMultiValue\":true,\"isNullable\":true,\"allTypes\":[\"Double\",\"Float\"]}]}," +
                            "{\"label\":\"node2\"," +
                            "\"properties\":[]}" +
                        "]," +
                        "\"edges\":[" +
                            "{\"label\":\"edge1\"," +
                                "\"properties\":[" +
                                    "{\"property\":\"prop1\",\"dataType\":\"String\",\"isMultiValue\":false,\"isNullable\":false,\"allTypes\":[\"String\"]}," +
                                    "{\"property\":\"prop2\",\"dataType\":\"Double\",\"isMultiValue\":true,\"isNullable\":true,\"allTypes\":[\"Double\",\"Float\"]}]}," +
                            "{\"label\":\"edge2\"," +
                            "\"properties\":[]}" +
                        "]}"
        ));

        stats.incrementNodeStats(new Label("node1"));
        stats.incrementNodeStats(new Label("node2"));
        stats.incrementEdgeStats(new Label("edge1"));
        stats.incrementEdgeStats(new Label("edge2"));

        stats.addTo(neptuneExportNode, schema);

        String formattedStats = stats.formatStats(schema);

        String expectedStats =
                "Source:\n" +
                "  Nodes: 0\n" +
                "  Edges: 0\n" +
                "Export:\n" +
                "  Nodes: 2\n" +
                "  Edges: 2\n" +
                "  Properties: 0\n" +
                "Details:\n" +
                "  Nodes: \n" +
                "    node1: 1\n" +
                "        |_ prop1 {propertyCount=0, minCardinality=-1, maxCardinality=-1, recordCount=0, dataTypeCounts=[]}\n" +
                "        |_ prop2 {propertyCount=0, minCardinality=-1, maxCardinality=-1, recordCount=0, dataTypeCounts=[]}\n" +
                "    node2: 1\n" +
                "  Edges: \n" +
                "    edge2: 1\n" +
                "    edge1: 1\n" +
                "        |_ prop1 {propertyCount=0, minCardinality=-1, maxCardinality=-1, recordCount=0, dataTypeCounts=[]}\n" +
                "        |_ prop2 {propertyCount=0, minCardinality=-1, maxCardinality=-1, recordCount=0, dataTypeCounts=[]}\n";

        assertEquals(expectedStats, formattedStats);
    }
}
