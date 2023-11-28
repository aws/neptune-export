package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.io.File;

public class ExportServiceIntegrationTest extends AbstractExportIntegrationTest{

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void testExportPgToCsv() {
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new EquivalentResultsAssertion("src/test/resources/IntegrationTest/testExportPgToCsv"));

        final String[] command = {
                "nesvc",
                "--root-path", outputDir.getPath(),
                "--json", "{"+
                    "\"command\": \"export-pg\",\n" +
                "    \"params\": {\n" +
                "    \"endpoint\": \""+neptuneEndpoint+"\"\n" +
                "    }\n" +
                "}"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();
    }

    @Test
    public void testExportPgML() {
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new EquivalentResultsAssertion("src/test/resources/IntegrationTest/testExportPgWithEdgeAndVertexLabelsWithoutTypes"));
        exit.checkAssertionAfterwards(new EquivalentTrainingConfigAssertion("src/test/resources/IntegrationTest/ml-training-data-configs/v2.json"));

        final String[] command = {
                "nesvc",
                "--root-path", outputDir.getPath(),
                "--json", "{\n" +
                "        \"command\": \"export-pg\",\n" +
                "        \"params\": {\n" +
                "          \"endpoint\": \""+neptuneEndpoint+"\",\n" +
                "          \"profile\": \"neptune_ml\"\n" +
                "        },\n" +
                "        \"additionalParams\": {\n" +
                "          \"neptune_ml\": {\n" +
                "            \"version\": \"v2.0\",\n" +
                "            \"targets\": [\n" +
                "              {\n" +
                "                \"node\": \"Airport\",\n" +
                "                \"property\": \"city\",\n" +
                "                \"type\": \"classification\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      }"+
                "}"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();
    }

    @Test
    public void testExportPgFromQueriesML() {
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new EquivalentResultsAssertion("src/test/resources/IntegrationTest/testExportPgWithEdgeAndVertexLabelsWithoutTypes"));
        exit.checkAssertionAfterwards(new EquivalentTrainingConfigAssertion("src/test/resources/IntegrationTest/ml-training-data-configs/v2.json"));

        final String[] command = {
                "nesvc",
                "--root-path", outputDir.getPath(),
                "--json", "{\n" +
                "        \"command\": \"export-pg-from-queries\",\n" +
                "        \"params\": {\n" +
                "          \"endpoint\": \""+neptuneEndpoint+"\",\n" +
                "          \"profile\": \"neptune_ml\",\n" +
                "          \"query\" : \"query=g.V().union(elementMap(), outE().elementMap())\",\n" +
                "          \"structuredOutput\" : true \n" +
                "        },\n" +
                "        \"additionalParams\": {\n" +
                "          \"neptune_ml\": {\n" +
                "            \"version\": \"v2.0\",\n" +
                "            \"targets\": [\n" +
                "              {\n" +
                "                \"node\": \"Airport\",\n" +
                "                \"property\": \"city\",\n" +
                "                \"type\": \"classification\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      }"+
                "}"
        };
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();
    }

    private class EquivalentResultsAssertion implements Assertion {
        private String expectedResultsPath;

        public EquivalentResultsAssertion(String expectedResultsPath) {
            this.expectedResultsPath = expectedResultsPath;
        }

        @Override
        public void checkAssertion() throws Exception {
            final File resultDir = outputDir.listFiles()[0].listFiles()[0];
            assertEquivalentResults(new File(expectedResultsPath), resultDir);
        }
    }

    private class EquivalentTrainingConfigAssertion implements Assertion {
        private String expectedTrainingConfigJsonPath;

        public EquivalentTrainingConfigAssertion(String expectedTrainingConfigJsonPath) {
            this.expectedTrainingConfigJsonPath = expectedTrainingConfigJsonPath;
        }

        @Override
        public void checkAssertion() throws Exception {
            final File resultDir = outputDir.listFiles()[0].listFiles()[0];

            assertJSONContentMatches(
                    new File(expectedTrainingConfigJsonPath),
                    resultDir.listFiles((dir, name) -> name.equals("training-data-configuration.json"))[0],
                    "training-data-configuration.json does not match expected results"
            );
        }
    }

}
