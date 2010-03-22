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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.print.attribute.AttributeSet;

public abstract class PrintServiceLookup {
    private static List<PrintService> services;

    private static List<PrintServiceLookup> providers;
    static {
        services = new ArrayList<PrintService>();
        providers = new ArrayList<PrintServiceLookup>();
        ClassLoader classLoader = AccessController
                .doPrivileged(new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        if (cl == null) {
                            cl = ClassLoader.getSystemClassLoader();
                        }
                        return cl;
                    }
                });
        if (classLoader != null) {
            Enumeration<URL> providersEnum;
            try {
                providersEnum = classLoader
                        .getResources("META-INF/services/javax.print.PrintServiceLookup");
            } catch (IOException e) {
                providersEnum = null;
            }
            if (providersEnum != null) {
                while (providersEnum.hasMoreElements()) {
                    registerProvidersFromURL(providersEnum.nextElement());
                }
            }
        }
    }

    private static void registerProvidersFromURL(URL url) {
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        BufferedReader providerNameReader;
        String name = null;
        try {
            Reader temp = new InputStreamReader(is, "UTF-8");
            providerNameReader = new BufferedReader(temp);
        } catch (UnsupportedEncodingException uee) {
            // UTF-8 must be supported by the JRE
            throw new AssertionError(uee);
        }
        if (providerNameReader != null) {
            try {
                name = providerNameReader.readLine();
                while (name != null) {
                    if (name.length() > 0 && name.charAt(0) != '#') {
                        final Class<?> providerClass = Class.forName(name);
                        class Action implements PrivilegedAction<Object> {
                            public Object run() {
                                try {
                                    Object inst = providerClass.newInstance();
                                    return inst;
                                } catch (InstantiationException ie) {
                                    System.err.println("Can't instantiate class "
                                            + providerClass.getName() + ": " + ie);
                                } catch (IllegalAccessException iae) {
                                    System.out.println("Illegal access for class "
                                            + providerClass.getName() + ": " + iae);
                                }
                                return null;
                            }
                        }
                        Object provider = AccessController.doPrivileged(new Action());
                        if (provider != null) {
                            registerServiceProvider((PrintServiceLookup) provider);
                        }
                    }
                    name = providerNameReader.readLine();
                }
            } catch (IOException e) {
                System.out.println("IOException during reading file:" + e);
            } catch (ClassNotFoundException e) {
                System.out.println("Class" + name + " is not found:" + e);
            }
        }
    }

    public PrintServiceLookup() {
        super();
    }

    public abstract PrintService getDefaultPrintService();

    public abstract PrintService[] getPrintServices();

    public abstract PrintService[] getPrintServices(DocFlavor flavor, AttributeSet attributes);

    public abstract MultiDocPrintService[] getMultiDocPrintServices(DocFlavor[] flavors,
            AttributeSet attributes);

    public static final PrintService lookupDefaultPrintService() {
        synchronized (providers) {
            if (providers.isEmpty()) {
                return null;
            }
            PrintServiceLookup provider = providers.get(0);
            return provider.getDefaultPrintService();
        }
    }

    public static final PrintService[] lookupPrintServices(DocFlavor flavor,
            AttributeSet attributes) {
        synchronized (providers) {
            List<PrintService> printServices = new ArrayList<PrintService>();
            int providersSize = providers.size();
            for (int i = 0; i < providersSize; i++) {
                PrintServiceLookup provider = providers.get(i);
                PrintService[] providerServices = provider.getPrintServices(flavor, attributes);
                for (int j = 0; j < providerServices.length; j++) {
                    printServices.add(providerServices[j]);
                }
            }
            return printServices.toArray(new PrintService[printServices.size()]);
        }
    }

    public static final MultiDocPrintService[] lookupMultiDocPrintServices(DocFlavor[] flavors,
            AttributeSet attributes) {
        synchronized (providers) {
            List<MultiDocPrintService> printServices = new ArrayList<MultiDocPrintService>();
            int providersSize = providers.size();
            for (int i = 0; i < providersSize; i++) {
                PrintServiceLookup provider = providers.get(i);
                MultiDocPrintService[] providerServices = provider.getMultiDocPrintServices(
                        flavors, attributes);
                if (providerServices != null) {
                    for (int j = 0; j < providerServices.length; j++) {
                        printServices.add(providerServices[j]);
                    }
                }
            }
            return printServices.toArray(new MultiDocPrintService[printServices.size()]);
        }
    }

    public static boolean registerService(PrintService service) {
        synchronized (services) {
            return services.add(service);
        }
    }

    public static boolean registerServiceProvider(PrintServiceLookup provider) {
        synchronized (providers) {
            return providers.add(provider);
        }
    }
}
