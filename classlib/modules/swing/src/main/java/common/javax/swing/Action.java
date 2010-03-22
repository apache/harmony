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

package javax.swing;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

public interface Action extends ActionListener {
    String DEFAULT = "Default";

    String NAME = "Name";

    String SHORT_DESCRIPTION = "ShortDescription";

    String LONG_DESCRIPTION = "LongDescription";

    String SMALL_ICON = "SmallIcon";

    String ACTION_COMMAND_KEY = "ActionCommandKey";

    String ACCELERATOR_KEY = "AcceleratorKey";

    String MNEMONIC_KEY = "MnemonicKey";

    void putValue(String key, Object value);

    Object getValue(String key);

    void removePropertyChangeListener(PropertyChangeListener l);

    void addPropertyChangeListener(PropertyChangeListener l);

    void setEnabled(boolean b);

    boolean isEnabled();
}
