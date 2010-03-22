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
public class BorderLayoutTest extends AWTTestCase {

    class TestButton extends Button {
        TestButton(String name, Dimension min, Dimension pref) {
            super(name);

            setMinimumSize(min);
            setPreferredSize(pref);
        }
    }

    private final int MIN_SIZE = 50;
    private final int PREF_SIZE = 100;
    private final TestButton n, s, w, c, e;
    private BorderLayout layout;
    private Frame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        frame = new Frame();
        layout = new BorderLayout();
        frame.setLayout(layout);
        frame.setVisible(true);
    }

    @Override
    protected void tearDown() throws Exception {
        frame.dispose();

        super.tearDown();
    }

    public BorderLayoutTest() {
        Dimension min = new Dimension(MIN_SIZE, MIN_SIZE),
                  pref = new Dimension(PREF_SIZE, PREF_SIZE);
        n = new TestButton("n", min, pref);
        s = new TestButton("s", min, pref);
        c = new TestButton("c", min, pref);
        w = new TestButton("w", min, pref);
        e = new TestButton("e", min, pref);
    }

    public final void testBorderLayoutintint() {
        BorderLayout layout = new BorderLayout(10, 5);
        assertEquals(layout.getHgap(), 10);
        assertEquals(layout.getVgap(), 5);
    }

    public final void testBorderLayout() {
        BorderLayout layout = new BorderLayout();
        assertEquals(layout.getHgap(), 0);
        assertEquals(layout.getVgap(), 0);
    }

    public final void testToString() {
        BorderLayout layout = new BorderLayout(30, 300);
        assertTrue(new String("java.awt.BorderLayout[hgap=30,vgap=300]").equals(layout.toString()));
    }

    public final void testAddLayoutComponentComponentObject() {
        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.SOUTH);
        frame.add(w, BorderLayout.WEST);
        frame.add(c, BorderLayout.CENTER);
        frame.add(e, BorderLayout.EAST);
        frame.remove(c);
        frame.add(c);

        frame.remove(n);
        frame.remove(s);
        frame.remove(w);
        frame.remove(c);

        TestButton t = new TestButton("", null, null);

        boolean notString = false;
        boolean wrongString = false;
        boolean oneTwice = false;
        try {
            frame.add(t, new Integer(5));
        } catch (IllegalArgumentException e) {
            notString = true;
        }
        try {
            frame.add(t, "Dummy");
        } catch (IllegalArgumentException e) {
            wrongString = true;
        }
        try {
            layout.addLayoutComponent(e, BorderLayout.WEST);
        } catch (IllegalArgumentException e) {
            oneTwice = true;
        }
        assertTrue(notString);
        assertTrue(wrongString);
        assertFalse(oneTwice);
        
        // Regression test HARMONY-1667
        try {
            layout.addLayoutComponent(null, BorderLayout.CENTER);
            fail("Expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public final void testRemoveLayoutComponent() {
        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.SOUTH);
        frame.add(w, BorderLayout.WEST);
        frame.add(c, BorderLayout.CENTER);
        frame.add(e, BorderLayout.EAST);

        frame.removeAll();
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

    public final void testGetLayoutAlignmentX() {
        assertTrue(layout.getLayoutAlignmentX(frame) == Component.CENTER_ALIGNMENT);
    }

    public final void testGetLayoutAlignmentY() {
        assertTrue(layout.getLayoutAlignmentY(frame) == Component.CENTER_ALIGNMENT);
    }

    public final void testMaximumLayoutSize() {
        assertEquals(layout.maximumLayoutSize(frame), new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    public final void testMinimumLayoutSize() {
        Dimension expected = new Dimension();
        Insets insets = frame.getInsets();

        assertEquals(layout.minimumLayoutSize(frame),
                new Dimension(insets.left + insets.right,
                        insets.top + insets.bottom));

        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.SOUTH);
        frame.add(w, BorderLayout.WEST);
        frame.add(c, BorderLayout.CENTER);
        c.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(e, BorderLayout.EAST);
        layout.setVgap(10);
        layout.setHgap(20);

        expected.width = Math.max(n.getMinimumSize().width,
                Math.max(s.getMinimumSize().width,
                        w.getMinimumSize().width + c.getMinimumSize().width +
                                e.getMinimumSize().width + 2 * layout.getHgap()))
                        + insets.left + insets.right;
        expected.height = n.getMinimumSize().height
                + s.getMinimumSize().height
                + Math.max(c.getMinimumSize().height,
                        Math.max(w.getMinimumSize().height, e.getMinimumSize().height))
                + 2 * layout.getVgap() + insets.bottom + insets.top;
        assertEquals(layout.minimumLayoutSize(frame), expected);

        frame.remove(s);
        frame.validate();
        expected.width = Math.max(n.getMinimumSize().width,
                Math.max(0,
                        w.getMinimumSize().width + c.getMinimumSize().width +
                                e.getMinimumSize().width + 2 * layout.getHgap()))
                        + insets.left + insets.right;
        expected.height = n.getMinimumSize().height
                + Math.max(c.getMinimumSize().height,
                        Math.max(w.getMinimumSize().height, e.getMinimumSize().height))
                + layout.getVgap() + insets.bottom + insets.top;
        assertEquals(layout.minimumLayoutSize(frame), expected);
        frame.add(s, BorderLayout.SOUTH);

        frame.remove(c);
        expected.width = Math.max(n.getMinimumSize().width,
                Math.max(s.getMinimumSize().width,
                        w.getMinimumSize().width + e.getMinimumSize().width + layout.getHgap()))
                        + insets.left + insets.right;
        expected.height = n.getMinimumSize().height
                + s.getMinimumSize().height
                + Math.max(c.getMinimumSize().height, e.getMinimumSize().height)
                + 2 * layout.getVgap() + insets.bottom + insets.top;
        assertEquals(layout.minimumLayoutSize(frame), expected);

        frame.remove(w);
        frame.remove(e);
        expected.width = Math.max(n.getMinimumSize().width, s.getMinimumSize().width)
                        + insets.left + insets.right;
        expected.height = n.getMinimumSize().height
                + s.getMinimumSize().height
                + layout.getVgap() + insets.bottom + insets.top;
        assertEquals(layout.minimumLayoutSize(frame), expected);
    }

    public final void testPreferredLayoutSize() {
        Dimension expected = new Dimension();
        Insets insets = frame.getInsets();

        assertEquals(layout.preferredLayoutSize(frame),
                new Dimension(insets.left + insets.right,
                        insets.top + insets.bottom));

        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.SOUTH);
        frame.add(w, BorderLayout.WEST);
        frame.add(c, BorderLayout.CENTER);
        c.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(e, BorderLayout.EAST);
        layout.setVgap(10);
        layout.setHgap(20);

        expected.width = Math.max(n.getPreferredSize().width,
                Math.max(s.getPreferredSize().width,
                        w.getPreferredSize().width + c.getPreferredSize().width +
                                e.getPreferredSize().width + 2 * layout.getHgap()))
                        + insets.left + insets.right;
        expected.height = n.getPreferredSize().height
                + s.getPreferredSize().height
                + Math.max(c.getPreferredSize().height,
                        Math.max(w.getPreferredSize().height, e.getPreferredSize().height))
                + 2 * layout.getVgap() + insets.bottom + insets.top;
        assertEquals(layout.preferredLayoutSize(frame), expected);

        frame.remove(s);
        frame.validate();
        expected.width = Math.max(n.getPreferredSize().width,
                Math.max(0,
                        w.getPreferredSize().width + c.getPreferredSize().width +
                                e.getPreferredSize().width + 2 * layout.getHgap()))
                        + insets.left + insets.right;
        expected.height = n.getPreferredSize().height
                + Math.max(c.getPreferredSize().height,
                        Math.max(w.getPreferredSize().height, e.getPreferredSize().height))
                + layout.getVgap() + insets.bottom + insets.top;
        assertEquals(layout.preferredLayoutSize(frame), expected);
        frame.add(s, BorderLayout.SOUTH);

        frame.remove(c);
        expected.width = Math.max(n.getPreferredSize().width,
                Math.max(s.getPreferredSize().width,
                        w.getPreferredSize().width + e.getPreferredSize().width + layout.getHgap()))
                        + insets.left + insets.right;
        expected.height = n.getPreferredSize().height
                + s.getPreferredSize().height
                + Math.max(c.getPreferredSize().height, e.getPreferredSize().height)
                + 2 * layout.getVgap() + insets.bottom + insets.top;
        assertEquals(layout.preferredLayoutSize(frame), expected);

        frame.remove(w);
        frame.remove(e);
        expected.width = Math.max(n.getPreferredSize().width, s.getPreferredSize().width)
                        + insets.left + insets.right;
        expected.height = n.getPreferredSize().height
                + s.getPreferredSize().height
                + layout.getVgap() + insets.bottom + insets.top;
        assertEquals(layout.preferredLayoutSize(frame), expected);
    }

    public final void testLayoutContainer() {

        Insets insets = frame.getInsets();

        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.SOUTH);
        frame.add(w, BorderLayout.WEST);
        frame.add(c, BorderLayout.CENTER);
        c.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(e, BorderLayout.EAST);
        layout.setHgap(10);
        layout.setVgap(20);
        frame.validate();

        int x = insets.left;
        int y = insets.top;
        int midH = Math.max(c.getPreferredSize().height,
                            Math.max(w.getPreferredSize().height,
                                     e.getPreferredSize().height));
        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        assertEquals(n.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.left - insets.right, n.getPreferredSize().height));
        assertEquals(s.getBounds(), new Rectangle(x, frame.getSize().height
                - insets.bottom - s.getPreferredSize().height,
                frame.getSize().width - insets.left - insets.right,
                s.getPreferredSize().height));
        y += n.getBounds().height + layout.getVgap();
        assertEquals(w.getBounds(), new Rectangle(x, y,
                w.getPreferredSize().width, midH));
        x += w.getBounds().width + layout.getHgap();
        assertEquals(c.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.right - insets.left - w.getPreferredSize().width
                - e.getPreferredSize().width - 2 * layout.getHgap(), midH));
        x += c.getBounds().width + layout.getHgap();
        assertEquals(e.getBounds(), new Rectangle(x, y,
                e.getPreferredSize().width, midH));

        frame.setSize(frame.getSize().width, insets.top
                + insets.bottom
                + Math.max(s.getPreferredSize().height,
                           n.getPreferredSize().height) + 1);
        frame.validate();
        x = insets.left;
        y = insets.top;
        midH = Math.max(c.getPreferredSize().height,
                        Math.max(w.getPreferredSize().height,
                                 e.getPreferredSize().height));
        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        assertEquals(n.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.left - insets.right, n.getPreferredSize().height));
        assertEquals(s.getBounds(), new Rectangle(x, frame.getSize().height
                - insets.bottom - s.getPreferredSize().height,
                frame.getSize().width - insets.left - insets.right,
                s.getPreferredSize().height));

        x = insets.left;
        y = insets.top;
        midH = Math.max(c.getPreferredSize().height,
                        Math.max(w.getPreferredSize().height,
                                 e.getPreferredSize().height)) + 1;
        frame.setSize(layout.preferredLayoutSize(frame).width + 1,
                      layout.preferredLayoutSize(frame).height + 1);
        frame.validate();
        assertEquals(n.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.left - insets.right, n.getPreferredSize().height));
        assertEquals(s.getBounds(), new Rectangle(x, frame.getSize().height
                - insets.bottom - s.getPreferredSize().height,
                frame.getSize().width - insets.left - insets.right,
                s.getPreferredSize().height));
        y += n.getBounds().height + layout.getVgap();
        assertEquals(w.getBounds(), new Rectangle(x, y,
                w.getPreferredSize().width, midH));
        x += w.getBounds().width + layout.getHgap();
        assertEquals(c.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.right - insets.left - w.getPreferredSize().width
                - e.getPreferredSize().width - 2 * layout.getHgap(), midH));
        x += c.getBounds().width + layout.getHgap();
        assertEquals(e.getBounds(), new Rectangle(x, y,
                e.getPreferredSize().width, midH));

        x = insets.left;
        y = insets.top;
        midH = Math.max(c.getPreferredSize().height,
                        Math.max(w.getPreferredSize().height,
                                 e.getPreferredSize().height));
        frame.remove(s);
        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        assertEquals(n.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.left - insets.right, n.getPreferredSize().height));
        y += n.getBounds().height + layout.getVgap();
        assertEquals(w.getBounds(), new Rectangle(x, y,
                w.getPreferredSize().width, midH));
        x += w.getBounds().width + layout.getHgap();
        assertEquals(c.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.right - insets.left - w.getPreferredSize().width
                - e.getPreferredSize().width - 2 * layout.getHgap(), midH));
        x += c.getBounds().width + layout.getHgap();
        assertEquals(e.getBounds(), new Rectangle(x, y,
                e.getPreferredSize().width, midH));
        frame.add(s, BorderLayout.SOUTH);

        x = insets.left;
        y = insets.top;
        midH = Math.max(c.getPreferredSize().height,
                        e.getPreferredSize().height);
        frame.remove(w);
        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        assertEquals(n.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.left - insets.right, n.getPreferredSize().height));
        assertEquals(s.getBounds(), new Rectangle(x, frame.getSize().height
                - insets.bottom - s.getPreferredSize().height,
                frame.getSize().width - insets.left - insets.right,
                s.getPreferredSize().height));
        y += n.getBounds().height + layout.getVgap();
        assertEquals(c.getBounds(), new Rectangle(x, y, frame.getSize().width
                - insets.right - insets.left - e.getPreferredSize().width
                - layout.getHgap(), midH));
        x += c.getBounds().width + layout.getHgap();
        assertEquals(e.getBounds(), new Rectangle(x, y,
                e.getPreferredSize().width, midH));

    }

    public final void testGetLayoutComponentObject() {
        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.SOUTH);
        frame.add(w, BorderLayout.WEST);
        frame.add(c, BorderLayout.CENTER);
        frame.add(e, BorderLayout.EAST);

        assertEquals(layout.getLayoutComponent(BorderLayout.NORTH), n);
        assertEquals(layout.getLayoutComponent(BorderLayout.SOUTH), s);
        assertEquals(layout.getLayoutComponent(BorderLayout.WEST), w);
        assertEquals(layout.getLayoutComponent(BorderLayout.CENTER), c);
        assertEquals(layout.getLayoutComponent(BorderLayout.EAST), e);
        assertNull(layout.getLayoutComponent(BorderLayout.PAGE_START));

        try {
            layout.getLayoutComponent("Z");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public final void testGetLayoutComponentContainerObject() {
        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.SOUTH);
        frame.add(w, BorderLayout.WEST);
        frame.add(c, BorderLayout.CENTER);
        frame.add(e, BorderLayout.EAST);

        assertEquals(n, layout.getLayoutComponent(frame, BorderLayout.NORTH));
        assertEquals(s, layout.getLayoutComponent(frame, BorderLayout.SOUTH));
        assertEquals(w, layout.getLayoutComponent(frame, BorderLayout.WEST));
        assertEquals(c, layout.getLayoutComponent(frame, BorderLayout.CENTER));
        assertEquals(e, layout.getLayoutComponent(frame, BorderLayout.EAST));        
        try {
            layout.getLayoutComponent(frame, BorderLayout.PAGE_START);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            layout.getLayoutComponent(frame, "Z");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        frame.removeAll();
        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.PAGE_END);
        frame.add(new Button("q"), BorderLayout.SOUTH);
        frame.add(w, BorderLayout.LINE_START);
        frame.add(c, BorderLayout.CENTER);
        frame.add(e, BorderLayout.LINE_END);
        frame.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertEquals(n, layout.getLayoutComponent(frame, BorderLayout.NORTH));
        assertEquals(s, layout.getLayoutComponent(frame, BorderLayout.SOUTH));
        assertEquals(w, layout.getLayoutComponent(frame, BorderLayout.EAST));
        assertEquals(c, layout.getLayoutComponent(frame, BorderLayout.CENTER));
        assertEquals(e, layout.getLayoutComponent(frame, BorderLayout.WEST));

        
    }

    public final void testGetConstraintsComponent() {
        frame.add(n, BorderLayout.NORTH);
        frame.add(s, BorderLayout.SOUTH);
        frame.add(w, BorderLayout.WEST);
        frame.add(c, BorderLayout.CENTER);
        frame.add(e, BorderLayout.EAST);

        assertEquals(BorderLayout.NORTH, layout.getConstraints(n));
        assertEquals(BorderLayout.SOUTH, layout.getConstraints(s));
        assertEquals(BorderLayout.WEST, layout.getConstraints(w));
        assertEquals(BorderLayout.CENTER, layout.getConstraints(c));
        assertEquals(BorderLayout.EAST, layout.getConstraints(e));
        assertNull(layout.getConstraints(null));
        assertNull(layout.getConstraints(new Button("Z")));
    }

}
