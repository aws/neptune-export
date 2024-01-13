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

package com.amazonaws.services.neptune.io;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.neptune.cli.AbstractTargetModule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KinesisConfigTest {

    private AbstractTargetModule target;

    @Before
    public void resetTargetModule() {
        target = spy(AbstractTargetModule.class);
    }

    @Test
    public void shouldCreateStreamIfNameAndRegionAreProvided() {
        when(target.getStreamName()).thenReturn("test");
        when(target.getRegion()).thenReturn("us-west-2");
        KinesisConfig config = new KinesisConfig(target);

        assertNotNull(config.stream());
    }

    @Test
    public void shouldNotCreateStreamIfNameNotProvided() {
        when(target.getStreamName()).thenReturn("");
        when(target.getRegion()).thenReturn("us-west-2");
        KinesisConfig config = new KinesisConfig(target);

        Throwable t = assertThrows(IllegalArgumentException.class, () -> config.stream());
        assertEquals("You must supply an AWS Region and Amazon Kinesis Data Stream name", t.getMessage());
    }

    @Test
    public void shouldNotCreateStreamIfRegionNotProvided() {
        when(target.getStreamName()).thenReturn("test");
        when(target.getRegion()).thenReturn("");
        KinesisConfig config = new KinesisConfig(target);

        Throwable t = assertThrows(IllegalArgumentException.class, () -> config.stream());
        assertEquals("You must supply an AWS Region and Amazon Kinesis Data Stream name", t.getMessage());
    }

    @Test
    public void shouldUseProvidedCredentialsProvider() {
        when(target.getStreamName()).thenReturn("test");
        when(target.getRegion()).thenReturn("us-west-2");
        AWSCredentials mockedCreds = mock(AWSCredentials.class);
        AWSCredentialsProvider mockedCredsProvider = mock(AWSCredentialsProvider.class);
        when(mockedCredsProvider.getCredentials()).thenReturn(mockedCreds);
        when(target.getCredentialsProvider()).thenReturn(mockedCredsProvider);

        KinesisConfig config = new KinesisConfig(target);
        config.stream().publish("test");

        verify(mockedCredsProvider, Mockito.atLeast(1)).getCredentials();
    }
}
