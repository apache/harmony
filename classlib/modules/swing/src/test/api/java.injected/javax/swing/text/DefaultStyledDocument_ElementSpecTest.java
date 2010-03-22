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

import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.TestCase;

public class DefaultStyledDocument_ElementSpecTest extends TestCase {
    /**
     * ElementSpec created with ElementSpec(AttributeSet, short) constructor.
     */
    private ElementSpec spec1;

    /**
     * ElementSpec created with ElementSpec(AttributeSet, short, int)
     * constructor.
     */
    private ElementSpec spec2;

    /**
     * ElementSpec created with
     * ElementSpec(AttributeSet, short, char[], int, int) constructor.
     */
    private ElementSpec spec3;

    private static final AttributeSet attrs1;

    private static final AttributeSet attrs2;

    private static final AttributeSet attrs3;

    private char[] text;
    static {
        MutableAttributeSet mas;
        mas = new SimpleAttributeSet();
        StyleConstants.setBold(mas, true);
        attrs1 = mas.copyAttributes();
        mas = new SimpleAttributeSet();
        StyleConstants.setItalic(mas, true);
        attrs2 = mas.copyAttributes();
        mas = new SimpleAttributeSet(attrs1);
        mas.addAttributes(attrs2);
        attrs3 = mas.copyAttributes();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        spec1 = new ElementSpec(attrs1, ElementSpec.StartTagType);
        spec2 = new ElementSpec(attrs2, ElementSpec.ContentType, 10);
        spec3 = new ElementSpec(attrs3, ElementSpec.EndTagType, text = "sample text"
                .toCharArray(), 2, 4);
    }

    /*
     * Constructors are tested in all the getters. The only side effect of
     * constructor is setting fields which are accessed through getters.
     *
     * ElementSpec(AttributeSet, short)
     * ElementSpec(AttributeSet, short, char[], int, int)
     * ElementSpec(AttributeSet, short, int)
     */
    /*
     public void testElementSpecAttributeSetshort() {
     }
     public void testElementSpecAttributeSetshortcharArrayintint() {
     }
     public void testElementSpecAttributeSetshortint() {
     }
     */
    public void testGetArray() {
        assertNull(spec1.getArray());
        assertNull(spec2.getArray());
        assertSame(text, spec3.getArray());
    }

    public void testGetAttributes() {
        assertSame(attrs1, spec1.getAttributes());
        assertSame(attrs2, spec2.getAttributes());
        assertSame(attrs3, spec3.getAttributes());
    }

    public void testGetDirection() {
        assertEquals(ElementSpec.OriginateDirection, spec1.getDirection());
        assertEquals(ElementSpec.OriginateDirection, spec2.getDirection());
        assertEquals(ElementSpec.OriginateDirection, spec3.getDirection());
    }

    public void testGetLength() {
        assertEquals(0, spec1.getLength());
        assertEquals(10, spec2.getLength());
        assertEquals(4, spec3.getLength());
    }

    public void testGetOffset() {
        assertEquals(0, spec1.getOffset());
        assertEquals(0, spec2.getOffset());
        assertEquals(2, spec3.getOffset());
    }

    public void testGetType() {
        assertEquals(ElementSpec.StartTagType, spec1.getType());
        assertEquals(ElementSpec.ContentType, spec2.getType());
        assertEquals(ElementSpec.EndTagType, spec3.getType());
    }

    public void testSetDirection() {
        assertEquals(ElementSpec.OriginateDirection, spec1.getDirection());
        spec1.setDirection(ElementSpec.JoinNextDirection);
        assertEquals(ElementSpec.JoinNextDirection, spec1.getDirection());
    }

    public void testSetType() {
        assertEquals(ElementSpec.StartTagType, spec1.getType());
        spec1.setType(ElementSpec.EndTagType);
        assertEquals(ElementSpec.EndTagType, spec1.getType());
    }

    /*
     * String toString()
     */
    public void testToString() {
        assertEquals("StartTag:Originate:0", spec1.toString());
        assertEquals("Content:Originate:10", spec2.toString());
        assertEquals("EndTag:Originate:4", spec3.toString());
    }

    /**
     * Checks how directions represented in string.
     */
    public void testToStringDirection() {
        final short[] direction = new short[] { ElementSpec.OriginateDirection,
                ElementSpec.JoinFractureDirection, ElementSpec.JoinNextDirection,
                ElementSpec.JoinPreviousDirection };
        final String[] text = new String[] { "Originate", "Fracture", "JoinNext",
                "JoinPrevious" };
        for (int i = 0; i < direction.length; i++) {
            spec1.setDirection(direction[i]);
            assertEquals("@ " + i, text[i], spec1.toString().split(":")[1]);
        }
    }

    public void testToStringIvalidDirection() {
        spec1.setDirection((short) 25);
        assertEquals((short) 25, spec1.getDirection());
        assertEquals("StartTag:??:0", spec1.toString());
    }

    public void testToStringInvalidType() throws Exception {
        spec1.setType((short) 25);
        assertEquals((short) 25, spec1.getType());
        assertEquals("??:Originate:0", spec1.toString());
    }
}
