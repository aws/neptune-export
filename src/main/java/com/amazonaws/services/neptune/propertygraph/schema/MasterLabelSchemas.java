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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MasterLabelSchemas {

    private final Map<Label, MasterLabelSchema> masterLabelSchemas;
    private final GraphElementType graphElementType;

    public MasterLabelSchemas(Map<Label, MasterLabelSchema> masterLabelSchemas, GraphElementType graphElementType) {
        this.masterLabelSchemas = masterLabelSchemas;
        this.graphElementType = graphElementType;
    }

    public MasterLabelSchemas(Collection<FileSpecificLabelSchemas> fileSpecificLabelSchemasCollection, GraphElementType graphElementType) {
        this(convertFileSpecificLabelSchemas(fileSpecificLabelSchemasCollection), graphElementType);
    }

    public Collection<MasterLabelSchema> schemas() {
        return masterLabelSchemas.values();
    }

    public GraphElementType graphElementType() {
        return graphElementType;
    }

    public GraphElementSchemas toGraphElementSchemas() {
        GraphElementSchemas graphElementSchemas = new GraphElementSchemas();
        for (MasterLabelSchema masterLabelSchema : masterLabelSchemas.values()) {
            graphElementSchemas.addLabelSchema(masterLabelSchema.labelSchema(), masterLabelSchema.outputIds());
        }
        return graphElementSchemas;
    }

    private static Map<Label, MasterLabelSchema> convertFileSpecificLabelSchemas(Collection<FileSpecificLabelSchemas> fileSpecificLabelSchemasCollection) {
        Set<Label> labels = new HashSet<>();

        fileSpecificLabelSchemasCollection.forEach(s -> labels.addAll(s.labels()));

        Map<Label, MasterLabelSchema> masterLabelSchemas = new HashMap<>();

        for (Label label : labels) {

            LabelSchema masterLabelSchema = new LabelSchema(label);
            Collection<FileSpecificLabelSchema> fileSpecificLabelSchemas = new ArrayList<>();

            for (FileSpecificLabelSchemas fileSpecificLabelSchemasForTask : fileSpecificLabelSchemasCollection) {
                if (fileSpecificLabelSchemasForTask.hasSchemasForLabel(label)) {
                    Set<LabelSchema> labelSchemaSet = new HashSet<>();
                    for (FileSpecificLabelSchema fileSpecificLabelSchema :
                            fileSpecificLabelSchemasForTask.fileSpecificLabelSchemasFor(label)) {
                        fileSpecificLabelSchemas.add(fileSpecificLabelSchema);
                        labelSchemaSet.add(fileSpecificLabelSchema.labelSchema());
                    }
                    for (LabelSchema labelSchema : labelSchemaSet) {
                        masterLabelSchema = masterLabelSchema.union(labelSchema);
                    }
                }
            }

            masterLabelSchemas.put(
                    label,
                    new MasterLabelSchema(masterLabelSchema, fileSpecificLabelSchemas));
        }
        return masterLabelSchemas;
    }
}
