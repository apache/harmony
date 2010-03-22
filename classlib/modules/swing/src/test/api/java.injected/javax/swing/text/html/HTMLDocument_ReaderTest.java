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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument.HTMLReader;
import javax.swing.text.html.HTMLDocument.HTMLReader.TagAction;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

public class HTMLDocument_ReaderTest extends HTMLDocumentTestCase {
    
    protected HTMLReader reader;
    protected HTMLDocument doc;
    private Marker createMarker;
    private Marker insertMarker;
    private boolean editable; 
    
    protected void tearDown() throws Exception {
        doc = null;
        reader = null;
        super.tearDown();
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        init();
    }
    
    public void testFlush_Create() throws Exception {
        final String text = "tag";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.StartTagType));
        reader.parseBuffer.add(new ElementSpec(new SimpleAttributeSet(), ElementSpec.ContentType, text.toCharArray(), 0, 3));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        assertEquals(3, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertTrue(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        assertEquals(3, ((ElementSpec[])createMarker.getAuxiliary()).length);
    }
    
    public void testFlush_NoCreate_IfEmptyBuffer() throws Exception {
        final String text = "tag";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        assertEquals(0, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testFlush_Create_PushPopNull() throws Exception {
        final String text = "tag";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B);
        reader = (HTMLReader)doc.getReader(0, 5, 5, null);
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.StartTagType));
        reader.parseBuffer.add(new ElementSpec(new SimpleAttributeSet(), ElementSpec.ContentType, text.toCharArray(), 0, 3));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        assertEquals(3, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertTrue(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        assertEquals(3, ((ElementSpec[])createMarker.getAuxiliary()).length);
    }
    
    public void testFlush_Insert_Tag() throws Exception {
        final String text = "tag";
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B);
        reader = (HTMLReader)doc.getReader(0, 0, 0, Tag.B);
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.StartTagType));
        reader.parseBuffer.add(new ElementSpec(new SimpleAttributeSet(), ElementSpec.ContentType, text.toCharArray(), 0, 3));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        assertEquals(3, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertTrue(insertMarker.isOccurred());
        assertEquals(3, ((ElementSpec[])insertMarker.getAuxiliary()).length);
        assertEquals(text, doc.getText(0, doc.getLength()));
    }
    
    public void testFlush_Insert_PushPopTag() throws Exception {
        final String text = "tag";
        editable = false;
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B);
        reader = (HTMLReader)doc.getReader(0, 15, 33, Tag.B);
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.StartTagType));
        reader.parseBuffer.add(new ElementSpec(new SimpleAttributeSet(), ElementSpec.ContentType, text.toCharArray(), 0, 3));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        assertEquals(3, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertTrue(insertMarker.isOccurred());
        assertEquals(3, ((ElementSpec[])insertMarker.getAuxiliary()).length);
    }
    
    public void testFlush_Insert_PushPopTag_Wierd() throws Exception {
        final String text = "tag";
        editable = false;
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.CONTENT);
        reader = (HTMLReader)doc.getReader(1000, -15, 330, Tag.HTML);
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.StartTagType));
        reader.parseBuffer.add(new ElementSpec(new SimpleAttributeSet(), ElementSpec.ContentType, text.toCharArray(), 0, 3));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.StartTagType));
        assertEquals(6, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertTrue(insertMarker.isOccurred());
        assertEquals(6, ((ElementSpec[])insertMarker.getAuxiliary()).length);
    }
    
    public void testFlush_Insert_PushPopTag_ApplicationMoment1() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader = (HTMLReader)doc.getReader(1, 10, 100, Tag.I);
        reader.handleStartTag(Tag.I, new SimpleAttributeSet(), 0);
        assertEquals(110, reader.parseBuffer.size());
    }
    
    public void testFlush_Insert_PushPopTag_ApplicationMoment2() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader = (HTMLReader)doc.getReader(-100, 10, 100, Tag.I);
        reader.registerTag(Tag.I, reader.new TagAction());
        reader.handleStartTag(Tag.I, new SimpleAttributeSet(), 1000);
        reader.handleSimpleTag(Tag.I, new SimpleAttributeSet(), 1000);
        reader.handleSimpleTag(Tag.BR, new SimpleAttributeSet(), 1000);
        assertEquals(0, reader.parseBuffer.size());
    }
    
    public void testFlush_Insert_PushPopTag_ApplicationMoment3() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader = (HTMLReader)doc.getReader(-100, 10, 100, Tag.I);
        reader.registerTag(Tag.I, reader.new BlockAction());
        reader.handleStartTag(Tag.I, new SimpleAttributeSet(), 1000);
        assertEquals(111, reader.parseBuffer.size());
    }
    
    public void testFlush_Insert_PushPopTag_ApplicationMoment4() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader = (HTMLReader)doc.getReader(-100, 10, 100, Tag.I);
        reader.new BlockAction().start(Tag.I, new SimpleAttributeSet());
        assertEquals(111, reader.parseBuffer.size());
    }
    
    public void testFlush_Insert_PushPopTag_ApplicationMoment5() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader = (HTMLReader)doc.getReader(-100, 0, 0, Tag.I);
        reader.registerTag(Tag.I, reader.new CharacterAction());
        reader.handleStartTag(Tag.I, new SimpleAttributeSet(), 1000);
        assertEquals(0, reader.parseBuffer.size());
        reader.handleSimpleTag(Tag.IMG, new SimpleAttributeSet(), 100);
        assertEquals(1, reader.parseBuffer.size());
    }
    
    public void testFlush_Insert_PushPopTag_SpecsOrder() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        reader = (HTMLReader)doc.getReader(-100, 2, 3, Tag.I);
        reader.handleStartTag(Tag.I, new SimpleAttributeSet(), 1000);
        assertEquals(5, reader.parseBuffer.size());
        for (int i = 0; i < 5; i++) {
            checkSpecType("eoeosnsnsn", 0, (ElementSpec)reader.parseBuffer.get(0));
        }
    }
    
    public void testFlush_Insert() throws Exception {
        final String text = "tag";
        final String initialText = "text";
        doc.insertString(0, initialText, SimpleAttributeSet.EMPTY);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.StartTagType));
        reader.parseBuffer.add(new ElementSpec(SimpleAttributeSet.EMPTY, ElementSpec.ContentType, text.toCharArray(), 0, 3));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        assertEquals(3, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertTrue(insertMarker.isOccurred());
        assertEquals(3, ((ElementSpec[])insertMarker.getAuxiliary()).length);
        assertEquals(text + initialText, doc.getText(0, doc.getLength()));
    }
    
    public void testFlush_NoInsert_IfEmptyBuffer() throws Exception {
        final String text = "tag";
        final String initialText = "text";
        doc.insertString(0, initialText, SimpleAttributeSet.EMPTY);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        assertEquals(0, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        if (!isHarmony()) {
            assertTrue(insertMarker.isOccurred());
            assertEquals(0, ((ElementSpec[])insertMarker.getAuxiliary()).length);
        } else {
            assertFalse(insertMarker.isOccurred());
        }
    }
    
    public void testFlush_Insert_Offset() throws Exception {
        final String text = "tag";
        final String initialText = "text";
        reader = (HTMLReader)doc.getReader(initialText.length());
        doc.insertString(0, initialText, new SimpleAttributeSet());
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.StartTagType));
        reader.parseBuffer.add(new ElementSpec(new SimpleAttributeSet(), ElementSpec.ContentType, text.toCharArray(), 0, 3));
        reader.parseBuffer.add(new ElementSpec(attr, ElementSpec.EndTagType));
        assertEquals(3, reader.parseBuffer.size());
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertTrue(insertMarker.isOccurred());
        assertEquals(3, ((ElementSpec[])insertMarker.getAuxiliary()).length);
        assertEquals(initialText + text, doc.getText(0, doc.getLength()));
    }
    
    public void testHandleComment() {
        String text1 = "data";
        String text2 = "datadata";
        reader.handleComment(text1.toCharArray(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        Vector comments = (Vector)doc.getProperty(HTMLDocument.AdditionalComments);
        assertEquals(1, comments.size());
        assertEquals(text1, comments.get(0));
        
        reader.handleStartTag(Tag.P, new SimpleAttributeSet(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        reader.handleComment(text1.toCharArray(), 100);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.COMMENT);
        checkAttributes(spec.getAttributes(), HTML.Attribute.COMMENT, text1);
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        reader.handleEndTag(Tag.P, 0);
        assertEquals(1, comments.size());

        reader.handleComment(text2.toCharArray(), 0);
        assertEquals(2, comments.size());
        assertEquals(text2, comments.get(1));
    }
    
    public void testHandleEndOfLineString() throws Exception {
        String text1 = "text1";
        String text2 = "text2";
        
        reader.handleEndOfLineString(text1);
        assertEquals(text1, doc.getProperty(DefaultEditorKit.EndOfLineStringProperty));
        
        reader.handleEndOfLineString(text2);
        assertEquals(text2, doc.getProperty(DefaultEditorKit.EndOfLineStringProperty));
    }
    
    public void testHandleSimpleTag() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        final Marker endMarker = new Marker();
        final Marker startMarker = new Marker();
        Tag tag = new Tag("mytag");
        reader.registerTag(tag, reader.new TagAction() {
            public void end(final Tag tag) {
                endMarker.setAuxiliary(tag);
                endMarker.setOccurred();
            }
            
            public void start(final Tag tag, final MutableAttributeSet attr) {
                startMarker.setOccurred();
                startMarker.setAuxiliary(attr);
            }
        });
        reader.handleSimpleTag(tag, attr, 1);
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertTrue(endMarker.isOccurred());
        assertEquals(tag, endMarker.getAuxiliary());
        assertTrue(startMarker.isOccurred());
        assertSame(attr, startMarker.getAuxiliary());
    }
    
    public void testHandleEndTag() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        final Marker endMarker = new Marker();
        final Marker startMarker = new Marker();
        Tag tag = new Tag("mytag");
        reader.registerTag(tag, reader.new TagAction() {
            public void end(final Tag tag) {
                endMarker.setAuxiliary(tag);
                endMarker.setOccurred();
            }
            
            public void start(final Tag tag, final MutableAttributeSet attr) {
                startMarker.setOccurred();
                startMarker.setAuxiliary(attr);
            }
        });
        reader.handleEndTag(tag, 0);
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertTrue(endMarker.isOccurred());
        assertEquals(tag, endMarker.getAuxiliary());
        assertFalse(startMarker.isOccurred());
    }
    
    public void testHandleStartTag() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        final Marker endMarker = new Marker();
        final Marker startMarker = new Marker();
        Tag tag = new Tag("mytag");
        reader.registerTag(tag, reader.new TagAction() {
            public void end(final Tag tag) {
                endMarker.setAuxiliary(tag);
                endMarker.setOccurred();
            }
            
            public void start(final Tag tag, final MutableAttributeSet attr) {
                startMarker.setOccurred();
                startMarker.setAuxiliary(attr);
            }
        });
        reader.handleStartTag(tag, attr, 0);
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertTrue(startMarker.isOccurred());
        assertSame(attr, startMarker.getAuxiliary());
        assertFalse(endMarker.isOccurred());
    }
    
    public void testHandleStartTag_StyleAttr() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.STYLE, "color: red");
        reader.handleStartTag(Tag.P, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        final AttributeSet specAttr = spec.getAttributes();
        assertNotSame(attr, specAttr);
        assertNull(specAttr.getAttribute(HTML.Attribute.STYLE));
        checkAttributes(specAttr, CSS.Attribute.COLOR, "red");
    }

    public void testHandleSimpleTag_StyleAttr() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.STYLE, "color: red");
        reader.handleSimpleTag(Tag.HR, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertNotSame(attr, specAttr);
        assertNull(specAttr.getAttribute(HTML.Attribute.STYLE));
        checkAttributes(specAttr, CSS.Attribute.COLOR, "red");
    }

    public void testHandleText() throws Exception {
        String text = "data";
        reader.handleText(text.toCharArray(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        
        reader.handleStartTag(Tag.P, new SimpleAttributeSet(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        reader.handleText(text.toCharArray(), 0);
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, text.toCharArray());
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(1, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testHandleText_ContentMethodsCalls_Body() throws Exception {
        final Marker addContentMarker = new Marker(); 
        final Marker preContentMarker = new Marker(); 
        final Marker textAreaMarker = new Marker(); 
        createContentMarkersInstrumentedReader(addContentMarker, preContentMarker, textAreaMarker);
        String text = "data";

        reader.handleText(text.toCharArray(), 0);
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());
        
        reader.handleStartTag(Tag.BODY, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());
        addContentMarker.reset();
        
        reader.handleEndTag(Tag.BODY, 0);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());
        addContentMarker.reset();
    }

    public void testHandleText_ContentMethodsCalls_P() throws Exception {
        final Marker addContentMarker = new Marker(); 
        final Marker preContentMarker = new Marker(); 
        final Marker textAreaMarker = new Marker(); 
        createContentMarkersInstrumentedReader(addContentMarker, preContentMarker, textAreaMarker);
        String text = "data";

        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        
        reader.handleStartTag(Tag.P, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());
        addContentMarker.reset();
        
        reader.handleEndTag(Tag.P, 0);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());
        addContentMarker.reset();
    }
    
    public void testHandleText_ContentMethodsCalls_Implied() throws Exception {
        final Marker addContentMarker = new Marker(); 
        final Marker preContentMarker = new Marker(); 
        final Marker textAreaMarker = new Marker(); 
        createContentMarkersInstrumentedReader(addContentMarker, preContentMarker, textAreaMarker);
        String text = "data";

        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.handleText(text.toCharArray(), 0);
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());
        addContentMarker.reset();
        
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());
        addContentMarker.reset();
    }
    
    public void testHandleText_ContentMethodsCalls_Pre() throws Exception {
        final Marker addContentMarker = new Marker(); 
        final Marker preContentMarker = new Marker(); 
        final Marker textAreaMarker = new Marker(); 
        createContentMarkersInstrumentedReader(addContentMarker, preContentMarker, textAreaMarker);
        String text = "data";

        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        
        reader.handleStartTag(Tag.PRE, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertTrue(addContentMarker.isOccurred());
        assertTrue(preContentMarker.isOccurred());
        addContentMarker.reset();
        preContentMarker.reset();
        
        reader.handleEndTag(Tag.PRE, 0);
        reader.handleText(text.toCharArray(), 0);
        if (!isHarmony()) {
            assertTrue(addContentMarker.isOccurred());
        }
        assertFalse(preContentMarker.isOccurred());
    }
    
    public void testHandleText_ContentMethodsCalls_Title() throws Exception {
        final Marker addContentMarker = new Marker(); 
        final Marker preContentMarker = new Marker(); 
        final Marker textAreaMarker = new Marker(); 
        createContentMarkersInstrumentedReader(addContentMarker, preContentMarker, textAreaMarker);
        String text = "data";

        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        
        reader.handleStartTag(Tag.TITLE, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());

        reader.handleEndTag(Tag.TITLE, 0);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        assertFalse(textAreaMarker.isOccurred());
    }
    
    public void testHandleText_ContentMethodsCalls_Option() throws Exception {
        final Marker addContentMarker = new Marker(); 
        final Marker preContentMarker = new Marker(); 
        final Marker textAreaMarker = new Marker(); 
        createContentMarkersInstrumentedReader(addContentMarker, preContentMarker, textAreaMarker);
        String text = "data";

        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        
        reader.handleStartTag(Tag.OPTION, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());

        reader.handleEndTag(Tag.OPTION, 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());

        reader.handleStartTag(Tag.P, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        addContentMarker.reset();

        reader.handleStartTag(Tag.OPTION, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        if (!isHarmony()) {
            assertFalse(addContentMarker.isOccurred());
        } else {
            assertTrue(addContentMarker.isOccurred());
        }
        assertFalse(preContentMarker.isOccurred());

        reader.handleEndTag(Tag.OPTION, 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        addContentMarker.reset();
    }
    
    public void testHandleText_ContentMethodsCalls_TextArea() throws Exception {
        final Marker addContentMarker = new Marker(); 
        final Marker preContentMarker = new Marker(); 
        final Marker textAreaMarker = new Marker(); 
        createContentMarkersInstrumentedReader(addContentMarker, preContentMarker, textAreaMarker);
        String text = "data";

        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        
        reader.handleStartTag(Tag.TEXTAREA, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        textAreaMarker.reset();

        reader.handleEndTag(Tag.TEXTAREA, 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        addContentMarker.reset();

        reader.handleStartTag(Tag.TEXTAREA, new SimpleAttributeSet(), 0);
        reader.handleStartTag(Tag.SELECT, new SimpleAttributeSet(), 0);
        reader.handleStartTag(Tag.OPTION, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(textAreaMarker.isOccurred());
        assertFalse(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        textAreaMarker.reset();

        reader.handleStartTag(Tag.PRE, new SimpleAttributeSet(), 0);
        reader.handleText(text.toCharArray(), 0);
        assertTrue(textAreaMarker.isOccurred());
        // this is caused by blockOpen(IMPLIED) call
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        textAreaMarker.reset();
        addContentMarker.reset();

        reader.handleEndTag(Tag.TEXTAREA, 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertTrue(addContentMarker.isOccurred());
        assertTrue(preContentMarker.isOccurred());
        preContentMarker.reset();
        addContentMarker.reset();

        reader.handleEndTag(Tag.PRE, 0);
        reader.handleText(text.toCharArray(), 0);
        assertFalse(textAreaMarker.isOccurred());
        assertTrue(addContentMarker.isOccurred());
        assertFalse(preContentMarker.isOccurred());
        addContentMarker.reset();
    }
    
    public void testHandleText_Implied() throws Exception {
        String text = "data";
        final SimpleAttributeSet attr = new SimpleAttributeSet();
        reader.handleStartTag(Tag.BODY, attr, 0);
        final Tag tag = Tag.SUB;
        reader.handleStartTag(tag, attr, 0);
        assertEquals(2, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(attr, reader.charAttr.getAttribute(tag));
        
        reader.handleText(text.toCharArray(), 0);
        assertEquals(3, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertEquals(3, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(2);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, text.toCharArray());
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(3, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(specAttr, tag, attr);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        checkOpenImpliedSpec(spec);
        
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testHandleText_BlockOpenClose_P1() {
        reader.blockOpen(Tag.H1, new SimpleAttributeSet());
        reader.blockClose(Tag.H1);
        checkTextInNotInserted();
    }
    
    public void testHandleText_BlockOpenClose_P2() {
        reader.blockOpen(Tag.H1, new SimpleAttributeSet());
        reader.blockOpen(Tag.H1, new SimpleAttributeSet());
        reader.blockOpen(Tag.H1, new SimpleAttributeSet());
        reader.blockClose(Tag.H1);
        checkTextIsInserted();
    }
    
    public void testHandleText_BlockOpenClose_P3() {
        reader.blockOpen(Tag.H1, new SimpleAttributeSet());
        reader.blockOpen(Tag.H2, new SimpleAttributeSet());
        reader.blockOpen(Tag.H3, new SimpleAttributeSet());
        reader.blockClose(Tag.H2);
        reader.blockClose(Tag.H3);
        reader.blockClose(Tag.P);
        checkTextInNotInserted();
    }
    
    public void testHandleText_BlockOpenClose_Implied1() {
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockClose(Tag.IMPLIED);
        checkTextIsInserted();
    }
    
    public void testHandleText_BlockOpenClose_Implied2() {
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        checkTextInNotInserted();
    }
    
    public void testHandleText_BlockOpenClose_Implied3() {
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        checkTextIsInserted();
    }
    
    public void testHandleText_BlockOpenClose_P_Implied1() {
        reader.blockOpen(Tag.P, new SimpleAttributeSet());
        reader.blockOpen(Tag.P, new SimpleAttributeSet());
        reader.blockOpen(Tag.P, new SimpleAttributeSet());
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        checkTextIsInserted();
    }

    public void testHandleText_BlockOpenClose_P_Implied2() {
        reader.blockOpen(Tag.P, new SimpleAttributeSet());
        reader.blockOpen(Tag.P, new SimpleAttributeSet());
        reader.blockOpen(Tag.P, new SimpleAttributeSet());
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        reader.blockClose(Tag.IMPLIED);
        checkTextInNotInserted();
    }

    public void testHandleText_BlockOpenClose_P_Implied3() {
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockClose(Tag.P);
        reader.blockClose(Tag.P);
        reader.blockClose(Tag.P);
        checkTextInNotInserted();
    }
    
    public void testHandleText_BlockOpenClose_P_Implied4() {
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockOpen(Tag.IMPLIED, new SimpleAttributeSet());
        reader.blockClose(Tag.P);
        reader.blockClose(Tag.P);
        checkTextIsInserted();
    }

    public void testAddContent_BlockOpenClose_P1() {
        reader.blockOpen(Tag.H1, new SimpleAttributeSet());
        reader.blockClose(Tag.H1);
        int numSpecs = reader.parseBuffer.size();
        reader.addContent("text".toCharArray(), 0, 4, true);
        assertEquals(numSpecs + 2, reader.parseBuffer.size());
    }
    
    public void testAddContent_BlockOpenClose_P2() {
        reader.blockOpen(Tag.H1, new SimpleAttributeSet());
        reader.blockClose(Tag.H1);
        int numSpecs = reader.parseBuffer.size();
        reader.addContent("text".toCharArray(), 0, 4, false);
        assertEquals(numSpecs + 1, reader.parseBuffer.size());
    }
    
    public void testAddContent_BlockOpenClose_P_Implied5() {
        TagAction action = reader.new ParagraphAction(); 
        reader.handleStartTag(Tag.BODY, new SimpleAttributeSet(), 0);
        assertEquals(1, reader.parseBuffer.size());
        reader.handleText("0000".toCharArray(), 0);
        assertEquals(3, reader.parseBuffer.size());
        action.start(Tag.P, new SimpleAttributeSet());
        assertEquals(6, reader.parseBuffer.size());
        reader.handleText("1111".toCharArray(), 0);
        assertEquals(7, reader.parseBuffer.size());
        action.end(Tag.P);
        assertEquals(9, reader.parseBuffer.size());
    }
    
    public void testHTMLReaderIntIntIntTag() throws Exception {
        reader = (HTMLReader)doc.getReader(10, 10, 20, Tag.B);
        assertNotNull(reader.parseBuffer);
        assertEquals(0, reader.parseBuffer.size());
        
        assertNotNull(reader.charAttr);
        assertEquals(0, reader.charAttr.getAttributeCount());
    }
    
    public void testHTMLReaderIntIntIntTag_TagParameter() throws Exception {
        checkConstructorTagParameter(Tag.BR, "<a>link</a><b>asdasd</b>", 0);
        checkConstructorTagParameter(Tag.BR, "<a>link</a><b>asdasd</b><br>", 1);
        checkConstructorTagParameter(Tag.BR, "<a>link</a><b><br>asdasd</b>", 2);
        checkConstructorTagParameter(Tag.BR, "<a>link</a><br><b>asdasd</b>", 2);
        checkConstructorTagParameter(Tag.BR, "<a>link<br></a><b>asdasd</b>", 2);
        checkConstructorTagParameter(Tag.BR, "<a><br>link</a><b>asdasd</b>", 3);
        checkConstructorTagParameter(Tag.BR, "<br><a>link</a><b>asdasd</b>", 3);
        checkConstructorTagParameter(null, "<br><a>link</a><b>asdasd</b>", 15);
    }
    
    public void testHTMLReaderIntIntIntTag_TagParameter_Closed() throws Exception {
        final String text = "tag";
        editable = false;
        reader = (HTMLReader)doc.getReader(0, 0, 0, Tag.FORM);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.FORM.toString());
        reader.handleStartTag(Tag.HTML, attr, 0);
        reader.handleStartTag(Tag.HEAD, attr, 0);
        reader.handleEndTag(Tag.HEAD, 0);
        reader.handleStartTag(Tag.BODY, attr, 0);
        reader.handleEndTag(Tag.FORM, 0);
        reader.handleText(text.toCharArray(), 0);
        reader.handleEndTag(Tag.BODY, 0);
        reader.handleEndTag(Tag.HTML, 0);
        reader.flush();
        assertEquals(0, reader.parseBuffer.size());
        assertFalse(insertMarker.isOccurred());
        assertFalse(createMarker.isOccurred());
    }
    
    
    public void testHTMLReaderIntIntIntTag_OffsetParameter() throws Exception {
        final String str = "<a>0000</a><b>1111</b>";
        checkConstructorOffsetParameter(Tag.A, str, 0);
        checkConstructorOffsetParameter(Tag.A, str, 1);
        checkConstructorOffsetParameter(Tag.A, str, 2);
        checkConstructorOffsetParameter(Tag.A, str, 3);
        checkConstructorOffsetParameter(Tag.A, str, 4);
        checkConstructorOffsetParameter(Tag.A, str, 5);
    }
    
    public void testHTMLReaderIntIntIntTag_PopParameter() throws Exception {
        final String str = "<a>link</a><b>asdasd</b>";
        checkConstructorPopParameter(Tag.A, str, 0, "coco");
        if (isHarmony()) {
            checkConstructorPopParameter(Tag.A, str, 1, "cpeocococo");
        }
        checkConstructorPopParameter(Tag.A, str, 2, "cpeoeocococo");
        checkConstructorPopParameter(Tag.A, str, 3, "cpeoeoeocococo");
        checkConstructorPopParameter(Tag.A, str, 4, "cpeoeoeoeocococo");
        checkConstructorPopParameter(Tag.A, str, 5, "cpeoeoeoeoeocococo");
        checkConstructorPopParameter(Tag.A, str, 6, "cpeoeoeoeoeoeocococo");
    }
    
    public void testHTMLReaderIntIntIntTag_PushParameter() throws Exception {
        final String str = "<a>link</a><b>asdasd</b>";
        checkConstructorPushParameter(Tag.A, str, 0, "coco");
        checkConstructorPushParameter(Tag.A, str, 1, "cpsncoco");
        checkConstructorPushParameter(Tag.A, str, 2, "cpsnsncoco");
        checkConstructorPushParameter(Tag.A, str, 3, "cpsnsnsncoco");
        checkConstructorPushParameter(Tag.A, str, 4, "cpsnsnsnsncoco");
        checkConstructorPushParameter(Tag.A, str, 5, "cpsnsnsnsnsncoco");
        checkConstructorPushParameter(Tag.A, str, 6, "cpsnsnsnsnsnsncoco");
    }
    
    public void testHTMLReaderIntIntIntTag_PushPopParameter() throws Exception {
        final String str = "<a>link</a><b>asdasd</b>";
        checkConstructorPopPushParameter(Tag.A, str, 0, 0, "coco");
        checkConstructorPopPushParameter(Tag.A, str, 1, 1, "cpeosncoco");
        checkConstructorPopPushParameter(Tag.A, str, 2, 1, "cpeoeosncoco");
        checkConstructorPopPushParameter(Tag.A, str, 1, 2, "cpeosnsncoco");
        checkConstructorPopPushParameter(Tag.A, str, 2, 2, "cpeoeosnsncoco");
        checkConstructorPopPushParameter(Tag.A, str, 3, 1, "cpeoeoeosncoco");
        checkConstructorPopPushParameter(Tag.A, str, 3, 2, "cpeoeoeosnsncoco");
        checkConstructorPopPushParameter(Tag.A, str, 1, 3, "cpeosnsnsncoco");
        checkConstructorPopPushParameter(Tag.A, str, 2, 3, "cpeoeosnsnsncoco");
        checkConstructorPopPushParameter(Tag.A, str, 3, 3, "cpeoeoeosnsnsncoco");
    }
    
    public void testHTMLReaderIntIntIntTag_PushPopParameter_JoinPrevSpec1() throws Exception {
        editable = false;
        HTMLReader reader = (HTMLReader)doc.getReader(0, 0, 1, Tag.A);
        reader.new CharacterAction().start(Tag.A, new SimpleAttributeSet());
        assertEquals(2, reader.parseBuffer.size());
        checkJoinPrevSpec((ElementSpec)reader.parseBuffer.get(0));
    }
    
    public void testHTMLReaderIntIntIntTag_PushPopParameter_JoinPrevSpec2() throws Exception {
        editable = false;
        HTMLReader reader = (HTMLReader)doc.getReader(1, 1, 0, Tag.A);
        reader.new CharacterAction().start(Tag.A, new SimpleAttributeSet());
        assertEquals(1, reader.parseBuffer.size());
        assertTrue(ElementSpec.JoinPreviousDirection != ((ElementSpec)reader.parseBuffer.get(0)).getDirection());
    }
    
    public void testHTMLReaderIntIntIntTag_PushPopParameter_JoinPrevSpec3() throws Exception {
        editable = false;
        HTMLReader reader = (HTMLReader)doc.getReader(0, 1, 0, Tag.A);
        reader.new CharacterAction().start(Tag.A, new SimpleAttributeSet());
        assertEquals(2, reader.parseBuffer.size());
        checkJoinPrevSpec((ElementSpec)reader.parseBuffer.get(0));
    }
    
    public void testHTMLReaderIntIntIntTag_PushPopParameter_JoinPrevSpec4() throws Exception {
        editable = false;
        HTMLReader reader = (HTMLReader)doc.getReader(0, 1, 0, Tag.P);
        reader.new ParagraphAction().start(Tag.P, new SimpleAttributeSet());
        assertEquals(3, reader.parseBuffer.size());
        checkJoinPrevSpec((ElementSpec)reader.parseBuffer.get(0));
    }

    public void testHTMLReaderIntIntIntTag_PushPopParameter_JoinPrevSpec5() throws Exception {
        final Tag[] tags = HTML.getAllTags();
        Tag[] oddTags;
        if (!isHarmony()) {
            oddTags = new Tag[] {Tag.AREA, Tag.BASE, Tag.MAP, Tag.OPTION, Tag.PARAM, Tag.STYLE};
        } else {
            oddTags = new Tag[] {Tag.AREA, Tag.APPLET, Tag.IFRAME, Tag.LEGEND, Tag.COL, Tag.COLGROUP, Tag.SCRIPT, Tag.BASE, Tag.MAP, Tag.OPTION, Tag.OPTGROUP, Tag.PARAM, Tag.STYLE};
        }
        
        for (int i = 0; i < tags.length; i++) {
            final Tag tag = tags[i];
            if (foundInArray(oddTags, tag)) {
                continue;
            }
            init();
            editable = false;
            HTMLReader reader = (HTMLReader)doc.getReader(0, 1, 0, tag);
            reader.handleStartTag(tag, new SimpleAttributeSet(), 0);
            assertTrue(tag.toString(), reader.parseBuffer.size() > 0);
            ElementSpec firstSpec = (ElementSpec)reader.parseBuffer.get(0);
            assertEquals(tag.toString(), ElementSpec.ContentType, firstSpec.getType());
            assertEquals(tag.toString(), ElementSpec.JoinPreviousDirection, firstSpec.getDirection());
            assertEquals(tag.toString(), '\n', firstSpec.getArray()[0]);
        }
    }

    public void testHTMLReaderIntIntIntTag_PushPopParameter_BlockParagraph() throws Exception {
        // trailing specs cutting for block and paragraph tags
        if (!isHarmony()) { 
            return;
        }
        final String str = "<P>link</P>";
        init();
        editable = false;
        ParserCallback reader = doc.getReader(0, 2, 3, Tag.P);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.S.toString());
        doc.insertString(0, "0000", attr);
        assertFalse(insertMarker.isOccurred());
        parse(str, reader);
        reader.flush();
        assertTrue(insertMarker.isOccurred());
        ElementSpec[] specs = (ElementSpec[])insertMarker.getAuxiliary();
        String specsDescr = "cpeoeosnsnsnsoco";
        assertEquals(specsDescr.length()/2, specs.length);
        insertMarker.reset();
        for (int i = 0; i < specs.length; i++) {
            checkSpecType(specsDescr, i, (ElementSpec)specs[i]);
        }
    }
    
    public void testHTMLReaderIntIntIntTag_OffsetPushPopParameter() throws Exception {
        final String str = "<a>link</a><b>asdasd</b>";
        checkConstructorOffsetPopPushParameter(Tag.A, str, 0, 0, 0, "coco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 20, 1, 1, "eosncoco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 0, 2, 1, "cpeoeosncoco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 0, 1, 2, "cpeosnsncoco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 200, 2, 2, "eoeosnsncoco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 0, 3, 1, "cpeoeoeosncoco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 20, 3, 2, "eoeoeosnsncoco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 10, 1, 3, "eosnsnsncoco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 0, 2, 3, "cpeoeosnsnsncoco");
        checkConstructorOffsetPopPushParameter(Tag.A, str, 20, 3, 3, "eoeoeosnsnsncoco");
    }
    
    public void testHTMLReaderInt() {
        assertNotNull(reader.parseBuffer);
        assertEquals(0, reader.parseBuffer.size());
        
        assertNotNull(reader.charAttr);
        assertEquals(0, reader.charAttr.getAttributeCount());
    }
    
    public void testPushPopCharacterStyle() {
        reader.charAttr.addAttribute("initial", "true");
        final MutableAttributeSet attr1 = reader.charAttr; 
        final SimpleAttributeSet attr2 = new SimpleAttributeSet();
        attr2.addAttribute("bbbb", "aaaa");
        final SimpleAttributeSet attr3 = new SimpleAttributeSet();
        attr3.addAttribute("aaaa", "bbbb");
        
        reader.pushCharacterStyle();
        reader.charAttr = attr2;
        reader.pushCharacterStyle();
        reader.charAttr = attr3;
        reader.pushCharacterStyle();
        reader.charAttr = null;
        reader.popCharacterStyle();
        assertEquals(attr3, reader.charAttr);
        reader.popCharacterStyle();
        assertEquals(attr2, reader.charAttr);
        reader.popCharacterStyle();
        assertEquals(attr1, reader.charAttr);
        reader.popCharacterStyle();
        assertEquals(attr1, reader.charAttr);
        reader.charAttr = null;
        reader.popCharacterStyle();
        assertNull(reader.charAttr);
        
        reader.charAttr = null;
        try {
            reader.pushCharacterStyle();
            fail("no exception has been thrown");
        } catch (Exception e) {}
        
        reader.charAttr = attr2;
        reader.pushCharacterStyle();
        reader.charAttr = attr3;
        reader.pushCharacterStyle();
        reader.pushCharacterStyle();
        reader.charAttr = null;
        reader.popCharacterStyle();
        assertEquals(attr3, reader.charAttr);
        reader.popCharacterStyle();
        assertEquals(attr3, reader.charAttr);
        reader.popCharacterStyle();
        assertEquals(attr2, reader.charAttr);
    }
    
    public void testAddContentCharArrayIntIntBoolean_Implying_Necess() {
        String text = "data";
        assertEquals(0, reader.parseBuffer.size());
        reader.addContent(text.toCharArray(), 1, 3, true);
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkOpenImpliedSpec(spec);
        spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 1, 3, text.toCharArray());
        assertEquals(1, spec.getAttributes().getAttributeCount());
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.CONTENT);
        assertEquals(text, new String(spec.getArray()));
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testAddContentCharArrayIntIntBoolean_Implying_Unnecess() {
        String text = "data";
        reader.handleStartTag(Tag.H1, new SimpleAttributeSet(), 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        reader.addContent(text.toCharArray(), 1, 3, true);
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        assertEquals(1, spec.getAttributes().getAttributeCount());
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.H1);
        spec = (ElementSpec)reader.parseBuffer.get(1);
        assertEquals(1, spec.getAttributes().getAttributeCount());
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 1, 3, text.toCharArray());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testAddContentCharArrayIntIntBoolean_NotImplying() {
        String text = "data";
        assertEquals(0, reader.parseBuffer.size());
        reader.addContent(text.toCharArray(), 1, 3, false);
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertEquals(1, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 1, 3, text.toCharArray());
        assertEquals(1, spec.getAttributes().getAttributeCount());
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.CONTENT);
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testAddContentCharArrayIntInt_FullLength() {
        assertEquals(0, reader.parseBuffer.size());
        String text = "da\na";
        reader.charAttr.addAttribute("aaaa", "bbbb");
        reader.addContent(text.toCharArray(), 0, 4);
        assertEquals(2, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(reader.charAttr, "aaaa", "bbbb");
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, text.toCharArray());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testAddContentCharArrayIntInt_PartLength() {
        assertEquals(0, reader.parseBuffer.size());
        final SimpleAttributeSet initialSet = new SimpleAttributeSet();
        reader.charAttr = initialSet; 
        String text = "data";
        reader.addContent(text.toCharArray(), 1, 3);
        assertEquals(1, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 1, 3, text.toCharArray());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testAddContent_CharAttr() {
        String text = "data";
        assertEquals(0, reader.parseBuffer.size());
        final SimpleAttributeSet initialSet = new SimpleAttributeSet();
        initialSet.addAttribute("aaaa", "bbbb");
        reader.charAttr = initialSet; 
        reader.addContent(text.toCharArray(), 1, 3);
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        assertEquals(2, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(reader.charAttr, "aaaa", "bbbb");
        reader.charAttr = null;
        reader.popCharacterStyle();
        assertNull(reader.charAttr);
    }
    
    public void testAddSpecialElement() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.handleStartTag(Tag.P, attr, 0);
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        
        reader.addSpecialElement(Tag.HTML, attr);
        assertEquals(2, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.HTML);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        assertNotSame(specAttr, attr);
        assertEquals(2, attr.getAttributeCount());
        checkAttributes(attr, StyleConstants.NameAttribute, Tag.HTML);
        checkAttributes(attr, "aaaa", "bbbb");
    }
    
    public void testAddSpecialElement_AfterBlockOpen1() {
        checkAddSpecialAfterBlockOpen(Tag.HTML, Tag.IMPLIED);
    }

    public void testAddSpecialElement_AfterBlockOpen2() {
        checkAddSpecialAfterBlockOpen(Tag.HTML, Tag.I);
    }

    public void testAddSpecialElement_AfterBlockOpen3() {
        checkAddSpecialAfterBlockOpen(Tag.IMG, Tag.P);
    }

    public void testAddSpecialElement_AllTagsImpliedBlockOpenCheck() {
        final Tag[] allTags = HTML.getAllTags();
        for (int i = 0; i < allTags.length; i++) {
            final Tag tag = allTags[i];
            if (Tag.FRAME.equals(tag)) {
                continue;
            }
            init();
            SimpleAttributeSet attr = new SimpleAttributeSet();
            attr.addAttribute("aaaa", "bbbb");
            reader.addSpecialElement(tag , attr);
            assertEquals(2, reader.parseBuffer.size());
            reader.blockClose(Tag.TABLE);
            assertEquals(5, reader.parseBuffer.size());
        }
    }

    public void testAddSpecialElement_FrameImpliedBlockOpenCheck() {
        Tag specialTag = Tag.FRAME;
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.addSpecialElement(specialTag , attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, specialTag);
        checkAttributes(specAttr, "aaaa", "bbbb");
    }
    
    public void testAddSpecialElement_FrameImpliedBlockCloseCheck() {
        Tag specialTag = Tag.FRAME;
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.blockOpen(Tag.FRAMESET , attr);
        reader.addSpecialElement(specialTag , attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, specialTag);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        reader.blockClose(Tag.FRAMESET);
        assertEquals(3, reader.parseBuffer.size());
        spec = (ElementSpec)reader.parseBuffer.get(2);
        checkEndTagSpec(spec);
        specAttr = spec.getAttributes();
    }
    
    private void checkAddSpecialAfterBlockOpen(final Tag specialTag, final Tag blockTag) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.blockOpen(blockTag, attr);
        assertEquals(1, reader.parseBuffer.size());
        
        reader.addSpecialElement(specialTag, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(2);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, specialTag);
        checkAttributes(specAttr, "aaaa", "bbbb");
    }
    
    public void testBlockOpen() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        reader.blockOpen(Tag.B, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.B);
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);

        assertNotSame(attr, specAttr);
        assertEquals(2, attr.getAttributeCount());
        checkAttributes(attr, StyleConstants.NameAttribute, Tag.B);
    }
    
    public void testBlockOpen_ImpliedAttribute() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        attr.addAttribute(HTMLEditorKit.ParserCallback.IMPLIED, Boolean.TRUE);
        reader.blockOpen(Tag.B, attr);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        final AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.B);
        checkAttributes(specAttr, "aaaa", "bbbb");
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);

        assertNotSame(attr, specAttr);
        assertEquals(2, attr.getAttributeCount());
        checkAttributes(attr, StyleConstants.NameAttribute, Tag.B);
        checkAttributes(attr, "aaaa", "bbbb");
    }
    
    public void testBlockClose() {
        reader.blockClose(Tag.B);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(1, reader.parseBuffer.size());
        final ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkEndTagSpec(spec);
    }

    public void testBlockOpenClose() {
       SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.blockOpen(Tag.B, attr);
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.blockClose(Tag.B);
        assertEquals(5, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        assertEquals(2, spec.getAttributes().getAttributeCount());
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.B);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(2);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {'\n'});
        assertEquals(2, spec.getAttributes().getAttributeCount());
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(spec.getAttributes(), "CR", Boolean.TRUE);
        
        spec = (ElementSpec)reader.parseBuffer.get(3);
        checkEndTagSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);
    }
    
    public void testBlockOpenOpenCloseClose() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.blockOpen(Tag.B, attr);
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.blockOpen(Tag.I, attr);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.blockClose(Tag.I);
        assertEquals(6, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());

        reader.blockClose(Tag.B);
        assertEquals(7, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.B);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.I);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        spec = (ElementSpec)reader.parseBuffer.get(2);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(3);
        specAttr = spec.getAttributes();
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {'\n'});
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(specAttr, "CR", Boolean.TRUE);
        
        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(5);
        checkEndTagSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(6);
        checkEndTagSpec(spec);
    }
    
    public void testBlockOpenOpenCloseClose_Implied() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.blockOpen(Tag.B, attr);
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.blockOpen(Tag.IMPLIED, attr);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.blockClose(Tag.IMPLIED);
        assertEquals(5, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());

        reader.blockClose(Tag.B);
        assertEquals(6, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.B);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.IMPLIED);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        spec = (ElementSpec)reader.parseBuffer.get(2);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(3);
        specAttr = spec.getAttributes();
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {'\n'});
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(specAttr, "CR", Boolean.TRUE);
        
        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(5);
        checkEndTagSpec(spec);
    }
    
    public void testBlockOpenOpenCloseClose_ImpliedImplied() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.blockOpen(Tag.TITLE, attr);
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.blockOpen(Tag.IMPLIED, attr);
        assertEquals(2, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        reader.blockClose(Tag.IMPLIED);
        assertEquals(5, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());

        reader.blockClose(Tag.IMPLIED);
        assertEquals(6, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.TITLE);
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        specAttr = spec.getAttributes();
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.IMPLIED);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        spec = (ElementSpec)reader.parseBuffer.get(2);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(3);
        specAttr = spec.getAttributes();
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {'\n'});
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(specAttr, "CR", Boolean.TRUE);
        
        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(5);
        checkEndTagSpec(spec);
    }
    
    public void testBlockOpenContentClose_B() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.blockOpen(Tag.B, attr);
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        final String text = "text";
        reader.addContent(text.toCharArray(), 0, 4);
        assertEquals(3, reader.parseBuffer.size());
        assertEquals(1, reader.charAttr.getAttributeCount());
        
        reader.blockClose(Tag.B);
        assertEquals(6, reader.parseBuffer.size());
        assertEquals(1, reader.charAttr.getAttributeCount());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.B);
        checkAttributes(specAttr, "aaaa", "bbbb");

        spec = (ElementSpec)reader.parseBuffer.get(1);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(2);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, text.toCharArray());
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.B);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        spec = (ElementSpec)reader.parseBuffer.get(3);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {'\n'});
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.B);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(5);
        checkEndTagSpec(spec);
    }
    
    public void testBlockOpenContentClose_Implied() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        reader.blockOpen(Tag.IMPLIED, attr);
        assertEquals(1, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        
        final String text = "text";
        reader.addContent(text.toCharArray(), 0, 4);
        assertEquals(3, reader.parseBuffer.size());
        assertEquals(1, reader.charAttr.getAttributeCount());
        
        reader.blockClose(Tag.IMPLIED);
        assertEquals(5, reader.parseBuffer.size());
        assertEquals(1, reader.charAttr.getAttributeCount());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
        AttributeSet specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.IMPLIED);
        checkAttributes(specAttr, "aaaa", "bbbb");
        
        spec = (ElementSpec)reader.parseBuffer.get(1);
        checkOpenImpliedSpec(spec);
        
        spec = (ElementSpec)reader.parseBuffer.get(2);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, text.toCharArray());
        specAttr = spec.getAttributes();
        assertEquals(1, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        
        spec = (ElementSpec)reader.parseBuffer.get(3);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {'\n'});
        specAttr = spec.getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        checkAttributes(specAttr, "CR", Boolean.TRUE);
        
        spec = (ElementSpec)reader.parseBuffer.get(4);
        checkEndTagSpec(spec);
    }
    
    public void testRegisterTag() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        final Marker endMarker = new Marker();
        final Marker startMarker = new Marker();
        reader.registerTag(Tag.B, reader.new TagAction() {
            public void end(final Tag tag) {
                endMarker.setAuxiliary(tag);
                endMarker.setOccurred();
            }
            
            public void start(final Tag tag, final MutableAttributeSet attr) {
                startMarker.setOccurred();
                startMarker.setAuxiliary(attr);
            }
        });
        reader.handleSimpleTag(Tag.B, attr, 0);
        assertEquals(0, reader.parseBuffer.size());
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
        assertTrue(startMarker.isOccurred());
        assertTrue(endMarker.isOccurred());
        assertSame(attr, startMarker.getAuxiliary());
        assertEquals(Tag.B, endMarker.getAuxiliary());
    }
    
    public void testPreContent() {
        String text = "data";
        assertEquals(0, reader.parseBuffer.size());
        reader.charAttr.addAttribute("aaaa", "bbbb");
        reader.preContent(text.toCharArray());
        assertEquals(2, reader.charAttr.getAttributeCount());
        checkAttributes(reader.charAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertEquals(2, reader.parseBuffer.size());
        
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(0);
        checkOpenImpliedSpec(spec);

        spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, text.toCharArray());
        assertEquals(2, spec.getAttributes().getAttributeCount());
        checkAttributes(spec.getAttributes(), "aaaa", "bbbb");
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.CONTENT);
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
    }
    
    public void testPreContent_Calls() {
        final Marker contentMarker = new Marker();
        doc = new HTMLDocument() {
            public ParserCallback getReader(int pos) {
                return new HTMLReader(0) {
                    protected void addContent(char[] data, int offs, int length) {
                        contentMarker.setOccurred();
                        ArrayList callInfo = new ArrayList();
                        callInfo.add(data);
                        callInfo.add(new Integer(offs));
                        callInfo.add(new Integer(length));
                        contentMarker.setAuxiliary(callInfo);
                    };
                };
            }
        };
        reader = (HTMLReader)doc.getReader(0);
        String text = "precontent";
        reader.preContent(text.toCharArray());
        assertTrue(contentMarker.isOccurred());
        ArrayList callInfo = (ArrayList)contentMarker.getAuxiliary();
        assertEquals(text, new String((char[])callInfo.get(0)));
        assertEquals(new Integer(0), callInfo.get(1));
        assertEquals(text.length(), ((Integer)callInfo.get(2)).intValue());
    }
    
    public void testTextAreaContent() throws Exception {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        String text1 = "data";
        String text2 = "atada";
        reader.handleStartTag(Tag.TEXTAREA, attr, 0);
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(2, reader.parseBuffer.size());
        ElementSpec spec = (ElementSpec)reader.parseBuffer.get(1);
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[] {' '});
        assertEquals(2, spec.getAttributes().getAttributeCount());
        checkAttributes(spec.getAttributes(), StyleConstants.NameAttribute, Tag.TEXTAREA);
        Object contentModel = spec.getAttributes().getAttribute(StyleConstants.ModelAttribute);
        assertNotNull(contentModel);
        assertTrue(contentModel instanceof PlainDocument);
        final PlainDocument plainDocument = (PlainDocument)contentModel;
        assertEquals("", plainDocument.getText(0, plainDocument.getLength()));
        reader.parseBuffer.add(new ElementSpec(null, ElementSpec.EndTagType));
        assertEquals(3, reader.parseBuffer.size());
        
        reader.textAreaContent(text1.toCharArray());
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());
        assertEquals(4, plainDocument.getLength());
        assertEquals(text1, plainDocument.getText(0, plainDocument.getLength()));
        
        reader.textAreaContent(text2.toCharArray());
        assertEquals(0, reader.charAttr.getAttributeCount());
        assertEquals(3, reader.parseBuffer.size());
        assertEquals(9, plainDocument.getLength());
        assertEquals(text1 + text2, plainDocument.getText(0, plainDocument.getLength()));
        
        assertFalse(createMarker.isOccurred());
        assertFalse(insertMarker.isOccurred());
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
        
        init();
        doc.setPreservesUnknownTags(false);
        reader.handleStartTag(Tag.HTML, attr, 0);
        reader.handleStartTag(Tag.BODY, attr, 0);
        reader.handleStartTag(Tag.P, attr, 0);
        assertEquals(3, reader.parseBuffer.size());
        reader.handleSimpleTag(new Tag("fake"), attr, 0);
        assertEquals(3, reader.parseBuffer.size());
    }
    
    public void testHandleStartEndTag_Unknown() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute("aaaa", "bbbb");
        
        doc.setPreservesUnknownTags(true);
        reader.handleStartTag(Tag.HTML, attr, 0);
        reader.handleStartTag(Tag.BODY, attr, 0);
        reader.handleStartTag(Tag.P, attr, 0);
        assertEquals(3, reader.parseBuffer.size());
        reader.handleStartTag(new Tag("fake"), attr, 0);
        reader.handleEndTag(new Tag("fake"), 0);
        assertEquals(3, reader.parseBuffer.size());
    }
    
    private void init() {
        createMarker = new Marker();
        insertMarker = new Marker();
        doc = new HTMLDocument() {
            protected void create(ElementSpec[] data) {
                createMarker.setOccurred();
                createMarker.setAuxiliary(data);
                if (editable) {
                    super.create(data);
                }
            }
            
            protected void insert(int offset, ElementSpec[] data) throws BadLocationException {
                insertMarker.setOccurred();
                insertMarker.setAuxiliary(data);
                if (editable) {
                    super.insert(offset, data);
                }
            }
        };
        reader = (HTMLReader)doc.getReader(0);
        editable = true;
    }
    
    private void checkConstructorTagParameter(final Tag tag, final String str, final int numSpecs) throws Exception {
        init();
        editable = false;
        ParserCallback reader = doc.getReader(0, 0, 0, tag);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        doc.insertString(0, "0000", attr);
        assertFalse("no inserts", insertMarker.isOccurred());
        parse(str, reader);
        reader.flush();
        if (numSpecs == 0 && isHarmony()) {
            assertFalse("inserted", insertMarker.isOccurred());
        } else {
            assertTrue("inserted", insertMarker.isOccurred());
            ElementSpec[] specs = (ElementSpec[])insertMarker.getAuxiliary();
            assertEquals("number of specs inserted", numSpecs, specs.length);
            insertMarker.reset();
        }
    }
    
    private void checkConstructorOffsetParameter(final Tag tag, final String str, final int offset) throws Exception {
        insertMarker.reset();
        doc = new HTMLDocument() {
            protected void insert(int offset, ElementSpec[] data) throws BadLocationException {
                insertMarker.setOccurred();
                insertMarker.setAuxiliary(new Integer(offset));
            }
        };
        ParserCallback reader = doc.getReader(offset, 0, 0, tag);
        parse(str, reader);
        reader.flush();
        assertTrue(insertMarker.isOccurred());
        assertEquals(new Integer(offset), insertMarker.getAuxiliary());
    }
    
    private void checkConstructorPopPushParameter(final Tag tag, final String str, final int pop, final int push, final String specsDescr) throws Exception {
        init();
        editable = false;
        ParserCallback reader = doc.getReader(0, pop, push, tag);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.S.toString());
        doc.insertString(0, "0000", attr);
        assertFalse(insertMarker.isOccurred());
        parse(str, reader);
        reader.flush();
        assertTrue(insertMarker.isOccurred());
        ElementSpec[] specs = (ElementSpec[])insertMarker.getAuxiliary();
        assertEquals(specsDescr.length()/2, specs.length);
        insertMarker.reset();
        for (int i = 0; i < specs.length; i++) {
            checkSpecType(specsDescr, i, (ElementSpec)specs[i]);
        }
    }

    private void checkConstructorOffsetPopPushParameter(final Tag tag, final String str, final int offset, final int pop, final int push, final String specsDescr) throws Exception {
        init();
        ParserCallback reader = doc.getReader(offset, pop, push, tag);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.S.toString());
        editable = false;
        insertMarker.reset();
        parse(str, reader);
        reader.flush();
        assertTrue(insertMarker.isOccurred());
        ElementSpec[] specs = (ElementSpec[])insertMarker.getAuxiliary();
        assertEquals(specsDescr.length()/2, specs.length);
        insertMarker.reset();
        for (int i = 0; i < specs.length; i++) {
            checkSpecType(specsDescr, i, (ElementSpec)specs[i]);
        }
    }
    
    private void checkTextIsInserted() {
        int numSpecs = reader.parseBuffer.size();
        reader.handleText("text".toCharArray(), 0);
        assertEquals(numSpecs + 2, reader.parseBuffer.size());
    }

    private void checkTextInNotInserted() {
        int numSpecs = reader.parseBuffer.size();
        reader.handleText("text".toCharArray(), 0);
        assertEquals(numSpecs, reader.parseBuffer.size());
    }

    private void checkConstructorPopParameter(final Tag tag, final String str, final int pop, final String specsDescr) throws Exception {
        checkConstructorPopPushParameter(tag, str, pop, 0, specsDescr);
    }
    
    private void checkConstructorPushParameter(final Tag tag, final String str, final int push, final String specsDescr) throws Exception {
        checkConstructorPopPushParameter(tag, str, 0, push, specsDescr);
    }
    
    private void parse(final String str, ParserCallback reader) throws IOException {
        new ParserDelegator().parse(new StringReader(str), reader, true);
    }
    
    private void checkSpecType(final String specsDescr, int i, final DefaultStyledDocument.ElementSpec spec) {
        final char typeChar = specsDescr.charAt(i*2);
        short specType = 0;
        if (typeChar == 's') {
            specType = ElementSpec.StartTagType;
        } else if (typeChar == 'c') {
            specType = ElementSpec.ContentType;
        } else if (typeChar == 'e') {
            specType = ElementSpec.EndTagType;
        }
        final char dirChar = specsDescr.charAt(i*2 + 1);
        short specDir = 0;
        if (dirChar == 'n') {
            specDir = ElementSpec.JoinNextDirection;
        } else if (dirChar == 'p') {
            specDir = ElementSpec.JoinPreviousDirection;
        } else if (dirChar == 'o') {
            specDir = ElementSpec.OriginateDirection;
        }
        assertEquals("spec direction", specDir, spec.getDirection());
        assertEquals("spec type", specType, spec.getType());
    }

    private void createContentMarkersInstrumentedReader(final Marker addContentMarker, final Marker preContentMarker, final Marker textAreaMarker) {
        doc = new HTMLDocument() {
            public ParserCallback getReader(int pos) {
                return new HTMLReader(pos) {
                    protected void addContent(char[] data, int offset, int length, boolean createImpliedPIfNecessary) {
                        addContentMarker.setOccurred();
                        super.addContent(data, offset, length, createImpliedPIfNecessary);
                    };
                    
                    protected void preContent(char[] data) {
                        preContentMarker.setOccurred();
                        super.preContent(data);
                    }
                    
                    protected void textAreaContent(char[] data) {
                        textAreaMarker.setOccurred();
                        super.textAreaContent(data);
                    }
                };
            }
        };
        reader = (HTMLReader)doc.getReader(0);
    }

    private void checkJoinPrevSpec(final ElementSpec firstSpec) {
        assertEquals(ElementSpec.ContentType, firstSpec.getType());
        assertEquals(ElementSpec.JoinPreviousDirection, firstSpec.getDirection());
        assertEquals('\n', firstSpec.getArray()[0]);
    }

    private boolean foundInArray(final Tag[] array, final Tag tag) {
        for (int i = 0; i < array.length; i++) {
            if (tag.equals(array[i])) {
                return true;
            }
        }
        return false;
    }

}
