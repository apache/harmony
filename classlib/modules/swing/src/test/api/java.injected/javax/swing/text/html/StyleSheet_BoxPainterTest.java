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

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.View;
import javax.swing.text.html.CSS.Attribute;
import javax.swing.text.html.StyleSheet.BoxPainter;

public class StyleSheet_BoxPainterTest extends BasicSwingTestCase {
    private HTMLDocument doc;
    private StyleSheet ss;
    private BoxPainter bp;
    private MutableAttributeSet attrs;
    private View view;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        doc = new HTMLDocument(ss);
        attrs = new SimpleAttributeSet();
        bp = ss.getBoxPainter(attrs);

        view = new BlockView(doc.getDefaultRootElement().getElement(0),
                             View.Y_AXIS) {
            public AttributeSet getAttributes() {
                return attrs;
            }
        };
    }

    public void testGetInsetNoAttributes() throws Exception {
        assertEquals(0, bp.getInset(View.TOP, view), 0f);
        assertEquals(0, bp.getInset(View.RIGHT, view), 0f);
        assertEquals(0, bp.getInset(View.BOTTOM, view), 0f);
        assertEquals(0, bp.getInset(View.LEFT, view), 0f);
    }

    public void testGetInsetWithMargin() throws Exception {
        ss.addCSSAttribute(attrs, Attribute.MARGIN, "11pt 21pt 30pt 03pt");
        assertEquals(4, attrs.getAttributeCount());
        assertEquals(0, bp.getInset(View.TOP, view), 0f);
        assertEquals(0, bp.getInset(View.RIGHT, view), 0f);
        assertEquals(0, bp.getInset(View.BOTTOM, view), 0f);
        assertEquals(0, bp.getInset(View.LEFT, view), 0f);
    }

    public void testGetInsetWithPadding() throws Exception {
        ss.addCSSAttribute(attrs, Attribute.PADDING, "11pt 21pt 30pt 03pt");
        assertEquals(4, attrs.getAttributeCount());
        assertEquals(11, bp.getInset(View.TOP, view), 0f);
        assertEquals(21, bp.getInset(View.RIGHT, view), 0f);
        assertEquals(30, bp.getInset(View.BOTTOM, view), 0f);
        assertEquals(03, bp.getInset(View.LEFT, view), 0f);
    }

    public void testGetInsetWithPaddingPercent() throws Exception {
        ss.addCSSAttribute(attrs, Attribute.PADDING, "10%");
        assertEquals(4, attrs.getAttributeCount());
        assertEquals(0, bp.getInset(View.TOP, view), 0f);
        assertEquals(0, bp.getInset(View.RIGHT, view), 0f);
        assertEquals(0, bp.getInset(View.BOTTOM, view), 0f);
        assertEquals(0, bp.getInset(View.LEFT, view), 0f);
    }

    public void testGetInsetWithPaddingPercentWithParent() throws Exception {
        BoxView parent = new BoxView(doc.getDefaultRootElement(), View.Y_AXIS) {
            public int getWidth() {
                return 361;
            }
            public int getHeight() {
                return 257;
            }
        };
        view.setParent(parent);
        ss.addCSSAttribute(attrs, Attribute.PADDING, "10%");
        assertEquals(4, attrs.getAttributeCount());
        final float width  = isHarmony() ? 361 * 0.1f : 0;
        assertEquals(width, bp.getInset(View.TOP, view), 1e-5f);
        assertEquals(width, bp.getInset(View.RIGHT, view), 1e-5f);
        assertEquals(width, bp.getInset(View.BOTTOM, view), 1e-5f);
        assertEquals(width, bp.getInset(View.LEFT, view), 1e-5f);
    }

    public void testGetInsetWithPaddingEm() throws Exception {
        ss.addCSSAttribute(attrs, Attribute.PADDING, "1em");
        final int fontSize = isHarmony() ? ss.getFont(attrs).getSize() : 0;
        assertEquals(4, attrs.getAttributeCount());
        assertEquals(fontSize, bp.getInset(View.TOP, view), 0f);
        assertEquals(fontSize, bp.getInset(View.RIGHT, view), 0f);
        assertEquals(fontSize, bp.getInset(View.BOTTOM, view), 0f);
        assertEquals(fontSize, bp.getInset(View.LEFT, view), 0f);
    }

    public void testGetInsetWithPaddingEx() throws Exception {
        ss.addCSSAttribute(attrs, Attribute.PADDING, "1ex");
        final int fontSize = isHarmony() ? ss.getFont(attrs).getSize() / 2 : 0;
        assertEquals(4, attrs.getAttributeCount());
        assertEquals(fontSize, bp.getInset(View.TOP, view), 0f);
        assertEquals(fontSize, bp.getInset(View.RIGHT, view), 0f);
        assertEquals(fontSize, bp.getInset(View.BOTTOM, view), 0f);
        assertEquals(fontSize, bp.getInset(View.LEFT, view), 0f);
    }

    public void testGetInsetDifferentViews() throws Exception {
        ss.addCSSAttribute(attrs, Attribute.MARGIN, "11pt 21pt 30pt 03pt");
        assertEquals(4, view.getAttributes().getAttributeCount());

        final MutableAttributeSet va = new SimpleAttributeSet();
        final View v = new InlineView(doc.getDefaultRootElement()) {
            public AttributeSet getAttributes() {
                return va;
            }
        };
        ss.addCSSAttribute(va, Attribute.MARGIN, "24pt 33pt 07pt 15pt");
        assertEquals(4, va.getAttributeCount());

        assertNotSame(attrs, va);

        bp = ss.getBoxPainter(view.getAttributes());
        if (isHarmony()) {
            bp.setView(view);
        }

        // view argument has no effect
        assertEquals(11, bp.getInset(View.TOP, v), 0f);
        assertEquals(21, bp.getInset(View.RIGHT, v), 0f);
        assertEquals(30, bp.getInset(View.BOTTOM, v), 0f);
        assertEquals(3,  bp.getInset(View.LEFT, v), 0f);
    }

    public void testGetInsetInvalid01() throws Exception {
        testExceptionalCase(new IllegalArgumentCase() {
            public void exceptionalAction() throws Exception {
                bp.getInset(0, view);
            }
        });
    }

    public void testGetInsetInvalid02() throws Exception {
        testExceptionalCase(new IllegalArgumentCase() {
            public void exceptionalAction() throws Exception {
                bp.getInset(5, view);
            }
        });
    }
}
