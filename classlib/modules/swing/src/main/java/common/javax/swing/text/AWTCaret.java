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

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.PeriodicTimer;
import org.apache.harmony.awt.text.AWTHighlighter;
import org.apache.harmony.awt.text.TextCaret;




final class AWTCaret extends DefaultCaret implements TextCaret {
    AWTHighlighter highlighter = new AWTHighlighter();

    public AWTCaret() {
        setBlinkRate(getCaretBlinkRate());
    }

    public void setMagicCaretPosition(final int pos, final int direction, final Point oldPoint) {
        super.setMagicCaretPosition(pos, direction, oldPoint);
    }

    public AWTHighlighter getHighlighter() {
        return highlighter;
    }

    public Bias getDotBias() {
        return super.getDotBias();
    }

    public void moveDot(final int pos, final Bias b) {
        super.moveDot(pos, b);
    }

    public void setDot(final int pos, final Bias b) {
        super.setDot(pos, b);

    }

    public void setComponent(final Component c) {
        super.setComponent(c);
        highlighter.setComponent(c);
        textKit.addCaretListeners(this);
    }

    Object createTimer(final boolean isMagicTimer, final int delay) {
        return isMagicTimer ? new PeriodicTimer(DEFAULT_MAGIC_DELAY,
                                                (Runnable)getMagicAction())
            :  new PeriodicTimer(getCaretBlinkRate(),
                                 (Runnable)getBlinkAction());
    }

    void startTimer(final Object timer) {
         ((PeriodicTimer)timer).start();
    }

    void setTimerDelay(final Object timer, final int delay) {
    }

    void stopTimer(final Object timer) {
        ((PeriodicTimer)timer).stop();
    }

    Object getMagicAction() {
        if (magicAction == null) {
            magicAction = new Runnable() {
                public void run() {
                    if (magicCaretPosition == null) {
                         magicCaretPosition = new Point(x, y);
                    }
                }
            };
        }
        return magicAction;
    }

    Object getBlinkAction() {
        if (blinkAction == null) {
            blinkAction = new Runnable() {
                public void run() {
                    shouldDraw = !shouldDraw;
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            repaint();
                        }
                    });
                }
            };
       }
       return blinkAction;
    }

    boolean isRestoreSelectionCondition(final Component c) {
        return false;
    }

    private int getCaretBlinkRate() {
        Object blinkRateObj = Toolkit.getDefaultToolkit()
            .getDesktopProperty("awt.cursorBlinkRate");
        return blinkRateObj instanceof Integer
           ? ((Integer)blinkRateObj).intValue() : 500;
    }

    Color getCaretColor() {
        return Color.BLACK;
    }

    Color getSelectionColor() {
        return SystemColor.textHighlight;
    }

    boolean isComponentEditable() {
        return true;
    }

    boolean isDragEnabled() {
        return false;
    }

    Object addHighlight(final int p0, final int p1) {
        Object result = null;
        try {
           result = highlighter.addHighlight(p0, p1);
        } catch (BadLocationException e) {
        }
        return result;
    }

    void changeHighlight(final Object tag, final int p0, final int p1) {
        try {
             highlighter.changeHighlight(p0, p1);
        } catch (final BadLocationException e) {
        }
    }

    void removeHighlight(final Object tag) {
        highlighter.removeHighlight();
    }

    public void mouseClicked(final MouseEvent me) {
        if (textKit.isScrollBarArea(me.getX(), me.getY())) {
            return;
        }
        super.mouseClicked(me);
    }

    public void mouseDragged(final MouseEvent me) {
        if (textKit.isScrollBarArea(me.getX(), me.getY())) {
            return;
        }
        super.mouseDragged(me);
    }

    public void mousePressed(final MouseEvent me) {
        if (textKit.isScrollBarArea(me.getX(), me.getY())) {
            return;
        }
        super.mousePressed(me);
    }
}

