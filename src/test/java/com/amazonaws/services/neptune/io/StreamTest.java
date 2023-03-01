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

package com.amazonaws.services.neptune.io;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class StreamTest {

    @Test
    public void callbackShouldCallFlushOnProducerWhenSuccessful() {
        KinesisProducer producer = mock(KinesisProducer.class);
        doNothing().when(producer).flush();

        boolean success = true;

        Stream.UserRecordCallback userRecordCallback = new Stream.UserRecordCallback(producer);
        userRecordCallback.onSuccess(new UserRecordResult(Collections.emptyList(), "seq-no", "shard-id", success));

        verify(producer, times(1)).flush();
    }

    @Test
    public void callbackShouldNotCallFlushOnProducerWhenUnsuccessful() {
        KinesisProducer producer = mock(KinesisProducer.class);
        doNothing().when(producer).flush();

        boolean success = false;

        Stream.UserRecordCallback userRecordCallback = new Stream.UserRecordCallback(producer);
        userRecordCallback.onSuccess(new UserRecordResult(Collections.emptyList(), "seq-no", "shard-id", success));

        verify(producer, times(0)).flush();
    }

    @Test
    public void callbackShouldNotCallFlushOnProducerWhenFailed() {
        KinesisProducer producer = mock(KinesisProducer.class);
        doNothing().when(producer).flush();

        boolean success = false;

        Stream.UserRecordCallback userRecordCallback = new Stream.UserRecordCallback(producer);
        userRecordCallback.onFailure(new IllegalStateException());

        verify(producer, times(0)).flush();
    }

}