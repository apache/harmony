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


import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.standard.*;

import junit.framework.TestCase;

public class HashAttributeTest extends TestCase {


    public static void main(String[] args) {
        junit.textui.TestRunner.run(HashAttributeTest.class);
    }

    static {
        System.out.println("HashAttribute testing...");
    }

    private HashAttributeSet aset;


    /*
     * HashAttributeSet() constructors testing. 
     */
    public final void testHashAttributeSetConstructors() {

        try {
            Attribute att = null;
            AttributeSet aset = new HashAttributeSet(att);
            fail("HashAttributeSet(Attribute attribute) " +
                    "doesn't throw NullPointerException " +
                    "if attribute is null");
        } catch (NullPointerException e) {}

        try {
            Attribute att = null;
            Attribute attrs[] = {att};
            AttributeSet aset = new HashAttributeSet(attrs);
            fail("HashAttributeSet(Attribute[] attributes) " +
                    "doesn't throw NullPointerException " +
                    "if attributes is null");
        } catch (NullPointerException e) {}

        AttributeSet aset1 = null;
        AttributeSet aset2 = new HashAttributeSet(aset1);
    }


    /*
     * add(Attribute attribute) method testing. Tests that after adding 
     * some attribute to specified attribute set this attribute is in 
     * this attribute set.
     */
    public final void testAdd() {

        aset = new HashAttributeSet();
        Attribute att1 = PrintQuality.HIGH;
        boolean flag = aset.add(att1);

        Attribute att2 = aset.get(PrintQuality.class);

        if (flag){
            assertTrue("Results add() & get() is different: att1 = "
                            +att1+", att2 = "+att2,
                                        att1 == att2 );
            assertTrue("Attribute set doesn't contain added attribute value",
                            aset.containsValue(PrintQuality.HIGH));
        }else{
            fail("add() == false");
        }

    }


    /*
     * add(Attribute attribute) method testing. Tests if add some attribute 
     * that is already in current attributeSet than add() returns false.
     */
    public final void testAdd1() {

        aset = new HashAttributeSet();

        Attribute att = new Copies(10);
        aset.add(att);
        assertFalse(aset.add(att));

        att = Sides.DUPLEX;
        aset.add(att);
        assertFalse(aset.add(att));

    }

    /*
     * add(Attribute attribute) method testing. 
     * Tests that if this attribute set does not support the add() operation
     * UnmodifiableSetException is thrown.
     */
    public final void testAdd2() {

        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        if (service != null) {
            PrintServiceAttributeSet psaset = service.getAttributes();
            try{
                psaset.add(ColorSupported.NOT_SUPPORTED);
                fail("add() doesn't throw Exception when" +
                    "trying to add attribute to unmodifiable attribute set ");
            } catch (UnmodifiableSetException e){
            }
        } else {
            System.out.println("Warning: default print service wasn't found!" +
                    "\nPlease check if you have any printers connected to your"
                    + " computer");
        }

        aset = new HashAttributeSet();
        AttributeSet as = AttributeSetUtilities.unmodifiableView(aset);
        try{
            as.add(new Copies(3));
            fail("add() doesn't throw Exception when" +
                    "trying to add attribute to unmodifiable attribute set ");
        } catch (UnmodifiableSetException e) {
        }
    }


    /*
     * addAll(AttributeSet attributeSet) method testing.
     */
    public final void testAddAll() {

        HashAttributeSet aset1 = new HashAttributeSet(new Attribute[]
                                                              {new Copies(10),
                                                               Sides.DUPLEX} );
        HashAttributeSet aset2 = new HashAttributeSet();
        aset2.addAll(aset1);
        assertTrue(aset2.equals(aset1));
    }

    /*
     * addAll(AttributeSet attributeSet) method testing. Test that addAll()  
     * throws NullPointerException if attributeSet is null.
     */
    public final void testAddAll1() {

        HashAttributeSet aset1 = new HashAttributeSet();
        HashAttributeSet aset2 = null;
        try {
            aset.addAll(aset2);
            fail("addAll() doesn't throw NullPointerException when" +
                    "trying to add null attribute set");
        } catch (NullPointerException e) {

        }
    }

    /*
     * addAll(AttributeSet attributeSet) method testing. 
     * 
     */
    public final void testAddAll2() {

        HashPrintServiceAttributeSet aset1 = new HashPrintServiceAttributeSet();
        HashAttributeSet aset2 = new HashAttributeSet();
        aset2.add(new JobName("jobName", null));
        try {
            aset1.addAll(aset2);
            fail("addAll() doesn't throw ClassCastException" +
                    "when specification req");
        } catch (ClassCastException e) {

        }
    }

    /*
     * clear() method testing. 
     * Test that clear() remove all values from this attribute set.
     */
    public final void testClear() {

        aset = new HashAttributeSet();
        aset.add(SheetCollate.COLLATED);
        if (aset.get(SheetCollate.class) != null) {
            aset.clear();
            assertNull(aset.get(SheetCollate.class));
        } else {
            fail("add() or get() works wrong");
        }

        aset.add(SheetCollate.COLLATED);
        aset.add(ColorSupported.SUPPORTED);
        if ( !aset.isEmpty() ) {
            aset.clear();
            assertNull(aset.get(SheetCollate.class));
            assertNull(aset.get(ColorSupported.class));
        } else {
            fail("add() or isEmpty() works wrong");
        }

    }


    /*
     * containsKey(class attributeCategory) method testing. Tests if current
     * attribute set contains value for given attribute category than 
     * containsKey() returns true.
     */
    public final void testContainsKey() {

        aset = new HashAttributeSet();
        aset.add(SheetCollate.COLLATED);
        assertTrue(aset.containsKey(SheetCollate.class));
    }


    /*
     * containsKey(Attribute attribute) method testing. Tests if current
     * attribute set don't contain value for given attribute category than
     * containsKey() returns false.
     */
    public final void testContainsKey1() {

        aset = new HashAttributeSet();
        assertFalse(aset.containsKey(SheetCollate.class));
    }


    /*
     * containsValue(Attribute attribute) method testing. Tests if current
     * attribute set doesn't contain value for given attribute for it's 
     * attribute than containsKey() returns false.
     */
    public final void testContainsValue() {

        aset = new HashAttributeSet();
        aset.add(SheetCollate.UNCOLLATED);
        assertFalse(aset.containsValue(SheetCollate.COLLATED));
    }

    /*
     * containsValue(Attribute attribute) method testing. Test if input 
     * parameter is null than containsValue() returns false.
     */
    public final void testContainsValue1() {

        aset = new HashAttributeSet();
        assertFalse(aset.containsValue(null));
    }


    /*
     * equals(Object object) method testing.
     */
    public final void testEquals() {

        aset = new HashAttributeSet();
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        if (service != null) {
            PrintServiceAttributeSet psaset = service.getAttributes();
            if (aset.addAll(psaset)){
                assertTrue(aset.equals(psaset));
            }
        } else {
            System.out.println("Warning: default print service wasn't found!\n"
                    + "Please check if you have any printers connected to your "
                    + "computer.");
        }

        JobPriority priority = new JobPriority(2);
        PrintRequestAttributeSet set1 = new HashPrintRequestAttributeSet();
        set1.add(priority);
        set1.add(MediaName.ISO_A4_TRANSPARENT);
        PrintRequestAttribute[] arr = { priority, MediaName.ISO_A4_TRANSPARENT };
        PrintRequestAttributeSet set2 = new HashPrintRequestAttributeSet(arr);
        assertTrue(set2.equals(set1));

        HashPrintRequestAttributeSet paset1 = new HashPrintRequestAttributeSet(
                new PrintRequestAttribute[] {new Copies(3),
                        new JobName("JobName", null), MediaSizeName.A});
        HashPrintRequestAttributeSet paset2 = new HashPrintRequestAttributeSet();
        paset2.add(new JobName("JobName", null));
        paset2.add(MediaSizeName.A);
        assertFalse(paset1.equals(paset2));
        paset2.add( new Copies(3));
        assertTrue(paset1.equals(paset2));

        aset = new HashAttributeSet();
        HashAttributeSet nullAset = null;
        assertFalse(aset.equals(nullAset));
    }

    /*
     * equals(Object object) method testing. Tests if given object is not
     * an attribute set equals() return false.
     */
    public final void testEquals1() {

        aset = new HashAttributeSet();
        Attribute att = new Copies(1);
        assertFalse("equals return true when given object is " +
                    "not an attribute set", aset.equals(att));
    }

    /*
     * get(Class attributeCategory) method testing. Tests if given object is not
     * a class that implements interface Attribute than get() throw 
     * ClassCastException.
     */
    public final void testGet1() {

        aset = new HashAttributeSet();
        try {
            aset.get(SimpleDoc.class);
            fail("get() doesn't throw ClassCastException when given object" +
                    "is not a Class that implements interface Attribute");
        } catch (ClassCastException e) {

        }
    }

    /*
     * hashCode() method testing. Tests if two object is the same they must have
     * same hashCode.
     */
    public final void testHashCode() {

        HashAttributeSet aset1 = new HashAttributeSet();
        aset1.add(new Copies(5));
        HashAttributeSet aset2 = new HashAttributeSet();
        aset2.add(new Copies(5));
        assertEquals("Two equals object have different hash codes",
                        aset1.hashCode(), aset2.hashCode());
    }

    /*
     * isEmpty() method testing. Tests if given attribute set is empty 
     * than isempty return true.
     */
    public final void testIsEmpty() {

        aset = new HashAttributeSet();
        assertTrue(aset.isEmpty());
    }

    /*
     * isEmpty() method testing. Tests if given attribute set isn't empty 
     * than isempty return false.
     */
    public final void testIsEmpty1() {

        aset = new HashAttributeSet();
        aset.add(new Copies(2));
        assertFalse(aset.isEmpty());
    }

    /*
     * remove(Attribute attribute) method testing. Tests that if given attribute
     * wasn't in attribute set than remove(Attribute attribute) returns false.
     */
    public final void testRemove() {

        aset = new HashAttributeSet();
        aset.add(new Copies(2));
        assertFalse(aset.remove(Sides.DUPLEX));
    }


    /*
     * remove(Class attributeCategory) method testing. 
     * Tests that remove(Class attributeCategory)
     * is really remove attribute for given attribute category.
     */
    public final void testRemove1() {

        aset = new HashAttributeSet();
        aset.add(Sides.DUPLEX);
        assertTrue(aset.remove(Sides.class));
        assertNull(aset.get(Sides.class));
    }

    /*
     * size() method testing. Tests that size return right values
     * when attribute set contains no or one attribute value.
     */
    public final void testSize() {

        aset = new HashAttributeSet();
        assertEquals("size() return not 0 for the empty attribute set",
                                0, aset.size());
        aset.add(new Copies(5));
        assertEquals(1, aset.size());
    }


    /*
     * toArray() method testing. Tests that toArray() returns 
     * array of attribute with right attribute value.
     */
    public final void testToArray() {

        aset = new HashAttributeSet();
        aset.add(PrintQuality.HIGH);
        Attribute[] arr = aset.toArray();
        assertEquals(PrintQuality.HIGH, arr[0]);
    }

    /*
     * toArray() method testing. Tests if given attribute set is empty toArray() 
     * returns zero length array.
     */
    public final void testToArray1() {

        aset = new HashAttributeSet();
        assertEquals(0, (aset.toArray()).length);
    }


}
