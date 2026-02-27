/*
Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.rdf;

import com.amazonaws.services.neptune.cluster.ConnectionConfig;
import com.amazonaws.services.neptune.cluster.NeptuneClusterMetadata;
import com.amazonaws.services.neptune.export.FeatureToggle;
import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.io.OutputWriter;
import com.amazonaws.services.neptune.io.PrintOutputWriter;
import com.amazonaws.services.neptune.rdf.io.RdfExportFormat;
import com.amazonaws.services.neptune.rdf.io.RdfTargetConfig;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NeptuneSparqlClientTest {

    private SPARQLRepository mockSPARQLRepository;
    private SailRepository sailRepository;
    private final String testDataNTriples = "<http://aws.amazon.com/neptune/csv2rdf/resource/0> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://aws.amazon.com/neptune/csv2rdf/class/Version> .\n" +
            "<http://aws.amazon.com/neptune/csv2rdf/resource/0> <http://aws.amazon.com/neptune/csv2rdf/datatypeProperty/type> \"version\" .\n" +
            "<http://aws.amazon.com/neptune/csv2rdf/resource/0> <http://aws.amazon.com/neptune/csv2rdf/datatypeProperty/code> \"0.77\" .\n" +
            "<http://aws.amazon.com/neptune/csv2rdf/resource/0> <http://aws.amazon.com/neptune/csv2rdf/datatypeProperty/desc> \"Version: 0.77 Generated: 2017-10-06 16:24:52 UTC\\nGraph created by Kelvin R. Lawrence\\nPlease let me know of any errors you find in the graph.\" .\n";

    private OutputWriter writer;

    @Before
    public void setup() throws IOException {
        // init a mock SPARQLRepository which is backed by a SailRepository containing test data.
        sailRepository = new SailRepository(new MemoryStore());
        sailRepository.getConnection().add(new File("src/test/resources/IntegrationTest/testExportRdfVersionNamedGraph/statements/statements.ttl"));
        mockSPARQLRepository = mock(SPARQLRepository.class);

        HttpClient mockHttpClient = mock(HttpClient.class);
        BasicHttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 200, "Success");
        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(new ByteArrayInputStream(new byte[]{}));
        response.setEntity(httpEntity);
        doReturn(response).when(mockHttpClient).execute(any());

        doReturn(sailRepository.getConnection()).when(mockSPARQLRepository).getConnection();
        doReturn(sailRepository.getHttpClientSessionManager()).when(mockSPARQLRepository).getHttpClientSessionManager();
        doReturn(sailRepository.getValueFactory()).when(mockSPARQLRepository).getValueFactory();
        doReturn(mockHttpClient).when(mockSPARQLRepository).getHttpClient();
    }

    @Test
    public void testExecuteTupleQuerySelectAll() throws Exception {
        StringWriter outputWriter = new StringWriter();
        NeptuneSparqlClient client = createNeptuneSparqlClient();

        // Test data does not have named graphs but a ?g binding is required by TupleQueryHandler
        client.executeTupleQuery("SELECT * WHERE { BIND(<http://aws.amazon.com/neptune/csv2rdf/graph/version> AS ?g) ?s ?p ?o }", getMockTargetConfig(outputWriter));

        assertEquals(testDataNTriples, outputWriter.toString());
    }

    @Test
    public void completeExportShouldExportDefaultGraphViaGSPWithOldNeptune() throws Exception {
        NeptuneSparqlClient client = createNeptuneSparqlClient("1.2.0.0");
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeCompleteExport(targetConfig);

        verify(client, times(1)).executeGSPExport(targetConfig, "default");
        verify(client, never()).executeTupleQuery(any(), any());
        verifyWriterClosed();
    }

    @Test
    public void completeExportShouldExportSPARQLWithNewNeptune() throws Exception {
        NeptuneSparqlClient client = createNeptuneSparqlClient("1.4.6.3");
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeCompleteExport(targetConfig);

        verify(client, never()).executeGSPExport(any(), any());
        verify(client, times(1)).executeTupleQuery(any(), any());
        verifyWriterClosed();
    }

    @Test
    public void completeExportShouldExportSPARQLWithOldNeptuneAndNQuads() throws Exception {
        NeptuneSparqlClient client = createNeptuneSparqlClient("1.2.0.0");
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter(), RdfExportFormat.nquads);

        client.executeCompleteExport(targetConfig);

        verify(client, never()).executeGSPExport(any(), any());
        verify(client, times(1)).executeTupleQuery(any(), any());
        verifyWriterClosed();
    }

    @Test
    public void completeExportShouldExecuteTupleQueryIfNoGSP() throws Exception {
        NeptuneSparqlClient client = createNeptuneSparqlClient(FeatureToggle.No_GSP);
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeCompleteExport(targetConfig);

        verify(client, times(1)).executeTupleQuery("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }", targetConfig);
        verify(client, never()).executeGSPExport(any(), any());
        verifyWriterClosed();
    }

    @Test
    public void namedGraphExportShouldExportViaGSPWithOldNeptune() throws Exception {
        NeptuneSparqlClient client = createNeptuneSparqlClient("1.2.0.0");
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeNamedGraphExport(targetConfig, "GraphName");

        verify(client, times(1)).executeGSPExport(targetConfig, "graph=GraphName");
        verify(client, never()).executeTupleQuery(any(), any());
        verifyWriterClosed();
    }

    @Test
    public void namedGraphExportShouldExportViaSPARQLWithNewNeptune() throws Exception {
        NeptuneSparqlClient client = createNeptuneSparqlClient("1.4.6.3");
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeNamedGraphExport(targetConfig, "http://graphname.example.com");

        verify(client, never()).executeGSPExport(any(), any());
        verify(client, times(1)).executeTupleQuery(any(), any());
        verifyWriterClosed();
    }

    @Test
    public void namedGraphExportShouldExportViaSPARQLWithOldNeptuneAndNQuads() throws Exception {
        NeptuneSparqlClient client = createNeptuneSparqlClient("1.2.0.0");
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter(), RdfExportFormat.nquads);

        client.executeNamedGraphExport(targetConfig, "http://graphname.example.com");

        verify(client, never()).executeGSPExport(any(), any());
        verify(client, times(1)).executeTupleQuery(any(), any());
        verifyWriterClosed();
    }

    @Test
    public void namedGraphExportShouldExecuteTupleQueryIfNoGSP() throws Exception {
        NeptuneSparqlClient client = createNeptuneSparqlClient(FeatureToggle.No_GSP);
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeNamedGraphExport(targetConfig, "http://example.com");

        verify(client, times(1)).executeTupleQuery("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } FILTER(?g = <http://example.com>) .}", targetConfig);
        verify(client, never()).executeGSPExport(any(), any());
        verifyWriterClosed();
    }

    private NeptuneSparqlClient createNeptuneSparqlClient(FeatureToggle ... featureToggles) throws IOException {
        return createNeptuneSparqlClient("1.4.6.3", featureToggles);
    }

    private NeptuneSparqlClient createNeptuneSparqlClient(String engineVersion, FeatureToggle ... featureToggles) throws IOException {
        ConnectionConfig mockConnectionConfig = mock(ConnectionConfig.class);
        doReturn(Collections.singletonList("localhost")).when(mockConnectionConfig).endpoints();

        NeptuneClusterMetadata mockClusterMetadata = mock(NeptuneClusterMetadata.class);
        doReturn(engineVersion).when(mockClusterMetadata).engineVersion();

        NeptuneSparqlClient client = spy(NeptuneSparqlClient.create(mockConnectionConfig, mockClusterMetadata, new FeatureToggles(Arrays.asList(featureToggles))));

        doReturn(mockSPARQLRepository).when(client).chooseRepository();

        return client;
    }

    private RdfTargetConfig getMockTargetConfig(Writer outputWriter) throws Exception {
        return getMockTargetConfig(outputWriter, RdfExportFormat.ntriples);
    }

    private RdfTargetConfig getMockTargetConfig(Writer outputWriter, RdfExportFormat format) throws Exception {
        RdfTargetConfig target = spy(new RdfTargetConfig(null, null, null, format));
        writer = spy(new PrintOutputWriter("TestOutputWriter", outputWriter));
        doReturn(writer).when(target).createOutputWriter();

        verify(writer, never()).close();

        return target;
    }

    private void verifyWriterClosed() throws Exception {
        verify(writer, atLeastOnce()).close();
    }
}
