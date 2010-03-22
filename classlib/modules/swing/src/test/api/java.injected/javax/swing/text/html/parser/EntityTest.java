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

public class EntityTest extends TestCase {
    public void testEntity() {
        String name  = "first";
        int type = 26;
        String data = "firstData";
        Entity entity = new Entity(name, type, data.toCharArray());
        Utils.checkEntity(entity, name, type, data, false, false);

        name = "second";
        type = 106;
        data = "secondData";
        entity = new Entity(name, type, data.toCharArray());
        Utils.checkEntity(entity, name, type, data, false, false);

        name = null;
        type = 0;
        data = "";
        entity = new Entity(name, type, data.toCharArray());
        Utils.checkEntity(entity, name, type, data, false, false);
    }

    //TODO Investigate how is it defined.
    public void testIsGeneral() throws Exception{
        Entity entity2 = new Entity("name", DTDConstants.GENERAL, new char[0]); //$NON-NLS-1$
        assertTrue(entity2.isGeneral());
        
        entity2 = new Entity("name", DTDConstants.GENERAL | DTDConstants.CDATA, new char[0]); //$NON-NLS-1$
        assertTrue(entity2.isGeneral());
        
        entity2 = new Entity("name", DTDConstants.CDATA, new char[0]); //$NON-NLS-1$
        assertFalse(entity2.isGeneral());
    }

    //TODO Investigate how is it defined.
    public void testIsParameter() throws Exception{
        //regression for HARMONY-1349
        Entity entity2 = new Entity("name", DTDConstants.PARAMETER, new char[0]); //$NON-NLS-1$
        assertTrue(entity2.isParameter());
        
        entity2 = new Entity("name", DTDConstants.PARAMETER | DTDConstants.CDATA, new char[0]); //$NON-NLS-1$
        assertTrue(entity2.isParameter());
        
        entity2 = new Entity("name", DTDConstants.CDATA, new char[0]); //$NON-NLS-1$
        assertFalse(entity2.isParameter());
    }

    public void testName2type() {
        String[] names = Utils.getDTDConstantsNames();
        for (int i = 0; i < names.length; i++) {
            String key = names[i];
            int value = Entity.name2type(key);
            if (key.equals("PUBLIC")) {
                assertEquals(DTDConstants.PUBLIC, value);
            } else if (key.equals("SDATA")) {
                assertEquals(DTDConstants.SDATA, value);
            } else if (key.equals("PI")) {
                assertEquals(DTDConstants.PI, value);
            } else if (key.equals("STARTTAG")) {
                assertEquals(DTDConstants.STARTTAG, value);
            } else if (key.equals("ENDTAG")) {
                assertEquals(DTDConstants.ENDTAG, value);
            } else if (key.equals("MS")) {
                assertEquals(DTDConstants.MS, value);
            } else if (key.equals("MD")) {
                assertEquals(DTDConstants.MD, value);
            } else if (key.equals("SYSTEM")) {
                assertEquals(DTDConstants.SYSTEM, value);
            } else {
                assertEquals(DTDConstants.CDATA, value);
            }
        }
    }
    
    /**
     * @test javax.swing.text.html.parser.Entity#getType()
     */
    public void testType() throws Exception{
        //regression for HARMONY-1349
        DTD dtd = DTD.getDTD("dummy"); //$NON-NLS-1$
        Entity space = dtd.getEntity("#SPACE"); //$NON-NLS-1$
        assertEquals(65536, space.type);
        assertEquals(0, space.getType()); 
    }
}
