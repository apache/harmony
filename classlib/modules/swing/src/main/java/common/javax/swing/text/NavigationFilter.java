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

import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.plaf.basic.BasicTextUI;

public class NavigationFilter {

    public abstract static class FilterBypass {

        public FilterBypass() {

        }

        public abstract Caret getCaret();

        public abstract void moveDot(int pos, Position.Bias bias);

        public abstract void setDot(int pos, Position.Bias bias);

    }

    public NavigationFilter() {

    }

    public int getNextVisualPositionFrom(final JTextComponent c, final int pos,
            final Position.Bias bias, final int direction, final Position.Bias[] biasRet)
            throws BadLocationException {
        if (c == null)
            return 0;
        BasicTextUI ui = (BasicTextUI)c.getUI();
        View rootView = ui.getRootView(c);
        Rectangle rect = c.getVisibleRect();
        if (rect == null)
            return 0;
        Insets insets = c.getInsets();
        rect.x += insets.left;
        rect.y += insets.top;
        rect.width -= insets.left + insets.right;
        rect.height -= insets.top + insets.bottom;
        return rootView.getNextVisualPositionFrom(pos,bias,rect,direction, biasRet);
    }

    public void moveDot(final NavigationFilter.FilterBypass filter, final int pos,
            final Position.Bias bias) {
        filter.moveDot(pos, bias);
    }

    public void setDot(final NavigationFilter.FilterBypass filter, final int pos,
            final Position.Bias bias) {
        filter.setDot(pos, bias);
    }

}

