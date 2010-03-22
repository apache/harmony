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
 * tested class: java.lang.Class
 * tested method: getAnnotation
 * 
 */

package java.lang;

import junit.framework.TestCase;

/**
 * @tested class: java.lang.Class
 * @tested method: getClasses
 */
public class ClassTestGetAnnotation extends TestCase {
    
     /*
      * Regression test for HARMONY-886
      * [classlib][core][drlvm] compatibility: Harmony method 
      * Class.getAnnotation(null) return null while RI throws NPE
      *
      */
     public void test_HARMONY_886() {
         boolean et = false;
         try {
              Object.class.getAnnotation(null);
         } catch (NullPointerException e) {
             et = true;
         }
         assertTrue("NullPointerException expected", et);
     }
}
