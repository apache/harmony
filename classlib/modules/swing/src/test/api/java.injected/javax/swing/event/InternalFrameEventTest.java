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
 * @author Vadim L. Bogdanov
 */
package javax.swing.event;

import javax.swing.JInternalFrame;
import javax.swing.SwingTestCase;

public class InternalFrameEventTest extends SwingTestCase {
    private JInternalFrame frame;

    /*
     * Constructor for InternalFrameEventTest.
     * @param name
     */
    public InternalFrameEventTest(final String name) {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
    }

    /*
     * Class under test for JInternalFrame getInternalFrame()
     */
    public void testGetInternalFrame() {
        InternalFrameEvent event = new InternalFrameEvent(frame,
                InternalFrameEvent.INTERNAL_FRAME_CLOSED);
        assertTrue("== frame", event.getInternalFrame() == frame);
        assertTrue("== getSource()", event.getInternalFrame() == event.getSource());
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        InternalFrameEvent event = new InternalFrameEvent(frame,
                InternalFrameEvent.INTERNAL_FRAME_ACTIVATED);
        assertTrue("paramString() cannot return null", event.paramString() != null);
    }
}
