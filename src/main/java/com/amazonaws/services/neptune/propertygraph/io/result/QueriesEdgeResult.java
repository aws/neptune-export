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

package com.amazonaws.services.neptune.propertygraph.io.result;

import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.T;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueriesEdgeResult implements PGResult {
    private final Map<?, ?> edgeMap;

    private final Map<?, ?> properties;

    public QueriesEdgeResult(Map<?, ?> input) {
        edgeMap = input;
        properties = new HashMap<>(input);
        properties.remove(T.label);
        properties.remove(T.id);
        properties.remove(Direction.OUT);
        properties.remove(Direction.IN);
    }

    @Override
    public GraphElementType getGraphElementType() {
        return GraphElementType.edges;
    }

    public List<String> getLabel() {
        return Collections.singletonList(String.valueOf(edgeMap.get(T.label)));
    }

    @Override
    public String getId() {
        return String.valueOf(edgeMap.get(T.id));
    }

    @Override
    public Map<String, Object> getProperties() {
        return (Map<String, Object>) properties;
    }

    @Override
    public String getFrom() {
        return String.valueOf(((Map<String, Object>)edgeMap.get(Direction.OUT)).get(T.id));
    }

    @Override
    public String getTo() {
        return String.valueOf(((Map<String, Object>)edgeMap.get(Direction.IN)).get(T.id));
    }

    @Override
    public List<String> getFromLabels() {
        return Collections.singletonList(String.valueOf(((Map<String, Object>)edgeMap.get(Direction.OUT)).get(T.label)));
    }

    @Override
    public List<String> getToLabels() {
        return Collections.singletonList(String.valueOf(((Map<String, Object>)edgeMap.get(Direction.IN)).get(T.label)));
    }

}
