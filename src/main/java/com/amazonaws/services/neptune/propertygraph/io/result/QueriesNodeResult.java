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
        List<String> labels = new ArrayList<>(1);
        labels.add(String.valueOf(nodeMap.get(T.label)));
        return labels;
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
