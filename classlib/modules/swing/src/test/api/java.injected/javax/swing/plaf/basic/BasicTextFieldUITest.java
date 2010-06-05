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

import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingTestCase;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.FieldView;
import javax.swing.text.View;

public class BasicTextFieldUITest extends SwingTestCase {
    JFrame jf;

    JTextField jtf;

    JTextField jtfBidi;

    BasicTextFieldUI ui;

    final String S_RTL = "\u05dc" + "\u05dc" + "\u05dc" + "\u05dc";

    final String S_LTR = "aaaa";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jf = new JFrame();
        jtf = new JTextField("JTextField \n JTextField");
        jtfBidi = new JTextField(S_LTR + S_RTL + "\n" + S_LTR);
        ui = new BasicTextFieldUI();
        jtf.setUI(ui);
        jtfBidi.setUI(new BasicTextFieldUI());
        Container container = jf.getContentPane();
        container.setLayout(new GridLayout(2, 1, 4, 4));
        container.add(jtf);
        container.add(jtfBidi);
        jf.setSize(200, 200);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testCreateElement() {
        Element element = jtf.getDocument().getDefaultRootElement();
        View view = ui.create(element);
        assertTrue(view instanceof FieldView);
        element = element.getElement(0);
        view = ui.create(element);
        assertTrue(view instanceof FieldView);
        /* no view support for bidi text
         ui = (BasicTextFieldUI)jtfBidi.getUI();
         element = jtfBidi.getDocument().getDefaultRootElement();
         view = ui.create(element);
         assertFalse(view instanceof FieldView);
         */
    }

    public void testGetPropertyPrefix() {
        assertEquals("TextField", ui.getPropertyPrefix());
    }

    public void testInstallUIJComponent() {
    }

    public void testPropertyChange() {
    }

    public void testBasicTextFieldUI() {
    }

    public void testCreateUIJComponent() {
        ComponentUI componentUI = BasicTextFieldUI.createUI(null);
        assertTrue(componentUI instanceof BasicTextFieldUI);
        assertNotSame(BasicTextFieldUI.createUI(jtf), componentUI);
    }
}
