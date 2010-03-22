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

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import javax.swing.JOptionPane;

/**
 * This class is responsible to read policy text from and write policy text to policy files.<br>
 * We're aware of the UTF-8 policy file encoding.
 */
public class PolicyFileHandler {

    /** Encoding of the policy file by the specification. */
    private static final String POLICY_FILE_ENCODING = "UTF-8";

    /** Platform dependent line separator. */
    private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

    /** Parent component to be used when displaying dialogs. */
    private static Component dialogParentComponent;

    /**
     * Sets the parent component to be used when displaying dialogs.
     * @param dialogParentComponent component to be used when displaying dialogs
     */
    public static void setDialogParentComponent( final Component dialogParentComponent ) {
        PolicyFileHandler.dialogParentComponent = dialogParentComponent;
    }

    /**
     * Loads the content of a policy file.
     * @param policyFile policy file whose content to be loaded
     * @return the policy text within the given policy file
     */
    public static String loadPolicyFile( final File policyFile ) {
        final StringBuilder policyTextBuilder = new StringBuilder();

        BufferedReader input = null;
        try {

            input = new BufferedReader( new InputStreamReader( new FileInputStream( policyFile ), POLICY_FILE_ENCODING ) );
            String line;
            while ( ( line = input.readLine() ) != null )
                policyTextBuilder.append( line ).append( LINE_SEPARATOR );

        } catch ( final FileNotFoundException fnfe ) {
            JOptionPane.showMessageDialog( dialogParentComponent, "The file does not exist!", "Error", JOptionPane.ERROR_MESSAGE );
            return null;
        } catch ( final UnsupportedEncodingException uee ) {
            // This should never happen.
            uee.printStackTrace();
            return null;
        } catch ( final IOException ie ) {
            JOptionPane.showMessageDialog( dialogParentComponent, new String[] { "I/O error occured, can't read the file!", ie.getMessage() }, "Error", JOptionPane.ERROR_MESSAGE );
            return null;
        } finally {
            if ( input != null )
                try {
                    input.close();
                } catch ( final IOException ie ) {
                    ie.printStackTrace();
                }
        }

        return policyTextBuilder.toString();
    }

    /**
     * Saves policy text to a policy file.
     * @param policyFile policy file to save to
     * @param policyText policy text to be saved
     * @return true, if saving was successful; false otherwise
     */
    public static boolean savePolicyFile( final File policyFile, final String policyText ) {
        OutputStreamWriter output = null;
        try {
            output = new OutputStreamWriter( new FileOutputStream( policyFile ), POLICY_FILE_ENCODING );

            output.write( policyText );

            return true;
        } catch ( final UnsupportedEncodingException uee ) {
            // This should never happen.
            uee.printStackTrace();
        } catch ( final FileNotFoundException ffe ) {
            JOptionPane.showMessageDialog( dialogParentComponent, new String[] { "Cannot open file for writing!", ffe.getMessage() }, "Error", JOptionPane.ERROR_MESSAGE );
            ffe.printStackTrace();
        } catch ( final IOException ie ) {
            JOptionPane.showMessageDialog( dialogParentComponent, new String[] { "Write error!", ie.getMessage() }, "Error", JOptionPane.ERROR_MESSAGE );
            ie.printStackTrace();
        } finally {
            if ( output != null )
                try {
                    output.close();
                } catch ( final IOException ie ) {
                    ie.printStackTrace();
                }
        }

        return false;
    }

}
