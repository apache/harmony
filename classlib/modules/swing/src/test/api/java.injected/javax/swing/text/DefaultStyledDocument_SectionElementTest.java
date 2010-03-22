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

import javax.swing.text.DefaultStyledDocument.SectionElement;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument.SectionElement class.
 *
 */
public class DefaultStyledDocument_SectionElementTest extends TestCase {
    private DefaultStyledDocument doc;

    private SectionElement section;

    public void testGetName() {
        assertSame(AbstractDocument.SectionElementName, section.getName());
    }

    public void testSectionElement() {
        assertNull(section.getParent());
        assertSame(section, section.getAttributes());
        assertEquals(0, section.getAttributeCount());
        assertEquals(0, section.getElementCount());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        section = doc.new SectionElement();
    }
}
