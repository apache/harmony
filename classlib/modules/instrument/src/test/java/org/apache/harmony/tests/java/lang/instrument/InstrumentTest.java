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

package org.apache.harmony.tests.java.lang.instrument;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import tests.support.Support_Exec;

public class InstrumentTest extends TestCase {

    /**
     * @tests try to add a null Transformer
     */
    public void test_addTransformer_null() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/Premain_addTransformer_null.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/Premain_addTransformer_null.jar";
        String result = Support_Exec.execJava(arg, path, true);
        assertEquals("", result);
    }

    /**
     * @tests try to remove a null Transformer
     */
    public void test_removeTransformer_null() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/Premain_removeTransformer_null.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/Premain_removeTransformer_null.jar";
        String result = Support_Exec.execJava(arg, path, true);
        assertEquals("", result);
    }

    /**
     * @tests try to remove a non-exists Transformer
     */
    public void test_removeTransformer_notExists() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/Premain_removeTransformer_notExists.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/Premain_removeTransformer_notExists.jar";
        String result = Support_Exec.execJava(arg, path, true);
        assertEquals("", result);
    }

    /**
     * @tests try to load a class that does not exist
     */
    public void test_loadClass_null() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/loading_class.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/loading_class.jar";
        try {
            Support_Exec.execJava(arg, path, true);
            fail("Should fail here!");
        } catch (AssertionFailedError e) {
            // class loader changes, can not load classes
            assertTrue(-1 != e.getMessage().indexOf("NoClassDefFoundError"));
        }
    }

    /**
     * @tests try to use a new ClassLoader
     */
    public void test_new_classLoader_Exists() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/new_classloader.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/new_classloader.jar";
        String result = Support_Exec.execJava(arg, path, true);
        assertNotNull(result);
    }

    /**
     * @tests test if attribute of "Premain-Class" is null
     */
    public void test_Property_Premain_null() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/Property_Premain_null.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/Property_Premain_null.jar";
        try {
            Support_Exec.execJava(arg, path, true);
            fail("Should fail here!");
        } catch (AssertionFailedError e) {
            // expected
            assertTrue(-1 != e.getMessage().indexOf("Failed")
                    || -1 != e.getMessage().indexOf("error"));
            assertTrue(-1 != e.getMessage().indexOf("Premain-Class"));
            assertTrue(-1 != e.getMessage().indexOf("attribute"));
        }
    }

    /**
     * @tests test if attributes of "Can-Redefine-Classes" and "Boot-Class-Path"
     *        is null
     */
    public void test_Property_other_null() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/Property_other_null.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/Property_other_null.jar";
        String result = Support_Exec.execJava(arg, path, true);
        assertNotNull(result);
    }

    /**
     * @tests test if the attributes is case-sensitive
     */
    public void test_Properity_case_sensitive() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/Properity_case_sensitive.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/Properity_case_sensitive.jar";
        String result = Support_Exec.execJava(arg, path, true);
        assertEquals("", result);
    }

    /**
     * @tests test if the jar file is bad
     */
    public void test_BadFormatJar() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/BadFormatJar.jar";
        arg[1] = "";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/BadFormatJar.jar";
        try {
            Support_Exec.execJava(arg, path, true);
            fail("Should fail here!");
        } catch (AssertionFailedError e) {
            // expected
            assertTrue(-1 != e.getMessage().indexOf("error")
                    || -1 != e.getMessage().indexOf("Error"));
            assertTrue(-1 != e.getMessage().indexOf("open"));
            assertTrue(-1 != e.getMessage().indexOf("file"));
        }
    }

    /**
     * @tests test if premain class is null
     */
    public void test_Premain_Class_null() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/Premain_Class_null.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/Premain_Class_null.jar";
        try {
            Support_Exec.execJava(arg, path, true);
            fail("Should fail here!");
        } catch (AssertionFailedError e) {
            // expected
            assertTrue(-1 != e.getMessage().indexOf("ClassNotFoundException"));
        }
    }

    /**
     * @tests test transforming all classes to a zero byte
     */
    public void test_zero_byte_transformer() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/zero_byte_class.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/zero_byte_class.jar";
        String result = Support_Exec.execJava(arg, path, true);
        assertEquals("", result);
    }

    /**
     * @tests test if complied by version 1.4
     */
    public void test_old_version() throws Exception {
        String[] arg = new String[2];
        arg[0] = "-javaagent:src/test/resources/jars/org/apache/harmony/tests/instrument/old_version_class.jar";
        arg[1] = "org/apache/harmony/tests/java/lang/instrument/TestMain";
        String[] path = new String[1];
        path[0] = "src/test/resources/jars/org/apache/harmony/tests/instrument/old_version_class.jar";
        String result = Support_Exec.execJava(arg, path, true);
        assertEquals("", result);
    }
}
