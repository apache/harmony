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
package javax.swing.text;

import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocumentTest.DisAbstractedDocument;

/**
 * Tests AbstractDocument.LeafElement class methods implementing
 * TreeNode interface.
 *
 */
public class AbstractDocument_LeafElement_TreeNodeTest extends
        AbstractDocument_AbstractElement_TreeNodeTest {
    @Override
    protected void setUp() throws Exception {
        doc = new DisAbstractedDocument(new GapContent());
        doc.writeLock();
        BranchElement branch = doc.new BranchElement(null, null);
        aElement = doc.new LeafElement(null, null, 0, 3);
        parented = doc.new LeafElement(parent = branch, null, 5, 8);
        doc.writeUnlock();
    }
}