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

package javax.swing.text;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.FlowView_FlowStrategyTest.PartFactory;
import javax.swing.text.FlowView_FlowStrategyTest.PartView;
import javax.swing.text.Position.Bias;
import javax.swing.text.ViewTestHelpers.ChildView;
import javax.swing.text.ViewTestHelpers.ChildrenFactory;

public class ParagraphViewTest extends BasicSwingTestCase {
    private static final int HEIGHT = 30;

    private static final int WIDTH = PartView.CHAR_WIDTH * 10;

    private StyledDocument doc;

    private Element root;

    private Element paragraph;

    private ParagraphView view;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        doc = new DefaultStyledDocument();
        doc.insertString(0, "plainbolditalic\nparagraph2", null);
        //                   0123456789012345
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        doc.setCharacterAttributes(5, 4, attrs, false);
        attrs = new SimpleAttributeSet();
        StyleConstants.setItalic(attrs, true);
        doc.setCharacterAttributes(9, 6, attrs, false);
        root = doc.getDefaultRootElement();
        paragraph = root.getElement(0);
        view = new ParagraphView(paragraph);
    }

    public void testParagraphView() {
        final Marker propertiesSet = new Marker();
        view = new ParagraphView(paragraph) {
            @Override
            protected void setPropertiesFromAttributes() {
                propertiesSet.setOccurred();
                super.setPropertiesFromAttributes();
            };
        };
        assertTrue(propertiesSet.isOccurred());
        assertEquals(0, view.getViewCount());
        assertEquals(View.Y_AXIS, view.getAxis());
        assertEquals(View.X_AXIS, view.getFlowAxis());
        assertNull(view.layoutPool);
        // Testing insets
        assertEquals(0, view.getTopInset());
        assertEquals(0, view.getLeftInset());
        assertEquals(0, view.getBottomInset());
        assertEquals(0, view.getRightInset());
        AttributeSet attrs = getInsetsAttributeSet();
        doc.setParagraphAttributes(0, 1, attrs, false);
        view.setPropertiesFromAttributes();
        assertEquals(9, view.getTopInset());
        assertEquals(4, view.getLeftInset());
        assertEquals(1, view.getBottomInset());
        assertEquals(7, view.getRightInset());
    }

    public void testGetAlignment() {
        loadChilrenAndLayout();
        final int height = view.getSpan(View.Y_AXIS, 0);
        assertEquals(0, view.firstLineIndent);
        assertEquals(0.5f, view.getAlignment(View.X_AXIS), 1e-5f);
        assertEquals((height / 2) / view.getPreferredSpan(View.Y_AXIS), view
                .getAlignment(View.Y_AXIS), 1e-5f);
        // But if height is of type float the assertion fails
        //        assertEquals((height / 2.0) / view.getPreferredSpan(View.Y_AXIS),
        //                     view.getAlignment(View.Y_AXIS), 1e-5f);
        view.layoutChanged(View.X_AXIS);
        view.layout(WIDTH, HEIGHT);
        assertEquals(0, view.getOffset(View.X_AXIS, 0));
        assertEquals(view.firstLineIndent, ((CompositeView) view.getView(0)).getLeftInset());
        assertEquals(0.5f, view.getAlignment(View.X_AXIS), 1e-5f);
        assertEquals((height / 2) / view.getPreferredSpan(View.Y_AXIS), view
                .getAlignment(View.Y_AXIS), 1e-5f);
    }

    public void testGetAlignmentNoChildren() {
        view.loadChildren(null);
        assertEquals(0.5f, view.getAlignment(View.X_AXIS), 1e-5f);
        assertEquals(0.5f, view.getAlignment(View.Y_AXIS), 1e-5f);
    }

    public void testChangedUpdate() {
        assertEquals(0, view.firstLineIndent);
        view.setFirstLineIndent(3.21f);
        assertEquals(3, view.firstLineIndent);
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFirstLineIndent(attrs, 5.61f);
        doc.setParagraphAttributes(0, 1, attrs, false);
        assertTrue(view.getAttributes().isDefined(StyleConstants.FirstLineIndent));
        view.loadChildren(null);
        assertNotNull(view.layoutPool);
        view.changedUpdate(((AbstractDocument) doc).new DefaultDocumentEvent(0, paragraph
                .getEndOffset(), EventType.CHANGE), null, null);
        assertEquals(5, view.firstLineIndent);
    }

    public void testChangedUpdateInvalidate() {
        loadChilrenAndLayout();
        assertTrue(view.isAllocationValid());
        view.changedUpdate(((AbstractDocument) doc).new DefaultDocumentEvent(0, paragraph
                .getEndOffset(), EventType.CHANGE), null, null);
        assertFalse(view.isAllocationValid());
    }

    public void testCreateRow() {
        View row = view.createRow();
        assertEquals(0, row.getViewCount());
        assertTrue(row instanceof BoxView);
        assertEquals("javax.swing.text.ParagraphView$Row", row.getClass().getName());
        assertEquals(View.X_AXIS, ((BoxView) row).getAxis());
        CompositeView rowView = (CompositeView) row;
        assertEquals(0, rowView.getTopInset());
        assertEquals(0, rowView.getLeftInset());
        assertEquals(0, rowView.getBottomInset());
        assertEquals(0, rowView.getRightInset());
        AttributeSet attrs = getInsetsAttributeSet();
        doc.setParagraphAttributes(0, 1, attrs, false);
        view.setPropertiesFromAttributes();
        rowView = (CompositeView) view.createRow();
        assertEquals(0, rowView.getTopInset());
        assertEquals(0, rowView.getLeftInset());
        assertEquals(0, rowView.getBottomInset());
        assertEquals(0, rowView.getRightInset());
    }

    public void testCreateRowFirstLineIndent() {
        view.setFirstLineIndent(PartView.CHAR_WIDTH * 3);
        CompositeView row = (CompositeView) view.createRow();
        assertEquals(0, row.getTopInset());
        assertEquals(0, row.getLeftInset());
        assertEquals(0, row.getBottomInset());
        assertEquals(0, row.getRightInset());
        loadChilrenAndLayout();
        row = (CompositeView) view.getView(0);
        assertEquals(view.firstLineIndent, row.getLeftInset());
        assertEquals(0, row.getTopInset());
        assertEquals(0, row.getBottomInset());
        assertEquals(0, row.getRightInset());
    }

    /*public void testGetFlowStartEmpty() {
     assertEquals(0, view.getFlowStart(0));

     try {
     view.getFlowStart(1);

     fail("ArrayIndexOutOfBoundsException is expected");
     } catch (ArrayIndexOutOfBoundsException e) { }

     try {
     view.getFlowStart(-1);

     fail("ArrayIndexOutOfBoundsException is expected");
     } catch (ArrayIndexOutOfBoundsException e) { }
     }*/
    /*public void testGetFlowSpanEmpty() {
     final boolean[] callSuper = new boolean[] {true};

     view = new ParagraphView(paragraph) {
     public View getView(int index) {
     return callSuper[0] ? super.getView(index) : null;
     }
     };
     assertEquals(view.layoutSpan, view.getFlowSpan(0));

     try {
     view.getFlowSpan(1);

     fail("ArrayIndexOutOfBoundsException is expected");
     } catch (ArrayIndexOutOfBoundsException e) { }

     try {
     view.getFlowSpan(-1);

     fail("ArrayIndexOutOfBoundsException is expected");
     } catch (ArrayIndexOutOfBoundsException e) { }

     callSuper[0] = false;

     assertEquals(view.layoutSpan, view.getFlowSpan(0));
     assertEquals(view.layoutSpan, view.getFlowSpan(1));
     assertEquals(view.layoutSpan, view.getFlowSpan(-1));
     }*/
    public void testGetFlowStart() {
        view.replace(0, view.getViewCount(), new View[] { view.createRow(), view.createRow() });
        assertEquals(0, view.firstLineIndent);
        assertEquals(0, view.getFlowStart(0));
        assertEquals(0, view.getFlowStart(1));
        view.firstLineIndent = 31;
        assertEquals(31, view.getFlowStart(0));
        assertEquals(0, view.getFlowStart(1));
        ((CompositeView) view.getView(0)).setInsets((short) 7, (short) 9 /*left*/, (short) 21,
                (short) 11);
        assertEquals(31 + 9, view.getFlowStart(0));
    }

    public void testGetFlowSpan() {
        view.replace(0, view.getViewCount(), new View[] { view.createRow(), view.createRow() });
        assertEquals(0, view.firstLineIndent);
        assertEquals(Short.MAX_VALUE, view.layoutSpan);
        assertEquals(Short.MAX_VALUE, view.getFlowSpan(0));
        assertEquals(Short.MAX_VALUE, view.getFlowSpan(1));
        view.firstLineIndent = 31;
        assertEquals(Short.MAX_VALUE - 31, view.getFlowSpan(0));
        assertEquals(Short.MAX_VALUE, view.getFlowSpan(1));
        view.layoutSpan = 531;
        assertEquals(500, view.getFlowSpan(0));
        assertEquals(531, view.getFlowSpan(1));
        ((CompositeView) view.getView(0)).setInsets((short) 7, (short) 9, (short) 21,
                (short) 11);
        assertEquals(500 - 9 - 11, view.getFlowSpan(0));
    }

    public void testNextTabStop() {
        assertNull(view.getTabSet());
        assertEquals(72f, view.nextTabStop(0, 0), 1e-5f);
        assertEquals(72f, view.nextTabStop(71.9f, 0), 1e-5f);
        assertEquals(72f * 2, view.nextTabStop(72f, 0), 1e-5f);
    }

    public void testNextTabStopTabSet() {
        final TabSet tabSet = new TabSet(new TabStop[] { new TabStop(10f), new TabStop(12f),
                new TabStop(15f) });
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setTabSet(attrs, tabSet);
        doc.setParagraphAttributes(0, 1, attrs, false);
        assertSame(tabSet, view.getTabSet());
        assertEquals(10f, view.nextTabStop(0, 0), 1e-5f);
        assertEquals(10f, view.nextTabStop(9.99f, 0), 1e-5f);
        assertEquals(12f, view.nextTabStop(10f, 0), 1e-5f);
        assertEquals(12f, view.nextTabStop(11.99f, 0), 1e-5f);
        assertEquals(15f, view.nextTabStop(12f, 0), 1e-5f);
        assertEquals(15f, view.nextTabStop(14.99f, 0), 1e-5f);
        if (isHarmony()) {
            assertEquals(72f, view.nextTabStop(15f, 0), 1e-5f);
            assertEquals(72f, view.nextTabStop(20f, 0), 1e-5f);
            assertEquals(72f * 2, view.nextTabStop(72f, 0), 1e-5f);
        } else {
            assertEquals(20f, view.nextTabStop(15f, 0), 1e-5f);
            assertEquals(25f, view.nextTabStop(20f, 0), 1e-5f);
        }
        assertEquals(10f, view.nextTabStop(0, 4), 1e-5f);
        assertEquals(10f, view.nextTabStop(0, 5), 1e-5f);
        assertEquals(10f, view.nextTabStop(0, 15), 1e-5f);
        assertTrue(doc.getLength() + 1 < 50);
        assertEquals(10f, view.nextTabStop(0, 50), 1e-5f);
    }

    public void testNextTabStopTabSetTabBase() {
        final TabSet tabSet = new TabSet(new TabStop[] { new TabStop(10f), new TabStop(12f),
                new TabStop(15f) });
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setTabSet(attrs, tabSet);
        doc.setParagraphAttributes(0, 1, attrs, false);
        view.paint(createTestGraphics(), new Rectangle(11, 21, 431, 527));
        assertEquals(11, view.getTabBase(), 1e-5f);
        assertSame(tabSet, view.getTabSet());
        assertEquals(11 + 10f, view.nextTabStop(0, 0), 1e-5f);
        assertEquals(11 + 10f, view.nextTabStop(10f, 0), 1e-5f);
        assertEquals(11 + 10f, view.nextTabStop(20.99f, 0), 1e-5f);
        assertEquals(11 + 12f, view.nextTabStop(21f, 0), 1e-5f);
        assertEquals(11 + 15f, view.nextTabStop(23f, 0), 1e-5f);
    }

    public void testNextTabStopTabSetAlign() throws BadLocationException {
        doc.remove(0, doc.getLength());
        doc.insertString(0, "1\tleft\tcenter\tright\tdecimal 10.124\t"
        //  01 23456 7890123 456789 012345678901234
                //  0           1           2         3
                + "bar\tleft\tnext tab\tnext", null);
        //    5678 90123 456789012 3456
        //          4          5
        paragraph = root.getElement(0);
        final TabSet tabSet = new TabSet(new TabStop[] {
                new TabStop(5f, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
                new TabStop(20f, TabStop.ALIGN_CENTER, TabStop.LEAD_DOTS),
                new TabStop(39f, TabStop.ALIGN_RIGHT, TabStop.LEAD_EQUALS),
                new TabStop(61f, TabStop.ALIGN_DECIMAL, TabStop.LEAD_HYPHENS),
                new TabStop(71.9f, TabStop.ALIGN_BAR, TabStop.LEAD_NONE),
                new TabStop(79.213f, TabStop.ALIGN_LEFT, TabStop.LEAD_UNDERLINE), });
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setTabSet(attrs, tabSet);
        doc.setParagraphAttributes(0, 1, attrs, false);
        view = new ParagraphView(paragraph);
        loadChildren();
        assertSame(tabSet, view.getTabSet());
        assertEquals(5f, view.nextTabStop(0, 0), 1e-5f);
        assertEquals(20f, view.nextTabStop(5f, 0), 1e-5f);
        assertEquals(39f, view.nextTabStop(20f, 0), 1e-5f);
        assertEquals(61f, view.nextTabStop(39f, 0), 1e-5f);
        assertEquals(71.9f, view.nextTabStop(61f, 0), 1e-5f);
        assertEquals(79.213f, view.nextTabStop(71.9f, 0), 1e-5f);
        assertEquals(5f, view.nextTabStop(0, 0), 1e-5f);
        assertEquals(5f, view.nextTabStop(0, 1), 1e-5f);
        assertEquals(14f, view.nextTabStop(5f, 2), 1e-5f);
        assertEquals(16f, view.nextTabStop(5f, 3), 1e-5f);
        assertEquals(20f, view.nextTabStop(5f, 5), 1e-5f);
        assertEquals(8f, view.nextTabStop(5f, 6), 1e-5f);
    }

    public void testBreakView() {
        loadChilrenAndLayout();
        assertEquals(2, view.getViewCount());
        int width = (int) view.getPreferredSpan(View.X_AXIS);
        int height = (int) view.getPreferredSpan(View.Y_AXIS);
        assertEquals(64 /*WIDTH*/, width);
        assertEquals(PartView.CHAR_HEIGHT * 2, height);
        Shape alloc = new Rectangle(WIDTH, HEIGHT * 2);
        assertSame(view, view.breakView(View.X_AXIS, width / 2, alloc));
        assertSame(view, view.breakView(View.X_AXIS, width, alloc));
        assertSame(view, view.breakView(View.X_AXIS, width + 10, alloc));
        assertSame(view, view.breakView(View.Y_AXIS, PartView.CHAR_HEIGHT / 2, alloc));
        assertSame(view, view.breakView(View.Y_AXIS, PartView.CHAR_HEIGHT, alloc));
        assertSame(view, view.breakView(View.Y_AXIS, PartView.CHAR_HEIGHT + 10, alloc));
        assertSame(view, view.breakView(View.Y_AXIS, height, alloc));
        assertSame(view, view.breakView(View.Y_AXIS, height + 10, alloc));
    }

    public void testGetBreakWeight() {
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.X_AXIS, 1));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 1));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.X_AXIS, 0));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0));
        View row = view.createRow();
        view.append(row);
        row.append(new PartView(paragraph.getElement(0)));
        assertEquals(PartView.CHAR_HEIGHT, (int) row.getPreferredSpan(View.Y_AXIS));
        assertEquals(View.BadBreakWeight, view
                .getBreakWeight(View.Y_AXIS, PartView.CHAR_HEIGHT));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT + 0.01f));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT + 1f));
        view.append(row);
        assertEquals(View.BadBreakWeight, view
                .getBreakWeight(View.Y_AXIS, PartView.CHAR_HEIGHT));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT + 0.01f));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT * 2));
        assertFalse(view.isAllocationValid());
        loadChildren();
        view.layout(WIDTH, 30);
        assertTrue(view.isAllocationValid());
        assertEquals(2, view.getViewCount());
        row = view.getView(0);
        assertEquals(PartView.CHAR_HEIGHT, (int) row.getPreferredSpan(View.Y_AXIS));
        assertEquals(View.BadBreakWeight, view
                .getBreakWeight(View.Y_AXIS, PartView.CHAR_HEIGHT));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT + 0.01f));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT + 1f));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT * 2));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT * 2 + 0.01f));
        Graphics g = createTestGraphics();
        Rectangle area = new Rectangle(21, 13, WIDTH, HEIGHT);
        g.setClip(area);
        view.paint(g, area);
        assertEquals(PartView.CHAR_HEIGHT, (int) row.getPreferredSpan(View.Y_AXIS));
        assertEquals(View.BadBreakWeight, view
                .getBreakWeight(View.Y_AXIS, PartView.CHAR_HEIGHT));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT + 0.01f));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT + 1f));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT * 2));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT * 2 + 0.01f));
    }

    public void testGetBreakWeightSuper() throws Exception {
        View row = view.createRow();
        view.append(row);
        row.append(new PartView(paragraph.getElement(0)));
        assertEquals(PartView.CHAR_HEIGHT, (int) row.getPreferredSpan(View.Y_AXIS));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT));
        assertEquals(View.GoodBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT + 0.01f));
        assertEquals(View.GoodBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT + 1f));
        view.append(row);
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS,
                PartView.CHAR_HEIGHT, PartView.CHAR_HEIGHT));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT * 2));
        assertEquals(View.GoodBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT * 2 + 0.01f));
        assertFalse(view.isAllocationValid());
        loadChilrenAndLayout();
        assertTrue(view.isAllocationValid());
        assertEquals(2, view.getViewCount());
        row = view.getView(0);
        assertEquals(PartView.CHAR_HEIGHT, (int) row.getPreferredSpan(View.Y_AXIS));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT + 0.01f));
        assertEquals(View.BadBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT * 2));
        assertEquals(View.GoodBreakWeight, view.getBreakWeight(View.Y_AXIS, 0,
                PartView.CHAR_HEIGHT * 2 + 0.01f));
    }

    public void testSetJustification() {
        view.setJustification(-10);
        loadChilrenAndLayout();
        assertEquals(0, view.getOffset(View.X_AXIS, 0));
        assertEquals(0, view.getOffset(View.Y_AXIS, 0));
        assertEquals(WIDTH, view.getSpan(View.X_AXIS, 0));
        assertEquals(PartView.CHAR_HEIGHT, view.getSpan(View.Y_AXIS, 0));
        final View row = view.getView(1);
        assertEquals(0.5f, row.getAlignment(View.X_AXIS), 1e-5);
        assertEquals(0f, row.getAlignment(View.Y_AXIS), 1e-5);
        final int len = row.getEndOffset() - row.getStartOffset();
        assertEquals(PartView.CHAR_HEIGHT, view.getOffset(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, view.getSpan(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_WIDTH * len, view.getSpan(View.X_AXIS, 1));
        assertEquals((WIDTH - view.getSpan(View.X_AXIS, 1)) / 2, view.getOffset(View.X_AXIS, 1));
    }

    public void testSetJustificationLeft() {
        view.setJustification(StyleConstants.ALIGN_LEFT);
        loadChilrenAndLayout();
        assertEquals(0, view.getOffset(View.X_AXIS, 0));
        assertEquals(0, view.getOffset(View.Y_AXIS, 0));
        assertEquals(WIDTH, view.getSpan(View.X_AXIS, 0));
        assertEquals(PartView.CHAR_HEIGHT, view.getSpan(View.Y_AXIS, 0));
        final View row = view.getView(1);
        assertEquals(0f, row.getAlignment(View.X_AXIS), 1e-5);
        assertEquals(0f, row.getAlignment(View.Y_AXIS), 1e-5);
        final int len = row.getEndOffset() - row.getStartOffset();
        assertEquals(0, view.getOffset(View.X_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, view.getOffset(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_WIDTH * len, view.getSpan(View.X_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, view.getSpan(View.Y_AXIS, 1));
    }

    public void testSetJustificationCenter() {
        view.setJustification(StyleConstants.ALIGN_CENTER);
        loadChilrenAndLayout();
        assertEquals(0, view.getOffset(View.X_AXIS, 0));
        assertEquals(0, view.getOffset(View.Y_AXIS, 0));
        assertEquals(WIDTH, view.getSpan(View.X_AXIS, 0));
        assertEquals(PartView.CHAR_HEIGHT, view.getSpan(View.Y_AXIS, 0));
        final View row = view.getView(1);
        assertEquals(0.5f, row.getAlignment(View.X_AXIS), 1e-5);
        assertEquals(0f, row.getAlignment(View.Y_AXIS), 1e-5);
        final int len = row.getEndOffset() - row.getStartOffset();
        assertEquals((WIDTH - view.getSpan(View.X_AXIS, 1)) / 2, view.getOffset(View.X_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, view.getOffset(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_WIDTH * len, view.getSpan(View.X_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, view.getSpan(View.Y_AXIS, 1));
    }

    public void testSetJustificationRight() {
        view.setJustification(StyleConstants.ALIGN_RIGHT);
        loadChilrenAndLayout();
        assertEquals(0, view.getOffset(View.X_AXIS, 0));
        assertEquals(0, view.getOffset(View.Y_AXIS, 0));
        assertEquals(WIDTH, view.getSpan(View.X_AXIS, 0));
        assertEquals(PartView.CHAR_HEIGHT, view.getSpan(View.Y_AXIS, 0));
        final View row = view.getView(1);
        assertEquals(1.0f, row.getAlignment(View.X_AXIS), 1e-5);
        assertEquals(0f, row.getAlignment(View.Y_AXIS), 1e-5);
        final int len = row.getEndOffset() - row.getStartOffset();
        assertEquals(WIDTH - view.getSpan(View.X_AXIS, 1), view.getOffset(View.X_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, view.getOffset(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_WIDTH * len, view.getSpan(View.X_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, view.getSpan(View.Y_AXIS, 1));
    }

    public void testSetLineSpacing() {
        view.setLineSpacing(2);
        loadChilrenAndLayout();
        assertEquals(0, view.getOffset(View.X_AXIS, 0));
        assertEquals(0, view.getOffset(View.Y_AXIS, 0));
        assertEquals(WIDTH, view.getSpan(View.X_AXIS, 0));
        assertEquals(PartView.CHAR_HEIGHT * 3, view.getSpan(View.Y_AXIS, 0));
        final View row = view.getView(1);
        final int len = row.getEndOffset() - row.getStartOffset();
        assertEquals(0, view.getOffset(View.X_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT * 3, view.getOffset(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_WIDTH * len, view.getSpan(View.X_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT * 3, view.getSpan(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, (int) row.getView(0).getPreferredSpan(View.Y_AXIS));
        assertEquals(PartView.CHAR_HEIGHT * 3, (int) row.getPreferredSpan(View.Y_AXIS));
        assertEquals(0, ((CompositeView) row).getTopInset());
        assertEquals(PartView.CHAR_HEIGHT * 2, ((CompositeView) row).getBottomInset());
    }

    public void testSetLineSpacingFraction() {
        view.setLineSpacing(1.25f);
        loadChilrenAndLayout();
        assertEquals(0, view.getOffset(View.X_AXIS, 0));
        assertEquals(0, view.getOffset(View.Y_AXIS, 0));
        assertEquals(WIDTH, view.getSpan(View.X_AXIS, 0));
        assertEquals((int) (PartView.CHAR_HEIGHT * 2.25f), view.getSpan(View.Y_AXIS, 0));
        final View row = view.getView(1);
        final int len = row.getEndOffset() - row.getStartOffset();
        assertEquals(0, view.getOffset(View.X_AXIS, 1));
        assertEquals((int) (PartView.CHAR_HEIGHT * 2.25f), view.getOffset(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_WIDTH * len, view.getSpan(View.X_AXIS, 1));
        assertEquals((int) (PartView.CHAR_HEIGHT * 2.25f), view.getSpan(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, (int) row.getView(0).getPreferredSpan(View.Y_AXIS));
        assertEquals((int) (PartView.CHAR_HEIGHT * 2.25f), (int) row
                .getPreferredSpan(View.Y_AXIS));
    }

    public void testSetLineSpacingNegative() {
        view.setLineSpacing(-1f);
        loadChilrenAndLayout();
        assertEquals(0, view.getOffset(View.X_AXIS, 0));
        assertEquals(0, view.getOffset(View.Y_AXIS, 0));
        assertEquals(WIDTH, view.getSpan(View.X_AXIS, 0));
        assertEquals(0, view.getSpan(View.Y_AXIS, 0));
        final View row = view.getView(1);
        final int len = row.getEndOffset() - row.getStartOffset();
        assertEquals(0, view.getOffset(View.X_AXIS, 1));
        assertEquals(0, view.getOffset(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_WIDTH * len, view.getSpan(View.X_AXIS, 1));
        assertEquals(0, view.getSpan(View.Y_AXIS, 1));
        assertEquals(PartView.CHAR_HEIGHT, (int) row.getView(0).getPreferredSpan(View.Y_AXIS));
        assertEquals(0, (int) row.getPreferredSpan(View.Y_AXIS));
    }

    public void testSetFirstLineIndent() {
        assertEquals(0, view.firstLineIndent);
        view.setFirstLineIndent(1.2f);
        assertEquals(1, view.firstLineIndent);
        view.setFirstLineIndent(1.9f);
        assertEquals(1, view.firstLineIndent);
        view.setFirstLineIndent(2f);
        assertEquals(2, view.firstLineIndent);
    }

    public void testSetFirstLineIndentRow() {
        loadChilrenAndLayout();
        assertTrue(view.isAllocationValid());
        assertEquals(0, ((CompositeView) view.getView(0)).getLeftInset());
        assertEquals(WIDTH / PartView.CHAR_WIDTH, view.getView(0).getEndOffset());
        view.setFirstLineIndent(PartView.CHAR_WIDTH * 3);
        assertTrue(view.isAllocationValid());
        assertEquals(view.firstLineIndent, ((CompositeView) view.getView(0)).getLeftInset());
        view.layout(WIDTH, HEIGHT);
        assertEquals(WIDTH / PartView.CHAR_WIDTH, view.getView(0).getEndOffset());
        view.layoutChanged(View.X_AXIS);
        view.layout(WIDTH, HEIGHT);
        assertEquals(WIDTH / PartView.CHAR_WIDTH - 3, view.getView(0).getEndOffset());
    }

    public void testSetPropertiesFromAttributes() {
        final Marker jm = new Marker();
        final Marker flim = new Marker();
        final Marker lsm = new Marker();
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrs, -2);
        StyleConstants.setFirstLineIndent(attrs, 1.167f);
        StyleConstants.setLineSpacing(attrs, 1.324f);
        doc.setParagraphAttributes(0, 1, attrs, false);
        view = new ParagraphView(paragraph) {
            @Override
            protected void setJustification(int j) {
                jm.setOccurred();
                jm.setAuxiliary(new Integer(j));
            };

            @Override
            protected void setFirstLineIndent(float fi) {
                flim.setOccurred();
                flim.setAuxiliary(new Float(fi));
            };

            @Override
            protected void setLineSpacing(float ls) {
                lsm.setOccurred();
                lsm.setAuxiliary(new Float(ls));
            };
        };
        view.setPropertiesFromAttributes();
        assertTrue(jm.isOccurred());
        assertEquals(-2, ((Integer) jm.getAuxiliary()).intValue());
        assertTrue(flim.isOccurred());
        assertEquals(1.167f, ((Float) flim.getAuxiliary()).floatValue(), 1e-5);
        assertTrue(lsm.isOccurred());
        assertEquals(1.324f, ((Float) lsm.getAuxiliary()).floatValue(), 1e-5);
    }

    public void testGetLayoutViewCount() {
        assertNull(view.layoutPool);
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getLayoutViewCount();
            }
        });
        view.layoutPool = new BoxView(paragraph, View.Y_AXIS);
        ((CompositeView) view.layoutPool).loadChildren(new ChildrenFactory());
        assertTrue(paragraph.getElementCount() > 0);
        assertEquals(paragraph.getElementCount(), view.getLayoutViewCount());
    }

    public void testGetLayoutView() {
        assertNull(view.layoutPool);
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                view.getLayoutView(0);
            }
        });
        view.layoutPool = new BoxView(paragraph, View.Y_AXIS);
        ((CompositeView) view.layoutPool).loadChildren(new ChildrenFactory());
        assertTrue(paragraph.getElementCount() > 0);
        assertSame(view.layoutPool.getView(0), view.getLayoutView(0));
        assertSame(view.layoutPool.getView(1), view.getLayoutView(1));
    }

    public void testGetPartialSize() {
        loadChildren();
        View child = view.layoutPool.getView(0);
        assertTrue(child instanceof GlyphView);
        if (isHarmony()) {
            assertSame(view, ((GlyphView) child).getTabExpander());
        } else {
            assertNull(((GlyphView) child).getTabExpander());
        }
        assertTrue(view.layoutPool.getView(0) instanceof TabableView);
        assertTrue(view.layoutPool.getView(1) instanceof TabableView);
        GlyphView gv = (GlyphView) child;
        int start = gv.getStartOffset() + 1;
        int end = gv.getEndOffset() - 1;
        assertEquals(gv.getPartialSpan(start, end), view.getPartialSize(start, end), 1e-5f);
        float size = gv.getPartialSpan(start, end + 1);
        gv = (GlyphView) view.layoutPool.getView(1);
        size += gv.getPartialSpan(gv.getStartOffset(), gv.getEndOffset() - 1);
        assertEquals(size, view.getPartialSize(start, gv.getEndOffset() - 1), 1e-5f);
        view.layoutPool.replace(0, 2, new View[] { new ChildView(paragraph.getElement(0), 0),
                new ChildView(paragraph.getElement(1), 1) });
        assertFalse(view.layoutPool.getView(0) instanceof TabableView);
        assertFalse(view.layoutPool.getView(1) instanceof TabableView);
        size = view.layoutPool.getView(0).getPreferredSpan(View.X_AXIS)
                + view.layoutPool.getView(1).getPreferredSpan(View.X_AXIS);
        assertEquals(0, view.getPartialSize(start, gv.getEndOffset() - 1), 1e-5f);
        child = view.layoutPool.getView(0);
        assertEquals(child.getPreferredSpan(View.X_AXIS), view.getPartialSize(child
                .getStartOffset(), child.getEndOffset()), 1e-5f);
        assertEquals(0, view.getPartialSize(child.getStartOffset(), gv.getEndOffset() - 1),
                1e-5f);
        assertEquals(0, view.getPartialSize(child.getStartOffset() + 1, child.getEndOffset()),
                1e-5f);
        child = view.layoutPool.getView(1);
        assertEquals(child.getPreferredSpan(View.X_AXIS), view.getPartialSize(child
                .getStartOffset(), child.getEndOffset()), 1e-5f);
        assertEquals(0, view.getPartialSize(child.getStartOffset(), child.getEndOffset() - 1),
                1e-5f);
        assertEquals(size, view.getPartialSize(view.layoutPool.getView(0).getStartOffset(),
                view.layoutPool.getView(1).getEndOffset()), 1e-5);
    }

    public void testFindOffsetToCharactersInString() throws BadLocationException {
        // 0123456789012345
        assertEquals("plainbolditalic\n", doc.getText(view.getStartOffset(), view
                .getEndOffset()));
        char[] chars = "abcd".toCharArray();
        int[] offset = new int[] {
        //  0  1  2  3  4
                2, 2, 2, 5, 5,
                //  5  6  7  8  9
                5, 8, 8, 8, 11,
                //  10  11  12  13  14
                11, 11, 14, 14, 14, -1, -1 };
        for (int i = 0; i < offset.length; i++) {
            assertEquals("@ " + i, offset[i], view.findOffsetToCharactersInString(chars, i));
        }
        assertEquals(-1, view.findOffsetToCharactersInString("e\t.,".toCharArray(), 0));
    }

    public void testGetTabSet() {
        assertFalse(view.getAttributes().isDefined(StyleConstants.TabSet));
        assertNull(view.getTabSet());
        final TabSet tabSet = new TabSet(new TabStop[] { new TabStop(10f), new TabStop(12f),
                new TabStop(15f) });
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setTabSet(attrs, tabSet);
        doc.setParagraphAttributes(0, 1, attrs, false);
        assertTrue(view.getAttributes().isDefined(StyleConstants.TabSet));
        assertSame(tabSet, view.getTabSet());
    }

    public void testGetTabBase() {
        assertEquals(0, view.getLeftInset());
        assertEquals(0, (int) view.getTabBase());
        AttributeSet attrs = getInsetsAttributeSet();
        doc.setParagraphAttributes(0, 1, attrs, false);
        view.setPropertiesFromAttributes();
        assertEquals(4, view.getLeftInset());
        assertEquals(0, view.getTabBase(), 1e-5f);
        view.paint(createTestGraphics(), new Rectangle(7, 10, WIDTH, HEIGHT));
        assertEquals(7 + 4, (int) view.getTabBase());
    }

    //    public void testGetClosestPositionTo() {
    //
    //    }
    public void testGetNextNorthSouthVisualPositionFrom() throws BadLocationException {
        final class Params {
            boolean valid;

            int pos;

            Bias b;

            int dir;

            int row;

            int x;

            public void check(int pos, Bias b, int dir, int row, int x) {
                assertTrue("valid", valid);
                assertEquals("offset", pos, this.pos);
                assertSame("bias", b, this.b);
                assertEquals("direction", dir, this.dir);
                assertEquals("row", row, this.row);
                assertEquals("x", x, this.x);
                valid = false;
            }
        }
        final Rectangle alloc = new Rectangle(11, 7, WIDTH, HEIGHT);
        final JTextArea textArea = new JTextArea();
        final Params params = new Params();
        view = new ParagraphView(paragraph) {
            @Override
            protected int getClosestPositionTo(int pos, Bias b, Shape a, int dir,
                    Bias[] biasRet, int rowIndex, int x) throws BadLocationException {
                params.valid = true;
                params.pos = pos;
                params.b = b;
                assertSame(alloc, a);
                params.dir = dir;
                params.row = rowIndex;
                params.x = x;
                return 0;
            }

            @Override
            public Container getContainer() {
                return textArea;
            }
        };
        loadChilrenAndLayout();
        Bias[] bias = new Bias[1];
        view.getNextNorthSouthVisualPositionFrom(0, Bias.Forward, alloc, SwingConstants.SOUTH,
                bias);
        params.check(0, Bias.Forward, SwingConstants.SOUTH, 1, 0);
        view.getNextNorthSouthVisualPositionFrom(1, Bias.Forward, alloc, SwingConstants.SOUTH,
                bias);
        params.check(1, Bias.Forward, SwingConstants.SOUTH, 1, 0);
        view.getNextNorthSouthVisualPositionFrom(1, Bias.Forward, alloc, SwingConstants.NORTH,
                bias);
        assertFalse(params.valid);
        view.getNextNorthSouthVisualPositionFrom(0, Bias.Forward, alloc, SwingConstants.NORTH,
                bias);
        assertFalse(params.valid);
        final int end = view.getEndOffset() - 3;
        view.getNextNorthSouthVisualPositionFrom(end, Bias.Forward, alloc,
                SwingConstants.SOUTH, bias);
        assertFalse(params.valid);
        view.getNextNorthSouthVisualPositionFrom(end - 1, Bias.Forward, alloc,
                SwingConstants.SOUTH, bias);
        assertFalse(params.valid);
        view.getNextNorthSouthVisualPositionFrom(end - 1, Bias.Forward, alloc,
                SwingConstants.NORTH, bias);
        params.check(end - 1, Bias.Forward, SwingConstants.NORTH, 0, 0);
        view.getNextNorthSouthVisualPositionFrom(end, Bias.Forward, alloc,
                SwingConstants.NORTH, bias);
        params.check(end, Bias.Forward, SwingConstants.NORTH, 0, 0);
    }

    public void testGetAttributesRow() {
        final View row = view.createRow();
        assertNull(row.getAttributes());
        row.setParent(view);
        assertSame(view.getAttributes(), row.getAttributes());
    }

    private static AttributeSet getInsetsAttributeSet() {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setSpaceAbove(attrs, 9.53f);
        StyleConstants.setLeftIndent(attrs, 4.25f);
        StyleConstants.setSpaceBelow(attrs, 1.41f);
        StyleConstants.setRightIndent(attrs, 7.12f);
        return attrs;
    }

    private void loadChildren() {
        view.loadChildren(null);
        ((CompositeView) view.layoutPool).loadChildren(new PartFactory());
    }

    private void loadChilrenAndLayout() {
        loadChildren();
        view.layout(WIDTH, HEIGHT);
    }
}
