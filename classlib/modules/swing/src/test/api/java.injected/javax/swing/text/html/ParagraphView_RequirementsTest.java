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

import java.awt.FontMetrics;
import java.io.StringReader;

import javax.swing.BasicSwingTestCase;
import javax.swing.SizeRequirements;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.ParagraphViewTest.ParagraphViewImpl;

/**
 * Tests <code>calculateMinorAxisRequirements</code> method.
 */
public class ParagraphView_RequirementsTest extends BasicSwingTestCase {
    private static final int CHAR_WIDTH =
        InlineViewTest.FixedPainter.CHAR_WIDTH;

    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private ParagraphView view;
    private ViewFactory factory;

    private FontMetrics metrics;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        kit = new HTMLEditorKit();
        doc = (HTMLDocument)kit.createDefaultDocument();
        StringReader reader = new StringReader("<html><head></head><body>" +
               "<p><small>one long</small>Word" +
               "<p>very LongWord" +
               "</body></html>");
        kit.read(reader, doc, 0);

        metrics = getFontMetrics(null, CHAR_WIDTH);
        factory = new ViewFactory() {
            public View create(final Element element) {
                InlineView result = new InlineView(element) {
                    protected FontMetrics getFontMetrics() {
                        return metrics;
                    }
                };
                result.setGlyphPainter(BlockViewTest.InlineViewFactory.painter);
                return result;
            }
        };
    }

    public void testCalculateMinorAxisRequirements01() {
        view = new ParagraphViewImpl(doc.getParagraphElement(10), factory);
        SizeRequirements sr =
            view.calculateMinorAxisRequirements(View.X_AXIS, null);
        assertEquals(4 * CHAR_WIDTH, sr.minimum);
        assertEquals((isHarmony() ? 13 : 12) * CHAR_WIDTH, sr.preferred);
        assertEquals(Integer.MAX_VALUE, sr.maximum);
    }

    public void testCalculateMinorAxisRequirements02() throws Exception {
        view = new ParagraphViewImpl(doc.getParagraphElement(20), factory);
        SizeRequirements sr =
            view.calculateMinorAxisRequirements(View.X_AXIS, null);
        assertEquals(8 * CHAR_WIDTH, sr.minimum);
        assertEquals((isHarmony() ? 14 : 13) * CHAR_WIDTH, sr.preferred);
        assertEquals(Integer.MAX_VALUE, sr.maximum);
    }

    public void testCalculateMinorAxisRequirements03() throws Exception {
        factory = new ViewFactory() {
            public View create(Element element) {
                LabelView result = new LabelView(element);
                result.setGlyphPainter(BlockViewTest.InlineViewFactory.painter);
                return result;
            }
        };
        view = new ParagraphViewImpl(doc.getParagraphElement(10), factory);
        SizeRequirements sr =
            view.calculateMinorAxisRequirements(View.X_AXIS, null);

        final View layoutPool = ((ParagraphViewImpl)view).getLayoutPool();
        int min = 0;
        int pref = 0;
        for (int i = 0; i < layoutPool.getViewCount(); i++) {
            View child = layoutPool.getView(i);
            int ps = (int)child.getPreferredSpan(View.X_AXIS);
            min = Math.max(min, ps);
            pref += ps;
        }
        assertEquals(min, 8 * CHAR_WIDTH);
        assertEquals(pref, (isHarmony() ? 13 : 12) * CHAR_WIDTH);

        assertEquals(8 * CHAR_WIDTH, sr.minimum);
        assertEquals((isHarmony() ? 13 : 12) * CHAR_WIDTH, sr.preferred);
        assertEquals(Integer.MAX_VALUE, sr.maximum);
    }

    public void testCalculateMinorAxisRequirements04() throws Exception {
        factory = new ViewFactory() {
            public View create(Element element) {
                PlainView result = new PlainView(element) {
                    public float getPreferredSpan(int axis) {
                        if (axis == X_AXIS) {
                            return CHAR_WIDTH
                                   * (getEndOffset() - getStartOffset());
                        }
                        return super.getPreferredSpan(axis);
                    }
                };
                return result;
            }
        };
        view = new ParagraphViewImpl(doc.getParagraphElement(10), factory);
        SizeRequirements sr =
            view.calculateMinorAxisRequirements(View.X_AXIS, null);
        assertEquals(8 * CHAR_WIDTH, sr.minimum);
        assertEquals(13 * CHAR_WIDTH, sr.preferred);
        assertEquals(Integer.MAX_VALUE, sr.maximum);
    }
}
