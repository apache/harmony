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

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.security.SecurityPermission;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyPermission;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.Channel;

import org.apache.harmony.lang.RuntimePermissionCollection;
import org.apache.harmony.vm.VMStack;
import org.apache.harmony.luni.platform.Environment;
//import org.apache.harmony.drlvm.VMHelper;
//import org.apache.harmony.drlvm.gc_gen.GCHelper;

/**
 * @com.intel.drl.spec_ref 
 * 
 * @author Roman S. Bushmanov
 */
public final class System {

    /**
     * This class can not be instantiated.
     */
    private System() {
    }

    static String getPropertyUnsecure(String key) {
        return getPropertiesUnsecure().getProperty(key);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static final PrintStream err = createErr();

    /**
     * @com.intel.drl.spec_ref
     */
    public static final InputStream in = createIn();

    /**
     * @com.intel.drl.spec_ref
     */
    public static final PrintStream out = createOut();

    /**
     * Current system security manager
     */
    private static SecurityManager securityManager = null;

    /**
     * Current system properties
     */
    private static Properties systemProperties = null;

    /**
     * @com.intel.drl.spec_ref
     */
    public static void arraycopy(Object src, int srcPos, Object dest,
                                 int destPos, int length) {
        VMMemoryManager.arrayCopy(src, srcPos, dest, destPos, length);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static long currentTimeMillis() {
        return VMExecutionEngine.currentTimeMillis();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void gc() {
        Runtime.getRuntime().gc();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static String getenv(String name) {
        if (name == null) {
            throw new NullPointerException("name should not be null");
        }
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("getenv." + name));
        }
        return Environment.getenv(name);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Map<String, String> getenv() {
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPermission(RuntimePermissionCollection.GETENV_PERMISSION);
        }
        return Environment.getenv();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Properties getProperties() {
        if (securityManager != null) {
            securityManager.checkPropertiesAccess();
        }
        return getPropertiesUnsecure();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static String getProperty(String key, String def) {
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPropertyAccess(key);
        } else if (key.length() == 0) {
            throw new IllegalArgumentException("key is empty");
        }
        Properties props = getPropertiesUnsecure();
        return props.getProperty(key, def);
    }
    
    /**
     * @com.intel.drl.spec_ref
     */
    public static String clearProperty(String key){
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPermission(new PropertyPermission(key, "write"));
        } else if (key.length() == 0) {
            throw new IllegalArgumentException("key is empty");
        }
        Properties props = getPropertiesUnsecure();
        return (String)props.remove(key);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static SecurityManager getSecurityManager() {
        return securityManager;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static int identityHashCode(Object object) {
//	if (VMHelper.isVMMagicPackageSupported()) {
//                return GCHelper.get_hashcode(object);
//        } else {
//                return VMMemoryManager.getIdentityHashCode(object);
//        } 
        return VMMemoryManager.getIdentityHashCode(object);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Channel inheritedChannel() throws IOException{
    	//XXX:does it mean the permission of the "access to the channel"?
    	//If YES then this checkPermission must be removed because it should be presented into java.nio.channels.spi.SelectorProvider.inheritedChannel()
    	//If NO  then some other permission name (which one?) should be used here
    	//and the corresponding constant should be placed within org.apache.harmony.lang.RuntimePermission class: 
        if (securityManager != null) {
        	securityManager.checkPermission(new RuntimePermission("inheritedChannel")); //see java.nio.channels.spi.SelectorProvider.inheritedChannel() spec
        }
        
        return SelectorProvider.provider().inheritedChannel();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void load(String filename) {
        Runtime.getRuntime().load0(
                filename,
                VMClassRegistry.getClassLoader(VMStack.getCallerClass(0)), 
                true);
    }

    public static void loadLibrary(String libname) {
        Runtime.getRuntime().loadLibrary0(
                libname,
                VMClassRegistry.getClassLoader(VMStack.getCallerClass(0)), 
                true);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static String mapLibraryName(String libname) {
        if (libname == null) {
            throw new NullPointerException("libname should not be empty");
        }
        return VMExecutionEngine.mapLibraryName(libname);
    }
  
    /**
     * @com.intel.drl.spec_ref
     */
    public static long nanoTime() {
        return VMExecutionEngine.nanoTime();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void runFinalization() {
        Runtime.getRuntime().runFinalization();
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public static void runFinalizersOnExit(boolean value) {
        Runtime.runFinalizersOnExit(value);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void setErr(PrintStream err) {
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPermission(RuntimePermissionCollection.SET_IO_PERMISSION);
        }
        setErrUnsecure(err);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void setIn(InputStream in) {
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPermission(RuntimePermissionCollection.SET_IO_PERMISSION);
        }
        setInUnsecure(in);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void setOut(PrintStream out) {
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPermission(RuntimePermissionCollection.SET_IO_PERMISSION);
        }
        setOutUnsecure(out);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void setProperties(Properties props) {
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPropertiesAccess();
        }
        systemProperties = props;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static String setProperty(String key, String value) {
        if (key.length() == 0) {
            throw new IllegalArgumentException("key is empty");
        }
        SecurityManager sm = securityManager;
        if (sm != null) {
            sm.checkPermission(new PropertyPermission(key, "write"));
        }
        Properties props = getPropertiesUnsecure();
        return (String)props.setProperty(key, value);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static synchronized void setSecurityManager(SecurityManager sm) {
        if (securityManager != null) {
            securityManager
                .checkPermission(RuntimePermissionCollection.SET_SECURITY_MANAGER_PERMISSION);
        }

        if (sm != null) {
            // before the new manager assumed office, make a pass through 
            // the common operations and let it load needed classes (if any),
            // to avoid infinite recursion later on 
            try {
                sm.checkPermission(new SecurityPermission("getProperty.package.access")); 
            } catch (Exception ignore) {}
            try {
                sm.checkPackageAccess("java.lang"); 
            } catch (Exception ignore) {}
        }

        securityManager = sm;
    }

    /**
     * Constructs a system <code>err</code> stream. This method is used only
     * for initialization of <code>err</code> field
     */
    private static PrintStream createErr() {
        return new PrintStream(new BufferedOutputStream(new FileOutputStream(
                FileDescriptor.err)), true);
    }

    /**
     * Constructs a system <code>in</code> stream. This method is used only
     * for initialization of <code>in</code> field
     */
    private static InputStream createIn() {
        return new BufferedInputStream(new FileInputStream(FileDescriptor.in));
    }

    /**
     * Constructs a system <code>out</code> stream. This method is used only
     * for initialization of <code>out</code> field
     */
    private static PrintStream createOut() {
        return new PrintStream(new BufferedOutputStream(new FileOutputStream(
                FileDescriptor.out)), true);
    }

    /**
     * Returns system properties without security checks. Initializes the system
     * properties if it isn't done yet.
     */
    private static Properties getPropertiesUnsecure() {
        Properties sp = systemProperties;
        if (sp == null) {
            systemProperties = sp = VMExecutionEngine.getProperties();
        }
        return sp;
    }

    /**
     * Initiaies the VM shutdown sequence.
     */
    static void execShutdownSequence() {
        Runtime.getRuntime().execShutdownSequence();
    }

    /**
     * Sets the value of <code>err</code> field without any security checks
     */
    private static native void setErrUnsecure(PrintStream err);

    /**
     * Sets the value of <code>in</code> field without any security checks
     */
    private static native void setInUnsecure(InputStream in);

    /**
     * Sets the value of <code>out</code> field without any security checks
     */
    private static native void setOutUnsecure(PrintStream out);

    /**
     *  Helps to throw an arbitrary throwable without mentioning within 
     *  <code>throw</code> clause and so bypass 
     *  exception checking by a compiler.
     *  
     *  @see java.lang.Class#newInstance()
     */
    native static void rethrow(Throwable tr);
}

