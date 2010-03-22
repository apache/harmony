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

package javax.accessibility;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import junit.framework.TestCase;

public class AccessibleContextTest extends TestCase {
    private AccessibleContext context;

    private PropertyChangeListenerImpl propertyChangeListener;

    @Override
    public void setUp() {
        context = new AccessibleContext() {
            @Override
            public AccessibleRole getAccessibleRole() {
                return null;
            }

            @Override
            public AccessibleStateSet getAccessibleStateSet() {
                return null;
            }

            @Override
            public int getAccessibleIndexInParent() {
                return 0;
            }

            @Override
            public int getAccessibleChildrenCount() {
                return 0;
            }

            @Override
            public Accessible getAccessibleChild(int i) {
                return null;
            }

            @Override
            public Locale getLocale() {
                return Locale.ENGLISH;
            }
        };
        propertyChangeListener = new PropertyChangeListenerImpl();
        context.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void tearDown() {
        context = null;
        propertyChangeListener.lastEvent = null;
    }

    public void testSetGetAccessibleName() {
        String name = "componentName";
        context.setAccessibleName(name);
        assertEquals(AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
                propertyChangeListener.lastEvent.getPropertyName());
        assertEquals(name, propertyChangeListener.lastEvent.getNewValue());
        assertEquals(name, context.getAccessibleName());
        assertEquals(name, context.accessibleName);
    }

    public void testSetGetAccessibleDescription() {
        String description = "componentDescription";
        context.setAccessibleDescription(description);
        assertEquals(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY,
                propertyChangeListener.lastEvent.getPropertyName());
        assertEquals(description, propertyChangeListener.lastEvent.getNewValue());
        assertEquals(description, context.getAccessibleDescription());
        assertEquals(description, context.accessibleDescription);
    }

    public void testSetGetAccessibleParent() {
        Accessible parent = new Accessible() {
            private String name = "parentName";

            public AccessibleContext getAccessibleContext() {
                return null;
            }

            @Override
            public String toString() {
                return name;
            }
        };
        context.setAccessibleParent(parent);
        assertEquals(parent, context.getAccessibleParent());
        assertEquals(parent, context.accessibleParent);
        assertNull(propertyChangeListener.lastEvent);
    }

    public void testGetAccessibleOthers() {
        assertNull(context.getAccessibleAction());
        assertNull(context.getAccessibleComponent());
        assertNull(context.getAccessibleSelection());
        assertNull(context.getAccessibleText());
        assertNull(context.getAccessibleEditableText());
        assertNull(context.getAccessibleValue());
        assertNull(context.getAccessibleIcon());
        assertSame(context.getAccessibleRelationSet(), context.getAccessibleRelationSet());
        assertNull(context.getAccessibleTable());
        assertNull(propertyChangeListener.lastEvent);
    }

    private static class PropertyChangeListenerImpl implements PropertyChangeListener {
        PropertyChangeEvent lastEvent;

        PropertyChangeListenerImpl() {
            super();
        }

        public void propertyChange(PropertyChangeEvent event) {
            this.lastEvent = event;
        }
    }
}
