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

public class CardLayoutRTest extends TestCase {
    Container emptyContainer;
    Dimension defSize;
    CardLayout layout;
    private Dimension maxSize;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        emptyContainer = new Container();
        defSize = new Dimension();
        maxSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        layout = new CardLayout();
    }
    
    public void testFirst() {        
        try {
            layout.first(emptyContainer);
        } catch (IllegalArgumentException iae) {
            return;            
        }
        fail("Expected IllegalArgumentException");
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
    
    public void testLast() {        
        try {
            layout.last(emptyContainer);
        } catch (IllegalArgumentException iae) {
            return;            
        }
        fail("Expected IllegalArgumentException");
    }
    
    public void testLayoutContainer() {       
        layout.layoutContainer(emptyContainer);
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
    
    public void testNext() {        
        try {
            layout.next(emptyContainer);
        } catch (IllegalArgumentException iae) {
            return;            
        }
        fail("Expected IllegalArgumentException");
    }
    
    public final void testPreferredLayoutSize() {
        assertEquals(defSize, layout.preferredLayoutSize(emptyContainer));
    }   
    
    public void testPrevious() {        
        try {
            layout.previous(emptyContainer);
        } catch (IllegalArgumentException iae) {
            return;            
        }
        fail("Expected IllegalArgumentException");
    }
    
    public void testRemoveLayoutComponent() {       
        layout.removeLayoutComponent(emptyContainer);
    }
    
}
