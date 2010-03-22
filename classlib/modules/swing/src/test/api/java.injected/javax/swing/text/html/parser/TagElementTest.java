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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text.html.parser;

import javax.swing.text.html.HTML;

import junit.framework.TestCase;

public class TagElementTest extends TestCase {
    Element element;
    TagElement tagElement;

    protected void setUp() throws Exception {
        super.setUp();
        element = new Element();
        element.name = "test";
        tagElement = new TagElement(element);
    }

    public void testFictional() {
        assertFalse(tagElement.fictional());
        tagElement = new TagElement(element, true);
        assertTrue(tagElement.fictional());
        tagElement = new TagElement(element, false);
        assertFalse(tagElement.fictional());
    }

    public void testGetHTMLTag() {
        HTML.Tag tag = tagElement.getHTMLTag();
        assertTrue(tag instanceof HTML.UnknownTag);
        //name check
        assertEquals("test", tag.toString());

        element.name = DTDTest.HTML;
        tagElement = new TagElement(element);
        tag = tagElement.getHTMLTag();
        assertFalse(tag instanceof HTML.UnknownTag);
        assertEquals(HTML.Tag.HTML, tag);
    }

    public void testGetElement() {
        assertEquals(element, tagElement.getElement());
    }

    public void testIsPreformatted() {
        assertFalse(tagElement.isPreformatted());
        element.name = DTDTest.conv("pre");
        tagElement = new TagElement(element);
        assertTrue(tagElement.isPreformatted());
        element.name = DTDTest.conv("textarea");
        tagElement = new TagElement(element);
        assertTrue(tagElement.isPreformatted());
    }

    public void testBreaksFlow() {
        element.name = DTDTest.conv("td");
        tagElement = new TagElement(element);
        assertTrue(tagElement.breaksFlow());

        element.name = DTDTest.conv("textarea");
        tagElement = new TagElement(element);
        assertFalse(tagElement.breaksFlow());
    }
}
