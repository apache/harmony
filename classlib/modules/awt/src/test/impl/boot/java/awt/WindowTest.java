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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import junit.framework.TestCase;

public class WindowTest extends TestCase {
    Frame f;
    Window w;

    private boolean listenerCalled;
    private String propName;
    private Object oldValue;
    private Object newValue;
    private Object src;
    PropertyChangeListener propListener = new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent pce) {
            listenerCalled = true;
            propName = pce.getPropertyName();
            oldValue = pce.getOldValue();
            newValue = pce.getNewValue();
            src = pce.getSource();
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        f = new Frame("Window Test");
        w = new Window(f);
        cleanPropertyFields();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (w != null) {
            w.dispose();
            w = null;
        }
        if (f != null) {
            f.dispose();
            f = null;
        }
    }

    @SuppressWarnings("deprecation")
    public void testSetLocationRelativeTo() {
        Rectangle screenRect = f.getGraphicsConfiguration().getBounds();
        Point centerScreen = screenRect.getLocation();
        centerScreen.translate((screenRect.width - 1) / 2,
                               screenRect.height / 2);
        assertNotNull(f);
        assertNotNull(w);
        f.show();
        f.setLocationRelativeTo(null);
        Point center = f.getLocation();
        Dimension size = f.getSize();
        center.translate(size.width / 2, size.height / 2);
        assertEquals(centerScreen, center);
        f.setLocationRelativeTo(w);
        assertEquals(centerScreen, center);
        f.setBounds(0, 0, 200, 200);
        w.setSize(100, 100);
        w.setLocationRelativeTo(f);
        assertEquals(new Point(50, 50), w.getLocation());

    }

    @SuppressWarnings("deprecation")
    public final void testApplyResourceBundle() {
        assertNotNull(w);
        assertSame(ComponentOrientation.UNKNOWN, w.getComponentOrientation());
        w.applyResourceBundle( new ResourceBundle() {

            @Override
            public Enumeration<String> getKeys() {
                return null;
            }

            @Override
            protected Object handleGetObject(String arg0) {
                return null;
            }

            @Override
            public Locale getLocale(){
                return new Locale("ar");
            }

        });
        assertSame(ComponentOrientation.RIGHT_TO_LEFT,
                   w.getComponentOrientation());
    }

    @SuppressWarnings("deprecation")
    public final void testApplyResourceBundleString() {
        assertNotNull(w);
        assertSame(ComponentOrientation.UNKNOWN, w.getComponentOrientation());
        w.applyResourceBundle("java.awt.MyResourceBundle");
        assertSame(ComponentOrientation.RIGHT_TO_LEFT,
                   w.getComponentOrientation());
    }

    @SuppressWarnings("deprecation")
    public final void testSetGetCursorType() {
        assertNotNull(f);
        assertEquals(Frame.DEFAULT_CURSOR, f.getCursorType());
        int newCursor = Frame.CROSSHAIR_CURSOR;
        f.setCursor(newCursor);
        assertEquals(newCursor, f.getCursorType());
        newCursor = -1;
        boolean exception = false;
        try {
            f.setCursor(newCursor);
        }
        catch (IllegalArgumentException e) {
            exception = true;
        }
        assertTrue(exception);
    }

    private void cleanPropertyFields() {
        listenerCalled = false;
        propName = null;
        oldValue = newValue = src = null;
    }

    private void checkPropertyFields(String propName, Object source,
                                     Object newVal) {
        assertTrue(listenerCalled);
        assertEquals(propName, this.propName);
        assertSame(source, src);
        assertEquals(newVal, newValue);
        if (newVal != null) {
            assertFalse(newVal.equals(oldValue));
        }
    }

    public final void testAddPropertyChangeListener() {
        w.addPropertyChangeListener(propListener);
        w.setFocusableWindowState(false);
        checkPropertyFields("focusableWindowState", w, Boolean.FALSE);
        assertEquals(Boolean.TRUE, oldValue);
        cleanPropertyFields();
        w.setAlwaysOnTop(true);
        checkPropertyFields("alwaysOnTop", w, Boolean.TRUE);
        assertEquals(Boolean.FALSE, oldValue);
    }
    
    /*
     * Check if getFont() returns null for if font wasn't set before.
     */
    public void testGetFont_Default(){
        // regression test for Harmony-1605
        assertEquals(null, w.getFont());
    }

    public void testPack() {
        final Button b = new Button();

        assertNull(b.getFont());
        f.add(b);
        assertNull(b.getFont());
        assertFalse(b.isDisplayable());
        f.pack();
        assertTrue(f.isDisplayable());
        assertTrue(b.isDisplayable());
        assertNotNull(b.getFont());
        assertNotNull(f.getFont());
        f.dispose();
    }
}
