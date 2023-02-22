package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertTrue;

public class CreatePgConfigIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testCreatePgConfig() throws FileNotFoundException {
        String[] command = {"create-pg-config", "-e", neptuneEndpoint, "-d", outputDir.getPath()};
        NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testCreatePgConfig"), resultDir);
    }

}
