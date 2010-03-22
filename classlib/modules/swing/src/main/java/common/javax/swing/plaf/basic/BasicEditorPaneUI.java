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
package javax.swing.plaf.basic;

import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;

import org.apache.harmony.x.swing.StringConstants;


public class BasicEditorPaneUI extends BasicTextUI {
    private static final String propertyPrefix = "EditorPane";

    public static ComponentUI createUI(final JComponent c) {
        return new BasicEditorPaneUI();
    }

    public EditorKit getEditorKit(final JTextComponent c) {
        JTextComponent textComponent = getComponent();
        if (textComponent instanceof JEditorPane)
            return ((JEditorPane)textComponent).getEditorKit();
        return super.getEditorKit(c);
    }


    protected String getPropertyPrefix() {
        return propertyPrefix;
    }

    protected void propertyChange(final PropertyChangeEvent e) {
        super.propertyChange(e);
        if (StringConstants.EDITOR_PANE_EDITOR_KIT_PROPERTY
                .equals(e.getPropertyName()))
           installUIActionMap();
    }
}

