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

package java.net;


import java.io.UnsupportedEncodingException;

/**
 * This class is used to encode a string using the format required by
 * <code>application/x-www-form-urlencoded</code> MIME content type.
 */
public class URLEncoder {
	static final String digits = "0123456789ABCDEF";

	/**
	 * Prevents this class from being instantiated.
	 * 
	 */
	private URLEncoder() {
	}

	/**
	 * This class contains a utility method for converting a string to the
	 * format required by the <code>application/x-www-form-urlencoded</code>
	 * MIME content type.
	 * <p>
	 * All characters except letters ('a'..'z', 'A'..'Z') and numbers ('0'..'9')
	 * and characters '.', '-', '*', '_' are converted into their hexidecimal
	 * value prepended by '%'.
	 * <p>
	 * For example: '#' -> %23
	 * <p>
	 * In addition, spaces are substituted by '+'
	 * 
	 * @return java.lang.String the string to be converted
	 * @param s
	 *            java.lang.String the converted string
	 * 
	 * @deprecated use URLEncoder#encode(String, String) instead
	 */
	public static String encode(String s) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
					|| (ch >= '0' && ch <= '9') || ".-*_".indexOf(ch) > -1)
				buf.append(ch);
			else if (ch == ' ')
				buf.append('+');
			else {
				byte[] bytes = new String(new char[] { ch }).getBytes();
				for (int j = 0; j < bytes.length; j++) {
					buf.append('%');
					buf.append(digits.charAt((bytes[j] & 0xf0) >> 4));
					buf.append(digits.charAt(bytes[j] & 0xf));
				}
			}
		}
		return buf.toString();
	}

	/**
	 * This class contains a utility method for converting a string to the
	 * format required by the <code>application/x-www-form-urlencoded</code>
	 * MIME content type.
	 * <p>
	 * All characters except letters ('a'..'z', 'A'..'Z') and numbers ('0'..'9')
	 * and characters '.', '-', '*', '_' are converted into their hexidecimal
	 * value prepended by '%'.
	 * <p>
	 * For example: '#' -> %23
	 * <p>
	 * In addition, spaces are substituted by '+'
	 * 
	 * @return java.lang.String the string to be converted
	 * @param s
	 *            java.lang.String the converted string
	 */
	public static String encode(String s, String enc)
			throws UnsupportedEncodingException {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
					|| (ch >= '0' && ch <= '9') || ".-*_".indexOf(ch) > -1)
				buf.append(ch);
			else if (ch == ' ')
				buf.append('+');
			else {
				byte[] bytes = new String(new char[] { ch }).getBytes(enc);
				for (int j = 0; j < bytes.length; j++) {
					buf.append('%');
					buf.append(digits.charAt((bytes[j] & 0xf0) >> 4));
					buf.append(digits.charAt(bytes[j] & 0xf));
				}
			}
		}
		return buf.toString();
	}
}
