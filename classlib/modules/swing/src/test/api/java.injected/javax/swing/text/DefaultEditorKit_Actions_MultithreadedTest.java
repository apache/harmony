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
 * Created on 05.03.2005

 */
package javax.swing.text;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import javax.swing.Action;
import javax.swing.BasicSwingTestCase;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

public class DefaultEditorKit_Actions_MultithreadedTest extends BasicSwingTestCase {
    protected DefaultEditorKit kit = null;

    protected JFrame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        kit = new DefaultEditorKit();
    }

    @Override
    protected void tearDown() throws Exception {
        kit = null;
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        super.tearDown();
    }

    protected Action getAction(final String actionName) {
        Action[] actions = kit.getActions();
        for (int i = 0; i < actions.length; i++) {
            if (actionName.equals(actions[i].getValue(Action.NAME))) {
                return actions[i];
            }
        }
        return null;
    }

    protected void performAction(final Object source, final Action action, final String command)
            throws InterruptedException, InvocationTargetException {
        final ActionEvent actionEvent = new ActionEvent(source, ActionEvent.ACTION_PERFORMED,
                command);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                action.actionPerformed(actionEvent);
            }
        });
    }

    protected void putStringToClipboard(final String str) throws InterruptedException,
            InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final Clipboard systemClipboard = Toolkit.getDefaultToolkit()
                        .getSystemClipboard();
                if (systemClipboard == null) {
                    fail("unable to get systemClipboard");
                }
                systemClipboard.setContents(new StringSelection(str), null);
            }
        });
    }

    protected String getStringFromClipboard() throws InterruptedException,
            InvocationTargetException {
        class ResultableThread implements Runnable {
            public String result;

            public void run() {
                try {
                    final Clipboard systemClipboard = Toolkit.getDefaultToolkit()
                            .getSystemClipboard();
                    if (systemClipboard == null) {
                        fail("unable to get systemClipboard");
                    }
                    result = (String) systemClipboard.getContents(null).getTransferData(
                            DataFlavor.stringFlavor);
                } catch (HeadlessException e) {
                    fail(e.getMessage());
                } catch (UnsupportedFlavorException e) {
                    fail(e.getMessage());
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
        }
        ;
        ResultableThread thread = new ResultableThread();
        SwingUtilities.invokeAndWait(thread);
        return thread.result;
    }

    protected void performAction(final Object source, final Action action)
            throws InterruptedException, InvocationTargetException {
        performAction(source, action, "command");
    }

    private JTextArea initComponent(final JTextArea c, final int startPos, final int endPos,
            final String text) throws Exception {
        if (frame != null) {
            frame.dispose();
        }
        c.setText(text);
        frame = new JFrame();
        JScrollPane scroll = new JScrollPane(c);
        ((JViewport) c.getParent()).setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        int strHeight = c.getFontMetrics(c.getFont()).getHeight();
        scroll.setPreferredSize(new Dimension(300, strHeight * 5));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        frame.getContentPane().add(scroll);
        frame.pack();
        if (!isHarmony()) {
            frame.setVisible(true);
            waitForIdle();
        }
        java.awt.EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                c.setCaretPosition(startPos);
                if (endPos >= 0) {
                    c.moveCaretPosition(endPos);
                }
            }
        });
        return c;
    }

    protected JTextArea getInitedComponent(final int startPos, final int endPos,
            final String text) throws Exception {
        JTextArea c = createTextArea();
        return initComponent(c, startPos, endPos, text);
    }

    protected JTextArea getInitedComponent(final int caretPos, final String text)
            throws Exception {
        JTextArea c = createTextArea();
        return initComponent(c, caretPos, -1, text);
    }

    private JTextArea createTextArea() {
        return new JTextArea() {
            private static final long serialVersionUID = 1L;

            @Override
            public FontMetrics getFontMetrics(final Font f) {
                return DefaultEditorKit_Actions_MultithreadedTest.this.getFontMetrics(f, 6);
            };
        };
    }

    public void testUnselectActionPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.unselectAction);
        JTextArea c = getInitedComponent(10, 15, text);
        assertEquals("selected text ", "89\nas", c.getSelectedText());
        performAction(c, action);
        assertNull("selected text ", c.getSelectedText());
    }

    public void testToggleComponentOrientationPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.toggleComponentOrientationAction);
        JTextArea c = getInitedComponent(10, 15, text);
        assertTrue("component is horizontal", c.getComponentOrientation().isHorizontal());
        assertTrue("component is LR", c.getComponentOrientation().isLeftToRight());
        performAction(c, action);
        assertTrue("component is horizontal", c.getComponentOrientation().isHorizontal());
        assertFalse("component is RL", c.getComponentOrientation().isLeftToRight());
    }

    public void testDumpModelActionPerformed() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldErr = System.err;
        System.setErr(new PrintStream(out));
        try {
            String text = "012345  6789\nasfd\nasd  asd";
            Action action = getAction("dump-model");
            JTextArea c = getInitedComponent(10, 15, text);
            performAction(c, action);
            assertEquals("<paragraph>\n" + "  <content>\n" + "    [0,13][012345  6789\n"
                    + "]\n" + "  <content>\n" + "    [13,18][asfd\n" + "]\n" + "  <content>\n"
                    + "    [18,27][asd  asd\n" + "]\n" + "<bidi root>\n" + "  <bidi level\n"
                    + "    bidiLevel=0\n" + "  >\n" + "    [0,27][012345  6789\n" + "asfd\n"
                    + "asd  asd\n" + "]\n", AbstractDocumentTest.filterNewLines(out.toString()));
        } finally {
            System.setErr(oldErr);
        }
    }

    public void testPageActionPerformed() throws Exception {
        String text = "01\n23\n45\n677777777777777777777777777777777777777777777777777\n89\n0-\nqwe\nrty\nasd\n\n\n\n\nzxc\nvbn";
        Action action = getAction("selection-page-right");
        JTextArea c = getInitedComponent(3, 7, text);
        performAction(c, action);
        assertEquals("selected string",
                "23\n45\n677777777777777777777777777777777777777777777777777\n89", c
                        .getSelectedText());
        assertEquals("caret position", 63, c.getCaretPosition());
        performAction(c, action);
        assertEquals("selected string",
                "23\n45\n677777777777777777777777777777777777777777777777777\n89", c
                        .getSelectedText());
        assertEquals("caret position", 63, c.getCaretPosition());
        action = getAction("selection-page-left");
        c = getInitedComponent(60, text);
        performAction(c, action);
        assertEquals("selected string",
                "01\n23\n45\n677777777777777777777777777777777777777777777777777", c
                        .getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(10, text);
        performAction(c, action);
        assertEquals("selected string", "01\n23\n45\n6", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        performAction(c, action);
        assertEquals("selected string", "01\n23\n45\n6", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
    }

    public void testVerticalPageActionPerformed() throws Exception {
        String text = "111111111111111\n2\n3\n44444444\n55555555555\n6\n7\n8\n9\n0\n1\n2\n3\n4\n5555555555555555555";
        Action action = getAction(DefaultEditorKit.pageDownAction);
        JTextArea c = getInitedComponent(4, 6, text);
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 35, c.getCaretPosition());
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 48, c.getCaretPosition());
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 56, c.getCaretPosition());
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 78, c.getCaretPosition());
        action = getAction(DefaultEditorKit.pageUpAction);
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 50, c.getCaretPosition());
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 35, c.getCaretPosition());
        action = getAction(DefaultEditorKit.pageUpAction);
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 6, c.getCaretPosition());
        action = getAction(DefaultEditorKit.pageUpAction);
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 6, c.getCaretPosition());
        action = getAction(DefaultEditorKit.pageDownAction);
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 35, c.getCaretPosition());
        action = getAction(DefaultEditorKit.pageUpAction);
        c = getInitedComponent(0, text);
        performAction(c, action, null);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionPageDownAction);
        c = getInitedComponent(3, 7, text);
        performAction(c, action);
        assertEquals("selected string", "111111111111\n2\n3\n44444444\n5555555", c
                .getSelectedText());
        assertEquals("caret position", 36, c.getCaretPosition());
        performAction(c, action);
        assertEquals("selected string",
                "111111111111\n2\n3\n44444444\n55555555555\n6\n7\n8\n9", c.getSelectedText());
        assertEquals("caret position", 48, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionPageUpAction);
        c = getInitedComponent(19, text);
        performAction(c, action);
        assertEquals("selected string", "11111111111111\n2\n3", c.getSelectedText());
        assertEquals("caret position", 1, c.getCaretPosition());
        c = getInitedComponent(10, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 10, c.getCaretPosition());
    }

    public void testWritableActionPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.writableAction);
        JTextArea c = getInitedComponent(13, 15, text);
        c.setEditable(false);
        assertFalse("component is now read-only ", c.isEditable());
        performAction(c, action);
        assertTrue("component is now writable ", c.isEditable());
    }

    public void testReadOnlyActionPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.readOnlyAction);
        JTextArea c = getInitedComponent(13, 15, text);
        assertTrue("component is now writable ", c.isEditable());
        performAction(c, action);
        assertFalse("component is now read-only ", c.isEditable());
    }

    public void testEndParagraphActionPerformed() throws Exception {
        String text = "\t012345  6789\nasfd\n\n\tasd  asd";
        Action action = getAction(DefaultEditorKit.endParagraphAction);
        JTextArea c = getInitedComponent(6, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 14, c.getCaretPosition());
        c = getInitedComponent(16, text);
        performAction(c, action);
        assertNull("resulted string", c.getSelectedText());
        assertEquals("caret position", 19, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionEndParagraphAction);
        c = getInitedComponent(6, text);
        performAction(c, action);
        assertEquals("selected string", "5  6789\n", c.getSelectedText());
        assertEquals("caret position", 14, c.getCaretPosition());
        c = getInitedComponent(16, text);
        performAction(c, action);
        assertEquals("resulted string", "fd\n", c.getSelectedText());
        assertEquals("caret position", 19, c.getCaretPosition());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("resulted string", c.getSelectedText());
    }

    public void testBeginParagraphActionPerformed() throws Exception {
        String text = "\t012345  6789\nasfd\n\n\tasd  asd";
        Action action = getAction(DefaultEditorKit.beginParagraphAction);
        JTextArea c = getInitedComponent(6, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(16, text);
        performAction(c, action);
        assertNull("resulted string", c.getSelectedText());
        assertEquals("caret position", 14, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionBeginParagraphAction);
        c = getInitedComponent(6, text);
        performAction(c, action);
        assertEquals("selected string", "\t01234", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(16, text);
        performAction(c, action);
        assertEquals("resulted string", "as", c.getSelectedText());
        assertEquals("caret position", 14, c.getCaretPosition());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("resulted string", c.getSelectedText());
    }

    public void testBeginWordActionPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.beginWordAction);
        JTextArea c = getInitedComponent(13, 15, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 13, c.getCaretPosition());
        c = getInitedComponent(1, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(0, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionBeginWordAction);
        c = getInitedComponent(13, 17, text);
        performAction(c, action);
        assertEquals("selected string", "asfd", c.getSelectedText());
        assertEquals("caret position", 17, c.getCaretPosition());
        c = getInitedComponent(15, 17, text);
        performAction(c, action);
        assertEquals("selected string", "fd", c.getSelectedText());
        assertEquals("caret position", 17, c.getCaretPosition());
        c = getInitedComponent(2, 1, text);
        performAction(c, action);
        assertEquals("selected string", "01", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(0, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
    }

    public void testEndWordActionPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.endWordAction);
        JTextArea c = getInitedComponent(13, 15, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 17, c.getCaretPosition());
        c = getInitedComponent(25, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 26, c.getCaretPosition());
        c = getInitedComponent(26, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 26, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionEndWordAction);
        c = getInitedComponent(14, 16, text);
        performAction(c, action);
        assertEquals("selected string", "sfd", c.getSelectedText());
        assertEquals("caret position", 17, c.getCaretPosition());
        c = getInitedComponent(24, 25, text);
        performAction(c, action);
        assertEquals("selected string", "sd", c.getSelectedText());
        assertEquals("caret position", 26, c.getCaretPosition());
        c = getInitedComponent(26, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 26, c.getCaretPosition());
    }

    public void testPreviousWordActionPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.previousWordAction);
        JTextArea c = getInitedComponent(13, 15, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 13, c.getCaretPosition());
        c = getInitedComponent(1, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(0, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionPreviousWordAction);
        c = getInitedComponent(13, 17, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 13, c.getCaretPosition());
        c = getInitedComponent(15, 17, text);
        performAction(c, action);
        assertEquals("selected string", "as", c.getSelectedText());
        assertEquals("caret position", 13, c.getCaretPosition());
        c = getInitedComponent(2, 1, text);
        performAction(c, action);
        assertEquals("selected string", "01", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(0, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
    }

    public void testNextWordActionPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.nextWordAction);
        JTextArea c = getInitedComponent(13, 15, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 18, c.getCaretPosition());
        c = getInitedComponent(25, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 26, c.getCaretPosition());
        c = getInitedComponent(26, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 26, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionNextWordAction);
        c = getInitedComponent(14, 16, text);
        performAction(c, action);
        assertEquals("selected string", "sfd\n", c.getSelectedText());
        assertEquals("caret position", 18, c.getCaretPosition());
        c = getInitedComponent(24, 25, text);
        performAction(c, action);
        assertEquals("selected string", "sd", c.getSelectedText());
        assertEquals("caret position", 26, c.getCaretPosition());
        c = getInitedComponent(26, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 26, c.getCaretPosition());
    }

    public void testBeginLineActionPerformed() throws Exception {
        Action action = getAction(DefaultEditorKit.beginLineAction);
        String text = "0123456789\n12341234\n12341234";
        JTextArea c = getInitedComponent(14, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 11, c.getCaretPosition());
        c = getInitedComponent(8, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionBeginLineAction);
        c = getInitedComponent(8, 3, text);
        performAction(c, action);
        assertEquals("selected string", "01234567", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
    }

    public void testEndLineActionPerformed() throws Exception {
        Action action = getAction(DefaultEditorKit.endLineAction);
        String text = "0123456789\n12341234\n12341234";
        JTextArea c = getInitedComponent(14, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 19, c.getCaretPosition());
        c = getInitedComponent(10, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 10, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionEndLineAction);
        c = getInitedComponent(3, 8, text);
        performAction(c, action);
        assertEquals("selected string", "3456789", c.getSelectedText());
        assertEquals("caret position", 10, c.getCaretPosition());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
    }

    public void testEndActionPerformed() throws Exception {
        Action action = getAction(DefaultEditorKit.endAction);
        String text = "0123456789\n12341234\n12341234";
        JTextArea c = getInitedComponent(8, 14, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 28, c.getCaretPosition());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionEndAction);
        c = getInitedComponent(12, 14, text);
        performAction(c, action);
        assertEquals("selected string", "2341234\n12341234", c.getSelectedText());
        assertEquals("caret position", 28, c.getCaretPosition());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
    }

    public void testBeginActionPerformed() throws Exception {
        Action action = getAction(DefaultEditorKit.beginAction);
        String text = "0123456789\n12341234\n12341234";
        JTextArea c = getInitedComponent(8, 14, text);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        action = getAction(DefaultEditorKit.selectionBeginAction);
        c = getInitedComponent(16, 14, text);
        performAction(c, action);
        assertEquals("selected string", "0123456789\n12341", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("selected string", c.getSelectedText());
        assertEquals("caret position", 0, c.getCaretPosition());
    }

    public void testSelectWordActionPerformed() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.selectWordAction);
        JTextArea c = getInitedComponent(15, text);
        performAction(c, action);
        assertEquals("resulted string", "asfd", c.getSelectedText());
        c = getInitedComponent(9, text);
        performAction(c, action);
        assertEquals("resulted string", "6789", c.getSelectedText());
        c = getInitedComponent(7, text);
        performAction(c, action);
        assertEquals("resulted string", "  ", c.getSelectedText());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("resulted string", c.getSelectedText());
    }

    public void testSelectLineActionPerformed() throws Exception {
        String text = "0123  456789\nasdf";
        Action action = getAction(DefaultEditorKit.selectLineAction);
        JTextArea c = getInitedComponent(5, text);
        performAction(c, action);
        assertEquals("resulted string", "0123  456789", c.getSelectedText());
        c = getInitedComponent(14, text);
        performAction(c, action);
        assertEquals("resulted string", "asdf", c.getSelectedText());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("resulted string", c.getSelectedText());
    }

    public void testSelectParagraphActionPerformed() throws Exception {
        String text = "\t012345  6789\nasfd\n\n\tasd  asd";
        Action action = getAction(DefaultEditorKit.selectParagraphAction);
        JTextArea c = getInitedComponent(6, text);
        performAction(c, action);
        String res = "\t012345  6789" + "\n";
        assertEquals("resulted string", res, c.getSelectedText());
        c = getInitedComponent(15, text);
        performAction(c, action);
        res = "asfd" + "\n";
        assertEquals("resulted string", res, c.getSelectedText());
        c = getInitedComponent(20, text);
        performAction(c, action);
        res = "\tasd  asd";
        assertEquals("resulted string", res, c.getSelectedText());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("resulted string", c.getSelectedText());
    }

    public void testSelectAllActionPerformed() throws Exception {
        String text = "0123456789\nasdasd";
        Action action = getAction(DefaultEditorKit.selectAllAction);
        JTextArea c = getInitedComponent(2, 7, text);
        performAction(c, action);
        assertEquals("resulted string", text, c.getSelectedText());
        c = getInitedComponent(0, null);
        performAction(c, action);
        assertNull("resulted string", c.getSelectedText());
    }

    public void testDeleteNextCharActionPerformed() throws Exception {
        Action action = getAction(DefaultEditorKit.deleteNextCharAction);
        JTextArea c = getInitedComponent(2, 7, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "01789", c.getText());
        c = getInitedComponent(3, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "012456789", c.getText());
        c = getInitedComponent(10, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "0123456789", c.getText());
        c = getInitedComponent(2, 10, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "01", c.getText());
    }

    public void testDeletePrevCharActionPerformed() throws Exception {
        Action action = getAction(DefaultEditorKit.deletePrevCharAction);
        JTextArea c = getInitedComponent(2, 7, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "01789", c.getText());
        c = getInitedComponent(3, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "013456789", c.getText());
        c = getInitedComponent(0, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "0123456789", c.getText());
        c = getInitedComponent(5, 0, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "56789", c.getText());
    }

    public void testInsertContentActionPerformed() throws Exception {
        Action action = getAction(DefaultEditorKit.insertContentAction);
        JTextArea c = getInitedComponent(2, 7, "0123456789");
        performAction(c, action, "aaa");
        assertEquals("resulted string", "01aaa789", c.getText());
        c = getInitedComponent(2, 7, "0123456789");
        performAction(c, action, null);
        assertEquals("resulted string", "0123456789", c.getText());
        c = getInitedComponent(2, 7, "0123456789");
        performAction(c, action, "command\ncontent");
        assertEquals("resulted string", "01command\ncontent789", c.getText());
    }

    public void testCopyActionPerformed() throws Exception {
        // TODO: Uncomment when Clipboard is fully supported
        //        DefaultEditorKit.CopyAction action = new DefaultEditorKit.CopyAction();
        //        putStringToClipboard("");
        //        JTextArea c = getInitedComponent(2, 7, "0123456789");
        //        performAction(c, action);
        //        Object result = null;
        //        try {
        //            result = getStringFromClipboard();
        //        } catch (HeadlessException e) {
        //            fail(e.getMessage());
        //        }
        //        assertEquals("selected string", "23456", result);
    }

    public void testCutActionPerformed() throws Exception {
        // TODO: Uncomment when Clipboard is fully supported
        //        DefaultEditorKit.CutAction action = new DefaultEditorKit.CutAction();
        //        putStringToClipboard("");
        //        JTextArea c = getInitedComponent(2, 7, "0123456789");
        //        performAction(c, action);
        //        Object result = null;
        //        try {
        //            result = getStringFromClipboard();
        //        } catch (HeadlessException e) {
        //            fail(e.getMessage());
        //        }
        //        assertEquals("cut string", "23456", result);
        //        assertEquals("remained string", "01789", c.getText());
    }

    public void testPasteActionPerformed() throws Exception {
        // TODO: Uncomment when Clipboard is fully supported
        //        DefaultEditorKit.PasteAction action = new DefaultEditorKit.PasteAction();
        //        putStringToClipboard("98765");
        //        JTextArea c = getInitedComponent(2, 7, "0123456789");
        //        performAction(c, action);
        //        assertEquals("resulted string", "0198765789", c.getText());
    }

    public void testInsertTabActionPerformed() throws Exception {
        DefaultEditorKit.InsertTabAction action = new DefaultEditorKit.InsertTabAction();
        JTextArea c = getInitedComponent(2, 7, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "01\t789", c.getText());
    }

    public void testInsertBreakActionPerformed() throws Exception {
        DefaultEditorKit.InsertBreakAction action = new DefaultEditorKit.InsertBreakAction();
        JTextArea c = getInitedComponent(2, 7, "0123456789");
        performAction(c, action);
        assertEquals("resulted string", "01\n789", c.getText());
    }

    public void testDefaultKeyTypedActionPerformed() throws Exception {
        DefaultEditorKit.DefaultKeyTypedAction action = new DefaultEditorKit.DefaultKeyTypedAction();
        JTextArea c = getInitedComponent(2, 7, "0123456789");
        performAction(c, action, "asd");
        assertEquals("resulted string", "01asd789", c.getText());
    }

    public void testBeepActionPerformed() throws Exception {
        DefaultEditorKit.BeepAction action = new DefaultEditorKit.BeepAction();
        JComponent c = new JPanel();
        performAction(c, action);
    }

    public void testNextVisualPositionActionPerformedCaretForward() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.forwardAction);
        JTextArea c = getInitedComponent(8, text);
        performAction(c, action, null);
        assertEquals("caret position", 9, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(text.length(), text);
        performAction(c, action, null);
        assertEquals("caret position", text.length(), c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(5, 7, text);
        performAction(c, action, null);
        assertEquals("caret position", 8, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
    }

    public void testNextVisualPositionActionPerformedCaretBackward() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.backwardAction);
        JTextArea c = getInitedComponent(8, text);
        performAction(c, action);
        assertEquals("caret position", 7, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(0, text);
        performAction(c, action);
        assertEquals("caret position", 0, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(5, 7, text);
        performAction(c, action);
        assertEquals("caret position", 6, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
    }

    public void testNextVisualPositionActionPerformedSelectionForward() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.selectionForwardAction);
        JTextArea c = getInitedComponent(8, text);
        performAction(c, action);
        assertEquals("caret position", 9, c.getCaretPosition());
        assertEquals("selected text ", "6", c.getSelectedText());
        c = getInitedComponent(text.length(), text);
        performAction(c, action);
        assertEquals("caret position", text.length(), c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(3, 7, text);
        performAction(c, action);
        assertEquals("caret position", 8, c.getCaretPosition());
        assertEquals("selected text ", "345  ", c.getSelectedText());
    }

    public void testNextVisualPositionActionPerformedSelectionBackward() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.selectionBackwardAction);
        JTextArea c = getInitedComponent(8, text);
        performAction(c, action);
        assertEquals("caret position", 7, c.getCaretPosition());
        assertEquals("selected text ", " ", c.getSelectedText());
        c = getInitedComponent(0, text);
        performAction(c, action);
        assertEquals("caret position", 0, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(8, 5, text);
        performAction(c, action);
        assertEquals("caret position", 4, c.getCaretPosition());
        assertEquals("selected text ", "45  ", c.getSelectedText());
    }

    public void testNextVisualPositionActionPerformedCaretUp() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.upAction);
        JTextArea c = getInitedComponent(15, text);
        performAction(c, action);
        assertEquals("caret position", 2, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        performAction(c, action);
        assertEquals("caret position", 2, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(24, 26, text);
        performAction(c, action);
        assertEquals("caret position", 17, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
    }

    public void testNextVisualPositionActionPerformedCaretDown() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.downAction);
        JTextArea c = getInitedComponent(8, text);
        performAction(c, action);
        assertEquals("magic caret pos", new Point(48, 0), c.getCaret().getMagicCaretPosition());
        assertEquals("caret position", 17, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(text.length(), text);
        performAction(c, action);
        assertEquals("magic caret pos", 48, c.getCaret().getMagicCaretPosition().x);
        assertEquals("caret position", text.length(), c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(5, 7, text);
        performAction(c, action);
        assertEquals("magic caret pos", new Point(42, 0), c.getCaret().getMagicCaretPosition());
        performAction(c, action);
        assertEquals("magic caret pos", new Point(42, 0), c.getCaret().getMagicCaretPosition());
        assertEquals("caret position", 25, c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
    }

    public void testNextVisualPositionActionPerformedSelectionUp() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.selectionUpAction);
        JTextArea c = getInitedComponent(15, text);
        performAction(c, action);
        assertEquals("caret position", 2, c.getCaretPosition());
        assertEquals("selected text ", "2345  6789\nas", c.getSelectedText());
        performAction(c, action);
        assertEquals("caret position", 2, c.getCaretPosition());
        assertEquals("selected text ", "2345  6789\nas", c.getSelectedText());
        c = getInitedComponent(26, 24, text);
        performAction(c, action);
        assertEquals("caret position", 17, c.getCaretPosition());
        assertEquals("selected text ", "\nasd  asd", c.getSelectedText());
    }

    public void testNextVisualPositionActionPerformedSelectionDown() throws Exception {
        String text = "012345  6789\nasfd\nasd  asd";
        Action action = getAction(DefaultEditorKit.selectionDownAction);
        JTextArea c = getInitedComponent(8, text);
        performAction(c, action);
        assertEquals("caret position", 17, c.getCaretPosition());
        assertEquals("selected text ", "6789\nasfd", c.getSelectedText());
        c = getInitedComponent(text.length(), text);
        performAction(c, action);
        assertEquals("caret position", text.length(), c.getCaretPosition());
        assertNull("selected text ", c.getSelectedText());
        c = getInitedComponent(5, 7, text);
        performAction(c, action);
        performAction(c, action);
        assertEquals("caret position", 25, c.getCaretPosition());
        assertEquals("selected text ", "5  6789\nasfd\nasd  as", c.getSelectedText());
    }

    public void testDefaultKeyTypedActionFiltering() throws Exception {
        HashSet<Character> nonTypingChars = new HashSet<Character>();
        for (char i = 0; i < 32; i++) {
            nonTypingChars.add(new Character(i));
        }
        nonTypingChars.add(new Character((char) 127));
        DefaultEditorKit.DefaultKeyTypedAction action = new DefaultEditorKit.DefaultKeyTypedAction();
        JTextArea c = getInitedComponent(4, 5, "0123456789");
        String prevText = c.getText();
        for (char i = 0; i < 255; i++) {
            performAction(c, action, String.valueOf(i));
            if (prevText.equals(c.getText())) {
                if (!nonTypingChars.contains(new Character(i))) {
                    fail("regular character haven't been typed by DEK. code is: " + (int) i);
                }
            } else {
                prevText = c.getText();
                if (nonTypingChars.contains(new Character(i))) {
                    fail("non-typing character have been typed by DEK. code is: " + (int) i);
                }
            }
        }
        assertEquals("resulted string length", 231, c.getText().length());
    }

    public void testConstants() {
        assertEquals("caret-backward", DefaultEditorKit.backwardAction);
        assertEquals("beep", DefaultEditorKit.beepAction);
        assertEquals("caret-begin", DefaultEditorKit.beginAction);
        assertEquals("caret-begin", DefaultEditorKit.beginAction);
        assertEquals("caret-begin-line", DefaultEditorKit.beginLineAction);
        assertEquals("caret-begin-paragraph", DefaultEditorKit.beginParagraphAction);
        assertEquals("caret-begin-word", DefaultEditorKit.beginWordAction);
        assertEquals("copy-to-clipboard", DefaultEditorKit.copyAction);
        assertEquals("cut-to-clipboard", DefaultEditorKit.cutAction);
        assertEquals("default-typed", DefaultEditorKit.defaultKeyTypedAction);
        assertEquals("delete-next", DefaultEditorKit.deleteNextCharAction);
        assertEquals("delete-previous", DefaultEditorKit.deletePrevCharAction);
        assertEquals("caret-down", DefaultEditorKit.downAction);
        assertEquals("dump-model", DefaultEditorKit.dumpModelAction);
        assertEquals("caret-end", DefaultEditorKit.endAction);
        assertEquals("caret-end-line", DefaultEditorKit.endLineAction);
        assertEquals("__EndOfLine__", DefaultEditorKit.EndOfLineStringProperty);
        assertEquals("caret-end-paragraph", DefaultEditorKit.endParagraphAction);
        assertEquals("caret-end-word", DefaultEditorKit.endWordAction);
        assertEquals("caret-forward", DefaultEditorKit.forwardAction);
        assertEquals("insert-break", DefaultEditorKit.insertBreakAction);
        assertEquals("insert-content", DefaultEditorKit.insertContentAction);
        assertEquals("insert-tab", DefaultEditorKit.insertTabAction);
        assertEquals("caret-next-word", DefaultEditorKit.nextWordAction);
        assertEquals("page-down", DefaultEditorKit.pageDownAction);
        assertEquals("page-up", DefaultEditorKit.pageUpAction);
        assertEquals("paste-from-clipboard", DefaultEditorKit.pasteAction);
        assertEquals("caret-previous-word", DefaultEditorKit.previousWordAction);
        assertEquals("set-read-only", DefaultEditorKit.readOnlyAction);
        assertEquals("select-all", DefaultEditorKit.selectAllAction);
        assertEquals("selection-backward", DefaultEditorKit.selectionBackwardAction);
        assertEquals("selection-begin", DefaultEditorKit.selectionBeginAction);
        assertEquals("selection-begin-line", DefaultEditorKit.selectionBeginLineAction);
        assertEquals("selection-begin-paragraph",
                DefaultEditorKit.selectionBeginParagraphAction);
        assertEquals("selection-begin-word", DefaultEditorKit.selectionBeginWordAction);
        assertEquals("selection-down", DefaultEditorKit.selectionDownAction);
        assertEquals("selection-end", DefaultEditorKit.selectionEndAction);
        assertEquals("selection-end-line", DefaultEditorKit.selectionEndLineAction);
        assertEquals("selection-end-paragraph", DefaultEditorKit.selectionEndParagraphAction);
        assertEquals("selection-end-word", DefaultEditorKit.selectionEndWordAction);
        assertEquals("selection-forward", DefaultEditorKit.selectionForwardAction);
        assertEquals("selection-next-word", DefaultEditorKit.selectionNextWordAction);
        assertEquals("selection-page-down", DefaultEditorKit.selectionPageDownAction);
        assertEquals("selection-page-left", DefaultEditorKit.selectionPageLeftAction);
        assertEquals("selection-page-right", DefaultEditorKit.selectionPageRightAction);
        assertEquals("selection-page-up", DefaultEditorKit.selectionPageUpAction);
        assertEquals("selection-previous-word", DefaultEditorKit.selectionPreviousWordAction);
        assertEquals("selection-up", DefaultEditorKit.selectionUpAction);
        assertEquals("select-line", DefaultEditorKit.selectLineAction);
        assertEquals("select-paragraph", DefaultEditorKit.selectParagraphAction);
        assertEquals("select-word", DefaultEditorKit.selectWordAction);
        assertEquals("toggle-componentOrientation",
                DefaultEditorKit.toggleComponentOrientationAction);
        assertEquals("unselect", DefaultEditorKit.unselectAction);
        assertEquals("caret-up", DefaultEditorKit.upAction);
        assertEquals("set-writable", DefaultEditorKit.writableAction);
    }
}