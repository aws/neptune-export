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

package org.apache.tinkerpop.gremlin.driver;

import com.amazonaws.services.neptune.auth.HandshakeRequestConfig;
import com.amazonaws.services.neptune.auth.LBAwareAwsSigV4ClientHandshaker;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.tinkerpop.gremlin.driver.handler.WebSocketClientHandler;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LBAwareSigV4WebSocketChannelizerTest  {
    @Test
    public void configureShouldAddSigV4HandshakerToPipeline() throws URISyntaxException {
        System.setProperty("SERVICE_REGION", "us-west-2");

        ChannelPipeline mockedPipeline = new EmbeddedChannel().pipeline();
        LBAwareSigV4WebSocketChannelizer channelizer = new LBAwareSigV4WebSocketChannelizer();
        Connection mockedConnection = mock(Connection.class);
        Cluster mockedCluster = mock(Cluster.class);

        when(mockedConnection.getCluster()).thenReturn(mockedCluster);
        when(mockedConnection.getUri()).thenReturn(new URI("ws:localhost"));

        when(mockedCluster.connectionPoolSettings()).thenReturn(mock(Settings.ConnectionPoolSettings.class));
        when(mockedCluster.authProperties()).thenReturn(new AuthProperties().with(AuthProperties.Property.JAAS_ENTRY, new HandshakeRequestConfig(Collections.emptyList(), 8182, false).value()));

        channelizer.init(mockedConnection);
        channelizer.configure(mockedPipeline);
        ChannelHandler handler = mockedPipeline.get(LBAwareSigV4WebSocketChannelizer.WEB_SOCKET_HANDLER);

        assertTrue(handler instanceof WebSocketClientHandler);
        assertTrue(((WebSocketClientHandler) handler).handshaker() instanceof LBAwareAwsSigV4ClientHandshaker);
    }
}
