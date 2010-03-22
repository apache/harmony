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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.harmony.jretools.policytool.control.Controller;
import org.apache.harmony.jretools.policytool.model.Principal;

/**
 * Form dialog to view and edit the principals of a grant entry.
 */
public class PrincipalEditFormDialog extends LAEFormDialog {

    /** Names of the principal types. */
    private static final String[] DEFAULT_PRINCIPAL_TYPE_NAMES       =
        new String[] { "Principal Type:", "KerberosPrincipal"                             , "X500Principal"                          };
    /** Default names of the principal type classes to be set when chosen. Null value means not to change it. */
    private static final String[] DEFAULT_PRINCIPAL_TYPE_CLASS_NAMES =
        new String[] { null             , "javax.security.auth.kerberos.KerberosPrincipal", "javax.security.auth.x500.X500Principal" };

    /** Reference to the initial editable principal or null, if we are creating a new one. */
    private final Principal         initialPrincipal;
    /** List of principals where to store if new principal is to be created.               */
    private final List< Principal > principalList;

    /** Combo box to view and choose the principal type.                 */
    private final JComboBox         principalTypeComboBox  = new JComboBox( DEFAULT_PRINCIPAL_TYPE_NAMES );
    /** Text field to view and edit the principal type (the class name). */
    private final JTextField        principalTypeTextField = new JTextField( 20 );
    /** Text field to view and edit the principal name.                  */
    private final JTextField        principalNameTextField = new JTextField( 20 );

    /**
     * Creates a new PrincipalEditFormDialog.
     * @param ownerDialog reference to the owner dialog
     * @param ownerEditorPanel reference to the owner editor panel
     * @param principal reference to the editable principal or null, if we are creating a new one
     * @param principalList list of principals where to store if new principal is to be created
     */
    public PrincipalEditFormDialog( final Dialog ownerDialog, final EditorPanel ownerEditorPanel, final Principal principal, final List< Principal > principalList ) {
        super( ownerDialog, "Principal", ownerEditorPanel );

        this.initialPrincipal = principal;
        this.principalList    = principalList;

        prepareForDisplay();
    }

    @Override
    protected void buildGUI() {
        final JPanel panel = new JPanel( new GridLayout( 2, 2, 5, 10 ) );

        principalTypeComboBox.addActionListener( new ActionListener() {
            public void actionPerformed( final ActionEvent ae ) {
                final String classNameForSelectedType = DEFAULT_PRINCIPAL_TYPE_CLASS_NAMES[ principalTypeComboBox.getSelectedIndex() ];
                if ( classNameForSelectedType != null )
                    principalTypeTextField.setText( classNameForSelectedType );
            }
        } );
        panel.add( principalTypeComboBox );
        panel.add( principalTypeTextField );

        panel.add( new JLabel( "Principal Name:" ) );
        panel.add( principalNameTextField );

        if ( initialPrincipal != null ) {
            // Should we choose anything in the principal type combo box?
            if ( initialPrincipal.getType() != null )
                for ( int i = 0; i < DEFAULT_PRINCIPAL_TYPE_CLASS_NAMES.length; i++ )
                    if ( DEFAULT_PRINCIPAL_TYPE_CLASS_NAMES[ i ] != null && DEFAULT_PRINCIPAL_TYPE_CLASS_NAMES[ i ].equals( initialPrincipal.getType() ) ) {
                        principalTypeComboBox.setSelectedIndex( i );
                        break;
                    }
            principalTypeTextField.setText( initialPrincipal.getType() );
            principalNameTextField.setText( initialPrincipal.getName() );
        }

        final JPanel flowPanel = new JPanel();
        flowPanel.add( panel );
        add( new JScrollPane( flowPanel ), BorderLayout.CENTER );
    }

    @Override
    public void onOkButtonPressed() {
        // validation
        final StringBuilder errorStringBuilder = new StringBuilder( NOT_ALLOWED_QUOTATION_MARKS_MESSAGE );
        boolean validationFails = false;
        if ( principalNameTextField.getText().indexOf( '"' ) >= 0 ) {
            validationFails = true;
            errorStringBuilder.append( "Principal Name" );
        }

        if ( !validationFails )
            if ( principalTypeTextField.getText().length() == 0 || principalNameTextField.getText().length() == 0 ) {
                validationFails = true;
                errorStringBuilder.setLength( 0 );
                errorStringBuilder.append( "Principal Type and Principal Name must have a value!" );
            }

        if ( validationFails ) {
            Controller.logError( errorStringBuilder.toString() );
            JOptionPane.showMessageDialog( this, errorStringBuilder.toString(), "Error!", JOptionPane.ERROR_MESSAGE );
            return;
        }
        // validation end

        final Principal principal = initialPrincipal == null ? new Principal() : initialPrincipal;

        principal.setType( principalTypeTextField.getText() );
        principal.setName( principalNameTextField.getText() );

        if ( initialPrincipal == null ) {
            principalList.add( principal );
            listModel.addElement( principal );
        }

        finishSuccessfulEdit( false );
    }

}
