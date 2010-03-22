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
package javax.swing.colorchooser;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public abstract class AbstractColorChooserPanel extends JPanel {
    int mnemonic;
    int displayedMnemonicIndex = -1;

    private JColorChooser colorChooser;
    private ChangeListener modelListener;

    public int getMnemonic() {
        return mnemonic;
    }

    public int getDisplayedMnemonicIndex() {
        return displayedMnemonicIndex;
    }

    public void installChooserPanel(final JColorChooser enclosingChooser) {
        colorChooser = enclosingChooser;
        modelListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateChooser();
            }
        };
        buildChooser();
        updateChooser();
        colorChooser.getSelectionModel().addChangeListener(modelListener);
    }

    public void uninstallChooserPanel(final JColorChooser enclosingChooser) {
        colorChooser.getSelectionModel().removeChangeListener(modelListener);
        modelListener = null;
        colorChooser = null;
    }

    public ColorSelectionModel getColorSelectionModel() {
        return colorChooser.getSelectionModel();
    }

    protected Color getColorFromModel() {
        return colorChooser.getSelectionModel().getSelectedColor();
    }

    public abstract String getDisplayName();
    public abstract Icon getSmallDisplayIcon();
    public abstract Icon getLargeDisplayIcon();
    public abstract void updateChooser();
    protected abstract void buildChooser();

}
