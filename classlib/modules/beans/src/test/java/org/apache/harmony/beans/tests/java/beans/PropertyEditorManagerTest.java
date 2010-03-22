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
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.AnotherSampleProperty;
import org.apache.harmony.beans.tests.support.AnotherSamplePropertyEditor;
import org.apache.harmony.beans.tests.support.OtherEditor;
import org.apache.harmony.beans.tests.support.SampleProperty;
import org.apache.harmony.beans.tests.support.SamplePropertyEditor;
import org.apache.harmony.beans.tests.support.mock.Foz;
import org.apache.harmony.beans.tests.support.mock.Fozz;
import org.apache.harmony.beans.tests.support.mock.FozzEditor;
import org.apache.harmony.beans.tests.support.mock.Fozzz;
import org.apache.harmony.beans.tests.support.mock.MockButton;
import org.apache.harmony.beans.tests.support.mock.MockFoo;

/**
 * Unit test for PropertyEditorManager
 */
public class PropertyEditorManagerTest extends TestCase {

    /*
     * Constructors
     */
    public void testPropertyEditorManager() {
        new PropertyEditorManager();
    }

    /*
     * find the editor which has been registered through registerEditor.
     */
    public void testFindEditor_registered() {
        Class<FozRegisteredEditor> editorClass = FozRegisteredEditor.class;
        Class<Foz> type = Foz.class;
        PropertyEditorManager.registerEditor(type, editorClass);
        PropertyEditor editor = PropertyEditorManager.findEditor(type);
        assertTrue(editor instanceof FozRegisteredEditor);
        assertEquals(editorClass, editor.getClass());
    }

    /*
     * Find editor of which name is XXXEditor in the same package
     */
    public void testFindEditor_SamePackage() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Fozz.class);
        assertTrue(editor instanceof FozzEditor);
        assertEquals(FozzEditor.class, editor.getClass());
    }

    /*
     * Find editor in search path
     */
    public void testFindEditor_DifferentPackage() {
        String[] original = PropertyEditorManager.getEditorSearchPath();
        PropertyEditorManager
                .setEditorSearchPath(new String[] { "org.apache.harmony.beans.tests.java.beans" });
        PropertyEditor editor = PropertyEditorManager.findEditor(Fozzz.class);
        assertTrue(editor instanceof FozzzEditor);
        assertEquals(FozzzEditor.class, editor.getClass());

        PropertyEditorManager.setEditorSearchPath(original);
    }

    /*
     * Find editor for Java primitive types and java.lang.String.
     * java.awt.Color, and java.awt.Font
     */
    public void testFindEditor_DefaultType() {
        PropertyEditorManager.findEditor(Integer.TYPE);
    }

    // Regression test for HARMONY-258
    public void testFindEditor_TypeNull() {
        try {
            PropertyEditorManager.findEditor(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testFindEditor_TypeNoEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(MockFoo.class);
        assertNull(editor);
    }

    public void testGetEditorSearchPath_default() {
        String[] path = PropertyEditorManager.getEditorSearchPath();
        assertEquals(1, path.length);
        assertTrue(path[0].endsWith("beans.editors"));
    }

    public void testGetEditorSearchPath() {
        String[] original = PropertyEditorManager.getEditorSearchPath();

        String[] path = new String[] { "java.beans",
                "org.apache.harmony.beans.tests.java.beans.editor", "", };
        PropertyEditorManager.setEditorSearchPath(path);
        String[] newPath = PropertyEditorManager.getEditorSearchPath();

        assertTrue(Arrays.equals(path, newPath));

        PropertyEditorManager.setEditorSearchPath(original);
    }

    /*
     * RegisterEditor
     */
    public void testRegisterEditor() {
        Class<MockButton> type = MockButton.class;

        PropertyEditorManager.registerEditor(type, ButtonEditor.class);
        PropertyEditor editor = PropertyEditorManager.findEditor(type);
        assertEquals(ButtonEditor.class, editor.getClass());

        PropertyEditorManager.registerEditor(type, FozRegisteredEditor.class);
        editor = PropertyEditorManager.findEditor(type);
        assertEquals(FozRegisteredEditor.class, editor.getClass());

        PropertyEditorManager.registerEditor(type, null);
        editor = PropertyEditorManager.findEditor(type);
        assertNull(editor);
    }

    /*
     * registerEditor for type null Regression test for HARMONY-258
     */
    public void testRegisterEditorType_Null() {
        try {
            PropertyEditorManager.registerEditor(null, ButtonEditor.class);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * set search path as {null}
     */
    public void testSetEditorSearchPath_nullpath() {
        String[] original = PropertyEditorManager.getEditorSearchPath();
        PropertyEditorManager.setEditorSearchPath(new String[] { null });
        assertEquals(1, PropertyEditorManager.getEditorSearchPath().length);
        assertNull(PropertyEditorManager.getEditorSearchPath()[0]);
        PropertyEditorManager.setEditorSearchPath(original);
    }

    /*
     * set search null
     */
    public void testSetEditorSearchPath_null() {
        String[] original = PropertyEditorManager.getEditorSearchPath();
        PropertyEditorManager.setEditorSearchPath(null);
        assertEquals(0, PropertyEditorManager.getEditorSearchPath().length);
        PropertyEditorManager.setEditorSearchPath(original);
    }

    // Test internal Editor
    public void testBoolEditor_setAsText() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Boolean.TYPE);
        editor.setAsText("false");
        assertEquals("false", editor.getAsText().toLowerCase());
        assertEquals("false", editor.getJavaInitializationString());
        assertEquals("True", editor.getTags()[0]);
        assertEquals("False", editor.getTags()[1]);
        assertEquals(Boolean.FALSE, editor.getValue());

        editor.setAsText("TrUE");
        assertEquals("true", editor.getAsText().toLowerCase());
        assertEquals("true", editor.getJavaInitializationString());
        assertEquals(Boolean.TRUE, editor.getValue());
    }

    public void testBoolEditor_setAsText_null() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Boolean.TYPE);
        try {
            editor.setAsText(null);
            fail("Should throw a NPException");
        } catch (NullPointerException e) {
        }
    }

    public void testBoolEditor_setAsText_Invalid() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Boolean.TYPE);
        try {
            editor.setAsText("yes");
            fail("Should throw a IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            editor.setAsText("***true***");
            fail("Should throw a IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    // Regression test for HARMONY-5711
    @SuppressWarnings("nls")
    public void testBooleanEditor() {
        PropertyEditor propertyEditor = PropertyEditorManager
                .findEditor(boolean.class);
        assertNotNull(propertyEditor);
        String tags[] = propertyEditor.getTags();
        assertEquals(2, tags.length);
        assertEquals("True", tags[0]);
        assertEquals("False", tags[1]);

        propertyEditor.setValue(Boolean.FALSE);
        assertEquals("False", propertyEditor.getAsText());
        assertEquals("false", propertyEditor.getJavaInitializationString());
        propertyEditor.setAsText("False");
        assertEquals("False", propertyEditor.getAsText());
        assertEquals("false", propertyEditor.getJavaInitializationString());
        propertyEditor.setAsText("false");
        assertEquals("False", propertyEditor.getAsText());
        assertEquals("false", propertyEditor.getJavaInitializationString());

        propertyEditor.setValue(Boolean.TRUE);
        assertEquals("True", propertyEditor.getAsText());
        assertEquals("true", propertyEditor.getJavaInitializationString());
        propertyEditor.setAsText("True");
        assertEquals("True", propertyEditor.getAsText());
        assertEquals("true", propertyEditor.getJavaInitializationString());
        propertyEditor.setAsText("true");
        assertEquals("True", propertyEditor.getAsText());
        assertEquals("true", propertyEditor.getJavaInitializationString());
    }
    
    public void testByteEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Byte.TYPE);
        byte b = (byte) 0x7F;
        editor.setAsText(Byte.toString(b));
        assertEquals(Byte.toString(b), editor.getAsText());
        assertEquals("((byte)127)", editor.getJavaInitializationString());
        assertEquals(new Byte(b), editor.getValue());
        assertNull(editor.getTags());
    }

    public void testByteEditor_null() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Byte.TYPE);
        try {
            editor.setAsText(null);
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testByteEditor_invalid() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Byte.TYPE);
        try {
            editor.setAsText("invalid");
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testByteEditor_invalid2() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Byte.TYPE);
        try {
            editor.setAsText("128");
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testDoubleEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Double.TYPE);
        double d = 12345.678;
        editor.setAsText(Double.toString(d));
        assertEquals(Double.toString(d), editor.getAsText());
        assertEquals(Double.toString(d), editor.getJavaInitializationString());
        assertEquals(new Double(d), editor.getValue());
        assertNull(editor.getTags());
    }

    public void testDoubleEditor_SetAsText_Null() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Double.TYPE);
        try {
            editor.setAsText(null);
            fail("Should throw a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testDoubleEditor_SetAsText_Invalid() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Double.TYPE);
        try {
            editor.setAsText("invalid");
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testFloatEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Float.TYPE);
        float f = 12345.678f;
        String text = Float.toString(f);
        editor.setAsText(text);
        assertEquals(text, editor.getAsText());
        assertEquals(text + "F", editor.getJavaInitializationString());
        assertEquals(new Float(f), editor.getValue());
        assertNull(editor.getTags());
    }

    public void testFloatEditor_SetAsText_Null() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Float.TYPE);
        try {
            editor.setAsText(null);
            fail("Should throw a NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testFloatEditor_SetAsText_Invalid() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Float.TYPE);
        try {
            editor.setAsText("invalid");
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testLongEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Long.TYPE);
        long l = 123456789;
        String text = Long.toString(l);
        editor.setAsText(text);
        assertEquals(text, editor.getAsText());
        assertEquals(text + "L", editor.getJavaInitializationString());
        assertEquals(new Long(l), editor.getValue());
        assertNull(editor.getTags());
    }

    public void testLongEditor_SetAsText_Null() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Long.TYPE);
        try {
            editor.setAsText(null);
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testLongEditor_SetAsText_Invalid() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Long.TYPE);
        try {
            editor.setAsText("invalid");
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testShortEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Short.TYPE);
        short s = (short) 123456789;
        String text = Short.toString(s);
        editor.setAsText(text);
        assertEquals(text, editor.getAsText());
        assertEquals("((short)" + text + ")", editor
                .getJavaInitializationString());
        assertEquals(new Short(s), editor.getValue());
        assertNull(editor.getTags());
    }

    public void testShortEditor_SetAsText_Null() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Short.TYPE);
        try {
            editor.setAsText(null);
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testShortEditor_SetAsText_Invalid() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Short.TYPE);
        try {
            editor.setAsText("invalid");
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testIntegerEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Integer.TYPE);
        int i = 123456789;
        String text = Integer.toString(i);
        editor.setAsText(text);
        assertEquals(text, editor.getAsText());
        assertEquals(text, editor.getJavaInitializationString());
        assertEquals(new Integer(i), editor.getValue());
        assertNull(editor.getTags());
    }

    public void testIntegerEditor_SetAsText_Null() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Integer.TYPE);
        try {
            editor.setAsText(null);
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
        }
    }

    public void testIntegerEditor_SetAsText_Invalid() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Integer.TYPE);
        try {
            editor.setAsText("invalid");
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {

        }
    }

    public void testStringEditor() {
        PropertyEditor editor = PropertyEditorManager.findEditor(String.class);
        String text = "A sample string";
        editor.setAsText(text);
        assertEquals(text, editor.getAsText());
        assertEquals("\"" + text + "\"", editor.getJavaInitializationString());
        assertEquals(text, editor.getValue());
        assertNull(editor.getTags());
    }

    public void testStringEditor_SetAsText_Null() {
        PropertyEditor editor = PropertyEditorManager.findEditor(String.class);

        editor.setAsText("null");
        assertEquals("null", editor.getAsText());
        assertEquals("\"null\"", editor.getJavaInitializationString());
        assertEquals("null", editor.getValue());

        editor.setAsText("");
        assertEquals("", editor.getAsText());
        assertEquals("\"\"", editor.getJavaInitializationString());

        editor.setAsText(null);
        assertEquals("null", editor.getAsText());
        assertEquals("\"null\"", editor.getJavaInitializationString());
        assertNull(editor.getValue());
    }

    public void testStringEditor_SetAsText_SpecialChars() {
        PropertyEditor editor = PropertyEditorManager.findEditor(String.class);
        String str = "\n\t\\a\"";
        editor.setAsText(str);
        assertEquals(str, editor.getAsText());
        assertEquals("\"\n\t\\a\"\"", editor
                .getJavaInitializationString());
    }

    public static class FozRegisteredEditor implements PropertyEditor {

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#addPropertyChangeListener(java.beans.PropertyChangeListener)
         */
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getAsText()
         */
        public String getAsText() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getCustomEditor()
         */
        public Component getCustomEditor() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getJavaInitializationString()
         */
        public String getJavaInitializationString() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getTags()
         */
        public String[] getTags() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getValue()
         */
        public Object getValue() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#isPaintable()
         */
        public boolean isPaintable() {
            // TO DO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#paintValue(java.awt.Graphics,
         *      java.awt.Rectangle)
         */
        public void paintValue(Graphics graphics, Rectangle box) {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#removePropertyChangeListener(java.beans.PropertyChangeListener)
         */
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#setAsText(java.lang.String)
         */
        public void setAsText(String text) throws IllegalArgumentException {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#setValue(java.lang.Object)
         */
        public void setValue(Object value) {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#supportsCustomEditor()
         */
        public boolean supportsCustomEditor() {
            // TO DO Auto-generated method stub
            return false;
        }

    }

    public static class ButtonEditor implements PropertyEditor {

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#addPropertyChangeListener(java.beans.PropertyChangeListener)
         */
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getAsText()
         */
        public String getAsText() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getCustomEditor()
         */
        public Component getCustomEditor() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getJavaInitializationString()
         */
        public String getJavaInitializationString() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getTags()
         */
        public String[] getTags() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#getValue()
         */
        public Object getValue() {
            // TO DO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#isPaintable()
         */
        public boolean isPaintable() {
            // TO DO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#paintValue(java.awt.Graphics,
         *      java.awt.Rectangle)
         */
        public void paintValue(Graphics graphics, Rectangle box) {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#removePropertyChangeListener(java.beans.PropertyChangeListener)
         */
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#setAsText(java.lang.String)
         */
        public void setAsText(String text) throws IllegalArgumentException {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#setValue(java.lang.Object)
         */
        public void setValue(Object value) {
            // TO DO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyEditor#supportsCustomEditor()
         */
        public boolean supportsCustomEditor() {
            // TO DO Auto-generated method stub
            return false;
        }

    }

    /**
     * The test checks the method findEditor() for registered editors
     */
    public void testFindRegisteredEditor() {
        PropertyEditorManager.registerEditor(SampleProperty.class,
                OtherEditor.class);
        PropertyEditor pe = PropertyEditorManager
                .findEditor(SampleProperty.class);
        assertNotNull("No property editor found", pe);
        assertTrue(pe instanceof OtherEditor);
        PropertyEditorManager.registerEditor(SampleProperty.class, null);
    }

    /**
     * The test checks the method findEditor() for editors found by name
     */
    public void testFindEditorByNameAddOn() {
        PropertyEditor pe = PropertyEditorManager
                .findEditor(SampleProperty.class);
        assertNotNull("No property editor found", pe);
        assertTrue(pe instanceof SamplePropertyEditor);
    }

    /**
     * The test checks the method findEditor() for editors on search path
     */
    public void testFindEditorByDefaultLocation() {
        PropertyEditorManager
                .setEditorSearchPath(new String[] { "org.apache.harmony.beans.tests.java.beans.editors" });
        PropertyEditor pe = PropertyEditorManager
                .findEditor(AnotherSampleProperty.class);

        assertNotNull("No property editor found", pe);
        assertTrue(pe instanceof AnotherSamplePropertyEditor);
    }
    
    public void testFontEditor() throws Exception{
        PropertyEditor e2 = PropertyEditorManager.findEditor(Font.class);
        Font font = new Font("Helvetica", Font.PLAIN, 12);
        e2.setValue(font);
        assertNull(e2.getAsText());
        assertNull(e2.getTags());
        assertSame(font, e2.getValue());
        assertTrue(e2.isPaintable());
        Component c = e2.getCustomEditor();
        assertSame(c, e2);
        e2.addPropertyChangeListener(new ExceptionPropertyChangeListener());

        try {
            e2.setValue(null);
            fail("Should throw an error");
        } catch (MockError e) {
            // expected
            assertNull(e2.getValue());
        }
        
        try {
            e2.setValue(new Font("Arial", Font.BOLD, 10));
            fail("Should throw an error");
        } catch (MockError e) {
            // expected
        }
    }
    
    @SuppressWarnings("serial")
    public static class MockError extends Error {
        public MockError(String msg) {
            super(msg);
        }
    }
    public static class ExceptionPropertyChangeListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent event) {
            throw new MockError("property changed");
        }

    }
    public void testColorEditor() throws Exception{
        PropertyEditor e2 = PropertyEditorManager.findEditor(Color.class);
        assertNull(e2.getValue());
        assertEquals("null", e2.getAsText());
        e2.setValue(Color.RED);
        e2.setAsText(e2.getAsText());
        assertNull(e2.getTags());
        assertNotSame(Color.RED, e2.getValue());
        assertEquals(Color.RED, e2.getValue());
        assertTrue(e2.isPaintable());
        assertTrue(e2 instanceof Panel);
        assertTrue(e2 instanceof PropertyEditor);
        assertTrue(e2.supportsCustomEditor());
        assertSame(e2, e2.getCustomEditor());
        assertEquals("new java.awt.Color(255,0,0)", e2
                .getJavaInitializationString());
        assertNull(e2.getTags());

        ExceptionPropertyChangeListener listener = new ExceptionPropertyChangeListener();
        e2.addPropertyChangeListener(listener);

        e2.setValue(null);
        assertEquals(Color.RED.getRed(), ((Color) e2.getValue()).getRed());

        try {
            e2.setValue(Color.yellow);
            fail("Should throw an error");
        } catch (MockError e) {
            // expected
        }

        assertEquals("255,255,0", e2.getAsText());

        try {
            e2.setAsText(null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            e2.setAsText("text");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        e2.removePropertyChangeListener(listener);
        e2.setAsText("255,255,255");
        assertEquals("java.awt.Color[r=255,g=255,b=255]", ((Color) e2
                .getValue()).toString());
    }
    
    public void testGetSetEditorPath() throws Exception{
      String[] s = new String[]{"path1", "path2"};
      PropertyEditorManager.setEditorSearchPath(s);
      s[1] = "path3";
      String[] s2 = PropertyEditorManager.getEditorSearchPath();
      assertFalse(s==s2);
      assertEquals("path1", s2[0]);
    }
    
    String[] defaultSearchPath;
    
    public void setUp(){
        defaultSearchPath = PropertyEditorManager.getEditorSearchPath();
    }
    
    public void tearDown(){
        PropertyEditorManager.setEditorSearchPath(defaultSearchPath);
    }
}
