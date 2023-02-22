package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertTrue;

public class ExportPgFromConfigIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testExportPgFromConfig() throws FileNotFoundException {
        String[] command = {"export-pg-from-config", "-e", neptuneEndpoint,
                "-c", "src/test/resources/IntegrationTest/ExportPgFromConfigIntegrationTest/input/config.json",
                "-d", outputDir.getPath()};
        NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/ExportPgFromConfigIntegrationTest/testExportPgFromConfig"), resultDir);
    }

}