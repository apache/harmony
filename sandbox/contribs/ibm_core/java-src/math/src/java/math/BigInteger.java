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

import java.util.Random;


/**
 * BigInteger objects represent arbitrary precision decimal integers. They
 * contain values that cannot be changed. Thus, most operations on the
 * BigInteger objects yield new instances of BigInteger.
 */
public class BigInteger extends Number implements Comparable {
	// Version ID indicating compatability with standard.
	static final long serialVersionUID = -8287574255936472291L;

	/**
	 * Constant: 0 as represented by a BigInteger.
	 */
	public final static BigInteger ZERO = null;

	/**
	 * Constant: 1 as represented by a BigInteger.
	 */
	public final static BigInteger ONE = null;

	/**
	 * Answers a new instance of this class whose value is greater than zero,
	 * length is the given number of bits, and whose likeyhood of being prime is
	 * not less than 2 ^ -100.
	 * 
	 * @param bitLength
	 *            the number of bits contained in the returned instance.
	 * @param rnd
	 *            a random source of bits for selection of the probable prime.
	 * @return the probable prime number.
	 */
	public static BigInteger probablePrime(int bitLength, Random rnd) {
		return null;
	}

	/**
	 * Constructs a new instance of this class of the specified length, whose
	 * content is produced by aquiring random bits from the specified random
	 * number generator.
	 * 
	 * @param bitLength
	 *            int the number of bits to have in the result.
	 * @param rnd
	 *            Random the generator to produce the bits.
	 */
	public BigInteger(int bitLength, Random rnd) {
	}

	/**
	 * Constructs a new instance of this class of the specified length, whose
	 * content is produced by aquiring random bits from the specified random
	 * number generator. The result is guaranteed to be prime up to the given
	 * degree of certainty.
	 * 
	 * @param bitLength
	 *            int the number of bits to have in the result.
	 * @param certainty
	 *            int the degree of certainty required that the result is prime.
	 * @param rnd
	 *            Random the generator to produce the bits.
	 */
	public BigInteger(int bitLength, int certainty, Random rnd) {
	}

	/**
	 * Constructs a new instance of this class given an array containing bytes
	 * representing the bit pattern for the answer.
	 * 
	 * @param bytes
	 *            byte[] the bits of the value of the new instance.
	 */
	public BigInteger(byte[] bytes) {
	}

	/**
	 * Constructs a new instance of this class given an array containing bytes
	 * representing the bit pattern for the answer, and a sign flag.
	 * 
	 * @param sign
	 *            int the sign of the result.
	 * @param bytes
	 *            byte[] the bits of the value of the new instance.
	 */
	public BigInteger(int sign, byte[] bytes) {
	}

	/**
	 * Answers an array of bytes containing the value of the receiver in the
	 * same format used by the matching constructor.
	 * 
	 * @return byte[] the bits of the value of the receiver.
	 */
	public byte[] toByteArray() {
		return null;
	}

	/**
	 * Answers true if the receiver is probably prime to the given degree of
	 * certainty.
	 * 
	 * @param certainty
	 *            int the degree of certainty required.
	 * @return boolean true if the receiver is prime and false otherwise.
	 */
	public boolean isProbablePrime(int certainty) {
		return false;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. In this
	 * case the argument must also be a BigInteger which represents the same
	 * number
	 * 
	 * @param o
	 *            Object the object to compare with this object.
	 * @return boolean <code>true</code> if the object is the same as this
	 *         object <code>false</code> if it is different from this object.
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		return false;
	}

	/**
	 * Answers an integer indicating the relative positions of the receiver and
	 * the argument in the natural order of elements of the receiver's class.
	 * 
	 * @return int which should be <0 if the receiver should sort before the
	 *         argument, 0 if the receiver should sort in the same position as
	 *         the argument, and >0 if the receiver should sort after the
	 *         argument.
	 * @param val
	 *            BigInteger an object to compare the receiver to
	 * @exception ClassCastException
	 *                if the argument can not be converted into something
	 *                comparable with the receiver.
	 */
	public int compareTo(BigInteger val) {
		return 0;
	}

	/**
	 * Answers an integer indicating the relative positions of the receiver and
	 * the argument in the natural order of elements of the receiver's class.
	 * 
	 * @return int which should be <0 if the receiver should sort before the
	 *         argument, 0 if the receiver should sort in the same position as
	 *         the argument, and >0 if the receiver should sort after the
	 *         argument.
	 * @param o
	 *            Object an object to compare the receiver to
	 * @exception ClassCastException
	 *                if the argument can not be converted into something
	 *                comparable with the receiver.
	 */
	public int compareTo(Object o) {
		return 0;
	}

	/**
	 * Answers the int value which the receiver represents
	 * 
	 * @return int the value of the receiver.
	 */
	public int intValue() {
		return 0;
	}

	/**
	 * Answers the long value which the receiver represents
	 * 
	 * @return long the value of the receiver.
	 */
	public long longValue() {
		return 0;
	}

	/**
	 * Answers a BigInteger with the same value as val
	 * 
	 * @return BigInteger (BigInteger) val
	 */
	public static BigInteger valueOf(long val) {
		return null;
	}

	/**
	 * Answers the sum of the receiver and a BigInteger
	 * 
	 * @param val
	 *            a BigInteger to add
	 * 
	 * @return BigInteger this + val
	 */
	public BigInteger add(BigInteger val) {
		return null;
	}

	/**
	 * Answers the negative of the receiver
	 * 
	 * @return BigInteger (-1) * this
	 */
	public BigInteger negate() {
		return null;
	}

	/**
	 * Answers the sign of the receiver
	 * 
	 * @return BigInteger -1, 0, or 1 if the receiver is negative, zero, or
	 *         positive
	 */
	public int signum() {
		return 0;
	}

	/**
	 * Answers the absolute value of the receiver
	 * 
	 * @return BigInteger absolute value of the receiver
	 */
	public BigInteger abs() {
		return null;
	}

	/**
	 * Answers the receiver to the power of exponent.
	 * 
	 * @exception ArithmeticException
	 *                if the exponent is negative.
	 * 
	 * @return BigInteger this ^ exponent
	 */
	public BigInteger pow(int exponent) {
		return null;
	}

	/**
	 * Answers the receiver to the power of exponent modulo a BigInteger
	 * 
	 * @exception ArithmeticException
	 *                modulo is <= 0
	 * 
	 * @return BigInteger this ^ exponent (mod modulo)
	 */
	public BigInteger modPow(BigInteger exponent, BigInteger modulo) {
		return null;
	}

	/**
	 * Answers the greatest common divisor of abs(this) and abs(val), zero if
	 * this==val==0
	 * 
	 * @return BigInteger gcd(abs(this), abs(val))
	 */
	public BigInteger gcd(BigInteger val) {
		return null;
	}

	/**
	 * Answers the inverse of the receiver modulo a BigInteger, if it exists.
	 * 
	 * @param modulo
	 *            BigInteger a BigInteger to divide
	 * @return BigInteger this^(-1) (mod modulo)
	 * 
	 * @exception ArithmeticException
	 *                if modulo is <= 0, or gcd(this,modulo) != 1
	 */
	public BigInteger modInverse(BigInteger modulo) {
		return null;
	}

	/**
	 * Answers the index of the lowest set bit in the receiver, or -1 if no bits
	 * are set.
	 * 
	 * @return BigInteger the bit index of the least significant set bit in the
	 *         receiver.
	 */
	public int getLowestSetBit() {
		return 0;
	}

	/**
	 * Answers a BigInteger with the value of the reciever divided by
	 * 2^shiftval.
	 * 
	 * @param shiftval
	 *            int the amount to shift the receiver.
	 * @return BigInteger this >> val
	 */
	public BigInteger shiftRight(int shiftval) {
		return null;
	}

	/**
	 * Answers a BigInteger with the value of the reciever multiplied by
	 * 2^shiftval.
	 * 
	 * @param shiftval
	 *            int the amount to shift the receiver.
	 * @return BigInteger this << val
	 */
	public BigInteger shiftLeft(int shiftval) {
		return null;
	}

	/**
	 * Answers the difference of the receiver and a BigInteger.
	 * 
	 * @param val
	 *            BigInteger the value to subtract
	 * @return BigInteger this - val
	 */
	public BigInteger subtract(BigInteger val) {
		return null;
	}

	/**
	 * Answers the product of the receiver and a BigInteger.
	 * 
	 * @param val
	 *            BigInteger the value to multiply
	 * @return BigInteger this * val
	 */
	public BigInteger multiply(BigInteger val) {
		return null;
	}

	/**
	 * Answers the quotient of the receiver and a BigInteger.
	 * 
	 * @param val
	 *            BigInteger the value to divide
	 * @return BigInteger this / val
	 * 
	 * @exception ArithmeticException
	 *                if val is zero.
	 */
	public BigInteger divide(BigInteger val) {
		return null;
	}

	/**
	 * Answers the remainder of the receiver divided by a BigInteger
	 * 
	 * @param val
	 *            a BigInteger to divide
	 * 
	 * @exception ArithmeticException
	 *                if val is zero
	 * 
	 * @return BigInteger this % val
	 */
	public BigInteger remainder(BigInteger val) {
		return null;
	}

	/**
	 * Answers the remainder of the receiver modulo a BigInteger (a positive
	 * value).
	 * 
	 * @param val
	 *            the value to divide
	 * @return BigInteger this (mod val)
	 * 
	 * @exception ArithmeticException
	 *                if val is zero
	 */
	public BigInteger mod(BigInteger val) {
		return null;
	}

	/**
	 * Answers the quotient and remainder of the receiver divided by a
	 * BigInteger.
	 * 
	 * @param val
	 *            BigInteger the value to divide.
	 * @return BigInteger[2] {this / val, this % val )}
	 * 
	 * @exception ArithmeticException
	 *                if val is zero.
	 */
	public BigInteger[] divideAndRemainder(BigInteger val) {
		return null;
	}

	/**
	 * Constructs a new instance of this class given a string containing a
	 * representation of a decimal number.
	 * 
	 * @param val
	 *            String the decimal digits of the answer.
	 */
	public BigInteger(String val) {
	}

	/**
	 * Constructs a new instance of this class given a string containing digits
	 * in the specified radix.
	 * 
	 * @param val
	 *            String the digits of the answer.
	 * @param radix
	 *            int the radix to use for conversion.
	 */
	public BigInteger(String val, int radix) {
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver. In this case, a string of decimal digits.
	 * 
	 * @return String a printable representation for the receiver.
	 */
	public String toString() {
		return this.toString(10);
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver as a sequence of digits in the specified radix.
	 * 
	 * @return String a printable representation for the receiver.
	 */
	public String toString(int radix) {
		return null;
	}

	/**
	 * Answers the most positive of either the receiver or the argument.
	 * 
	 * @param val
	 *            BigInteger the value to compare.
	 * @return BigInteger the larger value.
	 */
	public BigInteger max(BigInteger val) {
		return null;
	}

	/**
	 * Answers the most negative of either the receiver or the argument.
	 * 
	 * @param val
	 *            BigInteger the value to compare.
	 * @return BigInteger the smaller value.
	 */
	public BigInteger min(BigInteger val) {
		return null;
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>.equals</code> must
	 * answer the same value for this method.
	 * 
	 * @return int the receiver's hash.
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		return 0;
	}

	/**
	 * Answers true if the specified bit is set in the receiver.
	 * 
	 * @param n
	 *            int the bit to check.
	 * @return boolean if the specified bit is set.
	 */
	public boolean testBit(int n) {
		return false;
	}

	/**
	 * Sets the specified bit in the receiver.
	 * 
	 * @param n
	 *            int the bit to set.
	 */
	public BigInteger setBit(int n) {
		return null;
	}

	/**
	 * Unsets the specified bit in the receiver.
	 * 
	 * @param n
	 *            int the bit to clear.
	 */
	public BigInteger clearBit(int n) {
		return null;
	}

	/**
	 * Toggles the specified bit in the receiver.
	 * 
	 * @param n
	 *            int the bit to flip.
	 */
	public BigInteger flipBit(int n) {
		return null;
	}

	/**
	 * Answers the bitwise AND of the receiver and the argument.
	 * 
	 * @param val
	 *            BigInteger the value to AND.
	 * @return BigInteger this & val
	 */
	public BigInteger and(BigInteger val) {
		return null;
	}

	/**
	 * Answers the bitwise OR of the receiver and the argument.
	 * 
	 * @param val
	 *            BigInteger the value to OR.
	 * @return BigInteger this | val
	 */
	public BigInteger or(BigInteger val) {
		return null;
	}

	/**
	 * Answers the bitwise XOR of the receiver and the argument.
	 * 
	 * @param val
	 *            BigInteger the value to XOR.
	 * @return BigInteger this XOR val
	 */
	public BigInteger xor(BigInteger val) {
		return null;
	}

	/**
	 * Answers the bitwise negation of the receiver.
	 * 
	 * @return BigInteger NOT(this)
	 */
	public BigInteger not() {
		return null;
	}

	/**
	 * Answers the bitwise NAND of the receiver and the argument.
	 * 
	 * @param val
	 *            BigInteger the value to NAND.
	 * @return BigInteger this & NOT(val)
	 */
	public BigInteger andNot(BigInteger val) {
		return null;
	}

	/**
	 * Answers the length in bits of the receiver.
	 * 
	 * @return int the receiver's bit length.
	 */
	public int bitLength() {
		return 0;
	}

	/**
	 * Answers the number of set bits in the receiver.
	 * 
	 * @return int the receiver's bit count.
	 */
	public int bitCount() {
		return 0;
	}

	/**
	 * Answers the double value which the receiver represents
	 * 
	 * @return double the value of the receiver.
	 */
	public double doubleValue() {
		return 0;
	}

	/**
	 * Answers the float value which the receiver represents
	 * 
	 * @return float the value of the receiver.
	 */
	public float floatValue() {
		return 0;
	}
}
