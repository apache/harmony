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
 * @author Alexander T. Simbirtsev
 * Created on 02.03.2005

 */
package javax.swing.text;

import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWaitTestCase;
import junit.framework.TestCase;

public class TextAction_MultithreadedTest extends TestCase {
    JDialog window1;

    JDialog window2;

    JDialog window3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        if (window1 != null) {
            window1.dispose();
            window1 = null;
        }
        if (window2 != null) {
            window2.dispose();
            window2 = null;
        }
        if (window3 != null) {
            window3.dispose();
            window3 = null;
        }
        super.tearDown();
    }

    public void testGetFocusedComponent() throws Exception {
        TextAction action = new TextAction("") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        window1 = new JDialog();
        window2 = new JDialog();
        window3 = new JDialog();
        JButton component1 = new JButton();
        JComponent component2 = new JTextField();
        JComponent component3 = new JTextArea();
        ((JTextComponent) component2).setText("3");
        window1.getContentPane().add(component1);
        window2.getContentPane().add(component2);
        window3.getContentPane().add(component3);
        window1.pack();
        window2.pack();
        window3.pack();
        window2.setVisible(true);
        SwingWaitTestCase.requestFocusInWindowForComponent(component2);
        Object res = action.getFocusedComponent();
        assertEquals("focused component", component2, res);
        window3.setVisible(true);
        SwingWaitTestCase.requestFocusInWindowForComponent(component3);
        res = action.getFocusedComponent();
        assertEquals("focused component", component3, res);
        window1.setVisible(true);
        SwingWaitTestCase.requestFocusInWindowForComponent(component1);
        res = action.getFocusedComponent();
        assertEquals("focused component", component3, res);
    }

    public void testGetTextComponent() throws Exception {
        TextAction action = new TextAction("") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
            }
        };
        window1 = new JDialog();
        window2 = new JDialog();
        JButton componentNoText1 = new JButton();
        JButton componentNoText2 = new JButton();
        JTextComponent componentText1 = new JTextField();
        JTextComponent componentText2 = new JTextField();
        componentText1.setText("1");
        componentText1.setText("2");
        ActionEvent eventNoTextComponentInside = new ActionEvent(componentNoText2, 0, "event1");
        ActionEvent eventTextComponentInside = new ActionEvent(componentText2, 0, "event2");
        window1.getContentPane().add(componentNoText1);
        window2.getContentPane().add(componentText1);
        window1.pack();
        window2.pack();
        Object previouslyFocused = action.getFocusedComponent();
        window1.setVisible(true);
        SwingWaitTestCase.requestFocusInWindowForComponent(componentNoText1);
        Object res = action.getTextComponent(eventNoTextComponentInside);
        assertEquals("focused component", previouslyFocused, res);
        window1.setVisible(true);
        SwingWaitTestCase.requestFocusInWindowForComponent(componentNoText1);
        res = action.getTextComponent(eventTextComponentInside);
        assertEquals("focused component", componentText2, res);
        window2.setVisible(true);
        SwingWaitTestCase.requestFocusInWindowForComponent(componentText1);
        res = action.getTextComponent(eventNoTextComponentInside);
        assertEquals("focused component", componentText1, res);
        SwingWaitTestCase.requestFocusInWindowForComponent(componentText1);
        res = action.getTextComponent(eventTextComponentInside);
        assertEquals("focused component", componentText2, res);
    }
}