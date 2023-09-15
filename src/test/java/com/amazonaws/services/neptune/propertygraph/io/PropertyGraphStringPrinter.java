package com.amazonaws.services.neptune.propertygraph.io;

import com.amazonaws.services.neptune.propertygraph.schema.PropertySchema;
import com.amazonaws.services.neptune.util.NotImplementedException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class PropertyGraphStringPrinter implements PropertyGraphPrinter {

    StringBuilder output = new StringBuilder();

    public String getOutput() {
        return output.toString();
    }

    @Override
    public String outputId() {
        return null;
    }

    @Override
    public void printHeaderMandatoryColumns(String... columns) {
        throw new NotImplementedException();
    }

    @Override
    public void printHeaderRemainingColumns(Collection<PropertySchema> remainingColumns) {
        throw new NotImplementedException();
    }

    @Override
    public void printProperties(Map<?, ?> properties) throws IOException {
        output.append("Properties{");
        properties.forEach((key, value) -> {
            output.append(key.toString() + ":" + value.toString() + ", ");
        });
        output.append("} ");
    }

    @Override
    public void printProperties(Map<?, ?> properties, boolean applyFormatting) throws IOException {
        printProperties(properties);
    }

    @Override
    public void printProperties(String id, String streamOperation, Map<?, ?> properties) throws IOException {
        printProperties(properties);
    }

    @Override
    public void printEdge(String id, String label, String from, String to) throws IOException {
        output.append(String.format("Edge[%s, %s, %s, %s] ", id, label, from, to));
    }

    @Override
    public void printEdge(String id, String label, String from, String to, Collection<String> fromLabels, Collection<String> toLabels) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Edge[%s, %s, %s, %s, fromLabels{", id, label, from, to));
        for (String fromLabel : fromLabels) {
            builder.append(fromLabel).append(", ");
        }
        builder.append("}, toLabels{");
        for (String toLabel : toLabels) {
            builder.append(toLabel).append(", ");
        }
        builder.append("}] ");
        output.append(builder.toString());
    }

    @Override
    public void printNode(String id, List<String> labels) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Node[%s, Labels{", id));
        for (String label : labels) {
            builder.append(label).append(", ");
        }
        builder.append("}] ");
        output.append(builder.toString());
    }

    @Override
    public void printStartRow() throws IOException {
        output.append("Start Row\n");
    }

    @Override
    public void printEndRow() throws IOException {
        output.append("\n");
    }

    @Override
    public void close() throws Exception {

    }
}
