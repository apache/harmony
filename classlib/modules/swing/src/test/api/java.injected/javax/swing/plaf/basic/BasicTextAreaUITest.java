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
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.WrappedPlainView;
import junit.framework.AssertionFailedError;

public class BasicTextAreaUITest extends SwingTestCase {
    JFrame jf;

    JTextArea jta;

    JTextArea bidiJta;

    String sLTR = "aaaa";

    String sRTL = "\u05dc" + "\u05dc" + "\u05dc" + "\u05dc";

    String content = "Edison accumul\tator, Edison base: Edison battery"
            + " Edison cap, \tEdison effect\n"
            + "Edison screw, Edison screw cap, Edison screw \n"
            + "holder, Edison screw lampholder, Edison screw " + "plug\n"
            + "Edison screw terminal, Edison storage battery" + "Edison storage \t\tcell";

    String bidiContent = sLTR + sRTL + sRTL + " \t" + sLTR + sRTL + sLTR + "\n" + sRTL + "."
            + sLTR + sRTL + "\t" + sRTL + "\n" + sLTR + sLTR + sRTL + sRTL + sRTL + sLTR + sLTR
            + sLTR + sRTL + sLTR + sRTL + sLTR;

    AssertionFailedError afe[];

    int i;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        afe = new AssertionFailedError[50];
        i = 0;
        UIManager.put("TextAreaUI", "javax.swing.plaf.basic.TextAreaUI");
        jta = new JTextArea(content);
        bidiJta = new JTextArea(bidiContent);
        jf = new JFrame();
        Container cont = jf.getContentPane();
        cont.setLayout(new GridLayout(1, 2, 4, 4));
        cont.add(jta);
        cont.add(bidiJta);
        jf.setLocation(200, 300);
        jf.setSize(350, 400);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        UIManager.put("TextAreaUI", "javax.swing.plaf.basic.BasicTextAreaUI");
        super.tearDown();
    }

    // TODO add test for bidirectional text (after creation PlainViewi18n)
    public void testCreateElement() throws Exception {
        Document doc = jta.getDocument();
        Element elem = doc.getDefaultRootElement();
        BasicTextUI ui = (BasicTextUI) jta.getUI();
        assertTrue(ui.create(elem) instanceof PlainView);
        jta.setLineWrap(true);
        assertTrue(ui.create(elem) instanceof WrappedPlainView);
        jta.setLineWrap(false);
        elem = elem.getElement(0);
        assertTrue(ui.create(elem) instanceof PlainView);
        jta.setLineWrap(true);
        assertTrue(ui.create(elem) instanceof WrappedPlainView);

        try {      
            new BasicTextAreaUI().create(null);  
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {              
            // PASSED            
        } 
    }

    public void testGetPropertyPrefix() {
        assertEquals("TextArea", ((BasicTextAreaUI) jta.getUI()).getPropertyPrefix());
        assertEquals("TextArea", ((BasicTextAreaUI) bidiJta.getUI()).getPropertyPrefix());
    }

    public void testPropertyChange() throws Exception {
        TextAreaUI ui = (TextAreaUI) jta.getUI();
        ui.flagModelChanged = false;
        jta.setLineWrap(true);
        assertTrue(ui.flagModelChanged);
        ui.flagModelChanged = false;
        jta.setLineWrap(false);
        assertTrue(ui.flagModelChanged);
        ui.flagModelChanged = false;
        jta.setWrapStyleWord(true);
        assertTrue(ui.flagModelChanged);
        ui.flagModelChanged = false;
        jta.setWrapStyleWord(false);
        assertTrue(ui.flagModelChanged);
    }

    public void testCreateUIJComponent() {
        JTextArea jta = new JTextArea();
        ComponentUI ui = BasicTextAreaUI.createUI(jta);
        assertTrue(ui instanceof BasicTextAreaUI);
        assertNotSame(ui, BasicTextAreaUI.createUI(jta));
    }

    public void testGetPrefferedSize() {
    }

    public void testGetMinimumSize() {
    }

    public void testInstallDefaults() {
    }
}
