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

package com.amazonaws.services.neptune.propertygraph.io;

import com.amazonaws.services.neptune.cluster.ConcurrencyConfig;
import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.io.Directories;
import com.amazonaws.services.neptune.io.DirectoryStructure;
import com.amazonaws.services.neptune.io.Target;
import com.amazonaws.services.neptune.propertygraph.Label;
import com.amazonaws.services.neptune.propertygraph.schema.DataType;
import com.amazonaws.services.neptune.propertygraph.schema.FileSpecificLabelSchema;
import com.amazonaws.services.neptune.propertygraph.schema.GraphElementType;
import com.amazonaws.services.neptune.propertygraph.schema.LabelSchema;
import com.amazonaws.services.neptune.propertygraph.schema.MasterLabelSchema;
import com.amazonaws.services.neptune.propertygraph.schema.MasterLabelSchemas;
import com.amazonaws.services.neptune.propertygraph.schema.PropertySchema;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * End-to-end coverage for {@link RewriteAndMergeCsv} (the rewrite-and-consolidate pass that merges
 * several per-file CSVs for a label into one consolidated file). Drives the real
 * {@link RewriteAndMergeCsv#execute} against real files on disk and asserts that, across the merge, a
 * present empty-string property survives as a quoted empty string ({@code ""}) while an absent
 * property remains a blank field.
 */
public class RewriteAndMergeCsvTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void preservesEmptyStringVersusAbsentPropertyAcrossMergedFiles() throws Exception {
        LabelSchema labelSchema = personSchema();

        // Two header-less source files for the same label, merged into one consolidated file:
        //   file 1 -> bob: nickname is a present empty string ("")
        //   file 2 -> carol: nickname is absent (blank field)
        List<String> output = runRewriteAndMerge(labelSchema, Arrays.asList(
                Collections.singletonList("\"bob\",\"person\",\"Bob\",\"\""),
                Collections.singletonList("\"carol\",\"person\",\"Carol\",")));

        assertEquals(Arrays.asList(
                "\"bob\",\"person\",\"Bob\",\"\"",
                "\"carol\",\"person\",\"Carol\","), output);
    }

    private LabelSchema personSchema() {
        LabelSchema labelSchema = new LabelSchema(new Label("person"));
        labelSchema.put("name", new PropertySchema("name", false, DataType.String, false, EnumSet.noneOf(DataType.class)));
        labelSchema.put("nickname", new PropertySchema("nickname", false, DataType.String, false, EnumSet.noneOf(DataType.class)));
        return labelSchema;
    }

    // Drives the real RewriteAndMergeCsv.execute against multiple header-less node CSVs on disk and
    // returns the data lines of the single consolidated output file.
    private List<String> runRewriteAndMerge(LabelSchema labelSchema, List<List<String>> perFileRows) throws Exception {
        File root = tmp.newFolder();
        Directories directories = Directories.createFor(DirectoryStructure.PropertyGraph, root, "export-id", "", "");

        List<FileSpecificLabelSchema> fileSchemas = new ArrayList<>();
        int index = 0;
        for (List<String> rows : perFileRows) {
            File sourceFile = new File(tmp.newFolder(), String.format("nodes-source-%d.csv", index++));
            try (Writer writer = new FileWriter(sourceFile)) {
                for (String row : rows) {
                    writer.write(row);
                    writer.write("\n");
                }
            }
            fileSchemas.add(new FileSpecificLabelSchema(
                    sourceFile.getAbsolutePath(), PropertyGraphExportFormat.csv, labelSchema));
        }

        PropertyGraphTargetConfig targetConfig = new PropertyGraphTargetConfig(
                directories,
                null,
                new PrinterOptions(CsvPrinterOptions.builder().build()),
                PropertyGraphExportFormat.csv,
                Target.files,
                true,
                false,
                true);

        MasterLabelSchema masterLabelSchema = new MasterLabelSchema(labelSchema, fileSchemas);
        Map<Label, MasterLabelSchema> schemas = new HashMap<>();
        schemas.put(labelSchema.label(), masterLabelSchema);
        MasterLabelSchemas input = new MasterLabelSchemas(schemas, GraphElementType.nodes);

        RewriteAndMergeCsv command = new RewriteAndMergeCsv(
                targetConfig, new ConcurrencyConfig(1), new FeatureToggles(Collections.emptyList()));

        MasterLabelSchemas result = command.execute(input);

        List<String> outputIds = result.schemas().stream()
                .flatMap(s -> s.fileSpecificLabelSchemas().stream())
                .map(FileSpecificLabelSchema::outputId)
                .collect(Collectors.toList());

        assertEquals("expected a single consolidated output file", 1, outputIds.size());

        return dataLines(outputIds.get(0));
    }

    // Returns the data rows of the consolidated file. Data rows always start with a quoted id (");
    // any header row (written by the writer factory) starts with the token prefix (~) and is excluded.
    private List<String> dataLines(String path) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(new File(path).toPath(), StandardCharsets.UTF_8)) {
            String trimmed = line.replace("\r", "");
            if (trimmed.startsWith("\"")) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}
