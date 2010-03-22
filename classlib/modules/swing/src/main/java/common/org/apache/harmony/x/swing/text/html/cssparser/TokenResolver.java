/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Anton Avtamonov, Alexey A. Ivanov
 */
package org.apache.harmony.x.swing.text.html.cssparser;

public class TokenResolver {
    private static final String SEPARATOR = " ";

    public static String resolve(final Token token) {
        return token.image;
    }

    public static String resolve(final Token start, final Token end, final boolean allowSeparation) {
        StringBuilder result = new StringBuilder();
        Token nextToken = start;
        do {
            if (allowSeparation && result.length() != 0) {
                result.append(SEPARATOR);
            }
            result.append(resolve(nextToken));
            if (nextToken == end) {
                break;
            }
            nextToken = nextToken.next;
        } while (nextToken != null);

        return result.toString();
    }

}
