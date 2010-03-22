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

package org.apache.harmony.x.swing.plaf.resources.basic;

import java.awt.event.KeyEvent;
import java.util.ListResourceBundle;

public final class BasicResourceBundle extends ListResourceBundle {
    /**
     * Returns the contents of the resource bundle.
     *
     * @return array of resources
     */
    protected final Object[][] getContents() {
        Object[][] objects = new Object[][] {
                       { "AbstractButton.clickText", "click" },
                       
                       { "AbstractDocument.additionText", "Text added" },
                       { "AbstractDocument.deletionText", "Text deleted" },
                       { "AbstractDocument.redoText", "Redo" },
                       { "AbstractDocument.styleChangeText", "Style changed" },
                       { "AbstractDocument.undoText", "Undo" },
                       
                       { "AbstractUndoableEdit.redoText", "Redo" },
                       { "AbstractUndoableEdit.undoText", "Undo" },
                       
                       { "ColorChooser.cancelText", "Cancel" },
                       { "ColorChooser.hsbBlueText", "Blue" },
                       { "ColorChooser.hsbBrightnessText", "Brightness" },
                       { "ColorChooser.hsbDisplayedMnemonicIndex", "0" },
                       { "ColorChooser.hsbGreenText", "Green" },
                       { "ColorChooser.hsbHueText", "Hue" },
                       { "ColorChooser.hsbMnemonic", new Integer(KeyEvent.VK_H) },
                       { "ColorChooser.hsbNameText", "HSB" },
                       { "ColorChooser.hsbRedText", "Red" },
                       { "ColorChooser.hsbSaturationText", "Saturation" },
                       { "ColorChooser.okText", "OK" },
                       { "ColorChooser.previewText", "Preview" },
                       { "ColorChooser.resetMnemonic", new Integer(KeyEvent.VK_R) },
                       { "ColorChooser.resetText", "Reset" },
                       { "ColorChooser.rgbBlueText", "Blue" },
                       { "ColorChooser.rgbBlueMnemonic", new Integer(KeyEvent.VK_B) },
                       { "ColorChooser.rgbDisplayedMnemonicIndex", "1" },
                       { "ColorChooser.rgbGreenText", "Green" },
                       { "ColorChooser.rgbGreenMnemonic", new Integer(KeyEvent.VK_E) },
                       { "ColorChooser.rgbMnemonic", new Integer(KeyEvent.VK_G) },
                       { "ColorChooser.rgbNameText", "RGB" },
                       { "ColorChooser.rgbRedText", "Red" },
                       { "ColorChooser.rgbRedMnemonic", new Integer(KeyEvent.VK_D) },
                       { "ColorChooser.sampleText", "Text Sample" },
                       { "ColorChooser.swatchesDisplayedMnemonicIndex", "0" },
                       { "ColorChooser.swatchesMnemonic", "83" },
                       { "ColorChooser.swatchesNameText", "Swatches" },
                       { "ColorChooser.swatchesRecentText", "Recent:" },
                       
                       { "ComboBox.togglePopupText", "togglePopup" },
                       
                       { "FileChooser.acceptAllFileFilterText", "All Files" },
                       { "FileChooser.cancelButtonText", "Cancel" },
                       { "FileChooser.cancelButtonToolTipText", "Cancel file chooser dialog" },
                       { "FileChooser.directoryDescriptionText", "Directory" },
                       { "FileChooser.directoryOpenButtonText", "Open" },
                       { "FileChooser.directoryOpenButtonMnemonic", new Integer(KeyEvent.VK_O) },
                       { "FileChooser.directoryOpenButtonToolTipText", "Open selected directory" },
                       { "FileChooser.fileDescriptionText", "Generic File" },
                       { "FileChooser.helpButtonText", "Help" },
                       { "FileChooser.helpButtonMnemonic", new Integer(KeyEvent.VK_H) },
                       { "FileChooser.helpButtonToolTipText", "FileChooser help" },
                       { "FileChooser.newFolderErrorSeparator", ": " },
                       { "FileChooser.newFolderErrorText", "Error creating folder" },
                       { "FileChooser.openButtonText", "Open" },
                       { "FileChooser.openButtonToolTipText", "Open file" },
                       { "FileChooser.openDialogTitleText", "Open" },
                       { "FileChooser.other.newFolder", "NewFolder" },
                       { "FileChooser.other.newFolder.subsequent", "NewFolder.{0}" },
                       { "FileChooser.saveButtonText", "Save" },
                       { "FileChooser.saveButtonToolTipText", "Save file" },
                       { "FileChooser.saveDialogTitleText", "Save" },
                       { "FileChooser.updateButtonText", "Refresh" },
                       { "FileChooser.updateButtonToolTipText", "Refresh directory listing" },
                       { "FileChooser.win32.newFolder", "New Folder" },
                       { "FileChooser.win32.newFolder.subsequent", "New Folder ({0})" },
                       
                       { "FormView.browseFileButtonText", "Browse..." },
                       { "FormView.resetButtonText", "Reset" },
                       { "FormView.submitButtonText", "Submit Query" },
                       
                       { "InternalFrame.closeButtonToolTip", "Close" },
                       { "InternalFrame.iconButtonToolTip", "Minimize" },
                       { "InternalFrame.maxButtonToolTip", "Maximize" },
                       { "InternalFrame.restoreButtonToolTip", "Restore" },
                       
                       { "InternalFrameTitlePane.closeButtonText", "Close" },
                       { "InternalFrameTitlePane.maximizeButtonText", "Maximize" },
                       { "InternalFrameTitlePane.minimizeButtonText", "Minimize" },
                       { "InternalFrameTitlePane.moveButtonText", "Move" },
                       { "InternalFrameTitlePane.restoreButtonText", "Restore" },
                       { "InternalFrameTitlePane.sizeButtonText", "Size" },
                       
                       { "IsindexView.prompt", "This is a searchable index.  Enter search keywords:" },
                       
                       { "OptionPane.cancelButtonMnemonic", new Integer(KeyEvent.VK_UNDEFINED) },
                       { "OptionPane.cancelButtonText", "Cancel" },
                       { "OptionPane.inputDialogTitle", "Input" },
                       { "OptionPane.messageDialogTitle", "Info" },
                       { "OptionPane.noButtonMnemonic", new Integer(KeyEvent.VK_N) },
                       { "OptionPane.noButtonText", "No" },
                       { "OptionPane.okButtonMnemonic", new Integer(KeyEvent.VK_UNDEFINED) },
                       { "OptionPane.okButtonText", "OK" },
                       { "OptionPane.titleText", "Choose an Option" },
                       { "OptionPane.yesButtonMnemonic", new Integer(KeyEvent.VK_Y) },
                       { "OptionPane.yesButtonText", "Yes" },
                       
                       { "ProgressMonitor.progressText", "In progress..." },
                       
                       { "SplitPane.leftButtonText", "left button" },
                       { "SplitPane.rightButtonText", "right button" } };
        return objects;
    }
}