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

import javax.swing.text.StyleConstants;

public class StyleSheet_ConvertAttr_TextIndentTest
    extends StyleSheet_ConvertAttr_MarginTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        scKey = StyleConstants.FirstLineIndent;
        cssKey = CSS.Attribute.TEXT_INDENT;
    }

    // Overrides the super class test-case with same name.
    // The last assertion is modified (that's the only difference)
    public void testLengthMinus11_1pt() {
        ss.addCSSAttribute(simple, cssKey, "-11.1pt");
        attr = ss.createSmallAttributeSet(simple);

        cssValue = attr.getAttribute(cssKey);
        scValue = attr.getAttribute(scKey);
        assertEquals("-11.1pt", cssValue.toString());
        assertEquals(-11.1f, ((Float)scValue).floatValue(), 0f);
    }
}
