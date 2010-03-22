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
 * @author Dmitry A. Durnev
 */
package java.awt;

import junit.framework.TestCase;

public class BorderLayoutRTest extends TestCase {
    Container emptyContainer;
    Dimension defSize;
    BorderLayout layout;
    private Dimension maxSize;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        emptyContainer = new Container();
        defSize = new Dimension();
        maxSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        layout = new BorderLayout();
    }
    
    public final void testGetLayoutAlignmentX1() {
        layout.getLayoutAlignmentX(null);
    }
    
    public final void testGetLayoutAlignmentX2() {        
        layout.getLayoutAlignmentX(emptyContainer);
    }
    
    public final void testGetLayoutAlignmentY1() {
        layout.getLayoutAlignmentY(null);
    }
    
    public final void testGetLayoutAlignmentY2() {        
        layout.getLayoutAlignmentY(emptyContainer);
    }
    
    public final void testInvalidateLayout1() {
        layout.invalidateLayout(null);
    }
    
    public final void testInvalidateLayout2() {        
        layout.invalidateLayout(emptyContainer);
    }
    
    public final void testMaximumLayoutSize1() {
        assertEquals(maxSize, layout.maximumLayoutSize(null));
    }
    
    public final void testMaximumLayoutSize2() {        
        assertEquals(maxSize, layout.maximumLayoutSize(emptyContainer));
    }


    public final void testMinimumLayoutSize() {
        assertEquals(defSize, layout.minimumLayoutSize(emptyContainer));
    }

    public final void testPreferredLayoutSize() {
        assertEquals(defSize, layout.preferredLayoutSize(emptyContainer));
    }   
    
    /**
     * Regression for HARMONY-4085
     */
    public final void testHarmony4085() {
        final Frame f = new Frame();
        final boolean[] isInvoked = new boolean[4];

        f.add(new Component() {
            @Override
            public void setSize(int width, int height) {
                isInvoked[0] = true;
                super.setSize(width, height);
            }
        }, BorderLayout.EAST);
        f.add(new Component() {
            @Override
            public void setSize(int width, int height) {
                isInvoked[1] = true;
                super.setSize(width, height);
            }
        }, BorderLayout.WEST);
        f.add(new Component() {
            @Override
            public void setSize(int width, int height) {
                isInvoked[2] = true;
                super.setSize(width, height);
            }
        }, BorderLayout.NORTH);
        f.add(new Component() {
            @Override
            public void setSize(int width, int height) {
                isInvoked[3] = true;
                super.setSize(width, height);
            }
        }, BorderLayout.SOUTH);

        f.setSize(100, 100);
        f.setVisible(true);
        f.dispose();

        assertTrue(isInvoked[0]);
        assertTrue(isInvoked[1]);
        assertTrue(isInvoked[2]);
        assertTrue(isInvoked[3]);
    }
}
