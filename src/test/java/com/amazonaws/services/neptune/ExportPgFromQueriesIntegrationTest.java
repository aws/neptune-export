package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class ExportPgFromQueriesIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testExportPgFromQueries() throws FileNotFoundException {
        String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().hasLabel('airport').has('runways', gt(2)).project('code', 'runways', 'city', 'country').by('code').by('runways').by('city').by('country')"
        };
        NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgFromQueries"), resultDir);
    }

    @Override
    protected void assertEquivalentResults(File expected, File actual) throws FileNotFoundException {
        assertTrue("queries.json does not match expected results", areJsonContentsEqual(expected.listFiles((dir, name) -> name.equals("queries.json"))[0], actual.listFiles((dir, name) -> name.equals("queries.json"))[0]));
        for (File expectedResultsDir : expected.listFiles((dir, name) -> name.equals("results"))[0].listFiles()) {
            assertTrue(expectedResultsDir.isDirectory());
            String dirName = expectedResultsDir.getName();
            assertTrue("results/"+dirName+" directory does not match expected results", areLabelledDirContentsEquivalent(expectedResultsDir, new File(actual+"/results/"+dirName), dirName));
        }
    }



}
