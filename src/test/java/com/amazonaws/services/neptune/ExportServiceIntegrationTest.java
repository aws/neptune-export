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
import com.amazonaws.services.neptune.util.S3ObjectInfo;
import com.amazonaws.services.neptune.util.TransferManagerWrapper;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;

import java.io.File;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;

public class ExportServiceIntegrationTest extends AbstractExportIntegrationTest{

    private static String s3Path;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @BeforeClass
    public static void setupS3() {
        s3Path = System.getenv("S3_PATH");
        assertNotNull("S3 path must be provided through \"S3_PATH\" environment variable", s3Path);
        if (s3Path.endsWith("/")) {
            s3Path = s3Path.substring(0, s3Path.length() - 1);
        }
    }

    @Test
    public void testExportPgToCsv() {
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new EquivalentResultsAssertion("src/test/resources/IntegrationTest/testExportPgToCsv"));
        exit.checkAssertionAfterwards(new S3EquivalentResultsAssertion(s3Path, "src/test/resources/IntegrationTest/testExportPgToCsv", "testExportPgToCsv"));

        final String[] command = {
                "nesvc",
                "--root-path", outputDir.getPath(),
                "--json", "{"+
                    "\"command\": \"export-pg\",\n" +
                "    \"params\": {\n" +
                "    \"endpoint\": \""+neptuneEndpoint+"\",\n" +
                "    \"useIamAuth\": true\n" +
                "    },\n" +
                "    \"outputS3Path\": \""+s3Path+"/testExportPgToCsv\"\n" +
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
        exit.checkAssertionAfterwards(new S3EquivalentResultsAssertion(s3Path, "src/test/resources/IntegrationTest/testExportPgWithEdgeAndVertexLabelsWithoutTypes", "testExportPgML"));

        final String[] command = {
                "nesvc",
                "--root-path", outputDir.getPath(),
                "--json", "{\n" +
                "        \"command\": \"export-pg\",\n" +
                "        \"params\": {\n" +
                "          \"endpoint\": \""+neptuneEndpoint+"\",\n" +
                "          \"profile\": \"neptune_ml\",\n" +
                "          \"useIamAuth\": true\n" +
                "        },\n" +
                "        \"outputS3Path\": \""+s3Path+"/testExportPgML\",\n" +
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
        exit.checkAssertionAfterwards(new S3EquivalentResultsAssertion(s3Path, "src/test/resources/IntegrationTest/testExportPgWithEdgeAndVertexLabelsWithoutTypes", "testExportPgFromQueriesML"));

        final String[] command = {
                "nesvc",
                "--root-path", outputDir.getPath(),
                "--json", "{\n" +
                "        \"command\": \"export-pg-from-queries\",\n" +
                "        \"params\": {\n" +
                "          \"endpoint\": \""+neptuneEndpoint+"\",\n" +
                "          \"profile\": \"neptune_ml\",\n" +
                "          \"query\" : \"query=g.V().union(elementMap(), outE().elementMap())\",\n" +
                "          \"structuredOutput\" : true,\n" +
                "          \"useIamAuth\": true\n" +
                "        },\n" +
                "        \"outputS3Path\": \""+s3Path+"/testExportPgFromQueriesML\",\n" +
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

    private class S3EquivalentResultsAssertion implements Assertion {
        private final String s3BasePath;
        private final String expectedResultsPath;
        private final String testName;

        public S3EquivalentResultsAssertion(String s3BasePath, String expectedResultsPath, String testName) {
            this.s3BasePath = s3BasePath;
            this.expectedResultsPath = expectedResultsPath;
            this.testName = testName;
        }

        @Override
        public void checkAssertion() throws Exception {
            String fullS3Path = s3BasePath + "/" + testName;
            S3ObjectInfo s3ObjectInfo = new S3ObjectInfo(fullS3Path);
            
            File downloadDir = tempFolder.newFolder("s3-download-" + testName);
            
            try {
                downloadDirectoryFromS3(s3ObjectInfo, downloadDir);
                
                File resultDir = downloadDir.listFiles()[0];
                assertEquivalentResults(new File(expectedResultsPath), resultDir);
            } finally {
                deleteS3Directory(s3ObjectInfo);
            }
        }
    }

    private void downloadDirectoryFromS3(S3ObjectInfo s3ObjectInfo, File targetDir) {
        try (TransferManagerWrapper transferManager = new TransferManagerWrapper(null)) {
            S3Client s3Client = S3Client.create();
            
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(s3ObjectInfo.bucket())
                    .prefix(s3ObjectInfo.key())
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : listResponse.contents()) {
                String key = s3Object.key();
                String relativePath = key.substring(s3ObjectInfo.key().length());
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                
                File targetFile = new File(targetDir, relativePath);
                targetFile.getParentFile().mkdirs();
                
                DownloadFileRequest downloadRequest = DownloadFileRequest.builder()
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(s3ObjectInfo.bucket())
                                .key(key)
                                .build())
                        .destination(targetFile.toPath())
                        .build();
                
                FileDownload download = transferManager.get().downloadFile(downloadRequest);
                download.completionFuture().join();
            }
            
            s3Client.close();
        } catch (CancellationException | CompletionException e) {
            throw new RuntimeException("Failed to download directory from S3", e);
        }
    }

    private void deleteS3Directory(S3ObjectInfo s3ObjectInfo) {
        S3Client s3Client = S3Client.create();
        
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(s3ObjectInfo.bucket())
                    .prefix(s3ObjectInfo.key())
                    .build();
            
            ListObjectsV2Response listResponse;
            do {
                listResponse = s3Client.listObjectsV2(listRequest);
                
                if (listResponse.hasContents()) {
                    List<ObjectIdentifier> objectsToDelete = listResponse.contents().stream()
                            .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
                            .collect(Collectors.toList());
                    
                    DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                            .bucket(s3ObjectInfo.bucket())
                            .delete(Delete.builder().objects(objectsToDelete).build())
                            .build();
                    
                    s3Client.deleteObjects(deleteRequest);
                }
                
                listRequest = listRequest.toBuilder()
                        .continuationToken(listResponse.nextContinuationToken())
                        .build();
                
            } while (listResponse.isTruncated());
            
        } finally {
            s3Client.close();
        }
    }

}
