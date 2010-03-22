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
 * @author  Mikhail A. Markov
 */
package java.rmi.server;

import java.io.InputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.harmony.luni.util.InputStreamHelper;
import org.apache.harmony.rmi.DefaultRMIClassLoaderSpi;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * @com.intel.drl.spec_ref
 * This class could not be instantiated.
 *
 * @author  Mikhail A. Markov
 */
public class RMIClassLoader {

    // The name of property for custom RMIClassLoaderSpi.
    private static final String spiProp = "java.rmi.server.RMIClassLoaderSpi"; //$NON-NLS-1$

    // The name of resource for custom RMIClassLoaderSpi.
    private static final String spiResource =
            "META-INF/services/java.rmi.server.RMIClassLoaderSpi"; //$NON-NLS-1$

    /*
     * Default RMIClassLoaderSpi instance.
     */
    private static RMIClassLoaderSpi defaultSpi =
            new DefaultRMIClassLoaderSpi();

    /*
     * Implementation of RMIClassLoaderSpi which will be used for delegating
     * method calls of this class to.
     */
    private static RMIClassLoaderSpi activeSpi =
            (RMIClassLoaderSpi) AccessController.doPrivileged(
                    new PrivilegedAction<Object>() {
                        public Object run() {
                            return initActiveSpi();
                        }
                    });

    // This class could not be instantiated.
    private RMIClassLoader() {
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Class<?> loadProxyClass(String codebase,
                                       String[] interf,
                                       ClassLoader defaultCl)
            throws ClassNotFoundException, MalformedURLException {
        return activeSpi.loadProxyClass(codebase, interf, defaultCl);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Class<?> loadClass(String codebase,
                                  String name,
                                  ClassLoader defaultCl)
            throws MalformedURLException, ClassNotFoundException {
        return activeSpi.loadClass(codebase, name, defaultCl);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Class<?> loadClass(URL codebase, String name)
            throws MalformedURLException, ClassNotFoundException {
        return activeSpi.loadClass((codebase == null) ? null
                                    : codebase.toString(), name, null);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Class<?> loadClass(String codebase, String name)
            throws MalformedURLException, ClassNotFoundException {
        return activeSpi.loadClass(codebase, name, null);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static String getClassAnnotation(Class<?> cl) {
        return activeSpi.getClassAnnotation(cl);
    }

    /**
     * @com.intel.drl.spec_ref
     * It's depricated so we just return null.
     * @deprecated since Java v1.2 this method is no longer used by RMI
     *  framework
     */
    @Deprecated
    public static Object getSecurityContext(ClassLoader loader) {
        return null;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static ClassLoader getClassLoader(String codebase)
            throws MalformedURLException, SecurityException {
        return activeSpi.getClassLoader(codebase);
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated method loadClass(String, String) should be used instead
     */
    @Deprecated
    public static Class<?> loadClass(String name)
            throws MalformedURLException, ClassNotFoundException {
        return loadClass((String) null, name);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static RMIClassLoaderSpi getDefaultProviderInstance() {
        SecurityManager mgr = System.getSecurityManager();

        if (mgr != null) {
            mgr.checkPermission(new RuntimePermission("setFactory")); //$NON-NLS-1$
        }
        return defaultSpi;
    }

    /*
     * Initialize active RMIClassLoaderSpi.
     */
    private static RMIClassLoaderSpi initActiveSpi() {
        String spi = System.getProperty(spiProp);

        if (spi != null) {
            if (spi.equals("default")) { //$NON-NLS-1$
                return defaultSpi;
            }

            try {
                return (RMIClassLoaderSpi) (Class.forName(spi, false,
                        ClassLoader.getSystemClassLoader()).newInstance());
            } catch (Exception ex) {
                // rmi.1B=Unable to initialize RMIClassLoaderSpi instance {0}, specified in {1} property
                throw new Error(Messages.getString("rmi.1B", spi, spiProp), ex); //$NON-NLS-1$
            }
        }

        try {
            spi = getSpiFromResource();
        } catch (IOException ioe) {
            // rmi.1C=Unable to get RMIClassLoaderSpi name from resource {0}
            throw new Error(Messages.getString("rmi.1C", spiResource), ioe); //$NON-NLS-1$
        }

        if (spi != null) {
            try {
                return (RMIClassLoaderSpi) (Class.forName(spi, true,
                        ClassLoader.getSystemClassLoader()).newInstance());
            } catch (Exception ex) {
                // rmi.1D=Unable to initialize RMIClassLoaderSpi instance {0}, specified in {1} resource
                throw new Error(Messages.getString("rmi.1D", spi, spiResource), ex); //$NON-NLS-1$
            }
        }
        return defaultSpi;
    }

    /*
     * Returns provider obtained from default resource.
     *
     * @return provider obtained from default resource
     *
     * @throws IOException if any I/O error occurred while trying to read
     *         provider name from default resource
     */
    private static String getSpiFromResource() throws IOException {
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(
                spiResource);

        if (in == null) {
            // resource not found
            return null;
        }
        byte[] buf = InputStreamHelper.readFullyAndClose(in);
        String str = new String(buf, "UTF-8"); //$NON-NLS-1$
        StringTokenizer tok = new StringTokenizer(str, "\n\r"); //$NON-NLS-1$

        while (tok.hasMoreTokens()) {
            String spiName = tok.nextToken();
            int idx = spiName.indexOf("#"); //$NON-NLS-1$

            if (idx != -1) {
                // this is commented line
                spiName = spiName.substring(0, idx);
            }
            spiName = spiName.trim();

            if (spiName.length() > 0) {
                // not empty line
                return spiName;
            }

            // skip empty line
        }

        // we did not found any uncommented non-empty lines
        return ""; //$NON-NLS-1$
    }
}
