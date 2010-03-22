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

package org.apache.harmony.jndi.provider.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.StartTlsRequest;
import javax.net.SocketFactory;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.provider.ldap.LdapContextImpl.UnsolicitedListener;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Decodable;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.event.ECNotificationControl;
import org.apache.harmony.jndi.provider.ldap.event.PersistentSearchControl;
import org.apache.harmony.jndi.provider.ldap.event.PersistentSearchResult;
import org.apache.harmony.security.asn1.ASN1Integer;

/**
 * LdapClient is the actual class used to communicate with Ldap Server.
 * 
 */
public class LdapClient {
    /**
     * Socket used to communicate with Ldap Server.
     */
    private Socket socket;

    /**
     * Input stream of socket.
     */
    private InputStream in;

    /**
     * Output stream of socket.
     */
    private OutputStream out;

    /**
     * Address of connection
     */
    private String address;

    /**
     * port of connection
     */
    private int port;

    /**
     * blocked requests list which wait for response
     */
    private Hashtable<Integer, Element> requests = new Hashtable<Integer, Element>();

    private Hashtable<Integer, Element> batchedSearchRequests = new Hashtable<Integer, Element>();

    /**
     * the max time to wait server response in milli-second
     */
    private long MAX_WAIT_TIME = 30 * 1000;

    /**
     * responsible for dispatching received messages
     */
    private Dispatcher dispatcher;

    /**
     * registered UnsolicitedListener
     */
    private List<UnsolicitedListener> unls = new ArrayList<UnsolicitedListener>();

    /**
     * how may references point to this client
     */
    private int referCount = 0;

    // constructor for test
    public LdapClient() {
        // do nothing
    }

    /**
     * Constructor for LdapClient.
     * 
     * @param factory
     *            used to construct socket through its factory method
     * @param address
     *            the Internet Protocol (IP) address of ldap server
     * @param port
     *            the port number of ldap server
     * @throws UnknownHostException
     *             if the host cannot be resolved
     * @throws IOException
     *             if an error occurs while instantiating the socket
     */
    public LdapClient(SocketFactory factory, String address, int port)
            throws UnknownHostException, IOException {
        this.address = address;
        this.port = port;
        socket = factory.createSocket(address, port);
        // FIXME: Use of InputStreamWrap here is to deal with a potential bug of
        // RI.
        in = new InputStreamWrap(socket.getInputStream());
        out = socket.getOutputStream();
        dispatcher = new Dispatcher();
        dispatcher.start();
    }

    /**
     * The instance of the class is daemon thread, which read messages from
     * server and dispatch to corresponding thread.
     */
    class Dispatcher extends Thread {

        private boolean isStopped = false;

        public Dispatcher() {
            /**
             * must be daemon thread, otherwise can't destory by gc
             */
            setDaemon(true);
        }

        public boolean isStopped() {
            return isStopped;
        }

        public void setStopped(boolean isStopped) {
            this.isStopped = isStopped;
        }

        @Override
        public void run() {
            while (!isStopped) {
                try {
                    // set response op to null, load later
                    LdapMessage response = new LdapMessage(null) {

                        /**
                         * Dispatcher can't know which response operation should
                         * be used until messageId had determined.
                         * 
                         * @return response according messageId
                         */
                        @Override
                        public ASN1Decodable getResponseOp() {
                            // responseOp has been load, just return it
                            if (super.getResponseOp() != null) {
                                return super.getResponseOp();
                            }

                            int messageId = getMessageId();

                            // Unsolicited Notification
                            if (messageId == 0) {
                                return new UnsolicitedNotificationImpl();
                            }

                            // get response operation according messageId
                            Element element = requests.get(Integer
                                    .valueOf(messageId));
                            if (element == null) {
                                element = batchedSearchRequests.get(Integer
                                        .valueOf(messageId));
                            }

                            if (element != null) {
                                return element.response.getResponseOp();
                            }

                            /*
                             * FIXME: if messageId not find in request list,
                             * what should we do?
                             */
                            return null;
                        }
                    };

                    Exception ex = null;
                    /**
                     * TODO read message data by ourselves then decode, this
                     * would be robust
                     */
                    try {
                        // read next message
                        response.decode(in);
                    } catch (IOException e) {
                        // may socket has problem or decode occurs error
                        ex = e;
                    } catch (RuntimeException e) {
                        // may socket has problem or decode occurs error
                        ex = e;
                    }

                    processResponse(response, ex);

                } catch (Exception e) {
                    // may never reach
                    e.printStackTrace();
                }
            }

        }

        private void processResponse(LdapMessage response, Exception ex) {
            // unsolicited notification
            if (response.getMessageId() == 0) {
                notifyUnls(response);
                return;
            }

            Element element = requests.get(Integer.valueOf(response
                    .getMessageId()));
            if (element == null
                    && batchedSearchRequests.contains(Integer.valueOf(response
                            .getMessageId()))) {
                element = batchedSearchRequests.get(Integer.valueOf(response
                        .getMessageId()));
                // error occurs when read response
                if (ex != null) {
                    ((SearchOp) response.getResponseOp()).getSearchResult()
                            .setException(ex);
                    batchedSearchRequests.remove(Integer.valueOf(response
                            .getMessageId()));
                    return;
                }

                // wait time out
                if (element.response.getMessageId() != response.getMessageId()) {
                    // ldap.31=Read LDAP response message time out
                    ((SearchOp) response.getResponseOp()).getSearchResult()
                            .setException(
                                    new IOException(Messages
                                            .getString("ldap.31"))); //$NON-NLS-1$);
                    batchedSearchRequests.remove(Integer.valueOf(response
                            .getMessageId()));
                    return;
                }

            }
            if (element != null) {
                element.response = response;
                element.ex = ex;
                // persistent search response || search response
                if (element.lock == null) {
                    notifyPersistenSearchListener(element);

                } else {
                    if (element.response.getOperationIndex() == LdapASN1Constant.OP_EXTENDED_RESPONSE
                            && ((ExtendedOp) element.response.getResponseOp())
                                    .getExtendedRequest().getID().equals(
                                            StartTlsRequest.OID)) {
                        /*
                         * When establishing TLS by StartTls extended operation,
                         * no
                         */
                        isStopped = true;
                    }

                    /*
                     * notify the thread which send request and wait for
                     * response
                     */
                    synchronized (element.lock) {
                        element.lock.notify();
                    }
                } // end of if (element.lock == null) else
            } // end of if (element != null)

            else if (ex != null) {
                /*
                 * may asn1 decode error or socket problem, can get message id,
                 * so couldn't know which thread should be notified
                 */
                // FIXME: any better way?
                close();
            }
            // FIXME message id not found and no exception, what shoud we do?

        } // end of processResponse
    } // Dispatcher

    private void notifyUnls(LdapMessage response) {
        UnsolicitedNotificationImpl un = (UnsolicitedNotificationImpl) response
                .getResponseOp();
        for (UnsolicitedListener listener : unls) {
            listener.receiveNotification(un, response.getControls());
        }
    }

    /**
     * Carry out the ldap operation encapsulated in operation with controls.
     * 
     * @param operation
     *            the ldap operation
     * @param controls
     *            extra controls for some ldap operations
     * @return the encapsulated response message from ldap server
     * @throws IOException
     */
    public LdapMessage doOperation(LdapOperation operation, Control[] controls)
            throws IOException {
        return doOperation(operation.getRequestId(), operation.getRequest(),
                operation.getResponse(), controls);
    }

    /**
     * Send out the ldap operation in request with controls, and decode response
     * into LdapMessage.
     * 
     * @param opIndex
     * @param request
     *            the ldap request
     * @param response
     *            the ldap response
     * @param controls
     *            extra controls for some ldap operations
     * @return the encapsulated response message from ldap server
     * @throws IOException
     */
    public LdapMessage doOperation(int opIndex, ASN1Encodable request,
            ASN1Decodable response, Control[] controls) throws IOException {

        if (opIndex == LdapASN1Constant.OP_SEARCH_REQUEST) {
            return doSearchOperation(request, response, controls);
        }

        LdapMessage requestMsg = new LdapMessage(opIndex, request, controls);

        Integer messageID = Integer.valueOf(requestMsg.getMessageId());

        Object lock = new Object();
        requests.put(messageID, new Element(lock, new LdapMessage(response)));

        try {
            out.write(requestMsg.encode());
            out.flush();
            return waitResponse(messageID, lock);

        } finally {
            // remove request from list
            requests.remove(messageID);
        }

    }

    /**
     * Block the current thread until get response from server or occurs error
     * 
     * @param messageID
     *            id of request message, is same as id of response message
     * @param response
     *            decoder of the response
     * @return response message, may not be null
     * 
     * @throws Exception
     */
    private LdapMessage waitResponse(Integer messageID, Object lock)
            throws IOException {
        Element element = requests.get(messageID);

        /*
         * test if dispatcher has not received response message from server,
         * wait response
         */
        if (element.response.getMessageId() != messageID.intValue()) {

            synchronized (lock) {
                try {
                    lock.wait(MAX_WAIT_TIME);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        element = requests.get(messageID);

        // wait time out
        if (element.response.getMessageId() != messageID.intValue()) {
            // ldap.31=Read LDAP response message time out
            throw new IOException(Messages.getString("ldap.31")); //$NON-NLS-1$
        }

        // error occurs when read response
        if (element.ex != null) {
            // socket is not connected
            if (!socket.isConnected()) {
                close();
            }
            // element.ex must be one of IOException or RuntimeException
            if (element.ex instanceof IOException) {
                throw (IOException) element.ex;
            }

            throw (RuntimeException) element.ex;
        }

        return element.response;

    }

    private LdapMessage doSearchOperation(ASN1Encodable request,
            ASN1Decodable response, Control[] controls)
            throws IOException {
        int batchSize = ((SearchOp) request).getBatchSize();

        LdapMessage requestMsg = new LdapMessage(
                LdapASN1Constant.OP_SEARCH_REQUEST, request, controls);

        Integer messageID = Integer.valueOf(requestMsg.getMessageId());

        Object lock = new Object();
        requests.put(messageID, new Element(lock, new LdapMessage(response)));

        try {
            out.write(requestMsg.encode());
            out.flush();
            LdapMessage responseMsg = waitResponse(messageID, lock);
            int size = 1;
            while (responseMsg.getOperationIndex() != LdapASN1Constant.OP_SEARCH_RESULT_DONE) {
                if (size == batchSize) {
                    batchedSearchRequests.put(messageID, requests
                            .get(messageID));
                    break;
                }
                responseMsg = waitResponse(messageID, lock);
                ++size;
            }

            return responseMsg;
        } finally {
            // remove request from list
            requests.remove(messageID);
        }

    }

    public void abandon(final int messageId, Control[] controls)
            throws IOException {
        doOperationWithoutResponse(LdapASN1Constant.OP_ABANDON_REQUEST,
                new ASN1Encodable() {

                    public void encodeValues(Object[] values) {
                        values[0] = ASN1Integer.fromIntValue(messageId);
                    }

                }, controls);
    }

    public void doOperationWithoutResponse(int opIndex, ASN1Encodable op,
            Control[] controls) throws IOException {
        LdapMessage request = new LdapMessage(opIndex, op, controls);
        out.write(request.encode());
        out.flush();
    }

    public int addPersistentSearch(SearchOp op) throws IOException {
        LdapMessage request = new LdapMessage(
                LdapASN1Constant.OP_SEARCH_REQUEST, op.getRequest(),
                new Control[] { new PersistentSearchControl() });

        Integer messageID = Integer.valueOf(request.getMessageId());

        // set lock to null, indicate this is persistent search
        requests.put(messageID, new Element(null, new LdapMessage(op
                .getResponse())));
        try {
            out.write(request.encode());
            out.flush();
            return request.getMessageId();
        } catch (IOException e) {
            // send request faild, remove request from list
            requests.remove(messageID);
            throw e;
        }

    }

    public void removePersistentSearch(int messageId, Control[] controls)
            throws IOException {
        requests.remove(Integer.valueOf(messageId));
        abandon(messageId, controls);
    }

    /**
     * Close network connection, stop dispather thread, and release all other
     * resources
     * 
     * NOTE: invoke this method should be careful when this
     * <code>LdapClient</code> instance is shared by multi
     * <code>LdapContext</code>
     * 
     */
    public void close() {
        // close socket
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }

        socket = null;
        in = null;
        out = null;

        // try to stop dispather
        if (dispatcher != null) {
            dispatcher.setStopped(true);
            dispatcher.interrupt();
        }

        // notify all blocked thread
        if (requests != null) {
            for (Element element : requests.values()) {
                if (element.lock != null) {
                    synchronized (element.lock) {
                        element.lock.notify();
                    }
                } else {
                    // TODO notify persistent search listeners
                }
            }
            requests.clear();
            requests = null;
        }

    }

    /**
     * Get new instance of LdapClient according environment variable
     * 
     * @param envmt
     * @return
     * @throws NamingException
     */
    public static LdapClient newInstance(String host, int port,
            Hashtable<?, ?> envmt) throws NamingException {
        return newInstance(host, port, envmt, false);
    }

    public static LdapClient newInstance(String host, int port,
            Hashtable<?, ?> envmt, boolean isLdaps) throws NamingException {
        SocketFactory factory = LdapUtils.getSocketFactory(envmt, isLdaps);

        // TODO: get LdapClient from pool first.

        try {
            return new LdapClient(factory, host, port);
        } catch (IOException e) {
            CommunicationException ex = new CommunicationException();
            ex.setRootCause(e);
            throw ex;
        }
    }

    /**
     * struct for holding necessary info to add to requests list
     */
    static class Element {
        Object lock;

        LdapMessage response;

        Exception ex;

        public Element(Object lock, LdapMessage response) {
            this.lock = lock;
            this.response = response;
        }
    }

    // TODO: This class is used to deal with a potential bug of RI, may be
    // removed in the future.
    /**
     * When use <code>InputStream</code> from SSL Socket, if invoke
     * <code>InputStream.read(byte[])</code> with byte array of zero length,
     * the method will be blocked. Seems it's bug of ri.
     * 
     * This wrap class delegate all request to wrapped instance, except
     * returning immediately when the invoke
     * <code>InputStream.read(byte[])</code> with byte array of zero length.
     */
    static class InputStreamWrap extends InputStream {
        InputStream in;

        public InputStreamWrap(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public int read(byte[] bs, int offset, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            return in.read(bs, offset, len);
        }

        @Override
        public void reset() throws IOException {
            in.reset();

        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public void mark(int readlimit) {
            in.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return in.markSupported();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return in.read(b);
        }

        @Override
        public long skip(long n) throws IOException {
            return in.skip(n);
        }
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    protected void finalize() {
        close();
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new InputStreamWrap(socket.getInputStream());
        this.out = socket.getOutputStream();
        if (dispatcher != null) {
            dispatcher.setStopped(true);
            dispatcher.interrupt();
        }
        this.dispatcher = new Dispatcher();
        this.dispatcher.start();
    }

    public void addUnsolicitedListener(UnsolicitedListener listener) {
        if (unls == null) {
            unls = new ArrayList<UnsolicitedListener>();
        }

        if (!unls.contains(listener)) {
            unls.add(listener);
        }
    }

    // FIXME simple implementation
    public void use() {
        referCount++;
    }

    // FIXME simple implementation
    public void unuse() {
        referCount--;
    }

    private void notifyPersistenSearchListener(Element element) {
        PersistentSearchResult psr = (PersistentSearchResult) ((SearchOp) element.response
                .getResponseOp()).getSearchResult();
        // test error
        if (psr.getResult() != null) {
            psr.receiveNotificationHook(psr.getResult());
        }

        // notify listener
        Control[] cs = element.response.getControls();
        if (cs != null) {
            for (int i = 0; i < cs.length; i++) {
                Control control = cs[i];
                if (ECNotificationControl.OID.equals(control.getID())) {
                    psr.receiveNotificationHook(new ECNotificationControl(
                            control.getEncodedValue()));
                }
            }
        }
    }

    public int getReferCount() {
        return referCount;
    }
}
