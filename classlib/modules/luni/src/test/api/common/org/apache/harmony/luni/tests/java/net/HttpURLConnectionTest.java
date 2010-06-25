/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.tests.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ResponseCache;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import tests.support.Support_Configuration;
import tests.support.Support_Jetty;

public class HttpURLConnectionTest extends junit.framework.TestCase {

    URL url;

    HttpURLConnection uc;

    private boolean isGetCalled;

    private boolean isPutCalled;

    private boolean isCacheWriteCalled;

    private boolean isAbortCalled;

    private Map<String, List<String>> mockHeaderMap;

    private InputStream mockIs = new MockInputStream();
    
    private static int port;

    static {
        // run-once set up
        try {
            port = Support_Jetty.startDefaultHttpServer();
        } catch (Exception e) {
            fail("Exception during setup jetty : " + e.getMessage());
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getResponseCode()
     */
    public void test_getResponseCode() {
        try {
            assertEquals("Wrong response", 200, uc.getResponseCode());
        } catch (IOException e) {
            fail("Unexpected exception : " + e.getMessage());
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getResponseMessage()
     */
    public void test_getResponseMessage() {
        try {
            assertTrue("Wrong response: " + uc.getResponseMessage(), uc
                    .getResponseMessage().equals("OK"));
        } catch (IOException e) {
            fail("Unexpected exception : " + e.getMessage());
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getHeaderFields()
     */
    public void test_getHeaderFields() {
        try {
            uc.getInputStream();
        } catch (IOException e) {
            fail();
        }
        Map headers = uc.getHeaderFields();
        List list = (List) headers.get("Content-Length");
        if (list == null) {
            list = (List) headers.get("content-length");
        }
        assertNotNull(list);

        // content-length should always appear
        String contentLength = (String) list.get(0);
        assertNotNull(contentLength);

        // there should be at least 2 headers
        assertTrue(headers.size() > 1);

        try {
            // the map should be unmodifiable
            headers.put("hi", "bye");
            fail();
        } catch (UnsupportedOperationException e) {
        }

        try {
            // the list should be unmodifiable
            list.set(0, "whatever");
            fail();
        } catch (UnsupportedOperationException e) {
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getRequestProperties()
     */
    public void test_getRequestProperties() {
        uc.setRequestProperty("whatever", "you like");
        Map headers = uc.getRequestProperties();

        List newHeader = (List) headers.get("whatever");
        assertNotNull(newHeader);

        assertEquals("you like", newHeader.get(0));

        try {
            // the map should be unmodifiable
            headers.put("hi", "bye");
            fail();
        } catch (UnsupportedOperationException e) {
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getRequestProperty(String)
     */
    public void test_getRequestPropertyLjava_lang_String_BeforeConnected()
            throws MalformedURLException, IOException {
        uc.setRequestProperty("whatever", "you like"); //$NON-NLS-1$//$NON-NLS-2$
        String res = uc.getRequestProperty("whatever"); //$NON-NLS-1$
        assertEquals("you like", res); //$NON-NLS-1$

        uc.setRequestProperty("", "you like"); //$NON-NLS-1$//$NON-NLS-2$
        res = uc.getRequestProperty(""); //$NON-NLS-1$
        assertEquals("you like", res); //$NON-NLS-1$

        uc.setRequestProperty("", null); //$NON-NLS-1$
        res = uc.getRequestProperty(""); //$NON-NLS-1$
        assertEquals(null, res);
        try {
            uc.setRequestProperty(null, "you like"); //$NON-NLS-1$
            fail("Should throw NullPointerException"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getRequestProperty(String)
     */
    public void test_getRequestPropertyLjava_lang_String_AfterConnected()
            throws IOException {
        uc.connect();
        try {
            uc.setRequestProperty("whatever", "you like"); //$NON-NLS-1$//$NON-NLS-2$
            fail("Should throw IllegalStateException"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
            // expected
        }
        try {
            uc.setRequestProperty(null, "you like"); //$NON-NLS-1$
            fail("Should throw IllegalStateException"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
            // expected
        }
        String res = uc.getRequestProperty("whatever"); //$NON-NLS-1$
        assertEquals(null, res);
        res = uc.getRequestProperty(null);
        assertEquals(null, res);
        try {
            uc.getRequestProperties();
            fail("Should throw IllegalStateException"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.HttpURLConnection#setFixedLengthStreamingMode_I()
     */
    public void test_setFixedLengthStreamingModeI() throws Exception {
        try {
            uc.setFixedLengthStreamingMode(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        uc.setFixedLengthStreamingMode(0);
        uc.setFixedLengthStreamingMode(1);
        try {
            uc.setChunkedStreamingMode(1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        uc.connect();
        try {
            uc.setFixedLengthStreamingMode(-1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        try {
            uc.setChunkedStreamingMode(-1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        MockHttpConnection mock = new MockHttpConnection(url);
        assertEquals(-1, mock.getFixedLength());
        mock.setFixedLengthStreamingMode(0);
        assertEquals(0, mock.getFixedLength());
        mock.setFixedLengthStreamingMode(1);
        assertEquals(1, mock.getFixedLength());
        mock.setFixedLengthStreamingMode(0);
        assertEquals(0, mock.getFixedLength());
    }

    /**
     * @tests java.net.HttpURLConnection#setChunkedStreamingMode_I()
     */
    public void test_setChunkedStreamingModeI() throws Exception {
        uc.setChunkedStreamingMode(0);
        uc.setChunkedStreamingMode(-1);
        uc.setChunkedStreamingMode(-2);

        try {
            uc.setFixedLengthStreamingMode(-1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        try {
            uc.setFixedLengthStreamingMode(1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        uc.connect();
        try {
            uc.setFixedLengthStreamingMode(-1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        try {
            uc.setChunkedStreamingMode(1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        MockHttpConnection mock = new MockHttpConnection(url);
        assertEquals(-1, mock.getChunkLength());
        mock.setChunkedStreamingMode(-1);
        int defaultChunk = mock.getChunkLength();
        assertTrue(defaultChunk > 0);
        mock.setChunkedStreamingMode(0);
        assertEquals(mock.getChunkLength(), defaultChunk);
        mock.setChunkedStreamingMode(1);
        assertEquals(1, mock.getChunkLength());
    }

    /**
     * @tests java.net.HttpURLConnection#setFixedLengthStreamingMode_I()
     */
    public void test_setFixedLengthStreamingModeI_effect() throws Exception {
        String posted = "just a test";
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url
                .openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode(posted.length() - 1);
        assertNull(conn.getRequestProperty("Content-length"));
        conn.setRequestProperty("Content-length", String.valueOf(posted
                .length()));
        assertEquals(String.valueOf(posted.length()), conn
                .getRequestProperty("Content-length"));
        OutputStream out = conn.getOutputStream();
        try {
            out.write(posted.getBytes());
            fail("should throw IOException");
        } catch (IOException e) {
            // correct, too many bytes written
        }
        try {
            out.close();
            fail("should throw IOException");
        } catch (IOException e) {
            // correct, too many bytes written
        }
    }
    
    /**
     * When an OutputStream of HtttpURLConnection is closed with below
     * situation: fixed-length mod is disable and the content-length of the
     * HtttpURLConnection is larger than 0, it should not throw IOExeption which
     * indicates there are more bytes need be written into the underlying
     * Socket.
     * 
     * @throws IOException
     */
    public void test_closeWithFixedLengthDisableMod() throws IOException {
        String posted = "just a test";
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url
                .openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Length", "" + (888));

        OutputStream out = conn.getOutputStream();
        out.write(posted.getBytes());
        //should not throw IOExeption
        out.close();
    }
    
    /**
     * When write bytes to HttpOutputSteam, only if fixed mode is true and the write number of bytes is
     * greater than the limit of HttpOutputStream, the write method will throw IOException
     * @throws IOException
     */
    public void test_writeWithFixedLengthDisableMode() throws IOException {
        String bigString = "big String:/modules/luni/src/main/java/org/apache/harmony/luni/internal/net/www/protocol/http/HttpURLConnectionImpl.java b/modules/luni/src/main/java/org/apache/harmony/luni/internal/net/www/protocol/http/HttpURLConnectionImpl.java";

        java.net.HttpURLConnection httpURLConnection = (HttpURLConnection) url
                .openConnection();

        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Length", "" + (168));

        OutputStream out = httpURLConnection.getOutputStream();
        //should not throw IOExeption
        out.write(bigString.getBytes());
    }

    /**
     * @tests java.net.HttpURLConnection#setChunkedStreamingMode_I()
     */
    public void test_setChunkedStreamingModeI_effect() throws Exception {
        String posted = "just a test";
        // for test, use half length of the string
        int chunkSize = posted.length() / 2;
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url
                .openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setChunkedStreamingMode(chunkSize);
        assertNull(conn.getRequestProperty("Transfer-Encoding"));
        // does not take effect
        conn.setRequestProperty("Content-length", String.valueOf(posted
                .length() - 1));
        assertEquals(conn.getRequestProperty("Content-length"), String
                .valueOf(posted.length() - 1));
        OutputStream out = conn.getOutputStream();
        // no error occurs
        out.write(posted.getBytes());
        out.close();
        // no assert here, pass if no exception thrown
        assertTrue(conn.getResponseCode() > 0);
    }

    public void test_getOutputStream_afterConnection() throws Exception {
        uc.setDoOutput(true);
        uc.connect();
        assertNotNull(uc.getOutputStream());
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetInputStream() and Connect()
     */
    public void test_UseCache_HttpURLConnection_Connect_GetInputStream()
            throws Exception {
        // set cache before URLConnection created, or it does not take effect
        ResponseCache rc = new MockNonCachedResponseCache();
        ResponseCache.setDefault(rc);
        uc = (HttpURLConnection) url.openConnection();
        assertFalse(isGetCalled);
        uc.setUseCaches(true);
        uc.setDoOutput(true);
        uc.connect();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        InputStream is = uc.getInputStream();
        assertTrue(isPutCalled);
        is.close();
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetOutputStream() and Connect()
     */
    public void test_UseCache_HttpURLConnection_Connect_GetOutputStream()
            throws Exception {
        // set cache before URLConnection created, or it does not take effect
        ResponseCache rc = new MockNonCachedResponseCache();
        ResponseCache.setDefault(rc);
        uc.setUseCaches(true);
        URLConnection uc = url.openConnection();
        uc.setDoOutput(true);
        assertFalse(isGetCalled);
        uc.connect();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        OutputStream os = uc.getOutputStream();
        assertFalse(isPutCalled);
        os.close();
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetOutputStream()
     */
    public void test_UseCache_HttpURLConnection_GetOutputStream()
            throws Exception {
        // set cache before URLConnection created, or it does not take effect
        ResponseCache rc = new MockNonCachedResponseCache();
        ResponseCache.setDefault(rc);
        uc = (HttpURLConnection) url.openConnection();
        assertFalse(isGetCalled);
        uc.setDoOutput(true);
        uc.setUseCaches(true);
        OutputStream os = uc.getOutputStream();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        os.write(1);
        os.flush();
        os.close();
        ((HttpURLConnection) uc).getResponseCode();
        assertTrue(isGetCalled);
        assertTrue(isPutCalled);
        isGetCalled = false;
        isPutCalled = false;
        InputStream is = uc.getInputStream();
        assertFalse(isGetCalled);
        assertFalse(isPutCalled);
        is.close();
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetInputStream()
     */
    public void test_UseCache_HttpURLConnection_GetInputStream()
            throws Exception {
        // set cache before URLConnection created, or it does not take effect
        ResponseCache rc = new MockNonCachedResponseCache();
        ResponseCache.setDefault(rc);
        URLConnection uc = url.openConnection();
        assertFalse(isGetCalled);
        uc.setDoOutput(true);
        uc.setUseCaches(true);
        InputStream is = uc.getInputStream();
        assertTrue(isGetCalled);
        assertTrue(isPutCalled);
        ((HttpURLConnection) uc).getResponseCode();
        is.close();
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using a MockResponseCache returns cache of
     *        null
     */
    public void test_UseCache_HttpURLConnection_NonCached() throws IOException {
        ResponseCache.setDefault(new MockNonCachedResponseCache());
        uc = (HttpURLConnection) url.openConnection();

        // default useCaches is true
        assertTrue(uc.getUseCaches());

        // make sure ResponseCache.get/put is called
        isGetCalled = false;
        isPutCalled = false;
        InputStream is = uc.getInputStream();
        assertFalse(is instanceof MockInputStream);
        assertTrue(isGetCalled);
        assertTrue(isPutCalled);

        // make sure protocol handler has tried to write to cache.
        isCacheWriteCalled = false;
        is.read();
        assertTrue(isCacheWriteCalled);

        // make sure protocol handler has tried to write to cache.
        isCacheWriteCalled = false;
        byte[] buf = new byte[1];
        is.read(buf);
        assertTrue(isCacheWriteCalled);

        // make sure protocol handler has tried to write to cache.
        isCacheWriteCalled = false;
        buf = new byte[1];
        is.read(buf, 0, 1);
        assertTrue(isCacheWriteCalled);

        // make sure protocol handler has tried to call abort.
        isAbortCalled = false;
        is.close();
        assertTrue(isAbortCalled);
        uc.disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using a MockResponseCache returns a mock
     *        cache
     */
    public void test_UseCache_HttpURLConnection_Cached() throws IOException {
        ResponseCache.setDefault(new MockCachedResponseCache());
        URL u = new URL("http://" + Support_Configuration.InetTestAddress);
        HttpURLConnection uc = (HttpURLConnection) u.openConnection();

        // default useCaches is true
        assertTrue(uc.getUseCaches());

        // make sure ResponseCache.get/put is called
        isGetCalled = false;
        isPutCalled = false;
        InputStream is = uc.getInputStream();
        assertTrue(is instanceof MockInputStream);
        assertTrue(isGetCalled);

        // make sure protocol handler doesn't try to write to cache, since
        // it has been in cache already.
        isCacheWriteCalled = false;
        is.read();
        assertFalse(isCacheWriteCalled);

        // make sure protocol handler doesn't try to write to cache, since
        // it has been in cache already.
        isCacheWriteCalled = false;
        byte[] buf = new byte[1];
        is.read(buf);
        assertFalse(isCacheWriteCalled);

        // make sure protocol handler doesn't try to write to cache, since
        // it has been in cache already.
        isCacheWriteCalled = false;
        buf = new byte[1];
        is.read(buf, 0, 1);
        assertFalse(isCacheWriteCalled);

        // make sure abort is not called since no write is performed
        isAbortCalled = false;
        is.close();
        assertFalse(isAbortCalled);
        uc.disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using getHeaderFields()
     */
    public void test_UseCache_HttpURLConnection_getHeaderFields()
            throws IOException {
        ResponseCache.setDefault(new MockCachedResponseCache());
        URL u = new URL("http://" + Support_Configuration.InetTestAddress);
        HttpURLConnection uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerMap = uc.getHeaderFields();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        assertEquals(mockHeaderMap, headerMap);
        assertEquals(uc.getInputStream(), mockIs);

        // REGRESSION for HARMONY-6542 item 1 in descriptions.txt attachement
        assertEquals("value1", uc.getHeaderField(0));
        assertEquals("value2", uc.getHeaderField(1));
        assertEquals("value1", uc.getHeaderField(2));
        assertEquals("value2", uc.getHeaderField(3));
        assertNull(uc.getHeaderField(4));

        uc.disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetOutputStream()
     */
    public void test_UseCache_HttpURLConnection_NoCached_GetOutputStream()
            throws Exception {
        ResponseCache.setDefault(new MockNonCachedResponseCache());
        uc = (HttpURLConnection) url.openConnection();
        uc.setChunkedStreamingMode(10);
        uc.setDoOutput(true);
        uc.getOutputStream();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        assertFalse(isAbortCalled);
        uc.disconnect();
    }
    
    /**
     * @tests java.net.URLConnection#getErrorStream()
     */
    public void test_getErrorStream() throws Exception {
        uc.setDoOutput(true);
        uc.connect();
        assertEquals(200, uc.getResponseCode());        
        // no error stream
        assertNull(uc.getErrorStream());        
        uc.disconnect();
        assertNull(uc.getErrorStream());
    }
    
    /**
     * @tests {@link java.net.HttpURLConnection#setFollowRedirects(boolean)}
     * @tests {@link java.net.HttpURLConnection#getFollowRedirects()}
     */
    public void test_followRedirects() {
        assertTrue("The default value of followRedirects is not true",
                HttpURLConnection.getFollowRedirects());

        HttpURLConnection.setFollowRedirects(false);
        assertFalse(HttpURLConnection.getFollowRedirects());

        HttpURLConnection.setFollowRedirects(true);
        assertTrue(HttpURLConnection.getFollowRedirects());
    }

    /**
     * @throws ProtocolException 
     * @tests {@link java.net.HttpURLConnection#setRequestMethod(String)}
     * @tests {@link java.net.HttpURLConnection#getRequestMethod()}
     */
    public void test_requestMethod() throws MalformedURLException, ProtocolException{
        URL url = new URL("http://harmony.apache.org/");
        
        HttpURLConnection con = new MyHttpURLConnection(url);
        assertEquals("The default value of requestMethod is not \"GET\"", "GET",
                con.getRequestMethod());

        String[] methods = { "GET", "DELETE", "HEAD", "OPTIONS", "POST", "PUT",
                "TRACE" };
        // Nomal set. Should not throw ProtocolException
        for (String method : methods) {
            con.setRequestMethod(method);
            assertEquals("The value of requestMethod is not " + method, method,
                    con.getRequestMethod());
        }
            
        try {
            con.setRequestMethod("Wrong method");
            fail("Should throw ProtocolException");
        } catch (ProtocolException e) {
            // Expected
        }
    }

    private static class MyHttpURLConnection extends HttpURLConnection {

        protected MyHttpURLConnection(URL url) {
            super(url);
        }

        @Override
        public void disconnect() {
            // do nothing
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
            // do nothing
        }
    }
    
    /**
     * @tests java.net.URLConnection#getPermission()
     */
    public void test_Permission() throws Exception {
        uc.connect();
        Permission permission = uc.getPermission();
        assertNotNull(permission);
        permission.implies(new SocketPermission("localhost","connect"));
    }

    class MockNonCachedResponseCache extends ResponseCache {

        public CacheResponse get(URI arg0, String arg1, Map arg2)
                throws IOException {
            isGetCalled = true;
            return null;
        }

        public CacheRequest put(URI arg0, URLConnection arg1)
                throws IOException {
            isPutCalled = true;
            return new MockCacheRequest();
        }
    }

    class MockCachedResponseCache extends ResponseCache {

        public CacheResponse get(URI arg0, String arg1, Map arg2)
                throws IOException {
            if (null == arg0 || null == arg1 || null == arg2) {
                throw new NullPointerException();
            }
            isGetCalled = true;
            return new MockCacheResponse();
        }

        public CacheRequest put(URI arg0, URLConnection arg1)
                throws IOException {
            if (null == arg0 || null == arg1) {
                throw new NullPointerException();
            }
            isPutCalled = true;
            return new MockCacheRequest();
        }
    }

    class MockCacheRequest extends CacheRequest {

        public OutputStream getBody() throws IOException {
            isCacheWriteCalled = true;
            return new MockOutputStream();
        }

        public void abort() {
            isAbortCalled = true;
        }

    }

    class MockCacheResponse extends CacheResponse {

        public Map<String, List<String>> getHeaders() throws IOException {
            return mockHeaderMap;
        }

        public InputStream getBody() throws IOException {
            return mockIs;
        }
    }

    class MockInputStream extends InputStream {

        public int read() throws IOException {
            return 1;
        }

        public int read(byte[] arg0, int arg1, int arg2) throws IOException {
            return 1;
        }

        public int read(byte[] arg0) throws IOException {
            return 1;
        }

    }

    class MockOutputStream extends OutputStream {

        public void write(int b) throws IOException {
            isCacheWriteCalled = true;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            isCacheWriteCalled = true;
        }

        public void write(byte[] b) throws IOException {
            isCacheWriteCalled = true;
        }
    }

    class MockHttpConnection extends HttpURLConnection {

        protected MockHttpConnection(URL url) {
            super(url);
        }

        public void disconnect() {
            // do nothing
        }

        public boolean usingProxy() {
            return false;
        }

        public void connect() throws IOException {
            // do nothing
        }

        public int getChunkLength() {
            return super.chunkLength;
        }

        public int getFixedLength() {
            return super.fixedContentLength;
        }

    }

    protected void setUp() {
        try {
            url = new URL("http://localhost:"+port+"/");
            uc = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            fail("Exception during setup : " + e.getMessage());
        }
        mockHeaderMap = new Hashtable<String, List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add("value1");
        valueList.add("value2");
        mockHeaderMap.put("field1", valueList);
        mockHeaderMap.put("field2", valueList);
        isGetCalled = false;
        isPutCalled = false;
        isCacheWriteCalled = false;
    }

    protected void tearDown() {
        uc.disconnect();
        ResponseCache.setDefault(null);
    }
}
