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
 * @author Sergey Burlak
 */
package javax.swing;

import javax.swing.event.ListSelectionListener;

public interface ListSelectionModel {

    public static final int MULTIPLE_INTERVAL_SELECTION = 2;

    public static final int SINGLE_INTERVAL_SELECTION = 1;

    public static final int SINGLE_SELECTION = 0;

    public void addListSelectionListener(final ListSelectionListener l);

    public void addSelectionInterval(final int index0, final int index1);

    public void clearSelection();

    public int getAnchorSelectionIndex();

    public int getLeadSelectionIndex();

    public int getMaxSelectionIndex();

    public int getMinSelectionIndex();

    public int getSelectionMode();

    public boolean getValueIsAdjusting();

    public void insertIndexInterval(final int index, final int length, final boolean before);

    public boolean isSelectedIndex(final int index);

    public boolean isSelectionEmpty();

    public void removeIndexInterval(final int index0, final int index1);

    public void removeListSelectionListener(final ListSelectionListener l);

    public void removeSelectionInterval(final int index0, final int index1);

    public void setAnchorSelectionIndex(final int index);

    public void setLeadSelectionIndex(final int index1);

    public void setSelectionInterval(final int index0, final int index1);

    public void setSelectionMode(final int index);

    public void setValueIsAdjusting(final boolean value);
}

