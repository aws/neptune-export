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
