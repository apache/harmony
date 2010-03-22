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
 * @author Evgeniya G. Maenkova
 */
package javax.swing;

import javax.accessibility.AccessibleState;

public class JEditorPane_AccessibleJEditorPaneTest extends SwingTestCase {
    JEditorPane jep;

    JFrame jf;

    JEditorPane.AccessibleJEditorPane accessible;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        jep = new JEditorPane();
        jf = new JFrame();
        jf.getContentPane().add(jep);
        jf.setSize(200, 300);
        jf.pack();
        accessible = jep.new AccessibleJEditorPane();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testGetAccessibleStateSet() {
        assertTrue(accessible.getAccessibleStateSet().contains(AccessibleState.MULTI_LINE));
    }

    public void testGetAccessibleDescription() {
        assertEquals("text/plain", accessible.getAccessibleDescription());
        // TODO: uncomment when HTML support is implemented
        //        jep.setContentType("text/html");
        //        assertEquals("text/html", accessible.getAccessibleDescription());
        jep.setContentType("test");
        assertEquals("text/plain", accessible.getAccessibleDescription());
        // TODO: uncomment when RTF support is implemented
        //        jep.setContentType("text/rtf");
        //        assertEquals("text/rtf", accessible.getAccessibleDescription());
    }
}