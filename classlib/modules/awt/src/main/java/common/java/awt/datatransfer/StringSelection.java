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

package java.awt.datatransfer;

import java.io.IOException;
import java.io.StringBufferInputStream;

@SuppressWarnings("deprecation")
public class StringSelection implements Transferable, ClipboardOwner {

    private static final DataFlavor[] supportedFlavors = { DataFlavor.stringFlavor,
            DataFlavor.plainTextFlavor };

    private final String string;

    public StringSelection(String data) {
        string = data;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
            IOException {
        if (flavor.equals(DataFlavor.stringFlavor)) {
            return string;
        } else if (flavor.equals(DataFlavor.plainTextFlavor)) {
            return new StringBufferInputStream(string);
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return (flavor.equals(DataFlavor.stringFlavor) || flavor
                .equals(DataFlavor.plainTextFlavor));
    }

    public DataFlavor[] getTransferDataFlavors() {
        return supportedFlavors.clone();
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
