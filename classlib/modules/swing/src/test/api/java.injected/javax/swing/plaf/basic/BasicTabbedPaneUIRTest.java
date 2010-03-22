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
package javax.swing.plaf.basic;

import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingTestCase;

public class BasicTabbedPaneUIRTest extends SwingTestCase {
    JTabbedPane tp;

    BasicTabbedPaneUI ui;

    public BasicTabbedPaneUIRTest(final String name) {
        super(name);
        tp = new JTabbedPane();
        ui = new BasicTabbedPaneUI();
        tp.setUI(ui);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPreferredLayoutSize() {
        JPanel tabContent1 = new JPanel();
        tabContent1.setPreferredSize(new Dimension(300, 300));
        tabContent1.setMinimumSize(new Dimension(200, 200));
        JPanel tabContent2 = new JPanel();
        tabContent2.setPreferredSize(new Dimension(500, 200));
        tabContent2.setMinimumSize(new Dimension(400, 100));
        tp.addTab("tab1", tabContent1);
        tp.addTab("tab2", tabContent2);
        Dimension size = tp.getPreferredSize();
        assertTrue(size.width > 500);
        assertTrue(size.height > 300);
        size = tp.getMinimumSize();
        assertTrue(size.width > 400);
        assertTrue(size.height > 200);
    }
}
