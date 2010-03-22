/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package java.beans;

/**
 * A simple map.
 * Keys are equal iff they are the same object (reference equals).
 * The put() do not check key duplication, so always do a get() before put().
 * Noop if either key or value is null.
 * 
 */
class ReferenceMap {

    private static class Pair {
        Object key;

        Object value;

        Pair(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final int GROWING_UNIT = 16;

    private Pair pairs[] = new Pair[GROWING_UNIT];

    private int size = 0;
    
    public void clear() {
        size = 0;
    }

    public Object get(Object key) {
        if (key == null) {
            return null;
        }
        for (int i = size - 1; i >= 0; i--) {
            Pair p = pairs[i];
            if (p.key == key) {
                return p.value;
            }
        }
        return null;
    }

    public void put(Object key, Object value) {
        if (key == null || value == null) {
            return;
        }

        // grow pairs if necessary
        if (pairs.length <= size) {
            int newLength = pairs.length + GROWING_UNIT;
            while (newLength <= size) {
                newLength += GROWING_UNIT;
            }
            Pair newPairs[] = new Pair[newLength];
            System.arraycopy(pairs, 0, newPairs, 0, size);
            pairs = newPairs;
        }

        pairs[size++] = new Pair(key, value);
    }

    public Object remove(Object key) {
        if (key == null) {
            return key;
        }
        for (int i = size - 1; i >= 0; i--) {
            Pair p = pairs[i];
            if (p.key == key) {
                pairs[i] = pairs[--size];
                return p.value;
            }
        }
        return null;
    }

}



 
