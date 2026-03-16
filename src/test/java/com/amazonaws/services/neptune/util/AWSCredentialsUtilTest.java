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

package com.amazonaws.services.neptune.util;

import org.junit.Before;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.File;
import java.io.IOException;

import static com.amazonaws.services.neptune.util.AWSCredentialsUtil.getProfileCredentialsProvider;
import static com.amazonaws.services.neptune.util.AWSCredentialsUtil.getSTSAssumeRoleCredentialsProvider;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

public class AWSCredentialsUtilTest {

    TemporaryFolder tempFolder;
    File credentialsFile;

    @Before
    public void setup() throws IOException {
        tempFolder = new TemporaryFolder();
        tempFolder.create();
        credentialsFile = tempFolder.newFile("credentialsFile");
    }

    @Test
    public void shouldGetDefaultCredsIfConfigIsNull() {
        AwsCredentialsProvider provider = getProfileCredentialsProvider(null, null);
        assertTrue(provider instanceof DefaultCredentialsProvider);
    }

    @Test
    public void shouldAttemptToUseProvidedPath() {
        Throwable t = assertThrows(IllegalStateException.class, () -> getProfileCredentialsProvider(
                null, tempFolder.getRoot().getAbsolutePath() + File.separator + "non-existent-file").resolveCredentials());
        assertEquals("Profile file '"+
                tempFolder.getRoot().getAbsolutePath() + File.separator + "non-existent-file' does not exist.", t.getMessage());
    }

    @Test
    public void shouldUseDefaultCredsIfProfileNameNull() {
        Throwable t = assertThrows(SdkClientException.class, () -> getProfileCredentialsProvider(
                null, credentialsFile.getAbsolutePath()).resolveCredentials());
        assertTrue(t.getMessage().contains("Profile file contained no credentials for profile 'default'"));
    }

    @Test
    public void shouldAttemptToUseProvidedProfileName() {
        Throwable t = assertThrows(SdkClientException.class, () -> getProfileCredentialsProvider(
                "test", credentialsFile.getAbsolutePath()).resolveCredentials());
        assertTrue(t.getMessage().contains("Profile file contained no credentials for profile 'test'"));
    }

    @Test
    public void shouldUseSourceCredsProviderWhenAssumingRole() {
        AwsCredentialsProvider mockSourceCredsProvider = spy(AnonymousCredentialsProvider.create());
        try {
            getSTSAssumeRoleCredentialsProvider("fakeARN", "sessionName", null, mockSourceCredsProvider, "us-west-2")
                    .resolveCredentials();
        }
        catch (SdkException e) {
            System.out.println("Test");
        } //Expected to fail as sourceCredsProvider does not have permission to assume role

        Mockito.verify(mockSourceCredsProvider, Mockito.atLeast(1)).resolveCredentials();
    }
}
