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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.ibm.oti.util.Msg;

/**
 * AttributedString
 */
public class AttributedString {

	String text;

	Map attributeMap;

	static class Range {
		int start, end;

		Object value;

		Range(int s, int e, Object v) {
			start = s;
			end = e;
			value = v;
		}
	}

	static class AttributedIterator implements AttributedCharacterIterator {

		private int begin, end, offset;

		private AttributedString attrString;

		private HashSet attributesAllowed;

		AttributedIterator(AttributedString attrString) {
			this.attrString = attrString;
			begin = 0;
			end = attrString.text.length();
			offset = 0;
		}

		AttributedIterator(AttributedString attrString,
				AttributedCharacterIterator.Attribute[] attributes, int begin,
				int end) {
			if (begin < 0 || end > attrString.text.length() || begin > end)
				throw new IllegalArgumentException();
			this.begin = begin;
			this.end = end;
			offset = begin;
			this.attrString = attrString;
			if (attributes != null) {
				HashSet set = new HashSet((attributes.length * 4 / 3) + 1);
				for (int i = attributes.length; --i >= 0;)
					set.add(attributes[i]);
				attributesAllowed = set;
			}
		}

		/**
		 * Answers a new StringCharacterIterator with the same source String,
		 * begin, end, and current index as this StringCharacterIterator.
		 * 
		 * @return a shallow copy of this StringCharacterIterator
		 * 
		 * @see java.lang.Cloneable
		 */
		public Object clone() {
			try {
				AttributedIterator clone = (AttributedIterator) super.clone();
				if (attributesAllowed != null)
					clone.attributesAllowed = (HashSet) attributesAllowed
							.clone();
				return clone;
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}

		/**
		 * Answers the character at the current index in the source String.
		 * 
		 * @return the current character, or DONE if the current index is past
		 *         the end
		 */
		public char current() {
			if (offset == end)
				return DONE;
			return attrString.text.charAt(offset);
		}

		/**
		 * Sets the current position to the begin index and answers the
		 * character at the begin index.
		 * 
		 * @return the character at the begin index
		 */
		public char first() {
			if (begin == end)
				return DONE;
			offset = begin;
			return attrString.text.charAt(offset);
		}

		/**
		 * Answers the begin index in the source String.
		 * 
		 * @return the index of the first character to iterate
		 */
		public int getBeginIndex() {
			return begin;
		}

		/**
		 * Answers the end index in the source String.
		 * 
		 * @return the index one past the last character to iterate
		 */
		public int getEndIndex() {
			return end;
		}

		/**
		 * Answers the current index in the source String.
		 * 
		 * @return the current index
		 */
		public int getIndex() {
			return offset;
		}

		private boolean inRange(Range range) {
			if (!(range.value instanceof Annotation))
				return true;
			return range.start >= begin && range.start < end
					&& range.end > begin && range.end <= end;
		}

		private boolean inRange(ArrayList ranges) {
			Iterator it = ranges.iterator();
			while (it.hasNext()) {
				Range range = (Range) it.next();
				if (range.start >= begin && range.start < end) {
					return !(range.value instanceof Annotation)
							|| (range.end > begin && range.end <= end);
				} else if (range.end > begin && range.end <= end) {
					return !(range.value instanceof Annotation)
							|| (range.start >= begin && range.start < end);
				}
			}
			return false;
		}

		public Set getAllAttributeKeys() {
			if (begin == 0 && end == attrString.text.length()
					&& attributesAllowed == null)
				return attrString.attributeMap.keySet();

			HashSet result = new HashSet(
					(attrString.attributeMap.size() * 4 / 3) + 1);
			Iterator it = attrString.attributeMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				if (attributesAllowed == null
						|| attributesAllowed.contains(entry.getKey())) {
					ArrayList ranges = (ArrayList) entry.getValue();
					if (inRange(ranges))
						result.add(entry.getKey());
				}
			}
			return result;
		}

		private Object currentValue(ArrayList ranges) {
			Iterator it = ranges.iterator();
			while (it.hasNext()) {
				Range range = (Range) it.next();
				if (offset >= range.start && offset < range.end)
					return inRange(range) ? range.value : null;
			}
			return null;
		}

		public Object getAttribute(
				AttributedCharacterIterator.Attribute attribute) {
			if (attributesAllowed != null
					&& !attributesAllowed.contains(attribute))
				return null;
			ArrayList ranges = (ArrayList) attrString.attributeMap
					.get(attribute);
			if (ranges == null)
				return null;
			return currentValue(ranges);
		}

		public Map getAttributes() {
			HashMap result = new HashMap(
					(attrString.attributeMap.size() * 4 / 3) + 1);
			Iterator it = attrString.attributeMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				if (attributesAllowed == null
						|| attributesAllowed.contains(entry.getKey())) {
					Object value = currentValue((ArrayList) entry.getValue());
					if (value != null)
						result.put(entry.getKey(), value);
				}
			}
			return result;
		}

		public int getRunLimit() {
			return getRunLimit(getAllAttributeKeys());
		}

		private int runLimit(ArrayList ranges) {
			int result = end;
			ListIterator it = ranges.listIterator(ranges.size());
			while (it.hasPrevious()) {
				Range range = (Range) it.previous();
				if (range.end <= begin)
					break;
				if (offset >= range.start && offset < range.end) {
					return inRange(range) ? range.end : result;
				} else if (offset >= range.end)
					break;
				result = range.start;
			}
			return result;
		}

		public int getRunLimit(AttributedCharacterIterator.Attribute attribute) {
			if (attributesAllowed != null
					&& !attributesAllowed.contains(attribute))
				return end;
			ArrayList ranges = (ArrayList) attrString.attributeMap
					.get(attribute);
			if (ranges == null)
				return end;
			return runLimit(ranges);
		}

		public int getRunLimit(Set attributes) {
			int limit = end;
			Iterator it = attributes.iterator();
			while (it.hasNext()) {
				AttributedCharacterIterator.Attribute attribute = (AttributedCharacterIterator.Attribute) it
						.next();
				int newLimit = getRunLimit(attribute);
				if (newLimit < limit)
					limit = newLimit;
			}
			return limit;
		}

		public int getRunStart() {
			return getRunStart(getAllAttributeKeys());
		}

		private int runStart(ArrayList ranges) {
			int result = begin;
			Iterator it = ranges.iterator();
			while (it.hasNext()) {
				Range range = (Range) it.next();
				if (range.start >= end)
					break;
				if (offset >= range.start && offset < range.end) {
					return inRange(range) ? range.start : result;
				} else if (offset < range.start)
					break;
				result = range.end;
			}
			return result;
		}

		public int getRunStart(AttributedCharacterIterator.Attribute attribute) {
			if (attributesAllowed != null
					&& !attributesAllowed.contains(attribute))
				return begin;
			ArrayList ranges = (ArrayList) attrString.attributeMap
					.get(attribute);
			if (ranges == null)
				return begin;
			return runStart(ranges);
		}

		public int getRunStart(Set attributes) {
			int start = begin;
			Iterator it = attributes.iterator();
			while (it.hasNext()) {
				AttributedCharacterIterator.Attribute attribute = (AttributedCharacterIterator.Attribute) it
						.next();
				int newStart = getRunStart(attribute);
				if (newStart > start)
					start = newStart;
			}
			return start;
		}

		/**
		 * Sets the current position to the end index - 1 and answers the
		 * character at the current position.
		 * 
		 * @return the character before the end index
		 */
		public char last() {
			if (begin == end)
				return DONE;
			offset = end - 1;
			return attrString.text.charAt(offset);
		}

		/**
		 * Increments the current index and returns the character at the new
		 * index.
		 * 
		 * @return the character at the next index, or DONE if the next index is
		 *         past the end
		 */
		public char next() {
			if (offset >= (end - 1)) {
				offset = end;
				return DONE;
			}
			return attrString.text.charAt(++offset);
		}

		/**
		 * Decrements the current index and returns the character at the new
		 * index.
		 * 
		 * @return the character at the previous index, or DONE if the previous
		 *         index is past the beginning
		 */
		public char previous() {
			if (offset == begin)
				return DONE;
			return attrString.text.charAt(--offset);
		}

		/**
		 * Sets the current index in the source String.
		 * 
		 * @return the character at the new index, or DONE if the index is past
		 *         the end
		 * 
		 * @exception IllegalArgumentException
		 *                when the new index is less than the begin index or
		 *                greater than the end index
		 */
		public char setIndex(int location) {
			if (location < begin || location > end)
				throw new IllegalArgumentException();
			offset = location;
			if (offset == end)
				return DONE;
			return attrString.text.charAt(offset);
		}
	}

	public AttributedString(AttributedCharacterIterator iterator) {
		StringBuffer buffer = new StringBuffer();
		while (iterator.current() != CharacterIterator.DONE) {
			buffer.append(iterator.current());
			iterator.next();
		}
		text = buffer.toString();
		Set attributes = iterator.getAllAttributeKeys();
		attributeMap = new HashMap((attributes.size() * 4 / 3) + 1);

		Iterator it = attributes.iterator();
		while (it.hasNext()) {
			AttributedCharacterIterator.Attribute attribute = (AttributedCharacterIterator.Attribute) it
					.next();
			iterator.setIndex(0);
			while (iterator.current() != CharacterIterator.DONE) {
				int start = iterator.getRunStart(attribute);
				int limit = iterator.getRunLimit(attribute);
				Object value = iterator.getAttribute(attribute);
				if (value != null)
					addAttribute(attribute, value, start, limit);
				iterator.setIndex(limit);
			}
		}
	}

	private AttributedString(AttributedCharacterIterator iterator, int start,
			int end, Set attributes) {
		if (start < iterator.getBeginIndex() || end > iterator.getEndIndex()
				|| start > end)
			throw new IllegalArgumentException();

		StringBuffer buffer = new StringBuffer();
		iterator.setIndex(start);
		while (iterator.getIndex() < end) {
			buffer.append(iterator.current());
			iterator.next();
		}
		text = buffer.toString();
		attributeMap = new HashMap((attributes.size() * 4 / 3) + 1);

		Iterator it = attributes.iterator();
		while (it.hasNext()) {
			AttributedCharacterIterator.Attribute attribute = (AttributedCharacterIterator.Attribute) it
					.next();
			iterator.setIndex(start);
			while (iterator.getIndex() < end) {
				Object value = iterator.getAttribute(attribute);
				int runStart = iterator.getRunStart(attribute);
				int limit = iterator.getRunLimit(attribute);
				if ((value instanceof Annotation && runStart >= start && limit <= end)
						|| (value != null && !(value instanceof Annotation))) {
					addAttribute(attribute, value, (runStart < start ? start
							: runStart)
							- start, (limit > end ? end : limit) - start);
				}
				iterator.setIndex(limit);
			}
		}
	}

	public AttributedString(AttributedCharacterIterator iterator, int start,
			int end) {
		this(iterator, start, end, iterator.getAllAttributeKeys());
	}

	public AttributedString(AttributedCharacterIterator iterator, int start,
			int end, AttributedCharacterIterator.Attribute[] attributes) {
		this(iterator, start, end, new HashSet(Arrays.asList(attributes)));
	}

	public AttributedString(String value) {
		if (value == null)
			throw new NullPointerException();
		text = value;
		attributeMap = new HashMap(11);
	}

	public AttributedString(String value, Map attributes) {
		if (value == null)
			throw new NullPointerException();
		if (value.length() == 0 && !attributes.isEmpty())
			throw new IllegalArgumentException(Msg.getString("K000e"));
		text = value;
		attributeMap = new HashMap((attributes.size() * 4 / 3) + 1);
		Iterator it = attributes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			ArrayList ranges = new ArrayList(1);
			ranges.add(new Range(0, text.length(), entry.getValue()));
			attributeMap.put(entry.getKey(), ranges);
		}
	}

	public void addAttribute(AttributedCharacterIterator.Attribute attribute,
			Object value) {
		if (text.length() == 0)
			throw new IllegalArgumentException();

		ArrayList ranges = (ArrayList) attributeMap.get(attribute);
		if (ranges == null) {
			ranges = new ArrayList(1);
			attributeMap.put(attribute, ranges);
		} else {
			ranges.clear();
		}
		ranges.add(new Range(0, text.length(), value));
	}

	public void addAttribute(AttributedCharacterIterator.Attribute attribute,
			Object value, int start, int end) {
		if (start < 0 || end > text.length() || start >= end)
			throw new IllegalArgumentException();

		ArrayList ranges = (ArrayList) attributeMap.get(attribute);
		if (ranges == null) {
			ranges = new ArrayList(1);
			ranges.add(new Range(start, end, value));
			attributeMap.put(attribute, ranges);
			return;
		}
		ListIterator it = ranges.listIterator();
		while (it.hasNext()) {
			Range range = (Range) it.next();
			if (end <= range.start) {
				it.previous();
				break;
			} else if (start < range.end
					|| (start == range.end && (value == null ? range.value == null
							: value.equals(range.value)))) {
				Range r1 = null, r3;
				it.remove();
				r1 = new Range(range.start, start, range.value);
				r3 = new Range(end, range.end, range.value);

				while (end > range.end && it.hasNext()) {
					range = (Range) it.next();
					if (end <= range.end) {
						if (end > range.start
								|| (end == range.start && (value == null ? range.value == null
										: value.equals(range.value)))) {
							it.remove();
							r3 = new Range(end, range.end, range.value);
							break;
						}
					} else
						it.remove();
				}

				if (value == null ? r1.value == null : value.equals(r1.value)) {
					if (value == null ? r3.value == null : value
							.equals(r3.value)) {
						it.add(new Range(r1.start < start ? r1.start : start,
								r3.end > end ? r3.end : end, r1.value));
					} else {
						it.add(new Range(r1.start < start ? r1.start : start,
								end, r1.value));
						if (r3.start < r3.end)
							it.add(r3);
					}
				} else {
					if (value == null ? r3.value == null : value
							.equals(r3.value)) {
						if (r1.start < r1.end)
							it.add(r1);
						it.add(new Range(start, r3.end > end ? r3.end : end,
								r3.value));
					} else {
						if (r1.start < r1.end)
							it.add(r1);
						it.add(new Range(start, end, value));
						if (r3.start < r3.end)
							it.add(r3);
					}
				}
				return;
			}
		}
		it.add(new Range(start, end, value));
	}

	public void addAttributes(Map attributes, int start, int end) {
		Iterator it = attributes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			addAttribute(
					(AttributedCharacterIterator.Attribute) entry.getKey(),
					entry.getValue(), start, end);
		}
	}

	public AttributedCharacterIterator getIterator() {
		return new AttributedIterator(this);
	}

	public AttributedCharacterIterator getIterator(
			AttributedCharacterIterator.Attribute[] attributes) {
		return new AttributedIterator(this, attributes, 0, text.length());
	}

	public AttributedCharacterIterator getIterator(
			AttributedCharacterIterator.Attribute[] attributes, int start,
			int end) {
		return new AttributedIterator(this, attributes, start, end);
	}

}
