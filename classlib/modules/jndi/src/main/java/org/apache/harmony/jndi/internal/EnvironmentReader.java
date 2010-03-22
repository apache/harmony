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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.jndi.internal;

import java.applet.Applet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This is a utility class that reads environment properties.
 */
public final class EnvironmentReader {

    // The name of application resource files
    private static final String APPLICATION_RESOURCE_FILE = "jndi.properties"; //$NON-NLS-1$

    // The name of provider resource file
    private static final String PROVIDER_RESOURCE_FILE = "jndiprovider.properties"; //$NON-NLS-1$

    // Not allowed to create an instance
    private EnvironmentReader() {
        super();
    }

    /*
     * Merge additional properties with already read ones.
     * 
     * @param src - the source containing additional properties @param dst - the
     * destination to put additional properties @param valueAddToList - whether
     * to add new values of C-type properties
     */
    public static void mergeEnvironment(final Hashtable<?, ?> src,
            final Hashtable<Object, Object> dst, final boolean valueAddToList) {

        Object key = null;
        String val = null;
        Enumeration<?> keys = src.keys();

        while (keys.hasMoreElements()) {
            key = keys.nextElement();

            if (!dst.containsKey(key)) {
                /*
                 * If this property doesn't exist yet, add it.
                 */
                dst.put(key, src.get(key));
            } else if (valueAddToList
                    && (LdapContext.CONTROL_FACTORIES.equals(key)
                            || Context.OBJECT_FACTORIES.equals(key)
                            || Context.STATE_FACTORIES.equals(key) || Context.URL_PKG_PREFIXES
                            .equals(key))) {
                /*
                 * Otherwise, if this property can contain a list of values, add
                 * the additional values if the flag "valueAddToList" is true.
                 */

                // Read the original value
                val = (String) dst.get(key);
                // Values are combined into a single list separated by colons.
                val = val + ":" + src.get(key); //$NON-NLS-1$
                // The final value becomes the resulting value of that property
                dst.put(key, val);
            } else {
                /*
                 * Otherwise, ignore the found value.
                 */
            }
        }
    }

    /*
     * Get the required 7 JNDI properties from JNDI properties source. This
     * method is designed as package visibility to improve performance when
     * called by anonymous inner classes.
     * 
     * @return a hashtable holding the required properties.
     */
    static Hashtable<Object, Object> filterProperties(
            final JNDIPropertiesSource source) {
        final Hashtable<Object, Object> filteredProperties = new Hashtable<Object, Object>();
        String propValue = null;

        propValue = source.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        if (null != propValue) {
            filteredProperties.put(Context.INITIAL_CONTEXT_FACTORY, propValue);
        }

        propValue = source.getProperty(Context.DNS_URL);
        if (null != propValue) {
            filteredProperties.put(Context.DNS_URL, propValue);
        }

        propValue = source.getProperty(Context.PROVIDER_URL);
        if (null != propValue) {
            filteredProperties.put(Context.PROVIDER_URL, propValue);
        }

        propValue = source.getProperty(Context.OBJECT_FACTORIES);
        if (null != propValue) {
            filteredProperties.put(Context.OBJECT_FACTORIES, propValue);
        }

        propValue = source.getProperty(Context.STATE_FACTORIES);
        if (null != propValue) {
            filteredProperties.put(Context.STATE_FACTORIES, propValue);
        }

        propValue = source.getProperty(Context.URL_PKG_PREFIXES);
        if (null != propValue) {
            filteredProperties.put(Context.URL_PKG_PREFIXES, propValue);
        }

        propValue = source.getProperty(LdapContext.CONTROL_FACTORIES);
        if (null != propValue) {
            filteredProperties.put(LdapContext.CONTROL_FACTORIES, propValue);
        }

        return filteredProperties;
    }

    /*
     * Read the required 7 JNDI properties from system properties and merge with
     * existing properties. Note that the values of C-type properties are only
     * included when no corresponding value is presented in existing properties.
     * 
     * @param existingProps - existing properties
     */
    public static void readSystemProperties(
            final Hashtable<Object, Object> existingProps) {
        /*
         * Privileged code is used to access system properties. This is required
         * if JNDI is run in Applet or other applications which only have
         * limited permissions to access certain resources.
         */
        Hashtable<Object, Object> systemProperties = AccessController
                .doPrivileged(new PrivilegedAction<Hashtable<Object, Object>>() {
                    public Hashtable<Object, Object> run() {
                        return filterProperties(new SystemPropertiesSource());
                    }
                });
        mergeEnvironment(systemProperties, existingProps, false);
    }

    /*
     * Read the required 7 JNDI properties from applet parameters and merge with
     * existing properties. Note that the values of C-type properties are only
     * included when no corresponding value is presented in existing properties.
     * 
     * @param applet - the applet object @param existingProps - existing
     * properties
     */
    public static void readAppletParameters(Object applet,
            Hashtable<Object, Object> existingProps) {
        if (null != applet) {
            Hashtable<Object, Object> appletParameters = filterProperties(new AppletParametersSource(
                    (Applet) applet));
            mergeEnvironment(appletParameters, existingProps, false);
        }
    }

    /*
     * Read multiple resource files from the classpaths given the file name.
     * This method is designed as package visibility to improve performance when
     * called by anonymous inner classes.
     * 
     * @param name - the name of the resource file @param existingProps -
     * existing properties, cannot be null @param filter - to filter properties
     */
    static Hashtable<Object, Object> readMultipleResourceFiles(
            final String name, final Hashtable<Object, Object> existingProps,
            ClassLoader cl) throws NamingException {

        if (null == cl) {
            cl = ClassLoader.getSystemClassLoader();
        }

        Enumeration<URL> e = null;
        try {
            // Load all resource files
            e = cl.getResources(name);
        } catch (IOException ex) {
            // Unexpected ClassLoader exception
            // jndi.23=Failed to load JNDI resource files.
            ConfigurationException newEx = new ConfigurationException(Messages
                    .getString("jndi.23")); //$NON-NLS-1$
            newEx.setRootCause(ex);
            throw newEx;
        }

        // Read all the loaded properties and merge
        URL url = null;
        InputStream is = null;
        final Properties p = new Properties();
        while (e.hasMoreElements()) {
            url = e.nextElement();
            try {
                if (null != (is = url.openStream())) {
                    p.load(is);
                    mergeEnvironment(p, existingProps, true);
                    p.clear();
                }
            } catch (IOException ex) {
                // Can't read this resource file
                // jndi.24=Failed to read JNDI resource files.
                ConfigurationException newEx = new ConfigurationException(
                        Messages.getString("jndi.24")); //$NON-NLS-1$
                newEx.setRootCause(ex);
                throw newEx;
            } finally {
                try {
                    if (null != is) {
                        is.close();
                    }
                } catch (IOException ex) {
                    // Ignore closing exception
                } finally {
                    is = null;
                }
            }
        }
        return existingProps;
    }

    /*
     * Read application/applet resource files.
     * 
     * @param existingProps - existing properties, cannot be null.
     */
    public static Hashtable<Object, Object> readApplicationResourceFiles(
            final Hashtable<Object, Object> existingProps)
            throws NamingException {
        // Use privileged code to read the application resource files
        try {
            AccessController
                    .doPrivileged(new PrivilegedExceptionAction<Void>() {
                        public Void run() throws NamingException {
                            readMultipleResourceFiles(
                                    APPLICATION_RESOURCE_FILE, existingProps,
                                    Thread.currentThread()
                                            .getContextClassLoader());
                            return null;
                        }
                    });
        } catch (PrivilegedActionException e) {
            Exception rootCause = e.getException();
            if (rootCause instanceof NamingException) {
                throw (NamingException) rootCause;
            } else if (rootCause instanceof RuntimeException) {
                throw (RuntimeException) rootCause;
            } else {
                // This should not happen.
            }
        }
        return existingProps;
    }

    /*
     * Read the properties file "java.home"/lib/jndi.properties. Pay attention
     * to the privileged code for accessing this external resource file. This is
     * required if JNDI is run in Applet or other applications which only have
     * limited permissions to access certain resources.
     * 
     * @param existingProps - existing properties, cannot be null.
     */
    public static Hashtable<Object, Object> readLibraryResourceFile(
            final Hashtable<Object, Object> existingProps)
            throws NamingException {
        final String sep = System.getProperty("file.separator"); //$NON-NLS-1$

        String resPath = null;
        // Construct the full filename of "java.home"/lib/jndi.properties
        resPath = AccessController.doPrivileged(new PrivilegedAction<String>(){
            public String run() {
                return System.getProperty("java.home"); //$NON-NLS-1$ 
            }
        }); 
        
        if (!resPath.endsWith(sep)) {
            resPath += sep;
        }
        resPath += "lib" + sep + APPLICATION_RESOURCE_FILE; //$NON-NLS-1$

        // Try to read this properties if it exists
        InputStream is = null;
        final File resFile = new File(resPath);
        final Properties p = new Properties();
        // Use privileged code to determine whether the file exists
        boolean resFileExists = AccessController.doPrivileged(
                new PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        return Boolean.valueOf(resFile.exists());
                    }
                }).booleanValue();
        if (resFileExists) {
            try {
                // Use privileged code to read the file
                is = AccessController
                        .doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                            public FileInputStream run() throws IOException {
                                FileInputStream localInputStream = new FileInputStream(
                                        resFile);
                                p.load(localInputStream);
                                return localInputStream;
                            }
                        });
                mergeEnvironment(p, existingProps, true);
            } catch (PrivilegedActionException e) {
                // Can't read "java.home"/lib/jndi.properties
                // jndi.25=Failed to read JNDI resource files in java home
                // library.
                ConfigurationException newEx = new ConfigurationException(
                        Messages.getString("jndi.25")); //$NON-NLS-1$
                newEx.setRootCause(e.getException());
                throw newEx;
            } finally {
                try {
                    if (null != is) {
                        is.close();
                    }
                } catch (IOException ex) {
                    // Ignore closing exception
                }
            }
        }
        return existingProps;
    }

    /*
     * Read the service provider resource file.
     * 
     * @param context - the context @param existingProps - existing properties,
     * cannot be null.
     */
    public static Hashtable<Object, Object> readProviderResourceFiles(
            final Context context, final Hashtable<Object, Object> existingProps)
            throws NamingException {

        String factory = context.getClass().getName();
        String resPath = null;
        int len = factory.lastIndexOf('.');

        // Construct the full filename of the service provider resource file
        if (-1 == len) {
            // Default package
            resPath = PROVIDER_RESOURCE_FILE;
        } else {
            // Replace "." with '/'
            resPath = factory.substring(0, len + 1);
            resPath = resPath.replace('.', '/');
            resPath += PROVIDER_RESOURCE_FILE;
        }

        // Use privileged code to read the provider resource files
        try {
            final String finalResPath = resPath;
            AccessController
                    .doPrivileged(new PrivilegedExceptionAction<String>() {
                        public String run() throws NamingException {
                            readMultipleResourceFiles(finalResPath,
                                    existingProps, context.getClass()
                                            .getClassLoader());
                            return null;
                        }
                    });
        } catch (PrivilegedActionException e) {
            Exception rootCause = e.getException();
            if (rootCause instanceof NamingException) {
                throw (NamingException) rootCause;
            } else if (rootCause instanceof RuntimeException) {
                throw (RuntimeException) rootCause;
            } else {
                // This should not happen.
                throw new AssertionError(rootCause);
            }
        }
        return existingProps;
    }

    /*
     * Get the list of the specified factory names from the supplied environment
     * and the resource provider files of the given Context.
     * 
     * @param envmt The supplied environment. @param ctx The Context whose
     * resource provider files will be read. @param key The name of the factory.
     * @return The list of the desired factory names. @throws NamingException If
     * an error occurs when reading the provider resource files.
     */
    public static String[] getFactoryNamesFromEnvironmentAndProviderResource(
            Hashtable<?, ?> envmt, Context ctx, String key)
            throws NamingException {

        List<String> fnames = new ArrayList<String>();

        // collect tokens from envmt with key
        if (null != envmt) {
            String str = (String) envmt.get(key);
            if (null != str) {
                StringTokenizer st = new StringTokenizer(str, ":"); //$NON-NLS-1$
                while (st.hasMoreTokens()) {
                    fnames.add(st.nextToken());
                }
            }
        }
        // collect tokens from ctx's provider resource file
        if (null != ctx) {
            Hashtable<Object, Object> h = new Hashtable<Object, Object>();
            // read provider resource file from ctx's package
            EnvironmentReader.readProviderResourceFiles(ctx, h);
            String str = (String) h.get(key);
            if (null != str) {
                StringTokenizer st = new StringTokenizer(str, ":"); //$NON-NLS-1$
                while (st.hasMoreTokens()) {
                    fnames.add(st.nextToken());
                }
            }
        }
        // if key is Context.URL_PKG_PREFIXES, append "com.sun.jndi.url" at the
        // end
        if (Context.URL_PKG_PREFIXES.equals(key)) {
            fnames.add("com.sun.jndi.url"); //$NON-NLS-1$
        }
        // return factory names
        return fnames.toArray(new String[fnames.size()]);
    }

    /*
     * Wrapper interface for JNDI properties source.
     */
    private interface JNDIPropertiesSource {
        // Get a JNDI property with the specified name
        String getProperty(final String propName);
    }

    /*
     * Wrapper class for system properties source.
     */
    private static class SystemPropertiesSource implements JNDIPropertiesSource {

        public SystemPropertiesSource() {
            super();
        }

        public String getProperty(final String propName) {
            return System.getProperty(propName);
        }
    }

    /*
     * Wrapper class for applet parameters source.
     */
    private static class AppletParametersSource implements JNDIPropertiesSource {

        private Applet applet;

        public AppletParametersSource(Applet applet) {
            this.applet = applet;
        }

        public String getProperty(final String propName) {
            return applet.getParameter(propName);
        }
    }

}
