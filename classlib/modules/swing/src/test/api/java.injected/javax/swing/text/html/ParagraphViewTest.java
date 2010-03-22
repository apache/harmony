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

import java.io.StringReader;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class ParagraphViewTest extends BasicSwingTestCase {
    public static class ParagraphViewImpl extends ParagraphView {
        private final ViewFactory factory;

        public ParagraphViewImpl(final Element element,
                                 final ViewFactory factory) {
            super(element);
            this.factory = factory;
            loadChildren(getViewFactory());
        }

        public ViewFactory getViewFactory() {
            return factory;
        }

        public View getLayoutPool() {
            return layoutPool;
        }
    }

    private static class ParagraphViewNotVisible extends ParagraphViewImpl {
        public ParagraphViewNotVisible(final Element element,
                                       final ViewFactory factory) {
            super(element, factory);
        }

        public boolean isVisible() {
            return false;
        }
    }

    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private Element block;
    private ParagraphView view;
    private ViewFactory factory;
    private AttributeSet attrs;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        kit = new HTMLEditorKit();
        doc = (HTMLDocument)kit.createDefaultDocument();
        StringReader reader = new StringReader("<html><head></head>" +
               "<body>" +
               "<p>Normal <em>em inside</em> paragraph." +
               "</body></html>");
        kit.read(reader, doc, 0);

        block = doc.getParagraphElement(10);
        assertEquals(HTML.Tag.P.toString(), block.getName());

        factory = new BlockViewTest.InlineViewFactory();
        view = new ParagraphViewImpl(block, factory);
        attrs = view.getAttributes();
    }

    public void testParagraphView() {
        assertEquals(View.Y_AXIS, view.getAxis());
        assertNotSame(block.getAttributes(), view.getAttributes());
    }

    public void testGetAttributes() {
        assertSame(attrs, view.getAttributes());
    }

    public void testGetAttributesStyleSheet() {
        final Marker ssMarker = new Marker(true);
        view = new ParagraphView(block) {
            protected StyleSheet getStyleSheet() {
                ssMarker.setOccurred();
                return super.getStyleSheet();
            };
        };
        assertTrue(ssMarker.isOccurred());
        view.getAttributes();
        assertFalse(ssMarker.isOccurred());
    }

    public void testGetMinimumSpanNotVisible() {
        view = new ParagraphViewNotVisible(block, factory);
        assertFalse(view.isVisible());
        assertEquals(0, view.getMinimumSpan(View.X_AXIS), 0f);
        assertEquals(0, view.getMinimumSpan(View.Y_AXIS), 0f);
    }

    public void testGetPreferredSpanNotVisible() {
        view = new ParagraphViewNotVisible(block, factory);
        assertFalse(view.isVisible());
        assertEquals(0, view.getPreferredSpan(View.X_AXIS), 0f);
        assertEquals(0, view.getPreferredSpan(View.Y_AXIS), 0f);
    }

    public void testGetMaximumSpanNotVisible() {
        view = new ParagraphViewNotVisible(block, factory);
        assertFalse(view.isVisible());
        assertEquals(0, view.getMaximumSpan(View.X_AXIS), 0f);
        assertEquals(0, view.getMaximumSpan(View.Y_AXIS), 0f);
    }

    public void testIsVisible() {
        assertEquals(0, view.getViewCount());
        assertTrue(view.isVisible());
    }

    public void testIsVisibleNotLoaded() {
        view = new ParagraphView(block);
        testExceptionalCase(new NullPointerCase() {
            public void exceptionalAction() throws Exception {
                view.isVisible(); // Since layoutPool is null
            }
        });
    }

    public void testIsVisibleNotVisible() {
        final int elemCount = block.getElementCount();
        final int[] createCount = new int[] {0};
        final int[] isVisibleCount = new int[] {0};
        final View[] viewsRequested = new View[elemCount];
        final boolean[] results = new boolean[elemCount];

        view = new ParagraphViewImpl(block, new ViewFactory() {
            public View create(final Element element) {
                ++createCount[0];
                return new InlineView(element) {
                    public boolean isVisible() {
                        viewsRequested[isVisibleCount[0]] = this;
                        results[isVisibleCount[0]] =
                            getEndOffset() - getStartOffset() == 1;
                        ++isVisibleCount[0];
                        return results[isVisibleCount[0] - 1];
                    }
                };
            }
        });
        assertEquals(elemCount, createCount[0]);
        assertEquals(0, view.getViewCount());

        assertFalse(view.isVisible());

        assertEquals(elemCount - 1, isVisibleCount[0]);
        final View layoutPool = ((ParagraphViewImpl)view).getLayoutPool();
        for (int i = 0; i < isVisibleCount[0]; i++) {
            assertSame("@ " + i, layoutPool.getView(i), viewsRequested[i]);
            assertFalse("@ " + i, results[i]);
        }
    }

    public void testIsVisibleOneChild() throws Exception {
        doc = (HTMLDocument)kit.createDefaultDocument();
        StringReader reader = new StringReader("<html><body>00001111</body></html>");
        kit.read(reader, doc, 0);
        final SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        doc.insertString(5, "\n", attr);
        block = doc.getParagraphElement(3);

        view = new ParagraphViewImpl(block, factory);
        assertTrue(view.isVisible());
    }

    public void testIsVisibleCRChild() throws Exception {
        doc = (HTMLDocument)kit.createDefaultDocument();
        StringReader reader = new StringReader("<html><body>0000</body></html>");
        kit.read(reader, doc, 0);
        final boolean[] ordinaryViewVisible = new boolean[] {true};
        block = doc.getParagraphElement(1);

        view = new ParagraphViewImpl(block, new ViewFactory() {
            public View create(Element element) {
                return new InlineView(element) {
                    public boolean isVisible() {
                        return getElement().getAttributes()
                               .getAttribute("CR") != null
                               || ordinaryViewVisible[0];
                    }
                };
            }
        });
        final View layoutPool = ((ParagraphViewImpl)view).getLayoutPool();
        assertEquals(2, layoutPool.getViewCount());
        assertTrue(layoutPool.getView(0).isVisible());
        assertTrue(layoutPool.getView(1).isVisible());

        assertTrue(view.isVisible());


        ordinaryViewVisible[0] = false;
        assertFalse(layoutPool.getView(0).isVisible());
        assertTrue(layoutPool.getView(1).isVisible());
        assertNotNull(layoutPool.getView(1).getElement()
                                 .getAttributes().getAttribute("CR"));

        assertFalse(view.isVisible());
    }

    public void testIsVisibleNewLineChild() throws Exception {
        doc = (HTMLDocument)kit.createDefaultDocument();
        StringReader reader = new StringReader("<html><body>0000</body></html>");
        kit.read(reader, doc, 0);
        final SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        doc.insertString(2, "\n\n", attr);
        block = doc.getParagraphElement(3);
        assertEquals(3, block.getStartOffset());
        assertEquals(4, block.getEndOffset());

        view = new ParagraphViewImpl(block, factory);
        final View layoutPool = ((ParagraphViewImpl)view).getLayoutPool();
        assertEquals(1, layoutPool.getViewCount());

        final View newLineView = layoutPool.getView(0);
        assertTrue(newLineView.isVisible());
        assertNull(newLineView.getElement().getAttributes().getAttribute("CR"));

        assertTrue(view.isVisible());
    }

    public void testSetParent() {
        final Marker propertiesMarker = new Marker(true);
        view = new ParagraphView(block) {
            protected void setPropertiesFromAttributes() {
                propertiesMarker.setOccurred();
                super.setPropertiesFromAttributes();
            }
        };
        assertTrue(propertiesMarker.isOccurred());
        view.setParent(null);
        assertFalse(propertiesMarker.isOccurred());

        view.setParent(new PlainView(doc.getDefaultRootElement()));
        assertTrue(propertiesMarker.isOccurred());

        view.setParent(null);
        assertFalse(propertiesMarker.isOccurred());
    }

    public void testGetStyleSheet() {
        assertSame(doc.getStyleSheet(), view.getStyleSheet());
    }

    public void testSetPropertiesFromAttributes() {
        final Marker spaceAbove = new Marker();
        final Marker spaceBelow = new Marker();
        final Marker leftIndent = new Marker();
        final Marker rightIndent = new Marker();
        final Marker flIndent = new Marker();
        final Marker alignment = new Marker();
        final Marker lineSpacing = new Marker();
        final Marker paddingTop = new Marker();
        final Marker paddingRight = new Marker();
        final Marker paddingBottom = new Marker();
        final Marker paddingLeft = new Marker();

        view = new ParagraphViewImpl(block, factory) {
            private AttributeSet attributes;
            public AttributeSet getAttributes() {
                if (attributes == null) {
                    attributes = new SimpleAttributeSet(super.getAttributes()) {
                        public Object getAttribute(final Object name) {
                            if (name == StyleConstants.SpaceAbove) {
                                spaceAbove.setOccurred();
                            } else if (name == StyleConstants.SpaceBelow) {
                                spaceBelow.setOccurred();
                            } else if (name == StyleConstants.LeftIndent) {
                                leftIndent.setOccurred();
                            } else if (name == StyleConstants.RightIndent) {
                                rightIndent.setOccurred();
                            } else if (name == StyleConstants.FirstLineIndent) {
                                flIndent.setOccurred();
                            } else if (name == StyleConstants.Alignment) {
                                alignment.setOccurred();
                            } else if (name == StyleConstants.LineSpacing) {
                                lineSpacing.setOccurred();
                            } else if (name == CSS.Attribute.PADDING_TOP) {
                                paddingTop.setOccurred();
                            } else if (name == CSS.Attribute.PADDING_RIGHT) {
                                paddingRight.setOccurred();
                            } else if (name == CSS.Attribute.PADDING_BOTTOM) {
                                paddingBottom.setOccurred();
                            } else if (name == CSS.Attribute.PADDING_LEFT) {
                                paddingLeft.setOccurred();
                            }
                            return super.getAttribute(name);
                        }
                    };
                }
                return attributes;
            }
        };

        assertTrue(spaceAbove.isOccurred());
        assertTrue(spaceBelow.isOccurred());
        assertTrue(leftIndent.isOccurred());
        assertTrue(rightIndent.isOccurred());
        assertTrue(flIndent.isOccurred());
        assertTrue(alignment.isOccurred());
        assertTrue(lineSpacing.isOccurred());
        assertTrue(paddingTop.isOccurred());
        assertTrue(paddingRight.isOccurred());
        assertTrue(paddingBottom.isOccurred());
        assertTrue(paddingLeft.isOccurred());
    }

    public void testGetBoxPainter() {
        final Marker bpMarker = new Marker();
        StyleSheet ss = new StyleSheet() {
            public BoxPainter getBoxPainter(AttributeSet attr) {
                bpMarker.setOccurred();
                return super.getBoxPainter(attr);
            }
        };
        doc = new HTMLDocument(ss);
        block = doc.getParagraphElement(doc.getLength());
        view = new ParagraphView(block);
        assertTrue(bpMarker.isOccurred());
    }

//    public void testPaint() {
//
//    }
}
