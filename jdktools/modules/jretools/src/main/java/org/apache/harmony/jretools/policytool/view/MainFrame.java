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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.apache.harmony.jretools.policytool.Consts;
import org.apache.harmony.jretools.policytool.control.Controller;

/**
 * This is the main frame of policytool.
 */
public class MainFrame extends JFrame {

    /** Text field to display the current policy file. */
    private final JTextField policyFileDisplayerTextField = new JTextField();

    /**
     * Creates a new <code>MainFrame</code> with no initial poilcy file.
     */
    public MainFrame() {
        this( null );
    }

    /**
     * Creates a new <code>MainFrame</code>.
     * @param policyFileName policy file name to be loaded initially
     */
    public MainFrame( final String policyFileName ) {
        super( Consts.APPLICATION_NAME );

        // I set icon image before controller, because controller creates WarningLogDialog passign main frame as owner,
        // and icon image is inherited. This way Warning Log Dialog will have the same icon.
        final URL apahceIconURL = MainFrame.class.getResource( "apache.gif" );
        if ( apahceIconURL != null )
            setIconImage( new ImageIcon( apahceIconURL ).getImage() );

        final EditorPanel[] editorPanels = new EditorPanel[] { new GraphicalEditorPanel( this ), new DirectTextEditorPanel( this ) };
        final Controller    controller   = new Controller( this, editorPanels, policyFileName );

        buildGUI( controller );

        setLocation( Consts.MAIN_FRAME_START_POS_X, Consts.MAIN_FRAME_START_POS_X );
        setSize( Consts.MAIN_FRAME_WIDTH, Consts.MAIN_FRAME_HEIGHT );
        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
        addWindowListener( new WindowAdapter() {
            public void windowClosing( final WindowEvent we ) {
                controller.exit();
            }
        } );
    }

    /**
     * Builds the graphical user interface of the main frame.
     * @param controller reference to the controller
     */
    private void buildGUI( final Controller controller ) {
        buildMenusAndMenuBar( controller );

        final JPanel panel = new JPanel( new BorderLayout( 5, 0 ) );
        panel.add( new JLabel( " Policy file:" ), BorderLayout.WEST );

        policyFileDisplayerTextField.setEditable( false );
        panel.add( policyFileDisplayerTextField, BorderLayout.CENTER );
        add( panel, BorderLayout.NORTH );

        buildTabbedPane( controller );
    }

    /**
     * Menu items of policytool.
     */
    public static enum MenuItemEnum {
        /** File menu                  */
        FILE            ( true, "File"       ),
        /** New menu item              */
        NEW             ( "New"              ),
        /** Save menu item             */
        OPEN            ( "Open"             ),
        /** Save menu item             */
        SAVE            ( "Save"             ),
        /** Save as menu item          */
        SAVE_AS         ( "Save As...", 'a'  ),
        /** View warning log menu item */
        VIEW_WARNING_LOG( "View Warning Log" ),
        /** Exit menu item             */
        EXIT            ( "Exit", 'x'        ),
        /** KeyStore menu              */
        KEY_STORE       ( true, "KeyStore"   ),
        /** Edit menu item             */
        EDIT            ( "Edit"             );

        /** If true, then this represents a menu instead of a menu item. */
        public final boolean isMenu;
        /** Text of the menu item.                                       */
        public final String  text;
        /** Mnemonic for the menu item.                                  */
        public final char    mnemonic;

        /**
         * Creates a new MenuItemEnum with a default mnemonic of the first character of its text.
         * @param isMenu indicating if this will be a menu
         * @param text text of the menu item
         */
        private MenuItemEnum( final boolean isMenu, final String text ) {
            this( isMenu, text, text.charAt( 0 ) );
        }

        /**
         * Creates a new MenuItemEnum with a default mnemonic of the first character of its text.
         * @param text text of the menu item
         */
        private MenuItemEnum( final String text ) {
            this( false, text, text.charAt( 0 ) );
        }

        /**
         * Creates a new MenuItemEnum.
         * @param text text of the menu item
         * @param mnemonic mnemonic for the menu item
         */
        private MenuItemEnum( final String text, final char mnemonic ) {
            this( false, text, mnemonic );
        }

        /**
         * Creates a new MenuItemEnum.
         * @param isMenu indicating if this will be a menu
         * @param text text of the menu item
         * @param mnemonic mnemonic for the menu item
         */
        private MenuItemEnum( final boolean isMenu, final String text, final char mnemonic ) {
            this.isMenu   = isMenu;
            this.text     = text;
            this.mnemonic = mnemonic;
        }

    };

    /**
     * Builds the menus and the menu bar.
     * @param controller reference to the controller
     */
    private void buildMenusAndMenuBar( final Controller controller ) {
        final JMenuBar menuBar = new JMenuBar();

        JMenu menu = null;
        for ( final MenuItemEnum menuItemEnum : MenuItemEnum.values() ) {
            if ( menuItemEnum.isMenu ) {
                menu = new JMenu( menuItemEnum.text );
                menu.setMnemonic( menuItemEnum.mnemonic );
                menuBar.add( menu );
            } else {
                final JMenuItem menuItem = new JMenuItem( menuItemEnum.text );
                menuItem.setMnemonic( menuItemEnum.mnemonic );
                menuItem.addActionListener( controller );
                menuItem.setActionCommand( Integer.toString( menuItemEnum.ordinal() ) );
                menu.add( menuItem );
                if ( menuItemEnum == MenuItemEnum.EDIT )
                    controller.setKeystoreEditMenuItem( menuItem );
            }
        }

        setJMenuBar( menuBar );
    }

    /**
     * Builds the tabbed pane containing the editor panels.
     * @param controller reference to the controller
     */
    private void buildTabbedPane( final Controller controller ) {
        final JTabbedPane   tabbedPane   = new JTabbedPane();
        final EditorPanel[] editorPanels = controller.getEditorPanels();

        for ( int i = 0; i < editorPanels.length; i++ ) {
            final EditorPanel editorPanel = editorPanels[ i ];
            final String      panelTitle  = (i+1) + " " + editorPanel.getPanelTitle();

            tabbedPane.addTab( panelTitle, editorPanel );

            if ( i < 9 ) // We only set 1..9 mnemonic characters
                tabbedPane.setMnemonicAt( i, '1' + i );
        }

        tabbedPane.addChangeListener( controller );

        add( tabbedPane , BorderLayout.CENTER );
    }

    /**
     * Sets the displayed policy file.
     * @param displayedPolicyFile displayed policy file to be set
     */
    public void setDisplayedPolicyFile( final File displayedPolicyFile ) {
        try {
            policyFileDisplayerTextField.setText( displayedPolicyFile == null ? null : displayedPolicyFile.getCanonicalPath() );
        } catch ( final IOException ie ) {
            // This should never happen...
            ie.printStackTrace();
        }
    }

}
