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
