package com.amazonaws.services.neptune.rdf.io;

import com.amazonaws.neptune.auth.NeptuneSigV4SignerException;
import com.amazonaws.services.neptune.cluster.ConnectionConfig;
import org.apache.http.Header;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NeptuneExportSparqlRepositoryTest {

    @Test
    public void ShouldGetEmptyErrorMessageFromNewRepository() throws NeptuneSigV4SignerException {
        ConnectionConfig mockedConfig = mock(ConnectionConfig.class);
        NeptuneExportSparqlRepository repo = new NeptuneExportSparqlRepository("test", null, null, mockedConfig);
        assertEquals("", repo.getErrorMessageFromTrailers());
    }

    @Test
    public void ShouldGetTrailerErrorMessagesFromChunkedStream() throws NeptuneSigV4SignerException {
        ConnectionConfig mockedConfig = mock(ConnectionConfig.class);
        NeptuneExportSparqlRepository repo = new NeptuneExportSparqlRepository("test", null, null, mockedConfig);

        ChunkedInputStream mockedStream = mock(ChunkedInputStream.class);
        when(mockedStream.getFooters()).thenReturn(new Header[]{new BasicHeader("name", "value")});

        HttpContext mockedContext = mock(HttpContext.class);
        when(mockedContext.getAttribute("raw-response-inputstream")).thenReturn(mockedStream);

        repo.setLastContext(mockedContext);

        assertEquals("name: value\n", repo.getErrorMessageFromTrailers());
    }

    @Test
    public void ShouldGetTrailerErrorMessagesFromEofSensorInputStream() throws NeptuneSigV4SignerException {
        ConnectionConfig mockedConfig = mock(ConnectionConfig.class);
        NeptuneExportSparqlRepository repo = new NeptuneExportSparqlRepository("test", null, null, mockedConfig);

        ChunkedInputStream mockedStream = mock(ChunkedInputStream.class);
        when(mockedStream.getFooters()).thenReturn(new Header[]{new BasicHeader("name", "value")});

        EofSensorInputStream eofSensorInputStream = new EofSensorInputStream(mockedStream, null);

        HttpContext mockedContext = mock(HttpContext.class);
        when(mockedContext.getAttribute("raw-response-inputstream")).thenReturn(eofSensorInputStream);

        repo.setLastContext(mockedContext);

        assertEquals("name: value\n", repo.getErrorMessageFromTrailers());
    }
}
