/*
Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/
package com.amazonaws.services.neptune.cluster;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.services.neptune.NeptuneClient;
import software.amazon.awssdk.services.neptune.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class NeptuneClusterMetadataTest {

    @RunWith(Parameterized.class)
    public static class SuccessfulCases {
        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"1.0.5.0", "neptune1"},
                    {"1.1.0.0", "neptune1"},
                    {"1.1.1.0", "neptune1"},
                    {"1.2.0.0", "neptune1.2"},
                    {"1.2.1.1", "neptune1.2"},
                    {"1.3.0.0", "neptune1.3"},
                    {"1.4.5.0", "neptune1.4"},
                    {"1.5.6.7", "neptune1.5"},
                    {"2.0.0.0", "neptune2.0"},
                    {"12.24.48.64", "neptune12.24"}
            });
        }

        @Parameterized.Parameter(0)
        public static String engineVersion;
        @Parameterized.Parameter(1)
        public static String expectedParameterGroupFamily;

        @Test
        public void shouldExtractParamGroupFamilyFromSDKWhenPossible() {
            NeptuneClient mockNeptuneClient = createMockNeptuneClient("engineVersion");

            DBClusterParameterGroup parameterGroup = DBClusterParameterGroup.builder()
                .dbParameterGroupFamily(expectedParameterGroupFamily)
                .dbClusterParameterGroupName("default."+expectedParameterGroupFamily)
                .build();

            when(mockNeptuneClient.describeDBClusterParameterGroups(any(DescribeDbClusterParameterGroupsRequest.class)))
                    .thenReturn(DescribeDbClusterParameterGroupsResponse.builder()
                            .dbClusterParameterGroups(Collections.singletonList(parameterGroup))
                            .build());
            // Verify the parameter group family is correctly determined
            assertEquals(expectedParameterGroupFamily, NeptuneClusterMetadata.createFromClusterId("test", () -> mockNeptuneClient).dbParameterGroupFamily());
        }

        @Test
        public void shouldUseVersionBasedFallbackForParamGroupFamily() {
            NeptuneClient mockNeptuneClient = createMockNeptuneClient(engineVersion);

            // Mock the describeDBClusterParameterGroups to throw NeptuneException
            when(mockNeptuneClient.describeDBClusterParameterGroups(any(DescribeDbClusterParameterGroupsRequest.class)))
                    .thenThrow(NeptuneException.builder().message("Access Denied").build());
            // Verify the parameter group family is correctly determined from the engine version
            assertEquals(expectedParameterGroupFamily, NeptuneClusterMetadata.createFromClusterId("test", () -> mockNeptuneClient).dbParameterGroupFamily());
        }
    }

    @RunWith(Parameterized.class)
    public static class FailingCases {
        @Parameterized.Parameters(name = "{0}")
        public static Collection<String> data() {
            return Arrays.asList("Non-Parseable Engine Version", null, "1234", "");
        }

        @Parameterized.Parameter
        public static String engineVersion;

        @Test
        public void shouldRethrowExceptionIfFallbackForParamGroupFamilyFails() {
            NeptuneClient mockNeptuneClient = createMockNeptuneClient(engineVersion);

            Exception expectedException = NeptuneException.builder().message("Access Denied").build();

            // Mock the describeDBClusterParameterGroups to throw NeptuneException
            when(mockNeptuneClient.describeDBClusterParameterGroups(any(DescribeDbClusterParameterGroupsRequest.class)))
                    .thenThrow(expectedException);

            try {
                NeptuneClusterMetadata.createFromClusterId("test", () -> mockNeptuneClient).dbParameterGroupFamily();
                fail("Expected NeptuneException to be thrown");
            } catch (Exception caughtException) {
                assertEquals(expectedException, caughtException);
            }
        }
    }

    protected static NeptuneClient createMockNeptuneClient(String engineVersion) {
        NeptuneClient mockNeptuneClient = mock(NeptuneClient.class);
        when(mockNeptuneClient.describeDBClusters(any(DescribeDbClustersRequest.class)))
                .thenReturn(DescribeDbClustersResponse.builder()
                        .dbClusters(Collections.singletonList(DBCluster.builder()
                                .dbClusterIdentifier("test")
                                .dbClusterArn("arn:aws:rds:us-east-1:123456789012:cluster:test")
                                .dbClusterParameterGroup("default.neptune1")
                                .engineVersion(engineVersion)
                                .port(8182)
                                .iamDatabaseAuthenticationEnabled(false)
                                .dbClusterMembers(
                                        DBClusterMember.builder()
                                                .dbInstanceIdentifier("instance-1")
                                                .isClusterWriter(true)
                                                .build()
                                )
                                .build()))
                        .build());

        when(mockNeptuneClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tagList(Collections.emptyList())
                        .build());

        when(mockNeptuneClient.describeDBClusterParameters(any(DescribeDbClusterParametersRequest.class)))
                .thenReturn(DescribeDbClusterParametersResponse.builder()
                        .parameters(Collections.singletonList(Parameter.builder()
                                .parameterName("neptune_streams")
                                .parameterValue("0")
                                .build()))
                        .build());

        when(mockNeptuneClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .thenReturn(DescribeDbInstancesResponse.builder()
                        .dbInstances(Collections.singletonList(DBInstance.builder()
                                .dbInstanceIdentifier("instance-1")
                                .dbInstanceClass("db.r5.large")
                                .dbParameterGroups(Collections.singletonList(
                                        DBParameterGroupStatus.builder()
                                                .dbParameterGroupName("default.neptune1")
                                                .build()
                                ))
                                .endpoint(Endpoint.builder()
                                        .address("instance-1.test.us-east-1.neptune.amazonaws.com")
                                        .port(8182)
                                        .build())
                                .build()))
                        .build());
        return mockNeptuneClient;
    }
}
