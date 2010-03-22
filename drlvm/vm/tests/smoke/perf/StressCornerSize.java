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
 * @author Vera Volynets
 */  
package perf;

/** This is a simple program that allocates objects of 25kb and 45kb and create list.
 * This test should stress gc_v4 (mark-compact) algorithm
 * because off "corner" size of allocated objects (64kb). Set heap size 128Mb. 
 * Garbage is interleaved with live objects.
 */
public class StressCornerSize {

   static class CornerObject {
        byte array[];
	CornerObject next;
	CornerObject(int size)
	{
	    array=new byte[size*1024];
	}
    }

    public static void main(String[] args) {
      	long itime=System.currentTimeMillis();
        int iterations = 2500;
        CornerObject corner_head = new CornerObject(20);
	CornerObject corner;
	    
	for (int i = 0; i < iterations; i++) {
	    if((i % 2) == 0){
	        corner = new CornerObject(45);
	    }else{
	        corner = new CornerObject(25);
	    }
	    corner.next = corner_head;
	    corner_head = corner;
	    if((i != 0) && ( (i % 8) == 0)){
	        corner_head.next = corner_head.next.next;
	    }
	}
	System.out.println("The test run: "+(System.currentTimeMillis()-itime)+" ms\n");
	System.out.println ("PASSED");
    }
}

