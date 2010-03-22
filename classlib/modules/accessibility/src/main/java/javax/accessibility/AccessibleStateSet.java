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
 * @author Dennis Ushakov
 */

package javax.accessibility;

import java.util.Iterator;
import java.util.Vector;

public class AccessibleStateSet {

    protected Vector<AccessibleState> states;

    public AccessibleStateSet() {
        super();
    }

    public AccessibleStateSet(AccessibleState[] states) {
        super();
        if (states.length == 0) {
            return;
        }
        this.states = new Vector<AccessibleState>(states.length);
        for (AccessibleState element : states) {
            if (!this.states.contains(element)) {
                this.states.addElement(element);
            }
        }
    }

    public boolean add(final AccessibleState state) {
        initStorage();
        if (states.contains(state)) {
            return false;
        }
        states.add(state);
        return true;
    }

    public void addAll(final AccessibleState[] states) {
        if (states.length == 0) {
            return;
        }
        initStorage(states.length);
        for (AccessibleState element : states) {
            if (!this.states.contains(element)) {
                this.states.addElement(element);
            }
        }
    }

    public boolean contains(AccessibleState state) {
        return states == null ? false : states.contains(state);
    }

    public boolean remove(AccessibleState state) {
        return states == null ? false : states.remove(state);
    }

    public void clear() {
        if (states != null) {
            states.clear();
        }
    }

    public AccessibleState[] toArray() {
        return states == null ? new AccessibleState[0] : states
                .toArray(new AccessibleState[states.size()]);
    }

    @Override
    public String toString() {
        if (states == null) {
            return null;
        }
        StringBuilder str = new StringBuilder();
        for (Iterator<AccessibleState> it = states.iterator(); it.hasNext();) {
            str.append(it.next().toString());
            if (it.hasNext()) {
                str.append(","); //$NON-NLS-1$
            }
        }
        return str.toString();
    }

    private void initStorage(final int capacity) {
        if (states == null) {
            states = new Vector<AccessibleState>(capacity);
        }
    }

    private void initStorage() {
        if (states == null) {
            states = new Vector<AccessibleState>();
        }
    }
}
