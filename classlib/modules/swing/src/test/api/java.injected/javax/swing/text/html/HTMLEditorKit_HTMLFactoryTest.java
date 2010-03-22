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
 * @author Vadim L. Bogdanov
 */
package javax.swing.text.html;

import javax.swing.SwingTestCase;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

public class HTMLEditorKit_HTMLFactoryTest extends SwingTestCase {
    private static class TestHTMLDocument extends HTMLDocument {
        public void callWriteLock() {
            writeLock();
        }
    }

    private TestHTMLDocument doc;
    private HTMLEditorKit.HTMLFactory factory;

    public HTMLEditorKit_HTMLFactoryTest(final String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        
        setIgnoreNotImplemented(true);

        doc = new TestHTMLDocument();
        factory = new HTMLEditorKit.HTMLFactory();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateForCONTENT() {
        implTestCreateForTag(HTML.Tag.CONTENT, "javax.swing.text.html.InlineView");
    }

    public void testCreateForIMPLIED() {
        implTestCreateForTag(HTML.Tag.IMPLIED,
                             "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForP() {
        implTestCreateForTag(HTML.Tag.P, "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForH1() {
        implTestCreateForTag(HTML.Tag.H1, "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForH2() {
        implTestCreateForTag(HTML.Tag.H2, "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForH3() {
        implTestCreateForTag(HTML.Tag.H3, "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForH4() {
        implTestCreateForTag(HTML.Tag.H4, "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForH5() {
        implTestCreateForTag(HTML.Tag.H5, "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForH6() {
        implTestCreateForTag(HTML.Tag.H6, "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForDT() {
        implTestCreateForTag(HTML.Tag.DT, "javax.swing.text.html.ParagraphView");
    }

    public void testCreateForMENU() {
        implTestCreateForTag(HTML.Tag.MENU, "javax.swing.text.html.ListView");
    }

    public void testCreateForDIR() {
        implTestCreateForTag(HTML.Tag.DIR, "javax.swing.text.html.ListView");
    }

    public void testCreateForUL() {
        implTestCreateForTag(HTML.Tag.UL, "javax.swing.text.html.ListView");
    }

    public void testCreateForOL() {
        implTestCreateForTag(HTML.Tag.OL, "javax.swing.text.html.ListView");
    }

    public void testCreateForLI() {
        View view = implTestCreateForTag(HTML.Tag.LI,
                                         "javax.swing.text.html.BlockView");
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForDL() {
        View view = implTestCreateForTag(HTML.Tag.DL,
                                         "javax.swing.text.html.BlockView");
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForDD() {
        View view = implTestCreateForTag(HTML.Tag.DD,
                                         "javax.swing.text.html.BlockView");
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForBODY() {
        doc.callWriteLock();
        Element elem = doc.getDefaultRootElement();
        setTag(elem, HTML.Tag.BODY);
        View view = factory.create(elem);
        assertTrue(view instanceof BlockView);
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForHTML() {
        View view = implTestCreateForTag(HTML.Tag.HTML,
                                         "javax.swing.text.html.BlockView");
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForCENTER() {
        View view = implTestCreateForTag(HTML.Tag.CENTER,
                                         "javax.swing.text.html.BlockView");
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForDIV() {
        View view = implTestCreateForTag(HTML.Tag.DIV,
                                         "javax.swing.text.html.BlockView");
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForBLOCKQUOTE() {
        View view = implTestCreateForTag(HTML.Tag.BLOCKQUOTE,
                                         "javax.swing.text.html.BlockView");
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForPRE() {
        View view = implTestCreateForTag(HTML.Tag.PRE,
                                         "javax.swing.text.html.BlockView");
        assertEquals(View.Y_AXIS, ((BlockView)view).getAxis());
    }

    public void testCreateForIMG() {
        implTestCreateForTag(HTML.Tag.IMG, "javax.swing.text.html.ImageView");
    }

    public void testCreateForHR() {
        if (isHarmony()) {
            implTestCreateForTag(HTML.Tag.HR, "javax.swing.text.html.HRuleTagView");
        }
    }

    public void testCreateForBR() {
        implTestCreateForTag(HTML.Tag.BR, "javax.swing.text.html.BRView");
    }

    public void testCreateForTABLE() {
        if (isHarmony()) {
            implTestCreateForTag(HTML.Tag.TABLE, "javax.swing.text.html.TableTagView");
        }
    }

    public void testCreateForINPUT() {
        implTestCreateForTag(HTML.Tag.INPUT, "javax.swing.text.html.FormView");
    }

    public void testCreateForSELECT() {
        implTestCreateForTag(HTML.Tag.SELECT, "javax.swing.text.html.FormView");
    }

    public void testCreateForTEXTAREA() {
        implTestCreateForTag(HTML.Tag.TEXTAREA, "javax.swing.text.html.FormView");
    }

    public void testCreateForOBJECT() {
        implTestCreateForTag(HTML.Tag.OBJECT, "javax.swing.text.html.ObjectView");
    }

    public void testCreateForFRAMESET() {
        if (isHarmony()) {
            implTestCreateForTag(HTML.Tag.FRAMESET,
                                 "javax.swing.text.html.FrameSetTagView");
        }
    }

    public void testCreateForFRAME() {
        if (isHarmony()) {
            implTestCreateForTag(HTML.Tag.FRAME, "javax.swing.text.html.FrameTagView");
        }
    }

    private View implTestCreateForTag(final HTML.Tag tag, final String expected) {
        doc.callWriteLock();
        Element elem = doc.getDefaultRootElement();
        ((HTMLDocument.BlockElement)elem).addAttribute(HTML.Attribute.ROWS, "1");
        setTag(elem, tag);
        View result = factory.create(elem);
        assertEquals(expected, result.getClass().getName());
        return result;
    }

    private void setTag(final Element elem, final HTML.Tag tag) {
        ((HTMLDocument.BlockElement)elem).addAttribute(
                StyleConstants.NameAttribute, tag);
    }
}
