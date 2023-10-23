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
