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
package javax.swing.text;

import java.io.Serializable;

import org.apache.harmony.misc.HashCode;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class TabSet implements Serializable {

    private TabStop[] tabs;

    public TabSet(final TabStop[] tabs) {
        this.tabs = new TabStop[tabs.length];
        System.arraycopy(tabs, 0, this.tabs, 0, tabs.length);
    }

    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TabSet)) {
            return false;
        }
        final TabSet aTabSet = (TabSet)obj;
        boolean result = tabs.length == aTabSet.tabs.length;
        for (int i = 0; i < tabs.length && result; i++) {
            result = tabs[i].equals(aTabSet.tabs[i]);
        }

        return result;
    }

    public TabStop getTab(final int i) {
        if (i < 0 || i >= tabs.length) {
            throw new IllegalArgumentException(Messages.getString("swing.9C", i)); //$NON-NLS-1$
        }
        return tabs[i];
    }

    public TabStop getTabAfter(final float location) {
        int index = getTabIndexAfter(location);
        if (index != -1) {
            return tabs[index];
        }

        return null;
    }

    public int getTabCount() {
        return tabs.length;
    }

    public int getTabIndex(final TabStop tab) {
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i].equals(tab)) {
                return i;
            }
        }

        return -1;
    }

    public int getTabIndexAfter(final float location) {
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i].getPosition() > location) {
                return i;
            }
        }

        return -1;
    }

    public int hashCode() {
        HashCode hash = new HashCode();
        for (int i = 0; i < tabs.length; i++) {
            hash = hash.append(tabs[i].hashCode());
        }
        return hash.hashCode();
    }

    public String toString() {
        StringBuilder result = new StringBuilder("[ ");
        for (int i = 0; i < tabs.length; i++) {
            if (i != 0) {
                result.append(" - ");
            }
            result.append(tabs[i].toString());
        }
        result.append(" ]");

        return result.toString();
    }
}


