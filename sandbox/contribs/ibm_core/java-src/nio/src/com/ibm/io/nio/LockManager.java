/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

package com.ibm.io.nio;


import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The lock manager is responsible for tracking acquired and pending locks on
 * the underlying file channel.
 * 
 */
final class LockManager {
	// The set of acquired and pending locks.
	private final Comparator lockComparator = new Comparator() {
		public int compare(Object lock1, Object lock2) {
			long position1 = ((FileLock) lock1).position();
			long position2 = ((FileLock) lock2).position();
			return position1 > position2 ? 1 : (position1 < position2 ? -1 : 0);
		}
	};

	private final SortedSet locks = new TreeSet(lockComparator);

	/*
	 * Default Constructor.
	 */
	protected LockManager() {
		super();
	}

	/*
	 * Add a new pending lock to the manager. Thows an exception if the lock
	 * would overlap an existing lock. Once the lock is acquired it remains in
	 * this set as an acquired lock.
	 */
	synchronized void addLock(FileLock lock)
			throws OverlappingFileLockException {
		long lockEnd = lock.position() + lock.size();
		for (Iterator keyItr = locks.iterator(); keyItr.hasNext();) {
			FileLock existingLock = (FileLock) keyItr.next();
			if (existingLock.position() > lockEnd) {
				// This, and all remaining locks, start beyond our end (so
				// cannot overlap).
				break;
			}
			if (existingLock.overlaps(lock.position(), lock.size())) {
				throw new OverlappingFileLockException();
			}
		}
		locks.add(lock);
	}

	/*
	 * Removes an acquired lock from the lock manager. If the lock did not exist
	 * in the lock manager the operation is a no-op.
	 */
	synchronized void removeLock(FileLock lock) {
		locks.remove(lock);
	}
}
