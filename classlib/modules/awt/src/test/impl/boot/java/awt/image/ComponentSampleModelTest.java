/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance    
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package java.awt.image;

import junit.framework.TestCase;

public class ComponentSampleModelTest extends TestCase {
    
    public ComponentSampleModelTest(String name) {
        super(name);
    }
    
    public final void testSetDataElements(){
        // Checking ArrayIndexOutOfBoundsException when passes wrong x or y
        int[] offsets = new int[4];
        ComponentSampleModel csm = new
            ComponentSampleModel(DataBuffer.TYPE_USHORT,238,4,7,14,offsets);
        ComponentSampleModel obj = new
            ComponentSampleModel(DataBuffer.TYPE_USHORT,1,2,3,15, offsets);
               
        DataBufferFloat db = new DataBufferFloat(4);
        try{
            csm.setDataElements(-1399, 2, obj, db);
            fail("Expected ArrayIndexOutOfBoundsException didn't throw");
        }catch (ClassCastException e) {
            fail("Unexpected ClassCastException was thrown");
        }catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }
    
    public final void testGetDataElements(){
        // Checking ArrayIndexOutOfBoundsException when passes wrong x or y
        int[] offsets = new int[4];
        ComponentSampleModel csm = new
            ComponentSampleModel(DataBuffer.TYPE_USHORT,238,4,7,14,offsets);
        ComponentSampleModel obj = new
            ComponentSampleModel(DataBuffer.TYPE_USHORT,1,2,3,15, offsets);
               
        DataBufferFloat db = new DataBufferFloat(4);
        try{
            csm.getDataElements(-1399, 2, obj, db);
            fail("Expected ArrayIndexOutOfBoundsException didn't throw");
        }catch (ClassCastException e) {
            fail("Unexpected ClassCastException was thrown");
        }catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    public void testGetPixelsMaxValue()  throws Exception {
        ComponentSampleModel csm = new ComponentSampleModel(0, 10, 10, 1, 10, new int[]{0}); 
        DataBufferInt dbi = new DataBufferInt(100); 

        try { 
            csm.getPixels(8, Integer.MAX_VALUE, 1, 1, (int[]) null, dbi);
            fail("Exception expected");
        } catch(ArrayIndexOutOfBoundsException expectedException) { 
            // expected
        } 
    }


    public void testGetSamples() {
        // regression for HARMONY-2801
        ComponentSampleModel csm = new ComponentSampleModel(3, 10, 10, 1, 10, new int[]{0});

        try {
            int[] returnValue =
                    csm.getSamples(Integer.MAX_VALUE,4,1,1,0,new int[]{0},(DataBuffer) null);
            fail("No exception");
        } catch(NullPointerException expectedException) {
            // expected
        }
    } 
}

