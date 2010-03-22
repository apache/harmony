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

package org.apache.harmony.jndi.provider.ldap.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import org.apache.harmony.jndi.provider.ldap.LdapMessage;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.security.asn1.ASN1Integer;

/**
 * This class is a mock ldap server which only support one connection.
 * 
 * NOTE: before client send request to the mock server, must set expected
 * response message sequence using <code>etResponseSeq(LdapMessage[])</code>
 * method, so the server will send response messages in order of parameter
 * <code>LdapMessage[]</code>.
 */
public class MockLdapServer implements Runnable {

    private ServerSocket server;

    private Socket socket;

    private LinkedList<LdapMessage> responses = new LinkedList<LdapMessage>();

    private int port;

    private Object lock = new Object();

    private boolean isStopped;

    public MockLdapServer() {
        // do nothing
    }

    public MockLdapServer(MockLdapServer mockServer) {
        this.server = mockServer.server;
        port = mockServer.port;
    }

    public void start() throws IOException {
        if (server == null) {
            server = new ServerSocket(0);
            port = server.getLocalPort();
        }

        isStopped = false;
        new Thread(this).start();
    }

    public void stop() {
        isStopped = true;

        synchronized (lock) {
            lock.notify();
        }

        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                // ignore
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void setResponseSeq(LdapMessage[] msges) {
        synchronized (responses) {
            for (LdapMessage message : msges) {
                responses.addLast(message);
            }
        }

        synchronized (lock) {
            lock.notify();
        }
    }

    public void run() {
        InputStream in = null;
        OutputStream out = null;
        int searchID = -1;

        try {
            socket = server.accept();
            in = socket.getInputStream();
            out = socket.getOutputStream();
            while (!isStopped) {
                if (responses.size() == 0) {
                    try {
                        synchronized (lock) {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                } else {

                    boolean isContinue = false;

                    while (true) {
                        LdapMessage temp = null;
                        synchronized (responses) {
                            if (responses.size() == 0) {
                                break;
                            }
                            temp = responses.removeFirst();
                        }

                        final MockLdapMessage response = new MockLdapMessage(
                                temp);

                        if (!isContinue) {
                            LdapMessage request = new LdapMessage(null) {
                                public void decodeValues(Object[] values) {
                                    response.setMessageId(ASN1Integer
                                            .toIntValue(values[0]));
                                }
                            };

                            request.decode(in);

                            if (response.getOperationIndex() == LdapASN1Constant.OP_SEARCH_RESULT_ENTRY
                                    || response.getOperationIndex() == LdapASN1Constant.OP_SEARCH_RESULT_REF) {
                                isContinue = true;
                                searchID = response.getMessageId();
                            } else {
                                isContinue = false;
                            }
                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // ignore
                        }

                        if (isContinue) {
                            response.setMessageId(searchID);
                        }
                        out.write(response.encode());
                    }
                }
            }
        } catch (IOException e) {
            // FIXME deal with the exception
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }

                if (in != null) {
                    in.close();
                }

                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void disconnectNotify() throws IOException {
        MockLdapMessage message = new MockLdapMessage(new LdapMessage(
                LdapASN1Constant.OP_EXTENDED_RESPONSE,
                new DisconnectResponse(), null));
        message.setMessageId(0);
        OutputStream out = socket.getOutputStream();
        out.write(message.encode());
    }

    public String getURL() {
        return "ldap://localhost:" + port;
    }
}
