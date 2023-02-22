package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;


import static org.junit.Assert.*;

public class ExportPgIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testExportPgToCsv() throws FileNotFoundException {
        String[] command = {"export-pg", "-e", neptuneEndpoint, "-d", outputDir.getPath()};
        NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgToCsv"), resultDir);
    }

}
