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

import java.awt.Container.AccessibleAWTContainer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import junit.framework.TestCase;

/**
 * AccessibleAWTContainerTest
 */
@SuppressWarnings("serial")
public class AccessibleAWTContainerTest extends TestCase {

    private AccessibleAWTContainer aContainer;
    private Container container;
    private PropertyChangeListener propListener;
    private PropertyChangeEvent lastPropEvent;

    private class MyContainer extends Container implements Accessible {
        AccessibleAWTContainer aac;
        @Override
        public AccessibleContext getAccessibleContext() {
            if (aac == null) {
                aac = new AccessibleAWTContainer(){};
            }
            return aac;
        }

    }

    private class MyComponent extends Component implements Accessible {
        AccessibleAWTComponent aac;
        @Override
        public AccessibleContext getAccessibleContext() {
            if (aac == null) {
                aac = new AccessibleAWTComponent(){};
            }
            return aac;
        }

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AccessibleAWTContainerTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        container = new MyContainer();

        lastPropEvent = null;
        aContainer = (AccessibleAWTContainer) container.getAccessibleContext();
        propListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent pce) {
                lastPropEvent = pce;
            }

        };
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testGetAccessibleChildrenCount() {
        assertEquals("no accessible children", 0,
                     aContainer.getAccessibleChildrenCount());
        container.add(new Component(){});
        assertEquals("no accessible children", 0,
                     aContainer.getAccessibleChildrenCount());
        container.add(new MyComponent());
        assertEquals("1 accessible child", 1,
                     aContainer.getAccessibleChildrenCount());
        container.add(new MyContainer(), 0);
        assertEquals("2 accessible children", 2,
                     aContainer.getAccessibleChildrenCount());
    }

    public final void testAddPropertyChangeListener() {
        String propName = AccessibleContext.ACCESSIBLE_CHILD_PROPERTY;
        aContainer.addPropertyChangeListener(propListener);
        assertNotNull(aContainer.accessibleContainerHandler);
        assertSame(container.getContainerListeners()[0],
                   aContainer.accessibleContainerHandler);
        Component c = new MyComponent();
        container.add(c);
        assertNotNull(lastPropEvent);
        assertEquals(propName, lastPropEvent.getPropertyName());
        assertNull(lastPropEvent.getOldValue());
        assertSame(c.getAccessibleContext(), lastPropEvent.getNewValue());
        lastPropEvent = null;
        container.remove(c);
        assertNotNull(lastPropEvent);
        assertEquals(propName, lastPropEvent.getPropertyName());
        assertNull(lastPropEvent.getNewValue());
        assertSame(c.getAccessibleContext(), lastPropEvent.getOldValue());
        lastPropEvent = null;
        aContainer.removePropertyChangeListener(propListener);
        container.add(c);
        assertNull("listener not called", lastPropEvent);
    }

    public final void testGetAccessibleChild() {
        container.add(new Component(){});
        Component c = new MyComponent();
        container.add(c);
        assertNull(aContainer.getAccessibleChild(-1));
        assertSame(c, aContainer.getAccessibleChild(0));
        assertNull(aContainer.getAccessibleChild(1));
    }

    @SuppressWarnings("deprecation")
    public final void testGetAccessibleAt() {
        container.setSize(100, 100);

        Point p = null;
        assertSame(container, aContainer.getAccessibleAt(p));

        p = new Point();
        assertSame(container, aContainer.getAccessibleAt(p));

        Component c1 = new MyComponent();
        Component c2 = new MyComponent();
        Component c = new Component(){};
        Container cont1 = new MyContainer();
        Component c3 = new MyComponent();
        c3.setSize(13, 13);
        c.setSize(40, 40);
        c1.setBounds(3, 3, 10, 20);
        c2.setSize(20, 20);
        cont1.setBounds(27, 27, 60, 56);
        cont1.add(c3);
        container.add(c);
        container.add(c2);
        container.add(cont1);
        container.add(c1);


        p.setLocation(2, 2);
        assertSame(container, aContainer.getAccessibleAt(p));
        Frame f = new Frame();
        f.add(container);
        f.setSize(100, 300);
        f.show();
        
        try {
            assertSame(c2, aContainer.getAccessibleAt(p));
            p.translate(cont1.getX(), cont1.getY());

            assertSame(cont1, aContainer.getAccessibleAt(p));
            p = new Point();
            assertSame(c3, cont1.getAccessibleContext()
                    .getAccessibleComponent().getAccessibleAt(p));
        } finally {
            f.dispose();
        }
    }

    public final void testAccessibleAWTContainer() {
        assertNotNull(aContainer);
        assertNull(aContainer.accessibleContainerHandler);
    }

}
