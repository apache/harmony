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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import javax.print.attribute.PrintRequestAttributeSet;
import org.apache.harmony.x.print.ServiceUIDialog;

public class ServiceUI {
    public static PrintService printDialog(GraphicsConfiguration gc, int x, int y,
            PrintService[] services, PrintService defaultService, DocFlavor flavor,
            PrintRequestAttributeSet attributes) throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        int initialIndex = checkServices(services, defaultService, attributes);
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getActiveWindow();
        Window dialogOwner = getDialogOwner(activeWindow);
        ServiceUIDialog dialog = new ServiceUIDialog(gc, x, y, services, initialIndex, flavor,
                attributes, dialogOwner);
        dialog.show();
        if (dialogOwner != activeWindow) {
            dialogOwner.dispose();
        }
        if (dialog.getResult() == ServiceUIDialog.APPROVE_PRINT) {
            attributes.clear();
            attributes.addAll(dialog.getAttributes());
            return dialog.getPrintService();
        }
        return null;
    }

    /**
     * This function checks services, defaultService and attributes input
     * parameters for printDialog(...) method.
     * 
     * @return defaultService index in services array or 0 if defaultService is
     *         null.
     * 
     * @throws IllegalArgumentException if services is null or empty, or
     *         attributes is null, or the initial PrintService is not in the
     *         list of browseable services
     */
    static int checkServices(PrintService[] services, PrintService defaultService,
            PrintRequestAttributeSet attributes) {
        if (services == null) {
            throw new IllegalArgumentException("Services list is null!");
        } else if (services.length == 0) {
            throw new IllegalArgumentException("Services list is empty!");
        } else if (attributes == null) {
            throw new IllegalArgumentException("Attribute set is null!");
        }
        int serviceIndex = 0;
        boolean defaultServiceFound = (defaultService == null);
        for (int i = 0; i < services.length; i++) {
            if (services[i].equals(defaultService)) {
                serviceIndex = i;
                defaultServiceFound = true;
                break;
            }
        }
        if (!defaultServiceFound) {
            throw new IllegalArgumentException(
                    "Default service is absent in the services list!");
        }
        return serviceIndex;
    }

    /**
     * This functions checks if current activeWindow can be parent for
     * ServiceUIDialog window.
     * 
     * @return window if the given window is Dialog or Frame, otherwise returns
     *         new Frame object.
     */
    private static Window getDialogOwner(Window window) {
        return ((window instanceof Dialog) || (window instanceof Frame)) ? window : new Frame();
    }
} /* End of ServiceUI class */
