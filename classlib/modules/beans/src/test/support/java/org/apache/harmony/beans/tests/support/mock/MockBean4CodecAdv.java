/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.beans.tests.support.mock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

public class MockBean4CodecAdv extends MockBean4Codec {

    String id;

    ArrayList<Object> arraylist = new ArrayList<Object>();

    Calendar calendar = Calendar.getInstance();

    Currency currency = Currency.getInstance(Locale.US);

    Date date = new Date();

    Locale locale = Locale.US;

    Properties properties = new Properties();

    TimeZone timezone = TimeZone.getTimeZone("GMT+8");

    /**
     * 
     */
    public MockBean4CodecAdv(String id) {
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * @return Returns the arraylist.
     */
    public ArrayList<Object> getArraylist() {
        return arraylist;
    }

    /**
     * @param arraylist
     *            The arraylist to set.
     */
    public void setArraylist(ArrayList<Object> arraylist) {
        this.arraylist = arraylist;
    }

    /**
     * @return Returns the calendar.
     */
    public Calendar getCalendar() {
        return calendar;
    }

    /**
     * @param calendar
     *            The calendar to set.
     */
    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    /**
     * @return Returns the currency.
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * @param currency
     *            The currency to set.
     */
    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    /**
     * @return Returns the date.
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date
     *            The date to set.
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * @return Returns the locale.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @param locale
     *            The locale to set.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @return Returns the properties.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * @param properties
     *            The properties to set.
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * @return Returns the timezone.
     */
    public TimeZone getTimezone() {
        return timezone;
    }

    /**
     * @param timezone
     *            The timezone to set.
     */
    public void setTimezone(TimeZone timezone) {
        this.timezone = timezone;
    }
}
