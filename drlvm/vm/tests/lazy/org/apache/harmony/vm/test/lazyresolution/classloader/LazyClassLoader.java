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

package org.apache.harmony.vm.test.lazyresolution.classloader;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import junit.framework.*;

public class LazyClassLoader extends ClassLoader {

    public static final String DATA_PACKAGE_PREFIX = "org.apache.harmony.vm.test.lazyresolution.data.";

    public int numLoads = 0;
    
    private Set<String> hiddenClasses = new HashSet<String>();
    private List<String> loadedInTest = new ArrayList<String>();
    public boolean useBrokenPackage = false;

    private ClassLoader cl = null;

    public LazyClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        //System.err.println("LOAD1:"+name);
        if (name.startsWith(DATA_PACKAGE_PREFIX)) {
            if (hiddenClasses.contains(name)) {
                throw new ClassNotFoundException("Error while loading class: "+name);
            }
            //System.err.println("LOAD2:"+name);
//            new Exception().printStackTrace();
            numLoads++;
            loadedInTest.add(name.substring(DATA_PACKAGE_PREFIX.length()));

            switch(numLoads) { 
                //Special check to force recursive compilation. 
                //Simplifies debugging of Jitrino.OPT
                case 1: foo1(); break;
                case 2: foo2(); break;
                case 3: foo3(); break;
                case 4: foo4(); break;
                case 5: foo5(); break;
                case 6: foo6(); break;
            }
            return loadFromStream(name);
        } 
        return cl.loadClass(name);
    }

    private static long time;
    private static void foo1(){time = System.currentTimeMillis();}
    private static void foo2(){time = System.currentTimeMillis();}
    private static void foo3(){time = System.currentTimeMillis();}
    private static void foo4(){time = System.currentTimeMillis();}
    private static void foo5(){time = System.currentTimeMillis();}
    private static void foo6(){time = System.currentTimeMillis();}

    private Class loadFromStream(String name) throws ClassNotFoundException {
        String resourceName = name.replace(".", "/") + ".class";
        if (useBrokenPackage) {
            resourceName = "broken/"+resourceName;
        }
        InputStream is = cl.getResourceAsStream(resourceName);
        try {
            int length = is.available();
            byte[] buf = new byte[length];
            int readLength = is.read(buf);
            return defineClass(name, buf, 0, length);
        } catch (Throwable t) {
            //t.printStackTrace();
            throw new ClassNotFoundException("Error while loading class: "+name ,t);
        }
    }

    public void runTest(TestCase junit, String testNameWithClassSuffix) throws Throwable {
        try {
            int dot = testNameWithClassSuffix.indexOf('.');
            String testName = testNameWithClassSuffix.substring(dot+1);
            String classSuffix = testNameWithClassSuffix.substring(0, dot);
//            System.err.println("CREATE TEST");
            Object test = newTest(junit, classSuffix);
//            System.err.println("FIND METHOD");
            Method method = test.getClass().getMethod(testName, (Class[])null);
//            System.err.println("FIND INVOKE");
            method.invoke(test);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
 

    private Class testClass;
    public Object newTest(TestCase junitTest, String classSuffix) throws Exception {
        Constructor<TestCase> c = getTestClass(classSuffix).getConstructor();
        Object res = c.newInstance();
        return res;
    }
    
    public Class<TestCase> getTestClass(String classSuffix) throws Exception {
        if (testClass == null) {
            testClass = loadClass(DATA_PACKAGE_PREFIX +classSuffix);
        }
        return testClass;
    }
    
    public Test newTest(String classSuffix) throws Exception {
        Constructor<TestCase> c = getTestClass(classSuffix).getConstructor();
        Test res = c.newInstance();
        return res;
    }

    public void hideClass(String name) {
        String className = DATA_PACKAGE_PREFIX + name;
        hiddenClasses.add(className);
    }

    public void restoreClass(String name) {
        String className = DATA_PACKAGE_PREFIX + name;
        hiddenClasses.remove(className);
    }


    public void startTest() {
       loadedInTest.clear();
       numLoads = 0;
    }

    public void endTest() {
       //System.out.println("loaded:" +loadedInTest);
    }

    public void assertNumLoads(int expected) {
        //Assert.assertTrue((start);
    }

    public void assertLoaded(String name) throws Throwable {
        Assert.assertTrue(name + " is not in " + loadedInTest, loadedInTest.contains(name));
    }

}
