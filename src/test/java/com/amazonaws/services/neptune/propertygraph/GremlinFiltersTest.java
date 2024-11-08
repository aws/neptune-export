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

package com.amazonaws.services.neptune.propertygraph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GremlinFiltersTest {


    @Test
    public void shouldNotModifyTraversalWithNoFilters() {
        GremlinFilters filters = new GremlinFilters(null, null, null, false);

        assertEquals("__.inject(\"x\")", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\")", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    @Test
    public void shouldApplyGremlinFilterToNodesAndEdges() {
        GremlinFilters filters = new GremlinFilters("constant(\"both\")", null, null, false);

        assertEquals("__.inject(\"x\").constant(\"both\")", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").constant(\"both\")", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    @Test
    public void shouldPreferFilterNodesOnly() {
        GremlinFilters filters = new GremlinFilters(null, "constant(\"node\")", null, false);

        assertEquals("__.inject(\"x\").constant(\"node\")", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\")", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    @Test
    public void shouldPreferFilterEdgesOnly() {
        GremlinFilters filters = new GremlinFilters(null, null, "constant(\"edge\")", false);

        assertEquals("__.inject(\"x\")", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").constant(\"edge\")", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    @Test
    public void shouldPreferSpecificFilters() {
        GremlinFilters filters = new GremlinFilters("constant(\"both\")", "constant(\"node\")", "constant(\"edge\")", false);

        assertEquals("__.inject(\"x\").constant(\"node\")", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").constant(\"edge\")", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    @Test
    public void shouldWorkWithRepeatedUse() {
        GremlinFilters filters = new GremlinFilters("constant(\"both\")", "constant(\"node\")", "constant(\"edge\")", false);

        filters.applyToNodes(anonymousTraversal());
        filters.applyToNodes(anonymousTraversal());
        filters.applyToNodes(anonymousTraversal());

        filters.applyToEdges(anonymousTraversal());
        filters.applyToEdges(anonymousTraversal());
        filters.applyToEdges(anonymousTraversal());

        assertEquals("__.inject(\"x\").constant(\"node\")", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").constant(\"edge\")", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    private GraphTraversal anonymousTraversal() {
        return __.inject("x");
    }

}
