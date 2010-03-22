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

import java.awt.Container;
import java.awt.FontMetrics;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;

/**
 * Tests PlainViewI18N class.
 */
public class PlainViewI18NTest extends SwingTestCase {
    private JTextArea area;

    private Document doc;

    private int line2Width;

    private int lineCount;

    private FontMetrics metrics;

    private Element root;

    private PlainViewI18N view;

    public void testGetMaximumSpan() throws Exception {
        if (!isHarmony()) {
            return;
        }
        assertEquals(lineCount * metrics.getHeight(), (int) view.getMaximumSpan(View.Y_AXIS));
        assertEquals(Integer.MAX_VALUE, (int) view.getMaximumSpan(View.X_AXIS));
    }

    public void testGetMinimumSpan() throws Exception {
        if (!isHarmony()) {
            return;
        }
        assertEquals(lineCount * metrics.getHeight(), (int) view.getMinimumSpan(View.Y_AXIS));
        assertEquals(line2Width, (int) view.getMinimumSpan(View.X_AXIS));
    }

    public void testGetPreferredSpan() throws Exception {
        if (!isHarmony()) {
            return;
        }
        assertEquals(lineCount * metrics.getHeight(), (int) view.getPreferredSpan(View.Y_AXIS));
        assertEquals(line2Width, (int) view.getPreferredSpan(View.X_AXIS));
    }

    public void testGetViewFactory() {
        if (!isHarmony()) {
            return;
        }
        assertNull(view.getParent());
        assertNotNull(view.getViewFactory());
    }

    public void testNextTabStop() throws Exception {
        if (!isHarmony()) {
            return;
        }
        Object tabSizeProperty = doc.getProperty(PlainDocument.tabSizeAttribute);
        int tabSize = ((Integer) tabSizeProperty).intValue();
        int tabStop = metrics.charWidth('m') * tabSize;
        assertEquals(tabStop, (int) view.nextTabStop(0, 0));
        assertEquals(tabStop, (int) view.nextTabStop(tabStop / 2, 0));
        assertEquals(2 * tabStop, (int) view.nextTabStop(tabStop, 0));
    }

    public void testPlainViewI18N() {
        if (!isHarmony()) {
            return;
        }
        view = new PlainViewI18N(root);
        assertEquals(View.Y_AXIS, view.getAxis());
        assertEquals(0, view.getViewCount());
        view.loadChildren(view.getViewFactory());
        assertEquals(root.getElementCount(), view.getViewCount());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!isHarmony()) {
            return;
        }
        doc = new PlainDocument();
        doc.insertString(0, "line1\nline two", null);
        root = doc.getDefaultRootElement();
        view = new PlainViewI18N(root) {
            @Override
            public Container getContainer() {
                return area;
            }
        };
        view.loadChildren(view.getViewFactory());
        area = new JTextArea();
        metrics = area.getFontMetrics(area.getFont());
        lineCount = root.getElementCount();
        Element line2 = root.getElement(1);
        line2Width = metrics.stringWidth(doc.getText(line2.getStartOffset(), line2
                .getEndOffset()
                - line2.getStartOffset()));
    }
}
