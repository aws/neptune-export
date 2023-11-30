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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AWSCredentialsUtil {

    private static final Logger logger = LoggerFactory.getLogger(AWSCredentialsUtil.class);

    public static AWSCredentialsProvider getProfileCredentialsProvider(String profileName, String profilePath) {
        if (StringUtils.isEmpty(profileName) && StringUtils.isEmpty(profilePath)) {
            return new DefaultAWSCredentialsProviderChain();
        }
        if (StringUtils.isEmpty(profilePath)) {
            logger.debug(String.format("Using ProfileCredentialsProvider with profile: %s", profileName));
            return new ProfileCredentialsProvider(profileName);
        }
        logger.debug(String.format("Using ProfileCredentialsProvider with profile: %s and credentials file: ", profileName, profilePath));
        return new ProfileCredentialsProvider(profilePath, profileName);
    }

    public static AWSCredentialsProvider getSTSAssumeRoleCredentialsProvider(String roleARN, String sessionName, String externalId) {
        return getSTSAssumeRoleCredentialsProvider(roleARN, sessionName, externalId, new DefaultAWSCredentialsProviderChain());
    }

    public static AWSCredentialsProvider getSTSAssumeRoleCredentialsProvider(String roleARN,
                                                                             String sessionName,
                                                                             String externalId,
                                                                             AWSCredentialsProvider sourceCredentialsProvider) {
        return getSTSAssumeRoleCredentialsProvider(roleARN, sessionName, externalId, sourceCredentialsProvider,
                new DefaultAwsRegionProviderChain().getRegion());
    }

    public static AWSCredentialsProvider getSTSAssumeRoleCredentialsProvider(String roleARN,
                                                                             String sessionName,
                                                                             String externalId,
                                                                             AWSCredentialsProvider sourceCredentialsProvider,
                                                                             String region) {
        STSAssumeRoleSessionCredentialsProvider.Builder providerBuilder = new STSAssumeRoleSessionCredentialsProvider.Builder(roleARN, sessionName)
                .withStsClient(
                        AWSSecurityTokenServiceClient.builder().withCredentials(sourceCredentialsProvider).withRegion(region).build());
        if (externalId != null) {
            providerBuilder = providerBuilder.withExternalId(externalId);
        }
        logger.debug(String.format("Assuming Role: %s with session name: %s", roleARN, sessionName));
        return providerBuilder.build();
    }

}
