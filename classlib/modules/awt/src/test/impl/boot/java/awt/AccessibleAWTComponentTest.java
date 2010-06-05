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

import java.awt.Component.AccessibleAWTComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import junit.framework.TestCase;

/**
 * AccessibleAWTComponentTest
 */
@SuppressWarnings("serial")
public class AccessibleAWTComponentTest extends TestCase {

    private class MyComponent extends Component implements Accessible {
        @Override
        public void requestFocus() {
            methodCalled = true;
            super.requestFocus();
        }
    }

    private AccessibleAWTComponent aComponent;
    private Component comp;
    private PropertyChangeListener propListener;
    private FocusListener focusListener;
    private PropertyChangeEvent lastPropEvent;
    private FocusEvent lastFocusEvent;
    protected boolean methodCalled;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        comp = new MyComponent();
        assertNull("accessible context is null", comp.getAccessibleContext());
        lastPropEvent = null;
        lastFocusEvent = null;
        methodCalled = false;
        aComponent = comp.new AccessibleAWTComponent(){};
        propListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent pce) {
                lastPropEvent = pce;
            }

        };
        focusListener = new FocusListener() {

            public void focusGained(FocusEvent fe) {
                lastFocusEvent = fe;
            }

            public void focusLost(FocusEvent fe) {
                lastFocusEvent = fe;
            }

        };
    }

    public final void testGetAccessibleChildrenCount() {
        assertEquals("No accessible children", 0,
                     aComponent.getAccessibleChildrenCount());
    }

    public final void testGetAccessibleIndexInParent() {
        assertEquals(-1, aComponent.getAccessibleIndexInParent());
        Container parent = new Panel(); // parent must be accessible
        parent.add(new Component(){});
        parent.add(new MyComponent()); // child must also be accessible
        parent.add(comp);
        assertEquals(1, aComponent.getAccessibleIndexInParent());
        parent = new Container() {};
        parent.add(comp);
        assertEquals(-1, aComponent.getAccessibleIndexInParent());

    }

    public final void testAddRemovePropertyChangeListener() {
        aComponent.addPropertyChangeListener(propListener);
        assertEquals(0, comp.getPropertyChangeListeners().length);
        assertEquals(0, comp.getListeners(PropertyChangeListener.class).length);
        // handlers are created only if property change listener is present
        assertNotNull(aComponent.accessibleAWTComponentHandler);
        assertNotNull(aComponent.accessibleAWTFocusHandler);
        // they're also added to component's listeners
        assertSame(aComponent.accessibleAWTFocusHandler,
                   comp.getFocusListeners()[0]);
        assertSame(aComponent.accessibleAWTComponentHandler,
                   comp.getComponentListeners()[0]);
        assertNull(lastPropEvent);
        String name = "component name";
        aComponent.setAccessibleName(name);
        assertNotNull("property listener called", lastPropEvent);
        assertEquals("property name is correct",
                     AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
                     lastPropEvent.getPropertyName());
        assertEquals(name, lastPropEvent.getNewValue());
        assertNull(lastPropEvent.getOldValue());
        // verify handlers:

        aComponent.removePropertyChangeListener(propListener);
        // handlers & listeners are removed, if last property listener is removed
        assertNull(aComponent.accessibleAWTComponentHandler);
        assertNull(aComponent.accessibleAWTFocusHandler);
        assertEquals(0, comp.getFocusListeners().length);
        assertEquals(0, comp.getComponentListeners().length);
        lastPropEvent = null;
        aComponent.setAccessibleName(name = "");
        assertNull(lastPropEvent);

    }

    public final void testGetLocale() {
        new Frame().add(comp);
        assertEquals(comp.getLocale(), aComponent.getLocale());
        comp.setLocale(Locale.FRANCE);
        assertEquals("component's locale is used" , comp.getLocale(),
                     aComponent.getLocale());
    }

    public final void testGetAccessibleParent() {
        assertNull(aComponent.getAccessibleParent());
        Container parent = new Panel(); // accessible parent
        parent.add(comp);
        assertSame(parent, aComponent.getAccessibleParent());
        parent.remove(comp); // no parent
        assertNull(aComponent.getAccessibleParent());
        parent = new Container() {}; // non-accessible parent
        parent.add(comp);
        assertNull(aComponent.getAccessibleParent());
        Accessible aParent = new Panel();
        aComponent.setAccessibleParent(aParent);
        assertSame(aParent, aComponent.getAccessibleParent());
        new Panel().add(comp);
        assertSame(aParent, aComponent.getAccessibleParent());
    }

    public final void testGetAccessibleChild() {
        assertNull(aComponent.getAccessibleChild(-5));
        assertNull(aComponent.getAccessibleChild(0));
        assertNull(aComponent.getAccessibleChild(100));
    }

    public final void testGetAccessibleComponent() {
        assertSame(aComponent, aComponent.getAccessibleComponent());
    }

    public final void testGetAccessibleDescription() {
        assertNull(aComponent.getAccessibleDescription());
        aComponent.setAccessibleDescription("q");
        assertEquals("q", aComponent.getAccessibleDescription());
    }

    public final void testGetAccessibleRole() {
        assertSame("accessible role is correct", AccessibleRole.AWT_COMPONENT,
                   aComponent.getAccessibleRole());
    }

    public final void testGetAccessibleStateSet() {
        AccessibleStateSet ass = aComponent.getAccessibleStateSet();
        assertNotNull(ass);
        assertFalse(ass.contains(AccessibleState.ACTIVE));
        assertTrue(ass.contains(AccessibleState.ENABLED));
        assertTrue(ass.contains(AccessibleState.FOCUSABLE));
        assertTrue(ass.contains(AccessibleState.VISIBLE));
        assertFalse(ass.contains(AccessibleState.FOCUSED));
        assertFalse(ass.contains(AccessibleState.OPAQUE));
        assertFalse(ass.contains(AccessibleState.SHOWING));
    }

    public final void testAccessibleAWTComponent() {
        assertNotNull(aComponent);
        assertNull(aComponent.accessibleAWTComponentHandler);
        assertNull(aComponent.accessibleAWTFocusHandler);
    }

    public final void testAddRemoveFocusListener() {
        assertEquals(0, comp.getFocusListeners().length);
        aComponent.addFocusListener(focusListener);
        FocusListener[] listeners = comp.getFocusListeners();
        assertEquals(1, listeners.length);
        assertSame(focusListener, listeners[0]);
        assertNull(lastFocusEvent);
        comp.processEvent(new FocusEvent(comp, FocusEvent.FOCUS_GAINED));
        assertNotNull("focus listener called", lastFocusEvent);
        lastFocusEvent = null;
        aComponent.removeFocusListener(null);
        listeners = comp.getFocusListeners();
        assertSame(focusListener, listeners[0]);
        comp.processEvent(new FocusEvent(comp, FocusEvent.FOCUS_LOST));
        assertNotNull("focus listener called", lastFocusEvent);
        lastFocusEvent = null;
        aComponent.removeFocusListener(focusListener);
        listeners = comp.getFocusListeners();
        comp.processEvent(new FocusEvent(comp, FocusEvent.FOCUS_LOST, true));
        assertEquals(0, listeners.length);
        assertNull("listener not called", lastFocusEvent);

    }

    public final void testContains() {
        assertFalse(aComponent.contains(new Point()));
        comp.setSize(15, 20);
        assertTrue(aComponent.contains(new Point(5, 19)));
        assertFalse(aComponent.contains(new Point(5, 20)));
        assertFalse(aComponent.contains(new Point(0, -5)));
    }

    public final void testGetAccessibleAt() {
        comp.setSize(15, 20);
        Point p = new Point(5, 5);
        assertNull(aComponent.getAccessibleAt(p));
    }

    public final void testGetBackground() {
        assertEquals(comp.getBackground(), aComponent.getBackground());
        comp.setBackground(Color.CYAN);
        assertEquals(comp.getBackground(), aComponent.getBackground());
    }

    public final void testGetBounds() {
        Rectangle r = new Rectangle();
        assertEquals(r, aComponent.getBounds());
        r.setBounds(5, 6, 7, 8);
        comp.setBounds(r);
        assertEquals(r, aComponent.getBounds());
        assertNotSame(r, aComponent.getBounds());
    }

    public final void testGetCursor() {
        Cursor cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        comp.setCursor(cursor);
        assertEquals(cursor, aComponent.getCursor());
    }

    public final void testGetFont() {
        Font font = Font.decode("Arial");
        comp.setFont(font);
        assertEquals(font, aComponent.getFont());
    }

    public final void testGetFontMetrics() {
        Frame f = new Frame();
        f.add(comp);
        f.addNotify();
        Font font = comp.getFont();
        assertSame(comp.getFontMetrics(font), aComponent.getFontMetrics(font));
        f.dispose();
    }

    public final void testGetForeground() {
        assertEquals(comp.getForeground(), aComponent.getForeground());
        comp.setForeground(Color.BLUE);
        assertEquals(comp.getForeground(), aComponent.getForeground());
    }

    public final void testGetLocation() {
        assertEquals(comp.getLocation(), aComponent.getLocation());
        Point loc = new Point(50, 75);
        comp.setLocation(loc);
        assertEquals(loc, aComponent.getLocation());
        assertNotSame(loc, aComponent.getLocation());
    }

    @SuppressWarnings("deprecation")
    public final void testGetLocationOnScreen() {
        Frame f = new Frame();
        f.setLayout(null);
        f.add(comp);
        Point p = new Point(50, 50);
        f.setLocation(p);
        f.show();
        assertEquals(p, aComponent.getLocationOnScreen());
        f.dispose();
    }

    public final void testGetSize() {
        assertEquals(comp.getSize(), aComponent.getSize());
        Dimension size = new Dimension(30, 40);
        comp.setSize(size);
        assertEquals(size, aComponent.getSize());
        assertNotSame(size, aComponent.getSize());
    }

    public final void testIsEnabled() {
        assertTrue(aComponent.isEnabled());
        comp.setEnabled(false);
        assertFalse(aComponent.isEnabled());
        comp.setEnabled(true);
        assertTrue(aComponent.isEnabled());
    }

    public final void testIsFocusTraversable() {
        assertTrue(aComponent.isFocusTraversable());
        comp.setFocusable(false);
        assertFalse(aComponent.isFocusTraversable());
        comp.setFocusable(true);
        assertTrue(aComponent.isFocusTraversable());
    }

    @SuppressWarnings("deprecation")
    public final void testIsShowing() {
        assertFalse(aComponent.isShowing());
        Frame f = new Frame();
        f.add(comp);
        assertFalse(aComponent.isShowing());
        f.show();
        assertTrue(aComponent.isShowing());
        f.dispose();
        assertFalse(aComponent.isShowing());
    }

    public final void testIsVisible() {
        assertTrue(aComponent.isVisible());
        comp.setVisible(false);
        assertFalse(aComponent.isVisible());
        comp.setVisible(true);
        assertTrue(aComponent.isVisible());
    }

    public final void testRequestFocus() {
        aComponent.requestFocus();
        assertTrue("requestFocus() is delegated to Component", methodCalled);
    }

    public final void testSetBackground() {
        Color color = Color.DARK_GRAY;
        assertFalse(comp.isBackgroundSet());
        aComponent.setBackground(color);
        assertEquals(color, aComponent.getBackground());
        assertTrue("setBackground() is delegated to Component",
                   comp.isBackgroundSet());
        assertEquals(color, comp.getBackground());
    }

    public final void testSetBounds() {
        Rectangle bounds = new Rectangle(1, 2, 3, 4);
        assertEquals(new Rectangle(), comp.getBounds());
        aComponent.setBounds(bounds);
        assertEquals(bounds, aComponent.getBounds());
        assertEquals("setBounds() is delegated to Component",
                     bounds, comp.getBounds());
    }

    public final void testSetCursor() {
        Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        assertFalse(comp.isCursorSet());
        aComponent.setCursor(cursor);
        assertSame(cursor, aComponent.getCursor());
        assertTrue("setCursor() is delegated to Component",
                    comp.isCursorSet());
        assertSame(cursor, comp.getCursor());
    }

    public final void testSetEnabled() {
        assertTrue(comp.isEnabled());
        aComponent.setEnabled(false);
        assertFalse(aComponent.isEnabled());
        assertFalse("setEnabled() is delegated to Component",
                   comp.isEnabled());
    }

    public final void testSetFont() {
        Font font = Font.decode(null);
        assertFalse(comp.isFontSet());
        aComponent.setFont(font);
        assertSame(font, aComponent.getFont());
        assertTrue("setFont() is delegated to Component",
                    comp.isFontSet());
        assertSame(font, comp.getFont());
    }

    public final void testSetForeground() {
        Color color = Color.GREEN;
        assertFalse(comp.isForegroundSet());
        aComponent.setForeground(color);
        assertEquals(color, aComponent.getForeground());
        assertTrue("setForeground() is delegated to Component",
                   comp.isForegroundSet());
        assertEquals(color, comp.getForeground());
    }

    public final void testSetLocation() {
        Point location = new Point(1, 2);
        assertEquals(new Point(), comp.getLocation());
        aComponent.setLocation(location);
        assertEquals(location, aComponent.getLocation());
        assertEquals("setLocation() is delegated to Component",
                     location, comp.getLocation());
    }

    public final void testSetSize() {
        Dimension size = new Dimension(3, 4);
        assertEquals(new Dimension(), comp.getSize());
        aComponent.setSize(size);
        assertEquals(size, aComponent.getSize());
        assertEquals("setSize() is delegated to Component",
                     size, comp.getSize());
    }

    public final void testSetVisible() {
        assertTrue(comp.isVisible());
        aComponent.setVisible(false);
        assertFalse(aComponent.isVisible());
        assertFalse("setVisible() is delegated to Component",
                   comp.isVisible());
    }

}
