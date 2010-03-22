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

package org.apache.harmony.beans.tests.support.mock;

/**
 * A bean that has get/set method different from getXXX and setXXX.
 */
public class MockFoo2 extends MockFoo2Parent {
    private int prop1;

    public MockFoo2() {
    }

    public MockFoo2(int prop) {
        this.prop1 = prop;
    }

    public int myget() {
        return this.prop1;
    }

    public void myset(int prop) {
        this.prop1 = prop;
    }
}
