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
 * @author Anton Avtamonov
 */


package javax.swing.plaf;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

public abstract class FileChooserUI extends ComponentUI {
    public abstract FileFilter getAcceptAllFileFilter(JFileChooser fc);
    public abstract FileView getFileView(JFileChooser fc);
    public abstract String getApproveButtonText(JFileChooser fc);
    public abstract String getDialogTitle(JFileChooser fc);
    public abstract void rescanCurrentDirectory(JFileChooser fc);
    public abstract void ensureFileIsVisible(JFileChooser fc, File f);
}
