package com.amazonaws.services.neptune.propertygraph.io.result;

import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;

import java.util.List;
import java.util.Map;

public interface PGResult {
    public GraphElementType getGraphElementType();
    public List<String> getLabel();
    public String getId();
    public Map<String, Object> getProperties();
    public String getFrom();
    public String getTo();
    public List<String> getFromLabels();
    public List<String> getToLabels();
}
