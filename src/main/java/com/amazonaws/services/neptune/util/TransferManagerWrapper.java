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

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import org.apache.commons.lang3.StringUtils;

public class TransferManagerWrapper implements AutoCloseable {

    private final S3TransferManager transferManager;

    public TransferManagerWrapper(String s3Region) {
        this(s3Region, null);
    }

    public TransferManagerWrapper(String s3Region, AwsCredentialsProvider credentialsProvider) {

        S3AsyncClientBuilder amazonS3ClientBuilder = S3AsyncClient.builder();
        if (credentialsProvider != null) {
            amazonS3ClientBuilder = amazonS3ClientBuilder.credentialsProvider(credentialsProvider);
        }

        if (StringUtils.isNotEmpty(s3Region)) {
            amazonS3ClientBuilder = amazonS3ClientBuilder.region(Region.of(s3Region));
        }

        transferManager = S3TransferManager.builder()
                .s3Client(amazonS3ClientBuilder.build())
                .build();
    }

    public S3TransferManager get() {
        return transferManager;
    }

    @Override
    public void close() {
        transferManager.close();
    }
}
