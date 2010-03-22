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

import java.awt.Graphics;
import java.awt.Shape;

public interface Highlighter {

    /**
     * @author Evgeniya G. Maenkova
     */
    interface Highlight {
        int getEndOffset();

        Highlighter.HighlightPainter getPainter();

        int getStartOffset();
    }

    /**
     * @author Evgeniya G. Maenkova
     */
    interface HighlightPainter {
        void paint(final Graphics g, final int p1, final int p2,
                          final Shape shape, final JTextComponent c);
    }

    Object addHighlight(final int p0, final int p1,
                               final Highlighter.HighlightPainter p)
            throws BadLocationException;

    void changeHighlight(Object obj, int p0, int p1)
            throws BadLocationException;

    void deinstall(JTextComponent c);

    Highlighter.Highlight[] getHighlights();

    void install(JTextComponent c);

    void paint(Graphics g);

    void removeAllHighlights();

    void removeHighlight(Object obj);

}

