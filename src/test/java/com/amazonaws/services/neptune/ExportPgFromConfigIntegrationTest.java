package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import org.junit.Test;

import java.io.File;

public class ExportPgFromConfigIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testExportPgFromConfig() {
        final String[] command = {"export-pg-from-config", "-e", neptuneEndpoint,
                "-c", "src/test/resources/IntegrationTest/ExportPgFromConfigIntegrationTest/input/config.json",
                "-d", outputDir.getPath()};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/ExportPgFromConfigIntegrationTest/testExportPgFromConfig"), resultDir);
    }

    @Test
    public void testExportPgFromConfigWithGremlinFilter() {
        final String[] command = {"export-pg-from-config", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "-c", "src/test/resources/IntegrationTest/ExportPgFromConfigIntegrationTest/input/config.json",
                "--gremlin-filter", "has(\"runways\", 2)"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgToCsvWithGremlinFilter"), resultDir);
    }

    @Test
    public void testExportEdgesFromConfigWithGremlinFilter() {
        final String[] command = {"export-pg-from-config", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "-c", "src/test/resources/IntegrationTest/ExportPgFromConfigIntegrationTest/input/config.json",
                "--gremlin-filter", "hasLabel(\"route\")"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportEdgesToCsvWithGremlinFilter"), resultDir);
    }

    @Test
    public void testExportEdgesFromConfigWithGremlinFilterWithEarlyGremlinFilter() {
        final String[] command = {"export-pg-from-config", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "-c", "src/test/resources/IntegrationTest/ExportPgFromConfigIntegrationTest/input/config.json",
                "--gremlin-filter", "hasLabel(\"route\")", "--filter-edges-early"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportEdgesToCsvWithGremlinFilter"), resultDir);
    }

}
