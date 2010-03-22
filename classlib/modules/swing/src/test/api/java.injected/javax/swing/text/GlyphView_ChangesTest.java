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

import java.awt.Rectangle;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import junit.framework.TestCase;

/**
 * Tests GlyphView methods which react to changes in a document.
 *
 */
public class GlyphView_ChangesTest extends TestCase implements DocumentListener {
    private static final class PreferenceChange {
        private View child;

        private boolean width;

        private boolean height;

        void setData(final View child, final boolean width, final boolean height) {
            this.child = child;
            this.width = width;
            this.height = height;
        }

        void check(final View child, final boolean width, final boolean height) {
            assertSame("Child", child, this.child);
            assertEquals("Width", width, this.width);
            assertEquals("Height", height, this.height);
        }
    }

    private GlyphView[] views;

    private StyledDocument doc;

    private Element root;

    private Element branch;

    private Element[] leaves;

    private BoxView boxView;

    private DocumentEvent event;

    private static final PreferenceChange change = new PreferenceChange();

    private static final String TEXT = "abcdef012345lalala";

    //                                  012345678901234567
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        doc.insertString(0, TEXT, null);
        doc.setCharacterAttributes(6, 12, SimpleAttributeSet.EMPTY, false);
        branch = root.getElement(0);
        leaves = new Element[branch.getElementCount()];
        boxView = new BoxView(branch, View.X_AXIS) {
            @Override
            public void preferenceChanged(final View child, final boolean width,
                    final boolean height) {
                change.setData(child, width, height);
                super.preferenceChanged(child, width, height);
            }
        };
        views = new GlyphView[leaves.length];
        for (int i = 0; i < views.length; i++) {
            views[i] = new GlyphView(leaves[i]);
        }
        boxView.replace(0, 0, views);
        doc.addDocumentListener(this);
    }

    public void testInsertUpdate() throws BadLocationException {
        doc.insertString(1, "^", null);
        for (int i = 0; i < views.length; i++) {
            views[i].insertUpdate(event, new Rectangle(), null);
            change.check(views[i], true, false);
        }
    }

    public void testRemoveUpdate() throws BadLocationException {
        doc.remove(0, 1);
        for (int i = 0; i < views.length; i++) {
            views[i].removeUpdate(event, new Rectangle(), null);
            change.check(views[i], true, false);
        }
    }

    public void testChangedUpdate() {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute("key", "value");
        doc.setParagraphAttributes(0, 1, attrs, false);
        for (int i = 0; i < views.length; i++) {
            views[i].changedUpdate(event, new Rectangle(), null);
            change.check(views[i], true, true);
        }
    }

    public void insertUpdate(DocumentEvent e) {
        event = e;
    }

    public void removeUpdate(DocumentEvent e) {
        event = e;
    }

    public void changedUpdate(DocumentEvent e) {
        event = e;
    }
}
