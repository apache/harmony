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

import javax.swing.BasicSwingTestCase;
import javax.swing.text.CompositeViewTest.CompositeViewImpl;
import junit.framework.TestCase;

public class CompositeViewRTest extends TestCase {
    private PlainDocument doc; // Document used in tests

    private Element root; // Default root element of the document

    private CompositeView view; // View object used in tests

    private ViewFactory factory; // View factory used to create new views

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "line1\nline2\n\u05DC\u05DD\nline3\n", null);
        // positions:        012345 678901 2     3     4 567890
        //                   0          1                     2
        view = new CompositeViewImpl(root = doc.getDefaultRootElement());
        view.loadChildren(factory = new ViewFactory() {
            public View create(final Element line) {
                return new PlainView(line);
            }
        });
    }

    /**
     * NullPointerException is thrown if <code>replace</code>
     * is called when <code>views</code> parameter is <code>null</code>.
     */
    public void testReplace02() {
        final int count = view.getViewCount();
        view.replace(0, 0, null);
        assertEquals(count, view.getViewCount());
        assertTrue(count > 0);
        view.replace(0, view.getViewCount(), null); // = removeAll()
        assertEquals(0, view.getViewCount());
    }

    public void testReplace03() throws Exception {
        View child = view.getView(0);
        assertSame(view, child.getParent());
        // This removes and places the same view at the same place
        view.replace(0, 1, new View[] { child });
        assertSame(view, child.getParent());
    }

    public void testReplace04() throws Exception {
        View child = view.getView(0);
        assertSame(view, child.getParent());
        View parent = new PlainView(root);
        child.setParent(parent);
        assertSame(parent, child.getParent());
        view.remove(0);
        assertSame(parent, child.getParent());
    }

    /**
     * <code>loadChildren</code> doesn't call <code>replace</code>.
     */
    public void testLoadChildren02() {
        final boolean[] called = new boolean[] { false };
        view = new CompositeViewImpl(root) {
            @Override
            public void replace(final int index, final int length, final View[] views) {
                called[0] = true;
                assertEquals(0, index);
                if (BasicSwingTestCase.isHarmony()) {
                    assertEquals(getViewCount(), length);
                } else {
                    assertEquals(0, length);
                }
                assertEquals(root.getElementCount(), views.length);
                super.replace(index, length, views);
            }
        };
        view.loadChildren(factory);
        assertTrue(called[0]);
        called[0] = false;
        assertEquals(root.getElementCount(), view.getViewCount());
        assertTrue(view.getViewCount() > 0);
        view.loadChildren(factory);
        assertTrue(called[0]);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(root.getElementCount(), view.getViewCount());
        } else {
            assertEquals(2 * root.getElementCount(), view.getViewCount());
        }
    }
}
