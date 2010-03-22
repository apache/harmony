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

package javax.print.attribute;


import javax.print.Doc;
import javax.print.SimpleDoc;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Compression;
import javax.print.attribute.standard.Copies;

import junit.framework.TestCase;


public class AttributeSetUtilitiesTest extends TestCase {


    public static void main(String[] args) {
        junit.textui.TestRunner.run(AttributeSetUtilitiesTest.class);
    }

    static {
        System.out.println("AttributeSetUtilities testing...");
    }

    /*
     * verifyAttributeCategory(Object object, Class interfaceName) method testing. 
     * Tests that method returns the same Class and no exception is thown 
     * if arguments are valid.
     */
    public final void testVerifyAttributeCategory() {
        assertEquals(ColorSupported.class,
            AttributeSetUtilities.
                verifyAttributeCategory(ColorSupported.class,
                    PrintServiceAttribute.class));
    }

    /*
     * verifyAttributeCategory(Object object, Class interfaceName) method testing. 
     * Tests that method throws ClassCastException if object is not 
     * a Class that implements interfaceName.
     */
    public final void testVerifyAttributeCategory1() {

        try {
            AttributeSetUtilities.
                verifyAttributeCategory(ColorSupported.class, DocAttribute.class);
                fail("method doesn't throw ClassCastException " +
                        "if object is not a Class that implements interfaceName");
        } catch (ClassCastException e) {
            //System.out.println("testVerifyAttributeCategory1 - " + e.toString());
        }

    }

    /*
     * verifyAttributeCategory(Object object, Class interfaceName) method testing. 
     * Tests that method throws NullPointerException if object is null.
     */
    public final void testVerifyAttributeCategory2() {

        try {
            Attribute att = null;
            AttributeSetUtilities.
                verifyAttributeCategory(att, DocAttribute.class);
            fail("method doesn't throw NullPointerException if object is null");
        } catch (NullPointerException e) {
            //System.out.println("testVerifyAttributeCategory2 - " + e.toString());
        }

    }

    /*
     * verifyAttributeCategory(Object object, Class interfaceName) method testing. 
     * Tests that method throws exception if object is a Class that implements 
     * interfaceName but interfaceName is not a class that implements interface
     * Attribute.
     */
    public final void testVerifyAttributeCategory3() {

        //fails in "some" invironment
        //see AttributeSetUtilities, line 337-339

        try {
            AttributeSetUtilities.
                verifyAttributeCategory(SimpleDoc.class, Doc.class);
            fail("method doesn't throw ClassCastException if object " +
                    "is a Class that implements interfaceName but " +
                        "interfaceName is not a class that implements " +
                            "interface Attribute");
        } catch (ClassCastException e) {
            //System.out.println("testVerifyAttributeCategory3 - " + e.toString());
        }

    }


    /*
     * verifyAttributeValue(Object attribute, Class interfaceName) method testing. 
     * Tests that method returns object downcasted to type Attribute and no
     * exception is thown if arguments are valid.
     */
    public final void testVerifyAttributeValue() {
        PrintJobAttribute att = new Copies(10);
        assertEquals(att,
            AttributeSetUtilities.
                verifyAttributeValue(att, PrintJobAttribute.class));
        assertEquals(att,
                AttributeSetUtilities.
                    verifyAttributeValue(att, PrintRequestAttribute.class));
    }

    /*
     * verifyAttributeValue(Object attribute, Class interfaceName) method testing. 
     * Tests that method throws ClassCastException if object isn't an instance
     * of interfaceName.
     */
    public final void testVerifyAttributeValue1() {

        try {
            DocAttribute att = Compression.COMPRESS;
            AttributeSetUtilities.
                    verifyAttributeValue(att, PrintJobAttribute.class);

        } catch (ClassCastException e) {
            //System.out.println("testVerifyAttributeValue1 - " + e.toString());
        }
    }

    /*
     * verifyAttributeValue(Object attribute, Class interfaceName) method testing. 
     * Tests that method throws NullPointerException if object is null.
     */
    public final void testVerifyAttributeValue2() {

        try {
            AttributeSetUtilities.verifyAttributeValue(null, DocAttribute.class);
            fail("method doesn't throw NullPointerException if object is null");
        } catch (NullPointerException e) {
            //System.out.println("testVerifyAttributeValue2 - " + e.toString());
        }
    }


    /*
     * verifyCategoryForValue(Class attributeCategory, Attribute attribute) method testing. 
     * Tests that method throws no exception if arguments are valid.
     */
    public final void testVerifyCategoryForValue() {
        PrintServiceAttribute att = ColorSupported.NOT_SUPPORTED;
        AttributeSetUtilities.
            verifyCategoryForValue(ColorSupported.class, att);
    }

    /*
     * verifyCategoryForValue(Class attributeCategory, Attribute attribute) method testing. 
     * Tests that method throws IllegalArgumentException if the category is 
     * not equal to the category of the attribute.
     */
    public final void testVerifyCategoryForValue1() {

        try {
            PrintServiceAttribute att = ColorSupported.NOT_SUPPORTED;
            AttributeSetUtilities.
                verifyCategoryForValue(DocAttribute.class, att);
            fail("method doesn't thrown IllegalArgumentException if the category " +
                    "is not equal to the category of the attribute.");
        } catch (IllegalArgumentException e) {
            //System.out.println("testVerifyCategoryForValue1 - " + e.toString());
        }
    }

    /*
     * verifyCategoryForValue(Class attributeCategory, Attribute attribute) method testing. 
     * Tests that method throws NullPointerException if the attribute is null.
     */
    public final void testVerifyCategoryForValue2() {

        try {
            AttributeSetUtilities.verifyCategoryForValue(DocAttribute.class, null);
            fail("method doesn't throw NullPointerException if object is null");
        } catch (NullPointerException e) {
            //System.out.println("testVerifyCategoryForValue2 - " + e.toString());
        }
    }


    /*
     * unmodifiableView(AttributeSet attributeSet) methods testing. 
     * Tests that methods provides a client "read-only" access to the created 
     * unmodifiable view of some attribute set.
     */
    public final void testUnmodifiableView() {

        AttributeSet aset = new HashAttributeSet();
        aset = AttributeSetUtilities.unmodifiableView(aset);
        try {
            aset.add(ColorSupported.SUPPORTED);
            aset.addAll(aset);
            aset.clear();
            aset.remove(ColorSupported.NOT_SUPPORTED);
            aset.remove(ColorSupported.class);
            fail("method doesn't throw UnmodifiableSetException if someone" +
                    "try to modify attribute set");
        } catch (Exception e) {
            //System.out.println("testUnmodifiableView - " + e.toString());
        }
    }


    /*
     * synchronizedView(AttributeSet attributeSet) methods testing. 
     */
    public final void testSynchronizedView() {
        //To-Do
    }

}
