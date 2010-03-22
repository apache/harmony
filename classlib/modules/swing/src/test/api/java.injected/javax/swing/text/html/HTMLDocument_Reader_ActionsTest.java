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
* @author Alexander T. Simbirtsev
*/
package javax.swing.text.html;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument.HTMLReader;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;

public class HTMLDocument_Reader_ActionsTest extends HTMLDocumentTestCase {

    protected HTMLDocument.HTMLReader reader;
    protected HTMLDocument doc;
    protected HTMLDocument.HTMLReader.TagAction action;
    
    protected void setUp() throws Exception {
        super.setUp();
        doc = new HTMLDocument();
        reader = (HTMLDocument.HTMLReader)doc.getReader(0);
    }

    protected void tearDown() throws Exception {
        action = null;
        doc = null;
        reader = null;
        super.tearDown();
    }

    public void testHandleSimpleTag_Unknown() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        doc.setPreservesUnknownTags(true);
        reader.handleStartTag(Tag.HTML, attr, 0);
        reader.handleStartTag(Tag.BODY, attr, 0);
        reader.handleStartTag(Tag.P, attr, 0);
        assertEquals(3, reader.parseBuffer.size());
        reader.handleSimpleTag(new Tag("fake"), attr, 0);
        assertEquals(4, reader.parseBuffer.size());
    }
    
    public void testBlockStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new BlockAction();
        
        action.start(Tag.B, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.B);
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
    }

    public void testBlockEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new BlockAction();
        
        action.end(Tag.B);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkEndTagSpec(spec);
    }
    
    public void testCharacterStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new CharacterAction();
        
        reader.charAttr.addAttribute("bbbb", "aaaa");
        action.start(Tag.B, attr);
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(2, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, "bbbb", "aaaa");
        checkAttributes(reader.charAttr, Tag.B, attr);
        reader.popCharacterStyle();
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, "bbbb", "aaaa");

        assertNotSame(reader.charAttr.getAttribute(Tag.B), attr);
    }

    public void testFontStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.COLOR, "black");
        attr.addAttribute(HTML.Attribute.SIZE, isHarmony() ? "1pt" : "1");
        attr.addAttribute(HTML.Attribute.FACE, "1111");
        attr.addAttribute("aaaa", "bbbb");
        
        reader.charAttr.addAttribute("bbbb", "aaaa");
        reader.handleStartTag(Tag.HTML, new SimpleAttributeSet(), 0);
        reader.handleStartTag(Tag.BODY, new SimpleAttributeSet(), 0);
        reader.handleStartTag(Tag.FONT, attr, 0);
        assertEquals(5, reader.charAttr.getAttributeCount());
        assertNotNull(reader.charAttr.getAttribute("bbbb"));
        assertNotNull(reader.charAttr.getAttribute(Tag.FONT));
        assertNotNull(reader.charAttr.getAttribute(CSS.Attribute.COLOR));
        assertNotNull(reader.charAttr.getAttribute(CSS.Attribute.FONT_FAMILY));
        assertNotNull(reader.charAttr.getAttribute(CSS.Attribute.FONT_SIZE));
    }

    public void testCharacterStart_vs_HandleStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new CharacterAction();
        
        reader.charAttr.addAttribute("bbbb", "aaaa");
        reader.handleStartTag(Tag.B, attr, 0);
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(3, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, "bbbb", "aaaa");
        checkAttributes(reader.charAttr, Tag.B, attr);
        checkAttributes(reader.charAttr, CSS.Attribute.FONT_WEIGHT, "bold");
        reader.popCharacterStyle();
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, "bbbb", "aaaa");
    }

    public void testCharacterEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new CharacterAction();
        
        reader.charAttr.addAttribute("bbbb", "aaaa");
        reader.pushCharacterStyle();
        reader.charAttr = null;
        action.end(Tag.B);
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, "bbbb", "aaaa");
    }

    public void testParagraphStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new ParagraphAction();
        
        action.start(Tag.P, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.P);
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
    }

    public void testParagraphEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new ParagraphAction();
        
        action.end(Tag.P);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkEndTagSpec(spec);
    }

    public void testSpecialStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new SpecialAction();
        
        action.start(Tag.IMG, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr;
        checkOpenImpliedSpec(spec);
        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, "img");
        checkAttributes(specAttr, "aaaa", "bbbb");
        checkImplicitContentSpec(spec);

        assertEquals(2, attr.getAttributeCount());
        checkAttributes(attr, StyleConstants.NameAttribute, "img");
        checkAttributes(attr, "aaaa", "bbbb");
    }

    public void testAnchorStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        // If href attribute is absent, after 4606 handleStartTag(Tag.A) does
        // nothing
        reader.handleStartTag(Tag.A, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        // After addition the href attribute, Tag.A works as before
        reader.charAttr.addAttribute("bbbb", "aaaa");
        attr.addAttribute(HTML.Attribute.HREF, "");
        reader.handleStartTag(Tag.A, attr, 0);
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(2, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, "bbbb", "aaaa");
        checkAttributes(reader.charAttr, Tag.A, attr);
        reader.popCharacterStyle();
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, "bbbb", "aaaa");

        assertNotSame(reader.charAttr.getAttribute(Tag.B), attr);
    }

    public void testAnchorEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.handleEndTag(Tag.A, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
    }

    public void testAnchorStartEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        // If href attribute is absent, after 4606 handleStartTag(Tag.A) does
        // nothing. After addition the href attribute, Tag.A works as before
        attr.addAttribute(HTML.Attribute.HREF, "");
        final Tag tag = Tag.A;
        reader.handleStartTag(tag, attr, 0);
        assertEquals(1, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
        reader.handleEndTag(tag, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        // The rest part of the test is deleted because according to H-4574
        // there is no underscore added if no text encountered. all the
        // verification in that part occurs in the testAnchorStartTextEnd        
    }

    public void testAnchorStartTextEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        attr.addAttribute(HTML.Attribute.HREF, "file:///index.html");
        final Tag tag = Tag.A;
        reader.handleStartTag(Tag.BODY, attr, 0);
        reader.handleStartTag(tag, attr, 0);
        assertEquals(1, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        reader.handleText("text".toCharArray(), 0);
        assertEquals(2, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());
        reader.handleEndTag(tag, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());
    }

    public void testSpecialStart_AfterSpecialStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader = (HTMLReader)doc.getReader(0, 0, 0, Tag.I);
        reader.new SpecialAction().start(Tag.I, new SimpleAttributeSet());
        assertEquals(1, reader.parseBuffer.size());
        
        reader.new SpecialAction().start(Tag.IMG, attr);
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.IMG);
        checkImplicitContentSpec(spec);
    }

    public void testSpecialStart_AfterCharacterStart1() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader = (HTMLReader)doc.getReader(0, 0, 0, Tag.I);
        reader.new CharacterAction().start(Tag.I, new SimpleAttributeSet());
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(1, reader.charAttr.getAttributeCount());
        
        reader.new SpecialAction().start(Tag.IMG, attr);
        assertEquals(1, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.IMG);
        checkImplicitContentSpec(spec);
    }

    public void testSpecialStart_AfterCharacterStart2() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader = (HTMLReader)doc.getReader(10, 10, 10, null);
        reader.new CharacterAction().start(Tag.I, new SimpleAttributeSet());
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(1, reader.charAttr.getAttributeCount());
        
        reader.new SpecialAction().start(Tag.IMG, attr);
        assertEquals(2, reader.parseBuffer.size());
    }

    public void testFormStart_AfterImplied() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        
        reader.handleStartTag(Tag.BODY, new SimpleAttributeSet(), 0);
        reader.handleText("text".toCharArray(), 0);
        assertEquals(3, reader.parseBuffer.size());

        reader.handleStartTag(Tag.FORM, attr, 0);
        assertEquals(6, reader.parseBuffer.size());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(3);
        checkCRSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(5);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.FORM);
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);

        assertEquals(2, attr.getAttributeCount());
        checkAttributes(attr, StyleConstants.NameAttribute, Tag.FORM);
        checkAttributes(attr, "aaaa", "bbbb");
    }

    public void testSpecialEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        HTMLDocument.HTMLReader.TagAction action = reader.new SpecialAction();
        
        action.end(Tag.IMG);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
    }

    public void testSpecialStart_Calls() {
        final Marker specialMarker = new Marker();
        doc = new HTMLDocument() {
            public ParserCallback getReader(int pos) {
                return new HTMLReader(0) {
                    protected void addSpecialElement(final Tag tag, final MutableAttributeSet attr) {
                        specialMarker.setOccurred();
                        ArrayList callInfo = new ArrayList();
                        callInfo.add(tag);
                        callInfo.add(attr);
                        specialMarker.setAuxiliary(callInfo);
                    }
                };
            }
        };
        reader = (HTMLReader)doc.getReader(0);
        String text = "precontent";
        Tag tag = Tag.HTML;
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.addSpecialElement(tag, attr);
        assertTrue(specialMarker.isOccurred());
        ArrayList callInfo = (ArrayList)specialMarker.getAuxiliary();
        assertEquals(tag, callInfo.get(0));
        assertEquals(attr, callInfo.get(1));
    }
    
    public void testPreStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new PreAction();
        
        action.start(Tag.PRE, attr);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.PRE);
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);

        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.IMPLIED);
        checkAttributes(specAttr, CSS.Attribute.WHITE_SPACE, "pre");
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
    }
    
    public void testPreStart_InParagraph() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new PreAction();
        
        reader.handleStartTag(Tag.P, attr, 0);
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        action.start(Tag.PRE, attr);
        assertEquals(3, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.PRE);
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);

        spec = (ElementSpec)reader.parseBuffer.get(2);
        specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.IMPLIED);
        checkAttributes(specAttr, CSS.Attribute.WHITE_SPACE, "pre");
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
    }
    
    public void testPreStartEnd_Specs() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new PreAction();
        
        action.start(Tag.PRE, attr);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        action.end(Tag.PRE);
        assertEquals(5, reader.parseBuffer.size());

        checkCRSpec((ElementSpec)reader.parseBuffer.get(2));
        checkEndTagSpec((ElementSpec)reader.parseBuffer.get(3));
        checkEndTagSpec((ElementSpec)reader.parseBuffer.get(4));
    }
    
    public void testPreStartEnd_BlockCalls() {
        final Marker blockOpen = new Marker();
        final Marker blockClose = new Marker();
        doc = new HTMLDocument() {
            public ParserCallback getReader(int pos) {
                return new HTMLReader(0) {
                    protected void blockOpen(Tag tag, MutableAttributeSet attr) {
                        blockOpen.setOccurred();
                        ArrayList callInfo = (blockOpen.getAuxiliary() == null)
                                              ? new ArrayList() 
                                              : (ArrayList)blockOpen.getAuxiliary();
                        callInfo.add(tag);
                        callInfo.add(attr.copyAttributes());
                        blockOpen.setAuxiliary(callInfo);
                        super.blockOpen(tag, attr);
                    }

                    protected void blockClose(Tag tag) {
                        super.blockClose(tag);
                        ArrayList callInfo = (blockClose.getAuxiliary() == null)
                                              ? new ArrayList() 
                                              : (ArrayList)blockClose.getAuxiliary();
                        blockClose.setOccurred();
                        callInfo.add(tag);
                        blockClose.setAuxiliary(callInfo);
                    }
                };
            }
        };
        reader = (HTMLReader)doc.getReader(0);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        SimpleAttributeSet initial = (SimpleAttributeSet)attr.copyAttributes();
        action = reader.new PreAction();
        
        action.start(Tag.PRE, attr);
        assertTrue(blockOpen.isOccurred());
        assertFalse(blockClose.isOccurred());
        ArrayList callInfo = (ArrayList)blockOpen.getAuxiliary();
        assertEquals(4, callInfo.size());
        assertEquals(Tag.PRE, callInfo.get(0));
        assertEquals(initial, callInfo.get(1));
        assertEquals(Tag.IMPLIED, callInfo.get(2));
        final AttributeSet attrs = (AttributeSet)callInfo.get(3);
        checkAttributes(attrs, "aaaa", "bbbb");
        checkAttributes(attrs, StyleConstants.NameAttribute, Tag.PRE);
        checkAttributes(attrs, CSS.Attribute.WHITE_SPACE, "pre");
        blockOpen.reset();
        
        action.end(Tag.PRE);
        assertFalse(blockOpen.isOccurred());
        assertTrue(blockClose.isOccurred());
        callInfo = (ArrayList)blockClose.getAuxiliary();
        assertEquals(2, callInfo.size());
        assertEquals(Tag.IMPLIED, callInfo.get(0));
        assertEquals(Tag.PRE, callInfo.get(1));
    }
    
    public void testPreStartEnd_PreContentCalls() {
        final Marker preContentMarker = new Marker();
        doc = new HTMLDocument() {
            public ParserCallback getReader(int pos) {
                return new HTMLReader(0) {
                    protected void preContent(char[] data) {
                        preContentMarker.setOccurred();
                        preContentMarker.setAuxiliary(data);
                        super.preContent(data);
                    }
                };
            }
        };
        reader = (HTMLReader)doc.getReader(0);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new PreAction();
        
        action.start(Tag.PRE, attr);
        String text = "data";
        assertFalse(preContentMarker.isOccurred());
        reader.handleText(text.toCharArray(), 0);
        assertTrue(preContentMarker.isOccurred());
        assertEquals(text, new String((char [])preContentMarker.getAuxiliary()));
        action.end(Tag.PRE);
        preContentMarker.reset();
        reader.handleText(text.toCharArray(), 0);
        assertFalse(preContentMarker.isOccurred());
    }
    
    public void testPre_ContentWhitespaces1() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new PreAction();
        
        action.start(Tag.PRE, attr);
        String text = "data       \t \r \f  data";
        assertEquals(2, reader.parseBuffer.size());
        reader.handleText(text.toCharArray(), 0);
        
        assertEquals(7, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(6);
        assertEquals(text.length(), spec.getArray().length);
    }
    
    public void testPre_ContentWhitespaces2() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new PreAction();
        
        action.start(Tag.PRE, attr);
        String text = "data       \t \r \f  data";
        assertEquals(2, reader.parseBuffer.size());
        reader.preContent(text.toCharArray());
        
        assertEquals(7, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(6);
        assertEquals(text.length(), spec.getArray().length);
    }
    
    public void testHarmony_4582() throws Exception {
        final Element pre;
        final HTMLDocument doc = new HTMLDocument();

        new HTMLEditorKit().read(new StringReader("<html><body><pre>line1\n" //$NON-NLS-1$
                + "line2</pre></body></html>"), doc, 0); //$NON-NLS-1$

        assertEquals("line1", doc.getText(1, 5)); //$NON-NLS-1$
        assertEquals("line2", doc.getText(7, 5)); //$NON-NLS-1$

        pre = doc.getRootElements()[0].getElement(1).getElement(0);
        assertEquals(1, pre.getElement(0).getStartOffset());
        assertEquals(7, pre.getElement(0).getEndOffset());
        assertEquals(7, pre.getElement(1).getStartOffset());
        assertEquals(13, pre.getElement(1).getEndOffset());
    }
    
    public void testHarmony_4615() throws Exception {
        final HTMLDocument doc = new HTMLDocument();

        new HTMLEditorKit().read(new StringReader("<html><body><pre>line1\n" //$NON-NLS-1$
                + "<font color='red'>line2 \n line3</font>" //$NON-NLS-1$
                + "line3</pre>line4 \n line4</body></html>"), doc, 0); //$NON-NLS-1$

        assertEquals("line1\n", doc.getText(1, 6)); //$NON-NLS-1$
        assertEquals("line2 \n line3", doc.getText(7, 13)); //$NON-NLS-1$
        assertEquals("line3", doc.getText(20, 5)); //$NON-NLS-1$
        assertEquals("line4 line4", doc.getText(26, 11)); //$NON-NLS-1$
    }
    
    public void testTag_ContentWhitespaces() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new PreAction();
        
        action.start(Tag.P, attr);
        String text = "data       \t \r \f  data";
        assertEquals(2, reader.parseBuffer.size());
        reader.handleText(text.toCharArray(), 0);
        
        assertEquals(3, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(2);
        assertEquals(text.length(), spec.getLength());
        assertEquals(text.length(), spec.getArray().length);
    }
    
    public void testPreEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        HTMLDocument.HTMLReader.TagAction action = reader.new PreAction();
        
        action.end(Tag.PRE);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());

        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkEndTagSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(1);
        checkEndTagSpec(spec);
    }

    public void testIsindexStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        final String prompt = "text";
        attr.addAttribute(HTML.Attribute.PROMPT, prompt);
        action = reader.new IsindexAction();
        
        action.start(Tag.ISINDEX, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(5, reader.parseBuffer.size());

        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        checkOpenImpliedSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(2);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.ISINDEX);
        checkAttributes(specAttr, "aaaa", "bbbb");
        checkAttributes(specAttr, HTML.Attribute.PROMPT, prompt);
        checkImplicitContentSpec(spec);
        assertNotSame(specAttr, attr);

        spec = (ElementSpec)reader.parseBuffer.get(3);
        checkCRSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);
    }

    public void testIsindexStart_InParagraph() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        final String prompt = "text";
        attr.addAttribute(HTML.Attribute.PROMPT, prompt);
        action = reader.new IsindexAction();
        
        reader.handleStartTag(Tag.P, new SimpleAttributeSet(), 0);
        assertEquals(1, reader.parseBuffer.size());

        action.start(Tag.ISINDEX, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(5, reader.parseBuffer.size());

        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(1, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.P);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        checkOpenImpliedSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(2);
        specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.ISINDEX);
        checkAttributes(specAttr, "aaaa", "bbbb");
        checkAttributes(specAttr, HTML.Attribute.PROMPT, prompt);
        checkImplicitContentSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(3);
        checkCRSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);

        reader.blockClose(Tag.IMPLIED);
        int numSpecs = reader.parseBuffer.size();
        reader.handleText("text".toCharArray(), 0);
        assertEquals(numSpecs, reader.parseBuffer.size());
    }
    
    public void testIsindex_Calls() {
        final Marker blockOpenMarker = new Marker();
        final Marker blockCloseMarker = new Marker();
        final Marker contentMarker = new Marker();
        doc = new HTMLDocument() {
            public ParserCallback getReader(int pos) {
                return new HTMLReader(0) {
                    protected void blockOpen(Tag tag, MutableAttributeSet attr) {
                        blockOpenMarker.setOccurred();
                        ArrayList callInfo = (blockOpenMarker.getAuxiliary() == null)
                                              ? new ArrayList() 
                                              : (ArrayList)blockOpenMarker.getAuxiliary();
                        callInfo.add(tag);
                        callInfo.add(attr.copyAttributes());
                        blockOpenMarker.setAuxiliary(callInfo);
                        super.blockOpen(tag, attr);
                    }

                    protected void blockClose(Tag tag) {
                        super.blockClose(tag);
                        ArrayList callInfo = (blockCloseMarker.getAuxiliary() == null)
                                              ? new ArrayList() 
                                              : (ArrayList)blockCloseMarker.getAuxiliary();
                        blockCloseMarker.setOccurred();
                        callInfo.add(tag);
                        blockCloseMarker.setAuxiliary(callInfo);
                    }
                    
                   protected void addContent(char[] data, int offs, int length, boolean generateImpliedPIfNecessary) {
                       ArrayList callInfo = (contentMarker.getAuxiliary() == null)
                                            ? new ArrayList() 
                                            : (ArrayList)contentMarker.getAuxiliary();
                       contentMarker.setOccurred();
                       callInfo.add(data);
                       callInfo.add(new Integer(offs));
                       callInfo.add(new Integer(length));
                       callInfo.add(Boolean.valueOf(generateImpliedPIfNecessary));
                       contentMarker.setAuxiliary(callInfo);
                       super.addContent(data, offs, length, generateImpliedPIfNecessary);
                   }
                };
            }
        };
        reader = (HTMLReader)doc.getReader(0);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new IsindexAction();
        
        action.start(Tag.ISINDEX, attr);
        assertTrue(blockOpenMarker.isOccurred());
        
        SimpleAttributeSet a = new SimpleAttributeSet();
        ArrayList callInfo = (ArrayList)blockOpenMarker.getAuxiliary();
        assertEquals(2, callInfo.size()/2);
        assertEquals(Tag.IMPLIED, callInfo.get(0));
        assertEquals(a, callInfo.get(1));
        assertEquals(Tag.IMPLIED, callInfo.get(2));
        assertEquals(a, callInfo.get(3));

        assertTrue(blockCloseMarker.isOccurred());
        callInfo = (ArrayList)blockCloseMarker.getAuxiliary();
        assertEquals(1, callInfo.size());
        assertEquals(Tag.IMPLIED, callInfo.get(0));

        assertTrue(contentMarker.isOccurred());
        callInfo = (ArrayList)contentMarker.getAuxiliary();
        assertEquals(4, callInfo.size());
        final char[] data = (char[])callInfo.get(0);
        assertEquals(1, data.length);
        assertEquals('\n', data[0]);
        assertEquals(new Integer(0), callInfo.get(1));
        assertEquals(new Integer(1), callInfo.get(2));
        assertEquals(Boolean.TRUE, callInfo.get(3));
        
        
    }
    
    public void testHiddenStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new HiddenAction();

        action.start(Tag.SCRIPT, attr);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr;
        checkOpenImpliedSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.SCRIPT);
        checkAttributes(specAttr, "aaaa", "bbbb");
        checkImplicitContentSpec(spec);
        
        assertNotSame(specAttr, attr);
    }
    
    public void testHiddenEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new HiddenAction();
        
        action.start(Tag.SCRIPT, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        action.end(Tag.SCRIPT);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());

        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(2);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.SCRIPT);
        checkAttributes(specAttr, HTML.Attribute.ENDTAG, Boolean.TRUE);
        checkImplicitContentSpec(spec);
    }
    
    public void testBaseStart() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        final String url1 = "http://www.aaa.ru/aaa";
        final String url2 = "http://www.bbb.ru";
        final String tail1 = "/bbb#bbb";
        final String tail2 = "dumb/dumm/#attr";
        final String target = "target";
        
        attr.addAttribute(HTML.Attribute.HREF, tail2);
        reader.handleSimpleTag(Tag.BASE, attr, 0);
        assertNull(doc.getBase());
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());

        doc.setBase(new URL(url2));

        attr.addAttribute(HTML.Attribute.HREF, url1 + tail1);
        reader.handleSimpleTag(Tag.BASE, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(url1 + tail1, doc.getBase().toString());

        attr.addAttribute(HTML.Attribute.HREF, tail2);
        reader.handleSimpleTag(Tag.BASE, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(url1 + "/" + tail2, doc.getBase().toString());
    }
    
    public void testStyle_InHead() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new StyleAction();
      
        reader.handleStartTag(Tag.HEAD, new SimpleAttributeSet(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        
        action.start(Tag.STYLE, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        
        final String text = "H1 { color: blue }";
        reader.handleText(text.toCharArray(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());

        assertEquals(0, doc.getStyleSheet().getRule("BODY").getAttributeCount());
        action.end(Tag.STYLE);
        Style rule = doc.getStyleSheet().getRule("h1");
        assertEquals(0, rule.getAttributeCount());

        reader.handleEndTag(Tag.HEAD, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(5, reader.parseBuffer.size());
        
        rule = doc.getStyleSheet().getRule("h1");
        assertEquals(2, rule.getAttributeCount());
        assertEquals("h1", rule.getAttribute(StyleConstants.NameAttribute));
        assertEquals("blue", rule.getAttribute(CSS.Attribute.COLOR).toString());
    }

    public void testStyle_InHead_Twice() throws Exception {
        final String text1 = "H1 { color: blue }";
        final String text2 = "H2 { color: red }";

        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new StyleAction();
      
        reader.handleStartTag(Tag.HEAD, new SimpleAttributeSet(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        
        action.start(Tag.STYLE, attr);
        assertEquals(1, reader.parseBuffer.size());
        
        reader.handleText(text1.toCharArray(), 0);
        assertEquals(1, reader.parseBuffer.size());

        assertEquals(0, doc.getStyleSheet().getRule("h1").getAttributeCount());
        assertEquals(0, doc.getStyleSheet().getRule("h2").getAttributeCount());
        action.end(Tag.STYLE);
        assertEquals(0, doc.getStyleSheet().getRule("h1").getAttributeCount());
        assertEquals(0, doc.getStyleSheet().getRule("h2").getAttributeCount());

        action.start(Tag.STYLE, attr);
        assertEquals(1, reader.parseBuffer.size());
        
        reader.handleText(text2.toCharArray(), 0);
        assertEquals(1, reader.parseBuffer.size());

        assertEquals(0, doc.getStyleSheet().getRule("h1").getAttributeCount());
        assertEquals(0, doc.getStyleSheet().getRule("h2").getAttributeCount());
        action.end(Tag.STYLE);
        assertEquals(0, doc.getStyleSheet().getRule("h1").getAttributeCount());
        assertEquals(0, doc.getStyleSheet().getRule("h2").getAttributeCount());

        reader.handleEndTag(Tag.HEAD, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(5, reader.parseBuffer.size());
        
        Style rule = doc.getStyleSheet().getRule("h1");
        assertEquals(2, rule.getAttributeCount());
        assertEquals("h1", rule.getAttribute(StyleConstants.NameAttribute));
        assertEquals("blue", rule.getAttribute(CSS.Attribute.COLOR).toString());

        rule = doc.getStyleSheet().getRule("h2");
        assertEquals(2, rule.getAttributeCount());
        assertEquals("h2", rule.getAttribute(StyleConstants.NameAttribute));
        assertEquals("red", rule.getAttribute(CSS.Attribute.COLOR).toString());
    }

    public void testStyle() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new StyleAction();
      
        action.start(Tag.STYLE, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
        
        final String text = "H1 { color: blue }";
        reader.handleText(text.toCharArray(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());

        assertEquals(0, doc.getStyleSheet().getRule("BODY").getAttributeCount());
        action.end(Tag.STYLE);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
        final Style rule = doc.getStyleSheet().getRule("h1");
        assertEquals(0, rule.getAttributeCount());
    }

    public void testTitleStart() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.handleStartTag(Tag.TITLE, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(1, specAttr.getAttributeCount());
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.TITLE);
        checkAttributes(specAttr, "aaaa", "bbbb");
        checkImplicitContentSpec(spec);
        assertNotSame(attr, specAttr);
    }
    
    public void testTitleText() throws Exception {
        final String title = "brand new title";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader.handleStartTag(Tag.TITLE, attr, 0);

        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        reader.handleText(title.toCharArray(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        
        assertEquals(title, doc.getProperty(HTMLDocument.TitleProperty));
    }
    
    public void testTitleEnd() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader.handleStartTag(Tag.TITLE, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        reader.handleEndTag(Tag.TITLE, 0);
        
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(2);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.TITLE);
        checkAttributes(specAttr, HTML.Attribute.ENDTAG, Boolean.TRUE);
        checkImplicitContentSpec(spec);
    }
    
    public void testTitleEnd_Impied() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader.handleEndTag(Tag.TITLE, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr;
        checkOpenImpliedSpec(spec);
        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.TITLE);
        checkAttributes(specAttr, HTML.Attribute.ENDTAG, Boolean.TRUE);
    }
    
    public void testLinkStart() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.handleStartTag(Tag.LINK, attr, 0);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr;
        checkOpenImpliedSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.LINK);
        checkAttributes(specAttr, "aaaa", "bbbb");
        checkImplicitContentSpec(spec);
    }
    
    public void testLinkStart_InTitle() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.handleStartTag(Tag.TITLE, attr, 0);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.handleStartTag(Tag.LINK, attr, 0);
        assertEquals(3, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(2);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.LINK);
        checkAttributes(specAttr, "aaaa", "bbbb");
        checkImplicitContentSpec(spec);
    }

    public void testLinkEnd() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.handleStartTag(Tag.LINK, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        reader.handleEndTag(Tag.LINK, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
    }
    
    public void testLinkEnd_InTitle() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.handleStartTag(Tag.TITLE, attr, 0);
        reader.handleStartTag(Tag.LINK, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());
        reader.handleEndTag(Tag.LINK, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());
    }
    
    public void testLink_LoadCSS() throws Exception {
        if (!isHarmony()) {
            return;
        }
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.TYPE, "text/css");
        URL url = HTMLEditorKit.class.getResource(HTMLEditorKit.DEFAULT_CSS);
        attr.addAttribute(HTML.Attribute.HREF, url.toString());
        
        assertEquals(0, new SimpleAttributeSet(doc.getStyleSheet().getRule("h3")).getAttributeCount());

        reader.handleStartTag(Tag.TITLE, new SimpleAttributeSet(), 0);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.handleStartTag(Tag.LINK, attr, 0);
        assertEquals(3, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        assertTrue(new SimpleAttributeSet(doc.getStyleSheet().getRule("h3")).getAttributeCount() > 0);
    }

    public void testAreaStart() throws Exception {
        // TODO: implement
    }
    
    public void testAreaEnd() throws Exception {
        // TODO: implement
    }
    
    public void testMetaStart() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader.handleStartTag(Tag.META, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr;
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertEquals(1, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.META);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{' '});
    }
    
    public void testMetaStart_InTitle() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.handleStartTag(Tag.TITLE, attr, 0);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.handleStartTag(Tag.META, attr, 0);
        assertEquals(3, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(2);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.META);
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{' '});
    }
    
    public void testMetaEnd() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.handleStartTag(Tag.META, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        reader.handleEndTag(Tag.META, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
    }
    
    public void testFormStart() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        
        action.start(Tag.FORM, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        AttributeSet specAttr;
        checkOpenImpliedSpec(spec);
        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.FORM);
        checkAttributes(specAttr, "aaaa", "bbbb");
        checkImplicitContentSpec(spec);

        assertEquals(2, attr.getAttributeCount());
        checkAttributes(attr, StyleConstants.NameAttribute, Tag.FORM);
        checkAttributes(attr, "aaaa", "bbbb");
    }

    public void testFormEnd() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        action = reader.new FormAction();
        
        action.end(Tag.FORM);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
    }

    public void testLabelStart() throws Exception {
        // TODO: implement
    }
    
    public void testLabelEnd() throws Exception {
        // TODO: implement
    }
    
    private void checkCRSpec(final ElementSpec spec) {
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(specAttr, "CR", Boolean.TRUE);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
    }
}
