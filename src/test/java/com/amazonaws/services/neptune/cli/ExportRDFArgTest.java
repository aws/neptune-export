/*
Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.cli;

import com.amazonaws.services.neptune.ExportRdfGraph;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static com.github.rvesse.airline.SingleCommand.singleCommand;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class ExportRDFArgTest {

    @Test
    public void cannotUseQueryScopeWithoutSparql() {
        RdfExportScopeModule withSparql = getScopeModule(singleCommand(ExportRdfGraph.class).parse(
                new String[]{"-e", "", "-d", "", "--rdf-export-scope", "query", "--sparql", "testquery"}));
        RdfExportScopeModule withoutSparql = getScopeModule(singleCommand(ExportRdfGraph.class).parse(
                new String[]{"-e", "", "-d", "", "--rdf-export-scope", "query"}));

        assertNotNull(withSparql.createJob(null, null));
        assertThrows(IllegalStateException.class, ()->{withoutSparql.createJob(null, null);});
    }

    @Test
    public void cannotUseInvalidNamedGraphURI() {
        RdfExportScopeModule validURI = getScopeModule(singleCommand(ExportRdfGraph.class).parse(
                new String[]{"-e", "", "-d", "", "--named-graph", "http://example.com"}));
        RdfExportScopeModule whitespace = getScopeModule(singleCommand(ExportRdfGraph.class).parse(
                new String[]{"-e", "", "-d", "", "--named-graph", " "}));
        RdfExportScopeModule illegalChar = getScopeModule(singleCommand(ExportRdfGraph.class).parse(
                new String[]{"-e", "", "-d", "", "--named-graph", "http://example.com/^"}));
        RdfExportScopeModule noProtocol = getScopeModule(singleCommand(ExportRdfGraph.class).parse(
                new String[]{"-e", "", "-d", "", "--named-graph", "example.com"}));

        assertNotNull(validURI.createJob(null, null));
        assertThrows(IllegalArgumentException.class, ()->{whitespace.createJob(null, null);});
        assertThrows(IllegalArgumentException.class, ()->{illegalChar.createJob(null, null);});
        assertThrows(IllegalArgumentException.class, ()->{noProtocol.createJob(null, null);});
    }

    private static RdfExportScopeModule getScopeModule(ExportRdfGraph job) {
        Field scopeModuleField;
        try {
            scopeModuleField = ExportRdfGraph.class.getDeclaredField("exportScope");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        scopeModuleField.setAccessible(true);

        try {
            return (RdfExportScopeModule) scopeModuleField.get(job);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
