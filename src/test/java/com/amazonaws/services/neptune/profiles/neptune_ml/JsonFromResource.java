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

package com.amazonaws.services.neptune.profiles.neptune_ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class JsonFromResource {

    public static JsonNode get(String filename, Class<?> testClass) throws IOException {
        String path = String.format("%s/%s", testClass.getSimpleName(), filename);

        ClassLoader classLoader = testClass.getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(path)).getFile());

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(file);
    }
}
