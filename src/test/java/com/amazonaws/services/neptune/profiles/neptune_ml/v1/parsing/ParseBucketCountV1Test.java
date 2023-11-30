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

package com.amazonaws.services.neptune.profiles.neptune_ml.v1.parsing;

import com.amazonaws.services.neptune.profiles.neptune_ml.common.parsing.ParsingContext;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ParseBucketCountV1Test {

    @Test
    public void throwsErrorIfInvalidBucketCount() {

        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("num_buckets", "one");

        try {
            new ParseBucketCountV1(json, new ParsingContext("context")).parseBucketCount();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Error parsing 'num_buckets' field for context. Expected an integer.", e.getMessage());
        }
    }

}