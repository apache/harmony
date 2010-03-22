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
import javax.swing.SwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.PlainViewI18N_BidiTextViewTest.PreferenceChange;
import javax.swing.text.PlainViewI18N_LineViewTest.PlainViewI18NWithTextArea;

/**
 * Tests PlainViewI18N.LineView class, in particular how it reacts to changes
 * in a document.
 */
public class PlainViewI18N_LineView_UpdateTest extends SwingTestCase implements
        DocumentListener {
    private PlainDocument doc;

    private Element root;

    private Element bidi;

    private PlainViewI18N parent;

    private PlainViewI18N.LineView view;

    private PreferenceChange preferenceParams;

    private DocumentEvent insertEvent;

    private DocumentEvent removeEvent;

    private Element line;

    private int startOffset;

    private final Rectangle shape = new Rectangle(27, 74, 91, 41);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!isHarmony()) {
            return;
        }
        doc = new PlainDocument();
        doc.insertString(0, PlainViewI18N_LineViewTest.defText, null);
        root = doc.getDefaultRootElement();
        bidi = doc.getBidiRootElement();
        parent = new PlainViewI18NWithTextArea(root, doc);
        line = root.getElement(3);
        startOffset = line.getStartOffset();
        view = parent.new LineView(line) {
            @Override
            public void preferenceChanged(View child, boolean w, boolean h) {
                preferenceParams = new PreferenceChange(child, w, h);
            }
        };
        view.setParent(parent);
        doc.addDocumentListener(this);
    }

    public void testInsertUpdateLTRNoChange() throws Exception {
        if (!isHarmony()) {
            return;
        }
        doc.insertString(startOffset + PlainViewI18N_LineViewTest.RTLLength + 1,
                PlainViewI18N_LineViewTest.LTR, null);
        view.insertUpdate(insertEvent, shape, null);
        assertNull(insertEvent.getChange(bidi));
        preferenceParams.check(view.getView(1), true, false);
    }

    public void testInsertUpdateRTLNoChange() throws Exception {
        if (!isHarmony()) {
            return;
        }
        doc.insertString(startOffset + 1, PlainViewI18N_LineViewTest.RTL, null);
        view.insertUpdate(insertEvent, shape, null);
        assertNull(insertEvent.getChange(bidi));
        preferenceParams.check(view.getView(0), true, false);
    }

    public void testInsertUpdateLTRWithChange() throws Exception {
        if (!isHarmony()) {
            return;
        }
        doc.insertString(startOffset + 1, PlainViewI18N_LineViewTest.LTR, null);
        view.insertUpdate(insertEvent, shape, null);
        ElementChange change = insertEvent.getChange(bidi);
        assertNotNull(change);
        assertEquals(3, change.getChildrenRemoved().length);
        assertEquals(6, change.getChildrenAdded().length);
        assertEquals(6, view.getViewCount());
    }

    public void testInsertUpdateRTLWithChange() throws Exception {
        if (!isHarmony()) {
            return;
        }
        doc.insertString(startOffset + PlainViewI18N_LineViewTest.RTLLength + 1,
                PlainViewI18N_LineViewTest.RTL, null);
        view.insertUpdate(insertEvent, shape, null);
        ElementChange change = insertEvent.getChange(bidi);
        assertNotNull(change);
        assertEquals(3, change.getChildrenRemoved().length);
        assertEquals(6, change.getChildrenAdded().length);
        assertEquals(6, view.getViewCount());
    }

    // The tests ...NoChange are invalid 'cause the structure is changed
    // tho' it needn't have been modified. If document is optimized to
    // handle this situation, these tests may become actual.
    /*
     public void testRemoveUpdateLTRNoChange() throws Exception {
     doc.remove(startOffset + PlainViewI18N_LineViewTest.RTLLength + 1, 1);

     view.removeUpdate(removeEvent, shape, null);
     assertNull(removeEvent.getChange(bidi));
     preferenceParams.check(view.getView(1), true, false);
     }

     public void testRemoveUpdateRTLNoChange() throws Exception {
     doc.remove(startOffset + 1, 1);

     view.removeUpdate(removeEvent, shape, null);
     assertNull(removeEvent.getChange(bidi));
     preferenceParams.check(view.getView(0), true, false);
     }
     */
    public void testRemoveUpdateWithChange() throws Exception {
        if (!isHarmony()) {
            return;
        }
        doc.remove(startOffset + 1, 2);
        view.removeUpdate(removeEvent, shape, null);
        ElementChange change = removeEvent.getChange(bidi);
        assertNotNull(change);
        assertEquals(3, change.getChildrenRemoved().length);
        assertEquals(4, change.getChildrenAdded().length);
        assertEquals(4, view.getViewCount());
    }

    public void changedUpdate(DocumentEvent event) {
    }

    public void insertUpdate(DocumentEvent event) {
        insertEvent = event;
    }

    public void removeUpdate(DocumentEvent event) {
        removeEvent = event;
    }
}
