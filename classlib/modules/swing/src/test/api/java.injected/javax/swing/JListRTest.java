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
 * @author Vladimir Ivanov
 */
package javax.swing;

import junit.framework.TestCase;

public class JListRTest extends TestCase {
    public void testAddSelectionInterval() throws Exception {
        JList jl = new JList();
        jl.addSelectionInterval(10000000, 1);
    }

    public void testSetSelectedIndicies() throws Exception {
        JList l = new JList(new String[] { "", "", "", "", "" });
        l.setSelectedIndices(new int[] { -1, 2, 3, 4, 200, 250 });
        assertEquals(2, l.getSelectionModel().getMinSelectionIndex());
        assertEquals(4, l.getSelectionModel().getMaxSelectionIndex());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JListRTest.class);
    }
}
