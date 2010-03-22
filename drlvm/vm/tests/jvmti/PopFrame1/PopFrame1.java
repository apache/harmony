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
 * @author Pavel Rebriy
 */

package PopFrame1;

import junit.framework.TestCase;

public class PopFrame1 extends TestCase {

	static boolean status = false;

    static public void main(String args[]) {
    	new PopFrame1().test();
        if (status) {
            System.out.println("Test passed!");
        } else {
            System.out.println("Test failed!");
        }
        assertTrue(status);
    }
    
    public void test() {
    	Thread inst = new Thread("Test thread") {
	    	public void run() {
	        	System.out.println("Test: First step!");
	            second_step();
	            return;
	        }
	
	    	private void second_step() {
	    		if(status) {
	    			return;
	    		}
	        	System.out.println("Test: Second step!");
	            third_step();
	            return;
	        }
	
	    	private void third_step() {
	    		if(status) {
	    			return;
	    		}
	        	System.out.println("Test: Third step!");
	            long x = 0;
	            for (long a = 0; a < 100000L; a++) {
                    if(status) {
                    	return;
                    }
                    x += a;
	            }
	            if(status) {
	            	return;
	            }
	            System.out.println("Test: done!");
	        }
	    };
    	inst.start();
	    
    	// waiting when thread is terminated
    	try {
        	inst.join();
    	} catch(InterruptedException exc) {
    		System.out.println("Main: control thread join was interruped");
    	}
        assertTrue(status);
        return;
    }
}
