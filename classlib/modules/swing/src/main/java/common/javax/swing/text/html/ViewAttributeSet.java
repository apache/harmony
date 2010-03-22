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

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.html.CSS.RelativeValueResolver;

class ViewAttributeSet extends CompositeAttributeSet {
    final View view;

    ViewAttributeSet(final StyleSheet ss, final View view) {
        super(calculateElementAttr(ss, view),
              calculateCSSRules(ss, view));
        this.view = view;
    }

    public AttributeSet getResolveParent() {
        final View parent = view.getParent();
        return parent != null ? parent.getAttributes() : null;
    }

    public Object getAttribute(final Object key) {
        if (key == ResolveAttribute) {
            return getResolveParent();
        }

        Object result = getElementAttr().getAttribute(key);
        if (result != null) {
            return result;
        }

        if (getCssRules().getAttributeCount() > 0) {
            result =
                ((CascadedStyle)getCssRules()).getAttribute(key, view.getElement());
            if (result != null) {
                return result;
            }
        }

        final AttributeSet resolver = getResolveParent();
        if (resolver == null) {
            return null;
        }

        if (key instanceof CSS.Attribute) {
            CSS.Attribute cssKey = (CSS.Attribute)key;
            if (cssKey.isInherited()) {
                result = resolver.getAttribute(key);
                if (result instanceof RelativeValueResolver) {
                    return ((RelativeValueResolver)result)
                           .getComputedValue(view.getParent());
                }
                return result;
            }
            return null;
        }
        return resolver.getAttribute(key);
    }

    private static AttributeSet calculateElementAttr(final StyleSheet ss,
                                                     final View view) {
        final Element element = view.getElement();
        return ss.translateHTMLToCSS(element.getAttributes());
    }

    private static AttributeSet calculateCSSRules(final StyleSheet ss,
                                                  final View view) {
        HTML.Tag tag = SelectorMatcher.getTag(view.getElement());
        return tag != null ? ss.getRule(tag, view.getElement()) : ss.getEmptySet();
    }

    private AttributeSet getElementAttr() {
        return getPrimarySet();
    }

    private AttributeSet getCssRules() {
        return getSecondarySet();
    }
}
