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
package javax.swing;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.LabelUI;

public class JLabelTest extends BasicSwingTestCase {
    private JLabel label;

    private TestPropertyChangeListener listener;

    public JLabelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        label = new JLabel();
        listener = new TestPropertyChangeListener();
        label.addPropertyChangeListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        label = null;
    }

    public void testJLabel() throws Exception {
        label = new JLabel();
        assertEquals(SwingConstants.CENTER, label.getVerticalAlignment());
        assertEquals(SwingConstants.LEADING, label.getHorizontalAlignment());
        assertEquals("", label.getText());
        assertTrue(label.getAlignmentX() == 0);
        assertTrue(label.getAlignmentY() == 0.5);
        final Icon icon = createTestIcon();
        label = new JLabel(icon);
        assertEquals(SwingConstants.CENTER, label.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, label.getHorizontalAlignment());
        assertNull(label.getText());
        assertTrue(label.getAlignmentX() == 0);
        assertTrue(label.getAlignmentY() == 0.5);
        label = new JLabel(icon, SwingConstants.RIGHT);
        assertEquals(SwingConstants.CENTER, label.getVerticalAlignment());
        assertEquals(SwingConstants.RIGHT, label.getHorizontalAlignment());
        assertNull(label.getText());
        assertTrue(label.getAlignmentX() == 0);
        assertTrue(label.getAlignmentY() == 0.5);
        label = new JLabel("any");
        assertEquals(SwingConstants.CENTER, label.getVerticalAlignment());
        assertEquals(SwingConstants.LEADING, label.getHorizontalAlignment());
        assertTrue(label.getAlignmentX() == 0);
        assertTrue(label.getAlignmentY() == 0.5);
        label = new JLabel("any", SwingConstants.TRAILING);
        assertEquals(SwingConstants.CENTER, label.getVerticalAlignment());
        assertEquals(SwingConstants.TRAILING, label.getHorizontalAlignment());
        assertTrue(label.getAlignmentX() == 0);
        assertTrue(label.getAlignmentY() == 0.5);
        label = new JLabel("any", icon, SwingConstants.RIGHT);
        assertEquals(SwingConstants.CENTER, label.getVerticalAlignment());
        assertEquals(SwingConstants.RIGHT, label.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, label.getHorizontalTextPosition());
        assertTrue(label.getAlignmentX() == 0);
        assertTrue(label.getAlignmentY() == 0.5);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JLabel(icon, SwingConstants.BOTTOM);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JLabel("any", SwingConstants.TOP);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JLabel("any", icon, SwingConstants.TOP);
            }
        });
    }

    public void testCheckHorizontalKey() throws Exception {
        JLabel label = new JLabel();
        label.checkHorizontalKey(SwingConstants.LEFT, null);
        label.checkHorizontalKey(SwingConstants.CENTER, null);
        label.checkHorizontalKey(SwingConstants.RIGHT, null);
        label.checkHorizontalKey(SwingConstants.LEADING, null);
        label.checkHorizontalKey(SwingConstants.TRAILING, null);
        checkHorizontalKey(label, SwingConstants.BOTTOM);
        checkHorizontalKey(label, SwingConstants.TOP);
    }

    public void testCheckVerticalKey() throws Exception {
        JLabel label = new JLabel();
        label.checkVerticalKey(SwingConstants.TOP, null);
        label.checkVerticalKey(SwingConstants.CENTER, null);
        label.checkVerticalKey(SwingConstants.BOTTOM, null);
        checkVerticalKey(label, SwingConstants.LEADING);
        checkVerticalKey(label, SwingConstants.TRAILING);
        checkVerticalKey(label, SwingConstants.LEFT);
        checkVerticalKey(label, SwingConstants.RIGHT);
    }

    public void testGetAccessibleContext() throws Exception {
        assertNotNull(label.getAccessibleContext());
        assertEquals(AccessibleRole.LABEL, label.getAccessibleContext().getAccessibleRole());
    }

    public void testGetSetIcons() throws Exception {
        assertNull(label.getIcon());
        assertNull(label.getDisabledIcon());
        Icon icon = createTestIcon();
        label.setIcon(icon);
        assertEquals(icon, label.getIcon());
        assertNotNull(label.getDisabledIcon());
        assertNotSame(icon, label.getDisabledIcon());
        assertEquals("new instances should not be produced", label.getDisabledIcon(), label
                .getDisabledIcon());
        label.setDisabledIcon(null);
        assertNotNull(label.getDisabledIcon());
        label.setIcon(new Icon() {
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            }

            public int getIconWidth() {
                return 10;
            }

            public int getIconHeight() {
                return 10;
            }
        });
        assertNull(label.getDisabledIcon());
        TestPropertyChangeListener listener = new TestPropertyChangeListener();
        label.addPropertyChangeListener(listener);
        label.setDisabledIcon(icon);
        assertEquals(icon, label.getDisabledIcon());
        assertTrue(listener.isPropertyChanged("disabledIcon"));
    }

    public void testGetSetDisplayedMnemonic() throws Exception {
        listener.reset();
        label.setDisplayedMnemonic('a');
        assertEquals(KeyEvent.VK_A, label.getDisplayedMnemonic());
        label.setDisplayedMnemonic(KeyEvent.VK_B);
        assertEquals(KeyEvent.VK_B, label.getDisplayedMnemonic());
        assertTrue(listener.isPropertyChanged("displayedMnemonic"));
        label.setLabelFor(new JButton());
        assertNull(label.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(
                KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK)));
        assertNotNull(label.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(
                KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK)));
    }

    public void testGetSetDisplayedMnemonicIndex() throws Exception {
        listener.reset();
        label = new JLabel("abcd");
        label.addPropertyChangeListener(listener);
        assertEquals(-1, label.getDisplayedMnemonicIndex());
        label.setDisplayedMnemonic('a');
        assertEquals(0, label.getDisplayedMnemonicIndex());
        assertTrue(listener.isPropertyChanged("displayedMnemonicIndex"));
        label.setDisplayedMnemonic('e');
        assertEquals(-1, label.getDisplayedMnemonicIndex());
        assertTrue(listener.isPropertyChanged("displayedMnemonicIndex"));
        listener.reset();
        label.setDisplayedMnemonicIndex(3);
        assertEquals(3, label.getDisplayedMnemonicIndex());
        assertTrue(listener.isPropertyChanged("displayedMnemonicIndex"));
        label.setDisplayedMnemonicIndex(-1);
        assertEquals(-1, label.getDisplayedMnemonicIndex());
        label.setText(null);
        label.setDisplayedMnemonicIndex(-1);
        assertEquals(-1, label.getDisplayedMnemonicIndex());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                label.setDisplayedMnemonicIndex(5);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                label.setDisplayedMnemonicIndex(-10);
            }
        });
    }

    public void testGetSetHorizontalAlignment() throws Exception {
        assertEquals(SwingConstants.LEADING, label.getHorizontalAlignment());
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        assertEquals(SwingConstants.RIGHT, label.getHorizontalAlignment());
        assertTrue(listener.isPropertyChanged("horizontalAlignment"));
        label = new JLabel(createTestIcon());
        assertEquals(SwingConstants.CENTER, label.getHorizontalAlignment());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                label.setHorizontalAlignment(SwingConstants.BOTTOM);
            }
        });
    }

    public void testGetSetHorizontalTextPosition() throws Exception {
        assertEquals(SwingConstants.TRAILING, label.getHorizontalTextPosition());
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
        assertEquals(SwingConstants.RIGHT, label.getHorizontalTextPosition());
        assertTrue(listener.isPropertyChanged("horizontalTextPosition"));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                label.setHorizontalTextPosition(SwingConstants.BOTTOM);
            }
        });
    }

    public void testGetSetIconTextGap() throws Exception {
        assertEquals(4, label.getIconTextGap());
        label.setIconTextGap(7);
        assertEquals(7, label.getIconTextGap());
        assertTrue(listener.isPropertyChanged("iconTextGap"));
    }

    public void testGetSetLabelFor() throws Exception {
        assertNull(label.getLabelFor());
        JComponent c = new JPanel();
        label.setLabelFor(c);
        assertEquals(c, label.getLabelFor());
        assertTrue(listener.isPropertyChanged("labelFor"));
    }

    public void testGetSetText() throws Exception {
        assertEquals("", label.getText());
        String text = "any";
        label.setText(text);
        assertEquals(text, label.getText());
        assertTrue(listener.isPropertyChanged("text"));
        label = new JLabel(text);
        assertEquals(text, label.getText());
        label.setDisplayedMnemonic('y');
        assertEquals('Y', label.getDisplayedMnemonic());
        assertEquals(2, label.getDisplayedMnemonicIndex());
        label.setText("handy");
        assertEquals('Y', label.getDisplayedMnemonic());
        assertEquals(4, label.getDisplayedMnemonicIndex());
        label.setText("ok");
        assertEquals('Y', label.getDisplayedMnemonic());
        assertEquals(-1, label.getDisplayedMnemonicIndex());
    }

    public void testGetSetUpdateUI() throws Exception {
        LabelUI defaultUI = label.getUI();
        assertNotNull(defaultUI);
        LabelUI ui = new LabelUI() {
        };
        label.setUI(ui);
        assertEquals(ui, label.getUI());
        label.updateUI();
        assertEquals(defaultUI, label.getUI());
    }

    public void testGetUIClassID() throws Exception {
        assertEquals("LabelUI", label.getUIClassID());
    }

    public void testGetSetVerticalAlignment() throws Exception {
        assertEquals(SwingConstants.CENTER, label.getVerticalAlignment());
        label.setVerticalAlignment(SwingConstants.TOP);
        assertEquals(SwingConstants.TOP, label.getVerticalAlignment());
        assertTrue(listener.isPropertyChanged("verticalAlignment"));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                label.setVerticalAlignment(SwingConstants.RIGHT);
            }
        });
    }

    public void testGetSetVerticalTextPosition() throws Exception {
        assertEquals(SwingConstants.CENTER, label.getVerticalTextPosition());
        label.setVerticalTextPosition(SwingConstants.TOP);
        assertEquals(SwingConstants.TOP, label.getVerticalTextPosition());
        assertTrue(listener.isPropertyChanged("verticalTextPosition"));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                label.setVerticalAlignment(SwingConstants.RIGHT);
            }
        });
    }

    public void testImageUpdate() throws Exception {
        Icon icon = createTestIcon();
        label.setIcon(icon);
        assertFalse(label.imageUpdate(((ImageIcon) createTestIcon()).getImage(), 0, 0, 0, 0, 0));
    }

    public void testMnemonicProcessing() throws Exception {
        final JFrame frame = new JFrame();
        final JLabel label = new JLabel("label");
        final JButton button = new JButton("button");
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setSize(100, 100);
                frame.setLocation(100, 100);
                frame.getContentPane().add(label);
                frame.getContentPane().add(button);
                label.setLabelFor(button);
                label.setDisplayedMnemonic(KeyEvent.VK_A);
                label.setFocusable(true);
                frame.setVisible(true);
                label.requestFocus();
            }
        });
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                assertEquals(label, KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getFocusOwner());
                label
                        .dispatchEvent(new KeyEvent(label, KeyEvent.KEY_PRESSED, EventQueue
                                .getMostRecentEventTime(), InputEvent.ALT_DOWN_MASK,
                                KeyEvent.VK_A, 'a'));
                label
                        .dispatchEvent(new KeyEvent(label, KeyEvent.KEY_RELEASED, EventQueue
                                .getMostRecentEventTime(), InputEvent.ALT_DOWN_MASK,
                                KeyEvent.VK_A, 'a'));
            }
        });
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                assertEquals(button, KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getFocusOwner());
            }
        });
        frame.dispose();
    }

    public void testGetInheritsPopupMenu() throws Exception {
        // Regression test for HARMONY-2570
        assertTrue(label.getInheritsPopupMenu());
    }

    private void checkHorizontalKey(final JLabel label, final int key) {
        final String message = "any";
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                label.checkHorizontalKey(key, message);
            }

            @Override
            public String expectedExceptionMessage() {
                return message;
            }
        });
    }

    private void checkVerticalKey(final JLabel label, final int key) {
        final String message = "any";
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                label.checkVerticalKey(key, message);
            }

            @Override
            public String expectedExceptionMessage() {
                return message;
            }
        });
    }

    private Icon createTestIcon() {
        return new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_BGR));
    }

    private class TestPropertyChangeListener implements PropertyChangeListener {
        private Set<String> changedPropertyNames = new HashSet<String>();

        public void propertyChange(final PropertyChangeEvent event) {
            changedPropertyNames.add(event.getPropertyName());
        }

        public void reset() {
            changedPropertyNames.clear();
        }

        public boolean isPropertyChanged(final String name) {
            return changedPropertyNames.contains(name);
        }
    }
}
