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

import java.util.ArrayList;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.parser.ParserDelegator;

public class HTMLDocument_InsertsTest extends HTMLDocumentTestCase {

    protected PublicHTMLDocument htmlDoc;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        htmlDoc = new PublicHTMLDocument();
        timeoutDelay = Integer.MAX_VALUE;
    }

    protected void tearDown() throws Exception {
        htmlDoc = null;
        super.tearDown();
    }

    public void testInsertAfterEnd_Specs() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        loadDocument(htmlDoc, "<table><tr><td>table</td></td></tr></table>");
        htmlDoc.setEditable(false);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(1);
        Element table = body.getElement(0);
        Element tr = table.getElement(0);
        Element td = tr.getElement(0);

        htmlDoc.insertAfterEnd(td, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(3, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        assertSpec(specs[2], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[2].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);

        htmlDoc.insertAfterEnd(tr, "<a>link</a>");
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(4, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkEndTagSpec(specs[2]);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[3].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        
        htmlDoc.insertAfterEnd(table, "<a>link</a>");
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(5, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkEndTagSpec(specs[2]);
        checkEndTagSpec(specs[3]);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[4].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
    }

    public void testInsertAfterEnd_Specs2() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        htmlDoc.setEditable(false);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);

        htmlDoc.insertAfterEnd(p, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(0), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(2, specs.length);
        checkEndTagSpec(specs[0]);
        assertSpec(specs[1], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[1].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
    }

    private ArrayList getInsertInfo(Marker insertMarker) {
        return (ArrayList)insertMarker.getAuxiliary();
    }

    public void testInsertAfterEnd_Events() throws Exception {
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);
        addElement();

        htmlDoc.setParser(new ParserDelegator());
        DocumentController listener = new DocumentController();
        htmlDoc.addDocumentListener(listener);
        htmlDoc.insertAfterEnd(p, "<a>link</a><b>bold</b>");
        assertEquals(1, listener.getNumEvents());
        checkEvent(body, listener.getEvent(0), DocumentEvent.EventType.INSERT, 3, 8, 4);

        listener.reset();
        htmlDoc.insertAfterEnd(body, "<a>link</a><b>bold</b>");
        assertEquals(1, listener.getNumEvents());
        checkEvent(root, listener.getEvent(0), DocumentEvent.EventType.INSERT, 3, 8, 12);
    }

    public void testInsertAfterEnd_Structure() throws Exception {
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);
        addElement();
        
        htmlDoc.setParser(new ParserDelegator());
        assertEquals(1, body.getElementCount());
        htmlDoc.insertAfterEnd(p, "<a>link</a><b>bold</b>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(4), getInsertInfo(insertMarker).get(1));
        insertMarker.reset();
        assertEquals("0000linkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "p", "b", "content", "content", "p", "content"});
        assertEquals(4, body.getElementCount());
        assertEquals(1, p.getElementCount());
        assertEquals(1, root.getElementCount());

        htmlDoc.insertAfterEnd(body, "<a>link</a><b>bold</b>");
        assertEquals(new Integer(12), getInsertInfo(insertMarker).get(1));
        insertMarker.reset();
        assertEquals("0000linkboldlinkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "p", "b", "content", "content", "content", "content", "body", "p", "content"});
        assertEquals(3, body.getElementCount());
        assertEquals(1, p.getElementCount());
        assertEquals(4, root.getElementCount());
        
        htmlDoc.insertAfterEnd(root, "<a>link</a><b>bold</b>");
        assertEquals("0000linkboldlinkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "p", "b", "content", "content", "content", "content", "body", "p", "content"});
        assertEquals(3, body.getElementCount());
        assertEquals(1, p.getElementCount());
        assertEquals(4, root.getElementCount());
    }
        
    public void testInsertAfterEnd_Exceptions() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);
        Element leaf = p.getElement(0);

        try {
            htmlDoc.insertAfterEnd(leaf, "<a>link</a>");
            fail("IllegalStateException should be thrown");
        } catch (IllegalStateException e) {
        }

        htmlDoc.setParser(new ParserDelegator());
        htmlDoc.insertAfterEnd(null, "<a>link</a>");
    }

    public void testInsertAfterStart_Specs() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        loadDocument(htmlDoc, "<table><tr><td><div>cell</div></td></tr></table>");
        htmlDoc.setEditable(true);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(1);
        Element table = body.getElement(0);
        Element tr = table.getElement(0);
        Element td = tr.getElement(0);
        Element div = td.getElement(0);

        htmlDoc.insertAfterStart(div, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(8, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        checkStartJNTagSpec(specs[4]);
        checkStartJNTagSpec(specs[5]);
        checkStartJNTagSpec(specs[6]);
        assertSpec(specs[7], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[7].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);

        htmlDoc.insertAfterStart(tr, "<a>link</a>");
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(6, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        checkStartJNTagSpec(specs[4]);
        assertSpec(specs[5], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[5].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);

        htmlDoc.insertAfterStart(table, "<a>link</a>");
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(5, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[4].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
    }

    public void testInsertAfterStart_Specs2() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        htmlDoc.setEditable(false);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);

        htmlDoc.insertAfterStart(p, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(0), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[0].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
    }

    public void testInsertAfterStart_Events() throws Exception {
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);
        addElement();

        htmlDoc.setParser(new ParserDelegator());
        DocumentController listener = new DocumentController();
        htmlDoc.addDocumentListener(listener);
        htmlDoc.insertAfterStart(p, "<a>link</a><b>bold</b>");
        assertEquals(1, listener.getNumEvents());
        checkEvent(p, listener.getEvent(0), DocumentEvent.EventType.INSERT, 3, 8, 0);

        listener.reset();
        htmlDoc.insertAfterStart(body, "<a>link</a><b>bold</b>");
        assertEquals(1, listener.getNumEvents());
        if (!isHarmony()) {
            checkEvent(body, listener.getEvent(0), DocumentEvent.EventType.INSERT, 3, 8, 0);
        } else {
            checkEvent(body, listener.getEvent(0), DocumentEvent.EventType.INSERT, 2, 8, 0);
        }
    }
    
    public void testInsertAfterStart_Strucutre() throws Exception {
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);
        addElement();
        
        htmlDoc.setParser(new ParserDelegator());
        assertEquals(1, body.getElementCount());
        htmlDoc.insertAfterStart(p, "<a>link</a><b>bold</b>");
        assertEquals("linkbold0000", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "p", "content", "content", "b", "content"});
        assertEquals(1, body.getElementCount());
        assertEquals(4, p.getElementCount());
        assertEquals(1, root.getElementCount());

        htmlDoc.insertAfterStart(body, "<a>link</a><b>bold</b>");
        assertEquals("linkboldlinkbold0000", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "content", "content", "p", "content", "content", "b", "content"});
        assertEquals(1, root.getElementCount());
        assertEquals(3, body.getElementCount());
        p = body.getElement(2);
        assertEquals(4, p.getElementCount());
        
        htmlDoc.insertAfterStart(root, "<a>link</a><b>bold</b>");
        assertEquals("linkboldlinkboldlinkbold0000", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "content", "content", "body", "content", "content", "p", "content", "content", "b", "content"});
        assertEquals(3, root.getElementCount());
        body = root.getElement(2);
        assertEquals(3, body.getElementCount());
        assertEquals(4, p.getElementCount());
    }
    
    public void testInsertAfterStart_Exceptions() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
        Element branch3 = branch2.getElement(0);

        try {
            htmlDoc.insertAfterStart(branch3, "<a>link</a>");
            fail("IllegalStateException should be thrown");
        } catch (IllegalStateException e) {
        }

        htmlDoc.setParser(new ParserDelegator());

        try {
            htmlDoc.insertAfterStart(branch3, "<a>link</a>");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
        }

        try {
            htmlDoc.insertAfterStart(null, "<a>link</a>");
            fail("NullPointerException should be thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testInsertBeforeEnd_Specs() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        loadDocument(htmlDoc, "<table><tr><td><div>table</div></td</td></tr></table>");
        htmlDoc.setEditable(false);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(1);
        Element table = body.getElement(0);
        Element tr = table.getElement(0);
        Element td = tr.getElement(0);

        htmlDoc.insertBeforeEnd(td, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(7), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(3, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        assertSpec(specs[2], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[2].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        insertMarker.reset();

        htmlDoc.insertBeforeEnd(tr, "<a>link</a>");
        assertEquals(new Integer(7), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(4, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkEndTagSpec(specs[2]);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[3].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);

        htmlDoc.insertBeforeEnd(table, "<a>link</a>");
        assertEquals(new Integer(7), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(5, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkEndTagSpec(specs[2]);
        checkEndTagSpec(specs[3]);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[4].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
    }

    public void testInsertBeforeEnd_Specs2() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        htmlDoc.setEditable(false);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);

        htmlDoc.insertBeforeEnd(p, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(0), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[0].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
    }

    public void testInsertBeforeEnd_Events() throws Exception {
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);
        addElement();

        htmlDoc.setParser(new ParserDelegator());
        DocumentController listener = new DocumentController();
        htmlDoc.addDocumentListener(listener);
        htmlDoc.insertBeforeEnd(p, "<a>link</a><b>bold</b>");
        assertEquals(1, listener.getNumEvents());
        checkEvent(p, listener.getEvent(0), DocumentEvent.EventType.INSERT, 3, 8, 4);

        listener.reset();
        htmlDoc.insertBeforeEnd(body, "<a>link</a><b>bold</b>");
        assertEquals(1, listener.getNumEvents());
        checkEvent(body, listener.getEvent(0), DocumentEvent.EventType.INSERT, 2, 8, 13);
    }
    
    public void testInsertBeforeEnd_Strucutre() throws Exception {
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);
        addElement();
        
        htmlDoc.setParser(new ParserDelegator());
        assertEquals(1, body.getElementCount());
        htmlDoc.insertBeforeEnd(p, "<a>link</a><b>bold</b>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(4), getInsertInfo(insertMarker).get(1));
        assertEquals("0000linkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "p", "b", "content", "content", "content"});
        assertEquals(1, body.getElementCount());
        assertEquals(4, p.getElementCount());
        assertEquals(1, root.getElementCount());
        insertMarker.reset();

        htmlDoc.insertBeforeEnd(body, "<a>link</a><b>bold</b>");
        assertEquals(new Integer(13), getInsertInfo(insertMarker).get(1));
        assertEquals("0000linkbold\nlinkbol", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "p", "b", "content", "content", "content", "content", "content"});
        assertEquals(3, body.getElementCount());
        assertEquals(4, p.getElementCount());
        assertEquals(1, root.getElementCount());
        insertMarker.reset();
        
        htmlDoc.insertBeforeEnd(root, "<a>link</a><b>bold</b>");
        assertEquals(new Integer(21), getInsertInfo(insertMarker).get(1));
        assertEquals("0000linkbold\nlinkboldlinkbol", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "p", "b", "content", "content", "content", "content", "content", "content", "content"});
        assertEquals(3, body.getElementCount());
        assertEquals(4, p.getElementCount());
        assertEquals(3, root.getElementCount());
    }

    public void testInsertBeforeEnd_Exceptions() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
        Element branch3 = branch2.getElement(0);

        try {
            htmlDoc.insertBeforeEnd(branch3, "<a>link</a>");
            fail("IllegalStateException should be thrown");
        } catch (IllegalStateException e) {
        }

        htmlDoc.setParser(new ParserDelegator());
    
        try {
            htmlDoc.insertBeforeEnd(branch3, "<a>link</a>");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
        }
        
        try {
            htmlDoc.insertBeforeEnd(null, "<a>link</a>");
            fail("NullPointerException should be thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testInsertBeforeStart_Specs() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        loadDocument(htmlDoc, "<table><tr><td>table</td></tr></table>");
        htmlDoc.setEditable(true);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(1);
        Element table = body.getElement(0);
        Element tr = table.getElement(0);
        Element td = tr.getElement(0);

        htmlDoc.insertBeforeStart(td, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(6, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        checkStartJNTagSpec(specs[4]);
        assertSpec(specs[5], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[5].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);

        htmlDoc.insertBeforeStart(tr, "<a>link</a>");
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(5, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[4].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);

        htmlDoc.insertBeforeStart(table, "<a>link</a>");
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(4, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[3].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
    }

    public void testInsertBeforeStart_Specs2() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        htmlDoc.setEditable(false);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);

        htmlDoc.insertBeforeStart(p, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(0), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(2, specs.length);
        checkEndTagSpec(specs[0]);
        assertSpec(specs[1], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[1].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
    }

    public void testInsertBeforeStart_Events() throws Exception {
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
        addElement();

        htmlDoc.setParser(new ParserDelegator());
        DocumentController listener = new DocumentController();
        htmlDoc.addDocumentListener(listener);
        htmlDoc.insertBeforeStart(branch2, "<a>link</a><b>bold</b>");
        assertEquals(1, listener.getNumEvents());
        if (!isHarmony()) {
            checkEvent(branch1, listener.getEvent(0), DocumentEvent.EventType.INSERT, 3, 8, 0);
        } else {
            checkEvent(branch1, listener.getEvent(0), DocumentEvent.EventType.INSERT, 2, 8, 0);
        }

        listener.reset();
        htmlDoc.insertBeforeStart(branch1, "<a>link</a><b>bold</b>");
        assertEquals(1, listener.getNumEvents());
        if (!isHarmony()) {
            checkEvent(root, listener.getEvent(0), DocumentEvent.EventType.INSERT, 3, 8, 0);
        } else {
            checkEvent(root, listener.getEvent(0), DocumentEvent.EventType.INSERT, 2, 8, 0);
        }
    }
    
    public void testInsertBeforeStart_Structure() throws Exception {
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);
        addElement();
        
        htmlDoc.setParser(new ParserDelegator());
        assertEquals(1, body.getElementCount());
        htmlDoc.insertBeforeStart(p, "<a>link</a><b>bold</b>");
        assertEquals("linkbold0000", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "body", "content", "content", "p", "b", "content"});
        assertEquals(1, root.getElementCount());
        assertEquals(3, body.getElementCount());
        p = body.getElement(2);
        assertEquals(2, p.getElementCount());

        htmlDoc.insertBeforeStart(body, "<a>link</a><b>bold</b>");
        assertEquals("linkboldlinkbold0000", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "content", "content", "body", "content", "content", "p", "b", "content"});
        assertEquals(3, root.getElementCount());
        body = root.getElement(2);
        assertEquals(3, body.getElementCount());
        assertEquals(2, p.getElementCount());
        
        htmlDoc.insertBeforeStart(root, "<a>link</a><b>bold</b>");
        assertEquals("linkboldlinkbold0000", htmlDoc.getText(0, htmlDoc.getLength()));
        checkStructure(htmlDoc, new String[]{"html", "content", "content", "body", "content", "content", "p", "b", "content"});
        assertEquals(3, root.getElementCount());
        assertEquals(3, body.getElementCount());
        assertEquals(2, p.getElementCount());
    }
    
    public void testInsertBeforeStart_Exceptions() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
        Element branch3 = branch2.getElement(0);
    
        try {
            htmlDoc.insertBeforeStart(branch3, "<a>link</a>");
            fail("IllegalStateException should be thrown");
        } catch (IllegalStateException e) {
        }
        
        htmlDoc.setParser(new ParserDelegator());
        htmlDoc.insertBeforeStart(null, "<a>link</a>");
    }

    public void testSetInnerHTML_Specs() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        loadDocument(htmlDoc, "<table><tr><td>table</td></tr></table>");
        htmlDoc.setEditable(false);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(1);
        Element table = body.getElement(0);
        Element tr = table.getElement(0);
        Element td = tr.getElement(0);

        htmlDoc.setInnerHTML(td, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(10, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        checkStartJNTagSpec(specs[4]);
        checkStartJNTagSpec(specs[5]);
        assertSpec(specs[6], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[6].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(specs[7], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
        checkEndTagSpec(specs[8]);
        checkEndTagSpec(specs[9]);

        htmlDoc.setInnerHTML(tr, "<a>link</a>");
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(9, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        checkStartJNTagSpec(specs[4]);
        assertSpec(specs[5], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[5].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(specs[6], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
        checkEndTagSpec(specs[7]);
        checkEndTagSpec(specs[8]);

        htmlDoc.setInnerHTML(table, "<a>link</a>");
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(8, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[4].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(specs[5], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
        checkEndTagSpec(specs[6]);
        checkEndTagSpec(specs[7]);
    }

    public void testSetInnerHTML_Specs2() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        htmlDoc.setEditable(false);
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);

        htmlDoc.setInnerHTML(p, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(0), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[0].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(specs[1], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
        checkEndTagSpec(specs[2]);
        checkEndTagSpec(specs[3]);
    }

    public void testSetInnerHTML_Events() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        final Element branch2 = branch1.getElement(0);
        Element branch3 = branch2.getElement(0);

        htmlDoc.setParser(new ParserDelegator());
        DocumentController listener = new DocumentController();
        htmlDoc.addDocumentListener(listener);
        htmlDoc.setInnerHTML(branch2, "<a>link</a><b>bold</b>");
        assertEquals(2, listener.getNumEvents());
        checkEvent(branch2, listener.getEvent(0), DocumentEvent.EventType.INSERT, 4, 9, 0);
        checkEvent(branch2, listener.getEvent(1), DocumentEvent.EventType.REMOVE, 4, 14, 8);

        listener.reset();
        htmlDoc.setInnerHTML(branch1, "<a>link</a><b>bold</b>");
        assertEquals(2, listener.getNumEvents());
        checkEvent(branch1, listener.getEvent(0), DocumentEvent.EventType.INSERT, 4, 9, 0);
        checkEvent(branch1, listener.getEvent(1), DocumentEvent.EventType.REMOVE, 2, 10, 8);
    }

    public void testSetInnerHTML_Structure() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
        Element branch3 = branch2.getElement(0);
        
        htmlDoc.setParser(new ParserDelegator());
        assertEquals(4, branch2.getElementCount());
        htmlDoc.setInnerHTML(branch2, "<a>link</a><b>bold</b>");
        assertEquals("linkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        assertEquals(3, branch2.getElementCount());
        
        htmlDoc.setInnerHTML(branch1, "<a>link</a><b>bold</b>");
        assertEquals("linkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        assertEquals(3, branch1.getElementCount());
        
        htmlDoc.setInnerHTML(root, "<a>link</a><b>bold</b>");
        assertEquals("linkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        assertEquals(3, root.getElementCount());
    }
    
    public void testSetInnerHTML_Exceptions() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
        Element branch3 = branch2.getElement(0);
        
        try {
            htmlDoc.setInnerHTML(branch3, "<a>link</a>");
            fail("IllegalStateException should be thrown");
        } catch (IllegalStateException e) {
        }
        
        htmlDoc.setParser(new ParserDelegator());
        try {
            htmlDoc.setInnerHTML(branch3, "<a>link</a>");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
        }

        htmlDoc.setInnerHTML(null, "<a>link</a>");
    }

    public void testSetOuterHTML_Specs() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        loadDocument(htmlDoc, "<table><tr><td><br>table</td></tr></table>");
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(1);
        Element table = body.getElement(0);
        Element tr = table.getElement(0);
        Element td = tr.getElement(0);
        Element br = td.getElement(0).getElement(0);

        htmlDoc.setOuterHTML(br, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        insertMarker.reset();
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        checkStartJNTagSpec(specs[4]);
        checkStartJNTagSpec(specs[5]);
        checkStartJNTagSpec(specs[6]);
        assertSpec(specs[7], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());

        htmlDoc.setOuterHTML(td, "<a>link</a>");
        insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        insertMarker.reset();
        assertEquals(9, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        checkStartJNTagSpec(specs[4]);
        assertSpec(specs[5], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[5].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(specs[6], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
        checkEndTagSpec(specs[7]);
        checkEndTagSpec(specs[8]);

        htmlDoc.setOuterHTML(tr, "<a>link</a>");
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        insertMarker.reset();
        assertEquals(8, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        checkStartJNTagSpec(specs[3]);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[4].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(specs[5], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
        checkEndTagSpec(specs[6]);
        checkEndTagSpec(specs[7]);

        htmlDoc.setOuterHTML(table, "<a>link</a>");
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        assertEquals(new Integer(1), getInsertInfo(insertMarker).get(1));
        insertMarker.reset();
        assertEquals(6, specs.length);
        checkEndTagSpec(specs[0]);
        checkEndTagSpec(specs[1]);
        checkStartJNTagSpec(specs[2]);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        specAttr = specs[3].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(specs[4], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
        checkEndTagSpec(specs[5]);
    }

    public void testSetOuterHTML_Specs2() throws Exception {
        htmlDoc.setParser(new ParserDelegator());
        
        Element root = htmlDoc.getDefaultRootElement();
        Element body = root.getElement(0);
        Element p = body.getElement(0);

        htmlDoc.setOuterHTML(p, "<a>link</a>");
        Marker insertMarker = htmlDoc.getInsertMarker();
        assertEquals(new Integer(0), getInsertInfo(insertMarker).get(1));
        ElementSpec[] specs = (ElementSpec[])(getInsertInfo(insertMarker).get(0));
        insertMarker.reset();
        assertEquals(4, specs.length);
        checkEndTagSpec(specs[0]);
        assertSpec(specs[1], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, "link".toCharArray());
        AttributeSet specAttr = specs[1].getAttributes();
        assertEquals(2, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, Tag.CONTENT);
        assertSpec(specs[2], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{'\n'});
        checkEndTagSpec(specs[3]);
    }

    public void testSetOuterHTML_Events() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
        Element branch3 = branch2.getElement(0);
        htmlDoc.setParser(new ParserDelegator());
        
        DocumentController listener = new DocumentController();
        htmlDoc.addDocumentListener(listener);
        
        assertEquals(4, branch2.getElementCount());
        htmlDoc.setOuterHTML(branch3, "<u>link</u>");
        assertEquals(4, branch2.getElementCount());
        assertEquals(2, listener.getNumEvents());
        checkEvent(branch2, listener.getEvent(0), DocumentEvent.EventType.INSERT, 2, 4, 0);
        checkEvent(branch2, listener.getEvent(1), DocumentEvent.EventType.REMOVE, 1, 4, 4);
        
        
        assertEquals(1, branch1.getElementCount());
        listener.reset();
        htmlDoc.setOuterHTML(branch2, "<a>link</a><b>aaaaaa</b>");
        assertEquals(3, branch1.getElementCount());
        assertEquals(2, listener.getNumEvents());
        checkEvent(branch1, listener.getEvent(0), EventType.INSERT, 4, 11, 0);
        checkEvent(branch1, listener.getEvent(1), EventType.REMOVE, 2, 14, 10);
    }
    
    public void testSetOuterHTML_Structure() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
    
        htmlDoc.setParser(new ParserDelegator());
        assertEquals(1, branch1.getElementCount());
        htmlDoc.setOuterHTML(branch2, "<a>link</a><b>bold</b>");
        assertEquals("linkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        assertEquals(3, branch1.getElementCount());
    
        htmlDoc.setOuterHTML(branch1, "<a>link</a><b>bold</b>");
        assertEquals("linkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        assertEquals(3, root.getElementCount());
        
        htmlDoc.setOuterHTML(root, "<a>link</a><b>bold</b>");
        assertEquals("linkbold", htmlDoc.getText(0, htmlDoc.getLength()));
        assertEquals(3, htmlDoc.getDefaultRootElement().getElementCount());
    }

    public void testSetOuterHTML_Exceptions() throws Exception {
        addElements();
        Element root = htmlDoc.getDefaultRootElement();
        Element branch1 = root.getElement(0);
        Element branch2 = branch1.getElement(0);
        Element branch3 = branch2.getElement(0);
        
        try {
            htmlDoc.setOuterHTML(branch3, "<a>link</a>");
            fail("IllegalStateException should be thrown");
        } catch (IllegalStateException e) {
        }

        htmlDoc.setParser(new ParserDelegator());
        htmlDoc.setOuterHTML(null, "<a>link</a>");
    }
    
    private void checkEvent(final Element elem, final DocumentEvent event, final EventType type,
                            final int numChanged,
                            final int length, final int offset) {
        assertEquals("type", type, event.getType());
        if (type == EventType.INSERT) {
            assertEquals("inserted", numChanged, event.getChange(elem).getChildrenAdded().length);
        } else if (type == EventType.REMOVE) {
            assertEquals("removed", numChanged, event.getChange(elem).getChildrenRemoved().length);
        }
        assertEquals("length", length, event.getLength());
        assertEquals("offset", offset, event.getOffset());
    }

    private void addElements() throws BadLocationException {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        htmlDoc.insertString(0, "0000", attr);
        attr.addAttribute(StyleConstants.NameAttribute, Tag.I.toString());
        htmlDoc.insertString(4, "1111", attr);
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        htmlDoc.insertString(8, "2222", attr);
    }

    private void addElement() throws BadLocationException {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.NameAttribute, Tag.B.toString());
        htmlDoc.insertString(0, "0000", attr);
    }

    private void checkStructure(final Document doc,  final String[] names) {
        Element root = doc.getDefaultRootElement();
        ArrayList array = new ArrayList();
        addChildren(array, root);
        String[] structure = (String[])array.toArray(new String[array.size()]);
        assertEquals(names.length, structure.length);
        for (int i = 0; i < structure.length; i++) {
            assertEquals(names[i], structure[i]);
        }
    }
    
    private void addChildren(final ArrayList array, final Element parent) {
        array.add(parent.getName());
        for (int i = 0; i < parent.getElementCount(); i++) {
            addChildren(array, parent.getElement(i));
        }
    }
}
