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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void shouldHandleDatetimeFunction() {
        GremlinFilters filters = new GremlinFilters("constant(datetime(\"2018-03-22T00:35:44.741Z\"))", null, null, false);

        assertEquals("__.inject(\"x\").constant(new Date(1521678944741L))", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").constant(new Date(1521678944741L))", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));

        filters = new GremlinFilters("constant(datetime(\"2018-03-22T00:35:44.741+1600\"))", null, null, false);

        assertEquals("__.inject(\"x\").constant(new Date(1521621344741L))", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").constant(new Date(1521621344741L))", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    @Test
    public void shouldFailOnInvalidGremlinFilter() {
        assertTrue("Exception does not contain expected message", assertThrows(IllegalStateException.class,
                ()->new GremlinFilters("notARealGremlinStep()", null, null, false)
        ).getMessage().contains("Invalid Gremlin filter: notARealGremlinStep()"));
    }

    @Test
    public void shouldFailOnInvalidGremlinNodeFilter() {
        assertTrue("Exception does not contain expected message", assertThrows(IllegalStateException.class,
                ()->new GremlinFilters(null, "notARealGremlinStep()", null, false)
        ).getMessage().contains("Invalid Gremlin node filter: notARealGremlinStep()"));
    }

    @Test
    public void shouldFailOnInvalidGremlinEdgeFilter() {
        assertTrue("Exception does not contain expected message", assertThrows(IllegalStateException.class,
                ()->new GremlinFilters(null, null, "notARealGremlinStep()", false)
        ).getMessage().contains("Invalid Gremlin edge filter: notARealGremlinStep()"));
    }

    @Test
    public void shouldApplyComplexGremlinFilterToNodesAndEdges() {
        GremlinFilters filters = new GremlinFilters("where(__.or(hasLabel('person'), has('name', 'Cole'))).has('age',inside(20,30))", null, null, false);

        assertEquals("__.inject(\"x\").where(__.or(__.hasLabel(\"person\"),__.has(\"name\",\"Cole\"))).has(\"age\",P.gt((int) 20).and(P.lt((int) 30)))", GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").where(__.or(__.hasLabel(\"person\"),__.has(\"name\",\"Cole\"))).has(\"age\",P.gt((int) 20).and(P.lt((int) 30)))", GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    private GraphTraversal anonymousTraversal() {
        return __.inject("x");
    }

}
