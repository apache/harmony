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

package java.util;


/**
 * Observable is used to notify a group of Observer objects when a change
 * occurs.
 */
public class Observable {
	
	Vector observers = new Vector();

	boolean changed = false;

	/**
	 * Constructs a new Observable object.
	 */
	public Observable() {
		super();
	}

	/**
	 * Adds the specified Observer to the list of observers.
	 * 
	 * @param observer
	 *            the Observer to add
	 */
	public synchronized void addObserver(Observer observer) {
		if (observer == null) {
			throw new NullPointerException();
		}
		if (!observers.contains(observer))
			observers.addElement(observer);
	}

	/**
	 * Clears the changed flag for this Observable.
	 */
	protected synchronized void clearChanged() {
		changed = false;
	}

	/**
	 * Answers the number of Observers in the list of observers.
	 * 
	 * @return the number of observers
	 */
	public synchronized int countObservers() {
		return observers.size();
	}

	/**
	 * Removes the specified Observer from the list of observers.
	 * 
	 * @param observer
	 *            the Observer to remove
	 */
	public synchronized void deleteObserver(Observer observer) {
		observers.removeElement(observer);
	}

	/**
	 * Removes all Observers from the list of observers.
	 */
	public synchronized void deleteObservers() {
		observers.setSize(0);
	}

	/**
	 * Answers the changed flag for this Observable.
	 * 
	 * @return true when the changed flag for this Observable is set, false
	 *         otherwise
	 */
	public synchronized boolean hasChanged() {
		return changed;
	}

	/**
	 * If the changed flag is set, calls the <code>update()</code> method for
	 * every Observer in the list of observers using null as the argument.
	 * Clears the changed flag.
	 */
	public void notifyObservers() {
		notifyObservers(null);
	}

	/**
	 * If the changed flag is set, calls the <code>update()</code> method for
	 * every Observer in the list of observers using the specified argument.
	 * Clears the changed flag.
	 * 
	 * @param data
	 *            the argument passed to update()
	 */
	public void notifyObservers(Object data) {
		if (changed) {
			// Must clone the vector in case deleteObserver is called
			Vector clone = (Vector) observers.clone();
			int size = clone.size();
			for (int i = 0; i < size; i++)
				((Observer) clone.elementAt(i)).update(this, data);
			clearChanged();
		}
	}

	/**
	 * Sets the changed flag for this Observable.
	 */
	protected synchronized void setChanged() {
		changed = true;
	}
}
