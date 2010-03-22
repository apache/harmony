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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import org.apache.harmony.awt.text.TextFieldKit;
import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class PasswordView extends FieldView {
    private static final char DEFAULT_ECHO_CHAR = '*';

    public PasswordView(final Element element) {
        super(element);
    }

    protected int drawEchoCharacter(final Graphics g,
                                    final int x,
                                    final int y,
                                    final char c) {
        g.drawString(String.valueOf(c), x, y);
        return getFontMetrics().charWidth(c) + x;
    }

    protected int drawSelectedText(final Graphics g,
                                   final int x,
                                   final int y,
                                   final int p0,
                                   final int p1) throws BadLocationException {
        return echoCharIsSet()
            ?   drawString(g, paintParams.selColor, p0, p1, x, y)
            :   super.drawSelectedText(g, x, y, p0, p1);
    }

    protected int drawUnselectedText(final Graphics g,
                                     final int x,
                                     final int y,
                                     final int p0,
                                     final int p1)
       throws BadLocationException {

        return echoCharIsSet()
            ? drawString(g, paintParams.color, p0, p1, x, y)
            : super.drawUnselectedText(g, x, y, p0, p1);
    }

    public float getPreferredSpan(final int axis) {
        if (axis == Y_AXIS || !echoCharIsSet()) {
            return super.getPreferredSpan(axis);
        } else {
            return getFontMetrics().stringWidth(getText());
        }
    }

    public Shape modelToView(final int pos,
                             final Shape shape,
                             final Position.Bias b)
       throws BadLocationException {
        if (echoCharIsSet()) {
            String text = getText();
            if (pos < 0 || pos > text.length()) {
                throw new BadLocationException(Messages.getString("swing.95", pos), //$NON-NLS-1$
                                               pos);
            }
            if (shape == null) {
                return null;
            }
            text = text.substring(0, pos);
            Rectangle rect = getOldAllocation(shape);
            FontMetrics fontMetrics = getFontMetrics();
            return new Rectangle(rect.x + fontMetrics.stringWidth(text),
                                 rect.y, 1, fontMetrics.getHeight());
        } else {
            return super.modelToView(pos, shape, b);
        }
    }

    public int viewToModel(final float x,
                           final float y,
                           final Shape shape,
                           final Position.Bias[] biasRet) {
        if (echoCharIsSet()) {
            biasRet[0] = Position.Bias.Forward;
            String text = getText();
            return getTextOffset(text, (int) x - shape.getBounds().x);
        } else {
            return super.viewToModel(x, y, shape, biasRet);
        }
    }

    private TextFieldKit getPasswordTextKit() {
        return TextUtils.getTextFieldKit(getComponent());
    }

    private boolean echoCharIsSet() {
        TextFieldKit tfk = getPasswordTextKit();
        return tfk != null && tfk.echoCharIsSet();
    }

    private char getEchoChar() {
        TextFieldKit tfk = getPasswordTextKit();
        return tfk != null ? tfk.getEchoChar() : DEFAULT_ECHO_CHAR;
    }

    private String getText() {
        TextFieldKit tfk = getPasswordTextKit();
        if (tfk == null) {
            return "";
        }
        int length = getDocument().getLength();
        char echo = tfk.getEchoChar();
        String result = "";
        for (int i = 0; i < length; i++) {
            result += echo;
        }
        return result;
    }

    private int getTextLength() {
        Document doc = getDocument();
        return doc != null ? doc.getLength() : 0;
    }

    private int drawString(final Graphics g, final Color color,
                           final int p0,
                           final int p1,
                           final int x,
                           final int y)  throws BadLocationException {
        int length = getTextLength();
        if (p0 < 0) {
            throw new BadLocationException(Messages.getString("swing.96"), p0); //$NON-NLS-1$
        } else if (p1 < p0 || p1 > length) {
            throw new BadLocationException(Messages.getString("swing.96"), p1); //$NON-NLS-1$
        }

        Color old = g.getColor();
        g.setColor(color);
        int result = x;
        char echo = getEchoChar();
        for (int i = 0; i < p1 - p0; i++) {
            result = drawEchoCharacter(g, result, y, echo);
        }
        g.setColor(old);
        return result;
    }

    private int getTextOffset(final String text,
                               final int width) {
        int index = 0;
        int length = text.length();
        while (getFontMetrics().stringWidth(text.substring(0, index)) < width) {
            if (index++ == length) {
                return length;
            }
        }
        return index;
    }
}
