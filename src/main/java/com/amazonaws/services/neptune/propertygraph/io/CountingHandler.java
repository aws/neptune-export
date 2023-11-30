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

package com.amazonaws.services.neptune.propertygraph.io;

import java.io.IOException;

class CountingHandler<T> implements GraphElementHandler<T> {

    private final GraphElementHandler<T> parent;
    private long counter = 0;

    CountingHandler(GraphElementHandler<T> parent) {
        this.parent = parent;
    }

    @Override
    public void handle(T input, boolean allowTokens) throws IOException {
        parent.handle(input, allowTokens);
        counter++;
    }

    long numberProcessed() {
        return counter;
    }

    @Override
    public void close() throws Exception {
        parent.close();
    }
}
