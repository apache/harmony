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
import java.awt.Frame;
import java.awt.GridLayout;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.harmony.jretools.policytool.control.Controller;
import org.apache.harmony.jretools.policytool.model.CommentEntry;
import org.apache.harmony.jretools.policytool.model.KeystoreEntry;
import org.apache.harmony.jretools.policytool.model.KeystorePasswordURLEntry;
import org.apache.harmony.jretools.policytool.model.PolicyEntry;

/**
 * Form dialog to view and edit the keystore and the keystore password url entries.
 */
public class KeystoreEntryEditFormDialog extends BaseFormDialog {

    /** Reference to the initial editable keystore entry or null, if we are creating a new one. */
    private final KeystoreEntry            initialKeystoreEntry;
    /** Reference to the initial editable keystore password URL entry.                          */
    private final KeystorePasswordURLEntry initialKeystorePasswordURLEntry;
    /** List of policy entries where to store if new entry is to be created.                    */
    private final List< PolicyEntry >      policyEntryList;

    /** Text field to view and edit the value of keystore URL.          */
    private final JTextField keystoreURLTextField         = new JTextField( 20 );
    /** Text field to view and edit the value of keystore type.         */
    private final JTextField keystoreTypeTextField        = new JTextField( 20 );
    /** Text field to view and edit the value of keystore provider.     */
    private final JTextField keystoreProviderTextField    = new JTextField( 20 );
    /** Text field to view and edit the value of keystore password URL. */
    private final JTextField keystorePasswordURLTextField = new JTextField( 20 );

    /**
     * Creates a new KeystoreEntryEditFormDialog.
     * @param ownerFrame reference to the owner frame
     * @param ownerEditorPanel reference to the owner editor panel
     * @param keystoreEntry reference to the editable keystore entry or null, if we are creating a new one
     * @param keystorePasswordURLEntry reference to the editable password URL entry
     * @param policyEntryList list of policy entries where to store if new entries are to be created
     */
    public KeystoreEntryEditFormDialog( final Frame ownerFrame, final EditorPanel ownerEditorPanel, final KeystoreEntry keystoreEntry, final KeystorePasswordURLEntry keystorePasswordURLEntry, final List< PolicyEntry > policyEntryList ) {
        super( ownerFrame, "KeyStore", ownerEditorPanel );

        this.initialKeystoreEntry            = keystoreEntry;
        this.initialKeystorePasswordURLEntry = keystorePasswordURLEntry;
        this.policyEntryList                 = policyEntryList;

        prepareForDisplay();
    }

    @Override
    protected void buildGUI() {
        final JPanel panel = new JPanel( new GridLayout( 4, 2, 5, 10 ) );

        panel.add( new JLabel( "KeyStore URL:" ) );
        panel.add( keystoreURLTextField );

        panel.add( new JLabel( "KeyStore Type:" ) );
        panel.add( keystoreTypeTextField );

        panel.add( new JLabel( "KeyStore Provider:" ) );
        panel.add( keystoreProviderTextField );

        panel.add( new JLabel( "KeyStore Password URL:" ) );
        panel.add( keystorePasswordURLTextField );

        if ( initialKeystoreEntry != null ) {
            keystoreURLTextField     .setText( initialKeystoreEntry.getUrl     () );
            keystoreTypeTextField    .setText( initialKeystoreEntry.getType    () );
            keystoreProviderTextField.setText( initialKeystoreEntry.getProvider() );
        }

        if ( initialKeystorePasswordURLEntry != null ) {
            keystorePasswordURLTextField.setText( initialKeystorePasswordURLEntry.getUrl() );
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
        if ( keystoreURLTextField.getText().indexOf( '"' ) >= 0 ) {
            validationFails = true;
            errorStringBuilder.append( "Keystore URL" );
        }
        if ( keystoreTypeTextField.getText().indexOf( '"' ) >= 0 ) {
            errorStringBuilder.append( validationFails ? ", Keystore Type" : "Keystore Type" );
            validationFails = true;
        }
        if ( keystoreProviderTextField.getText().indexOf( '"' ) >= 0 ) {
            errorStringBuilder.append( validationFails ? ", Keystore Provider" : "Keystore Provider" );
            validationFails = true;
        }
        if ( keystorePasswordURLTextField.getText().indexOf( '"' ) >= 0 ) {
            errorStringBuilder.append( validationFails ? ", Keystore Password URL" : "Keystore Password URL" );
            validationFails = true;
        }

        if ( !validationFails && keystoreURLTextField.getText().length() > 0 ) {
            try {
                new URL( keystoreURLTextField.getText() );
            } catch ( final MalformedURLException mue ) {
                validationFails = true;
                errorStringBuilder.setLength( 0 );
                errorStringBuilder.append( "Keystore URL contains a malformed URL: " + mue.getMessage() );
            }
        }

        if ( !validationFails && keystorePasswordURLTextField.getText().length() > 0 ) {
            try {
                new URL( keystorePasswordURLTextField.getText() );
            } catch ( final MalformedURLException mue ) {
                validationFails = true;
                errorStringBuilder.setLength( 0 );
                errorStringBuilder.append( "Keystore Password URL contains a malformed URL: " + mue.getMessage() );
            }
        }

        if ( validationFails ) {
            Controller.logError( errorStringBuilder.toString() );
            JOptionPane.showMessageDialog( this, errorStringBuilder.toString(), "Error!", JOptionPane.ERROR_MESSAGE );
            return;
        }
        // validation end

        final KeystoreEntry keystoreEntry = initialKeystoreEntry == null ? new KeystoreEntry() : initialKeystoreEntry;

        if ( keystoreURLTextField.getText().length() == 0 && keystoreTypeTextField.getText().length() == 0 && keystoreProviderTextField.getText().length() == 0 ) {
            // We want no keystore entry!
            if ( initialKeystoreEntry != null )
                policyEntryList.remove( initialKeystoreEntry );
        } else {
            keystoreEntry.setUrl     ( keystoreURLTextField     .getText() );
            keystoreEntry.setType    ( keystoreTypeTextField    .getText() );
            keystoreEntry.setProvider( keystoreProviderTextField.getText() );

            if ( initialKeystoreEntry == null ) { // If it is a new, we have to add it to the "global" list
                // We want to add it to be the first non-comment entry
                int index = 0;
                for ( ; index < policyEntryList.size(); index++ )
                    if ( !( policyEntryList.get( index ) instanceof CommentEntry ) )
                        break;
                policyEntryList.add( index, keystoreEntry );
            }
        }

        if ( keystorePasswordURLTextField.getText().length() == 0 ) {
            // We want no keystore password URL entry!
            if ( initialKeystorePasswordURLEntry != null )
                policyEntryList.remove( initialKeystorePasswordURLEntry );
        } else {
            final KeystorePasswordURLEntry keystorePasswordURLEntry = initialKeystorePasswordURLEntry == null ? new KeystorePasswordURLEntry() : initialKeystorePasswordURLEntry;
            keystorePasswordURLEntry.setUrl( keystorePasswordURLTextField.getText() );

            if ( initialKeystorePasswordURLEntry == null ) { // If it is a new, we have to add it to the "global" list
                // We want to add it after the keystore entry
                int index = policyEntryList.indexOf( keystoreEntry );
                if ( index < 0 ) { // No such entry exists, we want it to be the first non-comment entry then
                    index = 0;
                    for ( ; index < policyEntryList.size(); index++ )
                        if ( !( policyEntryList.get( index ) instanceof CommentEntry ) )
                            break;
                } else
                    index++;  // => after the keystore entry (not before)
                policyEntryList.add( index, keystorePasswordURLEntry );
            }
        }

        finishSuccessfulEdit();
    }

}
