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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.*;

public class NamedQueries {

    public static NamedQueries fromJson(JsonNode json) {
        String name = json.path("name").textValue();

        if (json.has("query")){
            String query = json.path("query").textValue();
            return new NamedQueries(name, Collections.singletonList(query));
        } else {

            ArrayNode queries = (ArrayNode) json.path("queries");

            List<String> collection = new ArrayList<>();

            for (JsonNode query : queries) {
                collection.add(query.textValue());
            }

            return new NamedQueries(name, collection);
        }
    }

    private final String name;
    private final Collection<String> queries;

    public NamedQueries(String name, Collection<String> queries) {
        this.name = name;
        this.queries = Collections.synchronizedCollection(queries);
    }

    public String name() {
        return name;
    }

    public Collection<String> queries() {
        return queries;
    }

    public void addTo(Collection<NamedQuery> namedQueries) {
        for (String query : queries) {
            namedQueries.add(new NamedQuery(name, query));
        }
    }

    public ArrayNode toJson() {
        ArrayNode json = JsonNodeFactory.instance.arrayNode();

        for (String query : queries) {
            json.add(query);
        }

        return json;
    }

    /**
     * Splits each query into n smaller queries.
     */
    public void split(LazyQueriesRangeFactoryProvider rangeFactoryProvider) {
        Collection<String> splitQueries = Collections.synchronizedCollection(new ArrayList<>());
        queries.forEach(q -> {
            RangeFactory rangeFactory = null;
            if (q.startsWith("g.V()")) {
                rangeFactory = rangeFactoryProvider.getNodesRangeFactory();
            } else if (q.startsWith("g.E()")) {
                rangeFactory = rangeFactoryProvider.getEdgesRangeFactory();
            }

            if (rangeFactory != null) {
                while (!rangeFactory.isExhausted()) {
                    Range range = rangeFactory.nextRange();
                    if (range.isAll()) {
                        // Keep unaltered query if range is all. This works-around an issue where range(0,-1) does not
                        // produce any results in some versions of Neptune.
                        splitQueries.add(q);
                        break;
                    }
                    if (q.startsWith("g.V()")) {
                        splitQueries.add(q.replaceFirst("g.V\\(\\)", "g.V()."+range.toString()));
                    } else if (q.startsWith("g.E()")) {
                        splitQueries.add(q.replaceFirst("g.E\\(\\)", "g.E()."+range.toString()));
                    }
                }
                rangeFactory.reset();
            }
        });
        queries.clear();
        queries.addAll(splitQueries);
    }
}
