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
 * @author Salikh Zakirov
 */  
package outofmemory;

/**
 * Out of memory tests have no dependecy on class library.
 * @keyword X_ipf_bug_3886
 */
public class VectorRef {
    public static void main (String[] args) {
        int size = 1000*1048576/8; // a little less than 1G
        try {
            System.out.println("allocating Object[" + size + "]");
            Object[] b = new Object[size];
            Object[] c = new Object[size];
            System.out.println("allocation succeeded");
            b[0] = System.in;
            c[0] = System.in;
            System.out.println("write to start succeeded");
            b[b.length-1] = System.out;
            c[c.length-1] = System.out;
            System.out.println("write to end succeeded");
        } catch (OutOfMemoryError e) {
            System.out.println("\nPASS"); System.out.flush();
            e.printStackTrace(System.out);
        }
    }
}
