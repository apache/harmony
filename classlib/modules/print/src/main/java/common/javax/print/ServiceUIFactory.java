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

package javax.print;

public abstract class ServiceUIFactory {
    public static final int ABOUT_UIROLE = 1;

    public static final int ADMIN_UIROLE = 2;

    public static final int MAIN_UIROLE = 3;

    public static final int RESERVED_UIROLE = 99;

    public static final String DIALOG_UI = "java.awt.Dialog";

    public static final String JCOMPONENT_UI = "javax.swing.JComponent";

    public static final String JDIALOG_UI = "javax.swing.JDialog";

    public static final String PANEL_UI = "java.awt.Panel";

    public abstract Object getUI(int role, String ui);

    public abstract String[] getUIClassNamesForRole(int role);
}
