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

package javax.swing.plaf.basic;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DateEditor;

import org.apache.harmony.awt.text.TextUtils;
import org.apache.harmony.x.swing.Utilities;


final class BasicSpinnerKeyboardActions {
    public static AbstractAction incrementAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JSpinner spinner = (JSpinner)e.getSource();
            if (spinner.getEditor() instanceof DateEditor) {
                DateEditor dateEditor = (DateEditor)spinner.getEditor();
                JFormattedTextField textField = dateEditor.getTextField();
                int calendarField = getCalendarField(textField);
                dateEditor.getModel().setCalendarField(calendarField);

                nextValue(spinner);

                selectCalendarField(textField, calendarField);
            } else {
                nextValue(spinner);
            }

        }

        private void nextValue(final JSpinner spinner) {
            Object nextValue = spinner.getNextValue();
            if (nextValue != null) {
                spinner.setValue(nextValue);
            }
        }
    };

    public static AbstractAction decrementAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JSpinner spinner = (JSpinner)e.getSource();
            if (spinner.getEditor() instanceof DateEditor) {
                DateEditor dateEditor = (DateEditor)spinner.getEditor();
                JFormattedTextField textField = dateEditor.getTextField();
                int calendarField = getCalendarField(textField);
                dateEditor.getModel().setCalendarField(calendarField);

                previousValue(spinner);

                selectCalendarField(textField, calendarField);
            } else {
                previousValue(spinner);
            }
        }

        private void previousValue(final JSpinner spinner) {
            Object previousValue = spinner.getPreviousValue();
            if (previousValue != null) {
                spinner.setValue(previousValue);
            }
        }
    };

    public static void installKeyboardActions(final JSpinner spinner) {
        Utilities.installKeyboardActions(spinner, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "Spinner.ancestorInputMap", null);

        spinner.getActionMap().put("increment", incrementAction);
        spinner.getActionMap().put("decrement", decrementAction);
    }

    public static void uninstallKeyboardActions(final JSpinner spinner) {
        Utilities.uninstallKeyboardActions(spinner, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }


    private static int getCalendarField(final JFormattedTextField textField) {
        return TextUtils.getCalendarField(textField);
    }

    private static void selectCalendarField(final JFormattedTextField textField,
                                            final int calendarField) {
        TextUtils.selectCalendarField(textField, calendarField);
    }
}
