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
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;

import org.apache.harmony.awt.text.ComposedTextParams;
import org.apache.harmony.awt.text.PropertyNames;
import org.apache.harmony.awt.text.TextKit;


/**
 * Stores parameters to paint text in plain text views.
 *
 */
final class TextPaintParams {
    private static final int DEFAULT_TAB_SIZE = 8;

    private static final float MIN_TAB_SIZE = 1;

    final Segment buffer = new Segment();

    Color color;

    int composedEnd;
    int composedStart;
    ComposedTextParams composedText;

    FontMetrics metrics;

    Color selColor;
    int selEnd;
    int selStart;

    float tabSize;

    final View view;

    TextPaintParams(final View view) {
        this.view = view;
    }

    void conditionalUpdateMetrics() {
        if (areMetricsValid()) {
            updateMetrics();
            view.preferenceChanged(null, true, true);
        }
    }

    int getTabSize() {
        final Document doc = view.getDocument();
        final Object value = doc.getProperty(PlainDocument.tabSizeAttribute);
        return value != null ? ((Integer)value).intValue()
                             : DEFAULT_TAB_SIZE;
    }

    boolean areMetricsValid() {
        return metrics == null
               || !view.getComponent().getFont().equals(metrics.getFont());
    }

    float nextTabStop(final float x) {
        conditionalUpdateMetrics();
        if (Math.abs(tabSize) <= MIN_TAB_SIZE) {
            return x;
        }
        final int count = (int)x / (int)tabSize;
        return (count + 1) * tabSize;
    }

    void updateFields() {
        final Component component = view.getComponent();
        final TextKit textKit = view.getTextKit();
        selStart = textKit.getSelectionStart();
        selEnd   = textKit.getSelectionEnd();

        composedText = (ComposedTextParams)view.getDocument().getProperty(
                PropertyNames.COMPOSED_TEXT_PROPERTY);

        if (composedText != null) {
            composedStart = composedText.getComposedTextStart();
            composedEnd = composedStart + composedText.getComposedTextLength();
        }

        if (component.isEnabled()) {
            color = component.getForeground();
            selColor = textKit.getSelectedTextColor();
        } else {
            color = textKit.getDisabledTextColor();
            selColor = color;
        }
    }

    void updateMetrics() {
        final Component component = view.getComponent();
        final Font font = component.getFont();
        metrics = component.getFontMetrics(font);
        tabSize = metrics.charWidth('m') * getTabSize();
    }
}
