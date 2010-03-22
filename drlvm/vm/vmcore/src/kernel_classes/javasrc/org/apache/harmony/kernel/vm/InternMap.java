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

package org.apache.harmony.kernel.vm;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Implements weak hash map specialized for storing interned string pool.
 * @see java.util.WeakHashMap
 * @see WeakReference
 */
public class InternMap {

    private final ReferenceQueue<String> referenceQueue;

    int elementCount;

    Entry[] elementData;

    private final int loadFactor;

    private int threshold;

    //Simple utility method to isolate unchecked cast for array creation
    private static Entry[] newEntryArray(int size) {
        return new Entry[size];
    }

    private static final class Entry extends WeakReference<String> {
        int hash;
        Entry next;
        Entry(String key, ReferenceQueue<String> queue) {
            super(key, queue);
            hash = key.hashCode();
        }
    }

    public InternMap(int capacity) {
        if (capacity >= 0) {
            elementCount = 0;
            elementData = newEntryArray(capacity == 0 ? 1 : capacity);
            loadFactor = 7500; // Default load factor of 0.75
            computeMaxSize();
            referenceQueue = new ReferenceQueue<String>();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void computeMaxSize() {
        threshold = (int) ((long) elementData.length * loadFactor / 10000);
    }

    void poll()
    {
        Entry toRemove;
        while ((toRemove = (Entry)(WeakReference<String>)referenceQueue.poll()) != null) {
                removeEntry(toRemove);
        }
    }

    void removeEntry(Entry toRemove)
    {
        Entry entry, last = null;
        int index = (toRemove.hash & 0x7FFFFFFF) % elementData.length;
        entry = elementData[index];
        // Ignore queued entries which cannot be found, the user could
        // have removed them before they were queued, i.e. using clear()
        while (entry != null) {
            if (toRemove == entry) {
                if (last == null) {
                    elementData[index] = entry.next;
                } else {
                    last.next = entry.next;
                }
                elementCount--;
                break;
            }
            last = entry;
            entry = entry.next;
        }
    }

    public String intern(String key)
    {
        int index = 0;
        Entry entry;
        String interned = null;
        if (key == null) 
            return null;
        int hash = key.hashCode();
        int length = elementData.length;
        index = (hash & 0x7FFFFFFF) % length;
        entry = elementData[index];
        while (entry != null && !key.equals(interned = (String)entry.get())) {
            entry = entry.next;
        }

        // if we found the entry, return it
        if (entry != null) {
            return interned;
        }

        // no interned string found, put a new entry for it
        if (++elementCount > threshold) {
            rehash();
            index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
        }
        entry = new Entry(key, referenceQueue);
        entry.next = elementData[index];
        elementData[index] = entry;
        return key;
    }

    private void rehash()
    {
        poll();
        int length = elementData.length << 1;
        if (length == 0) {
            length = 1;
        }
        Entry[] newData = newEntryArray(length);
        for (int i = 0; i < elementData.length; i++) {
            Entry entry = elementData[i];
            while (entry != null) {
                int index = (entry.hash & 0x7FFFFFFF) % length;
                Entry next = entry.next;
                entry.next = newData[index];
                newData[index] = entry;
                entry = next;
            }
        }
        elementData = newData;
        computeMaxSize();
    }
}

