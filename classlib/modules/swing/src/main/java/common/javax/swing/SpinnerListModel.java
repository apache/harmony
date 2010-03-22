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
package javax.swing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class SpinnerListModel extends AbstractSpinnerModel implements Serializable {
    private static final String[] DEFAULT_VALUES = {"empty"};
    private List<?> values;
    private int index = 0;

    public SpinnerListModel() {
        this(Arrays.asList(DEFAULT_VALUES));
    }

    public SpinnerListModel(final List<?> values) {
        if (values == null || values.size() <= 0) {
            throw new IllegalArgumentException(Messages.getString("swing.5C")); //$NON-NLS-1$
        }
        this.values = values;
    }

    public SpinnerListModel(final Object[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(Messages.getString("swing.5D")); //$NON-NLS-1$
        }
        this.values = Arrays.asList(values);
    }

    public List<?> getList() {
        return values;
    }

    public void setList(final List<?> list) {
        if (list == null || list.size() <= 0) {
            throw new IllegalArgumentException("");
        }
        values = list;
        index = 0;
        fireStateChanged();
    }

    public Object getValue() {
        return values.get(index);
    }

    public void setValue(final Object obj) {
        if (!values.contains(obj)) {
            throw new IllegalArgumentException(Messages.getString("swing.58")); //$NON-NLS-1$
        }
        index = values.indexOf(obj);
        fireStateChanged();
    }

    public Object getNextValue() {
        return (index + 1 >= values.size()) ? null : values.get(index + 1);
    }

    public Object getPreviousValue() {
        return (index == 0) ? null : values.get(index - 1);
    }
}


