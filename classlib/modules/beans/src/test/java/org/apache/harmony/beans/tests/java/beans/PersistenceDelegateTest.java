/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.beans.tests.java.beans;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.awt.dnd.DropTarget;
import java.beans.*;
import java.beans.beancontext.BeanContextSupport;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI.ListDataHandler;
import javax.swing.tree.DefaultMutableTreeNode;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.mock.MockFoo;
import org.apache.harmony.beans.tests.support.mock.MockFooStop;

/**
 * Test java.beans.PersistenceDelegate
 */
public class PersistenceDelegateTest extends TestCase {

    /*
     * Test the constructor.
     */
    public void testConstructor() {
        new DummyPersistenceDelegate();
    }

    /*
     * Tests writeObject() under normal condition when mutatesTo() returns True.
     */
    public void testWriteObject_NormalMutatesToTrue() {
        MockPersistenceDelegate2 pd = new MockPersistenceDelegate2(true);
        MockEncoder2 enc = new MockEncoder2();
        MockFoo foo = new MockFoo();

        pd.writeObject(foo, enc);

        assertEquals("initialize", pd.popMethod());
        assertEquals("mutatesTo", pd.popMethod());
    }

    /*
     * Tests writeObject() under normal condition when mutatesTo() returns
     * false.
     */
    public void testWriteObject_NormalMutatesToFalse() {
        MockPersistenceDelegate2 pd = new MockPersistenceDelegate2(false);
        MockEncoder2 enc = new MockEncoder2();
        MockFoo foo = new MockFoo();

        pd.writeObject(foo, enc);

        assertEquals("instantiate", pd.popMethod());
        assertEquals("mutatesTo", pd.popMethod());
        assertWasAdded(MockFoo.class.getClass(), "new", null, enc);
    }

    /*
     * Tests writeObject() when object is null.
     */
    public void testWriteObject_NullObject() {
        MockPersistenceDelegate2 pd = new MockPersistenceDelegate2();
        Encoder enc = new Encoder();

        try {
            pd.writeObject(null, enc);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Tests writeObject() when encoder is null.
     */
    public void testWriteObject_NullEncoder() {
        MockPersistenceDelegate2 pd = new MockPersistenceDelegate2();

        try {
            pd.writeObject(new MockFoo(), null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Tests initialize() under normal conditions.
     */
    public void testInitialize_Normal() {
        DummyPersistenceDelegate pd = new DummyPersistenceDelegate();
        MockPersistenceDelegate3 pd3 = new MockPersistenceDelegate3();
        Encoder enc = new Encoder();

        enc.setPersistenceDelegate(MockFooStop.class, pd3);
        pd.initialize(MockFoo.class, new MockFoo(), new MockFoo(), enc);
        assertEquals("initialize", pd3.popMethod());
        assertFalse("Extra statement has been detected", pd3.hasMoreMethods());

        // test interface
        pd3 = new MockPersistenceDelegate3();
        enc.setPersistenceDelegate(MockInterface.class, pd3);
        pd
                .initialize(MockObject.class, new MockObject(),
                        new MockObject(), enc);
        assertFalse("Extra statement has been detected", pd3.hasMoreMethods());
    }

    /*
     * Tests initialize() with null class.
     */
    public void testInitialize_NullClass() {
        DummyPersistenceDelegate pd = new DummyPersistenceDelegate();
        Encoder enc = new Encoder();

        enc.setPersistenceDelegate(MockFooStop.class,
                new DummyPersistenceDelegate());

        try {
            pd.initialize(null, new Object(), new Object(), enc);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Tests initialize() with null old and new instances.
     */
    public void testInitialize_NullInstances() {
        DummyPersistenceDelegate pd = new DummyPersistenceDelegate();
        MockPersistenceDelegate3 pd3 = new MockPersistenceDelegate3();
        Encoder enc = new Encoder();

        enc.setPersistenceDelegate(MockFooStop.class, pd3);
        pd.initialize(MockFoo.class, null, null, enc);
        assertEquals("initialize", pd3.popMethod());
    }

    /*
     * Tests initialize() with null encoder.
     */
    public void testInitialize_NullEncoder() {
        DummyPersistenceDelegate pd = new DummyPersistenceDelegate();

        try {
            pd.initialize(MockFoo.class, new Object(), new Object(), null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /**
     * Circular redundancy check. Should not hang. Regression test for
     * HARMONY-2073
     */
    public void testInitialize_circularRedundancy() {
        Encoder enc = new Encoder();
        DummyPersistenceDelegate pd = new DummyPersistenceDelegate();

        enc.setPersistenceDelegate(MockFooStop.class, pd);
        pd.initialize(MockFoo.class, new MockFoo(), new MockFoo(), enc);
    }

    /*
     * Tests mutatesTo() under normal conditions.
     */
    public void testMutatesTo_Normal() {
        DummyPersistenceDelegate pd = new DummyPersistenceDelegate();
        assertTrue(pd.mutatesTo("test1", "test2"));
        assertFalse(pd.mutatesTo(new Object(), new Object() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        }));
        assertFalse(pd.mutatesTo(new MockFoo(), new MockFooStop()));
    }

    /*
     * Tests mutatesTo() with null parameters.
     */
    public void testMutatesTo_Null() {
        DummyPersistenceDelegate pd = new DummyPersistenceDelegate();

        assertFalse(pd.mutatesTo("test", null));
        assertFalse(pd.mutatesTo(null, null));
        assertFalse(pd.mutatesTo(null, "test"));
    }

    public void test_writeObject_LInteger_LXMLEncoder() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
                byteArrayOutputStream));

        encoder.writeObject(1);
        encoder.writeObject(Integer.SIZE);
        encoder.writeObject(Integer.MAX_VALUE);
        encoder.writeObject(Integer.MIN_VALUE);
        encoder.close();

        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Integer one  = (Integer) decoder.readObject();
        Integer size = (Integer) decoder.readObject();
        Integer max  = (Integer) decoder.readObject();
        Integer min  = (Integer) decoder.readObject();
        assertEquals(new Integer(1), one);
        assertEquals(new Integer(Integer.SIZE), size);
        assertEquals(new Integer(Integer.MAX_VALUE), max);
        assertEquals(new Integer(Integer.MIN_VALUE), min);

        stream = new DataInputStream(
                PersistenceDelegateTest.class
                        .getResourceAsStream("/xml/Integer.xml"));
        decoder = new XMLDecoder(stream);
        one  = (Integer) decoder.readObject();
        size = (Integer) decoder.readObject();
        max  = (Integer) decoder.readObject();
        min  = (Integer) decoder.readObject();
        assertEquals(new Integer(1), one);
        assertEquals(new Integer(Integer.SIZE), size);
        assertEquals(new Integer(Integer.MAX_VALUE), max);
        assertEquals(new Integer(Integer.MIN_VALUE), min);
    }
    
    public void test_writeObject_Null_LXMLEncoder() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
                byteArrayOutputStream));
        encoder.writeObject(null);
        encoder.close();

        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        assertNull(decoder.readObject());
        stream = new DataInputStream(PersistenceDelegateTest.class
                .getResourceAsStream("/xml/null.xml"));
        decoder = new XMLDecoder(stream);
        assertNull(decoder.readObject());
    }

    public void test_writeObject_LArray_LXMLEncoder() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
                byteArrayOutputStream));
        int[] intArray = new int[2];
        intArray[0] = 1;
        
        encoder.writeObject(intArray);
        encoder.close();
        
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        
        XMLDecoder decoder = new XMLDecoder(stream);
        int[] array =  (int[]) decoder.readObject();
        assertEquals(2, array.length);
        assertEquals(1, array[0]);
        
        stream = new DataInputStream(PersistenceDelegateTest.class
                .getResourceAsStream("/xml/Array.xml"));
        
        decoder = new XMLDecoder(stream);
        array =  (int[]) decoder.readObject();
        assertEquals(2, array.length);
        assertEquals(1, array[0]);
    }

    public static interface Foo {
        void say();
        String toString();
    };

    public void test_writeObject_Ljava_Lang_reflect_Proxy() {
        InvocationHandler handler = new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] params)
                    throws Throwable {
                if(method.getName().contains("toString")) {
                    return "FooProxy";
                }
                System.out.println(method.getName());
                return null;
            }

        };
        Foo f = (Foo) Proxy.newProxyInstance(Foo.class.getClassLoader(),
                new Class[] { Foo.class }, handler);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
                byteArrayOutputStream));
        
        encoder.writeObject(f);
        encoder.close();

        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));

        XMLDecoder decoder = new XMLDecoder(stream);
        try {
            decoder.readObject();
            fail("Should throw Exception");
        } catch (Exception ex) {
            // expected
        }
        
        stream = new DataInputStream(PersistenceDelegateTest.class
                .getResourceAsStream("/xml/Proxy.xml"));

        decoder = new XMLDecoder(stream);
        try {
            decoder.readObject();
            fail("Should throw Exception");
        } catch (Exception ex) {
            // expected
        }
    }
    
    public void test_writeObject_java_lang_string(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        encoder.writeObject("test");
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Object object = decoder.readObject();
        assertEquals("test", object);
    }

    public void test_writeObject_java_lang_class(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        encoder.writeObject(Class.class);
        encoder.writeObject(2);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Class clz = (Class) decoder.readObject();
        assertEquals(Class.class, clz);
        Integer number = (Integer) decoder.readObject();
        assertEquals(new Integer(2), number);
    }

    class Bar {
        public int value;

        public void barTalk() {
            System.out.println("Bar is coming!");
        }
    }

    public void test_writeObject_java_lang_reflect_Field()
            throws SecurityException, NoSuchFieldException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
                byteArrayOutputStream));
        Field value = Bar.class.getField("value");
        encoder.writeObject(value);
        encoder.close();

        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));

        XMLDecoder decoder = new XMLDecoder(stream);
        Field field = (Field) decoder.readObject();

        assertEquals(value, field);
        assertEquals(value.getName(), field.getName());
    }

    public void test_writeObject_java_lang_reflect_Method()
            throws SecurityException, NoSuchMethodException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
                byteArrayOutputStream));
        Method method = Bar.class.getMethod("barTalk", (Class[]) null);

        encoder.writeObject(method);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Method aMethod = (Method) decoder.readObject();
        assertEquals(method, aMethod);
        assertEquals(method.getName(), aMethod.getName());
        assertEquals("barTalk", aMethod.getName());
    }

    @SuppressWarnings("unchecked")
    public void test_writeObject_java_util_Collection() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
                byteArrayOutputStream));
        LinkedList<Integer> list = new LinkedList<Integer>();
        list.add(10);
        list.addFirst(2);

        encoder.writeObject(list);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        LinkedList<Integer> l = (LinkedList<Integer>) decoder.readObject();
        assertEquals(list, l);
        assertEquals(2, l.size());
        assertEquals(new Integer(10), l.get(1));

    }

    public void test_writeObject_java_awt_Choice() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
                byteArrayOutputStream));
        Choice choice = new Choice();
        choice.setBackground(Color.blue);
        choice.setFocusTraversalKeysEnabled(true);
        choice.setBounds(0, 0, 10, 10);
        choice.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        choice.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        choice.setFocusTraversalKeysEnabled(true);
        choice.setFocusable(true);
        choice.setFont(new Font("Arial Bold", Font.ITALIC, 0));
        choice.setForeground(Color.green);
        choice.setIgnoreRepaint(true);
        choice.setLocation(1, 2);
        choice.setName("choice");
        choice.setSize(new Dimension(200, 100));
        choice.addItem("addItem");
        choice.add("add");

        ComponentListener cl = new ComponentAdapter() {
        };
        choice.addComponentListener(cl);
        FocusListener fl = new FocusAdapter() {
        };
        choice.addFocusListener(fl);
        HierarchyBoundsListener hbl = new HierarchyBoundsAdapter() {
        };
        choice.addHierarchyBoundsListener(hbl);
        HierarchyListener hl = new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
            }
        };
        choice.addHierarchyListener(hl);
        InputMethodListener il = new InputMethodListener() {
            public void caretPositionChanged(InputMethodEvent e) {
            }

            public void inputMethodTextChanged(InputMethodEvent e) {
            }
        };
        choice.addInputMethodListener(il);
        ItemListener il2 = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            }
        };
        choice.addItemListener(il2);
        KeyListener kl = new KeyAdapter() {
        };
        choice.addKeyListener(kl);
        MouseListener ml = new MouseAdapter() {
        };
        choice.addMouseListener(ml);
        MouseMotionListener mml = new MouseMotionAdapter() {
        };
        choice.addMouseMotionListener(mml);
        MouseWheelListener mwl = new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
            }
        };
        choice.addMouseWheelListener(mwl);
        PropertyChangeListener pcl = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
            }
        };
        choice.addPropertyChangeListener(pcl);

        encoder.writeObject(choice);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Choice aChoice = (Choice) decoder.readObject();
        assertEquals(choice.getFocusTraversalKeysEnabled(), aChoice
                .getFocusTraversalKeysEnabled());
        assertEquals(Color.blue, aChoice.getBackground());
        assertEquals(new Rectangle(1, 2, 200, 100), aChoice.getBounds());
        // ComponentOrientation is not persistent
        assertTrue(aChoice.getComponentOrientation().isLeftToRight());

        // Cursor will not be persisted
        assertEquals(Cursor.DEFAULT_CURSOR, aChoice.getCursor().getType());
        // DropTarget will not be persisted
        assertNull(aChoice.getDropTarget());

        assertEquals(choice.getName(), aChoice.getName());

        assertEquals(choice.getItem(0), aChoice.getItem(0));
        assertEquals(1, choice.getComponentListeners().length);
        assertEquals(0, aChoice.getComponentListeners().length);
        assertEquals(1, choice.getFocusListeners().length);
        assertEquals(0, aChoice.getFocusListeners().length);
        assertEquals(1, choice.getHierarchyBoundsListeners().length);
        assertEquals(0, aChoice.getHierarchyBoundsListeners().length);
        assertEquals(1, choice.getInputMethodListeners().length);
        assertEquals(0, aChoice.getInputMethodListeners().length);
        assertEquals(1, choice.getItemListeners().length);
        assertEquals(0, aChoice.getItemListeners().length);
        assertEquals(1, choice.getKeyListeners().length);
        assertEquals(0, aChoice.getKeyListeners().length);
        assertEquals(1, choice.getMouseListeners().length);
        assertEquals(0, aChoice.getMouseListeners().length);
        assertEquals(1, choice.getMouseMotionListeners().length);
        assertEquals(0, aChoice.getMouseMotionListeners().length);
        assertEquals(1, choice.getMouseWheelListeners().length);
        assertEquals(0, aChoice.getMouseWheelListeners().length);
        assertEquals(1, choice.getPropertyChangeListeners().length);
        assertEquals(0, aChoice.getPropertyChangeListeners().length);

        stream = new DataInputStream(PersistenceDelegateTest.class
                .getResourceAsStream("/xml/Choice.xml"));

        decoder = new XMLDecoder(stream);
        aChoice = (Choice) decoder.readObject();
        assertEquals(choice.getFocusTraversalKeysEnabled(), aChoice
                .getFocusTraversalKeysEnabled());
        assertEquals(Color.blue, aChoice.getBackground());
        assertEquals(new Rectangle(1, 2, 200, 100), aChoice.getBounds());
        // ComponentOrientation is not persistent
        assertTrue(aChoice.getComponentOrientation().isLeftToRight());

        // Cursor will not be persisted
        assertEquals(Cursor.DEFAULT_CURSOR, aChoice.getCursor().getType());
        // DropTarget will not be persisted
        assertNull(aChoice.getDropTarget());

        assertEquals(choice.getName(), aChoice.getName());

        assertEquals(choice.getItem(0), aChoice.getItem(0));
    }
    
    
    public void test_writeObject_java_util_HashTable(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        Hashtable<Integer, String> hTable = new Hashtable<Integer, String>();
        hTable.put(1, "1");

        encoder.writeObject(hTable);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Hashtable aHtable = (Hashtable) decoder.readObject();
        assertEquals(hTable.size(), aHtable.size());
        assertEquals(hTable.get(1), aHtable.get(1));
    }

    public void test_writeObject_java_beans_beancontext_BeanContextSupport() throws PropertyVetoException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        BeanContextSupport support = new BeanContextSupport();

        encoder.writeObject(support);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        BeanContextSupport aSupport = (BeanContextSupport) decoder.readObject();
        assertEquals(Locale.getDefault(), aSupport.getLocale());
    }
    
    public void test_writeObject_java_awt_SystemColor() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        encoder.writeObject(SystemColor.activeCaption);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        SystemColor color = (SystemColor) decoder.readObject();
        assertEquals(SystemColor.activeCaption, color);
    }

    public void test_writeObject_java_awt_font_TextAttribute() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        encoder.writeObject(TextAttribute.BACKGROUND);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        TextAttribute attribute = (TextAttribute) decoder.readObject();
        assertEquals(TextAttribute.BACKGROUND, attribute);
    }

    public void test_writeObject_java_awt_MenuShortcut() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        MenuShortcut shortCut = new MenuShortcut(2);

        encoder.writeObject(shortCut);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        MenuShortcut aMenuShortcut = (MenuShortcut) decoder.readObject();
        assertEquals(shortCut, aMenuShortcut);
        assertEquals(shortCut.getKey(), aMenuShortcut.getKey());
    }

    public static class MockComponent extends Component {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
    }
    public void test_writeObject_java_awt_Component() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        
        Component component = new MockComponent();
        component.add(new PopupMenu("PopupMenu"));
        component.setBackground(Color.black);
        component.setBounds(new Rectangle(1, 1, 10, 10));
        component.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        component.setEnabled(true);
        component.setFocusable(true);
        component.setFont(new Font("Arial", 1, 1));
        component.setForeground(Color.blue);
        component.setIgnoreRepaint(true);
        component.setLocale(Locale.CANADA);
        component.setName("MockComponent");
        component.setVisible(true);
        component.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        component.setDropTarget(new DropTarget());

        encoder.writeObject(component);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Component aComponent = (Component) decoder.readObject();
        assertEquals(component.getBackground(), aComponent.getBackground());
        assertEquals(component.getForeground(), aComponent.getForeground());
        assertEquals(component.getFont().getFamily(), aComponent.getFont().getFamily());
        assertEquals(component.getFont().getStyle(), aComponent.getFont().getStyle());
        assertEquals(component.getFont().getSize(), aComponent.getFont().getSize());
        assertEquals(component.getName(), aComponent.getName());
        assertEquals(component.getBounds(), aComponent.getBounds());
    }

    public void test_writeObject_java_awt_Container() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        Container container = new Container();
        container.setBackground(Color.blue);
        container.setFocusTraversalKeysEnabled(true);
        container.setBounds(0, 0, 10, 10);
        container.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        container.setComponentZOrder(new Label("label"), 0);
        container.setComponentZOrder(new JTabbedPane(), 1);
        container.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        container.setFocusTraversalKeysEnabled(true);
        container.setFocusable(true);
        container.setFocusTraversalPolicyProvider(true);
        container.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        container.setFont(new Font("Arial Bold", Font.ITALIC, 0));
        container.setForeground(Color.green);
        container.setIgnoreRepaint(true);
        container.setLocation(1, 2);
        container.setName("container");
        container.setSize(new Dimension(200, 100));
        container.setEnabled(true);
        container.setFocusCycleRoot(true);
        container.setLayout(new BorderLayout());
        container.setLocale(Locale.CANADA);
        container.setVisible(true);
        container.add(new Label("label"));
        container.add(new JTabbedPane());

        encoder.writeObject(container);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Container aContainer = (Container) decoder.readObject();
        assertEquals(container.getFocusTraversalKeysEnabled(), aContainer
                .getFocusTraversalKeysEnabled());
        assertEquals(Color.blue, aContainer.getBackground());
        assertEquals(new Rectangle(1, 2, 200, 100), aContainer.getBounds());
        
        // ComponentOrientation is not persistent
        assertTrue(aContainer.getComponentOrientation().isLeftToRight());
        Component [] components = aContainer.getComponents();
        assertTrue(components[0] instanceof Label);
        assertEquals(0, aContainer.getComponentZOrder(components[0]));
        assertTrue(components[1] instanceof JTabbedPane);
        assertEquals(1, aContainer.getComponentZOrder(components[1]));
        
        // Cursor will not be persisted
        assertEquals(Cursor.DEFAULT_CURSOR, aContainer.getCursor().getType());
        // DropTarget will not be persisted
        assertNull(aContainer.getDropTarget());
        
        assertEquals(container.getFocusTraversalPolicy().getClass(), aContainer
                .getFocusTraversalPolicy().getClass());
        assertEquals(container.getName(), aContainer.getName());

        container = new Container();
        container.setFocusCycleRoot(true);
        byteArrayOutputStream = new ByteArrayOutputStream();
        encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        encoder.writeObject(container);
        encoder.close();
        stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        decoder = new XMLDecoder(stream);
        aContainer = (Container) decoder.readObject();
        assertTrue(aContainer.isFocusCycleRoot());
        
    }
    
    public void test_writeObject_java_awt_Menu() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        Menu menu = new Menu();
        String action = "menu action command";
        menu.setActionCommand(action);
        menu.setEnabled(true);
        menu.setFont(new Font("Arial Black", Font.BOLD, 10));
        menu.setLabel("menu");
        menu.setName("menu");
        menu.setShortcut(new MenuShortcut(10));
        menu.insertSeparator(1);

        encoder.writeObject(menu);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Menu aMenu = (Menu) decoder.readObject();
        assertTrue(aMenu.isEnabled());
        assertEquals(action, aMenu.getActionCommand());
        assertEquals(menu.getFont().getSize(), aMenu.getFont().getSize());
        assertEquals(menu.getFont().getStyle(), aMenu.getFont().getStyle());
        assertEquals(menu.getLabel(), aMenu.getLabel());
        assertEquals(menu.getName(), aMenu.getName());
        assertEquals(menu.getShortcut().getKey(), aMenu.getShortcut().getKey());
        assertEquals(1, menu.getItemCount());
        assertEquals(menu.getItem(0).getLabel(), aMenu.getItem(0).getLabel());
        assertEquals(menu.getItem(0).getName(), aMenu.getItem(0).getName());
        assertEquals(menu.getItem(0).getFont().getStyle(), aMenu.getItem(0).getFont().getStyle());
        assertEquals(menu.getFont().getSize(), aMenu.getItem(0).getFont().getSize());
        
    }

    public void test_writeObject_java_awt_MenuBar() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
 
        MenuBar bar = new MenuBar();
        bar.add(new Menu("menu1"));

        encoder.writeObject(bar);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        MenuBar aBar = (MenuBar) decoder.readObject();
        assertEquals(bar.getMenu(0).getName(), aBar.getMenu(0).getName());
        
    }

    public void test_writeObject_java_awt_List() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        java.awt.List list = new java.awt.List();
        list.setBounds(0, 0, 10, 10);
        list.add(new PopupMenu("popupMenu"));
        list.add("1");
        list.add("2", 2);
        list.setBackground(Color.BLUE);
        encoder.writeObject(list);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        java.awt.List aList = (java.awt.List) decoder.readObject();
        assertEquals(list.getItem(0), aList.getItem(0));
        assertEquals(list.getHeight(), aList.getHeight());
        assertEquals(list.getBackground(), aList.getBackground());
    }

    public void test_writeObject_java_awt_BorderLayout(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        BorderLayout layout = new BorderLayout();
        layout.setHgap(2);
        layout.setVgap(3);
        encoder.writeObject(layout);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        BorderLayout aLayout = (BorderLayout) decoder.readObject();
        assertEquals(layout.getHgap(), aLayout.getHgap());
        assertEquals(layout.getVgap(), aLayout.getVgap());
    }

    public void test_writeObject_java_awt_CardLayout(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        
        CardLayout layout = new CardLayout();
        layout.addLayoutComponent(new Label("label"), "constraints");
        layout.setHgap(2);
        layout.setVgap(3);
        encoder.writeObject(layout);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        CardLayout aLayout = (CardLayout) decoder.readObject();
        assertEquals(layout.getHgap(), aLayout.getHgap());
        assertEquals(layout.getVgap(), aLayout.getVgap());
    }

    public void test_writeObject_java_awt_GridBagLayout() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        GridBagLayout layout = new GridBagLayout();
        layout.addLayoutComponent(new Label("label"), new GridBagConstraints(0,
                0, 100, 60, 0.1, 0.1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0,0, 99, 59), 0, 0));
        encoder.writeObject(layout);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        GridBagLayout aLayout = (GridBagLayout) decoder.readObject();
        assertEquals(layout.getConstraints(new Label("label")).weightx, aLayout
                .getConstraints(new Label("label")).weightx);
        assertEquals(layout.getConstraints(new Label("label")).insets.left, aLayout
                .getConstraints(new Label("label")).insets.left);
    }
    
    public void test_writeObject_java_awt_Cursor() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        Cursor cursor = new Cursor(Cursor.CROSSHAIR_CURSOR);

        encoder.writeObject(cursor);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Cursor aCursor = (Cursor) decoder.readObject();
        assertEquals(cursor.getName(), aCursor
                .getName());
        assertEquals(cursor.getType(), aCursor.getType());
    }
    
    public void test_writeObject_java_awt_Insets() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        Insets inset = new Insets(0, 0, 10, 10);

        encoder.writeObject(inset);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Insets aInset = (Insets) decoder.readObject();
        assertEquals(inset.left, aInset.left);
        assertEquals(inset.top, aInset.top);
    }
    
    public void test_writeObject_java_awt_point() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        Point point = new Point(10, 20);
        
        encoder.writeObject(point);
        encoder.close();
        
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Point aPoint = (Point) decoder.readObject();
        assertEquals(point, aPoint);
    }

    public void test_writeObject_java_awt_ScrollPane() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        ScrollPane scrollPane = new ScrollPane();
        
        encoder.writeObject(scrollPane);
        encoder.close();
        
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        ScrollPane aScrollPane = (ScrollPane) decoder.readObject();
        assertEquals(scrollPane.getAlignmentX(), aScrollPane.getAlignmentX());
        assertEquals(scrollPane.getAlignmentY(), aScrollPane.getAlignmentY());
        assertEquals(scrollPane.getScrollbarDisplayPolicy(), aScrollPane
                .getScrollbarDisplayPolicy());
    }
    
    public void test_writeObject_java_util_Map(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        TreeMap<Integer, String> map = new TreeMap<Integer, String>();
        map.put(new Integer(10), "first element");

        encoder.writeObject(map);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        TreeMap aMap = (TreeMap) decoder.readObject();
        assertEquals(map.size(), aMap.size());
        assertEquals(map.get(10), aMap.get(10));
    }
    
    public void test_writeObject_java_util_List() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        Vector vector = new Vector();
        vector.add("test");
        vector.add(1);

        encoder.writeObject(vector);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Vector aVector = (Vector) decoder.readObject();
        assertEquals(vector, aVector);
        assertEquals(2, aVector.size());
        assertEquals(vector.get(0), aVector.get(0));
        assertEquals(1, aVector.get(1));
        
        Vector v = new Vector();
        v.add("The first item.");
        v.add("The second item.");
        Vector v2 = new Vector();
        v2.add("A nested item.");
        v.add(v2);
        
        byteArrayOutputStream = new ByteArrayOutputStream();
        encoder = new XMLEncoder(byteArrayOutputStream);
        encoder.writeObject(v);
        encoder.flush();
        encoder.close();
        
        stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        decoder = new XMLDecoder(stream);
        aVector = (Vector) decoder.readObject();
        assertEquals(v, aVector);
        assertEquals(3, aVector.size());
        assertEquals(v.get(0), aVector.get(0));
        assertEquals(v.get(2), aVector.get(2));
    }

    public void test_writeObject_java_util_AbstractList(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        arrayList.add(10);

        encoder.writeObject(arrayList);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        ArrayList aList = (ArrayList) decoder.readObject();
        assertEquals(arrayList.size(), aList.size());
        assertEquals(arrayList.get(0), arrayList.get(0));
    }

    public void test_writeObject_java_util_AbstractMap() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        HashMap<Integer, String> hMap = new HashMap<Integer, String>();
        hMap.put(1, "1");
        hMap.put(2, "test");

        encoder.writeObject(hMap);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        HashMap aHmap = (HashMap) decoder.readObject();
        assertEquals(hMap.size(), aHmap.size());
        assertEquals(hMap.get(1), aHmap.get(1));
        assertEquals("test", aHmap.get(2));
    }
    
    public void test_writeObject_javax_swing_JFrame() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        
        JFrame frame = new JFrame("JFrame"); 
        frame.setAlwaysOnTop(true);
        frame.setBounds(1, 1, 100, 100);
        frame.add(new JMenu("JMenu"));
        encoder.writeObject(frame);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        JFrame aFrame = (JFrame) decoder.readObject();
        assertEquals(frame.getTitle(), aFrame.getTitle());
        assertEquals(frame.getBounds(), aFrame.getBounds());
    }

    public void test_writeObject_javax_swing_DefaultListModel() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));

        DefaultListModel model = new DefaultListModel();
        model.add(0, 1);
        model.add(1, 2);
        ListDataHandler listDataHandler = new BasicComboBoxUI().new ListDataHandler();
        model.addListDataListener(listDataHandler);
        
        encoder.writeObject(model);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        DefaultListModel aModel = (DefaultListModel) decoder.readObject();
        assertEquals(model.getSize(), aModel.getSize());
    }

    public void test_writeObject_javax_swing_DefaultComboBoxModel() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        Object a[] = {1, 2};
        DefaultComboBoxModel model = new DefaultComboBoxModel(a);

        encoder.writeObject(model);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        DefaultComboBoxModel aModel = (DefaultComboBoxModel) decoder.readObject();
        assertEquals(model.getSize(), aModel.getSize());
        assertEquals(model.getElementAt(0), aModel.getElementAt(0));
        assertEquals(model.getElementAt(1), aModel.getElementAt(1));
    }

    public void test_writeObject_javax_swing_tree_DefaultMutableTreeNode() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(1);

        encoder.writeObject(node);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) decoder.readObject();
        assertEquals(node.getUserObject(), aNode.getUserObject());
    }

    public void test_writeObject_javax_swing_ToolTipManager() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        ToolTipManager manager = ToolTipManager.sharedInstance();
        manager.setDismissDelay(10);

        encoder.writeObject(manager);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        ToolTipManager aManager = (ToolTipManager) decoder.readObject();
        assertEquals(manager, aManager);
        assertEquals(manager.getDismissDelay(), aManager.getDismissDelay());
    }
    
    public void test_writeObject_javax_swing_Box() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        Box box = new Box(1);
        box.setAlignmentX(12.21f);

        encoder.writeObject(box);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Box aBox = (Box) decoder.readObject();
        assertEquals(box.getAlignmentX(), aBox.getAlignmentX());
    }

    public void test_writeObject_javax_swing_JMenu(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
            byteArrayOutputStream));
        JMenu menu = new JMenu("menu");

        encoder.writeObject(menu);
        encoder.close();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        JMenu aMenu = (JMenu) decoder.readObject();
        assertEquals(menu.getVerifyInputWhenFocusTarget(), aMenu
                .getVerifyInputWhenFocusTarget());
        assertEquals(menu.getName(), aMenu.getName());
    }
    
    public void test_writeObject_Integer_Class() {
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        System.err.println("Test 5");
        Object[] objects5 = { Integer.class, Integer.TYPE };
        XMLEncoder xmlEnc5 = new XMLEncoder(os);
        for (int i = 0; i < objects5.length; i++) {
            xmlEnc5.writeObject(objects5[i]);
        }
        xmlEnc5.flush();
        xmlEnc5.close();
        
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                os.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Class intClass = (Class) decoder.readObject();
        Class aIntClass = (Class) decoder.readObject();
        assertEquals(Integer.class, intClass);
        assertEquals(Integer.TYPE, aIntClass);
    }
    
    public static class DummyBean {

        private String value;

        public DummyBean() {
        }

        public DummyBean(String s) {
            value = s;
        }

        public String getDummyValue() {
            return value;
        }

        public void setDummyValue(String s) {
            value = s;
        }
        
        public boolean equals(Object bean) {
            if (!(bean instanceof DummyBean)) {
                return false;
            }
            DummyBean aBean = (DummyBean) bean;
            
            if (aBean.value == null && value != null || value != null
                    && aBean.value == null) {
                return false;
            } else if(value != null && aBean.value != null){
                return value.equals(aBean.value);
            }
            return true;
        }
    }
    public void test_writeExpression_writeObject() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder( output );

        Date date = new Date(2007, 06, 26);
        Expression expression = new Expression( date, "toString", null );
        String date_string = null;
        date_string = (String) expression.getValue();

        DummyBean bean = new DummyBean( date_string );
        // The expression knows about the date object.
        encoder.writeExpression( expression );
        encoder.writeObject( date );
        // The value for the bean is already part of the expression
        // so instead of writing the value we write a reference to
        // the expression.
        encoder.writeObject( bean );

        encoder.flush();
        encoder.close();
        
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                output.toByteArray()));
        XMLDecoder decoder = new XMLDecoder(stream);
        Date aDate = (Date) decoder.readObject();
        assertEquals(date, aDate);
        
        DummyBean aBean = (DummyBean) decoder.readObject();
        assertEquals(bean, aBean);
    }

    // <--

    private void assertWasAdded(Class<?> targetClass, String methodName,
            Object[] args, MockEncoder2 enc) {
        try {
            while (true) {
                Statement stmt = enc.pop();

                if (equals(stmt, targetClass, methodName, args)) {
                    break;
                }
            }
        } catch (EmptyStackException e) {
            fail("Required statement was not found");
        }
    }

    private boolean equals(Statement stmt, Class<?> targetClass,
            String methodName, Object[] args) {

        if (stmt == null || !methodName.equals(stmt.getMethodName())) {
            return false;
        }

        if (targetClass != null) {
            // check if we have the object of the same class at least
            if (targetClass != stmt.getTarget().getClass()) {
                return false;
            }
        } else {
            if (stmt.getTarget() != null) {
                return false;
            }
        }

        if (args != null) {
            if (stmt.getArguments() == null
                    || args.length != stmt.getArguments().length) {
                return false;
            }

            for (int i = 0; i < args.length; i++) {

                if (args[i] != null) {
                    if (!args[i].equals(stmt.getArguments()[i])) {
                        return false;
                    }
                } else {
                    if (stmt.getArguments()[i] != null) {
                        return false;
                    }
                }
            }
        } else {
            if (stmt.getArguments() != null && stmt.getArguments().length > 0) {
                return false;
            }
        }

        return true;
    }

    /*
     * Mock interface.
     */
    static interface MockInterface {

    }

    /*
     * Mock interface.
     */
    static class MockObject implements MockInterface {

    }

    static class MockEncoder2 extends Encoder {
        Stack<Statement> stmts = new Stack<Statement>();

        @Override
        public void writeExpression(Expression expr) {
            stmts.push(expr);
            super.writeExpression(expr);
        }

        @Override
        public void writeStatement(Statement stmt) {
            stmts.push(stmt);
            super.writeStatement(stmt);
        }

        @Override
        public void writeObject(Object obj) {
            super.writeObject(obj);
        }

        public Statement pop() {
            return stmts.pop();
        }

    }

    static class MockPersistenceDelegate2 extends PersistenceDelegate {
        private Boolean mutatesToFlag = null;

        Stack<String> methods = new Stack<String>();

        public MockPersistenceDelegate2() {
        }

        public MockPersistenceDelegate2(boolean mutatesToFlag) {
            this.mutatesToFlag = Boolean.valueOf(mutatesToFlag);
        }

        @Override
        public void initialize(Class<?> type, Object oldInstance,
                Object newInstance, Encoder enc) {
            methods.push("initialize");
            super.initialize(type, oldInstance, newInstance, enc);
        }

        @Override
        public Expression instantiate(Object oldInstance, Encoder out) {
            methods.push("instantiate");
            return new Expression(oldInstance.getClass(), "new", null);
        }

        @Override
        public boolean mutatesTo(Object oldInstance, Object newInstance) {
            methods.push("mutatesTo");

            if (mutatesToFlag != null) {
                return mutatesToFlag.booleanValue();
            }
            return super.mutatesTo(oldInstance, newInstance);
        }

        String popMethod() {
            return methods.pop();
        }

        boolean hasMoreMethods() {
            return !methods.empty();
        }
    }

    static class MockPersistenceDelegate3 extends PersistenceDelegate {
        Stack<String> methods = new Stack<String>();

        @Override
        public void initialize(Class<?> type, Object oldInstance,
                Object newInstance, Encoder enc) {
            methods.push("initialize");
        }

        @Override
        public Expression instantiate(Object oldInstance, Encoder out) {
            methods.push("instantiate");
            return null;
        }

        @Override
        public boolean mutatesTo(Object oldInstance, Object newInstance) {
            methods.push("mutatesTo");
            return true;
        }

        String popMethod() {
            return methods.pop();
        }

        boolean hasMoreMethods() {
            return !methods.empty();
        }
    }

    /*
     * Dummy PersistenceDelegate subclass.
     */
    static class DummyPersistenceDelegate extends PersistenceDelegate {
        @Override
        public Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance.getClass(), "new", null);
        }

        @Override
        public void initialize(Class<?> type, Object oldInstance,
                Object newInstance, Encoder enc) {
            super.initialize(type, oldInstance, newInstance, enc);
        }

        @Override
        public boolean mutatesTo(Object oldInstance, Object newInstance) {
            return super.mutatesTo(oldInstance, newInstance);
        }

        @Override
        public void writeObject(Object oldInstance, Encoder enc) {
            super.writeObject(oldInstance, enc);
        }

    }

}
