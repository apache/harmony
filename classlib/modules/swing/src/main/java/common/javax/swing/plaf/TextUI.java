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
 * @author Sergey Burlak
 */
package javax.swing.plaf;

import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;

public abstract class TextUI extends ComponentUI {

    public abstract void damageRange(final JTextComponent a0, final int a1, final int a2);

    public abstract void damageRange(final JTextComponent a0, final int a1, final int a2,
            final Position.Bias a3, final Position.Bias a4);

    public abstract EditorKit getEditorKit(final JTextComponent a0);

    public abstract int getNextVisualPositionFrom(final JTextComponent a0, final int a1,
            final Position.Bias a2, final int a3, final Position.Bias[] a4)
            throws BadLocationException;

    public abstract View getRootView(final JTextComponent a0);

    public String getToolTipText(final JTextComponent a0, final Point a1) {

        return null;
    }

    public abstract Rectangle modelToView(final JTextComponent a0, final int a1)
            throws BadLocationException;

    public abstract Rectangle modelToView(final JTextComponent a0, final int a1,
            final Position.Bias a2) throws BadLocationException;

    public abstract int viewToModel(final JTextComponent a0, final Point a1);

    public abstract int viewToModel(final JTextComponent a0, final Point a1,
            final Position.Bias[] a2);

}

