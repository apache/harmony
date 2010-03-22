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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Alexei Y. Zakharov
 */

package org.apache.harmony.jndi.provider.dns;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

/**
 * A cache for received resource records. Common for all active resolvers.
 * 
 * TODO handling of records with TTL set to 0; should not be cached.
 */
class ResolverCache {

    /** keys - zone & host names; values - vectors with RRs */
    HashMap<String, Vector<CacheEntry>> names = new HashMap<String, Vector<CacheEntry>>();

    /**
     * Since <code>ResolverCache</code> is singleton class its constructor
     * should be hidden.
     */
    private ResolverCache() {
        names = new HashMap<String, Vector<CacheEntry>>();
    }

    private static ResolverCache instance = null;

    /**
     * <code>ResolverCache</code> is a singleton class.
     * 
     * @return active instance of <code>ResolverCache</code>
     */
    static ResolverCache getInstance() {
        if (instance == null) {
            instance = new ResolverCache();
        }
        return instance;
    }

    /**
     * Looks through the cache and returns all suitable resource records
     * 
     * @param question
     *            a question record that determines which records we want to get
     *            from the cache
     * @return Enumeration of found Resource Records.
     */
    synchronized Enumeration<ResourceRecord> get(QuestionRecord question) {
        String name = question.getQName().toLowerCase();
        Vector<CacheEntry> vect = names.get(name);
        int qClass = question.getQClass();
        int qType = question.getQType();
        Vector<ResourceRecord> resVect = new Vector<ResourceRecord>();

        if (vect != null) {
            for (int i = 0; i < vect.size(); i++) {
                CacheEntry curEntry = vect.elementAt(i);
                ResourceRecord curRR = curEntry.getRR();

                if (curEntry.getBestBefore() < System.currentTimeMillis()) {
                    // the record is out of date
                    vect.removeElementAt(i--);
                    continue;
                }
                if (qClass == ProviderConstants.ANY_QCLASS
                        || qClass != curRR.getRRClass()) {
                    continue;
                }
                if (qType == ProviderConstants.ANY_QTYPE
                        || qType != curRR.getRRType()) {
                    continue;
                }
                resVect.addElement(curRR);
            }
        }
        return resVect.elements();
    }

    /**
     * Puts element into the cache. Doesn't put records with zero TTLs. Doesn't
     * put records with bad TTLs.
     * 
     * @param record
     *            a resource record to insert
     */
    synchronized void put(ResourceRecord record) {
        String name = record.getName().toLowerCase();
        Vector<CacheEntry> vect = names.get(name);
        long curTime = System.currentTimeMillis();
        CacheEntry entry = null;

        if (vect == null) {
            vect = new Vector<CacheEntry>();
            names.put(name, vect);
        }
        // TTL should be between 0 and 2^31; if greater - should be set to 0
        // See RFC 2181 point 8
        if (record.getTtl() >> 31 != 0) {
            record.setTtl(0);
        }
        // skip records with wildcards in names or with zero TTL
        if (record.getTtl() > 0 && (record.getName().indexOf('*') == -1)) {
            entry = new CacheEntry(record, curTime + record.getTtl());
            // remove old occurrence if any
            for (int i = 0; i < vect.size(); i++) {
                CacheEntry exEntry = vect.elementAt(i);
                ResourceRecord exRec = exEntry.rr;

                if (ProviderMgr
                        .namesAreEqual(record.getName(), exRec.getName())
                        && record.getRRClass() == exRec.getRRClass()
                        && record.getRRType() == exRec.getRRType()) {
                    if (record.getRData() != null && exRec.getRData() != null
                            && record.getRData().equals(exRec.getRData())) {
                        vect.remove(i);
                        break;
                    }
                }
            }
            vect.addElement(entry);
        }
    }

    /**
     * Removes all cached entries.
     */
    synchronized void clear() {
        names = new HashMap<String, Vector<CacheEntry>>();
    }

    /**
     * Represents SLIST cache entry.
     */
    static class CacheEntry {

        private ResourceRecord rr;

        private long bestBefore;

        /**
         * Constructs new cache entry.
         * 
         * @param rr
         *            Resource Record
         * @param bestBefore
         *            best before (time in millis)
         */
        public CacheEntry(ResourceRecord rr, long bestBefore) {
            this.rr = rr;
            this.bestBefore = bestBefore;
        }

        /**
         * @return Returns the bestBefore.
         */
        public long getBestBefore() {
            return bestBefore;
        }

        /**
         * @return Returns the Resource Record.
         */
        public ResourceRecord getRR() {
            return rr;
        }
    }

}
