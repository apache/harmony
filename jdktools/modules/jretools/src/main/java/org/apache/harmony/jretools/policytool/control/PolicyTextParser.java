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

import java.util.ArrayList;
import java.util.List;

import org.apache.harmony.jretools.policytool.model.CommentEntry;
import org.apache.harmony.jretools.policytool.model.GrantEntry;
import org.apache.harmony.jretools.policytool.model.KeystoreEntry;
import org.apache.harmony.jretools.policytool.model.KeystorePasswordURLEntry;
import org.apache.harmony.jretools.policytool.model.Permission;
import org.apache.harmony.jretools.policytool.model.PolicyEntry;
import org.apache.harmony.jretools.policytool.model.Principal;

/**
 * Policy text parser.
 *
 */
public class PolicyTextParser {

    /** String containing the white spaces. */
    private static final String WHITE_SPACES  = " \t\n\r";

    /** Policy text to be parsed.                                                     */
    private final String              policyText;
    /** Char array of the policy text.                                                */
    private final char[]              policyTextChars;
    /** The created equivalent list of policy entries.                                */
    private final List< PolicyEntry > policyEntryList = new ArrayList< PolicyEntry >();

    /** First and last indices of the current/first token determining the next entry. */
    private int[] firstLastTokenIndices;
    /** Position of the parsing.                                                      */
    private int                       index           = 0;
    /** New position of parsing after the current entry is processed.                 */
    private int                       newIndex        = -1;

    /**
     * Creates a new PolicyTextParser.
     * @param policyText policy text to be parsed
     */
    public PolicyTextParser( final String policyText ) {
        this.policyText = policyText.replace( "\r", "" ); // I only want to handle a unified line terminator
        policyTextChars = this.policyText.toCharArray();
    }

    /**
     * Parses a policy text and returns an equivalent list of policy entries from it.
     * @param policyText policy text to be parsed
     * @return an equivalent list of policy entries created from the policy text, equivalent to policy text
     * @throws InvalidPolicyTextException thrown if policyText is invalid
     */
    public static List< PolicyEntry > parsePolicyText( final String policyText ) throws InvalidPolicyTextException {
        return new PolicyTextParser( policyText ).parsePolicyText();
    }

    /**
     * Parses the policy text and creates an equivalent list of policy entries from it.
     * @return an equivalent list of policy entries created from the policy text, equivalent to policy text
     * @throws InvalidPolicyTextException thrown if policyText is invalid
     */
    public List< PolicyEntry > parsePolicyText() throws InvalidPolicyTextException {
        while ( ( firstLastTokenIndices = peekTokenAheadCommentsIncluded( index ) ) != null ) {

            final String nextToken        = policyText.substring( firstLastTokenIndices[ 0 ], firstLastTokenIndices[ 1 ] );
            final String loweredNextToken = nextToken.toLowerCase();

            // Line comment
            if ( nextToken.startsWith( "//" ) )
                parseLineComment();

            // Block comment
            else if ( nextToken.startsWith( "/*" ) )
                parseBlockComment();

            // Keystore entry
            else if ( loweredNextToken.equals( KeystoreEntry.LOWERED_KEYWORD ) )
                parseKeystoreEntry();

            // Keystore password URL entry
            else if ( loweredNextToken.equals( KeystorePasswordURLEntry.LOWERED_KEYWORD ) )
                parseKeystorePasswordURLEntry();

            // Grant entry
            else if ( loweredNextToken.equals( GrantEntry.LOWERED_KEYWORD ) )
                parseGrantEntry();

            // Couldn't recognize any entry!
            else
                throw new InvalidPolicyTextException( "Unknown entry: " + nextToken );

            if ( newIndex >= 0 && newIndex < policyTextChars.length && policyTextChars[ newIndex ] == '\n' )
                newIndex++;

            index = newIndex;
        }

        return policyEntryList;
    }

    /**
     * Parses a line comment.
     */
    private void parseLineComment() {
        newIndex = policyText.indexOf( '\n', firstLastTokenIndices[ 0 ] + 2 );
        if ( newIndex < 0 ) // This is the last line
            newIndex = policyTextChars.length;
        policyEntryList.add( new CommentEntry( policyText.substring( index, newIndex ) ) );
    }

    /**
     * Parses a block comment.
     * @throws InvalidPolicyTextException thrown if closing bracket is missing for block comment
     */
    private void parseBlockComment() throws InvalidPolicyTextException {
        newIndex = policyText.indexOf( "*/", firstLastTokenIndices[ 0 ] + 2 );
        if ( newIndex < 0 ) // No closing bracket
            throw new InvalidPolicyTextException( "No closing bracket for block comment!" );
        newIndex += 2;  // length of "*/"
        policyEntryList.add( new CommentEntry( policyText.substring( index, newIndex ) ) );
    }

    /**
     * Parses a keystore entry.<br>
     * The keystoer entry syntax:
     * <pre>
     * keystore "some_keystore_url", "keystore_type", "keystore_provider";
     * </pre>
     * @throws InvalidPolicyTextException thrown if invalid keystore entry syntax is detected
     */
    private void parseKeystoreEntry() throws InvalidPolicyTextException {
        final int[] keystoreURLIndices = peekQuotedStringAhead( firstLastTokenIndices[ 1 ] );
        if ( keystoreURLIndices == null )
            throw new InvalidPolicyTextException( "Incomplete keystore entry, found no quoted string for keystore URL!" );

        int[] keystoreTypeIndices     = null;
        int[] keystoreProviderIndices = null;

        char nextChar = peekNextNonWhiteSpaceChar( keystoreURLIndices[ 1 ] );
        if ( nextChar != PolicyEntry.TERMINATOR_CHAR ) {
            if ( nextChar != ',' )
                throw new InvalidPolicyTextException( "Was expecting semicolon but found something else!" );
            keystoreTypeIndices = peekQuotedStringAhead( skipWhiteSpaces( keystoreURLIndices[ 1 ] ) + 1 );
            if ( keystoreTypeIndices == null )
                throw new InvalidPolicyTextException( "Incomplete keystore entry, found no quoted string for keystore type!" );

            nextChar = peekNextNonWhiteSpaceChar( keystoreTypeIndices[ 1 ] );
            if ( nextChar != PolicyEntry.TERMINATOR_CHAR ) {
                if ( nextChar != ',' )
                    throw new InvalidPolicyTextException( "Was expecting semicolon but found something else!" );
                keystoreProviderIndices = peekQuotedStringAhead( skipWhiteSpaces( keystoreTypeIndices[ 1 ] ) + 1 );
                if ( keystoreProviderIndices == null )
                    throw new InvalidPolicyTextException( "Incomplete keystore entry, found no quoted string for keystore provider!" );

                if ( peekNextNonWhiteSpaceChar( keystoreProviderIndices[ 1 ] ) != PolicyEntry.TERMINATOR_CHAR )
                    throw new InvalidPolicyTextException( "Was expecting semicolon but found something else!" );
                else
                    newIndex = skipWhiteSpaces( keystoreProviderIndices[ 1 ] ) + 1;
            }
            else
                newIndex = skipWhiteSpaces( keystoreTypeIndices[ 1 ] ) + 1;
        }
        else
            newIndex = skipWhiteSpaces( keystoreURLIndices[ 1 ] ) + 1;

        final KeystoreEntry keystoreEntry = new KeystoreEntry();
        keystoreEntry.setUrl( policyText.substring( keystoreURLIndices[ 0 ] + 1, keystoreURLIndices[ 1 ] - 1 ) );
        if ( keystoreTypeIndices != null )
            keystoreEntry.setType( policyText.substring( keystoreTypeIndices[ 0 ] + 1, keystoreTypeIndices[ 1 ] - 1 ) );
        if ( keystoreProviderIndices != null )
            keystoreEntry.setProvider( policyText.substring( keystoreProviderIndices[ 0 ] + 1, keystoreProviderIndices[ 1 ] - 1 ) );
        policyEntryList.add( keystoreEntry );
    }

    /**
     * Parses a keystore password URL entry.<br>
     * The keystore password URL syntax:
     * <pre>
     * keystorePasswordURL "some_password_url";
     * </pre>
     * @throws InvalidPolicyTextException thrown if invalid keystore password URL entry syntax is detected
     */
    private void parseKeystorePasswordURLEntry() throws InvalidPolicyTextException {
        final int[] keystorePasswordURLIndices = peekQuotedStringAhead( firstLastTokenIndices[ 1 ] );
        if ( keystorePasswordURLIndices == null )
            throw new InvalidPolicyTextException( "Incomplete keystore password URL entry, found no quoted string for keystore password URL!" );

        final char nextChar = peekNextNonWhiteSpaceChar( keystorePasswordURLIndices[ 1 ] );
        if ( nextChar != PolicyEntry.TERMINATOR_CHAR )
            throw new InvalidPolicyTextException( "Was expecting semicolon but found something else!" );
        else
            newIndex = skipWhiteSpaces( keystorePasswordURLIndices[ 1 ] ) + 1;

        final KeystorePasswordURLEntry keystorePasswordURLEntry = new KeystorePasswordURLEntry();
        keystorePasswordURLEntry.setUrl( policyText.substring( keystorePasswordURLIndices[ 0 ] + 1, keystorePasswordURLIndices[ 1 ] - 1 ) );
        policyEntryList.add( keystorePasswordURLEntry );
    }

    /**
     * Parses a grant entry.<br>
     * The grant entry syntax:
     * <pre>
     * grant signedBy "signer_names", codeBase "URL",
     *       principal principal_class_name "principal_name",
     *       principal principal_class_name "principal_name",
     *       ... {
     *
     *     permission permission_class_name "target_name", "action", 
     *         signedBy "signer_names";
     *     permission permission_class_name "target_name", "action", 
     *         signedBy "signer_names";
     *     ...
     * };
     * </pre>
     * @throws InvalidPolicyTextException thrown if invalid grant entry syntax is detected
     */
    private void parseGrantEntry() throws InvalidPolicyTextException {
        final GrantEntry grantEntry = new GrantEntry();

        newIndex = firstLastTokenIndices[ 1 ];

        boolean firstIteration = true;
        char    nextChar;
        // First we parse the grant parameters such as signedBy, codeBase and the principals.
        while ( ( nextChar = peekNextNonWhiteSpaceChar( newIndex ) ) != '{' ) {

            if ( firstIteration )
                firstIteration = false;
            else
                if ( nextChar != ',' )
                    throw new InvalidPolicyTextException( "Was expecting comma but found something else!" );
                else
                    newIndex = skipWhiteSpaces( newIndex ) + 1;

            final int[] paramNameIndices = peekTokenAhead( newIndex );
            if ( paramNameIndices == null )
                throw new InvalidPolicyTextException( "Incomplete grant entry!" );
            final String loweredParamName = policyText.substring( paramNameIndices[ 0 ], paramNameIndices[ 1 ] ).toLowerCase();

            if ( loweredParamName.equals( "signedby" ) ) {
                final int[] paramValueIndices = peekQuotedStringAhead( paramNameIndices[ 1 ] );
                if ( paramValueIndices == null )
                    throw new InvalidPolicyTextException( "Incomplete keystore entry, found no quoted string for signedBy!" );
                if ( grantEntry.getSignedBy() != null )
                    throw new InvalidPolicyTextException( "Invalid keystore entry, only 1 signedBy allowed!" );
                if ( grantEntry.getCodeBase() != null || !grantEntry.getPrincipalList().isEmpty() )
                    throw new InvalidPolicyTextException( "Invalid keystore entry, signedBy cannot be after codeBase or principals!" );
                grantEntry.setSignedBy( policyText.substring( paramValueIndices[ 0 ] + 1, paramValueIndices[ 1 ] - 1 ) );
                newIndex = paramValueIndices[ 1 ];
            }
            else if ( loweredParamName.equals( "codebase" ) ) {
                final int[] firstLastParamValueIndices = peekQuotedStringAhead( paramNameIndices[ 1 ] );
                if ( firstLastParamValueIndices == null )
                    throw new InvalidPolicyTextException( "Incomplete keystore entry, found no quoted string for codeBase!" );
                if ( grantEntry.getCodeBase() != null )
                    throw new InvalidPolicyTextException( "Invalid keystore entry, only 1 codeBase allowed!" );
                if ( !grantEntry.getPrincipalList().isEmpty() )
                    throw new InvalidPolicyTextException( "Invalid keystore entry, signedBy cannot be after principals!" );
                grantEntry.setCodeBase( policyText.substring( firstLastParamValueIndices[ 0 ] + 1, firstLastParamValueIndices[ 1 ] - 1 ) );
                newIndex = firstLastParamValueIndices[ 1 ];
            }
            else if ( loweredParamName.equals( "principal" ) ) {
                final int[] classNameIndices = peekTokenAhead( paramNameIndices[ 1 ] );
                if ( classNameIndices == null || policyTextChars[ classNameIndices[ 0 ] ] == '{' )
                    throw new InvalidPolicyTextException( "Invalid grant entry, found no class name for principal!" );

                final int[] principalNameIndices = peekQuotedStringAhead( classNameIndices[ 1 ] );
                if ( principalNameIndices == null )
                    throw new InvalidPolicyTextException( "Invalid grant entry, found no name for principal!" );

                final Principal principal = new Principal();
                principal.setType( policyText.substring( classNameIndices[ 0 ], classNameIndices[ 1 ] ) );
                principal.setName( policyText.substring( principalNameIndices[ 0 ] + 1, principalNameIndices[ 1 ] - 1 ) );
                grantEntry.getPrincipalList().add( principal );

                newIndex = principalNameIndices[ 1 ];
            }
        }

        newIndex = policyText.indexOf( '{', newIndex ) + 1;

        // Now permissions what we have left
        while ( ( nextChar = peekNextNonWhiteSpaceChar( newIndex ) ) != '}' ) {

            final int[] keywordIndices = peekTokenAhead( newIndex );
            if ( keywordIndices == null )
                throw new InvalidPolicyTextException( "Incomplete grant entry!" );
            final String loweredKeyword = policyText.substring( keywordIndices[ 0 ], keywordIndices[ 1 ] ).toLowerCase();

            if ( !loweredKeyword.equals( "permission" ) )
                throw new InvalidPolicyTextException( "Incomplete grant entry, was expecting permission but found something else!" );

            final int[] classNameIndices = peekTokenAhead( keywordIndices[ 1 ] );
            if ( classNameIndices == null || policyTextChars[ classNameIndices[ 0 ] ] == '}' )
                throw new InvalidPolicyTextException( "Invalid grant entry, found no class name for permission!" );
            if ( classNameIndices[ 1 ] - classNameIndices[ 0 ] == 0 ) {
                if ( policyTextChars[ classNameIndices[ 0 ] ] == ',' || policyTextChars[ classNameIndices[ 0 ] ] == PolicyEntry.TERMINATOR_CHAR ) // Class name is only a comma or a semicolon
                    throw new InvalidPolicyTextException( "Invalid grant entry, found no class name for permission!" );
            }
            else {
                if ( policyTextChars[ classNameIndices[ 1 ] - 1 ] == ',' || policyTextChars[ classNameIndices[ 1 ] - 1  ] == PolicyEntry.TERMINATOR_CHAR ) 
                    classNameIndices[ 1 ]--; // Class name is more than 1 character long and comma or semicolon is at its end; we step back from it
            }

            int[] targetNameIndices = null;
            int[] actionsIndices    = null;
            int[] signedByIndices   = null;

            nextChar = peekNextNonWhiteSpaceChar( classNameIndices[ 1 ] );
            if ( nextChar != PolicyEntry.TERMINATOR_CHAR ) {
                if ( nextChar != ',' ) {
                    // There must be a target name here!
                    targetNameIndices = peekQuotedStringAhead( classNameIndices[ 1 ] );
                    if ( targetNameIndices == null )
                        throw new InvalidPolicyTextException( "Invalid grant entry, found no name for permission!" );
                }

                newIndex = targetNameIndices == null ? classNameIndices[ 1 ] : targetNameIndices[ 1 ];
                nextChar = peekNextNonWhiteSpaceChar( newIndex );
                if ( nextChar != PolicyEntry.TERMINATOR_CHAR ) {
                    if ( nextChar != ',' )
                        throw new InvalidPolicyTextException( "Was expecting semicolon but found something else!" );

                    newIndex = skipWhiteSpaces( newIndex );
                    nextChar = peekNextNonWhiteSpaceChar( newIndex + 1 );
                    if ( nextChar == '"' ) { // actions are specified
                        actionsIndices = peekQuotedStringAhead( newIndex + 1 );
                        if ( actionsIndices == null )
                            throw new InvalidPolicyTextException( "Incomplete keystore entry, found no quoted string for permission actions!" );
                        newIndex = actionsIndices[ 1 ];
                    }

                    nextChar = peekNextNonWhiteSpaceChar( newIndex );
                    if ( nextChar != PolicyEntry.TERMINATOR_CHAR ) {
                        if ( nextChar != ',' )
                            throw new InvalidPolicyTextException( "Was expecting semicolon but found something else!" );
                        final int[] signedByKeywordIndices = peekTokenAhead( skipWhiteSpaces( newIndex ) + 1 );
                        final String loweredSignedByKeyword = policyText.substring( signedByKeywordIndices[ 0 ], signedByKeywordIndices[ 1 ] ).toLowerCase();

                        if ( !loweredSignedByKeyword.equals( "signedby" ) )
                            throw new InvalidPolicyTextException( "Incomplete grant entry, was expecting signedBy but found something else!" );

                        signedByIndices = peekQuotedStringAhead( signedByKeywordIndices[ 1 ] );
                        if ( signedByIndices == null )
                            throw new InvalidPolicyTextException( "Incomplete keystore entry, found no quoted string for permission signedBy!" );

                        if ( peekNextNonWhiteSpaceChar( signedByIndices[ 1 ] ) != PolicyEntry.TERMINATOR_CHAR )
                            throw new InvalidPolicyTextException( "Was expecting semicolon but found something else!" );
                        else
                            newIndex = skipWhiteSpaces( signedByIndices[ 1 ] ) + 1;
                    }
                    else
                        newIndex = skipWhiteSpaces( newIndex ) + 1;
                }
                else
                    newIndex = skipWhiteSpaces( newIndex ) + 1;
            }
            else
                newIndex = skipWhiteSpaces( classNameIndices[ 1 ] ) + 1;

            final Permission permission = new Permission();
            permission.setClassName( policyText.substring( classNameIndices[ 0 ], classNameIndices[ 1 ] ) );
            if ( targetNameIndices != null )
                permission.setTargetName( policyText.substring( targetNameIndices[ 0 ] + 1, targetNameIndices[ 1 ] - 1 ) );
            if ( actionsIndices != null )
                permission.setActions( policyText.substring( actionsIndices[ 0 ] + 1, actionsIndices[ 1 ] - 1 ) );
            if ( signedByIndices != null )
                permission.setSignedBy( policyText.substring( signedByIndices[ 0 ] + 1, signedByIndices[ 1 ] - 1 ) );
            grantEntry.getPermissionList().add( permission );
        }

        newIndex = skipWhiteSpaces( newIndex ) + 1; // index of '}' + 1 (comments skipped)
        if ( peekNextNonWhiteSpaceChar( newIndex ) != PolicyEntry.TERMINATOR_CHAR )
            throw new InvalidPolicyTextException( "Was expecting semicolon but found something else!" );
        newIndex = skipWhiteSpaces( newIndex )  + 1;

        policyEntryList.add( grantEntry );
    }

    /**
     * Skips the following white spaces starting from the specified index,
     * and returns the next non-white space index or -1 if end of string reached.
     * Comments are skipped over if found.
     *
     * @param index index to start from
     * @return the next non-white space index or -1 if end of string reached
     */
    private int skipWhiteSpaces( int index ) {
        try {
            boolean foundComment;
            do {
                foundComment = false;
                while ( WHITE_SPACES.indexOf( policyTextChars[ index ] ) >= 0 )
                    index++;

                if ( policyTextChars[ index ] == '/' && index < policyTextChars.length - 1 ) { 
                    if ( policyTextChars[ index + 1 ] == '/' ) {
                        foundComment = true;
                        index = policyText.indexOf( '\n', index + 2 ) + 1;
                    }
                    else if ( policyTextChars[ index + 1 ] == '*' ) {
                        foundComment = true;
                        index = policyText.indexOf( "*/", index + 2 ) + 2;
                    }
                }
            } while ( foundComment ); // If foundComment, we have to recheck, because another comment might follow the the one we just found

            return index;
        } catch ( final ArrayIndexOutOfBoundsException aioobe ) {
            return -1;
        }
    }

    /**
     * Returns the first non-whitespace character starting from a given index or -1 if all the remaining characters are white spaces.
     * Comments are skipped over if found.
     * @param index index to start from
     * @return the first non-whitespace character starting from a given index or -1 if all the remaining characters are white spaces
     */
    private char peekNextNonWhiteSpaceChar( int index ) {
        index = skipWhiteSpaces( index );
        if ( index < 0 )
            return (char) -1;
        else
            return policyTextChars[ index ];
    }

    /**
     * Returns the first (inclusive) and last index (exclusive) of the next token or null if no more tokens.
     * Comments are skipped over if found.
     * @param index index to start from
     * @return the first (inclusive) and last index (exclusive) of the next token or null if no more tokens
     */
    private int[] peekTokenAhead ( int index ) {
        index = skipWhiteSpaces( index );
        if ( index < 0 )
            return null;

        final int firstIndex = index;

        while ( index++ < policyTextChars.length )
            if ( index == policyTextChars.length || WHITE_SPACES.indexOf( policyTextChars[ index ] ) >= 0  )
                break;

        return new int[] { firstIndex, index };
    }

    /**
     * Returns the first (inclusive) and last index (exclusive) of the next quoted string or null if found something else.<br>
     * The string denoted by the returned indices includes the quotes in the beginning and in the end of the quoted string.
     * Comments are skipped over if found.
     *
     * @param index index to start from
     * @return the first (inclusive) and last index (exclusive) of the next quoted string or null if found something else
     * @throws InvalidPolicyTextException thrown if no quoted string found
     */
    private int[] peekQuotedStringAhead ( int index ) throws InvalidPolicyTextException {
        try {
            index = skipWhiteSpaces( index );
            if ( index < 0 )
                return null;

            if ( policyTextChars[ index ] != '"' )
                throw new InvalidPolicyTextException( "Could not find expected quoted string (missing opener quotation mark)!" );

            final int firstIndex = index;

            while ( ++index <= policyTextChars.length ) {
                if ( index == policyTextChars.length )
                    throw new InvalidPolicyTextException( "Could not find expected quoted string (missing closer quotation mark)!" );

                if ( policyTextChars[ index ] == '"' )
                    break;
            }

            return new int[] { firstIndex, index+1 }; // +1 because end index must be exclusive

        } catch ( final ArrayIndexOutOfBoundsException aioobe ) {
            return null;
        }
    }

    /**
     * Skips the following white spaces starting from the specified index,
     * and returns the next non-white space index or -1 if end of string reached.
     * Comments are not skipped over.
     *
     * @param index index to start from
     * @return the next non-white space index or -1 if end of string reached
     */
    private int skipWhiteSpacesCommentsIncluded( int index ) {
        try {
            while ( WHITE_SPACES.indexOf( policyTextChars[ index ] ) >= 0 )
                index++;

            return index;
        } catch ( final ArrayIndexOutOfBoundsException aioobe ) {
            return -1;
        }
    }

    /**
     * Returns the first (inclusive) and last index (exclusive) of the next token or null if no more tokens.
     * Comments are not skipped over.
     * @param index index to start from
     * @return the first (inclusive) and last index (exclusive) of the next token or null if no more tokens
     */
    private int[] peekTokenAheadCommentsIncluded( int index ) {
        index = skipWhiteSpacesCommentsIncluded( index );
        if ( index < 0 )
            return null;

        final int firstIndex = index;

        while ( index++ < policyTextChars.length )
            if ( index == policyTextChars.length || WHITE_SPACES.indexOf( policyTextChars[ index ] ) >= 0  )
                break;

        return new int[] { firstIndex, index };
    }

}
