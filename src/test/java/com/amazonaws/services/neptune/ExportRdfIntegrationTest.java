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
