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
package org.apache.harmony.jndi.provider.ldap.parser;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.naming.ConfigurationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.harmony.jndi.internal.nls.Messages;

public class SchemaParser {

    public final static String SPACE = " "; //$NON-NLS-1$

    public final static String LEFT_PARENTHESIS = "("; //$NON-NLS-1$

    public final static String RIGHT_PARENTHESIS = ")"; //$NON-NLS-1$

    public final static String SINGLE_QUOTE = "'"; //$NON-NLS-1$

    public final static String DOLLAR = "$"; //$NON-NLS-1$

    public final static String NAME = "NAME"; //$NON-NLS-1$

    public final static String ORIG = "orig"; //$NON-NLS-1$

    public final static String NUMERICOID = "NUMERICOID"; //$NON-NLS-1$

    public final static String MUST = "MUST"; //$NON-NLS-1$

    public final static String MAY = "MAY"; //$NON-NLS-1$

    public final static String SUP = "SUP"; //$NON-NLS-1$

    public final static String ABSTRACT = "ABSTRACT"; //$NON-NLS-1$

    public final static String STRUCTURAL = "STRUCTURAL"; //$NON-NLS-1$

    public final static String AUXILIARY = "AUXILIARY"; //$NON-NLS-1$

    public final static String SINGLE_VALUE = "SINGLE-VALUE"; //$NON-NLS-1$

    public final static String NO_USER_MODIFICATION = "NO-USER-MODIFICATION"; //$NON-NLS-1$

    public final static String X_PREFIX = "X-"; //$NON-NLS-1$

    public final static String DESC = "DESC"; //$NON-NLS-1$

    public final static String USAGE = "USAGE"; //$NON-NLS-1$

    public final static String EQUALITY = "EQUALITY"; //$NON-NLS-1$

    public final static String SYNTAX = "SYNTAX"; //$NON-NLS-1$

    public final static String SUBSTR = "SUBSTR"; //$NON-NLS-1$

    public final static String ORDERING = "ORDERING"; //$NON-NLS-1$

    public static String getName(String schemaLine) {
        StringTokenizer st = new StringTokenizer(schemaLine);
        st.nextToken();

        String name = st.nextToken();
        if (st.hasMoreTokens()) {
            if (st.nextToken().equalsIgnoreCase(NAME)) {
                name = st.nextToken();

                if (name.equals(LEFT_PARENTHESIS)) {
                    name = st.nextToken();
                }
                name = name.substring(1, name.length() - 1);
            }
        }

        return name;
    }

    public static Hashtable<String, Object> parseValue(String schemaLine) {
        StringTokenizer st = new StringTokenizer(schemaLine);
        // Skip (
        st.nextToken();

        String oid = st.nextToken();

        Hashtable<String, Object> schemaDef = new Hashtable<String, Object>();
        schemaDef.put(ORIG, schemaLine);
        schemaDef.put(NUMERICOID, oid);
        String token = null;
        ArrayList<String> values = null;
        StringBuilder desc = new StringBuilder();
        while (st.hasMoreTokens()) {
            
            String attrName = st.nextToken().toUpperCase();
            if (attrName.startsWith(X_PREFIX)) {
            	token = st.nextToken();
                // remove the ending ' symbol
                token = token.substring(1, token.length() - 1);
                schemaDef.put(attrName, token);
            } else if (attrName.equals(USAGE) || attrName.equals(EQUALITY)
                    || attrName.equals(SYNTAX) || attrName.equals(ORDERING)
                    || attrName.equals(SUBSTR)) {
                token = st.nextToken();
                schemaDef.put(attrName, token);
            } else if (attrName.equals(DESC)) {
                token = st.nextToken();

                // remove the leading ' symbol
                if (token.startsWith(SINGLE_QUOTE)) {
                    token = token.substring(1);
                }
                while (token != null && !token.endsWith(SINGLE_QUOTE)) {
                    desc.append(token).append(SPACE);
                    token = st.nextToken();
                }

                if (token != null) {
                    // remove the ending ' symbol
                    desc.append(token.substring(0, token.length() - 1));
                    schemaDef.put(attrName, desc.toString());
                    desc.delete(0, desc.length());
                }
            } else if (attrName.equals(NAME)) {
                token = st.nextToken();
                values = new ArrayList<String>();
                // Name has multiple values
                if (token.startsWith(LEFT_PARENTHESIS)) {
                    token = st.nextToken();
                    while (!token.equals(RIGHT_PARENTHESIS)) {
                        // remove the leading ' symbol
                        if (token.startsWith(SINGLE_QUOTE)) {
                            token = token.substring(1);
                        }
                        while (!token.endsWith(SINGLE_QUOTE)) {
                            desc.append(token).append(SPACE);
                            token = st.nextToken();
                        }

                        // remove the ending ' symbol
                        desc.append(token.substring(0, token.length() - 1));
                        values.add(desc.toString());
                        desc.delete(0, desc.length());

                        token = st.nextToken();
                    }
                } else {
                    // remove the leading ' symbol
                    if (token.startsWith(SINGLE_QUOTE)) {
                        token = token.substring(1);
                    }
                    while (token != null && !token.endsWith(SINGLE_QUOTE)) {
                        desc.append(token).append(SPACE);
                        token = st.nextToken();
                    }

                    if (token != null) {// remove the ending ' symbol
                        desc.append(token.substring(0, token.length() - 1));
                        values.add(desc.toString());
                        desc.delete(0, desc.length());
                    }
                }
                schemaDef.put(attrName, values);
            } else if (attrName.equals(MUST) || attrName.equals(SUP)
                    || attrName.equals(MAY)) {
                token = st.nextToken();
                values = new ArrayList<String>();
                // has multiple values
                if (token.startsWith(LEFT_PARENTHESIS)) {
                    token = st.nextToken();
                    while (!token.equals(RIGHT_PARENTHESIS)) {
                        if (!token.equals(DOLLAR))
                            values.add(token);
                        token = st.nextToken();
                    }
                } else {
                    values.add(token);
                }
                schemaDef.put(attrName, values);
            } else if (attrName.equals(ABSTRACT) || attrName.equals(STRUCTURAL)
                    || attrName.equals(AUXILIARY)
                    || attrName.equals(SINGLE_VALUE)
                    || attrName.equals(NO_USER_MODIFICATION)) {
                schemaDef.put(attrName, "true"); //$NON-NLS-1$
            }
        }
        return schemaDef;
    }

    /*
     * Format Attributes object to a string representation which can be
     * understanded by server.
     */
    public static String format(Attributes attributes) throws NamingException {
        StringBuilder builder = new StringBuilder();
        builder.append(LEFT_PARENTHESIS);
        builder.append(SPACE);

        Attribute attribute = attributes.get(NUMERICOID);
        // The NUMERICOID must be presented. Throw exception if not.
        if (attribute == null) {
            // ldap.36=Must have numeric OID
            throw new ConfigurationException(Messages.getString("ldap.36")); //$NON-NLS-1$
        }
        builder.append(attribute.get());
        builder.append(SPACE);

        NamingEnumeration<String> ids = attributes.getIDs();

        /*
         * Iterate each attribute, and append the correspoind string
         * representation for each one.
         */
        while (ids.hasMoreElements()) {
            String id = ids.nextElement();
            attribute = attributes.get(id);
            // id is never null, so id.equals("something") can be used.
            if (id.equals(MUST) || id.equals(MAY) || id.equals(SUP)) {
                // Same kinds of attributes may have more than one value.
                if (attribute.size() == 1) {
                    builder.append(id);
                    builder.append(SPACE);
                    builder.append(attribute.get());
                    builder.append(SPACE);
                } else {
                    builder.append(id);
                    builder.append(SPACE);
                    builder.append(LEFT_PARENTHESIS);
                    builder.append(SPACE);
                    builder.append(attribute.get(0));
                    builder.append(SPACE);
                    for (int i = 1; i < attribute.size(); i++) {
                        builder.append(DOLLAR);
                        builder.append(SPACE);
                        builder.append(attribute.get(i));
                        builder.append(SPACE);
                    }
                    builder.append(RIGHT_PARENTHESIS);
                    builder.append(SPACE);
                }
            } else if (id.equals(ABSTRACT) || id.equals(STRUCTURAL)
                    || id.equals(AUXILIARY) || id.equals(SINGLE_VALUE)
                    || id.equals(NO_USER_MODIFICATION)) {
                builder.append(id);
                builder.append(SPACE);
            } else if (!(id.equalsIgnoreCase(NUMERICOID))) {
                builder.append(id);
                builder.append(SPACE);
                builder.append(SINGLE_QUOTE);
                builder.append(attribute.get());
                builder.append(SINGLE_QUOTE);
                builder.append(SPACE);
            }
        }
        builder.append(RIGHT_PARENTHESIS);

        return builder.toString();
    }
}
