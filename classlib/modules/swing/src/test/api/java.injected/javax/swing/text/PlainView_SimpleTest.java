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
import java.awt.Graphics;
import javax.swing.JTextArea;
import junit.framework.TestCase;

/**
 * Tests methods that don't require the proper view initialization.
 *
 * <p>This class uses very simple initialization. It is not valid to
 * call methods that depend on the "real" state of the view (assigned
 * to a text component).
 *
 */
public class PlainView_SimpleTest extends TestCase {
    private JTextArea area;

    private Document doc;

    private Element root;

    private boolean updateMetricsCalled;

    private PlainView view;

    public void testGetLineBuffer() {
        Segment buffer = view.getLineBuffer();
        assertNotNull(buffer);
        // An instance always returns the same line buffer
        assertSame(buffer, view.getLineBuffer());
        // Another instance returns distinct line buffer
        assertNotSame(buffer, new PlainView(root).getLineBuffer());
    }

    public void testGetTabSize() {
        assertEquals(8, view.getTabSize());
        doc.putProperty(PlainDocument.tabSizeAttribute, new Integer(4));
        assertEquals(4, view.getTabSize());
    }

    public void testPlainView() {
        view = new PlainView(null);
        assertNull(view.getElement());
        view = new PlainView(root);
        assertSame(root, view.getElement());
        assertNull(view.metrics); // metrics are lazily initialized
        view = new PlainView(root) {
            @Override
            public Container getContainer() {
                return area;
            }

            @Override
            public Graphics getGraphics() {
                return area.getGraphics();
            }
        };
        assertSame(root, view.getElement());
        assertNull(view.metrics);
    }

    public void testSetSize() {
        view = new PlainView(root) {
            @Override
            public Container getContainer() {
                return new JTextArea();
            }

            @Override
            protected void updateMetrics() {
                updateMetricsCalled = true;
            }
        };
        assertFalse(updateMetricsCalled);
        view.setSize(500, 500);
        assertTrue("setSize is expected to call updateMetrics", updateMetricsCalled);
    }

    /**
     * Creates PlainDocument, PlainView on <code>doc</code>'s
     * default root, and rectangular shape.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        root = doc.getDefaultRootElement();
        view = new PlainView(root);
    }
}