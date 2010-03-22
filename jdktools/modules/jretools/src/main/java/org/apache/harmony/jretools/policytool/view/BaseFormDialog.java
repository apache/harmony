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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * Represents a base form dialog which will be used to query data of policy entries.
 */
public abstract class BaseFormDialog extends JDialog implements ActionListener {

    /** Error message for not allowed quotation marks. */
    public static final String NOT_ALLOWED_QUOTATION_MARKS_MESSAGE = "The following fields may not contain quotation marks: ";

    /** Reference to the owner window.    */
    protected final Window      ownerWindow;

    /** Ok button of the form dialog.     */
    private final   JButton     okButton     = new JButton( "OK"     );
    /** Cancel button of the form dialog. */
    private final   JButton     cancelButton = new JButton( "Cancel" );

    /** Reference to the owner editor panel. This reference can (will) be used to indicate new data/dirty state. */
    protected final EditorPanel ownerEditorPanel;

    /**
     * Creates a new BaseFormDialog.
     * @param ownerFrame owner frame of the dialog
     * @param title title of the dialog
     * @param ownerEditorPanel reference to the owner editor panel
     */
    public BaseFormDialog( final Frame ownerFrame, final String title, final EditorPanel ownerEditorPanel ) {
        super( ownerFrame, title, true );

        this.ownerWindow      = ownerFrame;
        this.ownerEditorPanel = ownerEditorPanel;

        initialize();
    }

    /**
     * Creates a new BaseFormDialog.
     * @param ownerDialog owner dialog of the dialog
     * @param title title of the dialog
     * @param ownerEditorPanel reference to the owner editor panel
     */
    public BaseFormDialog( final Dialog ownerDialog, final String title, final EditorPanel ownerEditorPanel ) {
        super( ownerDialog, title, true );

        this.ownerWindow      = ownerDialog;
        this.ownerEditorPanel = ownerEditorPanel;

        initialize();
    }

    /**
     * Initializes the dialog.<br>
     * Part of the constructor.
     */
    private void initialize() {
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );

        buildBaseGUI();

        // We cannot call or perform the prepareForDisplay() operation here,
        // because the actual GUI might require fields initialized after this constructor.
    }

    /**
     * Builds the graphical user interface of the base dialog.<br>
     * Adds a button panel to the bottom of the dialog containing an ok and a cancel button.
     */
    protected void buildBaseGUI() {
        final JPanel panel = new JPanel();

        okButton.setMnemonic( okButton.getText().charAt( 0 ) );
        okButton.addActionListener( this );
        panel.add( okButton );

        cancelButton.setMnemonic( cancelButton.getText().charAt( 0 ) );
        cancelButton.addActionListener( this );
        panel.add( cancelButton );

        add( panel, BorderLayout.SOUTH );
    }

    /**
     * Builds the GUI of the dialog.
     */
    protected abstract void buildGUI();

    /**
     * Prepares the dialog for displaying.<br>
     * This includes finishing building the gui and sizing and positioning it.
     */
    protected void prepareForDisplay() {
        buildGUI();

        pack();
        center();
    }

    /**
     * Centers the dialog to its owner frame.
     */
    public void center() {
        setLocation( ownerWindow.getX() + ownerWindow.getWidth () / 2 - getWidth () / 2,
                     ownerWindow.getY() + ownerWindow.getHeight() / 2 - getHeight() / 2 );
    }

    /**
     * Handles the action events of the dialog's ok and cancel buttons.
     */
    public void actionPerformed( final ActionEvent ae ) {
        if ( ae.getSource() == okButton )
            onOkButtonPressed();
        if ( ae.getSource() == cancelButton )
            onCancelButtonPressed();
    }

    /**
     * Called when the ok button of the dialog is pressed.
     */
    public abstract void onOkButtonPressed();

    /**
     * Finishes a successful edit action.
     */
    protected void finishSuccessfulEdit() {
        finishSuccessfulEdit( true );
    }

    /**
     * Finishes a successful edit action.
     * @param setDirtyFlag tells whether dirty flag has to be set (to true)
     */
    protected void finishSuccessfulEdit( final boolean setDirtyFlag ) {
        if ( setDirtyFlag )
            ownerEditorPanel.setHasDirty( true );
        dispose();
    }

    /**
     * Called when the cancel button of the dialog is pressed.<br>
     * Simply disposes the dialog.
     */
    public void onCancelButtonPressed() {
        dispose();
    }

}
