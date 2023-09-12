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
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class NodeWriter implements LabelWriter<PGResult> {

    private final PropertyGraphPrinter propertyGraphPrinter;

    public NodeWriter(PropertyGraphPrinter propertyGraphPrinter) {
        this.propertyGraphPrinter = propertyGraphPrinter;
    }

    @Override
    public void handle(PGResult node, boolean allowTokens) throws IOException {

        Map<?, Object> properties = node.getProperties();
        String id = String.valueOf(node.getId());
        List<String> labels = node.getLabel();

        labels = Label.fixLabelsIssue(labels);

        propertyGraphPrinter.printStartRow();
        propertyGraphPrinter.printNode(id, labels);
        propertyGraphPrinter.printProperties(id, "vp", properties);
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
