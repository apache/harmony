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
 * @author Alexei Fedotov
 */  
package stress;


/**
 * @keyword 
 */
public class Vector {

    synchronized boolean test(int end) {
        java.util.Vector v = new java.util.Vector(120);

        for (int i = 0; i < end; i++) {
            String s = new String("String N " + i);
            v.add(new String(s));
        }
        return true;
    }

    public static void main(String[] args) {
        boolean pass = false;

        try {
            Vector test = new Vector();
            pass = test.test(5000);
        } catch (Throwable e) {
            System.out.println("Got exception: " + e.toString());            
        }
        System.out.println("Vector test " + (pass ? "passed" : "failed"));
    }
}
