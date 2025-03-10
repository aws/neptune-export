/*
Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.neptune.NeptuneClient;
import software.amazon.awssdk.services.neptune.model.ApplyMethod;
import software.amazon.awssdk.services.neptune.model.CreateDbClusterParameterGroupRequest;
import software.amazon.awssdk.services.neptune.model.CreateDbClusterParameterGroupResponse;
import software.amazon.awssdk.services.neptune.model.CreateDbInstanceRequest;
import software.amazon.awssdk.services.neptune.model.CreateDbInstanceResponse;
import software.amazon.awssdk.services.neptune.model.CreateDbParameterGroupRequest;
import software.amazon.awssdk.services.neptune.model.CreateDbParameterGroupResponse;
import software.amazon.awssdk.services.neptune.model.DBCluster;
import software.amazon.awssdk.services.neptune.model.DBClusterParameterGroup;
import software.amazon.awssdk.services.neptune.model.DBInstance;
import software.amazon.awssdk.services.neptune.model.DBParameterGroup;
import software.amazon.awssdk.services.neptune.model.DescribeDbClusterParametersRequest;
import software.amazon.awssdk.services.neptune.model.DescribeDbClusterParametersResponse;
import software.amazon.awssdk.services.neptune.model.DescribeDbParametersRequest;
import software.amazon.awssdk.services.neptune.model.DescribeDbParametersResponse;
import software.amazon.awssdk.services.neptune.model.ModifyDbClusterParameterGroupRequest;
import software.amazon.awssdk.services.neptune.model.Parameter;
import software.amazon.awssdk.services.neptune.model.RestoreDbClusterToPointInTimeRequest;
import software.amazon.awssdk.services.neptune.model.RestoreDbClusterToPointInTimeResponse;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddCloneTaskTest {

    @Test
    public void shouldNotSetAuditLogsWhenEnableAuditLogsIsFalse() {
        NeptuneClient mockNeptune = createMockNeptune();
        ArgumentCaptor<ModifyDbClusterParameterGroupRequest> clusterParamsCaptor = ArgumentCaptor.forClass(ModifyDbClusterParameterGroupRequest.class);
        ArgumentCaptor<RestoreDbClusterToPointInTimeRequest> cloneClusterRequestCaptor = ArgumentCaptor.forClass(RestoreDbClusterToPointInTimeRequest.class);

        AddCloneTask noLogsTask = new AddCloneTask("sourceClusterId", "targetClusterId", "db_r5_large", 1, null,
                () -> mockNeptune, null, false);

        // Mock static method to skip creating NeptuneClusterMetadata for test
        try (MockedStatic<NeptuneClusterMetadata> classMock = mockStatic(NeptuneClusterMetadata.class)) {
            classMock.when(() -> NeptuneClusterMetadata.createFromClusterId(any(), any())).thenReturn(mock(NeptuneClusterMetadata.class));
            noLogsTask.execute();
        }

        verify(mockNeptune).modifyDBClusterParameterGroup(clusterParamsCaptor.capture());
        verify(mockNeptune).restoreDBClusterToPointInTime(cloneClusterRequestCaptor.capture());

        ModifyDbClusterParameterGroupRequest capturedParamsRequest = clusterParamsCaptor.getValue();

        // Assert that "neptune_enable_audit_log" parameter has not been set
        assertEquals(0, capturedParamsRequest.parameters().stream().filter((p) -> (p.parameterName().equals("neptune_enable_audit_log"))).count());

        RestoreDbClusterToPointInTimeRequest capturedCloneRequest = cloneClusterRequestCaptor.getValue();

        // Assert that cluster log exports are disabled
        assertEquals(0, capturedCloneRequest.enableCloudwatchLogsExports().size());
    }

    @Test
    public void shouldSetAuditLogsWhenEnableAuditLogsIsTrue() {
        NeptuneClient mockNeptune = createMockNeptune();
        ArgumentCaptor<ModifyDbClusterParameterGroupRequest> clusterParamsCaptor = ArgumentCaptor.forClass(ModifyDbClusterParameterGroupRequest.class);
        ArgumentCaptor<RestoreDbClusterToPointInTimeRequest> cloneClusterRequestCaptor = ArgumentCaptor.forClass(RestoreDbClusterToPointInTimeRequest.class);

        AddCloneTask noLogsTask = new AddCloneTask("sourceClusterId", "targetClusterId", "db_r5_large", 1, null,
                () -> mockNeptune, null, true);

        // Mock static method to skip creating NeptuneClusterMetadata for test
        try (MockedStatic<NeptuneClusterMetadata> classMock = mockStatic(NeptuneClusterMetadata.class)) {
            classMock.when(() -> NeptuneClusterMetadata.createFromClusterId(any(), any())).thenReturn(mock(NeptuneClusterMetadata.class));
            noLogsTask.execute();
        }

        verify(mockNeptune).modifyDBClusterParameterGroup(clusterParamsCaptor.capture());
        verify(mockNeptune).restoreDBClusterToPointInTime(cloneClusterRequestCaptor.capture());

        ModifyDbClusterParameterGroupRequest capturedParamsRequest = clusterParamsCaptor.getValue();

        // Assert that "neptune_enable_audit_log" parameter exists and has been set to "1"
        assertEquals(1,
                capturedParamsRequest.parameters().stream()
                        .filter((p) -> (p.parameterName().equals("neptune_enable_audit_log")))
                        .peek((parameter -> assertEquals("1", parameter.parameterValue())))
                        .count());

        RestoreDbClusterToPointInTimeRequest capturedCloneRequest = cloneClusterRequestCaptor.getValue();

        // Assert that cluster audit log exports are enabled
        assertEquals(Arrays.asList("audit"), capturedCloneRequest.enableCloudwatchLogsExports());
    }

    private NeptuneClient createMockNeptune() {
        NeptuneClient mockNeptune = mock(NeptuneClient.class);
        DescribeDbClusterParametersResponse mockClusterParamsResponse = mock(DescribeDbClusterParametersResponse.class);
        DescribeDbParametersResponse mockParamsResponse = mock(DescribeDbParametersResponse.class);
        RestoreDbClusterToPointInTimeResponse mockRestoreResponse = mock(RestoreDbClusterToPointInTimeResponse.class);
        CreateDbInstanceResponse mockCreateInstanceResponse = mock(CreateDbInstanceResponse.class);
        CreateDbClusterParameterGroupResponse mockCreateClusterParameterGroupResponse = mock(CreateDbClusterParameterGroupResponse.class);
        CreateDbParameterGroupResponse mockCreateDbParameterGroupResponse = mock(CreateDbParameterGroupResponse.class);

        DBCluster targetDbCluster = DBCluster.builder().status("available").build();

        DBInstance targetDbInstance = DBInstance.builder().dbInstanceStatus("available").build();

        when(mockNeptune.createDBClusterParameterGroup((CreateDbClusterParameterGroupRequest) any())).thenReturn(mockCreateClusterParameterGroupResponse);
        when(mockNeptune.describeDBClusterParameters((DescribeDbClusterParametersRequest) any())).thenReturn(mockClusterParamsResponse);
        when(mockNeptune.createDBParameterGroup((CreateDbParameterGroupRequest) any())).thenReturn(mockCreateDbParameterGroupResponse);
        when(mockNeptune.describeDBParameters((DescribeDbParametersRequest) any())).thenReturn(mockParamsResponse);
        when(mockNeptune.restoreDBClusterToPointInTime((RestoreDbClusterToPointInTimeRequest) any())).thenReturn(mockRestoreResponse);
        when(mockNeptune.createDBInstance((CreateDbInstanceRequest) any())).thenReturn(mockCreateInstanceResponse);

        when(mockClusterParamsResponse.parameters()).thenReturn(Arrays.asList(Parameter.builder()
                .parameterName("neptune_query_timeout")
                .parameterValue("2147483647")
                .applyMethod(ApplyMethod.PENDING_REBOOT)
                .build()));
        when(mockParamsResponse.parameters()).thenReturn(Arrays.asList(Parameter.builder()
                .parameterName("neptune_query_timeout")
                .parameterValue("2147483647")
                .applyMethod(ApplyMethod.PENDING_REBOOT)
                .build()));
        when(mockRestoreResponse.dbCluster()).thenReturn(targetDbCluster);
        when(mockCreateInstanceResponse.dbInstance()).thenReturn(targetDbInstance);
        when(mockCreateClusterParameterGroupResponse.dbClusterParameterGroup()).thenReturn(mock(DBClusterParameterGroup.class));
        when(mockCreateDbParameterGroupResponse.dbParameterGroup()).thenReturn(mock(DBParameterGroup.class));


        return mockNeptune;
    }

}
