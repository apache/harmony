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
 * @author Anton Avtamonov
 */
package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;
import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingTestCase;

public class MetalFileChooserUITest extends SwingTestCase {
    private MetalFileChooserUI ui;

    private JFileChooser fc;

    public MetalFileChooserUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        fc = new JFileChooser();
        ui = new MetalFileChooserUI(fc);
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
        fc = null;
    }

    public void testCreateUI() throws Exception {
        assertNotSame(MetalFileChooserUI.createUI(fc), MetalFileChooserUI.createUI(fc));
    }

    public void testGetButtonPanel() throws Exception {
        JPanel buttonPanel = ui.getButtonPanel();
        assertNotNull(buttonPanel);
        assertEquals(0, buttonPanel.getComponentCount());
        assertEquals(ui.getButtonPanel(), ui.getButtonPanel());
    }

    public void testGetBottomPanel() throws Exception {
        JPanel bottomPanel = ui.getBottomPanel();
        assertNotNull(bottomPanel);
        assertEquals(0, bottomPanel.getComponentCount());
        assertEquals(ui.getBottomPanel(), ui.getBottomPanel());
    }

    public void testGetActionMap() throws Exception {
        ActionMap actionMap = ui.getActionMap();
        assertNotNull(actionMap);
        assertNotSame(ui.getActionMap(), ui.getActionMap());
    }

    public void testCreateActionMap() throws Exception {
        ui.installUI(fc);
        ActionMap map = ui.createActionMap();
        List<Object> allKeys = Arrays.asList(map.allKeys());
        assertEquals(3, allKeys.size());
        assertTrue(allKeys.contains("approveSelection"));
        assertEquals(ui.getApproveSelectionAction(), map.get("approveSelection"));
        assertTrue(allKeys.contains("cancelSelection"));
        assertEquals(ui.getCancelSelectionAction(), map.get("cancelSelection"));
        assertTrue(allKeys.contains("Go Up"));
        assertEquals(ui.getChangeToParentDirectoryAction(), map.get("Go Up"));
    }

    public void testCreateList() throws Exception {
        ui.installUI(fc);
        JPanel listPanel = ui.createList(fc);
        assertNotNull(listPanel);
        if (isHarmony()) {
            assertEquals(2, listPanel.getComponentCount());
        }
        assertTrue(listPanel.getComponent(0) instanceof JScrollPane);
        assertTrue(((JScrollPane) listPanel.getComponent(0)).getViewport().getView() instanceof JList);
        JList list = (JList) ((JScrollPane) listPanel.getComponent(0)).getViewport().getView();
        assertEquals(ui.getModel(), list.getModel());
        assertNotSame(ui.createList(fc), ui.createList(fc));
    }

    // TODO: detail view is not implemented yet
    public void testCreateDetailsView() throws Exception {
        ui.installUI(fc);
        //        JPanel detailsView = ui.createDetailsView(fc);
        //        assertNotNull(detailsView);
        //        assertEquals(1, detailsView.getComponentCount());
        //        assertTrue(detailsView.getComponent(0) instanceof JScrollPane);
        //        assertTrue(((JScrollPane)detailsView.getComponent(0)).getViewport().getView() instanceof JTable);
        //        JTable table = (JList)((JScrollPane)listPanel.getComponent(0)).getViewport().getView();
        //        assertEquals(ui.getModel(), list.getModel());
        //        assertNotSame(ui.createList(fc), ui.createList(fc));
    }

    public void testCreateListSelectionListener() throws Exception {
        assertNotNull(ui.createListSelectionListener(null));
        assertNotSame(ui.createListSelectionListener(null), ui
                .createListSelectionListener(null));
    }

    public void testGetPreferredSize() throws Exception {
        assertNotNull(ui.getPreferredSize(fc));
    }

    public void testGetMinimumSize() throws Exception {
        assertNotNull(ui.getMinimumSize(fc));
    }

    public void testGetMaxiumumSize() throws Exception {
        assertNotNull(ui.getMaximumSize(null));
        assertEquals(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE), ui
                .getMaximumSize(null));
    }

    public void testCreatePropertyChangeListener() throws Exception {
        assertNotNull(ui.createPropertyChangeListener(null));
    }

    public void testGetApproveSelectionButton() throws Exception {
        ui.installUI(fc);
        JButton button = ui.getApproveButton(null);
        assertNotNull(button);
        assertEquals(ui.getApproveButtonText(fc), button.getText());
    }

    public void testGetApproveButton() throws Exception {
        ui.installUI(fc);
        assertNotNull(ui.getApproveButton(fc));
        assertEquals(ui.getApproveButton(fc), ui.getApproveButton(fc));
    }
    //    private void traverse(final Container cont) {
    //        for (int i = 0; i < cont.getComponentCount(); i++) {
    //            Component child = cont.getComponent(i);
    //            if (child instanceof JButton) {
    //                System.err.println("Button " + ((JButton)child).getText());
    //            }
    //
    //            if (child instanceof Container) {
    //                traverse((Container)child);
    //            }
    //        }
    //    }
}
