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

import java.awt.List.AccessibleAWTList;
import java.awt.List.AccessibleAWTList.AccessibleAWTListChild;
import java.awt.event.FocusEvent;
import java.util.Locale;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import junit.framework.TestCase;

/**
 * AccessibleAWTListChildTest
 */
public class AccessibleAWTListChildTest extends TestCase {

    List list;
    AccessibleContext ac;
    AccessibleContext ac1, ac2, ac3;
    AccessibleComponent aComp1, aComp2, aComp3;
    protected FocusEvent lastFocusEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        list = new List();
        ac = list.getAccessibleContext();
        list.add("item1");
        list.add("item2");
        list.add("item3");
        ac1 = ac.getAccessibleChild(0).getAccessibleContext();
        aComp1 = ac1.getAccessibleComponent();
        ac2 = ac.getAccessibleChild(1).getAccessibleContext();
        aComp2 = ac2.getAccessibleComponent();
        ac3 = ac.getAccessibleChild(2).getAccessibleContext();
        aComp3 = ac3.getAccessibleComponent();
        lastFocusEvent = null;
    }

    public final void testGetAccessibleChildrenCount() {
        assertEquals(0, ac2.getAccessibleChildrenCount());
    }

    public final void testGetAccessibleIndexInParent() {
        assertEquals(0, ac1.getAccessibleIndexInParent());
        assertEquals(2, ac3.getAccessibleIndexInParent());
    }

    public final void testGetLocale() {
        Locale locale = Locale.GERMANY;
        list.setLocale(locale);
        assertSame(locale, ac2.getLocale());
        assertSame(list.getLocale(), ac1.getLocale());
    }

    public final void testGetAccessibleChild() {
        assertNull(ac1.getAccessibleChild(0));
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.LIST_ITEM, ac2.getAccessibleRole());
        assertSame(AccessibleRole.LIST_ITEM, ac3.getAccessibleRole());
    }

    public final void testGetAccessibleStateSet() {
        final AccessibleState SELECTED = AccessibleState.SELECTED;
        assertFalse(ac2.getAccessibleStateSet().contains(SELECTED));
        list.select(1);
        assertTrue(ac2.getAccessibleStateSet().contains(SELECTED));
        assertFalse(ac1.getAccessibleStateSet().contains(SELECTED));
        assertFalse(ac3.getAccessibleStateSet().contains(SELECTED));
    }

    public final void testAddFocusListener() {
        // does nothing?
    }

    public final void testContains() {
        list.setSize(100, 100);
        // always false(not implemented yet?)
        assertFalse(aComp1.contains(new Point(5,5)));
    }

    public final void testGetAccessibleAt() {
        list.setSize(100, 100);
        Point p = new Point(5, 5);
        // always null
        assertNull(aComp1.getAccessibleAt(p));
        assertNull(aComp3.getAccessibleAt(p));
    }

    public final void testGetBackground() {
        assertEquals(list.getBackground(), aComp1.getBackground());
        Color bkColor = Color.CYAN;
        list.setBackground(bkColor);
        assertEquals(bkColor, aComp1.getBackground());
        assertEquals(bkColor, aComp2.getBackground());
        assertEquals(bkColor, aComp3.getBackground());
    }

    public final void testGetBounds() {
        Rectangle r = new Rectangle();
        assertNull(aComp1.getBounds());
        r.setBounds(5, 6, 7, 8);
        list.setBounds(r);
        // always null(unimplemented yet)
        assertNull(aComp1.getBounds());
    }

    public final void testGetCursor() {
        Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        list.setCursor(cursor);
        assertSame(cursor, aComp1.getCursor());
        assertSame(cursor, aComp2.getCursor());
    }

    public final void testGetFont() {
        Font font = Font.decode("Arial");
        list.setFont(font);
        assertSame(font, aComp2.getFont());
    }

    public final void testGetFontMetrics() {
        Frame f = new Frame();
        f.add(list);
        f.addNotify();
        Font font = list.getFont();
        assertSame(list.getFontMetrics(font), aComp3.getFontMetrics(font));
        f.dispose();
    }

    public final void testGetForeground() {
        assertEquals(list.getForeground(), aComp1.getForeground());
        Color color = Color.BLUE;
        list.setForeground(color);
        assertEquals(color, aComp1.getForeground());
        assertEquals(color, aComp3.getForeground());
    }

    public final void testGetLocation() {
        assertNull(aComp1.getLocation());
        Point loc = new Point(50, 75);
        list.setLocation(loc);
        // always null(unimplemented yet)
        assertNull(aComp1.getLocation());
        assertNull(aComp2.getLocation());
    }

    @SuppressWarnings("deprecation")
    public final void testGetLocationOnScreen() {
        assertNull(aComp1.getLocationOnScreen());
        Frame f = new Frame();
        f.setLayout(null);
        f.add(list);
        Point p = new Point(50, 50);
        f.setLocation(p);
        f.show();
        // always null(unimplemented yet)
        assertNull(aComp2.getLocationOnScreen());
        f.dispose();
    }

    public final void testGetSize() {
        assertNull(aComp1.getSize());
        Dimension size = new Dimension(30, 40);
        list.setSize(size);
        // always null(unimplemented yet)
        assertNull(aComp2.getSize());
    }

    public final void testIsEnabled() {
        assertTrue(aComp1.isEnabled());
        list.setEnabled(false);
        assertFalse(aComp1.isEnabled());
        assertFalse(aComp2.isEnabled());
        assertFalse(aComp3.isEnabled());
        list.setEnabled(true);
        assertTrue(aComp2.isEnabled());
    }

    public final void testIsFocusTraversable() {
        assertFalse(aComp1.isFocusTraversable());
        assertFalse(aComp2.isFocusTraversable());
        assertFalse(aComp3.isFocusTraversable());
        list.setFocusable(true);
        // always false !
        assertFalse(aComp1.isFocusTraversable());
        assertFalse(aComp2.isFocusTraversable());
        assertFalse(aComp3.isFocusTraversable());
    }

    @SuppressWarnings("deprecation")
    public final void testIsShowing() {
        assertFalse(aComp3.isShowing());
        Frame f = new Frame();
        f.add(list);
        assertFalse(aComp2.isShowing());
        f.show();
        assertTrue(list.isShowing());
        // always false !
        assertFalse(aComp1.isShowing());
        assertFalse(aComp2.isShowing());
        assertFalse(aComp3.isShowing());
        f.dispose();

    }

    public final void testIsVisible() {
        assertTrue(list.isVisible());
        // always false
        assertFalse(aComp1.isVisible());
//        assertTrue(ac1.getAccessibleStateSet().contains(AccessibleState.VISIBLE));
        assertFalse(aComp2.isVisible());
        assertFalse(aComp3.isVisible());
    }

    public final void testRemoveFocusListener() {
        // does nothing?
    }

    public final void testRequestFocus() {
        // does nothing?
    }

    public final void testSetBackground() {
        Color color = Color.DARK_GRAY;
        assertFalse(list.isBackgroundSet());
        aComp3.setBackground(color);
        assertEquals(color, aComp1.getBackground());
        assertEquals(color, aComp2.getBackground());
        assertEquals(color, aComp3.getBackground());
        assertTrue("setBackground() is delegated to List",
                   list.isBackgroundSet());
        assertEquals(color, list.getBackground());
    }

    public final void testSetBounds() {
        Rectangle bounds = new Rectangle(1, 2, 3, 4);
        aComp2.setBounds(bounds); // does nothing
        assertNull(aComp2.getBounds());

    }

    public final void testSetCursor() {
        Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        assertFalse(list.isCursorSet());
        aComp1.setCursor(cursor);
        assertSame(cursor, aComp1.getCursor());
        assertSame(cursor, aComp3.getCursor());
        assertTrue("setCursor() is delegated to List",
                    list.isCursorSet());
        assertSame(cursor, list.getCursor());
    }

    public final void testSetEnabled() {
        assertTrue(list.isEnabled());
        aComp3.setEnabled(false);
        assertFalse(aComp3.isEnabled());
        assertFalse(aComp1.isEnabled());
        assertFalse("setEnabled() is delegated to List",
                   list.isEnabled());
    }

    public final void testSetFont() {
        Font font = Font.decode(null);
        assertFalse(list.isFontSet());
        aComp2.setFont(font);
        assertSame(font, aComp2.getFont());
        assertSame(font, aComp1.getFont());
        assertTrue("setFont() is delegated to List",
                    list.isFontSet());
        assertSame(font, list.getFont());
    }

    public final void testSetForeground() {
        Color color = Color.LIGHT_GRAY;
        assertFalse(list.isForegroundSet());
        aComp2.setForeground(color);
        assertEquals(color, aComp1.getForeground());
        assertEquals(color, aComp2.getForeground());
        assertEquals(color, aComp3.getForeground());
        assertTrue("setBackground() is delegated to List",
                   list.isForegroundSet());
        assertEquals(color, list.getForeground());
    }

    public final void testSetLocation() {
        Point location = new Point(1, 2);
        aComp2.setLocation(location); // does nothing
        assertNull(aComp2.getLocation());
    }

    public final void testSetSize() {
        Dimension size = new Dimension(3, 4);
        aComp3.setSize(size);// does nothing
        assertNull(aComp3.getSize());
    }

    public final void testSetVisible() {
        assertTrue(list.isVisible());
        aComp2.setVisible(false);
        assertFalse(aComp1.isVisible());
        assertFalse("setVisible() is delegated to Component",
                   list.isVisible());
        aComp1.setVisible(true);
        assertTrue(list.isVisible());
        assertFalse(aComp1.isVisible());
    }

    public final void testAccessibleAWTListChild() {
        AccessibleAWTList aal = (AccessibleAWTList) ac;
        AccessibleAWTListChild aalc = aal.new AccessibleAWTListChild(list, 1);
        assertEquals(1, aalc.getAccessibleIndexInParent());
        assertSame(list, aalc.getAccessibleParent());
        List list1 = new List();
        aalc = aal.new AccessibleAWTListChild(list1, 1);
        // check that operations are delegated to
        // parent directly, but not through calling super
        aalc.setEnabled(false);
        assertTrue(list.isEnabled());
        assertFalse(list1.isEnabled());
    }

    public final void testGetAccessibleContext() {
        assertNotNull(ac1);
        assertNotNull(ac2);
        assertNotNull(ac3);
    }

}
