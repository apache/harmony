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
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.StyleContext.NamedStyle;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument class: methods related to style management and
 * getting standard attributes from an AttributeSet.
 *
 */
public class DefaultStyledDocument_StylesAndStdAttrsTest extends TestCase {
    private DefaultStyledDocument doc;

    private StyleContext styles;

    private AttributeSet attrSet;

    private boolean getBackground;

    private boolean getFont;

    private boolean getForeground;

    private static final String STYLE_NAME = "aStyle";

    public void testAddStyle() {
        Style aStyle = doc.addStyle(STYLE_NAME, null);
        assertSame(styles.getStyle(STYLE_NAME), aStyle);
    }

    public void testGetBackground() {
        assertFalse(getBackground);
        doc.getBackground(attrSet);
        assertTrue(getBackground);
    }

    public void testGetFont() {
        assertFalse(getFont);
        doc.getFont(attrSet);
        assertTrue(getFont);
    }

    public void testGetForeground() {
        assertFalse(getForeground);
        doc.getForeground(attrSet);
        assertTrue(getForeground);
    }

    public void testGetStyle() {
        Style aStyle = styles.addStyle(STYLE_NAME, null);
        assertSame(aStyle, doc.getStyle(STYLE_NAME));
    }

    public void testGetStyleNames() {
        final String[] names = new String[] { "one", "two", "three" };
        for (int i = 0; i < names.length; i++) {
            styles.addStyle(names[i], null);
        }
        boolean[] found = new boolean[names.length];
        Enumeration<?> styleNames = doc.getStyleNames();
        while (styleNames.hasMoreElements()) {
            Object name = styleNames.nextElement();
            for (int i = 0; i < names.length; i++) {
                found[i] = found[i] || name.equals(names[i]);
            }
        }
        for (int i = 0; i < found.length; i++) {
            assertTrue("@ " + i, found[i]);
        }
    }

    public void testRemoveStyle() {
        doc.addStyle(STYLE_NAME, null);
        assertNotNull(doc.getStyle(STYLE_NAME));
        doc.removeStyle(STYLE_NAME);
        assertNull(doc.getStyle(STYLE_NAME));
    }

    public void testStyleChanged() {
        final Style[] changed = new Style[1];
        doc = new DefaultStyledDocument() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void styleChanged(final Style style) {
                changed[0] = style;
                super.styleChanged(style);
            }
        };
        styles = (StyleContext) doc.getAttributeContext();
        final NamedStyle aStyle = (NamedStyle) doc.addStyle(STYLE_NAME, null);
        assertEquals(0, styles.getChangeListeners().length);
        assertEquals(0, aStyle.listenerList.getListenerCount());
        final DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
            }

            public void removeUpdate(DocumentEvent e) {
            }

            public void changedUpdate(DocumentEvent e) {
            }
        };
        doc.addDocumentListener(listener);
        assertEquals(1, styles.getChangeListeners().length);
        assertEquals(1, aStyle.listenerList.getListenerCount());
        final NamedStyle anotherStyle = (NamedStyle) styles.addStyle("otherStyle", aStyle);
        assertEquals(1, anotherStyle.listenerList.getListenerCount());
        final NamedStyle nullStyle = (NamedStyle) styles.addStyle(null, null);
        assertEquals(0, nullStyle.listenerList.getListenerCount());
        assertNull(changed[0]);
        aStyle.addAttribute("key", "value");
        assertSame(aStyle, changed[0]);
        doc.removeDocumentListener(listener);
        assertEquals(0, styles.getChangeListeners().length);
        assertEquals(0, aStyle.listenerList.getListenerCount());
        assertEquals(0, anotherStyle.listenerList.getListenerCount());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument(new StyleContext() {
            private static final long serialVersionUID = 1L;

            @Override
            public Color getBackground(AttributeSet as) {
                getBackground = true;
                return super.getBackground(as);
            }

            @Override
            public Font getFont(AttributeSet as) {
                getFont = true;
                return super.getFont(as);
            }

            @Override
            public Color getForeground(AttributeSet as) {
                getForeground = true;
                return super.getForeground(as);
            }
        });
        styles = (StyleContext) doc.getAttributeContext();
        attrSet = styles.getEmptySet();
    }
}
