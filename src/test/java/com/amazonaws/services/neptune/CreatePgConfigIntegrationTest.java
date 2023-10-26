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

    @Test
    public void testCreatePgConfigWithGremlinFilter() {
        final String[] command = {"create-pg-config", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "--gremlin-filter", "has(\"runways\", 2)"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testCreatePgConfigWithGremlinFilter"), resultDir);
    }

    @Test
    public void testCreatePgConfigWithEdgeGremlinFilter() {
        final String[] command = {"create-pg-config", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "--gremlin-filter", "hasLabel(\"route\")"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testCreatePgConfigWithEdgeGremlinFilter"), resultDir);
    }

    @Test
    public void testCreatePgConfigWithEdgeGremlinFilterAndEarlyGremlinFilter() {
        final String[] command = {"create-pg-config", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "--gremlin-filter", "hasLabel(\"route\")", "--filter-edges-early"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testCreatePgConfigWithEdgeGremlinFilter"), resultDir);
    }

}
