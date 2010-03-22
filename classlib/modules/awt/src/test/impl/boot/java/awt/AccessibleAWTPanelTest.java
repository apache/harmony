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

import java.awt.Panel.AccessibleAWTPanel;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import junit.framework.TestCase;

/**
 * AccessibleAWTPanelTest
 */
public class AccessibleAWTPanelTest extends TestCase {
    Panel panel;
    AccessibleContext ac;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new Panel();
        ac = panel.getAccessibleContext();
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.PANEL, ac.getAccessibleRole());
    }

    public final void testAccessibleAWTPanel() {
        assertTrue(ac instanceof AccessibleAWTPanel);
    }

}
