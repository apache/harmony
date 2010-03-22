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

import javax.swing.text.html.HTML.Attribute;

import junit.framework.TestCase;

public class HTML_AttributeTest extends TestCase {
    private Attribute attr;

    public void testACTION() {
        attr = HTML.Attribute.ACTION;
        assertEquals("action", attr.toString());
    }

    public void testALIGN() {
        attr = HTML.Attribute.ALIGN;
        assertEquals("align", attr.toString());
    }

    public void testALINK() {
        attr = HTML.Attribute.ALINK;
        assertEquals("alink", attr.toString());
    }

    public void testALT() {
        attr = HTML.Attribute.ALT;
        assertEquals("alt", attr.toString());
    }

    public void testARCHIVE() {
        attr = HTML.Attribute.ARCHIVE;
        assertEquals("archive", attr.toString());
    }

    public void testBACKGROUND() {
        attr = HTML.Attribute.BACKGROUND;
        assertEquals("background", attr.toString());
    }

    public void testBGCOLOR() {
        attr = HTML.Attribute.BGCOLOR;
        assertEquals("bgcolor", attr.toString());
    }

    public void testBORDER() {
        attr = HTML.Attribute.BORDER;
        assertEquals("border", attr.toString());
    }

    public void testCELLPADDING() {
        attr = HTML.Attribute.CELLPADDING;
        assertEquals("cellpadding", attr.toString());
    }

    public void testCELLSPACING() {
        attr = HTML.Attribute.CELLSPACING;
        assertEquals("cellspacing", attr.toString());
    }

    public void testCHECKED() {
        attr = HTML.Attribute.CHECKED;
        assertEquals("checked", attr.toString());
    }

    public void testCLASS() {
        attr = HTML.Attribute.CLASS;
        assertEquals("class", attr.toString());
    }

    public void testCLASSID() {
        attr = HTML.Attribute.CLASSID;
        assertEquals("classid", attr.toString());
    }

    public void testCLEAR() {
        attr = HTML.Attribute.CLEAR;
        assertEquals("clear", attr.toString());
    }

    public void testCODE() {
        attr = HTML.Attribute.CODE;
        assertEquals("code", attr.toString());
    }

    public void testCODEBASE() {
        attr = HTML.Attribute.CODEBASE;
        assertEquals("codebase", attr.toString());
    }

    public void testCODETYPE() {
        attr = HTML.Attribute.CODETYPE;
        assertEquals("codetype", attr.toString());
    }

    public void testCOLOR() {
        attr = HTML.Attribute.COLOR;
        assertEquals("color", attr.toString());
    }

    public void testCOLS() {
        attr = HTML.Attribute.COLS;
        assertEquals("cols", attr.toString());
    }

    public void testCOLSPAN() {
        attr = HTML.Attribute.COLSPAN;
        assertEquals("colspan", attr.toString());
    }

    public void testCOMMENT() {
        attr = HTML.Attribute.COMMENT;
        assertEquals("comment", attr.toString());
    }

    public void testCOMPACT() {
        attr = HTML.Attribute.COMPACT;
        assertEquals("compact", attr.toString());
    }

    public void testCONTENT() {
        attr = HTML.Attribute.CONTENT;
        assertEquals("content", attr.toString());
    }

    public void testCOORDS() {
        attr = HTML.Attribute.COORDS;
        assertEquals("coords", attr.toString());
    }

    public void testDATA() {
        attr = HTML.Attribute.DATA;
        assertEquals("data", attr.toString());
    }

    public void testDECLARE() {
        attr = HTML.Attribute.DECLARE;
        assertEquals("declare", attr.toString());
    }

    public void testDIR() {
        attr = HTML.Attribute.DIR;
        assertEquals("dir", attr.toString());
    }

    public void testDUMMY() {
        attr = HTML.Attribute.DUMMY;
        assertEquals("dummy", attr.toString());
    }

    public void testENCTYPE() {
        attr = HTML.Attribute.ENCTYPE;
        assertEquals("enctype", attr.toString());
    }

    public void testENDTAG() {
        attr = HTML.Attribute.ENDTAG;
        assertEquals("endtag", attr.toString());
    }

    public void testFACE() {
        attr = HTML.Attribute.FACE;
        assertEquals("face", attr.toString());
    }

    public void testFRAMEBORDER() {
        attr = HTML.Attribute.FRAMEBORDER;
        assertEquals("frameborder", attr.toString());
    }

    public void testHALIGN() {
        attr = HTML.Attribute.HALIGN;
        assertEquals("halign", attr.toString());
    }

    public void testHEIGHT() {
        attr = HTML.Attribute.HEIGHT;
        assertEquals("height", attr.toString());
    }

    public void testHREF() {
        attr = HTML.Attribute.HREF;
        assertEquals("href", attr.toString());
    }

    public void testHSPACE() {
        attr = HTML.Attribute.HSPACE;
        assertEquals("hspace", attr.toString());
    }

    public void testHTTPEQUIV() {
        attr = HTML.Attribute.HTTPEQUIV;
        assertEquals("http-equiv", attr.toString());
    }

    public void testID() {
        attr = HTML.Attribute.ID;
        assertEquals("id", attr.toString());
    }

    public void testISMAP() {
        attr = HTML.Attribute.ISMAP;
        assertEquals("ismap", attr.toString());
    }

    public void testLANG() {
        attr = HTML.Attribute.LANG;
        assertEquals("lang", attr.toString());
    }

    public void testLANGUAGE() {
        attr = HTML.Attribute.LANGUAGE;
        assertEquals("language", attr.toString());
    }

    public void testLINK() {
        attr = HTML.Attribute.LINK;
        assertEquals("link", attr.toString());
    }

    public void testLOWSRC() {
        attr = HTML.Attribute.LOWSRC;
        assertEquals("lowsrc", attr.toString());
    }

    public void testMARGINHEIGHT() {
        attr = HTML.Attribute.MARGINHEIGHT;
        assertEquals("marginheight", attr.toString());
    }

    public void testMARGINWIDTH() {
        attr = HTML.Attribute.MARGINWIDTH;
        assertEquals("marginwidth", attr.toString());
    }

    public void testMAXLENGTH() {
        attr = HTML.Attribute.MAXLENGTH;
        assertEquals("maxlength", attr.toString());
    }

    public void testMETHOD() {
        attr = HTML.Attribute.METHOD;
        assertEquals("method", attr.toString());
    }

    public void testMULTIPLE() {
        attr = HTML.Attribute.MULTIPLE;
        assertEquals("multiple", attr.toString());
    }

    public void testN() {
        attr = HTML.Attribute.N;
        assertEquals("n", attr.toString());
    }

    public void testNAME() {
        attr = HTML.Attribute.NAME;
        assertEquals("name", attr.toString());
    }

    public void testNOHREF() {
        attr = HTML.Attribute.NOHREF;
        assertEquals("nohref", attr.toString());
    }

    public void testNORESIZE() {
        attr = HTML.Attribute.NORESIZE;
        assertEquals("noresize", attr.toString());
    }

    public void testNOSHADE() {
        attr = HTML.Attribute.NOSHADE;
        assertEquals("noshade", attr.toString());
    }

    public void testNOWRAP() {
        attr = HTML.Attribute.NOWRAP;
        assertEquals("nowrap", attr.toString());
    }

    public void testPROMPT() {
        attr = HTML.Attribute.PROMPT;
        assertEquals("prompt", attr.toString());
    }

    public void testREL() {
        attr = HTML.Attribute.REL;
        assertEquals("rel", attr.toString());
    }

    public void testREV() {
        attr = HTML.Attribute.REV;
        assertEquals("rev", attr.toString());
    }

    public void testROWS() {
        attr = HTML.Attribute.ROWS;
        assertEquals("rows", attr.toString());
    }

    public void testROWSPAN() {
        attr = HTML.Attribute.ROWSPAN;
        assertEquals("rowspan", attr.toString());
    }

    public void testSCROLLING() {
        attr = HTML.Attribute.SCROLLING;
        assertEquals("scrolling", attr.toString());
    }

    public void testSELECTED() {
        attr = HTML.Attribute.SELECTED;
        assertEquals("selected", attr.toString());
    }

    public void testSHAPE() {
        attr = HTML.Attribute.SHAPE;
        assertEquals("shape", attr.toString());
    }

    public void testSHAPES() {
        attr = HTML.Attribute.SHAPES;
        assertEquals("shapes", attr.toString());
    }

    public void testSIZE() {
        attr = HTML.Attribute.SIZE;
        assertEquals("size", attr.toString());
    }

    public void testSRC() {
        attr = HTML.Attribute.SRC;
        assertEquals("src", attr.toString());
    }

    public void testSTANDBY() {
        attr = HTML.Attribute.STANDBY;
        assertEquals("standby", attr.toString());
    }

    public void testSTART() {
        attr = HTML.Attribute.START;
        assertEquals("start", attr.toString());
    }

    public void testSTYLE() {
        attr = HTML.Attribute.STYLE;
        assertEquals("style", attr.toString());
    }

    public void testTARGET() {
        attr = HTML.Attribute.TARGET;
        assertEquals("target", attr.toString());
    }

    public void testTEXT() {
        attr = HTML.Attribute.TEXT;
        assertEquals("text", attr.toString());
    }

    public void testTITLE() {
        attr = HTML.Attribute.TITLE;
        assertEquals("title", attr.toString());
    }

    public void testTYPE() {
        attr = HTML.Attribute.TYPE;
        assertEquals("type", attr.toString());
    }

    public void testUSEMAP() {
        attr = HTML.Attribute.USEMAP;
        assertEquals("usemap", attr.toString());
    }

    public void testVALIGN() {
        attr = HTML.Attribute.VALIGN;
        assertEquals("valign", attr.toString());
    }

    public void testVALUE() {
        attr = HTML.Attribute.VALUE;
        assertEquals("value", attr.toString());
    }

    public void testVALUETYPE() {
        attr = HTML.Attribute.VALUETYPE;
        assertEquals("valuetype", attr.toString());
    }

    public void testVERSION() {
        attr = HTML.Attribute.VERSION;
        assertEquals("version", attr.toString());
    }

    public void testVLINK() {
        attr = HTML.Attribute.VLINK;
        assertEquals("vlink", attr.toString());
    }

    public void testVSPACE() {
        attr = HTML.Attribute.VSPACE;
        assertEquals("vspace", attr.toString());
    }

    public void testWIDTH() {
        attr = HTML.Attribute.WIDTH;
        assertEquals("width", attr.toString());
    }
}
