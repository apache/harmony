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
 * Represents the keystore entry of the policy text.<br>
 * There may be several keystore entries in a policy text, but only the first one is interpreted and used, the rest are ignored.<br>
 * The keystore entry must be provided if:
 * <ul>
 *     <li>there are grant entries which specify signer aliases
 *     <li>or there are grant entries which specify principal aliases
 * </ul>
 * @see KeystorePasswordURLEntry
 */
public class KeystoreEntry extends PolicyEntry {

    /** Keyword of the keystore entry in the policy text.               */
    public static final String KEYWORD         = "keystore";
    /** Stored value of the lowercased keyword for fast policy parsing. */
    public static final String LOWERED_KEYWORD = KEYWORD.toLowerCase();

    /** URL of the keystore.      */
    private String url;
    /** Type of the keystore.     */
    private String type;
    /** Provider of the keystore. */
    private String provider;

    /**
     * Returns the keystore url.
     * @return the keystore url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the keystore url.
     * @param url keystore url to be set
     */
    public void setUrl( final String url ) {
        this.url = url;
    }

    /**
     * Returns the keystore type.
     * @return the keystore type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the keystore type.
     * @param type keystore type to be set
     */
    public void setType( final String type ) {
        this.type = type;
    }

    /**
     * Returns the keystore provider.
     * @return the keystore provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * sets the keystore provider.
     * @param provider keystore provider to be set
     */
    public void setProvider( final String provider ) {
        this.provider = provider;
    }

    @Override
    public String getText() {
        final StringBuilder textBuilder = new StringBuilder( KEYWORD );

        if ( url != null && url.length() > 0 ) {
            textBuilder.append( " \"" ).append( url ).append( '"' );

            if ( type != null && type.length() > 0 ) {
                textBuilder.append( ", \"" ).append( type ).append( '"' );

                if ( provider != null && provider.length() > 0 )
                    textBuilder.append( ", \"" ).append( provider ).append( '"' );
            }
        }

        textBuilder.append( TERMINATOR_CHAR );

        return textBuilder.toString();
    }

}
