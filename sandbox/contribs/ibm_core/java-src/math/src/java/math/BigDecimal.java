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

package java.math;

/**
 * BigDecimal objects represent an arbitrary precisioned decimal Number. They
 * contain values that cannot be changed. Thus, most operations on the
 * BigDecimal object yield new instances of BigDecimal.
 * <p>
 * BigDecimal is respresented by an unscaled BigInteger value and an integer
 * representing the scale of the object. The scale of the BigDecimal is the
 * number of digits after the decimal point. Eg. 1.234 would have a scale of 3
 * and an unscaled value of 1234. Therefore, decimal representation of a
 * BigDecimal is BigIntegerValue/10^scale.
 * 
 * @see java.math.BigInteger
 */
public class BigDecimal extends Number implements Comparable {

	static final long serialVersionUID = 6108874887143696463L;

	/**
	 * Rounding mode Constants
	 */
	public final static int ROUND_UP = 0;

	public final static int ROUND_DOWN = 1;

	public final static int ROUND_CEILING = 2;

	public final static int ROUND_FLOOR = 3;

	public final static int ROUND_HALF_UP = 4;

	public final static int ROUND_HALF_DOWN = 5;

	public final static int ROUND_HALF_EVEN = 6;

	public final static int ROUND_UNNECESSARY = 7;

	/**
	 * Constructs a BigDecimal with unscaled value initialized as bval and scale
	 * as 0.
	 */
	public BigDecimal(BigInteger bval) {
	}

	/**
	 * Constructs a BigDecimal with unscaled value initialized as bval and scale
	 * as scale from the argument.
	 */
	public BigDecimal(BigInteger bval, int sc) {
	}

	/**
	 * Constructs a BigDecimal with a double value as an arugment.
	 * 
	 * @exception NumberFormatException
	 *                If the is Infinity, Negative Infinity or NaN.
	 */
	public BigDecimal(double bval) {
	}

	/**
	 * Constructs a BigDecimal from the strong which can only contan digits of
	 * 0-9, a decimal point and a negative sign.
	 * 
	 * @exception NumberFormatException
	 *                If the argument contained characters other than digits.
	 */
	public BigDecimal(String bval) {
	}

	/**
	 * Answers the absolute value of this BigDecimal.
	 * 
	 * @return BigDecimal absolute value of the receiver.
	 */
	public BigDecimal abs() {
		return null;
	}

	/**
	 * Answers the sum of the receiver and argument.
	 * 
	 * @return BigDecimal The sum of adding two BigDecimal.
	 */
	public BigDecimal add(BigDecimal bval) {
		return null;
	}

	/**
	 * Compares an receiver to the argument Object.
	 * 
	 * @return int 0 - equal; 1 - this > val; -1 - this < val
	 * @exception ClassCastException
	 *                if the argument is not of type BigDecimal
	 */
	public int compareTo(Object o) {
		return 0;
	}

	/**
	 * Compares the receiver BigDecimal and argument BigDecimal e.x 1.00 & 1.0
	 * will return 0 in compareTo.
	 * 
	 * @return int 0 - equal; 1 - this > val; -1 - this < val.
	 */
	public int compareTo(BigDecimal bval) {
		return 0;
	}

	/**
	 * Answers the result of (this / val).
	 * 
	 * @return BigDecimal result of this/val.
	 */
	public BigDecimal divide(BigDecimal bval, int roundingMode) {
		return null;
	}

	/**
	 * Answers the result of (this / val) and whose scale is specified.
	 * 
	 * @return BigDecimal result of this/val.
	 * @exception ArithmeticException
	 *                division by zero.
	 * @exception IllegalArgumentException
	 *                roundingMode is not valid.
	 */
	public BigDecimal divide(BigDecimal bval, int bscale, int roundingMode) {
		return null;
	}

	/**
	 * Converts this BigDecimal to a double. If magnitude of the BigDecimal
	 * value is larger than what can be represented by a double, either Infinity
	 * or -Infinity is returned.
	 * 
	 * @return double the value of the receiver.
	 */
	public double doubleValue() {
		return 0;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. The
	 * implementation in Object answers true only if the argument is the exact
	 * same object as the receiver (==).
	 * 
	 * @param o
	 *            Object the object to compare with this object.
	 * @return boolean <code>true</code> if the object is the same as this
	 *         object <code>false</code> if it is different from this object.
	 * @see hashCode
	 */
	public boolean equals(Object obj) {
		return false;
	}

	/**
	 * Converts this BigDecimal to a float.If magnitude of the BigDecimal value
	 * is larger than what can be represented by a float, either Infinity or
	 * -Infinity is returned.
	 * 
	 * @return float the value of the receiver.
	 */
	public float floatValue() {
		return 0;
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>.equals</code> must
	 * answer the same value for this method.
	 * 
	 * @return int the receiver's hash.
	 * 
	 * @see #equals(Object)
	 */
	public int hashCode() {
		return 0;
	}

	/**
	 * Converts this BigDecimal to an int.
	 * 
	 * @return int the value of the receiver.
	 */
	public int intValue() {
		return 0;
	}

	/**
	 * Converts this BigDecimal to a long.
	 * 
	 * @return long long representation of the receiver.
	 */
	public long longValue() {
		return 0;
	}

	/**
	 * Answers the max value between the receiver and this BigDecimal.
	 * 
	 * @return BigDecimal max BigDecimal.
	 */
	public BigDecimal max(BigDecimal bval) {
		return null;
	}

	/**
	 * Answers the min value between the receiver and argument.
	 * 
	 * @return BigDecimal max BigDecimal.
	 */
	public BigDecimal min(BigDecimal bval) {
		return null;
	}

	/**
	 * Moves the decimal point of this BigDecimal n places to the left.
	 * 
	 * @return BigDecimal new BigDecimal with decimal moved n places to the
	 *         left.
	 */
	public BigDecimal movePointLeft(int n) {
		return null;
	}

	/**
	 * Moves the decimal point of this BigDecimal n places to the right.
	 * 
	 * @return BigDecimal new BigDecimal with decimal moved n places to the
	 *         right.
	 */
	public BigDecimal movePointRight(int n) {
		return null;
	}

	/**
	 * Answers the multiplication result of the receiver and argument.
	 * 
	 * @return BigDecimal result of multiplying two bigDecimals.
	 */
	public BigDecimal multiply(BigDecimal bval) {
		return null;
	}

	/**
	 * Negates this BigDecimal value.
	 * 
	 * @return BigDecimal new BigDecimal with value negated.
	 */
	public BigDecimal negate() {
		return null;
	}

	/**
	 * Returns the scale of this BigDecimal.
	 * 
	 * @return int scale value.
	 */
	public int scale() {
		return 0;
	}

	/**
	 * Sets the scale of this BigDecimal.
	 * 
	 * @return BigDecimal a BigDecimal with the same value, but specified scale.
	 */
	public BigDecimal setScale(int newScale) {
		return null;
	}

	/**
	 * Sets the scale of this BigDecimal. The unscaled value is determined by
	 * the rounding Mode
	 * 
	 * @return BigDecimal a BigDecimal with the same value, but specified cale.
	 * @exception ArithmeticException
	 *                rounding mode must be specified if lose of precision due
	 *                to setting scale.
	 * @exception IllegalArgumentException
	 *                invalid rounding mode
	 */
	public BigDecimal setScale(int newScale, int roundingMode) {
		return null;
	}

	/**
	 * Answers the signum function of this instance.
	 * 
	 * @return int -1, 0, or 1 if the receiver is negative, zero, or positive.
	 */
	public int signum() {
		return 0;
	}

	/**
	 * Answers the subtract result of the receiver and argument.
	 * 
	 * @return BigDecimal The result of adding two BigDecimal.
	 */
	public BigDecimal subtract(BigDecimal bval) {
		return null;
	}

	/**
	 * Converts this to a BigInteger.
	 * 
	 * @return BigInteger BigDecimal equivalent of bigInteger.
	 */
	public BigInteger toBigInteger() {
		return null;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return String a printable representation for the receiver.
	 */
	public String toString() {
		return null;
	}

	/**
	 * Returns an unscaled value of this BigDecimal.
	 * 
	 * @return BigInteger The unscaled value.
	 */
	public BigInteger unscaledValue() {
		return null;
	}

	/**
	 * Translate long value into a BigDecimal with scale of zero.
	 * 
	 * @return BigDecimal BigDecimal equivalence of a long value.
	 */
	public static BigDecimal valueOf(long bval) {
		return null;
	}

	/**
	 * Translate long unscaled value into a BigDecimal specified by the scale.
	 * 
	 * @return BigDecimal BigDecimal equalvalence of a long value.
	 * @exception NumberFormatException
	 *                the scale value is < 0;
	 */
	public static BigDecimal valueOf(long bval, int scale) {
		return null;
	}
}
