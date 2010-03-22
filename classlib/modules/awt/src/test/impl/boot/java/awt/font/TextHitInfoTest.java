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
/*
 * @author Oleg V. Khaschansky
 */

package java.awt.font;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

public class TextHitInfoTest extends TestCase
{

    private TextHitInfo l4, t3, tm1;

    public TextHitInfoTest(String name)
    {
        super(name);
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        l4 = TextHitInfo.leading(4);
        t3 = TextHitInfo.trailing(3);
        tm1 = TextHitInfo.trailing(-1);
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testToString() throws Exception
    {
        l4.toString();
        tm1.toString();
    }

    public void testEquals() throws Exception
    {
        assertEquals(t3, TextHitInfo.trailing(3));
    }

    public void testEquals1() throws Exception
    {
        assertNotSame(t3, new String("q"));
    }

    public void testGetOffsetHit() throws Exception
    {
        TextHitInfo i1 = t3.getOffsetHit(2);
        assertEquals(TextHitInfo.trailing(5), i1);
        TextHitInfo i2 = l4.getOffsetHit(-2);
        assertEquals(TextHitInfo.leading(2), i2);
        TextHitInfo i3 = tm1.getOffsetHit(-1);
        assertEquals(TextHitInfo.trailing(-2), i3);
    }

    public void testGetOtherHit() throws Exception
    {
        TextHitInfo i1 = t3.getOtherHit();
        assertEquals(l4, i1);

        TextHitInfo i2 = l4.getOtherHit();
        assertEquals(t3, i2);
    }

    public void testIsLeadingEdge() throws Exception
    {
        assertFalse(t3.isLeadingEdge());
        assertTrue(l4.isLeadingEdge());
    }

    public void testHashCode() throws Exception
    {
        assertEquals(TextHitInfo.trailing(3).hashCode(), t3.hashCode());
        assertTrue(t3.hashCode() != l4.hashCode());
        assertTrue(TextHitInfo.leading(3).hashCode() != t3.hashCode());
    }

    public void testGetInsertionIndex() throws Exception
    {
        assertEquals(4, t3.getInsertionIndex());
        assertEquals(4, l4.getInsertionIndex());
    }

    public void testGetCharIndex() throws Exception
    {
        assertEquals(3, t3.getCharIndex());
        assertEquals(4, l4.getCharIndex());
    }

    public void testTrailing() throws Exception
    {
        assertEquals(4, TextHitInfo.trailing(4).getCharIndex());
        assertFalse(TextHitInfo.trailing(-1).isLeadingEdge());
    }

    public void testLeading() throws Exception
    {
        assertEquals(4, TextHitInfo.leading(4).getCharIndex());
        assertTrue(TextHitInfo.leading(1).isLeadingEdge());
    }

    public void testBeforeOffset() throws Exception
    {
        assertEquals(t3, TextHitInfo.beforeOffset(4));
    }

    public void testAfterOffset() throws Exception
    {
        assertEquals(l4, TextHitInfo.afterOffset(4));
    }

    public static Test suite()
    {
        return new TestSuite(TextHitInfoTest.class);
    }
}
