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

package org.apache.harmony.jndi.internal;

/**
 * Identifies the simplest URL syntax:
 * <code>{scheme}:{scheme specific part}</code>.
 */
public class UrlParser {

    /*
     * Prevent instantiate.
     */
    private UrlParser() {
        super();
    }

    /**
     * Returns an URL's scheme part, in lower case. If the url is not a valid
     * URL, null is returned.
     * 
     * @param url
     *            a url string
     * @return the URL's scheme part, in lower case. If the url is not a valid
     *         URL, null is returned.
     */
    public static String getScheme(String url) {
        if (null == url) {
            return null;
        }
        int colPos = url.indexOf(':');
        if (colPos < 0) {
            return null;
        }
        String scheme = url.substring(0, colPos);
        char c;
        boolean inCharSet;
        for (int i = 0; i < scheme.length(); i++) {
            c = scheme.charAt(i);
            inCharSet = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '+' || c == '.'
                    || c == '-' || c == '_';
            if (!inCharSet) {
                return null;
            }
        }
        return scheme;
    }

}
