/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.beans;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;

import junit.framework.TestCase;

/**
 * Test the internal class java.beans.ArrayPersistenceDelegate.
 */
public class ArrayPersistenceDelegateTest extends TestCase {

    private ArrayPersistenceDelegate pd = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pd = new ArrayPersistenceDelegate();
    }

    @Override
    protected void tearDown() throws Exception {
        pd = null;
        super.tearDown();
    }
    
    public void testMutates() {
        assertFalse(pd.mutatesTo(new int[] { 1 }, null));
        assertFalse(pd.mutatesTo(null, null));
        assertFalse(pd.mutatesTo(new int[1], new int[2]));
        assertTrue(pd.mutatesTo(new int[] { 1, 3 }, new int[] { 1, 2 }));
    }
    
    /*
     * test mutates with wrapper array
     */
    public void test_MutatesTo_WrapperArray() {
        // Regression for Harmony-4022
        // one wrapper array and null
        assertFalse(pd.mutatesTo(new Integer[] { 1, 2, 3 }, null));
        assertFalse(pd.mutatesTo(new Boolean[] { true, false }, null));
        assertFalse(pd.mutatesTo(new Short[] { 1, 2 }, null));
        assertFalse(pd.mutatesTo(new Long[] { 23000000094382l, 23000000094383l }, null));
        assertFalse(pd.mutatesTo(new Character[] { 'a', 'b', 'c'}, null));
        assertFalse(pd.mutatesTo(new Float[] { 0.1f, 0.2f }, null));
        assertFalse(pd.mutatesTo(new Double[] { 0.1, 0.2 }, null));
        
        // two wrapper arries with the same component type but different length
        assertFalse(pd.mutatesTo(new Integer[] { 1, 2, 3 }, new Integer[] { 1, 2}));
        assertFalse(pd.mutatesTo(new Boolean[] { true, false }, new Boolean[] { true }));
        assertFalse(pd.mutatesTo(new Short[] { 1, 2 }, new Short[] { 1, 2, 3}));
        assertFalse(pd.mutatesTo(new Long[] { 23000000094382l, 23000000094383l }, new Long[] { 23000000094382l}));
        assertFalse(pd.mutatesTo(new Character[] { 'a', 'b', 'c'}, new Character[] {}));
        assertFalse(pd.mutatesTo(new Float[] { 0.1f, 0.2f }, new Float[] { 0.1f, 0.2f, 0.3f}));
        assertFalse(pd.mutatesTo(new Double[] { 0.1, 0.2 }, new Double[] { 0.1 }));
        
        // two wrapper arries with the same length but different component types
        assertFalse(pd.mutatesTo(new Integer[] { 1, 2, 3 }, new Boolean[] { true, false }));
        assertFalse(pd.mutatesTo(new Boolean[] { true, false }, new Short[] { 1, 2 }));
        assertFalse(pd.mutatesTo(new Short[] { 1, 2 }, new Long[] { 23000000094382l, 23000000094383l }));
        assertFalse(pd.mutatesTo(new Long[] { 23000000094382l, 23000000094383l }, new Character[] { 'a', 'b', 'c'}));
        assertFalse(pd.mutatesTo(new Character[] { 'a', 'b', 'c'}, new Float[] { 0.1f, 0.2f }));
        assertFalse(pd.mutatesTo(new Float[] { 0.1f, 0.2f }, new Double[] { 0.1 }));
        
        // two wrapper arries with the same length and component type but different internal values
        assertTrue(pd.mutatesTo(new Integer[] { 1, 2, 3 }, new Integer[] { 5, 6, 7 }));
        assertTrue(pd.mutatesTo(new Boolean[] { true, false, false}, new Boolean[] { false, true, true}));
        assertTrue(pd.mutatesTo(new Short[] { 1, 2 }, new Short[] { 4, 5 }));
        assertTrue(pd.mutatesTo(new Long[] { 23000000094382l, 23000000094383l }, new Long[] { 534300002l, 23020094383l }));
        assertTrue(pd.mutatesTo(new Character[] { 'a', 'b', 'c'}, new Character[] { 'd', 'e', 'f'}));
        assertTrue(pd.mutatesTo(new Float[] { 0.1f, 0.2f }, new Float[] { 0.4f, 0.6f }));
        assertTrue(pd.mutatesTo(new Double[] { 0.1, 0.2 }, new Double[] { 0.3, 0.343 }));
    }
    
    /*
     * test mutatesTo with object array
     */
    public void test_MutatesTo_ObjectArray(){
        // Regression for Harmony-4022
        // one object array and null
        assertFalse(pd.mutatesTo(new MockAObject[] { new MockAObject() }, null));
        assertFalse(pd.mutatesTo(new MockBObject[] { new MockBObject() }, null));
        assertFalse(pd.mutatesTo(new MockObject[] { new MockAObject(), new MockBObject()}, null));
        
        // two wrapper arries with the same component type but different length
        assertFalse(pd.mutatesTo(new MockObject[1], new MockObject[2]));
        assertFalse(pd.mutatesTo(new MockAObject[1], new MockAObject[2]));
        assertFalse(pd.mutatesTo(new MockBObject[1], new MockBObject[2]));
        
        // two object array with the same length but different component types
        assertFalse(pd.mutatesTo(new MockAObject[] { new MockAObject() }, new MockBObject[] { new MockBObject() }));
        assertFalse(pd.mutatesTo(new MockObject[] {new MockObject()}, new MockAObject[] { new MockAObject() }));
        assertFalse(pd.mutatesTo(new MockObject[] {new MockAObject()}, new MockAObject[] { new MockAObject() }));
        assertFalse(pd.mutatesTo(new MockObject[] {new MockBObject()}, new MockAObject[] { new MockAObject() }));
        assertFalse(pd.mutatesTo(new MockObject[] {new MockObject()}, new MockBObject[] { new MockBObject() }));
        assertFalse(pd.mutatesTo(new MockObject[] {new MockBObject()}, new MockBObject[] { new MockBObject() }));
        assertFalse(pd.mutatesTo(new MockObject[] {new MockAObject()}, new MockBObject[] { new MockBObject() }));
        
        // two object array with the same length and component type but different internal values
        assertTrue(pd.mutatesTo(new MockObject[] { new MockAObject() }, new MockObject[] { new MockBObject() }));
        assertTrue(pd.mutatesTo(new MockAObject[] { new MockAObject(1) }, new MockAObject[] { new MockAObject(2) }));
        assertTrue(pd.mutatesTo(new MockBObject[] { new MockBObject(1) }, new MockBObject[] { new MockBObject(2) }));
    }
    
    public void testInitialize() {
        // TBD
    }

    public void testInstantiate_Normal() throws Exception {
        Object obj = new int[] { 1, 2, 3 };
        Expression exp = pd.instantiate(obj, new Encoder());
        assertSame(obj, exp.getValue());
        assertSame(Array.class, exp.getTarget());
        assertEquals("newInstance", exp.getMethodName());
        assertEquals(2, exp.getArguments().length);
        assertSame(Integer.TYPE, exp.getArguments()[0]);
        assertEquals(new Integer(3), exp.getArguments()[1]);
    }
    
    public class MockObject {
        
    }
    
    public class MockAObject extends MockObject {
        String name = "A Object";
        int id = 0x01;
        
        public MockAObject() {
            
        }
        
        public MockAObject(int idValue){
            id = idValue;
        }
    }
    
    public class MockBObject extends MockObject {
        String name = "B Object";
        int id = 0x02;
        
        public MockBObject(){
            
        }
        
        public MockBObject(int idValue){
            id = idValue;
        }
    }

    public static class ParentClass {

        String multiArray[][][] = { { { "1", "2" } }, { { "3", "4", "5" } },
                { { "1", "2" }, { "3", "4", "5" } } };

        public ParentClass() {
        }

        public String[][][] getMultiArray() {
            return multiArray;
        }

        public void setMultiArray(String[][][] array) {
            multiArray = array;
        }

    }

    public static class ChildClass extends ParentClass {

        public ChildClass() {

        }

    }

    public void testInitialize_MultiArray() throws Exception {
        ChildClass child = new ChildClass();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLEncoder xmlEncoder = new XMLEncoder(bos);
        xmlEncoder.writeObject(child);
        xmlEncoder.close();
        assertFalse(bos.toString("UTF-8").contains("multiArray"));
    }

}
