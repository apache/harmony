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
 * @author Alexander T. Simbirtsev
 */
package javax.swing.text;

import java.awt.event.KeyEvent;
import junit.framework.TestCase;

public class JTextComponentRTest extends TestCase {
    class JMyTextComponent extends JTextComponent {
        private static final long serialVersionUID = 1L;

        @Override
        public String getUIClassID() {
            return "TextFieldUI";
        }

        public void test(final KeyEvent event) {
            processKeyEvent(event);
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testProcessKeyEvent() {
        JMyTextComponent c = new JMyTextComponent();
        KeyEvent event = new KeyEvent(c, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, '\n');
        c.setKeymap(null);
        c.test(event);
    }

    public void testUpdateUI() {
        // regression test for HARMONY-1475
        JMyTextComponent c = new JMyTextComponent();
        c.updateUI();
        c.setText("q");
        assertEquals("q", c.getText());
    }
}
