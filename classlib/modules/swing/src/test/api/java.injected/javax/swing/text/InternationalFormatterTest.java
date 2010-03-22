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

import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.JFormattedTextField;
import javax.swing.SwingTestCase;

public class InternationalFormatterTest extends SwingTestCase {
    InternationalFormatter formatter;

    JFormattedTextField ftf;

    boolean bWasException;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        formatter = new InternationalFormatter() {
            private static final long serialVersionUID = 1L;
            //            boolean getSupportsIncrement() {
            //                return super.getStrue;
            //            }
        };
        ftf = new JFormattedTextField();
        bWasException = false;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void checkException() {
        assertTrue(bWasException);
        bWasException = false;
    }

    private void checkMainProperties() {
        assertTrue(formatter.getAllowsInvalid());
        assertFalse(formatter.getCommitsOnValidEdit());
        assertFalse(formatter.getOverwriteMode());
        //assertNull(formatter.getActions());
        //System.out.println(formatter.getActions()[1].getValue(Action.NAME));
        assertNull(formatter.getMaximum());
        assertNull(formatter.getMinimum());
    }

    public void testInternationalFormatter() {
        assertNull(formatter.getFormat());
        checkMainProperties();
    }

    public void testInternationalFormatterFormat() {
        Format format = NumberFormat.getNumberInstance();
        formatter = new InternationalFormatter(format);
        assertEquals(format, formatter.getFormat());
        checkMainProperties();
    }

    public void testClone() {
        Object clone = null;
        formatter.install(ftf);
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);
        formatter.setOverwriteMode(true);
        Format format = NumberFormat.getCurrencyInstance();
        formatter.setFormat(format);
        Comparable<?> max = new Integer(23);
        Comparable<?> min = new Integer(24);
        formatter.setMaximum(max);
        formatter.setMinimum(min);
        try {
            clone = formatter.clone();
        } catch (CloneNotSupportedException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        assertTrue(clone instanceof InternationalFormatter);
        InternationalFormatter form = (InternationalFormatter) clone;
        assertFalse(form.getAllowsInvalid());
        assertTrue(form.getCommitsOnValidEdit());
        assertTrue(form.getOverwriteMode());
        assertEquals(format, form.getFormat());
        assertEquals(max, form.getMaximum());
        assertEquals(min, form.getMinimum());
    }

    public void testGetActions() {
    }

    public void testStringToValue() {
        assertNull(formatter.getFormat());
        assertNull(formatter.getValueClass());
        try {
            assertNull(formatter.stringToValue(null));
            assertEquals("234", formatter.stringToValue("234"));
            formatter.setFormat(NumberFormat.getNumberInstance());
            assertEquals(new Long(723), formatter.stringToValue("723"));
            formatter.setMaximum(new Long(23)); //if Integer
            formatter.setMinimum(new Long(20));
            assertEquals(new Long(21), formatter.stringToValue("21"));
        } catch (ParseException e) {
            assertTrue("Unexpected exception: ", false);
        }
        try {
            formatter.stringToValue("-27");
        } catch (ParseException e) {
            bWasException = true;
        }
        checkException();
        try {
            formatter.stringToValue("abc12");
        } catch (ParseException e) {
            bWasException = true;
        }
        checkException();
        try {
            formatter.stringToValue("true");
        } catch (ParseException e) {
            bWasException = true;
        }
        checkException();
        try {
            formatter.setFormat(null);
            formatter.setValueClass(Boolean.class);
            formatter.setMaximum(null);
            formatter.setMinimum(null);
            assertEquals(Boolean.TRUE, formatter.stringToValue("true"));
        } catch (ParseException e) {
            assertTrue("Unexpected exception: ", false);
        }
    }

    public void testValueToString() {
        try {
            assertEquals("", formatter.valueToString(null));
            Object value;
            assertNull(formatter.getFormat());
            assertEquals("234", formatter.valueToString(new Integer(234)));
            value = new DefaultCaret();
            assertEquals(value.toString(), formatter.valueToString(value));
            Format format = NumberFormat.getPercentInstance();
            formatter.setFormat(format);
            value = new Integer(456);
            assertEquals(format.format(value), formatter.valueToString(value));
            format = NumberFormat.getCurrencyInstance();
            formatter.setFormat(format);
            value = new Integer(345);
            assertEquals(format.format(value), formatter.valueToString(value));
        } catch (ParseException e) {
            assertTrue("Unexpected exception: ", false);
        }
    }

    public void testGetFields() {
        ftf.setValue(new Integer(345));
        formatter = (InternationalFormatter) ftf.getFormatter();
        Format format = formatter.getFormat();
        Format.Field[] fields = formatter.getFields(0);
        assertEquals(1, fields.length);
        AttributedCharacterIterator iter = format.formatToCharacterIterator(new Integer(345));
        assertTrue(iter.getAttributes().containsKey(fields[0]));
        assertEquals(0, formatter.getFields(-7).length);
        //TODO
        //formatter.setFormat(null);
        //assertEquals(0, formatter.getFields(0).length);
    }

    public void testSetGetFormat() {
        Format format = NumberFormat.getCurrencyInstance();
        formatter.setFormat(format);
        assertEquals(format, formatter.getFormat());
    }

    public void testSetGetMaximum() {
        Integer max = new Integer(35);
        Integer min = new Integer(40);
        formatter.setMaximum(max);
        assertEquals(max, formatter.getMaximum());
        formatter.setMinimum(min);
        assertEquals(min, formatter.getMinimum());
    }

    public void testSetGetMinimum() {
        Integer max = new Integer(10);
        Integer min = new Integer(20);
        formatter.setMinimum(min);
        assertEquals(min, formatter.getMinimum());
        formatter.setMaximum(max);
        assertEquals(max, formatter.getMaximum());
    }
}
