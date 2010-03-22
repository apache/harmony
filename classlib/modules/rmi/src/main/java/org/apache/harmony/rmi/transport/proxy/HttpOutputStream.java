/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
package org.apache.harmony.rmi.transport.proxy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;

import org.apache.harmony.rmi.common.GetStringPropAction;
import org.apache.harmony.rmi.common.RMIConstants;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Output stream for HTTP connections.
 * It sends data only once, wrapped into HTTP response.
 *
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public class HttpOutputStream extends ByteArrayOutputStream
        implements ProxyConstants {

    /**
     * Underlying output stream.
     */
    private DataOutputStream out;

    /**
     * If this is inbound connection stream.
     */
    private boolean inbound;

    /**
     * Target host name (for HTTP headers).
     */
    private String host;

    /**
     * Target port number (for HTTP headers).
     */
    private int port;

    /**
     * If this is CGI stream.
     */
    private boolean cgi;

    /**
     * If this stream was closed.
     */
    private boolean isClosed = false;

    /**
     * Constructs this stream by wrapping the specified output stream.
     * The resulting stream doesn't use CGI.
     *
     * @param   out
     *          Output stream to wrap.
     *
     * @param   inbound
     *          If this is inbound connection stream.
     *
     * @param   host
     *          Target host name.
     *          Used for HTTP headers (for outbound streams)
     *          and for diagnostics.
     *          Optional (can be <code>null</code>) for inbound streams.
     *
     * @param   port
     *          Target port number.
     *          Used for HTTP headers (for outbound streams),
     *          and for diagnostics.
     *          Is ignored if <code>host</code> is <code>null</code>).
     */
    public HttpOutputStream(OutputStream out, boolean inbound,
            String host, int port) {
        this(out, inbound, host, port, false);
    }

    /**
     * Constructs this stream by wrapping the specified output stream.
     *
     * @param   out
     *          Output stream to wrap.
     *
     * @param   inbound
     *          If this is inbound connection stream.
     *
     * @param   host
     *          Target host name.
     *          Used for HTTP headers for outbound streams,
     *          may be used for diagnostics purposes for all streams.
     *          Optional (can be <code>null</code>) for inbound streams.
     *
     * @param   port
     *          Target port number.
     *          Used for HTTP headers for outbound streams,
     *          may be used for diagnostics purposes for all streams.
     *          Optional for inbound streams.
     *
     * @param   cgi
     *          If this is CGI stream (ignored for inbound streams).
     */
    public HttpOutputStream(OutputStream out, boolean inbound,
            String host, int port, boolean cgi) {
        super();
        this.out = new DataOutputStream(out);
        this.inbound = inbound;
        this.host = host;
        this.port = port;
        this.cgi = (inbound ? false : cgi);
    }

    /**
     * Converts the specified string to bytes using {@link String#getBytes()}
     * and writes to the stream.
     *
     * @param   s
     *          String to write.
     *
     * @throws  IOException
     *          If I/O error occurs.
     *
     * @see     java.io.DataOutput#writeBytes(String)
     */
    public final void writeBytes(String s) throws IOException {
        write(s.getBytes());
    }

    /**
     * Wraps all data contained in this stream into HTTP response and writes it
     * into the underlying output stream. This method can only be called once.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public synchronized void close() throws IOException {
        if (isClosed) {
            // rmi.88=Repeated attempt to close HttpOutputStream
            throw new IOException(Messages.getString("rmi.88")); //$NON-NLS-1$
        }

        // Port the outbound connection is established to.
        int connectPort = (cgi ? RMIConstants.HTTP_DEFAULT_PORT : port);

        // Sending HTTP POST request or OK response.
        //
        // Note: the following things may need reconsidering in future:
        //          - What headers should really be present here.
        //          - Which HTTP protocol version should be used.
        //          - What User-Agent name and version should be used.
        //          - What proxy control headers should be included.
        //
        // Note: reference implementation uses the following headers
        //      (retrieved using the black box testing by writing a dummy
        //       socket server that logs everything that comes to it,
        //       and making an RMI request to it with reference implementation):
        //          POST http://HOST:PORT/ HTTP/1.1
        //          Content-type: application/octet-stream
        //          Cache-Control: no-cache
        //          Pragma: no-cache
        //          User-Agent: Java/1.4.2_04
        //          Host: HOST:PORT
        //          Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2
        //          Proxy-Connection: keep-alive
        //          Content-Length: LENGTH
        out.writeBytes((inbound ? HTTP_RESPONSE_HEADER :
                (HTTP_REQUEST_SIGNATURE + "http://" + host + ':' + connectPort //$NON-NLS-1$
                    + '/' + (cgi ? ("cgi-bin/java-rmi?forward=" + port) : "") //$NON-NLS-1$ //$NON-NLS-2$
                    + " HTTP/1.1" + EOLN //$NON-NLS-1$
                + "Cache-Control: no-cache" + EOLN + "Pragma: no-cache" + EOLN //$NON-NLS-1$ //$NON-NLS-2$
                + "Host: " + host + ':' + connectPort + EOLN //$NON-NLS-1$
                + "Proxy-Connection: keep-alive" + EOLN //$NON-NLS-1$
                + "User-Agent: DRL/" + (String) AccessController.doPrivileged( //$NON-NLS-1$
                        new GetStringPropAction("java.version")))) + EOLN); //$NON-NLS-1$

        out.writeBytes("Content-type: application/octet-stream" + EOLN //$NON-NLS-1$
                + CONTENT_LENGTH_SIGNATURE + ' ' + count + EOLN + EOLN);
        out.write(buf, 0, count);
        out.flush();

        reset();

        isClosed = true;

        if (proxyTransportLog.isLoggable(RMILog.VERBOSE)) {
            proxyTransportLog.log(RMILog.VERBOSE,
                    "HTTP " + (inbound ? "response" : "request") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + ((host != null) ? (" to [" + host + ':' + port + ']') //$NON-NLS-1$
                            : "") + " sent."); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
