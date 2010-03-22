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
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import java.awt.Graphics;

/**
 * Interval with unselected text.
 */
final class UnselectedTextInterval extends TextInterval {
    /**
     * Creates <em>unselected</em> text interval.
     *
     * @param start the start of the interval.
     * @param end the end of the interval.
     * @param painter the painter to use.
     */
    public UnselectedTextInterval(final int start, final int end,
                                  final TextIntervalPainter painter) {
        super(start, end, painter);
    }

    public TextInterval[] dissect(final TextInterval anotherInterval) {
        return dissect(this, anotherInterval);
    }

    public String getType() {
        return "Unselected";
    }

    public int paint(final Graphics g,
                     final int x, final int y) throws BadLocationException {
        return painter.paintUnselected(g, start, end, x, y);
    }
}