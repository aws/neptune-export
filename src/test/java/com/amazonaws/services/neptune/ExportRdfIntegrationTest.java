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

package com.amazonaws.services.neptune;

import com.amazonaws.services.neptune.export.NeptuneExportRunner;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class ExportRdfIntegrationTest extends AbstractExportIntegrationTest{

    @Test
    public void testExportRdf() {
        final String[] command = {"export-rdf", "-e", neptuneEndpoint, "-d", outputDir.getPath()};
        final NeptuneExportRunner runner = new NeptuneExportRunner(command);
        runner.run();

        final File resultDir = outputDir.listFiles()[0];

        assertTrue("Returned statements don't match expected", areStatementsEqual("src/test/resources/IntegrationTest/testExportRdf/statements/statements.ttl", resultDir+"/statements/statements.ttl"));
    }

    private boolean areStatementsEqual(final String expected, final String actual) {
        final ArrayList expectedStatements = new ArrayList();
        final ArrayList actualStatements = new ArrayList();
        final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
        rdfParser.setRDFHandler(new StatementCollector(expectedStatements));
        try {
            rdfParser.parse(new FileInputStream(expected));
        }
        catch (Exception e) {

        }
        rdfParser.setRDFHandler(new StatementCollector(actualStatements));
        try {
            rdfParser.parse(new FileInputStream(actual));
        }
        catch (Exception e) {

        }

        return expectedStatements.containsAll(actualStatements) && actualStatements.containsAll(expectedStatements);
    }

}
