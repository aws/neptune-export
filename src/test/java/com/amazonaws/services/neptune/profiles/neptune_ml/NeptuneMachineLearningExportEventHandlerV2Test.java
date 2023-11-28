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
