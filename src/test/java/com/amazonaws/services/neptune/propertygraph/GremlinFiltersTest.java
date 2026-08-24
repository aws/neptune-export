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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    // ---------------------------------------------------------------------------------------------------------------
    // gremlin-lang parsing layer regression tests
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldRejectDropStep() {
        // The gremlin-lang grammar happily parses mutating steps, so the INVALID_OPERATORS denylist is the only thing
        // that stops them - and it fires at apply time, not at construction time.
        GremlinFilters filters = new GremlinFilters("drop()", null, null, false);

        assertTrue("Exception does not name the rejected operator", assertThrows(IllegalArgumentException.class,
                () -> filters.applyToNodes(anonymousTraversal())
        ).getMessage().contains("Invalid operator: 'drop'"));

        assertTrue("Exception does not name the rejected operator", assertThrows(IllegalArgumentException.class,
                () -> filters.applyToEdges(anonymousTraversal())
        ).getMessage().contains("Invalid operator: 'drop'"));
    }

    @Test
    public void shouldRejectAddVStep() {
        GremlinFilters filters = new GremlinFilters("addV('x')", null, null, false);

        assertTrue("Exception does not name the rejected operator", assertThrows(IllegalArgumentException.class,
                () -> filters.applyToNodes(anonymousTraversal())
        ).getMessage().contains("Invalid operator: 'addV'"));
    }

    @Test
    public void shouldRejectPropertyStep() {
        GremlinFilters filters = new GremlinFilters("property('k','v')", null, null, false);

        assertTrue("Exception does not name the rejected operator", assertThrows(IllegalArgumentException.class,
                () -> filters.applyToNodes(anonymousTraversal())
        ).getMessage().contains("Invalid operator: 'property'"));
    }

    @Test
    public void shouldRejectMutatingStepInNodeFilter() {
        GremlinFilters filters = new GremlinFilters(null, "drop()", null, false);

        assertTrue("Exception does not name the rejected operator", assertThrows(IllegalArgumentException.class,
                () -> filters.applyToNodes(anonymousTraversal())
        ).getMessage().contains("Invalid operator: 'drop'"));
    }

    @Test
    public void shouldRejectMutatingStepInEdgeFilter() {
        GremlinFilters filters = new GremlinFilters(null, null, "addE('x')", false);

        assertTrue("Exception does not name the rejected operator", assertThrows(IllegalArgumentException.class,
                () -> filters.applyToEdges(anonymousTraversal())
        ).getMessage().contains("Invalid operator: 'addE'"));
    }

    @Test
    public void shouldStripSyntheticRootFromFilter() {
        // The filter is parsed as 'g.V().hasLabel("route")' internally; the synthetic V() root must not survive
        // into the destination traversal, which is spliced at an arbitrary mid-chain position.
        GremlinFilters filters = new GremlinFilters("hasLabel(\"route\")", null, null, false);

        String nodes = GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal()));
        String edges = GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal()));

        assertEquals("__.inject(\"x\").hasLabel(\"route\")", nodes);
        assertEquals("__.inject(\"x\").hasLabel(\"route\")", edges);

        assertFalse("Synthetic V() root leaked into the traversal", nodes.contains(".V("));
        assertFalse("Synthetic E() root leaked into the traversal", nodes.contains(".E("));
        assertFalse("Synthetic V() root leaked into the traversal", edges.contains(".V("));
        assertFalse("Synthetic E() root leaked into the traversal", edges.contains(".E("));
    }

    @Test
    public void shouldFailFastOnLambdaSyntaxUnsupportedByNeptune() {
        // This is an earlier, clearer failure rather than a lost capability. Neptune does not support lambdas in
        // bytecode requests, so a closure-bearing filter parsed fine at CLI time under the Groovy script engine and
        // then failed server-side, mid-export. gremlin-lang rejects it up front instead.
        assertTrue("Exception does not contain expected message", assertThrows(IllegalStateException.class,
                () -> new GremlinFilters("has('x', 1).map{ it.get() }", null, null, false)
        ).getMessage().contains("Invalid Gremlin filter: has('x', 1).map{ it.get() }"));
    }

    @Test
    public void shouldSpliceMultiStepFragmentsInOrder() {
        GremlinFilters filters = new GremlinFilters("where(__.has('name','Cole')).hasLabel('person').has('age',31)", null, null, false);

        assertEquals("__.inject(\"x\").where(__.has(\"name\",\"Cole\")).hasLabel(\"person\").has(\"age\",(int) 31)",
                GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").where(__.has(\"name\",\"Cole\")).hasLabel(\"person\").has(\"age\",(int) 31)",
                GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    // The multi-statement guard emits this distinctive tail. It is unique to the guard: neither the ScriptException
    // catch (which appends parser text) nor the instanceof-failure check (which says "...evaluated to <class>.")
    // produce this sentence, so asserting on it actually pins the guard rather than falling through to another site.
    private static final String MULTI_STATEMENT_GUARD_TAIL = "Remove any newline or semicolon characters.";

    @Test
    public void shouldPreserveUserAuthoredLeadingVStep() {
        // A user filter whose first step is itself named V() parses internally as g.V().V().hasLabel('x'). apply()
        // strips ONLY the first (synthetic) V instruction, so the user's own V() must survive. This pins that
        // exactly one root is discarded, not every V/E step in the fragment.
        GremlinFilters filters = new GremlinFilters("V().hasLabel('x')", null, null, false);

        assertEquals("__.inject(\"x\").V().hasLabel(\"x\")",
                GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
        assertEquals("__.inject(\"x\").V().hasLabel(\"x\")",
                GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
    }

    @Test
    public void shouldRejectMultiStatementFilterWithNewline() {
        // gremlin-lang evaluates only the LAST statement of a multi-statement script, which would silently discard
        // the internal synthetic root along with the user's fragment.
        String message = assertThrows(IllegalStateException.class,
                () -> new GremlinFilters("has('a',1)\ng", null, null, false)
        ).getMessage();
        assertTrue("Exception does not contain expected prefix", message.contains("Invalid Gremlin filter: "));
        assertTrue("Exception was not raised by the multi-statement guard", message.contains(MULTI_STATEMENT_GUARD_TAIL));
    }

    @Test
    public void shouldRejectMultiStatementFilterWithCarriageReturn() {
        // The guard also checks for '\r'; exercise it explicitly so a future edit that drops '\r' from the guard
        // is caught. Assert both the prefix and the guard's distinctive tail so it cannot pass by falling through
        // to another throw site.
        String message = assertThrows(IllegalStateException.class,
                () -> new GremlinFilters("has('a',1)\rg", null, null, false)
        ).getMessage();
        assertTrue("Exception does not contain expected prefix", message.contains("Invalid Gremlin filter: "));
        assertTrue("Exception was not raised by the multi-statement guard", message.contains(MULTI_STATEMENT_GUARD_TAIL));
    }

    @Test
    public void shouldRejectMultiStatementFilterWithSemicolon() {
        String message = assertThrows(IllegalStateException.class,
                () -> new GremlinFilters("has('a',1);g", null, null, false)
        ).getMessage();
        assertTrue("Exception does not contain expected prefix", message.contains("Invalid Gremlin filter: "));
        assertTrue("Exception was not raised by the multi-statement guard", message.contains(MULTI_STATEMENT_GUARD_TAIL));
    }

    @Test
    public void shouldRejectMultiStatementFilterSmugglingMutatingStep() {
        // The multi-statement guard rejects this input up front, at construction time, before apply() and its
        // operator denylist are ever reached. Asserting the guard's distinctive tail pins that specific throw site.
        String message = assertThrows(IllegalStateException.class,
                () -> new GremlinFilters("has('a',1)\ng.addV('x')", null, null, false)
        ).getMessage();
        assertTrue("Exception does not contain expected prefix", message.contains("Invalid Gremlin filter: "));
        assertTrue("Exception was not raised by the multi-statement guard", message.contains(MULTI_STATEMENT_GUARD_TAIL));
    }

    @Test
    public void shouldRejectMultiStatementNodeFilter() {
        String message = assertThrows(IllegalStateException.class,
                () -> new GremlinFilters(null, "has('a',1)\ng.addV('x')", null, false)
        ).getMessage();
        assertTrue("Exception does not contain expected prefix", message.contains("Invalid Gremlin node filter: "));
        assertTrue("Exception was not raised by the multi-statement guard", message.contains(MULTI_STATEMENT_GUARD_TAIL));
    }

    @Test
    public void shouldRejectMultiStatementEdgeFilter() {
        // The trailing statement here ('g') evaluates to a bare TraversalSource, so without the guard the
        // instanceof-failure check would also reject it - making a prefix-only assertion tautological. Pin the
        // guard by additionally asserting its distinctive tail.
        String message = assertThrows(IllegalStateException.class,
                () -> new GremlinFilters(null, null, "has('a',1);g", false)
        ).getMessage();
        assertTrue("Exception does not contain expected prefix", message.contains("Invalid Gremlin edge filter: "));
        assertTrue("Exception was not raised by the multi-statement guard", message.contains(MULTI_STATEMENT_GUARD_TAIL));
    }

    @Test
    public void shouldBeSafeToShareOneInstanceAcrossThreads() throws Exception {
        // ExportPropertyGraphJob shares a single GremlinFilters instance across a fixed thread pool, so
        // applyToNodes/applyToEdges run concurrently against the same parsed traversals. Access must stay read-only.
        String expected = "__.inject(\"x\").where(__.has(\"name\",\"Cole\")).hasLabel(\"person\")";

        GremlinFilters filters = new GremlinFilters("where(__.has('name','Cole')).hasLabel('person')", null, null, false);

        assertEquals(expected, GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));

        int threadCount = 8;
        int iterations = 200;

        AtomicInteger errors = new AtomicInteger();
        List<String> results = new CopyOnWriteArrayList<>();
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startGate.await();
                        for (int j = 0; j < iterations; j++) {
                            // Each call targets a distinct destination traversal, as it does during an export.
                            results.add(GremlinQueryDebugger.queryAsString(filters.applyToNodes(anonymousTraversal())));
                            results.add(GremlinQueryDebugger.queryAsString(filters.applyToEdges(anonymousTraversal())));
                        }
                    } catch (Throwable t) {
                        errors.incrementAndGet();
                    }
                });
            }

            startGate.countDown();
            executorService.shutdown();
            assertTrue("Concurrent filter application did not complete in time",
                    executorService.awaitTermination(60, TimeUnit.SECONDS));
        } finally {
            executorService.shutdownNow();
        }

        assertEquals("Concurrent filter application threw", 0, errors.get());
        assertEquals("Unexpected number of results", threadCount * iterations * 2, results.size());

        for (String result : results) {
            assertEquals(expected, result);
        }
    }

    private GraphTraversal anonymousTraversal() {
        return __.inject("x");
    }

}
