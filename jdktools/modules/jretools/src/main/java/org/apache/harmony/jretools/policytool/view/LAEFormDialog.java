/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.jretools.policytool.view;

import java.awt.Dialog;
import java.awt.Frame;

import javax.swing.DefaultListModel;
import javax.swing.JList;

/**
 * An extended form dialog which is tied to a ListAndEdit component.<br>
 * The form is destined to view and edit the entities of a ListAndEdit component.
 */
public abstract class LAEFormDialog extends BaseFormDialog {

    /** Reference to the visualization list component of the tied ListAndEdit component.   */
    protected JList            visualizationJListforLAE;
    /** Reference to the model of the visualization list as <code>DefaultListModel</code>. */
    protected DefaultListModel listModel;

    /**
     * A delegator constructor toward the ancestor's constructor.
     * @param ownerFrame reference to the owner frame
     * @param title title of the dialog
     * @param ownerEditorPanel reference to the owner editor panel
     */
    public LAEFormDialog( final Frame ownerFrame, final String title, final EditorPanel ownerEditorPanel ) {
        super( ownerFrame, title, ownerEditorPanel );
    }

    /**
     * A delegator constructor toward the ancestor's constructor.
     * @param ownerDialog reference to the owner dialog
     * @param title title of the dialog
     * @param ownerEditorPanel reference to the owner editor panel
     */
    public LAEFormDialog( final Dialog ownerDialog, final String title, final EditorPanel ownerEditorPanel ) {
        super( ownerDialog, title, ownerEditorPanel );
    }

    /**
     * Sets the visualization list component and its list model of the tied ListAndEdit component.
     * @param visualizationJListforLAE visualization list component of the tied ListAndEdit component
     * @param listModel list model of the visualization list component
     */
    public void setVisualizationListForLAE( final JList visualizationJListforLAE, final DefaultListModel listModel ) {
        this.visualizationJListforLAE = visualizationJListforLAE;
        this.listModel                = listModel;
    }

    @Override
    protected void finishSuccessfulEdit( final boolean setDirtyFlag ) {
        refreshVisualizationList();
        super.finishSuccessfulEdit( setDirtyFlag );
    }

    /**
     * Refreshes the visualization list.<br>
     * Should be called if the entities of the list might have changed but the list model was not modified.
     */
    public void refreshVisualizationList() {
        final Object newTempItem = new String();
        // There is a bug if we edit an item, we change it in a way that its toString() method will return a very long string
        // (long as it won't fit in the displayed width), its string will be truncated (ended with "..."), and no scrollbars will be displayed
        // The addElement() operation causes to recalculate the viewport size.
        listModel.addElement   ( newTempItem );
        listModel.removeElement( newTempItem );
    }

}
