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

package com.amazonaws.services.neptune.cluster;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class GetLastEventIdTest {

    @Test
    public void shouldReturnIntegerMaxValueForEngineVersions1041AndBelow(){

        String expectedValue = String.valueOf(Integer.MAX_VALUE);

        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.1.0"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.1.1"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.1.2"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.2.0"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.2.1"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.2.2"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.3.0"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.4.0"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.4.1"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.4.2"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.5.0"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.5.1"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.1.0.0"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.1.1.0"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.2.0.0"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.2.0.1"));

    }

    @Test
    public void shouldReturnLongMaxValueForEngineVersions1041AndBelow(){

        String expectedValue = String.valueOf(Long.MAX_VALUE);

        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.1.0"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.1.1"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.1.2"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.2.0"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.2.1"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.2.2"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.3.0"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.4.0"));
        Assert.assertNotEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.4.1"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.4.2"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.5.0"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.0.5.1"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.1.0.0"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.1.1.0"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.2.0.0"));
        Assert.assertEquals(expectedValue, GetLastEventId.MaxCommitNumValueForEngine("1.2.0.1"));

    }

}