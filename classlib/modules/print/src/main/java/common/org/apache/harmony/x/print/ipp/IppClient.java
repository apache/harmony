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
/** 
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print.ipp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/*
 * IppClient is a print client based on IPP protocol.
 * (see Internet Printing Protocol, http://www.pwg.org/ipp/index.html)
 */


public class IppClient {
    protected static int verbose = 0;

    protected URI uri;
    protected URL url;
    protected HttpURLConnection server;

    public static int getVerbose() {
        return verbose;
    }

    public static void setVerbose(int newverbose) {
        verbose = newverbose;
    }

    public static void doVerbose(String v) {
        System.out.println(v);
    }

    public static void doVerbose(int level, String v) {
        if (verbose >= level) {
            System.out.println(v);
        }
    }

    public IppClient(URI clienturi) throws Exception {
        this.uri = clienturi;
        this.url = new URL("http", uri.getHost(), (uri.getPort() == -1 ? 631
                : uri.getPort()), (uri.getPath() == null ? "" : uri.getPath()));
    }

    public void request(String method, String content_type) throws Exception {
        connect();
        server.setDoInput(true);
        server.setDoOutput(true);
        server.setRequestMethod(method);
        server.setRequestProperty("Content-type", content_type);
        server.setAllowUserInteraction(false);
        server.connect();
    }

    public IppResponse request(byte[] data) throws Exception {
        connect();
        request("POST", "application/ipp", data);
        IppResponse response = new IppResponse(response().toByteArray());
        disconnect();

        return response;
    }

    public IppResponse request(IppRequest request) throws Exception {
        connect();
        request("POST", "application/ipp", request);
        IppResponse response = new IppResponse(response().toByteArray());
        disconnect();

        return response;
    }

    public void request(String method, String content_type, byte[] data)
            throws Exception {
        connect();
        server.setDoInput(true);
        server.setDoOutput(true);
        server.setRequestMethod(method);
        server.setRequestProperty("Content-type", content_type);
        server.setAllowUserInteraction(false);
        server.connect();

        doVerbose(2, "IppClient.java: request(): Write data to output stream");
        BufferedOutputStream bw = new BufferedOutputStream(server
                .getOutputStream());
        bw.write(data, 0, data.length);
        bw.flush();
        bw.close();
        doVerbose(2, "IppClient.java: request(): Write " + data.length
                + " bytes OK");
    }

    public void request(String method, String content_type, IppRequest request)
            throws Exception {
        connect();
        server.setDoInput(true);
        server.setDoOutput(true);
        server.setRequestMethod(method);
        server.setRequestProperty("Content-type", content_type);
        server.setAllowUserInteraction(false);
        server.connect();

        doVerbose(2, "IppClient.java: request(): Write data to output stream");
        DataOutputStream bw = new DataOutputStream(new BufferedOutputStream(
                server.getOutputStream()));

        byte[] data = request.getAgroups().getBytes();
        Object document = request.getDocument();

        bw.write(request.getVersion());
        bw.writeShort(request.getOperationId());
        bw.writeInt(request.getRequestId());
        bw.write(data, 0, data.length);
        doVerbose(2, "IppClient.java: request(): Write header OK");

        if (document != null) {
            if (document instanceof InputStream) {
                InputStream stream = (InputStream) document;
                byte[] buf = new byte[1024 * 8];
                int count = 0;

                doVerbose(
                        2,
                        "IppClient.java: request(): Write document data to output stream from InpuStream");
                while ((count = stream.read(buf, 0, buf.length - 10)) != -1) {
                    doVerbose(2, "IppClient.java: request(): Read " + count
                            + " bytes");
                    bw.write(buf, 0, count);
                    doVerbose(2, "IppClient.java: request(): Wrote " + count
                            + " bytes");
                }
                ((InputStream) document).close();
                doVerbose(2, "IppClient.java: request(): Close InputStream");
            } else if (document instanceof URL) {
                URLConnection urlconnection = ((URL) document).openConnection();

                doVerbose(2,
                        "IppClient.java: request(): Write document data to printer's stream from URL");
                doVerbose(1, "IppClient.java: request(): document to print: "
                        + ((URL) document).toString());
                try {
                    BufferedInputStream stream = new BufferedInputStream(
                            urlconnection.getInputStream());
                    byte[] buf = new byte[1024 * 8];
                    int count = 0;
                    while ((count = stream.read(buf, 0, buf.length)) != -1) {
                        doVerbose(2, "IppClient.java: request(): Read " + count
                                + " bytes from " + stream.toString());
                        bw.write(buf, 0, count);
                        doVerbose(2, "IppClient.java: request(): Wrote "
                                + count + " bytes");
                    }
                    stream.close();
                    doVerbose(2,
                            "IppClient.java: request(): Close InputStream "
                                    + stream.toString());
                } catch (IOException e) {
                    if (urlconnection instanceof HttpURLConnection
                            && ((HttpURLConnection) urlconnection)
                                    .getResponseCode() == 401) {
                        throw new IppException(
                                "HTTP/1.x 401 Unauthorized access to \n\t"
                                        + ((URL) document).toString());
                    }
                    throw e;
                }
            } else if (document instanceof byte[]) {
                InputStream stream = new ByteArrayInputStream((byte[]) document);
                byte[] buf = new byte[1024 * 8];
                int count = 0;

                while ((count = stream.read(buf, 0, buf.length)) != -1) {
                    bw.write(buf, 0, count);
                }
                stream.close();
            } else if (document instanceof char[]) {
                CharArrayReader stream = new CharArrayReader((char[]) document);
                char[] buf = new char[1024 * 8];
                int count = 0;

                while ((count = stream.read(buf, 0, buf.length)) != -1) {
                    bw.writeChars(new String(buf, 0, count));
                }
                stream.close();
            } else if (document instanceof String) {
                bw.writeChars((String) document);
            } else if (document instanceof Reader) {
                char[] buf = new char[1024 * 8];
                int count = 0;

                while ((count = ((Reader) document).read(buf, 0, buf.length)) != -1) {
                    bw.writeChars(new String(buf, 0, count));
                }
                ((Reader) document).close();
            }
        }

        bw.flush();
        bw.close();
        doVerbose(2, "IppClient.java: request(): Write OK");
    }

    public ByteArrayOutputStream response() throws Exception {
        ByteArrayOutputStream resp = new ByteArrayOutputStream(1024);
        byte[] buf = new byte[1024 * 8];
        int hasread = 0;

        doVerbose(2, "IppClient.java: response(): Read from server '"
                + server.toString() + "' to ByteArrayOutputStream");
        BufferedInputStream s = new BufferedInputStream(server.getInputStream());
        while ((hasread = s.read(buf, 0, buf.length)) > 0) {
            doVerbose(2, "IppClient.java: response(): Read  " + hasread
                    + " bytes from BufferedInputStream");
            resp.write(buf, 0, hasread);
            doVerbose(2, "IppClient.java: response(): Write " + hasread
                    + " bytes to ByteArrayOutputStream");
        }
        s.close();
        doVerbose(2, "IppClient.java: response(): Close  BufferedInputStream");

        return resp;
    }

    public HttpURLConnection connect() throws IOException {
        if (server == null) {
            //Authenticator.setDefault(new IppHttpAuthenticator());
            server = (HttpURLConnection) url.openConnection();
        }
        return server;
    }

    public void disconnect() {
        server.disconnect();
        server = null;
    }

}
