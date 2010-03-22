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

import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;

import junit.framework.TestCase;

public class SynthContextTest extends TestCase {

    private static final SynthStyle ss = new SynthStyle() {
        @Override
        @SuppressWarnings("unused")//$NON-NLS-1$
        protected Font getFontForState(SynthContext context) {
            return null;
        }

        @Override
        @SuppressWarnings("unused")//$NON-NLS-1$
        protected Color getColorForState(SynthContext context, ColorType type) {
            return Color.RED;
        }
    };

    private static final String BUTTON_NAME = "testJButton"; //$NON-NLS-1$

    private static final JButton testButton = new JButton(BUTTON_NAME);

    private static final SynthContext sc = new SynthContext(testButton,
            Region.BUTTON, ss, SynthConstants.ENABLED);

    public static void testFields() {
        assertSame(Region.BUTTON, sc.getRegion());
        assertSame(ss, sc.getStyle());
        assertSame(Color.RED, sc.getStyle().getColorForState(sc, null));
        assertTrue(sc.getComponentState() == SynthConstants.ENABLED);
        assertSame(testButton, sc.getComponent());
    }

    public static void testStates() {

        assertTrue(SynthContext.isEnabled(sc.getComponentState()));
        sc.setState(SynthConstants.DISABLED);
        assertFalse(SynthConstants.ENABLED == sc.getComponentState());
        assertFalse(SynthContext.isEnabled(sc.getComponentState()));
        assertTrue(SynthContext.isDisabled(sc.getComponentState()));

        sc.gainState(SynthConstants.DISABLED);
        assertTrue(SynthConstants.DISABLED == sc.getComponentState());

        sc.gainState(SynthConstants.FOCUSED);
        assertTrue(SynthContext.isDisabled(sc.getComponentState()));
        assertTrue(SynthContext.isFocused(sc.getComponentState()));

        assertFalse(SynthContext.isMouseOver(sc.getComponentState()));
        sc.lossState(SynthConstants.MOUSE_OVER);
        assertFalse(SynthContext.isMouseOver(sc.getComponentState()));

        sc.lossState(SynthConstants.FOCUSED);
        assertFalse(SynthContext.isFocused(sc.getComponentState()));

        sc.setState(SynthConstants.ENABLED);
        assertTrue(SynthContext.isEnabled(sc.getComponentState()));

    }

}
