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

/**
 * <p>
 * <i>InputMap</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class InputMap implements Serializable {
    private static final long serialVersionUID = -6824008057073482094L;

    private InputMap parent;

    private HashMap<KeyStroke, Object> table;

    public void put(KeyStroke keyStroke, Object key) {
        if (keyStroke == null) {
            return;
        }
        if (key != null) {
            if (table == null) {
                table = new HashMap<KeyStroke, Object>();
            }
            table.put(keyStroke, key);
        } else {
            remove(keyStroke);
        }
    }

    public Object get(KeyStroke keyStroke) {
        Object key = null;
        if (table != null) {
            key = table.get(keyStroke);
        }
        if (key == null && getParent() != null) {
            key = getParent().get(keyStroke);
        }
        return key;
    }

    public void remove(KeyStroke keyStroke) {
        if (table != null) {
            table.remove(keyStroke);
        }
    }

    public KeyStroke[] keys() {
        if (table == null) {
            return new KeyStroke[0];
        }
        return table.keySet().toArray(new KeyStroke[table.size()]);
    }

    public KeyStroke[] allKeys() {
        KeyStroke[] keys = keys();
        if (parent == null) {
            return keys;
        }
        KeyStroke[] parentKeys = parent.allKeys();
        if (keys.length == 0) {
            return parentKeys;
        }
        if (parentKeys.length == 0) {
            return keys;
        }
        HashSet<KeyStroke> keySet = new HashSet<KeyStroke>(Arrays.asList(keys));
        keySet.addAll(Arrays.asList(parentKeys));
        return keySet.toArray(new KeyStroke[keySet.size()]);
    }

    public void setParent(InputMap parent) {
        this.parent = parent;
    }

    public InputMap getParent() {
        return parent;
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
