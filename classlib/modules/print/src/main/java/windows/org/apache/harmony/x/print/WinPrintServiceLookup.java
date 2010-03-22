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
package org.apache.harmony.x.print;

import java.util.Vector;

import javax.print.DocFlavor;
import javax.print.MultiDocPrintService;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;

public class WinPrintServiceLookup extends PrintServiceLookup {

    @Override
    public PrintService getDefaultPrintService() {
        try {
            final String name = WinPrinterFactory.getDefaultPrinterName();
            return (name != null) ? new WinPrintService(name) : null;
        } catch (PrintException e) {
            return null;
        }
    }

    @Override
    public MultiDocPrintService[] getMultiDocPrintServices(
                    final DocFlavor[] flavors, final AttributeSet attributes) {
        return new MultiDocPrintService[0];
    }

    @Override
    public PrintService[] getPrintServices() {
        try {
            final String[] names = WinPrinterFactory.getConnectedPrinterNames();
            final PrintService[] srv = new PrintService[names.length];

            for (int i = 0; i < names.length; i++) {
                srv[i] = new WinPrintService(names[i]);
            }

            return srv;
        } catch (PrintException e) {
            return new PrintService[0];
        }
    }

    @Override
    public PrintService[] getPrintServices(final DocFlavor flavor,
                    final AttributeSet attributes) {
        final PrintService[] matchingServices;
        final PrintService[] allServices = getPrintServices();
        final Vector<PrintService> v = new Vector<PrintService>(
                        allServices.length);

        for (PrintService srv : allServices) {
            if (((flavor == null) || srv.isDocFlavorSupported(flavor))
                            && (srv
                                            .getUnsupportedAttributes(flavor,
                                                            attributes) == null)) {
                v.add(srv);
            }
        }

        matchingServices = new PrintService[v.size()];
        return v.toArray(matchingServices);
    }
}
