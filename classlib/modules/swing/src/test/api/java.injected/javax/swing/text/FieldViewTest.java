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
package javax.swing.text;

import java.awt.FontMetrics;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingTestCase;
import javax.swing.plaf.basic.BasicTextUI;

public class FieldViewTest extends SwingTestCase {
    JFrame jf;

    JTextField jtf;

    FieldView fv;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jf = new JFrame();
        jtf = new JTextField("JTextField for FieldView testing");
        jf.getContentPane().add(jtf);
        fv = (FieldView) ((BasicTextUI) jtf.getUI()).getRootView(jtf).getView(0);
        jf.setSize(200, 100);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testGetPreferredSpan() {
        FontMetrics fm = fv.getFontMetrics();
        assertEquals(fm.getHeight(), (int) fv.getPreferredSpan(View.Y_AXIS));
        assertEquals(fm.stringWidth(jtf.getText()), (int) fv.getPreferredSpan(View.X_AXIS));
        jtf.setFont(new java.awt.Font("SimSun", 0, 12));
        fv = (FieldView) ((BasicTextUI) jtf.getUI()).getRootView(jtf).getView(0);
        fm = jtf.getFontMetrics(jtf.getFont());
        assertEquals(fm.stringWidth(jtf.getText()), (int) fv.getPreferredSpan(View.X_AXIS));
    }

    public void testGetResizeWeight() {
        assertEquals(1, fv.getResizeWeight(View.X_AXIS));
        assertEquals(0, fv.getResizeWeight(View.Y_AXIS));
        assertEquals(0, fv.getResizeWeight(5000));
    }

    public void testGetFontMetrics() {
        assertEquals(jtf.getFontMetrics(jtf.getFont()), fv.getFontMetrics());
        jtf.setFont(new java.awt.Font("SimSun", 0, 12));
        fv = (FieldView) ((BasicTextUI) jtf.getUI()).getRootView(jtf).getView(0);
        assertEquals(jtf.getFontMetrics(jtf.getFont()), fv.getFontMetrics());
    }
}