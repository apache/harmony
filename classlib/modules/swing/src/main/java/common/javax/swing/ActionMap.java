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

package javax.swing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class ActionMap implements Serializable {
    private static final long serialVersionUID = -6277518704513986346L;

    private ActionMap parent;

    private HashMap<Object, Action> table;

    public void put(final Object key, final Action action) {
        if (action != null) {
            if (table == null) {
                table = new HashMap<Object, Action>();
            }
            table.put(key, action);
        } else {
            remove(key);
        }
    }

    public Action get(final Object key) {
        Action action = null;
        if (table != null) {
            action = table.get(key);
        }
        if (action == null && getParent() != null) {
            action = getParent().get(key);
        }
        return action;
    }

    public void setParent(final ActionMap parent) {
        this.parent = parent;
    }

    public ActionMap getParent() {
        return parent;
    }

    public void remove(final Object key) {
        if (table != null) {
            table.remove(key);
        }
    }

    public Object[] keys() {
        if (table == null) {
            return new Object[0];
        }
        return table.keySet().toArray(new Object[table.size()]);
    }

    public Object[] allKeys() {
        Object[] keys = keys();
        if (parent == null) {
            return keys;
        }
        Object[] parentKeys = parent.allKeys();
        if (keys.length == 0) {
            return parentKeys;
        }
        if (parentKeys.length == 0) {
            return keys;
        }
        HashSet<Object> keySet = new HashSet<Object>(Arrays.asList(keys));
        keySet.addAll(Arrays.asList(parentKeys));
        return keySet.toArray(new Object[keySet.size()]);
    }

    public void clear() {
        if (table != null) {
            table.clear();
        }
    }

    public int size() {
        return (table != null) ? table.size() : 0;
    }
}
