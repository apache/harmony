/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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

package java.lang.ref;


/**
 * The implementation of this class is provided. The non-public implementation
 * details are documented so the vm vendor can use the implementation.
 * 
 * ReferenceQueue is the container on which reference objects are enqueued when
 * their reachability type is detected for the referent.
 * 
 * @since JDK1.2
 */
public class ReferenceQueue extends Object {
	private Reference[] references;

	private int head, tail;

	private boolean empty;

	static private final int DEFAULT_QUEUE_SIZE = 128;

	/**
	 * Returns the next available reference from the queue if one is enqueued,
	 * null otherwise. Does not wait for a reference to become available.
	 * 
	 * @return Reference next available Reference or NULL.
	 */
	public Reference poll() {
		Reference ref;

		synchronized (this) {
			if (empty) {
				return null;
			}
			ref = references[head++];
			ref.dequeue();
			if (head == references.length) {
				head = 0;
			}
			if (head == tail) {
				empty = true;
			}
		}
		return ref;
	}

	/**
	 * Return the next available enqueued reference on the queue, blocking
	 * indefinately until one is available.
	 * 
	 * @return Reference a Reference object if one is available, null otherwise.
	 * @exception InterruptedException
	 *                to interrupt the wait.
	 */
	public Reference remove() throws InterruptedException {
		return remove(0L);
	}

	/**
	 * Return the next available enqueued reference on the queue, blocking up to
	 * the time given until one is available. Return null if no reference became
	 * available.
	 * 
	 * @param timeout
	 *            maximum time spent waiting for a reference object to become
	 *            available.
	 * @return Reference a Reference object if one is available, null otherwise.
	 * @exception IllegalArgumentException
	 *                if the wait period is negative. InterruptedException to
	 *                interrupt the wait.
	 */
	public Reference remove(long timeout) throws IllegalArgumentException,
			InterruptedException {
		if (timeout < 0) {
			throw new IllegalArgumentException();
		}

		Reference ref;
		synchronized (this) {
			if (empty) {
				wait(timeout);
				if (empty) {
					return null;
				}
			}
			ref = references[head++];
			ref.dequeue();
			if (head == references.length) {
				head = 0;
			}
			if (head == tail) {
				empty = true;
			} else {
				notifyAll();
			}
		}
		return ref;
	}

	/**
	 * Enqueue the reference object on the receiver.
	 * 
	 * @param reference
	 *            reference object to be enqueued.
	 * @return boolean true if reference is enqueued. false if reference failed
	 *         to enqueue.
	 */
	boolean enqueue(Reference reference) {
		synchronized (this) {
			if (!empty && head == tail) {
				/* Queue is full - grow */
				int newQueueSize = (int) (references.length * 1.10);
				Reference newQueue[] = new Reference[newQueueSize];
				System.arraycopy(references, head, newQueue, 0,
						references.length - head);
				if (tail > 0) {
					System.arraycopy(references, 0, newQueue, references.length
							- head, tail);
				}
				head = 0;
				tail = references.length;
				references = newQueue;
			}
			references[tail++] = reference;
			if (tail == references.length) {
				tail = 0;
			}
			empty = false;
			notifyAll();
		}
		return true;
	}

	/**
	 * Constructs a new instance of this class.
	 */
	public ReferenceQueue() {
		references = new Reference[DEFAULT_QUEUE_SIZE];
		head = 0;
		tail = 0;
		empty = true;
	}
}
