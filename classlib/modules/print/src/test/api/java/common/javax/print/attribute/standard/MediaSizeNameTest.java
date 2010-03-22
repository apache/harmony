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
 * @author Elena V. Sayapina 
 */ 
 
package javax.print.attribute.standard;

import javax.print.attribute.EnumSyntax;

import junit.framework.TestCase;

public class MediaSizeNameTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MediaSizeNameTest.class);
    }

    static {
        System.out.println("MediaSizeName testing...");
    }

    /*
     * MediaSizeName constructor testing.
     */
    public final void testMediaName() {
        MediaSizeName msn = new mediaSizeName(0);
        assertEquals(0, msn.getValue());
        assertEquals("iso-a0", msn.toString());

        msn = mediaSizeName.A;
        assertEquals("a", msn.toString());

        msn = mediaSizeName.FOLIO;
        assertEquals("folio", msn.toString());
    }


    /*
     * getStringTable(), getEnumValueTable() method testing.
     */
    public void testGetStringTable() {
        mediaSizeName msn = new mediaSizeName(1);
        String[] str = msn.getStringTableEx();
        EnumSyntax[] table = msn.getEnumValueTableEx();
        assertEquals(str.length, table.length);
        assertEquals(73, str.length);

        //Tests that StringTable isn't changed for MediaSizeName class
        msn = new mediaSizeName(1);
        str = msn.getStringTableEx();
        str[1] = "MediaSizeName1";
        //System.out.println(msn.getStringTable()[1]);
        assertFalse(msn.getStringTableEx()[1].equals("MediaSizeName1"));
    }


    /*
     * Auxiliary class
     */
    public class mediaSizeName extends MediaSizeName {

        public mediaSizeName(int value) {
            super(value);
        }

        public String[] getStringTableEx() {
            return getStringTable();
        }

        public EnumSyntax[] getEnumValueTableEx() {
            return getEnumValueTable();
        }
    }


}
