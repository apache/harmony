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

public interface AccessibleTable {
    Accessible getAccessibleCaption();
    void setAccessibleCaption(Accessible a);
    Accessible getAccessibleSummary();
    void setAccessibleSummary(Accessible a);
    int getAccessibleRowCount();
    int getAccessibleColumnCount();
    Accessible getAccessibleAt(int r, int c);
    int getAccessibleRowExtentAt(int r, int c);
    int getAccessibleColumnExtentAt(int r, int c);
    AccessibleTable getAccessibleRowHeader();
    void setAccessibleRowHeader(AccessibleTable table);
    AccessibleTable getAccessibleColumnHeader();
    void setAccessibleColumnHeader(AccessibleTable table);
    Accessible getAccessibleRowDescription(int r);
    void setAccessibleRowDescription(int r, Accessible a);
    Accessible getAccessibleColumnDescription(int c);
    void setAccessibleColumnDescription(int c, Accessible a);
    boolean isAccessibleSelected(int r, int c);
    boolean isAccessibleRowSelected(int r);
    boolean isAccessibleColumnSelected(int c);
    int[] getSelectedAccessibleRows();
    int[] getSelectedAccessibleColumns();
}
