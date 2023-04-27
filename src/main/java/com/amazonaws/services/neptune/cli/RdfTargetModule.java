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
import com.amazonaws.services.neptune.rdf.io.RdfExportFormat;
import com.amazonaws.services.neptune.rdf.io.RdfTargetConfig;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.*;

public class RdfTargetModule extends AbstractTargetModule {

    @Option(name = {"--format"}, description = "Output format (optional, default 'turtle').")
    @Once
    @AllowedEnumValues(RdfExportFormat.class)
    private RdfExportFormat format = RdfExportFormat.turtle;

    public RdfTargetConfig config(Directories directories) {
        return new RdfTargetConfig(directories,
                new KinesisConfig(this),
                getOutput(), format);
    }

    @Override
    protected DirectoryStructure directoryStructure(){
        if (format == RdfExportFormat.neptuneStreamsSimpleJson){
            return DirectoryStructure.SimpleStreamsOutput;
        } else {
            return DirectoryStructure.Rdf;
        }
    }
}
