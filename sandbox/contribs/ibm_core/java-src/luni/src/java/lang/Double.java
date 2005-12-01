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

package java.lang;


/**
 * Doubles are objects (non-base types) which represent double values.
 */
public final class Double extends Number implements Comparable {

	static final long serialVersionUID = -9172774392245257468L;

	/**
	 * The value which the receiver represents.
	 */
	final double value;

	/**
	 * Largest and smallest possible double values.
	 */
	public static final double MAX_VALUE = 1.79769313486231570e+308;

	public static final double MIN_VALUE = 5e-324;

	/* 4.94065645841246544e-324 gets rounded to 9.88131e-324 */

	/**
	 * A value which represents all invalid double results (NaN ==> Not a
	 * Number)
	 */
	public static final double NaN = 0.0 / 0.0;

	/**
	 * Values to represent infinite results
	 */
	public static final double POSITIVE_INFINITY = 1.0 / 0.0;

	public static final double NEGATIVE_INFINITY = -1.0 / 0.0;

	/**
	 * The java.lang.Class that represents this class.
	 */
	public static final Class TYPE = new double[0].getClass()
			.getComponentType();

	// Note: This can't be set to "double.class", since *that* is
	// defined to be "java.lang.Double.TYPE";

	/**
	 * Constructs a new instance of the receiver which represents the double
	 * valued argument.
	 * 
	 * @param value
	 *            the double to store in the new instance.
	 */
	public Double(double value) {
		this.value = value;
	}

	/**
	 * Constructs a new instance of this class given a string.
	 * 
	 * @param string
	 *            a string representation of a double quantity.
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a double quantity.
	 */
	public Double(String string) throws NumberFormatException {
		this(parseDouble(string));
	}

	/**
	 * Compares the receiver with the Double parameter.
	 * 
	 * @param object
	 *            the Double to compare to the receiver
	 * 
	 * @return Returns greater than zero when this.doubleValue() >
	 *         object.doubleValue(), zero when this.doubleValue() ==
	 *         object.doubleValue(), and less than zero when this.doubleValue() <
	 *         object.doubleValue()
	 */
	public int compareTo(Double object) {
		return value > object.value ? 1 : (value < object.value ? -1 : 0);
	}

	/**
	 * Compares the receiver with a Double parameter.
	 * 
	 * @param object
	 *            the Double to compare to the receiver
	 * 
	 * @return Returns greater than zero when this.doubleValue() >
	 *         object.doubleValue(), zero when this.doubleValue() ==
	 *         object.doubleValue(), and less than zero when this.doubleValue() <
	 *         object.doubleValue()
	 * 
	 * @throws ClassCastException
	 *             when object is not a Double
	 */
	public int compareTo(Object object) {
		return compareTo((Double) object);
	}

	/**
	 * Answers the byte value which the receiver represents
	 * 
	 * @return byte the value of the receiver.
	 */
	public byte byteValue() {
		return (byte) value;
	}

	/**
	 * Answers the binary representation of the argument, as a long.
	 * 
	 * @param value
	 *            The double value to convert
	 * @return the bits of the double.
	 */
	public static native long doubleToLongBits(double value);

	/**
	 * Answers the binary representation of the argument, as a long.
	 * 
	 * @param value
	 *            The double value to convert
	 * @return the bits of the double.
	 */
	public static native long doubleToRawLongBits(double value);

	/**
	 * Answers the receiver's value as a double.
	 * 
	 * @return the receiver's value
	 */
	public double doubleValue() {
		return value;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. For
	 * Doubles, the check verifies that the receiver's value's bit pattern
	 * matches the bit pattern of the argument, which must also be a Double.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		return (object == this)
				|| (object instanceof Double)
				&& (doubleToLongBits(this.value) == doubleToLongBits(((Double) object).value));
	}

	/**
	 * Answers the float value which the receiver represents
	 * 
	 * @return float the value of the receiver.
	 */
	public float floatValue() {
		return (float) value;
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>equals</code> must
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		long v = doubleToLongBits(value);
		return (int) (v ^ (v >>> 32));
	}

	/**
	 * Answers the receiver's value as an integer.
	 * 
	 * @return the receiver's value as an integer
	 */
	public int intValue() {
		return (int) value;
	}

	/**
	 * Answers true if the receiver represents an infinite quantity, and false
	 * otherwise.
	 * 
	 * @return <code>true</code> if the argument is positive or negative
	 *         infinity <code>false</code> if it is not an infinite value
	 */
	public boolean isInfinite() {
		return isInfinite(value);
	}

	/**
	 * Answers true if the argument represents an infinite quantity, and false
	 * otherwise.
	 * 
	 * @param d
	 *            value to check for infinitness.
	 * @return <code>true</code> if the argument is positive or negative
	 *         infinity <code>false</code> if it is not an infinite value
	 */
	public static boolean isInfinite(double d) {
		return (d == POSITIVE_INFINITY) || (d == NEGATIVE_INFINITY);
	}

	/**
	 * Answers true if the receiver does not represent a valid float quantity.
	 * 
	 * @return <code>true</code> if the argument is Not A Number
	 *         <code>false</code> if it is a (potentially infinite) float
	 *         number
	 */
	public boolean isNaN() {
		return isNaN(value);
	}

	/**
	 * Answers true if the argument does not represent a valid double quantity.
	 * 
	 * @param d
	 *            value to check for numberness.
	 * @return <code>true</code> if the argument is Not A Number
	 *         <code>false</code> if it is a (potentially infinite) double
	 *         number
	 */
	public static boolean isNaN(double d) {
		return d != d;
	}

	/**
	 * Answers a double built from the binary representation given in the
	 * argument.
	 * 
	 * @param bits
	 *            the bits of the double
	 * @return the double which matches the bits
	 */
	public static native double longBitsToDouble(long bits);

	/**
	 * Answers the long value which the receiver represents
	 * 
	 * @return long the value of the receiver.
	 */
	public long longValue() {
		return (long) value;
	}

	/**
	 * Answers the double which matches the passed in string.
	 * NumberFormatException is thrown if the string does not represent a valid
	 * double.
	 * 
	 * @param string
	 *            the value to convert
	 * @return a double which would print as the argument
	 */
	public static double parseDouble(String string)
			throws NumberFormatException {
		return com.ibm.oti.util.FloatingPointParser.parseDouble(string);
	}

	/**
	 * Answers the short value which the receiver represents
	 * 
	 * @return short the value of the receiver.
	 */
	public short shortValue() {
		return (short) value;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		return Double.toString(value);
	}

	/**
	 * Answers a string containing a printable representation of the argument.
	 * 
	 * @param d
	 *            the double to print
	 * @return a printable representation of the argument.
	 */
	public static String toString(double d) {
		return com.ibm.oti.util.NumberConverter.convert(d);
	}

	/**
	 * Answers the double which matches the passed in string.
	 * NumberFormatException is thrown if the string does not represent a valid
	 * double.
	 * 
	 * @param string
	 *            the value to convert
	 * @return a double which would print as the argument
	 */
	public static Double valueOf(String string) throws NumberFormatException {
		return new Double(parseDouble(string));
	}

	/**
	 * Compares the two doubles.
	 * 
	 * @param double1
	 *            the first value to compare
	 * @param double2
	 *            the second value to compare
	 * 
	 * @return Returns greater than zero when double1 > double2, zero when
	 *         double1 == double2, and less than zero when double1 < double2
	 */
	public static int compare(double double1, double double2) {
		return double1 > double2 ? 1 : (double1 < double2 ? -1 : 0);
	}
}
