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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.harmony.jretools.policytool.Consts;

/**
 * Dialog to view the warning and error logs.
 */
public class WarningLogDialog extends JDialog implements ActionListener {

    /** Text area to store and display the log. */
    private final JTextArea logTextArea; 
    /** Reference to the owner frame.           */
    private final Frame     ownerFrame;
    
    /**
     * Creates a new WarningLogDialog.
     * @param ownerFrame reference to the owner frame
     */
    public WarningLogDialog( final Frame ownerFrame ) {
        super( ownerFrame, "Warning log", false );
        this.ownerFrame = ownerFrame;
        setDefaultCloseOperation( HIDE_ON_CLOSE );

        logTextArea = new JTextArea( 15, 50 );
        logTextArea.setEditable( false );
        add( new JScrollPane( logTextArea ), BorderLayout.CENTER );

        final JPanel  panel       = new JPanel();

        final JButton closeButton = new JButton( "Close" );
        closeButton.setMnemonic( closeButton.getText().charAt( 0 ) );
        closeButton.addActionListener( this );
        panel.add( closeButton );

        add( panel, BorderLayout.SOUTH );

        pack();
    }

    /**
     * If parameter <code>visibility</code> is true, first centers the dialog. Calls <code>super.setVisible()</code> afterwards.
     */
    @Override
    public void setVisible( final boolean visibility ) {
        if ( visibility )
            setLocation( ownerFrame.getX() + ownerFrame.getWidth () / 2 - getWidth () / 2,
                         ownerFrame.getY() + ownerFrame.getHeight() / 2 - getHeight() / 2 );
        super.setVisible( visibility );
    }

    /**
     * Adds a message to the log.
     * @param message message to be added
     */
    public void addMessage( final String message ) {
        logTextArea.append( message );
        logTextArea.append( Consts.NEW_LINE_STRING );
        logTextArea.setCaretPosition( logTextArea.getDocument().getLength() );
    }

    /**
     * Handles the action events of the close button.
     */
    public void actionPerformed( final ActionEvent ae ) {
        setVisible( false );
    }

}
