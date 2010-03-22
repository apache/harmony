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

/**
 * Represents a principal for the grant entries.
 */
public class Permission implements Cloneable {

    /** Name of the class of the permission.                   */
    private String className;
    /** Name of target of the permission.                      */
    private String targetName;
    /** Actions of the permission.                             */
    private String actions;
    /** Signed by alias of the permission (from the keystore). */
    private String signedBy;

    /**
     * Returns the name of the class of the permission.
     * @return the name of the class of the permission
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the name of the class of the permission.
     * @param className the name of the class of the permission to be set
     */
    public void setClassName( final String className ) {
        this.className = className;
    }

    /**
     * Returns the name of target of the permission.
     * @return the name of target of the permission
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * Sets the name of target of the permission
     * @param targetName the name of target of the permission to be set
     */
    public void setTargetName( final String targetName ) {
        this.targetName = targetName;
    }

    /**
     * Returns the actions of the permission.
     * @return the actions of the permission
     */
    public String getActions() {
        return actions;
    }

    /**
     * Sets the actions of the permission
     * @param actions actions of the permission to be set
     */
    public void setActions( final String actions ) {
        this.actions = actions;
    }

    /**
     * Returns the signed by alias of the permission.
     * @return the signed by alias of the permission
     */
    public String getSignedBy() {
        return signedBy;
    }

    /**
     * Returns the signed by alias of the permission.
     * @param signedBy the signed by alias of the permission to be set
     */
    public void setSignedBy( final String signedBy ) {
        this.signedBy = signedBy;
    }

    @Override
    public String toString() {
        return toString( "" );
    }

    /**
     * Returns a string representation of the permission.
     * @param signedByPartPrefix string to be put before the signedBy part; omitted if signedBy is missing
     * @return a string representation of the permission
     */
    public String toString( final String signedByPartPrefix ) {
        final StringBuilder stringBuilder = new StringBuilder( className );

        if ( targetName != null && targetName.length() > 0 )
            stringBuilder.append( " \"" ).append( targetName ).append( '"' );
        if ( actions != null && actions.length() > 0 )
            stringBuilder.append( ", \"" ).append( actions ).append( '"');
        if ( signedBy != null && signedBy.length() > 0 )
            stringBuilder.append( ", " ).append( signedByPartPrefix ).append( "signedBy \"" ).append( signedBy ).append( '"' );

        return stringBuilder.toString();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch ( final CloneNotSupportedException cnse ) {
            // This' never gonna happen.
            cnse.printStackTrace();
            return null;
        }
    }

}
