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
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;

import junit.framework.TestCase;

public class SynthLookAndFeelTest extends TestCase {

    SynthLookAndFeel laf = new SynthLookAndFeel();

    JLabel l = new JLabel();

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthLookAndFeel.isNativeLookAndFeel()'
     */
    public void testIsNativeLookAndFeel() {
        assertFalse(laf.isNativeLookAndFeel());
    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthLookAndFeel.isSupportedLookAndFeel()'
     */
    public void testIsSupportedLookAndFeel() {
        assertTrue(laf.isSupportedLookAndFeel());
    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthLookAndFeel.getStyleFactory()'
     */
    public void testGetStyleFactory() {
        SynthStyleFactory sf = new SynthStyleFactory() {
            @Override
            @SuppressWarnings("unused")
            public SynthStyle getStyle(JComponent c, Region id) {
                return null;
            }
        };
        SynthLookAndFeel.setStyleFactory(sf);
        assertSame(sf, SynthLookAndFeel.getStyleFactory());
    }

//    /*
//     * Test method for
//     * 'javax.swing.plaf.synth.SynthLookAndFeel.createUI(JComponent)'
//     */
//    public void testCreateUI() {
//        l.setEnabled(false);
//        ComponentUI labelUI = SynthLookAndFeel.createUI(l);
//        assertTrue(labelUI instanceof SynthUI);
//        assertTrue(labelUI instanceof SynthLabelUI);
//        assertTrue(((SynthLabelUI) labelUI).getLabelState(l) == SynthConstants.DISABLED);
//    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthLookAndFeel.updateStyles(Component)'
     */
    public void testUpdateStyles() {
        // TODO:
    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthLookAndFeel.getRegion(JComponent)'
     */
    public void testGetRegion() {
        assertSame(Region.LABEL, SynthLookAndFeel.getRegion(l));
    }

    /*
     * Test method for 'javax.swing.plaf.synth.SynthLookAndFeel.getName()'
     */
    public void testGetName() {
        assertEquals("Synth Look and Feel", laf.getName()); //$NON-NLS-1$
    }

    /*
     * Test method for 'javax.swing.plaf.synth.SynthLookAndFeel.getID()'
     */
    public void testGetID() {
        assertEquals("Synth", laf.getID()); //$NON-NLS-1$
    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthLookAndFeel.shouldUpdateStyleOnAncestorChanged()'
     */
    public void testShouldUpdateStyleOnAncestorChanged() {
        assertFalse(laf.shouldUpdateStyleOnAncestorChanged());
    }

}
