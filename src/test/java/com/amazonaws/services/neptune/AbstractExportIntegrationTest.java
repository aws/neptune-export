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

package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.io.JsonResource;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import com.amazonaws.services.neptune.propertygraph.schema.GraphSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.json.compare.CompareMode;
import io.json.compare.JSONCompare;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractExportIntegrationTest {

    protected static String neptuneEndpoint;
    protected File outputDir;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupClass(){
        neptuneEndpoint = System.getenv("NEPTUNE_ENDPOINT");
        assertNotNull("endpoint must be provided through \"NEPTUNE_ENDPOINT\" environment variable", neptuneEndpoint);

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

        assertJSONContentMatches(expected.listFiles((dir, name) -> name.equals("stats.json"))[0],
                actual.listFiles((dir, name) -> name.equals("stats.json"))[0],
                "stats.json does not match expected results");
        assertJSONContentMatches(expected.listFiles((dir, name) -> name.equals("config.json"))[0],
                actual.listFiles((dir, name) -> name.equals("config.json"))[0],
                "config.json does not match expected results");

        if (expected.listFiles(((dir, name) -> name.equals("nodes"))).length >= 1) {
            assertTrue("nodes directory does not match expected results", areDirContentsEquivalent(expected + "/nodes", actual + "/nodes", config));
        }
        if (expected.listFiles(((dir, name) -> name.equals("edges"))).length >= 1) {
            assertTrue("edges directory does not match expected results", areDirContentsEquivalent(expected + "/edges", actual + "/edges", config));
        }
    }

    protected void assertJSONContentMatches(final File expected, final File actual, final String message) {
        final Set<CompareMode> jsonCompareModes = new HashSet();
        jsonCompareModes.add(CompareMode.JSON_OBJECT_NON_EXTENSIBLE);
        jsonCompareModes.add(CompareMode.JSON_ARRAY_NON_EXTENSIBLE);
        jsonCompareModes.add(CompareMode.REGEX_DISABLED);

        final ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode expectedTree = mapper.readTree(expected);
            JsonNode actualTree = mapper.readTree(actual);

            JSONCompare.assertMatches(expectedTree, actualTree, jsonCompareModes, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean areDirContentsEquivalent(final String expectedPath, final String actualPath, final GraphSchema config) {
        final File expectedDir = new File(expectedPath);
        final File actualDir = new File(actualPath);

        assertTrue("Expected path to a directory", expectedDir.isDirectory() && actualDir.isDirectory());

        GraphElementSchemas schemas;

        if(expectedDir.getName().equals("nodes")) {
            if(!config.hasNodeSchemas()) {
                return true;
            }
            schemas = config.graphElementSchemasFor(GraphElementType.nodes);
        } else if(expectedDir.getName().equals("edges")) {
            if(!config.hasEdgeSchemas()) {
                return true;
            }
            schemas = config.graphElementSchemasFor(GraphElementType.edges);
        } else {
            throw new IllegalArgumentException("directory must end in either /nodes or /edges");
        }

        for(Label l : schemas.labels()) {
            String label = l.fullyQualifiedLabel();

            if(!areLabelledDirContentsEquivalent(expectedDir, actualDir, label)) {
                return false;
            }
        }

        return true;
    }

    protected boolean areLabelledDirContentsEquivalent(final File expectedDir, final File actualDir, final String label) {
        final String escapedLabel = label.replaceAll("\\(", "%28").replaceAll("\\)", "%29");

        final List<String> expectedNodes = new ArrayList<>();
        final List<String> actualNodes = new ArrayList<>();
        for(File file : expectedDir.listFiles((dir, name) -> name.startsWith(escapedLabel))){
            try {
                CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.RFC4180);
                Collection<String> list = parser.stream()
                        .map(csvRecord -> {
                            List<String> properties = csvRecord.toList();
                            Collections.sort(properties);
                            return properties.toString();
                        })
                        .collect(Collectors.toList());
                expectedNodes.addAll(list);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for(File file : actualDir.listFiles((dir, name) -> name.startsWith(escapedLabel))){
            try {
                CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.RFC4180);
                Collection<String> list = parser.stream()
                        .map(csvRecord -> {
                            List<String> properties = csvRecord.toList();
                            Collections.sort(properties);
                            return properties.toString();
                        })
                        .collect(Collectors.toList());
                actualNodes.addAll(list);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return expectedNodes.containsAll(actualNodes) && actualNodes.containsAll(expectedNodes);
    }
}
