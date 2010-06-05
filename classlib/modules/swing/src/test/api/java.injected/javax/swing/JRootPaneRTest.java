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
package javax.swing;

import java.beans.PropertyVetoException;

public class JRootPaneRTest extends SwingTestCase {
    private JFrame frame;

    public JRootPaneRTest(final String name) {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public void testDefaultButton() throws PropertyVetoException {
        JInternalFrame iframe = new JInternalFrame("", true, true, true, true);
        JDesktopPane desktop = new JDesktopPane();
        desktop.add(iframe);
        JButton button = new JButton();
        iframe.getContentPane().add(button);
        iframe.getRootPane().setDefaultButton(button);
        frame = new JFrame();
        frame.setContentPane(desktop);
        frame.setVisible(true);
        iframe.setIcon(true);
        assertNull(iframe.getRootPane().getDefaultButton());
        iframe.setIcon(false);
        assertSame(button, iframe.getRootPane().getDefaultButton());
        iframe.setIcon(true);
        iframe.getContentPane().remove(button);
        iframe.setIcon(false);
        assertNull(iframe.getRootPane().getDefaultButton());
    }
}
