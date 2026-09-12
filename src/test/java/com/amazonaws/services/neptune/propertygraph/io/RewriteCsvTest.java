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
 * End-to-end coverage for {@link RewriteCsv} (the second, rewrite pass of a two-pass property-graph
 * export). Drives the real {@link RewriteCsv#execute} against real files on disk and asserts on the
 * rewritten output file, verifying that a present empty-string property survives as a quoted empty
 * string ({@code ""}) while an absent property remains a blank field.
 */
public class RewriteCsvTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void preservesEmptyStringVersusAbsentPropertyThroughRewrite() throws Exception {
        LabelSchema labelSchema = personSchema();

        // Header-less source rows as written by the first export pass:
        //   bob   -> nickname is a present empty string ("")
        //   carol -> nickname is absent (blank field)
        List<String> output = runRewrite(labelSchema, Arrays.asList(
                "\"bob\",\"person\",\"Bob\",\"\"",
                "\"carol\",\"person\",\"Carol\","));

        assertEquals(Arrays.asList(
                "\"bob\",\"person\",\"Bob\",\"\"",
                "\"carol\",\"person\",\"Carol\","), output);
    }

    @Test
    public void preservesMultiValueColumnValuesThroughRewrite() throws Exception {
        // The rewrite path re-emits each stored value via the scalar String formatter, so a
        // multi-value column round-trips as its opaque joined string (this does not exercise
        // DataType.formatList). Covers present-empty, absent, and populated multi-value values.
        LabelSchema labelSchema = new LabelSchema(new Label("person"));
        labelSchema.put("name", new PropertySchema("name", false, DataType.String, false, EnumSet.noneOf(DataType.class)));
        labelSchema.put("nickname", new PropertySchema("nickname", false, DataType.String, false, EnumSet.noneOf(DataType.class)));
        labelSchema.put("tags", new PropertySchema("tags", false, DataType.String, true, EnumSet.noneOf(DataType.class)));

        List<String> output = runRewrite(labelSchema, Arrays.asList(
                // present-empty scalar, populated multi-value list
                "\"bob\",\"person\",\"Bob\",\"\",\"x;y\"",
                // absent scalar, absent multi-value list
                "\"carol\",\"person\",\"Carol\",,",
                // populated scalar, present-empty multi-value list
                "\"dave\",\"person\",\"Dave\",\"Dizzy\",\"\""));

        assertEquals(Arrays.asList(
                "\"bob\",\"person\",\"Bob\",\"\",\"x;y\"",
                "\"carol\",\"person\",\"Carol\",,",
                "\"dave\",\"person\",\"Dave\",\"Dizzy\",\"\""), output);
    }

    private LabelSchema personSchema() {
        LabelSchema labelSchema = new LabelSchema(new Label("person"));
        labelSchema.put("name", new PropertySchema("name", false, DataType.String, false, EnumSet.noneOf(DataType.class)));
        labelSchema.put("nickname", new PropertySchema("nickname", false, DataType.String, false, EnumSet.noneOf(DataType.class)));
        return labelSchema;
    }

    // Drives the real RewriteCsv.execute against a header-less node CSV on disk and returns the data
    // lines of the rewritten output file.
    private List<String> runRewrite(LabelSchema labelSchema, List<String> sourceRows) throws Exception {
        File root = tmp.newFolder();
        Directories directories = Directories.createFor(DirectoryStructure.PropertyGraph, root, "export-id", "", "");

        File sourceFile = new File(tmp.newFolder(), "nodes-source.csv");
        try (Writer writer = new FileWriter(sourceFile)) {
            for (String row : sourceRows) {
                writer.write(row);
                writer.write("\n");
            }
        }

        PropertyGraphTargetConfig targetConfig = new PropertyGraphTargetConfig(
                directories,
                null,
                new PrinterOptions(CsvPrinterOptions.builder().build()),
                PropertyGraphExportFormat.csv,
                Target.files,
                false,
                false,
                true);

        FileSpecificLabelSchema fileSchema =
                new FileSpecificLabelSchema(sourceFile.getAbsolutePath(), PropertyGraphExportFormat.csv, labelSchema);
        MasterLabelSchema masterLabelSchema =
                new MasterLabelSchema(labelSchema, Collections.singletonList(fileSchema));
        Map<Label, MasterLabelSchema> schemas = new HashMap<>();
        schemas.put(labelSchema.label(), masterLabelSchema);
        MasterLabelSchemas input = new MasterLabelSchemas(schemas, GraphElementType.nodes);

        RewriteCsv command = new RewriteCsv(
                targetConfig, new ConcurrencyConfig(1), new FeatureToggles(Collections.emptyList()));

        MasterLabelSchemas result = command.execute(input);

        List<String> outputIds = result.schemas().stream()
                .flatMap(s -> s.fileSpecificLabelSchemas().stream())
                .map(FileSpecificLabelSchema::outputId)
                .collect(Collectors.toList());

        assertEquals("expected a single rewritten output file", 1, outputIds.size());

        return dataLines(outputIds.get(0));
    }

    // Returns the data rows of the output file. Data rows always start with a quoted id (");
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
