/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Dennis Ushakov
 */
package javax.swing;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class SpinnerDateModel extends AbstractSpinnerModel implements Serializable {

    private static final int DEFAULT_CALENDAR_FIELD = Calendar.DAY_OF_MONTH;
    private Date value;
    private Comparable start;
    private Comparable end;
    private int calendarField;

    public SpinnerDateModel() {
        this(new Date(), null, null, DEFAULT_CALENDAR_FIELD);
    }

    public SpinnerDateModel(final Date value, final Comparable start,
                            final Comparable end, final int calendarField) {
        if (value == null) {
            throw new IllegalArgumentException(Messages.getString("swing.03","value")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (start != null && start.compareTo(value) > 0) {
            throw new IllegalArgumentException(Messages.getString("swing.59")); //$NON-NLS-1$
        }
        if (end != null && end.compareTo(value) < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.59")); //$NON-NLS-1$
        }
        if (!isValidCalendarField(calendarField)) {
            throw new IllegalArgumentException(Messages.getString("swing.5A")); //$NON-NLS-1$
        }

        this.value = value;
        this.start = start;
        this.end = end;
        this.calendarField = calendarField;
    }

    public void setStart(final Comparable start) {
        if (this.start != start) {
            this.start = start;
            fireStateChanged();
        }
    }

    public Comparable getStart() {
        return start;
    }

    public void setEnd(final Comparable end) {
        if (this.end != end) {
            this.end = end;
            fireStateChanged();
        }
    }

    public Comparable getEnd() {
        return end;
    }

    public void setCalendarField(final int calendarField) {
        if (!isValidCalendarField(calendarField)) {
            throw new IllegalArgumentException(Messages.getString("swing.5A")); //$NON-NLS-1$
        }
        if (this.calendarField != calendarField) {
            this.calendarField = calendarField;
            fireStateChanged();
        }
    }

    public int getCalendarField() {
        return calendarField;
    }

    public Object getNextValue() {
        return TextUtils.getNextValue(value, calendarField, end);
    }

    public Object getPreviousValue() {
        return TextUtils.getPreviousValue(value, calendarField, start);
    }

    public Date getDate() {
        return (Date)value.clone();
    }

    public Object getValue() {
        return getDate();
    }

    public void setValue(final Object value) {
        if (!(value instanceof Date)) {
            throw new IllegalArgumentException(Messages.getString("swing.5B")); //$NON-NLS-1$
        }
        if (this.value != value) {
            this.value = (Date)value;
            fireStateChanged();
        }
    }

    private boolean isValidCalendarField(final int calendarField) {
        switch (calendarField) {
        case Calendar.ERA:
        case Calendar.YEAR:
        case Calendar.MONTH:
        case Calendar.WEEK_OF_YEAR:
        case Calendar.WEEK_OF_MONTH:
        case Calendar.DAY_OF_MONTH:
        case Calendar.DAY_OF_YEAR:
        case Calendar.DAY_OF_WEEK:
        case Calendar.DAY_OF_WEEK_IN_MONTH:
        case Calendar.AM_PM:
        case Calendar.HOUR:
        case Calendar.HOUR_OF_DAY:
        case Calendar.MINUTE:
        case Calendar.SECOND:
        case Calendar.MILLISECOND: return true;
        default: return false;
        }
    }
}
