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

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.jsr223.CachedGremlinScriptEngineManager;
import org.apache.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

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

    public GremlinFilters(String gremlinFilter, String gremlinNodeFilter, String gremlinEdgeFilter, boolean filterEdgesEarly) {
        this.filterEdgesEarly = filterEdgesEarly;

        CachedGremlinScriptEngineManager scriptEngineManager = new CachedGremlinScriptEngineManager();
        GremlinScriptEngine engine = scriptEngineManager.getEngineByName("gremlin-groovy");
        Bindings engineBindings = engine.createBindings();
        engineBindings.put("datetime", new DatetimeConverter());

        try {
            this.gremlinFilter = StringUtils.isNotEmpty(gremlinFilter) ?
                    (Traversal.Admin) engine.eval(gremlinFilter, engineBindings) : null;
        } catch (ScriptException e) {
            throw new IllegalStateException(String.format("Invalid Gremlin filter: %s. %s", gremlinFilter, e.getMessage()), e);
        }

        try {
            this.gremlinNodeFilter = StringUtils.isNotEmpty(gremlinNodeFilter) ?
                    (Traversal.Admin) engine.eval(gremlinNodeFilter, engineBindings) : null;
        } catch (ScriptException e) {
            throw new IllegalStateException(String.format("Invalid Gremlin node filter: %s. %s", gremlinNodeFilter, e.getMessage()), e);
        }

        try {
            this.gremlinEdgeFilter = StringUtils.isNotEmpty(gremlinEdgeFilter) ?
                    (Traversal.Admin) engine.eval(gremlinEdgeFilter, engineBindings) : null;
        } catch (ScriptException e) {
            throw new IllegalStateException(String.format("Invalid Gremlin edge filter: %s. %s", gremlinEdgeFilter, e.getMessage()), e);
        }
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
        for (Bytecode.Instruction instruction : gremlin.getBytecode().getInstructions()) {
            String operator = instruction.getOperator();
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

    private static class DatetimeConverter {
        private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeParser().withZoneUTC();

        public Object call(String args) {
            return dateTimeFormatter.parseDateTime(args).toDate();
        }
    }
}
