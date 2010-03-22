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

import java.awt.ItemSelectable;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import javax.swing.event.ChangeListener;

public interface ButtonModel extends ItemSelectable {
    void addItemListener(ItemListener listener);

    void removeItemListener(ItemListener listener);

    void addActionListener(ActionListener listener);

    void removeActionListener(ActionListener listener);

    void addChangeListener(ChangeListener listener);

    void removeChangeListener(ChangeListener listener);

    void setSelected(boolean selected);

    boolean isSelected();

    void setRollover(boolean rollover);

    boolean isRollover();

    void setPressed(boolean pressed);

    boolean isPressed();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void setArmed(boolean armed);

    boolean isArmed();

    void setMnemonic(int mnemonic);

    int getMnemonic();

    void setGroup(ButtonGroup group);

    void setActionCommand(String command);

    String getActionCommand();
}
