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
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print.cups;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Vector;

import javax.print.DocFlavor;
import javax.print.MultiDocPrintService;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;

import org.apache.harmony.x.print.DefaultPrintService;
import org.apache.harmony.x.print.ipp.IppAttribute;
import org.apache.harmony.x.print.ipp.IppAttributeGroup;
import org.apache.harmony.x.print.ipp.IppClient;
import org.apache.harmony.x.print.ipp.IppOperation;
import org.apache.harmony.x.print.ipp.IppPrinter;
import org.apache.harmony.x.print.ipp.IppRequest;
import org.apache.harmony.x.print.ipp.IppResponse;


/*
 * The class extends PrintServiceLookup and is intended for
 * looking up CUPS/IPP printers
 * 
 * 1. The class allways looks printers on http://localhost:631
 *    This URL is default URL for default installation of CUPS server
 * 2. The class accepts two properties:
 *      print.cups.servers - a list of CUPS servers
 *      print.ipp.printers - a list of IPP printers 
 *                           (note, that CUPS printer is IPP printer too) 
 */
public class CUPSPrintServiceProvider extends PrintServiceLookup {
    private static String cupsdefault = "http://localhost:631";
    private static ArrayList services = new ArrayList();
    /*
     * 0 - no
     * 1 - more
     * 2 - more and more
     * ...
     */
    private static int verbose = 0;

    static {
        String verbose_property = (String) AccessController
                .doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        return System.getProperty("print.cups.verbose");
                    }
                });
        if (verbose_property != null) {
            try {
                Integer v = new Integer(verbose_property);
                setVerbose(v.intValue());
            } catch (NumberFormatException e) {
                setVerbose(0);
            }

        }
    }

    public CUPSPrintServiceProvider() {
        super();
    }

    /*
     * The method returns array of URLs of CUPS servers
     */
    private static String[] getCUPSServersByProperty() {
        ArrayList cupslist = new ArrayList();
        cupslist.add(cupsdefault);

        String cupspath = (String) AccessController
                .doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        return System.getProperty("print.cups.servers");
                    }
                });
        String pathsep = ",";
        if (cupspath != null && !cupspath.equals("")) {
            String[] cupss = cupspath.split(pathsep);
            for (int i = 0, ii = cupss.length; i < ii; i++) {
                if (!cupss[i].equals("")) {
                    try {
                        URI cupsuri = new URI(cupss[i]);
                        cupslist.add(cupsuri.toString());
                    } catch (URISyntaxException e) {
                        if (verbose > 0) {
                            System.err.println("CUPS url: " + cupss[i]);
                            e.printStackTrace();
                        } else {
                            // IGNORE bad URI exception
                        }
                    }
                }
            }
        }

        return (String[]) cupslist.toArray(new String[0]);
    }

    /*
     * The method returns array of URLs of IPP printers
     */
    private static String[] getIppPrintersByProperty() {
        ArrayList ipplist = new ArrayList();

        String ipppath = (String) AccessController
                .doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        return System.getProperty("print.ipp.printers");
                    }
                });
        String pathsep = ","; //System.getProperty("path.separator");
        if (ipppath != null && !ipppath.equals("")) {
            String[] ipps = ipppath.split(pathsep);
            for (int i = 0, ii = ipps.length; i < ii; i++) {
                if (!ipps[i].equals("")) {
                    try {
                        URI cupsuri = new URI(ipps[i]);
                        ipplist.add(cupsuri.toString());
                    } catch (URISyntaxException e) {
                        if (verbose > 0) {
                            System.err.println("IPP url: " + ipps[i]);
                            e.printStackTrace();
                        } else {
                            // IGNORE bad URI exception
                        }
                    }
                }
            }
        }

        return (String[]) ipplist.toArray(new String[0]);
    }

    /*
     * @see javax.print.PrintServiceLookup#getDefaultPrintService()
     */
    public PrintService getDefaultPrintService() {
        synchronized (this) {
            String defaultService = findDefaultPrintService();

            if (defaultService != null) {
                PrintService service = getServiceStored(defaultService,
                        services);
                if (service != null) {
                    return service;
                }

                CUPSClient client;
                try {
                    client = new CUPSClient(defaultService);
                    service = new DefaultPrintService(defaultService, client);
                    services.add(service);
                    return service;
                } catch (PrintException e) {
                    // just ignore
                    e.printStackTrace();
                }
            }

            if (services.size() == 0) {
                getPrintServices();
            }
            if (services.size() > 0) {
                return (PrintService) services.get(0);
            }

        }
        return null;
    }

    /*
     * @see javax.print.PrintServiceLookup#getPrintServices()
     */
    public PrintService[] getPrintServices() {
        synchronized (this) {
            String[] serviceNames = findPrintServices();
            if (serviceNames == null || serviceNames.length == 0) {
                services.clear();
                return new PrintService[0];
            }

            ArrayList newServices = new ArrayList();
            for (int i = 0; i < serviceNames.length; i++) {
                PrintService service = getServiceStored(serviceNames[i],
                        services);
                if (service != null) {
                    newServices.add(service);
                } else if (getServiceStored(serviceNames[i], newServices) == null) {
                    try {
                        CUPSClient client = new CUPSClient(serviceNames[i]);

                        service = new DefaultPrintService(serviceNames[i],
                                client);
                        newServices.add(service);
                    } catch (PrintException e) {
                        // just ignore
                        e.printStackTrace();
                    }
                }
            }

            services.clear();
            services = newServices;
            return (services.size() == 0) ? new PrintService[0]
                    : (PrintService[]) services.toArray(new PrintService[0]);
        }
    }

    /*
     * find printers on particular CUPS server
     */
    private PrintService[] getCUPSPrintServices(String cups) {
        synchronized (this) {
            // just update static field 'services'
            findPrintServices();

            // next find services on server 'cups'
            String[] serviceNames = (String[]) findCUPSPrintServices(cups)
                    .toArray(new String[0]);
            if (serviceNames == null || serviceNames.length == 0) {
                return new PrintService[0];
            }

            // return only those are stored in field 'services'
            ArrayList newServices = new ArrayList();
            for (int i = 0; i < serviceNames.length; i++) {
                PrintService service = getServiceStored(serviceNames[i],
                        services);
                if (service != null) {
                    newServices.add(service);
                }
            }

            return (newServices.size() == 0) ? new PrintService[0]
                    : (PrintService[]) services.toArray(new PrintService[0]);
        }
    }

    /*
     * find printers on localhost only
     */
    public PrintService[] getPrintServicesOnLocalHost() {
        return getCUPSPrintServices(cupsdefault);
    }

    /*
     * find service which name is same as serviceName
     */
    private PrintService getServiceStored(String serviceName,
            ArrayList servicesList) {
        for (int i = 0; i < servicesList.size(); i++) {
            PrintService service = (PrintService) servicesList.get(i);
            if (service.getName().equals(serviceName)) {
                return service;
            }
        }
        return null;
    }

    /*
     * @see javax.print.PrintServiceLookup#getPrintServices(javax.print.DocFlavor
     *      , javax.print.attribute.AttributeSet)
     */
    public PrintService[] getPrintServices(DocFlavor flavor,
            AttributeSet attributes) {
        PrintService[] cupsservices = getPrintServices();
        if (flavor == null && attributes == null) {
            return cupsservices;
        }

        ArrayList requestedServices = new ArrayList();
        for (int i = 0; i < cupsservices.length; i++) {
            try {
                AttributeSet unsupportedSet = cupsservices[i]
                        .getUnsupportedAttributes(flavor, attributes);
                if (unsupportedSet == null) {
                    requestedServices.add(cupsservices[i]);
                }
            } catch (IllegalArgumentException iae) {
                // DocFlavor not supported by service, skiping.
            }
        }
        return (requestedServices.size() == 0) ? new PrintService[0]
                : (PrintService[]) requestedServices
                        .toArray(new PrintService[0]);
    }

    /*
     * @see javax.print.PrintServiceLookup#getMultiDocPrintServices(javax.print.DocFlavor[]
     *      , javax.print.attribute.AttributeSet)
     */
    public MultiDocPrintService[] getMultiDocPrintServices(DocFlavor[] flavors,
            AttributeSet attributes) {
        // No multidoc print services available, yet.
        return new MultiDocPrintService[0];
    }

    /*
     * find all printers
     */
    private static String[] findPrintServices() {
        ArrayList ippservices = new ArrayList();

        /*
         * First, find on localhost and servers from print.cups.servers property
         * and add them to full list
         */
        String[] cupses = CUPSPrintServiceProvider.getCUPSServersByProperty();
        for (int j = 0; j < cupses.length; j++) {
            ippservices.addAll(findCUPSPrintServices(cupses[j]));
        }

        /*
         * Then, check URLs from print.ipp.printers property and 
         * if is valid ipp printer add them to full list
         */
        String[] ippp = CUPSPrintServiceProvider.getIppPrintersByProperty();
        for (int j = 0; j < ippp.length; j++) {
            try {
                URI ippuri = new URI(ippp[j]);
                IppPrinter printer = new IppPrinter(ippuri);
                IppResponse response;

                response = printer.requestPrinterAttributes(
                        "printer-uri-supported", null);

                Vector gg = response
                        .getGroupVector(IppAttributeGroup.TAG_GET_PRINTER_ATTRIBUTES);
                if (gg != null) {
                    for (int i = 0, ii = gg.size(); i < ii; i++) {
                        IppAttributeGroup g = (IppAttributeGroup) gg.get(i);
                        int ai = g.findAttribute("printer-uri-supported");

                        if (ai >= 0) {
                            IppAttribute a = (IppAttribute) g.get(ai);
                            Vector v = a.getValue();
                            if (v.size() > 0) {
                                ippservices.add(new String((byte[]) v.get(0)));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (verbose > 0) {
                    System.err.println("IPP url: " + ippp[j]);
                    e.printStackTrace();
                } else {
                    // IGNORE - connection refused due to no server, etc.
                }
            }
        }

        // return array of printers
        return (String[]) ippservices.toArray(new String[0]);
    }

    /*
     * find ipp printers on CUPS server 'cups'
     */
    public static ArrayList findCUPSPrintServices(String cups) {
        ArrayList ippservices = new ArrayList();

        URI cupsuri = null;
        IppClient c = null;
        IppRequest request;
        IppResponse response;
        IppAttributeGroup agroup;
        Vector va = new Vector();

        request = new IppRequest(1, 1, IppOperation.TAG_CUPS_GET_PRINTERS,
                "utf-8", "en-us");
        agroup = request.getGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
        va.add("printer-uri-supported".getBytes());
        agroup.add(new IppAttribute(IppAttribute.TAG_KEYWORD,
                "requested-attributes", va));

        try {
            cupsuri = new URI(cups);
            c = new IppClient(cupsuri);

            response = c.request(request.getBytes());

            Vector gg = response
                    .getGroupVector(IppAttributeGroup.TAG_GET_PRINTER_ATTRIBUTES);
            if (gg != null) {
                for (int i = 0, ii = gg.size(); i < ii; i++) {
                    IppAttributeGroup g = (IppAttributeGroup) gg.get(i);
                    int ai = g.findAttribute("printer-uri-supported");

                    if (ai >= 0) {
                        IppAttribute a = (IppAttribute) g.get(ai);
                        Vector v = a.getValue();
                        if (v.size() > 0) {
                            ippservices.add(new String((byte[]) v.get(0)));
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (verbose > 0) {
                System.err.println("CUPS url: " + cups);
                System.err.println("CUPS uri: " + cupsuri);
                System.err.println("Ipp client: " + c);
                System.err.println(request.toString());
                e.printStackTrace();
            } else {
                // IGNORE - connection refused due to no server, etc.
            }
        }

        return ippservices;
    }

    /*
     * find default printer
     * At first, try to find default printer on CUPS servers and return first found 
     * If failed, return first found IPP printer
     * If failed return null
     */
    private static String findDefaultPrintService() {
        String serviceName = null;

        String[] cupses = CUPSPrintServiceProvider.getCUPSServersByProperty();
        for (int i = 0; i < cupses.length; i++) {
            try {
                URI cupsuri = new URI(cupses[i]);
                IppClient c = new IppClient(cupsuri);
                IppRequest request;
                IppResponse response;
                IppAttributeGroup agroup;
                Vector va = new Vector();

                request = new IppRequest(1, 1,
                        IppOperation.TAG_CUPS_GET_DEFAULT, "utf-8", "en-us");
                agroup = request
                        .getGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
                va.add("printer-uri-supported".getBytes());
                agroup.add(new IppAttribute(IppAttribute.TAG_KEYWORD,
                        "requested-attributes", va));

                response = c.request(request.getBytes());

                IppAttributeGroup g = response
                        .getGroup(IppAttributeGroup.TAG_GET_PRINTER_ATTRIBUTES);
                if (g != null) {
                    int ai = g.findAttribute("printer-uri-supported");

                    if (ai >= 0) {
                        IppAttribute a = (IppAttribute) g.get(ai);
                        Vector v = a.getValue();
                        if (v.size() > 0) {
                            serviceName = new String((byte[]) v.get(0));
                            break;
                        }
                    }
                }
            } catch (URISyntaxException e) {
                //e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        if (serviceName != null && !serviceName.equals("")) {
            return serviceName;
        }

        String[] ippp = CUPSPrintServiceProvider.getIppPrintersByProperty();
        for (int i = 0; i < ippp.length; i++) {
            try {
                URI ippuri = new URI(ippp[i]);
                IppClient c = new IppClient(ippuri);
                IppRequest request;
                IppResponse response;
                IppAttributeGroup agroup;
                Vector va = new Vector();

                request = new IppRequest(1, 1,
                        IppOperation.GET_PRINTER_ATTRIBUTES, "utf-8", "en-us");
                agroup = request
                        .getGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
                va.add("printer-uri-supported".getBytes());
                agroup.add(new IppAttribute(IppAttribute.TAG_KEYWORD,
                        "requested-attributes", va));

                response = c.request(request.getBytes());

                IppAttributeGroup g = response
                        .getGroup(IppAttributeGroup.TAG_GET_PRINTER_ATTRIBUTES);
                if (g != null) {
                    int ai = g.findAttribute("printer-uri-supported");

                    if (ai >= 0) {
                        IppAttribute a = (IppAttribute) g.get(ai);
                        Vector v = a.getValue();
                        if (v.size() > 0) {
                            serviceName = new String((byte[]) v.get(0));
                            break;
                        }
                    }
                }
            } catch (URISyntaxException e) {
                //e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        return serviceName;
    }

    public static int isVerbose() {
        return verbose;
    }

    public static void setVerbose(int newverbose) {
        CUPSPrintServiceProvider.verbose = newverbose;
        CUPSClient.setVerbose(newverbose);
    }
}
