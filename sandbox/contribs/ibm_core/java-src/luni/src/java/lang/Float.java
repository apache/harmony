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
 * Floats are objects (non-base types) which represent float values.
 */
public final class Float extends Number implements Comparable {

	static final long serialVersionUID = -2671257302660747028L;

	/**
	 * The value which the receiver represents.
	 */
	final float value;

	/**
	 * Largest and smallest possible float values.
	 */
	public static final float MAX_VALUE = 3.40282346638528860e+38f;

	public static final float MIN_VALUE = 1.40129846432481707e-45f;

	/**
	 * A value which represents all invalid float results (NaN ==> Not a Number)
	 */
	public static final float NaN = 0.0f / 0.0f;

	/**
	 * Values to represent infinite results
	 */
	public static final float POSITIVE_INFINITY = 1.0f / 0.0f;

	public static final float NEGATIVE_INFINITY = -1.0f / 0.0f;

	/**
	 * The java.lang.Class that represents this class.
	 */
	public static final Class TYPE = new float[0].getClass().getComponentType();

	// Note: This can't be set to "float.class", since *that* is
	// defined to be "java.lang.Float.TYPE";

	/**
	 * Constructs a new instance of the receiver which represents the float
	 * valued argument.
	 * 
	 * @param value
	 *            the float to store in the new instance.
	 */
	public Float(float value) {
		this.value = value;
	}

	/**
	 * Constructs a new instance of the receiver which represents the double
	 * valued argument.
	 * 
	 * @param value
	 *            the double to store in the new instance.
	 */
	public Float(double value) {
		this.value = (float) value;
	}

	/**
	 * Constructs a new instance of this class given a string.
	 * 
	 * @param string
	 *            a string representation of a float quantity.
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a float quantity.
	 */
	public Float(String string) throws NumberFormatException {
		this(parseFloat(string));
	}

	/**
	 * Compares the receiver with the Float parameter.
	 * 
	 * @param object
	 *            the Float to compare to the receiver
	 * 
	 * @return Returns greater than zero when this.floatValue() >
	 *         object.floatValue(), zero when this.floatValue() ==
	 *         object.floatValue(), and less than zero when this.floatValue() <
	 *         object.floatValue()
	 */
	public int compareTo(Float object) {
		return value > object.value ? 1 : (value < object.value ? -1 : 0);
	}

	/**
	 * Compares the receiver with a Float parameter.
	 * 
	 * @param object
	 *            the Float to compare to the receiver
	 * 
	 * @return Returns greater than zero when this.floatValue() >
	 *         object.floatValue(), zero when this.floatValue() ==
	 *         object.floatValue(), and less than zero when this.floatValue() <
	 *         object.floatValue()
	 * 
	 * @throws ClassCastException
	 *             when object is not a Float
	 */
	public int compareTo(Object object) {
		return compareTo((Float) object);
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
	 * Answers the double value which the receiver represents
	 * 
	 * @return double the value of the receiver.
	 */
	public double doubleValue() {
		return value;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. For Floats,
	 * the check verifies that the receiver's value's bit pattern matches the
	 * bit pattern of the argument, which must also be a Float.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		return (object == this)
				|| (object instanceof Float)
				&& (floatToIntBits(this.value) == floatToIntBits(((Float) object).value));
	}

	/**
	 * Answers the binary representation of the argument, as an int.
	 * 
	 * @param value
	 *            The float value to convert
	 * @return the bits of the float.
	 */
	public static native int floatToIntBits(float value);

	/**
	 * Answers the binary representation of the argument, as an int.
	 * 
	 * @param value
	 *            The float value to convert
	 * @return the bits of the float.
	 */
	public static native int floatToRawIntBits(float value);

	/**
	 * Answers the receiver's value as a float.
	 * 
	 * @return the receiver's value
	 */
	public float floatValue() {
		return value;
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
		return floatToIntBits(value);
	}

	/**
	 * Answers a float built from the binary representation given in the
	 * argument.
	 * 
	 * @param bits
	 *            the bits of the float
	 * @return the float which matches the bits
	 */
	public static native float intBitsToFloat(int bits);

	/**
	 * Answers the int value which the receiver represents
	 * 
	 * @return int the value of the receiver.
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
	 * @param f
	 *            value to check for infinitness.
	 * @return <code>true</code> if the argument is positive or negative
	 *         infinity <code>false</code> if it is not an infinite value
	 */
	public static boolean isInfinite(float f) {
		return (f == POSITIVE_INFINITY) || (f == NEGATIVE_INFINITY);
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
	 * Answers true if the argument does not represent a valid float quantity.
	 * 
	 * @param f
	 *            value to check for numberness.
	 * @return <code>true</code> if the argument is Not A Number
	 *         <code>false</code> if it is a (potentially infinite) float
	 *         number
	 */
	public static boolean isNaN(float f) {
		return f != f;
	}

	/**
	 * Answers the long value which the receiver represents
	 * 
	 * @return long the value of the receiver.
	 */
	public long longValue() {
		return (long) value;
	}

	/**
	 * Answers the float which matches the passed in string.
	 * NumberFormatException is thrown if the string does not represent a valid
	 * float.
	 * 
	 * @param string
	 *            the value to convert
	 * @return a float which would print as the argument
	 */
	public static float parseFloat(String string) throws NumberFormatException {
		return com.ibm.oti.util.FloatingPointParser.parseFloat(string);
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
		return Float.toString(value);
	}

	/**
	 * Answers a string containing a printable representation of the argument.
	 * 
	 * @param f
	 *            the float to print
	 * @return a printable representation of the argument.
	 */
	public static String toString(float f) {
		return com.ibm.oti.util.NumberConverter.convert(f);
	}

	/**
	 * Answers the float which matches the passed in string.
	 * NumberFormatException is thrown if the string does not represent a valid
	 * float.
	 * 
	 * @param string
	 *            the value to convert
	 * @return a float which would print as the argument
	 */
	public static Float valueOf(String string) throws NumberFormatException {
		return new Float(parseFloat(string));
	}

	/**
	 * Compares the two floats.
	 * 
	 * @param float1
	 *            the first value to compare
	 * @param float2
	 *            the second value to compare
	 * 
	 * @return Returns greater than zero when float1 > float2, zero when float1 ==
	 *         float2, and less than zero when float1 < float2
	 */
	public static int compare(float float1, float float2) {
		return float1 > float2 ? 1 : (float1 < float2 ? -1 : 0);
	}
}
