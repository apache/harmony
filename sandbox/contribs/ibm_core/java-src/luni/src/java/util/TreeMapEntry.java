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


/**
 * TreeMapEntry is an internal class which is used to hold the entries of a
 * TreeMap.
 */
class TreeMapEntry extends MapEntry {
	
	TreeMapEntry parent, left, right;

	boolean color;

	TreeMapEntry(Object key) {
		super(key);
	}

	TreeMapEntry(Object key, Object value) {
		super(key, value);
	}

	TreeMapEntry clone(TreeMapEntry parentEntry) {
		TreeMapEntry clone = (TreeMapEntry) super.clone();
		clone.parent = parentEntry;
		if (left != null) {
			clone.left = left.clone(clone);
		}
		if (right != null) {
			clone.right = right.clone(clone);
		}
		return clone;
	}
}
