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
package java.awt;


import javax.swing.DebugGraphics;
import javax.swing.JButton;

import junit.framework.TestCase;


public class ContainerRTest extends TestCase {

    public final void testRemoveComponent() {
        // Regression test for HARMONY-1476
        try {
            new Container().remove((Component) null);
            fail("NPE was not thrown");
        } catch (NullPointerException ex) {
            // passed
        }
    }

    public final void testSetFocusTraversalKeys() {
        try {
            Button btn = new Button();
            btn
                    .setFocusTraversalKeys(
                            KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
                            new Container()
                                    .getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    @SuppressWarnings("serial")
    public final void testAddNotify() {
        Container c1 = new Container(){};
        Container c2 = new Container(){};
        assertFalse(c1.isDisplayable());
        assertFalse(c2.isDisplayable());

        c1.add(c2);
        c1.addNotify();

        assertTrue(c1.isDisplayable());
        assertTrue(c2.isDisplayable());
    }
    
    public final void testPaint() {
        // Regression test for HARMONY-2527
        final Component c = new Frame();
        c.paint(c.getGraphics());
        // End of regression for HARMONY-2527
        
        // Regression for HARMONY-3443
        final Graphics g;
        final Frame f = new Frame();

        f.add(new JButton());
        f.setVisible(true);
        g = f.getGraphics();

        try {
            g.setClip(null);
            assertNull(g.getClip());
            f.paint(g);
        } finally {
            f.dispose();
        }
        // End of regression for HARMONY-3443
        
        // Regression for HARMONY-3430
        new Container().paint(new DebugGraphics());
        // End of regression for HARMONY-3430
    }

    public void testAddComponent() {
        try {         
            Container s = new Container(); 
            s.add((Component) null); 
            fail("NPE should be thrown"); 
        } catch (NullPointerException npe) {    
            // PASSED            
        }
    }

    // regression for HARMONY-2443
    public void testPaintComponentsImpl() {
        ScrollPane scp = new ScrollPane();

        scp.printComponents((Graphics) null);
    }

}
