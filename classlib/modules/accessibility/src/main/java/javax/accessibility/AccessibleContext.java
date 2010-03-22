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

package javax.accessibility;

import java.awt.IllegalComponentStateException;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.swing.event.SwingPropertyChangeSupport;

@SuppressWarnings("nls")
public abstract class AccessibleContext{
    public static final String ACCESSIBLE_NAME_PROPERTY = "AccessibleName";
    public static final String ACCESSIBLE_DESCRIPTION_PROPERTY = "AccessibleDescription";
    public static final String ACCESSIBLE_STATE_PROPERTY = "AccessibleState";
    public static final String ACCESSIBLE_VALUE_PROPERTY = "AccessibleValue";
    public static final String ACCESSIBLE_SELECTION_PROPERTY = "AccessibleSelection";
    public static final String ACCESSIBLE_CARET_PROPERTY = "AccessibleCaret";
    public static final String ACCESSIBLE_VISIBLE_DATA_PROPERTY = "AccessibleVisibleData";
    public static final String ACCESSIBLE_CHILD_PROPERTY = "AccessibleChild";
    public static final String ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY = "AccessibleActiveDescendant";
    public static final String ACCESSIBLE_TABLE_CAPTION_CHANGED = "accessibleTableCaptionChanged";
    public static final String ACCESSIBLE_TABLE_SUMMARY_CHANGED = "accessibleTableSummaryChanged";
    public static final String ACCESSIBLE_TABLE_MODEL_CHANGED = "accessibleTableModelChanged";
    public static final String ACCESSIBLE_TABLE_ROW_HEADER_CHANGED = "accessibleTableRowHeaderChanged";
    public static final String ACCESSIBLE_TABLE_ROW_DESCRIPTION_CHANGED = "accessibleTableRowDescriptionChanged";
    public static final String ACCESSIBLE_TABLE_COLUMN_HEADER_CHANGED = "accessibleTableColumnHeaderChanged";
    public static final String ACCESSIBLE_TABLE_COLUMN_DESCRIPTION_CHANGED = "accessibleTableColumnDescriptionChanged";
    public static final String ACCESSIBLE_ACTION_PROPERTY = "accessibleActionProperty";
    public static final String ACCESSIBLE_HYPERTEXT_OFFSET = "AccessibleHypertextOffset";
    public static final String ACCESSIBLE_TEXT_PROPERTY = "AccessibleText";
    public static final String ACCESSIBLE_INVALIDATE_CHILDREN = "accessibleInvalidateChildren";
    public static final String ACCESSIBLE_TEXT_ATTRIBUTES_CHANGED = "accessibleTextAttributesChanged";
    public static final String ACCESSIBLE_COMPONENT_BOUNDS_CHANGED = "accessibleComponentBoundsChanged";

    protected Accessible accessibleParent;
    protected String accessibleName;
    protected String accessibleDescription;

    private AccessibleRelationSet accessibleRelationSet;
    private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    public String getAccessibleName() {
        return accessibleName;
    }

    public void setAccessibleName(final String s) {
        String old = accessibleName;
        accessibleName = s;
        firePropertyChange(ACCESSIBLE_NAME_PROPERTY, old, accessibleName);
    }

    public String getAccessibleDescription() {
        return accessibleDescription;
    }

    public void setAccessibleDescription(final String s) {
        String old = accessibleDescription;
        accessibleDescription = s;
        firePropertyChange(ACCESSIBLE_DESCRIPTION_PROPERTY, old, accessibleDescription);
    }

    public Accessible getAccessibleParent() {
        return accessibleParent;
    }

    public void setAccessibleParent(final Accessible a) {
        accessibleParent = a;
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public AccessibleAction getAccessibleAction() {
        return null;
    }

    public AccessibleComponent getAccessibleComponent() {
        return null;
    }

    public AccessibleSelection getAccessibleSelection() {
        return null;
    }

    public AccessibleText getAccessibleText() {
        return null;
    }

    public AccessibleEditableText getAccessibleEditableText() {
        return null;
    }

    public AccessibleValue getAccessibleValue() {
        return null;
    }

    public AccessibleIcon[] getAccessibleIcon() {
        return null;
    }

    public AccessibleRelationSet getAccessibleRelationSet() {
        if (accessibleRelationSet == null) {
            accessibleRelationSet = new AccessibleRelationSet();
        }
        return accessibleRelationSet;
    }

    public AccessibleTable getAccessibleTable() {
        return null;
    }

    public void firePropertyChange(final String propertyName, final Object oldValue,
                                   final Object newValue) {

        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    public abstract AccessibleRole getAccessibleRole();
    public abstract AccessibleStateSet getAccessibleStateSet();
    public abstract int getAccessibleIndexInParent();
    public abstract int getAccessibleChildrenCount();
    public abstract Accessible getAccessibleChild(int i);
    public abstract Locale getLocale() throws IllegalComponentStateException;
}

