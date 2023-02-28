package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import org.junit.Test;

import java.io.File;

public class CreatePgConfigIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testCreatePgConfig() {
        final String[] command = {"create-pg-config", "-e", neptuneEndpoint, "-d", outputDir.getPath()};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testCreatePgConfig"), resultDir);
    }

}
