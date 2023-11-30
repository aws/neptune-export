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
import com.amazonaws.services.neptune.io.CommandWriter;
import com.amazonaws.services.neptune.io.Directories;
import com.amazonaws.services.neptune.io.DirectoryStructure;
import com.amazonaws.services.neptune.io.LargeStreamRecordHandlingStrategy;
import com.amazonaws.services.neptune.io.Target;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.AllowedEnumValues;
import com.github.rvesse.airline.annotations.restrictions.Once;
import com.github.rvesse.airline.annotations.restrictions.PathKind;
import com.github.rvesse.airline.annotations.restrictions.Required;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static com.amazonaws.services.neptune.util.AWSCredentialsUtil.getSTSAssumeRoleCredentialsProvider;

public abstract class AbstractTargetModule implements CommandWriter {
    @Inject
    private CredentialProfileModule credentialProfileModule = new CredentialProfileModule();

    @Option(name = {"-d", "--dir"}, description = "Root directory for output.")
    @Required
    @com.github.rvesse.airline.annotations.restrictions.Path(mustExist = false, kind = PathKind.DIRECTORY)
    @Once
    private File directory;

    @Option(name = {"-t", "--tag"}, description = "Directory prefix (optional).")
    @Once
    private String tag = "";

    @Option(name = {"-o", "--output"}, description = "Output target (optional, default 'file').")
    @Once
    @AllowedEnumValues(Target.class)
    private Target output = Target.files;

    @Option(name = {"--stream-name"}, description = "Name of an Amazon Kinesis Data Stream.")
    @Once
    private String streamName;

    @Option(name = {"--region", "--stream-region"}, description = "AWS Region in which your Amazon Kinesis Data Stream is located.")
    @Once
    private String region;

    @Option(name = {"--stream-large-record-strategy"}, description = "Strategy for dealing with records to be sent to Amazon Kinesis that are larger than 1 MB.")
    @Once
    @AllowedEnumValues(LargeStreamRecordHandlingStrategy.class)
    private LargeStreamRecordHandlingStrategy largeStreamRecordHandlingStrategy = LargeStreamRecordHandlingStrategy.splitAndShred;

    @Option(name = {"--disable-stream-aggregation"}, description = "Disable aggregation of Kinesis Data Stream records).")
    @Once
    private boolean disableAggregation = false;

    @Option(name = {"--stream-role-arn"}, description = "Optional. Assume specified role for upload to Kinesis stream.")
    @Once
    private String streamRoleArn = null;

    @Option(name = {"--stream-role-session-name"}, description = "Optional. To be used with '--stream-role-arn'. Use specified session name when assuming stream role.")
    @Once
    private String streamRoleSessionName = "Neptune-Export";

    @Option(name = {"--stream-role-external-id"}, description = "Optional. To be used with '--stream-role-arn'. Use specified external id when assuming stream role.")
    @Once
    private String streamRoleExternalId = null;

    @Option(name = {"--export-id"}, description = "Export ID")
    @Once
    private String exportId = UUID.randomUUID().toString().replace("-", "");

    @Option(name = {"--partition-directories"}, description = "Partition directory path (e.g. 'year=2021/month=07/day=21').")
    @Once
    private String partitionDirectories = "";

    public AbstractTargetModule() {}
    public AbstractTargetModule(Target target) {
        this.output =  target;
    }

    public File getDirectory() {
        return directory;
    }

    public String getTag() {
        return tag;
    }

    public Target getOutput() {
        return output;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getRegion() {
        return region;
    }

    public LargeStreamRecordHandlingStrategy getLargeStreamRecordHandlingStrategy() {
        return largeStreamRecordHandlingStrategy;
    }

    public boolean isEnableAggregation() {
        return !disableAggregation;
    }

    public Directories createDirectories() throws IOException {
        return Directories.createFor(directoryStructure(), directory, exportId, tag, partitionDirectories );
    }

    public Directories createDirectories(DirectoryStructure directoryStructure) throws IOException {
        return Directories.createFor(directoryStructure, directory, exportId, tag, partitionDirectories );
    }

    @Override
    public void writeReturnValue(String value){
        output.writeReturnValue(value);
    }

    @Override
    public void writeMessage(String value) {
        output.writeMessage(value);
    }

    protected abstract DirectoryStructure directoryStructure();

    public AWSCredentialsProvider getCredentialsProvider() {
        if (StringUtils.isEmpty(streamRoleArn)) {
            return credentialProfileModule.getCredentialsProvider();
        }
        return getSTSAssumeRoleCredentialsProvider(streamRoleArn, streamRoleSessionName, streamRoleExternalId, credentialProfileModule.getCredentialsProvider(), region);
    }

}
