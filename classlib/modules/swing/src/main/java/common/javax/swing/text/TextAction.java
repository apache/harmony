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
 * @author Alexander T. Simbirtsev, Evgeniya G. Maenkova
 */

package javax.swing.text;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.Action;

import org.apache.harmony.awt.text.AWTTextAction;
import org.apache.harmony.awt.text.ActionSet;
import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.text.TextUtils;

public abstract class TextAction extends AbstractAction {
    public static final Action[] augmentList(
            final Action[] list1, final Action[] list2) {
        Comparator comparator = new Comparator() {
            public int compare(final Object item1, final Object item2) {
                return ((String)item2).compareTo((String)item1);
            }
        };

        TreeMap map = new TreeMap(comparator);
        if (list2 != null) {
            for (int i = 0; i < list2.length; i++) {
                map.put(list2[i].getValue(Action.NAME), list2[i]);
            }
        }
        if (list1 != null) {
            for (int i = 0; i < list1.length; i++) {
                String name = (String)list1[i].getValue(Action.NAME);
                if (!map.containsKey(name)) {
                    map.put(name, list1[i]);
                }
            }
        }

        return (Action[])(map).values().toArray(new Action[map.size()]);
    }

    public TextAction(final String name) {
        putValue(Action.NAME, name);
    }

    protected final JTextComponent getFocusedComponent() {
        return JTextComponent.getLastFocusedTextComponent();
    }

    protected final JTextComponent getTextComponent(final ActionEvent e) {
        if (e != null) {
            Object eventSource = e.getSource();
            if (eventSource instanceof JTextComponent) {
                return (JTextComponent)eventSource;
            }
        }

        return getFocusedComponent();
    }

    final JTextComponent getEditableTextComponent(final ActionEvent e) {
        JTextComponent c = getTextComponent(e);
        return (c != null && c.isEditable()) ? c : null;
    }

    /**
     * sets new value to dot or mark of given component's caret
     * depending on <code>isMovingCaret</code> value
     */
    final void changeCaretPosition(
            final JTextComponent component, final int newPos,
            final boolean isMovingCaret) {
        changeCaretPosition(component, newPos, isMovingCaret, Position.Bias.Forward);
    }

    /**
     * sets new value to dot or mark of given component's caret
     * depending on <code>isMovingCaret</code> value
     */
    final void changeCaretPosition(
            final JTextComponent component, final int newPos,
            final boolean isMovingCaret, final Position.Bias newBias) {
        TextKit textKit = TextUtils.getTextKit(component);
        TextUtils.changeCaretPosition(textKit, newPos, isMovingCaret, newBias);
    }

    /**
     * determines and sets value to given component's caret magic position
     *
     * @param source - component which caret will be modified
     * @param pos - caret position
     * @param direction in which caret is moving one of
     * <code>SwingConstants</code> values
     * @param oldPoint - current magic position value
     * @throws BadLocationException
     */
    final void setMagicPosition(
            final JTextComponent source, final int pos,
            final int direction, final Point oldPoint)
            throws BadLocationException {
        TextKit textKit = TextUtils.getTextKit(source);
        textKit.getCaret().setMagicCaretPosition(pos, direction, oldPoint);
    }

    final void setCurrentPositionAsMagic(final JTextComponent c) {
        TextUtils.setCurrentPositionAsMagic(TextUtils.getTextKit(c));
    }

    AWTTextAction action;

    TextAction(final String name, final boolean findAWTTextAction) {
        super(name);
        this.action = (AWTTextAction)ActionSet.actionMap.get(name);
    }

    final void performTextAction(final ActionEvent e) {
        JTextComponent source = getTextComponent(e);
        if (source != null) {
            action.performAction(TextUtils.getTextKit(source));
        }
    }
}