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

import com.amazonaws.services.neptune.AmazonNeptune;
import com.amazonaws.services.neptune.model.ApplyMethod;
import com.amazonaws.services.neptune.model.DBCluster;
import com.amazonaws.services.neptune.model.DBClusterParameterGroup;
import com.amazonaws.services.neptune.model.DBInstance;
import com.amazonaws.services.neptune.model.DBParameterGroup;
import com.amazonaws.services.neptune.model.DescribeDBClusterParametersResult;
import com.amazonaws.services.neptune.model.DescribeDBParametersResult;
import com.amazonaws.services.neptune.model.ModifyDBClusterParameterGroupRequest;
import com.amazonaws.services.neptune.model.Parameter;
import com.amazonaws.services.neptune.model.RestoreDBClusterToPointInTimeRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddCloneTaskTest {

    @Test
    public void shouldNotSetAuditLogsWhenEnableAuditLogsIsFalse() {
        AmazonNeptune mockNeptune = createMockNeptune();
        ArgumentCaptor<ModifyDBClusterParameterGroupRequest> clusterParamsCaptor = ArgumentCaptor.forClass(ModifyDBClusterParameterGroupRequest.class);
        ArgumentCaptor<RestoreDBClusterToPointInTimeRequest> cloneClusterRequestCaptor = ArgumentCaptor.forClass(RestoreDBClusterToPointInTimeRequest.class);

        AddCloneTask noLogsTask = new AddCloneTask("sourceClusterId", "targetClusterId", "db_r5_large", 1, null,
                () -> mockNeptune, null, false);

        // Mock static method to skip creating NeptuneClusterMetadata for test
        try (MockedStatic<NeptuneClusterMetadata> classMock = mockStatic(NeptuneClusterMetadata.class)) {
            classMock.when(() -> NeptuneClusterMetadata.createFromClusterId(any(), any())).thenReturn(mock(NeptuneClusterMetadata.class));
            noLogsTask.execute();
        }

        verify(mockNeptune).modifyDBClusterParameterGroup(clusterParamsCaptor.capture());
        verify(mockNeptune).restoreDBClusterToPointInTime(cloneClusterRequestCaptor.capture());

        ModifyDBClusterParameterGroupRequest capturedParamsRequest = clusterParamsCaptor.getValue();

        // Assert that "neptune_enable_audit_log" parameter has not been set
        assertEquals(0, capturedParamsRequest.getParameters().stream().filter((p) -> (p.getParameterName().equals("neptune_enable_audit_log"))).count());

        RestoreDBClusterToPointInTimeRequest capturedCloneRequest = cloneClusterRequestCaptor.getValue();

        // Assert that cluster log exports are disabled
        assertEquals(null, capturedCloneRequest.getEnableCloudwatchLogsExports());
    }

    @Test
    public void shouldSetAuditLogsWhenEnableAuditLogsIsTrue() {
        AmazonNeptune mockNeptune = createMockNeptune();
        ArgumentCaptor<ModifyDBClusterParameterGroupRequest> clusterParamsCaptor = ArgumentCaptor.forClass(ModifyDBClusterParameterGroupRequest.class);
        ArgumentCaptor<RestoreDBClusterToPointInTimeRequest> cloneClusterRequestCaptor = ArgumentCaptor.forClass(RestoreDBClusterToPointInTimeRequest.class);

        AddCloneTask noLogsTask = new AddCloneTask("sourceClusterId", "targetClusterId", "db_r5_large", 1, null,
                () -> mockNeptune, null, true);

        // Mock static method to skip creating NeptuneClusterMetadata for test
        try (MockedStatic<NeptuneClusterMetadata> classMock = mockStatic(NeptuneClusterMetadata.class)) {
            classMock.when(() -> NeptuneClusterMetadata.createFromClusterId(any(), any())).thenReturn(mock(NeptuneClusterMetadata.class));
            noLogsTask.execute();
        }

        verify(mockNeptune).modifyDBClusterParameterGroup(clusterParamsCaptor.capture());
        verify(mockNeptune).restoreDBClusterToPointInTime(cloneClusterRequestCaptor.capture());

        ModifyDBClusterParameterGroupRequest capturedParamsRequest = clusterParamsCaptor.getValue();

        // Assert that "neptune_enable_audit_log" parameter exists and has been set to "1"
        assertEquals(1,
                capturedParamsRequest.getParameters().stream()
                        .filter((p) -> (p.getParameterName().equals("neptune_enable_audit_log")))
                        .peek((parameter -> assertEquals("1", parameter.getParameterValue())))
                        .count());

        RestoreDBClusterToPointInTimeRequest capturedCloneRequest = cloneClusterRequestCaptor.getValue();

        // Assert that cluster audit log exports are enabled
        assertEquals(Arrays.asList("audit"), capturedCloneRequest.getEnableCloudwatchLogsExports());
    }

    private AmazonNeptune createMockNeptune() {
        AmazonNeptune mockNeptune = mock(AmazonNeptune.class);
        DescribeDBClusterParametersResult mockClusterParamsResult = mock(DescribeDBClusterParametersResult.class);
        DescribeDBParametersResult mockParamsResult = mock(DescribeDBParametersResult.class);

        DBCluster targetDbCluster = new DBCluster();
        targetDbCluster.setStatus("available");

        DBInstance targetDbInstance = new DBInstance();
        targetDbInstance.setDBInstanceStatus("available");

        when(mockNeptune.createDBClusterParameterGroup(any())).thenReturn(mock(DBClusterParameterGroup.class));
        when(mockNeptune.describeDBClusterParameters(any())).thenReturn(mockClusterParamsResult);
        when(mockNeptune.createDBParameterGroup(any())).thenReturn(mock(DBParameterGroup.class));
        when(mockNeptune.describeDBParameters(any())).thenReturn(mockParamsResult);
        when(mockNeptune.restoreDBClusterToPointInTime(any())).thenReturn(targetDbCluster);
        when(mockNeptune.createDBInstance(any())).thenReturn(targetDbInstance);

        when(mockClusterParamsResult.getParameters()).thenReturn(Arrays.asList(new Parameter()
                .withParameterName("neptune_query_timeout")
                .withParameterValue("2147483647")
                .withApplyMethod(ApplyMethod.PendingReboot)));
        when(mockParamsResult.getParameters()).thenReturn(Arrays.asList(new Parameter()
                .withParameterName("neptune_query_timeout")
                .withParameterValue("2147483647")
                .withApplyMethod(ApplyMethod.PendingReboot)));

        return mockNeptune;
    }

}
