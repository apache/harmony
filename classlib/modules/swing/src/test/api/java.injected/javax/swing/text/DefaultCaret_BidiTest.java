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

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;

public class DefaultCaret_BidiTest extends SwingTestCase {
    AbstractDocument ad;

    boolean bWasException = false;

    DefaultCaret dc = null;

    JFrame jf = null;

    JTextArea jta = null;

    String s = null;

    String sLTR = new String("\u0061");

    String sRTL = new String("\u05DC");

    public DefaultCaret_BidiTest(final String name) {
        super(name);
    }

    void dotTest(final int dot, final Position.Bias bias, final String s) {
        assertEquals(dot, dc.getDot());
        assertEquals(bias, dc.getDotBias());
        assertEquals(s, dc.toString());
    }

    void insertCase(final int offset, final String ins, final int sDot,
            final Position.Bias sBias, final int gDot, final Position.Bias gBias) {
        jta.setText(sLTR + sRTL + sLTR + sRTL + sLTR + sRTL);
        dc.setDot(sDot, sBias);
        try {
            ad.insertString(offset, ins, null);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        assertEquals(gDot, dc.getDot());
        assertEquals(gBias, dc.getDotBias());
    }

    void removeCase(final String sText, final int offset, final int length, final int sDot,
            final Position.Bias sBias, final int gDot, final Position.Bias gBias) {
        jta.setText(sText);
        dc.setDot(sDot, sBias);
        try {
            ad.remove(offset, length);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        assertEquals(gDot, dc.getDot());
        assertEquals(gBias, dc.getDotBias());
    }

    void setDotCase(final int dot, final Position.Bias bias, final String s) {
        dc.setDot(dot, bias);
        assertEquals(dot, dc.getDot());
        assertEquals(bias, dc.getDotBias());
        String tmp = "Dot=(" + dc.getDot() + ", " + dc.getDotBias().toString() + ") "
                + "Mark=(" + dc.getMark() + ", ";
        assertEquals(tmp + s, dc.toString());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jta = new JTextArea(sLTR + sRTL + sLTR + sRTL + sLTR + sRTL);
        dc = new DefaultCaret();
        jf = new JFrame();
        bWasException = false;
        s = null;
        ad = (AbstractDocument) jta.getDocument();
        jf.getContentPane().add(jta);
        jta.getCaret().deinstall(jta);
        jta.setCaret(dc);
        jf.setLocation(100, 100);
        jf.setSize(350, 200);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testBidiInsert() throws Exception {
        insertCase(0, sLTR, 2, Position.Bias.Forward, 3, Position.Bias.Forward);
        insertCase(1, sLTR, 2, Position.Bias.Forward, 3, Position.Bias.Forward);
        insertCase(2, sLTR, 2, Position.Bias.Forward, 3, Position.Bias.Backward);//b=f
        insertCase(3, sLTR, 2, Position.Bias.Forward, 2, Position.Bias.Forward);
        insertCase(0, sRTL, 2, Position.Bias.Forward, 3, Position.Bias.Forward);
        insertCase(1, sRTL, 2, Position.Bias.Forward, 3, Position.Bias.Forward);
        insertCase(2, sRTL, 2, Position.Bias.Forward, 3, Position.Bias.Backward);
        insertCase(3, sRTL, 2, Position.Bias.Forward, 2, Position.Bias.Forward);
        insertCase(0, sLTR, 2, Position.Bias.Backward, 3, Position.Bias.Backward);
        insertCase(1, sLTR, 2, Position.Bias.Backward, 3, Position.Bias.Backward);
        insertCase(2, sLTR, 2, Position.Bias.Backward, 3, Position.Bias.Backward);
        insertCase(3, sLTR, 2, Position.Bias.Backward, 2, Position.Bias.Backward);
        insertCase(0, sRTL, 2, Position.Bias.Backward, 3, Position.Bias.Backward);
        insertCase(1, sRTL, 2, Position.Bias.Backward, 3, Position.Bias.Backward);
        insertCase(2, sRTL, 2, Position.Bias.Backward, 3, Position.Bias.Backward);
        insertCase(3, sRTL, 2, Position.Bias.Backward, 2, Position.Bias.Backward);
        insertCase(1, sLTR, 3, Position.Bias.Forward, 4, Position.Bias.Forward);
        insertCase(2, sLTR, 3, Position.Bias.Forward, 4, Position.Bias.Forward);
        insertCase(3, sLTR, 3, Position.Bias.Forward, 4, Position.Bias.Backward);
        insertCase(4, sLTR, 3, Position.Bias.Forward, 3, Position.Bias.Forward);
        insertCase(1, sRTL, 3, Position.Bias.Forward, 4, Position.Bias.Forward);
        insertCase(2, sRTL, 3, Position.Bias.Forward, 4, Position.Bias.Forward);
        insertCase(3, sRTL, 3, Position.Bias.Forward, 4, Position.Bias.Backward);
        insertCase(4, sRTL, 3, Position.Bias.Forward, 3, Position.Bias.Forward);
        insertCase(1, sLTR, 3, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(2, sLTR, 3, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(3, sLTR, 3, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(4, sLTR, 3, Position.Bias.Backward, 3, Position.Bias.Backward);
        insertCase(1, sRTL, 3, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(2, sRTL, 3, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(3, sRTL, 3, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(4, sRTL, 3, Position.Bias.Backward, 3, Position.Bias.Backward);
        insertCase(0, sRTL + sRTL, 2, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(1, sRTL + sRTL, 2, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(2, sRTL + sRTL, 2, Position.Bias.Backward, 4, Position.Bias.Backward);
        insertCase(3, sRTL + sRTL, 2, Position.Bias.Backward, 2, Position.Bias.Backward);
    }

    public void testBidiRemove() throws Exception {
        String s1 = sLTR + sRTL + sLTR + sRTL + sLTR + sRTL + sLTR + sRTL + sLTR;
        String s2 = sRTL + sRTL + sLTR + sLTR + sRTL + sRTL + sLTR + sLTR + sRTL;
        //s1 = sLTR + sRTL + sLTR + sRTL + sLTR + sRTL + sLTR + sRTL +
        // sLTR;
        removeCase(s1, 2, 1, 3, Position.Bias.Forward, 2, Position.Bias.Forward);
        //removeCase(s1,2,3,3,Position.Bias.Forward,2,Position.Bias.Forward);//b=f
        removeCase(s1, 2, 1, 3, Position.Bias.Backward, 2, Position.Bias.Backward);
        removeCase(s1, 2, 3, 3, Position.Bias.Backward, 2, Position.Bias.Backward);
        //removeCase(s1,1,2,3,Position.Bias.Forward,1,Position.Bias.Backward);//!!!
        //removeCase(s1,1,3,3,Position.Bias.Forward,1,Position.Bias.Backward);//b=f
        removeCase(s1, 1, 2, 3, Position.Bias.Backward, 1, Position.Bias.Backward);
        removeCase(s1, 1, 3, 3, Position.Bias.Backward, 1, Position.Bias.Backward);
        removeCase(s1, 3, 1, 3, Position.Bias.Forward, 3, Position.Bias.Backward); // b=f
        removeCase(s1, 3, 2, 3, Position.Bias.Forward, 3, Position.Bias.Backward); // !!!
        removeCase(s1, 3, 1, 3, Position.Bias.Backward, 3, Position.Bias.Backward);
        removeCase(s1, 3, 2, 3, Position.Bias.Backward, 3, Position.Bias.Backward);
        //s2 = sRTL + sRTL + sLTR + sLTR + sRTL + sRTL + sLTR + sLTR +
        // sRTL;
        //removeCase(s2,2,1,3,Position.Bias.Forward,2,Position.Bias.Backward);//b=f
        // begin
        //removeCase(s2,2,3,3,Position.Bias.Forward,2,Position.Bias.Backward);//
        // b=f
        removeCase(s2, 2, 1, 3, Position.Bias.Backward, 2, Position.Bias.Backward);
        removeCase(s2, 2, 3, 3, Position.Bias.Backward, 2, Position.Bias.Backward);
        //removeCase(s2,1,2,3,Position.Bias.Forward,1,Position.Bias.Backward);
        // //b=f
        //removeCase(s2,1,3,3,Position.Bias.Forward,1,Position.Bias.Backward);
        // //b=f begin
        removeCase(s2, 1, 2, 3, Position.Bias.Backward, 1, Position.Bias.Backward);
        removeCase(s2, 1, 3, 3, Position.Bias.Backward, 1, Position.Bias.Backward);
        //removeCase(s2,3,1,3,Position.Bias.Forward,3,Position.Bias.Backward);//!!!
        //removeCase(s2,3,2,3,Position.Bias.Forward,3,Position.Bias.Backward);//b=f
        removeCase(s2, 3, 1, 3, Position.Bias.Backward, 3, Position.Bias.Backward);
        removeCase(s2, 3, 2, 3, Position.Bias.Backward, 3, Position.Bias.Backward);
    }

    public void testBidiSetDot() throws Exception {
        setDotCase(0, Position.Bias.Forward, "Forward)");
        setDotCase(1, Position.Bias.Backward, "Backward)");
        setDotCase(2, Position.Bias.Forward, "Forward)");
        setDotCase(3, Position.Bias.Backward, "Backward)");
        setDotCase(ad.getLength(), Position.Bias.Backward, "Backward)");
    }

    public void testBidiMoveDot() throws Exception {
        jta.setText(sLTR + sRTL + sLTR + sRTL + sLTR);
        dc.setDot(1, Position.Bias.Backward);
        dotTest(1, Position.Bias.Backward, "Dot=(1, Backward) Mark=(1, Backward)");
        dc.moveDot(3);
        dotTest(3, Position.Bias.Forward, "Dot=(3, Forward) Mark=(1, Backward)");
        dc.setDot(2, Position.Bias.Forward);
        dotTest(2, Position.Bias.Forward, "Dot=(2, Forward) Mark=(2, Forward)");
        dc.moveDot(3);
        dotTest(3, Position.Bias.Forward, "Dot=(3, Forward) Mark=(2, Forward)");
    }
}
