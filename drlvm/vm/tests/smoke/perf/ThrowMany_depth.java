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
 * @author Pavel Afremov, Vera Volynets
 */  
 
package perf;

public class ThrowMany_depth {

    private final static int MAX_THROW = 100000; 
    private final static int MAX_DEPTH = 30;

    static class TestLazyException extends Exception {
        public static final long serialVersionUID = 0L;
    }
    
    private final static TestLazyException testLazyException = new TestLazyException();
    
    private void runTest() {
        for (int i = 0; i < MAX_THROW; i++) {
            try {
                depthThrow(MAX_DEPTH);
            } catch (TestLazyException tle) {}
        }
    }
   
    private void depthThrow(int depth) throws TestLazyException{
        if (depth == 0) {
            throw testLazyException;
        } else {
            depthThrow(depth - 1);
        }
    }

    public static void main(String argv[]) {
        ThrowMany_depth test = new ThrowMany_depth();
        test.runTest();
	System.out.println("PASSED");	
    }
}


