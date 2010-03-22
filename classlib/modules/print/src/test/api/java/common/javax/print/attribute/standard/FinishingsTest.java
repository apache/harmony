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

public class FinishingsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FinishingsTest.class);
    }

    static {
        System.out.println("Finishings testing...");
    }

    finishings fin;


    /*
     * Finishings constructor testing.
     */
    public final void testFinishings() {

        Finishings finishings = new finishings(20);
        assertEquals(20, finishings.getValue());

        fin = new finishings(40);
        assertEquals(40, fin.getValue());

        finishings = Finishings.BIND;
        assertEquals("bind", finishings.toString());
        assertTrue(finishings.getValue() < 18);
        assertTrue(finishings.getValue() > 0);

    }


    /*
     * getCategory() method testing.
     */
    public final void testGetCategory() {

        Finishings finishings = Finishings.NONE;
        assertEquals(Finishings.class, finishings.getCategory());
    }

    /*
     * getName() method testing.
     */
    public final void testGetName() {
        Finishings finishings = Finishings.SADDLE_STITCH;
        assertEquals("finishings", finishings.getName());
    }

    /*
     * getStringTable() method testing.
     */
    public final void testGetStringTable() {

        int quantity = 0;
        fin = new finishings(20);
        String[] str = fin.getStringTableEx();
        for (int i = 0; i < str.length; i++) {
            if (str[i] != null) {
                quantity++;
            }
            //System.out.println(str[i]);
        }
        assertEquals(18, quantity);

        //Tests that StringTable isn't changed for Finishings
        fin = new finishings(3);
        str[3] = "finishings3";
        //System.out.println((fin.getStringTableEx()[3]);
        assertFalse(fin.getStringTableEx()[3].equals("finishings3"));

        Finishings finishings = Finishings.STAPLE_TOP_LEFT;
        assertEquals("staple-top-left", finishings.toString());
    }

    /*
     * getEnumValueTable() method testing.
     */
    public final void testGetEnumValueTable() {
        int quantity = 0;
        fin = new finishings(20);
        EnumSyntax[] table = fin.getEnumValueTableEx();
        for (int i = 0; i < table.length; i++) {
            if (table[i] != null) {
                quantity++;
            }
        }
        assertEquals(18, quantity);
    }

    /*
     * getOffset() method testing.
     */
    public final void testGetOffset() {
        fin = new finishings(5);
        assertEquals(3, fin.getOffsetEx());
    }


    /*
     * Auxiliary class
     */
    public class finishings extends Finishings {

        public finishings(int value) {
            super(value);
        }

        public String[] getStringTableEx() {
            return getStringTable();
        }

        public EnumSyntax[] getEnumValueTableEx() {
            return getEnumValueTable();
        }

        public int getOffsetEx() {
            return getOffset();
        }
    }



}
