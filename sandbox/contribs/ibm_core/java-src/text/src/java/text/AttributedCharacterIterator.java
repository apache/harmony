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

package java.text;


import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.ibm.oti.util.Msg;

/**
 * AttributedCharacterIterator
 */
public interface AttributedCharacterIterator extends CharacterIterator {

	public static class Attribute implements Serializable {

		static final long serialVersionUID = -9142742483513960612L;

		public static final Attribute INPUT_METHOD_SEGMENT = new Attribute(
				"input_method_segment");

		public static final Attribute LANGUAGE = new Attribute("language");

		public static final Attribute READING = new Attribute("reading");

		private String name;

		protected Attribute(String name) {
			this.name = name;
		}

		public final boolean equals(Object object) {
			if (object == null || !(object.getClass().equals(this.getClass())))
				return false;
			return name.equals(((Attribute) object).name);
		}

		protected String getName() {
			return name;
		}

		public final int hashCode() {
			return name.hashCode();
		}

		protected Object readResolve() throws InvalidObjectException {
			if (this.getClass() != Attribute.class)
				throw new InvalidObjectException(Msg.getString("K000c"));
			if (this.equals(INPUT_METHOD_SEGMENT))
				return INPUT_METHOD_SEGMENT;
			if (this.equals(LANGUAGE))
				return LANGUAGE;
			if (this.equals(READING))
				return READING;
			throw new InvalidObjectException(Msg.getString("K000d"));
		}

		public String toString() {
			return getClass().getName() + '(' + getName() + ')';
		}
	}

	public Set getAllAttributeKeys();

	public Object getAttribute(Attribute attribute);

	public Map getAttributes();

	public int getRunLimit();

	public int getRunLimit(Attribute attribute);

	public int getRunLimit(Set attributes);

	public int getRunStart();

	public int getRunStart(Attribute attribute);

	public int getRunStart(Set attributes);
}
