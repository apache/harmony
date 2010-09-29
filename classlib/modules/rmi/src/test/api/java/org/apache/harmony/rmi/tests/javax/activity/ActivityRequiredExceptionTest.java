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
package org.apache.harmony.rmi.tests.javax.activity;

import javax.activity.ActivityCompletedException;
import javax.activity.ActivityRequiredException;

import junit.framework.TestCase;

public class ActivityRequiredExceptionTest extends TestCase {

    public void test_Constructor(){
        ActivityRequiredException are = new ActivityRequiredException();
        assertNull(null, are.getMessage());
    }
    
    public void test_Constructor_String(){
        ActivityRequiredException are = new ActivityRequiredException("Hello");
        assertEquals("Hello", are.getMessage());
    }
    
    public void test_Constructor_String_Throwable() {
        Throwable cause = new Throwable();
        ActivityRequiredException ace = new ActivityRequiredException(
                "Hello", cause);

        assertSame(cause, ace.getCause());
    }

    public void test_Constructor_Throwable() {
        Throwable cause = new Throwable();
        ActivityRequiredException ace = new ActivityRequiredException(cause);
        
        assertSame(cause, ace.getCause());
    }
}
