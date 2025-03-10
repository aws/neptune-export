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
import software.amazon.awssdk.services.neptune.NeptuneClient;
import software.amazon.awssdk.services.neptune.model.*;

import java.util.List;
import java.util.function.Supplier;

public class GetClusterIdFromCorrelationId {
    private final String correlationId;
    private final Supplier<NeptuneClient> amazonNeptuneClientSupplier;

    public GetClusterIdFromCorrelationId(String correlationId, Supplier<NeptuneClient> amazonNeptuneClientSupplier) {

        this.correlationId = correlationId;
        this.amazonNeptuneClientSupplier = amazonNeptuneClientSupplier;
    }

    public String execute() {
        NeptuneClient neptune = amazonNeptuneClientSupplier.get();

        try {

            return Timer.timedActivity("getting cluster ID from correlation ID", false,
                    (Activity.Callable<String>) () -> getClusterId(neptune));
        } finally {
            if (neptune != null) {
                neptune.close();
            }
        }
    }

    private String getClusterId(NeptuneClient neptune) {
        DescribeDbClustersResponse describeDBClustersResult = neptune.describeDBClusters(DescribeDbClustersRequest.builder().build());

        for (DBCluster dbCluster : describeDBClustersResult.dbClusters()) {
            String clusterCorrelationId = getCorrelationId(dbCluster.dbClusterArn(), neptune);
            if (StringUtils.isNotEmpty(clusterCorrelationId) && clusterCorrelationId.equals(correlationId)) {
                String clusterId = dbCluster.dbClusterIdentifier();
                System.err.println(String.format("Found cluster ID %s for correlation ID %s", clusterId, correlationId));
                return clusterId;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }

        System.err.println(String.format("Unable to find cluster ID for correlation ID %s", correlationId));

        return null;
    }

    private String getCorrelationId(String dbClusterArn, NeptuneClient neptune) {

        List<Tag> tagList = neptune.listTagsForResource(
                ListTagsForResourceRequest.builder()
                        .resourceName(dbClusterArn).build()).tagList();

        for (Tag tag : tagList) {
            if (tag.key().equalsIgnoreCase(NeptuneClusterMetadata.NEPTUNE_EXPORT_CORRELATION_ID_KEY)) {
                return tag.value();
            }
        }

        return null;
    }
}
