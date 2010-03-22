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
 * @author Alexey A. Ivanov
 */
package javax.swing.text.html;

final class SimpleSelector {
    final String tag;
    final String id;
    final String clazz;
    final String pseudo;

    SimpleSelector(final String selector) {
        tag    = getTagName(selector);
        id     = getID(selector);
        clazz  = getClass(selector);
        pseudo = getPseudo(selector);
    }

    public String toString() {
        final StringBuilder result = new StringBuilder();
        if (tag != null) {
            result.append(tag);
        }
        if (id != null) {
            result.append('#').append(id);
        }
        if (clazz != null) {
            result.append('.').append(clazz);
        }
        if (pseudo != null) {
            result.append(':').append(pseudo);
        }
        return result.toString();
    }

    static String getTagName(final String selector) {
        String tag = selector.split("#|\\.|:")[0];
        return tag.length() > 0 ? tag.toLowerCase() : null;
    }

    static String getID(final String selector) {
        int hashIndex = selector.indexOf('#');
        if (hashIndex < 0) {
            return null;
        }

        return selector.substring(hashIndex + 1).split("\\.|:")[0];
    }

    static String getClass(final String selector) {
        int dotIndex = selector.indexOf('.');
        if (dotIndex < 0) {
            return null;
        }
        return selector.substring(dotIndex + 1).split(":")[0];
    }

    static String getPseudo(final String selector) {
        int colonIndex = selector.indexOf(':');
        if (colonIndex < 0) {
            return null;
        }
        return selector.substring(colonIndex + 1)/*.split(":")[0]*/;
    }

    public boolean matches(final SimpleSelector another) {
        return matches(another.tag, another.id, another.clazz);
    }

    public boolean matches(final String eName, final String eID, final String eClass) {
        if (id != null) {
            if (eID == null) {
                return false;
            }
            if (!id.equals(eID)) {
                return false;
            }
        }

        if (clazz != null) {
            if (eClass == null) {
                return false;
            }
            if (!clazz.equals(eClass)) {
                return false;
            }
        }

        if (tag != null) {
            if (eName == null) {
                return false;
            }
            if (!tag.equals(eName)) {
                return false;
            }
        }

        return true;
    }

    public boolean applies(final SimpleSelector another) {
        return applies(another.tag, another.id, another.clazz);
    }

    public boolean applies(final String eName,
                           final String eID,
                           final String eClass) {
        if (id != null && eID != null && !id.equals(eID)) {
            return false;
        }

        if (clazz != null && eClass != null && !clazz.equals(eClass)) {
            return false;
        }

        if (tag != null && eName != null && !tag.equals(eName)) {
            return false;
        }

        return true;
    }
}
