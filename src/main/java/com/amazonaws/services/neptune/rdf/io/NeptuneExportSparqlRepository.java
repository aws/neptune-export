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

package com.amazonaws.services.neptune.rdf.io;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.neptune.auth.NeptuneApacheHttpSigV4Signer;
import com.amazonaws.neptune.auth.NeptuneSigV4Signer;
import com.amazonaws.neptune.auth.NeptuneSigV4SignerException;
import com.amazonaws.services.neptune.cluster.ConnectionConfig;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.protocol.HttpContext;
import org.eclipse.rdf4j.http.client.util.HttpClientBuilders;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class NeptuneExportSparqlRepository extends SPARQLRepository {
    private final String regionName;
    private final AWSCredentialsProvider awsCredentialsProvider;
    private final ConnectionConfig config;
    private NeptuneSigV4Signer<HttpUriRequest> v4Signer;

    private HttpContext lastContext;

    public NeptuneExportSparqlRepository(String endpointUrl, AWSCredentialsProvider awsCredentialsProvider, String regionName, ConnectionConfig config) throws NeptuneSigV4SignerException {
        super(getSparqlEndpoint(endpointUrl));
        if (config == null) {
            throw new IllegalArgumentException("ConnectionConfig is required to be non-null");
        }
        this.config = config;
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.regionName = regionName;
        this.initAuthenticatingHttpClient();

        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("te", "trailers"); //Asks Neptune to send trailing headers which may contain error messages
        this.setAdditionalHttpHeaders(additionalHeaders);
    }

    protected void initAuthenticatingHttpClient() throws NeptuneSigV4SignerException {

        HttpClientBuilder httpClientBuilder = config.useSsl() ?
                HttpClientBuilders.getSSLTrustAllHttpClientBuilder() :
                HttpClientBuilder.create();

        httpClientBuilder.addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
            lastContext = context;
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                context.setAttribute("raw-response-inputstream", entity.getContent());
            }
        });

        if (config.useIamAuth()) {
            v4Signer = new NeptuneApacheHttpSigV4Signer(regionName, awsCredentialsProvider);
            HttpClient v4SigningClient = httpClientBuilder.addInterceptorLast((HttpRequestInterceptor) (req, ctx) -> {
                if (req instanceof HttpUriRequest) {
                    HttpUriRequest httpUriReq = (HttpUriRequest) req;

                    try {
                        v4Signer.signRequest(httpUriReq);
                    } catch (NeptuneSigV4SignerException var5) {
                        throw new HttpException("Problem signing the request: ", var5);
                    }
                } else {
                    throw new HttpException("Not an HttpUriRequest");
                }
            }).build();
            setHttpClient(v4SigningClient);
        } else {
            setHttpClient(httpClientBuilder.build());
        }
    }

    private static String getSparqlEndpoint(String endpointUrl) {
        return endpointUrl + "/sparql";
    }

    /**
     * Attempts to extract error messages from trailing headers from the most recent response received by 'repository'.
     * If no trailers are found an empty String is returned.
     */
    public String getErrorMessageFromTrailers() {
        if (this.lastContext == null) {
            return "";
        }
        InputStream responseInStream = (InputStream) this.lastContext.getAttribute("raw-response-inputstream");
        ChunkedInputStream chunkedInStream;
        if (responseInStream instanceof ChunkedInputStream) {
            chunkedInStream = (ChunkedInputStream) responseInStream;
        }
        else if (responseInStream instanceof EofSensorInputStream) {
            // HTTPClient 4.5.13 provides no methods for accessing trailers from a wrapped stream requiring the use of
            // reflection to break encapsulation. This bug is being tracked in https://issues.apache.org/jira/browse/HTTPCLIENT-2263.
            try {
                Method getWrappedStream = EofSensorInputStream.class.getDeclaredMethod("getWrappedStream");
                getWrappedStream.setAccessible(true);
                chunkedInStream = (ChunkedInputStream) getWrappedStream.invoke(responseInStream);
                getWrappedStream.setAccessible(false);
            } catch (Exception e) {
                return "";
            }
        }
        else {
            return "";
        }
        Header[] trailers = chunkedInStream.getFooters();
        StringBuilder messageBuilder = new StringBuilder();
        for (Header trailer : trailers) {
            try {
                messageBuilder.append(URLDecoder.decode(trailer.toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                messageBuilder.append(trailer);
            }
            messageBuilder.append('\n');
        }
        return messageBuilder.toString();
    }

    protected void setLastContext(HttpContext context) {
        this.lastContext = context;
    }

}
