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
 * @author Denis M. Kishenko
 */
package org.apache.harmony.awt.gl;

import java.awt.Rectangle;

import org.apache.harmony.awt.gl.MultiRectArea;

import junit.framework.TestCase;



public class MultiRectAreaRTest extends TestCase {

    public MultiRectAreaRTest(String name) {
        super(name);
    }

    public void test6766() {
        MultiRectArea area = new MultiRectArea(new Rectangle(20, 30, 40, 50));
        area.intersect(new Rectangle());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MultiRectAreaRTest.class);
    }

}
