/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.util;


/**
 * Used to parse a string and return either a single or double precision
 * floating point number.
 */
public final class FloatingPointParser {
    /*
     * All number with exponent larger than MAX_EXP can be treated as infinity.
     * All number with exponent smaller than MIN_EXP can be treated as zero.
     * Exponent is 10 based.
     * Eg. double's min value is 5e-324, so double "1e-325" should be parsed as 0.0 
     */
    private static final int FLOAT_MIN_EXP = -46;
    private static final int FLOAT_MAX_EXP = 38;
    private static final int DOUBLE_MIN_EXP = -324;
    private static final int DOUBLE_MAX_EXP = 308;

	private static final class StringExponentPair {
		String s;

		int e;

		boolean negative;

		StringExponentPair(String s, int e, boolean negative) {
			this.s = s;
			this.e = e;
			this.negative = negative;
		}
	}

	/**
	 * Takes a String and an integer exponent. The String should hold a positive
	 * integer value (or zero). The exponent will be used to calculate the
	 * floating point number by taking the positive integer the String
	 * represents and multiplying by 10 raised to the power of the
	 * exponent. Returns the closest double value to the real number
	 * 
	 * @param s
	 *            the String that will be parsed to a floating point
	 * @param e
	 *            an int represent the 10 to part
	 * @return the double closest to the real number
	 * 
	 * @exception NumberFormatException
	 *                if the String doesn't represent a positive integer value
	 */
	private static native double parseDblImpl(String s, int e);

	/**
	 * Takes a String and an integer exponent. The String should hold a positive
	 * integer value (or zero). The exponent will be used to calculate the
	 * floating point number by taking the positive integer the String
	 * represents and multiplying by 10 raised to the power of the
	 * exponent. Returns the closest float value to the real number
	 * 
	 * @param s
	 *            the String that will be parsed to a floating point
	 * @param e
	 *            an int represent the 10 to part
	 * @return the float closest to the real number
	 * 
	 * @exception NumberFormatException
	 *                if the String doesn't represent a positive integer value
	 */
	private static native float parseFltImpl(String s, int e);

	/**
	 * Takes a String and does some initial parsing. Should return a
	 * StringExponentPair containing a String with no leading or trailing white
	 * space and trailing zeroes eliminated. The exponent of the
	 * StringExponentPair will be used to calculate the floating point number by
	 * taking the positive integer the String represents and multiplying by 10
	 * raised to the power of the exponent.
	 * 
	 * @param s
	 *            the String that will be parsed to a floating point
	 * @param length
	 *            the length of s
	 * @return a StringExponentPair with necessary values
	 * 
	 * @exception NumberFormatException
	 *                if the String doesn't pass basic tests
	 */
	private static StringExponentPair initialParse(String s, int length) {
		boolean negative = false;
		char c;
		int start, end, decimal, shift;
		int e = 0;

		start = 0;
		if (length == 0)
			throw new NumberFormatException(s);

		c = s.charAt(length - 1);
		if (c == 'D' || c == 'd' || c == 'F' || c == 'f') {
			length--;
			if (length == 0)
				throw new NumberFormatException(s);
		}

		end = Math.max(s.indexOf('E'), s.indexOf('e'));
		if (end > -1) {
			if (end + 1 == length)
				throw new NumberFormatException(s);

                        int exponent_offset = end + 1;
                        if (s.charAt(exponent_offset) == '+') {
                                if (s.charAt(exponent_offset + 1) == '-') {
                                        throw new NumberFormatException(s);
                                }
                                exponent_offset++; // skip the plus sign
                                if (exponent_offset == length)
                                    throw new NumberFormatException(s);
                        }
            String strExp = s.substring(exponent_offset, length);
			try {
				e = Integer.parseInt(strExp);
            } catch (NumberFormatException ex) {
                // strExp is not empty, so there are 2 situations the exception be thrown
                // if the string is invalid we should throw exception, if the actual number
                // is out of the range of Integer, we can still parse the original number to
                // double or float
                char ch;
                for (int i = 0; i < strExp.length(); i++) {
                    ch = strExp.charAt(i);
                    if (ch < '0' || ch > '9') {
                        if (i == 0 && ch == '-')
                            continue;
                        // ex contains the exponent substring
                        // only so throw a new exception with
                        // the correct string
                        throw new NumberFormatException(s);
                    }
                }
                e = strExp.charAt(0) == '-' ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            }
		} else {
			end = length;
		}
		if (length == 0)
			throw new NumberFormatException(s);

		c = s.charAt(start);
		if (c == '-') {
			++start;
			--length;
			negative = true;
		} else if (c == '+') {
			++start;
			--length;
		}
		if (length == 0)
			throw new NumberFormatException(s);

		decimal = s.indexOf('.');
		if (decimal > -1) {
		    shift = end - decimal - 1;
		    //prevent e overflow, shift >= 0
		    if (e >= 0 || e - Integer.MIN_VALUE > shift) {
		        e -= shift;
		    }
			s = s.substring(start, decimal) + s.substring(decimal + 1, end);
		} else {
			s = s.substring(start, end);
		}

		if ((length = s.length()) == 0)
			throw new NumberFormatException();

		end = length;
		while (end > 1 && s.charAt(end - 1) == '0')
			--end;

		start = 0;
		while (start < end - 1 && s.charAt(start) == '0')
			start++;

		if (end != length || start != 0) {
		    shift = length - end;
		    if (e <= 0 || Integer.MAX_VALUE - e > shift) {
		        e += shift;
		    }
			s = s.substring(start, end);
		}

        // Trim the length of very small numbers, natives can only handle down
        // to E-309
        final int APPROX_MIN_MAGNITUDE = -359;
        final int MAX_DIGITS = 52;
        length = s.length();
        if (length > MAX_DIGITS && e < APPROX_MIN_MAGNITUDE) {
            int d = Math.min(APPROX_MIN_MAGNITUDE - e, length - 1);
            s = s.substring(0, length - d);
            e += d;
        }

		return new StringExponentPair(s, e, negative);
	}

	/*
	 * Assumes the string is trimmed.
	 */
	private static double parseDblName(String namedDouble, int length) {
		// Valid strings are only +Nan, NaN, -Nan, +Infinity, Infinity,
		// -Infinity.
		if ((length != 3) && (length != 4) && (length != 8) && (length != 9)) {
			throw new NumberFormatException();
		}

		boolean negative = false;
		int cmpstart = 0;
		switch (namedDouble.charAt(0)) {
		case '-':
			negative = true; // fall through
		case '+':
			cmpstart = 1;
		default:
		}

		if (namedDouble.regionMatches(false, cmpstart, "Infinity", 0, 8)) {
			return negative ? Double.NEGATIVE_INFINITY
					: Double.POSITIVE_INFINITY;
		}

		if (namedDouble.regionMatches(false, cmpstart, "NaN", 0, 3)) {
			return Double.NaN;
		}

		throw new NumberFormatException();
	}

	/*
	 * Assumes the string is trimmed.
	 */
	private static float parseFltName(String namedFloat, int length) {
		// Valid strings are only +Nan, NaN, -Nan, +Infinity, Infinity,
		// -Infinity.
		if ((length != 3) && (length != 4) && (length != 8) && (length != 9)) {
			throw new NumberFormatException();
		}

		boolean negative = false;
		int cmpstart = 0;
		switch (namedFloat.charAt(0)) {
		case '-':
			negative = true; // fall through
		case '+':
			cmpstart = 1;
		default:
		}

		if (namedFloat.regionMatches(false, cmpstart, "Infinity", 0, 8)) {
			return negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
		}

		if (namedFloat.regionMatches(false, cmpstart, "NaN", 0, 3)) {
			return Float.NaN;
		}

		throw new NumberFormatException();
	}

	/*
	 * Answers true if the string should be parsed as a hex encoding.
	 * Assumes the string is trimmed.
	 */
    private static boolean parseAsHex(String s) {
        int length = s.length();
        if (length < 2) {
            return false;
        }
        char first = s.charAt(0);
        char second = s.charAt(1);
        if (first == '+' || first == '-') {
            // Move along
            if (length < 3) {
                return false;
            }
            first = second;
            second = s.charAt(2);
        }
        return (first == '0') && (second == 'x' || second == 'X');
    }

	/**
	 * Returns the closest double value to the real number in the string.
	 * 
	 * @param s
	 *            the String that will be parsed to a floating point
	 * @return the double closest to the real number
	 * 
	 * @exception NumberFormatException
	 *                if the String doesn't represent a double
	 */
	public static double parseDouble(String s) {
		s = s.trim();
		int length = s.length();

		if (length == 0) {
			throw new NumberFormatException(s);
		}

		// See if this could be a named double
		char last = s.charAt(length - 1);
		if ((last == 'y') || (last == 'N')) {
			return parseDblName(s, length);
		}
        
        // See if it could be a hexadecimal representation
        if (parseAsHex(s)) {
            return HexStringParser.parseDouble(s);
        }
        
		StringExponentPair info = initialParse(s, length);

		// two kinds of situation will directly return 0.0
		// 1. info.s is 0
		// 2. actual exponent is less than Double.MIN_EXPONENT
		if ("0".equals(info.s) || (info.e + info.s.length() - 1 < DOUBLE_MIN_EXP)) {
		    return info.negative ? -0.0 : 0.0;
		}
		// if actual exponent is larger than Double.MAX_EXPONENT, return infinity
		// prevent overflow, check twice
		if ((info.e > DOUBLE_MAX_EXP) || (info.e + info.s.length() - 1 > DOUBLE_MAX_EXP)) {
		    return info.negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		}
		double result = parseDblImpl(info.s, info.e);
		if (info.negative)
			result = -result;

		return result;
	}

	/**
	 * Returns the closest float value to the real number in the string.
	 * 
	 * @param s
	 *            the String that will be parsed to a floating point
	 * @return the float closest to the real number
	 * 
	 * @exception NumberFormatException
	 *                if the String doesn't represent a float
	 */
	public static float parseFloat(String s) {
		s = s.trim();
		int length = s.length();

		if (length == 0) {
			throw new NumberFormatException(s);
		}

		// See if this could be a named float
		char last = s.charAt(length - 1);
		if ((last == 'y') || (last == 'N')) {
			return parseFltName(s, length);
		}
        
        // See if it could be a hexadecimal representation
        if (parseAsHex(s)) {
            return HexStringParser.parseFloat(s);
        }
        
		StringExponentPair info = initialParse(s, length);

        // two kinds of situation will directly return 0.0f
        // 1. info.s is 0
        // 2. actual exponent is less than Float.MIN_EXPONENT
        if ("0".equals(info.s) || (info.e + info.s.length() - 1 < FLOAT_MIN_EXP)) {
            return info.negative ? -0.0f : 0.0f;
        }
        // if actual exponent is larger than Float.MAX_EXPONENT, return infinity
        // prevent overflow, check twice
        if ((info.e > FLOAT_MAX_EXP) || (info.e + info.s.length() - 1 > FLOAT_MAX_EXP)) {
            return info.negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        }
		float result = parseFltImpl(info.s, info.e);
		if (info.negative)
			result = -result;

		return result;
	}
}
