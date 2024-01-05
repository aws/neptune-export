package com.amazonaws.services.neptune.rdf;

import com.amazonaws.services.neptune.rdf.io.RdfExportFormat;
import com.amazonaws.services.neptune.rdf.io.RdfTargetConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ExportRdfGraphJobTest {

    @Test
    public void shouldExportExportNamedGraphIfNameProvided() throws IOException {
        NeptuneSparqlClient mockClient = mock(NeptuneSparqlClient.class);
        RdfTargetConfig mockTarget = mock(RdfTargetConfig.class);
        when(mockTarget.format()).thenReturn(mock(RdfExportFormat.class));

        ExportRdfGraphJob job = new ExportRdfGraphJob(mockClient, mockTarget, "test");

        try {
            job.execute();
        } catch (Exception e) {
            //Swallow exceptions due to incomplete mocks
        }

        verify(mockClient, Mockito.times(1)).executeNamedGraphExport(mockTarget, "test");
        verify(mockClient, Mockito.times(0)).executeCompleteExport(Mockito.any());

        verifyNoMoreInteractions(mockClient);
    }

    @Test
    public void shouldDoCompleteExportIfNoNamedGraphProvided() throws IOException {
        NeptuneSparqlClient mockClient = mock(NeptuneSparqlClient.class);
        RdfTargetConfig mockTarget = mock(RdfTargetConfig.class);
        when(mockTarget.format()).thenReturn(mock(RdfExportFormat.class));

        ExportRdfGraphJob job = new ExportRdfGraphJob(mockClient, mockTarget);

        try {
            job.execute();
        } catch (Exception e) {
            //Swallow exceptions due to incomplete mocks
        }

        verify(mockClient, Mockito.times(1)).executeCompleteExport(mockTarget);
        verify(mockClient, Mockito.times(0)).executeNamedGraphExport(Mockito.any(), Mockito.any());

        verifyNoMoreInteractions(mockClient);
    }

    @Test
    public void shouldDoCompleteExportIfNullNamedGraphProvided() throws IOException {
        NeptuneSparqlClient mockClient = mock(NeptuneSparqlClient.class);
        RdfTargetConfig mockTarget = mock(RdfTargetConfig.class);
        when(mockTarget.format()).thenReturn(mock(RdfExportFormat.class));

        ExportRdfGraphJob job = new ExportRdfGraphJob(mockClient, mockTarget, null);

        try {
            job.execute();
        } catch (Exception e) {
            //Swallow exceptions due to incomplete mocks
        }

        verify(mockClient, Mockito.times(1)).executeCompleteExport(mockTarget);
        verify(mockClient, Mockito.times(0)).executeNamedGraphExport(Mockito.any(), Mockito.any());

        verifyNoMoreInteractions(mockClient);
    }

    @Test
    public void shouldDoCompleteExportIfEmptyNamedGraphProvided() throws IOException {
        NeptuneSparqlClient mockClient = mock(NeptuneSparqlClient.class);
        RdfTargetConfig mockTarget = mock(RdfTargetConfig.class);
        when(mockTarget.format()).thenReturn(mock(RdfExportFormat.class));

        ExportRdfGraphJob job = new ExportRdfGraphJob(mockClient, mockTarget, "");

        try {
            job.execute();
        } catch (Exception e) {
            //Swallow exceptions due to incomplete mocks
        }

        verify(mockClient, Mockito.times(1)).executeCompleteExport(mockTarget);
        verify(mockClient, Mockito.times(0)).executeNamedGraphExport(Mockito.any(), Mockito.any());

        verifyNoMoreInteractions(mockClient);
    }

}
