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

import junit.framework.TestCase;

public class DTDConstantsTest extends TestCase {
    public void testConstants() {
        assertEquals(19, DTDConstants.ANY);
        assertEquals(1, DTDConstants.CDATA);
        assertEquals(4, DTDConstants.CONREF);
        assertEquals(3, DTDConstants.CURRENT);
        assertEquals(131072, DTDConstants.DEFAULT);
        assertEquals(17, DTDConstants.EMPTY);
        assertEquals(14, DTDConstants.ENDTAG);
        assertEquals(3, DTDConstants.ENTITIES);
        assertEquals(2, DTDConstants.ENTITY);
        assertEquals(1, DTDConstants.FIXED);
        assertEquals(65536, DTDConstants.GENERAL);
        assertEquals(4, DTDConstants.ID);
        assertEquals(5, DTDConstants.IDREF);
        assertEquals(6, DTDConstants.IDREFS);
        assertEquals(5, DTDConstants.IMPLIED);
        assertEquals(16, DTDConstants.MD);
        assertEquals(18, DTDConstants.MODEL);
        assertEquals(15, DTDConstants.MS);
        assertEquals(7, DTDConstants.NAME);
        assertEquals(8, DTDConstants.NAMES);
        assertEquals(9, DTDConstants.NMTOKEN);
        assertEquals(10, DTDConstants.NMTOKENS);
        assertEquals(11, DTDConstants.NOTATION);
        assertEquals(12, DTDConstants.NUMBER);
        assertEquals(13, DTDConstants.NUMBERS);
        assertEquals(14, DTDConstants.NUTOKEN);
        assertEquals(15, DTDConstants.NUTOKENS);
        assertEquals(262144, DTDConstants.PARAMETER);
        assertEquals(12, DTDConstants.PI);
        assertEquals(10, DTDConstants.PUBLIC);
        assertEquals(16, DTDConstants.RCDATA);
        assertEquals(2, DTDConstants.REQUIRED);
        assertEquals(11, DTDConstants.SDATA);
        assertEquals(13, DTDConstants.STARTTAG);
        assertEquals(17, DTDConstants.SYSTEM);
    }
}
