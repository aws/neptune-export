package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import com.amazonaws.services.neptune.propertygraph.io.JsonResource;
import com.amazonaws.services.neptune.propertygraph.schema.GraphSchema;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;

public class ExportPgFromQueriesIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testExportPgFromQueries() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().hasLabel('airport').has('runways', gt(2)).project('code', 'runways', 'city', 'country').by('code').by('runways').by('city').by('country')"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgFromQueries"), resultDir);
    }

    @Test
    public void testExportPgFromQueriesWithStructuredOutput() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().union(hasLabel('airport'), outE()).elementMap()",
                "--include-type-definitions",
                "--structured-output"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentStructuredOutput(new File("src/test/resources/IntegrationTest/testExportPgFromQueriesStructuredOutput"), resultDir);
    }

    @Test
    public void testExportPgFromQueriesWithStructuredOutputWithEdgeAndVertexLabels() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().union(hasLabel('airport'), outE()).elementMap()",
                "--include-type-definitions", "--edge-label-strategy", "edgeAndVertexLabels",
                "--structured-output"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentStructuredOutput(new File("src/test/resources/IntegrationTest/testExportPgFromQueriesStructuredOutput"), resultDir);
    }

    @Override
    protected void assertEquivalentResults(final File expected, final File actual) {
        assertTrue("queries.json does not match expected results", areJsonContentsEqual(expected.listFiles((dir, name) -> name.equals("queries.json"))[0], actual.listFiles((dir, name) -> name.equals("queries.json"))[0]));
        for (File expectedResultsDir : expected.listFiles((dir, name) -> name.equals("results"))[0].listFiles()) {
            assertTrue(expectedResultsDir.isDirectory());
            String dirName = expectedResultsDir.getName();
            assertTrue("results/"+dirName+" directory does not match expected results", areLabelledDirContentsEquivalent(expectedResultsDir, new File(actual+"/results/"+dirName), dirName));
        }
    }

    private void assertEquivalentStructuredOutput(final File expected, final File actual) {
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

        if (expected.listFiles(((dir, name) -> name.equals("nodes"))).length >= 1) {
            assertTrue("nodes directory does not match expected results", areDirContentsEquivalent(expected + "/nodes", actual + "/nodes", config));
        }
    }



}
