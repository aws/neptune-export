package com.amazonaws.services.neptune.util;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import org.junit.Before;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static com.amazonaws.services.neptune.util.AWSCredentialsUtil.getProfileCredentialsProvider;
import static com.amazonaws.services.neptune.util.AWSCredentialsUtil.getSTSAssumeRoleCredentialsProvider;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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
        AWSCredentialsProvider provider = getProfileCredentialsProvider(null, null);
        assertTrue(provider instanceof DefaultAWSCredentialsProviderChain);
    }

    @Test
    public void shouldAttemptToUseProvidedPath() {
        Throwable t = assertThrows(IllegalArgumentException.class, () -> getProfileCredentialsProvider(
                null, tempFolder.getRoot().getAbsolutePath()+"/non-existent-file").getCredentials());
        assertEquals("AWS credential profiles file not found in the given path: "+
                tempFolder.getRoot().getAbsolutePath()+"/non-existent-file", t.getMessage());
    }

    @Test
    public void shouldUseDefaultCredsIfProfileNameNull() {
        Throwable t = assertThrows(IllegalArgumentException.class, () -> getProfileCredentialsProvider(
                null, credentialsFile.getAbsolutePath()).getCredentials());
        assertTrue(t.getMessage().contains("No AWS profile named 'default'"));
    }

    @Test
    public void shouldAttemptToUseProvidedProfileName() {
        Throwable t = assertThrows(IllegalArgumentException.class, () -> getProfileCredentialsProvider(
                "test", credentialsFile.getAbsolutePath()).getCredentials());
        assertTrue(t.getMessage().contains("No AWS profile named 'test'"));
    }

    @Test
    public void shouldUseSourceCredsProviderWhenAssumingRole() {
        AWSCredentialsProvider mockSourceCredsProvider = mock(AWSCredentialsProvider.class);
        try {
            getSTSAssumeRoleCredentialsProvider("fakeARN", "sessionName", null, mockSourceCredsProvider, "us-west-2")
                    .getCredentials();
        }
        catch (AWSSecurityTokenServiceException e) {} //Expected to fail as sourceCredsProvider does not have permission to assume role

        Mockito.verify(mockSourceCredsProvider, Mockito.atLeast(1)).getCredentials();
    }
}
