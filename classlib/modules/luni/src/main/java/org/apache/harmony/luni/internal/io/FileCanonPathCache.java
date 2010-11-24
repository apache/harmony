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

package org.apache.harmony.luni.internal.io;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * A simple cache implementation for file's canonical path. The cache has fixed
 * size <code> CACHE_SIZE </code> and cached elements would be expired. If
 * <code>put<code> method is invoked when cache is full, the oldest element will be removed.
 *
 */
public class FileCanonPathCache {

    static private class CacheElement {
        final String canonicalPath;

        final long timestamp;

        public CacheElement(String path) {
            this.canonicalPath = path;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Max elements could be hold in the cache.
     */
    public static final int CACHE_SIZE = 256;

    private static final HashMap<String, CacheElement> cache = new HashMap<String, CacheElement>(
            CACHE_SIZE);

    /**
     * FIFO queue for tracking age of elements.
     */
    private static final LinkedList<String> list = new LinkedList<String>();

    private static final Object lock = new Object();

    /**
     * Expired time, 0 disable this cache.
     */
    private static volatile long timeout = 30000;

    public static final String FILE_CANONICAL_PATH_CACHE_TIMEOUT = "org.apache.harmony.file.canonical.path.cache.timeout";

    static {
        String value = System.getProperty(FILE_CANONICAL_PATH_CACHE_TIMEOUT);
        try {
            timeout = Long.parseLong(value);
        } catch (NumberFormatException e) {
            // use default timeout value
        }

        if (timeout < 0) {
            timeout = 0;
        }
    }

    /**
     * Retrieve element from cache.
     * 
     * @param path
     *            absolute path.
     * @return canonical path of <code>path</code> if it's in cache.
     * 
     */
    public static String get(String path) {
        long localTimeout = timeout;
        if (localTimeout == 0) {
            return null;
        }

        CacheElement element = null;
        synchronized (lock) {
            element = cache.get(path);
        }

        if (element == null) {
            return null;
        }

        long time = System.currentTimeMillis();
        if (time - element.timestamp > localTimeout) {
            // remove all elements older than this one
            synchronized (lock) {
                if (cache.get(path) != null) {
                    String oldest = null;
                    do {
                        oldest = list.removeFirst();
                        cache.remove(oldest);
                    } while (!path.equals(oldest));
                }
            }
            return null;
        }

        return element.canonicalPath;
    }

    /**
     * Put element to cache.
     * 
     * @param path
     *            absolute path.
     * @param canonicalPath
     *            the canonical path of <code>path</code>.
     */
    public static void put(String path, String canonicalPath) {
        if (timeout == 0) {
            return;
        }

        CacheElement element = new CacheElement(canonicalPath);
        synchronized (lock) {
            if (cache.size() >= CACHE_SIZE) {
                // cache is full
                String oldest = list.removeFirst();
                cache.remove(oldest);
            }
            cache.put(path, element);
            list.addLast(path);
        }
    }

    /**
     * Remove all elements from cache.
     */
    public static void clear() {
        if (timeout == 0) {
            return;
        }

        synchronized (lock) {
            cache.clear();
            list.clear();
        }
    }

    public static long getTimeout() {
        return timeout;
    }

    public static void setTimeout(long timeout) {
        synchronized (lock) {
            if (timeout <= 0) {
                timeout = 0;
                clear();
            }
            FileCanonPathCache.timeout = timeout;
        }
    }
}
