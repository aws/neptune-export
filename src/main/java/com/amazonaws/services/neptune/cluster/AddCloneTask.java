/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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


import com.amazonaws.services.neptune.util.Activity;
import com.amazonaws.services.neptune.util.Timer;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.services.neptune.NeptuneClient;
import software.amazon.awssdk.services.neptune.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class AddCloneTask {

    private final String sourceClusterId;
    private final String targetClusterId;
    private final String cloneClusterInstanceType;
    private final int replicaCount;
    private final String engineVersion;
    private final Supplier<NeptuneClient> amazonNeptuneClientSupplier;
    private final String cloneCorrelationId;
    private final boolean enableAuditLogs;

    public AddCloneTask(String sourceClusterId,
                        String targetClusterId,
                        String cloneClusterInstanceType,
                        int replicaCount,
                        String engineVersion,
                        Supplier<NeptuneClient> amazonNeptuneClientSupplier,
                        String cloneCorrelationId,
                        boolean enableAuditLogs) {
        this.sourceClusterId = sourceClusterId;
        this.targetClusterId = targetClusterId;
        this.cloneClusterInstanceType = cloneClusterInstanceType;
        this.replicaCount = replicaCount;
        this.engineVersion = engineVersion;
        this.amazonNeptuneClientSupplier = amazonNeptuneClientSupplier;
        this.cloneCorrelationId = cloneCorrelationId;
        this.enableAuditLogs = enableAuditLogs;
    }

    public NeptuneClusterMetadata execute() {
        return Timer.timedActivity(
                "cloning cluster",
                (Activity.Callable<NeptuneClusterMetadata>) this::cloneCluster);
    }

    private NeptuneClusterMetadata cloneCluster() {

        System.err.println("Cloning cluster " + sourceClusterId + "...");
        System.err.println();

        NeptuneClusterMetadata sourceClusterMetadata =
                NeptuneClusterMetadata.createFromClusterId(sourceClusterId, amazonNeptuneClientSupplier);

        InstanceType instanceType = StringUtils.isEmpty(cloneClusterInstanceType) ?
                InstanceType.parse(sourceClusterMetadata.instanceMetadataFor(sourceClusterMetadata.primary()).instanceType()) :
                InstanceType.parse(cloneClusterInstanceType);

        System.err.println(String.format("Source clusterId           : %s", sourceClusterId));
        System.err.println(String.format("Target clusterId           : %s", targetClusterId));
        System.err.println(String.format("Target instance type       : %s", instanceType));

        NeptuneClient neptune = amazonNeptuneClientSupplier.get();

        DBClusterParameterGroup dbClusterParameterGroup = Timer.timedActivity(
                "creating DB cluster parameter group",
                (Activity.Callable<DBClusterParameterGroup>) () ->
                        createDbClusterParameterGroup(sourceClusterMetadata, neptune));

        DBParameterGroup dbParameterGroup = Timer.timedActivity(
                "creating parameter groups",
                (Activity.Callable<DBParameterGroup>) () -> createDbParameterGroup(sourceClusterMetadata, neptune));


        DBCluster targetDbCluster = Timer.timedActivity(
                "creating target cluster",
                (Activity.Callable<DBCluster>) () ->
                        createCluster(sourceClusterMetadata, neptune, dbClusterParameterGroup));

        Timer.timedActivity("creating primary", (Activity.Runnable) () ->
                createInstance("primary",
                        neptune,
                        sourceClusterMetadata,
                        instanceType,
                        dbParameterGroup,
                        targetDbCluster));

        if (replicaCount > 0) {

            Timer.timedActivity("creating replicas", (Activity.Runnable) () ->
                    createReplicas(sourceClusterMetadata, instanceType, neptune, dbParameterGroup, targetDbCluster));
        }

        neptune.close();

        return NeptuneClusterMetadata.createFromClusterId(targetClusterId, amazonNeptuneClientSupplier);
    }

    private void createReplicas(NeptuneClusterMetadata sourceClusterMetadata,
                                InstanceType instanceType,
                                NeptuneClient neptune,
                                DBParameterGroup dbParameterGroup,
                                DBCluster targetDbCluster) {

        ExecutorService taskExecutor = Executors.newFixedThreadPool(replicaCount);

        for (int i = 0; i < replicaCount; i++) {

            taskExecutor.execute(() -> createInstance("replica",
                    neptune,
                    sourceClusterMetadata,
                    instanceType,
                    dbParameterGroup,
                    targetDbCluster));
        }

        taskExecutor.shutdown();

        try {
            taskExecutor.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private DBCluster createCluster(NeptuneClusterMetadata sourceClusterMetadata,
                                    NeptuneClient neptune,
                                    DBClusterParameterGroup
                                            dbClusterParameterGroup) {

        System.err.println("Creating target cluster...");

        RestoreDbClusterToPointInTimeRequest.Builder restoreDbClusterToPointInTimeRequestBuilder = RestoreDbClusterToPointInTimeRequest.builder()
                .sourceDBClusterIdentifier(sourceClusterId)
                .dbClusterIdentifier(targetClusterId)
                .restoreType("copy-on-write")
                .useLatestRestorableTime(true)
                .port(sourceClusterMetadata.port())
                .dbClusterParameterGroupName(dbClusterParameterGroup.dbClusterParameterGroupName())
                .enableIAMDatabaseAuthentication(sourceClusterMetadata.isIAMDatabaseAuthenticationEnabled())
                .dbSubnetGroupName(sourceClusterMetadata.dbSubnetGroupName())
                .vpcSecurityGroupIds(sourceClusterMetadata.vpcSecurityGroupIds())
                .tags(getTags(sourceClusterMetadata.clusterId()));

        if (this.enableAuditLogs) {
            restoreDbClusterToPointInTimeRequestBuilder = restoreDbClusterToPointInTimeRequestBuilder.enableCloudwatchLogsExports("audit");
        }

        DBCluster targetDbCluster = neptune.restoreDBClusterToPointInTime(restoreDbClusterToPointInTimeRequestBuilder.build()).dbCluster();

        String clusterStatus = targetDbCluster.status();

        while (clusterStatus.equals("creating")) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            clusterStatus = neptune.describeDBClusters(
                            DescribeDbClustersRequest.builder()
                                    .dbClusterIdentifier(targetDbCluster.dbClusterIdentifier())
                                    .build())
                    .dbClusters()
                    .get(0)
                    .status();
        }

        return targetDbCluster;
    }

    private Collection<Tag> getTags(String sourceClusterId) {
        Collection<Tag> tags = new ArrayList<>();
        tags.add(Tag.builder()
                .key("source")
                .value(sourceClusterId)
                .build());
        tags.add(Tag.builder()
                .key("application")
                .value(NeptuneClusterMetadata.NEPTUNE_EXPORT_APPLICATION_TAG)
                .build());

        if (StringUtils.isNotEmpty(cloneCorrelationId)) {
            tags.add(Tag.builder()
                    .key(NeptuneClusterMetadata.NEPTUNE_EXPORT_CORRELATION_ID_KEY)
                    .value(cloneCorrelationId)
                    .build());
        }

        return tags;
    }

    private DBParameterGroup createDbParameterGroup(NeptuneClusterMetadata sourceClusterMetadata,
                                                    NeptuneClient neptune) {

        DBParameterGroup dbParameterGroup;

        dbParameterGroup = neptune.createDBParameterGroup(
                CreateDbParameterGroupRequest.builder()
                        .dbParameterGroupName(String.format("%s-db-params", targetClusterId))
                        .description(String.format("%s DB Parameter Group", targetClusterId))
                        .dbParameterGroupFamily(sourceClusterMetadata.dbParameterGroupFamily())
                        .tags(getTags(sourceClusterMetadata.clusterId()))
                        .build()).dbParameterGroup();

        neptune.modifyDBParameterGroup(ModifyDbParameterGroupRequest.builder()
                .dbParameterGroupName(dbParameterGroup.dbParameterGroupName())
                .parameters(
                        Parameter.builder()
                                .parameterName("neptune_query_timeout")
                                .parameterValue("2147483647")
                                .applyMethod(ApplyMethod.PENDING_REBOOT)
                                .build()
                ).build());

        List<Parameter> dbParameters = neptune.describeDBParameters(
                        DescribeDbParametersRequest.builder()
                                .dbParameterGroupName(dbParameterGroup.dbParameterGroupName())
                                .build()
                ).parameters();

        while (dbParameters.stream().noneMatch(parameter ->
                parameter.parameterName().equals("neptune_query_timeout") &&
                        parameter.parameterValue().equals("2147483647"))) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dbParameters = neptune.describeDBClusterParameters(
                            DescribeDbClusterParametersRequest.builder()
                                    .dbClusterParameterGroupName(dbParameterGroup.dbParameterGroupName())
                                    .build()
                    ).parameters();
        }

        System.err.println(String.format("DB parameter group         : %s", dbParameterGroup.dbParameterGroupName()));
        System.err.println();

        return dbParameterGroup;
    }

    private DBClusterParameterGroup createDbClusterParameterGroup(NeptuneClusterMetadata sourceClusterMetadata,
                                                                  NeptuneClient neptune) {
        DBClusterParameterGroup dbClusterParameterGroup;

        dbClusterParameterGroup = neptune.createDBClusterParameterGroup(
                CreateDbClusterParameterGroupRequest.builder()
                        .dbClusterParameterGroupName(String.format("%s-db-cluster-params", targetClusterId))
                        .description(String.format("%s DB Cluster Parameter Group", targetClusterId))
                        .dbParameterGroupFamily(sourceClusterMetadata.dbParameterGroupFamily())
                        .tags(getTags(sourceClusterMetadata.clusterId()))
                        .build()
        ).dbClusterParameterGroup();

        String neptuneStreamsParameterValue = sourceClusterMetadata.isStreamEnabled() ? "1" : "0";

        try {
            ModifyDbClusterParameterGroupRequest.Builder requestBuilder = ModifyDbClusterParameterGroupRequest.builder()
                    .dbClusterParameterGroupName(dbClusterParameterGroup.dbClusterParameterGroupName())
                    .parameters(
                            Parameter.builder()
                                    .parameterName("neptune_enforce_ssl")
                                    .parameterValue("1")
                                    .applyMethod(ApplyMethod.PENDING_REBOOT)
                                    .build(),
                            Parameter.builder()
                                    .parameterName("neptune_query_timeout")
                                    .parameterValue("2147483647")
                                    .applyMethod(ApplyMethod.PENDING_REBOOT)
                                    .build(),
                            Parameter.builder()
                                    .parameterName("neptune_streams")
                                    .parameterValue(neptuneStreamsParameterValue)
                                    .applyMethod(ApplyMethod.PENDING_REBOOT)
                                    .build());

            if (this.enableAuditLogs) {
                requestBuilder = requestBuilder.parameters(Parameter.builder()
                        .parameterName("neptune_enable_audit_log")
                        .parameterValue("1")
                        .applyMethod(ApplyMethod.PENDING_REBOOT)
                        .build());
            }

            neptune.modifyDBClusterParameterGroup(requestBuilder.build());
        } catch (NeptuneException e) {
            ModifyDbClusterParameterGroupRequest.Builder requestBuilder = ModifyDbClusterParameterGroupRequest.builder()
                    .dbClusterParameterGroupName(dbClusterParameterGroup.dbClusterParameterGroupName())
                    .parameters(
                            Parameter.builder()
                                    .parameterName("neptune_query_timeout")
                                    .parameterValue("2147483647")
                                    .applyMethod(ApplyMethod.PENDING_REBOOT)
                                    .build(),
                            Parameter.builder()
                                    .parameterName("neptune_streams")
                                    .parameterValue(neptuneStreamsParameterValue)
                                    .applyMethod(ApplyMethod.PENDING_REBOOT)
                                    .build());

            if (this.enableAuditLogs) {
                requestBuilder = requestBuilder.parameters(Parameter.builder()
                        .parameterName("neptune_enable_audit_log")
                        .parameterValue("1")
                        .applyMethod(ApplyMethod.PENDING_REBOOT)
                        .build());
            }

            neptune.modifyDBClusterParameterGroup(requestBuilder.build());
        }

        List<Parameter> dbClusterParameters = neptune.describeDBClusterParameters(
                        DescribeDbClusterParametersRequest.builder()
                                .dbClusterParameterGroupName(dbClusterParameterGroup.dbClusterParameterGroupName())
                                .build()
                ).parameters();

        while (dbClusterParameters.stream().noneMatch(parameter ->
                parameter.parameterName().equals("neptune_query_timeout") &&
                        parameter.parameterValue().equals("2147483647"))) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dbClusterParameters = neptune.describeDBClusterParameters(
                            DescribeDbClusterParametersRequest.builder()
                                    .dbClusterParameterGroupName(dbClusterParameterGroup.dbClusterParameterGroupName())
                                    .build()
                    ).parameters();
        }

        System.err.println(String.format("DB cluster parameter group : %s", dbClusterParameterGroup.dbClusterParameterGroupName()));

        return dbClusterParameterGroup;
    }

    private void createInstance(String name,
                                NeptuneClient neptune,
                                NeptuneClusterMetadata sourceClusterMetadata,
                                InstanceType instanceType,
                                DBParameterGroup dbParameterGroup,
                                DBCluster targetDbCluster) {

        System.err.println("Creating target " + name + " instance...");

        CreateDbInstanceRequest.Builder requestBuilder = CreateDbInstanceRequest.builder()
                .dbInstanceClass(instanceType.value())
                .dbInstanceIdentifier(String.format("neptune-export-%s-%s", name, UUID.randomUUID().toString().substring(0, 5)))
                .dbClusterIdentifier(targetDbCluster.dbClusterIdentifier())
                .dbParameterGroupName(dbParameterGroup.dbParameterGroupName())
                .engine("neptune")
                .tags(getTags(sourceClusterMetadata.clusterId()))
                ;

        if (StringUtils.isNotEmpty(engineVersion)) {
            requestBuilder = requestBuilder.engineVersion(engineVersion);
        }

        DBInstance targetDbInstance = neptune.createDBInstance(requestBuilder.build()).dbInstance();

        String instanceStatus = targetDbInstance.dbInstanceStatus();

        while (instanceStatus.equals("creating")) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            instanceStatus = neptune.describeDBInstances(DescribeDbInstancesRequest.builder()
                            .dbInstanceIdentifier(targetDbInstance.dbInstanceIdentifier()).build())
                    .dbInstances()
                    .get(0)
                    .dbInstanceStatus();
        }
    }

}
