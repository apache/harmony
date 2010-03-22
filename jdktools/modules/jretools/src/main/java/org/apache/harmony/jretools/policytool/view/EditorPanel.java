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

import java.awt.LayoutManager;

import javax.swing.JPanel;

/**
 * Defines an abstract editor panel which can provide a GUI for editing a policy text.
 */
public abstract class EditorPanel extends JPanel {

    /** Reference to the main frame.                                      */
    protected final MainFrame mainFrame;

    /** The title of the panel.                                           */
    protected String          panelTitle;

    /** Tells whether this editor panel has unsaved changes.              */
    protected boolean         hasDirty;
    /** Tells whether this editor panel supports graphical keystore edit. */
    protected boolean         supportsGraphicalKeystoreEdit;

    /**
     * Creates a new EditorPanel.<br>
     * Calls the other constructor with an additional <code>supportsGraphicalKeystoreEdit=false</code>.
     * @param mainFrame reference to the main frame
     * @param panelTitle the title of the panel
     * @param layoutManager layout manager to be used
     */
    public EditorPanel( final MainFrame mainFrame, final String panelTitle, final LayoutManager layoutManager ) {
        this( mainFrame, panelTitle, layoutManager, false );
    }

    /**
     * Creates a new EditorPanel.<br>
     * Awaits a layout manager to be sent to the super class.
     * @param mainFrame reference to the main frame
     * @param panelTitle the title of the panel
     * @param layoutManager layout manager to be used
     * @param supportsGraphicalKeystoreEdit true if this editor panel supports graphical keystore edit; false otherwise
     */
    public EditorPanel( final MainFrame mainFrame, final String panelTitle, final LayoutManager layoutManager, final boolean supportsGraphicalKeystoreEdit ) {
        super( layoutManager );

        this.mainFrame                     = mainFrame;
        this.panelTitle                    = panelTitle;
        this.supportsGraphicalKeystoreEdit = supportsGraphicalKeystoreEdit;
    }

    /**
     * Returns the title of the panel.
     * @return the title of the panel
     */
    public String getPanelTitle() {
        return panelTitle;
    }

    /**
     * Loads the specified policy text into the editor panel.<br>
     * If loading fails, leaves the current policy text intact.
     * @param policyText policy text to be loaded
     * @return true if loading was successful; false otherwise
     */
    public abstract boolean loadPolicyText( final String policyText );

    /**
     * Returns the policy text hold by this editor panel.
     * @return the policy text hold by this editor panel
     */
    public abstract String getPolicyText();

    /**
     * Tells whether this editor panel has unsaved changes.
     * @return true if the editor panel has unsaved changes
     */
    public boolean getHasDirty() {
        return hasDirty;
    }

    /**
     * Sets the hasDirty property.
     * @param hasDirty value of hasDirty to be set
     */
    public void setHasDirty( final boolean hasDirty ) {
        this.hasDirty = hasDirty;
    }

    /**
     * Tells whether this editor panel supports graphical keystore edit.
     * @return true if this editor panel supports graphical keystore edit; false otherwise
     */
    public boolean supportsGraphicalKeystoreEdit() {
        return supportsGraphicalKeystoreEdit;
    }

}
