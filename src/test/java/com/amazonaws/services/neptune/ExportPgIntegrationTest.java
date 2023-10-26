package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;

import org.junit.Test;

import java.io.File;

public class ExportPgIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testExportPgToCsv() {
        final String[] command = {"export-pg", "-e", neptuneEndpoint, "-d", outputDir.getPath()};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgToCsv"), resultDir);
    }

    @Test
    public void testExportPgToCsvWithJanus() {
        final String[] command = {"export-pg", "-e", neptuneEndpoint, "-d", outputDir.getPath(), "--janus"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgToCsv"), resultDir);
    }

    @Test
    public void testExportPgToCsvWithGremlinFilter() {
        final String[] command = {"export-pg", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "--gremlin-filter", "has(\"runways\", 2)"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgToCsvWithGremlinFilter"), resultDir);
    }

    @Test
    public void testExportEdgesToCsvWithGremlinFilter() {
        final String[] command = {"export-pg", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "--gremlin-filter", "hasLabel(\"route\")"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportEdgesToCsvWithGremlinFilter"), resultDir);
    }

    @Test
    public void testExportEdgesToCsvWithGremlinFilterWithEarlyGremlinFilter() {
        final String[] command = {"export-pg", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "--gremlin-filter", "hasLabel(\"route\")", "--filter-edges-early"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportEdgesToCsvWithGremlinFilter"), resultDir);
    }
}
