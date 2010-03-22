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

import junit.framework.TestCase;

/**
 * Tests for getStaticAttribute, getStaticAttributeKey, readAttributes,
 * readAttributeSet, registerStaticAttributeKey, writeAttributes,
 * writeAttributeSet methods.
 *
 */
public class StyleContext_StaticAttrTest extends TestCase {
    /**
     * Object that is used with registerStaticAttributeKey.
     */
    private static class SampleAttribute {
        String name = "sampleAttribute";

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof SampleAttribute) {
                return name == ((SampleAttribute) obj).name;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Array of objects that are registered as static attribute keys.
     */
    private static final Object keys[] = { StyleConstants.Bold, StyleConstants.Italic,
            StyleConstants.Background, StyleConstants.Underline, StyleConstants.Alignment };

    /**
     * Array of strings that static attribute keys get registered with.
     */
    private static final Object strings[] = {
            "javax.swing.text.StyleConstants$FontConstants.bold",
            "javax.swing.text.StyleConstants$FontConstants.italic",
            "javax.swing.text.StyleConstants$ColorConstants.background",
            "javax.swing.text.StyleConstants$CharacterConstants.underline",
            "javax.swing.text.StyleConstants$ParagraphConstants.Alignment" };

    public void testGetStaticAttribute() {
        for (int i = 0; i < keys.length; i++) {
            assertSame("Iteration: " + i, keys[i], StyleContext.getStaticAttribute(strings[i]));
        }
    }

    public void testGetStaticAttributeKey() {
        for (int i = 0; i < keys.length; i++) {
            assertEquals("Iteration: " + i, strings[i], StyleContext
                    .getStaticAttributeKey(keys[i]));
        }
    }

    public void testRegisterStaticAttributeKey() {
        SampleAttribute key = new SampleAttribute();
        String str = (String) StyleContext.getStaticAttributeKey(key);
        // We haven't registered our object yet, so result should be null
        assertNull(StyleContext.getStaticAttribute(str));
        StyleContext.registerStaticAttributeKey(key);
        // The method should return the same object with which we called
        // register... method
        assertSame(key, StyleContext.getStaticAttribute(str));
    }
    /*
     The commented out methods are implicitly tested
     with StyleContext_SerializableTest
     public void testReadAttributes() {
     // implementation simply calls testReadAttributeSet
     }

     public void testWriteAttributes() {
     // implementation simply calls testWriteAttributeSet
     }

     public void testReadAttributeSet() {
     }

     public void testWriteAttributeSet() {
     }*/
}