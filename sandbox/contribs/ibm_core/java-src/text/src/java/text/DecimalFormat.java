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


import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Currency;
import java.util.Locale;
import java.util.Vector;

import com.ibm.oti.util.Msg;

/**
 * DecimalFormat is used to format and parse numbers, both integers and
 * fractions, based on a pattern. The pattern characters used can be either
 * localized or non-localized.
 */
public class DecimalFormat extends NumberFormat {

	static final long serialVersionUID = 864413376551465018L;

	private static final String patternChars = "0#.,;%\u2030E";

	private static final char CURRENCY_SYMBOL = '\u00a4';

	private static final int DOUBLE_INTEGER_DIGITS = 309;

	private static final int DOUBLE_FRACTION_DIGITS = 340;

	private byte groupingSize = 3;

	private int multiplier = 1;

	private String positivePrefix = "", positiveSuffix = "",
			negativePrefix = "-", negativeSuffix = "";

	private String posPrefixPattern, posSuffixPattern, negPrefixPattern,
			negSuffixPattern;

	private boolean decimalSeparatorAlwaysShown = false;

	transient private boolean posPrefixMonetary = false,
			posSuffixMonetary = false, negPrefixMonetary = false,
			negSuffixMonetary = false;

	private DecimalFormatSymbols symbols;

	private boolean useExponentialNotation = false;

	private byte minExponentDigits;

	private int serialVersionOnStream = 2;

	transient private char zero;

	private static final double log10 = Math.log(10);

	/**
	 * Constructs a new DecimalFormat for formatting and parsing numbers for the
	 * default Locale.
	 */
	public DecimalFormat() {
		this(getPattern(Locale.getDefault(), "Number"));
	}

	/**
	 * Constructs a new DecimalFormat using the specified non-localized pattern
	 * and the DecimalFormatSymbols for the default Locale.
	 * 
	 * @param pattern
	 *            the non-localized pattern
	 * 
	 * @exception IllegalArgumentException
	 *                when the pattern cannot be parsed
	 */
	public DecimalFormat(String pattern) {
		symbols = new DecimalFormatSymbols();
		zero = symbols.getZeroDigit();
		applyPattern(pattern);
	}

	/**
	 * Constructs a new DecimalFormat using the specified non-localized pattern
	 * and DecimalFormatSymbols.
	 * 
	 * @param pattern
	 *            the non-localized pattern
	 * @param value
	 *            the DecimalFormatSymbols
	 * 
	 * @exception IllegalArgumentException
	 *                when the pattern cannot be parsed
	 */
	public DecimalFormat(String pattern, DecimalFormatSymbols value) {
		symbols = (DecimalFormatSymbols) value.clone();
		zero = symbols.getZeroDigit();
		applyPattern(pattern);
	}

	/**
	 * Changes the pattern of this DecimalFormat to the specified pattern which
	 * uses localized pattern characters.
	 * 
	 * @param pattern
	 *            the localized pattern
	 * 
	 * @exception IllegalArgumentException
	 *                when the pattern cannot be parsed
	 */
	public void applyLocalizedPattern(String pattern) {
		applyPattern(convertPattern(pattern, symbols.getLocalPatternChars(),
				patternChars, false));
	}

	/**
	 * Changes the pattern of this SimpleDateFormat to the specified pattern
	 * which uses non-localized pattern characters.
	 * 
	 * @param pattern
	 *            the non-localized pattern
	 * 
	 * @exception IllegalArgumentException
	 *                when the pattern cannot be parsed
	 */
	public void applyPattern(String pattern) {
		if (pattern.length() == 0)
			return;
		int next, grouping = 0, intCount = 0, minInt = 0, minFraction = 0, maxFraction = 0, localMultiplier = 1, minExponent = 0;
		boolean inPrefix = true, countGrouping = false, exponent = false, fraction = false, negative = false, inSuffix = false;
		StringBuffer buffer = new StringBuffer();
		String prefix = "", suffix = "", negPrefix = "", negSuffix = "", format = null;
		boolean quote = false, lastQuote = false;
		final int patternLength = pattern.length();
		for (int i = 0; i < patternLength; i++) {
			next = (pattern.charAt(i));
			if (!quote
					&& ("0#,.".indexOf(next) != -1 || (!exponent && !inPrefix
							&& !inSuffix && next == 'E'))) {
				if (inPrefix) {
					if (negative)
						negPrefix = buffer.toString();
					else
						prefix = buffer.toString();
					buffer.setLength(0);
					inPrefix = false;
				}
				if (inSuffix)
					throw new IllegalArgumentException(Msg.getString("K0014",
							String.valueOf((char) next), pattern));
				buffer.append((char) next);
				if (next == 'E')
					exponent = true;
				if (negative)
					continue;
				switch (next) {
				case '0':
					if (countGrouping)
						grouping++;
					if (exponent)
						minExponent++;
					else {
						if (maxFraction > 0)
							throw new IllegalArgumentException(Msg.getString(
									"K0015", String.valueOf((char) next),
									pattern));
						if (fraction)
							minFraction++;
						else
							minInt++;
					}
					break;
				case '#':
					if ((!fraction && minInt > 0) || exponent)
						throw new IllegalArgumentException(Msg.getString(
								"K0016", String.valueOf((char) next), pattern));
					if (countGrouping)
						grouping++;
					if (fraction)
						maxFraction++;
					else
						intCount++;
					break;
				case ',':
					if (fraction || exponent)
						throw new IllegalArgumentException(Msg.getString(
								"K0016", String.valueOf((char) next), pattern));
					grouping = 0;
					countGrouping = true;
					break;
				case '.':
					if (fraction || exponent)
						throw new IllegalArgumentException(Msg.getString(
								"K0016", String.valueOf((char) next), pattern));
					countGrouping = false;
					fraction = true;
					break;
				case 'E':
					countGrouping = false;
					break;
				}
			} else {
				if (!inPrefix && !inSuffix) {
					if (!negative)
						format = buffer.toString();
					buffer.setLength(0);
				}
				if (!inPrefix)
					inSuffix = true;
				if (next == '\'') {
					if (lastQuote)
						buffer.append('\'');
					quote = !quote;
					lastQuote = true;
				} else {
					lastQuote = false;
					if (next == ';' && !quote) {
						if (format == null || format.length() == 0)
							throw new IllegalArgumentException(Msg.getString(
									"K0017", String.valueOf((char) next),
									pattern));
						if (exponent && minExponent == 0)
							throw new IllegalArgumentException(Msg.getString(
									"K0018", pattern));
						suffix = buffer.toString();
						buffer.setLength(0);
						negative = inPrefix = true;
						inSuffix = exponent = false;
						continue;
					}
					if (!negative
							&& (next == symbols.getPercent() || next == symbols
									.getPerMill()) && !quote) {
						if (localMultiplier != 1)
							throw new IllegalArgumentException(Msg.getString(
									"K0016", String.valueOf((char) next),
									pattern));
						localMultiplier = next == symbols.getPercent() ? 100
								: 1000;
					}
					buffer.append((char) next);
				}
			}
		}
		if (quote)
			throw new IllegalArgumentException(Msg.getString("K0019", pattern));
		if (countGrouping && grouping == 0)
			throw new IllegalArgumentException(Msg.getString("K001a", pattern));
		if (!negative && exponent && minExponent == 0)
			throw new IllegalArgumentException(Msg.getString("K0018", pattern));
		if (minExponent > 0
				&& intCount + minInt + minFraction + maxFraction == 0)
			throw new IllegalArgumentException(Msg.getString("K001b", pattern));
		if (inPrefix) {
			if (negative)
				negPrefix = buffer.toString();
			else
				prefix = buffer.toString();
		}
		if (inSuffix) {
			if (negative)
				negSuffix = buffer.toString();
			else
				suffix = buffer.toString();
		}

		maxFraction += minFraction;
		if (fraction && minFraction == 0 && minInt == 0 && intCount > 0) {
			minInt = 1;
			intCount--;
		} else if (maxFraction > 0 && minInt + intCount == 0
				&& minFraction == 0)
			minFraction = 1;
		useExponentialNotation = minExponent > 0;
		if (useExponentialNotation) {
			setMaximumIntegerDigits(intCount + minInt);
		} else
			setMaximumIntegerDigits(Integer.MAX_VALUE);
		setMinimumIntegerDigits(minInt);
		setMaximumFractionDigits(maxFraction);
		setMinimumFractionDigits(minFraction);
		setGroupingSize(grouping);
		setGroupingUsed(grouping > 0);
		setPositivePrefix(prefix.toString());
		setPositiveSuffix(suffix.toString());
		posPrefixMonetary = positivePrefix.indexOf(CURRENCY_SYMBOL) > -1;
		posPrefixPattern = expandAffix(positivePrefix);
		posSuffixMonetary = positiveSuffix.indexOf(CURRENCY_SYMBOL) > -1;
		posSuffixPattern = expandAffix(positiveSuffix);
		minExponentDigits = minExponent > Byte.MAX_VALUE ? Byte.MAX_VALUE
				: (byte) minExponent;
		if (!negative) {
			negPrefix = symbols.getMinusSign() + prefix;
			negSuffix = suffix;
		}
		setNegativePrefix(negPrefix);
		setNegativeSuffix(negSuffix);
		negPrefixMonetary = negPrefix.indexOf(CURRENCY_SYMBOL) > -1;
		negPrefixPattern = expandAffix(negPrefix);
		negSuffixMonetary = negSuffix.indexOf(CURRENCY_SYMBOL) > -1;
		negSuffixPattern = expandAffix(negSuffix);
		setDecimalSeparatorAlwaysShown(fraction
				&& (minInt + intCount == 0 || minFraction + maxFraction == 0));
		setMultiplier(localMultiplier);
	}

	/**
	 * Answers a new instance of DecimalFormat with the same pattern and
	 * properties as this DecimalFormat.
	 * 
	 * @return a shallow copy of this DecimalFormat
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		DecimalFormat clone = (DecimalFormat) super.clone();
		clone.symbols = (DecimalFormatSymbols) symbols.clone();
		return clone;
	}

	private String convertCurrencySymbols(String value) {
		StringBuffer output = null;
		for (int i = 0; i < value.length(); i++) {
			char next;
			if ((next = value.charAt(i)) == CURRENCY_SYMBOL) {
				if (output == null)
					output = new StringBuffer(value.substring(0, i));
				if (i + 1 < value.length()
						&& value.charAt(i + 1) == CURRENCY_SYMBOL) {
					i++;
					output.append(symbols.getInternationalCurrencySymbol());
				} else {
					output.append(symbols.getCurrencySymbol());
				}
			} else if (output != null)
				output.append(next);
		}
		if (output == null)
			return value;
		return output.toString();
	}

	/**
	 * Compares the specified object to this DecimalFormat and answer if they
	 * are equal. The object must be an instance of DecimalFormat with the same
	 * pattern and properties.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return true if the specified object is equal to this DecimalFormat,
	 *         false otherwise
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (!(object instanceof DecimalFormat))
			return false;
		DecimalFormat format = (DecimalFormat) object;
		return super.equals(object)
				&& symbols.equals(format.symbols)
				&& groupingSize == format.groupingSize
				&& multiplier == format.multiplier
				&& positivePrefix.equals(format.positivePrefix)
				&& positiveSuffix.equals(format.positiveSuffix)
				&& negativePrefix.equals(format.negativePrefix)
				&& negativeSuffix.equals(format.negativeSuffix)
				&& decimalSeparatorAlwaysShown == format.decimalSeparatorAlwaysShown
				&& isCurrency() == format.isCurrency();
	}

	private Number error(ParsePosition position, int offset) {
		position.setErrorIndex(offset);
		return null;
	}

	/**
	 * Formats the specified object using the rules of this DecimalNumberFormat
	 * and returns an AttributedCharacterIterator with the formatted number and
	 * attributes.
	 * 
	 * @param object
	 *            the object to format
	 * @return an AttributedCharacterIterator with the formatted number and
	 *         attributes
	 * 
	 * @exception IllegalArgumentException
	 *                when the object cannot be formatted by this Format
	 */
	public AttributedCharacterIterator formatToCharacterIterator(Object object) {
		if (!(object instanceof Number))
			throw new IllegalArgumentException();

		StringBuffer buffer = new StringBuffer();
		Vector fields = new Vector();

		// format the number, and find fields
		double dv = ((Number) object).doubleValue();
		long lv = ((Number) object).longValue();
		if (dv == lv)
			formatImpl(lv, buffer, new FieldPosition(0), fields);
		else
			formatImpl(dv, buffer, new FieldPosition(0), fields);

		// create an AttributedString with the formatted buffer
		AttributedString as = new AttributedString(buffer.toString());

		// add NumberFormat field attributes to the AttributedString
		for (int i = 0; i < fields.size(); i++) {
			FieldPosition pos = (FieldPosition) fields.elementAt(i);
			Format.Field attribute = pos.getFieldAttribute();
			as.addAttribute(attribute, attribute, pos.getBeginIndex(), pos
					.getEndIndex());
		}

		// return the CharacterIterator from AttributedString
		return as.getIterator();
	}

	/**
	 * Formats the double value into the specified StringBuffer using the
	 * pattern of this DecimalFormat. If the field specified by the
	 * FieldPosition is formatted, set the begin and end index of the formatted
	 * field in the FieldPosition.
	 * 
	 * @param value
	 *            the double to format
	 * @param buffer
	 *            the StringBuffer
	 * @param position
	 *            the FieldPosition
	 * @return the StringBuffer parameter <code>buffer</code>
	 */
	public StringBuffer format(double value, StringBuffer buffer,
			FieldPosition position) {
		return formatImpl(value, buffer, position, null);
	}

	private StringBuffer formatImpl(double value, StringBuffer buffer,
			FieldPosition position, Vector fields) {
		if (multiplier != 1)
			value *= multiplier;

		position.clear();
		if (Double.isNaN(value))
			return buffer.append(symbols.getNaN());

		long bits = Double.doubleToLongBits(value);
		// SIGN or CURRENCY fields
		String prefix = (bits < 0) ? getNegativePrefix() : getPositivePrefix();
		formatPrefixSuffix(prefix, buffer, position, fields);

		// INTEGER field begins
		FieldPosition positionInteger = null;
		positionInteger = new FieldPosition(Field.INTEGER);
		handleIntegerBegin(buffer.length(), position, fields, positionInteger);

		if (Double.isInfinite(value)) {
			buffer.append(symbols.getInfinity());

			// INTEGER field ends
			handleIntegerEnd(buffer.length(), position, fields, positionInteger);
		} else {
			double abs = value < 0 ? -value : value;

			boolean fixed = false;
			int scale = 0, exponent = 0;
			int minFract = 0, maxFract = 0;
			if (useExponentialNotation) {
				exponent = scale(abs);
				minFract = getMinimumIntegerDigits()
						+ getMinimumFractionDigits();
				fixed = getMinimumIntegerDigits() == getMaximumIntegerDigits();
				if (fixed) {
					minFract -= getMinimumIntegerDigits();
					scale = getMinimumIntegerDigits() - 1;
					if (value != 0)
						exponent -= scale;
				} else {
					scale = exponent % getMaximumIntegerDigits();
					if (exponent < -1
							&& scale + getMaximumIntegerDigits() < getMaximumIntegerDigits())
						scale += getMaximumIntegerDigits();
					if (getMinimumIntegerDigits() == 0)
						scale--;
					if (value != 0)
						exponent -= scale;
					if (scale >= 0)
						minFract -= scale + 1;
				}
				if (exponent != 0)
					abs /= Math.pow(10.0, exponent);
				maxFract = getMaximumIntegerDigits()
						+ getMaximumFractionDigits();
				if (scale >= 0)
					maxFract -= scale + 1;
			} else {
				scale = scale(abs);
				minFract = getMinimumFractionDigits();
				maxFract = getMaximumFractionDigits();
			}
			if (-maxFract - 1 >= (scale - 16)) {
				// Round decimal digits
				abs += Math.pow(10.0, -maxFract - 1) * 5;
			}

			// In case rounding changed the scale
			int checkScale = scale(abs);
			if (checkScale > scale) {
				// Rounding changed the scale
				if (useExponentialNotation) {
					if (fixed || getMinimumIntegerDigits() == 0) {
						abs /= 10.0;
						exponent++;
					} else {
						if (scale + 1 == getMaximumIntegerDigits()
								||
								// catch .1E0 instead of 100E-3 case
								(exponent + getMaximumIntegerDigits() == 0 && scale + 2 == getMaximumIntegerDigits())) {
							exponent += getMaximumIntegerDigits();
							abs /= Math.pow(10.0, getMaximumIntegerDigits());
							int adjust;
							if (exponent == 0)
								adjust = -1;
							else
								adjust = 0;
							minFract += scale - adjust;
							maxFract += scale - adjust;
							scale = adjust;
						} else {
							scale++;
							minFract--;
							maxFract--;
						}
					}
				} else
					scale = checkScale;
			}

			long digits;
			int shift = 0, dLength;
			String sValue = Double.toString(abs);
			dLength = sValue.lastIndexOf('E');
			if (dLength > -1)
				shift = Integer.parseInt(sValue.substring(dLength + 1));
			else
				dLength = sValue.length();
			int index = sValue.indexOf('.');
			if (index > -1) {
				String fValue = sValue.substring(index + 1, dLength);
				String iValue = sValue.substring(0, index);
				if (fValue.equals("0")) {
					sValue = iValue;
					dLength -= 2;
				} else {
					shift -= fValue.length();
					if (iValue.equals("0")) {
						int i = 0;
						while (fValue.charAt(i) == '0')
							i++;
						sValue = fValue.substring(i);
						dLength -= i + 2;
					} else {
						sValue = iValue + fValue;
						dLength--;
					}
				}
			} else
				sValue = sValue.substring(0, dLength);
			digits = Long.parseLong(sValue);

			int iLength = dLength;
			long number = digits;
			if (shift < 0) {
				if (-shift < dLength) {
					iLength = dLength + shift;
					long pow = intPow(-shift);
					number /= pow;
					digits -= number * pow;
					dLength = -shift;
				} else {
					number = 0;
					iLength = 1;
				}
			} else {
				iLength += shift;
				digits = 0;
				dLength = 0;
			}
			if (maxFract != 0 && maxFract < dLength) {
				long pow = intPow(dLength - maxFract);
				digits /= pow;
				shift += dLength - maxFract;
				dLength = maxFract;
			}

			int length = buffer.length();
			StringBuffer output = new StringBuffer();

			if (scale < 0 && !useExponentialNotation)
				formatInteger("0", buffer, position, fields);
			else {
				output.append(Long.toString(number));
				for (int i = output.length(); i < iLength; i++)
					output.append('0');
				if (useExponentialNotation) {
					for (int i = 0; i <= scale; i++) {
						if (i >= output.length())
							buffer.append(zero);
						else
							buffer
									.append((char) (zero + (output.charAt(i) - '0')));
					}
				} else
					formatInteger(output.toString(), buffer, position, fields);
			}

			if (maxFract == 0) {
				if (length == buffer.length())
					buffer.append(zero);
				// INTEGER field ends
				handleIntegerEnd(buffer.length(), position, fields,
						positionInteger);

				// format DECIMAL_SEPARATOR field
				formatFraction(new StringBuffer(), buffer, position, fields);
			} else {
				output.setLength(0);
				int lastDigit = 0, leading = -shift - dLength;
				if (digits > 0 && maxFract > leading) {
					String print = Long.toString(digits);
					if (dLength > print.length())
						leading += dLength - print.length();
					for (int i = 0; i < leading; i++)
						output.append(zero);
					for (int i = leading; i < maxFract; i++) {
						int digit;
						if (i - leading >= print.length())
							digit = 0;
						else
							digit = print.charAt(i - leading) - '0';
						output.append((char) (zero + digit));
						if (digit > 0)
							lastDigit = output.length();
					}
				}
				if (output.length() < minFract) {
					for (int i = output.length(); i < minFract; i++)
						output.append(zero);
				} else {
					output.setLength(lastDigit < minFract ? minFract
							: lastDigit);
				}
				if (output.length() == 0 && length == buffer.length())
					buffer.append(zero);

				// INTEGER Field ends
				handleIntegerEnd(buffer.length(), position, fields,
						positionInteger);

				// format DECIMAL_SEPARATOR and FRACTION fields
				formatFraction(output, buffer, position, fields);
			}

			if (useExponentialNotation)
				formatExponent(exponent, buffer, position, fields);
		}

		// handle SIGN, CURRENCY, PERMILLE or PERCENT fields
		String suffix = (bits < 0) ? getNegativeSuffix() : getPositiveSuffix();
		formatPrefixSuffix(suffix, buffer, position, fields);

		return buffer;
}

	/**
	 * Formats the long value into the specified StringBuffer using the pattern
	 * of this DecimalFormat. If the field specified by the FieldPosition is
	 * formatted, set the begin and end index of the formatted field in the
	 * FieldPosition.
	 * 
	 * @param value
	 *            the long to format
	 * @param buffer
	 *            the StringBuffer
	 * @param position
	 *            the FieldPosition
	 * @return the StringBuffer parameter <code>buffer</code>
	 */
	public StringBuffer format(long value, StringBuffer buffer,
			FieldPosition position) {
		return formatImpl(value, buffer, position, null);
	}

	private StringBuffer formatImpl(long value, StringBuffer buffer,
			FieldPosition position, Vector fields) {
		if (multiplier != 1) {
			int sign = 1;
			if (value < 0)
				sign *= -1;
			if (multiplier < 0)
				sign *= -1;
			long oldValue = value;
			value *= multiplier;
			if ((sign > 0 && oldValue >= value)
					|| (sign < 0 && oldValue <= value))
				return formatImpl((double) oldValue, buffer, position, fields);

			// Check for large overflows missed by the previous check
			double result = (double) value * multiplier;
			if (result > Long.MAX_VALUE || result < Long.MIN_VALUE)
				return formatImpl((double) value, buffer, position, fields);
		}

		// SIGN or CURRENCY fields
		String prefix = (value < 0) ? getNegativePrefix() : getPositivePrefix();
		formatPrefixSuffix(prefix, buffer, position, fields);

		// INTEGER field begins
		FieldPosition positionInteger = null;
		positionInteger = new FieldPosition(Field.INTEGER);
		handleIntegerBegin(buffer.length(), position, fields, positionInteger);

		int outLength = buffer.length();
		String digits = Long.toString(value);

		int exponent = 0, scale = 0, minFract = 0;
		if (useExponentialNotation) {
			if (value < 0)
				digits = digits.substring(1, digits.length());
			int length = digits.length();
			minFract = getMinimumIntegerDigits() + getMinimumFractionDigits();
			exponent = length - 1;

			boolean fixed = getMinimumIntegerDigits() == getMaximumIntegerDigits();
			if (fixed) {
				minFract -= getMinimumIntegerDigits();
				scale = getMinimumIntegerDigits() - 1;
				if (value != 0)
					exponent -= scale;
				scale++;
			} else {
				scale = exponent % getMaximumIntegerDigits();
				if (value != 0)
					exponent -= scale;
				if (getMinimumIntegerDigits() > 0)
					scale++;
				else if (value != 0)
					exponent++;
				minFract -= scale;
			}
			if (getMaximumIntegerDigits() + getMaximumFractionDigits() < length) {
				int chop = length
						- (getMaximumIntegerDigits() + getMaximumFractionDigits())
						- 1;
				for (int i = chop; --i >= 0;)
					value /= 10;
				// Round decimal digits
				if (value < 0)
					value -= 5;
				else
					value += 5;
				digits = Long.toString(value);
				if (value < 0)
					digits = digits.substring(1, digits.length());

				int remove = 1;
				if (digits.length() > (length - chop)) {
					// Rounding changed the scale
					if (fixed || getMinimumIntegerDigits() == 0) {
						remove++;
						exponent++;
					} else {
						if (scale == getMaximumIntegerDigits()) {
							exponent += getMaximumIntegerDigits();
							remove += getMaximumIntegerDigits();
							minFract += scale - 1;
							scale = 1;
						} else {
							scale++;
							minFract--;
						}
					}
				}
				digits = digits.substring(0, digits.length() - remove);
			}
			for (int i = 0; i < scale; i++) {
				if (i >= digits.length())
					buffer.append(zero);
				else
					buffer.append((char) (zero + (digits.charAt(i) - '0')));
			}
		} else
			formatInteger(digits, buffer, position, fields);

		// Always print a digit
		if (!useExponentialNotation && outLength == buffer.length())
			buffer.append(zero);

		// INTEGER Field ends
		handleIntegerEnd(buffer.length(), position, fields, positionInteger);

		if (useExponentialNotation) {
			StringBuffer output = new StringBuffer();
			int lastDigit = 0;
			for (int i = 0; i < digits.length() - scale; i++) {
				int digit = digits.charAt(scale + i) - '0';
				if (digit > 0)
					lastDigit = i + 1;
				output.append((char) (zero + digit));
			}
			for (int i = output.length(); i < minFract; i++)
				output.append(zero);
			output.setLength(lastDigit < minFract ? minFract : lastDigit);
			formatFraction(output, buffer, position, fields);
		} else {
			StringBuffer output = new StringBuffer();
			for (int i = 0; i < getMinimumFractionDigits(); i++)
				output.append(zero);
			formatFraction(output, buffer, position, fields);
		}

		if (useExponentialNotation)
			formatExponent(exponent, buffer, position, fields);

		// handle SIGN, CURRENCY, PERMILLE or PERCENT fields
		String suffix = (value < 0) ? getNegativeSuffix() : getPositiveSuffix();
		formatPrefixSuffix(suffix, buffer, position, fields);

		return buffer;
	}

	private void formatPrefixSuffix(String fix, StringBuffer buffer,
			FieldPosition position, Vector fields) {
		int beginIndex = buffer.length();
		buffer.append(fix);

		if (fix.length() > 0) {
			DecimalFormatSymbols formatSymbols = getDecimalFormatSymbols();
			String curSymbol = formatSymbols.getCurrencySymbol();

			int index;
			if ((index = fix.indexOf(curSymbol)) > -1)
				handleField(Field.CURRENCY, beginIndex + index, beginIndex
						+ index + curSymbol.length(), position, fields);

			if ((index = fix.indexOf(formatSymbols.getMinusSign())) > -1)
				handleField(Field.SIGN, beginIndex + index, beginIndex + index
						+ 1, position, fields);

			if ((index = fix.indexOf(formatSymbols.getPercent())) > -1)
				handleField(Field.PERCENT, beginIndex + index, beginIndex
						+ index + 1, position, fields);

			if ((index = fix.indexOf(formatSymbols.getPerMill())) > -1)
				handleField(Field.PERMILLE, beginIndex + index, beginIndex
						+ index + 1, position, fields);
		}
	}

	private void formatInteger(String output, StringBuffer buffer,
			FieldPosition position, Vector fields) {
		int offset = 0;
		int total = output.length();

		if (output.charAt(0) == '-') {
			offset = 1;
			total--;
		}

		boolean firstOccurrenceFound = false;
		int groupSize = getGroupingSize();
		if (!isGroupingUsed())
			groupSize = 0;
		if (getMinimumIntegerDigits() > total) {
			int extra = getMinimumIntegerDigits() - total;
			total = getMinimumIntegerDigits();
			for (int i = 0; i < extra; i++) {
				buffer.append(zero);
				total--;
				if (groupSize > 0 && total > 0 && total % groupSize == 0) {
					buffer.append(symbols.getGroupingSeparator());
					int index = buffer.length();
					if (fields != null) {
						addToFields(fields, Field.GROUPING_SEPARATOR,
								index - 1, index);
					} else {
						if (!firstOccurrenceFound
								&& position.getFieldAttribute() == Field.GROUPING_SEPARATOR) {
							position.setBeginIndex(index - 1);
							position.setEndIndex(index);
							firstOccurrenceFound = true;
						}
					}
				}

			}
		}

		if (total > getMaximumIntegerDigits()) {
			offset += total - getMaximumIntegerDigits();
			total = getMaximumIntegerDigits();
		}

		if (getMinimumIntegerDigits() == 0 && total == 1
				&& output.charAt(offset) == '0')
			return;

		for (int i = offset; i < output.length(); i++) {
			buffer.append((char) (zero + (output.charAt(i) - '0')));
			total--;
			if (groupSize > 0 && total > 0 && total % groupSize == 0) {
				buffer.append(symbols.getGroupingSeparator());
				int index = buffer.length();
				if (fields != null) {
					addToFields(fields, Field.GROUPING_SEPARATOR, index - 1,
							index);
				} else {
					if (!firstOccurrenceFound
							&& position.getFieldAttribute() == Field.GROUPING_SEPARATOR) {
						position.setBeginIndex(index - 1);
						position.setEndIndex(index);
						firstOccurrenceFound = true;
					}
				}
			}
		}
	}

	/**
	 * Appends decimal seperator and fraction fields to the buffer, and to the
	 * fields vector if needed.
	 * 
	 * Sets the begin and end index of the position if fields vector is null,
	 * and position has a DECIMAL_SEPARATOR or FRACTION field.
	 * 
	 * @param fraction
	 * @param buffer
	 * @param position
	 * @param fields
	 */
	private void formatFraction(StringBuffer fraction, StringBuffer buffer,
			FieldPosition position, Vector fields) {
		int index;
		int len = fraction.length();
		if (isDecimalSeparatorAlwaysShown() || len > 0) {
			// DECIMAL_SEPERATOR field
			buffer.append(getDecimalSeparator());
			index = buffer.length();
			handleField(Field.DECIMAL_SEPARATOR, index - 1, index, position,
					fields);
		}

		if (len > 0) {
			// FRACTION field
			buffer.append(fraction);
			index = buffer.length();
			if (fields != null)
				addToFields(fields, Field.FRACTION, index - len, index);
			else {
				if (position.getFieldAttribute() == Field.FRACTION
						|| (position.getFieldAttribute() == null && position
								.getField() == FRACTION_FIELD)) {
					position.setBeginIndex(index - len);
					position.setEndIndex(index);
				}
			}
		}
	}

	private void formatExponent(int exponent, StringBuffer buffer,
			FieldPosition position, Vector fields) {
		String output = Integer.toString(exponent);
		int offset = 0;
		int total = output.length();

		buffer.append(symbols.getExponential());

		int currentIndex = buffer.length();
		handleField(Field.EXPONENT_SYMBOL, currentIndex - 1, currentIndex,
				position, fields);

		if (exponent < 0) {
			buffer.append(getNegativePrefix());
			offset = 1;
			total--;
		} else
			buffer.append(getPositivePrefix());

		int prefixLen = (exponent < 0) ? getNegativePrefix().length()
				: getPositivePrefix().length();
		if (prefixLen > 0) {
			currentIndex = buffer.length();
			handleField(Field.EXPONENT_SIGN, currentIndex - prefixLen,
					currentIndex, position, fields);
		}

		if (minExponentDigits > total) {
			for (int i = minExponentDigits - total; --i >= 0;)
				buffer.append(zero);
		}

		for (int i = offset; i < output.length(); i++)
			buffer.append((char) (zero + (output.charAt(i) - '0')));

		handleField(Field.EXPONENT, currentIndex, buffer.length(), position,
				fields);

		if (exponent < 0)
			buffer.append(getNegativeSuffix());
		else
			buffer.append(getPositiveSuffix());

		int suffixLen = (exponent < 0) ? getNegativeSuffix().length()
				: getPositiveSuffix().length();
		if (suffixLen > 0) {
			currentIndex = buffer.length();
			handleField(Field.EXPONENT_SIGN, currentIndex - suffixLen,
					currentIndex, position, fields);
		}
	}

	/**
	 * Adds the field to the fields vector, or sets the <code>position</code>'s
	 * begin and end index if it has the same field attribute as
	 * <code>field</code>
	 * 
	 * @param field
	 * @param begin
	 * @param end
	 * @param position
	 * @param fields
	 */
	private void handleField(Field field, int begin, int end,
			FieldPosition position, Vector fields) {
		if (fields != null)
			addToFields(fields, field, begin, end);
		else {
			if (position.getFieldAttribute() == field) {
				position.setBeginIndex(begin);
				position.setEndIndex(end);
			}
		}
	}

	private void addToFields(Vector fields, Field field, int begin, int end) {
		FieldPosition pos = new FieldPosition(field);
		pos.setBeginIndex(begin);
		pos.setEndIndex(end);
		fields.add(pos);
	}

	private void handleIntegerBegin(int index, FieldPosition position,
			Vector fields, FieldPosition positionInteger) {
		if (fields != null)
			positionInteger.setBeginIndex(index);
		else {
			if (position.getFieldAttribute() == Field.INTEGER
					|| (position.getFieldAttribute() == null && position
							.getField() == INTEGER_FIELD))
				position.setBeginIndex(index);
		}
	}

	private void handleIntegerEnd(int index, FieldPosition position,
			Vector fields, FieldPosition positionInteger) {
		if (fields != null) {
			positionInteger.setEndIndex(index);
			fields.add(positionInteger);
		} else {
			if (position.getFieldAttribute() == Field.INTEGER
					|| (position.getFieldAttribute() == null && position
							.getField() == INTEGER_FIELD))
				position.setEndIndex(index);
		}
	}

	/**
	 * Answers the DecimalFormatSymbols used by this DecimalFormat.
	 * 
	 * @return a DecimalFormatSymbols
	 */
	public DecimalFormatSymbols getDecimalFormatSymbols() {
		return (DecimalFormatSymbols) symbols.clone();
	}

	private char getDecimalSeparator() {
		if (isCurrency())
			return symbols.getMonetaryDecimalSeparator();
		return symbols.getDecimalSeparator();
	}

	/**
	 * Answers the currency used by this decimal format.
	 * 
	 * @return currency of DecimalFormatSymbols used by this decimal format
	 * @see DecimalFormatSymbols#getCurrency()
	 */
	public Currency getCurrency() {
		return symbols.getCurrency();
	}

	/**
	 * Answers the number of digits grouped together by the grouping separator.
	 * 
	 * @return the number of digits grouped together
	 */
	public int getGroupingSize() {
		return groupingSize;
	}

	/**
	 * Answers the multiplier which is applied to the number before formatting
	 * or after parsing.
	 * 
	 * @return the multiplier
	 */
	public int getMultiplier() {
		return multiplier;
	}

	/**
	 * Answers the prefix which is formatted or parsed before a negative number.
	 * 
	 * @return the negative prefix
	 */
	public String getNegativePrefix() {
		return negPrefixPattern == null ? negativePrefix : negPrefixPattern;
	}

	/**
	 * Answers the suffix which is formatted or parsed after a negative number.
	 * 
	 * @return the negative suffix
	 */
	public String getNegativeSuffix() {
		return negSuffixPattern == null ? negativeSuffix : negSuffixPattern;
	}

	/**
	 * Answers the prefix which is formatted or parsed before a positive number.
	 * 
	 * @return the positive prefix
	 */
	public String getPositivePrefix() {
		return posPrefixPattern == null ? positivePrefix : posPrefixPattern;
	}

	/**
	 * Answers the suffix which is formatted or parsed after a positive number.
	 * 
	 * @return the positive suffix
	 */
	public String getPositiveSuffix() {
		return posSuffixPattern == null ? positiveSuffix : posSuffixPattern;
	}

	/**
	 * Answers an integer hash code for the receiver. Objects which are equal
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		return super.hashCode() + symbols.hashCode() + groupingSize
				+ multiplier + positivePrefix.hashCode()
				+ positiveSuffix.hashCode() + negativePrefix.hashCode()
				+ negativeSuffix.hashCode()
				+ (decimalSeparatorAlwaysShown ? 1231 : 1237)
				+ (isCurrency() ? 1231 : 1237);
	}

	private boolean isCurrency() {
		return posPrefixMonetary || posSuffixMonetary || negPrefixMonetary
				|| negSuffixMonetary;
	}

	/**
	 * Answers whether the decimal separator is shown when there are no
	 * fractional digits.
	 * 
	 * @return true if the decimal separator should always be formatted, false
	 *         otherwise
	 */
	public boolean isDecimalSeparatorAlwaysShown() {
		return decimalSeparatorAlwaysShown;
	}

	private int parseExponent(String string, ParsePosition position) {
		int sign = 0;
		int offset = position.getIndex();
		position.setIndex(-1);
		if (positivePrefix.length() != 0
				&& string.startsWith(positivePrefix, offset)) {
			sign = 1;
			offset += positivePrefix.length();
		} else {
			if (negativePrefix.length() != 0
					&& string.startsWith(negativePrefix, offset)) {
				sign = -1;
				offset += negativePrefix.length();
			} else if (positivePrefix.length() != 0
					&& negativePrefix.length() != 0)
				return 0;
		}

		boolean overflow = false;
		int number = 0, lastOffset = offset;
		int length = string.length();
		while (offset < length) {
			char ch = string.charAt(offset);
			int value = getDigit(ch);
			if (value == -1)
				break;
			int next = number * 10 - value;
			if (next > number)
				overflow = true;
			number = next;
			offset++;
		}
		if (offset == lastOffset)
			return 0;

		if (sign >= 0) {
			if (positiveSuffix.length() == 0) {
				position.setIndex(offset);
				sign = 1;
			} else if (string.startsWith(positiveSuffix, offset)) {
				position.setIndex(offset + positiveSuffix.length());
				sign = 1;
			} else if (sign == 1)
				return 0;
		}
		if (sign <= 0) {
			if (negativeSuffix.length() == 0) {
				position.setIndex(offset);
				sign = -1;
			} else if (string.startsWith(negativeSuffix, offset)) {
				position.setIndex(offset + negativeSuffix.length());
				sign = -1;
			} else
				return 0;
		}
		if (sign != -1) {
			number = -number;
			// catch MIN_VALUE case
			if (number < 0)
				overflow = true;
		}
		if (overflow)
			return 1024; // Return any overflow number
		return number;
	}

	/**
	 * Parse a Long or Double from the specified String starting at the index
	 * specified by the ParsePosition. If the string is successfully parsed, the
	 * index of the ParsePosition is updated to the index following the parsed
	 * text.
	 * 
	 * @param string
	 *            the String to parse
	 * @param position
	 *            the ParsePosition, updated on return with the index following
	 *            the parsed text, or on error the index is unchanged and the
	 *            error index is set to the index where the error occurred
	 * @return a Long or Double resulting from the parse, or null if there is an
	 *         error. The result will be a Long if the parsed number is an
	 *         integer in the range of a long, otherwise the result is a Double.
	 */
	public Number parse(String string, ParsePosition position) {
		int offset = position.getIndex();
		if (offset < 0)
			return error(position, offset);
		if (string.startsWith(symbols.getNaN(), offset)) {
			position.setIndex(offset + symbols.getNaN().length());
			return new Double(Double.NaN);
		}

		int sign = 0;
		boolean posPre = positivePrefix.length() > 0
				&& string.startsWith(positivePrefix, offset);
		if (positivePrefix.equals(negativePrefix)) {
			if (positivePrefix.length() > 0 && !posPre)
				return error(position, offset);
			offset += positivePrefix.length();
		} else {
			boolean negPre = negativePrefix.length() > 0
					&& string.startsWith(negativePrefix, offset);
			if ((posPre || positivePrefix.length() == 0
					&& negativePrefix.length() > 0)
					&& !negPre) {
				sign = 1;
				offset += positivePrefix.length();
			} else if ((negPre || negativePrefix.length() == 0
					&& positivePrefix.length() > 0)
					&& !posPre) {
				sign = -1;
				offset += negativePrefix.length();
			} else if (positivePrefix.length() != 0
					&& negativePrefix.length() != 0)
				return error(position, offset);
		}

		double dnumber = 0;
		long max = Long.MIN_VALUE / 10;
		boolean overflow = false, useFloat = false;
		long number = 0, fraction = 0;
		int length = string.length(), lastValid = -1, decimalDigits = 0, fractionDigits = 0, exponent = 0;
		if (string.startsWith(symbols.getInfinity(), offset)) {
			lastValid = (offset += symbols.getInfinity().length());
			dnumber = Double.POSITIVE_INFINITY;
		} else {
			boolean digits = false;
			while (offset < length) {
				char ch = string.charAt(offset);
				if (ch == getDecimalSeparator()) {
					if (!isParseIntegerOnly()) {
						offset++;
						lastValid = offset;
						StringBuffer fractDigits = new StringBuffer();
						while (offset < length) {
							ch = string.charAt(offset);
							if (getDigit(ch) == -1)
								break;
							fractDigits.append(ch);
							offset++;
							lastValid = offset;
						}
						int len = fractDigits.length() - 1;
						while (len >= 0
								&& getDigit(fractDigits.charAt(len)) == 0) {
							digits = true;
							len--;
						}
						for (int i = 0; i <= len; i++) {
							digits = true;
							int value = getDigit(fractDigits.charAt(i));
							if (!overflow) {
								if (max > fraction) {
									overflow = true;
									break;
								}
								long next = fraction * 10 - value;
								if (next <= fraction) {
									fraction = next;
									fractionDigits++;
								} else {
									overflow = true;
									break;
								}
							}
						}
						if (ch == symbols.getExponential()) {
							ParsePosition pos = new ParsePosition(offset + 1);
							exponent = parseExponent(string, pos);
							if (pos.getIndex() != -1)
								lastValid = pos.getIndex();
						}
					}
					break;
				}
				int value = getDigit(ch);
				if (value != -1) {
					digits = true;
					if (number != 0 || value > 0) {
						decimalDigits++;
						if (useFloat)
							dnumber = dnumber * 10 - value;
						else {
							if (max <= number) {
								long next = number * 10 - value;
								if (next > number) { // Overflow
									dnumber = number * 10.0 - value;
									useFloat = true;
								} else
									number = next;
							} else { // Overflow
								dnumber = number * 10.0 - value;
								useFloat = true;
							}
						}
					}
					lastValid = offset + 1;
				} else if (ch == symbols.getExponential()) {
					ParsePosition pos = new ParsePosition(offset + 1);
					exponent = parseExponent(string, pos);
					if (pos.getIndex() != -1)
						lastValid = pos.getIndex();
					break;
				} else if (ch != symbols.getGroupingSeparator()
						|| !isGroupingUsed())
					break;
				offset++;
			}
			if (!digits)
				return error(position, position.getIndex());
		}

		if (sign == 0) {
			boolean posSuf = positiveSuffix.length() > 0
					&& string.startsWith(positiveSuffix, offset);
			boolean negSuf = negativeSuffix.length() > 0
					&& string.startsWith(negativeSuffix, offset);
			if (posSuf != negSuf) {
				if (posSuf)
					sign = 1;
				else
					sign = -1;
			} else if (positiveSuffix.equals(negativeSuffix))
				return error(position, lastValid);
		}
		if (sign >= 0) {
			if (positiveSuffix.length() == 0) {
				position.setIndex(lastValid);
				sign = 1;
			} else if (offset == lastValid
					&& string.startsWith(positiveSuffix, offset)) {
				position.setIndex(offset + positiveSuffix.length());
				sign = 1;
			} else if (sign == 1)
				return error(position, lastValid);
		}
		if (sign <= 0) {
			if (negativeSuffix.length() == 0) {
				position.setIndex(lastValid);
				sign = -1;
			} else if (offset == lastValid
					&& string.startsWith(negativeSuffix, offset)) {
				position.setIndex(offset + negativeSuffix.length());
				sign = -1;
			} else
				return error(position, lastValid);
		}

		if (!useFloat
				&& ((fraction != 0 && exponent == 0) || exponent < 0
						|| exponent + decimalDigits - 1 > 19
						|| exponent < fractionDigits || overflow)) {
			dnumber = number;
			useFloat = true;
		}
		if (!useFloat && number != 0) {
			long next = number;
			for (int i = exponent; --i >= 0;) {
				if (max > next) { // Overflow
					dnumber = number;
					useFloat = true;
					break;
				}
				next *= 10;
			}
			number = next;
		}
		if (fraction != 0 && !useFloat) {
			long next = fraction;
			for (int i = exponent - fractionDigits; --i >= 0;)
				next *= 10;
			long result = number + next;
			if (next < number)
				number = result;
			else { // Overflow
				dnumber = number;
				exponent = 0;
				fraction = next;
				fractionDigits = 0;
				useFloat = true;
			}
		}
		if (useFloat) {
			if (fraction != 0)
				dnumber += fraction / Math.pow(10.0, fractionDigits);
			if (exponent != 0)
				dnumber *= Math.pow(10.0, exponent);
		}
		if (!useFloat && number == 0 && sign == -1 && !isParseIntegerOnly()) {
			dnumber = -1.0 * 0;
			useFloat = true;
		}

		if (multiplier != 1) {
			if (dnumber != 0) {
				dnumber /= multiplier;
			} else {
				long lResult = number / multiplier;
				double dResult = (double) number / multiplier;
				if (dResult == lResult)
					number = lResult;
				else {
					dnumber = dResult;
					useFloat = true;
				}
			}
		}

		if (number == Long.MIN_VALUE && sign == 1) {
			dnumber = number;
			useFloat = true;
		}

		if (useFloat)
			return new Double(sign < 0 ? dnumber : -dnumber);
		return new Long(sign < 0 ? number : -number);
	}

	private String quote(String string, String special) {
		for (int i = 0; i < special.length(); i++) {
			char ch = special.charAt(i);
			if (string.indexOf(ch) != -1
					&& (i < 5 || ch != patternChars.charAt(i)))
				return '\'' + string + '\'';
		}
		return string;
	}

	private int scale(double value) {
		if (value == 0.0)
			return 0;
		int scale = (int) Math.floor(Math.log(value) / log10);
		return Math.pow(10.0, scale) > value ? scale - 1 : scale;
	}

	private String expandAffix(String affix) {
		String newAffix = convertPattern(affix, patternChars, new String(
				symbols.patternChars), false);
		return convertCurrencySymbols(newAffix);
	}

	/**
	 * Sets the DecimalFormatSymbols used by this DecimalFormat.
	 * 
	 * @param value
	 *            the DecimalFormatSymbols
	 */
	public void setDecimalFormatSymbols(DecimalFormatSymbols value) {
		if (value != null) {
			symbols = (DecimalFormatSymbols) value.clone();
			zero = symbols.getZeroDigit();
			// Update localized affix
			if (posPrefixPattern != null)
				posPrefixPattern = expandAffix(positivePrefix);
			if (posSuffixPattern != null)
				posSuffixPattern = expandAffix(positiveSuffix);
			if (negPrefixPattern != null)
				negPrefixPattern = expandAffix(negativePrefix);
			if (negSuffixPattern != null)
				negSuffixPattern = expandAffix(negativeSuffix);
		}
	}

	/**
	 * Sets the currency used by this decimal format. The min and max fraction
	 * digits remain the same.
	 * 
	 * @param currency
	 * @see DecimalFormatSymbols#setCurrency(Currency)
	 */
	public void setCurrency(Currency currency) {
		symbols.setCurrency(currency);
	}

	/**
	 * Sets whether the decimal separator is shown when there are no fractional
	 * digits.
	 * 
	 * @param value
	 *            true if the decimal separator should always be formatted,
	 *            false otherwise
	 */
	public void setDecimalSeparatorAlwaysShown(boolean value) {
		decimalSeparatorAlwaysShown = value;
	}

	/**
	 * Sets the number of digits grouped together by the grouping separator.
	 * 
	 * @param value
	 *            the number of digits grouped together
	 */
	public void setGroupingSize(int value) {
		groupingSize = (byte) value;
	}

	/**
	 * Sets the maximum number of fraction digits that are printed when
	 * formatting. If the maximum is less than the number of fraction digits,
	 * the least significant digits are truncated. Limit the maximum to
	 * DOUBLE_FRACTION_DIGITS.
	 * 
	 * @param value
	 *            the maximum number of fraction digits
	 */
	public void setMaximumFractionDigits(int value) {
		super
				.setMaximumFractionDigits(value > DOUBLE_FRACTION_DIGITS ? DOUBLE_FRACTION_DIGITS
						: value);
	}

	/**
	 * Sets the maximum number of integer digits that are printed when
	 * formatting. If the maximum is less than the number of integer digits, the
	 * most significant digits are truncated. Limit the maximum to
	 * DOUBLE_INTEGER_DIGITS.
	 * 
	 * @param value
	 *            the maximum number of integer digits
	 */
	public void setMaximumIntegerDigits(int value) {
		super
				.setMaximumIntegerDigits(value > DOUBLE_INTEGER_DIGITS ? DOUBLE_INTEGER_DIGITS
						: value);
	}

	/**
	 * Sets the minimum number of fraction digits that are printed when
	 * formatting. Limit the minimum to DOUBLE_FRACTION_DIGITS.
	 * 
	 * @param value
	 *            the minimum number of fraction digits
	 */
	public void setMinimumFractionDigits(int value) {
		super
				.setMinimumFractionDigits(value > DOUBLE_FRACTION_DIGITS ? DOUBLE_FRACTION_DIGITS
						: value);
	}

	/**
	 * Sets the minimum number of integer digits that are printed when
	 * formatting. Limit the minimum to DOUBLE_INTEGER_DIGITS.
	 * 
	 * @param value
	 *            the minimum number of integer digits
	 */
	public void setMinimumIntegerDigits(int value) {
		super
				.setMinimumIntegerDigits(value > DOUBLE_INTEGER_DIGITS ? DOUBLE_INTEGER_DIGITS
						: value);
	}

	/**
	 * Sets the multiplier which is applied to the number before formatting or
	 * after parsing.
	 * 
	 * @param value
	 *            the multiplier
	 */
	public void setMultiplier(int value) {
		multiplier = value;
	}

	/**
	 * Sets the prefix which is formatted or parsed before a negative number.
	 * 
	 * @param value
	 *            the negative prefix
	 */
	public void setNegativePrefix(String value) {
		negativePrefix = value;
		negPrefixMonetary = false;
		negPrefixPattern = null;
	}

	/**
	 * Sets the suffix which is formatted or parsed after a negative number.
	 * 
	 * @param value
	 *            the negative suffix
	 */
	public void setNegativeSuffix(String value) {
		negativeSuffix = value;
		negSuffixMonetary = false;
		negSuffixPattern = null;
	}

	/**
	 * Sets the prefix which is formatted or parsed before a positive number.
	 * 
	 * @param value
	 *            the positive prefix
	 */
	public void setPositivePrefix(String value) {
		positivePrefix = value;
		posPrefixMonetary = false;
		posPrefixPattern = null;
	}

	/**
	 * Sets the suffix which is formatted or parsed after a positive number.
	 * 
	 * @param value
	 *            the positive suffix
	 */
	public void setPositiveSuffix(String value) {
		positiveSuffix = value;
		posSuffixMonetary = false;
		posSuffixPattern = null;
	}

	/**
	 * Answers the pattern of this DecimalFormat using localized pattern
	 * characters.
	 * 
	 * @return the localized pattern
	 */
	public String toLocalizedPattern() {
		return toPatternString(symbols.getLocalPatternChars());
	}

	/**
	 * Answers the pattern of this DecimalFormat using non-localized pattern
	 * characters.
	 * 
	 * @return the non-localized pattern
	 */
	public String toPattern() {
		return toPatternString(patternChars);
	}

	private String toPatternString(String localPatternChars) {
		StringBuffer pattern = new StringBuffer();
		boolean groupingUsed = isGroupingUsed() && getGroupingSize() > 0;
		if (!groupingUsed || groupingSize > getMinimumIntegerDigits()) {
			if (groupingUsed) {
				if (useExponentialNotation) {
					for (int i = 0; i < getMaximumIntegerDigits()
							- groupingSize - 1; i++)
						pattern.append('#');
				}
				pattern.append("#,");
				for (int i = 0; i < groupingSize - getMinimumIntegerDigits(); i++)
					pattern.append('#');
			} else if (useExponentialNotation) {
				for (int i = 0; i < getMaximumIntegerDigits()
						- getMinimumIntegerDigits(); i++)
					pattern.append('#');
			} else
				pattern.append('#');
			for (int i = 0; i < getMinimumIntegerDigits(); i++)
				pattern.append('0');
		} else {
			if (useExponentialNotation) {
				for (int i = 0; i < getMaximumIntegerDigits()
						- getMinimumIntegerDigits(); i++)
					pattern.append('#');
			} else
				pattern.append('#');
			for (int i = 0; i < getMinimumIntegerDigits() - groupingSize; i++)
				pattern.append('0');
			pattern.append(',');
			for (int i = 0; i < groupingSize; i++)
				pattern.append('0');
		}
		if (getMaximumFractionDigits() > 0 || isDecimalSeparatorAlwaysShown()) {
			pattern.append('.');
			for (int i = 0; i < getMinimumFractionDigits(); i++)
				pattern.append('0');
			for (int i = 0; i < getMaximumFractionDigits()
					- getMinimumFractionDigits(); i++)
				pattern.append('#');
		}
		if (useExponentialNotation) {
			pattern.append('E');
			for (int i = 0; i < minExponentDigits; i++)
				pattern.append('0');
		}

		String localPattern = pattern.toString();
		String result = quote(positivePrefix, localPatternChars) + localPattern
				+ quote(positiveSuffix, localPatternChars);
		if (!negativePrefix.equals(symbols.getMinusSign() + positivePrefix)
				|| !negativeSuffix.equals(positiveSuffix)) {
			result = result + ';' + quote(negativePrefix, localPatternChars)
					+ localPattern + quote(negativeSuffix, localPatternChars);
		}
		if (localPatternChars != patternChars)
			result = convertPattern(result, patternChars, localPatternChars,
					false);
		return result;
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		if (serialVersionOnStream == 0)
			useExponentialNotation = false;
		zero = symbols.getZeroDigit();
		serialVersionOnStream = 2;
		if (posPrefixPattern != null)
			posPrefixMonetary = positivePrefix.indexOf(CURRENCY_SYMBOL) > -1;
		if (posSuffixPattern != null)
			posSuffixMonetary = positiveSuffix.indexOf(CURRENCY_SYMBOL) > -1;
		if (negPrefixPattern != null)
			negPrefixMonetary = negativePrefix.indexOf(CURRENCY_SYMBOL) > -1;
		if (negSuffixPattern != null)
			negSuffixMonetary = negativeSuffix.indexOf(CURRENCY_SYMBOL) > -1;
		if (getMinimumIntegerDigits() > DOUBLE_INTEGER_DIGITS
				|| getMaximumIntegerDigits() > DOUBLE_INTEGER_DIGITS
				|| getMinimumFractionDigits() > DOUBLE_FRACTION_DIGITS
				|| getMaximumFractionDigits() > DOUBLE_FRACTION_DIGITS)
			throw new InvalidObjectException(com.ibm.oti.util.Msg
					.getString("K00f9"));
	}

	long intPow(int exp) {
		long result = 1;
		for (int i = 0; i < exp; i++)
			result *= 10;
		return result;
	}

	int getDigit(char ch) {
		int diff = ch - zero;
		if (diff >= 0 && diff <= 9)
			return diff;
		return Character.digit(ch, 10);
	}
}
