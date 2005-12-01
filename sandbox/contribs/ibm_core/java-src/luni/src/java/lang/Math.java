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
 * Class math provides various floating point support routines and some standard
 * constants.
 */
public final class Math {

	/**
	 * Standard math constants.
	 */
	public static final double E = 2.718281828459045;

	public static final double PI = 3.141592653589793;

	private static java.util.Random random;

	/**
	 * Prevents this class from being instantiated.
	 */
	private Math() {
	}

	/**
	 * Answers the absolute value of the argument.
	 * 
	 * @param d
	 *            the value to be converted
	 * @return the argument if it is positive, otherwise the negation of the
	 *         argument.
	 */
	public static double abs(double d) {
		long bits = Double.doubleToLongBits(d);
		bits &= 0x7fffffffffffffffL;
		return Double.longBitsToDouble(bits);
	}

	/**
	 * Answers the absolute value of the argument.
	 * 
	 * @param f
	 *            the value to be converted
	 * @return the argument if it is positive, otherwise the negation of the
	 *         argument.
	 */
	public static float abs(float f) {
		int bits = Float.floatToIntBits(f);
		bits &= 0x7fffffff;
		return Float.intBitsToFloat(bits);
	}

	/**
	 * Answers the absolute value of the argument.
	 * 
	 * @param i
	 *            the value to be converted
	 * @return the argument if it is positive, otherwise the negation of the
	 *         argument.
	 */
	public static int abs(int i) {
		return i >= 0 ? i : -i;
	}

	/**
	 * Answers the absolute value of the argument.
	 * 
	 * @param l
	 *            the value to be converted
	 * @return the argument if it is positive, otherwise the negation of the
	 *         argument.
	 */
	public static long abs(long l) {
		return l >= 0 ? l : -l;
	}

	/**
	 * Answers the closest double approximation of the arc cosine of the
	 * argument
	 * 
	 * @param d
	 *            the value to compute acos of
	 * @return the arc cosine of the argument.
	 */
	public static native double acos(double d);

	/**
	 * Answers the closest double approximation of the arc sine of the argument
	 * 
	 * @param d
	 *            the value to compute asin of
	 * @return the arc sine of the argument.
	 */
	public static native double asin(double d);

	/**
	 * Answers the closest double approximation of the arc tangent of the
	 * argument
	 * 
	 * @param d
	 *            the value to compute atan of
	 * @return the arc tangent of the argument.
	 */
	public static native double atan(double d);

	/**
	 * Answers the closest double approximation of the arc tangent of the result
	 * of dividing the first argument by the second argument.
	 * 
	 * @param d1
	 *            the numerator of the value to compute atan of
	 * @param d2
	 *            the denominator of the value to compute atan of
	 * @return the arc tangent of d1/d2.
	 */
	public static native double atan2(double d1, double d2);

	/**
	 * Answers the double conversion of the most negative (i.e. closest to
	 * negative infinity) integer value which is greater than the argument.
	 * 
	 * @param d
	 *            the value to be converted
	 * @return the ceiling of the argument.
	 */
	public static native double ceil(double d);

	/**
	 * Answers the closest double approximation of the cosine of the argument
	 * 
	 * @param d
	 *            the value to compute cos of
	 * @return the cosine of the argument.
	 */
	public static native double cos(double d);

	/**
	 * Answers the closest double approximation of the raising "e" to the power
	 * of the argument
	 * 
	 * @param d
	 *            the value to compute the exponential of
	 * @return the exponential of the argument.
	 */
	public static native double exp(double d);

	/**
	 * Answers the double conversion of the most positive (i.e. closest to
	 * positive infinity) integer value which is less than the argument.
	 * 
	 * @param d
	 *            the value to be converted
	 * @return the ceiling of the argument.
	 */
	public static native double floor(double d);

	/**
	 * Answers the remainder of dividing the first argument by the second using
	 * the IEEE 754 rules.
	 * 
	 * @param d1
	 *            the numerator of the operation
	 * @param d2
	 *            the denominator of the operation
	 * @return the result of d1/d2.
	 */
	public static native double IEEEremainder(double d1, double d2);

	/**
	 * Answers the closest double approximation of the natural logarithm of the
	 * argument
	 * 
	 * @param d
	 *            the value to compute the log of
	 * @return the natural logarithm of the argument.
	 */
	public static native double log(double d);

	/**
	 * Answers the most positive (i.e. closest to positive infinity) of the two
	 * arguments.
	 * 
	 * @param d1
	 *            the first argument to check
	 * @param d2
	 *            the second argument
	 * @return the larger of d1 and d2.
	 */
	public static double max(double d1, double d2) {
		if (d1 > d2)
			return d1;
		if (d1 < d2)
			return d2;
		/* if either arg is NaN, return NaN */
		if (d1 != d2)
			return Double.NaN;
		/* max( +0.0,-0.0) == +0.0 */
		if (d1 == 0.0
				&& ((Double.doubleToLongBits(d1) & Double.doubleToLongBits(d2)) & 0x8000000000000000L) == 0)
			return 0.0;
		return d1;
	}

	/**
	 * Answers the most positive (i.e. closest to positive infinity) of the two
	 * arguments.
	 * 
	 * @param f1
	 *            the first argument to check
	 * @param f2
	 *            the second argument
	 * @return the larger of f1 and f2.
	 */
	public static float max(float f1, float f2) {
		if (f1 > f2)
			return f1;
		if (f1 < f2)
			return f2;
		/* if either arg is NaN, return NaN */
		if (f1 != f2)
			return Float.NaN;
		/* max( +0.0,-0.0) == +0.0 */
		if (f1 == 0.0f
				&& ((Float.floatToIntBits(f1) & Float.floatToIntBits(f2)) & 0x80000000) == 0)
			return 0.0f;
		return f1;
	}

	/**
	 * Answers the most positive (i.e. closest to positive infinity) of the two
	 * arguments.
	 * 
	 * @param i1
	 *            the first argument to check
	 * @param i2
	 *            the second argument
	 * @return the larger of i1 and i2.
	 */
	public static int max(int i1, int i2) {
		return i1 > i2 ? i1 : i2;
	}

	/**
	 * Answers the most positive (i.e. closest to positive infinity) of the two
	 * arguments.
	 * 
	 * @param l1
	 *            the first argument to check
	 * @param l2
	 *            the second argument
	 * @return the larger of l1 and l2.
	 */
	public static long max(long l1, long l2) {
		return l1 > l2 ? l1 : l2;
	}

	/**
	 * Answers the most negative (i.e. closest to negative infinity) of the two
	 * arguments.
	 * 
	 * @param d1
	 *            the first argument to check
	 * @param d2
	 *            the second argument
	 * @return the smaller of d1 and d2.
	 */
	public static double min(double d1, double d2) {
		if (d1 > d2)
			return d2;
		if (d1 < d2)
			return d1;
		/* if either arg is NaN, return NaN */
		if (d1 != d2)
			return Double.NaN;
		/* min( +0.0,-0.0) == -0.0 */
		if (d1 == 0.0
				&& ((Double.doubleToLongBits(d1) | Double.doubleToLongBits(d2)) & 0x8000000000000000l) != 0)
			return 0.0 * (-1.0);
		return d1;
	}

	/**
	 * Answers the most negative (i.e. closest to negative infinity) of the two
	 * arguments.
	 * 
	 * @param f1
	 *            the first argument to check
	 * @param f2
	 *            the second argument
	 * @return the smaller of f1 and f2.
	 */
	public static float min(float f1, float f2) {
		if (f1 > f2)
			return f2;
		if (f1 < f2)
			return f1;
		/* if either arg is NaN, return NaN */
		if (f1 != f2)
			return Float.NaN;
		/* min( +0.0,-0.0) == -0.0 */
		if (f1 == 0.0f
				&& ((Float.floatToIntBits(f1) | Float.floatToIntBits(f2)) & 0x80000000) != 0)
			return 0.0f * (-1.0f);
		return f1;
	}

	/**
	 * Answers the most negative (i.e. closest to negative infinity) of the two
	 * arguments.
	 * 
	 * @param i1
	 *            the first argument to check
	 * @param i2
	 *            the second argument
	 * @return the smaller of i1 and i2.
	 */
	public static int min(int i1, int i2) {
		return i1 < i2 ? i1 : i2;
	}

	/**
	 * Answers the most negative (i.e. closest to negative infinity) of the two
	 * arguments.
	 * 
	 * @param l1
	 *            the first argument to check
	 * @param l2
	 *            the second argument
	 * @return the smaller of l1 and l2.
	 */
	public static long min(long l1, long l2) {
		return l1 < l2 ? l1 : l2;
	}

	/**
	 * Answers the closest double approximation of the result of raising the
	 * first argument to the power of the second.
	 * 
	 * @param d1
	 *            the base of the operation.
	 * @param d2
	 *            the exponent of the operation.
	 * @return d1 to the power of d2
	 */
	public static native double pow(double d1, double d2);

	/**
	 * Answers the double conversion of the result of rounding the argument to
	 * an integer.
	 * 
	 * @param d
	 *            the value to be converted
	 * @return the closest integer to the argument (as a double).
	 */
	public static native double rint(double d);

	/**
	 * Answers the result of rounding the argument to an integer.
	 * 
	 * @param d
	 *            the value to be converted
	 * @return the closest integer to the argument.
	 */
	public static long round(double d) {
		// check for NaN
		if (d != d)
			return 0L;
		return (long) Math.floor(d + 0.5d);
	}

	/**
	 * Answers the result of rounding the argument to an integer.
	 * 
	 * @param f
	 *            the value to be converted
	 * @return the closest integer to the argument.
	 */
	public static int round(float f) {
		// check for NaN
		if (f != f)
			return 0;
		return (int) Math.floor(f + 0.5f);
	}

	/**
	 * Answers the closest double approximation of the sine of the argument
	 * 
	 * @param d
	 *            the value to compute sin of
	 * @return the sine of the argument.
	 */
	public static native double sin(double d);

	/**
	 * Answers the closest double approximation of the square root of the
	 * argument
	 * 
	 * @param d
	 *            the value to compute sqrt of
	 * @return the square root of the argument.
	 */
	public static native double sqrt(double d);

	/**
	 * Answers the closest double approximation of the tangent of the argument
	 * 
	 * @param d
	 *            the value to compute tan of
	 * @return the tangent of the argument.
	 */
	public static native double tan(double d);

	/**
	 * Returns a pseudo-random number between 0.0 and 1.0.
	 * 
	 * @return a pseudo-random number
	 */
	public static double random() {
		if (random == null) {
			random = new java.util.Random();
		}
		return random.nextDouble();
	}

	/**
	 * Returns the measure in radians of the supplied degree angle
	 * 
	 * @param angdeg
	 *            an angle in degrees
	 * @return the radian measure of the angle.
	 */
	public static double toRadians(double angdeg) {
		return angdeg / 180d * PI;
	}

	/**
	 * Returns the measure in degrees of the supplied radian angle
	 * 
	 * @param angrad
	 *            an angle in radians
	 * @return the degree measure of the angle.
	 */
	public static double toDegrees(double angrad) {
		return angrad * 180d / PI;
	}
}
