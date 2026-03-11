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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.neptune.NeptuneClient;
import software.amazon.awssdk.services.neptune.model.*;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    private static final Logger logger = LoggerFactory.getLogger(AddCloneTask.class);

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

        logger.info("Creating {} replica instances with instance type {}", replicaCount, instanceType);
        ExecutorService taskExecutor = Executors.newFixedThreadPool(replicaCount);

        for (int i = 0; i < replicaCount; i++) {
            final int replicaNumber = i + 1;
            logger.debug("Scheduling creation of replica {}/{}", replicaNumber, replicaCount);
            
            taskExecutor.execute(() -> {
                logger.debug("Starting creation of replica instance {}/{}", replicaNumber, replicaCount);
                createInstance("replica",
                        neptune,
                        sourceClusterMetadata,
                        instanceType,
                        dbParameterGroup,
                        targetDbCluster);
                logger.debug("Completed creation of replica instance {}/{}", replicaNumber, replicaCount);
            });
        }

        taskExecutor.shutdown();

        try {
            final int timeoutMin = 30;
            boolean completed = taskExecutor.awaitTermination(timeoutMin, TimeUnit.MINUTES);
            if (completed) {
                logger.debug("Successfully created all {} replica instances", replicaCount);
            } else {
                logger.warn("Timed out waiting for all replicas to be created after {} minutes", timeoutMin);
            }
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
            logger.debug("Enabling audit logs for cloned cluster");
            restoreDbClusterToPointInTimeRequestBuilder = restoreDbClusterToPointInTimeRequestBuilder.enableCloudwatchLogsExports("audit");
        }

        DBCluster targetDbCluster;
        RestoreDbClusterToPointInTimeRequest request = restoreDbClusterToPointInTimeRequestBuilder.build();
        
        try {
            logger.debug("Sending restore DB cluster request: {}", request);
            targetDbCluster = neptune.restoreDBClusterToPointInTime(request).dbCluster();
        } catch (NeptuneException e) {
            logger.error("Failed to create target cluster: {} from source cluster: {} (Error code: {}, Message: {})",
                    targetClusterId, sourceClusterId, Optional.ofNullable(e.awsErrorDetails()).map(AwsErrorDetails::errorCode).orElse("N/A"),
                    e.getMessage(), e);
                
            throw e;
        }

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

        logger.debug("Cluster {} is now in {} state", targetClusterId, clusterStatus);

        // Check if the final status indicates success
        if (!clusterStatus.equals("available")) {
            logger.warn("Cluster {} is in {} state instead of 'available'", targetClusterId, clusterStatus);
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
        String paramGroupName = String.format("%s-db-cluster-params", targetClusterId);
        DBClusterParameterGroup dbClusterParameterGroup;
        
        try {
            CreateDbClusterParameterGroupRequest request = CreateDbClusterParameterGroupRequest.builder()
                    .dbClusterParameterGroupName(paramGroupName)
                    .description(String.format("%s DB Cluster Parameter Group", targetClusterId))
                    .dbParameterGroupFamily(sourceClusterMetadata.dbParameterGroupFamily())
                    .tags(getTags(sourceClusterMetadata.clusterId()))
                    .build();
                    
            dbClusterParameterGroup = neptune.createDBClusterParameterGroup(request).dbClusterParameterGroup();
            logger.debug("Successfully created DB cluster parameter group: {}", dbClusterParameterGroup.dbClusterParameterGroupName());
        } catch (NeptuneException e) {
            logger.error("Failed to create DB cluster parameter group: {} (Error code: {}, Message: {})",
                    paramGroupName, Optional.ofNullable(e.awsErrorDetails()).map(AwsErrorDetails::errorCode).orElse("N/A"),
                    e.getMessage(), e);
            throw e;
        }

        String neptuneStreamsParameterValue = sourceClusterMetadata.isStreamEnabled() ? "1" : "0";

        try {
            neptune.modifyDBClusterParameterGroup(ModifyDbClusterParameterGroupRequest.builder()
                    .dbClusterParameterGroupName(dbClusterParameterGroup.dbClusterParameterGroupName())
                    .parameters(buildClusterParameters(true, neptuneStreamsParameterValue))
                    .build());
        } catch (NeptuneException e) {
            neptune.modifyDBClusterParameterGroup(ModifyDbClusterParameterGroupRequest.builder()
                    .dbClusterParameterGroupName(dbClusterParameterGroup.dbClusterParameterGroupName())
                    .parameters(buildClusterParameters(false, neptuneStreamsParameterValue))
                    .build());
        }

        List<Parameter> dbClusterParameters = neptune.describeDBClusterParameters(
                        DescribeDbClusterParametersRequest.builder()
                                .dbClusterParameterGroupName(dbClusterParameterGroup.dbClusterParameterGroupName())
                                .build()
                ).parameters();

        int count = 0;
        while (dbClusterParameters.stream().noneMatch(parameter ->
                parameter.parameterName().equals("neptune_query_timeout") &&
                        parameter.parameterValue().equals("2147483647"))) {
            count++;
            if (count >= 30) {
                throw new IllegalStateException("Failed to create DB cluster parameter group: " + dbClusterParameterGroup.dbClusterParameterGroupName() + "after 5 minutes");
            }
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

    private Collection<Parameter> buildClusterParameters(boolean includeEnforceSsl, String neptuneStreamsParameterValue) {
        Collection<Parameter> parameters = new ArrayList<>(4);
        if (includeEnforceSsl) {
            parameters.add(Parameter.builder()
                    .parameterName("neptune_enforce_ssl")
                    .parameterValue("1")
                    .applyMethod(ApplyMethod.PENDING_REBOOT)
                    .build());
        }
        parameters.add(Parameter.builder()
                .parameterName("neptune_query_timeout")
                .parameterValue("2147483647")
                .applyMethod(ApplyMethod.PENDING_REBOOT)
                .build());
        parameters.add(Parameter.builder()
                .parameterName("neptune_streams")
                .parameterValue(neptuneStreamsParameterValue)
                .applyMethod(ApplyMethod.PENDING_REBOOT)
                .build());
        if (this.enableAuditLogs) {
            logger.debug("Adding neptune_enable_audit_log parameter");
            parameters.add(Parameter.builder()
                    .parameterName("neptune_enable_audit_log")
                    .parameterValue("1")
                    .applyMethod(ApplyMethod.PENDING_REBOOT)
                    .build());
        }
        return parameters;
    }

    private void createInstance(String name,
                                NeptuneClient neptune,
                                NeptuneClusterMetadata sourceClusterMetadata,
                                InstanceType instanceType,
                                DBParameterGroup dbParameterGroup,
                                DBCluster targetDbCluster) {

        String instanceId = String.format("neptune-export-%s-%s", name, UUID.randomUUID().toString().substring(0, 5));
        System.err.println("Creating target " + name + " instance...");

        CreateDbInstanceRequest.Builder requestBuilder = CreateDbInstanceRequest.builder()
                .dbInstanceClass(instanceType.value())
                .dbInstanceIdentifier(instanceId)
                .dbClusterIdentifier(targetDbCluster.dbClusterIdentifier())
                .dbParameterGroupName(dbParameterGroup.dbParameterGroupName())
                .engine("neptune")
                .tags(getTags(sourceClusterMetadata.clusterId()));

        if (StringUtils.isNotEmpty(engineVersion)) {
            requestBuilder = requestBuilder.engineVersion(engineVersion);
        }

        // Retry configuration
        int maxRetries = 3;
        long initialBackoffMillis = 1000; // 1 second
        DBInstance targetDbInstance = null;
        CreateDbInstanceRequest request = requestBuilder.build();
        
        // Retry loop with exponential backoff
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("Sending create DB instance request for {} in cluster {}", instanceId, targetDbCluster.dbClusterIdentifier());
                targetDbInstance = neptune.createDBInstance(request).dbInstance();
                // If we get here, the request was successful
                break;
            } catch (NeptuneException e) {
                // Check if we've exhausted our retries
                if (attempt == maxRetries) {
                    logger.error("Failed to create {} instance after {} attempts, with error {}", name, maxRetries, e.getMessage());
                    return;
                }
                
                // Calculate backoff time with exponential increase and some jitter
                long backoffMillis = initialBackoffMillis * (long) Math.pow(2, attempt);
                long jitterMillis = (long) (backoffMillis * 0.2 * Math.random()); // 20% jitter
                long totalBackoffMillis = backoffMillis + jitterMillis;

                logger.debug("Failed to create {} instance (attempt {} of {}): {}. Retrying...", name, attempt + 1, maxRetries, e.getMessage());
                
                try {
                    Thread.sleep(totalBackoffMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Instance creation interrupted", ie);
                }
            }
        }
        
        if (targetDbInstance == null) {
            logger.warn("Failed to create DB instance {} after exhausting all retries", name);
            return;
        }

        String instanceStatus = targetDbInstance.dbInstanceStatus();

        while (instanceStatus.equals("creating")) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            try {
                DescribeDbInstancesRequest describeRequest = DescribeDbInstancesRequest.builder()
                        .dbInstanceIdentifier(targetDbInstance.dbInstanceIdentifier())
                        .build();
                        
                instanceStatus = neptune.describeDBInstances(describeRequest)
                        .dbInstances()
                        .get(0)
                        .dbInstanceStatus();
                        
                logger.debug("Instance {} status: {}", targetDbInstance.dbInstanceIdentifier(), instanceStatus);
            } catch (NeptuneException e) {
                logger.error("Error checking instance status: {} (Error code: {}, Message: {})",
                    targetDbInstance.dbInstanceIdentifier(), Optional.ofNullable(e.awsErrorDetails()).map(AwsErrorDetails::errorCode).orElse("N/A"),
                        e.getMessage(), e);
                    
                if (e.awsErrorDetails() != null && e.awsErrorDetails().errorCode() != null && e.awsErrorDetails().errorCode().equals("DBInstanceNotFound")) {
                    logger.error("The instance {} was not found. It may have been deleted or failed to create properly.", 
                        targetDbInstance.dbInstanceIdentifier());
                }
                throw e;
            }
        }
        
        logger.debug("{} instance {} is now in {} state",
            name, targetDbInstance.dbInstanceIdentifier(), instanceStatus);
            
        // Check if the final status indicates success
        if (!instanceStatus.equals("available")) {
            logger.warn("{} instance {} is in {} state instead of 'available'", 
                name, targetDbInstance.dbInstanceIdentifier(), instanceStatus);
        }
    }

}
