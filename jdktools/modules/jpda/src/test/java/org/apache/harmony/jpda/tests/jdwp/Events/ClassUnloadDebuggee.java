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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Aleksander V. Budniy
 */

/**
 * Created on 25.11.2006
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Debuggee for ClassUnloadTest unit test.
 */
public class ClassUnloadDebuggee extends SyncDebuggee {

	public static final String TESTED_CLASS_NAME = 
		"org.apache.harmony.jpda.tests.jdwp.Events.ClassUnloadTestedClass";
	
	public static final int ARRAY_SIZE_FOR_MEMORY_STRESS = 1000000;
    
	public static volatile boolean classUnloaded = false;

	public static void main(String[] args) {
        runDebuggee(ClassUnloadDebuggee.class);
    }
    
    public void run() {
        logWriter.println("--> ClassUnloadDebuggee started");
        
        // Test class prepare
        logWriter.println("--> Load and prepare tested class");
        CustomLoader loader = new CustomLoader(logWriter);
        
        Class cls = null;
        try {
			cls = Class.forName(TESTED_CLASS_NAME, true, loader);
	        logWriter.println("--> Tested class loaded: " + cls);
		} catch (Exception e) {
	        logWriter.println("--> Unable to load tested class: " + e);
			throw new TestErrorException(e);
		}

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
		
        logWriter.println("--> Erase references to loaded class and its class loader");
        classUnloaded = false;
        cls = null;
        loader = null;
        
        logWriter.println("--> Create memory stress and start gc");
        createMemoryStress(1000000, ARRAY_SIZE_FOR_MEMORY_STRESS);
//        createMemoryStress(100000000, 1024);
        System.gc();
        
        String status = (classUnloaded ? "UNLOADED" : "LOADED");
        logWriter.println("--> Class status after memory stress: " + status);
        synchronizer.sendMessage(status);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        logWriter.println("--> ClassUnloadDebuggee finished");
    }
    
    /*
     * Stress algorithm for eating memory.
     */
    protected void createMemoryStress(int arrayLength_0, int arrayLength_1) {
        Runtime currentRuntime = Runtime.getRuntime();
        long freeMemory = currentRuntime.freeMemory();
        logWriter.println
        ("--> Debuggee: createMemoryStress: freeMemory (bytes) before memory stress = " + freeMemory);

        long[][] longArrayForCreatingMemoryStress = null;
        
        int i = 0;
        try {
            longArrayForCreatingMemoryStress = new long[arrayLength_0][];
            for (; i < longArrayForCreatingMemoryStress.length; i++) {
                longArrayForCreatingMemoryStress[i] = new long[arrayLength_1];
            }
            logWriter.println("--> Debuggee: createMemoryStress: NO OutOfMemoryError!!!");
        } catch ( OutOfMemoryError outOfMem ) {
            longArrayForCreatingMemoryStress = null;
            logWriter.println("--> Debuggee: createMemoryStress: OutOfMemoryError!!!");
        }
        freeMemory = currentRuntime.freeMemory();
        logWriter.println
        ("--> Debuggee: createMemoryStress: freeMemory after creating memory stress = " + freeMemory);
        
        longArrayForCreatingMemoryStress = null;
    }

    /**
     * More eager algorithm for eating memory.
     */
/*
	protected void createMemoryStress(int maxChunkSize, int minChunkSize) {
        Runtime currentRuntime = Runtime.getRuntime();
        long freeMemory = currentRuntime.freeMemory();
        logWriter.println
        ("--> Debuggee: createMemoryStress: freeMemory (bytes) before memory stress = " + freeMemory);

        LinkedList list = new LinkedList();
        int countOOM = 0;
        
        for (int chunkSize = maxChunkSize; chunkSize >= minChunkSize; chunkSize /= 2) {
        	try {
                for (;;) {
                	long[] chunk = new long[chunkSize];
                	list.add(chunk);
                }
            } catch (OutOfMemoryError outOfMem) {
                countOOM++;
                System.gc();
        	}
        }
        
        // enable to collect allocated memory
        list = null;

        freeMemory = currentRuntime.freeMemory();
        logWriter.println
        ("--> Debuggee: createMemoryStress: freeMemory after creating memory stress = " + freeMemory);

        logWriter.println
        ("--> Debuggee: createMemoryStress: OutOfMemoryError occured: " + countOOM);
    }
*/
    
    /**
     * Custom class loader to be used for tested class.
     * It will be collected and finalized when tested class is unloaded.
     */
    static class CustomLoader extends ClassLoader {
        private LogWriter logWriter;

        public CustomLoader(LogWriter writer) {
            this.logWriter = writer;
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (TESTED_CLASS_NAME.equals(name)) {
                // load only tested class with this loader
                return findClass(name);
            }
            return getParent().loadClass(name);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                logWriter.println("-->> CustomClassLoader: Find class: " + name);
	        	String res = name.replace('.', '/') + ".class";
	            URL url = getResource(res);
                logWriter.println("-->> CustomClassLoader: Found class file: " + res);
	    		InputStream is = url.openStream();
	            int size = 1024;
	            byte bytes[] = new byte[size];
	            int len = loadClassData(is, bytes, size);
                logWriter.println("-->> CustomClassLoader: Loaded class bytes: " + len);
	            Class cls = defineClass(name, bytes, 0, len);
                logWriter.println("-->> CustomClassLoader: Defined class: " + cls);
//	            resolveClass(cls);
//                logWriter.println("-->> CustomClassLoader: Resolved class: " + cls);
	            return cls;
        	} catch (Exception e) {
        		throw new ClassNotFoundException("Cannot load class: " + name, e);
        	}
        }

        private int loadClassData(InputStream in, byte[] raw, int size) throws IOException {
            int len = in.read(raw);
            if (len >= size)
                throw new IOException("Class file is too big: " + len);
            in.close();
            return len;
        }

        protected void finalize() throws Throwable {
            logWriter.println("-->> CustomClassLoader: Class loader finalized => tested class UNLOADED");
            ClassUnloadDebuggee.classUnloaded = true;
       }
    }
}

/**
 * Internal class used in ClassUnloadTest
 */
class ClassUnloadTestedClass {
}
