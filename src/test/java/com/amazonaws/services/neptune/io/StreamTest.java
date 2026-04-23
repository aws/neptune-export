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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import software.amazon.kinesis.producer.KinesisProducer;
import software.amazon.kinesis.producer.UserRecordResult;

import java.nio.ByteBuffer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StreamTest {

    private KinesisProducer kinesisProducer;
    private Stream stream;

    @Before
    public void setUp() {
        kinesisProducer = mock(KinesisProducer.class);
        when(kinesisProducer.getOutstandingRecordsCount()).thenReturn(0);
        UserRecordResult result = mock(UserRecordResult.class);
        when(result.isSuccessful()).thenReturn(true);
        ListenableFuture<UserRecordResult> future = Futures.immediateFuture(result);
        when(kinesisProducer.addUserRecord(anyString(), anyString(), any(ByteBuffer.class))).thenReturn(future);
        stream = new Stream(kinesisProducer, "test-stream", LargeStreamRecordHandlingStrategy.dropAll);
    }

    @Test
    public void shouldPublishRecordToKinesis() {
        stream.publish("{\"key\":\"value\"}");

        verify(kinesisProducer).addUserRecord(eq("test-stream"), anyString(), any(ByteBuffer.class));
    }

    @Test
    public void shouldNotPublishEmptyRecord() {
        stream.publish("");

        verify(kinesisProducer, never()).addUserRecord(anyString(), anyString(), any(ByteBuffer.class));
    }

    @Test
    public void shouldNotPublishMinimalJsonArray() {
        stream.publish("[]");

        verify(kinesisProducer, never()).addUserRecord(anyString(), anyString(), any(ByteBuffer.class));
    }

    @Test
    public void shouldFlushRecords() {
        stream.publish("{\"key\":\"value\"}");
        stream.flushRecords();

        verify(kinesisProducer).flushSync();
    }

    @Test
    public void shouldDropOversizedRecordWithDropAllStrategy() {
        // Build a record larger than 1MB
        StringBuilder sb = new StringBuilder();
        sb.append("{\"key\":\"");
        for (int i = 0; i < 1_100_000; i++) {
            sb.append("x");
        }
        sb.append("\"}");

        stream.publish(sb.toString());

        // Record exceeds 1MB and strategy is dropAll (no split), so it should be dropped
        verify(kinesisProducer, never()).addUserRecord(anyString(), anyString(), any(ByteBuffer.class));
    }
}
