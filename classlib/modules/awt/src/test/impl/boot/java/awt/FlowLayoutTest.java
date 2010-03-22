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
public class FlowLayoutTest extends AWTTestCase {

    class TestButton extends Button {
        TestButton(String name, Dimension min, Dimension pref) {
            super(name);

            setMinimumSize(min);
            setPreferredSize(pref);
        }
    }

    private final int MIN_SIZE = 50;
    private final int PREF_SIZE = 100;
    private final TestButton b1, b2, b3, b4, b5;
    private FlowLayout layout;
    private Frame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        frame = new Frame();
        layout = new FlowLayout();
        frame.setLayout(layout);
        frame.setVisible(true);
    }

    @Override
    protected void tearDown() throws Exception {
        frame.dispose();
        super.tearDown();
    }

    public FlowLayoutTest() {
        Dimension min = new Dimension(MIN_SIZE, MIN_SIZE),
                  pref = new Dimension(PREF_SIZE, PREF_SIZE);
        b1 = new TestButton("1", min, pref);
        b2 = new TestButton("2", min, pref);
        b3 = new TestButton("3", min, pref);
        b4 = new TestButton("4", min, pref);
        b5 = new TestButton("5", min, pref);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FlowLayoutTest.class);
    }

    public final void testToString() {
        layout = new FlowLayout(FlowLayout.LEADING, 10, 20);
        assertTrue(new String("java.awt.FlowLayout[hgap=10,vgap=20,align=leading]").equals(layout.toString()));
        layout = new FlowLayout(FlowLayout.LEFT, 0, 0);
        assertTrue(new String("java.awt.FlowLayout[hgap=0,vgap=0,align=left]").equals(layout.toString()));
        layout = new FlowLayout(FlowLayout.RIGHT, 0, 0);
        assertTrue(new String("java.awt.FlowLayout[hgap=0,vgap=0,align=right]").equals(layout.toString()));
        layout = new FlowLayout(FlowLayout.TRAILING, 0, 0);
        assertTrue(new String("java.awt.FlowLayout[hgap=0,vgap=0,align=trailing]").equals(layout.toString()));
        layout = new FlowLayout(FlowLayout.CENTER, 0, 0);
        assertTrue(new String("java.awt.FlowLayout[hgap=0,vgap=0,align=center]").equals(layout.toString()));
    }

    public final void testFlowLayoutint() {
        layout = new FlowLayout(FlowLayout.TRAILING);
        assertEquals(layout.getAlignment(), FlowLayout.TRAILING);
        assertEquals(layout.getHgap(), 5);
        assertEquals(layout.getVgap(), 5);
        layout = new FlowLayout(FlowLayout.CENTER);
        assertEquals(layout.getAlignment(), FlowLayout.CENTER);
        layout = new FlowLayout(FlowLayout.LEFT);
        assertEquals(layout.getAlignment(), FlowLayout.LEFT);
        layout = new FlowLayout(FlowLayout.LEADING);
        assertEquals(layout.getAlignment(), FlowLayout.LEADING);
        layout = new FlowLayout(FlowLayout.RIGHT);
        assertEquals(layout.getAlignment(), FlowLayout.RIGHT);
    }

    public final void testFlowLayout() {
        FlowLayout layout = new FlowLayout();
        assertEquals(layout.getAlignment(), FlowLayout.CENTER);
        assertEquals(layout.getHgap(), 5);
        assertEquals(layout.getVgap(), 5);
    }

    public final void testFlowLayoutintintint() {
        FlowLayout layout = new FlowLayout(FlowLayout.TRAILING, 7, 7);
        assertEquals(layout.getAlignment(), FlowLayout.TRAILING);
        assertEquals(layout.getHgap(), 7);
        assertEquals(layout.getVgap(), 7);
    }

    public final void testGetSetAlignment() {
        int align = FlowLayout.LEADING;
        layout.setAlignment(align);
        assertEquals(align, layout.getAlignment());
        layout.setAlignment(align = FlowLayout.CENTER);
        assertEquals(align, layout.getAlignment());
        layout.setAlignment(align = FlowLayout.RIGHT);
        assertEquals(align, layout.getAlignment());
        layout.setAlignment(align = FlowLayout.TRAILING);
        assertEquals(align, layout.getAlignment());
        layout.setAlignment(align = FlowLayout.LEFT);
        assertEquals(align, layout.getAlignment());
        
        layout.setAlignment(align = 5);
        assertEquals(align, layout.getAlignment());
        layout.setAlignment(align = -1);
        assertEquals(align, layout.getAlignment());

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

    public final void testMinimumLayoutSize() {
        Dimension expected = new Dimension();
        Insets insets = frame.getInsets();

        expected.setSize(insets.left + insets.right,
                         insets.top + insets.bottom);
        Dimension defMinSize = layout.minimumLayoutSize(new Container(){});
        expected.width += defMinSize.width;
        expected.height += defMinSize.height;
        assertEquals(expected, layout.minimumLayoutSize(frame));

        frame.add(b1);
        frame.add(b2);
        frame.add(b3);
        frame.add(b4);
        b5.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(b5);
        layout.setVgap(10);
        layout.setHgap(20);

        expected.width = insets.left + insets.right + b1.getMinimumSize().width
                + b2.getMinimumSize().width  + b3.getMinimumSize().width
                + b4.getMinimumSize().width + b5.getMinimumSize().width
                + 6 * layout.getHgap();
        expected.height = insets.bottom + insets.top +
                Math.max(b1.getMinimumSize().height,
                Math.max(b2.getMinimumSize().height,
                Math.max(b3.getMinimumSize().height,
                Math.max(b4.getMinimumSize().height,
                        b5.getMinimumSize().height))))
                + 2 * layout.getVgap();
        assertEquals(layout.minimumLayoutSize(frame), expected);
    }

    public final void testPreferredLayoutSize() {
        Dimension expected = new Dimension();
        Insets insets = frame.getInsets();

        expected.setSize(insets.left + insets.right,
                         insets.top + insets.bottom);
        Dimension defPrefSize = layout.preferredLayoutSize(new Container(){});
        expected.width += defPrefSize.width;
        expected.height += defPrefSize.height;
        assertEquals(expected, layout.preferredLayoutSize(frame));

        frame.add(b1);
        frame.add(b2);
        frame.add(b3);
        frame.add(b4);
        b5.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.add(b5);
        layout.setVgap(10);
        layout.setHgap(20);

        expected.width = insets.left + insets.right + b1.getPreferredSize().width
                + b2.getPreferredSize().width  + b3.getPreferredSize().width
                + b4.getPreferredSize().width + b5.getPreferredSize().width
                + 6 * layout.getHgap();
        expected.height = insets.bottom + insets.top +
                Math.max(b1.getPreferredSize().height,
                Math.max(b2.getPreferredSize().height,
                Math.max(b3.getPreferredSize().height,
                Math.max(b4.getPreferredSize().height,
                        b5.getPreferredSize().height))))
                + 2 * layout.getVgap();
        assertEquals(layout.preferredLayoutSize(frame), expected);
    }

    public final void testAddLayoutComponent() {
        Container c = new Container();
        c.add(b1);
        c.add(b2);
        layout.addLayoutComponent("", b3);
        assertEquals(new Dimension(), b1.getSize());
        assertEquals(new Dimension(), b2.getSize());
        assertEquals(new Dimension(), b3.getSize());
        layout.layoutContainer(c);
        assertEquals(b1.getPreferredSize(), b1.getSize());
        assertEquals(b2.getPreferredSize(), b2.getSize());
        // verify that addLayoutComponent has no effect:
        assertEquals(new Dimension(), b3.getSize());
    }

    public final void testRemoveLayoutComponent() {
        Container c = new Container();
        c.add(b1);
        c.add(b2);
        layout.removeLayoutComponent(b2);
        assertEquals(new Dimension(), b1.getSize());
        assertEquals(new Dimension(), b2.getSize());
        layout.layoutContainer(c);
        assertEquals(b1.getPreferredSize(), b1.getSize());
        // verify that removeLayoutComponent has no effect:
        assertEquals(b2.getPreferredSize(), b2.getSize());

       
    }

    public final void testLayoutContainer() {

        frame.add(b1);
        frame.add(b2);
        frame.add(b3);
        layout.setHgap(10);
        layout.setVgap(20);

        layout.setAlignment(FlowLayout.LEFT);
        frame.validate();
        checkLeft();
        layout.setAlignment(FlowLayout.RIGHT);
        frame.validate();
        checkRight();
        layout.setAlignment(FlowLayout.CENTER);
        frame.validate();
        checkCenter();
        frame.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        layout.setAlignment(FlowLayout.LEADING);
        frame.validate();
        checkLeft();
        layout.setAlignment(FlowLayout.TRAILING);
        frame.validate();
        checkRight();
        frame.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        layout.setAlignment(FlowLayout.LEADING);
        frame.validate();
        checkRightInverted();
        layout.setAlignment(FlowLayout.TRAILING);
        frame.validate();
        checkLeftInverted();
        layout.setAlignment(FlowLayout.LEFT);
        frame.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        b2.setFont(new Font("dialog", Font.PLAIN, 20));
        frame.validate();
        checkLeftSized();

    }

    private void checkLeftSized() {
        Insets insets = frame.getInsets();

        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        int x = insets.left + layout.getHgap();
        int y = insets.top + layout.getVgap();
        int diff = (b2.getSize().height - b1.getSize().height) / 2;
        assertEquals(b1.getBounds(), new Rectangle(x, y + diff, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y + diff, b3.getPreferredSize().width, b3.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width - 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap();
        y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y + diff, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x = insets.left + layout.getHgap();
        y += layout.getVgap() + Math.max(b1.getPreferredSize().height, b2.getPreferredSize().height);
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
    }

    private void checkLeftInverted() {
        Insets insets = frame.getInsets();

        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        int x = insets.left + layout.getHgap();
        int y = insets.top + layout.getVgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
        x += b3.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width + 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap();
        y = insets.top + layout.getVgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
        x += b3.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width - 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap();
        y = insets.top + layout.getVgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x = insets.left + layout.getHgap();
        y += layout.getVgap() + Math.max(b1.getPreferredSize().height, b2.getPreferredSize().height);
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
    }

    private void checkRightInverted() {
        Insets insets = frame.getInsets();

        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        int x = insets.left + layout.getHgap();
        int y = insets.top + layout.getVgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
        x += b3.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width + 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap() + 1;
        y = insets.top + layout.getVgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
        x += b3.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width - 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = frame.getSize().width - insets.right - layout.getHgap() - b1.getPreferredSize().width;
        y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x -= b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x = frame.getSize().width - insets.right - layout.getHgap() - b3.getPreferredSize().width;
        y += layout.getVgap() + Math.max(b1.getPreferredSize().height, b2.getPreferredSize().height);
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
    }

    private void checkCenter() {
        Insets insets = frame.getInsets();

        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        int x = insets.left + layout.getHgap();
        int y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width + 2, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap() + 1;
        y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width - 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap() + (b3.getPreferredSize().width + layout.getHgap() - 1) / 2;
        y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x = insets.left + (frame.getSize().width - insets.left - insets.right - b3.getPreferredSize().width) / 2;
        y += layout.getVgap() + Math.max(b1.getPreferredSize().height, b2.getPreferredSize().height);
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
    }

    private void checkRight() {
        Insets insets = frame.getInsets();

        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        int x = insets.left + layout.getHgap();
        int y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width + 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap() + 1;
        y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width - 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = frame.getSize().width - insets.right - layout.getHgap() - b2.getPreferredSize().width;
        y = insets.top + layout.getVgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x -= b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x = frame.getSize().width - insets.right - layout.getHgap() - b3.getPreferredSize().width;
        y += layout.getVgap() + Math.max(b1.getPreferredSize().height, b2.getPreferredSize().height);
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
    }

    private void checkLeft() {
        Insets insets = frame.getInsets();

        frame.setSize(layout.preferredLayoutSize(frame));
        frame.validate();
        int x = insets.left + layout.getHgap();
        int y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width + 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap();
        y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x += b2.getPreferredSize().width + layout.getHgap();
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));

        frame.setSize(layout.preferredLayoutSize(frame).width - 1, layout.preferredLayoutSize(frame).height);
        frame.validate();
        x = insets.left + layout.getHgap();
        y = insets.top + layout.getVgap();
        assertEquals(b1.getBounds(), new Rectangle(x, y, b1.getPreferredSize().width, b1.getPreferredSize().height));
        x += b1.getPreferredSize().width + layout.getHgap();
        assertEquals(b2.getBounds(), new Rectangle(x, y, b2.getPreferredSize().width, b2.getPreferredSize().height));
        x = insets.left + layout.getHgap();
        y += layout.getVgap() + Math.max(b1.getPreferredSize().height, b2.getPreferredSize().height);
        assertEquals(b3.getBounds(), new Rectangle(x, y, b3.getPreferredSize().width, b3.getPreferredSize().height));
    }

}
