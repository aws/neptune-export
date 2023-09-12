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

import com.amazonaws.services.neptune.io.Status;
import com.amazonaws.services.neptune.propertygraph.*;
import com.amazonaws.services.neptune.propertygraph.io.result.PGResult;
import com.amazonaws.services.neptune.propertygraph.schema.FileSpecificLabelSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementSchemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class ExportPropertyGraphTask implements Callable<FileSpecificLabelSchemas> {

    private static final Logger logger = LoggerFactory.getLogger(ExportPropertyGraphTask.class);

    private final GraphElementSchemas graphElementSchemas;
    private final LabelsFilter labelsFilter;
    private final GraphClient<? extends PGResult> graphClient;
    private final WriterFactory<? extends PGResult> writerFactory;
    private final PropertyGraphTargetConfig targetConfig;
    private final RangeFactory rangeFactory;
    private final GremlinFilters gremlinFilters;
    private final Status status;
    private final AtomicInteger index;
    private final LabelWriters<PGResult> labelWriters;

    public ExportPropertyGraphTask(GraphElementSchemas graphElementSchemas,
                                   LabelsFilter labelsFilter,
                                   GraphClient<? extends PGResult> graphClient,
                                   WriterFactory<? extends PGResult> writerFactory,
                                   PropertyGraphTargetConfig targetConfig,
                                   RangeFactory rangeFactory,
                                   GremlinFilters gremlinFilters,
                                   Status status,
                                   AtomicInteger index,
                                   AtomicInteger fileDescriptorCount,
                                   int maxFileDescriptorCount) {
        this.graphElementSchemas = graphElementSchemas;
        this.labelsFilter = labelsFilter;
        this.graphClient = graphClient;
        this.writerFactory = writerFactory;
        this.targetConfig = targetConfig;
        this.rangeFactory = rangeFactory;
        this.gremlinFilters = gremlinFilters;
        this.status = status;
        this.index = index;
        this.labelWriters = new LabelWriters<>(fileDescriptorCount, maxFileDescriptorCount);
    }

    @Override
    public FileSpecificLabelSchemas call() {

        FileSpecificLabelSchemas fileSpecificLabelSchemas = new FileSpecificLabelSchemas();

        CountingHandler handler = new CountingHandler(
                new ExportPGTaskHandler(
                        fileSpecificLabelSchemas,
                        graphElementSchemas,
                        targetConfig,
                        writerFactory,
                        labelWriters,
                        graphClient,
                        status,
                        index,
                        labelsFilter
                ));

        try {
            while (status.allowContinue()) {
                Range range = rangeFactory.nextRange();
                if (range.isEmpty()) {
                    status.halt();
                } else {
                    graphClient.queryForValues(handler, range, labelsFilter, gremlinFilters, graphElementSchemas);
                    if (range.sizeExceeds(handler.numberProcessed()) || rangeFactory.isExhausted()) {
                        status.halt();
                    }
                }
            }
        } finally {
            try {
                handler.close();
            } catch (Exception e) {
                logger.error("Error while closing handler", e);
            }
        }

        return fileSpecificLabelSchemas;
    }

}
