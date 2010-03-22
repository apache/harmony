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

package org.apache.harmony.x.swing;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

public class ExtendedListFactory {
    private static ExtendedListFactory factory;

    public static ExtendedListFactory getFactory() {
        if (factory == null) {
            factory = new ExtendedListFactory();
        }
        return factory;
    }

    public static void setFactory(final ExtendedListFactory factory) {
        ExtendedListFactory.factory = factory;
    }


    public AbstractExtendedListElement createGroupElement(final Object value) {
        return createGroupElement(value, getGroupFont());
    }

    public AbstractExtendedListElement createGroupElement(final Object value, final Font font) {
        return createGroupElement(value, font, 0);
    }

    public AbstractExtendedListElement createGroupElement(final Object value, final int level) {
        return createGroupElement(value, getGroupFont(), level);
    }

    public AbstractExtendedListElement createGroupElement(final Object value, final Font font, final int level) {
        return new AbstractExtendedListElement(value, font) {
            public boolean isChoosable() {
                return false;
            }

            public int getIndentationLevel() {
                return level;
            }
        };
    }

    public AbstractExtendedListElement createItemElement(final Object value) {
        return createItemElement(value, getItemFont());
    }

    public AbstractExtendedListElement createItemElement(final Object value, final int level) {
        return createItemElement(value, getItemFont(), level);
    }

    public AbstractExtendedListElement createItemElement(final Object value, final Font font) {
        return createItemElement(value, font, 0);
    }

    public AbstractExtendedListElement createItemElement(final Object value, final Font font, final int level) {
        return new AbstractExtendedListElement(value, font) {
            public int getIndentationLevel() {
                return level;
            }
        };
    }

    public ListCellRenderer createExtendedRenderer() {
        return new ExtendedListCellRenderer();
    }

    public Object[] getSelectedValues(final JList list) {
        Object[] allSelectedValues = list.getSelectedValues();
        List selectedValues = new ArrayList(allSelectedValues.length);
        for (int i = 0; i < allSelectedValues.length; i++) {
            Object selectedValue = allSelectedValues[i];
            if (!(selectedValue instanceof ExtendedListElement)
                || ((ExtendedListElement)selectedValue).isChoosable()) {

                selectedValues.add(selectedValue);
            }
        }

        return selectedValues.toArray(new Object[selectedValues.size()]);
    }


    protected ExtendedListFactory() {
    }



    private static Font getGroupFont() {
        return UIManager.getFont("List.font").deriveFont(Font.BOLD | Font.ITALIC);
    }

    private static Font getItemFont() {
        return UIManager.getFont("List.font").deriveFont(Font.PLAIN);
    }
}
