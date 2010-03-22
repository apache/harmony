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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocumentTest;
import javax.swing.text.Element;
import javax.swing.text.GapContent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.AbstractDocument.AttributeContext;
import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument.BlockElement;
import javax.swing.text.html.HTMLDocument.Iterator;
import javax.swing.text.html.HTMLDocument.RunElement;
import javax.swing.text.html.HTMLDocumentTestCase.DocumentController;
import javax.swing.text.html.HTMLDocumentTestCase.PublicHTMLDocument;
import javax.swing.text.html.parser.ParserDelegator;

public class HTMLDocumentTest extends DefaultStyledDocumentTest {

    protected PublicHTMLDocument htmlDoc;
    private Marker insertMarker;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        htmlDoc = new PublicHTMLDocument();
        timeoutDelay = Integer.MAX_VALUE;
        insertMarker = htmlDoc.getInsertMarker();
        doc = htmlDoc;
    }

    protected void tearDown() throws Exception {
        htmlDoc = null;
        super.tearDown();
    }

    public void testHTMLDocumentContentStyleSheet() throws MalformedURLException {
        StyleSheet styles = new StyleSheet();
        final GapContent gapContent = new GapContent(10);
        htmlDoc = new PublicHTMLDocument(gapContent, styles);
        assertSame(styles, htmlDoc.getAttributeContextPublicly());
        assertSame(gapContent, htmlDoc.getContentPublicly());

        URL u1 = new URL("http://www.apache.org");
        styles.setBase(u1);
        htmlDoc = new PublicHTMLDocument(gapContent, styles);
        assertNull(htmlDoc.getBase());
    }

    public void testHTMLDocumentStyleSheet() throws BadLocationException, MalformedURLException {
        StyleSheet styles = new StyleSheet();
        htmlDoc = new PublicHTMLDocument(styles);
        assertSame(styles, htmlDoc.getAttributeContextPublicly());
        final Content content = htmlDoc.getContentPublicly();
        assertTrue(content instanceof GapContent);
        
        URL u1 = new URL("http://www.apache.org");
        styles.setBase(u1);
        htmlDoc = new PublicHTMLDocument(styles);
        assertNull(htmlDoc.getBase());
    }

    public void testHTMLDocument() {
        htmlDoc = new PublicHTMLDocument();
        assertTrue(htmlDoc.getContentPublicly() instanceof GapContent);
        AttributeContext styleSheet = htmlDoc.getAttributeContextPublicly();
        assertTrue(styleSheet instanceof StyleSheet);
        final Enumeration styleNames = ((StyleSheet)styleSheet).getStyleNames();
        assertTrue(styleNames.hasMoreElements());
        assertEquals("default", styleNames.nextElement());
        assertFalse(styleNames.hasMoreElements());
        final Style style = ((StyleSheet)styleSheet).getStyle("default");
        assertEquals(1, style.getAttributeCount());
        assertEquals("default", style.getAttribute(StyleConstants.NameAttribute));
    }

    public void testCreateLeafElement() throws BadLocationException {
        Element leaf = htmlDoc.createLeafElement(null, null, 0, 1);
        assertTrue(leaf instanceof HTMLDocument.RunElement);
        assertNull(leaf.getParentElement());
        assertEquals(0, leaf.getStartOffset());
        assertEquals(1, leaf.getEndOffset());
        assertNotSame(htmlDoc.createLeafElement(null, null, 0, 1),
                      htmlDoc.createLeafElement(null, null, 0, 1));

        htmlDoc.insertString(0, "01234", null);

        Element leaf2 = htmlDoc.createLeafElement(leaf, null, 1, 3);
        assertTrue(leaf2 instanceof HTMLDocument.RunElement);
        assertSame(leaf, leaf2.getParentElement());
        assertEquals(1, leaf2.getStartOffset());
        assertEquals(3, leaf2.getEndOffset());

        htmlDoc.remove(0, 5);
        assertEquals(0, leaf2.getStartOffset());
        assertEquals(0, leaf2.getEndOffset());
    }

    public void testCreateBranchElement() {
        Element branch = htmlDoc.createBranchElement(null, null);
        assertTrue(branch instanceof HTMLDocument.BlockElement);
        assertNull(branch.getParentElement());

        assertNull(branch.getElement(0));
        assertNull(branch.getElement(1));

        assertEquals(0, branch.getElementCount());
        
        // Since this branch element has no children yet, it has no start and
        // end offsets. Thus calling get{Start,End}Offset on an empty branch
        // element causes the exception being thrown.
        if (isHarmony()) {
            try {
                assertEquals(0, branch.getStartOffset());

                fail("getStartOffset on an empty BranchElement "
                     + "causes exception");
            } catch (ArrayIndexOutOfBoundsException e) { }
            try {
                assertEquals(1, branch.getEndOffset());

                fail("getEndOffset on an empty BranchElement causes exception");
            } catch (ArrayIndexOutOfBoundsException e) { }
        } else {
            try {
                assertEquals(0, branch.getStartOffset());

                fail("getStartOffset on an empty BranchElement "
                     + "causes exception");
            } catch (NullPointerException e) { }
            try {
                assertEquals(1, branch.getEndOffset());

                fail("getEndOffset on an empty BranchElement causes exception");
            } catch (NullPointerException e) { }
        }
    }

    public void testCreateDefaultRoot() {
        try {
            htmlDoc.insertString(0, "123", null);
        } catch (BadLocationException e) {}

        final Element root = htmlDoc.createDefaultRoot();
        assertSame(htmlDoc, root.getDocument());
        assertTrue(root instanceof BlockElement);
        AttributeSet attributes = root.getAttributes();
        assertNotNull(attributes);
        assertEquals(1, attributes.getAttributeCount());
        assertEquals(StyleConstants.NameAttribute, attributes.getAttributeNames().nextElement());
        assertEquals(Tag.HTML, attributes.getAttribute(StyleConstants.NameAttribute));
        assertEquals("html", root.getName());
        assertNull(root.getParentElement());
        assertNull(((BlockElement)root).getResolveParent());

        assertEquals(1, root.getElementCount());
        assertTrue(root.getElement(0) instanceof BlockElement);
        assertSame(root, root.getElement(0).getParentElement());
        Element child = root.getElement(0);
        attributes = child.getAttributes();
        assertNotNull(attributes);
        assertEquals(1, attributes.getAttributeCount());
        assertEquals(StyleConstants.NameAttribute, child.getAttributes().getAttributeNames().nextElement());
        assertEquals(Tag.BODY, attributes.getAttribute(StyleConstants.NameAttribute));
        assertEquals("body", child.getName());
        assertNull(((BlockElement)child).getResolveParent());

        assertEquals(1, child.getElementCount());
        assertTrue(child.getElement(0) instanceof BlockElement);
        assertSame(child, child.getElement(0).getParentElement());
        child = child.getElement(0);
        attributes = child.getAttributes();
        assertNotNull(attributes);
        assertEquals(2, attributes.getAttributeCount());
        checkAttributes(attributes, StyleConstants.NameAttribute, Tag.P);
        checkAttributes(attributes, CSS.Attribute.MARGIN_TOP, "0");
        assertEquals("p", child.getName());
        assertNull(((BlockElement)child).getResolveParent());

        assertEquals(1, child.getElementCount());
        assertTrue(child.getElement(0) instanceof RunElement);
        assertSame(child, child.getElement(0).getParentElement());
        child = child.getElement(0);
        attributes = child.getAttributes();
        assertNotNull(attributes);
        assertEquals(1, attributes.getAttributeCount());
        assertEquals(StyleConstants.NameAttribute, child.getAttributes().getAttributeNames().nextElement());
        assertEquals(Tag.CONTENT, attributes.getAttribute(StyleConstants.NameAttribute));
        assertEquals("content", child.getName());
        assertNull(((RunElement)child).getResolveParent());
        if (!isHarmony()) {
            assertEquals(0, child.getStartOffset());
            assertEquals(1, child.getEndOffset());
        } else {
            assertEquals(0, child.getStartOffset());
            assertEquals(4, child.getEndOffset());
        }
    }

    public void testGetElementElementObjectObject() throws BadLocationException {
        final Element root = htmlDoc.getDefaultRootElement();
        final String value = "ASD";
        assertNull(htmlDoc.getElement(root, HTML.Attribute.ID, value));
        final SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(HTML.Attribute.NAME, Tag.B.toString());
        attr.addAttribute(HTML.Attribute.ID, value);

        htmlDoc.insertString(0, "0000", attr);
        Element child1 = root.getElement(0).getElement(0).getElement(0);
        assertSame(child1, htmlDoc.getElement(root, HTML.Attribute.ID, value));
        assertSame(child1, htmlDoc.getElement(root, HTML.Attribute.NAME, Tag.B.toString()));
        assertNull(htmlDoc.getElement(root, HTML.Attribute.ID, "AAA"));
        assertNull(htmlDoc.getElement(htmlDoc.getRootElements()[1], HTML.Attribute.ID, "AAA"));

        attr.addAttribute(HTML.Attribute.NAME, Tag.I.toString());
        htmlDoc.insertString(4, "0000", attr);
        child1 = root.getElement(0).getElement(0).getElement(0);
        Element child2 = root.getElement(0).getElement(0).getElement(1);
        assertSame(child1, htmlDoc.getElement(root, HTML.Attribute.ID, value));
        assertSame(child1, htmlDoc.getElement(root, HTML.Attribute.NAME, Tag.B.toString()));
        assertSame(child2, htmlDoc.getElement(root, HTML.Attribute.NAME, Tag.I.toString()));
        assertNull(htmlDoc.getElement(root, HTML.Attribute.ID, "AAA"));
        assertNull(htmlDoc.getElement(htmlDoc.getRootElements()[1], HTML.Attribute.ID, "AAA"));
    }

    public void testGetElementString() throws Exception {
        final Element root = htmlDoc.getDefaultRootElement();
        final String value = "B";
        assertNull(htmlDoc.getElement(value));
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, value);
        htmlDoc.insertString(0, "0000", attr);
        attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.P.toString());
        attr.addAttribute(HTML.Attribute.ID, value);
        htmlDoc.insertString(0, "0000", attr);
        assertSame(root.getElement(0).getElement(0).getElement(0), htmlDoc.getElement(value));
        assertNull(htmlDoc.getElement("AAA"));
    }

    public void testGetIterator() throws BadLocationException {
        final SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        htmlDoc.insertString(0, "0000", attr);
        StyleConstants.setItalic(attr, true);
        htmlDoc.insertString(4, "1111", attr);
        
        final Iterator iterator1 = htmlDoc.getIterator(Tag.HTML);
        final Iterator iterator2 = htmlDoc.getIterator(Tag.HTML);
        final Iterator iterator3 = htmlDoc.getIterator(Tag.A);
        final Iterator iterator4 = htmlDoc.getIterator(Tag.P);
        final Iterator iterator5 = htmlDoc.getIterator(Tag.B);
        assertNotNull(iterator1);
        assertNotNull(iterator2);
        assertNotNull(iterator3);
        if (isHarmony()) {
            assertNotNull(iterator4);
        }
        assertNotNull(iterator5);
        assertNotSame(iterator1, iterator2);
        assertNotSame(iterator2, iterator3);
        assertNotSame(iterator3, iterator5);

        assertEquals(Tag.HTML, iterator1.getTag());
        if (isHarmony()) {
            assertTrue(iterator1.isValid());
            assertEquals(0, iterator1.getStartOffset());
            assertEquals(9, iterator1.getEndOffset());
            assertNotNull(iterator1.getAttributes());
            iterator1.next();
            assertFalse(iterator1.isValid());
            assertEquals(-1, iterator1.getStartOffset());
            assertEquals(-1, iterator1.getEndOffset());
            assertNull(iterator1.getAttributes());
        }

        assertEquals(Tag.HTML, iterator2.getTag());
        if (isHarmony()) {
            assertTrue(iterator2.isValid());
            assertEquals(0, iterator2.getStartOffset());
            assertEquals(9, iterator2.getEndOffset());
            assertNotNull(iterator2.getAttributes());
            iterator2.next();
            assertFalse(iterator2.isValid());
            assertEquals(-1, iterator2.getStartOffset());
            assertEquals(-1, iterator2.getEndOffset());
            assertNull(iterator2.getAttributes());
        }

        assertEquals(Tag.A, iterator3.getTag());
        if (isHarmony()) {
            assertFalse(iterator3.isValid());
            assertNull(iterator3.getAttributes());
            iterator3.next();
            assertEquals(-1, iterator3.getStartOffset());
            assertEquals(-1, iterator3.getEndOffset());
        }

        if (isHarmony()) {
            assertEquals(Tag.P, iterator4.getTag());
            assertTrue(iterator4.isValid());
            assertEquals(0, iterator4.getStartOffset());
            assertEquals(9, iterator4.getEndOffset());
            iterator4.next();
            assertFalse(iterator4.isValid());
            assertEquals(-1, iterator4.getStartOffset());
            assertEquals(-1, iterator4.getEndOffset());
            assertNull(iterator4.getAttributes());
        }

        assertEquals(Tag.B, iterator5.getTag());
        if (isHarmony()) {
            assertTrue(iterator5.isValid());
            assertEquals(0, iterator5.getStartOffset());
            assertEquals(4, iterator5.getEndOffset());
            assertFalse(StyleConstants.isBold(iterator5.getAttributes()));
            assertFalse(StyleConstants.isItalic(iterator5.getAttributes()));
            iterator5.next();
            assertTrue(iterator5.isValid());
            assertEquals(4, iterator5.getStartOffset());
            assertEquals(8, iterator5.getEndOffset());
            assertFalse(StyleConstants.isBold(iterator5.getAttributes()));
            assertTrue(StyleConstants.isItalic(iterator5.getAttributes()));
            iterator5.next();
            assertFalse(iterator5.isValid());
            assertEquals(-1, iterator5.getStartOffset());
            assertEquals(-1, iterator5.getEndOffset());
            assertNull(iterator5.getAttributes());
        }
    }

    public void testGetReaderIntIntIntTag() {
        HTMLEditorKit.ParserCallback reader1 = htmlDoc.getReader(0, 10, 100, null);
        HTMLEditorKit.ParserCallback reader2 = htmlDoc.getReader(0, 10, 100, null);
        HTMLEditorKit.ParserCallback reader3 = htmlDoc.getReader(10, 100, 10, Tag.P);

        assertNotNull(reader1);
        assertNotNull(reader2);
        assertNotNull(reader3);
        assertTrue(reader1 instanceof HTMLDocument.HTMLReader);
        assertTrue(reader2 instanceof HTMLDocument.HTMLReader);
        assertTrue(reader3 instanceof HTMLDocument.HTMLReader);

        assertNotSame(reader1, reader2);
        assertNotSame(reader2, reader3);
    }

    public void testGetReaderInt() {
        HTMLEditorKit.ParserCallback reader1 = htmlDoc.getReader(0);
        HTMLEditorKit.ParserCallback reader2 = htmlDoc.getReader(0);
        HTMLEditorKit.ParserCallback reader3 = htmlDoc.getReader(1);

        assertNotNull(reader1);
        assertNotNull(reader2);
        assertNotNull(reader3);
        assertTrue(reader1 instanceof HTMLDocument.HTMLReader);
        assertTrue(reader2 instanceof HTMLDocument.HTMLReader);
        assertTrue(reader3 instanceof HTMLDocument.HTMLReader);

        assertNotSame(reader1, reader2);
        assertNotSame(reader2, reader3);
    }

    public void testGetStyleSheet() {
        AttributeContext styleSheet = htmlDoc.getAttributeContextPublicly();
        assertTrue(styleSheet instanceof StyleSheet);
        assertSame(styleSheet, htmlDoc.getStyleSheet());
    }

    public void testProcessHTMLFrameHyperlinkEvent() throws Exception {
        final String frameSetHTML = "<FRAMESET><FRAME name=\"1\" src=\"1.html\"><FRAME name=\"2\" src=\"2.html\"><img name=\"3\" src=\"3.jpg\"></FRAMESET>";
        HTMLDocumentTestCase.loadDocument(htmlDoc, frameSetHTML);
        final Element body = htmlDoc.getDefaultRootElement().getElement(1);
        final Element frameSet = body.getElement(0);
        final Element frame1 = frameSet.getElement(0);
        final Element frame2 = frameSet.getElement(1);

        final String urlStr1 = "file:/test1.html";
        final String urlStr2 = "file:/test2.html";
        HTMLFrameHyperlinkEvent event1 = new HTMLFrameHyperlinkEvent(htmlDoc, HyperlinkEvent.EventType.ACTIVATED, new URL(urlStr1), "3");        
        HTMLFrameHyperlinkEvent event2 = new HTMLFrameHyperlinkEvent(htmlDoc, HyperlinkEvent.EventType.ACTIVATED, new URL(urlStr1), frame2, "_self");        
        HTMLFrameHyperlinkEvent event3 = new HTMLFrameHyperlinkEvent(htmlDoc, HyperlinkEvent.EventType.ACTIVATED, new URL(urlStr2), frame2, "_top");        
        HTMLFrameHyperlinkEvent event4 = new HTMLFrameHyperlinkEvent(htmlDoc, HyperlinkEvent.EventType.ACTIVATED, new URL(urlStr2), frame2, "_parent");
        HTMLFrameHyperlinkEvent event5 = new HTMLFrameHyperlinkEvent(htmlDoc, HyperlinkEvent.EventType.ACTIVATED, new URL(urlStr1), "1");        

        final DocumentController controller = new DocumentController();
        htmlDoc.addDocumentListener(controller);

        htmlDoc.processHTMLFrameHyperlinkEvent(event1);
        assertFalse(controller.isChanged());
        controller.reset(); 

        assertSame(frame2, event2.getSourceElement());
        htmlDoc.processHTMLFrameHyperlinkEvent(event2);
        assertNull(htmlDoc.getParser());
        assertTrue(controller.isChanged());
        assertEquals(1, controller.getNumEvents());
        assertTrue(controller.getEvent(0) instanceof AbstractDocument.DefaultDocumentEvent);
        AbstractDocument.DefaultDocumentEvent event = (AbstractDocument.DefaultDocumentEvent)controller.getEvent(0);
        assertEquals(DocumentEvent.EventType.CHANGE, event.getType());
        assertEquals(frame2.getStartOffset(), event.getOffset());
        assertEquals(frame2.getEndOffset() - frame2.getStartOffset(), event.getLength());
        assertSame(htmlDoc, event.getDocument());
        assertNull(event.getChange(frame2));
        assertEquals(DocumentEvent.EventType.CHANGE, controller.getEvent(0).getType());
        assertEquals(urlStr1, frame2.getAttributes().getAttribute(HTML.Attribute.SRC));
        controller.reset(); 

        htmlDoc.setParser(null);
        htmlDoc.processHTMLFrameHyperlinkEvent(event4);
        assertNotNull(htmlDoc.getParser());
        assertTrue(controller.isChanged());
        assertEquals(2, controller.getNumEvents());
        assertEquals(DocumentEvent.EventType.INSERT, controller.getEvent(0).getType());
        assertEquals(DocumentEvent.EventType.REMOVE, controller.getEvent(1).getType());
        Element newFrame = body.getElement(0);
        AttributeSet frameAttr = newFrame.getAttributes();
        assertEquals(2, frameAttr.getAttributeCount());
        assertEquals(urlStr2, frameAttr.getAttribute(HTML.Attribute.SRC));
        assertEquals(Tag.FRAME, frameAttr.getAttribute(StyleConstants.NameAttribute));
        controller.reset(); 

        // tests improper use behaviour (for compatibility reasons)
        HTMLFrameHyperlinkEvent event6 = new HTMLFrameHyperlinkEvent(htmlDoc, HyperlinkEvent.EventType.ACTIVATED, new URL(urlStr1), newFrame, "_parent");
        ParserDelegator parser = new ParserDelegator();
        htmlDoc.setParser(parser);
        htmlDoc.processHTMLFrameHyperlinkEvent(event6);
        assertSame(parser, htmlDoc.getParser());
        assertTrue(controller.isChanged());
        assertEquals(2, controller.getNumEvents());
        assertEquals(DocumentEvent.EventType.INSERT, controller.getEvent(0).getType());
        assertEquals(DocumentEvent.EventType.REMOVE, controller.getEvent(1).getType());
        newFrame = htmlDoc.getDefaultRootElement().getElement(1);
        assertNotSame(frameSet, newFrame);
        frameAttr = newFrame.getAttributes();
        assertEquals(2, frameAttr.getAttributeCount());
        assertEquals(urlStr1, frameAttr.getAttribute(HTML.Attribute.SRC));
        assertEquals(Tag.FRAME, frameAttr.getAttribute(StyleConstants.NameAttribute));
        controller.reset(); 
    }
    
    public void testGetSetBase() throws Exception {
        URL u1 = new URL("http://www.apache.org");
        URL u2 = new URL("http://www.harmony.incubator.apache.org");
        String tail = "tail";
        htmlDoc.setBase(u1);
        assertSame(u1, htmlDoc.getBase());
        assertSame(u1, htmlDoc.getStyleSheet().getBase());
        htmlDoc.getStyleSheet().setBase(u2);
        assertSame(u2, htmlDoc.getStyleSheet().getBase());
        assertSame(u1, htmlDoc.getBase());
    }

    public void testGetSetParser() {
        assertNull(htmlDoc.getParser());
        ParserDelegator parser = new ParserDelegator();
        htmlDoc.setParser(parser);
        assertSame(parser, htmlDoc.getParser());
    }

    public void testGetSetPreservesUnknownTags() throws Exception {
        assertTrue(htmlDoc.getPreservesUnknownTags());

        htmlDoc.setPreservesUnknownTags(false);
        assertFalse(htmlDoc.getPreservesUnknownTags());

        Marker createMarker = htmlDoc.getCreateMarker(); 
        final String htmlStr = "<html><body><badtag>0</badtag></body></html>";
        HTMLDocumentTestCase.loadDocument(htmlDoc, htmlStr);
        Element parent = htmlDoc.getDefaultRootElement().getElement(1).getElement(0);
        ArrayList array = (ArrayList)createMarker.getAuxiliary();
        assertEquals(1, array.size());
        assertEquals(13, ((ElementSpec[])(array.get(0))).length);
        assertEquals(2, parent.getElementCount());
        createMarker.reset();

        htmlDoc = new PublicHTMLDocument();
        htmlDoc.setPreservesUnknownTags(true);
        createMarker = htmlDoc.getCreateMarker();
        HTMLDocumentTestCase.loadDocument(htmlDoc, htmlStr);
        parent = htmlDoc.getDefaultRootElement().getElement(1).getElement(0);
        assertTrue(createMarker.isOccurred());
        array = (ArrayList)createMarker.getAuxiliary();
        assertEquals(1, array.size());
        assertEquals(15, ((ElementSpec[])(array.get(0))).length);
        assertEquals(4, parent.getElementCount());
        assertEquals("badtag", parent.getElement(0).getName());
        assertEquals(Tag.CONTENT.toString(), parent.getElement(1).getName());
        assertEquals("badtag", parent.getElement(2).getName());
        checkAttributes(parent.getElement(2).getAttributes(), HTML.Attribute.ENDTAG, Boolean.TRUE);
        createMarker.reset();
    }

    public void testGetSetTokenThreshold() throws Exception {
        assertEquals(Integer.MAX_VALUE, htmlDoc.getTokenThreshold());

        htmlDoc.setTokenThreshold(100);
        assertEquals(100, htmlDoc.getTokenThreshold());

        final String longString = "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a" +
                             "<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a<br>a";

        htmlDoc = new PublicHTMLDocument();
        insertMarker = htmlDoc.getInsertMarker();
        Marker createMarker = htmlDoc.getCreateMarker();
        final ParserDelegator parser = new ParserDelegator();
        htmlDoc.setParser(parser);
        htmlDoc.setEditable(false);
        htmlDoc.setTokenThreshold(1);
        Element root = htmlDoc.getDefaultRootElement();
        Element branch = root.getElement(0).getElement(0);
        htmlDoc.insertAfterStart(branch, longString);
        assertFalse(createMarker.isOccurred());
        assertTrue(insertMarker.isOccurred());
        ArrayList info = (ArrayList)insertMarker.getAuxiliary();
        assertNotNull(info);
        assertEquals(5, info.size()/2);
        assertEquals(2, ((ElementSpec[])(info.get(0))).length);
        assertEquals(6, ((ElementSpec[])info.get(2)).length);
        assertEquals(26, ((ElementSpec[])info.get(4)).length);
        assertEquals(126, ((ElementSpec[])info.get(6)).length);
        assertEquals(305, ((ElementSpec[])info.get(8)).length);
        assertEquals(1, htmlDoc.getTokenThreshold());
        insertMarker.reset();
        
        htmlDoc = new PublicHTMLDocument();
        insertMarker = htmlDoc.getInsertMarker();
        htmlDoc.setEditable(false);
        htmlDoc.setParser(parser);
        htmlDoc.setTokenThreshold(2);
        root = htmlDoc.getDefaultRootElement();
        branch = root.getElement(0).getElement(0);
        htmlDoc.insertAfterStart(branch, longString);
        assertFalse(createMarker.isOccurred());
        assertTrue(insertMarker.isOccurred());
        info = (ArrayList)insertMarker.getAuxiliary();
        assertNotNull(info);
        assertEquals(5, info.size()/2);
        assertEquals(4, ((ElementSpec[])info.get(0)).length);
        assertEquals(12, ((ElementSpec[])info.get(2)).length);
        assertEquals(52, ((ElementSpec[])info.get(4)).length);
        assertEquals(252, ((ElementSpec[])info.get(6)).length);
        assertEquals(145, ((ElementSpec[])info.get(8)).length);
        assertEquals(2, htmlDoc.getTokenThreshold());
        insertMarker.reset();
        
        htmlDoc = new PublicHTMLDocument();
        insertMarker = htmlDoc.getInsertMarker();
        htmlDoc.setEditable(false);
        htmlDoc.setParser(parser);
        htmlDoc.setTokenThreshold(5);
        root = htmlDoc.getDefaultRootElement();
        branch = root.getElement(0).getElement(0);
        htmlDoc.insertAfterStart(branch, longString);
        assertFalse(createMarker.isOccurred());
        assertTrue(insertMarker.isOccurred());
        info = (ArrayList)insertMarker.getAuxiliary();
        assertNotNull(info);
        assertEquals(4, info.size()/2);
        assertEquals(6, ((ElementSpec[])info.get(0)).length);
        assertEquals(26, ((ElementSpec[])info.get(2)).length);
        assertEquals(126, ((ElementSpec[])info.get(4)).length);
        assertEquals(307, ((ElementSpec[])info.get(6)).length);
        assertEquals(5, htmlDoc.getTokenThreshold());
        insertMarker.reset();
    }

    public void testGetDefaultRootElement() {
    }

    public void testInsertUpdate() throws Exception {
        doc.insertString(0, "1111", null);
        final AttributeSet attr = doc.getCharacterElement(2).getAttributes();
        assertEquals(1, attr.getAttributeCount());
        assertEquals(Tag.CONTENT, attr.getAttribute(StyleConstants.NameAttribute));
    }

    public void testSerializable() throws Exception {
    }

    protected static void checkAttributes(final AttributeSet attr, final Object key, final Object value) {
        HTMLDocumentTestCase.checkAttributes(attr, key, value);
    }
    
}
