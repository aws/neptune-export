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

import static org.junit.Assert.assertTrue;

public class ExportPgFromQueriesIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testExportPgFromQueries() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().hasLabel('airport').has('runways', gt(2)).project('code', 'runways', 'city', 'country').by('code').by('runways').by('city').by('country')"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgFromQueries"), resultDir);
    }

    @Test
    public void testExportPgFromQueriesSplitQueries() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().hasLabel('airport').has('runways', gt(2)).project('code', 'runways', 'city', 'country').by('code').by('runways').by('city').by('country')",
                "--split-queries"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgFromQueries"), resultDir);
    }

    @Test
    public void testExportPgFromQueriesSplitQueriesAndRange() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().hasLabel('airport').has('runways', gt(2)).project('code', 'runways', 'city', 'country').by('code').by('runways').by('city').by('country')",
                "--split-queries", "--range", "25"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentResults(new File("src/test/resources/IntegrationTest/testExportPgFromQueries"), resultDir);
    }

    @Test
    public void testExportPgFromQueriesWithStructuredOutput() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().union(hasLabel('airport'), outE()).elementMap()",
                "--include-type-definitions",
                "--structured-output"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentStructuredOutput(new File("src/test/resources/IntegrationTest/testExportPgFromQueriesStructuredOutput"), resultDir);
    }

    @Test
    public void testExportPgFromQueriesWithStructuredOutputSplitQueries() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().union(hasLabel('airport'), outE()).elementMap()",
                "--include-type-definitions",
                "--split-queries", "--range", "25",
                "--structured-output"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentStructuredOutput(new File("src/test/resources/IntegrationTest/testExportPgFromQueriesStructuredOutput"), resultDir);
    }

    @Test
    public void testExportPgFromQueriesWithStructuredOutputWithEdgeAndVertexLabels() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "all=g.V().union(elementMap(), outE().elementMap())",
                "--edge-label-strategy", "edgeAndVertexLabels", "--structured-output"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentStructuredOutput(new File("src/test/resources/IntegrationTest/testExportPgWithEdgeAndVertexLabelsWithoutTypes"), resultDir);
    }

    @Test
    public void testExportPgFromQueriesWithStructuredOutputWithEdgeAndVertexLabelsIncludeTypes() {
        final String[] command = {"export-pg-from-queries", "-e", neptuneEndpoint,
                "-d", outputDir.getPath(),
                "-q", "airport=g.V().union(hasLabel('airport'), outE()).elementMap()",
                "--include-type-definitions", "--edge-label-strategy", "edgeAndVertexLabels",
                "--structured-output"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertEquivalentStructuredOutput(new File("src/test/resources/IntegrationTest/testExportPgFromQueriesStructuredOutputWithEdgeAndVertexLabels"), resultDir);
    }

    @Override
    protected void assertEquivalentResults(final File expected, final File actual) {
        assertJSONContentMatches(
                expected.listFiles((dir, name) -> name.equals("queries.json"))[0],
                actual.listFiles((dir, name) -> name.equals("queries.json"))[0],
                "queries.json does not match expected results");
        for (File expectedResultsDir : expected.listFiles((dir, name) -> name.equals("results"))[0].listFiles()) {
            assertTrue(expectedResultsDir.isDirectory());
            String dirName = expectedResultsDir.getName();
            assertTrue("results/"+dirName+" directory does not match expected results", areLabelledDirContentsEquivalent(expectedResultsDir, new File(actual+"/results/"+dirName), dirName));
        }
    }

    private void assertEquivalentStructuredOutput(final File expected, final File actual) {
        super.assertEquivalentResults(expected, actual);
    }



}
