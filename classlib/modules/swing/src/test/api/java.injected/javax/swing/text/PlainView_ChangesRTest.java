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

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainView_ChangesTest.LineRange;

public class PlainView_ChangesRTest extends BasicSwingTestCase implements DocumentListener {
    private boolean callSuperDamageRange;

    private Container container;

    private Document doc;

    private DocumentEvent event;

    private LineRange lineRange;

    private Rectangle paintRect;

    private Element root;

    private Shape shape;

    private PlainView view;

    /**
     * Tests for <code>PlainView.insertUpdate</code> throws NPE.
     * <p>
     * If a change occured outside the widest line and document structure
     * was not changed, <code>insertUpdate</code> throws NPE
     * when processing the notification.
     */
    public void testInsertUpdateNPE() throws BadLocationException {
        doc.insertString(0, "1:0123\n2:\n3:abcdefg", null);
        view.updateMetrics();
        doc.insertString(0, "^", null);
        view.insertUpdate(event, shape, null);
    }

    /**
     * Tests for <code>PlainView</code> repaints more lines
     * than necessary.
     * <p>
     * If a character (not a new line) is inserted into document,
     * document structure will not change, or in other words the number of
     * text lines will stay the same.
     * In this case only the modified line needs to be repainted
     * but <code>PlainView</code> repaints all following lines as well.
     */
    public void testInsertUpdateExtraRepaint() throws BadLocationException {
        doc.insertString(0, "1:0123\n2:\n3:abcdefg", null);
        view.updateMetrics();
        doc.insertString(0, "^", null);
        view.insertUpdate(event, shape, null);
        lineRange.check(0, 0, shape, container);
    }

    /**
     * Tests for calling <code>nextTabStop()</code> leaves
     * <code>PlainView.metrics</code> field <code>null</code>.
     * This is because <code>nextTabStop()</code> forwards to
     * <code>paintParams</code> and updates <code>metrics</code> there,
     * and <code>setSize()</code> doesn't update the field in
     * the <code>PlainView</code>.
     */
    public void testNextTabStop05() {
        assertNull(view.metrics);
        if (isHarmony()) {
            // Call to nextTabStop will lead to updateMetrics()
            assertTrue("nextTabStop() must return value > 0", view.nextTabStop(0, 0) > view
                    .getTabSize());
        }
        view.setSize(100, 200);
        assertNotNull("Metrics must be not null after setSize", view.metrics);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        root = doc.getDefaultRootElement();
        view = new PlainView(root) {
            @Override
            public Container getContainer() {
                if (container == null) {
                    container = new JTextArea() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void repaint(final int x, final int y, final int w, final int h) {
                            if (paintRect == null) {
                                paintRect = new Rectangle(x, y, w, h);
                            } else {
                                paintRect.add(new Rectangle(x, y, w, h));
                            }
                        }
                    };
                }
                return container;
            }

            @Override
            protected void damageLineRange(final int line0, final int line1, final Shape shape,
                    final Component host) {
                if (callSuperDamageRange) {
                    super.damageLineRange(line0, line1, shape, host);
                } else {
                    lineRange = new LineRange(line0, line1, shape, host);
                }
            }
        };
        shape = new Rectangle(500, 500);
        doc.addDocumentListener(this);
    }

    public void changedUpdate(final DocumentEvent changes) {
        fail("changedUpdate isn't supposed to be called");
    }

    public void insertUpdate(final DocumentEvent changes) {
        event = changes;
    }

    public void removeUpdate(final DocumentEvent changes) {
        event = changes;
    }
}
