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
 * @author Roman I. Chernyatchik
 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingTestCase;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

public class BasicTextPaneUITest extends SwingTestCase {
    JTextPane textPane;

    JFrame frame;

    BasicTextPaneUI ui;

    Font font = new Font("", Font.BOLD | Font.ITALIC, 25) {
        private static final long serialVersionUID = 1L;

        @Override
        public String getFamily() {
            return "My FontFamily";
        }

        @Override
        public String getFontName() {
            return "My FontName";
        }

        @Override
        public String getName() {
            return "My Name";
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        frame = new JFrame();
        textPane = new JTextPane();
        ui = (BasicTextPaneUI) textPane.getUI();
        frame.getContentPane().add(textPane);
        frame.setSize(200, 300);
        frame.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        frame.dispose();
        super.tearDown();
    }

    public void testCreateUI() {
        ComponentUI ui1 = BasicTextPaneUI.createUI(textPane);
        assertTrue(ui1 instanceof BasicTextPaneUI);
        ui1 = BasicTextPaneUI.createUI(new JTextField());
        ui1 = BasicTextPaneUI.createUI(new JTextArea());
        assertTrue(ui1 instanceof BasicTextPaneUI);
        assertTrue(ui1 instanceof BasicTextPaneUI);
        ComponentUI ui2 = BasicTextPaneUI.createUI(textPane);
        assertTrue(ui2 instanceof BasicTextPaneUI);
    }

    public void testGetPropertyPrefix() {
        assertEquals("TextPane", ui.getPropertyPrefix());
    }

    public void testPropertyChange() throws BadLocationException {
        Style style = textPane.getStyle(StyleContext.DEFAULT_STYLE);
        //Font
        assertFalse(25 == ((Integer) style.getAttribute(StyleConstants.FontSize)).intValue());
        textPane.setFont(font);
        assertEquals(25, ((Integer) style.getAttribute(StyleConstants.FontSize)).intValue());
        assertEquals(font.getName(), style.getAttribute(StyleConstants.FontFamily));
        assertFalse(font.getFontName().equals(style.getAttribute(StyleConstants.FontFamily)));
        assertEquals(font.getName(), style.getAttribute(StyleConstants.FontFamily));
        // Foreground
        assertFalse(Color.BLUE.equals(style.getAttribute(StyleConstants.Foreground)));
        textPane.setForeground(Color.BLUE);
        assertEquals(Color.BLUE, style.getAttribute(StyleConstants.Foreground));
        // Document
        style.addAttribute(StyleConstants.Subscript, Boolean.TRUE);
        StyledDocument newDoc = new DefaultStyledDocument();
        Style newStyle = newDoc.getStyle(StyleContext.DEFAULT_STYLE);
        assertNull(newStyle.getAttribute(StyleConstants.FontSize));
        assertNull(newStyle.getAttribute(StyleConstants.FontFamily));
        newStyle.addAttribute(StyleConstants.FontFamily, "family2");
        newStyle.addAttribute(StyleConstants.FontSize, new Integer(10));
        newStyle.addAttribute(StyleConstants.Italic, Boolean.FALSE);
        newStyle.addAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        newStyle.addAttribute(StyleConstants.Subscript, Boolean.FALSE);
        newStyle.addAttribute(StyleConstants.Foreground, Color.RED);
        textPane.setDocument(newDoc);
        assertNotSame(style, newStyle);
        assertEquals(25, ((Integer) newStyle.getAttribute(StyleConstants.FontSize)).intValue());
        assertEquals(font.getName(), newStyle.getAttribute(StyleConstants.FontFamily));
        assertEquals(Boolean.TRUE, newStyle.getAttribute(StyleConstants.Italic));
        assertEquals(Boolean.TRUE, newStyle.getAttribute(StyleConstants.Bold));
        assertEquals(Boolean.TRUE, newStyle.getAttribute(StyleConstants.StrikeThrough));
        assertEquals(Boolean.FALSE, newStyle.getAttribute(StyleConstants.Subscript));
        assertEquals(Color.BLUE, newStyle.getAttribute(StyleConstants.Foreground));
    }

    public void testProPertyChange_FontFamilyName() {
        Style style = textPane.getStyle(StyleContext.DEFAULT_STYLE);
        textPane.setFont(font);
        assertFalse(font.getFamily().equals(style.getAttribute(StyleConstants.FontFamily)));
        assertEquals(font.getName(), style.getAttribute(StyleConstants.FontFamily));
        StyledDocument newDoc = new DefaultStyledDocument();
        Style newStyle = newDoc.getStyle(StyleContext.DEFAULT_STYLE);
        textPane.setDocument(newDoc);
        assertFalse(font.getFamily().equals(newStyle.getAttribute(StyleConstants.FontFamily)));
        assertEquals(font.getName(), newStyle.getAttribute(StyleConstants.FontFamily));
    }
}
