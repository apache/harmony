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
package javax.swing.text.html;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTML.UnknownTag;

public class HTML_UnknownTagTest extends BasicSwingTestCase {
    private static final String tagName = "verb";
    private Tag tag;

    protected void setUp() throws Exception {
        super.setUp();
        tag = new UnknownTag(tagName);
    }

    public void testUnknownTag() {
        assertSame(tagName, tag.toString());
        assertFalse("breaks Flow", tag.breaksFlow());
        assertFalse("isBlock", tag.isBlock());
        assertFalse("isPre", tag.isPreformatted());
    }

    public void testEqualsObject() {
        Tag other = new UnknownTag(new String(tagName));
        assertNotSame(tag.toString(), other.toString());
        assertEquals(tag.toString(), other.toString());
        assertTrue(other.equals(tag));
        assertFalse(other.equals(tagName));
        assertFalse(other.equals(Tag.A));
        assertFalse(tag.equals(Tag.A));

        other = new UnknownTag(Tag.A.toString());
        assertSame(Tag.A.toString(), other.toString());
        assertFalse(other.equals(Tag.A));

        other = new UnknownTag("author");
        assertFalse(tag.equals(other));
    }

    public void testHashCode() {
        Tag other = new UnknownTag(new String(tagName));
        assertNotSame(tag.toString(), other.toString());
        assertEquals(tag.toString(), other.toString());
        assertEquals(tagName.hashCode(), tag.hashCode());
        assertEquals(other.hashCode(), tag.hashCode());

        other = new UnknownTag("author");
        assertFalse(other.hashCode() == tag.hashCode());
    }

    public void testNull() {
        tag = new UnknownTag(null);
        assertNull(tag.toString());

        Tag other = new UnknownTag(null);
        try {
            assertTrue(tag.equals(other));
            fail("NPE expected");
        } catch (NullPointerException e) { }

        try {
            assertEquals(0, tag.hashCode());
            fail("NPE expected");
        } catch (NullPointerException e) { }
    }

    public void testSerializable() throws Exception {
        Tag readTag = (Tag)serializeObject(tag);
        assertTrue(readTag instanceof UnknownTag);
        assertEquals(tagName, readTag.toString());
        assertTrue(tag.equals(readTag));
        assertFalse(readTag.breaksFlow());
        assertFalse(readTag.isBlock());
        assertFalse(readTag.isPreformatted());
    }
}
