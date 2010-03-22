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
 * @author Vadim L. Bogdanov
 */
package javax.swing.text.html;

import java.util.Enumeration;

import javax.swing.text.AttributeSet;

class CompositeAttributeSet implements AttributeSet {

    private final AttributeSet primarySet;
    private final AttributeSet secondarySet;

    CompositeAttributeSet(final AttributeSet primarySet,
                          final AttributeSet secondarySet) {
        this.primarySet = primarySet;
        this.secondarySet = secondarySet;
    }

    public boolean containsAttribute(final Object key, final Object value) {
        return primarySet.containsAttribute(key, value)
            || secondarySet.containsAttribute(key, value);
    }

    public boolean containsAttributes(final AttributeSet attrSet) {
        boolean result = true;
        final Enumeration keys = attrSet.getAttributeNames();
        while (keys.hasMoreElements() && result) {
            Object key = keys.nextElement();
            result = containsAttribute(key, attrSet.getAttribute(key));
        }

        return result;
    }

    public AttributeSet copyAttributes() {
        return this;
    }

    public Object getAttribute(final Object key) {
        Object v = primarySet.getAttribute(key);
        return v != null ? v : secondarySet.getAttribute(key);
    }

    public int getAttributeCount() {
        return primarySet.getAttributeCount() + secondarySet.getAttributeCount();
    }

    public Enumeration getAttributeNames() {
        return new Enumeration() {
            private final Enumeration primaryNames =
                primarySet.getAttributeNames();
            private final Enumeration secondaryNames =
                secondarySet.getAttributeNames();

            public boolean hasMoreElements() {
                return primaryNames.hasMoreElements()
                       || secondaryNames.hasMoreElements();
            }

            public Object nextElement() {
                return primaryNames.hasMoreElements()
                    ? primaryNames.nextElement()
                    : secondaryNames.nextElement();
            }
        };
    }

    public AttributeSet getResolveParent() {
        return (AttributeSet)getAttribute(AttributeSet.ResolveAttribute);
    }

    public boolean isDefined(final Object key) {
        return primarySet.isDefined(key) || secondarySet.isDefined(key);
    }

    public boolean isEqual(final AttributeSet attrSet) {
        if (getAttributeCount() != attrSet.getAttributeCount()) {
            return false;
        }
        return containsAttributes(attrSet);
    }

    protected final AttributeSet getPrimarySet() {
        return primarySet;
    }

    protected final AttributeSet getSecondarySet() {
        return secondarySet;
    }
}
