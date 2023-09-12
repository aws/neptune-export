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
