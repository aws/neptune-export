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
    public void testExportPgWithEdgeAndVertexLabels() {
        final String[] command = {"export-pg", "-e", neptuneEndpoint, "-d", outputDir.getPath(),
                "--edge-label-strategy", "edgeAndVertexLabels"};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgWithEdgeAndVertexLabels"), resultDir);
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
