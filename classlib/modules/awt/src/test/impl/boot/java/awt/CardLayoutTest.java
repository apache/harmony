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
public class CardLayoutTest extends AWTTestCase {

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
    private CardLayout layout;
    private Frame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        frame = new Frame();
        layout = new CardLayout();
        frame.setLayout(layout);
        frame.setVisible(true);
    }

    @Override
    protected void tearDown() throws Exception {
        frame.dispose();

        super.tearDown();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CardLayoutTest.class);
    }

    public CardLayoutTest(String name) {
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

    public void testCardLayoutintint() {
        layout = new CardLayout(10, 5);
        assertEquals(layout.getHgap(), 10);
        assertEquals(layout.getVgap(), 5);
    }

    public void testCardLayout() {
        assertEquals(layout.getHgap(), 0);
        assertEquals(layout.getVgap(), 0);
    }

    public void testToString() {
        assertTrue(new String("java.awt.CardLayout[hgap=10,vgap=20]").equals(new CardLayout(10,20).toString()));
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

    @SuppressWarnings("deprecation")
    public void testAddLayoutComponentComponentObject() {
        Container c = new Container();
        c.setSize(1, 2);
        c.add(b1);        
        layout.addLayoutComponent("", b2);
        assertEquals(new Dimension(), b1.getSize());
        assertEquals(new Dimension(), b2.getSize());        
        layout.layoutContainer(c);
        assertEquals(c.getSize(), b1.getSize());        
        // verify that addLayoutComponent has no effect:
        assertEquals(new Dimension(), b2.getSize());        
        

        boolean notString = false;
        try {
            layout.addLayoutComponent(b3, new Integer(3));
        } catch (IllegalArgumentException e) {
            notString = true;
        }
        assertTrue(notString);
    }

    @SuppressWarnings("deprecation")
    public void testRemoveLayoutComponent() {
        Container c = new Container();
        c.setSize(13, 13);
        c.add(b1);        
        layout.removeLayoutComponent(b1);
        assertEquals(new Dimension(), b1.getSize());        
        layout.layoutContainer(c);        
        // verify that removeLayoutComponent had no effect:
        assertEquals(c.getSize(), b1.getSize());
        layout.addLayoutComponent("q", b1);
        c.setLayout(layout);        
        b1.setVisible(false);
        layout.show(c, "q");
        assertTrue(b1.isVisible());
        layout.removeLayoutComponent(b1);
        b1.setVisible(false);
        layout.show(c, "q");        
        //verify that component name was removed from map:
        assertFalse(b1.isVisible());
        
    }

    public void testFirstLast() {

        frame.add(b1, "");
        frame.add(b2, "");
        frame.add(b3, "");
        frame.add(b4, "");
        frame.add(b5, "");
        frame.add(b6, "");

        frame.validate();
        assertTrue(b1.isVisible());
        layout.last(frame);
        assertTrue(b6.isVisible());
        layout.first(frame);
        assertTrue(b1.isVisible());

    }

    public void testNextPrev() {
        frame.add(b1, "");
        frame.add(b2, "");
        frame.add(b3, "");
        frame.add(b4, "");
        frame.add(b5, "");
        frame.add(b6, "");

        frame.validate();
        layout.previous(frame);
        assertTrue(b6.isVisible());
        layout.next(frame);
        assertTrue(b1.isVisible());
        layout.next(frame);
        assertTrue(b2.isVisible());
        layout.previous(frame);
        assertTrue(b1.isVisible());

    }

    public void testShow() {

        frame.add(b1, "1");
        frame.add(b2, "2");
        frame.add(b3, "3");
        frame.add(b4, "4");
        frame.add(b5, "5");
        frame.add(b6, "6");

        frame.validate();
        layout.show(frame, "5");
        assertTrue(b5.isVisible());
        layout.show(frame, "4");
        assertTrue(b4.isVisible());

    }

    public void testMaximumLayoutSize() {
        assertEquals(layout.maximumLayoutSize(frame), new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    public void testMinimumLayoutSize() {
        frame.add(b1, "1");
        frame.add(b2, "2");
        frame.add(b3, "3");
        frame.add(b4, "4");
        b5.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(b5, "5");
        frame.add(b6, "6");

        layout.setHgap(10);
        layout.setVgap(20);

        frame.validate();
        Insets insets = frame.getInsets();

        assertEquals(layout.minimumLayoutSize(frame),
                new Dimension(b5.getMinimumSize().width + 2 * layout.getHgap() + insets.left + insets.right,
                        b5.getMinimumSize().height + 2 * layout.getVgap() + insets.top + insets.bottom));
    }

    public void testPreferredLayoutSize() {
        frame.add(b1, "1");
        frame.add(b2, "2");
        frame.add(b3, "3");
        frame.add(b4, "4");
        b5.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(b5, "5");
        frame.add(b6, "6");

        layout.setHgap(10);
        layout.setVgap(20);

        frame.validate();
        Insets insets = frame.getInsets();

        assertEquals(layout.preferredLayoutSize(frame),
                new Dimension(b5.getPreferredSize().width + 2 * layout.getHgap() + insets.left + insets.right,
                        b5.getPreferredSize().height + 2 * layout.getVgap() + insets.top + insets.bottom));
    }

    public void testLayoutContainer() {

        frame.add(b1, "1");
        frame.add(b2, "2");
        frame.add(b3, "3");
        frame.add(b4, "4");
        b5.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(b5, "5");
        frame.add(b6, "6");

        layout.setHgap(10);
        layout.setVgap(20);

        frame.setSize(frame.getPreferredSize());
        Insets insets = frame.getInsets();

        frame.validate();
        assertTrue(b1.isVisible());
        assertEquals(b1.getBounds(), new Rectangle(insets.left
                + layout.getHgap(), insets.top + layout.getVgap(),
                frame.getSize().width - 2 * layout.getHgap() - insets.left
                        - insets.right, frame.getSize().height - 2
                        * layout.getVgap() - insets.top - insets.bottom));

        frame.remove(b1);
        frame.validate();
        assertTrue(b2.isVisible());
        assertEquals(b2.getBounds(), new Rectangle(insets.left
                + layout.getHgap(), insets.top + layout.getVgap(),
                frame.getSize().width - 2 * layout.getHgap() - insets.left
                        - insets.right, frame.getSize().height - 2
                        * layout.getVgap() - insets.top - insets.bottom));

    }

}
