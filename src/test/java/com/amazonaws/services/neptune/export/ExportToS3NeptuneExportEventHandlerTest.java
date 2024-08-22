package com.amazonaws.services.neptune.export;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.neptune.cluster.Cluster;
import com.amazonaws.services.neptune.io.Directories;
import com.amazonaws.services.neptune.propertygraph.ExportStats;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExportToS3NeptuneExportEventHandlerTest {

    @Test
    public void shouldThrowErrorIfDirectoryMissing() {
        ExportToS3NeptuneExportEventHandler handler = new ExportToS3NeptuneExportEventHandler(
                "localOutputPath",
                "outputS3Path",
                "s3Region",
                "completionFileS3Path",
                mock(ObjectNode.class),
                false,
                new ExportToS3NeptuneExportEventHandler.S3UploadParams(),
                Collections.EMPTY_SET,
                Collections.EMPTY_SET,
                "",
                mock(AWSCredentialsProvider.class)
        );

        Directories mockDir = mock(Directories.class);
        when(mockDir.rootDirectory()).thenReturn(Paths.get("nonExistentDirectory"));

        Throwable t = assertThrows(RuntimeException.class, () -> handler.onExportComplete(
                mockDir, mock(ExportStats.class), mock(Cluster.class)
        ));
        assertEquals(
                "Failed to upload files to S3 because upload directory from which to upload files does not exist",
                t.getMessage()
        );
    }
}
