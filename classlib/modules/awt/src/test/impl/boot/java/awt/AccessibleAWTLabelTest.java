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
 * @author Dmitry A. Durnev
 */
package java.awt;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import junit.framework.TestCase;

/**
 * AccessibleAWTLabelTest
 */
public class AccessibleAWTLabelTest extends TestCase {
    Label label;
    AccessibleContext ac;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        label = new Label();
        ac = label.getAccessibleContext();
        assertNotNull(ac);
    }


    public final void testGetAccessibleName() {
        assertEquals("", ac.getAccessibleName());
        String text = "label";
        label.setText(text);
        assertEquals(text, ac.getAccessibleName());
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.LABEL, ac.getAccessibleRole());
    }

    public final void testAccessibleAWTLabel() {
        assertNotNull(label);
        assertNotNull(label.new AccessibleAWTLabel());
    }

}
