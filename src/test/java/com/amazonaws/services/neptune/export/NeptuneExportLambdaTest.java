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

package com.amazonaws.services.neptune.export;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NeptuneExportLambdaTest {

    private Context context;

    private LambdaLogger logger;

    private ByteArrayOutputStream outputStreamCaptor;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Before
    public void setup() {
        context = mock(Context.class);
        logger = mock(LambdaLogger.class);
        when(context.getLogger()).thenReturn(logger);

        outputStreamCaptor = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outputStreamCaptor));
    }

    @Test
    public void shouldRejectIllegalArguments() throws IOException {
        exit.expectSystemExitWithStatus(-1);
        exit.checkAssertionAfterwards(new ErrorChecker("Found unexpected parameters:"));

        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, "us-west-2");

        NeptuneExportLambda lambda = new NeptuneExportLambda();
        String input = "{" +
                "\"params\": {\"endpoint\" : \"fakeEndpoint\"," +
                "\"illegalArgument\": \"test\"}}";

        lambda.handleRequest(new StringInputStream(input), mock(OutputStream.class), context);
    }

    @Test
    public void shouldRejectMissingRequiredArguments() throws IOException {
        exit.expectSystemExitWithStatus(-1);
        exit.checkAssertionAfterwards(new ErrorChecker("One/more of the following options must be specified: -e, --endpoint, --cluster-id, --cluster, --clusterid"));

        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, "us-west-2");

        NeptuneExportLambda lambda = new NeptuneExportLambda();
        String input = "{\"command\": \"export-pg\", \"params\": {}}";

        lambda.handleRequest(new StringInputStream(input), mock(OutputStream.class), context);
    }

    @Test
    public void shouldRejectMalformedJSON() throws IOException {
        NeptuneExportLambda lambda = new NeptuneExportLambda();
        String input = "{[}";

        assertThrows(JsonParseException.class,
                () -> lambda.handleRequest(new StringInputStream(input), mock(OutputStream.class), context));
    }

    private class ErrorChecker implements Assertion {
        private String expectedMessage;
        ErrorChecker(String expectedMessage) {
            this.expectedMessage = expectedMessage;
        }
        @Override
        public void checkAssertion() throws Exception {
            String capturedErrors = new String(outputStreamCaptor.toByteArray());
            assertTrue(capturedErrors.contains(expectedMessage));
        }
    }
}
