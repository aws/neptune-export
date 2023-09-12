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
