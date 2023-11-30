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

package com.amazonaws.services.neptune.profiles.neptune_ml.v2.config;

import com.amazonaws.services.neptune.profiles.neptune_ml.NeptuneMLSourceDataModel;
import com.amazonaws.services.neptune.profiles.neptune_ml.JsonFromResource;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.*;

public class TrainingDataWriterConfigV2Test {

    @Test
    public void shouldCreateSingleConfig() throws IOException {
        JsonNode json = JsonFromResource.get("t1.json", getClass());

        Collection<TrainingDataWriterConfigV2> config = TrainingDataWriterConfigV2.fromJson(json.path("neptune_ml"), NeptuneMLSourceDataModel.PropertyGraph);

        assertEquals(1, config.size());
    }

    @Test
    public void shouldConfigForEachElementInArray() throws IOException {
        JsonNode json = JsonFromResource.get("t2.json", getClass());

        Collection<TrainingDataWriterConfigV2> config = TrainingDataWriterConfigV2.fromJson(json.path("neptune_ml"), NeptuneMLSourceDataModel.PropertyGraph);

        assertEquals(3, config.size());
    }

    @Test
    public void shouldConfigForEachElementInJobsArray() throws IOException {
        JsonNode json = JsonFromResource.get("t3.json", getClass());

        Collection<TrainingDataWriterConfigV2> config = TrainingDataWriterConfigV2.fromJson(json.path("neptune_ml"), NeptuneMLSourceDataModel.PropertyGraph);

        assertEquals(5, config.size());
    }

}