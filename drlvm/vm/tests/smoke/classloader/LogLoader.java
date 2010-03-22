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
/** 
 * @author Pavel Pervov
 */  
package classloader;
import java.io.InputStream;

/**
 * Test delegation model.
 */
public class LogLoader extends ClassLoader {
    final static int LENGTH = 50000;

    public Class loadClass(String name) throws ClassNotFoundException {
        System.out.println("LogLoader.loadClass(\"" + name +"\")");
        if (name.equals(this.getClass().getSuperclass().getName())) {
            System.out.println("Delegating");
            final Class c = this.getParent().loadClass(name);
            System.out.println("The class " + name
                + " is loaded by parent classloader");
            passed = true;
            return c;
        }
        final String path = name.replace('.', '/') + ".class";
        InputStream is = ClassLoader.getSystemResourceAsStream(path);
        if (is == null) {
            System.out.println("Cannot find " + path);
            return null;
        }
        int bytes_read = 0, offset = 0;
        byte[] buffer = new byte[LENGTH];
        try {
            while ((bytes_read = is.read(buffer, offset, LENGTH - bytes_read)) != -1) {
                offset += bytes_read;
            }
        } catch (java.io.IOException ioe) {
            System.out.println("IOE = " + ioe);
        }
        System.out.println("offset = " + offset);
        if (offset == LENGTH) {
            System.out.println("Class too big, please increase LENGTH = "
            + LENGTH);
        }

        try {
            System.out.println("Defining");
            final Class c = this.defineClass(name, buffer, 0, offset);
            System.out.println("The class " + name
                + " is successfully defined");
            return c;
        } catch (Exception e) {
            System.out.println("E = " + e);
            return null;
        }
    }

    static boolean passed = false;
    public static void main(String[] s) {
        LogLoader ll = new LogLoader();
        try {
            ll.loadClass("classloader.LogLoader");
        } catch (Exception e) {
            System.out.println("E = " + e);
        }
        if (passed) {
            System.out.println("pass");
        }
    }
}

