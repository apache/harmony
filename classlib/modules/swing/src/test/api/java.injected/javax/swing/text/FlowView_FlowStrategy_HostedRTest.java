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
import java.awt.Rectangle;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.FlowView.FlowStrategy;
import javax.swing.text.FlowViewTest.FlowViewImplWithFactory;
import javax.swing.text.FlowView_FlowStrategyTest.PartFactory;

/**
 * This class tests the behavior of <code>{insert, remove, changed}Update</code>
 * methods in <code>FlowView.FlowStrategy</code> in situation where FlowView
 * has associated container.
 *
 */
public class FlowView_FlowStrategy_HostedRTest extends SwingTestCase implements
        DocumentListener {
    private DefaultStyledDocument doc;

    private Element p1;

    private DefaultDocumentEvent event;

    private FlowView view;

    private FlowStrategy strategy;

    private JTextArea textArea;

    private Rectangle alloc;

    private Rectangle paintRect;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        doc.insertString(0, "test text\n2nd par", null);
        p1 = doc.getDefaultRootElement().getElement(0);
        textArea = new JTextArea(doc) {
            private static final long serialVersionUID = 1L;

            @Override
            public void repaint(int x, int y, int width, int height) {
                paintRect = new Rectangle(x, y, width, height);
            }
        };
        view = new FlowViewImplWithFactory(p1, View.Y_AXIS, new PartFactory()) {
            @Override
            public Container getContainer() {
                return textArea;
            }
        };
        strategy = view.strategy;
        view.loadChildren(null);
        doc.addDocumentListener(this);
        alloc = new Rectangle(12, 17, 125, 541);
        paintRect = null;
        view.layout(alloc.width, alloc.height);
        assertTrue(view.isAllocationValid());
    }

    public void testInsertUpdate() throws BadLocationException {
        doc.insertString(p1.getStartOffset() + 1, "^^^", null);
        strategy.insertUpdate(view, event, alloc);
        assertEquals(alloc, paintRect);
        assertTrue(view.isAllocationValid());
    }

    public void testRemoveUpdate() throws BadLocationException {
        doc.remove(p1.getStartOffset() + 1, 3);
        paintRect = null;
        strategy.removeUpdate(view, event, alloc);
        assertEquals(alloc, paintRect);
        assertTrue(view.isAllocationValid());
    }

    public void testChangedUpdate() throws BadLocationException {
        doc.setCharacterAttributes(p1.getStartOffset(), 3, doc.getAttributeContext()
                .getEmptySet(), true);
        strategy.changedUpdate(view, event, alloc);
        assertEquals(alloc, paintRect);
        assertTrue(view.isAllocationValid());
    }

    public void insertUpdate(DocumentEvent e) {
        event = (DefaultDocumentEvent) e;
    }

    public void removeUpdate(DocumentEvent e) {
        event = (DefaultDocumentEvent) e;
    }

    public void changedUpdate(DocumentEvent e) {
        event = (DefaultDocumentEvent) e;
    }
}
