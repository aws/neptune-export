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

package com.amazonaws.services.neptune.propertygraph.schema;

import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.io.PropertyGraphExportFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FileSpecificLabelSchemas {

    private final Map<Label, Collection<FileSpecificLabelSchema>> fileSpecificLabelSchemas = new HashMap<>();

    public void add(String outputId, PropertyGraphExportFormat format, LabelSchema labelSchema) {

        if (!fileSpecificLabelSchemas.containsKey(labelSchema.label())) {
            fileSpecificLabelSchemas.put(labelSchema.label(), new ArrayList<>());
        }

        Collection<FileSpecificLabelSchema> schemas = fileSpecificLabelSchemas.get(labelSchema.label());

        for (FileSpecificLabelSchema schema : schemas) {
            if (schema.outputId().equals(outputId)){
                return;
            }
        }

        schemas.add(new FileSpecificLabelSchema(outputId, format, labelSchema));
    }

    public Collection<Label> labels() {
        return fileSpecificLabelSchemas.keySet();
    }

    public boolean hasSchemasForLabel(Label label){
        return fileSpecificLabelSchemas.containsKey(label);
    }

    public Collection<FileSpecificLabelSchema> fileSpecificLabelSchemasFor(Label label){
        return fileSpecificLabelSchemas.get(label);
    }
}
