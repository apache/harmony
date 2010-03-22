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
package org.apache.harmony.rmi;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.server.LoaderHandler;
import java.rmi.server.RMIClassLoaderSpi;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.harmony.rmi.common.GetStringPropAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Default implementation of RMIClassLoaderSpi.
 *
 * @author  Mikhail A. Markov
 */
public class DefaultRMIClassLoaderSpi extends RMIClassLoaderSpi
        implements LoaderHandler {

    // value of codebase property
    private static String userCodeBase;

    // table holding list of URLLoader-s
    private static final Map<TableKey, WeakReference<URLLoader>> urlLoaders = new HashMap<TableKey, WeakReference<URLLoader>>();

    static {
        String codebaseVal = (String) AccessController.doPrivileged(
                new GetStringPropAction(RMIProperties.CODEBASE_PROP));
        userCodeBase = (codebaseVal == null || codebaseVal.trim().length() == 0)
                ? null : codebaseVal.trim();
    }

    //  Logger where to write loader messages.
    private static final RMILog loaderLog = RMILog.getLoaderLog();

    /**
     * Constructs DefaultRMIClassLoaderSpi.
     */
    public DefaultRMIClassLoaderSpi() {
    }

    /**
     * @see RMIClassLoaderSpi.loadProxyClass(String, String[], ClassLoader)
     */
    public Class loadProxyClass(String codebase,
                                String[] interf,
                                ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException {
        if (loaderLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.25=Loading proxy class: interf=[{0}], codebase="{1}", defaultLoader={2}
            loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.25", //$NON-NLS-1$
                    new Object[]{Arrays.asList(interf),
                        ((codebase == null) ? "" : codebase), //$NON-NLS-1$
                        defaultLoader}));
        }
        Class[] interfCl = new Class[interf.length];
        ClassLoader codebaseLoader = null;
        Exception ex = null;
        stringToURLs(codebase);

        try {
            codebaseLoader = getClassLoader1(codebase);
        } catch (SecurityException se) {
            if (loaderLog.isLoggable(RMILog.BRIEF)) {
                // rmi.log.26=Could not obtain classloader for codebase "{0}" (access denied).
                loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.26", //$NON-NLS-1$
                        ((codebase == null) ? "" : codebase))); //$NON-NLS-1$
            }
            ex = se;
        }
        boolean failed = false;

        // try to resolve all interfaces
        if (defaultLoader != null) {
            for (int i = 0; i < interf.length; ++i) {
                try {
                    interfCl[i] = Class.forName(interf[i], false,
                            defaultLoader);
                } catch (Exception ex1) {
                    if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                        // rmi.log.27=Unable to load interface {0} via default loader {1}:{2}
                        loaderLog.log(RMILog.VERBOSE, Messages.getString(
                                "rmi.log.27", new Object[] { interf[i], //$NON-NLS-1$
                                        defaultLoader, ex1 }));
                    }
                    failed = true;
                }
            }
        }

        if (failed || (defaultLoader == null)) {
            if (ex != null) {
                ClassLoader curLoader =
                    Thread.currentThread().getContextClassLoader();

                if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.28=Trying thread context classloader ({0}).
                    loaderLog.log(RMILog.VERBOSE, Messages.getString(
                            "rmi.log.28", curLoader)); //$NON-NLS-1$
                }
                codebaseLoader = curLoader;

            }

            for (int i = 0; i < interf.length; ++i) {
                try {
                    interfCl[i] = Class.forName(interf[i], false,
                            codebaseLoader);
                } catch (Exception ex1) {
                    if (loaderLog.isLoggable(RMILog.BRIEF)) {
                        // rmi.log.29=Unable to load interface {0} via {1}
                        loaderLog.log(RMILog.BRIEF, Messages.getString(
                                "rmi.log.29", interf[i], codebaseLoader)); //$NON-NLS-1$
                    }

                    if (ex != null) {
                        // rmi.log.2A=Could not load proxy class (access to loader for codebase "{0}" denied).
                        String msg = Messages.getString("rmi.log.2A", //$NON-NLS-1$
                                ((codebase == null) ? "" : codebase)); //$NON-NLS-1$

                        if (loaderLog.isLoggable(RMILog.BRIEF)) {
                            loaderLog.log(RMILog.BRIEF, msg);
                        }
                        throw new ClassNotFoundException(msg, ex);
                    } else {
                        // rmi.25=Unable to load proxy class
                        throw new ClassNotFoundException(Messages.getString("rmi.25"), ex1); //$NON-NLS-1$
                    }
                }
            }
        }

        // successfully loaded all interfaces
        boolean allPublic = true;
        ClassLoader interfLoader = null;
        boolean sameLoader = true;

        // check if all interfaces are public
        for (int i = 0; i < interfCl.length; ++i) {
            if (!Modifier.isPublic(interfCl[i].getModifiers())) {
                allPublic = false;
                ClassLoader loader = interfCl[i].getClassLoader();

                if (interfLoader == null) {
                    interfLoader = loader;
                } else if (!interfLoader.equals(loader)) {
                    if (loaderLog.isLoggable(RMILog.BRIEF)) {
                        // rmi.log.2B=Non-public interface {0} is loaded by another loader ({1}) then others ({2})
                        loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2B", //$NON-NLS-1$
                                new Object[] { interfCl[i], loader, interfLoader }));
                    }
                    sameLoader = false;
                }
            }
        }

        if (allPublic) {
            // all interfaces are public
            Class proxyCl = null;

            try {
                proxyCl = Proxy.getProxyClass(codebaseLoader, interfCl);

                if (loaderLog.isLoggable(RMILog.BRIEF)) {
                    // rmi.log.2C=Loaded proxy class {0} via {1}
                    loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2C", //$NON-NLS-1$
                            proxyCl, codebaseLoader));
                }
            } catch (IllegalArgumentException iae) {
                try {
                    proxyCl = Proxy.getProxyClass(defaultLoader, interfCl);

                    if (loaderLog.isLoggable(RMILog.BRIEF)) {
                        // rmi.log.2C=Loaded proxy class {0} via {1}
                        loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2C", //$NON-NLS-1$
                                proxyCl, defaultLoader));
                    }
                } catch (IllegalArgumentException iae1) {
                    if (loaderLog.isLoggable(RMILog.BRIEF)) {
                        // rmi.log.2D=Unable to load proxy class via both loaders ({0}, {1})
                        loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2D", //$NON-NLS-1$
                                codebaseLoader, defaultLoader));
                    }
                    // rmi.25=Unable to load proxy class
                    throw new ClassNotFoundException(Messages.getString("rmi.25"), iae1); //$NON-NLS-1$
                }
            }
            return proxyCl;
        }

        // there are non-public interfaces
        if (sameLoader) {
            // they are defined in the same ClassLoader
            Class proxyCl = Proxy.getProxyClass(interfLoader, interfCl);

            if (loaderLog.isLoggable(RMILog.BRIEF)) {
                // rmi.log.2C=Loaded proxy class {0} via {1}
                loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2C", //$NON-NLS-1$
                        proxyCl, interfLoader));
            }
            return proxyCl;
        }
        // rmi.25=Unable to load proxy class
        throw new LinkageError(Messages.getString("rmi.25")); //$NON-NLS-1$
    }

    /**
     * @see RMIClassLoaderSpi.loadClass(String, String, ClassLoader)
     */
    public Class loadClass(String codebase,
                           String name,
                           ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException {
        if (loaderLog.isLoggable(RMILog.VERBOSE)) {
            // rmi.log.2E=Loading class: name="{0}", codebase="{1}", defaultLoader={2}
            loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.2E", //$NON-NLS-1$
                    new Object[]{name, ((codebase == null) ? "" : codebase), //$NON-NLS-1$
                    defaultLoader}));
        }
        stringToURLs(codebase);

        try {
            if (defaultLoader != null) {
                Class c = Class.forName(name, false, defaultLoader);

                if (loaderLog.isLoggable(RMILog.BRIEF)) {
                    // rmi.log.2F=Loaded class: {0} via default loader: {1}
                    loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2F", //$NON-NLS-1$
                            name, defaultLoader));
                }
                return c;
            }
        } catch (ClassNotFoundException cnfe) {
            // we ignore this exception and proceed
        }
        ClassLoader codebaseLoader = null;
        Exception ex = null;

        try {
            codebaseLoader = getClassLoader1(codebase);
        } catch (SecurityException se) {
            if (loaderLog.isLoggable(RMILog.BRIEF)) {
                loaderLog.log(RMILog.BRIEF,
                        Messages.getString("rmi.log.30", //$NON-NLS-1$
                        ((codebase == null) ? "" : codebase))); //$NON-NLS-1$
            }
            ex = se;
        }
        Class c;

        if (ex != null) {
            ClassLoader curLoader =
                    Thread.currentThread().getContextClassLoader();

            if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.31=Trying thread context classloader ({0}).
                loaderLog.log(RMILog.VERBOSE,
                        Messages.getString("rmi.log.31", curLoader)); //$NON-NLS-1$
            }

            try {
                c = Class.forName(name, false, curLoader);
            } catch (ClassNotFoundException cnfe1) {
                if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                    // rmi.log.32=Could not load class {0} via thread context classloader (access to codebase loader is denied).
                    loaderLog.log(RMILog.VERBOSE,
                            Messages.getString("rmi.log.32", name)); //$NON-NLS-1$
                }
                // rmi.94=Could not load class {0}(access to loader for codebase "{1}" denied).
                throw new ClassNotFoundException(Messages.getString("rmi.94", //$NON-NLS-1$
                        name, ((codebase == null) ? "" : codebase)), ex);//$NON-NLS-1$
                        
            }

            if (loaderLog.isLoggable(RMILog.BRIEF)) {
                // rmi.log.34=Loaded class: {0} via thread context classloader.
                loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.34", name)); //$NON-NLS-1$
            }
        } else {
            c = Class.forName(name, false, codebaseLoader);

            if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                // rmi.log.35=Loaded class: {0} via {1}
                loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.35", //$NON-NLS-1$
                        name, codebaseLoader));
            }
        }
        return c;
    }

    /**
     * @see RMIClassLoaderSpi.getClassAnnotation(Class)
     */
    public String getClassAnnotation(Class cl) {
       ClassLoader loader = cl.getClassLoader();
       ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

       if (loader == systemLoader || (systemLoader != null
               && loader == systemLoader.getParent())) {
           return userCodeBase;
       }

       if (loader instanceof URLLoader) {
           return ((URLLoader) loader).getAnnotations();
       } else if (loader instanceof URLClassLoader) {
           URL[] urls = ((URLClassLoader) loader).getURLs();
           String annot = urlsToCodebase(urls);

           if (annot == null) {
               return userCodeBase;
           }
           SecurityManager mgr = System.getSecurityManager();

           if (mgr != null) {
               try {
                   for (int i = 0; i < urls.length; ++i) {
                       Permission p = urls[i].openConnection().getPermission();

                       if (p != null) {
                           mgr.checkPermission(p);
                       }
                   }
               } catch (SecurityException se) {
                   return userCodeBase;
               } catch (IOException ioe) {
                   return userCodeBase;
               }
           }
           return annot;
       } else {
           return userCodeBase;
       }
    }

    /**
     * @see RMIClassLoaderSpi.getClassLoader(String)
     */
    public ClassLoader getClassLoader(String codebase)
            throws MalformedURLException {
        stringToURLs(codebase);
        SecurityManager mgr = System.getSecurityManager();

        if (mgr == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        mgr.checkPermission(new RuntimePermission("getClassLoader")); //$NON-NLS-1$
        return getClassLoader1(codebase);
    }

    /**
     * @see LoaderHandler.loadClass(String)
     */
    public Class loadClass(String name)
            throws MalformedURLException, ClassNotFoundException {
        return loadClass(null, name, null);
    }

    /**
     * @see LoaderHandler.loadClass(URL, String)
     */
    public Class loadClass(URL codebase, String name)
            throws MalformedURLException, ClassNotFoundException {
        return loadClass(codebase.toExternalForm(), name, null);
    }

    /**
     * Always returns null.
     * This method came from LoaderHandler class and not used.
     *
     * @see LoaderHandler.getSecurityContext(ClassLoader)
     */
    public Object getSecurityContext(ClassLoader loader) {
        return null;
    }

    /*
     * Finds loader in classloaders table. Returns it as a result if it's not
     * null, otherwise creates URLLoader, adds it to the table and returns it
     * as a result. Checks permission on found/created loader.
     *
     * @param codebase list of URLs separated by spaces
     *
     * @return ClassLoader found/created
     *
     * @throws MalformedURLException if the method was unable to parse one of
     *         provided URLs
     */
    private static ClassLoader getClassLoader1(String codebase)
            throws MalformedURLException {
        SecurityManager mgr = System.getSecurityManager();
        ClassLoader parentLoader =
                Thread.currentThread().getContextClassLoader();

        if (mgr == null) {
            return parentLoader;
        }

        if (codebase == null) {
            if (userCodeBase != null) {
                codebase = userCodeBase;
            } else {
                return parentLoader;
            }
        }
        URLLoader loader = getClassLoaderNoCheck(parentLoader, codebase);

        if (loader != null) {
            loader.checkPermissions();
        }
        return loader;
    }
    
    /*
     * Finds URLLoader in classloaders table. Returns it as a result if it's not
     * null, otherwise creates URLLoader, adds it to the table and returns it
     * as a result. Does not check permission on found/created loader.
     *
     * @param parentLoader parent classloader
     * @param codebase list of URLs separated by spaces
     *
     * @return URLLoader found/created
     *
     * @throws MalformedURLException if the method was unable to parse one of
     *         provided URLs
     */
    private static URLLoader getClassLoaderNoCheck(
            ClassLoader parentLoader,
            String codebase) throws MalformedURLException {
        TableKey key = new TableKey(parentLoader, codebase);
        URLLoader loader = null;

        synchronized (urlLoaders) {
            if (urlLoaders.containsKey(key)) {
                loader = urlLoaders.get(key).get();
    
                if (loader == null) {
                    urlLoaders.remove(key);
                } else {
                    return loader;
                }
            }
            AccessControlContext ctx = createLoaderACC(key.getURLs());
    
            // PrivilegedAction for URLLoader creation
            class CreateLoaderAction implements PrivilegedAction<URLLoader> {
                URL[] urls;
                ClassLoader parentLoader;
    
                public CreateLoaderAction(URL[] urls, ClassLoader parentLoader) {
                    this.urls = urls;
                    this.parentLoader = parentLoader;
                }
    
                public URLLoader run() {
                    return new URLLoader(urls, parentLoader);
                }
            }
            loader = AccessController.doPrivileged(
                    new CreateLoaderAction(key.getURLs(), parentLoader), ctx);
            urlLoaders.put(key, new WeakReference<URLLoader>(loader));
            return loader;
        }
    }

    /*
     * Creates and returns AccessControlContext required to create
     * URLClassLoader (i.e. context containing permissions sufficient
     * to create URLClassLoader).
     *
     * @param urls list of URLs permissions for which should be granted in
     *        created ACC
     *
     * @return AccessControlContext required to create URLClassLoader passing
     *         specified URLs to it's constructor
     */
    private static AccessControlContext createLoaderACC(URL[] urls) {
        // get existing permissions from the installed policy
        PermissionCollection perms = (PermissionCollection)
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        CodeSource cs =
                                new CodeSource(null, (Certificate []) null);
                        Policy policy = Policy.getPolicy();

                        if (policy != null) {
                            return policy.getPermissions(cs);
                        }
                        return new Permissions();
                    }
                });

        // add permissions for URLs
        addURLsPerms(urls, perms, true);

        // grant permission for ClassLoader creation
        perms.add(new RuntimePermission("createClassLoader")); //$NON-NLS-1$

        // create AccessControlContext from created Permissions
        ProtectionDomain[] domains;

        if (urls.length == 0) {
            domains = new ProtectionDomain[] { new ProtectionDomain(
                    new CodeSource(null, (Certificate []) null), perms) };
        } else {
            domains = new ProtectionDomain[urls.length];

            for (int i = 0; i < urls.length; ++i) {
                domains[i] = new ProtectionDomain(new CodeSource(
                        urls[i], (Certificate []) null), perms);
            }
        }
        return new AccessControlContext(domains);
    }

    /*
     * Adds Permissions required to access provided list of URLs to specified
     * PermissionCollection and returns this collection as a result.
     *
     * @param urls list of URLs for which Permissions should be added
     * @param perms PermissionCollection where permissions should be added
     * @param forACC if true then this method was called for creating loader's
     *        AccessControlContext and additional SocketPermission should be
     *        added in case if url is not "file:///"
     *
     * @return updated Permissioncollection as a result
     */
    private static PermissionCollection addURLsPerms(
            URL[] urls, PermissionCollection perms, boolean forACC) {
        for (int i = 0; i < urls.length; ++i) {
            Permission perm = null;

            try {
                perm = urls[i].openConnection().getPermission();
            } catch (IOException ioe) {
                continue;
            }

            if (perm == null) {
                continue;
            }

            if (perm instanceof FilePermission) {
                /*
                 * For proper handling of codebases like: file:///c:/tmp/,
                 * where tmp is a directory, we should add '-' symbol to allow
                 * the classloader to read this directory and all subdirectories
                 * if the classes with packages are in this directory unpacked
                 * (not in .jar file).
                 */
                String str = perm.getName();
                int idx = str.lastIndexOf(File.separatorChar);

                if (!str.endsWith(File.separator)) {
                    perms.add(perm);
                } else {
                    perms.add(new FilePermission(str + "-", "read")); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } else {
                perms.add(perm);

                if (forACC) {
                    /*
                     * This method was called in 'createLoaderACC()' method
                     * for loader's AccessControlContext creation and additional
                     * SocketPermission allowing connecting to url's host and
                     * accepting connections from it should be added.
                     */
                    String host = urls[i].getHost();

                    if (host != null) {
                        perms.add(new SocketPermission(host,
                                "connect, accept")); //$NON-NLS-1$
                    }
                }
            }
        }
        return perms;
    }

    /*
     * Converts string representation of urls to array of URLs.
     *
     * @param list list of urls separated by spaces
     *
     * @return array of URLs from specified list
     */
    private static URL[] stringToURLs(String list)
            throws MalformedURLException {
        if (list == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(list);
        URL[] urls = new URL[tok.countTokens()];

        for (int i = 0; i < urls.length; ++i) {
            urls[i] = new URL(tok.nextToken());
        }
        return urls;
    }

    /*
     * Converts list of URLs to a string, where URLs are separated by spaces.
     *
     * @param list of URLs to be converted
     *
     * @return string containing specified URLs separated by spaces
     */
    private static String urlsToCodebase(URL[] urls) {
        if (urls == null || urls.length == 0) {
            return null;
        }
        String str = ""; //$NON-NLS-1$

        for (int i = 0; i < urls.length - 1; ++i) {
            str += urls[i].toExternalForm() + " "; //$NON-NLS-1$
        }
        return str + urls[urls.length - 1].toExternalForm();
    }


    /*
     * This class is a subclass of URLClassLoader. It contains annotations for
     * classes and also permissions to be checked if requested.
     */
    private static class URLLoader extends URLClassLoader {
        // annotations for classes
        private String annot;

        // permissions to be checked if requested
        private Permissions perms;

        /*
         * Constructs URLLoader from list of URLs and parent ClassLoader.
         *
         * @param urls list of URLs
         * @param parent parent ClassLoader
         */
        URLLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
            perms = new Permissions();
            addURLsPerms(urls, perms, false);
            annot = urlsToCodebase(urls);
        }

        /*
         * Returns annotations for classes.
         *
         * @return annotations for classes
         */
        String getAnnotations() {
            return annot;
        }

        /*
         * Checks permissions contained in this loader if SecurityManager
         * installed is not null. The method will throw SecurityException
         * (as SecurityManager does) if one of the permissions is not granted.
         */
        void checkPermissions() {
            SecurityManager mgr = System.getSecurityManager();

            if (mgr != null) {
                for (Enumeration en = perms.elements(); en.hasMoreElements();
                        mgr.checkPermission((Permission) en.nextElement())) {
                }
            }
        }

        /**
         * Returns string representation of this loader.
         *
         * @return string representation of this loader
         */
        public String toString() {
            return getClass().getName() + "[annot:\"" + annot + "\"]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }


    /*
     * Class representing key for storing classloaders in a Map. It consists
     * of a pair: loader/URL[].
     */
    private static class TableKey {
        private ClassLoader loader;
        private URL[] urls;
        private int hashCode;

        /*
         * Constructs TableKey from string representation of the list of URLs.
         *
         * @param loader ClassLoader
         * @param codebase String represented codebase list
         *        separated by <space>
         */
        TableKey(ClassLoader loader, String codebase)
                throws MalformedURLException {
            this(loader, stringToURLs(codebase));
        }

        /*
         * Constructs TableKey from the specified array of URLs.
         *
         * @param loader ClassLoader
         * @param urls array of URLs
         */
         TableKey(ClassLoader loader, URL[] urls) {
             this.loader = loader;
             this.urls = urls;

             // calculate hashCode
             hashCode = (loader == null) ? 0 : loader.hashCode();

             for (int i = 0; i < urls.length; ++i) {
                 hashCode ^= urls[i].hashCode();
             }
         }

        /*
         * Returns ClassLoader contained in this TableKey.
         *
         * @return ClassLoader contained in this TableKey
         */
        ClassLoader getLoader() {
            return loader;
        }

        /*
         * Returns list of URLs contained in this TableKey.
         *
         * @return list of URLs contained in this TableKey
         */
        public URL[] getURLs() {
            return urls;
        }

        /**
         * Compares this object with another one. Returns true if the object for
         * comparison is an instance of TableKey and they contained the same
         * loader and urls fields.
         *
         * @param obj object for comparison
         *
         * @return true if object specified is equal to this TableKey and false
         *         otherwise
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof TableKey)) {
                return false;
            }
            TableKey key = (TableKey) obj;

            if (hashCode() != key.hashCode()) {
                return false;
            }
            return ((loader != null) ? loader.equals(key.loader)
                   : (key.loader == null)) && ((urls != null)
                   ? Arrays.equals(urls, key.urls) : (key.urls == null));
        }

        /**
         * Returns hash code for this TableKey.
         *
         * @return hash code for this TableKey
         */
        public int hashCode() {
            return hashCode;
        }
    }
}
