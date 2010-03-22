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

package org.apache.harmony.lang.generics;

import junit.framework.TestCase;

public class SpecialD extends TestCase {
    public void test() throws Throwable {
            new SpecialC();
            if(((SpecialClassLoader)this.getClass().getClassLoader()).checkFind("org.apache.harmony.lang.generics.SpecialC")==null) {
                fail("FAILED: " + this.getClass().getClassLoader().getClass() + " wasn't marked as initiating classloader for SpecialC");
            }
            
            // Trying to define SpecialC for the second time...
            ClassLoaderTest.flag++;
            try {
               ((SpecialClassLoader)this.getClass().getClassLoader()).loadClass("");
                fail("FAILED: LinkageError wasn't thrown");
            } catch (LinkageError err) {
                // expected
            }
    }
}