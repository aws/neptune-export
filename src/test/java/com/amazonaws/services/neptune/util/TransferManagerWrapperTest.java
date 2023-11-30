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

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import org.junit.Test;
import org.mockito.internal.verification.AtLeast;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransferManagerWrapperTest {

    private final String REGION = "us-west-2";
    @Test
    public void shouldHandleNullCredentialsProvider() {
        TransferManagerWrapper wrapper = new TransferManagerWrapper(REGION, null);
        assertNotNull(wrapper);
        assertNotNull(wrapper.get());
        assertNotNull(wrapper.get().getAmazonS3Client());
    }

    @Test
    public void shouldUseProvidedCredentials() {
        AWSCredentialsProvider mockCredentialsProvider = mock(AWSCredentialsProvider.class);
        when(mockCredentialsProvider.getCredentials()).thenReturn(new AnonymousAWSCredentials());

        TransferManagerWrapper wrapper = new TransferManagerWrapper(REGION, mockCredentialsProvider);
        assertNotNull(wrapper);
        assertNotNull(wrapper.get());
        assertNotNull(wrapper.get().getAmazonS3Client());

        //Expected to fail due to invalid credentials. This call is here to force the S3 client to call getCredentials()
        try {
            wrapper.get().getAmazonS3Client().listBuckets();
        }
        catch (SdkClientException e) {}

        verify(mockCredentialsProvider, new AtLeast(1)).getCredentials();
    }
}
