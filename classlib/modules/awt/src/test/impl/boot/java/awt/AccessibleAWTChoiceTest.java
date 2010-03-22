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

import java.awt.Choice.AccessibleAWTChoice;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import junit.framework.TestCase;

/**
 * AccessibleAWTChoice
 */
public class AccessibleAWTChoiceTest extends TestCase {
    Choice choice;
    AccessibleContext ac;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        choice = new Choice();
        ac = choice.getAccessibleContext();
        assertNotNull(ac);
    }

    public final void testGetAccessibleAction() {
        assertTrue(ac.getAccessibleAction() instanceof AccessibleAWTChoice);
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.COMBO_BOX, ac.getAccessibleRole());
    }

    public final void testAccessibleAWTChoice() {
        assertNotNull(choice.new AccessibleAWTChoice());
    }

    public final void testGetAccessibleActionCount() {
        assertEquals(0, ac.getAccessibleAction().getAccessibleActionCount());
    }

    public final void testDoAccessibleAction() {
        assertFalse(ac.getAccessibleAction().doAccessibleAction(0));
    }

    public final void testGetAccessibleActionDescription() {
        assertNull(ac.getAccessibleAction().getAccessibleActionDescription(0));
    }

}
