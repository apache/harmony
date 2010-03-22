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

import javax.swing.JFormattedTextField;
import javax.swing.SwingTestCase;

public class DefaultFormatterFactoryTest extends SwingTestCase {
    class FTF extends JFormattedTextField {
        private static final long serialVersionUID = 1L;

        boolean hasFocus;

        public FTF(final Object value, final boolean hasFocus) {
            super(value);
            setHasFocus(hasFocus);
        }

        @Override
        public boolean hasFocus() {
            return hasFocus;
        }

        public void setHasFocus(final boolean hasFocus) {
            this.hasFocus = hasFocus;
        }
    }

    DefaultFormatterFactory factory;

    DefaultFormatter formatter;

    FTF ftf;

    DefaultFormatter defaultDormatter;

    DefaultFormatter editFormatter;

    DefaultFormatter displayFormatter;

    DefaultFormatter nullFormatter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        factory = new DefaultFormatterFactory();
        formatter = new DefaultFormatter();
    }

    private void checkFormatter(final JFormattedTextField.AbstractFormatter formatter) {
        assertEquals(formatter, factory.getFormatter(ftf));
    }

    private void init(final Object value, final boolean hasFocus) {
        ftf = new FTF(value, hasFocus);
        initFormatters();
    }

    private void initFormatters() {
        defaultDormatter = new DefaultFormatter();
        editFormatter = new DefaultFormatter();
        displayFormatter = new DefaultFormatter();
        nullFormatter = new DefaultFormatter();
    }

    public void testGetFormatterNull() {
        init(null, false);
        assertNull(factory.getFormatter(ftf));
        factory.setDefaultFormatter(defaultDormatter);
        checkFormatter(defaultDormatter);
        factory.setEditFormatter(editFormatter);
        checkFormatter(defaultDormatter);
        factory.setDisplayFormatter(displayFormatter);
        checkFormatter(displayFormatter);
        factory.setNullFormatter(nullFormatter);
        checkFormatter(nullFormatter);
    }

    public void testGetFormatterNullHasFocus() {
        init(null, true);
        assertNull(factory.getFormatter(ftf));
        factory.setDefaultFormatter(defaultDormatter);
        checkFormatter(defaultDormatter);
        factory.setDisplayFormatter(displayFormatter);
        checkFormatter(defaultDormatter);
        factory.setEditFormatter(editFormatter);
        checkFormatter(editFormatter);
        factory.setNullFormatter(nullFormatter);
        checkFormatter(nullFormatter);
    }

    public void testGetFormatterNotNull_Number() {
        init(new Integer(333), false);
        assertNull(factory.getFormatter(ftf));
        factory.setNullFormatter(nullFormatter);
        assertNull(factory.getFormatter(ftf));
        factory.setEditFormatter(editFormatter);
        assertNull(factory.getFormatter(ftf));
        factory.setDefaultFormatter(defaultDormatter);
        checkFormatter(defaultDormatter);
        factory.setDisplayFormatter(displayFormatter);
        checkFormatter(displayFormatter);
    }

    public void testGetFormatterNotNullHasFocus() {
        init(new Integer(333), true);
        assertNull(factory.getFormatter(ftf));
        factory.setNullFormatter(nullFormatter);
        assertNull(factory.getFormatter(ftf));
        factory.setDisplayFormatter(displayFormatter);
        assertNull(factory.getFormatter(ftf));
        factory.setDefaultFormatter(defaultDormatter);
        checkFormatter(defaultDormatter);
        factory.setEditFormatter(editFormatter);
        checkFormatter(editFormatter);
    }

    public void testDefaultFormatterFactory() {
        assertNull(factory.getNullFormatter());
        assertNull(factory.getEditFormatter());
        assertNull(factory.getDisplayFormatter());
        assertNull(factory.getDefaultFormatter());
    }

    public void testDefaultFormatterFactoryDefault() {
        factory = new DefaultFormatterFactory(formatter);
        assertNull(factory.getNullFormatter());
        assertNull(factory.getEditFormatter());
        assertNull(factory.getDisplayFormatter());
        assertEquals(formatter, factory.getDefaultFormatter());
    }

    public void testDefaultFormatterFactoryDefaultDisplay() {
        DefaultFormatter formatter1 = new DefaultFormatter();
        factory = new DefaultFormatterFactory(formatter, formatter1);
        assertNull(factory.getNullFormatter());
        assertNull(factory.getEditFormatter());
        assertEquals(formatter1, factory.getDisplayFormatter());
        assertEquals(formatter, factory.getDefaultFormatter());
    }

    public void testDefaultFormatterFactoryDefaultDisplayEdit() {
        DefaultFormatter formatter1 = new DefaultFormatter();
        DefaultFormatter formatter2 = new DefaultFormatter();
        factory = new DefaultFormatterFactory(formatter, formatter1, formatter2);
        assertNull(factory.getNullFormatter());
        assertEquals(formatter2, factory.getEditFormatter());
        assertEquals(formatter1, factory.getDisplayFormatter());
        assertEquals(formatter, factory.getDefaultFormatter());
    }

    public void testDefaultFormatterFactoryDefaultDisplayEditNull() {
        DefaultFormatter formatter1 = new DefaultFormatter();
        DefaultFormatter formatter2 = new DefaultFormatter();
        DefaultFormatter formatter3 = new DefaultFormatter();
        factory = new DefaultFormatterFactory(formatter, formatter1, formatter2, formatter3);
        assertEquals(formatter3, factory.getNullFormatter());
        assertEquals(formatter2, factory.getEditFormatter());
        assertEquals(formatter1, factory.getDisplayFormatter());
        assertEquals(formatter, factory.getDefaultFormatter());
    }

    public void testSetGetDefaultFormatter() {
        assertNull(factory.getDefaultFormatter());
        factory.setDefaultFormatter(formatter);
        assertEquals(formatter, factory.getDefaultFormatter());
    }

    public void testSetGetDisplayFormatter() {
        assertNull(factory.getDisplayFormatter());
        factory.setDisplayFormatter(formatter);
        assertEquals(formatter, factory.getDisplayFormatter());
    }

    public void testSetGetNullFormatter() {
        assertNull(factory.getNullFormatter());
        factory.setNullFormatter(formatter);
        assertEquals(formatter, factory.getNullFormatter());
    }

    public void testSetGetEditFormatter() {
        assertNull(factory.getEditFormatter());
        factory.setEditFormatter(formatter);
        assertEquals(formatter, factory.getEditFormatter());
    }
}
