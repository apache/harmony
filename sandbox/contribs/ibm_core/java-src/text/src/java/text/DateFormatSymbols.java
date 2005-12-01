/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
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


import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * DateFormatSymbols holds the Strings used in the formating and parsing of
 * dates and times.
 */
public class DateFormatSymbols implements Serializable, Cloneable {

	static final long serialVersionUID = -5987973545549424702L;

	private String localPatternChars;

	String[] ampms, eras, months, shortMonths, shortWeekdays, weekdays;

	String[][] zoneStrings;

	/**
	 * Constructs a new DateFormatSymbols containing the symbols for the default
	 * Locale.
	 */
	public DateFormatSymbols() {
		this(Locale.getDefault());
	}

	/**
	 * Constructs a new DateFormatSymbols containing the symbols for the
	 * specified Locale.
	 * 
	 * @param locale
	 *            the Locale
	 */
	public DateFormatSymbols(Locale locale) {
		ResourceBundle bundle = Format.getBundle(locale);
		localPatternChars = bundle.getString("LocalPatternChars");
		ampms = bundle.getStringArray("ampm");
		eras = bundle.getStringArray("eras");
		months = bundle.getStringArray("months");
		shortMonths = bundle.getStringArray("shortMonths");
		shortWeekdays = bundle.getStringArray("shortWeekdays");
		weekdays = bundle.getStringArray("weekdays");
		zoneStrings = (String[][]) bundle.getObject("timezones");
	}

	/**
	 * Answers a new DateFormatSymbols with the same symbols as this
	 * DateFormatSymbols.
	 * 
	 * @return a shallow copy of this DateFormatSymbols
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		try {
			DateFormatSymbols symbols = (DateFormatSymbols) super.clone();
			symbols.ampms = (String[]) ampms.clone();
			symbols.eras = (String[]) eras.clone();
			symbols.months = (String[]) months.clone();
			symbols.shortMonths = (String[]) shortMonths.clone();
			symbols.shortWeekdays = (String[]) shortWeekdays.clone();
			symbols.weekdays = (String[]) weekdays.clone();
			symbols.zoneStrings = new String[zoneStrings.length][];
			for (int i = 0; i < zoneStrings.length; i++)
				symbols.zoneStrings[i] = (String[]) zoneStrings[i].clone();
			return symbols;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Compares the specified object to this DateFormatSymbols and answer if
	 * they are equal. The object must be an instance of DateFormatSymbols with
	 * the same symbols.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return true if the specified object is equal to this DateFormatSymbols,
	 *         false otherwise
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (!(object instanceof DateFormatSymbols))
			return false;
		DateFormatSymbols obj = (DateFormatSymbols) object;
		if (!localPatternChars.equals(obj.localPatternChars))
			return false;
		if (!Arrays.equals(ampms, obj.ampms))
			return false;
		if (!Arrays.equals(eras, obj.eras))
			return false;
		if (!Arrays.equals(months, obj.months))
			return false;
		if (!Arrays.equals(shortMonths, obj.shortMonths))
			return false;
		if (!Arrays.equals(shortWeekdays, obj.shortWeekdays))
			return false;
		if (!Arrays.equals(weekdays, obj.weekdays))
			return false;
		if (zoneStrings.length != obj.zoneStrings.length)
			return false;
		for (int i = 0; i < zoneStrings.length; i++) {
			if (zoneStrings[i].length != obj.zoneStrings[i].length)
				return false;
			for (int j = 0; j < zoneStrings[i].length; j++)
				if (zoneStrings[i][j] != obj.zoneStrings[i][j]
						&& !(zoneStrings[i][j].equals(obj.zoneStrings[i][j])))
					return false;
		}
		return true;
	}

	/**
	 * Answers the array of Strings which represent AM and PM. Use the Calendar
	 * constants Calendar.AM and Calendar.PM to index into the array.
	 * 
	 * @return an array of String
	 */
	public String[] getAmPmStrings() {
		return (String[]) ampms.clone();
	}

	/**
	 * Answers the array of Strings which represent BC and AD. Use the Calendar
	 * constants GregorianCalendar.BC and GregorianCalendar.AD to index into the
	 * array.
	 * 
	 * @return an array of String
	 */
	public String[] getEras() {
		return (String[]) eras.clone();
	}

	/**
	 * Answers the pattern characters used by SimpleDateFormat to specify date
	 * and time fields.
	 * 
	 * @return a String containing the pattern characters
	 */
	public String getLocalPatternChars() {
		return localPatternChars;
	}

	/**
	 * Answers the array of Strings containing the full names of the months. Use
	 * the Calendar constants Calendar.JANUARY, etc. to index into the array.
	 * 
	 * @return an array of String
	 */
	public String[] getMonths() {
		return (String[]) months.clone();
	}

	/**
	 * Answers the array of Strings containing the abbreviated names of the
	 * months. Use the Calendar constants Calendar.JANUARY, etc. to index into
	 * the array.
	 * 
	 * @return an array of String
	 */
	public String[] getShortMonths() {
		return (String[]) shortMonths.clone();
	}

	/**
	 * Answers the array of Strings containing the abbreviated names of the days
	 * of the week. Use the Calendar constants Calendar.SUNDAY, etc. to index
	 * into the array.
	 * 
	 * @return an array of String
	 */
	public String[] getShortWeekdays() {
		return (String[]) shortWeekdays.clone();
	}

	/**
	 * Answers the array of Strings containing the full names of the days of the
	 * week. Use the Calendar constants Calendar.SUNDAY, etc. to index into the
	 * array.
	 * 
	 * @return an array of String
	 */
	public String[] getWeekdays() {
		return (String[]) weekdays.clone();
	}

	/**
	 * Answers the two-dimensional array of Strings containing the names of the
	 * timezones. Each element in the array is an array of five Strings, the
	 * first is a TimeZone ID, and second and third are the full and abbreviated
	 * timezone names for standard time, and the fourth and fifth are the full
	 * and abbreviated names for daylight time.
	 * 
	 * @return a two-dimensional array of String
	 */
	public String[][] getZoneStrings() {
		String[][] clone = new String[zoneStrings.length][];
		for (int i = zoneStrings.length; --i >= 0;)
			clone[i] = (String[]) zoneStrings[i].clone();
		return clone;
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
		int hashCode;
		hashCode = localPatternChars.hashCode();
		for (int i = 0; i < ampms.length; i++)
			hashCode += ampms[i].hashCode();
		for (int i = 0; i < eras.length; i++)
			hashCode += eras[i].hashCode();
		for (int i = 0; i < months.length; i++)
			hashCode += months[i].hashCode();
		for (int i = 0; i < shortMonths.length; i++)
			hashCode += shortMonths[i].hashCode();
		for (int i = 0; i < shortWeekdays.length; i++)
			hashCode += shortWeekdays[i].hashCode();
		for (int i = 0; i < weekdays.length; i++)
			hashCode += weekdays[i].hashCode();
		for (int i = 0; i < zoneStrings.length; i++) {
			for (int j = 0; j < zoneStrings[i].length; j++)
				hashCode += zoneStrings[i][j].hashCode();
		}
		return hashCode;
	}

	/**
	 * Sets the array of Strings which represent AM and PM. Use the Calendar
	 * constants Calendar.AM and Calendar.PM to index into the array.
	 * 
	 * @param data
	 *            the array of Strings
	 */
	public void setAmPmStrings(String[] data) {
		ampms = (String[]) data.clone();
	}

	/**
	 * Sets the array of Strings which represent BC and AD. Use the Calendar
	 * constants GregorianCalendar.BC and GregorianCalendar.AD to index into the
	 * array.
	 * 
	 * @param data
	 *            the array of Strings
	 */
	public void setEras(String[] data) {
		eras = (String[]) data.clone();
	}

	/**
	 * Sets the pattern characters used by SimpleDateFormat to specify date and
	 * time fields.
	 * 
	 * @param data
	 *            the String containing the pattern characters
	 */
	public void setLocalPatternChars(String data) {
		localPatternChars = data;
	}

	/**
	 * Sets the array of Strings containing the full names of the months. Use
	 * the Calendar constants Calendar.JANUARY, etc. to index into the array.
	 * 
	 * @param data
	 *            the array of Strings
	 */
	public void setMonths(String[] data) {
		months = (String[]) data.clone();
	}

	/**
	 * Sets the array of Strings containing the abbreviated names of the months.
	 * Use the Calendar constants Calendar.JANUARY, etc. to index into the
	 * array.
	 * 
	 * @param data
	 *            the array of Strings
	 */
	public void setShortMonths(String[] data) {
		shortMonths = (String[]) data.clone();
	}

	/**
	 * Sets the array of Strings containing the abbreviated names of the days of
	 * the week. Use the Calendar constants Calendar.SUNDAY, etc. to index into
	 * the array.
	 * 
	 * @param data
	 *            the array of Strings
	 */
	public void setShortWeekdays(String[] data) {
		shortWeekdays = (String[]) data.clone();
	}

	/**
	 * Sets the array of Strings containing the full names of the days of the
	 * week. Use the Calendar constants Calendar.SUNDAY, etc. to index into the
	 * array.
	 * 
	 * @param data
	 *            the array of Strings
	 */
	public void setWeekdays(String[] data) {
		weekdays = (String[]) data.clone();
	}

	/**
	 * Sets the two-dimensional array of Strings containing the names of the
	 * timezones. Each element in the array is an array of five Strings, the
	 * first is a TimeZone ID, and second and third are the full and abbreviated
	 * timezone names for standard time, and the fourth and fifth are the full
	 * and abbreviated names for daylight time.
	 * 
	 * @param data
	 *            the two-dimensional array of Strings
	 */
	public void setZoneStrings(String[][] data) {
		zoneStrings = (String[][]) data.clone();
	}
}
