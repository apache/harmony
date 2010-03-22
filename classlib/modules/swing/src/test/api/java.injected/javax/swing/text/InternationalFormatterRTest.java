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

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BasicSwingTestCase;
import javax.swing.JFormattedTextField;
import javax.swing.SwingTestCase;

public class InternationalFormatterRTest extends SwingTestCase {
    InternationalFormatter formatter;

    boolean bWasException;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        formatter = new InternationalFormatter(new DecimalFormat());
        bWasException = false;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testStringToValue_Min() {
        formatter.setMinimum(new Integer(10));
        try {
            formatter.stringToValue("55");
        } catch (ParseException e) {
            bWasException = true;
        }
        assertFalse(bWasException);
    }

    public void testStringToValue_Value() {
        formatter.setValueClass(Integer.class);
        try {
            assertTrue(formatter.stringToValue("55") instanceof Integer);
        } catch (ParseException e) {
            bWasException = true;
        }
        assertFalse(bWasException);
    }

    public void testSetMin() {
        assertNull(formatter.getValueClass());
        formatter.setMinimum(new Integer(10));
        assertEquals(Integer.class, formatter.getValueClass());
    }

    public void testSetMax() {
        assertNull(formatter.getValueClass());
        formatter.setMaximum(new Integer(10));
        assertEquals(Integer.class, formatter.getValueClass());
    }

    public void testIncrementDecrement() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        JFormattedTextField ftf = new JFormattedTextField();
        ftf.setFormatterFactory(new DefaultFormatterFactory(new DateFormatter(
                new SimpleDateFormat("dd.MM.yyyy"))));
        ftf.setValue(new Date());
        ftf.setText("31.01.2006");
        ftf.setCaretPosition(0);
        TextAction action = new InternationalFormatter.IncrementAction("inc", 1);
        action.actionPerformed(new ActionEvent(ftf, 0, null));
        assertEquals("01.02.2006", ftf.getText());
        assertEquals(2, ftf.getCaretPosition());
        assertEquals("01", ftf.getSelectedText());
        action = new InternationalFormatter.IncrementAction("inc", -1);
        action.actionPerformed(new ActionEvent(ftf, 0, null));
        assertEquals("31.01.2006", ftf.getText());
        assertEquals(2, ftf.getCaretPosition());
        assertEquals("31", ftf.getSelectedText());
    }
}
