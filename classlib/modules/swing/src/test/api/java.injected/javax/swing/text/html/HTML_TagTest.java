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
import javax.swing.text.html.HTML.Tag;

import junit.framework.TestCase;

public class HTML_TagTest extends TestCase {
    private Tag tag;

    public void testTag() {
        tag = new Tag();
        assertNull(tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testTagString() {
        final String tagName = "newTag";
        tag = new Tag(tagName);
        assertEquals("newTag", tag.toString());
        assertSame(tagName, tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testTagStringboolbool() {
        tag = new Tag("tag1", true, false);
        assertSame("tag1", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());

        tag = new Tag("tag2", false, true);
        assertSame("tag2", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testIsPreformatted() throws Exception {
        tag = new Tag("pre", false, false);
        assertNotSame(Tag.PRE, tag);
        assertEquals(Tag.PRE.toString(), tag.toString());
        assertFalse("isPre", tag.isPreformatted());

        tag = new Tag("textarea", false, false);
        assertNotSame(Tag.TEXTAREA, tag);
        assertEquals(Tag.TEXTAREA.toString(), tag.toString());
        assertFalse("isPre", tag.isPreformatted());

        tag = new Tag("verb", true, false) {
            public boolean isPreformatted() {
                return true;
            }
        };
        assertEquals("verb", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertTrue("isPre", tag.isPreformatted());
    }

    public void testA() {
        tag = HTML.Tag.A;
        assertEquals("a", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testADDRESS() {
        tag = HTML.Tag.ADDRESS;
        assertEquals("address", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testAPPLET() {
        tag = HTML.Tag.APPLET;
        assertEquals("applet", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testAREA() {
        tag = HTML.Tag.AREA;
        assertEquals("area", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testB() {
        tag = HTML.Tag.B;
        assertEquals("b", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testBASE() {
        tag = HTML.Tag.BASE;
        assertEquals("base", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testBASEFONT() {
        tag = HTML.Tag.BASEFONT;
        assertEquals("basefont", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testBIG() {
        tag = HTML.Tag.BIG;
        assertEquals("big", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testBLOCKQUOTE() {
        tag = HTML.Tag.BLOCKQUOTE;
        assertEquals("blockquote", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testBODY() {
        tag = HTML.Tag.BODY;
        assertEquals("body", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testBR() {
        tag = HTML.Tag.BR;
        assertEquals("br", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testCAPTION() {
        tag = HTML.Tag.CAPTION;
        assertEquals("caption", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testCENTER() {
        tag = HTML.Tag.CENTER;
        assertEquals("center", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testCITE() {
        tag = HTML.Tag.CITE;
        assertEquals("cite", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testCODE() {
        tag = HTML.Tag.CODE;
        assertEquals("code", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testCOMMENT() {
        tag = HTML.Tag.COMMENT;
        assertEquals("comment", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testCONTENT() {
        tag = HTML.Tag.CONTENT;
        assertEquals("content", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testDD() {
        tag = HTML.Tag.DD;
        assertEquals("dd", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testDFN() {
        tag = HTML.Tag.DFN;
        assertEquals("dfn", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testDIR() {
        tag = HTML.Tag.DIR;
        assertEquals("dir", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testDIV() {
        tag = HTML.Tag.DIV;
        assertEquals("div", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testDL() {
        tag = HTML.Tag.DL;
        assertEquals("dl", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testDT() {
        tag = HTML.Tag.DT;
        assertEquals("dt", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testEM() {
        tag = HTML.Tag.EM;
        assertEquals("em", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testFONT() {
        tag = HTML.Tag.FONT;
        assertEquals("font", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testFORM() {
        tag = HTML.Tag.FORM;
        assertEquals("form", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testFRAME() {
        tag = HTML.Tag.FRAME;
        assertEquals("frame", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testFRAMESET() {
        tag = HTML.Tag.FRAMESET;
        assertEquals("frameset", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testH1() {
        tag = HTML.Tag.H1;
        assertEquals("h1", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testH2() {
        tag = HTML.Tag.H2;
        assertEquals("h2", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testH3() {
        tag = HTML.Tag.H3;
        assertEquals("h3", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testH4() {
        tag = HTML.Tag.H4;
        assertEquals("h4", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testH5() {
        tag = HTML.Tag.H5;
        assertEquals("h5", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testH6() {
        tag = HTML.Tag.H6;
        assertEquals("h6", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testHEAD() {
        tag = HTML.Tag.HEAD;
        assertEquals("head", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testHR() {
        tag = HTML.Tag.HR;
        assertEquals("hr", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testHTML() {
        tag = HTML.Tag.HTML;
        assertEquals("html", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testI() {
        tag = HTML.Tag.I;
        assertEquals("i", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testIMG() {
        tag = HTML.Tag.IMG;
        assertEquals("img", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testIMPLIED() {
        tag = HTML.Tag.IMPLIED;
        assertEquals("p-implied", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testINPUT() {
        tag = HTML.Tag.INPUT;
        assertEquals("input", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testISINDEX() {
        tag = HTML.Tag.ISINDEX;
        assertEquals("isindex", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testKBD() {
        tag = HTML.Tag.KBD;
        assertEquals("kbd", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testLI() {
        tag = HTML.Tag.LI;
        assertEquals("li", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testLINK() {
        tag = HTML.Tag.LINK;
        assertEquals("link", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testMAP() {
        tag = HTML.Tag.MAP;
        assertEquals("map", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testMENU() {
        tag = HTML.Tag.MENU;
        assertEquals("menu", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testMETA() {
        tag = HTML.Tag.META;
        assertEquals("meta", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testNOFRAMES() {
        tag = HTML.Tag.NOFRAMES;
        assertEquals("noframes", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testOBJECT() {
        tag = HTML.Tag.OBJECT;
        assertEquals("object", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testOL() {
        tag = HTML.Tag.OL;
        assertEquals("ol", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testOPTION() {
        tag = HTML.Tag.OPTION;
        assertEquals("option", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testP() {
        tag = HTML.Tag.P;
        assertEquals("p", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testPARAM() {
        tag = HTML.Tag.PARAM;
        assertEquals("param", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testPRE() {
        tag = HTML.Tag.PRE;
        assertEquals("pre", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertTrue("isPre", tag.isPreformatted());
    }

    public void testS() {
        tag = HTML.Tag.S;
        assertEquals("s", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSAMP() {
        tag = HTML.Tag.SAMP;
        assertEquals("samp", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSCRIPT() {
        tag = HTML.Tag.SCRIPT;
        assertEquals("script", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSELECT() {
        tag = HTML.Tag.SELECT;
        assertEquals("select", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSMALL() {
        tag = HTML.Tag.SMALL;
        assertEquals("small", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSPAN() {
        tag = HTML.Tag.SPAN;
        assertEquals("span", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSTRIKE() {
        tag = HTML.Tag.STRIKE;
        assertEquals("strike", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSTRONG() {
        tag = HTML.Tag.STRONG;
        assertEquals("strong", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSTYLE() {
        tag = HTML.Tag.STYLE;
        assertEquals("style", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSUB() {
        tag = HTML.Tag.SUB;
        assertEquals("sub", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testSUP() {
        tag = HTML.Tag.SUP;
        assertEquals("sup", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testTABLE() {
        tag = HTML.Tag.TABLE;
        assertEquals("table", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testTD() {
        tag = HTML.Tag.TD;
        assertEquals("td", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testTEXTAREA() {
        tag = HTML.Tag.TEXTAREA;
        assertEquals("textarea", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertTrue("isPre", tag.isPreformatted());
    }

    public void testTH() {
        tag = HTML.Tag.TH;
        assertEquals("th", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testTITLE() {
        tag = HTML.Tag.TITLE;
        assertEquals("title", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testTR() {
        tag = HTML.Tag.TR;
        assertEquals("tr", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testTT() {
        tag = HTML.Tag.TT;
        assertEquals("tt", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testU() {
        tag = HTML.Tag.U;
        assertEquals("u", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testUL() {
        tag = HTML.Tag.UL;
        assertEquals("ul", tag.toString());
        assertTrue("breaks Flow", tag.breaksFlow());
        assertTrue("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testVAR() {
        tag = HTML.Tag.VAR;
        assertEquals("var", tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    // Tags for HTML 4.0

    public void testABBR() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.ABBR;
        assertEquals("abbr", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testACRONYM() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.ACRONYM;
        assertEquals("acronym", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testBDO() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.BDO;
        assertEquals("bdo", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testBUTTON() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.BUTTON;
        assertEquals("button", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testCOL() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.COL;
        assertEquals("col", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testCOLGROUP() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.COLGROUP;
        assertEquals("colgroup", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testDEL() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.DEL;
        assertEquals("del", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testFIELDSET() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.FIELDSET;
        assertEquals("fieldset", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testIFRAME() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.IFRAME;
        assertEquals("iframe", tag.toString());
        assertTrue(tag.breaksFlow());
        assertTrue(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testINS() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.INS;
        assertEquals("ins", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testLABEL() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.LABEL;
        assertEquals("label", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testLEGEND() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.LEGEND;
        assertEquals("legend", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testNOSCRIPT() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.NOSCRIPT;
        assertEquals("noscript", tag.toString());
        assertTrue(tag.breaksFlow());
        assertTrue(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testOPTGROUP() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.OPTGROUP;
        assertEquals("optgroup", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testQ() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.Q;
        assertEquals("q", tag.toString());
        assertFalse(tag.breaksFlow());
        assertFalse(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testTBODY() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.TBODY;
        assertEquals("tbody", tag.toString());
        assertTrue(tag.breaksFlow());
        assertTrue(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testTFOOT() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.TFOOT;
        assertEquals("tfoot", tag.toString());
        assertTrue(tag.breaksFlow());
        assertTrue(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }

    public void testTHEAD() {
        if (!BasicSwingTestCase.isHarmony()) {
            return;
        }
        tag = HTML.Tag.THEAD;
        assertEquals("thead", tag.toString());
        assertTrue(tag.breaksFlow());
        assertTrue(tag.isBlock());
        assertFalse(tag.isPreformatted());
    }
}
