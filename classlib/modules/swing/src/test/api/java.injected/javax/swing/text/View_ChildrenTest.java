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

import javax.swing.text.ViewTest.DisAbstractedView;
import junit.framework.TestCase;

/**
 * Tests View class methods which manage child views.
 *
 */
public class View_ChildrenTest extends TestCase {
    private Document doc;

    private Element line;

    private boolean replaceIsCalled = false;

    private int replaceLength;

    private int replaceOffset;

    private View[] replaceViews;

    private View view;

    private View viewToAdd;

    public void testAppend() {
        view.append(viewToAdd);
        assertTrue(replaceIsCalled);
        assertEquals(view.getViewCount(), replaceOffset);
        assertEquals(0, replaceLength);
        assertEquals(1, replaceViews.length);
        assertSame(viewToAdd, replaceViews[0]);
    }

    public void testInsert() {
        view.insert(0, viewToAdd);
        assertTrue(replaceIsCalled);
        assertEquals(0, replaceOffset);
        assertEquals(0, replaceLength);
        assertEquals(1, replaceViews.length);
        assertSame(viewToAdd, replaceViews[0]);
        replaceIsCalled = false; // Reset the flag
        view.insert(2, viewToAdd);
        assertTrue(replaceIsCalled);
        assertEquals(2, replaceOffset);
        assertEquals(0, replaceLength);
        assertEquals(1, replaceViews.length);
        assertSame(viewToAdd, replaceViews[0]);
    }

    public void testRemove() {
        view.remove(0);
        assertTrue(replaceIsCalled);
        assertEquals(0, replaceOffset);
        assertEquals(1, replaceLength);
        assertNull(replaceViews);
        replaceIsCalled = false; // Reset the flag
        view.remove(2);
        assertTrue(replaceIsCalled);
        assertEquals(2, replaceOffset);
        assertEquals(1, replaceLength);
        assertNull(replaceViews);
    }

    public void testRemoveAll() {
        view.removeAll();
        assertTrue(replaceIsCalled);
        assertEquals(0, replaceOffset);
        assertEquals(view.getViewCount(), replaceLength);
        assertNull(replaceViews);
    }

    public void testReplace() {
        assertEquals(0, view.getViewCount());
        View viewsToAdd[] = new View[3];
        view.replace(5, 7, viewsToAdd);
        assertTrue(replaceIsCalled);
        assertEquals(5, replaceOffset);
        assertEquals(7, replaceLength);
        assertEquals(3, replaceViews.length);
        assertSame(viewsToAdd, replaceViews);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "01234\nabcde", null);
        line = doc.getDefaultRootElement().getElement(1);
        view = new DisAbstractedView(line) {
            @Override
            public void replace(final int offset, final int length, final View[] views) {
                replaceIsCalled = true;
                replaceOffset = offset;
                replaceLength = length;
                replaceViews = views;
            }
        };
        viewToAdd = ViewTest.getLine1View(doc);
    }
}