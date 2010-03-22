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
 * @author Dmitry A. Durnev
 */
package java.awt;

import junit.framework.TestCase;

/**
 * LabelTest
 */
public class LabelTest extends TestCase {
    Label label;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        label = new Label();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void Label(java.lang.String)
     */
    public final void testLabelString() {
        label = new Label(null);
        assertEquals(Label.LEFT, label.getAlignment());
        assertNull(label.getText());
        String text = "label text";
        label = new Label(text);
        assertEquals(text, label.getText());
    }

    /*
     * Class under test for void Label()
     */
    public final void testLabel() {
        assertEquals("", label.getText());
        assertEquals(Label.LEFT, label.getAlignment());
    }

    /*
     * Class under test for void Label(java.lang.String, int)
     */
    public final void testLabelStringint() {
        boolean iaeCaught = false;
        try {
            label = new Label(null, 100);
        } catch (IllegalArgumentException iae) {
            iaeCaught = true;
        }
        assertTrue(iaeCaught);
        int align =  Label.RIGHT;
        String text = "label";
        label = new Label(text, align);
        assertEquals(align, label.getAlignment());
        assertEquals(text, label.getText());
    }

    public final void testGetAlignment() {
        assertEquals(Label.LEFT, label.getAlignment());
    }

    public final void testGetText() {
        assertEquals("", label.getText());
    }

    public final void testSetAlignment() {
        boolean iaeCaught = false;
        try {
            label.setAlignment(-1);
        } catch (IllegalArgumentException iae) {
            iaeCaught = true;
        }
        assertTrue(iaeCaught);
        int align =  Label.CENTER;
        label.setAlignment(align);
        assertEquals(align, label.getAlignment());
    }

    public final void testSetText() {
        label.setText(null);
        assertNull(label.getText());
        String text = "";
        label.setText(text);
        assertEquals(text, label.getText());
    }

    public void testDeadLoop4887() {
        final int count[] = new int[1];
        Component c = new Label() {
            public void paint(Graphics g) {
                count[0]++;
                setEnabled(true);
            }
        };
        
        Tools.checkDeadLoop(c, count);
    }
    
}
