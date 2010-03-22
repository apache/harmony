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
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.BasicSwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefStyledDoc_Helpers;
import javax.swing.text.GapContent;
import javax.swing.text.StyleConstants;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

public abstract class HTMLDocumentTestCase extends BasicSwingTestCase {
    
    public static class PublicHTMLDocument extends HTMLDocument {
        private final Marker insertMarker = new Marker();
        private final Marker createMarker = new Marker();
        
        private boolean editable = true;
        
        public PublicHTMLDocument(final StyleSheet sheet) {
            super(sheet);
        }

        public PublicHTMLDocument() {
            super();
        }

        public PublicHTMLDocument(final GapContent content, final StyleSheet styles) {
            super(content, styles);
        }

        public void lockWrite() {
            writeLock();    
        }
        
        public void unlockWrite() {
            writeUnlock();    
        }
        
        public void setEditable(final boolean editable) {
            this.editable = editable;
        }
        
        public Content getContentPublicly() {
            return super.getContent();
        }

        public  AttributeContext getAttributeContextPublicly() {
            return super.getAttributeContext();
        }
        
        protected void insert(final int offset, final ElementSpec[] specs) throws BadLocationException {
            ArrayList info = insertMarker.getAuxiliary() != null ? (ArrayList)insertMarker.getAuxiliary() : new ArrayList();
            info.add(specs);
            info.add(new Integer(offset));
            insertMarker.setAuxiliary(info);
            insertMarker.setOccurred();
            if (editable) {
                super.insert(offset, specs);
            }
        }
        
        public Marker getInsertMarker() {
            return insertMarker;
        }
        
        protected void create(final ElementSpec[] specs) {
            ArrayList info = createMarker.getAuxiliary() != null ? (ArrayList)createMarker.getAuxiliary() : new ArrayList();
            info.add(specs);
            createMarker.setAuxiliary(info);
            createMarker.setOccurred();
            if (editable) {
                super.create(specs);
            }
        }

        public Marker getCreateMarker() {
            return createMarker;
        }
    }

    public static class DocumentController extends EventsController implements DocumentListener {
        public void insertUpdate(final DocumentEvent e) {
            processEvent(e);
        }

        public void removeUpdate(final DocumentEvent e) {
            processEvent(e);
        }

        public void changedUpdate(final DocumentEvent e) {
            processEvent(e);
        }

        protected void processEvent(final DocumentEvent e) {
            addEvent(Integer.toString(getNumEvents()), e);
            if (isVerbose()) {
                System.err.println(e);
            }
        }
        
        public DocumentEvent getEvent(final int index) {
            return (DocumentEvent)super.getEvent(Integer.toString(index));
        }
    }
    
    
    public static void assertSpec(final ElementSpec spec, final short type,
                                   final short direction, final int offset,
                                   final char[] array) {
        int length = array != null ? array.length : 0;
        assertSpec(spec, type, direction, offset, length, array);
    }

    public static void assertSpec(final ElementSpec spec, final short type,
                                  final short direction, final int offset, final int length, 
                                  final char[] array) {
       DefStyledDoc_Helpers.assertSpec(spec, type, direction, offset, length, length == 0);
       if (array != null) {
           assertEquals("text", new String(array), new String(spec.getArray()));
       }
   }

    public static void checkOpenImpliedSpec(final ElementSpec spec) {
        AttributeSet specAttr = spec.getAttributes();
        assertEquals("number of attributes", 1, specAttr.getAttributeCount());
        checkAttributes(specAttr, StyleConstants.NameAttribute, HTML.Tag.IMPLIED);
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.OriginateDirection, 0, null);
    }
    
    public static void checkAttributes(final AttributeSet attr, final Object key, final Object value) {
        final Enumeration attributeNames = attr.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            Object name = (Object)attributeNames.nextElement();
            if (name.equals(key)) {
                assertEquals("attribute value", value.toString(), attr.getAttribute(key).toString());
                return;
            }
        }
        fail("attribute is not found");
    }
    
    public static void checkEndTagSpec(final ElementSpec spec) {
        assertSpec(spec, ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, null);
        assertNull(spec.getAttributes());
    }

    public static void checkStartJNTagSpec(final ElementSpec spec) {
        assertSpec(spec, ElementSpec.StartTagType, ElementSpec.JoinNextDirection, 0, null);
        assertNull(spec.getAttributes());
    }

    public static void checkImplicitContentSpec(ElementSpec spec) {
        assertSpec(spec, ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, new char[]{' '});
    }
    
    public static void loadDocument(final HTMLDocument doc, final String content) throws Exception {
        final ParserCallback reader = doc.getReader(0);
        new ParserDelegator().parse(new StringReader(content), reader, true);
        reader.flush();
    }
}
