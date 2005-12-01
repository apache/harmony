/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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


import java.io.Serializable;

/**
 * EventObjects represent events. Typically applications subclass this class to
 * add event specific information.
 * 
 * @see EventListener
 */
public class EventObject implements Serializable {
	
	static final long serialVersionUID = 5516075349620653480L;

	/**
	 * The event source.
	 */
	protected transient Object source;

	/**
	 * Constructs a new instance of this class.
	 * 
	 * @param source
	 *            the object which fired the event
	 */
	public EventObject(Object source) {
		if (source != null)
			this.source = source;
		else
			throw new IllegalArgumentException();
	}

	/**
	 * Answers the event source.
	 * 
	 * @return the object which fired the event
	 */
	public Object getSource() {
		return source;
	}

	/**
	 * Answers the string representation of this EventObject.
	 * 
	 * @return the string representation of this EventObject
	 */
	public String toString() {
		return getClass().getName() + "[source=" + String.valueOf(source) + ']'; //$NON-NLS-1$
	}
}
