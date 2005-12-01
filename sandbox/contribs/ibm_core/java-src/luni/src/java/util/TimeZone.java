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

package java.util;


import java.io.Serializable;
import java.security.AccessController;
import java.text.DateFormatSymbols;

import com.ibm.oti.util.PriviAction;

/**
 * TimeZone is an abstract class which represents a local time zone and its
 * daylight savings time rules. Subclasses support a particular calendar type,
 * such as the gregorian calendar.
 * 
 * @see GregorianCalendar
 * @see SimpleTimeZone
 */

abstract public class TimeZone implements Serializable, Cloneable {
	static final long serialVersionUID = 3581463369166924961L;

	/**
	 * The SHORT display name style.
	 */
	public static final int SHORT = 0;

	/**
	 * The LONG display name style.
	 */
	public static final int LONG = 1;

	private static HashMap AvailableZones;

	private static TimeZone Default;

	static TimeZone GMT = new SimpleTimeZone(0, "GMT"); // Greenwich Mean Time

	private String ID;

	private static void initializeAvailable() {
		TimeZone[] zones = TimeZones.getTimeZones();
		AvailableZones = new HashMap((zones.length + 1) * 4 / 3);
		AvailableZones.put(GMT.getID(), GMT);
		for (int i = 0; i < zones.length; i++)
			AvailableZones.put(zones[i].getID(), zones[i]);
	}

	/**
	 * Constructs a new instance of this class.
	 * 
	 */
	public TimeZone() {
	}

	private void appendNumber(StringBuffer buffer, int count, int value) {
		String string = Integer.toString(value);
		if (count > string.length())
			for (int i = 0; i < count - string.length(); i++)
				buffer.append('0');
		buffer.append(string);
	}

	/**
	 * Answers a new TimeZone with the same ID, rawOffset and daylight savings
	 * time rules as this TimeZone.
	 * 
	 * @return a shallow copy of this TimeZone
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		try {
			TimeZone zone = (TimeZone) super.clone();
			return zone;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Gets the available time zone IDs.
	 * 
	 * @return an array of time zone ID strings
	 */
	public static synchronized String[] getAvailableIDs() {
		if (AvailableZones == null)
			initializeAvailable();
		int length = AvailableZones.size();
		String[] answer = new String[length];
		Iterator keys = AvailableZones.keySet().iterator();
		for (int i = 0; i < length; i++)
			answer[i] = (String) keys.next();
		return answer;
	}

	/**
	 * Gets the available time zone IDs which match the specified offset from
	 * GMT.
	 * 
	 * @param offset
	 *            the offset from GMT in milliseconds
	 * @return an array of time zone ID strings
	 */
	public static synchronized String[] getAvailableIDs(int offset) {
		if (AvailableZones == null)
			initializeAvailable();
		int count = 0, length = AvailableZones.size();
		String[] all = new String[length];
		Iterator zones = AvailableZones.values().iterator();
		for (int i = 0; i < length; i++) {
			TimeZone tz = (TimeZone) zones.next();
			if (tz.getRawOffset() == offset)
				all[count++] = tz.getID();
		}
		String[] answer = new String[count];
		System.arraycopy(all, 0, answer, 0, count);
		return answer;
	}

	/**
	 * Gets the default time zone.
	 * 
	 * @return the default time zone
	 */
	public static synchronized TimeZone getDefault() {
		if (Default == null)
			setDefault(null);
		return (TimeZone) Default.clone();
	}

	/**
	 * Gets the LONG name for this TimeZone for the default Locale in standard
	 * time. If the name is not available, the result is in the format
	 * GMT[+-]hh:mm.
	 * 
	 * @return the TimeZone name
	 */
	public final String getDisplayName() {
		return getDisplayName(false, LONG, Locale.getDefault());
	}

	/**
	 * Gets the LONG name for this TimeZone for the specified Locale in standard
	 * time. If the name is not available, the result is in the format
	 * GMT[+-]hh:mm.
	 * 
	 * @param locale
	 *            the Locale
	 * @return the TimeZone name
	 */
	public final String getDisplayName(Locale locale) {
		return getDisplayName(false, LONG, locale);
	}

	/**
	 * Gets the specified style of name (LONG or SHORT) for this TimeZone for
	 * the default Locale in either standard or daylight time as specifed. If
	 * the name is not available, the result is in the format GMT[+-]hh:mm.
	 * 
	 * @param daylightTime
	 *            true for daylight time, false for standard time
	 * @param style
	 *            Either LONG or SHORT
	 * @return the TimeZone name
	 */
	public final String getDisplayName(boolean daylightTime, int style) {
		return getDisplayName(daylightTime, style, Locale.getDefault());
	}

	/**
	 * Gets the specified style of name (LONG or SHORT) for this TimeZone for
	 * the specified Locale in either standard or daylight time as specifed. If
	 * the name is not available, the result is in the format GMT[+-]hh:mm.
	 * 
	 * @param daylightTime
	 *            true for daylight time, false for standard time
	 * @param style
	 *            Either LONG or SHORT
	 * @param locale
	 *            the Locale
	 * @return the TimeZone name
	 */
	public String getDisplayName(boolean daylightTime, int style, Locale locale) {
		if (style == SHORT || style == LONG) {
			boolean useDaylight = daylightTime && useDaylightTime();
			DateFormatSymbols data = new DateFormatSymbols(locale);
			String id = getID();
			String[][] zones = data.getZoneStrings();
			for (int i = 0; i < zones.length; i++)
				if (id.equals(zones[i][0]))
					return style == SHORT ? zones[i][useDaylight ? 4 : 2]
							: zones[i][useDaylight ? 3 : 1];
			int offset = getRawOffset();
			if (useDaylight && this instanceof SimpleTimeZone)
				offset += ((SimpleTimeZone) this).getDSTSavings();
			offset /= 60000;
			char sign = '+';
			if (offset < 0) {
				sign = '-';
				offset = -offset;
			}
			StringBuffer buffer = new StringBuffer(9);
			buffer.append("GMT");
			buffer.append(sign);
			appendNumber(buffer, 2, offset / 60);
			buffer.append(':');
			appendNumber(buffer, 2, offset % 60);
			return buffer.toString();
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Gets the ID of this TimeZone.
	 * 
	 * @return the time zone ID string
	 */
	public String getID() {
		return ID;
	}

	/**
	 * Gets the daylight savings offset in milliseconds for this TimeZone.
	 * <p>
	 * This implementation returns 3600000 (1 hour), or 0 if the time zone does
	 * not observe daylight savings.
	 * <p>
	 * Subclasses may override to return daylight savings values other than 1
	 * hour.
	 * <p>
	 * 
	 * @return the daylight savings offset in milliseconds if this Timezone
	 *         observes daylight savings, zero otherwise.
	 * 
	 */
	public int getDSTSavings() {
		if (useDaylightTime()) {
			return 3600000;
		}
		return 0;
	}

	/**
	 * Gets the offset from GMT of this TimeZone for the specified date. The
	 * offset includes daylight savings time if the specified date is within the
	 * daylight savings time period.
	 * 
	 * @param time
	 *            the date in milliseconds since January 1, 1970 00:00:00 GMT
	 * @return the offset from GMT in milliseconds
	 */
	public int getOffset(long time) {
		if (inDaylightTime(new Date(time))) {
			return getRawOffset() + getDSTSavings();
		}
		return getRawOffset();
	}

	/**
	 * Gets the offset from GMT of this TimeZone for the specified date and
	 * time. The offset includes daylight savings time if the specified date and
	 * time are within the daylight savings time period.
	 * 
	 * @param era
	 *            the GregorianCalendar era, either GregorianCalendar.BC or
	 *            GregorianCalendar.AD
	 * @param year
	 *            the year
	 * @param month
	 *            the Calendar month
	 * @param day
	 *            the day of the month
	 * @param dayOfWeek
	 *            the Calendar day of the week
	 * @param time
	 *            the time of day in milliseconds
	 * @return the offset from GMT in milliseconds
	 */
	abstract public int getOffset(int era, int year, int month, int day,
			int dayOfWeek, int time);

	/**
	 * Gets the offset for standard time from GMT for this TimeZone.
	 * 
	 * @return the offset from GMT in milliseconds
	 */
	abstract public int getRawOffset();

	/**
	 * Gets the time zone with the specified ID.
	 * 
	 * @param name
	 *            a time zone string ID
	 * @return the time zone with the specified ID or null if a time zone with
	 *         the specified ID does not exist
	 */
	public static synchronized TimeZone getTimeZone(String name) {
		if (AvailableZones == null)
			initializeAvailable();
		TimeZone zone = (TimeZone) AvailableZones.get(name);
		if (zone == null) {
			if (name.startsWith("GMT") && name.length() > 3) {
				char sign = name.charAt(3);
				if (sign == '+' || sign == '-') {
					int[] position = new int[1];
					String formattedName = formatTimeZoneName(name, 4);
					int hour = parseNumber(formattedName, 4, position);
					if (hour < 0 || hour > 23) {
						return (TimeZone) GMT.clone();
					}
					int index = position[0];
					if (index != -1) {
						int raw = hour * 3600000;
						if (index < formattedName.length()
								&& formattedName.charAt(index) == ':') {
							int minute = parseNumber(formattedName, index + 1,
									position);
							if (position[0] == -1 || minute < 0 || minute > 59)
								return (TimeZone) GMT.clone();
							raw += minute * 60000;
						} else if (hour >= 30 || index > 6) {
							raw = (hour / 100 * 3600000) + (hour % 100 * 60000);
						}
						if (sign == '-')
							raw = -raw;
						return new SimpleTimeZone(raw, formattedName);
					}
				}
			}
			zone = GMT;
		}
		return (TimeZone) zone.clone();
	}

	private static String formatTimeZoneName(String name, int offset) {
		StringBuffer buf = new StringBuffer();
		int index = offset, length = name.length();
		buf.append(name.substring(0, offset));

		while (index < length) {
			if (Character.digit(name.charAt(index), 10) != -1) {
				buf.append(name.charAt(index));
				if ((length - (index + 1)) == 2) {
					buf.append(':');
				}
			} else if (name.charAt(index) == ':') {
				buf.append(':');
			}
			index++;
		}

		if (buf.toString().indexOf(":") == -1) {
			buf.append(':');
			buf.append("00");
		}

		if (buf.toString().indexOf(":") == 5) {
			buf.insert(4, '0');
		}

		return buf.toString();
	}

	/**
	 * Answers if the specified TimeZone has the same raw offset as this
	 * TimeZone.
	 * 
	 * @param zone
	 *            a TimeZone
	 * @return true when the TimeZones have the same raw offset, false otherwise
	 */
	public boolean hasSameRules(TimeZone zone) {
		if (zone == null)
			return false;
		return getRawOffset() == zone.getRawOffset();
	}

	/**
	 * Answers if the specified Date is in the daylight savings time period for
	 * this TimeZone.
	 * 
	 * @param time
	 *            a Date
	 * @return true when the Date is in the daylight savings time period, false
	 *         otherwise
	 */
	abstract public boolean inDaylightTime(Date time);

	private static int parseNumber(String string, int offset, int[] position) {
		int index = offset, length = string.length(), digit, result = 0;
		while (index < length
				&& (digit = Character.digit(string.charAt(index), 10)) != -1) {
			index++;
			result = result * 10 + digit;
		}
		position[0] = index == offset ? -1 : index;
		return result;
	}

	/**
	 * Sets the default time zone.
	 * 
	 * @param timezone
	 *            a TimeZone object
	 */
	public static synchronized void setDefault(TimeZone timezone) {
		if (timezone != null) {
			Default = timezone;
			return;
		}

		String zone = (String) AccessController.doPrivileged(new PriviAction(
				"user.timezone"));

		// if property user.timezone is not set, we call the native method
		// getCustomTimeZone
		if (zone == null) {
			int[] tzinfo = new int[10];
			boolean[] isCustomTimeZone = new boolean[1];

			String zoneId = getCustomTimeZone(tzinfo, isCustomTimeZone);

			// if returned TimeZone is a user customized TimeZone
			if (isCustomTimeZone[0]) {
				// build a new SimpleTimeZone
				switch (tzinfo[1]) {
				case 0:
					// does not observe DST
					Default = new SimpleTimeZone(tzinfo[0], zoneId);
					break;
				default:
					// observes DST
					Default = new SimpleTimeZone(tzinfo[0], zoneId, tzinfo[5],
							tzinfo[4], tzinfo[3], tzinfo[2], tzinfo[9],
							tzinfo[8], tzinfo[7], tzinfo[6], tzinfo[1]);
				}
			} else {
				// get TimeZone
				Default = getTimeZone(zoneId);
			}
		} else {
			// if property user.timezone is set in command line (with -D option)
			Default = getTimeZone(zone);
		}
	}

	/**
	 * Sets the ID of this TimeZone.
	 * 
	 * @param name
	 *            a string which is the time zone ID
	 */
	public void setID(String name) {
		if (name == null)
			throw new NullPointerException();
		ID = name;
	}

	/**
	 * Sets the offset for standard time from GMT for this TimeZone.
	 * 
	 * @param offset
	 *            the offset from GMT in milliseconds
	 */
	abstract public void setRawOffset(int offset);

	/**
	 * Answers if this TimeZone has a daylight savings time period.
	 * 
	 * @return true if this time zone has a daylight savings time period, false
	 *         otherwise
	 */
	abstract public boolean useDaylightTime();

	/**
	 * Gets the name and the details of the user-selected TimeZone on the
	 * device.
	 * 
	 * @param tzinfo
	 *            int array of 10 elements to be filled with the TimeZone
	 *            information. Once filled, the contents of the array are
	 *            formatted as follows: tzinfo[0] -> the timezone offset;
	 *            tzinfo[1] -> the dst adjustment; tzinfo[2] -> the dst start
	 *            hour; tzinfo[3] -> the dst start day of week; tzinfo[4] -> the
	 *            dst start week of month; tzinfo[5] -> the dst start month;
	 *            tzinfo[6] -> the dst end hour; tzinfo[7] -> the dst end day of
	 *            week; tzinfo[8] -> the dst end week of month; tzinfo[9] -> the
	 *            dst end month;
	 * @param isCustomTimeZone
	 *            boolean array of size 1 that indicates if a timezone
	 *            match is found
	 * @return the name of the TimeZone or null if error occurs in native
	 *         method.
	 */
	private static native String getCustomTimeZone(int[] tzinfo,
			boolean[] isCustomTimeZone);
}
