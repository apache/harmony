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

import java.util.Enumeration;
import javax.swing.SerializableTestCase;

/**
 * Tests serialization of StyleContext.
 *
 */
public class StyleContext_SerializableTest extends SerializableTestCase {
    private StyleContext sc;

    @Override
    public void testSerializable() throws Exception {
        StyleContext loadedSC = (StyleContext) toLoad;
        Enumeration<?> names = loadedSC.getStyleNames();
        String name;
        assertTrue(names.hasMoreElements());
        while (names.hasMoreElements()) {
            name = (String) names.nextElement();
            Style original = sc.getStyle(name);
            Style loaded = loadedSC.getStyle(name);
            assertTrue("Style name: '" + name + "' #original: " + original + "  #loaded: "
                    + loaded, original.isEqual(loaded));
        }
    }

    @Override
    protected void setUp() throws Exception {
        sc = new StyleContext();
        Style style = sc.addStyle("aStyle", null);
        style.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        style.addAttribute(StyleConstants.FontFamily, new String("Arial"));
        style = sc.getStyle(StyleContext.DEFAULT_STYLE);
        style.addAttribute(StyleConstants.FontSize, new Integer(24));
        style.addAttribute(new String("attrKey"), new String("attrValue"));
        style.addAttribute(new Integer(15), new Float(1.07));
        toSave = sc;
        super.setUp();
    }
}
