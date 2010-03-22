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

import java.awt.ComponentOrientation;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.plaf.basic.TextCompUI;

public class JTextComponent_byAuxiliaryComponentTest extends SwingTestCase {
    JFrame jf;

    JTextArea jtc;

    SimplePropertyChangeListener pChListener;

    String sRTL = "\u05DC";

    String sLTR = "\u0061";

    JTextComp jtComp;

    String pattern = "@[^,}]*";

    void assertEqualsPropertyChangeEvent(final String name, final Object oldValue,
            final Object newValue, final PropertyChangeEvent e) {
        assertEquals(name, e.getPropertyName());
        assertEquals(oldValue, e.getOldValue());
        assertEquals(newValue, e.getNewValue());
    }

    class JTextComp extends JTextComponent {
        private static final long serialVersionUID = 1L;

        String UIClassId = "TextCompUIFirst";

        @Override
        public String getUIClassID() {
            return (UIClassId != null) ? UIClassId : "TextCompUIFirst";
        }
    }

    class SimplePropertyChangeListener implements PropertyChangeListener {
        PropertyChangeEvent event;

        public void propertyChange(final PropertyChangeEvent e) {
            if (e.getPropertyName() != "ancestor") {
                event = e;
            }
        }

        PropertyChangeEvent getEvent() {
            PropertyChangeEvent e = event;
            event = null;
            return e;
        }
    }

    public JTextComponent_byAuxiliaryComponentTest() {
        setIgnoreNotImplemented(true);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        UIManager.put("TextCompUISecond", "javax.swing.plaf.basic.BasicTextAreaUI");
        UIManager.put("TextCompUIFirst", "javax.swing.plaf.basic.TextCompUI");
        jf = new JFrame();
        jtComp = new JTextComp();
        jf.getContentPane().add(jtComp);
        jf.setLocation(200, 300);
        jf.setSize(300, 200);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testGetAccessibleContext() {
        AccessibleContext ac = jtComp.getAccessibleContext();
        assertTrue(jtComp.getAccessibleContext() instanceof JTextComponent.AccessibleJTextComponent);
        assertTrue(jtComp.getAccessibleContext() instanceof JTextComponent.AccessibleJTextComponent);
        assertEquals(ac, jtComp.getAccessibleContext());
    }

    public void testGetActions() {
        Action actions[] = new DefaultEditorKit().getActions();
        Action actions1[] = jtComp.getActions();
        Action actions2[] = new DefaultEditorKit().getActions();
        assertEquals(actions1.length, actions2.length);
        assertEquals(53, actions1.length);
        for (int i = 0; i < actions.length; i++) {
            assertEquals(actions1[i], actions2[i]);
        }
    }

    public void testUpdateUI() throws Exception {
        assertTrue(jtComp.getUI() instanceof TextCompUI);
        TextUI textUI1 = jtComp.getUI();
        SimplePropertyChangeListener listener = new SimplePropertyChangeListener();
        jtComp.addPropertyChangeListener(listener);
        jtComp.UIClassId = "TextCompUISecond";
        TextUI textUI2 = (TextUI) UIManager.getUI(jtComp);
        assertNotNull(textUI2);
        jtComp.updateUI();
        assertEqualsPropertyChangeEvent("UI", textUI1, jtComp.getUI(), listener.event);
        assertTrue(jtComp.getUI() instanceof BasicTextAreaUI);
    }

    public void testJTextComponent() {
        jtComp = new JTextComp();
        assertNotNull(jtComp);
        assertTrue(jtComp.getUI() instanceof TextCompUI);
        assertTrue(jtComp.getCaret() instanceof BasicTextUI.BasicCaret);
        assertEquals("Dot=(0, Forward) Mark=(0, Forward)", jtComp.getCaret().toString());
        assertTrue(jtComp.getHighlighter() instanceof BasicTextUI.BasicHighlighter);
        assertEquals(ComponentOrientation.UNKNOWN, jtComp.getComponentOrientation());
        assertTrue(jtComp.isEditable());
        assertFalse(jtComp.getDragEnabled());
        assertEquals('\0', jtComp.getFocusAccelerator());
        assertEquals("TextCompUI", jtComp.getKeymap().getName());
        assertEquals(0, jtComp.getKeymap().getBoundActions().length);
        assertEquals(0, jtComp.getKeymap().getBoundKeyStrokes().length);
        assertEquals(new InsetsUIResource(0, 0, 0, 0), jtComp.getMargin());
    }

    public void testSetGetUITextUI() throws Exception {
        assertTrue(jtComp.getUI() instanceof TextCompUI);
        TextUI textUI1 = jtComp.getUI();
        SimplePropertyChangeListener listener = new SimplePropertyChangeListener();
        jtComp.addPropertyChangeListener(listener);
        jtComp.UIClassId = "TextCompUISecond";
        TextUI textUI2 = (TextUI) UIManager.getUI(jtComp);
        jtComp.setUI(textUI2);
        assertEqualsPropertyChangeEvent("UI", textUI1, textUI2, listener.event);
        assertEquals(textUI2, jtComp.getUI());
    }
}