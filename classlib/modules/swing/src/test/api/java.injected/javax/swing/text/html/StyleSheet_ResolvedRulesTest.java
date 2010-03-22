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
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.swing.BasicSwingTestCase;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.html.CSS.Attribute;
import javax.swing.text.html.HTML.Tag;

public class StyleSheet_ResolvedRulesTest extends BasicSwingTestCase {
    private static final int P_EM_START_OFFSET = 20;
    private static final int PSMALL_EM_START_OFFSET = 82;

    private HTMLDocument doc;
    private StyleSheet ss;
    private Style rule;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        ss = new StyleSheet();
        doc = new HTMLDocument(ss);
        doc.setAsynchronousLoadPriority(-1);
        HTMLEditorKit kit = new HTMLEditorKit();
        StringReader reader = new StringReader("<html><head><style>\n" +
                "body { font-family: serif; font-size: large }\n" +
                "p { text-indent: 10pt }\n" +
                "p.small { font-size: small}\n" +
                "p.small em {color: red}" +
                "</style></head>" +
                "<body>" +
                "<p>Normal paragraph: <em>em inside</em>" +
                "   paragraph." +
                "<p class=\"small\">Paragraph with small class also contains" +
                "   <em>em tag</em> which should be in red." +
                "</body></html>");
        kit.read(reader, doc, 0);
    }

    public void testGetRuleTagElement_NoContext() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(0, rule.getAttributeCount());
        assertEquals("html body p em", rule.getName());
    }

    public void testGetRuleTagElement_NoContext_Same() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertSame(rule, ss.getRule(Tag.EM, em));
    }

    public void testGetRuleTagElement_Context() {
        Element em = doc.getCharacterElement(PSMALL_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(2, rule.getAttributeCount());
        assertEquals("red", rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("p.small em",
                     rule.getAttribute(AttributeSet.NameAttribute));
        assertEquals("html body p.small em", rule.getName());
    }

    public void testGetRuleTagElement_Context_Same() {
        Element em = doc.getCharacterElement(PSMALL_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertSame(rule, ss.getRule(Tag.EM, em));
    }

    public void testGetRuleTagElement_AutoChange_NoContext() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(0, rule.getAttributeCount());

        ss.addRule("em { background-color: rgb(255, 255, 150) }");
        assertEquals(2, rule.getAttributeCount());
        assertEquals("rgb(255, 255, 150)",
                     rule.getAttribute(Attribute.BACKGROUND_COLOR)
                     .toString());
        assertEquals("em",
                     rule.getAttribute(AttributeSet.NameAttribute));
    }

    public void testGetRuleTagElement_AutoChange_NoContextOverride() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(0, rule.getAttributeCount());

        ss.addRule("em { color: rgb(127, 0, 0) }");
        assertEquals(2, rule.getAttributeCount());
        assertEquals("rgb(127, 0, 0)",
                     rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("em",
                     rule.getAttribute(AttributeSet.NameAttribute));
    }

    public void testGetRuleTagElement_AutoChange_Context() {
        Element em = doc.getCharacterElement(PSMALL_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(2, rule.getAttributeCount());

        ss.addRule("em { background-color: rgb(255, 255, 150) }");
        assertEquals(4, rule.getAttributeCount());
        assertEquals("rgb(255, 255, 150)",
                     rule.getAttribute(Attribute.BACKGROUND_COLOR)
                     .toString());
        assertEquals("red", rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("p.small em",
                     rule.getAttribute(AttributeSet.NameAttribute));

        assertEquals(2, getNameAttributeCount(rule));
    }

    public void testGetRuleTagElement_AutoChange_ContextOverride() {
        Element em = doc.getCharacterElement(PSMALL_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(2, rule.getAttributeCount());

        ss.addRule("em { color: rgb(127, 0, 0) }");
        assertEquals(4, rule.getAttributeCount());
        assertEquals("red",
                     rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("p.small em",
                     rule.getAttribute(AttributeSet.NameAttribute));

        assertEquals(2, getNameAttributeCount(rule));
        assertEquals(2, getAttributeCount(rule, Attribute.COLOR));
    }

    public void testGetRuleTagElement_AutoChange_ChangeStyle() {
        Element em = doc.getCharacterElement(PSMALL_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(2, rule.getAttributeCount());

        ss.addCSSAttribute(rule, Attribute.BACKGROUND_COLOR, "#FFFFFF");
        assertEquals(2, rule.getAttributeCount());
        assertNull(rule.getAttribute(Attribute.BACKGROUND_COLOR));

        ss.addRule("em { color: rgb(127, 0, 0) }");
        assertEquals(4, rule.getAttributeCount());
        assertEquals("red",
                     rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("p.small em",
                     rule.getAttribute(AttributeSet.NameAttribute));

        assertEquals(2, getNameAttributeCount(rule));
        assertEquals(2, getAttributeCount(rule, Attribute.COLOR));
    }

    public void testGetRuleTagElement_AutoChange_ChangeStyle_Override() {
        Element em = doc.getCharacterElement(PSMALL_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(2, rule.getAttributeCount());

        ss.addCSSAttribute(rule, Attribute.COLOR, "#000000");
        assertEquals(2, rule.getAttributeCount());
        assertEquals("red", rule.getAttribute(Attribute.COLOR).toString());

        ss.addRule("em { color: rgb(127, 0, 0) }");
        assertEquals(4, rule.getAttributeCount());
        assertEquals("red",
                     rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("p.small em",
                     rule.getAttribute(AttributeSet.NameAttribute));

        assertEquals(2, getNameAttributeCount(rule));
        assertEquals(2, getAttributeCount(rule, Attribute.COLOR));
    }

    public void testGetRuleTagElement_AddStyle() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        assertEquals(5, getStyleNumber());
        assertNull(ss.getStyle("em"));

        rule = ss.getRule(Tag.EM, em);
        assertEquals("html body p em", rule.getName());
        assertEquals(0, rule.getAttributeCount());

        assertEquals(5, getStyleNumber());
        assertNull(ss.getStyle("em"));
    }

    public void testGetRuleString_AddStyle() {
        assertEquals(5, getStyleNumber());
        assertNull(ss.getStyle("em"));

        rule = ss.getRule("em");
        assertEquals("em", rule.getName());
        assertEquals(0, rule.getAttributeCount());

        assertEquals(5, getStyleNumber());
        assertNull(ss.getStyle("em"));
    }

    public void testGetRule_NotSame() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET); // With no context associated
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        Style ruleEm = ss.getRule("em");
        assertNotSame(rule, ruleEm);
        assertEquals("html body p em", rule.getName());
        assertEquals("em", ruleEm.getName());
    }

    public void testGetRule_Same() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET); // With no context associated
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals("html body p em", rule.getName());
        assertSame(rule, ss.getRule("html body p em"));
    }

    public void testGetRule_Same_Context() {
        Element em = doc.getCharacterElement(PSMALL_EM_START_OFFSET); // With context associated
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals("html body p.small em", rule.getName());
        assertSame(rule, ss.getRule("html body p.small em"));
    }

    public void testGetRuleString_NoContext() {
        rule = ss.getRule("em");
        assertEquals(0, rule.getAttributeCount());
    }

    public void testGetRuleString_ContextClass() {
        rule = ss.getRule("p.small em");
        assertEquals(2, rule.getAttributeCount());
    }

    public void testGetRuleString_ContextNoClass() {
        ss.addRule("p em { text-decoration: underline }");
        rule = ss.getRule("p em");
        assertEquals(2, rule.getAttributeCount());

        assertEquals("underline",
                     rule.getAttribute(Attribute.TEXT_DECORATION).toString());
        assertEquals("p em",
                     rule.getAttribute(AttributeSet.NameAttribute));
    }

    public void testGetRuleString_Autochange_ContextNoClass01() {
        ss.addRule("p em { text-decoration: underline }");
        rule = ss.getRule("p em");
        assertEquals(2, rule.getAttributeCount());

        ss.addRule("em { color: rgb(127, 0, 0) }");
        assertEquals(4, rule.getAttributeCount());
        assertEquals("rgb(127, 0, 0)",
                     rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("underline",
                     rule.getAttribute(Attribute.TEXT_DECORATION).toString());
        assertEquals("p em",
                     rule.getAttribute(AttributeSet.NameAttribute));

        assertEquals(2, getNameAttributeCount(rule));
    }

    public void testGetRuleString_Autochange_ContextNoClass02() {
        ss.addRule("p em { text-decoration: underline }");
        rule = ss.getRule("p em");
        assertEquals(2, rule.getAttributeCount());

        ss.addRule("p em { color: rgb(50%, 100%, 50%) }");
        assertEquals(3, rule.getAttributeCount());
        assertEquals("rgb(50%, 100%, 50%)",
                     rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("underline",
                     rule.getAttribute(Attribute.TEXT_DECORATION).toString());
        assertEquals("p em",
                     rule.getAttribute(AttributeSet.NameAttribute));
    }

    public void testGetRuleString_AutoChange_NoContext() {
        rule = ss.getRule("em");
        assertEquals(0, rule.getAttributeCount());

        ss.addRule("em { color: rgb(127, 0, 0) }");
        assertEquals(2, rule.getAttributeCount());
        assertEquals("rgb(127, 0, 0)",
                     rule.getAttribute(Attribute.COLOR).toString());
        assertEquals("em",
                     rule.getAttribute(AttributeSet.NameAttribute));
    }

    public void testGetRuleTagElement_Listeners() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        rule = ss.getRule(Tag.EM, em);
        assertEquals(0, rule.getAttributeCount());
        final boolean[] happened = new boolean[1];
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                happened[0] = true;
            }
        };
        rule.addChangeListener(listener);

        ss.addRule("em { background-color: rgb(255, 255, 150) }");
        assertEquals(2, rule.getAttributeCount());
        assertFalse(happened[0]);

        Style emStyle = ss.getStyle("em");
        ss.addCSSAttribute(emStyle, CSS.Attribute.COLOR, "black");
        assertEquals(3, rule.getAttributeCount());
        assertFalse(happened[0]);
    }

    public void testGetRuleTagElement_RemoveStyle() {
        Element em = doc.getCharacterElement(P_EM_START_OFFSET);
        assertNotNull(em.getAttributes().getAttribute(Tag.EM));

        ss.addRule("em { color: rgb(255, 255, 150) }");

        rule = ss.getRule(Tag.EM, em);
        assertEquals(2, rule.getAttributeCount());
        assertEquals("rgb(255, 255, 150)",
                     rule.getAttribute(Attribute.COLOR).toString());

        ss.removeStyle("em");
        assertEquals(0, rule.getAttributeCount());
        assertNull(rule.getAttribute(Attribute.COLOR));
    }

    public void testGetRuleString_Partial() {
        ss.addRule("em { color: rgb(255, 255, 150) }");

        rule = ss.getRule("em");

        Style qualifiedRule = ss.getRule("html body p em");
        assertNotSame(rule, qualifiedRule);
        assertTrue(rule.isEqual(qualifiedRule));
        assertEquals(2, qualifiedRule.getAttributeCount());
        assertEquals("rgb(255, 255, 150)",
                     qualifiedRule.getAttribute(Attribute.COLOR).toString());
    }

    public void testGetRuleString_NoRules() {
        rule = ss.getRule(HTML.Tag.ADDRESS.toString());

        assertEquals(0, rule.getAttributeCount());
        final Enumeration keys = rule.getAttributeNames();
        assertFalse(keys.hasMoreElements());

        testExceptionalCase(new ExceptionalCase() {
            public void exceptionalAction() throws Exception {
                keys.nextElement();
            }

            public Class expectedExceptionClass() {
                return NoSuchElementException.class;
            }
        });
    }

    public void testGetRuleString_NoRulesStyles() {
        ss.addStyleSheet(new StyleSheet());
        rule = ss.getRule(HTML.Tag.ADDRESS.toString());

        assertEquals(0, rule.getAttributeCount());
        final Enumeration keys = rule.getAttributeNames();
        assertFalse(keys.hasMoreElements());

        testExceptionalCase(new ExceptionalCase() {
            public void exceptionalAction() throws Exception {
                keys.nextElement();
            }

            public Class expectedExceptionClass() {
                return NoSuchElementException.class;
            }
        });
    }

    private static int getAttributeCount(final AttributeSet rule,
                                         final Object attrKey) {
        int result = 0;
        Enumeration keys = rule.getAttributeNames();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (attrKey.equals(key)) {
                ++result;
            }
        }
        return result;
    }

    private static int getNameAttributeCount(final AttributeSet rule) {
        return getAttributeCount(rule, AttributeSet.NameAttribute);
    }

    private int getStyleNumber() {
        int result = 0;
        final Enumeration styleNames = ss.getStyleNames();
        while (styleNames.hasMoreElements()) {
            ++result;
            styleNames.nextElement();
        }
        return result;
    }
}
