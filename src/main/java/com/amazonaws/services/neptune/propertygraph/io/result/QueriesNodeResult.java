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
import org.apache.tinkerpop.gremlin.structure.T;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class QueriesNodeResult implements PGResult {
    private final Map<?, ?> nodeMap;

    private final Map<?, ?> properties;

    public QueriesNodeResult(Map<?, ?> input) {
        nodeMap = input;
        properties = new HashMap<>(input);
        properties.remove(T.label);
        properties.remove(T.id);
    }

    @Override
    public GraphElementType getGraphElementType() {
        return GraphElementType.nodes;
    }

    public List<String> getLabel() {
        return Collections.singletonList(String.valueOf(nodeMap.get(T.label)));
    }

    @Override
    public String getId() {
        return String.valueOf(nodeMap.get(T.id));
    }

    @Override
    public Map<String, Object> getProperties() {
        return (Map<String, Object>) properties;
    }

    @Override
    public String getFrom() {
        throw new IllegalStateException("Illegal attempt to getFrom() from a Node Result");
    }

    @Override
    public String getTo() {
        throw new IllegalStateException("Illegal attempt to getTo() from a Node Result");
    }

    @Override
    public List<String> getFromLabels() {
        throw new IllegalStateException("Illegal attempt to getFromLabels() from a Node Result");
    }

    @Override
    public List<String> getToLabels() {
        throw new IllegalStateException("Illegal attempt to getToLabels() from a Node Result");
    }

}
