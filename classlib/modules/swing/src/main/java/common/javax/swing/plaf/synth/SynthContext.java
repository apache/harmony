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

package javax.swing.plaf.synth;

import javax.swing.JComponent;

public class SynthContext {

    private JComponent component;

    private SynthStyle style;

    private int state;

    private Region region;

    /**
     * Constructs SynthContext with given parameters
     */
    public SynthContext(JComponent component, Region region, SynthStyle style,
            int state) {
        this.component = component;
        this.style = style;
        this.state = state;
        this.region = region;
    }

    /**
     * For the most of UIs context needed to represent style and state
     */
    SynthContext(SynthStyle style, int state) {
        this.style = style;
        this.state = state;
    }

    /**
     * Sometimes context just represents currentState
     */
    SynthContext(int state) {
        this.state = state;
    }

    /**
     * @return The component uses this context
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * @return The state for the component uses this context
     */
    public int getComponentState() {
        return state;
    }

    /** @return parent style */
    public SynthStyle getStyle() {
        return style;
    }

    /** @return the region of the context */
    public Region getRegion() {
        return region;
    }

    void setState(int state) {
        this.state = state;
    }

    /**
     * Removes proposed state from current if it exists
     */
    void lossState(int proposed) {

        this.state &= ~proposed;
    }

    /**
     * Adds additional state to current if it doesn't exist
     */
    void gainState(int proposed) {
        this.state |= proposed;
    }

    static boolean isEnabled(int verifiedState) {
        return (verifiedState & SynthConstants.ENABLED) != 0;
    }

    static boolean isDisabled(int verifiedState) {
        return (verifiedState & SynthConstants.DISABLED) != 0;
    }

    static boolean isFocused(int verifiedState) {
        return (verifiedState & SynthConstants.FOCUSED) != 0;
    }

    static boolean isDefault(int verifiedState) {
        return (verifiedState & SynthConstants.DEFAULT) != 0;
    }

    static boolean isMouseOver(int verifiedState) {
        return (verifiedState & SynthConstants.MOUSE_OVER) != 0;
    }

    static boolean isPressed(int verifiedState) {
        return (verifiedState & SynthConstants.PRESSED) != 0;
    }

    static boolean isSelected(int verifiedState) {
        return (verifiedState & SynthConstants.SELECTED) != 0;
    }
    
    /**
     * Used in UI's. Created to reduce code doubling
     * */
    static int getCommonComponentState(JComponent c) {
        int result = c.isEnabled() ? SynthConstants.ENABLED
                : SynthConstants.DISABLED;
        if (c.isFocusOwner()) {
            result |= SynthConstants.FOCUSED;
        }
        return result;
    }
}
