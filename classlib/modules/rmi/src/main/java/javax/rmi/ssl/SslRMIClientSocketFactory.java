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

package javax.rmi.ssl;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.security.AccessController;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.harmony.rmi.internal.nls.Messages;

public class SslRMIClientSocketFactory implements RMIClientSocketFactory,
        Serializable {

    private static final long serialVersionUID = -8310631444933958385L;

    private static SSLSocketFactory factory;

    public SslRMIClientSocketFactory() {
        if (factory == null) {
            factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }

    public Socket createSocket(String host, int port) throws IOException {

        SSLSocket soc = (SSLSocket) factory.createSocket(host, port);
        String[] enabled =
            AccessController.doPrivileged(new java.security.PrivilegedAction<String[]>() {
                public String[] run() {
                    return new String[] {
                        System.getProperty("javax.rmi.ssl.client.enabledCipherSuites"), //$NON-NLS-1$
                        System.getProperty("javax.rmi.ssl.client.enabledProtocols")}; //$NON-NLS-1$
                }
            });
        try {
            if (enabled[0] != null) { //enabledCipherSuites
                soc.setEnabledCipherSuites(enabled[0].split(",")); //$NON-NLS-1$
            }

            if (enabled[1]!= null) { //enabledProtocols
                soc.setEnabledProtocols(enabled[1].split(",")); //$NON-NLS-1$
            }
        } catch (IllegalArgumentException e) {
            // rmi.96=Error in socket creation
            IOException ioe = new IOException(Messages.getString("rmi.96")); //$NON-NLS-1$
            ioe.initCause(e);
            soc.close();
            throw ioe;
        }

        return soc;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return this.getClass().equals(obj.getClass());
    }

    public int hashCode() {
        return "javax.rmi.ssl.SslRMIClientSocketFactory".hashCode(); //$NON-NLS-1$
    }

}
