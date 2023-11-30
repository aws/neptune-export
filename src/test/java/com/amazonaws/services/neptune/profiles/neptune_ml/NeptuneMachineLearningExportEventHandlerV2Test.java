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

package com.amazonaws.services.neptune.profiles.neptune_ml;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.neptune.export.Args;
import com.amazonaws.services.neptune.propertygraph.EdgeLabelStrategy;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class NeptuneMachineLearningExportEventHandlerV2Test {

    @Test
    public void onBeforeExportShouldSetExportPGArgs() {
        NeptuneMachineLearningExportEventHandlerV2 handler = createEmptyHandler();
        Args args = new Args("export-pg");

        handler.onBeforeExport(args, null);

        assertTrue(args.contains("export-pg"));
        assertTrue(args.contains("--exclude-type-definitions"));
        assertTrue(args.contains("--edge-label-strategy", EdgeLabelStrategy.edgeAndVertexLabels.name()));
        assertTrue(args.contains("--merge-files"));
    }

    @Test
    public void onBeforeExportShouldSetExportPGFromQueriesArgs() {
        NeptuneMachineLearningExportEventHandlerV2 handler = createEmptyHandler();
        Args args = new Args("export-pg-from-queries");

        handler.onBeforeExport(args, null);

        assertTrue(args.contains("export-pg-from-queries"));
        assertTrue(!args.contains("--include-type-definitions"));
        assertTrue(!args.contains("--exclude-type-definitions"));
        assertTrue(args.contains("--edge-label-strategy", EdgeLabelStrategy.edgeAndVertexLabels.name()));
        assertTrue(args.contains("--merge-files"));
        assertTrue(args.contains("--structured-output"));
    }

    @Test
    public void onBeforeExportShouldReplaceEdgeLabelStrategy() {
        NeptuneMachineLearningExportEventHandlerV2 handler = createEmptyHandler();
        Args args = new Args("export-pg --edge-label-strategy edgeLabelsOnly");

        assertTrue(args.contains("--edge-label-strategy", EdgeLabelStrategy.edgeLabelsOnly.name()));

        handler.onBeforeExport(args, null);

        assertTrue(args.contains("--edge-label-strategy", EdgeLabelStrategy.edgeAndVertexLabels.name()));
    }

    @Test
    public void onBeforeExportShouldReplaceEdgeLabelStrategyForQueries() {
        NeptuneMachineLearningExportEventHandlerV2 handler = createEmptyHandler();
        Args args = new Args("export-pg-from-queries --edge-label-strategy edgeLabelsOnly");

        assertTrue(args.contains("--edge-label-strategy", EdgeLabelStrategy.edgeLabelsOnly.name()));

        handler.onBeforeExport(args, null);

        assertTrue(args.contains("--edge-label-strategy", EdgeLabelStrategy.edgeAndVertexLabels.name()));
    }

    private NeptuneMachineLearningExportEventHandlerV2 createEmptyHandler() {
        return new NeptuneMachineLearningExportEventHandlerV2(
                "",
                "",
                false,
                new ObjectNode(JsonNodeFactory.instance),
                new Args(new String[]{}),
                Collections.EMPTY_SET,
                "",
                mock(AWSCredentialsProvider.class)
        );
    }
}
