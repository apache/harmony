/* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.net;


import java.security.AccessController;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.oti.util.PriviAction;

/**
 * This class is used to manage the negative name lookup cache.
 */
class NegativeCache extends LinkedHashMap {

	static NegativeCache negCache = null;

	// maximum number of entries in the cache
	static final int MAX_NEGATIVE_ENTRIES = 5;

	// the loading for the cache
	static final float LOADING = (float) 0.75;

	/**
	 * Answers the hostname for the cache element
	 * 
	 * @return hostName name of the host on which the lookup failed
	 */
	NegativeCache(int initialCapacity, float loadFactor, boolean accessOrder) {
		super(initialCapacity, loadFactor, accessOrder);
	}

	/**
	 * Answers if we should remove the Eldest entry. We remove the eldest entry
	 * if the size has grown beyond the maximum size allowed for the cache. We
	 * create the LinkedHashMap such that this deletes the least recently used
	 * entry
	 * 
	 * @param eldest
	 *            the map enty which will be deleted if we return true
	 */
	protected boolean removeEldestEntry(Map.Entry eldest) {
		return size() > MAX_NEGATIVE_ENTRIES;
	}

	/**
	 * Adds the host name and the corresponding name lookup fail message to the
	 * cache
	 * 
	 * @param hostName
	 *            the name of the host for which the lookup failed
	 * @param failedMessage
	 *            the message returned when we failed the lookup
	 */
	static void put(String hostName, String failedMessage) {
		checkCacheExists();
		negCache.put(hostName, new NegCacheElement(failedMessage));
	}

	/**
	 * Answers the message that occured when we failed to lookup the host if
	 * such a failure is within the cache and the entry has not yet expired
	 * 
	 * @param hostName
	 *            the name of the host for which we are looking for an entry
	 * @return the message which was returned when the host failed to be looked
	 *         up if there is still a valid entry within the cache
	 */
	static String getFailedMessage(String hostName) {
		checkCacheExists();
		NegCacheElement element = (NegCacheElement) negCache.get(hostName);
		if (element != null) {
			// check if element is still valid
			String ttlValue = (String) AccessController
					.doPrivileged(new PriviAction(
							"networkaddress.cache.negative.ttl"));
			int ttl = 10;
			try {
				if (ttlValue != null)
					ttl = Integer.decode(ttlValue).intValue();
			} catch (NumberFormatException e) {
			}
			if (ttl == 0) {
				negCache.clear();
				element = null;
			} else if (ttl != -1) {
				if (element.timeAdded + (ttl * 1000) < System
						.currentTimeMillis()) {
					// remove the element from the cache and return null
					negCache.remove(hostName);
					element = null;
				}
			}
		}
		if (element != null) {
			return element.hostName();
		}
		return null;
	}

	/**
	 * This method checks if we have created the cache and if not creates it
	 */
	static void checkCacheExists() {
		if (negCache == null) {
			// Create with the access order set so ordering is based on when the
			// entries were last accessed. We make the default cache size one
			// greater than the maximum number of entries as we will grow to one
			// larger and then delete the LRU entry
			negCache = new NegativeCache(MAX_NEGATIVE_ENTRIES + 1, LOADING,
					true);
		}
	}
}
