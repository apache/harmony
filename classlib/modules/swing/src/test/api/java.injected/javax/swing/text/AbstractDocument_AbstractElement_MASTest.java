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
import javax.swing.text.AbstractDocument_AbstractElementTest.DisAbstractedDocument;
import javax.swing.text.AbstractDocument_AbstractElementTest.DisAbstractedDocument.DisAbstractedElement;

/**
 * Tests AbstractDocument.AbstractElement class - the part which implements
 * MutableAttributeSet interface. The document is write-lock during
 * execution of test-methods.
 *
 */
public class AbstractDocument_AbstractElement_MASTest extends MutableAttributeSetTest {
    protected DisAbstractedDocument aDocument;

    protected DisAbstractedElement aElement;

    protected AbstractElement parented;

    protected AttributeSet aSet;

    public AbstractDocument_AbstractElement_MASTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aDocument = new DisAbstractedDocument(new GapContent());
        aDocument.writeLock();
        aElement = aDocument.new DisAbstractedElement(null, mas);
        mas = aElement;
        as = aElement;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        aDocument.writeUnlock();
    }
}