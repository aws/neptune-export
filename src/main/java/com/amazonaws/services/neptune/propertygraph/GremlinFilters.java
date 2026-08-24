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

package com.amazonaws.services.neptune.propertygraph;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.jsr223.CachedGremlinScriptEngineManager;
import org.apache.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;

public class GremlinFilters {

    public static final GremlinFilters EMPTY = new GremlinFilters(null, null, null, false);

    private final Traversal.Admin<?, ?> gremlinFilter;
    private final Traversal.Admin<?, ?> gremlinNodeFilter;
    private final Traversal.Admin<?, ?> gremlinEdgeFilter;
    private final boolean filterEdgesEarly;

    private static final List<String> INVALID_OPERATORS = Arrays.asList("addV", "addE", "write", "drop", "sideEffect", "property", "mergeV", "mergeE");

    /*
     * Filters are supplied by the user as bare anonymous fragments - e.g. has("runways", 2) - but the gremlin-lang
     * grammar only accepts a complete query rooted at 'g'. We therefore prepend a synthetic root before parsing,
     * and discard the leading V() instruction in apply() so that the remaining instructions can still be spliced
     * into a destination traversal at an arbitrary mid-chain position. Do not remove this prefix: without it the
     * grammar rejects every existing filter with "mismatched input ... expecting {EmptyStringLiteral, 'g'}".
     */
    private static final String ROOT_PREFIX = "g.V().";

    public GremlinFilters(String gremlinFilter, String gremlinNodeFilter, String gremlinEdgeFilter, boolean filterEdgesEarly) {
        this.filterEdgesEarly = filterEdgesEarly;

        CachedGremlinScriptEngineManager scriptEngineManager = new CachedGremlinScriptEngineManager();
        GremlinScriptEngine engine = scriptEngineManager.getEngineByName("gremlin-lang");
        Bindings engineBindings = engine.createBindings();

        // gremlin-lang requires a 'g' binding of type TraversalSource, otherwise evaluation fails with an NPE.
        // The traversal source below is inert: the parsed filters are never executed, cloned or attached to a graph -
        // they are used purely as bytecode carriers.
        engineBindings.put("g", AnonymousTraversalSource.traversal().withEmbedded(EmptyGraph.instance()));

        this.gremlinFilter = parseFilter(engine, engineBindings, gremlinFilter, "Gremlin filter");
        this.gremlinNodeFilter = parseFilter(engine, engineBindings, gremlinNodeFilter, "Gremlin node filter");
        this.gremlinEdgeFilter = parseFilter(engine, engineBindings, gremlinEdgeFilter, "Gremlin edge filter");
    }

    private static Traversal.Admin<?, ?> parseFilter(GremlinScriptEngine engine,
                                                     Bindings bindings,
                                                     String filter,
                                                     String description) {
        if (StringUtils.isEmpty(filter)) {
            return null;
        }

        /*
         * The gremlin-lang grammar accepts a multi-statement script and returns only the result of the LAST
         * statement. A filter containing a newline or a semicolon followed by its own g-rooted statement would
         * therefore silently discard ROOT_PREFIX along with the rest of the user's fragment, yielding something
         * that is not an anonymous traversal at all (e.g. a bare TraversalSource) and bypassing the operator
         * denylist in apply(). A filter is only ever a single anonymous traversal fragment, so statement
         * separators are never legitimate and are rejected up front.
         */
        if (StringUtils.containsAny(filter, '\n', '\r', ';')) {
            throw new IllegalStateException(String.format("Invalid %s: %s. %s", description, filter,
                    "A Gremlin filter must be a single Gremlin traversal fragment, and must not contain multiple " +
                            "statements. Remove any newline or semicolon characters."));
        }

        Object result;

        try {
            result = engine.eval(ROOT_PREFIX + filter, bindings);
        } catch (ScriptException e) {
            throw new IllegalStateException(String.format("Invalid %s: %s. %s", description, filter, e.getMessage()), e);
        }

        // Anything other than a traversal cannot be spliced into a destination traversal. Check rather than cast
        // blindly, so that a raw ClassCastException can never escape in place of the documented message.
        if (!(result instanceof Traversal.Admin)) {
            throw new IllegalStateException(String.format("Invalid %s: %s. %s", description, filter,
                    String.format("A Gremlin filter must be a single Gremlin traversal fragment, but this filter " +
                            "evaluated to %s.", result == null ? "null" : result.getClass().getName())));
        }

        return (Traversal.Admin<?, ?>) result;
    }

    public GraphTraversal<? extends Element, ?> applyToNodes(GraphTraversal<? extends Element, ?> t) {
        if (gremlinNodeFilter != null) {
            return apply(t, gremlinNodeFilter);
        } else if (gremlinFilter != null) {
            return apply(t, gremlinFilter);
        } else {
            return t;
        }
    }

    public GraphTraversal<? extends Element, ?> applyToEdges(GraphTraversal<? extends Element, ?> t) {
        if (gremlinEdgeFilter != null) {
            return apply(t, gremlinEdgeFilter);
        } else if (gremlinFilter != null) {
            return apply(t, gremlinFilter);
        } else {
            return t;
        }
    }

    public boolean filterEdgesEarly() {
        return filterEdgesEarly;
    }

    private GraphTraversal<? extends Element, ?> apply(GraphTraversal<? extends Element, ?> t, Traversal.Admin<?, ?> gremlin) {
        boolean isFirst = true;

        for (Bytecode.Instruction instruction : gremlin.getBytecode().getInstructions()) {
            String operator = instruction.getOperator();

            if (isFirst) {
                isFirst = false;
                // Discard the synthetic root added by ROOT_PREFIX so the fragment can be spliced mid-chain.
                if (GraphTraversal.Symbols.V.equals(operator) || GraphTraversal.Symbols.E.equals(operator)) {
                    continue;
                }
            }

            validateOperator(operator);
            t.asAdmin().getBytecode().addStep(operator, instruction.getArguments());
        }

        return t;
    }

    private void validateOperator(String operator) {
        if (INVALID_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException(String.format("Invalid operator: '%s'. Gremlin filter cannot contain side effect or mutating step.", operator));
        }
    }
}
