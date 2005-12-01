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


import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.oti.util.Msg;

/**
 * Format is the abstract superclass of classes which format and parse objects
 * according to Locale specific rules.
 */
public abstract class Format implements Serializable, Cloneable {

	static final long serialVersionUID = -299282585814624189L;

	/**
	 * Constructs a new instance of Format.
	 * 
	 */
	public Format() {
	}

	/**
	 * Answers a copy of this Format.
	 * 
	 * @return a shallow copy of this Format
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	static ResourceBundle getBundle(final Locale locale) {
		return (ResourceBundle) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return ResourceBundle.getBundle(
								"com.ibm.oti.locale.Locale", locale);
					}
				});
	}

	String convertPattern(String template, String fromChars, String toChars,
			boolean check) {
		if (!check && fromChars.equals(toChars))
			return template;
		boolean quote = false;
		StringBuffer output = new StringBuffer();
		int length = template.length();
		for (int i = 0; i < length; i++) {
			int index;
			char next = template.charAt(i);
			if (next == '\'')
				quote = !quote;
			if (!quote && (index = fromChars.indexOf(next)) != -1)
				output.append(toChars.charAt(index));
			else if (check
					&& !quote
					&& ((next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z')))
				throw new IllegalArgumentException(Msg.getString("K001c", String.valueOf(next), template));
			else
				output.append(next);
		}
		if (quote)
			throw new IllegalArgumentException(Msg.getString("K0019"));
		return output.toString();
	}

	/**
	 * Formats the specified object using the rules of this Format.
	 * 
	 * 
	 * @param object
	 *            the object to format
	 * @return the formatted String
	 * 
	 * @exception IllegalArgumentException
	 *                when the object cannot be formatted by this Format
	 */
	public final String format(Object object) {
		return format(object, new StringBuffer(), new FieldPosition(0))
				.toString();
	}

	/**
	 * Formats the specified object into the specified StringBuffer using the
	 * rules of this Format. If the field specified by the FieldPosition is
	 * formatted, set the begin and end index of the formatted field in the
	 * FieldPosition.
	 * 
	 * @param object
	 *            the object to format
	 * @param buffer
	 *            the StringBuffer
	 * @param field
	 *            the FieldPosition
	 * @return the StringBuffer parameter <code>buffer</code>
	 * 
	 * @exception IllegalArgumentException
	 *                when the object cannot be formatted by this Format
	 */
	public abstract StringBuffer format(Object object, StringBuffer buffer,
			FieldPosition field);

	/**
	 * Formats the specified object using the rules of this format and returns
	 * an AttributedCharacterIterator with the formatted String and no
	 * attributes.
	 * <p>
	 * Subclasses should return an AttributedCharacterIterator with the
	 * appropriate attributes.
	 * 
	 * @param object
	 *            the object to format
	 * @return an AttributedCharacterIterator with the formatted object and
	 *         attributes
	 * 
	 * @exception IllegalArgumentException
	 *                when the object cannot be formatted by this Format
	 */
	public AttributedCharacterIterator formatToCharacterIterator(Object object) {
		return new AttributedString(format(object)).getIterator();
	}

	/**
	 * Parse the specified String using the rules of this Format.
	 * 
	 * @param string
	 *            the String to parse
	 * @return the object resulting from the parse
	 * 
	 * @exception ParseException
	 *                when an error occurs during parsing
	 */
	public Object parseObject(String string) throws ParseException {
		ParsePosition position = new ParsePosition(0);
		Object result = parseObject(string, position);
		if (position.getErrorIndex() != -1 || position.getIndex() == 0)
			throw new ParseException(null, position.getErrorIndex());
		return result;
	}

	/**
	 * Parse the specified String starting at the index specified by the
	 * ParsePosition. If the string is successfully parsed, the index of the
	 * ParsePosition is updated to the index following the parsed text.
	 * 
	 * @param string
	 *            the String to parse
	 * @param position
	 *            the ParsePosition, updated on return with the index following
	 *            the parsed text, or on error the index is unchanged and the
	 *            error index is set to the index where the error occurred
	 * @return the object resulting from the parse, or null if there is an error
	 */
	public abstract Object parseObject(String string, ParsePosition position);

	static boolean upTo(String string, ParsePosition position,
			StringBuffer buffer, char stop) {
		int index = position.getIndex(), length = string.length();
		boolean lastQuote = false, quote = false;
		while (index < length) {
			char ch = string.charAt(index++);
			if (ch == '\'') {
				if (lastQuote)
					buffer.append('\'');
				quote = !quote;
				lastQuote = true;
			} else if (ch == stop && !quote) {
				position.setIndex(index);
				return true;
			} else {
				lastQuote = false;
				buffer.append(ch);
			}
		}
		position.setIndex(index);
		return false;
	}

	static boolean upToWithQuotes(String string, ParsePosition position,
			StringBuffer buffer, char stop, char start) {
		int index = position.getIndex(), length = string.length(), count = 1;
		boolean quote = false;
		while (index < length) {
			char ch = string.charAt(index++);
			if (ch == '\'')
				quote = !quote;
			if (!quote) {
				if (ch == stop)
					count--;
				if (count == 0) {
					position.setIndex(index);
					return true;
				}
				if (ch == start)
					count++;
			}
			buffer.append(ch);
		}
		position.setIndex(index);
		return false;
	}

	/**
	 * This inner class is used to represent Format attributes in the
	 * AttributedCharacterIterator that formatToCharacterIterator() method
	 * returns in the Format subclasses.
	 */
	public static class Field extends AttributedCharacterIterator.Attribute {

		/**
		 * Constructs a new instance of Field with the given fieldName.
		 */
		protected Field(String fieldName) {
			super(fieldName);
		}
	}
}
