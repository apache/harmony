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
 * @author Alexander T. Simbirtsev
 * Created on 07.10.2004

 */
package javax.swing;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class AccessibleJComponentTest extends SwingTestCase {
    protected class ConcreteFocusListener implements FocusListener {
        public boolean state = false;

        public void focusGained(final FocusEvent arg0) {
            state = true;
        }

        public void focusLost(final FocusEvent arg0) {
            state = true;
        }
    };

    protected JComponent panel;

    protected JComponent.AccessibleJComponent aContext;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            public AccessibleContext getAccessibleContext() {
                return new AccessibleJComponent() {
                    private static final long serialVersionUID = 1L;
                };
            }
        };
        aContext = (JComponent.AccessibleJComponent) panel.getAccessibleContext();
    }

    public void testGetAccessibleChildrenCount() {
        assertEquals(aContext.getAccessibleChildrenCount(), 0);
        panel.add(new JPanel());
        panel.add(new JPanel());
        panel.add(new JPanel());
        assertEquals(aContext.getAccessibleChildrenCount(), 3);
    }

    /*
     * Class under test for AccessibleRole getAccessibleRole()
     */
    public void testGetAccessibleRole() {
        assertTrue(aContext.getAccessibleRole().equals(AccessibleRole.SWING_COMPONENT));
    }

    public void testGetAccessibleKeyBinding() {
        assertNull(aContext.getAccessibleKeyBinding());
    }

    /*
     * Class under test for Accessible getAccessibleChild(int)
     */
    public void testGetAccessibleChildint() {
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        JPanel panel3 = new JPanel();
        assertNull(aContext.getAccessibleChild(0));
        panel.add(panel1);
        panel.add(panel2);
        panel.add(panel3);
        assertSame(aContext.getAccessibleChild(0), panel1);
        assertSame(aContext.getAccessibleChild(1), panel2);
        assertSame(aContext.getAccessibleChild(2), panel3);
    }

    public void testGetToolTipText() {
        panel.setToolTipText("text");
        assertNull(aContext.getToolTipText());
        panel = new JButton();
        aContext = (JComponent.AccessibleJComponent) panel.getAccessibleContext();
        String text = "text";
        panel.setToolTipText(text);
        assertTrue(aContext.getToolTipText().equals(text));
    }

    public void testGetTitledBorderText() {
        String title1 = "title1";
        String title2 = "title2";
        assertNull(aContext.getTitledBorderText());
        panel.setBorder(new TitledBorder(title1));
        assertEquals(aContext.getTitledBorderText(), title1);
        panel.setBorder(new CompoundBorder(new TitledBorder(title1), new TitledBorder(title2)));
        assertNull(aContext.getTitledBorderText());
    }

    public void testGetBorderTitle() {
        String title1 = "title1";
        String title2 = "title2";
        String title3 = "title3";
        String title4 = "title3";
        Border border = null;
        assertNull(aContext.getBorderTitle(border));
        border = new TitledBorder(title1);
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title1);
        border = new CompoundBorder(new TitledBorder(title1), new EmptyBorder(1, 1, 1, 1));
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title1);
        border = new CompoundBorder(new EmptyBorder(1, 1, 1, 1), new TitledBorder(title2));
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title2);
        border = new CompoundBorder(new TitledBorder(title1), new TitledBorder(title2));
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title2);
        border = new CompoundBorder(new CompoundBorder(new TitledBorder(title1),
                new TitledBorder(title2)), new CompoundBorder(new TitledBorder(title3),
                new TitledBorder(title4)));
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title4);
        border = new CompoundBorder(new CompoundBorder(new TitledBorder(title1),
                new TitledBorder(title2)), new CompoundBorder(new TitledBorder(title3),
                new EmptyBorder(1, 1, 1, 1)));
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title3);
        border = new CompoundBorder(new CompoundBorder(new TitledBorder(title1),
                new TitledBorder(title2)), new CompoundBorder(new EmptyBorder(1, 1, 1, 1),
                new EmptyBorder(1, 1, 1, 1)));
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title2);
        border = new CompoundBorder(new CompoundBorder(new EmptyBorder(1, 1, 1, 1),
                new TitledBorder(title2)), new CompoundBorder(new EmptyBorder(1, 1, 1, 1),
                new EmptyBorder(1, 1, 1, 1)));
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title2);
        border = new CompoundBorder(new CompoundBorder(new TitledBorder(title1),
                new EmptyBorder(1, 1, 1, 1)), new CompoundBorder(new EmptyBorder(1, 1, 1, 1),
                new EmptyBorder(1, 1, 1, 1)));
        assertNotNull(aContext.getBorderTitle(border));
        assertEquals(aContext.getBorderTitle(border), title1);
    }

    /*
     * Class under test for String getAccessibleName()
     */
    public void testGetAccessibleName() {
        String text1 = "text1";
        String text2 = "text2";
        assertNull(aContext.getAccessibleName());
        panel.setName(text2);
        assertNull(aContext.getAccessibleName());
        aContext.setAccessibleName(text1);
        assertEquals(aContext.getAccessibleName(), text1);
    }

    /*
     * Class under test for String getAccessibleDescription()
     */
    public void testGetAccessibleDescription() {
        String text1 = "text1";
        String text2 = "text2";
        panel = new JPanel();
        aContext = (JComponent.AccessibleJComponent) panel.getAccessibleContext();
        assertNull(aContext.getAccessibleDescription());
        aContext.setAccessibleDescription(text2);
        assertTrue(aContext.getAccessibleDescription().equals(text2));
        panel.setToolTipText(text1);
        assertTrue(aContext.getAccessibleDescription() != null);
        assertTrue(aContext.getAccessibleDescription().equals(text2));
    }
}
