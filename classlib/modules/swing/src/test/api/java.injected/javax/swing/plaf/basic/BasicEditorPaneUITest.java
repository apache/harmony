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
package javax.swing.plaf.basic;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingTestCase;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.DefaultEditorKit;

public class BasicEditorPaneUITest extends SwingTestCase {
    JEditorPane jep;

    JFrame jf;

    BasicEditorPaneUI ui;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        jf = new JFrame();
        jep = new JEditorPane();
        ui = (BasicEditorPaneUI) jep.getUI();
        jf.getContentPane().add(jep);
        jf.setSize(200, 300);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testCreateUI() {
        ComponentUI ui1 = BasicEditorPaneUI.createUI(jep);
        ComponentUI ui2 = BasicEditorPaneUI.createUI(jep);
        assertTrue(ui1 instanceof BasicEditorPaneUI);
        assertTrue(ui2 instanceof BasicEditorPaneUI);
        ui1 = BasicEditorPaneUI.createUI(new JTextField());
        assertTrue(ui1 instanceof BasicEditorPaneUI);
        ui1 = BasicEditorPaneUI.createUI(new JTextArea());
        assertTrue(ui1 instanceof BasicEditorPaneUI);
    }

    public void testGetEditorKit() {
        assertEquals(jep.getEditorKit(), ui.getEditorKit(jep));
        jep.setContentType("text/html");
        assertEquals(jep.getEditorKit(), ui.getEditorKit(jep));
        jep.setContentType("text/rtf");
        assertEquals(jep.getEditorKit(), ui.getEditorKit(jep));
        DefaultEditorKit kit = new DefaultEditorKit();
        jep.setEditorKit(kit);
        assertEquals(jep.getEditorKit(), ui.getEditorKit(jep));
        assertEquals(kit, ui.getEditorKit(new JTextField()));
        assertEquals(kit, ui.getEditorKit(new JTextArea()));
        BasicEditorPaneUI editorPaneUI = (BasicEditorPaneUI) BasicEditorPaneUI.createUI(jep);
        assertNull(editorPaneUI.getComponent());
    }

    public void testGetPropertyPrefix() {
        assertEquals("EditorPane", ui.getPropertyPrefix());
    }

    void checkNames(final Action a1[], final Object a2[]) {
        for (int i = 0; i < a1.length; i++) {
            String name = (String) a1[i].getValue(Action.NAME);
            boolean wasFound = false;
            for (int j = 0; j < a2.length; j++) {
                if (a2[j].equals(name)) {
                    wasFound = true;
                    break;
                }
            }
            assertTrue(wasFound);
            if (!wasFound) {
                System.out.println(name);
            }
        }
    }

    public void testPropertyChange() {
        Action a1[] = jep.getActions();
        Object a2[] = jep.getActionMap().getParent().getParent().allKeys();
        checkNames(a1, a2);
        jep.setContentType("text/html");
        a1 = jep.getActions();
        a2 = jep.getActionMap().getParent().getParent().allKeys();
        checkNames(a1, a2);
        jep.setContentType("text/rtf");
        a1 = jep.getActions();
        a2 = jep.getActionMap().getParent().getParent().allKeys();
        checkNames(a1, a2);
    }
}
