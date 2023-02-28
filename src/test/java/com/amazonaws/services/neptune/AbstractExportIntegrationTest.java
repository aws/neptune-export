package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.io.JsonResource;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import com.amazonaws.services.neptune.propertygraph.schema.GraphSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractExportIntegrationTest {

    protected static String neptuneEndpoint;
    protected File outputDir;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupClass(){
        neptuneEndpoint = System.getenv("NeptuneEndpoint");
        assertNotNull("endpoint must be provided through \"NeptuneEndpoint\" environment variable", neptuneEndpoint);

        fillDbWithTestData(neptuneEndpoint);
    }

    @Before
    public void setup() throws IOException {
        outputDir = tempFolder.newFolder();
    }

    private static void fillDbWithTestData(final String neptuneEndpoint) {
        //TODO:: For now assume that correct data is pre-loaded into DB.
//        Cluster cluster = Cluster.build(neptuneEndpoint).enableSsl(true).create();
//        GraphTraversalSource g = traversal().withRemote(DriverRemoteConnection.using(cluster, "g"));
    }

    protected void assertEquivalentResults(final File expected, final File actual) {
        GraphSchema config = null;
        try {
            config = new JsonResource<GraphSchema, Boolean>(
                    "Config file",
                    new URI(expected.getPath() + "/config.json"),
                    GraphSchema.class).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        assertTrue("stats.json does not match expected results", areJsonContentsEqual(expected.listFiles((dir, name) -> name.equals("stats.json"))[0], actual.listFiles((dir, name) -> name.equals("stats.json"))[0]));
        assertTrue("config.json does not match expected results", areJsonContentsEqual(expected.listFiles((dir, name) -> name.equals("config.json"))[0], actual.listFiles((dir, name) -> name.equals("config.json"))[0]));
        if (expected.listFiles(((dir, name) -> name.equals("nodes"))).length >= 1) {
            assertTrue("nodes directory does not match expected results", areDirContentsEquivalent(expected + "/nodes", actual + "/nodes", config));
        }
        if (expected.listFiles(((dir, name) -> name.equals("edges"))).length >= 1) {
            assertTrue("edges directory does not match expected results", areDirContentsEquivalent(expected + "/edges", actual + "/edges", config));
        }
    }

    protected boolean areJsonContentsEqual(final File expected, final File actual) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode expectedTree = mapper.readTree(expected);
            JsonNode actualTree = mapper.readTree(actual);

            return expectedTree.equals(actualTree);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean areDirContentsEquivalent(final String expectedPath, final String actualPath, final GraphSchema config) {
        final File expectedDir = new File(expectedPath);
        final File actualDir = new File(actualPath);

        assertTrue("Expected path to a directory", expectedDir.isDirectory() && actualDir.isDirectory());

        if(config.hasNodeSchemas()) {
            GraphElementSchemas nodeSchemas = config.graphElementSchemasFor(GraphElementType.nodes);
            for(Label l : nodeSchemas.labels()) {
                String label = l.fullyQualifiedLabel();

                if(!areLabelledDirContentsEquivalent(expectedDir, actualDir, label)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean areLabelledDirContentsEquivalent(final File expectedDir, final File actualDir, final String label) {
        final List<String> expectedNodes = new ArrayList<>();
        final List<String> actualNodes = new ArrayList<>();
        for(File file : expectedDir.listFiles((dir, name) -> name.startsWith(label))){
            try {
                CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.RFC4180);
                Collection<String> list = parser.stream()
                        .map(csvRecord -> {return csvRecord.toString();})
                        .collect(Collectors.toList());
                expectedNodes.addAll(list);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for(File file : actualDir.listFiles((dir, name) -> name.startsWith(label))){
            try {
                CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.RFC4180);
                Collection<String> list = parser.stream()
                        .map(csvRecord -> {return csvRecord.toString();})
                        .collect(Collectors.toList());
                actualNodes.addAll(list);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return expectedNodes.containsAll(actualNodes) && actualNodes.containsAll(expectedNodes);
    }
}
