package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import org.junit.Test;

import java.io.File;

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

    @Override
    protected void assertEquivalentResults(final File expected, final File actual) {
        assertTrue("queries.json does not match expected results", areJsonContentsEqual(expected.listFiles((dir, name) -> name.equals("queries.json"))[0], actual.listFiles((dir, name) -> name.equals("queries.json"))[0]));
        for (File expectedResultsDir : expected.listFiles((dir, name) -> name.equals("results"))[0].listFiles()) {
            assertTrue(expectedResultsDir.isDirectory());
            String dirName = expectedResultsDir.getName();
            assertTrue("results/"+dirName+" directory does not match expected results", areLabelledDirContentsEquivalent(expectedResultsDir, new File(actual+"/results/"+dirName), dirName));
        }
    }



}
