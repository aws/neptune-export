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
import com.amazonaws.services.neptune.export.FeatureToggle;
import com.amazonaws.services.neptune.export.FeatureToggles;
import com.amazonaws.services.neptune.io.PrintOutputWriter;
import com.amazonaws.services.neptune.rdf.io.RdfExportFormat;
import com.amazonaws.services.neptune.rdf.io.RdfTargetConfig;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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

    @Before
    public void setup() throws IOException {
        // init a mock SPARQLRepository which is backed by a SailRepository containing test data.
        sailRepository = new SailRepository(new MemoryStore());
        sailRepository.getConnection().add(new File("src/test/resources/IntegrationTest/testExportRdfVersionNamedGraph/statements/statements.ttl"));
        mockSPARQLRepository = mock(SPARQLRepository.class);

        doReturn(sailRepository.getConnection()).when(mockSPARQLRepository).getConnection();
        doReturn(sailRepository.getHttpClientSessionManager()).when(mockSPARQLRepository).getHttpClientSessionManager();
        doReturn(sailRepository.getValueFactory()).when(mockSPARQLRepository).getValueFactory();
    }

    @Test
    public void testExecuteTupleQuerySelectAll() throws IOException {
        StringWriter outputWriter = new StringWriter();
        NeptuneSparqlClient client = createNeptuneSparqlClient();

        // Test data does not have named graphs but a ?g binding is required by TupleQueryHandler
        client.executeTupleQuery("SELECT * WHERE { BIND(<http://aws.amazon.com/neptune/csv2rdf/graph/version> AS ?g) ?s ?p ?o }", getMockTargetConfig(outputWriter));

        assertEquals(testDataNTriples, outputWriter.toString());
    }

    @Test
    public void completeExportShouldExportDefaultGraphViaGSP() throws IOException {
        NeptuneSparqlClient client = createNeptuneSparqlClient();
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeCompleteExport(targetConfig);

        verify(client, times(1)).executeGSPExport(targetConfig, "default");
        verify(client, never()).executeTupleQuery(any(), any());
    }

    @Test
    public void completeExportShouldExecuteTupleQueryIfNoGSP() throws IOException {
        NeptuneSparqlClient client = createNeptuneSparqlClient(FeatureToggle.No_GSP);
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeCompleteExport(targetConfig);

        verify(client, times(1)).executeTupleQuery("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }", targetConfig);
        verify(client, never()).executeGSPExport(any(), any());
    }

    @Test
    public void namedGraphExportShouldExportDefaultGraphViaGSP() throws IOException {
        NeptuneSparqlClient client = createNeptuneSparqlClient();
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeNamedGraphExport(targetConfig, "GraphName");

        verify(client, times(1)).executeGSPExport(targetConfig, "graph=GraphName");
        verify(client, never()).executeTupleQuery(any(), any());
    }

    @Test
    public void namedGraphExportShouldExecuteTupleQueryIfNoGSP() throws IOException {
        NeptuneSparqlClient client = createNeptuneSparqlClient(FeatureToggle.No_GSP);
        RdfTargetConfig targetConfig = getMockTargetConfig(new StringWriter());

        client.executeNamedGraphExport(targetConfig, "http://example.com");

        verify(client, times(1)).executeTupleQuery("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } FILTER(?g = <http://example.com>) .}", targetConfig);
        verify(client, never()).executeGSPExport(any(), any());
    }

    private NeptuneSparqlClient createNeptuneSparqlClient(FeatureToggle ... featureToggles) throws IOException {
        NeptuneSparqlClient client = spy(NeptuneSparqlClient.create(mock(ConnectionConfig.class), new FeatureToggles(Arrays.asList(featureToggles))));

        doReturn(mockSPARQLRepository).when(client).chooseRepository();
        doNothing().when(client).executeGSPExport(any(), any());

        return client;
    }

    private RdfTargetConfig getMockTargetConfig(Writer outputWriter) throws IOException {
        RdfTargetConfig target = spy(new RdfTargetConfig(null, null, null, RdfExportFormat.ntriples));
        doReturn(new PrintOutputWriter("TestOutputWriter", outputWriter)).when(target).createOutputWriter();

        return target;
    }
}
