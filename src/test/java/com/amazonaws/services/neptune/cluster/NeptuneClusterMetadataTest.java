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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import software.amazon.awssdk.services.neptune.NeptuneClient;
import software.amazon.awssdk.services.neptune.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class NeptuneClusterMetadataTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "1.0.5.0", "neptune1" },
                { "1.1.0.0", "neptune1" },
                { "1.1.1.0", "neptune1" },
                { "1.2.0.0", "neptune1.2" },
                { "1.2.1.1", "neptune1.2" },
                { "1.3.0.0", "neptune1.3" },
                { "1.4.5.0", "neptune1.4" },
                { "1.5.6.7", "neptune1.5" },
                { "2.0.0.0", "neptune2.0" },
                { "12.24.48.64", "neptune12.24" }
        });
    }

    private final String engineVersion;
    private final String expectedParameterGroupFamily;

    public NeptuneClusterMetadataTest(String engineVersion, String expectedParameterGroupFamily) {
        this.engineVersion = engineVersion;
        this.expectedParameterGroupFamily = expectedParameterGroupFamily;
    }

    @Test
    public void shouldUseVersionBasedFallbackForParamGroupFamily() {
        // Setup mock NeptuneClient
        NeptuneClient mockNeptuneClient = Mockito.mock(NeptuneClient.class);
        
        // Mock the describeDBClusters response
        DBCluster mockDbCluster = DBCluster.builder()
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
                .build();
        
        DescribeDbClustersResponse mockDescribeDbClustersResponse = DescribeDbClustersResponse.builder()
                .dbClusters(Collections.singletonList(mockDbCluster))
                .build();
        
        when(mockNeptuneClient.describeDBClusters(any(DescribeDbClustersRequest.class)))
                .thenReturn(mockDescribeDbClustersResponse);
        
        // Mock the listTagsForResource response
        ListTagsForResourceResponse mockListTagsResponse = ListTagsForResourceResponse.builder()
                .tagList(Collections.emptyList())
                .build();

        when(mockNeptuneClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(mockListTagsResponse);
        
        // Mock the describeDBClusterParameterGroups to throw NeptuneException
        when(mockNeptuneClient.describeDBClusterParameterGroups(any(DescribeDbClusterParameterGroupsRequest.class)))
                .thenThrow(NeptuneException.builder().message("Access Denied").build());
        
        // Mock the describeDBClusterParameters response
        Parameter neptuneStreamsParameter = Parameter.builder()
                .parameterName("neptune_streams")
                .parameterValue("0")
                .build();

        DescribeDbClusterParametersResponse mockParametersResponse = DescribeDbClusterParametersResponse.builder()
                .parameters(Collections.singletonList(neptuneStreamsParameter))
                .build();

        when(mockNeptuneClient.describeDBClusterParameters(any(DescribeDbClusterParametersRequest.class)))
                .thenReturn(mockParametersResponse);
        
        // Mock the describeDBInstances response
        Endpoint mockEndpoint = Endpoint.builder()
                .address("instance-1.test.us-east-1.neptune.amazonaws.com")
                .port(8182)
                .build();

        DBInstance mockDbInstance = DBInstance.builder()
                .dbInstanceIdentifier("instance-1")
                .dbInstanceClass("db.r5.large")
                .dbParameterGroups(Collections.singletonList(
                        DBParameterGroupStatus.builder()
                                .dbParameterGroupName("default.neptune1")
                                .build()
                ))
                .endpoint(mockEndpoint)
                .build();

        DescribeDbInstancesResponse mockDescribeDbInstancesResponse = DescribeDbInstancesResponse.builder()
                .dbInstances(Collections.singletonList(mockDbInstance))
                .build();

        when(mockNeptuneClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .thenReturn(mockDescribeDbInstancesResponse);
        
        // Create supplier for mock NeptuneClient
        Supplier<NeptuneClient> mockNeptuneClientSupplier = () -> mockNeptuneClient;
        
        // Call the method under test
        NeptuneClusterMetadata clusterMetadata = NeptuneClusterMetadata.createFromClusterId("test", mockNeptuneClientSupplier);
        
        // Verify the parameter group family is correctly determined
        assertEquals(expectedParameterGroupFamily, clusterMetadata.dbParameterGroupFamily());
    }
}
