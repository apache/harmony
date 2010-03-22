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

import java.io.Serializable;
import javax.swing.JFormattedTextField;

public class DefaultFormatterFactory extends
    JFormattedTextField.AbstractFormatterFactory implements Serializable {

    private JFormattedTextField.AbstractFormatter nullFormatter;
    private JFormattedTextField.AbstractFormatter editFormatter;
    private JFormattedTextField.AbstractFormatter displayFormatter;
    private JFormattedTextField.AbstractFormatter defaultFormatter;

    public DefaultFormatterFactory() {
    }

    public DefaultFormatterFactory
         (final JFormattedTextField.AbstractFormatter defaultFormatter,
          final JFormattedTextField.AbstractFormatter displayFormatter,
          final JFormattedTextField.AbstractFormatter editFormatter) {
        this(defaultFormatter, displayFormatter, editFormatter, null);
    }

    public DefaultFormatterFactory
        (final JFormattedTextField.AbstractFormatter defaultFormatter,
         final JFormattedTextField.AbstractFormatter displayFormatter) {
        this(defaultFormatter, displayFormatter, null, null);
    }

    public DefaultFormatterFactory
        (final JFormattedTextField.AbstractFormatter defaultFormatter,
         final JFormattedTextField.AbstractFormatter displayFormatter,
         final JFormattedTextField.AbstractFormatter editFormatter,
         final JFormattedTextField.AbstractFormatter nullFormatter) {
        this.defaultFormatter = defaultFormatter;
        this.displayFormatter = displayFormatter;
        this.editFormatter = editFormatter;
        this.nullFormatter = nullFormatter;
    }

    public DefaultFormatterFactory
        (final JFormattedTextField.AbstractFormatter defaultFormatter) {
        this(defaultFormatter, null, null, null);
    }

    public JFormattedTextField.AbstractFormatter getDefaultFormatter() {
        return defaultFormatter;
    }

    public JFormattedTextField.AbstractFormatter getDisplayFormatter() {
        return displayFormatter;
    }

    public JFormattedTextField.AbstractFormatter getEditFormatter() {
        return editFormatter;
    }

    public JFormattedTextField.AbstractFormatter
        getFormatter(final JFormattedTextField ftf) {
        if (ftf.getValue() == null && nullFormatter != null) {
            return nullFormatter;
        }
        boolean hasFocus = ftf.hasFocus();
        JFormattedTextField.AbstractFormatter formatter =
            hasFocus ? editFormatter : displayFormatter;

        return (formatter != null) ? formatter : defaultFormatter;
    }

    public JFormattedTextField.AbstractFormatter getNullFormatter() {
        return nullFormatter;
    }

    public void setDefaultFormatter
        (final JFormattedTextField.AbstractFormatter formatter) {
        defaultFormatter = formatter;
    }

    public void setDisplayFormatter
        (final JFormattedTextField.AbstractFormatter formatter) {
        displayFormatter = formatter;
    }

    public void setEditFormatter
        (final JFormattedTextField.AbstractFormatter formatter) {
        editFormatter = formatter;
    }

    public void setNullFormatter
        (final JFormattedTextField.AbstractFormatter formatter) {
        nullFormatter = formatter;
    }
}
