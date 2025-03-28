/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.cli;

import com.amazonaws.services.neptune.io.*;
import com.amazonaws.services.neptune.propertygraph.io.PrinterOptions;
import com.amazonaws.services.neptune.propertygraph.io.PropertyGraphExportFormat;
import com.amazonaws.services.neptune.propertygraph.io.PropertyGraphTargetConfig;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.*;

public class PropertyGraphTargetModule extends AbstractTargetModule {

    @Option(name = {"--format"}, description = "Output format (optional, default 'csv').")
    @Once
    @AllowedEnumValues(PropertyGraphExportFormat.class)
    private PropertyGraphExportFormat format = PropertyGraphExportFormat.csv;

    @Option(name = {"--merge-files"}, description = "Merge files for each vertex or edge label (currently only supports CSV files for export-pg).")
    @Once
    private boolean mergeFiles = false;

    @Option(name = {"--per-label-directories"}, description = "Create a subdirectory for each distinct vertex or edge label.")
    @Once
    private boolean perLabelDirectories = false;

    public PropertyGraphTargetModule() {
    }

    public PropertyGraphTargetModule(Target target) {
        super(target);
    }

    public PropertyGraphTargetConfig config(Directories directories, PrinterOptions printerOptions){

        if (mergeFiles && (format != PropertyGraphExportFormat.csv && format != PropertyGraphExportFormat.csvNoHeaders)){
            throw new IllegalArgumentException("Merge files is only supported for CSV formats for export-pg");
        }

        KinesisConfig kinesisConfig = new KinesisConfig(this);

        return new PropertyGraphTargetConfig(directories, kinesisConfig, printerOptions, format, getOutput(), mergeFiles, perLabelDirectories, true);
    }

    public String description(){
        return format.description();
    }

    @Override
    protected DirectoryStructure directoryStructure(){
        if (format == PropertyGraphExportFormat.neptuneStreamsSimpleJson){
            return DirectoryStructure.SimpleStreamsOutput;
        } else {
            return DirectoryStructure.PropertyGraph;
        }
    }
}
