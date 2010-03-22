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

package org.apache.harmony.beans.tests.java.beans;

import java.awt.Color;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.InputStream;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Regression test for PropertyEditorManager
 */
public class PropertyEditorManagerRegressionTest extends TestCase {

    static String origPath[] = PropertyEditorManager.getEditorSearchPath();

    private final String MOCK_TEST_CLASS_EDITOR = "testPackage.TestClassEditor";

    private final String MOCK_TEST_CLASS_EDITOR_FILE
            = "binary/java/beans/mock/TestClassEditor.bin";

    public void testFindEditorAccordingPath_1() throws Exception {
        // Regression Harmony-1205
        String newPath[] = new String[origPath.length + 1];
        newPath[0] = "org.apache.harmony.beans.tests.support";
        for (int i = 0; i < origPath.length; i++) {
            newPath[i + 1] = origPath[i];
        }

        PropertyEditorManager.setEditorSearchPath(newPath);

        PropertyEditor editor = PropertyEditorManager.findEditor(Class
                .forName("java.lang.String"));

        assertEquals(org.apache.harmony.beans.tests.support.StringEditor.class,
                editor.getClass());
    }

    public void testFindEditorAccordingPath_2() throws Exception {
        // Regression Harmony-1205
        String newPath[] = new String[origPath.length + 1];
        newPath[origPath.length] = "org.apache.harmony.beans.tests.support";
        for (int i = 0; i < origPath.length; i++) {
            newPath[i] = origPath[i];
        }

        PropertyEditorManager.setEditorSearchPath(newPath);

        PropertyEditor editor = PropertyEditorManager.findEditor(Class
                .forName("java.lang.String"));

        assertEquals(org.apache.harmony.beans.editors.StringEditor.class,
                editor.getClass());
    }

    public void testFindEditorAccordingPath_3() throws Exception {
        // Regression Harmony-5477
        class TestClassLoader extends ClassLoader {
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (!MOCK_TEST_CLASS_EDITOR.equals(name)) {
                    throw new ClassNotFoundException(name);
                }

                try {
                    byte[] buf = new byte[1024];
                    InputStream in = getResourceAsStream(MOCK_TEST_CLASS_EDITOR_FILE);
                    int sz = 0;
                    int read;

                    while ((read = in.read(buf, sz, buf.length - sz)) >= 0) {
                        sz += read;
                    }
                    return defineClass(MOCK_TEST_CLASS_EDITOR, buf, 0, sz);
                } catch (IOException e) {
                    throw (ClassNotFoundException) new ClassNotFoundException(
                            e.getMessage()).initCause(e);
                }
            }
        }
        PropertyEditorManager.setEditorSearchPath(new String[] { "testPackage" });
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(new TestClassLoader());
            PropertyEditor editor = PropertyEditorManager.findEditor(TestClass.class);
            assertEquals(MOCK_TEST_CLASS_EDITOR, editor.getClass().getName());
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    public void testStringEditor() throws Exception {
        // Regression Harmony-1199
        PropertyEditorManager.setEditorSearchPath(origPath);
        PropertyEditor editor = PropertyEditorManager.findEditor(Class
                .forName("java.lang.String"));
        String text = "A sample string";

        editor.setAsText(text);
        assertEquals(text, editor.getAsText());
    }
    
    // Regression Harmony-4062
    public void testColorEditor() {
        PropertyEditor propertyEditor = PropertyEditorManager
                .findEditor(Color.class);
        propertyEditor.setValue(new Color(0, 0, 0));
        propertyEditor.setAsText("1,2,3");
        Color newColor = new Color(1, 2, 3);
        assertEquals(newColor, propertyEditor.getValue());

        try {
            propertyEditor.setAsText(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected.
        }

        try {
            propertyEditor.setAsText("illegalArugment");
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected.
        }

        try {
            propertyEditor.setAsText("1,2,3,4");
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //expected.
        }
    }

    String[] defaultSearchPath;
    
    public void setUp(){
        defaultSearchPath = PropertyEditorManager.getEditorSearchPath();
    }
    
    public void tearDown(){
        PropertyEditorManager.setEditorSearchPath(defaultSearchPath);
    }
}

class TestClass {}
