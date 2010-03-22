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

import javax.swing.text.AbstractDocument_LeafElementTest;
import javax.swing.text.StyleConstants;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.html.HTMLDocument.RunElement;
import javax.swing.text.html.HTMLDocument_BlockElementTest.LockableHTMLDocument;

public class HTMLDocument_RunElementTest extends
        AbstractDocument_LeafElementTest {

    protected LockableHTMLDocument htmlDoc;
    protected void setUp() throws Exception {
        super.setUp();

        htmlDoc = new LockableHTMLDocument();
        doc = htmlDoc;

        doc.insertString(0, "0123456789", as[0]);

        htmlDoc.lockWrite();
        BranchElement branch = doc.new BranchElement(null, as[1]);
        leaf1 = doc.new LeafElement(null, as[2], 0, 3);
        leaf2 = doc.new LeafElement(branch, as[2], 5, 8);
        htmlDoc.unlockWrite();
    }

    protected void tearDown() throws Exception {
        htmlDoc = null;
        super.tearDown();
    }

    public void testGetName() {
        AbstractElement run = htmlDoc.new RunElement(null, null, 0, 0);
        assertEquals("content", run.getName());
        htmlDoc.lockWrite();

        final String name = "asddsa";
        run = htmlDoc.new RunElement(null, null, 0, 0);
        run.addAttribute(StyleConstants.NameAttribute, name);
        assertEquals(name, run.getName());
    }

    public void testGetResolveParent() {
        AbstractElement parent = htmlDoc.new RunElement(null, null, 0, 0);
        AbstractElement block = htmlDoc.new RunElement(parent, null, 0, 0);
        assertNull(parent.getResolveParent());
        assertNull(block.getResolveParent());
    }

    public void testLeafElement() {
    }

    public void testRunElement() {
        htmlDoc.lockWrite();

        RunElement run = htmlDoc.new RunElement(leaf1, as[2],
                3, 9);

        assertSame(leaf1, run.getParent());
        assertSame(leaf1, run.getParentElement());
        assertSame(run, run.getAttributes());
        assertEquals(as[2].getAttributeCount(), run.getAttributeCount());
        assertEquals(3, run.getStartOffset());
        assertEquals(9, run.getEndOffset());

        int[] start    = {-1,  3,  3,  3}; // start offset
        int[] expStart = {0,  3,  3,  3};  // expectations for start offset
        int[] end      = {9, -1,  1, 20};  // end offset
        int[] expEnd   = {9,  0,  1, 20};  // expectations for end offset
        int[] intEnd   = {9,  3,  3, 20};  // expectations for DRL's end offset
        for (int i = 0; i < start.length; i++) {
            run = htmlDoc.new RunElement(null, null, start[i], end[i]);
            assertEquals("Start (" + i + ")", expStart[i],
                         run.getStartOffset());
            assertEquals("End (" + i + ")",
                         isHarmony() ? intEnd[i] : expEnd[i],
                         run.getEndOffset());
        }

        htmlDoc.unlockWrite();
    }
}
