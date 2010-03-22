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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.plaf.basic;

import javax.swing.JPasswordField;
import javax.swing.SwingTestCase;
import javax.swing.text.Element;
import javax.swing.text.PasswordView;
import javax.swing.text.PlainDocument;

public class BasicPasswordFieldUITest extends SwingTestCase {
    BasicPasswordFieldUI ui;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new BasicPasswordFieldUI();
    }

    public void testCreateElement() {
        Element element = new PlainDocument().getDefaultRootElement();
        assertTrue(ui.create(element) instanceof PasswordView);
    }

    public void testCreateUIJComponent() {
        assertTrue(BasicPasswordFieldUI.createUI(null) instanceof BasicPasswordFieldUI);
        assertTrue(BasicPasswordFieldUI.createUI(new JPasswordField()) instanceof BasicPasswordFieldUI);
    }

    public void testGetPropertyPrefix() {
        assertEquals("PasswordField", ui.getPropertyPrefix());
    }
}
