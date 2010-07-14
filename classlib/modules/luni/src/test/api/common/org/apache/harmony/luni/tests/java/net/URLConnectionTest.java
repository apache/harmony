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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.FileNameMap;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.harmony.luni.util.Util;

import tests.support.Support_Configuration;
import tests.support.Support_HttpServer;
import tests.support.Support_HttpServerSocket;
import tests.support.Support_HttpTests;
import tests.support.Support_Jetty;
import tests.support.Support_PortManager;
import tests.support.Support_URLConnector;
import tests.support.resource.Support_Resources;

public class URLConnectionTest extends junit.framework.TestCase {

    static class MockURLConnection extends URLConnection {

        public MockURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() {
            connected = true;
        }
    }

    static class NewHandler extends URLStreamHandler {
        protected URLConnection openConnection(URL u) throws IOException {
            return new HttpURLConnection(u) {
                @Override
                public void connect() throws IOException {
                    connected = true;
                }

                @Override
                public void disconnect() {
                    // do nothing
                }

                @Override
                public boolean usingProxy() {
                    return false;
                }
            };
        }
    }

    private static int port;

    static String getContentType(String fileName) throws IOException {
        String resourceName = "org/apache/harmony/luni/tests/" + fileName;
        URL url = ClassLoader.getSystemClassLoader().getResource(resourceName);
        assertNotNull("Cannot find test resource " + resourceName, url);
        return url.openConnection().getContentType();
    }

    URL url;

    URLConnection uc;

    protected void setUp() throws Exception {
        url = new URL("http://localhost:" + port + "/");
        uc = (HttpURLConnection) url.openConnection();
        port = Support_Jetty.startDefaultHttpServer();
    }

    protected void tearDown() {
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#addRequestProperty(String, String)
     */
    public void test_addRequestProperty() throws MalformedURLException,
            IOException {

        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        try {
            // Regression for HARMONY-604
            u.addRequestProperty(null, "someValue");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        u.addRequestProperty("key", "value");
        u.addRequestProperty("key", "value2");
        assertEquals("value2", u.getRequestProperty("key"));
        ArrayList list = new ArrayList();
        list.add("value2");
        list.add("value");

        Map<String,List<String>> propertyMap = u.getRequestProperties();
        // Check this map is unmodifiable
        try {
            propertyMap.put("test", null);
            fail("Map returned by URLConnection.getRequestProperties() should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        List<String> valuesList = propertyMap.get("key");
        // Check this list is also unmodifiable
        try {
            valuesList.add("test");
            fail("List entries in the map returned by URLConnection.getRequestProperties() should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        assertEquals(list, valuesList);

        u.connect();
        try {
            // state of connection is checked first
            // so no NPE in case of null 'field' param
            u.addRequestProperty(null, "someValue");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * Regression test for HARMONY-6452
     */
    public void test_RequestProperty_case_insensitivity() 
            throws MalformedURLException, IOException {

        URLConnection u =
            (URLConnection)(new URL("http://example.org/").openConnection());
        u.setRequestProperty("KEY", "upper");
        u.setRequestProperty("key", "lower");
        assertEquals("set for \"KEY\" is overwritten by set for \"key\"",
                     "lower", u.getRequestProperty("KEY"));
        assertEquals("value can be retrieved by either key case",
                     "lower", u.getRequestProperty("key"));
        assertEquals("value can be retrieved by arbitrary key case",
                     "lower", u.getRequestProperty("kEy"));

        Map<String, List<String>> props = u.getRequestProperties();
        List<String> values = props.get("KEY");
        assertNotNull("first key does have an entry", values);
        assertNull("second key does not have an entry", props.get("key"));

        assertEquals("returned value list is correct size", 1, values.size());
        assertTrue("returned value list contains expected value",
                   values.contains("lower"));


        // repeat the above with the case of keys reversed to confirm
        // that first key is significant one
        u = (URLConnection)(new URL("http://example.org/").openConnection());
        u.setRequestProperty("key", "lower");
        u.setRequestProperty("KEY", "upper");
        assertEquals("set for \"key\" is overwritten by set for \"KEY\"",
                     "upper", u.getRequestProperty("KEY"));
        assertEquals("value can be retrieved by either key case",
                     "upper", u.getRequestProperty("key"));
        assertEquals("value can be retrieved by arbitrary key case",
                     "upper", u.getRequestProperty("kEy"));

        props = u.getRequestProperties();
        values = props.get("key");
        assertNotNull("first key does have an entry", values);
        assertNull("second key does not have an entry", props.get("KEY"));

        assertEquals("returned value list is correct size", 1, values.size());
        assertTrue("returned value list contains expected value",
                   values.contains("upper"));


        // repeat the first test with set and add methods
        u = (URLConnection)(new URL("http://example.org/").openConnection());
        u.setRequestProperty("KEY", "value1");
        u.addRequestProperty("key", "value2");
        assertEquals("value for \"KEY\" is the last one added",
                     "value2", u.getRequestProperty("KEY"));
        assertEquals("value can be retrieved by either key case",
                     "value2", u.getRequestProperty("key"));
        assertEquals("value can be retrieved by arbitrary key case",
                     "value2", u.getRequestProperty("kEy"));

        props = u.getRequestProperties();
        values = props.get("KEY");
        assertNotNull("first key does have an entry", values);
        assertNull("second key does not have an entry", props.get("key"));

        assertEquals("returned value list is correct size", 2, values.size());
        assertTrue("returned value list contains first value",
                   values.contains("value1"));
        assertTrue("returned value list contains second value",
                   values.contains("value2"));


        // repeat the previous test with only add methods
        u = (URLConnection)(new URL("http://example.org/").openConnection());
        u.addRequestProperty("KEY", "value1");
        u.addRequestProperty("key", "value2");
        u.addRequestProperty("Key", "value3");
        assertEquals("value for \"KEY\" is the last one added",
                     "value3", u.getRequestProperty("KEY"));
        assertEquals("value can be retrieved by another key case",
                     "value3", u.getRequestProperty("key"));
        assertEquals("value can be retrieved by arbitrary key case",
                     "value3", u.getRequestProperty("kEy"));

        props = u.getRequestProperties();
        values = props.get("KEY");
        assertNotNull("first key does have an entry", values);
        assertNull("second key does not have an entry", props.get("key"));
        assertNull("third key does not have an entry", props.get("Key"));

        assertEquals("returned value list is correct size", 3, values.size());
        assertTrue("returned value list contains first value",
                   values.contains("value1"));
        assertTrue("returned value list contains second value",
                   values.contains("value2"));
        assertTrue("returned value list contains second value",
                   values.contains("value3"));
    }

    /**
     * @tests java.net.URLConnection#addRequestProperty(java.lang.String,java.lang.String)
     */
    public void test_addRequestPropertyLjava_lang_StringLjava_lang_String()
            throws IOException {
        uc.setRequestProperty("prop", "yo");
        uc.setRequestProperty("prop", "yo2");
        assertEquals("yo2", uc.getRequestProperty("prop"));
        Map<String, List<String>> map = uc.getRequestProperties();
        List<String> props = uc.getRequestProperties().get("prop");
        assertEquals(1, props.size());

        try {
            // the map should be unmodifiable
            map.put("hi", Arrays.asList(new String[] { "bye" }));
            fail("could modify map");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        try {
            // the list should be unmodifiable
            props.add("hi");
            fail("could modify list");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "hyts_att.jar");
        URL fUrl1 = new URL("jar:file:" + resources.getPath()
                + "/hyts_att.jar!/");
        JarURLConnection con1 = (JarURLConnection) fUrl1.openConnection();
        map = con1.getRequestProperties();
        assertNotNull(map);
        assertEquals(0, map.size());
        try {
            // the map should be unmodifiable
            map.put("hi", Arrays.asList(new String[] { "bye" }));
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    /**
     * @tests java.net.URLConnection#getAllowUserInteraction()
     */
    public void test_getAllowUserInteraction() {
        uc.setAllowUserInteraction(false);
        assertFalse("getAllowUserInteraction should have returned false", uc
                .getAllowUserInteraction());

        uc.setAllowUserInteraction(true);
        assertTrue("getAllowUserInteraction should have returned true", uc
                .getAllowUserInteraction());
    }

    /**
     * @tests java.net.URLConnection#getContent()
     */
    public void test_getContent() throws IOException {
        byte[] ba = new byte[600];
        ((InputStream) uc.getContent()).read(ba, 0, 600);
        String s = Util.toUTF8String(ba);
        assertTrue("Incorrect content returned",
                s.indexOf("Hello OneHandler") > 0);
    }

    /**
     * @tests java.net.URLConnection#getContent(Class[])
     */
    public void test_getContent_LjavalangClass() throws IOException {
        byte[] ba = new byte[600];

        try {
            ((InputStream) uc.getContent(null)).read(ba, 0, 600);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            ((InputStream) uc.getContent(new Class[] {})).read(ba, 0, 600);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            ((InputStream) uc.getContent(new Class[] { Class.class })).read(ba,
                    0, 600);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#getContentEncoding()
     */
    public void test_getContentEncoding() {
        // should not be known for a file
        assertNull("getContentEncoding failed: " + uc.getContentEncoding(), uc
                .getContentEncoding());
    }

    /**
     * @tests java.net.URLConnection#getContentLength()
     */
    public void test_getContentLength() throws IOException {
        assertEquals("getContentLength failed: " + uc.getContentLength(), 25,
                uc.getContentLength());
    }

    /**
     * @tests java.net.URLConnection#getContentType()
     */
    public void test_getContentType() throws IOException {
        // should not be known for a file
        assertTrue("getContentType failed: " + uc.getContentType(), uc
                .getContentType().contains("text/html"));

        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "Harmony.GIF");
        URL url = new URL("file:/" + resources.toString() + "/Harmony.GIF");
        URLConnection conn = url.openConnection();
        assertEquals("type not GIF", "image/gif", conn.getContentType());
    }

    /**
     * @tests java.net.URLConnection#getContentType()
     */
    public void test_getContentType_regression() throws IOException {
        // Regression for HARMONY-4699
        assertEquals("application/rtf", getContentType("test.rtf"));
        assertEquals("text/plain", getContentType("test.java"));
        // RI would return "content/unknown"
        assertEquals("application/msword", getContentType("test.doc"));
        assertEquals("text/html", getContentType("test.htx"));
        assertEquals("application/xml", getContentType("test.xml"));
        assertEquals("text/plain", getContentType("."));
    }

    /**
     * @tests java.net.URLConnection#getDate()
     */
    public void test_getDate() {
        // should be greater than 930000000000L which represents the past
        if (uc.getDate() == 0) {
            System.out
                    .println("WARNING: server does not support 'Date', in test_getDate");
        } else {
            assertTrue("getDate gave wrong date: " + uc.getDate(),
                    uc.getDate() > 930000000000L);
        }
    }

    /**
     * @tests java.net.URLConnection#getDefaultAllowUserInteraction()
     */
    public void test_getDefaultAllowUserInteraction() {
        boolean oldSetting = URLConnection.getDefaultAllowUserInteraction();

        URLConnection.setDefaultAllowUserInteraction(false);
        assertFalse(
                "getDefaultAllowUserInteraction should have returned false",
                URLConnection.getDefaultAllowUserInteraction());

        URLConnection.setDefaultAllowUserInteraction(true);
        assertTrue("getDefaultAllowUserInteraction should have returned true",
                URLConnection.getDefaultAllowUserInteraction());

        URLConnection.setDefaultAllowUserInteraction(oldSetting);
    }

    /**
     * @tests java.net.URLConnection#getDefaultRequestProperty(java.lang.String)
     */
    @SuppressWarnings("deprecation")
    public void test_getDefaultRequestPropertyLjava_lang_String() {
        URLConnection.setDefaultRequestProperty("Shmoo", "Blah");
        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Shmoo"));

        URLConnection.setDefaultRequestProperty("Shmoo", "Boom");
        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Shmoo"));

        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Kapow"));

        URLConnection.setDefaultRequestProperty("Shmoo", null);
    }

    /**
     * @tests java.net.URLConnection#getDefaultUseCaches()
     */
    public void test_getDefaultUseCaches() {
        boolean oldSetting = uc.getDefaultUseCaches();

        uc.setDefaultUseCaches(false);
        assertFalse("getDefaultUseCaches should have returned false", uc
                .getDefaultUseCaches());

        uc.setDefaultUseCaches(true);
        assertTrue("getDefaultUseCaches should have returned true", uc
                .getDefaultUseCaches());

        uc.setDefaultUseCaches(oldSetting);
    }

    /**
     * @tests java.net.URLConnection#getDoInput()
     */
    public void test_getDoInput() {
        assertTrue("Should be set to true by default", uc.getDoInput());

        uc.setDoInput(true);
        assertTrue("Should have been set to true", uc.getDoInput());

        uc.setDoInput(false);
        assertFalse("Should have been set to false", uc.getDoInput());
    }

    /**
     * @tests java.net.URLConnection#getDoOutput()
     */
    public void test_getDoOutput() {
        assertFalse("Should be set to false by default", uc.getDoOutput());

        uc.setDoOutput(true);
        assertTrue("Should have been set to true", uc.getDoOutput());

        uc.setDoOutput(false);
        assertFalse("Should have been set to false", uc.getDoOutput());
    }

    /**
     * @tests java.net.URLConnection#getExpiration()
     */
    public void test_getExpiration() {
        // should be unknown
        assertEquals("getExpiration returned wrong expiration", 0, uc
                .getExpiration());
    }

    /**
     * @tests java.net.URLConnection#getFileNameMap()
     */
    public void test_getFileNameMap() {
        // Tests for the standard MIME types -- users may override these
        // in their JRE
        FileNameMap map = URLConnection.getFileNameMap();

        // These types are defaulted
        assertEquals("text/html", map.getContentTypeFor(".htm"));
        assertEquals("text/html", map.getContentTypeFor(".html"));
        assertEquals("text/plain", map.getContentTypeFor(".text"));
        assertEquals("text/plain", map.getContentTypeFor(".txt"));

        // These types come from the properties file
        assertEquals("application/pdf", map.getContentTypeFor(".pdf"));
        assertEquals("application/zip", map.getContentTypeFor(".zip"));

        URLConnection.setFileNameMap(new FileNameMap() {
            public String getContentTypeFor(String fileName) {
                return "Spam!";
            }
        });
        try {
            assertEquals("Incorrect FileNameMap returned", "Spam!",
                    URLConnection.getFileNameMap().getContentTypeFor(null));
        } finally {
            // unset the map so other tests don't fail
            URLConnection.setFileNameMap(null);
        }
        // RI fails since it does not support fileName that does not begin with
        // '.'
        assertEquals("image/gif", map.getContentTypeFor("gif"));
    }

    /**
     * @tests java.net.URLConnection#getHeaderFieldDate(java.lang.String, long)
     */
    public void test_getHeaderFieldDateLjava_lang_StringJ() {

        if (uc.getHeaderFieldDate("Date", 22L) == 22L) {
            System.out
                    .println("WARNING: Server does not support 'Date', test_getHeaderFieldDateLjava_lang_StringJ not run");
            return;
        }
        assertTrue("Wrong value returned: "
                + uc.getHeaderFieldDate("Date", 22L), uc.getHeaderFieldDate(
                "Date", 22L) > 930000000000L);

        long time = uc.getHeaderFieldDate("Last-Modified", 0);
        assertEquals("Wrong date: ", time,
                Support_Configuration.URLConnectionLastModified);
    }

    /**
     * @tests java.net.URLConnection#getHeaderField(int)
     */
    public void test_getHeaderFieldI() {
        int i = 0;
        String hf;
        boolean foundResponse = false;
        while ((hf = uc.getHeaderField(i++)) != null) {
            if (hf.equals(Support_Configuration.HomeAddressSoftware)) {
                foundResponse = true;
            }
        }
        assertTrue("Could not find header field containing \""
                + Support_Configuration.HomeAddressSoftware + "\"",
                foundResponse);

        i = 0;
        foundResponse = false;
        while ((hf = uc.getHeaderField(i++)) != null) {
            if (hf.equals(Support_Configuration.HomeAddressResponse)) {
                foundResponse = true;
            }
        }
        assertTrue("Could not find header field containing \""
                + Support_Configuration.HomeAddressResponse + "\"",
                foundResponse);
    }

    /**
     * @tests java.net.URLConnection#getHeaderFieldKey(int)
     */
    public void test_getHeaderFieldKeyI() {
        String hf;
        boolean foundResponse = false;
        for (int i = 0; i < 100; i++) {
            hf = uc.getHeaderFieldKey(i);
            if (hf != null && hf.toLowerCase().equals("content-type")) {
                foundResponse = true;
                break;
            }
        }
        assertTrue(
                "Could not find header field key containing \"content-type\"",
                foundResponse);
    }

    /**
     * @tests java.net.URLConnection#getHeaderField(java.lang.String)
     */
    public void test_getHeaderFieldLjava_lang_String() {
        String hf;
        hf = uc.getHeaderField("Content-Encoding");
        if (hf != null) {
            assertNull(
                    "Wrong value returned for header field 'Content-Encoding': "
                            + hf, hf);
        }
        hf = uc.getHeaderField("Content-Length");
        if (hf != null) {
            assertEquals(
                    "Wrong value returned for header field 'Content-Length': ",
                    "25", hf);
        }
        hf = uc.getHeaderField("Content-Type");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'Content-Type': "
                    + hf, hf.contains("text/html"));
        }
        hf = uc.getHeaderField("content-type");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'content-type': "
                    + hf, hf.contains("text/html"));
        }
        hf = uc.getHeaderField("Date");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'Date': " + hf,
                    Integer.parseInt(hf.substring(hf.length() - 17,
                            hf.length() - 13)) >= 1999);
        }
        hf = uc.getHeaderField("Expires");
        if (hf != null) {
            assertNull(
                    "Wrong value returned for header field 'Expires': " + hf,
                    hf);
        }
        hf = uc.getHeaderField("SERVER");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'SERVER': " + hf
                    + " (expected " + Support_Configuration.HomeAddressSoftware
                    + ")", hf.equals(Support_Configuration.HomeAddressSoftware));
        }
        hf = uc.getHeaderField("Last-Modified");
        if (hf != null) {
            assertTrue(
                    "Wrong value returned for header field 'Last-Modified': "
                            + hf,
                    hf
                            .equals(Support_Configuration.URLConnectionLastModifiedString));
        }
        hf = uc.getHeaderField("accept-ranges");
        if (hf != null) {
            assertTrue(
                    "Wrong value returned for header field 'accept-ranges': "
                            + hf, hf.equals("bytes"));
        }
        hf = uc.getHeaderField("DoesNotExist");
        if (hf != null) {
            assertNull("Wrong value returned for header field 'DoesNotExist': "
                    + hf, hf);
        }
    }

    /**
     * @tests java.net.URLConnection#getHeaderFields()
     */
    public void test_getHeaderFields() throws IOException {
        try {
            uc.getInputStream();
        } catch (IOException e) {
            fail();
        }

        Map<String, List<String>> headers = uc.getHeaderFields();
        assertNotNull(headers);

        // content-length should always appear
        List<String> list = headers.get("Content-Length");
        if (list == null) {
            list = headers.get("content-length");
        }
        assertNotNull(list);
        String contentLength = (String) list.get(0);
        assertNotNull(contentLength);

        // there should be at least 2 headers
        assertTrue(headers.size() > 1);
        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "hyts_att.jar");
        URL fUrl1 = new URL("jar:file:" + resources.getPath()
                + "/hyts_att.jar!/");
        JarURLConnection con1 = (JarURLConnection) fUrl1.openConnection();
        headers = con1.getHeaderFields();
        assertNotNull(headers);
        assertEquals(0, headers.size());
        try {
            // the map should be unmodifiable
            headers.put("hi", Arrays.asList(new String[] { "bye" }));
            fail("The map should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    /**
     * @tests java.net.URLConnection#getIfModifiedSince()
     */
    public void test_getIfModifiedSince() {
        uc.setIfModifiedSince(200);
        assertEquals("Returned wrong ifModifiedSince value", 200, uc
                .getIfModifiedSince());
    }

    /**
     * @tests java.net.URLConnection#getInputStream()
     */
    public void test_getInputStream() throws IOException {
        InputStream is = uc.getInputStream();
        byte[] ba = new byte[600];
        is.read(ba, 0, 600);
        is.close();
        String s = Util.toUTF8String(ba);
        assertTrue("Incorrect input stream read",
                s.indexOf("Hello OneHandler") > 0);

        // open an non-existent file
        URL url = new URL("http://localhost:" + port + "/fred-zz6.txt");
        is = url.openStream();
        assertTrue("available() less than 0", is.available() >= 0);
        is.close();

        // create a server socket
        Support_HttpServerSocket serversocket = new Support_HttpServerSocket();

        // create a client connector
        Support_URLConnector client = new Support_URLConnector();

        // pass both to the HttpTest
        Support_HttpTests test = new Support_HttpTests(serversocket, client);

        // run various tests common to both HttpConnections and
        // HttpURLConnections
        test.runTests(this);

        // Authentication test is separate from other tests because it is only
        // in HttpURLConnection and not supported in HttpConnection

        serversocket = new Support_HttpServerSocket();
        Support_HttpServer server = new Support_HttpServer(serversocket, this);
        int p = Support_PortManager.getNextPort();
        server.startServer(p);

        // it is the Support_HttpServer's responsibility to close this
        // serversocket
        serversocket = null;

        final String authTestUrl = "http://localhost:" + server.getPort()
                + Support_HttpServer.AUTHTEST;

        // Authentication test
        // set up a very simple authenticator
        Authenticator.setDefault(new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("test", "password"
                        .toCharArray());
            }
        });
        try {
            client.open(authTestUrl);
            is = client.getInputStream();
            int c = is.read();
            while (c > 0) {
                c = is.read();
            }
            c = is.read();
            is.close();
        } catch (FileNotFoundException e) {
            fail("Error performing authentication test: " + e);
        }

        final String invalidLocation = "/missingFile.htm";
        final String redirectTestUrl = "http://localhost:" + server.getPort()
                + Support_HttpServer.REDIRECTTEST;

        // test redirecting to a non-existent URL on the same host
        try {
            // append the response code for the server to return

            client.open(redirectTestUrl + "/" + Support_HttpServer.MOVED_PERM
                    + "-" + invalidLocation);
            is = client.getInputStream();

            int c = is.read();
            while (c > 0) {
                c = is.read();
            }
            c = is.read();
            is.close();
            fail("Incorrect data returned on redirect to non-existent file.");
        } catch (FileNotFoundException e) {
        }
        server.stopServer();

    }

    /**
     * @tests java.net.URLConnection#getLastModified()
     */
    public void test_getLastModified() {
        if (uc.getLastModified() == 0) {
            System.out
                    .println("WARNING: Server does not support 'Last-Modified', test_getLastModified() not run");
            return;
        }
        assertTrue(
                "Returned wrong getLastModified value.  Wanted: "
                        + Support_Configuration.URLConnectionLastModified
                        + " got: " + uc.getLastModified(),
                uc.getLastModified() == Support_Configuration.URLConnectionLastModified);
    }

    /**
     * @tests java.net.URLConnection#getOutputStream()
     */
    public void test_getOutputStream() throws Exception {
        int port = Support_Jetty.startDefaultServlet();
        try {
            boolean exception = false;
            URL test;
            java.net.URLConnection conn2 = null;

            test = new URL("http://localhost:" + port + "/");
            conn2 = (java.net.URLConnection) test.openConnection();

            try {
                conn2.getOutputStream();
                fail("should throw ProtocolException");
            } catch (java.net.ProtocolException e) {
                // correct
            }

            conn2.setDoOutput(true);
            conn2.getOutputStream();
            conn2.connect();
            conn2.getOutputStream();

            try {
                conn2.getInputStream();
                conn2.getOutputStream();
                fail("should throw ProtocolException");
            } catch (ProtocolException e) {
                // expected.
            }

            URL u = new URL("http://localhost:" + port + "/");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u
                    .openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();
            String posted = "this is a test";
            out.write(posted.getBytes());
            out.close();
            conn.getResponseCode();
            InputStream is = conn.getInputStream();
            String response = "";
            byte[] b = new byte[1024];
            int count = 0;
            while ((count = is.read(b)) > 0) {
                response += new String(b, 0, count);
            }
            assertEquals("Response to POST method invalid 1", posted, response);

            posted = "just a test";
            u = new URL("http://localhost:" + port + "/");
            conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-length", String.valueOf(posted
                    .length()));
            out = conn.getOutputStream();
            out.write(posted.getBytes());
            out.close();
            conn.getResponseCode();
            is = conn.getInputStream();
            response = "";
            b = new byte[1024];
            count = 0;
            while ((count = is.read(b)) > 0) {
                response += new String(b, 0, count);
            }
            assertTrue("Response to POST method invalid 2", response
                    .equals(posted));

            posted = "just another test";
            u = new URL("http://localhost:" + port + "/");
            conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-length", String.valueOf(posted
                    .length()));
            out = conn.getOutputStream();
            out.write(posted.getBytes());
            // out.close();
            conn.getResponseCode();
            is = conn.getInputStream();
            response = "";
            b = new byte[1024];
            count = 0;
            while ((count = is.read(b)) > 0) {
                response += new String(b, 0, count);
            }
            assertTrue("Response to POST method invalid 3", response
                    .equals(posted));

            u = new URL("http://localhost:" + port + "/");
            conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            int result = conn.getResponseCode();
            assertTrue("Unexpected response code: " + result, result == 200);

        } finally {
            Support_Jetty.startDefaultServlet();
        }
    }

    /**
     * @tests java.net.URLConnection#getPermission()
     */
    public void test_getPermission() throws Exception {
        java.security.Permission p = uc.getPermission();
        assertTrue("Permission of wrong type: " + p.toString(),
                p instanceof java.net.SocketPermission);
        assertTrue("Permission has wrong name: " + p.getName(), p.getName()
                .contains("localhost:" + port));

        URL fileUrl = new URL("file:myfile");
        Permission perm = new FilePermission("myfile", "read");
        Permission result = fileUrl.openConnection().getPermission();
        assertTrue("Wrong file: permission 1:" + perm + " , " + result, result
                .equals(perm));

        fileUrl = new URL("file:/myfile/");
        perm = new FilePermission("/myfile", "read");
        result = fileUrl.openConnection().getPermission();
        assertTrue("Wrong file: permission 2:" + perm + " , " + result, result
                .equals(perm));

        fileUrl = new URL("file:///host/volume/file");
        perm = new FilePermission("/host/volume/file", "read");
        result = fileUrl.openConnection().getPermission();
        assertTrue("Wrong file: permission 3:" + perm + " , " + result, result
                .equals(perm));

        URL httpUrl = new URL("http://home/myfile/");
        assertTrue("Wrong http: permission", httpUrl.openConnection()
                .getPermission().equals(
                        new SocketPermission("home:80", "connect")));
        httpUrl = new URL("http://home2:8080/myfile/");
        assertTrue("Wrong http: permission", httpUrl.openConnection()
                .getPermission().equals(
                        new SocketPermission("home2:8080", "connect")));
        URL ftpUrl = new URL("ftp://home/myfile/");
        assertTrue("Wrong ftp: permission", ftpUrl.openConnection()
                .getPermission().equals(
                        new SocketPermission("home:21", "connect")));
        ftpUrl = new URL("ftp://home2:22/myfile/");
        assertTrue("Wrong ftp: permission", ftpUrl.openConnection()
                .getPermission().equals(
                        new SocketPermission("home2:22", "connect")));

        URL jarUrl = new URL("jar:file:myfile!/");
        perm = new FilePermission("myfile", "read");
        result = jarUrl.openConnection().getPermission();
        assertTrue("Wrong jar: permission:" + perm + " , " + result, result
                .equals(new FilePermission("myfile", "read")));
    }

    /**
     * @tests java.net.URLConnection#getRequestProperties()
     */
    public void test_getRequestProperties() {
        uc.setRequestProperty("whatever", "you like");
        Map headers = uc.getRequestProperties();

        // content-length should always appear
        List header = (List) headers.get("whatever");
        assertNotNull(header);

        assertEquals("you like", header.get(0));

        assertTrue(headers.size() >= 1);

        try {
            // the map should be unmodifiable
            headers.put("hi", "bye");
            fail();
        } catch (UnsupportedOperationException e) {
        }
        try {
            // the list should be unmodifiable
            header.add("hi");
            fail();
        } catch (UnsupportedOperationException e) {
        }

    }

    /**
     * @tests java.net.URLConnection#getRequestProperties()
     */
    public void test_getRequestProperties_Exception() throws IOException {
        URL url = new URL("http", "test", 80, "index.html", new NewHandler());
        URLConnection urlCon = url.openConnection();
        urlCon.connect();

        try {
            urlCon.getRequestProperties();
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#getRequestProperty(java.lang.String)
     */
    public void test_getRequestProperty_LString_Exception() throws IOException {
        URL url = new URL("http", "test", 80, "index.html", new NewHandler());
        URLConnection urlCon = url.openConnection();
        urlCon.setRequestProperty("test", "testProperty");
        assertEquals("testProperty", urlCon.getRequestProperty("test"));

        urlCon.connect();
        try {
            urlCon.getRequestProperty("test");
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#getRequestProperty(java.lang.String)
     */
    public void test_getRequestPropertyLjava_lang_String() {
        uc.setRequestProperty("Yo", "yo");
        assertTrue("Wrong property returned: " + uc.getRequestProperty("Yo"),
                uc.getRequestProperty("Yo").equals("yo"));
        assertNull("Wrong property returned: " + uc.getRequestProperty("No"),
                uc.getRequestProperty("No"));
    }

    /**
     * @tests java.net.URLConnection#getURL()
     */
    public void test_getURL() {
        assertTrue("Incorrect URL returned", uc.getURL().equals(url));
    }

    /**
     * @tests java.net.URLConnection#getUseCaches()
     */
    public void test_getUseCaches() {
        uc.setUseCaches(false);
        assertTrue("getUseCaches should have returned false", !uc
                .getUseCaches());
        uc.setUseCaches(true);
        assertTrue("getUseCaches should have returned true", uc.getUseCaches());
    }

    /**
     * @tests java.net.URLConnection#guessContentTypeFromStream(java.io.InputStream)
     */
    public void test_guessContentTypeFromStreamLjava_io_InputStream()
            throws IOException {
        String[] headers = new String[] { "<html>", "<head>", " <head ",
                "<body", "<BODY ", "<!DOCTYPE html", "<?xml " };
        String[] expected = new String[] { "text/html", "text/html",
                "text/html", "text/html", "text/html", "text/html",
                "application/xml" };

        String[] encodings = new String[] { "ASCII", "UTF-8", "UTF-16BE",
                "UTF-16LE", "UTF-32BE", "UTF-32LE" };
        for (int i = 0; i < headers.length; i++) {
            for (String enc : encodings) {
                InputStream is = new ByteArrayInputStream(toBOMBytes(
                        headers[i], enc));
                String mime = URLConnection.guessContentTypeFromStream(is);
                assertEquals("checking " + headers[i] + " with " + enc,
                        expected[i], mime);
            }
        }

        // Try simple case
        try {
            URLConnection.guessContentTypeFromStream(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        // Test magic bytes
        byte[][] bytes = new byte[][] { { 'P', 'K' }, { 'G', 'I' } };
        expected = new String[] { "application/zip", "image/gif" };

        for (int i = 0; i < bytes.length; i++) {
            InputStream is = new ByteArrayInputStream(bytes[i]);
            assertEquals(expected[i], URLConnection
                    .guessContentTypeFromStream(is));
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        assertNull(URLConnection.guessContentTypeFromStream(bais));
        bais.close();
    }

    /**
     * @tests java.net.URLConnection#setAllowUserInteraction(boolean)
     */
    public void test_setAllowUserInteractionZ() throws MalformedURLException {
        // Regression for HARMONY-72
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        u.connect();
        try {
            u.setAllowUserInteraction(false);
            fail("Assert 0: expected an IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }

    }

    /**
     * @tests java.net.URLConnection#setConnectTimeout(int)
     */
    public void test_setConnectTimeoutI() throws Exception {
        URLConnection uc = new URL("http://localhost").openConnection();
        assertEquals(0, uc.getConnectTimeout());
        uc.setConnectTimeout(0);
        assertEquals(0, uc.getConnectTimeout());
        try {
            uc.setConnectTimeout(-100);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(0, uc.getConnectTimeout());
        uc.setConnectTimeout(100);
        assertEquals(100, uc.getConnectTimeout());
        try {
            uc.setConnectTimeout(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(100, uc.getConnectTimeout());
    }

    /**
     * @tests java.net.URLConnection#setDefaultAllowUserInteraction(boolean)
     */
    public void test_setDefaultAllowUserInteractionZ() {
        assertTrue("Used to test", true);
    }

    /**
     * @tests java.net.URLConnection#setDefaultRequestProperty(java.lang.String,
     *        java.lang.String)
     */
    public void test_setDefaultRequestPropertyLjava_lang_StringLjava_lang_String() {
        assertTrue("Used to test", true);
    }

    /**
     * @tests java.net.URLConnection#setDefaultUseCaches(boolean)
     */
    public void test_setDefaultUseCachesZ() {
        assertTrue("Used to test", true);
    }

    /**
     * @throws IOException
     * @throws MalformedURLException
     * @tests java.net.URLConnection#setDoInput(boolean)
     */
    public void test_setDoInputZ() throws MalformedURLException, IOException {
        assertTrue("Used to test", true);
        HttpURLConnection u = null;

        u = (HttpURLConnection) (new URL("http://localhost:" + port)
                .openConnection());
        u.connect();

        try {
            u.setDoInput(true);
        } catch (IllegalStateException e) { // expected
        }
    }

    /**
     * @throws IOException
     * @throws MalformedURLException
     * @tests java.net.URLConnection#setDoOutput(boolean)
     */
    public void test_setDoOutputZ() throws MalformedURLException, IOException {
        assertTrue("Used to test", true);
        HttpURLConnection u = null;

        u = (HttpURLConnection) (new URL("http://localhost:" + port)
                .openConnection());
        u.connect();

        try {
            u.setDoOutput(true);
        } catch (IllegalStateException e) { // expected
        }
    }

    /**
     * @throws IOException
     * @tests java.net.URLConnection#setFileNameMap(java.net.FileNameMap)
     */
    public void test_setFileNameMapLjava_net_FileNameMap() throws IOException {
        // nothing happens if set null
        URLConnection.setFileNameMap(null);
        // take no effect
        assertNotNull(URLConnection.getFileNameMap());
    }

    /**
     * @tests java.net.URLConnection#setIfModifiedSince(long)
     */
    public void test_setIfModifiedSinceJ() throws IOException {
        URL url = new URL("http://localhost:8080/");
        URLConnection connection = url.openConnection();
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2000, Calendar.MARCH, 5);

        long sinceTime = cal.getTime().getTime();
        connection.setIfModifiedSince(sinceTime);
        assertEquals("Wrong date set", sinceTime, connection
                .getIfModifiedSince());

    }

    /**
     * @tests java.net.URLConnection#setReadTimeout(int)
     */
    public void test_setReadTimeoutI() throws Exception {
        URLConnection uc = new URL("http://localhost").openConnection();
        assertEquals(0, uc.getReadTimeout());
        uc.setReadTimeout(0);
        assertEquals(0, uc.getReadTimeout());
        try {
            uc.setReadTimeout(-100);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(0, uc.getReadTimeout());
        uc.setReadTimeout(100);
        assertEquals(100, uc.getReadTimeout());
        try {
            uc.setReadTimeout(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(100, uc.getReadTimeout());
    }

    /**
     * @tests java.net.URLConnection#setRequestProperty(String, String)
     */
    public void test_setRequestProperty() throws MalformedURLException,
            IOException {

        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        try {
            u.setRequestProperty(null, "someValue");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        u.connect();
        try {
            // state of connection is checked first
            // so no NPE in case of null 'field' param
            u.setRequestProperty(null, "someValue");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#setRequestProperty(java.lang.String,
     *        java.lang.String)
     */
    public void test_setRequestPropertyLjava_lang_StringLjava_lang_String() 
                throws MalformedURLException{
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));

        u.setRequestProperty("", "");
        assertEquals("", u.getRequestProperty(""));

        u.setRequestProperty("key", "value");
        assertEquals("value", u.getRequestProperty("key"));
    }

    /**
     * @tests java.net.URLConnection#setUseCaches(boolean)
     */
    public void test_setUseCachesZ() throws MalformedURLException {
        // Regression for HARMONY-71
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        u.connect();
        try {
            u.setUseCaches(true);
            fail("Assert 0: expected an IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#toString()
     */
    public void test_toString() {
        assertTrue("Wrong toString: " + uc.toString(), uc.toString().indexOf(
                "URLConnection") > 0);
    }

    private byte[] toBOMBytes(String text, String enc) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        if (enc.equals("UTF-8")) {
            bos.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
        }
        if (enc.equals("UTF-16BE")) {
            bos.write(new byte[] { (byte) 0xFE, (byte) 0xFF });
        }
        if (enc.equals("UTF-16LE")) {
            bos.write(new byte[] { (byte) 0xFF, (byte) 0xFE });
        }
        if (enc.equals("UTF-32BE")) {
            bos.write(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE,
                    (byte) 0xFF });
        }
        if (enc.equals("UTF-32LE")) {
            bos.write(new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00,
                    (byte) 0x00 });
        }

        bos.write(text.getBytes(enc));
        return bos.toByteArray();
    }
}
