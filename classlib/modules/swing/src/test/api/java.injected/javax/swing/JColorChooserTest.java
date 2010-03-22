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
 * @author Sergey Burlak
 */
package javax.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorChooserComponentFactory;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.colorchooser.DefaultColorSelectionModel;

public class JColorChooserTest extends BasicSwingTestCase {
    private JColorChooser ch;

    @Override
    public void setUp() {
        ch = new JColorChooser();
        propertyChangeController = new PropertyChangeController();
        propertyChangeController.setVerbose(false);
        ch.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    protected void tearDown() throws Exception {
        ch = null;
    }

    public void testJColorChooser() throws Exception {
        assertSame(DefaultColorSelectionModel.class, ch.getSelectionModel().getClass());
        assertNull(ch.accessibleContext);
        assertEquals(Color.WHITE, ch.getColor());
        ch = new JColorChooser(Color.BLACK);
        assertEquals(Color.BLACK, ch.getColor());
        ColorSelectionModel model = new DefaultColorSelectionModel();
        ch = new JColorChooser(model);
        assertFalse(propertyChangeController.isChanged());
        assertSame(model, ch.getSelectionModel());
        assertSame(BorderLayout.class, ch.getLayout().getClass());
    }

    public void testGetAccessibleContext() throws Exception {
        assertNull(ch.accessibleContext);
        assertNotNull(ch.getAccessibleContext());
        assertNotNull(ch.accessibleContext);
        assertSame(ch.getAccessibleContext(), ch.getAccessibleContext());
        assertSame(ch.accessibleContext, ch.getAccessibleContext());
        assertSame(JColorChooser.AccessibleJColorChooser.class, ch.accessibleContext.getClass());
    }

    public void testSetGetSelectionModel() throws Exception {
        assertNotNull(ch.getSelectionModel());
        ColorSelectionModel oldModel = ch.getSelectionModel();
        ColorSelectionModel model = new DefaultColorSelectionModel();
        ch.setSelectionModel(model);
        assertTrue(propertyChangeController.isChanged(JColorChooser.SELECTION_MODEL_PROPERTY));
        assertSame(model, ((PropertyChangeEvent) propertyChangeController.getLastEvent())
                .getNewValue());
        assertSame(oldModel, ((PropertyChangeEvent) propertyChangeController.getLastEvent())
                .getOldValue());
        ch.setSelectionModel(null);
        assertNull(ch.getSelectionModel());
    }

    public void testSetGetColor() throws Exception {
        ChangeController changeController = new ChangeController();
        ch.getSelectionModel().addChangeListener(changeController);
        assertEquals(Color.WHITE, ch.getColor());
        ch.setColor(Color.RED);
        assertFalse(propertyChangeController.isChanged());
        assertEquals(Color.RED, ch.getColor());
        assertTrue(changeController.isChanged());
        changeController.reset();
        ch.setColor(0, 255, 0);
        assertFalse(propertyChangeController.isChanged());
        assertEquals(Color.GREEN, ch.getColor());
        assertTrue(changeController.isChanged());
        changeController.reset();
        ch.setColor(255);
        assertFalse(propertyChangeController.isChanged());
        assertEquals(Color.BLUE, ch.getColor());
        assertTrue(changeController.isChanged());
    }

    public void testGetUIClassID() throws Exception {
        assertEquals("ColorChooserUI", ch.getUIClassID());
    }

    public void testSetGetDragEnabled() throws Exception {
        assertFalse(ch.getDragEnabled());
        ch.setDragEnabled(true);
        assertFalse(propertyChangeController.isChanged());
        assertTrue(ch.getDragEnabled());
    }

    public void testSetGetPreviewPanel() throws Exception {
        ch.addPropertyChangeListener(propertyChangeController);
        assertNotNull(ch.getPreviewPanel());
        JButton button = new JButton();
        ch.setPreviewPanel(button);
        assertTrue(propertyChangeController.isChanged(JColorChooser.PREVIEW_PANEL_PROPERTY));
        assertSame(button, ch.getPreviewPanel());
        assertSame(button, ((PropertyChangeEvent) propertyChangeController.getLastEvent())
                .getNewValue());
        propertyChangeController.reset();
        ch.setPreviewPanel(null);
        assertTrue(propertyChangeController.isChanged(JColorChooser.PREVIEW_PANEL_PROPERTY));
        assertNotNull(ch.getPreviewPanel());
        assertSame(ColorChooserComponentFactory.getPreviewPanel().getClass(), ch
                .getPreviewPanel().getClass());
    }

    public void testSetGetChooserPanels() throws Exception {
        AbstractColorChooserPanel[] oldChooserPanels = ch.getChooserPanels();
        assertEquals(3, oldChooserPanels.length);
        AbstractColorChooserPanel[] newPanels = new AbstractColorChooserPanel[] {};
        ch.setChooserPanels(newPanels);
        assertTrue(propertyChangeController.isChanged(JColorChooser.CHOOSER_PANELS_PROPERTY));
        assertSame(newPanels, ((PropertyChangeEvent) propertyChangeController.getLastEvent())
                .getNewValue());
        assertSame(oldChooserPanels, ((PropertyChangeEvent) propertyChangeController
                .getLastEvent()).getOldValue());
        assertSame(newPanels, ch.getChooserPanels());
        assertEquals(0, ch.getChooserPanels().length);
    }

    public void testAddRemoveChooserPanel() throws Exception {
        AbstractColorChooserPanel[] oldChooserPanels = ch.getChooserPanels();
        assertEquals(3, oldChooserPanels.length);
        AbstractColorChooserPanel panel = oldChooserPanels[0];
        assertSame(panel, ch.removeChooserPanel(panel));
        assertTrue(propertyChangeController.isChanged(JColorChooser.CHOOSER_PANELS_PROPERTY));
        assertEquals(2, ch.getChooserPanels().length);
        propertyChangeController.reset();
        try {
            ch.removeChooserPanel(panel);
            fail("IllegalArgumentException shal be thrown");
        } catch (IllegalArgumentException e) {
        }
        assertFalse(propertyChangeController.isChanged(JColorChooser.CHOOSER_PANELS_PROPERTY));
        assertEquals(2, ch.getChooserPanels().length);
        propertyChangeController.reset();
        ch.addChooserPanel(panel);
        assertTrue(propertyChangeController.isChanged(JColorChooser.CHOOSER_PANELS_PROPERTY));
        assertEquals(3, ch.getChooserPanels().length);
        assertSame(panel, ch.getChooserPanels()[2]);
        propertyChangeController.reset();
        try {
            ch.addChooserPanel(null);
            fail("NPE shall be thrown");
        } catch (NullPointerException npe) {
        }
        propertyChangeController.reset();
        try {
            ch.removeChooserPanel(null);
            fail("NPE shall be thrown");
        } catch (NullPointerException npe) {
        }
    }
}
