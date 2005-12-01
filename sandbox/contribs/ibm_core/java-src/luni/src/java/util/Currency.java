/* Copyright 2004, 2004 The Apache Software Foundation or its licensors, as applicable
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

package java.util;


import java.io.Serializable;

/**
 * This class represents a currency as identified in the ISO 4217 currency
 * codes.
 */
public final class Currency implements Serializable {

	private static final long serialVersionUID = -158308464356906721L;

	private static Hashtable codesToCurrencies = new Hashtable();

	private String currencyCode;

	private static String currencyVars = "EURO, HK, PREEURO"; //$NON-NLS-1$

	private transient int defaultFractionDigits;

	/**
	 * @param currencyCode
	 */
	private Currency(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	/**
	 * Answers the currency instance for this currency code.
	 * <p>
	 * 
	 * @param currencyCode
	 *            java.lang.String
	 * @return currency java.util.Currency
	 * 
	 * @throws java.lang.IllegalArgumentException
	 *             if the currency code is not a supported ISO 4217 currency
	 *             code
	 */
	public static Currency getInstance(String currencyCode) {
		Currency currency = (Currency) codesToCurrencies.get(currencyCode);

		if (currency == null) {
			ResourceBundle bundle = Locale.getBundle(
					"ISO4CurrenciesToDigits", Locale.getDefault()); //$NON-NLS-1$
			currency = new Currency(currencyCode);

			String defaultFractionDigits = null;
			try {
				defaultFractionDigits = bundle.getString(currencyCode);
			} catch (MissingResourceException e) {
				throw new IllegalArgumentException(com.ibm.oti.util.Msg
						.getString("K0322", currencyCode)); //$NON-NLS-1$
			}
			currency.defaultFractionDigits = Integer
					.parseInt(defaultFractionDigits);
			codesToCurrencies.put(currencyCode, currency);
		}

		return currency;
	}

	/***************************************************************************
	 * Answers the currency instance for this locale.
	 * 
	 * @param locale
	 *            java.util.Locale
	 * @return currency java.util.Currency
	 * 
	 * @throws java.lang.IllegalArgumentException
	 *             if the locale's country is not a supported ISO 3166 Country
	 */
	public static Currency getInstance(Locale locale) {
		String country = locale.getCountry();
		String variant = locale.getVariant();
		if (!variant.equals("") && currencyVars.indexOf(variant) > -1) //$NON-NLS-1$
			country = country + "_" + variant; //$NON-NLS-1$

		ResourceBundle bundle = Locale.getBundle(
				"ISO4Currencies", Locale.getDefault()); //$NON-NLS-1$
		String currencyCode = null;
		try {
			currencyCode = bundle.getString(country);
		} catch (MissingResourceException e) {
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString(
					"K0323", locale.toString())); //$NON-NLS-1$
		}

		if (currencyCode.equals("None")) //$NON-NLS-1$
			return null;

		return getInstance(currencyCode);
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public String getSymbol() {
		return getSymbol(Locale.getDefault());
	}

	/***************************************************************************
	 * Return the symbol for this currency in the given locale.
	 * <p>
	 * 
	 * If the locale doesn't have any countries (e.g.
	 * <code>Locale.JAPANESE, new Locale("en","")</code>), currencyCode is
	 * returned.
	 * <p>
	 * First the locale bundle is checked, if the locale has the same currency,
	 * the CurrencySymbol in this locale bundle is returned.
	 * <p>
	 * Then a currency bundle for this locale is searched.
	 * <p>
	 * If a currency bundle for this locale does not exist, or there is no
	 * symbol for this currency in this bundle, than <code>currencyCode</code>
	 * is returned.
	 * <p>
	 * 
	 * @param locale
	 *            java.lang.String locale
	 * @return symbol java.lang.String the representation of this Currency's
	 *         symbol in this locale
	 */
	public String getSymbol(Locale locale) {
		if (locale.getCountry().equals("")) //$NON-NLS-1$
			return currencyCode;

		// check in the Locale bundle first, if the local has the same currency
		ResourceBundle bundle = Locale.getBundle("Locale", locale); //$NON-NLS-1$
		if (((String) bundle.getObject("IntCurrencySymbol")).equals(currencyCode)) //$NON-NLS-1$
			return (String) bundle.getObject("CurrencySymbol"); //$NON-NLS-1$

		// search for a Currency bundle
		bundle = null;
		try {
			bundle = Locale.getBundle("Currency", locale); //$NON-NLS-1$
		} catch (MissingResourceException e) {
			return currencyCode;
		}

		// is the bundle found for a different country? (for instance the
		// default locale's currency bundle)
		if (!bundle.getLocale().getCountry().equals(locale.getCountry()))
			return currencyCode;

		// check if the currency bundle for this locale
		// has an entry for this currency
		String result = (String) bundle.handleGetObject(currencyCode);
		if (result != null)
			return result;
		else
			return currencyCode;
	}

	public int getDefaultFractionDigits() {
		return defaultFractionDigits;
	}

	public String toString() {
		return currencyCode;
	}

	private Object readResolve() {
		return getInstance(currencyCode);
	}
}
