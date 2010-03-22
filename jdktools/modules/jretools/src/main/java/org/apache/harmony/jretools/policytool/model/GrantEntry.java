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

package org.apache.harmony.jretools.policytool.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.jretools.policytool.Consts;

/**
 * Represents an entry which specifies some grant permission.
 */
public class GrantEntry extends PolicyEntry {

    /** Keyword of the keystore entry in the policy text.               */
    public static final String KEYWORD         = "grant";
    /** Stored value of the lowercased keyword for fast policy parsing. */
    public static final String LOWERED_KEYWORD = KEYWORD.toLowerCase();

    /** Code base of the grant entry.                     */
    private String codeBase;
    /** Signed by alias (from the keystore).              */
    private String signedBy;

    /** List of principals of the entry.  */
    private List< Principal  > principalList  = new ArrayList< Principal  >();
    /** List of permissions of the entry. */
    private List< Permission > permissionList = new ArrayList< Permission >();

    /**
     * Returns the code base of the entry.
     * @return the code base of the entry
     */
    public String getCodeBase() {
        return codeBase;
    }

    /**
     * Sets the code base of the entry.
     * @param codeBase the code base of the entry
     */
    public void setCodeBase( final String codeBase ) {
        this.codeBase = codeBase;
    }

    /**
     * Returns the signed by alias of the entry.
     * @return the signed by alias of the entry
     */
    public String getSignedBy() {
        return signedBy;
    }

    /**
     * Returns the signed by alias of the entry.
     * @param signedBy the signed by alias of the entry to be set
     */
    public void setSignedBy( final String signedBy ) {
        this.signedBy = signedBy;
    }

    /**
     * Returns the list of principals of the entry.
     * @return the list of principals of the entry
     */
    public List< Principal > getPrincipalList() {
        return principalList;
    }

    /**
     * Sets the list of principals of the entry.
     * @param principalList list of principals of the entry to be set
     */
    public void setPrincipalList( final List< Principal > principalList ) {
        this.principalList = principalList;
    }

    /**
     * Returns the list of permissions of the entry.
     * @return the list of permissions of the entry
     */
    public List< Permission > getPermissionList() {
        return permissionList;
    }

    /**
     * Sets the list of permissions of the entry.
     * @param permissionList list of permissions of the entry to be set
     */
    public void setPermissionList( final List< Permission > permissionList ) {
        this.permissionList = permissionList;
    }

    @Override
    public String getText() {
        final StringBuilder textBuilder = new StringBuilder( KEYWORD );

        if ( signedBy != null && signedBy.length() > 0 )
            textBuilder.append( " signedBy \"" ).append( signedBy ).append( '"' );

        if ( codeBase != null && codeBase.length() > 0 ) {
            if ( signedBy != null && signedBy.length() > 0 )
                textBuilder.append( ',' );
            textBuilder.append( " codeBase \"" ).append( codeBase ).append( '"' );
        }

        if ( principalList.isEmpty() )
            textBuilder.append( ' ' );
        else {
            if ( signedBy != null && signedBy.length() > 0
              || codeBase != null && codeBase.length() > 0 )
                textBuilder.append( ',' );
            textBuilder.append( Consts.NEW_LINE_STRING );

            for ( final Iterator< Principal > principalIterator = principalList.iterator(); principalIterator.hasNext(); ) {
                textBuilder.append( "      " ).append( principalIterator.next().toString() );
                if ( principalIterator.hasNext() )
                    textBuilder.append( ',' ).append( Consts.NEW_LINE_STRING );
                else
                    textBuilder.append( ' ' );
            }
        }

        textBuilder.append( '{' ).append( Consts.NEW_LINE_STRING );

        for ( final Permission permission : permissionList )
            textBuilder.append( "\tpermission " ).append( permission.toString( Consts.NEW_LINE_STRING + "\t\t" ) ).append( TERMINATOR_CHAR ).append( Consts.NEW_LINE_STRING );

        textBuilder.append( '}' ).append( TERMINATOR_CHAR );

        return textBuilder.toString();
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append( " CodeBase " );
        if ( codeBase != null && codeBase.length() > 0 )
            stringBuilder.append( '"' ).append( codeBase ).append( '"' );
        else
            stringBuilder.append( "<ALL>" );

        if ( signedBy != null && signedBy.length() > 0 )
            stringBuilder.append( stringBuilder.length() > 0 ? ", " : "" ).append( "SignedBy \"" ).append( signedBy ).append( '"' );

        for ( final Principal principal : principalList )
            stringBuilder.append( stringBuilder.length() > 0 ? ", " : "" ).append( principal );

        return stringBuilder.toString();
    }

}
