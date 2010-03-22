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
 * @author Dmitry A. Durnev, Pavel Dolgov
 */
package java.awt;

import org.apache.harmony.awt.wtk.NativeMouseInfo;

public class MouseInfo {

    public static int getNumberOfButtons() throws HeadlessException {
        Toolkit.checkHeadless();
        NativeMouseInfo ptrInfo =
            Toolkit.getDefaultToolkit().getNativeMouseInfo();
        return ptrInfo.getNumberOfButtons();
    }

    public static PointerInfo getPointerInfo() throws HeadlessException {
        Toolkit.checkHeadless();
        SecurityManager secMan = System.getSecurityManager();
        if (secMan != null) {
            secMan.checkPermission(new AWTPermission("watchMousePointer")); //$NON-NLS-1$
        }
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Point location = toolkit.getNativeMouseInfo().getLocation();
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsDevice gd = ge.getDefaultScreenDevice();

        // TODO:  For virtual screen devices, the coordinates
        // are given in the virtual coordinate system,
        // otherwise they are returned in the coordinate system
        // of the GraphicsDevice.[multi-screen support]

        return new PointerInfo(gd, location);
    }

    private MouseInfo() {
        //this class can't be instantiated
    }

}
