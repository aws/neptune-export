/*
Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.propertygraph;

import com.amazonaws.services.neptune.cluster.ConcurrencyConfig;
import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.propertygraph.schema.ExportSpecification;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.Optional;

public class LazyQueriesRangeFactoryProvider {

    private final RangeConfig rangeConfig;
    private final ConcurrencyConfig concurrencyConfig;
    private final NeptuneGremlinClient client;
    private final LabelsFilter nodeLabelsFilter;
    private final LabelsFilter edgeLabelsFilter;
    private final FeatureToggles featureToggles;

    private RangeFactory nodesRangeFactory;
    private RangeFactory edgesRangeFactory;

    public LazyQueriesRangeFactoryProvider(RangeConfig rangeConfig,
                                           ConcurrencyConfig concurrencyConfig,
                                           NeptuneGremlinClient client,
                                           Collection<ExportSpecification> exportSpecifications,
                                           FeatureToggles featureToggles) {
        this.rangeConfig = rangeConfig;
        this.concurrencyConfig = concurrencyConfig;
        this.client = client;
        this.featureToggles = featureToggles;

        Optional<ExportSpecification> nodeSpecification = exportSpecifications.stream()
                .filter(e -> e.getGraphElementType().equals(GraphElementType.nodes)).findFirst();
        nodeLabelsFilter = nodeSpecification.isPresent() ?
                nodeSpecification.get().getLabelsFilter() :
                new AllLabels(NodeLabelStrategy.nodeLabelsOnly);

        Optional<ExportSpecification> edgeSpecification = exportSpecifications.stream()
                .filter(e -> e.getGraphElementType().equals(GraphElementType.edges)).findFirst();
        edgeLabelsFilter = edgeSpecification.isPresent() ?
                edgeSpecification.get().getLabelsFilter() :
                new AllLabels(EdgeLabelStrategy.edgeLabelsOnly);
    }

    public RangeFactory getNodesRangeFactory() {
        if(nodesRangeFactory == null) {
            try (GraphTraversalSource g = client.newTraversalSource()) {
                nodesRangeFactory = RangeFactory.create(
                        new NodesClient(g, false, null, featureToggles),
                        nodeLabelsFilter, GremlinFilters.EMPTY, rangeConfig, concurrencyConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return nodesRangeFactory;
    }

    public RangeFactory getEdgesRangeFactory() {
        if(edgesRangeFactory == null) {
            try (GraphTraversalSource g = client.newTraversalSource()) {
                edgesRangeFactory = RangeFactory.create(
                        new EdgesClient(g, false, null, featureToggles),
                        edgeLabelsFilter, GremlinFilters.EMPTY, rangeConfig, concurrencyConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return edgesRangeFactory;
    }

}
