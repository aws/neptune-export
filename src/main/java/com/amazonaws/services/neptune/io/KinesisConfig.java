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

import com.amazonaws.services.kinesis.producer.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KinesisConfig {

    private final Stream stream;
    private static final Logger logger = LoggerFactory.getLogger(KinesisConfig.class);

    public KinesisConfig(String streamName, String region, LargeStreamRecordHandlingStrategy largeStreamRecordHandlingStrategy, boolean enableAggregation) {

        if (StringUtils.isNotEmpty(region) && StringUtils.isNotEmpty(streamName)) {
            logger.trace("Constructing new KinesisConfig for stream name: {}, in region: {}, with LargeStreamRecordHandlingStrategy: {} and AggregationEnabled={}",
                    streamName, region, largeStreamRecordHandlingStrategy, enableAggregation);

            this.stream = new Stream(
                    new KinesisProducer(new KinesisProducerConfiguration()
                            .setAggregationEnabled(enableAggregation)
                            .setRegion(region)
                            .setRateLimit(100)
                            .setConnectTimeout(12000)
                            .setRequestTimeout(12000)
                            .setRecordTtl(Integer.MAX_VALUE)),
                    streamName,
                    largeStreamRecordHandlingStrategy);
        }
        else {
            this.stream = null;
        }
    }

    public Stream stream() {
        if (stream == null) {
            throw new IllegalArgumentException("You must supply an AWS Region and Amazon Kinesis Data Stream name");
        }
        return stream;
    }
}
