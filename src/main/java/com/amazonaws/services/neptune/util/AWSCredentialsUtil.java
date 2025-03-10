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

import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.nio.file.Paths;

public class AWSCredentialsUtil {

    private static final Logger logger = LoggerFactory.getLogger(AWSCredentialsUtil.class);

    public static AwsCredentialsProvider getProfileCredentialsProvider(String profileName, String profilePath) {
        if (StringUtils.isEmpty(profileName) && StringUtils.isEmpty(profilePath)) {
            return DefaultCredentialsProvider.create();
        }
        if (StringUtils.isEmpty(profilePath)) {
            logger.debug(String.format("Using ProfileCredentialsProvider with profile: %s", profileName));
            return ProfileCredentialsProvider.builder().profileName(profileName).build();
        }
        logger.debug(String.format("Using ProfileCredentialsProvider with profile: %s and credentials file: ", profileName, profilePath));
        return ProfileCredentialsProvider.builder()
                .profileFile(ProfileFile.builder().content(Paths.get(profilePath)).type(ProfileFile.Type.CREDENTIALS).build())
                .profileName(profileName)
                .build();
    }

    public static AwsCredentialsProvider getSTSAssumeRoleCredentialsProvider(String roleARN, String sessionName, String externalId) {
        return getSTSAssumeRoleCredentialsProvider(roleARN, sessionName, externalId, DefaultCredentialsProvider.create());
    }

    public static AwsCredentialsProvider getSTSAssumeRoleCredentialsProvider(String roleARN,
                                                                             String sessionName,
                                                                             String externalId,
                                                                             AwsCredentialsProvider sourceCredentialsProvider) {
        return getSTSAssumeRoleCredentialsProvider(roleARN, sessionName, externalId, sourceCredentialsProvider,
                new DefaultAwsRegionProviderChain().getRegion());
    }

    public static AwsCredentialsProvider getSTSAssumeRoleCredentialsProvider(String roleARN,
                                                                             String sessionName,
                                                                             String externalId,
                                                                             AwsCredentialsProvider sourceCredentialsProvider,
                                                                             String region) {
        AssumeRoleRequest.Builder assumeRoleBuilder = AssumeRoleRequest.builder()
                .roleArn(roleARN)
                .roleSessionName(sessionName);
        if (externalId != null) {
            assumeRoleBuilder = assumeRoleBuilder.externalId(externalId);
        }

        StsAssumeRoleCredentialsProvider.Builder providerBuilder = StsAssumeRoleCredentialsProvider.builder()
                .refreshRequest(assumeRoleBuilder.build())
                .stsClient(StsClient.builder()
                        .credentialsProvider(sourceCredentialsProvider)
                        .region(Region.of(region)).build()
                );

        logger.debug(String.format("Assuming Role: %s with session name: %s", roleARN, sessionName));
        return providerBuilder.build();
    }

}
