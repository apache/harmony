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
 * @author Aleksei V. Ivaschenko 
 */ 

package org.apache.harmony.x.print;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import javax.print.DocFlavor;
import javax.print.MultiDocPrintService;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;

import org.apache.harmony.x.print.DefaultPrintService;

/*
 * Print service provider for windows. Loads native library,
 * searches printers and creates GDI clients for them.
 */
public class Win32PrintServiceProvider extends PrintServiceLookup {

    private static boolean libraryLoaded = false; 
    
    static {
        Object result = AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    System.loadLibrary("print");
                    return new Boolean(true);
                } catch (SecurityException se) {
                    // SecurityManager doesn't permit library loading.
                } catch (UnsatisfiedLinkError ule) {
                    // Can't find library.
                }
                return new Boolean(false);
            }
        });
        libraryLoaded = ((Boolean)result).booleanValue();
    }

    private static ArrayList services = new ArrayList(); 
    
    /*
     * Default public constructor.
     */
    public Win32PrintServiceProvider() {
        super();
    }

    /*
     * Searches default printer connected to current host.
     * @see javax.print.PrintServiceLookup#getDefaultPrintService()
     */
    public PrintService getDefaultPrintService() {
        if (libraryLoaded) {
            String defaultService = findDefaultPrintService();
            if (defaultService != null) {
                PrintService service = getServiceStored(defaultService);
                if (service != null) {
                    return service;
                }
                GDIClient client = new GDIClient(defaultService);
                service = new DefaultPrintService(defaultService, client);
                services.add(service);
                return service;
            }
        }
        return null;
    }

    /*
     * Searches all printers connected to current host.
     * @see javax.print.PrintServiceLookup#getPrintServices()
     */
    public PrintService[] getPrintServices() {
        if (!libraryLoaded) {
            return new PrintService[0];
        }
        String[] serviceNames = findPrintServices();
        if (serviceNames == null || serviceNames.length == 0) {
            services.clear();
            return new PrintService[0]; 
        }
        ArrayList newServices = new ArrayList();
        for (int i = 0; i < serviceNames.length; i++) {
            PrintService service = getServiceStored(serviceNames[i]);
            if (service != null) {
                newServices.add(service);
            } else {
                GDIClient client = new GDIClient(serviceNames[i]);
                service = new DefaultPrintService(serviceNames[i], client);
                newServices.add(service);
            }
        }
        services.clear();
        services = newServices;
        return (services.size() == 0) ? new PrintService[0] :
            (PrintService[])services.toArray(new PrintService[0]);
    }
    
    private PrintService getServiceStored(String serviceName) {
        for (int i = 0; i < services.size(); i++) {
            PrintService service = (PrintService)services.get(i);
            if (service.getName().equals(serviceName)) {
                return service;
            }
        }
        return null;
    }

    /*
     * Searches printers connected to current host, which match
     * requested doc's flavor and attributes. 
     * @see javax.print.PrintServiceLookup#getPrintServices(
     * javax.print.DocFlavor, javax.print.attribute.AttributeSet)
     */
    public PrintService[] getPrintServices(DocFlavor flavor,
            AttributeSet attributes) {
        PrintService[] services = getPrintServices();
        if (flavor == null && attributes == null) {
            return services;
        }
        ArrayList requestedServices = new ArrayList();
        for (int i = 0; i < services.length; i++) {
            
            try {
                AttributeSet unsupportedSet =
                    services[i].getUnsupportedAttributes(flavor, attributes);
                if (unsupportedSet == null && (flavor == null ||
                        services[i].isDocFlavorSupported(flavor))) {
                    requestedServices.add(services[i]);
                }
            } catch (IllegalArgumentException iae) {
                // DocFlavor not supported by service, skiping.
            }
        }
        return (requestedServices.size() == 0) ? new PrintService[0] :
            (PrintService[])requestedServices.toArray(new PrintService[0]);
    }

    /*
     * Searches printers connected to current host, which are able
     * to print multidocs and match requested doc's flavor and attributes. 
     * @see javax.print.PrintServiceLookup#getMultiDocPrintServices(
     * javax.print.DocFlavor[], javax.print.attribute.AttributeSet)
     */
    public MultiDocPrintService[] getMultiDocPrintServices(DocFlavor[] flavors,
            AttributeSet attributes) {
        // No multidoc print services available.
        return new MultiDocPrintService[0];
    }

    private static native String[] findPrintServices();
    private static native String findDefaultPrintService();
}
