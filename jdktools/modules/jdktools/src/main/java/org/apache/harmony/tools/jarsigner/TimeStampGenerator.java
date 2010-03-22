/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.tools.jarsigner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.harmony.security.pkcs7.ContentInfo;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.tsp.MessageImprint;
import org.apache.harmony.security.x509.tsp.TimeStampReq;
import org.apache.harmony.security.x509.tsp.TimeStampResp;


/**
 * Class to generate time stamps. 
 */
class TimeStampGenerator {
    
    
    /**
     * Generates a time-stamp request, sends it to the TSA, gets the response,
     * decodes it and returns the TimeStampToken.
     * 
     * @param digest
     * @param algID
     * @param tsaURI
     * @param proxyAddr
     * @param proxyPort
     * @param proxyType
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    static ContentInfo genTimeStamp(byte[] digest, AlgorithmIdentifier algID,
            URI tsaURI, String proxyAddr, int proxyPort, Proxy.Type proxyType)
            throws NoSuchAlgorithmException, IOException {
        
        String errMsgAddrUsing = tsaURI
                + " using "
                + ((proxyAddr == null) ? "direct connection (no proxy)"
                        : proxyType + " proxy " + proxyAddr + ":" + proxyPort);
        
        // create the time-stamp request
        byte[] timeStampReq = generateTimeStampReq(digest, algID);

        // set up the connection to TSA server
        HttpURLConnection conn;
        InputStream in;
        OutputStream out;
        try {
            conn = setConnection(tsaURI, proxyAddr, proxyPort, proxyType,
                    timeStampReq.length);
            conn.connect();
            in = conn.getInputStream();
            out = conn.getOutputStream();
        } catch (IOException e) {
            String errMsg = "Cannot connect to " + errMsgAddrUsing;
            throw (IOException) new IOException(errMsg).initCause(e);
        }
        
        // send the request
        try {
            out.write(timeStampReq);
            out.flush();
            out.close();
        } catch (IOException e) {
            String errMsg = "Cannot post the request to " + errMsgAddrUsing;
            throw (IOException) new IOException(errMsg).initCause(e);
        }
        
        // get the response
        // byte buffer to contain the response from TSA
        byte[] respBytes;
        // total response length
        int respLen = 0;
        try {
            // Time-stamp response is usually less than 8 Kbytes.
            respBytes = new byte[8092];
            // try to read the answer in several packets
            
            // length of the current chunk of data
            int chunkLen = 0;
            do {
                int freeRespBytesSpace = respBytes.length - respLen;
                chunkLen = in.read(respBytes, respLen, freeRespBytesSpace);
                if (chunkLen > 0) {
                    respLen += chunkLen;
                    
                    // if the respBytes buffer is full
                    if (chunkLen == freeRespBytesSpace) {
                        byte [] biggerBuffer = new byte [respBytes.length * 2];
                        System.arraycopy(respBytes, 0, biggerBuffer, 0,
                                respBytes.length);
                        respBytes = biggerBuffer;
                    }
                }
            } while (chunkLen > 0);
        } catch (IOException e) {
            String errMsg = "Cannot get response from " + errMsgAddrUsing;
            throw (IOException) new IOException(errMsg).initCause(e);
        }
        conn.disconnect();
        
        // return the decoded response or throw an IOException
        return decodeResponse(respBytes, respLen);
    }
    
    // generates a TimeStampReq and returns its ASN1 DER encoding
    private static byte[] generateTimeStampReq(byte[] digest,
            AlgorithmIdentifier algID) throws NoSuchAlgorithmException {
        MessageImprint msgImprint = new MessageImprint(algID, digest);
        SecureRandom random;
        String randAlgName = "SHA1PRNG";
        try {
            random = SecureRandom.getInstance(randAlgName);
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("The algorithm " + randAlgName
                    + " is not available in current environment.", e);
        }
        BigInteger nonce = BigInteger.valueOf(random.nextLong());
        TimeStampReq req = new TimeStampReq(1, // version
                msgImprint,     // message imprint
                null,           // not asking for a particular policy
                nonce,          // nonce
                Boolean.FALSE,  // don't need the certificate inside the stamp
                null);          // no extensions
        return req.getEncoded();
    }
    
    // Creates a connection and sets up its properties,
    // returns the created connection.
    private static HttpURLConnection setConnection(URI tsaURI,
            String proxyAddr, int proxyPort, Proxy.Type proxyType,
            int contentLength) throws IOException {

        URL tsaURL = tsaURI.toURL();
        
        // FIXME: if proxy is not set!
        InetSocketAddress proxyInetAddr = new InetSocketAddress(proxyAddr,
                proxyPort);
        Proxy proxy = new Proxy(proxyType, proxyInetAddr);
        HttpURLConnection conn = (HttpURLConnection) tsaURL
                .openConnection(proxy);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            // The exception cannot be thrown as it is thrown only if:
            // - the method is called after the connection is set,
            // - POST is not supported.
            throw new RuntimeException("\"POST\" is not supported by "
                    + "the HTTP protocol implementation");
        }
        conn.setRequestProperty("accept", "application/timestamp-reply");
        conn.setRequestProperty("content-type", "application/timestamp-query");
        conn.setRequestProperty("Content-Length",
                new String("" + contentLength));
        return conn;
    }
    
    // decodes the response from TSA
    private static ContentInfo decodeResponse(byte[] respBytes, int respLen)
            throws IOException {
        try {
            TimeStampResp resp = (TimeStampResp) TimeStampResp.ASN1.decode(
                    respBytes, 0, respLen);
            return resp.getTimeStampToken();
        } catch (IOException e) {
            // If failed to decode the response as a TimeStampResp,
            // try to decode it as a TimeStampToken because some TSA-s
            // return the token (not TimeStampResp) on success in spite 
            // of this conflicts with the RFC 3161.
            try {
                return (ContentInfo) ContentInfo.ASN1.decode(respBytes, 0,
                        respLen);
            } catch (IOException ioe) {
                String errMsg = "Cannot parse the response from TSA";
                throw (IOException) new IOException(errMsg).initCause(e);
            }
        }
    }
}

