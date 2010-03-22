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
import java.awt.Point;

import javax.swing.event.ChangeListener;

public interface Caret {

    void addChangeListener(ChangeListener listener);

    void deinstall(JTextComponent c);

    int getBlinkRate();

    int getDot();

    Point getMagicCaretPosition();

    int getMark();

    void install(JTextComponent c);

    boolean isSelectionVisible();

    boolean isVisible();

    void moveDot(int dot);

    void paint(Graphics g);

    void removeChangeListener(ChangeListener l);

    void setBlinkRate(int rate);

    void setDot(int dot);

    void setMagicCaretPosition(Point p);

    void setSelectionVisible(boolean v);

    void setVisible(boolean v);
}