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

public class JEditorPane_AccessibleJEditorPaneHTMLTest extends SwingTestCase {
    JEditorPane jep;

    JFrame jf;

    JEditorPane.AccessibleJEditorPaneHTML accessible;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        jep = new JEditorPane();
        jep.setContentType("text/html");
        jf = new JFrame();
        jf.getContentPane().add(jep);
        jf.setSize(200, 300);
        jf.pack();
        accessible = jep.new AccessibleJEditorPaneHTML();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testGetAccessibleText() {
        assertTrue(accessible.getAccessibleText() instanceof JEditorPane.JEditorPaneAccessibleHypertextSupport);
    }

    public void testGetAccessibleAt() {
    }

    public void testGetAccessibleChild(final int i) {
    }

    public void testGetAccessibleChildrenCount() {
    }
}