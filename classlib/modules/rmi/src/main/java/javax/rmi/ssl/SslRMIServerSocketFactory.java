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
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Arrays;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class SslRMIServerSocketFactory implements RMIServerSocketFactory {

    static SSLServerSocketFactory factory;

    private String[] enabledCipherSuites;

    private String[] enabledProtocols;

    private boolean needClientAuth;

    public SslRMIServerSocketFactory() {

        if (factory == null) {
            factory = (SSLServerSocketFactory) SSLServerSocketFactory
                    .getDefault();
        }
    }

    public SslRMIServerSocketFactory(String[] enabledCipherSuites,
            String[] enabledProtocols, boolean needClientAuth)
            throws IllegalArgumentException {

        if (factory == null) {
            factory = (SSLServerSocketFactory) SSLServerSocketFactory
                    .getDefault();
        }
        SSLServerSocket soc = null;
        try {
            soc = (SSLServerSocket) factory.createServerSocket();
            if (enabledProtocols != null) {
                soc.setEnabledProtocols(enabledProtocols);
            }
            if (enabledCipherSuites != null) {
                soc.setEnabledCipherSuites(enabledCipherSuites);
            }
            this.enabledCipherSuites = enabledCipherSuites;
            this.enabledProtocols = enabledProtocols;
            this.needClientAuth = needClientAuth;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            try {
                if (soc != null) {
                    soc.close();
                }
            } catch (IOException e) {
            }
        }

    }

    public final String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    public final String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    public final boolean getNeedClientAuth() {
        return needClientAuth;
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        SSLServerSocket soc;

        soc = (SSLServerSocket) factory.createServerSocket(port);
        if (enabledProtocols != null) {
            soc.setEnabledProtocols(enabledProtocols);
        }
        if (enabledCipherSuites != null) {
            soc.setEnabledCipherSuites(enabledCipherSuites);
        }
        soc.setNeedClientAuth(needClientAuth);
        return soc;
    }

    public boolean equals(Object obj) {

        if (obj instanceof SslRMIServerSocketFactory
                && Arrays.equals(enabledCipherSuites,
                        ((SslRMIServerSocketFactory) obj)
                            .getEnabledCipherSuites())
                && Arrays.equals(enabledProtocols,
                         ((SslRMIServerSocketFactory) obj)
                            .getEnabledProtocols())
                && (this.needClientAuth == ((SslRMIServerSocketFactory) obj)
                        .getNeedClientAuth())) {
            return true;
        }
        return false;
    }

    public int hashCode() {

        String hashSting = "javax.rmi.ssl.SslRMIServerSocketFactory"; //$NON-NLS-1$
        if (enabledCipherSuites != null) {
            for (int i = 0; i < enabledCipherSuites.length; i++) {
                hashSting = hashSting + enabledCipherSuites[i];
            }
        }
        if (enabledProtocols != null) {
            for (int i = 0; i < enabledProtocols.length; i++) {
                hashSting = hashSting + enabledProtocols[i];
            }
        }
        return hashSting.hashCode();
    }

}
