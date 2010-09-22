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
 * @author Michael Danilov
 */
package java.awt.event;

import java.awt.Component;

public abstract class InputEvent extends ComponentEvent {

    private static final long serialVersionUID = -2482525981698309786L;

    public static final int SHIFT_MASK = 1;

    public static final int CTRL_MASK = 2;

    public static final int META_MASK = 4;

    public static final int ALT_MASK = 8;

    public static final int ALT_GRAPH_MASK = 32;

    public static final int BUTTON1_MASK = 16;

    public static final int BUTTON2_MASK = 8;

    public static final int BUTTON3_MASK = 4;

    public static final int SHIFT_DOWN_MASK = 64;

    public static final int CTRL_DOWN_MASK = 128;

    public static final int META_DOWN_MASK = 256;

    public static final int ALT_DOWN_MASK = 512;

    public static final int BUTTON1_DOWN_MASK = 1024;

    public static final int BUTTON2_DOWN_MASK = 2048;

    public static final int BUTTON3_DOWN_MASK = 4096;

    public static final int ALT_GRAPH_DOWN_MASK = 8192;

    static final int MASKS = SHIFT_MASK | CTRL_MASK | META_MASK | ALT_MASK
            | ALT_GRAPH_MASK | BUTTON1_MASK | BUTTON2_MASK | BUTTON3_MASK;

    static final int DOWN_MASKS = SHIFT_DOWN_MASK | CTRL_DOWN_MASK
            | META_DOWN_MASK | ALT_DOWN_MASK | ALT_GRAPH_DOWN_MASK
            | BUTTON1_DOWN_MASK | BUTTON2_DOWN_MASK | BUTTON3_DOWN_MASK;

    private long when;
    int modifiers;
    
    InputEvent(Component source, int id, long when, int modifiers) {
        super(source, id);

        this.when = when;
        this.modifiers = modifiers;
    }

    public static String getModifiersExText(int modifiers) {
        return MouseEvent.addMouseModifiersExText(
                KeyEvent.getKeyModifiersExText(modifiers), modifiers);
    }
    
    public int getModifiers() {
        return modifiers & MASKS;
    }

    public int getModifiersEx() {
    	return modifiers & DOWN_MASKS;
    }

    public boolean isAltDown() {
        return ((modifiers & ALT_DOWN_MASK) != 0);
    }

    public boolean isAltGraphDown() {
        return ((modifiers & ALT_GRAPH_DOWN_MASK) != 0);
    }

    public boolean isControlDown() {
        return ((modifiers & CTRL_DOWN_MASK) != 0);
    }

    public boolean isMetaDown() {
        return ((modifiers & META_DOWN_MASK) != 0);
    }

    public boolean isShiftDown() {
        return ((modifiers & SHIFT_DOWN_MASK) != 0);
    }

    public long getWhen() {
        return when;
    }

    @Override
    public void consume() {
        super.consume();
    }

    @Override
    public boolean isConsumed() {
        return super.isConsumed();
    }
}
