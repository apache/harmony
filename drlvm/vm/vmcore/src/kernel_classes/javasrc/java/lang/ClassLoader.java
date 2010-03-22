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
package java.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.NoSuchElementException;

import org.apache.harmony.lang.RuntimePermissionCollection;
import org.apache.harmony.misc.EmptyEnum;
import org.apache.harmony.lang.ClassLoaderInfo;
import org.apache.harmony.vm.VMStack;

/**
 * Base class for all class loaders
 * 
 * @author Evgueni Brevnov
 */
public abstract class ClassLoader {

    /**
     * default protection domain.
     */
    private ProtectionDomain defaultDomain;

    /**
     * system default class loader. It is initialized while
     * getSystemClassLoader(..) method is executing.
     */
    private static ClassLoader systemClassLoader = null;

    /**
     * empty set of certificates
     */
    private static final Certificate[] EMPTY_CERTIFICATES = new Certificate[0];

    /**
     * this field has false as default value, it becomes true if system class
     * loader is initialized.
     */
    private static boolean initialized = false;

    /**
     * package private to access from the java.lang.Class class. The following
     * mapping is used <String name, Boolean flag>, where name - class name,
     * flag - true if assertion is enabled, false if disabled.
     */
    Hashtable<String, Boolean> classAssertionStatus;

    /**
     * package private to access from the java.lang.Class class. The following
     * mapping is used <String name, Object[] signers>, where name - class name,
     * signers - array of signers.
     */
    Hashtable<String, Object[]> classSigners;

    /**
     * package private to access from the java.lang.Class class.
     */
    int defaultAssertionStatus;
    boolean clearAssertionStatus;

    /**
     * package private to access from the java.lang.Class class. The following
     * mapping is used <String name, Boolean flag>, where name - package name,
     * flag - true if assertion is enabled, false if disabled.
     */
    Hashtable<String, Boolean> packageAssertionStatus;

    /**
     * packages defined by this class loader are stored in this hash. The
     * following mapping is used <String name, Package pkg>, where name -
     * package name, pkg - corresponding package.
     */
    private final HashMap<String, Package> definedPackages;

    /**
     * The class registry, provides strong referencing between the classloader 
     * and it's defined classes. Intended for class unloading implementation.
     * @see java.lang.Class#definingLoader
     * @see #registerLoadedClass()
     */
    private ArrayList<Class<?>> loadedClasses = new ArrayList<Class<?>>(); 

    /**
     * package private to access from the java.lang.Class class. The following
     * mapping is used <String name, Certificate[] certificates>, where name -
     * the name of a package, certificates - array of certificates.
     */
    private final Hashtable<String, Certificate[]> packageCertificates = 
        new Hashtable<String, Certificate[]>();

    /**
     * parent class loader
     */
    private final ClassLoader parentClassLoader;

    protected ClassLoader() {
        this(getSystemClassLoader());
    }

    protected ClassLoader(ClassLoader parent) {
        SecurityManager sc = System.getSecurityManager();
        if (sc != null) {
            sc.checkCreateClassLoader();
        }
        parentClassLoader = parent;
        // this field is used to determine whether class loader was initialized
        // properly.
        definedPackages = new HashMap<String, Package>();
    }

    public static ClassLoader getSystemClassLoader() {
        if (!initialized) {
            // we assume only one thread will initialize system class loader. So
            // we don't synchronize initSystemClassLoader() method.
            initSystemClassLoader();
            // system class loader is initialized properly.
            initialized = true;
            // setContextClassLoader(...) method throws SecurityException if
            // current thread isn't allowed to set systemClassLoader as a
            // context class loader. Actually, it is abnormal situation if
            // thread can not change his own context class loader.
            // Thread.currentThread().setContextClassLoader(systemClassLoader);
        }
        //assert initialized;
        SecurityManager sc = System.getSecurityManager();
        if (sc != null) {
            // we use VMClassRegistry.getClassLoader(...) method instead of
            // Class.getClassLoader() due to avoid redundant security
            // checking
            ClassLoader callerLoader = VMClassRegistry.getClassLoader(VMStack
                .getCallerClass(0));
            if (callerLoader != null && callerLoader != systemClassLoader) {
                sc.checkPermission(RuntimePermissionCollection.GET_CLASS_LOADER_PERMISSION);
            }
        }
        return systemClassLoader;
    }

    public static URL getSystemResource(String name) {
        return getSystemClassLoader().getResource(name);
    }

    public static InputStream getSystemResourceAsStream(String name) {
        return getSystemClassLoader().getResourceAsStream(name);
    }

    public static Enumeration<URL> getSystemResources(String name)
        throws IOException {
       return getSystemClassLoader().getResources(name);
    }

    public void clearAssertionStatus() {
        clearAssertionStatus = true;
        defaultAssertionStatus = -1;
        packageAssertionStatus = null;
        classAssertionStatus = null;
    }

    public final ClassLoader getParent() {
        SecurityManager sc = System.getSecurityManager();
        if (sc != null) {
            ClassLoader callerLoader = VMClassRegistry.getClassLoader(VMStack
                .getCallerClass(0));
            if (callerLoader != null && !callerLoader.isSameOrAncestor(this)) {
                sc.checkPermission(RuntimePermissionCollection.GET_CLASS_LOADER_PERMISSION);
            }
        }
        return parentClassLoader;
    }

    public URL getResource(String name) {
        String nm = name.toString();
        checkInitialized();
        URL foundResource = (parentClassLoader == null)
            ? BootstrapLoader.findResource(nm)
            : parentClassLoader.getResource(nm);
        return foundResource == null ? findResource(nm) : foundResource;
    }

    public InputStream getResourceAsStream(String name) {
        URL foundResource = getResource(name);
        if (foundResource != null) {
            try {
                return foundResource.openStream();
            } catch (IOException e) {
            }
        }
        return null;
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        checkInitialized();
        ClassLoader cl = this;
        final ArrayList<Enumeration<URL>> foundResources = 
            new ArrayList<Enumeration<URL>>();
        Enumeration<URL> resourcesEnum;
        do {
            resourcesEnum = cl.findResources(name);
            if (resourcesEnum != null && resourcesEnum.hasMoreElements()) {
                foundResources.add(resourcesEnum);
            }            
        } while ((cl = cl.parentClassLoader) != null);
        resourcesEnum = BootstrapLoader.findResources(name);
        if (resourcesEnum != null && resourcesEnum.hasMoreElements()) {
            foundResources.add(resourcesEnum);
        }
        return new Enumeration<URL>() {

                private int position = foundResources.size() - 1;

                public boolean hasMoreElements() {
                    while (position >= 0) {
                        if (foundResources.get(position).hasMoreElements()) {
                            return true;
                        }
                        position--;
                    }
                    return false;
                }

                public URL nextElement() {
                    while (position >= 0) {
                        try {
                            return (foundResources.get(position)).nextElement();
                        } catch (NoSuchElementException e) {}
                        position--;
                    }
                    throw new NoSuchElementException();
                }
            };
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    public void setClassAssertionStatus(String name, boolean flag) {
        if (name != null) {
            Class.disableAssertions = false;
            synchronized (definedPackages) {
                if (classAssertionStatus == null) {
                    classAssertionStatus = new Hashtable<String, Boolean>();
                }
            }
            classAssertionStatus.put(name, Boolean.valueOf(flag));
        }
    }

    public void setDefaultAssertionStatus(boolean flag) {
        if (flag) {
            Class.disableAssertions = false;
        }
        defaultAssertionStatus = flag ? 1 : -1;
    }

    /**
     * Empty string is used to denote default package.
     */
    public void setPackageAssertionStatus(String name, boolean flag) {
        if (name == null) {
            name = "";
        }
        Class.disableAssertions = false;
        synchronized (definedPackages) {
            if (packageAssertionStatus == null) {
                packageAssertionStatus = new Hashtable<String, Boolean>();
            }
        }
        packageAssertionStatus.put(name, Boolean.valueOf(flag));
    }

    @Deprecated
    protected final Class<?> defineClass(byte[] data, int offset, int len)
        throws ClassFormatError {
        return defineClass(null, data, offset, len);
    }

    protected final Class<?> defineClass(String name, byte[] data, int offset, int len)
        throws ClassFormatError {
        return defineClass(name, data, offset, len, null);
    }

    /**
     * Registers the defined class, invoked by VM.
     * Intended for class unloading implementation.
     */
    @SuppressWarnings("unused") 
    private void registerLoadedClass(Class<?> clazz) {
        synchronized (loadedClasses){
            loadedClasses.add(clazz); 
    	}
        clazz.definingLoader = this;
    }

    protected final Class<?> defineClass(String name, ByteBuffer b, ProtectionDomain protectionDomain)
        throws ClassFormatError {
		byte[] data = b.array();
        return defineClass(name, data, 0, data.length, protectionDomain);
    }

    protected final synchronized Class<?> defineClass(String name, byte[] data,
                                             int offset, int len,
                                             ProtectionDomain domain)
        throws ClassFormatError {
        checkInitialized();
        if (name != null && name.indexOf('/') != -1) {
            throw new NoClassDefFoundError(
                    "The name is expected in binary (canonical) form,"
                    + " therefore '/' symbols are not allowed: " + name);
        }
        if (offset < 0 || len < 0 || offset + len > data.length) {
            throw new IndexOutOfBoundsException(
                "Either offset or len is outside of the data array");
        }
        if (domain == null) {
            if (defaultDomain == null) {
                defaultDomain = new ProtectionDomain(
                        new CodeSource(null, (Certificate[])null), null, this, null);            
            }        
            domain = defaultDomain;
        }
        Certificate[] certs = null;
        String packageName = null;
        if (name != null) {
            if (name.startsWith("java.")) {
                throw new SecurityException(
                    "It is not allowed to define classes inside the java.* package: " + name);
            }
            int lastDot = name.lastIndexOf('.');
            packageName = lastDot == -1 ? "" : name.substring(0, lastDot);
            certs = getCertificates(packageName, domain.getCodeSource());
        }
        Class<?> clazz = defineClass0(name, data, offset, len);
        clazz.setProtectionDomain(domain);
        if (certs != null) {
            packageCertificates.put(packageName, certs);
        }
        return clazz;
    }
    
    /**
     * Loads new type into the classloader name space. 
     * The class loader is marked as defining class loader. 
     * @api2vm
     */
    private native Class<?> defineClass0(String name, byte[] data, int off, int len) 
    throws ClassFormatError;


    protected Package definePackage(String name, String specTitle,
                                    String specVersion, String specVendor,
                                    String implTitle, String implVersion,
                                    String implVendor, URL sealBase)
        throws IllegalArgumentException {
        synchronized (definedPackages) {
            if (getPackage(name) != null) {
                throw new IllegalArgumentException("Package " + name
                    + "has been already defined.");
            }
            Package pkg = new Package(this, name, specTitle, specVersion, specVendor,
                implTitle, implVersion, implVendor, sealBase);
            definedPackages.put(name, pkg);
            return pkg;
        }
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException("Can not find class " + name);
    }

    protected String findLibrary(String name) {
        return null;
    }

    protected final native Class<?> findLoadedClass(String name);

    protected URL findResource(String name) {
        return null;
    }

    protected Enumeration<URL> findResources(String name) throws IOException {
        return EmptyEnum.getInstance();
    }

    protected final Class<?> findSystemClass(String name)
        throws ClassNotFoundException {
        return getSystemClassLoader().loadClass(name, false);
    }

    protected Package getPackage(String name) {
        checkInitialized();
        Package pkg = null;
        if (name == null) {
            throw new NullPointerException();
        }
        synchronized (definedPackages) {
            pkg = definedPackages.get(name);
        }
        if (pkg == null) {
            if (parentClassLoader == null) {
                pkg = BootstrapLoader.getPackage(name);
            } else {
                pkg = parentClassLoader.getPackage(name);
            }
        }
        return pkg;
    }

    protected Package[] getPackages() {
        checkInitialized();
        ArrayList<Package> packages = new ArrayList<Package>();
        fillPackages(packages);
        return packages.toArray(new Package[packages.size()]);
    }

    /**
     * Registers this class loader as initiating for a class
     * Declared as package private to use it from java.lang.Class.forName
     */
    native void registerInitiatedClass(Class<?> clazz);

    protected synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
        checkInitialized();
        if (name == null) {
            throw new NullPointerException();
        }
        if(name.indexOf("/") != -1) {
            throw new ClassNotFoundException(name);
        }

        Class<?> clazz = findLoadedClass(name);
        if (clazz == null) {
            if (parentClassLoader == null) {
                clazz = VMClassRegistry.loadBootstrapClass(name);
            } else {
                try {
                    clazz = parentClassLoader.loadClass(name);
                    if(clazz != null) {
                        try {
                            VMStack.getCallerClass(0)
                                    .asSubclass(ClassLoader.class);
                        } catch(ClassCastException ex) {
                            // caller class is not a subclass of
                            // java/lang/ClassLoader so register as
                            // initiating loader as we are called from
                            // outside of ClassLoader delegation chain
                            registerInitiatedClass(clazz);
                        }
                    }
                } catch (ClassNotFoundException e) {
                }
            }
            if (clazz == null) {
                clazz = findClass(name);
                if (clazz == null) {
                    throw new ClassNotFoundException(name);
                }
            }
        }
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    protected final void resolveClass(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException();
        }
        VMClassRegistry.linkClass(clazz);
    }

    protected final void setSigners(Class<?> clazz, Object[] signers) {
        checkInitialized();
        String name = clazz.getName();
        ClassLoader classLoader = clazz.getClassLoaderImpl();
        if (classLoader != null) {
            if (classLoader.classSigners == null) {
                classLoader.classSigners = new Hashtable<String, Object[]>();
            }
            classLoader.classSigners.put(name, signers);
        }
    }

    /*
     * NON API SECTION
     */

    final boolean isSameOrAncestor(ClassLoader loader) {
        while (loader != null) {
            if (this == loader) {
                return true;
            }
            loader = loader.parentClassLoader;
        }
        return false;
    }

    /**
     * This method should be called from each method that performs unsafe
     * actions.
     */
    private void checkInitialized() {
        if (definedPackages == null) {
            throw new SecurityException(
                "Class loader was not initialized properly.");
        }
    }

    /**
     * Neither certs1 nor certs2 cann't be equal to null.
     */
    private boolean compareAsSet(Certificate[] certs1, Certificate[] certs2) {
        // TODO Is it possible to have multiple instances of same
        // certificate in array? This implementation assumes that it is
        // not possible.
        if (certs1.length != certs1.length) {
            return false;
        }
        if (certs1.length == 0) {
            return true;
        }
        boolean[] hasEqual = new boolean[certs1.length];
        for (int i = 0; i < certs1.length; i++) {
            boolean isMatch = false;
            for (int j = 0; j < certs2.length; j++) {
                if (!hasEqual[j] && certs1[i].equals(certs2[j])) {
                    hasEqual[j] = isMatch = true;
                    break;
                }
            }
            if (!isMatch) {
                return false;
            }
        }
        return true;
    }

    /**
     * Initializes the system class loader.
     */
    @SuppressWarnings("unchecked")
    private static void initSystemClassLoader() {
        if (systemClassLoader != null) {
            throw new IllegalStateException(
                "Recursive invocation while initializing system class loader");
        }
        systemClassLoader = SystemClassLoader.getInstance();
        
        String smName = System.getPropertyUnsecure("java.security.manager");
        if (smName != null) {
            try {
                final Class<SecurityManager> smClass;
                if ("".equals(smName) || "default".equalsIgnoreCase(smName)) {
                    smClass = java.lang.SecurityManager.class;
                } else {
                    smClass = (Class<SecurityManager>)systemClassLoader.loadClass(smName);
                    if (!SecurityManager.class.isAssignableFrom(smClass)) {
                        throw new Error(smClass
                            + " must inherit java.lang.SecurityManager");
                    }
                }   
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run() throws Exception {
                        System.setSecurityManager(smClass.newInstance());
                        return null;
                    }
                });
            } catch (Exception e) {
                throw (Error)new InternalError().initCause(e);
            }
        }

        String className = System.getPropertyUnsecure("java.system.class.loader");
        if (className != null) {
            try {
                final Class<?> userClassLoader = systemClassLoader
                    .loadClass(className);
                if (!ClassLoader.class.isAssignableFrom(userClassLoader)) {
                    throw new Error(userClassLoader.toString()
                        + " must inherit java.lang.ClassLoader");
                }
                systemClassLoader = AccessController
                    .doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
                        public ClassLoader run() throws Exception {
                            Constructor c = userClassLoader
                                .getConstructor(ClassLoader.class);
                            return (ClassLoader)c.newInstance(systemClassLoader);
                        }
                    });
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            } catch (PrivilegedActionException e) {
                throw new Error(e.getCause());
            }
        }
    }

    /**
     * Helper method for the getPackages() method.
     */
    private void fillPackages(ArrayList<Package> packages) {
        if (parentClassLoader == null) {
            packages.addAll(BootstrapLoader.getPackages());
        } else {
            parentClassLoader.fillPackages(packages);
        }
        synchronized (definedPackages) {
            packages.addAll(definedPackages.values());
        }
    }

    /**
     * Helper method for defineClass(...)
     * 
     * @return null if the package already has the same set of certificates, if
     *         first class in the package is being defined then array of
     *         certificates extracted from codeSource is returned.
     * @throws SecurityException if the package has different set of
     *         certificates than codeSource
     */
    private Certificate[] getCertificates(String packageName,
                                          CodeSource codeSource) {
        Certificate[] definedCerts = packageCertificates
            .get(packageName);
        Certificate[] classCerts = codeSource != null
            ? codeSource.getCertificates() : EMPTY_CERTIFICATES;
        classCerts = classCerts != null ? classCerts : EMPTY_CERTIFICATES;
        // not first class in the package
        if (definedCerts != null) {
            if (!compareAsSet(definedCerts, classCerts)) {
                throw new SecurityException("It is prohobited to define a "
                    + "class which has different set of signers than "
                    + "other classes in this package");
            }
            return null;
        }
        return classCerts;
    }

    /**
     * Helper method to avoid StringTokenizer using.
     */
    private static String[] fracture(String str, String sep) {
        if (str.length() == 0) {
            return new String[0];
        }
        ArrayList<String> res = new ArrayList<String>();
        int in = 0;
        int curPos = 0;
        int i = str.indexOf(sep);
        int len = sep.length();
        while (i != -1) {
            String s = str.substring(curPos, i); 
            res.add(s);
            in++;
            curPos = i + len;
            i = str.indexOf(sep, curPos);
        }

        len = str.length();
        if (curPos <= len) {
            String s = str.substring(curPos, len); 
            in++;
            res.add(s);
        }

        return res.toArray(new String[in]);
    }
   
    /* IBM SPECIFIC PART */
    
    static final ClassLoader getStackClassLoader(int depth) {
        Class<?> clazz = VMStack.getCallerClass(depth);
        return clazz != null ? clazz.getClassLoaderImpl() : null;
    }
    
    final boolean  isSystemClassLoader () {
        return ClassLoaderInfo.isSystemClassLoader(this); 
    }

    static final void loadLibraryWithClassLoader (String libName, ClassLoader loader) {
        SecurityManager sc = System.getSecurityManager();
        if (sc != null) {
                sc.checkLink(libName);
        }
        if (loader != null) {
                String fullLibName = loader.findLibrary(libName);
                if (fullLibName != null) {
                        loadLibrary(fullLibName, loader, null);
                        return;
                }
        }       
                String path = System.getProperty("java.library.path", "");
                path += System.getProperty("vm.boot.library.path", "");
        loadLibrary(libName, loader, path);
    }
   
    static final void loadLibrary (String libName, ClassLoader loader, String libraryPath) {
        SecurityManager sc = System.getSecurityManager();
        if (sc != null) {
                sc.checkLink(libName);
        }
        String pathSeparator = System.getProperty("path.separator");
        String fileSeparator = System.getProperty("file.separator");
        String st[] = fracture(libraryPath, pathSeparator);
        int l = st.length;
        for (int i = 0; i < l; i++) {
            try {
                VMClassRegistry.loadLibrary(st[i] + fileSeparator + libName, loader);
                return;
            } catch (UnsatisfiedLinkError e) {
            }
        }
        throw new UnsatisfiedLinkError(libName);
    } 
   
    /* END OF IBM SPECIFIC PART */

    static final class BootstrapLoader {

        // TODO avoid security checking
        private static final String bootstrapPath = System
            .getProperty("vm.boot.class.path", "");

        private static URLClassLoader resourceFinder = null;

        private static final HashMap<String, Package> systemPackages = 
            new HashMap<String, Package>();

        /**
         * This class contains static methods only. So it should not be
         * instantiated.
         */
        private BootstrapLoader() {
        }

        public static URL findResource(String name) {
            if (resourceFinder == null) {
                initResourceFinder();
            }
            return resourceFinder.findResource(name);
        }

        public static Enumeration<URL> findResources(String name) throws IOException {
            if (resourceFinder == null) {
                initResourceFinder();
            }
            return resourceFinder.findResources(name);
        }

        public static Package getPackage(String name) {
            synchronized (systemPackages) {
                updatePackages();
                return systemPackages.get(name.toString());
            }
        }

        public static Collection<Package> getPackages() {
            synchronized (systemPackages) {
                updatePackages();
                return systemPackages.values();
            }
        }

        private static void initResourceFinder() {
            synchronized (bootstrapPath) {
                if (resourceFinder != null) {
                    return;
                }                
                // -Xbootclasspath:"" should be interpreted as nothing defined,
                // like we do below:
                String st[] = fracture(bootstrapPath, File.pathSeparator);
                int l = st.length;
                ArrayList<URL> urlList = new ArrayList<URL>();
                for (int i = 0; i < l; i++) {
                    try {
                        urlList.add(new File(st[i]).toURI().toURL());
                    } catch (MalformedURLException e) {
                    }
                }
                URL[] urls = new URL[urlList.size()];
                resourceFinder = new URLClassLoader(urlList
                    .toArray(urls), null);
            }
        }

        private static void updatePackages() {
            String[][] packages = VMClassRegistry.getSystemPackages(systemPackages.size());
            if (null == packages) {
                return;
            }
            for (int i = 0; i < packages.length; i++) {
                
                String name = packages[i][0];
                if (systemPackages.containsKey(name)) {
                    continue;
                }             
                
                String jarURL = packages[i][1];             
                systemPackages.put(name, new Package(null, name, jarURL));
            }
        }
    }

    private static final class SystemClassLoader extends URLClassLoader {

        private boolean checkingPackageAccess = false;

        private SystemClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        @Override
        protected java.security.PermissionCollection getPermissions(CodeSource codesource) {
            java.security.PermissionCollection pc = super.getPermissions(codesource);
            pc.add(org.apache.harmony.lang.RuntimePermissionCollection.EXIT_VM_PERMISSION); 
            return pc;
        }
        
        @Override
        protected synchronized Class<?> loadClass(String className,
                boolean resolveClass) throws ClassNotFoundException {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null && !checkingPackageAccess) {
                int index = className.lastIndexOf('.');
                if (index > 0) { // skip if class is from a default package
                    try {
                        checkingPackageAccess = true;
                        sm.checkPackageAccess(className.substring(0, index));
                    } finally {
                        checkingPackageAccess = false;
                    }
                }
            }
            return super.loadClass(className, resolveClass);
        }

        private static URLClassLoader instance;

        static {
            ArrayList<URL> urlList = new ArrayList<URL>();
            // TODO avoid security checking?
            String extDirs = System.getProperty("java.ext.dirs", "");

            // -Djava.ext.dirs="" should be interpreted as nothing defined,
            // like we do below:
            String st[] = fracture(extDirs, File.pathSeparator);
            int l = st.length;
            for (int i = 0; i < l; i++) {
                try {
                    File dir = new File(st[i]).getAbsoluteFile();
                    File[] files = dir.listFiles();
                    for (int j = 0; j < files.length; j++) {
                        urlList.add(files[j].toURI().toURL());
                    }
                } catch (Exception e) {
                }
            }
            // TODO avoid security checking?
            String classPath = System.getProperty("java.class.path",
                    File.pathSeparator);
            st = fracture(classPath, File.pathSeparator);
            l = st.length;
            for (int i = 0; i < l; i++) {
                try {
                    if(st[i].length() == 0) {
                        st[i] = ".";
                    }
                    urlList.add(new File(st[i]).toURI().toURL());
                } catch (MalformedURLException e) {
                    assert false: e.toString();
                }
            }
            instance = new SystemClassLoader(urlList
                .toArray(new URL[urlList.size()]), null);
        }

        public static ClassLoader getInstance() {
            return instance;
        }
    }
}
