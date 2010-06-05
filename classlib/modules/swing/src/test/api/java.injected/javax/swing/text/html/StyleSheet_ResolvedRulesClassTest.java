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
import javax.swing.text.html.HTML.Tag;

public class StyleSheet_ResolvedRulesClassTest extends BasicSwingTestCase {
    private HTMLDocument doc;
    private StyleSheet ss;
    private Style rule;
    private Element p;
    private HTMLEditorKit kit;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        ss = new StyleSheet();
        doc = new HTMLDocument(ss);
        doc.setAsynchronousLoadPriority(-1);
        kit = new HTMLEditorKit();
        StringReader reader = new StringReader("<html><head>\n" +
                "<style type=\"text/css\">" +
                ".par { font-style: italic; color: blue }" +
                "</style>" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "<p class=\"par\">The first paragraph has <code>class</code>" +
                "                 attribute with value" +
                "                 <code>&quot;par&quot;</code>.</p>\n" +
                "<p class=\"par\">Also does the second one.</p>\n" +
                "<p>While the third paragraph has no attributes.</p>\n" +
                "</body></html>");
        kit.read(reader, doc, 0);
    }

    public void testGetRule() {
        p = doc.getParagraphElement(1);
        assertEquals(1, p.getStartOffset());
        assertEquals(59, p.getEndOffset());

        rule = ss.getRule(Tag.P, p);
        assertEquals("html body p.par", rule.getName());
        assertEquals(3, rule.getAttributeCount());
        assertEquals(".par", rule.getAttribute(AttributeSet.NameAttribute));
    }

    public void testGetRuleSame() {
        p = doc.getParagraphElement(59);
        assertEquals(59, p.getStartOffset());
        assertEquals(85, p.getEndOffset());
        assertEquals("par",
                     p.getAttributes().getAttribute(HTML.Attribute.CLASS));

        rule = ss.getRule(Tag.P, p);
        assertEquals("html body p.par", rule.getName());
        assertEquals(3, rule.getAttributeCount());
        assertEquals(".par", rule.getAttribute(AttributeSet.NameAttribute));

        Element firstP = doc.getParagraphElement(1);
        assertEquals(1, firstP.getStartOffset());
        assertEquals(59, firstP.getEndOffset());
        assertEquals("par",
                     firstP.getAttributes().getAttribute(HTML.Attribute.CLASS));

        Style firstPRule = ss.getRule(Tag.P, firstP);
        assertEquals("html body p.par", rule.getName());
        assertEquals(3, rule.getAttributeCount());
        assertEquals(".par", rule.getAttribute(AttributeSet.NameAttribute));

        assertSame(rule, firstPRule);
    }

    public void testGetRuleAutoChange() {
        p = doc.getParagraphElement(1);
        assertEquals(1, p.getStartOffset());
        assertEquals(59, p.getEndOffset());

        rule = ss.getRule(Tag.P, p);
        assertEquals("html body p.par", rule.getName());
        assertEquals(3, rule.getAttributeCount());

        ss.addRule("p { background-color: yellow }");

        assertEquals("html body p.par", rule.getName());
        assertEquals(5, rule.getAttributeCount());
        assertEquals(".par", rule.getAttribute(AttributeSet.NameAttribute));
    }

    public void testGetRuleOL_LI() throws Exception {
        reInit();

        Element li = doc.getParagraphElement(1).getParentElement();
        assertEquals("li", li.getName());
        assertEquals(1, li.getStartOffset());
        assertEquals(13, li.getEndOffset());

        rule = ss.getRule(Tag.LI, li);
        assertEquals("html body ol li.first", rule.getName());
        assertEquals(3, rule.getAttributeCount());
        assertEquals(".first", rule.getAttribute(AttributeSet.NameAttribute));
    }

    public void testGetRuleUL_LI() throws Exception {
        reInit();

        Element li = doc.getParagraphElement(22).getParentElement();
        assertEquals("li", li.getName());
        assertEquals(22, li.getStartOffset());
        assertEquals(34, li.getEndOffset());

        rule = ss.getRule(Tag.LI, li);
        assertEquals("html body ul li.first", rule.getName());
        assertEquals(3, rule.getAttributeCount());
        assertEquals(".first", rule.getAttribute(AttributeSet.NameAttribute));
    }

    private void reInit() throws Exception {
        ss = new StyleSheet();
        doc = new HTMLDocument(ss);
        doc.setAsynchronousLoadPriority(-1);
        StringReader reader = new StringReader("<html><head>\n" +
                "<style type=\"text/css\">" +
                ".first { font-style: italic; color: blue }" +
                "</style>" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "<ol>\n" +
                "    <li class=\"first\">first class</li>\n" +
                "    <li>no class</li>\n" +
                "</ol>\n" +
                "\n" +
                "<ul>\n" +
                "    <li class=\"first\">first class</li>\n" +
                "    <li>no class</li>\n" +
                "</ul>\n" +
                "</body></html>");
        kit.read(reader, doc, 0);
    }
}
