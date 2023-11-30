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

package com.amazonaws.services.neptune.propertygraph;

import com.amazonaws.services.neptune.auth.HandshakeRequestConfig;
import com.amazonaws.services.neptune.cluster.ConcurrencyConfig;
import com.amazonaws.services.neptune.cluster.ConnectionConfig;
import com.amazonaws.services.neptune.propertygraph.io.SerializationConfig;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.HandshakeInterceptor;
import org.apache.tinkerpop.gremlin.driver.LBAwareSigV4WebSocketChannelizer;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.junit.Test;
import org.apache.tinkerpop.gremlin.driver.Client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NeptuneGremlinClientTest {

    private final SerializationConfig defaultSerializationConfig = new SerializationConfig(
            Serializers.GRAPHBINARY_V1D0.name(), 50000000, NeptuneGremlinClient.DEFAULT_BATCH_SIZE, false);

    @Test
    public void testQueryClientSubmit() {
        Client mockedClient = mock(Client.class);
        NeptuneGremlinClient.QueryClient qc = new NeptuneGremlinClient.QueryClient(mockedClient);
        qc.submit("test", null);
        verify(mockedClient).submit("test");
    }

    @Test
    public void testConnectionConfigPassthrough() {
        com.amazonaws.services.neptune.cluster.Cluster mockedCluster = mock(com.amazonaws.services.neptune.cluster.Cluster.class);
        Collection endpoints = new HashSet();
        endpoints.add("localhost");

        //With SSL Enabled
        when(mockedCluster.connectionConfig()).thenReturn(new ConnectionConfig(
                null, endpoints, 1234, false, true, null));
        when(mockedCluster.concurrencyConfig()).thenReturn(new ConcurrencyConfig(1));

        NeptuneGremlinClient client = NeptuneGremlinClient.create(mockedCluster, defaultSerializationConfig);

        Cluster cluster = getClusterFromClient(client);
        cluster.init();

        assertEquals(1234, cluster.getPort());
        assertEquals("wss://localhost:1234/gremlin", cluster.allHosts().iterator().next().getHostUri().toString());
        assertEquals(true, cluster.isSslEnabled());

        //With SSL Disabled
        when(mockedCluster.connectionConfig()).thenReturn(new ConnectionConfig(
                null, endpoints, 1234, false, false, null));
        client = NeptuneGremlinClient.create(mockedCluster, defaultSerializationConfig);

        cluster = getClusterFromClient(client);
        cluster.init();

        assertEquals("ws://localhost:1234/gremlin", cluster.allHosts().iterator().next().getHostUri().toString());
        assertEquals(false, cluster.isSslEnabled());

    }

    @Test
    public void shouldUseHandshakeInterceptorForSigningDirectConnections() {
        ConnectionConfig mockedConfig = mock(ConnectionConfig.class);
        when(mockedConfig.isDirectConnection()).thenReturn(true);

        Cluster.Builder builder = Cluster.build();

        builder = NeptuneGremlinClient.configureIamSigning(builder, mockedConfig);

        Cluster cluster = builder.create();

        HandshakeInterceptor interceptor;

        try {
            Method getHandshakeInterceptor = cluster.getClass().getDeclaredMethod("getHandshakeInterceptor");
            getHandshakeInterceptor.setAccessible(true);
            interceptor = (HandshakeInterceptor) getHandshakeInterceptor.invoke(cluster);
            getHandshakeInterceptor.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertNotNull(interceptor);
        assertNotEquals(interceptor, HandshakeInterceptor.NO_OP);

    }

    @Test
    public void shouldUseLBAwareChannelizerForSigningProxyConnections() {
        ConnectionConfig mockedConfig = mock(ConnectionConfig.class);
        when(mockedConfig.isDirectConnection()).thenReturn(false);
        when(mockedConfig.handshakeRequestConfig()).thenReturn(mock(HandshakeRequestConfig.class));
        Cluster.Builder builder = Cluster.build();

        builder = NeptuneGremlinClient.configureIamSigning(builder, mockedConfig);

        assertEquals(LBAwareSigV4WebSocketChannelizer.class.getName(), builder.create().getChannelizer());
    }

    private static Cluster getClusterFromClient(NeptuneGremlinClient client) {
        try {
            Field clusterField = client.getClass().getDeclaredField("cluster");
            clusterField.setAccessible(true);
            Cluster cluster = (Cluster) clusterField.get(client);
            clusterField.setAccessible(false);
            return cluster;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
