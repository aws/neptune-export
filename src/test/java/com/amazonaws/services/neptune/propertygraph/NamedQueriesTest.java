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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.collection.UnmodifiableCollection;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NamedQueriesTest {

    Collection<String> testQueries;
    final Collection<String> sampleQueries;

    public NamedQueriesTest() {
        Collection<String> sampleData = new ArrayList<>();
        sampleData.add("g.V().hasLabel(\"person\")");
        sampleData.add("g.V().out()");
        sampleData.add("g.E().outV()");
        sampleQueries = UnmodifiableCollection.decorate(sampleData);
    }

    @Before
    public void setupClass() {
        testQueries = new ArrayList<>();
        testQueries.addAll(sampleQueries);
    }

    @Test
    public void shouldNotModifyQueriesWithEffectiveConcurrency1() {
        NamedQueries namedQueries = new NamedQueries("name", testQueries);
        namedQueries.split(initRangeFactoryProvider(
                new RangeConfig(-1, 0, Long.MAX_VALUE, -1, -1),
                new ConcurrencyConfig(1)
        ));

        assertTrue(CollectionUtils.isEqualCollection(sampleQueries, namedQueries.queries()));
    }

    @Test
    public void shouldSplitQueriesAccordingToRangeConfig() {
        NamedQueries namedQueries = new NamedQueries("name", testQueries);
        namedQueries.split(initRangeFactoryProvider(
                new RangeConfig(5, 0, Long.MAX_VALUE, -1, -1),
                new ConcurrencyConfig(4)
        ));

        Collection<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("g.V().range(0, 5).hasLabel(\"person\")");
        expectedQueries.add("g.V().range(5, -1).hasLabel(\"person\")");
        expectedQueries.add("g.V().range(0, 5).out()");
        expectedQueries.add("g.V().range(5, -1).out()");
        expectedQueries.add("g.E().range(0, 5).outV()");
        expectedQueries.add("g.E().range(5, 10).outV()");
        expectedQueries.add("g.E().range(10, -1).outV()");

        assertTrue(CollectionUtils.isEqualCollection(expectedQueries, namedQueries.queries()));
    }

    @Test
    public void shouldOnlySplitEdgeQueries() {
        NamedQueries namedQueries = new NamedQueries("name", testQueries);
        namedQueries.split(initRangeFactoryProvider(
                new RangeConfig(10, 0, Long.MAX_VALUE, -1, -1),
                new ConcurrencyConfig(4)
        ));

        Collection<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("g.V().hasLabel(\"person\")");
        expectedQueries.add("g.V().out()");
        expectedQueries.add("g.E().range(0, 10).outV()");
        expectedQueries.add("g.E().range(10, -1).outV()");

        assertTrue(CollectionUtils.isEqualCollection(expectedQueries, namedQueries.queries()));
    }

    @Test
    public void shouldOnlySplitVertexQueries() {
        NamedQueries namedQueries = new NamedQueries("name", testQueries);
        namedQueries.split(initRangeFactoryProvider(
                new RangeConfig(15, 0, Long.MAX_VALUE, 30, -1),
                new ConcurrencyConfig(4)
        ));

        Collection<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("g.V().range(0, 15).hasLabel(\"person\")");
        expectedQueries.add("g.V().range(15, -1).hasLabel(\"person\")");
        expectedQueries.add("g.V().range(0, 15).out()");
        expectedQueries.add("g.V().range(15, -1).out()");
        expectedQueries.add("g.E().outV()");

        assertTrue(CollectionUtils.isEqualCollection(expectedQueries, namedQueries.queries()));
    }

    @Test
    public void shouldSplitBasedOnApproxCountsAndConcurrency() {
        NamedQueries namedQueries = new NamedQueries("name", testQueries);
        namedQueries.split(initRangeFactoryProvider(
                new RangeConfig(-1, 0, Long.MAX_VALUE, 1200, 1500),
                new ConcurrencyConfig(3)
        ));

        Collection<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("g.V().range(0, 401).hasLabel(\"person\")");
        expectedQueries.add("g.V().range(401, 802).hasLabel(\"person\")");
        expectedQueries.add("g.V().range(802, -1).hasLabel(\"person\")");
        expectedQueries.add("g.V().range(0, 401).out()");
        expectedQueries.add("g.V().range(401, 802).out()");
        expectedQueries.add("g.V().range(802, -1).out()");
        expectedQueries.add("g.E().range(0, 501).outV()");
        expectedQueries.add("g.E().range(501, 1002).outV()");
        expectedQueries.add("g.E().range(1002, -1).outV()");

        assertTrue(CollectionUtils.isEqualCollection(expectedQueries, namedQueries.queries()));
    }

    static LazyQueriesRangeFactoryProvider initRangeFactoryProvider(RangeConfig rangeConfig, ConcurrencyConfig concurrencyConfig) {
        NeptuneGremlinClient mockClient = mock(NeptuneGremlinClient.class);
        when(mockClient.newTraversalSource()).thenReturn(TinkerFactory.createTheCrew().traversal());

        return new LazyQueriesRangeFactoryProvider(
                rangeConfig,
                concurrencyConfig,
                mockClient,
                createExportSpecifications(),
                new FeatureToggles(Collections.EMPTY_SET)
        );
    }

    private static Collection<ExportSpecification> createExportSpecifications() {
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
