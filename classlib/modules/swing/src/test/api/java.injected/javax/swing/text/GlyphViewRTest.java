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
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import javax.swing.BasicSwingTestCase;
import javax.swing.text.FlowView_FlowStrategyTest.PartFactory;

public class GlyphViewRTest extends BasicSwingTestCase {
    private GlyphView view;

    private StyledDocument doc;

    private Element root;

    private Element leaf;

    private Font font;

    private FontMetrics metrics;

    private float width;

    private static final String FULL_TEXT = "this text to check how view breaks";

    //   0123456789012345678901234567890123
    private static final int startOffset = 5;

    private static final int endOffset = 28;

    private static final int length = endOffset - startOffset;

    private static final String LEAF_TEXT = FULL_TEXT.substring(startOffset, endOffset);

    private static final int X_AXIS = View.X_AXIS;

    @SuppressWarnings("deprecation")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        doc.insertString(0, FULL_TEXT, null);
        doc.setCharacterAttributes(startOffset, length, SimpleAttributeSet.EMPTY, false);
        leaf = root.getElement(0).getElement(1);
        view = new GlyphView(leaf);
        font = view.getFont();
        metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
    }

    /**
     * Tries to breakes the view when the view fits in the length.
     */
    public void testBreakView() {
        View part;
        width = metrics.stringWidth(LEAF_TEXT);
        part = view.breakView(X_AXIS, startOffset, 0, width);
        assertEquals(startOffset, part.getStartOffset());
        assertEquals(endOffset, part.getEndOffset());
        assertSame(view, part);
        part = view.breakView(X_AXIS, startOffset + 1, 0, width);
        assertEquals(startOffset + 1, part.getStartOffset());
        assertEquals(endOffset, part.getEndOffset());
        assertNotSame(view, part);
    }

    public void testGetTabExpander() {
        assertNull(view.getParent());
        assertNull(view.getTabExpander());
        ParagraphView paragraphView = new ParagraphView(root.getElement(0));
        paragraphView.loadChildren(null);
        ((CompositeView) paragraphView.layoutPool).loadChildren(new PartFactory());
        View child = paragraphView.layoutPool.getView(0);
        assertTrue(child instanceof GlyphView);
        assertSame(paragraphView, child.getParent().getParent());
        if (isHarmony()) {
            assertSame(paragraphView, ((GlyphView) child).getTabExpander());
        } else {
            assertNull(((GlyphView) child).getTabExpander());
        }
    }
}
