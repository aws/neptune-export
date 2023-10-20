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

import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.LabelsFilter;
import com.amazonaws.services.neptune.propertygraph.io.result.PGEdgeResult;
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;

import java.io.IOException;
import java.util.*;

public class EdgeWriter implements LabelWriter<PGResult> {

    private final PropertyGraphPrinter propertyGraphPrinter;
    private final boolean hasFromAndToLabels;

    public EdgeWriter(PropertyGraphPrinter propertyGraphPrinter, Label label) {
        this.propertyGraphPrinter = propertyGraphPrinter;
        this.hasFromAndToLabels = label.hasFromAndToLabels();
    }

    @Override
    public void handle(PGResult edge, boolean allowTokens) throws IOException {
        String from = edge.getFrom();
        String to = edge.getTo();
        Map<?, Object> properties = edge.getProperties();
        String id = edge.getId();
        String label = edge.getLabel().get(0);

        propertyGraphPrinter.printStartRow();

        if (hasFromAndToLabels){
            List<String> fromLabels = edge.getFromLabels();
            List<String> toLabels = edge.getToLabels();

            // Temp fix for concatenated label issue
            fromLabels = Label.fixLabelsIssue(fromLabels);
            toLabels = Label.fixLabelsIssue(toLabels);

            propertyGraphPrinter.printEdge(id, label, from, to, fromLabels, toLabels);
        } else {
            propertyGraphPrinter.printEdge(id, label, from, to);
        }

        propertyGraphPrinter.printProperties(id, "ep", properties);
        propertyGraphPrinter.printEndRow();
    }

    @Override
    public void close() throws Exception {
        propertyGraphPrinter.close();
    }

    @Override
    public String outputId() {
        return propertyGraphPrinter.outputId();
    }
}
