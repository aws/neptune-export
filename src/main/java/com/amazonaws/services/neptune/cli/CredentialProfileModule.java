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

package com.amazonaws.services.neptune.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsProfileRegionProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.AwsRegionProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.neptune.util.AWSCredentialsUtil;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Once;
import org.apache.commons.lang.StringUtils;

public class CredentialProfileModule {
    @Option(name = {"--credentials-profile"}, description = "Use profile from credentials config file.", hidden = true)
    @Once
    private String credentialsProfile;

    @Option(name = {"--credentials-config-file"}, description = "Load credentials profile from specified config file.", hidden = true)
    @Once
    private String credentialsConfigFilePath;

    public AWSCredentialsProvider getCredentialsProvider() {
        return AWSCredentialsUtil.getProfileCredentialsProvider(credentialsProfile, credentialsConfigFilePath);
    }

    public AwsRegionProvider getRegionProvider() {
        if(StringUtils.isEmpty(credentialsProfile)) {
            return new DefaultAwsRegionProviderChain();
        }
        return new AwsRegionProviderChain(new AwsProfileRegionProvider(credentialsProfile), new DefaultAwsRegionProviderChain());
    }

}
