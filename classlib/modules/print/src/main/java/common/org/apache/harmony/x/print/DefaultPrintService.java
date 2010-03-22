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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeListener;

public class DefaultPrintService implements PrintService {

    //= Fields ===============================================================//

    private PrintClient client = null;
    private EventNotifier notifier = null;
    private String serviceName = null;

    //= Constructors =========================================================//

    public DefaultPrintService(String servicename, PrintClient printclient) {
        if (printclient == null || servicename == null) {
            throw new NullPointerException("Argument is null");
        }

        this.client = printclient;
        this.serviceName = servicename;
        notifier = EventNotifier.getNotifier();
    }

    //= Basic methods ======================================================//

    PrintClient getPrintClient() {
        return client;
    }

    public String getName() {
        return serviceName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof DefaultPrintService) {
            DefaultPrintService service = (DefaultPrintService) obj;
            if (service.getName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return serviceName.hashCode();
    }

    public String toString() {
        return "Printer : " + serviceName;
    }

    //= Print service attributes ===========================================//

    public PrintServiceAttribute getAttribute(Class category) {
        if (!PrintServiceAttribute.class.isAssignableFrom(category)) {
            throw new IllegalArgumentException();
        }
        PrintServiceAttributeSet attributes = getAttributes();
        if (attributes.containsKey(category)) {
            PrintServiceAttribute attribute = (PrintServiceAttribute) attributes
                    .get(category);
            return attribute;
        }
        return null;
    }

    public PrintServiceAttributeSet getAttributes() {
        return AttributeSetUtilities.unmodifiableView(client.getAttributes());
    }

    //= Print request attributes =============================================//

    public Class[] getSupportedAttributeCategories() {
        return client.getSupportedAttributeCategories();
    }

    public boolean isAttributeCategorySupported(Class category) {
        if (category == null) {
            throw new NullPointerException("Argument 'category' is null");
        }
        if (!(Attribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException(
                    "Argument 'category' must implement interface Attribute");
        }

        Class[] categories = getSupportedAttributeCategories();
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                return true;
            }
        }
        return false;
    }

    public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
            AttributeSet attributes) {
        if (attributes == null) {
            return null;
        }
        if (flavor != null && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException("Flavor " + flavor.getMimeType()
                    + " is not supported by print service");
        }
        
        Attribute[] attrs = attributes.toArray();
        HashAttributeSet unsupported = new HashAttributeSet();
        for (int i = 0; i < attrs.length; i++) {
            if (!isAttributeValueSupported(attrs[i], flavor, attributes)) {
                unsupported.add(attrs[i]);
            }
        }
        if (unsupported.size() > 0) {
            return unsupported;
        }
        return null;
    }

    public Object getDefaultAttributeValue(Class category) {
        if (category == null) {
            throw new NullPointerException("Argument 'category' is null");
        }
        if (!(Attribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException(
                    "Argument 'category' must implement interface Attribute");
        }

        return client.getDefaultAttributeValue(category);
    }

    public Object getSupportedAttributeValues(Class category, DocFlavor flavor,
            AttributeSet attributes) {
        if (category == null) {
            throw new NullPointerException("Argument is null");
        }
        if (!(Attribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException(
                    "Argument must implement interface Attribute");
        }
        if (flavor == null) {
            return client.getSupportedAttributeValues(category, flavor,
                    attributes);
        }

        DocFlavor clientFlavors[] = client.getSupportedDocFlavors();
        if (isDocFlavorSupportedByClient(flavor, clientFlavors)) {
            return client.getSupportedAttributeValues(category, flavor,
                    attributes);
        }
        /*
         * Searching stream print service factories, which
         * able to convert print data to flavor supported by
         * PrintClient (both user and internal). And then,
         * return supported attributes by created stream print
         * service
         */
        for (int i = 0; i < clientFlavors.length; i++) {
            StreamPrintServiceFactory[] factories = StreamPrintServiceFactory
                    .lookupStreamPrintServiceFactories(flavor, clientFlavors[i]
                            .getMimeType());
            for (int j = 0; j < factories.length; j++) {
                StreamPrintService sps = factories[j]
                        .getPrintService(new ByteArrayOutputStream());
                if (sps != null) {
                    try {
                        sps.getOutputStream().close();
                    } catch (IOException e) {
                        // just ignore
                    }
                    sps.dispose();
                    //return sps.getSupportedAttributeValues(category,
                    //        flavor, attributes);
                    return client.getSupportedAttributeValues(category,
                            clientFlavors[i], attributes);
                }
            }
        }

        throw new IllegalArgumentException("DocFlavor '" + flavor
                + "' is not supported by the print service");
    }

    public boolean isAttributeValueSupported(Attribute attrval,
            DocFlavor flavor, AttributeSet attributes) {
        if (attrval == null) {
            throw new NullPointerException("Argument is null");
        }

        if (flavor == null) {
            return client
                    .isAttributeValueSupported(attrval, flavor, attributes);
        }

        DocFlavor clientFlavors[] = client.getSupportedDocFlavors();
        if (isDocFlavorSupportedByClient(flavor, clientFlavors)) {
            return client
                    .isAttributeValueSupported(attrval, flavor, attributes);
        }

        /*
         * Searching stream print service factories, which
         * able to convert print data to flavor supported by
         * PrintClient (both user and internal). And then,
         * return supported attributes by created stream print
         * service
         */
        for (int i = 0; i < clientFlavors.length; i++) {
            StreamPrintServiceFactory[] factories = StreamPrintServiceFactory
                    .lookupStreamPrintServiceFactories(flavor, clientFlavors[i]
                            .getMimeType());
            for (int j = 0; j < factories.length; j++) {
                StreamPrintService sps = factories[j]
                        .getPrintService(new ByteArrayOutputStream());
                if (sps != null) {
                    try {
                        sps.getOutputStream().close();
                    } catch (IOException e) {
                        // just ignore
                    }
                    sps.dispose();
                    //return sps.isAttributeValueSupported(attrval, flavor, attributes);
                    return client.isAttributeValueSupported(attrval,
                            clientFlavors[i], attributes);
                }
            }
        }

        throw new IllegalArgumentException("DocFlavor '" + flavor
                + "' is not supported by the print service");

    }

    //= Listeners ============================================================//

    public void addPrintServiceAttributeListener(
            PrintServiceAttributeListener listener) {
        notifier.addListener(this, listener);
    }

    public void removePrintServiceAttributeListener(
            PrintServiceAttributeListener listener) {
        notifier.removeListener(this, listener);
    }

    //= DocFlavors ===========================================================//

    /*
     * Returns two categories of DocFlavors:
     *  1) DocFlavors supported by PrintClient
     *  2) DocFlavors that can be converted by StreamPrintServices to 
     *     PrintClient's DocFlavors
     * 
     *  If there is a DocFlavor that supported by PrintClient and by
     *  StreamPrintService, the method returns PrintClient's one only. 
     */

    public DocFlavor[] getSupportedDocFlavors() {
        DocFlavor clientFlavors[] = client.getSupportedDocFlavors();
        ArrayList flavors = new ArrayList();

        /*
         * Putting all PrintClient's supported flavors (except
         * internal flavors) into list of flavors supported by
         * this print service.
         */
        for (int i = 0; i < clientFlavors.length; i++) {
            if (!isInternalDocFlavor(clientFlavors[i])) {
                flavors.add(clientFlavors[i]);
            }
        }

        /*
         * Searching stream print service factories, which
         * able to convert print data to flavor supported by
         * PrintClient (both user and internal). And then,
         * gathering all flavors supported by those factories
         * and putting them into list of flavors supported
         * by this print service.
         */
        for (int i = 0; i < clientFlavors.length; i++) {
            StreamPrintServiceFactory[] factories = StreamPrintServiceFactory
                    .lookupStreamPrintServiceFactories(null, clientFlavors[i]
                            .getMimeType());
            for (int j = 0; j < factories.length; j++) {
                DocFlavor[] factoryFlavors = factories[j]
                        .getSupportedDocFlavors();
                for (int k = 0; k < factoryFlavors.length; k++) {
                    if (!flavors.contains(factoryFlavors[k])) {
                        flavors.add(factoryFlavors[k]);
                    }
                }
            }
        }
        return (DocFlavor[]) flavors.toArray(new DocFlavor[0]);
    }

    public boolean isDocFlavorSupported(DocFlavor flavor) {
        if (flavor == null) {
            throw new NullPointerException("DocFlavor flavor is null");
        }

        DocFlavor[] flavors = getSupportedDocFlavors();
        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Checks, whether specified falvor is internal or not.
     */
    private boolean isInternalDocFlavor(DocFlavor flavor) {
        if (flavor.getMimeType().toLowerCase().indexOf("internal") != -1) {
            return true;
        }
        return false;
    }

    /*
     * Checks, whether specified falvor is supported by
     * PrintClient or not.
     */
    boolean isDocFlavorSupportedByClient(DocFlavor flavor) {
        DocFlavor clientFlavors[] = client.getSupportedDocFlavors();
        for (int i = 0; i < clientFlavors.length; i++) {
            if (clientFlavors[i].equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    boolean isDocFlavorSupportedByClient(DocFlavor flavor,
            DocFlavor[] clientFlavors) {
        for (int i = 0; i < clientFlavors.length; i++) {
            if (clientFlavors[i].equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    //= Service user interface factory =======================================//

    public ServiceUIFactory getServiceUIFactory() {
        // We have not service user interface factory
        return null;
    }

    //= DocPrintJob ==========================================================//

    public DocPrintJob createPrintJob() {
        return new DefaultPrintJob(this);
    }
}