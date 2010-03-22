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

package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicGraphicsUtils;

public class MetalComboBoxEditor extends BasicComboBoxEditor {

    public static class UIResource extends MetalComboBoxEditor implements javax.swing.plaf.UIResource {
    }

    protected static Insets editorBorderInsets = new Insets(4, 2, 4, 0);
    private static Border border = new AbstractBorder() {
        public Insets getBorderInsets(final Component component) {
            return editorBorderInsets;
        }

        public Insets getBorderInsets(final Component component, final Insets insets) {
            if (insets == null) {
                return getBorderInsets(component);
            }
            insets.set(editorBorderInsets.top, editorBorderInsets.left, editorBorderInsets.bottom, editorBorderInsets.right);
            return insets;
        }

        public void paintBorder(final Component c, final Graphics g, final int x,
                                final int y, final int width, final int height) {
            BasicGraphicsUtils.drawGroove(g, x, y, width, height, MetalLookAndFeel.getControlDarkShadow(), MetalLookAndFeel.getControlHighlight());
        }
    };

    public MetalComboBoxEditor() {
        editor.setBorder(border);
    }
}
