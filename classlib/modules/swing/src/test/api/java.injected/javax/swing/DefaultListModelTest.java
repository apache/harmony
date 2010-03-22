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
 * @author Anton Avtamonov
 */
package javax.swing;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class DefaultListModelTest extends SwingTestCase {
    private DefaultListModel model;

    private TestListener listener;

    public DefaultListModelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        model = new DefaultListModel();
        listener = new TestListener();
        model.addListDataListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        model = null;
        listener = null;
    }

    public void testAddInsertElementAt() throws Exception {
        Object element1 = new Object();
        model.add(0, element1);
        assertEquals(element1, model.get(0));
        checkListDataEvent(ListDataEvent.INTERVAL_ADDED, 0, 0);
        listener.reset();
        Object element2 = new Object();
        model.insertElementAt(element2, 1);
        assertEquals(element2, model.get(1));
        checkListDataEvent(ListDataEvent.INTERVAL_ADDED, 1, 1);
        listener.reset();
        Object element3 = new Object();
        model.add(1, element3);
        assertEquals(element3, model.get(1));
        checkListDataEvent(ListDataEvent.INTERVAL_ADDED, 1, 1);
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.add(-1, "any");
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.insertElementAt("any", -1);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.add(4, "any");
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.insertElementAt("any", 4);
            }
        });
    }

    public void testAddElement() throws Exception {
        Object element1 = new Object();
        model.addElement(element1);
        checkListDataEvent(ListDataEvent.INTERVAL_ADDED, 0, 0);
        listener.reset();
        Object element2 = new Object();
        model.addElement(element2);
        checkListDataEvent(ListDataEvent.INTERVAL_ADDED, 1, 1);
        assertEquals(element1, model.get(0));
        assertEquals(element2, model.get(1));
    }

    public void testCapacity() throws Exception {
        assertEquals(new Vector<Object>().capacity(), model.capacity());
    }

    public void testClear() throws Exception {
        Object element1 = new Object();
        model.addElement(element1);
        Object element2 = new Object();
        model.addElement(element2);
        assertEquals(2, model.getSize());
        listener.reset();
        model.clear();
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 0, 1);
        assertEquals(0, model.getSize());
        listener.reset();
        model.clear();
        assertNull(listener.getEvent());
        assertEquals(0, model.getSize());
        model.addElement(element1);
        model.addElement(element2);
        assertEquals(2, model.getSize());
        listener.reset();
        model.removeAllElements();
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 0, 1);
        assertEquals(0, model.getSize());
    }

    public void testContains() throws Exception {
        Object element1 = new Object();
        model.addElement(element1);
        Object element2 = new Object();
        model.addElement(element2);
        assertTrue(model.contains(element1));
        assertTrue(model.contains(element2));
        assertFalse(model.contains("any"));
    }

    public void testCopyInfo() throws Exception {
        Object[] copied = new Object[2];
        model.copyInto(copied);
        assertNotNull(copied);
        assertNull(copied[0]);
        assertNull(copied[1]);
        Object element1 = new Object();
        model.addElement(element1);
        Object element2 = new Object();
        model.addElement(element2);
        model.copyInto(copied);
        assertEquals(element1, copied[0]);
        assertEquals(element2, copied[1]);
    }

    public void testElementAt() throws Exception {
        Object element1 = new Object();
        model.addElement(element1);
        Object element2 = new Object();
        model.addElement(element2);
        assertEquals(element1, model.elementAt(0));
        assertEquals(element2, model.elementAt(1));
    }

    public void testElements() throws Exception {
        assertFalse(model.elements().hasMoreElements());
        Object element = new Object();
        model.addElement(element);
        Enumeration<?> e = model.elements();
        assertTrue(e.hasMoreElements());
        assertEquals(element, e.nextElement());
        assertFalse(e.hasMoreElements());
    }

    public void testEnsureCapacity() throws Exception {
        model.ensureCapacity(500);
        assertEquals(500, model.capacity());
        model.ensureCapacity(1000);
        assertEquals(1000, model.capacity());
        model.ensureCapacity(800);
        assertEquals(1000, model.capacity());
    }

    public void testFirstElement() throws Exception {
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() {
                model.firstElement();
            }

            @Override
            public Class<?> expectedExceptionClass() {
                return NoSuchElementException.class;
            }
        });
        Object element1 = new Object();
        model.addElement(element1);
        Object element2 = new Object();
        model.addElement(element2);
        assertEquals(element1, model.firstElement());
        Object element0 = new Object();
        model.add(0, element0);
        assertEquals(element0, model.firstElement());
    }

    public void testGetGetElementAt() throws Exception {
        Object element1 = new Object();
        model.addElement(element1);
        Object element2 = new Object();
        model.addElement(element2);
        assertEquals(element1, model.get(0));
        assertEquals(element1, model.getElementAt(0));
        assertEquals(element2, model.get(1));
        assertEquals(element2, model.getElementAt(1));
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.get(-1);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.getElementAt(-1);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.get(2);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.getElementAt(2);
            }
        });
    }

    public void testSizeGetSize() throws Exception {
        assertEquals(0, model.size());
        assertEquals(0, model.getSize());
        model.addElement("any");
        assertEquals(1, model.size());
        assertEquals(1, model.getSize());
        model.addElement("any");
        assertEquals(2, model.size());
        assertEquals(2, model.getSize());
    }

    public void testIndexOf() throws Exception {
        assertEquals(-1, model.indexOf("1"));
        model.addElement("1");
        model.addElement("2");
        model.addElement("3");
        model.addElement("2");
        assertEquals(0, model.indexOf("1"));
        assertEquals(1, model.indexOf("2"));
        assertEquals(2, model.indexOf("3"));
        assertEquals(3, model.indexOf("2", 2));
        assertEquals(-1, model.indexOf("1", 2));
        assertEquals(-1, model.indexOf("1", 20));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() {
                model.indexOf("1", -1);
            }

            @Override
            public Class<?> expectedExceptionClass() {
                return IndexOutOfBoundsException.class;
            }
        });
    }

    public void testIsEmpty() throws Exception {
        assertTrue(model.isEmpty());
        model.addElement("1");
        assertFalse(model.isEmpty());
    }

    public void testLastElement() throws Exception {
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() {
                model.lastElement();
            }

            @Override
            public Class<?> expectedExceptionClass() {
                return NoSuchElementException.class;
            }
        });
        Object element1 = new Object();
        model.addElement(element1);
        Object element2 = new Object();
        model.addElement(element2);
        assertEquals(element2, model.lastElement());
        Object element3 = new Object();
        model.add(2, element3);
        assertEquals(element3, model.lastElement());
    }

    public void testLastIndexOf() throws Exception {
        assertEquals(-1, model.indexOf("1"));
        model.addElement("1");
        model.addElement("2");
        model.addElement("3");
        model.addElement("2");
        model.addElement("1");
        model.addElement("0");
        assertEquals(5, model.lastIndexOf("0"));
        assertEquals(4, model.lastIndexOf("1"));
        assertEquals(3, model.lastIndexOf("2"));
        assertEquals(2, model.lastIndexOf("3"));
        assertEquals(3, model.lastIndexOf("2", 3));
        assertEquals(-1, model.lastIndexOf("0", 1));
        assertEquals(-1, model.lastIndexOf("0", -1));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() {
                model.lastIndexOf("1", 100);
            }

            @Override
            public Class<?> expectedExceptionClass() {
                return IndexOutOfBoundsException.class;
            }
        });
    }

    public void testRemove() throws Exception {
        Object element = new Object();
        model.addElement(element);
        model.addElement(element);
        assertEquals(2, model.size());
        listener.reset();
        assertEquals(element, model.remove(0));
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 0, 0);
        assertEquals(1, model.size());
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.remove(4);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.remove(-1);
            }
        });
    }

    public void testRemoveElement() throws Exception {
        assertFalse(model.removeElement("1"));
        model.addElement("1");
        model.addElement("2");
        listener.reset();
        assertTrue(model.removeElement("2"));
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 1, 1);
        listener.reset();
        assertFalse(model.removeElement("2"));
        assertNull(listener.getEvent());
        assertEquals(1, model.size());
    }

    public void testRemoveElementAt() throws Exception {
        Object element = new Object();
        model.addElement(element);
        assertEquals(1, model.size());
        model.removeElementAt(0);
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 0, 0);
        assertEquals(0, model.size());
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.removeElementAt(4);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.removeElementAt(-1);
            }
        });
    }

    public void testRemoveRange() throws Exception {
        model.addElement("1");
        model.addElement("2");
        model.addElement("3");
        model.removeRange(1, 2);
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 1, 2);
        assertEquals(1, model.size());
        assertEquals("1", model.get(0));
        listener.reset();
        model.addElement("2");
        model.addElement("3");
        model.removeRange(0, 1);
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 0, 1);
        assertEquals(1, model.size());
        assertEquals("3", model.get(0));
        listener.reset();
        model.addElement("5");
        model.addElement("6");
        model.addElement("7");
        model.removeRange(0, 3);
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 0, 3);
        assertEquals(0, model.size());
        listener.reset();
        model.addElement("1");
        model.addElement("2");
        model.addElement("3");
        model.removeRange(2, 2);
        checkListDataEvent(ListDataEvent.INTERVAL_REMOVED, 2, 2);
        assertEquals(2, model.size());
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.removeRange(-1, 1);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.removeRange(1, 2);
            }
        });
    }

    public void testRemoveRange_1(){
        try {
            model.removeRange(0, -1);
            fail("IllegalArgumentException have to throw");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testSet() throws Exception {
        model.addElement("1");
        model.addElement("2");
        model.addElement("3");
        assertEquals("2", model.get(1));
        listener.reset();
        model.set(1, "another1");
        checkListDataEvent(ListDataEvent.CONTENTS_CHANGED, 1, 1);
        assertEquals("another1", model.get(1));
        listener.reset();
        model.setElementAt("another2", 2);
        checkListDataEvent(ListDataEvent.CONTENTS_CHANGED, 2, 2);
        assertEquals("another2", model.get(2));
        assertEquals(3, model.size());
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.set(-1, "any");
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.set(3, "any");
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.setElementAt("any", -1);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() {
                model.setElementAt("any", 3);
            }
        });
    }

    public void testSetSize() throws Exception {
        model.setSize(1000);
        assertEquals(1000, model.getSize());
        model.setSize(500);
        assertEquals(500, model.getSize());
    }

    public void testToArray() throws Exception {
        assertEquals(0, model.toArray().length);
        model.addElement("1");
        model.addElement("2");
        Object[] array = model.toArray();
        assertEquals(2, array.length);
        assertEquals("1", array[0]);
        assertEquals("2", array[1]);
    }

    public void testTrimToSize() throws Exception {
        model.ensureCapacity(1000);
        assertEquals(1000, model.capacity());
        model.addElement("1");
        model.trimToSize();
        assertEquals(1, model.capacity());
    }

    public void testToString() throws Exception {
        model.addElement("1");
        model.addElement("2");
        assertEquals("[1, 2]", model.toString());
    }

    private class TestListener implements ListDataListener {
        private ListDataEvent event;

        private int eventType = -1;

        public void contentsChanged(final ListDataEvent e) {
            event = e;
            eventType = ListDataEvent.CONTENTS_CHANGED;
        }

        public void intervalAdded(final ListDataEvent e) {
            event = e;
            eventType = ListDataEvent.INTERVAL_ADDED;
        }

        public void intervalRemoved(final ListDataEvent e) {
            event = e;
            eventType = ListDataEvent.INTERVAL_REMOVED;
        }

        public ListDataEvent getEvent() {
            return event;
        }

        public int getType() {
            return eventType;
        }

        public void reset() {
            event = null;
            eventType = -1;
        }
    }

    private void checkListDataEvent(final int expectedEventType, final int expectedIndex0,
            final int expectedIndex1) {
        assertNotNull(listener.getEvent());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(expectedEventType, listener.getEvent().getType());
        assertEquals(expectedIndex0, listener.getEvent().getIndex0());
        assertEquals(expectedIndex1, listener.getEvent().getIndex1());
    }

    private abstract class ArrayIndexOutOfBoundsCase extends ExceptionalCase {
        @Override
        public Class<?> expectedExceptionClass() {
            return ArrayIndexOutOfBoundsException.class;
        }
    }
}
