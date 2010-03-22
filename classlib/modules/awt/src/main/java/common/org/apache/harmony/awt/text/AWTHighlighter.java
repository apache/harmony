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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.SystemColor;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.View;

import org.apache.harmony.awt.internal.nls.Messages;

/**
 *
 * That's a simple highlighter for a text component.
 */
public class AWTHighlighter {
    private Component component;
    private TextKit textKit;
    private Position start;
    private Position end;
    private Document document;

    public void setComponent(final Component component) {
        this.component = component;
        textKit = TextUtils.getTextKit(component);
        document = textKit.getDocument();
    }

    public Object addHighlight(final int p0, final int p1)
       throws BadLocationException {
        if (p0 < 0 || p1 < p0 || p1 > getDocumentLength()) {
            // awt.29=Invalid range
            throw new BadLocationException(Messages.getString("awt.29"), 0); //$NON-NLS-1$
        }
        start = document.createPosition(p0);
        end = document.createPosition(p1);
        repaintComponent(TextUtils.getBoundsByOffsets(textKit, p0, p1));
        return Boolean.TRUE;
    }

    public void changeHighlight(final int p0, final int p1)
       throws BadLocationException {
        if (p0 < 0 || p1 < p0 || p1 > getDocumentLength()) {
            // awt.29=Invalid range
            throw new BadLocationException(Messages.getString("awt.29"), 0); //$NON-NLS-1$
        }
        int oldStart = getStartOffset();
        int oldEnd = getEndOffset();
        start = document.createPosition(p0);
        end = document.createPosition(p1);
        evaluateBounds(oldStart, oldEnd, p0, p1);
    }

    public void removeHighlight() {
        repaintComponent(TextUtils.getBoundsByOffsets(textKit,
                                                      getStartOffset(),
                                                      getEndOffset()));
        start = null;
        end = null;
    }

    public void paintLayeredHighlights(final Graphics g, final int p0,
                                       final int p1, final Shape viewBounds,
                                       final View view) {
        if (start == null || end == null) {
            return;
        }
         int startOffset = getStartOffset();
         int endOffset = getEndOffset();

         if (endOffset > getDocumentLength() || startOffset > p1
                || endOffset < p0) {
             return;
         }
         TextUtils.paintLayer(g, Math.max(p0, startOffset),
                              Math.min(p1, endOffset), viewBounds,
                              SystemColor.textHighlight, view, true);
    }

    private int getDocumentLength() {
        return textKit.getDocument().getLength();
    }

    private int getStartOffset() {
        return start.getOffset();
    }

    private int getEndOffset() {
        return end.getOffset();
    }

    private void repaintComponent(final Rectangle r) {
        if (r != null) {
            component.repaint(0, r.x, r.y, r.width, r.height);
        }
    }

    private void evaluateBounds(final int oldStart, final int oldEnd,
                                final int newStart, final int newEnd) {
      if (oldEnd < newStart || oldStart > oldEnd) {
          repaintComponent(oldStart, oldEnd, newStart, newEnd);
      } else {
          repaintComponent(Math.min(oldStart, newStart),
                           Math.max(oldStart, newStart),
                           Math.min(oldEnd, newEnd),
                           Math.max(oldEnd, newEnd));
      }
  }

  private void repaintComponent(final int p0, final int p1,
                                final int p2, final int p3) {
      repaintComponent(TextUtils.getBoundsByOffsets(textKit, p0, p1));
      repaintComponent(TextUtils.getBoundsByOffsets(textKit, p2, p3));
  }
}
