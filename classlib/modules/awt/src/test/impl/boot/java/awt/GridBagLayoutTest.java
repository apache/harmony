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
package java.awt;
@SuppressWarnings("serial")
public class GridBagLayoutTest extends AWTTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GridBagLayoutTest.class);
    }

    class TestButton extends Button {
        TestButton(String name) {
            super(name);
        }
    }

    private final TestButton first, second;
    private GridBagLayout layout;
    private Frame frame;

    public GridBagLayoutTest(String arg0) {
        super(arg0);

        first = new TestButton("First");
        first.setFont(new Font("dialog", Font.PLAIN, 50));
        second = new TestButton("Second");
        second.setFont(new Font("dialog", Font.PLAIN, 50));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        frame = new Frame();
        layout = new GridBagLayout();
        frame.setLayout(layout);
        frame.setVisible(true);
    }

    @Override
    protected void tearDown() throws Exception {
        frame.dispose();

        super.tearDown();
    }


    private void assertEquals(GridBagConstraints gbc1, GridBagConstraints gbc2) {
        assertNotNull(gbc1);
        assertNotNull(gbc2);
        assertEquals("gridx", gbc1.gridx, gbc2.gridx);
        assertEquals("gridy", gbc1.gridy, gbc2.gridy);
        assertEquals("gridwidth", gbc1.gridwidth, gbc2.gridwidth);
        assertEquals("gridheight", gbc1.gridheight, gbc2.gridheight);
        assertTrue("weightx", gbc1.weightx == gbc2.weightx);
        assertTrue("weighty", gbc1.weighty == gbc2.weighty);
        assertEquals("anchor", gbc1.anchor, gbc2.anchor);
        assertEquals("fill", gbc1.fill, gbc2.fill);
        assertEquals("insets", gbc1.insets, gbc2.insets);
        assertEquals("ipadx", gbc1.ipadx, gbc2.ipadx);
        assertEquals("ipady", gbc1.ipady, gbc2.ipady);
    }

    public final void testToString() {
        assertTrue(new String("java.awt.GridBagLayout").equals(layout.toString()));
    }

    public final void testAddLayoutComponentStringComponent() {
        layout.addLayoutComponent((String)null, first);
        assertEquals(0, layout.comptable.size());
        layout.addLayoutComponent("", first);
        assertEquals(0, layout.comptable.size());
        layout.addLayoutComponent("q", first);
        assertEquals(0, layout.comptable.size());
    }

    public final void testAddLayoutComponentComponentObject() {
        GridBagConstraints c = new GridBagConstraints();

        frame.add(first, c);
        assertEquals(c, layout.getConstraints(first));
        c.fill = GridBagConstraints.BOTH;
        frame.add(second, c);
        assertEquals(c, layout.getConstraints(second));
        frame.remove(second);
        assertNull(layout.comptable.get(second));
        assertEquals(layout.defaultConstraints, layout.getConstraints(second));
        frame.remove(first);
        assertNull(layout.comptable.get(first));
        assertEquals(layout.defaultConstraints, layout.getConstraints(first));


        frame.add(first, c);
        assertEquals(c, layout.getConstraints(first));

        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.HORIZONTAL;
        layout.addLayoutComponent(first, c1);
        assertEquals(c1, layout.getConstraints(first));
        // null constraints don't replace old:
        layout.addLayoutComponent(first, null);
        assertEquals(c1, layout.getConstraints(first));

        boolean notCons = false;
        try {
            frame.add(first, new Integer(3));
        } catch (IllegalArgumentException e) {
            notCons = true;
        }
        assertTrue(notCons);
    }

    public final void testRemoveLayoutComponent() {
        frame.add(first);
        frame.add(second);
        frame.remove(second);
        frame.add(second);
        frame.remove(first);
        frame.remove(second);
    }

    public final void testGetConstraints() {
        GridBagConstraints c = layout.defaultConstraints;
        assertEquals(c.gridx, GridBagConstraints.RELATIVE);
        assertEquals(c.gridy, GridBagConstraints.RELATIVE);
        assertEquals(c.gridwidth, 1);
        assertEquals(c.gridheight, 1);
        assertTrue(c.weightx == 0.);
        assertTrue(c.weighty == 0.);
        assertEquals(c.anchor, GridBagConstraints.CENTER);
        assertEquals(c.fill, GridBagConstraints.NONE);
        assertEquals(c.insets.top, 0);
        assertEquals(c.ipadx, 0);
        assertEquals(c.ipady, 0);

        layout.defaultConstraints.fill = GridBagConstraints.BOTH;
        assertEquals(layout.defaultConstraints, layout.getConstraints(first));

    }

    public final void testSetConstraints() {
        GridBagConstraints c = new GridBagConstraints();

        frame.add(first, c);

        c.gridx = -100;
        layout.setConstraints(second, c);
        assertNotNull("new component was added to comptable",
                      layout.comptable.get(second));
        assertEquals(c.gridx, layout.getConstraints(second).gridx);
    }

    public final void testMinimumLayoutSize() {
        Dimension expected;
        Insets insets = frame.getInsets();
        expected = new Dimension(insets.left + insets.right,
                                 insets.top + insets.bottom);
        assertEquals(expected, layout.minimumLayoutSize(frame));

        frame.add(first);
        expected = new Dimension(first.getMinimumSize());
        expected.height += insets.bottom + insets.top;
        expected.width += insets.left + insets.right;
        assertEquals(expected, layout.minimumLayoutSize(frame));
    }

    public final void testPreferredLayoutSize() {
        Insets insets = frame.getInsets();
        Dimension expected = new Dimension(insets.left + insets.right,
                                           insets.top + insets.bottom);
        assertEquals(expected, layout.minimumLayoutSize(frame));

        frame.add(first);
        expected = new Dimension(first.getPreferredSize());
        expected.height += insets.bottom + insets.top;
        expected.width += insets.left + insets.right;
        assertEquals(expected, layout.preferredLayoutSize(frame));
    }

    public final void testMaximumLayoutSize() {
        assertEquals(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE),
                     layout.maximumLayoutSize(frame));
    }

    public final void testGetLayoutDimensions() {
        frame.add(first);
        frame.validate();
        frame.setSize(frame.getPreferredSize());
        frame.validate();

        int dims[][] = layout.getLayoutDimensions();
        assertEquals(first.getPreferredSize().width, dims[0][0]);
        assertEquals(first.getPreferredSize().height, dims[1][0]);
        assertEquals(0, dims[0][1]);
        assertEquals(0, dims[1][1]);
    }

    public final void testGetLayoutWeights() {
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.;
        c.weighty = .5;
        frame.add(first, c);
        frame.validate();

        double ws[][] = layout.getLayoutWeights();
        assertTrue(ws[0][0] == 1.);
        assertTrue(ws[1][0] == .5);
        assertTrue(ws[0][1] == 0.);
        assertTrue(ws[1][1] == 0.);
    }

    public final void testGetLayoutOrigin() {
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.;
        c.weighty = 1.;
        frame.add(first, c);
        frame.setSize(frame.getPreferredSize());
        frame.validate();

        Insets insets = frame.getInsets();
        assertEquals(new Point(insets.left, insets.top), layout.getLayoutOrigin());
    }

    public final void testLocation() {
        GridBagConstraints c = new GridBagConstraints();
        frame.add(first, c);
        frame.add(second, c);
        frame.validate();
        frame.setSize(frame.getPreferredSize());
        frame.validate();

        Insets insets = frame.getInsets();
        int x = insets.left + first.getPreferredSize().width + 1;
        assertEquals(new Point(1, 0), layout.location(x, insets.top + 1));
    }

    public final void testLayoutContainer() {
        GridBagConstraints c = new GridBagConstraints();
        Insets insets = frame.getInsets();

        //Default constraints
        frame.add(first);
        frame.setSize(frame.getPreferredSize());
        frame.validate();
        Rectangle expected = new Rectangle(new Point(insets.left, insets.top),
                                           first.getPreferredSize());
        assertEquals(expected, first.getBounds());

        //Internal padding
        c.ipadx = 10;
        c.ipady = 20;
        layout.setConstraints(first, c);
        doLayout();

        expected.setSize(first.getPreferredSize());
        expected.width += c.ipadx;
        expected.height += c.ipady;
        assertEquals(expected, first.getBounds());

        //External
        c = new GridBagConstraints();
        c.insets = new Insets(10, 20, 30, 40);
        layout.setConstraints(first, c);
        doLayout();
        expected.setSize(first.getPreferredSize());
        expected.x += c.insets.left;
        expected.y += c.insets.top;
        assertEquals(expected, first.getBounds());

        //Fill
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        frame.setSize(frame.getPreferredSize().width + 10,
                      frame.getPreferredSize().height + 20);
        frame.validate();
        expected.setLocation(insets.left + 5, insets.top + 10);
        expected.setSize(first.getPreferredSize());
        assertEquals(expected, first.getBounds());

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.setLocation(insets.left, insets.top);
        expected.width += 10;
        expected.height += 20;
        assertEquals(expected, first.getBounds());

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.y += 10;
        expected.height = first.getPreferredSize().height;
        assertEquals(expected, first.getBounds());

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.x += 5;
        expected.y = insets.top;
        expected.setSize(first.getPreferredSize());
        expected.height += 20;
        assertEquals(expected, first.getBounds());

        //anchor
        c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;

        c.anchor = GridBagConstraints.CENTER;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.setLocation(insets.left, insets.top);
        expected.translate(5, 10);
        expected.setSize(first.getPreferredSize());
        assertEquals(expected, first.getBounds());

        c.anchor = GridBagConstraints.NORTH;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.translate(0, -10);
        assertEquals(expected, first.getBounds());

        c.anchor = GridBagConstraints.NORTHEAST;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.translate(5, 0);
        assertEquals(expected, first.getBounds());

        c.anchor = GridBagConstraints.EAST;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.translate(0, 10);
        assertEquals(expected, first.getBounds());

        c.anchor = GridBagConstraints.SOUTHEAST;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.translate(0, 10);
        assertEquals(expected, first.getBounds());

        c.anchor = GridBagConstraints.SOUTH;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.translate(-5, 0);
        assertEquals(expected, first.getBounds());

        c.anchor = GridBagConstraints.SOUTHWEST;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.translate(-5, 0);
        assertEquals(expected, first.getBounds());

        c.anchor = GridBagConstraints.WEST;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.translate(0, -10);
        assertEquals(expected, first.getBounds());

        c.anchor = GridBagConstraints.NORTHWEST;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.translate(0, -10);
        assertEquals(expected, first.getBounds());

        //weights
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.setLocation(insets.left, insets.top);
        expected.width += 10;
        expected.height += 20;
        assertEquals(expected, first.getBounds());
        c.weightx = 0;
        c.weighty = 0;
        layout.setConstraints(first, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        expected.setSize(first.getPreferredSize());
        expected.translate(5, 10);
        assertEquals(expected, first.getBounds());

        //coords
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        layout.setConstraints(first, c);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = GridBagConstraints.RELATIVE;
        frame.add(second, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        frame.setSize(frame.getPreferredSize().width,
                      frame.getPreferredSize().height);
        frame.validate();
        expected.setLocation(insets.left, insets.top);
        assertEquals(expected, first.getBounds());
        expected.translate(first.getPreferredSize().width, 0);
        expected.setSize(second.getPreferredSize());
        assertEquals(expected, second.getBounds());

        c.gridx = 1;
        c.gridy = 1;
        layout.setConstraints(second, c);
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        frame.setSize(frame.getPreferredSize().width,
                      frame.getPreferredSize().height);
        frame.validate();
        expected.translate(0, first.getPreferredSize().height);
        assertEquals(expected, second.getBounds());

        //size
        frame.remove(first);
        frame.remove(second);
        layout = new GridBagLayout();
        frame.setLayout(layout);

        Button third = new Button("Third");
        third.setFont(new Font("dialog", Font.PLAIN, 50));

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = GridBagConstraints.RELATIVE;
        frame.add(first, c);
        c.gridheight = GridBagConstraints.REMAINDER;
        frame.add(second, c);
        c.gridheight = 3;
        frame.add(third, c);
        frame.setSize(frame.getPreferredSize().width,
                      frame.getPreferredSize().height);
        frame.validate();
        Point p = new Point(insets.left, insets.top);
        assertEquals(p, first.getLocation());
        p.translate(0, first.getHeight());
        assertEquals(p, second.getLocation());
        p.translate(first.getWidth(), -first.getHeight());
        assertEquals(p, third.getLocation());

    }

    private void doLayout() {
        layout.invalidateLayout(frame);
        layout.layoutContainer(frame);
        frame.setSize(frame.getPreferredSize());
        frame.validate();
    }

}
