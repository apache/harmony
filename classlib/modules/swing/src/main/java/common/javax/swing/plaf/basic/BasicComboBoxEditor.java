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
 * @author Anton Avtamonov
 */

package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.ComboBoxEditor;
import javax.swing.JTextField;

public class BasicComboBoxEditor implements ComboBoxEditor, FocusListener {

    private Object item = "";
    
    public static class UIResource extends BasicComboBoxEditor implements javax.swing.plaf.UIResource {
    }

    protected JTextField editor = new JTextField();

    public void addActionListener(final ActionListener l) {
        editor.addActionListener(l);
    }

    public void removeActionListener(final ActionListener l) {
        editor.removeActionListener(l);
    }

    public void focusGained(final FocusEvent e) {
    }

    public void focusLost(final FocusEvent e) {

    }

    public Component getEditorComponent() {
        return editor;
    }

    public Object getItem() {
        String text = editor.getText();
        if (!text.equals(item.toString())) {
            item = text;
        }
        return item;
    }

    public void setItem(final Object item) {
        this.item = item != null ? item : "";
        editor.setText(this.item.toString());
    }

    public void selectAll() {
        editor.requestFocus();
        editor.selectAll();
    }
}
