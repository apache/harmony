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
import javax.swing.text.Style;
import javax.swing.text.View;
import javax.swing.text.html.HTML.Tag;

public class StyleSheet_ViewAttributesTest extends BasicSwingTestCase {
    private HTMLDocument doc;
    private StyleSheet ss;
    private View view;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        ss = new StyleSheet();
        doc = new HTMLDocument(ss);
    }

    public void testViewGetAttributes() throws Exception {
        final Marker marker = new Marker();
        ss = new StyleSheet() {
            public AttributeSet getViewAttributes(final View v) {
                marker.setOccurred();
                marker.setAuxiliary(v);
                return super.getViewAttributes(v);
            };
        };
        doc = new HTMLDocument(ss);
        view = new InlineView(doc.getCharacterElement(0));
        assertTrue(marker.isOccurred());
        assertSame(view, marker.getAuxiliary());
    }

    public void testGetViewAttributesGetRuleInline() throws Exception {
        final Marker tag = new Marker();
        final Marker sel = new Marker();
        ss = new StyleSheet() {
            public Style getRule(Tag t, Element elem) {
                tag.setOccurred();
                return super.getRule(t, elem);
            }

            public Style getRule(String selector) {
                sel.setOccurred();
                return super.getRule(selector);
            }
        };
        view = new InlineView(doc.getCharacterElement(0));
        ss.getViewAttributes(view);
        assertFalse(tag.isOccurred());
        assertFalse(sel.isOccurred());
    }

    public void testGetViewAttributesGetRuleBlock() throws Exception {
        final Marker tag = new Marker();
        final Marker sel = new Marker();
        ss = new StyleSheet() {
            public Style getRule(Tag t, Element elem) {
                tag.setOccurred();
                assertSame(HTML.Tag.P, t);
                assertSame(view.getElement(), elem);
                return super.getRule(t, elem);
            }

            public Style getRule(String selector) {
                sel.setOccurred();
                return super.getRule(selector);
            }
        };
        view = new BlockView(doc.getParagraphElement(0), View.Y_AXIS);
        ss.getViewAttributes(view);
        assertTrue(tag.isOccurred());
        assertFalse(sel.isOccurred());
    }

    public void testGetViewAttributesGetRuleInlineEm() throws Exception {
        final Marker tag = new Marker();
        final Marker sel = new Marker();
        ss = new StyleSheet() {
            public Style getRule(Tag t, Element elem) {
                if (view != null) {
                    tag.setOccurred();
                    assertSame(HTML.Tag.EM, t);
                    assertSame(view.getElement(), elem);
                }
                return super.getRule(t, elem);
            }

            public Style getRule(String selector) {
                sel.setOccurred();
                return super.getRule(selector);
            }
        };
        doc = new HTMLDocument(ss);
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.read(new StringReader("<em>emphasized</em>"), doc, 0);
        Element inline = doc.getCharacterElement(1);
        assertNotNull(inline.getAttributes()
                      .getAttribute(AttributeSet.NameAttribute));
        view = new InlineView(inline);
        ss.getViewAttributes(view);
        assertTrue(tag.isOccurred());
        assertFalse(sel.isOccurred());
    }

    public void testGetViewAttributesTranslate() throws Exception {
        final Marker marker = new Marker();
        final Element block = doc.getParagraphElement(0);
        ss = new StyleSheet() {
            public AttributeSet translateHTMLToCSS(final AttributeSet attrs) {
                marker.setOccurred();
                assertSame(block, attrs);
                return super.translateHTMLToCSS(attrs);
            }
        };
        view = new BlockView(block, View.Y_AXIS);
        assertFalse(marker.isOccurred());
        ss.getViewAttributes(view);
        assertTrue(marker.isOccurred());
    }

    public void testGetViewAttributesResolverNull() throws Exception {
        final Element block = doc.getParagraphElement(0);
        view = new BlockView(block, View.Y_AXIS);
        AttributeSet va = ss.getViewAttributes(view);
        assertNull(view.getParent());
        assertNull(va.getResolveParent());
    }

    public void testGetViewAttributesResolver() throws Exception {
        final Element block = doc.getParagraphElement(0);
        final Element inline = block.getElement(0);

        view = new InlineView(inline);
        View bv = new BlockView(block, View.Y_AXIS);
        view.setParent(bv);

        AttributeSet va = ss.getViewAttributes(view);
        assertSame(bv, view.getParent());
        assertSame(bv.getAttributes(), va.getResolveParent());
    }

    public void testGetViewAttributesResolverChange() throws Exception {
        final Element block = doc.getParagraphElement(0);
        final Element inline = block.getElement(0);

        view = new InlineView(inline);
        View bv = new BlockView(block, View.Y_AXIS);

        AttributeSet va = ss.getViewAttributes(view);
        assertNull(view.getParent());
        assertNull(va.getResolveParent());

        view.setParent(bv);
        assertSame(bv.getAttributes(), va.getResolveParent());
    }
}
