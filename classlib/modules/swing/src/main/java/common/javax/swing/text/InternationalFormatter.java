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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text;

import java.awt.event.ActionEvent;
import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner.DateEditor;

import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;



public class InternationalFormatter extends DefaultFormatter {
    private static final String DECREMENT_ACTION_NAME = "decrement";
    private static final String INCREMENT_ACTION_NAME = "increment";
    private Comparable max;
    private Comparable min;
    private Format format;
    private static Action[] actions;

    static class IncrementAction extends TextAction {
        private int increment;
        public IncrementAction(final String name, final int increment) {
            super(name);
            this.increment = increment;
        }

        public void actionPerformed(final ActionEvent e) {
            JTextComponent source = getTextComponent(e);
            if (source instanceof JFormattedTextField) {
                handleText((JFormattedTextField) source);
            }
        }


        private void handleText(final JFormattedTextField ftf) {
            if (ftf.getFormatter() instanceof DateFormatter) {
                try {
                    DateFormatter formatter = (DateFormatter)ftf.getFormatter();
                    int calendarField = TextUtils.getCalendarField(ftf);
                    Date date = (Date) formatter.stringToValue(ftf.getText());
                    date = increment > 0
                      ? (Date)TextUtils.getNextValue(date, calendarField, null)
                      : (Date)TextUtils.getPreviousValue(date, calendarField,
                                                            null);
                    ftf.setText(formatter.valueToString(date));
                    TextUtils.selectCalendarField(ftf, calendarField);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public InternationalFormatter() {
        this(null);
    }

    public InternationalFormatter(final Format format) {
        this.format = format;
        setOverwriteMode(false);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Format.Field[] getFields(final int offset) {
        if (format == null) {
            return null;        }

        Object value = getFormattedTextField().getValue();
        if (value == null) {
            return null;
        }

        AttributedCharacterIterator iterator
            = format.formatToCharacterIterator(value);
        if (offset < iterator.getBeginIndex()
                || offset > iterator.getEndIndex()) {
            return new Format.Field[0];
        }

        iterator.setIndex(offset);
        Set keys = iterator.getAttributes().keySet();
        Set result = new HashSet();
        Iterator iter = keys.iterator();

        while (iter.hasNext()) {
            Object key = iter.next();
            if (key instanceof Format.Field) {
                result.add(key);
            }
        }

        return (Format.Field[])result.toArray(new Format.Field[result.size()]);
    }

    protected Action[] getActions() {
        if (actions ==  null) {
            actions = new TextAction[] {
                new IncrementAction(INCREMENT_ACTION_NAME, 1),
                new IncrementAction(DECREMENT_ACTION_NAME, -1)
            };
        }
        return (Action[])actions.clone();
    }

    public Format getFormat() {
        return format;
    }


    public Comparable getMaximum() {
        return max;
    }

    public Comparable getMinimum() {
        return min;
    }

    public void install(final JFormattedTextField ftf) {
        super.install(ftf);
    }

    public void setFormat(final Format format) {
        this.format = format;
    }

    public void setMaximum(final Comparable max) {
        this.max = max;
        if (max != null) {
           setValueClass(max.getClass());
        }
    }

    public void setMinimum(final Comparable min) {
        this.min = min;
        if (min != null) {
            setValueClass(min.getClass());
        }
    }

    public Object stringToValue(final String string) throws ParseException {
        if (string == null) {
            return null;
        }
        Object result =null;
        if (getValueClass() == null) {
            result = format != null ? format.parseObject(string) : string;
        } else {
            result = super.stringToValue(string);
        }

        if (!checkRange(result)) {
            throw new ParseException(Messages.getString("swing.8F"), 0); //$NON-NLS-1$
        }
        return result;
    }

    private boolean checkRange(final Object value) {
        boolean result = false;
        try {
            result = (min == null || min.compareTo(value) <= 0) &&
                     (max == null || max.compareTo(value) >= 0);
        } catch (ClassCastException e) {

        }
        return result;
    }

    public String valueToString(final Object value) throws ParseException {
        if (value == null) {
            return "";
        } else {
           return format != null ? format.format(value) :  value.toString();
        }
    }

    final String getFormattedText(final String text) {
        Object value = null;
        try {
            value = stringToValue(text);
        } catch (ParseException e) {
        }
        if (value == null) {
            return "";
        }

        return (format != null) ? format.format(value) : text;
    }
}


