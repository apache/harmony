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
import java.awt.Dimension;
import java.awt.Frame;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.harmony.jretools.policytool.control.Controller;
import org.apache.harmony.jretools.policytool.model.GrantEntry;
import org.apache.harmony.jretools.policytool.model.Permission;
import org.apache.harmony.jretools.policytool.model.PolicyEntry;
import org.apache.harmony.jretools.policytool.model.Principal;

/**
 * Form dialog to view and edit the grant entries.
 */
public class GrantEntryEditFormDialog extends LAEFormDialog {

    /** Reference to the initial editable grant entry or null, if we are creating a new one. */
    private final GrantEntry          initialGrantEntry;
    /** List of policy entries where to store if new entry is to be created.                 */
    private final List< PolicyEntry > policyEntryList;

    /** Holds the reference to the new granty entry in case of we are creating a new one.    */
    private final GrantEntry          newGrantEntry;

    /** A deep clone of the edited grant entry's principal list.<br>
     * This is necessary because we have to be able to restore the original principal list
     * (which are edited by another instance of LAEFormDialog)
     * if cancel action is performed here on the grant entry's LAEFormDialog.                */
    private final List< Principal >   tempPrincipalList;
    /** A deep clone of the edited grant entry's permission list.<br>
     * This is necessary because we have to be able to restore the original permission list
     * (which are edited by another instance of LAEFormDialog)
     * if cancel action is performed here on the grant entry's LAEFormDialog.                */
    private final List< Permission >  tempPermissionList;

    /** Text field to view and edit the value of code base. */
    private final JTextField codeBaseTextField = new JTextField();
    /** Text field to view and edit the value of signed by. */
    private final JTextField signedByTextField = new JTextField();

    /**
     * Creates a new GrantEntryEditFormDialog.
     * @param ownerFrame reference to the owner frame
     * @param ownerEditorPanel reference to the owner editor panel
     * @param grantEntry reference to the editable grant entry or null, if we are creating a new one
     * @param policyEntryList list of policy entries where to store if new entry is to be created
     */
    public GrantEntryEditFormDialog( final Frame ownerFrame, final EditorPanel ownerEditorPanel, final GrantEntry grantEntry, final List< PolicyEntry > policyEntryList ) {
        super( ownerFrame, "Policy Entry", ownerEditorPanel );

        this.initialGrantEntry = grantEntry;
        this.policyEntryList   = policyEntryList;

        newGrantEntry = initialGrantEntry == null ? new GrantEntry() : null;
        tempPrincipalList  = deepclonePrincipalList ( ( initialGrantEntry == null ? newGrantEntry : initialGrantEntry ).getPrincipalList () );
        tempPermissionList = deepclonePermissionList( ( initialGrantEntry == null ? newGrantEntry : initialGrantEntry ).getPermissionList() );

        prepareForDisplay();
    }

    /**
     * Deepclones a principal list and returns it.<br>
     * This method uses the <code>Object.clone()</code> clone the elements.
     * 
     * @param principalList principal list to be deepcloned
     * @return a deepcloned principal list
     */
    private static List< Principal > deepclonePrincipalList( final List< Principal > principalList ) {
        final List< Principal > deepclonedPrincipalList = new ArrayList< Principal >( principalList.size() );

        for ( final Principal principal : principalList )
            deepclonedPrincipalList.add( (Principal) principal.clone() );

        return deepclonedPrincipalList;
    }

    /**
     * Deepclones a permission list and returns it.<br>
     * This method uses the <code>Object.clone()</code> clone the elements.
     * 
     * @param permissionList permission list to be deepcloned
     * @return a deepcloned permission list
     */
    private static List< Permission > deepclonePermissionList( final List< Permission > permissionList ) {
        final List< Permission > deepclonedPermissionList = new ArrayList< Permission >( permissionList.size() );

        for ( final Permission permission : permissionList )
            deepclonedPermissionList.add( (Permission) permission.clone() );

        return deepclonedPermissionList;
    }

    @Override
    protected void buildGUI() {
        final JPanel panel = new JPanel( new BorderLayout( 2,15 ) );

        final Box verticalBox = Box.createVerticalBox();

        verticalBox.add( Box.createVerticalStrut( 10 ) );

        Box hBox = Box.createHorizontalBox();
        JLabel label = new JLabel( "CodeBase: ", JLabel.RIGHT );
        label.setPreferredSize( new Dimension( 80, 20 ) );
        hBox.add( label );
        hBox.add( codeBaseTextField );
        verticalBox.add( hBox );

        verticalBox.add( Box.createVerticalStrut( 5 ) );

        hBox = Box.createHorizontalBox();
        label = new JLabel( "SignedBy: ", JLabel.RIGHT );
        label.setPreferredSize( new Dimension( 80, 20 ) );
        hBox.add( label );
        hBox.add( signedByTextField );
        verticalBox.add( hBox );

        if ( initialGrantEntry != null ) {
            codeBaseTextField.setText( initialGrantEntry.getCodeBase() );
            signedByTextField.setText( initialGrantEntry.getSignedBy() );
        }

        panel.add( verticalBox, BorderLayout.NORTH );

        // ListAndEdit component for Principals
        panel.add( new ListAndEditPanel< Principal >( "Principals:", "Principal", tempPrincipalList,
                new ListAndEditPanel.LAEFormDialogFactory< Principal > () {
                    public LAEFormDialog createFactoryForAddOrEdit( final Principal selectedEntity ) {
                        return new PrincipalEditFormDialog( GrantEntryEditFormDialog.this, ownerEditorPanel, selectedEntity, tempPrincipalList );
                    }
                }
            ), BorderLayout.CENTER );

        panel.add( new JLabel(), BorderLayout.SOUTH ); // To make some space between the 2 ListAndEdit components (vertical gap of the BorderLayout of the panel will be used)

        add( panel, BorderLayout.NORTH );

        // ListAndEdit component for Permissions
        final ListAndEditPanel< Permission > permissionsLAE =
            new ListAndEditPanel< Permission >( "Permissions:", "Permission", tempPermissionList,
                new ListAndEditPanel.LAEFormDialogFactory< Permission > () {
                    public LAEFormDialog createFactoryForAddOrEdit( final Permission selectedEntity ) {
                        return new PermissionEditFormDialog( GrantEntryEditFormDialog.this, ownerEditorPanel, selectedEntity, tempPermissionList );
                    }
                }
            );
        permissionsLAE.overrideMnemonics( 'd', 't', 'v' );
        add( permissionsLAE, BorderLayout.CENTER );
    }

    @Override
    public void onOkButtonPressed() {
        // validation
        final StringBuilder errorStringBuilder = new StringBuilder( NOT_ALLOWED_QUOTATION_MARKS_MESSAGE );
        boolean validationFails = false;
        if ( codeBaseTextField.getText().indexOf( '"' ) >= 0 ) {
            validationFails = true;
            errorStringBuilder.append( "codeBase" );
        }
        if ( signedByTextField.getText().indexOf( '"' ) >= 0 ) {
            validationFails = true;
            errorStringBuilder.append( validationFails ? ", signedBy" : "signedBy" );
        }

        if ( !validationFails && codeBaseTextField.getText().length() > 0 ) {
            try {
                new URL( codeBaseTextField.getText() );
            } catch ( final MalformedURLException mue ) {
                validationFails = true;
                errorStringBuilder.setLength( 0 );
                errorStringBuilder.append( "CodeBase contains a malformed URL: " + mue.getMessage() );
            }
        }

        if ( validationFails ) {
            Controller.logError( errorStringBuilder.toString() );
            JOptionPane.showMessageDialog( this, errorStringBuilder.toString(), "Error!", JOptionPane.ERROR_MESSAGE );
            return;
        }
        // validation end

        final GrantEntry grantEntry = initialGrantEntry == null ? newGrantEntry : initialGrantEntry;

        grantEntry.setCodeBase      ( codeBaseTextField.getText() );
        grantEntry.setSignedBy      ( signedByTextField.getText() );
        grantEntry.setPrincipalList ( tempPrincipalList           );
        grantEntry.setPermissionList( tempPermissionList          );

        if ( initialGrantEntry == null ) {
            policyEntryList.add( grantEntry );
            listModel.addElement( grantEntry );
        }

        finishSuccessfulEdit();
    }

}
