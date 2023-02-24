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
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.sparql.util.compose.DatasetLib;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.IO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractExportIntegrationTest {

    protected static String neptuneEndpoint;
    protected File outputDir;

    @Rule
    public TemporaryFolder tempFolder= new TemporaryFolder();

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

    private static void fillDbWithTestData(String neptuneEndpoint) {
        //TODO:: For now assume that correct data is pre-loaded into DB.
//        Cluster cluster = Cluster.build(neptuneEndpoint).enableSsl(true).create();
//        GraphTraversalSource g = traversal().withRemote(DriverRemoteConnection.using(cluster, "g"));
//
//        g.V().drop().iterate();
//        g.io("http://www.raw.githubusercontent.com/krlawrence/graph/v277/sample-data/air-routes-small.graphml").with(IO.reader,IO.graphml).read().iterate();
    }

    protected void assertEquivalentResults(File expected, File actual) {
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

    protected boolean areJsonContentsEqual(File expected, File actual) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode expectedTree = mapper.readTree(expected);
            JsonNode actualTree = mapper.readTree(actual);

            return expectedTree.equals(actualTree);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean areDirContentsEquivalent(String expectedPath, String actualPath, GraphSchema config) {
        File expectedDir = new File(expectedPath);
        File actualDir = new File(actualPath);

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

    protected boolean areLabelledDirContentsEquivalent(File expectedDir, File actualDir, String label) {
        List<String> expectedNodes = new ArrayList<>();
        List<String> actualNodes = new ArrayList<>();
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
