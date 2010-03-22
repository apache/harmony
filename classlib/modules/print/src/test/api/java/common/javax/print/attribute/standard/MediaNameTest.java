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

public class MediaNameTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MediaNameTest.class);
    }

    static {
        System.out.println("MediaName testing...");
    }

    mediaName name;

    /*
     * MediaName constructor testing.
     */
    public final void testMediaName() {
        name = new mediaName(11);
        assertEquals(11, name.getValue());
        assertEquals("11", name.toString());

        name = new mediaName(1111);
        assertEquals(1111, name.getValue());
        assertEquals("1111", name.toString());
    }


    /*
     * getStringTable() method testing.
     */
    public void testGetStringTable() {
        int quantity = 0;
        mediaName mn = new mediaName(1);
        String[] str = mn.getStringTableEx();
        String[] str1 = { "na-letter-white",
                          "na-letter-transparent",
                          "iso-a4-white",
                          "iso-a4-transparent"};
        for (int j=0; j < str.length; j++) {
            quantity++;
            assertEquals(str1[j], str[j]);
        }
        assertEquals(4, quantity);
    }

    /*
     * getEnumValueTable() method testing.
     */
    public final void testGetEnumValueTable() {
        int quantity = 0;
        mediaName mn = new mediaName(1);
        EnumSyntax[] table = mn.getEnumValueTableEx();
        assertEquals(4, table.length);
    }

    /*
     * Checks that enumTable and stringTable are immutable for
     * MediaName class
     */
    public final void testGetEnumValueTable1() {
        name = new mediaName(1);
        String[] str = name.getStringTableEx();
        EnumSyntax[] table = name.getEnumValueTableEx();
        str[1] = "media1";
        table[1] = new mediaName(10);
        //System.out.println(name.getEnumValueTable()[1]);
        assertFalse(name.getEnumValueTableEx()[1].equals("media"));
        assertFalse(name.getEnumValueTableEx()[1].equals(table[1]));
    }


    /*
     * Auxiliary class
     */
    public class mediaName extends MediaName {

        public mediaName(int value) {
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
