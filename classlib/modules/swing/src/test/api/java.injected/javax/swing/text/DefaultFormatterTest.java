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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text;

import java.awt.Color;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.JFormattedTextField;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import junit.framework.AssertionFailedError;

public class DefaultFormatterTest extends SwingTestCase {
    DefaultFormatter formatter;

    JFormattedTextField ftf;

    boolean bWasException;

    String message;

    AssertionFailedError assertion;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        formatter = new DefaultFormatter();
        ftf = new JFormattedTextField();
        bWasException = false;
        message = null;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testClone() {
        Object clone = null;
        formatter.install(ftf);
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);
        formatter.setOverwriteMode(false);
        try {
            clone = formatter.clone();
        } catch (CloneNotSupportedException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        assertTrue(clone instanceof DefaultFormatter);
        DefaultFormatter form = (DefaultFormatter) clone;
        assertEquals(Integer.class, form.getValueClass());
        assertFalse(form.getAllowsInvalid());
        assertTrue(form.getCommitsOnValidEdit());
        assertFalse(form.getOverwriteMode());
    }

    public void testGetDocumentFilter() {
        assertNotNull(formatter.getDocumentFilter());
    }

    public void testValueToString() {
        Object value = Color.RED;
        try {
            assertEquals(value.toString(), formatter.valueToString(value));
            value = "just value";
            assertEquals(value.toString(), formatter.valueToString(value));
            value = new Integer(123);
            assertEquals(value.toString(), formatter.valueToString(value));
        } catch (ParseException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
    }

    public void testDefaultFormatter() {
        assertNull(formatter.getValueClass());
        assertTrue(formatter.getAllowsInvalid());
        assertFalse(formatter.getCommitsOnValidEdit());
        assertTrue(formatter.getOverwriteMode());
    }

    public void testSetGetAllowsInvalid_NumberFormatter() {
        String text = "444555666";
        ftf.setValue(new Integer(334580));
        NumberFormatter numFormatter = (NumberFormatter) ftf.getFormatter();
        ftf.setText(text);
        assertEquals(text, ftf.getText());
        text = "111222333";
        numFormatter.setAllowsInvalid(false);
        ftf.setText(text);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    assertEquals(NumberFormat.getNumberInstance()
                            .format(new Integer(111222333)), ftf.getText());
                } catch (AssertionFailedError e) {
                    assertion = e;
                }
            }
        });
        if (assertion != null) {
            throw assertion;
        }
    }

    public void testSetGetCommitsOnValidEdit_NumberFormatter() {
        ftf.setValue(new Integer(334580));
        formatter = (DefaultFormatter) ftf.getFormatter();
        ftf.setText("567");
        assertEquals(new Integer(334580), ftf.getValue());
        formatter.setCommitsOnValidEdit(true);
        assertTrue(formatter.getCommitsOnValidEdit());
        ftf.setText("123456");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    assertEquals(new Long(123456), ftf.getValue());
                } catch (AssertionFailedError e) {
                    assertion = e;
                }
            }
        });
        if (assertion != null) {
            throw assertion;
        }
    }

    public void testSetGetCommitsOnValidEdit() {
        ftf.setValue(Boolean.TRUE);
        formatter = (DefaultFormatter) ftf.getFormatter();
        ftf.setText("false");
        assertEquals(Boolean.TRUE, ftf.getValue());
        formatter.setCommitsOnValidEdit(true);
        assertTrue(formatter.getCommitsOnValidEdit());
        ftf.setText("false");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    assertEquals(Boolean.FALSE, ftf.getValue());
                } catch (AssertionFailedError e) {
                    assertion = e;
                }
            }
        });
        if (assertion != null) {
            throw assertion;
        }
    }

    public void testSetGetOverwriteMode_NumberFormatter() {
        ftf.setValue(new Integer(334580));
        NumberFormatter numFormatter = (NumberFormatter) ftf.getFormatter();
        try {
            AbstractDocument doc = (AbstractDocument) ftf.getDocument();
            ftf.setText("00");
            doc.insertString(0, "123", null);
            assertEquals("12300", ftf.getText());
            doc.insertString(0, "456", null);
            assertEquals("45612300", ftf.getText());
            numFormatter.setOverwriteMode(true);
            doc.insertString(0, "789", null);
            assertEquals("78912300", ftf.getText());
            doc.insertString(3, "xxx", null);
            assertEquals("789xxx00", ftf.getText());
            assertEquals(numFormatter, ftf.getFormatter());
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
    }

    public void testSetGetOverwriteMode() {
        ftf.setValue(Boolean.FALSE);
        DefaultFormatter formatter = (DefaultFormatter) ftf.getFormatter();
        try {
            AbstractDocument doc = (AbstractDocument) ftf.getDocument();
            ftf.setText("00");
            doc.insertString(0, "123", null);
            assertEquals("123", ftf.getText());
            doc.insertString(0, "456", null);
            assertEquals("456", ftf.getText());
            formatter.setOverwriteMode(false);
            doc.insertString(0, "789", null);
            assertEquals("789456", ftf.getText());
            doc.insertString(3, "xxx", null);
            assertEquals("789xxx456", ftf.getText());
            assertEquals(formatter, ftf.getFormatter());
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
    }

    public void testStringToValue() {
        formatter.install(ftf);
        assertNull(ftf.getValue());
        assertNull(formatter.getValueClass());
        try {
            assertEquals("java.lang.String", formatter.stringToValue("546").getClass()
                    .getName());
            ftf.setValue(Boolean.TRUE);
            assertEquals("java.lang.Boolean", formatter.stringToValue("true").getClass()
                    .getName());
            formatter.setValueClass(Float.class);
            assertEquals("java.lang.Float", formatter.stringToValue("546").getClass().getName());
            formatter.setValueClass(Rectangle.class);
            assertEquals("java.lang.String", formatter.stringToValue("546").getClass()
                    .getName());
        } catch (ParseException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
        try {
            formatter.setValueClass(Integer.class);
            formatter.stringToValue("ttt");
        } catch (ParseException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Error creating instance", message);
    }

    public void testSetGetValueClass() {
        assertNull(formatter.getValueClass());
        formatter.setValueClass(Rectangle.class);
        assertEquals(Rectangle.class, formatter.getValueClass());
    }
}
