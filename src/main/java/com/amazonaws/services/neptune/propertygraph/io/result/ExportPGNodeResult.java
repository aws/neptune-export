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

import java.util.List;
import java.util.Map;

public class ExportPGNodeResult implements PGResult {
    private final Map<String, Object> nodeMap;

    public ExportPGNodeResult(Map<String, Object> input) {
        nodeMap = input;
    }

    @Override
    public GraphElementType getGraphElementType() {
        return GraphElementType.nodes;
    }

    @Override
    public List<String> getLabel() {
        return (List<String>) nodeMap.get("~label");
    }

    @Override
    public String getId() {
        return String.valueOf(nodeMap.get("~id"));
    }

    @Override
    public Map<String, Object> getProperties() {
        return (Map<String, Object>) nodeMap.get("properties");
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
