/*
Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.propertygraph;

import com.amazonaws.services.neptune.cluster.ConcurrencyConfig;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class NamedQueriesCollectionTest {
    @Test
    public void splitQueriesShouldSplitAllNamedQueries() {
        Collection<NamedQueries> namedQueries = new ArrayList<>();
        namedQueries.add(mock(NamedQueries.class));
        namedQueries.add(mock(NamedQueries.class));
        namedQueries.add(mock(NamedQueries.class));
        NamedQueriesCollection namedQueriesCollection = new NamedQueriesCollection(namedQueries);

        LazyQueriesRangeFactoryProvider rangeFactoryProvider = NamedQueriesTest.initRangeFactoryProvider(mock(RangeConfig.class), mock(ConcurrencyConfig.class));

        for(NamedQueries nq: namedQueries) {
            verify(nq, times(0)).split(any());
        }

        namedQueriesCollection.splitQueries(rangeFactoryProvider);

        for(NamedQueries nq: namedQueries) {
            verify(nq, times(1)).split(rangeFactoryProvider);
            verify(nq, times(1)).split(any());
            verifyNoMoreInteractions(nq);
        }
    }
}
