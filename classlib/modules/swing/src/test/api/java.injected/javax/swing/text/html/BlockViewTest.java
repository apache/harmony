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

import java.awt.Insets;
import java.io.StringReader;

import javax.swing.BasicSwingTestCase;
import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.PlainView;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.GlyphView.GlyphPainter;
import javax.swing.text.ViewTestHelpers.ChildrenFactory;
import javax.swing.text.html.InlineViewTest.FixedPainter;

public class BlockViewTest extends BasicSwingTestCase {
    private class Event implements DocumentEvent {
        public int getOffset() {
            return block.getStartOffset();
        }

        public int getLength() {
            return block.getEndOffset() - block.getStartOffset();
        }

        public Document getDocument() {
            return doc;
        }

        public EventType getType() {
            return EventType.CHANGE;
        }

        public ElementChange getChange(Element elem) {
            return null;
        }
    }

    public static class InlineViewFactory implements ViewFactory {
        public static final GlyphPainter painter = new FixedPainter();

        public View create(Element element) {
            GlyphView result = new InlineView(element);
            result.setGlyphPainter(painter);
            return result;
        }
    }

    private class BlockViewImpl extends BlockView {
        public BlockViewImpl(final Element element, final int axis) {
            super(element, axis);
            loadChildren();
        }

        public ViewFactory getViewFactory() {
            return factory;
        }

        public void loadChildren() {
            loadChildren(getViewFactory());
        }

        public void layoutMinorAxis(final int targetSpan, final int axis,
                                    final int[] offsets, final int[] spans) {
            super.layoutMinorAxis(targetSpan, axis, offsets, spans);
        }
    }

    private static final int Y_AXIS = View.Y_AXIS;
    private static final int X_AXIS = View.X_AXIS;

    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private Element block;
    private BlockView view;
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

        factory = new InlineViewFactory();
        view = new BlockViewImpl(block, Y_AXIS);
        attrs = view.getAttributes();
        doc.getStyleSheet().getStyleSheets()[0].addRule("p { margin-top: 15pt }");
    }

    public void testBlockView() {
        assertSame(block, view.getElement());
        assertNotSame(block.getAttributes(), view.getAttributes());
        assertEquals(block.getElementCount(), view.getViewCount());
    }

    public void testGetMinimumSpan() {
        assertEquals(view.getPreferredSpan(X_AXIS),
                     view.getMinimumSpan(X_AXIS), 0);
        assertEquals(view.getPreferredSpan(Y_AXIS),
                     view.getMinimumSpan(Y_AXIS), 0);
    }

    public void testGetPreferredSpan() {
        assertEquals(getMaxChildSpan(X_AXIS), view.getPreferredSpan(X_AXIS), 0);
        assertEquals(0, (int)view.getPreferredSpan(Y_AXIS));
    }

    public void testGetMaximumSpan() {
        assertEquals(Integer.MAX_VALUE, view.getMaximumSpan(X_AXIS), 0);
        assertEquals(0, (int)view.getMaximumSpan(Y_AXIS));
    }

    public void testGetAlignment() {
        factory = new ChildrenFactory();
        ((ChildrenFactory)factory).makeFlexible();

        view = new BlockViewImpl(block, Y_AXIS);
        assertEquals(0, view.getAlignment(X_AXIS), 0);
        assertEquals(0, view.getAlignment(Y_AXIS), 0);
    }

    public void testGetAlignmentFlexible() {
        factory = new ChildrenFactory();
        ((ChildrenFactory)factory).makeFlexible();
        view = new BlockViewImpl(block, Y_AXIS);

        assertEquals(0, view.getAlignment(X_AXIS), 0);
        assertEquals(0, view.getAlignment(Y_AXIS), 0);

        SizeRequirements r = view.calculateMajorAxisRequirements(Y_AXIS, null);
        assertEquals(0.5f, r.alignment, 0);

        r = view.calculateMajorAxisRequirements(X_AXIS, r);
        assertEquals(0.5f, r.alignment, 0);
    }

    public void testGetAttributes() {
        assertEquals(2, attrs.getAttributeCount());
        assertEquals("15pt",
                     attrs.getAttribute(CSS.Attribute.MARGIN_TOP).toString());
        assertEquals(HTML.Tag.P.toString(),
                     attrs.getAttribute(AttributeSet.NameAttribute));

        assertEquals(15, StyleConstants.getSpaceAbove(attrs), 0f);
    }

    public void testGetAttributesUpdate() {
        assertEquals(2, attrs.getAttributeCount());
        assertEquals("15pt",
                     attrs.getAttribute(CSS.Attribute.MARGIN_TOP).toString());
        assertEquals(HTML.Tag.P.toString(),
                     attrs.getAttribute(AttributeSet.NameAttribute));
        assertNull(attrs.getAttribute(CSS.Attribute.BACKGROUND_COLOR));

        doc.getStyleSheet().addRule("p { background-color: red }");
        assertEquals(4, attrs.getAttributeCount());
        assertEquals("red", attrs.getAttribute(CSS.Attribute.BACKGROUND_COLOR)
                            .toString());
    }

    public void testGetAttributesStyleSheet() {
        final Marker ssMarker = new Marker();
        view = new BlockView(block, Y_AXIS) {
            protected StyleSheet getStyleSheet() {
                ssMarker.setOccurred();
                return super.getStyleSheet();
            };
        };
        assertFalse(ssMarker.isOccurred());
        view.getAttributes();
        assertTrue(ssMarker.isOccurred());
    }

    public void testGetAttributesSame() {
        assertSame(attrs, view.getAttributes());
    }

    public void testGetResizeWeight() {
        assertEquals(1, view.getResizeWeight(X_AXIS));
        assertEquals(0, view.getResizeWeight(Y_AXIS));
    }

    public void testGetResizeWeightFlexible() {
        factory = new ChildrenFactory();
        ((ChildrenFactory)factory).makeFlexible();
        view = new BlockViewImpl(block, Y_AXIS);

        assertEquals(1, view.getResizeWeight(X_AXIS));
        assertEquals(isHarmony() ? 1 : 0, view.getResizeWeight(Y_AXIS));

        SizeRequirements r = view.calculateMajorAxisRequirements(Y_AXIS, null);
        assertEquals(44, r.minimum);
        assertEquals(400, r.maximum);
        assertEquals(44, (int)view.getMinimumSpan(Y_AXIS));
        assertEquals(400, (int)view.getMaximumSpan(Y_AXIS));
    }

    public void testChangedUpdate() {
        final Marker propMarker = new Marker();
        final Marker prefMarker = new Marker();
        view = new BlockViewImpl(block, Y_AXIS) {
            private int count;
            private boolean flag;

            protected void setPropertiesFromAttributes() {
                propMarker.setOccurred();
                super.setPropertiesFromAttributes();
            }

            public void preferenceChanged(View child,
                                          boolean width, boolean height) {
                prefMarker.setOccurred();
                assertSame(getView(count), child);
                if (isHarmony() || flag) {
                    count++;
                }
                flag = !flag;
                prefMarker.setAuxiliary(new Integer(count));

                assertNotNull(child);
                assertTrue(width);
                assertTrue(height);
                super.preferenceChanged(child, width, height);
            }
        };

        assertFalse(propMarker.isOccurred());
        assertFalse(prefMarker.isOccurred());

        view.changedUpdate(new Event(), null, null);

        assertTrue(propMarker.isOccurred());
        assertTrue(prefMarker.isOccurred());
        assertEquals(view.getViewCount(),
                     ((Integer)prefMarker.getAuxiliary()).intValue());
    }

    public void testChangedUpdateAttributes() {
        final Marker viewAttrMarker = new Marker(true);
        final StyleSheet ss = new StyleSheet() {
            public AttributeSet getViewAttributes(final View v) {
                viewAttrMarker.setOccurred();
                return super.getViewAttributes(v);
            }
        };
        view = new BlockView(block, View.Y_AXIS) {
            protected StyleSheet getStyleSheet() {
                return ss;
            }
        };

        attrs = view.getAttributes();
        assertTrue(viewAttrMarker.isOccurred());
        view.changedUpdate(new Event(), null, null);
        assertTrue(viewAttrMarker.isOccurred());
        assertNotSame(attrs, view.getAttributes());
    }

    public void testSetParent() {
        final Marker propertiesMarker = new Marker(true);
        view = new BlockView(block, Y_AXIS) {
            protected void setPropertiesFromAttributes() {
                propertiesMarker.setOccurred();
                super.setPropertiesFromAttributes();
            }
        };
        assertFalse(propertiesMarker.isOccurred());
        view.setParent(null);
        assertFalse(propertiesMarker.isOccurred());

        view.setParent(new PlainView(doc.getDefaultRootElement()));
        assertTrue(propertiesMarker.isOccurred());

        view.setParent(null);
        assertFalse(propertiesMarker.isOccurred());
    }

    public void testCalculateMajorAxisRequirements() {
        SizeRequirements r = view.calculateMajorAxisRequirements(Y_AXIS, null);
        assertEquals(r.preferred, r.minimum);
        assertEquals(0, r.preferred);
        assertEquals(0, r.maximum);

        doc.getStyleSheet().addRule("p {width: 305pt}");
        view.setPropertiesFromAttributes();
        SizeRequirements sr = view.calculateMajorAxisRequirements(Y_AXIS, r);
        assertSame(r, sr);
        assertEquals(0, r.minimum);
        assertEquals(0, r.preferred);
        assertEquals(0, r.maximum);

        doc.getStyleSheet().addRule("p {height: 40pt}");
        view.setPropertiesFromAttributes();
        view.calculateMajorAxisRequirements(Y_AXIS, r);
        assertEquals(25, r.minimum);
        assertEquals(25, r.preferred);
        assertEquals(25, r.maximum);
    }

    public void testCalculateMajorAxisRequirementsFlexible() {
        factory = new ChildrenFactory();
        ((ChildrenFactory)factory).makeFlexible();

        view = new BlockViewImpl(block, Y_AXIS);

        int minSpan = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            minSpan += (int)view.getView(i).getMinimumSpan(Y_AXIS);
        }
        assertEquals(44, minSpan);

        int prefSpan = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            prefSpan += (int)view.getView(i).getPreferredSpan(Y_AXIS);
        }
        assertEquals(112, prefSpan);

        int maxSpan = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            maxSpan += (int)view.getView(i).getMaximumSpan(Y_AXIS);
        }
        assertEquals(400, maxSpan);


        doc.getStyleSheet().addRule("p {height: 45pt}");
        view.setPropertiesFromAttributes();
        SizeRequirements r = view.calculateMajorAxisRequirements(Y_AXIS, null);
        assertEquals(44, r.minimum); // 45 - 15 def. margin = 30 < 44
        assertEquals(isHarmony() ? 112 : 44, r.preferred);
        assertEquals(400, r.maximum);

        doc.getStyleSheet().addRule("p {height: 60pt}");
        view.setPropertiesFromAttributes();
        view.calculateMajorAxisRequirements(Y_AXIS, r);
        assertEquals(45, r.minimum); // 60 - 15 def. margin = 45 > 44
        assertEquals(45, r.preferred);
        assertEquals(45, r.maximum);
    }

    public void testCalculateMinorAxisRequirements() {
        SizeRequirements r = view.calculateMinorAxisRequirements(X_AXIS, null);
        assertEquals(r.preferred, r.minimum);
        assertEquals((int)getMaxChildSpan(X_AXIS), r.preferred);
        assertEquals(Integer.MAX_VALUE, r.maximum);

        doc.getStyleSheet().addRule("p {height: 40pt}");
        view.setPropertiesFromAttributes();
        SizeRequirements sr = view.calculateMinorAxisRequirements(X_AXIS, r);
        assertSame(r, sr);
        assertEquals(r.preferred, r.minimum);
        assertEquals((int)getMaxChildSpan(X_AXIS), r.preferred);
        assertEquals(Integer.MAX_VALUE, r.maximum);

        doc.getStyleSheet().addRule("p {width: 305pt}");
        view.setPropertiesFromAttributes();
        view.calculateMinorAxisRequirements(X_AXIS, r);
        assertEquals(r.preferred, r.minimum);
        assertEquals(305, r.preferred);
        assertEquals(305, r.maximum);
    }

    public void testCalculateMinorAxisRequirementsFlexible() {
        factory = new ChildrenFactory();
        ((ChildrenFactory)factory).makeFlexible();

        view = new BlockViewImpl(block, Y_AXIS);

        int minSpan = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            minSpan = Math.max(minSpan,
                               (int)view.getView(i).getMinimumSpan(X_AXIS));
        }
        assertEquals(50, minSpan);

        int prefSpan = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            prefSpan = Math.max(prefSpan,
                                (int)view.getView(i).getPreferredSpan(X_AXIS));
        }
        assertEquals(100, prefSpan);


        doc.getStyleSheet().addRule("p {width: 46pt}");
        view.setPropertiesFromAttributes();
        SizeRequirements r = view.calculateMinorAxisRequirements(X_AXIS, null);
        assertEquals(50, r.minimum);
        assertEquals(isHarmony() ? 100 : 50, r.preferred);
        assertEquals(Integer.MAX_VALUE, r.maximum);

        doc.getStyleSheet().addRule("p {width: 146pt}");
        view.setPropertiesFromAttributes();
        view.calculateMinorAxisRequirements(X_AXIS, r);
        assertEquals(146, r.minimum);
        assertEquals(146, r.preferred);
        assertEquals(146, r.maximum);
    }

    public void testCalculateMinorAxisRequirementsOrthogonal() {
        view = new BlockViewImpl(block, X_AXIS);
        SizeRequirements r = view.calculateMinorAxisRequirements(Y_AXIS, null);
        assertEquals(r.preferred, r.minimum);
        assertEquals(0, r.preferred);
        assertEquals(Integer.MAX_VALUE, r.maximum);

        doc.getStyleSheet().addRule("p {width: 305pt}");
        view.setPropertiesFromAttributes();
        SizeRequirements sr = view.calculateMinorAxisRequirements(Y_AXIS, r);
        assertSame(r, sr);
        assertEquals(r.preferred, r.minimum);
        assertEquals(0, r.preferred);
        assertEquals(Integer.MAX_VALUE, r.maximum);

        doc.getStyleSheet().addRule("p {height: 40pt}");
        view.setPropertiesFromAttributes();
        view.calculateMinorAxisRequirements(Y_AXIS, r);
        assertEquals(r.preferred, r.minimum);
        assertEquals(25, r.preferred); // 40 - 15 (def. top margin) = 25
        assertEquals(25, r.maximum);
    }

    public void testLayoutMinorAxis() {
        final Marker marker = new Marker();
        view = new BlockViewImpl(block, Y_AXIS) {
            protected void baselineLayout(int targetSpan, int axis,
                                          int[] offsets, int[] spans) {
                marker.setOccurred();
                super.baselineLayout(targetSpan, axis, offsets, spans);
            }
        };

        final int[] offsets = new int[view.getViewCount()];
        final int[] spans = new int[view.getViewCount()];
        final int target = 305;
        ((BlockViewImpl)view).layoutMinorAxis(target, X_AXIS, offsets, spans);
        assertFalse(marker.isOccurred());
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            assertEquals(0.0, view.getAlignment(X_AXIS), 0);
            assertEquals((int)child.getPreferredSpan(X_AXIS), spans[i]);
            assertEquals((target - spans[i]) / 2, offsets[i]);
        }
    }

    public void testLayoutMinorAxisFlexible() {
        factory = new ChildrenFactory();
        ((ChildrenFactory)factory).makeFlexible();

        view = new BlockViewImpl(block, Y_AXIS);

        final int[] offsets = new int[view.getViewCount()];
        final int[] spans = new int[view.getViewCount()];
        final int target = 305;
        ((BlockViewImpl)view).layoutMinorAxis(target, X_AXIS, offsets, spans);
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            SizeRequirements sr =
                new SizeRequirements((int)child.getMinimumSpan(X_AXIS),
                                     (int)child.getPreferredSpan(X_AXIS),
                                     (int)child.getMaximumSpan(X_AXIS),
                                     child.getAlignment(X_AXIS));
            assertEquals(getChildSpan(target, sr), spans[i]);
            assertEquals(getChildOffset(target, spans[i], sr), offsets[i]);
        }
    }

    public void testLayoutMinorAxisFlexibleWide() {
        factory = new ChildrenFactory();
        ((ChildrenFactory)factory).makeFlexible();

        view = new BlockViewImpl(block, Y_AXIS);

        final int[] offsets = new int[view.getViewCount()];
        final int[] spans = new int[view.getViewCount()];
        final int target = 451;
        ((BlockViewImpl)view).layoutMinorAxis(target, X_AXIS, offsets, spans);
        for (int i = 0; i < view.getViewCount(); i++) {
            View child = view.getView(i);
            SizeRequirements sr =
                new SizeRequirements((int)child.getMinimumSpan(X_AXIS),
                                     (int)child.getPreferredSpan(X_AXIS),
                                     (int)child.getMaximumSpan(X_AXIS),
                                     child.getAlignment(X_AXIS));
            assertEquals(getChildSpan(target, sr), spans[i]);
            assertEquals(getChildOffset(target, spans[i], sr), offsets[i]);
        }
    }

    public void testGetStyleSheet() {
        assertSame(doc.getStyleSheet(), view.getStyleSheet());
    }

    public void testSetPropertiesFromAttributes() {
        final StyleSheet ss = doc.getStyleSheet();
        final Style pStyle = ss.getRule("p");
        assertEquals(2, pStyle.getAttributeCount());
        assertEquals("15pt", pStyle.getAttribute(CSS.Attribute.MARGIN_TOP)
                           .toString());

        final Marker insetMarker = new Marker(true);
        final Insets insets = new Insets(0, 0, 0, 0);
        view = new BlockViewImpl(block, Y_AXIS) {
            protected void setParagraphInsets(AttributeSet attrs) {
                fail("Unexpected call setParagraphInsets(AttributeSet)");
                super.setParagraphInsets(attrs);
            }

            protected void setInsets(short top, short left,
                                     short bottom, short right) {
                super.setInsets(top, left, bottom, right);
                insetMarker.setOccurred();
                insets.top = top;
                insets.left = left;
                insets.bottom = bottom;
                insets.right = right;
            }
        };
        view.setPropertiesFromAttributes();
        assertTrue(insetMarker.isOccurred());
        assertEquals(15, insets.top);
        assertEquals(0, insets.left);
        assertEquals(0, insets.bottom);
        assertEquals(0, insets.right);

        ss.addRule("p { padding-right: 31px; margin-bottom: 3pt }");
        view.setPropertiesFromAttributes();
        assertEquals(15, insets.top);
        assertEquals(0, insets.left);
        assertEquals(3, insets.bottom);
        assertEquals(40, insets.right);
    }

    public void testSetPropertiesFromAttributesBoxPainter() {
        final Marker boxMarker = new Marker();
        final Marker listMarker = new Marker();
        final StyleSheet ss = new StyleSheet() {
            public BoxPainter getBoxPainter(final AttributeSet attr) {
                boxMarker.setOccurred();
                return super.getBoxPainter(attr);
            }
            public ListPainter getListPainter(final AttributeSet attr) {
                listMarker.setOccurred();
                return null;
            }
        };
        view = new BlockView(block, Y_AXIS) {
            protected StyleSheet getStyleSheet() {
                return ss;
            }
        };
        assertFalse(boxMarker.isOccurred());
        assertFalse(listMarker.isOccurred());
        view.setPropertiesFromAttributes();
        assertTrue(boxMarker.isOccurred());
        assertFalse(listMarker.isOccurred());
    }

    private int getChildSpan(final int targetSpan, final SizeRequirements sr) {
        return Math.max(sr.minimum, Math.min(targetSpan, sr.maximum));
    }

    private int getChildOffset(final int targetSpan, final int childSpan,
                               final SizeRequirements sr) {
        return (int)((targetSpan - childSpan) * sr.alignment);
    }

    private float getMaxChildSpan(final int axis) {
        float result = 0;
        for (int i = 0; i < view.getViewCount(); i++) {
            result = Math.max(result, view.getView(i).getPreferredSpan(axis));
        }
        return result;
    }
}
