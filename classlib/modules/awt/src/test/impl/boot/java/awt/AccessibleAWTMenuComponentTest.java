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

import java.awt.MenuComponent.AccessibleAWTMenuComponent;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;

import junit.framework.TestCase;

/**
 * AccessibleAWTMenuComponentTest
 */
@SuppressWarnings("serial")
public class AccessibleAWTMenuComponentTest extends TestCase {

    class MyMenuComponent extends MenuComponent implements Accessible {
        AccessibleContext ac;
        @Override
        public AccessibleContext getAccessibleContext() {
            if (ac == null) {
                ac = new AccessibleAWTMenuComponent(){};
            }
            return ac;
        }
    }

    private MenuComponent menuComp;
    private AccessibleAWTMenuComponent aMenuComp;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuComp = new MyMenuComponent();
        aMenuComp = (AccessibleAWTMenuComponent) menuComp.getAccessibleContext();
//        aMenuComp = menuComp.new AccessibleAWTMenuComponent(){};
    }

    public final void testAccessibleAWTMenuComponent() {
        assertNotNull(aMenuComp);
        assertNull(new MenuComponent(){}.getAccessibleContext());
    }

    public final void testContains() {
        assertFalse(aMenuComp.contains(new Point()));
    }

    public final void testGetAccessibleAt() {
        assertNull(aMenuComp.getAccessibleAt(new Point()));
    }

    public final void testGetBackground() {
        assertNull(aMenuComp.getBackground());
    }

    public final void testGetBounds() {
        assertNull(aMenuComp.getBounds());
    }

    public final void testGetCursor() {
        assertNull(aMenuComp.getCursor());
    }

    public final void testGetFont() {
        assertNull(aMenuComp.getFont());
        Font font = Font.decode("Arial");
        menuComp.setFont(font);
        assertSame(font, aMenuComp.getFont());
    }

    public final void testGetFontMetrics() {
        Font font = Font.decode("Dialog");
        menuComp.setFont(font);
        assertSame(font, aMenuComp.getFont());
        assertNull(aMenuComp.getFontMetrics(font));


    }

    public final void testGetForeground() {
       assertNull(aMenuComp.getForeground());
    }

    public final void testGetLocation() {
        assertNull(aMenuComp.getLocation());
    }

    public final void testGetLocationOnScreen() {
        assertNull(aMenuComp.getLocationOnScreen());
    }

    public final void testGetSize() {
        assertNull(aMenuComp.getSize());
    }

    public final void testIsEnabled() {
        assertTrue(aMenuComp.isEnabled());
    }

    public final void testIsFocusTraversable() {
        assertTrue(aMenuComp.isFocusTraversable());
    }

    public final void testIsShowing() {
        assertTrue(aMenuComp.isShowing());
    }

    public final void testIsVisible() {
        assertTrue(aMenuComp.isVisible());
    }

    public final void testSetFont() {
        Font font = Font.decode(null);
        aMenuComp.setFont(font);
        assertSame(font, aMenuComp.getFont());
        assertSame("setFont() is delegated to MenuComponent",
                   font, menuComp.getFont());
    }

    public final void testAddAccessibleSelection() {
        int idx = 0;
        aMenuComp.addAccessibleSelection(idx); //does nothing
        assertEquals(0, aMenuComp.getAccessibleSelectionCount());
        assertNull(aMenuComp.getAccessibleSelection(idx));
    }

    public final void testClearAccessibleSelection() {
        aMenuComp.addAccessibleSelection(0);
        aMenuComp.addAccessibleSelection(1);
        assertEquals(0, aMenuComp.getAccessibleSelectionCount());
        aMenuComp.clearAccessibleSelection(); // does nothing
        assertEquals(0, aMenuComp.getAccessibleSelectionCount());
        assertNull(aMenuComp.getAccessibleSelection(0));
    }

    /*
     * Class under test for javax.accessibility.Accessible getAccessibleSelection(int)
     */
    public final void testGetAccessibleSelectionint() {
        assertNull(aMenuComp.getAccessibleSelection(0));
    }

    public final void testGetAccessibleSelectionCount() {
        assertEquals(0, aMenuComp.getAccessibleSelectionCount());
    }

    public final void testIsAccessibleChildSelected() {
        assertFalse(aMenuComp.isAccessibleChildSelected(0));
    }

    public final void testRemoveAccessibleSelection() {
        aMenuComp.addAccessibleSelection(0);
        aMenuComp.addAccessibleSelection(1);
        assertEquals(0, aMenuComp.getAccessibleSelectionCount());
        aMenuComp.removeAccessibleSelection(0); // does nothing
        assertEquals(0, aMenuComp.getAccessibleSelectionCount());
        assertNull(aMenuComp.getAccessibleSelection(0));
    }

    public final void testSelectAllAccessibleSelection() {
        assertEquals(0, aMenuComp.getAccessibleSelectionCount());
        aMenuComp.selectAllAccessibleSelection(); // does nothing
        assertEquals(0, aMenuComp.getAccessibleSelectionCount());
        assertNull(aMenuComp.getAccessibleSelection(0));
    }

    public final void testGetAccessibleChild() {
        assertNull(aMenuComp.getAccessibleChild(0));
    }

    public final void testGetAccessibleChildrenCount() {
        assertEquals(0, aMenuComp.getAccessibleChildrenCount());
    }

    public final void testGetAccessibleComponent() {
        assertSame(aMenuComp, aMenuComp.getAccessibleComponent());
    }

    public final void testGetAccessibleDescription() {
        assertNull(aMenuComp.getAccessibleDescription());
        String descr = "description";
        aMenuComp.setAccessibleDescription(descr);
        assertEquals(descr, aMenuComp.getAccessibleDescription());
    }

    public final void testGetAccessibleIndexInParent() {
        assertEquals(-1, aMenuComp.getAccessibleIndexInParent());
        Menu menu = new Menu();
        MenuItem item1 = new MenuItem("1");
        MenuItem item2 = new MenuItem("2");
        menu.add(item2);
        menu.add(item1);
        aMenuComp = item1.new AccessibleAWTMenuComponent(){};
        assertEquals(1, aMenuComp.getAccessibleIndexInParent());
    }

    public final void testGetAccessibleName() {
        String name = "name";
        String aName = "accessible name";
        menuComp.setName(name);
        assertNull(aMenuComp.getAccessibleName());
        aMenuComp.setAccessibleName(aName);
        assertEquals(aName, aMenuComp.getAccessibleName());

    }

    public final void testGetAccessibleParent() {
        assertNull(aMenuComp.getAccessibleParent());
        MenuBar menubar = new MenuBar();
        Menu menu = new Menu("1");
        menubar.add(menu);
        aMenuComp = menu.new AccessibleAWTMenuComponent(){};
        assertSame(menubar, aMenuComp.getAccessibleParent());
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.AWT_COMPONENT, aMenuComp.getAccessibleRole());
    }

    /*
     * Class under test for javax.accessibility.AccessibleSelection getAccessibleSelection()
     */
    public final void testGetAccessibleSelection() {
        assertSame(aMenuComp, aMenuComp.getAccessibleSelection());
    }

    public final void testGetAccessibleStateSet() {
        AccessibleStateSet set = aMenuComp.getAccessibleStateSet();
        assertNotNull(set);
        assertEquals("accessible state set is empty", 0, set.toArray().length);
    }

    public final void testGetLocale() {
        Locale locale = Locale.getDefault();
        assertSame(locale, aMenuComp.getLocale());
        Locale.setDefault(locale = Locale.TAIWAN);
        assertSame(locale, aMenuComp.getLocale());
    }

}
