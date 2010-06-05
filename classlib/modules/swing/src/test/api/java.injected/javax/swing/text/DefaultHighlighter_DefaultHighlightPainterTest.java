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
 * Created on 11.11.2004

 */
package javax.swing.text;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingWaitTestCase;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class DefaultHighlighter_DefaultHighlightPainterTest extends TestCase {
    JTextArea jta;

    JFrame jf;

    DefaultHighlighter dh;

    AssertionFailedError[] afe;

    DefaultHighlighter.DefaultHighlightPainter lp;

    boolean bWasException;

    String s = null;

    int i = 0;

    DefPainter dp;

    String str;

    Rectangle rectangle;

    int pos0;

    int pos1;

    String sRTL = new String("\u05DC");

    String sLTR = new String("\u0061");

    class DefPainter extends DefaultHighlighter.DefaultHighlightPainter {
        Graphics graphics;

        int i0;

        int i1;

        Shape shape;

        JTextComponent jtc;

        View view;

        DefPainter(final Color c) {
            super(c);
        }

        @Override
        public Shape paintLayer(final Graphics g, final int p0, final int p1, final Shape sh,
                final JTextComponent jt, final View v) {
            graphics = g;
            i0 = p0;
            i1 = p1;
            shape = sh;
            jtc = jt;
            view = v;
            return super.paintLayer(g, p0, p1, sh, jt, v);
        }

        public Shape paintAgain() {
            return paintLayer(graphics, i0, i1, shape, jtc, view);
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        jta = new JTextArea();
        jf = new JFrame();
        dh = new DefaultHighlighter();
        dp = new DefPainter(null);
        afe = new AssertionFailedError[50];
        lp = new DefaultHighlighter.DefaultHighlightPainter(Color.red);
        bWasException = false;
        s = null;
        jta.setHighlighter(dh);
        jf.getContentPane().add(jta);
        jf.setLocation(200, 300);
        jf.setSize(200, 300);
        jf.setVisible(true);
        SwingWaitTestCase.isRealized(jf);
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    void throwEx(final AssertionFailedError[] e) {
        for (int i = 0; i < e.length; i++) {
            if (e[i] != null) {
                throw e[i];
            }
        }
    }

    public void testDefaultHighlightPainter() {
        assertNotNull(lp);
        assertEquals(Color.red, lp.getColor());
        lp = new DefaultHighlighter.DefaultHighlightPainter(null);
        assertNotNull(lp);
        assertNull(lp.getColor());
    }

    public Shape getShape(final int p0, final int p1, final Shape shape, final View view) {
        Rectangle r = null;
        Rectangle shapeBounds = shape.getBounds();
        try {
            r = (Rectangle) view.modelToView(Math.min(p0, p1), Position.Bias.Forward, Math.max(
                    p0, p1), Position.Bias.Backward, shapeBounds);
        } catch (BadLocationException e) {
        }
        if (r == null) {
            return null;
        }
        return r;
    }
    /* hard link to coordinates
     public void testPaintLayer() throws Exception{
     String s1;
     String s2;
     paintCase("JTextArea",5,7,new Rectangle(28,0,12,16));
     paintCase(incString(sLTR,4) + incString(sRTL,4),3,4,
     new Rectangle(86,0,8,15));
     paintCase(incString(sLTR,4) + "\n" + incString(sRTL,4),3,4,
     new Rectangle(21,0,8,15));
     paintCase(sLTR + sRTL + "\n" + "\n" + sLTR + sRTL + sLTR,2,4,
     new Rectangle(0,15,1,15));
     paintCase("JTex\nArea for D\n\n\nefaultHighlighter testing",15,20,
     new Rectangle(0,60,11,15));
     s1 = incString(sLTR,3);
     s2 = incString(sRTL,1);
     s2 = s2 + "\n" + s2;
     paintCase(s1 + s2 + s1 + s2,15,16,new Rectangle(52,15,1,15));
     paintCase(s1 + s2 + s1 + s2,4,7,new Rectangle(40,0,1,15));
     paintCase(s1 + s2 + s1 + incString(sLTR,1) + "\n" + incString(sLTR,1),
     15,16,new Rectangle(54,15,1,15));
     paintCase(s1 + s2 + s1 + incString(sLTR,1) + "\n" + incString(sLTR,1),
     2,5,new Rectangle(46,0,7,15));

     throwEx(afe);
     }
     */
}
