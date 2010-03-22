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
import javax.swing.text.CompositeView_ModelViewTest.WithChildrenView;
import junit.framework.TestCase;

public class View_GetViewIndexTest extends TestCase {
    private PlainDocument doc; // Document used in tests

    private ViewFactory factory; // View factory used to create new views

    private Element root; // Default root element of the document

    private Rectangle shape; // View allocation (area to render into)

    private CompositeView view; // View object used in tests

    private static final int LINE_HEIGHT = CompositeView_ModelViewTest.LINE_HEIGHT;

    /**
     * Tests int View.getViewIndex(float, float, Shape)
     */
    public void testGetViewIndexFloatFloatShape() {
        assertEquals(-1, view.getViewIndex(0, 0, shape));
        assertEquals(0, view.getViewIndex(shape.x, shape.y, shape));
        assertEquals(0, view.getViewIndex(shape.x + shape.width - 1, shape.y + LINE_HEIGHT - 1,
                shape));
        assertEquals(-1, view.getViewIndex(shape.x + shape.width, shape.y + LINE_HEIGHT, shape));
        assertEquals(-1, view.getViewIndex(shape.x - 1, shape.y + LINE_HEIGHT, shape));
        assertEquals(1, view.getViewIndex(shape.x, shape.y + LINE_HEIGHT, shape));
        assertEquals(1, view.getViewIndex(shape.x + shape.width - 1, shape.y + LINE_HEIGHT,
                shape));
        assertEquals(2, view.getViewIndex(shape.x, shape.y + 2 * LINE_HEIGHT, shape));
        assertEquals(2, view.getViewIndex(shape.x + shape.width - 1, shape.y + 2 * LINE_HEIGHT,
                shape));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        doc.insertString(0, "1\n2\n3", null);
        //                   0123456789 012345678901
        //                   0          1         2
        root = doc.getDefaultRootElement();
        view = new WithChildrenView(root);
        factory = new ViewTestHelpers.ChildrenFactory();
        view.loadChildren(factory);
        shape = new Rectangle(100, 200, 190, 560);
        CompositeView_ModelViewTest.shape = shape;
    }
}
