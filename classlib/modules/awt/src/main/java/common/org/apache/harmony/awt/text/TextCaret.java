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
import java.awt.Point;

import javax.swing.text.Position;

public interface TextCaret {
    void setDot(final int dot, final Position.Bias bias);

    void moveDot(final int dot, final Position.Bias bias);

    int getDot();

    int getMark();

    Position.Bias getDotBias();

    Point getMagicCaretPosition();

    void setMagicCaretPosition(final int i1,
                                final int i2,
                                final Point p);
    void setMagicCaretPosition(final Point p);

    void paint(Graphics g);

    void setComponent(final Component c);

    AWTHighlighter getHighlighter();
}
