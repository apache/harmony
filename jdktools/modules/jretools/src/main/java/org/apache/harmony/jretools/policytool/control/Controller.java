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

package org.apache.harmony.jretools.policytool.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.harmony.jretools.policytool.view.EditorPanel;
import org.apache.harmony.jretools.policytool.view.GraphicalEditorPanel;
import org.apache.harmony.jretools.policytool.view.MainFrame;
import org.apache.harmony.jretools.policytool.view.WarningLogDialog;
import org.apache.harmony.jretools.policytool.view.MainFrame.MenuItemEnum;

/**
 * The controller handles the user actions, drives the GUI and connects it to the model.
 */
public class Controller implements ChangeListener, ActionListener{

    /** Thee warning log dialog. */
    private static WarningLogDialog warningLogDialog;

    /** Reference to the main frame.              */
    private final MainFrame     mainFrame;
    /** Array of the editor panels.               */
    private final EditorPanel[] editorPanels;
    /** Reference to the active editor panel.     */
    private EditorPanel         activeEditorPanel;

    /** Reference to the keystore edit menu item. */
    private JMenuItem           keystoreEditMenuItem;

    /** The currently edited policy file.         */
    private File                editedPolicyFile;

    /**
     * Logs an error message to the warning/error log.
     * @param errorMessage error message to be logged
     */
    public static void logError( final String errorMessage ) {
        warningLogDialog.addMessage( "Error: " + errorMessage );
    }

    /**
     * Logs a warning message to the warning/error log.
     * @param warningMessage warning message to be logged
     */
    public static void logWarning( final String warningMessage ) {
        warningLogDialog.addMessage( "Warning: " + warningMessage );
    }

    /**
     * Creates a new Controller.
     * @param mainFrame reference to the main frame
     * @param editorPanels array of the editor panels
     * @param policyFileName policy file name to be loaded initially
     */
    public Controller( final MainFrame mainFrame, final EditorPanel[] editorPanels, final String policyFileName ) {
        this.mainFrame    = mainFrame;
        this.editorPanels = editorPanels;
        warningLogDialog  = new WarningLogDialog( mainFrame );
        activeEditorPanel = editorPanels[ 0 ];

        PolicyFileHandler.setDialogParentComponent( mainFrame );

        if ( policyFileName != null ) {
            final File editedPolicyFile_ = new File( policyFileName );
            if ( activeEditorPanel.loadPolicyText( PolicyFileHandler.loadPolicyFile( editedPolicyFile_ ) ) )
                setEditedPolicyFile( editedPolicyFile_ );
        }
    }

    /**
     * Returns the array of editor panels.
     * @return the array of editor panels
     */
    public EditorPanel[] getEditorPanels() {
        return editorPanels;
    }

    /**
     * Sets the keystore edit menu item.
     * @param keystoreEditMenuItem the keystore edit menu item
     */
    public void setKeystoreEditMenuItem( final JMenuItem keystoreEditMenuItem ) {
        this.keystoreEditMenuItem = keystoreEditMenuItem;
        keystoreEditMenuItem.setEnabled( activeEditorPanel.supportsGraphicalKeystoreEdit() );
    }

    /**
     * Exits from the program.<br>
     * There might be unsaved changes in which case confirmation will be asked.
     */
    public void exit() {
        if ( allowedDirtySensitiveOperation( "exit" ) )
            System.exit( 0 );
    }

    /**
     * Determines if a dirty sensitive operation is allowed to be executed.<br>
     * There are operation which will throw away the edited policy text currently hold in the active editor
     * (for example exit or load a file, or start a new).<br>
     * This method checks whether there are unsaved changes, and if so, asks confirmation on what to do with them.<br>
     * Finally returns true, if the dirty data can be thrown away or has been saved successfully.
     * Returns false, if the effect of the operation (throwing away unsaved changes) is unwanted and therefore the operation is disallowed.
     * 
     * @param operationName name of the operation which will be included in the confirmation messages
     * @return true, if the operation now can be performed safely; false otherwise
     */
    private boolean allowedDirtySensitiveOperation( final String operationName ) {
        if ( activeEditorPanel.getHasDirty() ) {

            switch ( JOptionPane.showConfirmDialog( mainFrame, "There are unsaved changes. Save before " + operationName + "?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE ) ) {

            case JOptionPane.YES_OPTION:
                // We chose to save file first
                final JFileChooser fileChooser = new JFileChooser();

                if ( editedPolicyFile == null ) {
                    if ( fileChooser.showSaveDialog( mainFrame ) == JFileChooser.APPROVE_OPTION )
                        editedPolicyFile = fileChooser.getSelectedFile();
                }
                if ( editedPolicyFile != null ) {
                    if ( !PolicyFileHandler.savePolicyFile( editedPolicyFile, activeEditorPanel.getPolicyText() ) ) {
                        switch ( JOptionPane.showConfirmDialog( mainFrame, "Saving failed. Do you still want to " + operationName + "?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) ) {
                        case JOptionPane.YES_OPTION:
                            // We chose to still proceed
                            return true;

                        case JOptionPane.NO_OPTION:
                        case JOptionPane.CLOSED_OPTION:
                            // We chose not to proceed
                            return false;
                        }
                    } else {// Changes saved successfully
                        activeEditorPanel.setHasDirty( false );
                        mainFrame.setDisplayedPolicyFile( editedPolicyFile );
                        return true;
                    }
                }
                break;

            case JOptionPane.NO_OPTION:
                // We chose not to save and proceed
                return true;

            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
                // We chose not to proceed
                return false;

            }

        }

        return true;
    }

    /**
     * Handles change events of the editors tabbed pane.
     * @param ce details of the change event
     */
    public void stateChanged( final ChangeEvent ce ) {
        final EditorPanel newActiveEditorPanel = (EditorPanel) ( (JTabbedPane) ce.getSource() ).getSelectedComponent();

        newActiveEditorPanel.loadPolicyText( activeEditorPanel.getPolicyText() );
        newActiveEditorPanel.setHasDirty   ( activeEditorPanel.getHasDirty  () );
        activeEditorPanel = newActiveEditorPanel;

        keystoreEditMenuItem.setEnabled( activeEditorPanel.supportsGraphicalKeystoreEdit() );
    }

    /**
     * Handles the action events of the menu items.
     * @param ae details of the action event
     */
    public void actionPerformed( final ActionEvent ae ) {
        // The action command is the ordinal of the menu item enum.
        final MenuItemEnum menuItemEnum = MenuItemEnum.values()[ Integer.parseInt( ae.getActionCommand() ) ];

        File editedPolicyFile_ = null;

        final JFileChooser fileChooser = new JFileChooser();
        switch ( menuItemEnum ) {

        case NEW :
            if ( allowedDirtySensitiveOperation( "starting new file" ) ) {
                activeEditorPanel.loadPolicyText( "" );
                setEditedPolicyFile( null );
            }
            break;

        case OPEN :
            if ( allowedDirtySensitiveOperation( "opening file" ) )
                if ( fileChooser.showOpenDialog( mainFrame ) == JFileChooser.APPROVE_OPTION ) {
                    editedPolicyFile_ = fileChooser.getSelectedFile();
                    if ( activeEditorPanel.loadPolicyText( PolicyFileHandler.loadPolicyFile( editedPolicyFile_ ) ) )
                        setEditedPolicyFile( editedPolicyFile_ );
                }
            break;

        case SAVE :
            if ( editedPolicyFile == null ) {
                if ( fileChooser.showSaveDialog( mainFrame ) == JFileChooser.APPROVE_OPTION )
                    editedPolicyFile_ = fileChooser.getSelectedFile();
            } else
                editedPolicyFile_ = editedPolicyFile;

            if ( editedPolicyFile_ != null )
                if ( PolicyFileHandler.savePolicyFile( editedPolicyFile_, activeEditorPanel.getPolicyText() ) )
                    setEditedPolicyFile( editedPolicyFile_ );
            break;

        case SAVE_AS :
            if ( fileChooser.showSaveDialog( mainFrame ) == JFileChooser.APPROVE_OPTION ) {
                editedPolicyFile_ = fileChooser.getSelectedFile();
                if ( PolicyFileHandler.savePolicyFile( editedPolicyFile_, activeEditorPanel.getPolicyText() ) )
                    setEditedPolicyFile( editedPolicyFile_ );
            }
            break;

        case VIEW_WARNING_LOG :
            warningLogDialog.setVisible( true );
            break;

        case EXIT :
            exit();
            break;

        case EDIT :
            if ( activeEditorPanel instanceof GraphicalEditorPanel )
                ( (GraphicalEditorPanel) activeEditorPanel ).showKeystoreEntryEditDialog();
            break;

        }

    }

    /**
     * Sets the edited policy file and displays its name in the main frame. Also clears the dirty flag.
     * @param editedPolicyFile edited policy file to be set
     */
    private void setEditedPolicyFile( final File editedPolicyFile ) {
        activeEditorPanel.setHasDirty( false );
        this.editedPolicyFile = editedPolicyFile;
        mainFrame.setDisplayedPolicyFile( editedPolicyFile );
    }

}
