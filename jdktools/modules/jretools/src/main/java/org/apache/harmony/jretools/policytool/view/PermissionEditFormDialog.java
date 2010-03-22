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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.harmony.jretools.policytool.control.Controller;
import org.apache.harmony.jretools.policytool.model.Permission;

/**
 * Form dialog to view and edit the permissions of a grant entry.
 */
public class PermissionEditFormDialog extends LAEFormDialog {

    /** Names of the permission types. */
    private static final String[] DEFAULT_PERMISSION_TYPE_NAMES       =
        new String[] { "Permission:", "AllPermission"              , "AudioPermission"                    , "AuthPermission"                    , "AWTPermission"         , "DelegationPermission"                             , "FilePermission"        , "LoggingPermission"                  , "ManagementPermission"                     , "MBeanPermission"                 , "MBeanServerPermission"                 , "MBeanTrustPermission"                  , "NetPermission"        , "PrivateCredentialPermission"                    , "PropertyPermission"          , "ReflectPermission"                  , "RuntimePermission"          , "SecurityPermission"              , "SerializablePermission"        , "ServicePermission"                             , "SocketPermission"         , "SQLPermission"         , "SSLPermission"              , "SubjectDelegationPermission"                         };
    /** Default names of the permission type classes to be set when chosen. Null value means not to change it. */
    private static final String[] DEFAULT_PERMISSION_TYPE_CLASS_NAMES =
        new String[] { null         , "java.security.AllPermission", "javax.sound.sampled.AudioPermission", "javax.security.auth.AuthPermission", "java.awt.AWTPermission", "javax.security.auth.kerberos.DelegationPermission", "java.io.FilePermission", "java.util.logging.LoggingPermission", "java.lang.management.ManagementPermission", "javax.management.MBeanPermission", "javax.management.MBeanServerPermission", "javax.management.MBeanTrustPermission", "java.net.NetPermission", "javax.security.auth.PrivateCredentialPermission", "java.util.PropertyPermission", "java.lang.reflect.ReflectPermission", "java.lang.RuntimePermission", "java.security.SecurityPermission", "java.io.SerializablePermission", "javax.security.auth.kerberos.ServicePermission", "java.net.SocketPermission", "java.sql.SQLPermission", "javax.net.ssl.SSLPermission", "javax.management.remote.SubjectDelegationPermission" };

    /** Maps the permission type names to their possible target names and actions.<br>
     * The key is the permission type name, the value is a 2-length string array of string arrays, of which:
     * <ol start=0>
     *     <li>element is the array of possible target names (if null, target name text field has to be disabled)
     *     <li>element is the array of possible actions (if null, actions text field has to be disabled)
     * </ol>*/
    private static final Map< String, String[][] > permissionTypeTargetNamesActionsMap = new HashMap< String, String[][] >();
    static {
        permissionTypeTargetNamesActionsMap.put( "AllPermission"              , new String[][] { null, null } );
        permissionTypeTargetNamesActionsMap.put( "AudioPermission"            , new String[][] { new String[] { "play", "record" }, null } );
        permissionTypeTargetNamesActionsMap.put( "AuthPermission"             , new String[][] { new String[] { "doAs", "doAsPrivileged", "getSubject", "getSubjectFromDomainCombiner", "setReadOnly", "modifyPrincipals", "modifyPublicCredentials", "modifyPrivateCredentials", "refreshCredentials", "destroyCredentials", "createLoginContext.<name>", "getLoginConfiguration", "setLoginConfiguration", "createLoginConfiguration.<configuration type>", "refreshLoginConfiguration" }, null } );
        permissionTypeTargetNamesActionsMap.put( "AWTPermission"              , new String[][] { new String[] { "accessClipboard", "accessEventQueue" , "accessSystemTray" , "createRobot" , "fullScreenExclusive" , "listenToAllAWTEvents" , "readDisplayPixels" , "replaceKeyboardFocusManager" , "setAppletStub" , "setWindowAlwaysOnTop" , "showWindowWithoutWarningBanner" , "toolkitModality" , "watchMousePointer" }, null } );
        permissionTypeTargetNamesActionsMap.put( "DelegationPermission"       , new String[][] { new String[] {}, null } );
        permissionTypeTargetNamesActionsMap.put( "FilePermission"             , new String[][] { new String[] { "<<ALL FILES>>" }, new String[] { "read", "write", "delete", "execute" } } );
        permissionTypeTargetNamesActionsMap.put( "LoggingPermission"          , new String[][] { new String[] { "control" }, null } );
        permissionTypeTargetNamesActionsMap.put( "ManagementPermission"       , new String[][] { new String[] { "control", "monitor" }, null } );
        permissionTypeTargetNamesActionsMap.put( "MBeanPermission"            , new String[][] { new String[] {}, new String[] { "addNotificationListener", "getAttribute", "getClassLoader", "getClassLoaderFor", "getClassLoaderRepository", "getDomains", "getMBeanInfo", "getObjectInstance", "instantiate", "invoke", "isInstanceOf", "queryMBeans", "queryNames", "registerMBean", "removeNotificationListener", "setAttribute", "unregisterMBean" } } );
        permissionTypeTargetNamesActionsMap.put( "MBeanServerPermission"      , new String[][] { new String[] { "createMBeanServer", "findMBeanServer", "newMBeanServer", "releaseMBeanServer" }, null } );
        permissionTypeTargetNamesActionsMap.put( "MBeanTrustPermission"       , new String[][] { new String[] { "register" }, null } );
        permissionTypeTargetNamesActionsMap.put( "NetPermission"              , new String[][] { new String[] { "setDefaultAuthenticator", "requestPasswordAuthentication" , "specifyStreamHandler" , "setProxySelector" , "getProxySelector" , "setCookieHandler" , "getCookieHandler" , "setResponseCache" , "getResponseCache" }, null } );
        permissionTypeTargetNamesActionsMap.put( "PrivateCredentialPermission", new String[][] { new String[] {}, new String[] { "read" } } );
        permissionTypeTargetNamesActionsMap.put( "PropertyPermission"         , new String[][] { new String[] {}, new String[] { "read", "write" } } );
        permissionTypeTargetNamesActionsMap.put( "ReflectPermission"          , new String[][] { new String[] { "suppressAccessChecks" }, null } );
        permissionTypeTargetNamesActionsMap.put( "RuntimePermission"          , new String[][] { new String[] { "createClassLoader", "getClassLoader", "setContextClassLoader", "enableContextClassLoaderOverride", "setSecurityManage", "createSecurityManager", "getenv.<environment variable name>", "exitVM", "shutdownHooks", "setFactory", "setIO", "modifyThread", "stopThread", "modifyThreadGroup", "getProtectionDomain", "readFileDescriptor", "writeFileDescriptor", "loadLibrary.<library name>", "accessClassInPackage.<package name>", "defineClassInPackage.<package name>", "accessDeclaredMembers", "queuePrintJob", "getStackTrace", "setDefaultUncaughtExceptionHandler", "preferences", "usePolicy" }, null } );
        permissionTypeTargetNamesActionsMap.put( "SecurityPermission"         , new String[][] { new String[] { "createAccessControlContext", "getDomainCombiner", "getPolicy", "setPolicy", "createPolicy.<policy type>", "getProperty.<property name>", "setProperty.<property name>", "insertProvider.<provider name>", "removeProvider.<provider name>", "clearProviderProperties.<provider name>", "putProviderProperty.<provider name>", "removeProviderProperty.<provider name>" }, null } );
        permissionTypeTargetNamesActionsMap.put( "SerializablePermission"     , new String[][] { new String[] { "enableSubclassImplementation", "enableSubstitution" }, null } );
        permissionTypeTargetNamesActionsMap.put( "ServicePermission"          , new String[][] { new String[] {}, new String[] { "initiate", "accept" } } );
        permissionTypeTargetNamesActionsMap.put( "SocketPermission"           , new String[][] { new String[] {}, new String[] { "accept", "connect", "listen", "resolve" } } );
        permissionTypeTargetNamesActionsMap.put( "SQLPermission"              , new String[][] { new String[] { "setLog" }, null } );
        permissionTypeTargetNamesActionsMap.put( "SSLPermission"              , new String[][] { new String[] { "setHostnameVerifier", "getSSLSessionContext" }, null } );
        permissionTypeTargetNamesActionsMap.put( "SubjectDelegationPermission", new String[][] { new String[] {}, null } );
    }

    /** Default item for the target name combo box. */
    private static final String DEFAULT_TARGET_NAME_COMBO_BOX_ITEM = "Target Name:";
    /** Default item for the actions combo box.     */
    private static final String DEFAULT_ACTIONS_COMBO_BOX_ITEM     = "Actions:";

    /** Reference to the initial editable permission or null, if we are creating a new one. */
    private final Permission         initialPermission;
    /** List of permissions where to store if new permission is to be created.              */
    private final List< Permission > permissionList;

    /** Model for the target name combo box. */
    private final DefaultComboBoxModel targetNameComboBoxModel = new DefaultComboBoxModel( new Object[] { DEFAULT_TARGET_NAME_COMBO_BOX_ITEM } );
    /** Model for the actions combo box.     */
    private final DefaultComboBoxModel actionsComboBoxModel    = new DefaultComboBoxModel( new Object[] { DEFAULT_ACTIONS_COMBO_BOX_ITEM     } );

    /** Combo box to view and choose the permission type.             */
    private final JComboBox  permissionTypeComboBox  = new JComboBox( DEFAULT_PERMISSION_TYPE_NAMES );
    /** Text field to view and edit the permission type (class name). */
    private final JTextField permissionTypeTextField = new JTextField( 28 );
    /** Combo box to view and choose the target name.                 */
    private final JComboBox  targetNameComboBox      = new JComboBox( targetNameComboBoxModel );
    /** Text field to view and edit the target name.                  */
    private final JTextField targetNameTextField     = new JTextField( 28 );
    /** Combo box to view and choose the actions.                     */
    private final JComboBox  actionsComboBox         = new JComboBox( actionsComboBoxModel );
    /** Text field to view and edit the actions.                      */
    private final JTextField actionsTextField        = new JTextField( 28 );
    /** Text field to view and edit the signed by.                    */
    private final JTextField signedByTextField       = new JTextField( 28 );

    /**
     * Creates a new PermissionEditFormDialog.
     * @param ownerDialog reference to the owner dialog
     * @param ownerEditorPanel reference to the owner editor panel
     * @param permission reference to the editable permission or null, if we are creating a new one
     * @param permissionList list of permissions where to store if new permission is to be created
     */
    public PermissionEditFormDialog( final Dialog ownerDialog, final EditorPanel ownerEditorPanel, final Permission permission, final List< Permission > permissionList ) {
        super( ownerDialog, "Permission", ownerEditorPanel );

        this.initialPermission = permission;
        this.permissionList    = permissionList;

        prepareForDisplay();
    }

    @Override
    protected void buildGUI() {
        final JPanel panel = new JPanel( new GridLayout( 4, 2, 5, 10 ) );

        permissionTypeComboBox.addActionListener( new ActionListener() {
            public void actionPerformed( final ActionEvent ae ) {
                final String classNameForSelectedType = DEFAULT_PERMISSION_TYPE_CLASS_NAMES[ permissionTypeComboBox.getSelectedIndex() ];
                if ( classNameForSelectedType != null ) {
                    permissionTypeTextField.setText( classNameForSelectedType );

                    targetNameTextField.setText( null );
                    targetNameComboBoxModel.removeAllElements();
                    targetNameComboBoxModel.addElement( DEFAULT_TARGET_NAME_COMBO_BOX_ITEM );

                    actionsTextField.setText( null );
                    actionsComboBoxModel.removeAllElements();
                    actionsComboBoxModel.addElement( DEFAULT_ACTIONS_COMBO_BOX_ITEM );

                    final String[][] targetNameActions = permissionTypeTargetNamesActionsMap.get( DEFAULT_PERMISSION_TYPE_NAMES[ permissionTypeComboBox.getSelectedIndex() ] );
                    if ( targetNameActions[ 0 ] == null )
                        targetNameTextField.setEnabled( false );
                    else {
                        targetNameTextField.setEnabled( true );
                        for ( final String targetName : targetNameActions[ 0 ] )
                            targetNameComboBoxModel.addElement( targetName );
                    }
                    if ( targetNameActions[ 1 ] == null )
                        actionsTextField.setEnabled( false );
                    else {
                        actionsTextField.setEnabled( true );
                        for ( final String actions : targetNameActions[ 1 ] )
                            actionsComboBoxModel.addElement( actions );
                    }
                }
            }
        } );
        panel.add( permissionTypeComboBox );
        panel.add( permissionTypeTextField );

        targetNameComboBox.addActionListener( new ActionListener() {
            public void actionPerformed( final ActionEvent ae ) {
                if ( targetNameComboBox.getSelectedIndex() > 0 )
                    targetNameTextField.setText( targetNameComboBox.getSelectedItem().toString() );
            }
        } );
        panel.add( targetNameComboBox );
        panel.add( targetNameTextField );

        actionsComboBox.addActionListener( new ActionListener() {
            public void actionPerformed( final ActionEvent ae ) {
                if ( actionsComboBox.getSelectedIndex() > 0 )
                    actionsTextField.setText( actionsTextField.getText() + ( actionsTextField.getText().length() > 0 ? ", " : "" ) + actionsComboBox.getSelectedItem().toString() );
            }
        } );
        panel.add( actionsComboBox );
        panel.add( actionsTextField );

        panel.add( new JLabel( "Signed By:" ) );
        panel.add( signedByTextField );

        if ( initialPermission != null ) {
            // Should we choose anything in the permission type combo box?
            if ( initialPermission.getClassName() != null )
                for ( int i = 0; i < DEFAULT_PERMISSION_TYPE_CLASS_NAMES.length; i++ )
                    if ( DEFAULT_PERMISSION_TYPE_CLASS_NAMES[ i ] != null && DEFAULT_PERMISSION_TYPE_CLASS_NAMES[ i ].equals( initialPermission.getClassName() ) ) {
                        permissionTypeComboBox.setSelectedIndex( i );
                        break;
                    }
            permissionTypeTextField.setText( initialPermission.getClassName() );

            final String[][] targetNameActions = permissionTypeTargetNamesActionsMap.get( DEFAULT_PERMISSION_TYPE_NAMES[ permissionTypeComboBox.getSelectedIndex() ] );
            final String[]   targetNames       = targetNameActions[ 0 ];
            final String[]   actions           = targetNameActions[ 1 ];

            // Should we choose anything in the target name combo box?
            if ( initialPermission.getTargetName() != null && targetNames != null )
                for ( int i = 0; i < targetNames.length; i++ )
                    if ( targetNames[ i ].equals( initialPermission.getTargetName() ) ) {
                        targetNameComboBox.setSelectedIndex( i+1 ); // +1 for the constant "Target Name:" item
                        break;
                    }
            targetNameTextField.setText( initialPermission.getTargetName() );

            // Should we choose anything in the actions combo box?
            if ( initialPermission.getActions() != null && actions != null )
                for ( int i = 0; i < actions.length; i++ )
                    if ( actions[ i ].equals( initialPermission.getActions() ) ) {
                        actionsComboBox.setSelectedIndex( i ); // +1 for the constant "Actions:" item
                        break;
                    }
            actionsTextField.setText( initialPermission.getActions() );

            signedByTextField.setText( initialPermission.getSignedBy() );
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
        if ( targetNameTextField.getText().indexOf( '"' ) >= 0 ) {
            validationFails = true;
            errorStringBuilder.append( "Target Name" );
        }
        if ( actionsTextField.getText().indexOf( '"' ) >= 0 ) {
            errorStringBuilder.append( validationFails ? ", Actions" : "Actions" );
            validationFails = true;
        }
        if ( signedByTextField.getText().indexOf( '"' ) >= 0 ) {
            errorStringBuilder.append( validationFails ? ", Signed By" : "Signed By" );
            validationFails = true;
        }

        if ( !validationFails )
            if ( permissionTypeTextField.getText().length() == 0 || targetNameTextField.isEnabled() && targetNameTextField.getText().length() == 0 ) {
                validationFails = true;
                errorStringBuilder.setLength( 0 );
                errorStringBuilder.append( "Permission and target name must have a value!" );
            }

        if ( validationFails ) {
            Controller.logError( errorStringBuilder.toString() );
            JOptionPane.showMessageDialog( this, errorStringBuilder.toString(), "Error!", JOptionPane.ERROR_MESSAGE );
            return;
        }
        // validation end

        final Permission permission = initialPermission == null ? new Permission() : initialPermission;

        permission.setClassName ( permissionTypeTextField.getText() );
        permission.setTargetName( targetNameTextField    .getText() );
        permission.setActions   ( actionsTextField       .getText() );
        permission.setSignedBy  ( signedByTextField      .getText() );

        if ( initialPermission == null ) {
            permissionList.add( permission );
            listModel.addElement( permission );
        }

        finishSuccessfulEdit( false );
    }

}
