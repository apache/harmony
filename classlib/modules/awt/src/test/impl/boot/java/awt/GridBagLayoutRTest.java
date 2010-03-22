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

public class GridBagLayoutRTest extends TestCase {
    
    static Panel buttonsPanel = new Panel();
    static Panel tabbedPane = new Panel();

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GridBagLayoutRTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public final void testLayoutContainer() {
        Panel dialogPanel = getDialogPanel();
        dialogPanel.setSize(542, 444);
        dialogPanel.doLayout();
        double maxX = buttonsPanel.getBounds().getMaxX();
        assertEquals(maxX, tabbedPane.getBounds().getMaxX(), 0.0);


    }
    
    private static Panel getDialogPanel() {
        GridBagConstraints gridBagConstraints182=new GridBagConstraints();
        GridBagConstraints gridBagConstraints172=new GridBagConstraints();
        Panel dialogPanel=new Panel();
        dialogPanel.setLayout(new GridBagLayout());
        dialogPanel.setSize(532, 389);
        gridBagConstraints172.gridx=0;
        gridBagConstraints172.gridy=1;
        gridBagConstraints172.anchor=java.awt.GridBagConstraints.EAST;
        gridBagConstraints172.gridwidth=3;
        gridBagConstraints182.gridx=0;
        gridBagConstraints182.gridy=0;
        gridBagConstraints182.weightx=1.0;
        gridBagConstraints182.weighty=1.0;
        gridBagConstraints182.fill=java.awt.GridBagConstraints.BOTH;
        buttonsPanel.setPreferredSize(new java.awt.Dimension(170, 40));
        tabbedPane.setPreferredSize(new java.awt.Dimension(100, 100));
        dialogPanel.add(tabbedPane, gridBagConstraints182);
        dialogPanel.add(buttonsPanel, gridBagConstraints172);
        return dialogPanel;
    }
    
    public final void testPreferredLayoutSize() {
        GridBagLayout gbl = new GridBagLayout();
        Panel p = new Panel(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        Button big = new Button("Big button");
        Button b1 = new Button("b1"); 
        Button b2 = new Button("b2"); 
        big.setPreferredSize(new Dimension(500, 15));
        big.setMinimumSize(new Dimension(500, 15));
        b1.setPreferredSize(new Dimension(50, 15));
        b1.setMinimumSize(new Dimension(30, 10));
        b2.setPreferredSize(new Dimension(50, 15));
        b2.setMinimumSize(new Dimension(30, 10));
        p.add(big, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0.75;
        p.add(b1, gbc);
        gbc.weightx = 0.25;
        p.add(b2, gbc);        
        assertEquals(big.getPreferredSize().width, 
                     gbl.preferredLayoutSize(p).width);
        assertEquals(2 * b1.getPreferredSize().height, 
                      gbl.preferredLayoutSize(p).height);
        
          
    }
    
    public final void testAddLayoutComponent() {
        GridBagLayout gbl = new GridBagLayout();
        try {
            gbl.addLayoutComponent(null, new Object());
        } catch (IllegalArgumentException iae) {            
            return;
        }
        fail();
    }
    
    public final void testInvalidate() {
        GridBagLayout gbl = new GridBagLayout();
        gbl.invalidateLayout(null);
    }
    
    static void addToGridBag(Panel panel, Component comp, int x, int y, int w,
                             int h, double weightx, double weighty) {
        GridBagLayout gbl = (GridBagLayout) panel.getLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.gridheight = h;
        c.weightx = weightx;
        c.weighty = weighty;
        panel.add(comp);
        gbl.setConstraints(comp, c);
    }
    
    public final void testZeroWeights() {
        Dimension minSize = new Dimension(10, 25);
        final Panel p = new Panel(new GridBagLayout());
        addToGridBag(p, new Button(), 0, 0, 1, 1, 0, 0);
        addToGridBag(p, new Button(), 0, 1, 1, 1, 0, 0);
        addToGridBag(p, new Button(), 0, 2, 1, 1, 0, 0);
        for (int i=0; i < p.getComponentCount(); i++) {
            p.getComponent(i).setMinimumSize(minSize);
        }
        Dimension size = new Dimension(minSize);
        size.height -= 15;
        p.setSize(size);
        p.doLayout();
        assertEquals("first component has zero height", 0,
                     p.getComponent(0).getHeight());
        assertEquals("second component has height < min", 18,
                     p.getComponent(1).getHeight());
        assertEquals("third component has min height", minSize.height,
                     p.getComponent(2).getHeight());
    }
}
