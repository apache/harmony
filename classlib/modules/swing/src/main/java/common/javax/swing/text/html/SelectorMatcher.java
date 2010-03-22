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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;

class SelectorMatcher {
    static List findMatching(final Enumeration ruleNames,
                             final Element element) {
        List matched = new ArrayList();

        SimpleSelector ruleLast;

        while (ruleNames.hasMoreElements()) {
            final String ruleName = (String)ruleNames.nextElement();
            Selector rule = new Selector(ruleName);

            int rIndex = rule.simpleSelectors.length - 1;
            ruleLast = rule.simpleSelectors[rIndex];
            if (ruleLast.matches(getTagName(element),
                                 getID(element),
                                 getClass(element))) {
                boolean result = true;
                Element current = element;
                for (rIndex--; rIndex >= 0 && result && current != null;
                     rIndex--) {

                    ruleLast = rule.simpleSelectors[rIndex];
                    current = current.getParentElement();
                    while (current != null) {
                        if (ruleLast.matches(getTagName(current),
                                             getID(current),
                                             getClass(current))) {
                            break;
                        }
                        current = current.getParentElement();
                    }
                    result = current != null;
                }

                if (result) {
                    matched.add(rule);
                }
            }
        }
        Collections.sort(matched, SpecificityComparator.compator);
        return matched;
    }

    static List findMatching(final Enumeration ruleNames,
                             final String sel) {
        final List matched = new ArrayList();
        final Selector selector = new Selector(sel);

        SimpleSelector ruleLast;

        while (ruleNames.hasMoreElements()) {
            final String ruleName = (String)ruleNames.nextElement();
            Selector rule = new Selector(ruleName);

            int sIndex = selector.simpleSelectors.length - 1;
            SimpleSelector last = selector.simpleSelectors[sIndex];

            int rIndex = rule.simpleSelectors.length - 1;
            ruleLast = rule.simpleSelectors[rIndex];
            if (ruleLast.matches(last.tag,
                                 last.id,
                                 last.clazz)) {
                boolean result = true;
                for (rIndex--; rIndex >= 0 && result; rIndex--) {
                    ruleLast = rule.simpleSelectors[rIndex];
                    result = false;
                    while (sIndex > 0 && !result) {
                        SimpleSelector current =
                            selector.simpleSelectors[--sIndex];
                        result = ruleLast.matches(current.tag,
                                                  current.id,
                                                  current.clazz);
                    }
                }

                if (result) {
                    matched.add(rule);
                }
            }
        }
        Collections.sort(matched, SpecificityComparator.compator);
        return matched;
    }

    static String getID(final Element element) {
        return getID(getTag(element), element);
    }

    static String getID(final HTML.Tag tag, final Element element) {
        if (element.isLeaf()) {
            return getID((AttributeSet)element.getAttributes()
                                       .getAttribute(tag));
        }
        return getID(element.getAttributes());
    }

    static String getID(final AttributeSet attr) {
        return attr != null ? (String)attr.getAttribute(HTML.Attribute.ID)
                            : null;
    }


    static String getClass(final Element element) {
        return (String)element.getAttributes().getAttribute(HTML.Attribute.CLASS);
    }

    static String getClass(final HTML.Tag tag, final Element element) {
        if (element.isLeaf()) {
            return getClass((AttributeSet)element.getAttributes()
                                          .getAttribute(tag));
        }
        return getClass(element.getAttributes());
    }

    static String getClass(final AttributeSet attr) {
        return attr != null ? (String)attr.getAttribute(HTML.Attribute.CLASS)
                            : null;
    }


    static HTML.Tag getTag(final Element element) {
        return getTag(element.getAttributes());
    }

    static HTML.Tag getTag(final AttributeSet attr) {
        Object name = attr.getAttribute(StyleConstants.NameAttribute);
        if (name instanceof HTML.Tag && name != HTML.Tag.CONTENT) {
            return (HTML.Tag)name;
        }

        Enumeration keys = attr.getAttributeNames();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof HTML.Tag) {
                return (HTML.Tag)key;
            }
        }
        return null;
    }

    static String getTagName(final Element element) {
        return getTag(element).toString();
    }

    static String getTagName(final AttributeSet attr) {
        return getTag(attr).toString();
    }
}
