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

package com.amazonaws.services.neptune.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.neptune.NeptuneClient;
import com.amazonaws.services.neptune.export.EndpointValidator;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.neptune.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NeptuneClusterMetadata {

    public static final String NEPTUNE_EXPORT_APPLICATION_TAG = "neptune-export";
    public static final String NEPTUNE_EXPORT_CORRELATION_ID_KEY = "correlation-id";

    private static final Logger logger = LoggerFactory.getLogger(NeptuneClusterMetadata.class);

    public static String clusterIdFromEndpoint(String endpoint) {
        int index = endpoint.indexOf(".");
        if (index < 0) {
            throw new IllegalArgumentException(String.format("Unable to identify cluster ID from endpoint '%s'. Use the clusterId export parameter instead.", endpoint));
        }
        return endpoint.substring(0, index);
    }

    public static NeptuneClusterMetadata createFromEndpoints(Collection<String> endpoints, Supplier<NeptuneClient> amazonNeptuneClientSupplier) {
        NeptuneClient neptune = amazonNeptuneClientSupplier.get();

        String paginationToken = null;

        do {
            DescribeDbClustersResponse describeDbClustersResponse = neptune
                    .describeDBClusters(DescribeDbClustersRequest.builder()
                            .marker(paginationToken)
                            .filters(Filter.builder().name("engine").values("neptune").build())
                            .build()
                    );

            paginationToken = describeDbClustersResponse.marker();

            for (DBCluster dbCluster : describeDbClustersResponse.dbClusters()) {
                for (String endpoint : endpoints) {
                    String endpointValue = getEndpointValue(endpoint);
                    if (endpointValue.equals(getEndpointValue(dbCluster.endpoint()))){
                        return createFromClusterId(dbCluster.dbClusterIdentifier(), amazonNeptuneClientSupplier);
                    } else if (endpointValue.equals(getEndpointValue(dbCluster.readerEndpoint()))){
                        return createFromClusterId(dbCluster.dbClusterIdentifier(), amazonNeptuneClientSupplier);
                    }
                }
            }
        } while (paginationToken != null);

        paginationToken = null;

        do {

            DescribeDbInstancesResponse describeDbInstancesResponse = neptune.describeDBInstances(
                    DescribeDbInstancesRequest.builder()
                            .marker(paginationToken)
                            .filters(Filter.builder().name("engine").values("neptune").build())
                            .build()
            );

            paginationToken = describeDbInstancesResponse.marker();

            for (DBInstance dbInstance : describeDbInstancesResponse.dbInstances()) {
                for (String endpoint : endpoints) {
                    String endpointValue = getEndpointValue(endpoint);
                    if (endpointValue.equals(getEndpointValue(dbInstance.endpoint().address()))){
                        return createFromClusterId(dbInstance.dbClusterIdentifier(), amazonNeptuneClientSupplier);
                    }
                }
            }

        } while (paginationToken != null);

        throw new IllegalStateException(String.format("Unable to identify cluster ID from endpoints: %s", endpoints));

    }

    private static String getEndpointValue(String endpoint) {
        return EndpointValidator.validate(endpoint).toLowerCase();
    }

    public static NeptuneClusterMetadata createFromClusterId(String clusterId, Supplier<NeptuneClient> amazonNeptuneClientSupplier) {

        NeptuneClient neptune = amazonNeptuneClientSupplier.get();

        DescribeDbClustersResponse describeDbClustersResponse = neptune
                .describeDBClusters(DescribeDbClustersRequest.builder().dbClusterIdentifier(clusterId).build());

        if (describeDbClustersResponse.dbClusters().isEmpty()) {
            throw new IllegalArgumentException(String.format("Unable to find cluster %s", clusterId));
        }

        DBCluster dbCluster = describeDbClustersResponse.dbClusters().get(0);

        List<Tag> tags = neptune.listTagsForResource(
                ListTagsForResourceRequest.builder().resourceName(dbCluster.dbClusterArn()).build()
        ).tagList();

        Map<String, String> clusterTags = new HashMap<>();
        tags.forEach(t -> clusterTags.put(t.key(), t.value()));

        boolean isIAMDatabaseAuthenticationEnabled = dbCluster.iamDatabaseAuthenticationEnabled();
        Integer port = dbCluster.port();
        String dbClusterParameterGroup = dbCluster.dbClusterParameterGroup();
        String engineVersion = dbCluster.engineVersion();

        String dbParameterGroupFamily;

        try {
            DescribeDbClusterParameterGroupsResponse describeDbClusterParameterGroupsResponse = neptune.describeDBClusterParameterGroups(
                    DescribeDbClusterParameterGroupsRequest.builder()
                            .dbClusterParameterGroupName(dbClusterParameterGroup)
                            .build()
            );

            Optional<DBClusterParameterGroup> parameterGroup = describeDbClusterParameterGroupsResponse
                    .dbClusterParameterGroups().stream().findFirst();

            dbParameterGroupFamily = parameterGroup.isPresent() ?
                    parameterGroup.get().dbParameterGroupFamily() :
                    "neptune1";

        } catch (NeptuneException e) {
            logger.warn("Failed to retrieve DB parameter group family: {}. Falling back to version-based detection.", e.getMessage());

            // Older deployments of Neptune Export service may not have requisite permissions to
            // describe cluster parameter group, so we'll try and guess the group family.

            Pattern versionPattern = Pattern.compile("^(\\d+)\\.(\\d+)(\\.\\d+)*");
            Matcher matcher = versionPattern.matcher(engineVersion != null ? engineVersion : "");

            if (StringUtils.isNotEmpty(engineVersion) && matcher.find()) {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));

                if (major == 1 && minor <= 1) {
                    dbParameterGroupFamily = "neptune1";
                } else {
                    dbParameterGroupFamily = String.format("neptune%s.%s", major, minor);
                }
            } else {
                logger.error("Failed to determine correct DB Parameter group family from engine version [{}]", engineVersion);
                throw e;
            }
        }

        DescribeDbClusterParametersResponse describeDbClusterParametersResponse = neptune.describeDBClusterParameters(
                DescribeDbClusterParametersRequest.builder()
                        .dbClusterParameterGroupName(dbClusterParameterGroup)
                        .build()
        );
        Optional<Parameter> neptuneStreamsParameter = describeDbClusterParametersResponse.parameters().stream()
                .filter(parameter -> parameter.parameterName().equals("neptune_streams"))
                .findFirst();
        boolean isStreamEnabled = neptuneStreamsParameter.isPresent() &&
                neptuneStreamsParameter.get().parameterValue().equals("1");

        String dbSubnetGroup = dbCluster.dbSubnetGroup();
        List<VpcSecurityGroupMembership> vpcSecurityGroups = dbCluster.vpcSecurityGroups();
        List<String> vpcSecurityGroupIds = vpcSecurityGroups.stream()
                .map(VpcSecurityGroupMembership::vpcSecurityGroupId)
                .collect(Collectors.toList());

        List<DBClusterMember> dbClusterMembers = dbCluster.dbClusterMembers();
        Optional<DBClusterMember> clusterWriter = dbClusterMembers.stream()
                .filter(DBClusterMember::isClusterWriter)
                .findFirst();

        String primary = clusterWriter.map(DBClusterMember::dbInstanceIdentifier).orElse("");
        List<String> replicas = dbClusterMembers.stream()
                .filter(dbClusterMember -> !dbClusterMember.isClusterWriter())
                .map(DBClusterMember::dbInstanceIdentifier)
                .collect(Collectors.toList());

        DescribeDbInstancesRequest describeDBInstancesRequest = DescribeDbInstancesRequest.builder()
                .filters(Collections.singletonList(
                        Filter.builder()
                                .name("db-cluster-id")
                                .values(dbCluster.dbClusterIdentifier())
                                .build()
                )).build();

        DescribeDbInstancesResponse describeDbInstancesResponse = neptune
                .describeDBInstances(describeDBInstancesRequest);

        Map<String, NeptuneInstanceMetadata> instanceTypes = new HashMap<>();
        describeDbInstancesResponse.dbInstances()
                .forEach(c -> instanceTypes.put(
                        c.dbInstanceIdentifier(),
                        new NeptuneInstanceMetadata(
                                c.dbInstanceClass(),
                                c.dbParameterGroups().get(0).dbParameterGroupName(),
                                c.endpoint())
                ));

        neptune.close();

        return new NeptuneClusterMetadata(clusterId,
                port,
                engineVersion,
                dbClusterParameterGroup,
                dbParameterGroupFamily,
                isIAMDatabaseAuthenticationEnabled,
                isStreamEnabled,
                dbSubnetGroup,
                vpcSecurityGroupIds,
                primary,
                replicas,
                instanceTypes,
                clusterTags,
                amazonNeptuneClientSupplier);
    }

    private final String clusterId;
    private final int port;
    private final String engineVersion;
    private final String dbClusterParameterGroupName;
    private final String dbParameterGroupFamily;
    private final Boolean isIAMDatabaseAuthenticationEnabled;
    private final Boolean isStreamEnabled;
    private final String dbSubnetGroupName;
    private final Collection<String> vpcSecurityGroupIds;
    private final String primary;
    private final Collection<String> replicas;
    private final Map<String, NeptuneInstanceMetadata> instanceMetadata;
    private final Map<String, String> clusterTags;

    private final Supplier<NeptuneClient> amazonNeptuneClientSupplier;

    private NeptuneClusterMetadata(String clusterId,
                                   int port,
                                   String engineVersion,
                                   String dbClusterParameterGroupName,
                                   String dbParameterGroupFamily,
                                   Boolean isIAMDatabaseAuthenticationEnabled,
                                   Boolean isStreamEnabled,
                                   String dbSubnetGroupName,
                                   List<String> vpcSecurityGroupIds,
                                   String primary,
                                   Collection<String> replicas,
                                   Map<String, NeptuneInstanceMetadata> instanceMetadata,
                                   Map<String, String> clusterTags,
                                   Supplier<NeptuneClient> amazonNeptuneClientSupplier) {
        this.clusterId = clusterId;
        this.port = port;
        this.engineVersion = engineVersion;
        this.dbClusterParameterGroupName = dbClusterParameterGroupName;
        this.dbParameterGroupFamily = dbParameterGroupFamily;
        this.isIAMDatabaseAuthenticationEnabled = isIAMDatabaseAuthenticationEnabled;
        this.isStreamEnabled = isStreamEnabled;
        this.dbSubnetGroupName = dbSubnetGroupName;
        this.vpcSecurityGroupIds = vpcSecurityGroupIds;
        this.primary = primary;
        this.replicas = replicas;
        this.instanceMetadata = instanceMetadata;
        this.clusterTags = clusterTags;
        this.amazonNeptuneClientSupplier = amazonNeptuneClientSupplier;
    }

    public String clusterId() {
        return clusterId;
    }

    public int port() {
        return port;
    }

    public String engineVersion() {
        return engineVersion;
    }

    public String dbClusterParameterGroupName() {
        return dbClusterParameterGroupName;
    }

    public String dbParameterGroupFamily() {
        return dbParameterGroupFamily;
    }

    public Boolean isIAMDatabaseAuthenticationEnabled() {
        return isIAMDatabaseAuthenticationEnabled;
    }

    public Boolean isStreamEnabled() {
        return isStreamEnabled;
    }

    public String dbSubnetGroupName() {
        return dbSubnetGroupName;
    }

    public Collection<String> vpcSecurityGroupIds() {
        return vpcSecurityGroupIds;
    }

    public String primary() {
        return primary;
    }

    public Collection<String> replicas() {
        return replicas;
    }

    public NeptuneInstanceMetadata instanceMetadataFor(String key) {
        return instanceMetadata.get(key);
    }

    public List<String> endpoints() {
        return instanceMetadata.values().stream().map(i -> i.endpoint().address()).collect(Collectors.toList());
    }

    public boolean isTaggedWithNeptuneExport() {
        return clusterTags.containsKey("application") &&
                clusterTags.get("application").equalsIgnoreCase(NEPTUNE_EXPORT_APPLICATION_TAG);
    }

    public Supplier<NeptuneClient> clientSupplier() {
        return amazonNeptuneClientSupplier;
    }

    public void printDetails(){
        System.err.println("Cluster ID              : " + clusterId());
        System.err.println("Port                    : " + port());
        System.err.println("Engine                  : " + engineVersion());
        System.err.println("IAM DB Auth             : " + isIAMDatabaseAuthenticationEnabled());
        System.err.println("Streams enabled         : " + isStreamEnabled());
        System.err.println("Parameter group family  : " + dbParameterGroupFamily());
        System.err.println("Cluster parameter group : " + dbClusterParameterGroupName());
        System.err.println("Subnet group            : " + dbSubnetGroupName());
        System.err.println("Security group IDs      : " + String.join(", ", vpcSecurityGroupIds()));
        System.err.println("Instance endpoints      : " + String.join(", ", endpoints()));

        NeptuneInstanceMetadata primary = instanceMetadataFor(primary());
        System.err.println();
        System.err.println("Primary");
        System.err.println("  Instance ID              : " + primary());
        System.err.println("  Instance type            : " + primary.instanceType());
        System.err.println("  Endpoint                 : " + primary.endpoint().address());
        System.err.println("  Database parameter group : " + primary.dbParameterGroupName());

        if (!replicas().isEmpty()) {
            for (String replicaId : replicas()) {
                NeptuneInstanceMetadata replica = instanceMetadataFor(replicaId);
                System.err.println();
                System.err.println("Replica");
                System.err.println("  Instance ID              : " + replicaId);
                System.err.println("  Instance type            : " + replica.instanceType());
                System.err.println("  Endpoint                 : " + replica.endpoint().address());
                System.err.println("  Database parameter group : " + replica.dbParameterGroupName());
            }
        }
    }

    public static class NeptuneInstanceMetadata {
        private final String instanceType;
        private final String dbParameterGroupName;
        private final Endpoint endpoint;

        public NeptuneInstanceMetadata(String instanceType, String dbParameterGroupName, Endpoint endpoint) {
            this.instanceType = instanceType;
            this.dbParameterGroupName = dbParameterGroupName;
            this.endpoint = endpoint;
        }

        public String instanceType() {
            return instanceType;
        }

        public String dbParameterGroupName() {
            return dbParameterGroupName;
        }

        public Endpoint endpoint() {
            return endpoint;
        }
    }
}
