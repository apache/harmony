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

public class GridLayoutTest extends AWTTestCase {

    @SuppressWarnings("serial")
    class TestButton extends Button {
        TestButton(String name, Dimension min, Dimension pref) {
            super(name);

            setMinimumSize(min);
            setPreferredSize(pref);
        }
    }

    private final int MIN_SIZE = 50;
    private final int PREF_SIZE = 100;
    private final TestButton b1, b2, b3, b4, b5, b6;
    private GridLayout layout;
    private Frame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        frame = new Frame();
        layout = new GridLayout();
        frame.setLayout(layout);
        frame.setVisible(true);
    }

    @Override
    protected void tearDown() throws Exception {
        frame.dispose();

        super.tearDown();
    }

    public GridLayoutTest(String name) {
        super(name);

        Dimension min = new Dimension(MIN_SIZE, MIN_SIZE),
                pref = new Dimension(PREF_SIZE, PREF_SIZE);
        b1 = new TestButton("1", min, pref);
        b2 = new TestButton("2", min, pref);
        b3 = new TestButton("3", min, pref);
        b4 = new TestButton("4", min, pref);
        b5 = new TestButton("5", min, pref);
        b6 = new TestButton("6", min, pref);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BorderLayoutTest.class);
    }

    public void testGridLayout() {
        assertEquals(layout.getColumns(), 0);
        assertEquals(layout.getRows(), 1);
        assertEquals(layout.getHgap(), 0);
        assertEquals(layout.getVgap(), 0);
    }

    public void testGridLayoutintint() {
        layout = new GridLayout(2, 3);
        assertEquals(layout.getColumns(), 3);
        assertEquals(layout.getRows(), 2);
        assertEquals(layout.getHgap(), 0);
        assertEquals(layout.getVgap(), 0);

        boolean bothZero = false;
        try {
            layout = new GridLayout(0, 0);
        } catch (IllegalArgumentException e) {
            bothZero = true;
        }
        assertTrue(bothZero);
    }

    public void testGridLayoutintintintint() {
        layout = new GridLayout(2, 3, 10, 20);
        assertEquals(layout.getColumns(), 3);
        assertEquals(layout.getRows(), 2);
        assertEquals(layout.getHgap(), 10);
        assertEquals(layout.getVgap(), 20);

        boolean bothZero = false;
        try {
            layout = new GridLayout(0, 0);
        } catch (IllegalArgumentException e) {
            bothZero = true;
        }
        assertTrue(bothZero);
    }

    public void testToString() {
        layout = new GridLayout(1,2,3,4);
        assertTrue(new String("java.awt.GridLayout[hgap=3,vgap=4,rows=1,cols=2]").equals(layout.toString()));
    }

    public final void testGetSetHgap() {
        layout.setHgap(10);
        assertEquals(layout.getHgap(), 10);
        layout.setHgap(-1);
        assertEquals(layout.getHgap(), -1);
    }

    public final void testGetSetVgap() {
        layout.setVgap(10);
        assertEquals(layout.getVgap(), 10);
        layout.setVgap(-1);
        assertEquals(layout.getVgap(), -1);
    }

    public void testGetSetColumns() {
        layout = new GridLayout(2, 3);
        layout.setColumns(10);
        assertEquals(layout.getColumns(), 10);

        boolean bothZero = false;
        try {
            layout.setRows(0);
            layout.setColumns(0);
        } catch (IllegalArgumentException e) {
            bothZero = true;
        }
        assertTrue(bothZero);
    }

    public void testGetSetRows() {
        layout = new GridLayout(2, 3);
        layout.setRows(10);
        assertEquals(layout.getRows(), 10);

        boolean bothZero = false;
        try {
            layout.setColumns(0);
            layout.setRows(0);
        } catch (IllegalArgumentException e) {
            bothZero = true;
        }
        assertTrue(bothZero);
    }

    public void testAddLayoutComponent() {
        Container c = new Container();
        c.setSize(200, 20);
        c.add(b1);
        c.add(b2);
        layout.addLayoutComponent("", b3);
        assertEquals(new Dimension(), b1.getSize());
        assertEquals(new Dimension(), b2.getSize());
        assertEquals(new Dimension(), b3.getSize());
        layout.layoutContainer(c);
        Dimension expected = new Dimension(c.getWidth() / c.getComponentCount(),
                                           c.getHeight());
        assertEquals(expected, b1.getSize());
        assertEquals(expected, b2.getSize());
        // verify that addLayoutComponent has no effect:
        assertEquals(new Dimension(), b3.getSize());
    }

    public void testRemoveLayoutComponent() {
        Container c = new Container();
        c.setSize(200, 20);
        c.add(b1);
        c.add(b2);
        layout.removeLayoutComponent(b2);
        assertEquals(new Dimension(), b1.getSize());
        assertEquals(new Dimension(), b2.getSize());        
        layout.layoutContainer(c);
        Dimension expected = new Dimension(c.getWidth() / c.getComponentCount(),
                                           c.getHeight());
        assertEquals(expected, b1.getSize());
        // verify that removeLayoutComponent has no effect:
        assertEquals(expected, b2.getSize());
    }

    public void testMinimumLayoutSize() {
        Dimension expected = new Dimension();
        Insets insets = frame.getInsets();

        assertEquals(layout.minimumLayoutSize(frame),
                new Dimension(insets.left + insets.right,
                        insets.top + insets.bottom));

        layout.setColumns(0);
        layout.setRows(2);
        layout.setHgap(10);
        layout.setVgap(20);
        frame.add(b1);
        frame.add(b2);
        frame.add(b3);
        frame.add(b4);
        b5.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(b5);
        frame.add(b6);
        frame.validate();

        expected.width = b5.getMinimumSize().width * 6 / 2 + layout.getHgap() * (6 / 2 - 1) + insets.left + insets.right;
        expected.height = b5.getMinimumSize().height * 6 / 3 + layout.getVgap() * (6 / 3 - 1) + insets.top + insets.bottom;
        assertEquals(layout.minimumLayoutSize(frame), expected);
    }

    public void testPreferredLayoutSize() {
        Dimension expected = new Dimension();
        Insets insets = frame.getInsets();

        assertEquals(layout.preferredLayoutSize(frame),
                new Dimension(insets.left + insets.right,
                        insets.top + insets.bottom));

        layout.setColumns(0);
        layout.setRows(2);
        layout.setHgap(10);
        layout.setVgap(20);
        frame.add(b1);
        frame.add(b2);
        frame.add(b3);
        frame.add(b4);
        b5.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(b5);
        frame.add(b6);
        frame.validate();

        expected.width = b5.getPreferredSize().width * 6 / 2 + layout.getHgap() * (6 / 2 - 1) + insets.left + insets.right;
        expected.height = b5.getPreferredSize().height * 6 / 3 + layout.getVgap() * (6 / 3 - 1) + insets.top + insets.bottom;
        assertEquals(layout.preferredLayoutSize(frame), expected);
    }

    public void testLayoutContainer() {

        Insets insets = frame.getInsets();

        frame.add(b1);
        frame.add(b2);
        frame.add(b3);
        frame.add(b4);
        frame.add(b5);
        frame.add(b6);
        b5.setFont(new Font("dialog", Font.PLAIN, 20));
        layout.setHgap(10);
        layout.setVgap(20);
        layout.setColumns(0);
        layout.setRows(2);
        frame.setSize(insets.left + insets.right + 320, insets.top
                + insets.bottom + 220);
        frame.validate();

        assertEquals(b1.getBounds(), new Rectangle(insets.left, insets.top,
                100, 100));
        assertEquals(b6.getBounds(), new Rectangle(insets.left + 220,
                insets.top + 120, 100, 100));

        frame.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        frame.validate();
        assertEquals(b1.getBounds(), new Rectangle(insets.left + 220,
                insets.top, 100, 100));
        assertEquals(b6.getBounds(), new Rectangle(insets.left,
                insets.top + 120, 100, 100));

        frame.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        layout.setColumns(1);
        layout.setRows(0);
        frame.setSize(insets.left + insets.right + 200, insets.top
                + insets.bottom + 700);
        frame.validate();
        assertEquals(b1.getBounds(), new Rectangle(insets.left, insets.top,
                200, 100));
        assertEquals(b6.getBounds(), new Rectangle(insets.left,
                insets.top + 600, 200, 100));
    }

}
