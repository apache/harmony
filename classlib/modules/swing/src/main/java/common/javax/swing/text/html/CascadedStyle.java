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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * Storage for all the attributes applicable for the
 * {@link javax.swing.text.Element} for which CascadedStyle
 * is built. Applicability is defined by CSS1 specification (http://www.w3.org/TR/CSS1).
 */
final class CascadedStyle implements Style {
    private final StyleSheet styleSheet;
    private final List styleList;
    private final List sheetList;
    private final String name;
    private final Selector selector;

    CascadedStyle(final StyleSheet styleSheet, final Element element,
                  final List styleList, final Iterator sheetIt) {
        this(styleSheet, getElementTreeSelector(element), styleList, sheetIt);
    }

    CascadedStyle(final StyleSheet styleSheet, final String name,
                  final List styleList, final Iterator sheetIt) {
        this.styleSheet = styleSheet;
        this.styleList = styleList;
        this.name = name;
        this.selector = new Selector(name);

        sheetList = new LinkedList();
        while (sheetIt.hasNext()) {
            sheetList.add(((StyleSheet)sheetIt.next()).getRule(name));
        }
    }

    public static String getElementTreeSelector(final Element element) {
        final StringBuilder result = new StringBuilder();
        result.append(getFullName(element));
        Element parent = element.getParentElement();
        while (parent != null) {
            result.insert(0, ' ');
            result.insert(0, getFullName(parent));
            parent = parent.getParentElement();
        }
        return result.toString();
    }

    public static String getFullName(final Element element) {
        HTML.Tag tag = SelectorMatcher.getTag(element);
        if (tag == null) {
            return getFullName(element.getParentElement());
        }

        return getFullElementName(element, tag);
    }

    public static String getFullElementName(final Element element) {
        HTML.Tag tag = SelectorMatcher.getTag(element);
        if (tag == null) {
            return null;
        }

        return getFullElementName(element, tag);
    }

    public static String getFullElementName(final Element element,
                                            final HTML.Tag tag) {
        final String tagName = tag.toString();
        final String id      = SelectorMatcher.getID(tag, element);
        final String clazz   = SelectorMatcher.getClass(tag, element);

        final StringBuilder result = new StringBuilder();
        if (tagName != null) {
            result.append(tagName);
        }
        if (id != null) {
            result.append('#').append(id);
        }
        if (clazz != null) {
            result.append('.').append(clazz);
        }

        return result.toString();
    }

    public String getName() {
        return name;
    }

    public void addChangeListener(final ChangeListener arg0) {
    }

    public void removeChangeListener(final ChangeListener arg0) {
    }

    public void removeAttribute(final Object arg0) {
//        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    public void removeAttributes(final Enumeration arg0) {
//        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    public void addAttributes(final AttributeSet arg0) {
//        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    public void removeAttributes(final AttributeSet arg0) {
//        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    public void setResolveParent(final AttributeSet arg0) {
//        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    public void addAttribute(final Object arg0, final Object arg1) {
//        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    public int getAttributeCount() {
        Iterator it = styleList.iterator();
        int result = 0;
        while (it.hasNext()) {
            result += styleSheet.getStyle(it.next().toString())
                      .getAttributeCount();
        }

        it = sheetList.iterator();
        while (it.hasNext()) {
            result += ((AttributeSet)it.next()).getAttributeCount();
        }
        return result;
    }

    public boolean isDefined(final Object arg0) {
        Iterator it = styleList.iterator();
        boolean result = false;
        while (!result && it.hasNext()) {
            Style style = styleSheet.getStyle(it.next().toString());
            result = style.isDefined(arg0);
        }
        if (result) {
            return result;
        }

        it = sheetList.iterator();
        while (!result && it.hasNext()) {
            result = ((AttributeSet)it.next()).isDefined(arg0);
        }
        return result;
    }

    public Enumeration getAttributeNames() {
        return new Enumeration() {
            private final Iterator styleIt = styleList.iterator();
            private final Iterator sheetIt = sheetList.iterator();
            private Enumeration current;

            public boolean hasMoreElements() {
                return styleIt.hasNext()
                       || current != null && current.hasMoreElements()
                       || hasAttributesInSheets();
            }

            public Object nextElement() {
                if (current != null && current.hasMoreElements()) {
                    return current.nextElement();
                }
                if (styleIt.hasNext()) {
                    current = getAttributeNamesOfNextStyle();
                    return nextElement();
                }
                if (hasAttributesInSheets()) {
                    return current.nextElement();
                }

                throw new NoSuchElementException();
            }

            private boolean hasAttributesInSheets() {
                if (sheetIt.hasNext()) {
                    current =
                        ((AttributeSet)sheetIt.next()).getAttributeNames();
                }
                return current != null && current.hasMoreElements();
            }

            private Enumeration getAttributeNamesOfNextStyle() {
                return styleSheet.getStyle(styleIt.next().toString())
                                  .getAttributeNames();
            }
        };
    }

    public AttributeSet copyAttributes() {
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    public AttributeSet getResolveParent() {
        return (AttributeSet)getAttribute(StyleConstants.ResolveAttribute);
    }

    public boolean containsAttributes(final AttributeSet arg0) {
        boolean result = true;
        final Enumeration keys = arg0.getAttributeNames();
        while (result && keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = arg0.getAttribute(key);
            result = containsAttribute(key, value);
        }
        return result;
    }

    public boolean isEqual(final AttributeSet arg0) {
        if (getAttributeCount() != arg0.getAttributeCount()) {
            return false;
        }
        return containsAttributes(arg0);
    }

    public Object getAttribute(final Object key) {
        Iterator it = styleList.iterator();
        Object result = null;
        while (result == null && it.hasNext()) {
            Style style = styleSheet.getStyle(it.next().toString());
            result = style.getAttribute(key);
        }
        if (result != null) {
            return result;
        }
        it = sheetList.iterator();
        while (result == null && it.hasNext()) {
            result = ((AttributeSet)it.next()).getAttribute(key);
        }
        return result;
    }

    public Object getAttribute(final Object key, final Element element) {
        if (!(key instanceof CSS.Attribute)
            || ((CSS.Attribute)key).isInherited()) {

            return getAttribute(key);
        }
        final String elementName = getFullElementName(element);
        if (elementName == null) {
            return null;
        }
        final SimpleSelector elementSelector = new SimpleSelector(elementName);

        Iterator it = styleList.iterator();
        Object result = null;
        while (result == null && it.hasNext()) {
            Selector styleSelector = (Selector)it.next();
            if (!elementSelector.applies(styleSelector.getLastSelector())) {
                continue;
            }
            Style style = styleSheet.getStyle(styleSelector.toString());
            result = style.getAttribute(key);
        }
        if (result != null) {
            return result;
        }
        it = sheetList.iterator();
        while (result == null && it.hasNext()) {
            result = ((AttributeSet)it.next()).getAttribute(key);
        }
        return result;
    }

    public boolean containsAttribute(final Object arg0, final Object arg1) {
        Iterator it = styleList.iterator();
        boolean result = false;
        while (!result && it.hasNext()) {
            Style style = styleSheet.getStyle(it.next().toString());
            result = style.containsAttribute(arg0, arg1);
        }
        if (result) {
            return result;
        }
        it = sheetList.iterator();
        while (!result && it.hasNext()) {
            result = ((AttributeSet)it.next()).containsAttribute(arg0, arg1);
        }
        return result;
    }

    void addStyle(final String styleName) {
        Selector styleSelector = new Selector(styleName);
        if (selector.applies(styleSelector)) {
            styleList.add(styleSelector);
            Collections.sort(styleList, SpecificityComparator.compator);
        }
    }

    void removeStyle(final String styleName) {
        Iterator it = styleList.iterator();
        while (it.hasNext()) {
            Selector s = (Selector)it.next();
            if (styleName.equals(s.toString())) {
                it.remove();
                break;
            }
        }
    }

    void addStyleSheet(final StyleSheet ss) {
        sheetList.add(0, ss.getRule(name));
    }

    void removeStyleSheet(final StyleSheet ss) {
        final Iterator it = sheetList.iterator();
        while (it.hasNext()) {
            CascadedStyle rs = (CascadedStyle)it.next();
            if (rs.styleSheet == ss) {
                it.remove();
                break;
            }
        }
    }
}
