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
package org.apache.harmony.awt.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.EventListener;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.View;

public interface TextKit {
    boolean isEditable();

    void replaceSelectedText(final String text);

    TextCaret getCaret();

    Document getDocument();

    String getSelectedText();

    int getSelectionStart();

    int getSelectionEnd();

    Rectangle getVisibleRect();

    View getRootView();

    Rectangle modelToView(final int pos) throws BadLocationException;

    Rectangle modelToView(final int pos, final Position.Bias bias)
        throws BadLocationException;

    Component getComponent();

    int viewToModel(final Point p, final Position.Bias[] biasRet);

    void scrollRectToVisible(final Rectangle rect);

    boolean isScrollBarArea(final int x, final int y);

    void addCaretListeners(final EventListener listener);

    void paintLayeredHighlights(final Graphics g,
                                final int p0,
                                final int p1,
                                final Shape shape,
                                final View view);

    void revalidate();

    Color getDisabledTextColor();

    Color getSelectedTextColor();
}
