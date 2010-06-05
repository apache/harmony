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
package javax.swing.text.html;

import java.awt.Graphics;
import java.awt.Shape;
import java.io.StringReader;

import javax.swing.BasicSwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabExpander;
import javax.swing.text.View;
import javax.swing.text.GlyphView.GlyphPainter;
import javax.swing.text.Position.Bias;

public class InlineViewTest extends BasicSwingTestCase {
    public static class FixedPainter extends GlyphPainter {
        public static final int CHAR_WIDTH = 7;

        public float getSpan(GlyphView v, int startOffset, int endOffset,
                             TabExpander tabExpander, float x) {
            return CHAR_WIDTH * (endOffset - startOffset);
        }

        public int getBoundedPosition(GlyphView v, int startOffset,
                                      float x, float len) {
            int result = (int)len / CHAR_WIDTH + startOffset;
            return result <= v.getEndOffset() ? result : v.getEndOffset();
        }

        public float getHeight(GlyphView v) {
            return 0;
        }
        public float getAscent(GlyphView v) {
            return 0;
        }
        public float getDescent(GlyphView v) {
            return 0;
        }
        public void paint(GlyphView v, Graphics g, Shape alloc,
                          int startOffset, int endOffset) {
        }
        public Shape modelToView(GlyphView v, int offset, Bias bias,
                                 Shape alloc) throws BadLocationException {
            return null;
        }
        public int viewToModel(GlyphView v, float x, float y,
                               Shape alloc, Bias[] biasReturn) {
            return -1;
        }
    }

    private class Event implements DocumentEvent {
        public int getOffset() {
            return inline.getStartOffset() + 1;
        }

        public int getLength() {
            return inline.getEndOffset() - inline.getStartOffset() - 2;
        }

        public Document getDocument() {
            return doc;
        }

        public EventType getType() {
            return null;
        }

        public ElementChange getChange(final Element elem) {
            return null;
        }
    }

    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private Element inline;
    private InlineView view;
    private AttributeSet attrs;

    private int startOffset;
    private String text;
    private int whitespace;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        kit = new HTMLEditorKit();
        doc = (HTMLDocument)kit.createDefaultDocument();
        StringReader reader = new StringReader("<html><head></head>" +
               "<body>" +
               "<p>Normal paragraph: <em>em inside</em>" +
               "   paragraph." +
               "</body></html>");
        kit.read(reader, doc, 0);

        inline = doc.getCharacterElement(20);
        assertNotNull(inline.getAttributes().getAttribute(HTML.Tag.EM));

        view = new InlineView(inline);
        attrs = view.getAttributes();

        startOffset = inline.getStartOffset();
        text = doc.getText(startOffset, inline.getEndOffset() - startOffset);
        whitespace = text.indexOf(' ');
    }

    public void testInlineView() {
        assertSame(inline, view.getElement());
        assertNotSame(inline.getAttributes(), view.getAttributes());
    }

    public void testInlineViewUponParagraph() throws BadLocationException {
        final Element paragraph = doc.getParagraphElement(20);
        view = new InlineView(paragraph);
        assertSame(paragraph, view.getElement());
        assertNotSame(paragraph.getAttributes(), view.getAttributes());
    }

    public void testInlineViewUponStyledDocument() throws BadLocationException {
        final StyledDocument styledDoc = new DefaultStyledDocument();
        styledDoc.insertString(0, "a simple paragraph", null);
        inline = styledDoc.getCharacterElement(1);
        testExceptionalCase(new ClassCastCase() {
            public void exceptionalAction() throws Exception {
                new InlineView(inline);
            }

            public String expectedExceptionMessage() {
                return styledDoc.getClass().getName();
            }
        });
    }

    public void testGetAttributes() {
        assertEquals(2, attrs.getAttributeCount());
        assertEquals("italic",
                     attrs.getAttribute(CSS.Attribute.FONT_STYLE).toString());
        assertEquals(HTML.Tag.EM.toString(),
                     attrs.getAttribute(AttributeSet.NameAttribute));

        assertTrue(StyleConstants.isItalic(attrs));
    }

    public void testGetAttributesUpdate() {
        assertEquals(2, attrs.getAttributeCount());
        assertEquals("italic",
                     attrs.getAttribute(CSS.Attribute.FONT_STYLE).toString());
        assertEquals(HTML.Tag.EM.toString(),
                     attrs.getAttribute(AttributeSet.NameAttribute));
        assertNull(attrs.getAttribute(CSS.Attribute.BACKGROUND_COLOR));

        doc.getStyleSheet().addRule("em { background-color: white }");
        assertEquals(4, attrs.getAttributeCount());
        assertEquals("white", attrs.getAttribute(CSS.Attribute.BACKGROUND_COLOR)
                              .toString());
    }

    public void testGetAttributesStyleSheet() {
        final Marker ssMarker = new Marker();
        view = new InlineView(inline) {
            protected StyleSheet getStyleSheet() {
                ssMarker.setOccurred();
                return super.getStyleSheet();
            };
        };
        assertTrue(ssMarker.isOccurred());
        ssMarker.reset();
        view.getAttributes();
        assertFalse(ssMarker.isOccurred());
    }

    public void testGetAttributesSame() {
        assertSame(attrs, view.getAttributes());
    }

    public void testGetBreakWeight() throws BadLocationException {
        view.setGlyphPainter(new FixedPainter());
        assertNull(attrs.getAttribute(CSS.Attribute.WHITE_SPACE));

        assertEquals(View.BadBreakWeight,
                     view.getBreakWeight(View.X_AXIS, startOffset,
                                         FixedPainter.CHAR_WIDTH - 0.01f));
        assertEquals(View.GoodBreakWeight,
                     view.getBreakWeight(View.X_AXIS, startOffset,
                                         FixedPainter.CHAR_WIDTH));

        assertEquals(View.GoodBreakWeight,
                     view.getBreakWeight(View.X_AXIS, startOffset,
                                         FixedPainter.CHAR_WIDTH * whitespace));

        assertEquals(View.ExcellentBreakWeight,
                     view.getBreakWeight(View.X_AXIS, startOffset,
                                         FixedPainter.CHAR_WIDTH
                                         * (whitespace + 1)));
    }

    public void testGetBreakWeightNowrap() throws BadLocationException {
        view.setGlyphPainter(new FixedPainter());

        assertNull(attrs.getAttribute(CSS.Attribute.WHITE_SPACE));
        doc.getStyleSheet().addRule("em { white-space: nowrap }");
        view.setPropertiesFromAttributes();
        assertNotNull(attrs.getAttribute(CSS.Attribute.WHITE_SPACE));

        assertEquals(View.BadBreakWeight,
                     view.getBreakWeight(View.X_AXIS, startOffset,
                                         FixedPainter.CHAR_WIDTH - 0.01f));
        assertEquals(View.BadBreakWeight,
                     view.getBreakWeight(View.X_AXIS, startOffset,
                                         FixedPainter.CHAR_WIDTH));

        assertEquals(View.BadBreakWeight,
                     view.getBreakWeight(View.X_AXIS, startOffset,
                                         FixedPainter.CHAR_WIDTH * whitespace));

        assertEquals(View.BadBreakWeight,
                     view.getBreakWeight(View.X_AXIS, startOffset,
                                         FixedPainter.CHAR_WIDTH
                                         * (whitespace + 1)));
    }

    public void testBreakView() throws BadLocationException {
        view.setGlyphPainter(new FixedPainter());
        assertNull(attrs.getAttribute(CSS.Attribute.WHITE_SPACE));

        View fragment = view.breakView(View.X_AXIS, startOffset, 0,
                                       FixedPainter.CHAR_WIDTH - 0.01f);
        assertTrue(fragment instanceof InlineView);
        assertNotSame(view, fragment);
        assertEquals(view.getStartOffset(), fragment.getStartOffset());
        assertEquals(view.getEndOffset(), fragment.getEndOffset());

        fragment = view.breakView(View.X_AXIS, startOffset, 0,
                                  FixedPainter.CHAR_WIDTH);
        assertEquals(view.getStartOffset(), fragment.getStartOffset());
        assertEquals(view.getStartOffset() + 1, fragment.getEndOffset());

        fragment = view.breakView(View.X_AXIS, startOffset, 0,
                                  FixedPainter.CHAR_WIDTH * (whitespace + 1));
        assertEquals(view.getStartOffset(), fragment.getStartOffset());
        assertEquals(view.getStartOffset() + whitespace + 1,
                     fragment.getEndOffset());
    }

    public void testBreakViewNowrap() {
        view.setGlyphPainter(new FixedPainter());
        doc.getStyleSheet().addRule("em { white-space: nowrap }");
        view.setPropertiesFromAttributes();
        assertNotNull(attrs.getAttribute(CSS.Attribute.WHITE_SPACE));

        View fragment = view.breakView(View.X_AXIS, startOffset, 0,
                                       FixedPainter.CHAR_WIDTH - 0.01f);
        assertTrue(fragment instanceof InlineView);
        if (isHarmony()) {
            // The view is not breakable
            // if the value of 'white-space' is 'nowrap'
            assertSame(view, fragment);
            return;
        }
        assertNotSame(view, fragment);
        assertEquals(view.getStartOffset(), fragment.getStartOffset());
        assertEquals(view.getEndOffset(), fragment.getEndOffset());

        fragment = view.breakView(View.X_AXIS, startOffset, 0,
                                  FixedPainter.CHAR_WIDTH);
        assertEquals(view.getStartOffset(), fragment.getStartOffset());
        assertEquals(view.getStartOffset() + 1, fragment.getEndOffset());

        fragment = view.breakView(View.X_AXIS, startOffset, 0,
                                  FixedPainter.CHAR_WIDTH * (whitespace + 1));
        assertEquals(view.getStartOffset(), fragment.getStartOffset());
        assertEquals(view.getStartOffset() + whitespace + 1,
                     fragment.getEndOffset());
    }

    public void testChangedUpdate() {
        final Marker prefChanged = new Marker();
        final Marker setProps = new Marker();
        view = new InlineView(inline) {
            public void preferenceChanged(View child,
                                          boolean width, boolean height) {
                prefChanged.setOccurred();
                assertNull(child);
                assertTrue(width);
                assertTrue(height);
                super.preferenceChanged(child, width, height);
            }

            protected void setPropertiesFromAttributes() {
                setProps.setOccurred();
                super.setPropertiesFromAttributes();
            }
        };
        assertFalse(prefChanged.isOccurred());
        assertFalse(setProps.isOccurred());

        view.changedUpdate(new Event(), null, null);
        assertTrue(prefChanged.isOccurred());
        assertFalse(setProps.isOccurred());
    }

    public void testChangedUpdateAttributes() {
        final Marker viewAttrMarker = new Marker(true);
        final StyleSheet ss = new StyleSheet() {
            public AttributeSet getViewAttributes(final View v) {
                viewAttrMarker.setOccurred();
                return super.getViewAttributes(v);
            }
        };
        view = new InlineView(inline) {
            protected StyleSheet getStyleSheet() {
                return ss;
            }
        };

        assertTrue(viewAttrMarker.isOccurred());
        attrs = view.getAttributes();
        view.changedUpdate(new Event(), null, null);
        assertTrue(viewAttrMarker.isOccurred());
        assertNotSame(attrs, view.getAttributes());
    }

    public void testInsertUpdate() {
        final Marker prefChanged = new Marker();
        final Marker setProps = new Marker();
        view = new InlineView(inline) {
            public void preferenceChanged(View child,
                                          boolean width, boolean height) {
                prefChanged.setOccurred();
                assertNull(child);
                assertTrue(width);
                assertFalse(height);
                super.preferenceChanged(child, width, height);
            }

            protected void setPropertiesFromAttributes() {
                setProps.setOccurred();
                super.setPropertiesFromAttributes();
            }
        };
        assertFalse(prefChanged.isOccurred());
        assertFalse(setProps.isOccurred());

        view.insertUpdate(new Event(), null, null);
        assertTrue(prefChanged.isOccurred());
        assertFalse(setProps.isOccurred());
    }

    public void testRemoveUpdate() {
        final Marker prefChanged = new Marker();
        final Marker setProps = new Marker();
        view = new InlineView(inline) {
            public void preferenceChanged(View child,
                                          boolean width, boolean height) {
                prefChanged.setOccurred();
                assertNull(child);
                assertTrue(width);
                assertFalse(height);
                super.preferenceChanged(child, width, height);
            }

            protected void setPropertiesFromAttributes() {
                setProps.setOccurred();
                super.setPropertiesFromAttributes();
            }
        };
        assertFalse(prefChanged.isOccurred());
        assertFalse(setProps.isOccurred());

        view.removeUpdate(new Event(), null, null);
        assertTrue(prefChanged.isOccurred());
        assertFalse(setProps.isOccurred());
    }

    public void testSetPropertiesFromAttributes() {
        assertTrue(view.getFont().isItalic());
        assertFalse(view.isUnderline());
        doc.getStyleSheet().addRule("em { text-decoration: underline }");
        assertFalse(view.isUnderline());
        view.setPropertiesFromAttributes();
        assertTrue(view.isUnderline());
    }

    public void testSetPropertiesFromAttributesBoxPainter() {
        final Marker boxMarker = new Marker();
        final Marker listMarker = new Marker();
        final StyleSheet ss = new StyleSheet() {
            public BoxPainter getBoxPainter(final AttributeSet attr) {
                boxMarker.setOccurred();
                return null;
            }
            public ListPainter getListPainter(final AttributeSet attr) {
                listMarker.setOccurred();
                return null;
            }
        };
        view = new InlineView(inline) {
            protected StyleSheet getStyleSheet() {
                return ss;
            }
        };
        assertFalse(boxMarker.isOccurred());
        assertFalse(listMarker.isOccurred());
        view.setPropertiesFromAttributes();
        assertFalse(boxMarker.isOccurred());
        assertFalse(listMarker.isOccurred());
    }

    public void testSetPropertiesFromAttributesAttributes() {
        final Marker verticalAlign = new Marker();
        final Marker textDecoration = new Marker();
        final Marker whiteSpace = new Marker();
        view = new InlineView(inline) {
            private AttributeSet attributes;
            public AttributeSet getAttributes() {
                if (attributes == null) {
                    attributes = new SimpleAttributeSet(super.getAttributes()) {
                        public Object getAttribute(Object name) {
                            if (name == CSS.Attribute.VERTICAL_ALIGN) {
                                verticalAlign.setOccurred();
                            } else if (name == CSS.Attribute.TEXT_DECORATION) {
                                textDecoration.setOccurred();
                            } else if (name == CSS.Attribute.WHITE_SPACE) {
                                whiteSpace.setOccurred();
                            }
                            return super.getAttribute(name);
                        }
                    };
                }
                return attributes;
            }
        };
        view.setPropertiesFromAttributes();
        assertTrue(whiteSpace.isOccurred());
        if (isHarmony()) {
            assertFalse(verticalAlign.isOccurred());
            assertFalse(textDecoration.isOccurred());
        } else {
            assertTrue(verticalAlign.isOccurred());
            assertTrue(textDecoration.isOccurred());
        }
    }

    public void testGetStyleSheet() {
        assertSame(doc.getStyleSheet(), view.getStyleSheet());
    }

}
