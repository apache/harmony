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
package org.apache.harmony.x.swing.text.html.cssparser.metamodel;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

public final class RuleSet {
    private final List selectors = new LinkedList();
    private final List properties = new LinkedList();

    public void addSelector(final String selector) {
        if (selector == null) {
            return;
        }
        selectors.add(selector);
    }

    public void addProperty(final Property p) {
        if (p == null) {
            return;
        }
        properties.add(p);
    }

    public Iterator getSelectors() {
        return selectors.iterator();
    }

    public Iterator getProperties() {
        return properties.iterator();
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("\nselectors:")
              .append(selectors)
              .append('\n')
              .append("properties:")
              .append(properties)
              .append('\n');

        return result.toString();
    }
}
