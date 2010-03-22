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
 * Represents the keystore password URL entry of the policy text.<br>
 * Just as keystore entry there may be several keystore password URL entries in a policy text,
 * but only the first one is interpreted and used, the rest are ignored.<br>
 * The entry specifies the location of the keystore password.<br>
 * The entry should be put next the the keystore entry.
 * @see KeystoreEntry
 */
public class KeystorePasswordURLEntry extends PolicyEntry {

    /** Keyword of the keystore password URL entry in the policy text.  */
    public static final String KEYWORD         = "keystorePasswordURL";
    /** Stored value of the lowercased keyword for fast policy parsing. */
    public static final String LOWERED_KEYWORD = KEYWORD.toLowerCase();

    /** URL of the keystore password. */
    private String url;

    /**
     * Returns the keystore password url.
     * @return the keystore password url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the keystore password URL
     * @param url the keystore password URL to be set
     */
    public void setUrl( final String url ) {
        this.url = url;
    }

    @Override
    public String getText() {
        return KEYWORD + " \"" + url + '\"' + TERMINATOR_CHAR;
    }

}
