/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

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
