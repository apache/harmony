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

package org.apache.harmony.x.swing.plaf.resources.metal;

import java.awt.event.KeyEvent;
import java.util.ListResourceBundle;

public class MetalResourceBundle extends ListResourceBundle {

    /**
     * Return the contents of the resource bundle.
     *
     * @return array of resources
     */
    protected Object[][] getContents() {
        Object[][] objects = new Object[][] {
                   { "FileChooser.detailsViewButtonAccessibleName", "Details" },
                   { "FileChooser.detailsViewButtonToolTipText", "Detailed view" },
                   { "FileChooser.fileAttrHeaderText", "Attributes" },
                   { "FileChooser.fileDateHeaderText", "Date Modified" },
                   { "FileChooser.fileNameHeaderText", "Filename" },
                   { "FileChooser.fileNameLabelText", "File Name:" },
                   { "FileChooser.fileNameLabelMnemonic", new Integer(KeyEvent.VK_N) },
                   { "FileChooser.fileSizeHeaderText", "File size" },
                   { "FileChooser.filesOfTypeLabelText", "Files of Type:" },
                   { "FileChooser.filesOfTypeLabelMnemonic", new Integer(KeyEvent.VK_T) },
                   { "FileChooser.fileTypeHeaderText", "Type" },
                   { "FileChooser.homeFolderAccessibleName", "Home" },
                   { "FileChooser.homeFolderToolTipText", "Open Home Folder" },
                   { "FileChooser.listViewButtonAccessibleName", "List" },
                   { "FileChooser.listViewButtonToolTipText", "List view" },
                   { "FileChooser.lookInLabelText", "Open from:" },
                   { "FileChooser.lookInLabelMnemonic", new Integer(KeyEvent.VK_E) },
                   { "FileChooser.newFolderAccessibleName", "Create Folder" },
                   { "FileChooser.newFolderToolTipText", "Create New Folder" },
                   { "FileChooser.saveInLabelText", "Save To:" },
                   { "FileChooser.upFolderAccessibleName", "Up" },
                   { "FileChooser.upFolderToolTipText", "Go Up One Level" },

                   { "InternalFrameTitlePane.closeButtonAccessibleName", "Close" },
                   { "InternalFrameTitlePane.iconifyButtonAccessibleName", "Iconify" },
                   { "InternalFrameTitlePane.maximizeButtonAccessibleName", "Maximize" },

                   { "MetalTitlePane.closeMnemonic", new Integer(KeyEvent.VK_C) },
                   { "MetalTitlePane.closeTitle", "Close" },
                   { "MetalTitlePane.iconifyMnemonic", new Integer(KeyEvent.VK_E) },
                   { "MetalTitlePane.iconifyTitle", "Minimize" },
                   { "MetalTitlePane.maximizeMnemonic", new Integer(KeyEvent.VK_X) },
                   { "MetalTitlePane.maximizeTitle", "Maximize" },
                   { "MetalTitlePane.restoreMnemonic", new Integer(KeyEvent.VK_R) },
                   { "MetalTitlePane.restoreTitle", "Restore" }
        };
        return objects;
    }
}
