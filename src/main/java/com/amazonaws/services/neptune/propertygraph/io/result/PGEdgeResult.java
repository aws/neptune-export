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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PGEdgeResult implements PGResult{
    private final Map<String, Object> edgeMap;

    public PGEdgeResult(Map<String, Object> input) {
        edgeMap = input;
    }

    @Override
    public GraphElementType getGraphElementType() {
        return GraphElementType.nodes;
    }

    @Override
    public List<String> getLabel() {
        List<String> labels = new ArrayList<>();
        labels.add(String.valueOf(edgeMap.get("~label")));
        return labels;
    }

    @Override
    public String getId() {
        return String.valueOf(edgeMap.get("~id"));
    }

    @Override
    public Map<String, Object> getProperties() {
        return (Map<String, Object>) edgeMap.get("properties");
    }

    @Override
    public String getFrom() {
        return String.valueOf(edgeMap.get("~from"));
    }

    @Override
    public String getTo() {
        return String.valueOf(edgeMap.get("~to"));
    }

    @Override
    public List<String> getFromLabels() {
        return (List<String>) edgeMap.get("~fromLabels");
    }

    @Override
    public List<String> getToLabels() {
        return (List<String>) edgeMap.get("~toLabels");
    }
}
