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

import com.amazonaws.services.neptune.io.Directories;
import com.amazonaws.services.neptune.io.Status;
import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.LabelsFilter;
import com.amazonaws.services.neptune.propertygraph.StatsContainer;
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;
import com.amazonaws.services.neptune.propertygraph.schema.FileSpecificLabelSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.LabelSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

class ExportPGTaskHandler<T extends PGResult> implements GraphElementHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExportPGTaskHandler.class);

    private final FileSpecificLabelSchemas fileSpecificLabelSchemas;
    private final GraphElementSchemas graphElementSchemas;
    private final PropertyGraphTargetConfig targetConfig;
    private final WriterFactory<T> writerFactory;
    private final LabelWriters<T> labelWriters;
    private final StatsContainer statsContainer;
    private final Status status;
    private final AtomicInteger index;

    private final LabelsFilter labelsFilter;

    ExportPGTaskHandler(FileSpecificLabelSchemas fileSpecificLabelSchemas,
                        GraphElementSchemas graphElementSchemas,
                        PropertyGraphTargetConfig targetConfig,
                        WriterFactory<T> writerFactory,
                        LabelWriters<T> labelWriters,
                        StatsContainer statsContainer,
                        Status status,
                        AtomicInteger index,
                        LabelsFilter labelsFilter) {
        this.fileSpecificLabelSchemas = fileSpecificLabelSchemas;
        this.graphElementSchemas = graphElementSchemas;
        this.targetConfig = targetConfig;
        this.writerFactory = writerFactory;
        this.labelWriters = labelWriters;
        this.statsContainer = statsContainer;
        this.status = status;
        this.index = index;
        this.labelsFilter = labelsFilter;
    }

    @Override
    public void handle(T input, boolean allowTokens) throws IOException {
        status.update();
        Label label = labelsFilter.getLabelFor(input);
        if (!labelWriters.containsKey(label)) {
            createWriterFor(label);
        }
        if(statsContainer != null) {
            statsContainer.updateStats(label);
        }
        labelWriters.get(label).handle(input, allowTokens);
    }

    @Override
    public void close() {
        try {
            labelWriters.close();
        } catch (Exception e) {
            logger.warn("Error closing label writer: {}.", e.getMessage());
        }
    }

    private void createWriterFor(Label label) {
        try {
            LabelSchema labelSchema = graphElementSchemas.getSchemaFor(label);

            PropertyGraphPrinter propertyGraphPrinter = writerFactory.createPrinter(
                    Directories.fileName(label.fullyQualifiedLabel(), index),
                    labelSchema,
                    targetConfig);
            LabelWriter<T> labelWriter = writerFactory.createLabelWriter(propertyGraphPrinter, labelSchema.label());

            labelWriters.put(label, labelWriter);
            fileSpecificLabelSchemas.add(labelWriter.outputId(), targetConfig.format(), labelSchema);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
