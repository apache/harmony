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

public class GridLayoutRTest extends TestCase {
    Container emptyContainer;
    Dimension defSize;
    GridLayout layout;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GridLayoutRTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        emptyContainer = new Container();
        defSize = new Dimension();
        layout = new GridLayout();
    }

    public final void testMinimumLayoutSize() {
        assertEquals(defSize, layout.minimumLayoutSize(emptyContainer));
    }

    public final void testPreferredLayoutSize() {
        assertEquals(defSize, layout.preferredLayoutSize(emptyContainer));
    }
    
    public final void testLayoutContainer() {        
        layout.layoutContainer(emptyContainer);
    }
    
    public void testRemoveLayoutComponent() {        
        layout.removeLayoutComponent(emptyContainer);
    }

}
