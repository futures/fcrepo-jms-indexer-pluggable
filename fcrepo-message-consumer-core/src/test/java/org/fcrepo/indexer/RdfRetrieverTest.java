/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.indexer;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author ajs6f
 */
public class RdfRetrieverTest {

    private RdfRetriever testRetriever;

    @Mock
    private HttpClient mockClient;

    @Mock
    private HttpResponse mockResponse;

    @Mock
    private HttpEntity mockEntity;

    @Mock
    private StatusLine mockStatusLine;

    private final Triple testTriple =
            create(createURI("info:test"), createURI("info:test"),
                    createURI("info:test"));

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        when(mockClient.execute(any(HttpUriRequest.class))).thenReturn(
                mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    }

    @Test
    public void testSimpleRetrieval() throws Exception {
        final String testId = "testSimpleRetrieval";
        final Model input = createDefaultModel();
        input.add(input.asStatement(testTriple));
        when(mockStatusLine.getStatusCode()).thenReturn(SC_OK);
        try (StringWriter w = new StringWriter()) {
            input.write(w, "N3");
            try (
                InputStream rdf =
                    new ByteArrayInputStream(w.toString().getBytes())) {
                when(mockEntity.getContent()).thenReturn(rdf);
            }
        }

        testRetriever = new RdfRetriever(new URI(testId), mockClient);
        final Model result = testRetriever.get();
        assertTrue("Didn't find our test triple!", result.contains(result
                .asStatement(testTriple)));
    }

    @Test(expected = RuntimeException.class)
    public void testFailedRetrieval() throws URISyntaxException {
        final String testId = "testFailedRetrieval";
        when(mockStatusLine.getStatusCode()).thenReturn(SC_NOT_FOUND);
        new RdfRetriever(new URI(testId), mockClient).get();
    }

    @Test(expected = RuntimeException.class)
    public void testOtherFailedRetrieval() throws Exception {
        final String testId = "testFailedRetrieval";
        when(mockStatusLine.getStatusCode()).thenReturn(SC_OK);
        when(mockEntity.getContent()).thenThrow(new IOException());
        new RdfRetriever(new URI(testId), mockClient).get();
    }

    @Test(expected = RuntimeException.class)
    public void testYetOtherFailedRetrieval() throws Exception {
        final String testId = "testFailedRetrieval";
        reset(mockClient);
        when(mockClient.execute(any(HttpUriRequest.class))).thenThrow(
                new IOException());
        when(mockStatusLine.getStatusCode()).thenReturn(SC_OK);
        when(mockEntity.getContent()).thenThrow(new IOException());
        new RdfRetriever(new URI(testId), mockClient).get();
    }

    @Test
    public void testAuthRetrieval() throws Exception {
        final String testId = "testAuthRetrieval";
        final Model input = createDefaultModel();
        input.add(input.asStatement(testTriple));
        when(mockStatusLine.getStatusCode()).thenReturn(SC_OK);
        try (StringWriter w = new StringWriter()) {
            input.write(w, "N3");
            try (
                InputStream rdf =
                    new ByteArrayInputStream(w.toString().getBytes())) {
                when(mockEntity.getContent()).thenReturn(rdf);
            }
        }
        new RdfRetriever(new URI(testId), mockClient).get();
    }

    @Test(expected = RuntimeException.class)
    public void testAuthForbiddenRetrieval() throws URISyntaxException {
        final String testId = "testAuthForbiddenRetrieval";
        when(mockStatusLine.getStatusCode()).thenReturn(SC_FORBIDDEN);
        new RdfRetriever(new URI(testId), mockClient).get();
    }
}
