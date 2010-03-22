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

import javax.swing.text.AbstractDocument_BranchElementTest;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.swing.text.html.HTMLDocument.BlockElement;

public class HTMLDocument_BlockElementTest extends
        AbstractDocument_BranchElementTest {

    public static class LockableHTMLDocument extends HTMLDocument {
        public void lockWrite() {
            writeLock();
        }
        public void unlockWrite() {
            writeUnlock();
        }
    }

    protected LockableHTMLDocument htmlDoc;

    protected void setUp() throws Exception {
        super.setUp();

        htmlDoc = new LockableHTMLDocument();
        doc = htmlDoc;

        doc.insertString(0, LTR + RTL + LTR + RTL + "\n01234", as[0]);

        bidi  = (BranchElement)doc.getBidiRootElement();
        leaf1 = (LeafElement)bidi.getElement(0).getElement(0);
        par   = (BranchElement)doc.getDefaultRootElement();
        leaf2 = par != null ? par.getElement(0) : null;
        leaf3 = leaf2;
    }

    protected void tearDown() throws Exception {
        htmlDoc = null;
        super.tearDown();
    }

    public void testGetElementCount() {
        assertEquals(5, bidi.getElementCount());
        assertEquals(1, par.getElementCount());
    }

    public void testToString() {
        assertEquals("BranchElement(bidi root) 0,15\n", bidi.toString());
        assertEquals("BranchElement(html) 0,15\n", par.toString());
    }

    public void testGetName() {
        AbstractElement block = htmlDoc.new BlockElement(null, null);
        assertEquals("paragraph", block.getName());
        htmlDoc.lockWrite();

        final String name = "asddsa";
        block = htmlDoc.new BlockElement(null, null);
        block.addAttribute(StyleConstants.NameAttribute, name);
        assertEquals(name, block.getName());
    }

    public void testGetElement() {
        if (isHarmony()) {
            assertNull(par.getElement(-1));
        }
        assertEquals(leaf2, par.getElement(0));
        assertNull(par.getElement(1));
        assertNull(par.getElement(2));
    }

    public void testGetElementIndex01() {
        assertEquals(0, par.getElementIndex(-1));
        assertEquals(0, par.getElementIndex(7));
        assertEquals(0, par.getElementIndex(8));
        assertEquals(0, par.getElementIndex(9));
        assertEquals(0, par.getElementIndex(10));
        assertEquals(0, par.getElementIndex(11));
        assertEquals(0, par.getElementIndex(20));
    }

    public void testBranchElement() {
    }

    public void testGetResolveParent() {
        htmlDoc.lockWrite();
        AbstractElement parent = htmlDoc.new BlockElement(null, null);
        AbstractElement block = htmlDoc.new BlockElement(parent, null);
        assertNull(parent.getResolveParent());
        assertNull(block.getResolveParent());

        block.setResolveParent(parent);
        assertNull(block.getResolveParent());
    }

    public void testBlockElement() throws BadLocationException {
        htmlDoc.lockWrite();
        htmlDoc.remove(0, htmlDoc.getLength());
        BlockElement block = htmlDoc.new BlockElement(par, as[0]);
        assertSame(par, block.getParentElement());
        assertNull(block.getResolveParent());
        assertEquals(0, block.getElementCount());
    }
}