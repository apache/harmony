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

import java.awt.Dialog.AccessibleAWTDialog;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import junit.framework.TestCase;

/**
 * AccessibleAWTDialogTest
 */
public class AccessibleAWTDialogTest extends TestCase {

    AccessibleContext ac;
    private Dialog dialog;
    private Frame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new Frame();
        dialog = new Dialog(frame);
        ac = dialog.getAccessibleContext();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if ((frame != null) && frame.isDisplayable()) {
            frame.dispose();
        }
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.DIALOG, ac.getAccessibleRole());
    }

    public final void testGetAccessibleStateSet() {
        AccessibleStateSet aStateSet = ac.getAccessibleStateSet();
        assertFalse("accessible dialog is active",
                   aStateSet.contains(AccessibleState.ACTIVE));
        assertTrue("accessible dialog is resizable",
                   aStateSet.contains(AccessibleState.RESIZABLE));
        assertFalse("accessible dialog is NOT modal",
                    aStateSet.contains(AccessibleState.MODAL));

        dialog.setResizable(false);
        aStateSet = ac.getAccessibleStateSet();
        assertFalse("accessible dialog is NOT resizable",
                   aStateSet.contains(AccessibleState.RESIZABLE));
        dialog.setModal(true);
        aStateSet = ac.getAccessibleStateSet();
        assertTrue("accessible dialog is modal",
                    aStateSet.contains(AccessibleState.MODAL));
    }

    public final void testAccessibleAWTFrame() {
        assertTrue(ac instanceof AccessibleAWTDialog);
    }

}
