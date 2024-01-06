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
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class LazyQueriesRangeFactoryProviderTest {

    private NeptuneGremlinClient mockClient;
    private LazyQueriesRangeFactoryProvider rangeFactoryProvider;
    private GraphTraversalSource g;

    @Before
    public void setup() {
        mockClient = mock(NeptuneGremlinClient.class);
        g = spy(TinkerFactory.createModern().traversal());
        when(mockClient.newTraversalSource()).thenReturn(g);

        rangeFactoryProvider = new LazyQueriesRangeFactoryProvider(
                new RangeConfig(4, 0, Long.MAX_VALUE, -1, -1),
                new ConcurrencyConfig(4),
                mockClient,
                createExportSpecifications(),
                new FeatureToggles(Collections.EMPTY_SET)
        );
    }

    @Test
    public void shouldOnlyCountNodesWhenFirstRequested() {
        // Validate lazy initialization (count and creation of g is only done when requested)
        verify(mockClient, times(0)).newTraversalSource();
        verify(g, times(0)).V();

        // Validate traversal source is created and queried after first invocation
        rangeFactoryProvider.getNodesRangeFactory();
        verify(mockClient, times(1)).newTraversalSource();
        verify(g, times(1)).V();

        // Validate traversal source is not recreated or re-queried on second invocation
        rangeFactoryProvider.getNodesRangeFactory();
        verify(mockClient, times(1)).newTraversalSource();
        verify(g, times(1)).V();
    }

    @Test
    public void shouldOnlyCountEdgesWhenFirstRequested() {
        // Validate lazy initialization (count and creation of g is only done when requested)
        verify(mockClient, times(0)).newTraversalSource();
        verify(g, times(0)).E();

        // Validate traversal source is created and queried after first invocation
        rangeFactoryProvider.getEdgesRangeFactory();
        verify(mockClient, times(1)).newTraversalSource();
        verify(g, times(1)).E();

        // Validate traversal source is not recreated or re-queried on second invocation
        rangeFactoryProvider.getEdgesRangeFactory();
        verify(mockClient, times(1)).newTraversalSource();
        verify(g, times(1)).E();
    }

    private Collection<ExportSpecification> createExportSpecifications() {
        Collection<ExportSpecification> exportSpecifications = new ArrayList<>();
        exportSpecifications.add(new ExportSpecification(
                GraphElementType.nodes,
                new AllLabels(NodeLabelStrategy.nodeLabelsOnly),
                GremlinFilters.EMPTY,
                null,
                false,
                new FeatureToggles(Collections.EMPTY_SET)
        ));
        exportSpecifications.add(new ExportSpecification(
                GraphElementType.edges,
                new AllLabels(NodeLabelStrategy.nodeLabelsOnly),
                GremlinFilters.EMPTY,
                null,
                false,
                new FeatureToggles(Collections.EMPTY_SET)
        ));
        return exportSpecifications;
    }

}
