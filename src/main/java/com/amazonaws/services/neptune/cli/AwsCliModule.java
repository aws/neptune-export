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

package com.amazonaws.services.neptune.cli;

import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Once;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.neptune.NeptuneClient;
import software.amazon.awssdk.services.neptune.NeptuneClientBuilder;

import javax.inject.Inject;
import java.net.URI;
import java.util.function.Supplier;

public class AwsCliModule implements Supplier<NeptuneClient> {

    @Inject
    private CredentialProfileModule credentialProfileModule = new CredentialProfileModule();

    @Option(name = {"--aws-cli-endpoint-url"}, description = "AWS CLI endpoint URL.", hidden = true)
    @Once
    private String awsCliEndpointUrl;

    @Option(name = {"--aws-cli-region"}, description = "AWS CLI region.", hidden = true)
    @Once
    private String awsCliRegion;

    @Override
    public NeptuneClient get() {
        NeptuneClientBuilder builder = NeptuneClient.builder();

        if (StringUtils.isNotEmpty(awsCliEndpointUrl) && StringUtils.isNotEmpty(awsCliRegion)) {
            builder = builder.endpointOverride(URI.create(awsCliEndpointUrl)).region(Region.of(awsCliRegion));
        }

        if (credentialProfileModule.getCredentialsProvider() != null) {
            builder = builder
                    .credentialsProvider(credentialProfileModule.getCredentialsProvider())
                    .region(Region.of(credentialProfileModule.getRegionProvider().getRegion()));
        }

        return builder.build();
    }

}
