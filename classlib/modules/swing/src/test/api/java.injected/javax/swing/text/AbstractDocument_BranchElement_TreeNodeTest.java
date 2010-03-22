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

import javax.swing.text.AbstractDocument.AbstractElement;

/**
 * Tests AbstractDocument.BranchElement class methods
 * implementing TreeNode interface.
 *
 */
public class AbstractDocument_BranchElement_TreeNodeTest extends
        AbstractDocument_AbstractElement_TreeNodeTest {
    @Override
    protected void setUp() throws Exception {
        doc = new PlainDocument();
        doc.insertString(0, AbstractDocument_BranchElementTest.LTR
                + AbstractDocument_BranchElementTest.RTL
                + AbstractDocument_BranchElementTest.LTR
                + AbstractDocument_BranchElementTest.RTL + "\n01234", null);
        doc.writeLock();
        aElement = (AbstractElement) doc.getDefaultRootElement();
        parented = doc.new BranchElement(parent = aElement, null);
        doc.writeUnlock();
    }
}