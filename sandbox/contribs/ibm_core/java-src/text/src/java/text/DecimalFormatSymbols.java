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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * DecimalFormatSymbols holds the symbols used in the formating and parsing of
 * numbers.
 */
public final class DecimalFormatSymbols implements Cloneable, Serializable {

	static final long serialVersionUID = 5772796243397350300L;

	private final int ZeroDigit = 0, Digit = 1, DecimalSeparator = 2,
			GroupingSeparator = 3, PatternSeparator = 4, Percent = 5,
			PerMill = 6, Exponent = 7, MonetaryDecimalSeparator = 8,
			MinusSign = 9;

	transient char[] patternChars;

	private transient Currency currency;

	private transient Locale locale;

	private String infinity, NaN, currencySymbol, intlCurrencySymbol;

	/**
	 * Constructs a new DecimalFormatSymbols containing the symbols for the
	 * default Locale.
	 */
	public DecimalFormatSymbols() {
		this(Locale.getDefault());
	}

	/**
	 * Constructs a new DecimalFormatSymbols containing the symbols for the
	 * specified Locale.
	 * 
	 * @param locale
	 *            the Locale
	 */
	public DecimalFormatSymbols(Locale locale) {
		ResourceBundle bundle = Format.getBundle(locale);
		patternChars = bundle.getString("DecimalPatternChars").toCharArray();
		infinity = bundle.getString("Infinity");
		NaN = bundle.getString("NaN");
		this.locale = locale;
		try {
			currency = Currency.getInstance(locale);
			currencySymbol = currency.getSymbol(locale);
			intlCurrencySymbol = currency.getCurrencyCode();
		} catch (IllegalArgumentException e) {
			currency = Currency.getInstance("XXX");
			currencySymbol = bundle.getString("CurrencySymbol");
			intlCurrencySymbol = bundle.getString("IntCurrencySymbol");
		}
	}

	/**
	 * Answers a new DecimalFormatSymbols with the same symbols as this
	 * DecimalFormatSymbols.
	 * 
	 * @return a shallow copy of this DecimalFormatSymbols
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		try {
			DecimalFormatSymbols symbols = (DecimalFormatSymbols) super.clone();
			symbols.patternChars = (char[]) patternChars.clone();
			return symbols;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Compares the specified object to this DecimalFormatSymbols and answer if
	 * they are equal. The object must be an instance of DecimalFormatSymbols
	 * with the same symbols.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return true if the specified object is equal to this
	 *         DecimalFormatSymbols, false otherwise
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (!(object instanceof DecimalFormatSymbols))
			return false;
		DecimalFormatSymbols obj = (DecimalFormatSymbols) object;
		return Arrays.equals(patternChars, obj.patternChars)
				&& infinity.equals(obj.infinity) && NaN.equals(obj.NaN)
				&& currencySymbol.equals(obj.currencySymbol)
				&& intlCurrencySymbol.equals(obj.intlCurrencySymbol);
	}

	/**
	 * Answers the currency.
	 * <p>
	 * <code>null<code> is returned
	 * if <code>setInternationalCurrencySymbol()</code> has been previously called
	 * with a value that is not a valid ISO 4217 currency code.
	 * <p>
	 *
	 * @return		the currency that was set in the constructor, <code>setCurrency()</code>,
	 * 				or <code>setInternationalCurrencySymbol()</code>, or </code>null</code>
	 * 
	 * @see #setCurrency(Currency)
	 * @see #setInternationalCurrencySymbol(String)
	 */
	public Currency getCurrency() {
		return currency;
	}

	/**
	 * Answers the international currency symbol.
	 * 
	 * @return a String
	 */
	public String getInternationalCurrencySymbol() {
		return intlCurrencySymbol;
	}

	/**
	 * Answers the currency symbol.
	 * 
	 * @return a String
	 */
	public String getCurrencySymbol() {
		return currencySymbol;
	}

	/**
	 * Answers the character which represents the decimal point in a number.
	 * 
	 * @return a char
	 */
	public char getDecimalSeparator() {
		return patternChars[DecimalSeparator];
	}

	/**
	 * Answers the character which represents a single digit in a format
	 * pattern.
	 * 
	 * @return a char
	 */
	public char getDigit() {
		return patternChars[Digit];
	}

	/**
	 * Answers the character used as the thousands separator in a number.
	 * 
	 * @return a char
	 */
	public char getGroupingSeparator() {
		return patternChars[GroupingSeparator];
	}

	/**
	 * Answers the String which represents infinity.
	 * 
	 * @return a String
	 */
	public String getInfinity() {
		return infinity;
	}

	String getLocalPatternChars() {
		// Don't include the MonetaryDecimalSeparator or the MinusSign
		return new String(patternChars, 0, patternChars.length - 2);
	}

	/**
	 * Answers the minus sign character.
	 * 
	 * @return a char
	 */
	public char getMinusSign() {
		return patternChars[MinusSign];
	}

	/**
	 * Answers the character which represents the decimal point in a monetary
	 * value.
	 * 
	 * @return a char
	 */
	public char getMonetaryDecimalSeparator() {
		return patternChars[MonetaryDecimalSeparator];
	}

	/**
	 * Answers the String which represents NaN.
	 * 
	 * @return a String
	 */
	public String getNaN() {
		return NaN;
	}

	/**
	 * Answers the character which separates the positive and negative patterns
	 * in a format pattern.
	 * 
	 * @return a char
	 */
	public char getPatternSeparator() {
		return patternChars[PatternSeparator];
	}

	/**
	 * Answers the percent character.
	 * 
	 * @return a char
	 */
	public char getPercent() {
		return patternChars[Percent];
	}

	/**
	 * Answers the mille percent sign character.
	 * 
	 * @return a char
	 */
	public char getPerMill() {
		return patternChars[PerMill];
	}

	/**
	 * Answers the character which represents zero.
	 * 
	 * @return a char
	 */
	public char getZeroDigit() {
		return patternChars[ZeroDigit];
	}

	char getExponential() {
		return patternChars[Exponent];
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
		return new String(patternChars).hashCode() + infinity.hashCode()
				+ NaN.hashCode() + currencySymbol.hashCode()
				+ intlCurrencySymbol.hashCode();
	}

	/**
	 * Sets the currency.
	 * <p>
	 * The international currency symbol and currency symbol are updated, but
	 * the min and max number of fraction digits stay the same.
	 * <p>
	 * 
	 * @param currency
	 *            the new currency
	 * 
	 * @throws java.lang.NullPointerException
	 *             if currency is null
	 */
	public void setCurrency(Currency currency) {
		if (currency == null)
			throw new NullPointerException();
		if (currency == this.currency)
			return;
		this.currency = currency;
		intlCurrencySymbol = currency.getCurrencyCode();
		currencySymbol = currency.getSymbol(locale);
	}

	/**
	 * Sets the international currency symbol.
	 * <p>
	 * currency and currency symbol also are updated, if <code>value</code> is
	 * a valid ISO4217 currency code.
	 * <p>
	 * The min and max number of fraction digits stay the same.
	 * 
	 * @param value
	 *            currency code
	 */
	public void setInternationalCurrencySymbol(String value) {
		if (value == null) {
			currency = null;
			intlCurrencySymbol = null;
			return;
		}

		if (value.equals(intlCurrencySymbol))
			return;

		try {
			currency = Currency.getInstance(value);
			currencySymbol = currency.getSymbol(locale);
		} catch (IllegalArgumentException e) {
			currency = null;
		}
		intlCurrencySymbol = value;
	}

	/**
	 * Sets the currency symbol.
	 * 
	 * @param value
	 *            a String
	 */
	public void setCurrencySymbol(String value) {
		currencySymbol = value;
	}

	/**
	 * Sets the character which represents the decimal point in a number.
	 * 
	 * @param value
	 *            the decimal separator character
	 */
	public void setDecimalSeparator(char value) {
		patternChars[DecimalSeparator] = value;
	}

	/**
	 * Sets the character which represents a single digit in a format pattern.
	 * 
	 * @param value
	 *            the digit character
	 */
	public void setDigit(char value) {
		patternChars[Digit] = value;
	}

	/**
	 * Sets the character used as the thousands separator in a number.
	 * 
	 * @param value
	 *            the grouping separator character
	 */
	public void setGroupingSeparator(char value) {
		patternChars[GroupingSeparator] = value;
	}

	/**
	 * Sets the String which represents infinity.
	 * 
	 * @param value
	 *            the String
	 */
	public void setInfinity(String value) {
		infinity = value;
	}

	/**
	 * Sets the minus sign character.
	 * 
	 * @param value
	 *            the minus sign character
	 */
	public void setMinusSign(char value) {
		patternChars[MinusSign] = value;
	}

	/**
	 * Sets the character which represents the decimal point in a monetary
	 * value.
	 * 
	 * @param value
	 *            the monetary decimal separator character
	 */
	public void setMonetaryDecimalSeparator(char value) {
		patternChars[MonetaryDecimalSeparator] = value;
	}

	/**
	 * Sets the String which represents NaN.
	 * 
	 * @param value
	 *            the String
	 */
	public void setNaN(String value) {
		NaN = value;
	}

	/**
	 * Sets the character which separates the positive and negative patterns in
	 * a format pattern.
	 * 
	 * @param value
	 *            the pattern separator character
	 */
	public void setPatternSeparator(char value) {
		patternChars[PatternSeparator] = value;
	}

	/**
	 * Sets the percent character.
	 * 
	 * @param value
	 *            the percent character
	 */
	public void setPercent(char value) {
		patternChars[Percent] = value;
	}

	/**
	 * Sets the mille percent sign character.
	 * 
	 * @param value
	 *            the mille percent character
	 */
	public void setPerMill(char value) {
		patternChars[PerMill] = value;
	}

	/**
	 * Sets the character which represents zero.
	 * 
	 * @param value
	 *            the zero digit character
	 */
	public void setZeroDigit(char value) {
		patternChars[ZeroDigit] = value;
	}

	void setExponential(char value) {
		patternChars[Exponent] = value;
	}

	private static final ObjectStreamField[] serialPersistentFields = {
			new ObjectStreamField("currencySymbol", String.class),
			new ObjectStreamField("decimalSeparator", Character.TYPE),
			new ObjectStreamField("digit", Character.TYPE),
			new ObjectStreamField("exponential", Character.TYPE),
			new ObjectStreamField("groupingSeparator", Character.TYPE),
			new ObjectStreamField("infinity", String.class),
			new ObjectStreamField("intlCurrencySymbol", String.class),
			new ObjectStreamField("minusSign", Character.TYPE),
			new ObjectStreamField("monetarySeparator", Character.TYPE),
			new ObjectStreamField("NaN", String.class),
			new ObjectStreamField("patternSeparator", Character.TYPE),
			new ObjectStreamField("percent", Character.TYPE),
			new ObjectStreamField("perMill", Character.TYPE),
			new ObjectStreamField("serialVersionOnStream", Integer.TYPE),
			new ObjectStreamField("zeroDigit", Character.TYPE), };

	private void writeObject(ObjectOutputStream stream) throws IOException {
		ObjectOutputStream.PutField fields = stream.putFields();
		fields.put("currencySymbol", currencySymbol);
		fields.put("decimalSeparator", getDecimalSeparator());
		fields.put("digit", getDigit());
		fields.put("exponential", getExponential());
		fields.put("groupingSeparator", getGroupingSeparator());
		fields.put("infinity", infinity);
		fields.put("intlCurrencySymbol", intlCurrencySymbol);
		fields.put("minusSign", getMinusSign());
		fields.put("monetarySeparator", getMonetaryDecimalSeparator());
		fields.put("NaN", NaN);
		fields.put("patternSeparator", getPatternSeparator());
		fields.put("percent", getPercent());
		fields.put("perMill", getPerMill());
		fields.put("serialVersionOnStream", 1);
		fields.put("zeroDigit", getZeroDigit());
		stream.writeFields();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		ObjectInputStream.GetField fields = stream.readFields();
		patternChars = new char[10];
		currencySymbol = (String) fields.get("currencySymbol", "");
		setDecimalSeparator(fields.get("decimalSeparator", '.'));
		setDigit(fields.get("digit", '#'));
		setGroupingSeparator(fields.get("groupingSeparator", ','));
		infinity = (String) fields.get("infinity", "");
		intlCurrencySymbol = (String) fields.get("intlCurrencySymbol", "");
		setMinusSign(fields.get("minusSign", '-'));
		NaN = (String) fields.get("NaN", "");
		setPatternSeparator(fields.get("patternSeparator", ';'));
		setPercent(fields.get("percent", '%'));
		setPerMill(fields.get("perMill", '\u2030'));
		setZeroDigit(fields.get("zeroDigit", '0'));
		if (fields.get("serialVersionOnStream", 0) == 0) {
			setMonetaryDecimalSeparator(getDecimalSeparator());
			setExponential('E');
		} else {
			setMonetaryDecimalSeparator(fields.get("monetarySeparator", '.'));
			setExponential(fields.get("exponential", 'E'));
		}
	}
}
