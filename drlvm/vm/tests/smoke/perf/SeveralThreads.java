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

/**
 * This is a programm that generates 5 threads 
 * that allocate objects of different sizes.
 */
class NewThread extends Thread {
    static class CornerObject {
        byte array[];
	CornerObject next;
	CornerObject(int size)
	{
	    array=new byte[size*1024];
	}
    }

    public void run () {
    	int iterations = 1500;
	try{
    	    for(int k = 0; k < iterations; k++) {
		if((k % 2) == 0){
		    new CornerObject(105);
		    Thread.sleep(10);
		}else{
		    new CornerObject(5);
		    Thread.sleep(10);
		}
	    }
    	}catch(InterruptedException e) {}
    }
}

public class SeveralThreads {    
    public static void main (String[] args) {
      	long itime=System.currentTimeMillis();
        NewThread threads[] = new NewThread[5];
        for (int i = 0; i < 5; i++) {
	    threads[i] = new NewThread();
            threads[i].start();
        }
        for (int j = 0; j < 5; j++) {
            try {
                threads[j].join();
	    } catch (InterruptedException e) {}
        }
        System.out.println("The test run: "+(System.currentTimeMillis()-itime)+" ms\n");
        System.out.println("PASSED");
    }
}
