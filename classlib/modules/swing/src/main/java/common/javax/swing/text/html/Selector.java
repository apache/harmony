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

final class Selector {
    final SimpleSelector[] simpleSelectors;
    final Specificity specificity;

    Selector(final String complexSelector) {
        String[] selectors = complexSelector.split("\\s+");
        simpleSelectors = new SimpleSelector[selectors.length];
        for (int i = 0; i < selectors.length; i++) {
            simpleSelectors[i] = new SimpleSelector(selectors[i]);
        }
        specificity = new Specificity(this);
    }

    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(simpleSelectors[0]);
        for (int i = 1; i < simpleSelectors.length; i++) {
            result.append(' ').append(simpleSelectors[i]);
        }
        return result.toString();
    }

    boolean applies(final Selector styleSelector) {
        int ssIndex = styleSelector.simpleSelectors.length - 1;
        int index = simpleSelectors.length - 1;

        boolean result = false;
        for (; ssIndex >= 0; ssIndex--) {
            SimpleSelector simple = styleSelector.simpleSelectors[ssIndex];
            result = false;
            for (; index >= 0 && !result; index--) {
                SimpleSelector ss = simpleSelectors[index];
                if (simple.matches(ss.tag, ss.id, ss.clazz)) {
                    result = true;
                }
            }
        }

        return result;
    }

    SimpleSelector getLastSelector() {
        return simpleSelectors[simpleSelectors.length - 1];
    }
}
