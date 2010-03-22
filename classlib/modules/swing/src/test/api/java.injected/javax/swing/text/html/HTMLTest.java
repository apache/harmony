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

import java.net.URL;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleContext;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;

public class HTMLTest extends BasicSwingTestCase {

//    public void testHTML() {
//    }

    public void testGetAllTags() {
        final Tag[] expected = {
            Tag.A,
            Tag.ADDRESS,
            Tag.APPLET,
            Tag.AREA,
            Tag.B,
            Tag.BASE,
            Tag.BASEFONT,
            Tag.BIG,
            Tag.BLOCKQUOTE,
            Tag.BODY,
            Tag.BR,
            Tag.CAPTION,
            Tag.CENTER,
            Tag.CITE,
            Tag.CODE,
            //Tag.COMMENT,
            //Tag.CONTENT,
            Tag.DD,
            Tag.DFN,
            Tag.DIR,
            Tag.DIV,
            Tag.DL,
            Tag.DT,
            Tag.EM,
            Tag.FONT,
            Tag.FORM,
            Tag.FRAME,
            Tag.FRAMESET,
            Tag.H1,
            Tag.H2,
            Tag.H3,
            Tag.H4,
            Tag.H5,
            Tag.H6,
            Tag.HEAD,
            Tag.HR,
            Tag.HTML,
            Tag.I,
            Tag.IMG,
            //Tag.IMPLIED,
            Tag.INPUT,
            Tag.ISINDEX,
            Tag.KBD,
            Tag.LI,
            Tag.LINK,
            Tag.MAP,
            Tag.MENU,
            Tag.META,
            Tag.NOFRAMES,
            Tag.OBJECT,
            Tag.OL,
            Tag.OPTION,
            Tag.P,
            Tag.PARAM,
            Tag.PRE,
            Tag.S,
            Tag.SAMP,
            Tag.SCRIPT,
            Tag.SELECT,
            Tag.SMALL,
            Tag.SPAN,
            Tag.STRIKE,
            Tag.STRONG,
            Tag.STYLE,
            Tag.SUB,
            Tag.SUP,
            Tag.TABLE,
            Tag.TD,
            Tag.TEXTAREA,
            Tag.TH,
            Tag.TITLE,
            Tag.TR,
            Tag.TT,
            Tag.U,
            Tag.UL,
            Tag.VAR
        };

        Tag[] actual = HTML.getAllTags();
        assertEquals(73, expected.length);
        assertEquals(isHarmony() ? 91 : 74, actual.length);

        final boolean[] found = new boolean[expected.length];
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < actual.length; j++) {
                if (expected[i] == actual[j]) {
                    found[i] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < found.length; i++) {
            assertTrue("Tag " + expected[i] + " was not found, index " + i,
                       found[i]);
        }
    }

    public void testGetTag() {
        assertNull(HTML.getTag("comment"));
        assertNull(HTML.getTag(HTML.Tag.COMMENT.toString()));
        assertNull(HTML.getTag("content"));
        assertNull(HTML.getTag(HTML.Tag.CONTENT.toString()));
        assertNull(HTML.getTag("p-implied"));
        assertNull(HTML.getTag(HTML.Tag.IMPLIED.toString()));

        if (!isHarmony()) {
            assertNotNull("nobr");
        }

        assertSame(Tag.A,
                   HTML.getTag("a"));
        assertSame(Tag.ADDRESS,
                   HTML.getTag("address"));
        assertSame(Tag.APPLET,
                   HTML.getTag("applet"));
        assertSame(Tag.AREA,
                   HTML.getTag("area"));
        assertSame(Tag.B,
                   HTML.getTag("b"));
        assertSame(Tag.BASE,
                   HTML.getTag("base"));
        assertSame(Tag.BASEFONT,
                   HTML.getTag("basefont"));
        assertSame(Tag.BIG,
                   HTML.getTag("big"));
        assertSame(Tag.BLOCKQUOTE,
                   HTML.getTag("blockquote"));
        assertSame(Tag.BODY,
                   HTML.getTag("body"));
        assertSame(Tag.BR,
                   HTML.getTag("br"));
        assertSame(Tag.CAPTION,
                   HTML.getTag("caption"));
        assertSame(Tag.CENTER,
                   HTML.getTag("center"));
        assertSame(Tag.CITE,
                   HTML.getTag("cite"));
        assertSame(Tag.CODE,
                   HTML.getTag("code"));
        assertSame(Tag.DD,
                   HTML.getTag("dd"));
        assertSame(Tag.DFN,
                   HTML.getTag("dfn"));
        assertSame(Tag.DIR,
                   HTML.getTag("dir"));
        assertSame(Tag.DIV,
                   HTML.getTag("div"));
        assertSame(Tag.DL,
                   HTML.getTag("dl"));
        assertSame(Tag.DT,
                   HTML.getTag("dt"));
        assertSame(Tag.EM,
                   HTML.getTag("em"));
        assertSame(Tag.FONT,
                   HTML.getTag("font"));
        assertSame(Tag.FORM,
                   HTML.getTag("form"));
        assertSame(Tag.FRAME,
                   HTML.getTag("frame"));
        assertSame(Tag.FRAMESET,
                   HTML.getTag("frameset"));
        assertSame(Tag.H1,
                   HTML.getTag("h1"));
        assertSame(Tag.H2,
                   HTML.getTag("h2"));
        assertSame(Tag.H3,
                   HTML.getTag("h3"));
        assertSame(Tag.H4,
                   HTML.getTag("h4"));
        assertSame(Tag.H5,
                   HTML.getTag("h5"));
        assertSame(Tag.H6,
                   HTML.getTag("h6"));
        assertSame(Tag.HEAD,
                   HTML.getTag("head"));
        assertSame(Tag.HR,
                   HTML.getTag("hr"));
        assertSame(Tag.HTML,
                   HTML.getTag("html"));
        assertSame(Tag.I,
                   HTML.getTag("i"));
        assertSame(Tag.IMG,
                   HTML.getTag("img"));
        assertSame(Tag.INPUT,
                   HTML.getTag("input"));
        assertSame(Tag.ISINDEX,
                   HTML.getTag("isindex"));
        assertSame(Tag.KBD,
                   HTML.getTag("kbd"));
        assertSame(Tag.LI,
                   HTML.getTag("li"));
        assertSame(Tag.LINK,
                   HTML.getTag("link"));
        assertSame(Tag.MAP,
                   HTML.getTag("map"));
        assertSame(Tag.MENU,
                   HTML.getTag("menu"));
        assertSame(Tag.META,
                   HTML.getTag("meta"));
        assertSame(Tag.NOFRAMES,
                   HTML.getTag("noframes"));
        assertSame(Tag.OBJECT,
                   HTML.getTag("object"));
        assertSame(Tag.OL,
                   HTML.getTag("ol"));
        assertSame(Tag.OPTION,
                   HTML.getTag("option"));
        assertSame(Tag.P,
                   HTML.getTag("p"));
        assertSame(Tag.PARAM,
                   HTML.getTag("param"));
        assertSame(Tag.PRE,
                   HTML.getTag("pre"));
        assertSame(Tag.S,
                   HTML.getTag("s"));
        assertSame(Tag.SAMP,
                   HTML.getTag("samp"));
        assertSame(Tag.SCRIPT,
                   HTML.getTag("script"));
        assertSame(Tag.SELECT,
                   HTML.getTag("select"));
        assertSame(Tag.SMALL,
                   HTML.getTag("small"));
        assertSame(Tag.SPAN,
                   HTML.getTag("span"));
        assertSame(Tag.STRIKE,
                   HTML.getTag("strike"));
        assertSame(Tag.STRONG,
                   HTML.getTag("strong"));
        assertSame(Tag.STYLE,
                   HTML.getTag("style"));
        assertSame(Tag.SUB,
                   HTML.getTag("sub"));
        assertSame(Tag.SUP,
                   HTML.getTag("sup"));
        assertSame(Tag.TABLE,
                   HTML.getTag("table"));
        assertSame(Tag.TD,
                   HTML.getTag("td"));
        assertSame(Tag.TEXTAREA,
                   HTML.getTag("textarea"));
        assertSame(Tag.TH,
                   HTML.getTag("th"));
        assertSame(Tag.TITLE,
                   HTML.getTag("title"));
        assertSame(Tag.TR,
                   HTML.getTag("tr"));
        assertSame(Tag.TT,
                   HTML.getTag("tt"));
        assertSame(Tag.U,
                   HTML.getTag("u"));
        assertSame(Tag.UL,
                   HTML.getTag("ul"));
        assertSame(Tag.VAR,
                   HTML.getTag("var"));


        if (isHarmony()) {
            assertSame(Tag.ABBR,
                       HTML.getTag("abbr"));
            assertSame(Tag.ACRONYM,
                       HTML.getTag("acronym"));
            assertSame(Tag.BDO,
                       HTML.getTag("bdo"));
            assertSame(Tag.BUTTON,
                       HTML.getTag("button"));
            assertSame(Tag.COL,
                       HTML.getTag("col"));
            assertSame(Tag.COLGROUP,
                       HTML.getTag("colgroup"));
            assertSame(Tag.DEL,
                       HTML.getTag("del"));
            assertSame(Tag.FIELDSET,
                       HTML.getTag("fieldset"));
            assertSame(Tag.IFRAME,
                       HTML.getTag("iframe"));
            assertSame(Tag.INS,
                       HTML.getTag("ins"));
            assertSame(Tag.LABEL,
                       HTML.getTag("label"));
            assertSame(Tag.LEGEND,
                       HTML.getTag("legend"));
            assertSame(Tag.NOSCRIPT,
                       HTML.getTag("noscript"));
            assertSame(Tag.OPTGROUP,
                       HTML.getTag("optgroup"));
            assertSame(Tag.Q,
                       HTML.getTag("q"));
            assertSame(Tag.TBODY,
                       HTML.getTag("tbody"));
            assertSame(Tag.TFOOT,
                       HTML.getTag("tfoot"));
            assertSame(Tag.THEAD,
                       HTML.getTag("thead"));


        } else {


            assertNull(HTML.getTag("abbr"));
            assertNull(HTML.getTag("acronym"));
            assertNull(HTML.getTag("bdo"));
            assertNull(HTML.getTag("button"));
            assertNull(HTML.getTag("col"));
            assertNull(HTML.getTag("colgroup"));
            assertNull(HTML.getTag("del"));
            assertNull(HTML.getTag("fieldset"));
            assertNull(HTML.getTag("iframe"));
            assertNull(HTML.getTag("ins"));
            assertNull(HTML.getTag("label"));
            assertNull(HTML.getTag("legend"));
            assertNull(HTML.getTag("noscript"));
            assertNull(HTML.getTag("optgroup"));
            assertNull(HTML.getTag("q"));
            assertNull(HTML.getTag("tbody"));
            assertNull(HTML.getTag("tfoot"));
            assertNull(HTML.getTag("thead"));
        }
    }

    public void testGetAllAttributeKeys() {
        final Attribute[] expected = {
            Attribute.ACTION,
            Attribute.ALIGN,
            Attribute.ALINK,
            Attribute.ALT,
            Attribute.ARCHIVE,
            Attribute.BACKGROUND,
            Attribute.BGCOLOR,
            Attribute.BORDER,
            Attribute.CELLPADDING,
            Attribute.CELLSPACING,
            Attribute.CHECKED,
            Attribute.CLASS,
            Attribute.CLASSID,
            Attribute.CLEAR,
            Attribute.CODE,
            Attribute.CODEBASE,
            Attribute.CODETYPE,
            Attribute.COLOR,
            Attribute.COLS,
            Attribute.COLSPAN,
            Attribute.COMMENT,
            Attribute.COMPACT,
            Attribute.CONTENT,
            Attribute.COORDS,
            Attribute.DATA,
            Attribute.DECLARE,
            Attribute.DIR,
            Attribute.DUMMY,
            Attribute.ENCTYPE,
            Attribute.ENDTAG,
            Attribute.FACE,
            Attribute.FRAMEBORDER,
            Attribute.HALIGN,
            Attribute.HEIGHT,
            Attribute.HREF,
            Attribute.HSPACE,
            Attribute.HTTPEQUIV,
            Attribute.ID,
            Attribute.ISMAP,
            Attribute.LANG,
            Attribute.LANGUAGE,
            Attribute.LINK,
            Attribute.LOWSRC,
            Attribute.MARGINHEIGHT,
            Attribute.MARGINWIDTH,
            Attribute.MAXLENGTH,
            Attribute.METHOD,
            Attribute.MULTIPLE,
            Attribute.N,
            Attribute.NAME,
            Attribute.NOHREF,
            Attribute.NORESIZE,
            Attribute.NOSHADE,
            Attribute.NOWRAP,
            Attribute.PROMPT,
            Attribute.REL,
            Attribute.REV,
            Attribute.ROWS,
            Attribute.ROWSPAN,
            Attribute.SCROLLING,
            Attribute.SELECTED,
            Attribute.SHAPE,
            Attribute.SHAPES,
            Attribute.SIZE,
            Attribute.SRC,
            Attribute.STANDBY,
            Attribute.START,
            Attribute.STYLE,
            Attribute.TARGET,
            Attribute.TEXT,
            Attribute.TITLE,
            Attribute.TYPE,
            Attribute.USEMAP,
            Attribute.VALIGN,
            Attribute.VALUE,
            Attribute.VALUETYPE,
            Attribute.VERSION,
            Attribute.VLINK,
            Attribute.VSPACE,
            Attribute.WIDTH,
        };

        Attribute[] actual = HTML.getAllAttributeKeys();
        assertEquals(80, expected.length);
        assertEquals(isHarmony() ? 84 : 81, actual.length);

        final boolean[] found = new boolean[expected.length];
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < actual.length; j++) {
                if (expected[i] == actual[j]) {
                    found[i] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < found.length; i++) {
            assertTrue("Attribute " + expected[i]
                       + " was not found, index " + i,
                       found[i]);
        }
    }

    public void testGetAttributeKey() {
        assertNull(HTML.getAttributeKey("httpequiv")); // without hyphen
        assertNull(HTML.getAttributeKey("xmlns"));     // XML namespace

        if (!isHarmony()) {
            assertNotNull(HTML.getAttributeKey("media"));
        }

        assertSame(Attribute.ACTION,
                   HTML.getAttributeKey("action"));
        assertSame(Attribute.ALIGN,
                   HTML.getAttributeKey("align"));
        assertSame(Attribute.ALINK,
                   HTML.getAttributeKey("alink"));
        assertSame(Attribute.ALT,
                   HTML.getAttributeKey("alt"));
        assertSame(Attribute.ARCHIVE,
                   HTML.getAttributeKey("archive"));
        assertSame(Attribute.BACKGROUND,
                   HTML.getAttributeKey("background"));
        assertSame(Attribute.BGCOLOR,
                   HTML.getAttributeKey("bgcolor"));
        assertSame(Attribute.BORDER,
                   HTML.getAttributeKey("border"));
        assertSame(Attribute.CELLPADDING,
                   HTML.getAttributeKey("cellpadding"));
        assertSame(Attribute.CELLSPACING,
                   HTML.getAttributeKey("cellspacing"));
        assertSame(Attribute.CHECKED,
                   HTML.getAttributeKey("checked"));
        assertSame(Attribute.CLASS,
                   HTML.getAttributeKey("class"));
        assertSame(Attribute.CLASSID,
                   HTML.getAttributeKey("classid"));
        assertSame(Attribute.CLEAR,
                   HTML.getAttributeKey("clear"));
        assertSame(Attribute.CODE,
                   HTML.getAttributeKey("code"));
        assertSame(Attribute.CODEBASE,
                   HTML.getAttributeKey("codebase"));
        assertSame(Attribute.CODETYPE,
                   HTML.getAttributeKey("codetype"));
        assertSame(Attribute.COLOR,
                   HTML.getAttributeKey("color"));
        assertSame(Attribute.COLS,
                   HTML.getAttributeKey("cols"));
        assertSame(Attribute.COLSPAN,
                   HTML.getAttributeKey("colspan"));
        assertSame(Attribute.COMMENT,
                   HTML.getAttributeKey("comment"));
        assertSame(Attribute.COMPACT,
                   HTML.getAttributeKey("compact"));
        assertSame(Attribute.CONTENT,
                   HTML.getAttributeKey("content"));
        assertSame(Attribute.COORDS,
                   HTML.getAttributeKey("coords"));
        assertSame(Attribute.DATA,
                   HTML.getAttributeKey("data"));
        assertSame(Attribute.DECLARE,
                   HTML.getAttributeKey("declare"));
        assertSame(Attribute.DIR,
                   HTML.getAttributeKey("dir"));
        assertSame(Attribute.DUMMY,
                   HTML.getAttributeKey("dummy"));
        assertSame(Attribute.ENCTYPE,
                   HTML.getAttributeKey("enctype"));
        assertSame(Attribute.ENDTAG,
                   HTML.getAttributeKey("endtag"));
        assertSame(Attribute.FACE,
                   HTML.getAttributeKey("face"));
        assertSame(Attribute.FRAMEBORDER,
                   HTML.getAttributeKey("frameborder"));
        assertSame(Attribute.HALIGN,
                   HTML.getAttributeKey("halign"));
        assertSame(Attribute.HEIGHT,
                   HTML.getAttributeKey("height"));
        assertSame(Attribute.HREF,
                   HTML.getAttributeKey("href"));
        assertSame(Attribute.HSPACE,
                   HTML.getAttributeKey("hspace"));
        assertSame(Attribute.HTTPEQUIV,
                   HTML.getAttributeKey("http-equiv"));
        assertSame(Attribute.ID,
                   HTML.getAttributeKey("id"));
        assertSame(Attribute.ISMAP,
                   HTML.getAttributeKey("ismap"));
        assertSame(Attribute.LANG,
                   HTML.getAttributeKey("lang"));
        assertSame(Attribute.LANGUAGE,
                   HTML.getAttributeKey("language"));
        assertSame(Attribute.LINK,
                   HTML.getAttributeKey("link"));
        assertSame(Attribute.LOWSRC,
                   HTML.getAttributeKey("lowsrc"));
        assertSame(Attribute.MARGINHEIGHT,
                   HTML.getAttributeKey("marginheight"));
        assertSame(Attribute.MARGINWIDTH,
                   HTML.getAttributeKey("marginwidth"));
        assertSame(Attribute.MAXLENGTH,
                   HTML.getAttributeKey("maxlength"));
        assertSame(Attribute.METHOD,
                   HTML.getAttributeKey("method"));
        assertSame(Attribute.MULTIPLE,
                   HTML.getAttributeKey("multiple"));
        assertSame(Attribute.N,
                   HTML.getAttributeKey("n"));
        assertSame(Attribute.NAME,
                   HTML.getAttributeKey("name"));
        assertSame(Attribute.NOHREF,
                   HTML.getAttributeKey("nohref"));
        assertSame(Attribute.NORESIZE,
                   HTML.getAttributeKey("noresize"));
        assertSame(Attribute.NOSHADE,
                   HTML.getAttributeKey("noshade"));
        assertSame(Attribute.NOWRAP,
                   HTML.getAttributeKey("nowrap"));
        assertSame(Attribute.PROMPT,
                   HTML.getAttributeKey("prompt"));
        assertSame(Attribute.REL,
                   HTML.getAttributeKey("rel"));
        assertSame(Attribute.REV,
                   HTML.getAttributeKey("rev"));
        assertSame(Attribute.ROWS,
                   HTML.getAttributeKey("rows"));
        assertSame(Attribute.ROWSPAN,
                   HTML.getAttributeKey("rowspan"));
        assertSame(Attribute.SCROLLING,
                   HTML.getAttributeKey("scrolling"));
        assertSame(Attribute.SELECTED,
                   HTML.getAttributeKey("selected"));
        assertSame(Attribute.SHAPE,
                   HTML.getAttributeKey("shape"));
        assertSame(Attribute.SHAPES,
                   HTML.getAttributeKey("shapes"));
        assertSame(Attribute.SIZE,
                   HTML.getAttributeKey("size"));
        assertSame(Attribute.SRC,
                   HTML.getAttributeKey("src"));
        assertSame(Attribute.STANDBY,
                   HTML.getAttributeKey("standby"));
        assertSame(Attribute.START,
                   HTML.getAttributeKey("start"));
        assertSame(Attribute.STYLE,
                   HTML.getAttributeKey("style"));
        assertSame(Attribute.TARGET,
                   HTML.getAttributeKey("target"));
        assertSame(Attribute.TEXT,
                   HTML.getAttributeKey("text"));
        assertSame(Attribute.TITLE,
                   HTML.getAttributeKey("title"));
        assertSame(Attribute.TYPE,
                   HTML.getAttributeKey("type"));
        assertSame(Attribute.USEMAP,
                   HTML.getAttributeKey("usemap"));
        assertSame(Attribute.VALIGN,
                   HTML.getAttributeKey("valign"));
        assertSame(Attribute.VALUE,
                   HTML.getAttributeKey("value"));
        assertSame(Attribute.VALUETYPE,
                   HTML.getAttributeKey("valuetype"));
        assertSame(Attribute.VERSION,
                   HTML.getAttributeKey("version"));
        assertSame(Attribute.VLINK,
                   HTML.getAttributeKey("vlink"));
        assertSame(Attribute.VSPACE,
                   HTML.getAttributeKey("vspace"));
        assertSame(Attribute.WIDTH,
                   HTML.getAttributeKey("width"));
    }

    public void testGetIntegerAttributeValue() {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(Attribute.COLSPAN, new String("11"));
        assertEquals(11,
                     HTML.getIntegerAttributeValue(attrs,
                                                   Attribute.COLSPAN, -1));

        attrs = new SimpleAttributeSet();
        attrs.addAttribute(Attribute.HREF, new String("10101"));
        assertEquals(10101,
                     HTML.getIntegerAttributeValue(attrs,
                                                   Attribute.HREF, -1));

        attrs = new SimpleAttributeSet();
        attrs.addAttribute(Attribute.HREF, new String("not a number"));
        assertEquals(-1,
                     HTML.getIntegerAttributeValue(attrs,
                                                   Attribute.HREF, -1));

        attrs = new SimpleAttributeSet();
        assertEquals(-1,
                     HTML.getIntegerAttributeValue(attrs,
                                                   Attribute.HREF, -1));

        final MutableAttributeSet wrongValue = new SimpleAttributeSet();
        wrongValue.addAttribute(Attribute.HREF, new Integer("10101"));
        testExceptionalCase(new ClassCastCase() {
            public void exceptionalAction() throws Exception {
                HTML.getIntegerAttributeValue(wrongValue, Attribute.HREF, -1);
            }
        });
    }

    public void testStaticAttributeKeysOfAttributes() {
        final Attribute[] attrs = HTML.getAllAttributeKeys();
        for (int i = 0; i < attrs.length; i++) {
            Object staticKey = StyleContext.getStaticAttributeKey(attrs[i]);
            assertSame("Static attribute for Attribute" + attrs[i]
                       + ", index " + i,
                       attrs[i],
                       StyleContext.getStaticAttribute(staticKey));
        }
    }

    public void testStaticAttributeKeysOfTags() {
        final Tag[] tags = HTML.getAllTags();
        for (int i = 0; i < tags.length; i++) {
            Object staticKey = StyleContext.getStaticAttributeKey(tags[i]);
            assertSame("Static attribute for Tag " + tags[i] + ", index " + i,
                       tags[i],
                       StyleContext.getStaticAttribute(staticKey));
        }
    }

    public void testResolveURL() throws Exception {
        // Regression for HARMONY-4529
        String base = "jar:file:test.jar!/root/current";
        String relative = "dir/file";
        String absolute = "http://host/file";
        URL baseURL = new URL(base);
        URL absoluteURL = new URL(absolute);
        URL resolvedURL = new URL("jar:file:test.jar!/root/dir/file");

        assertEquals(resolvedURL, HTML.resolveURL(relative, base));
        assertEquals(resolvedURL, HTML.resolveURL(relative, baseURL));

        assertEquals(absoluteURL, HTML.resolveURL(absolute, base));
        assertEquals(absoluteURL, HTML.resolveURL(absolute, baseURL));
        assertEquals(absoluteURL, HTML.resolveURL(absoluteURL, base));
        assertEquals(absoluteURL, HTML.resolveURL(absoluteURL, baseURL));

        assertEquals(absoluteURL, HTML.resolveURL(absolute, (URL) null));
        assertEquals(absoluteURL, HTML.resolveURL(absolute, (String) null));
        assertEquals(absoluteURL, HTML.resolveURL(absoluteURL, (URL) null));
        assertEquals(absoluteURL, HTML.resolveURL(absoluteURL, (String) null));

        assertNull(HTML.resolveURL("", base));
        assertNull(HTML.resolveURL("", baseURL));
        assertNull(HTML.resolveURL((URL) null, base));
        assertNull(HTML.resolveURL((URL) null, baseURL));
        assertNull(HTML.resolveURL((String) null, base));
        assertNull(HTML.resolveURL((String) null, baseURL));

        assertNull(HTML.resolveURL("", (URL) null));
        assertNull(HTML.resolveURL("", (String) null));
        assertNull(HTML.resolveURL((URL) null, (URL) null));
        assertNull(HTML.resolveURL((URL) null, (String) null));
        assertNull(HTML.resolveURL((String) null, (URL) null));
        assertNull(HTML.resolveURL((String) null, (String) null));
    }
}
