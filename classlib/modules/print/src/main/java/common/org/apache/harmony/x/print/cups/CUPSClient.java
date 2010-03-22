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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Locale;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.RequestingUserName;

import org.apache.harmony.x.print.PrintClient;
import org.apache.harmony.x.print.ipp.IppAttribute;
import org.apache.harmony.x.print.ipp.IppAttributeGroup;
import org.apache.harmony.x.print.ipp.IppAttributeGroupSet;
import org.apache.harmony.x.print.ipp.IppDocument;
import org.apache.harmony.x.print.ipp.IppPrinter;
import org.apache.harmony.x.print.ipp.IppResponse;
import org.apache.harmony.x.print.ipp.util.Ipp2Java;


/*
 * CUPSClient is a print client based on CUPS protocol.
 * (see Common UNIX Printing System, http://www.cups.org/)
 * 
 * The CUPS itself extends IPP protocol
 * (see Internet Printing Protocol, http://www.pwg.org/ipp/index.html)
 * 
 * So, this class supports as CUPS as IPP print servers 
 * 
 * The class uses special IPP package org.apache.harmony.x.print.ipp for
 * ipp/cups specific operations.
 * 
 * CUPSClient implements PrintClient interface, therefore
 * see PrintClient.java for more information.
 * 
 * 
 */
class CUPSClient implements PrintClient {
    // for debug
    private static int verbose = 0;

    private IppPrinter printer;
    private URI printeruri;
    private PrintServiceAttributeSet attributeset;
    private DocFlavor[] supportedFlavors = null;

    CUPSClient(String name) throws PrintException {
        try {
            this.printeruri = new URI(name);
            this.printer = new IppPrinter(printeruri);
            this.attributeset = new HashPrintServiceAttributeSet();
        } catch (Exception e) {
            throw new PrintException(e);
        }
    }

    /* 
     * SPECIAL - supportedFlavors is global for performance 
     * but it can be set local for dynamic
     * 
     * @org.apache.harmony.x.print.PrintClient#getSupportedDocFlavors()
     */
    public DocFlavor[] getSupportedDocFlavors() {
        if (supportedFlavors == null) {
            ArrayList df = new ArrayList();

            try {
                String[] mimetypes = new String[ALLDOCFLAVORS.length];
                String[] validmimes;

                for (int i = 0, ii = ALLDOCFLAVORS.length; i < ii; i++) {
                    mimetypes[i] = ALLDOCFLAVORS[i].getMimeType();
                }
                validmimes = printer.requestGetSupportedMimeTypes(mimetypes);
                for (int i = 0, ii = ALLDOCFLAVORS.length; i < ii; i++) {
                    if (validmimes[i] != null) {
                        if (validmimes[i].equals("application/ps")) {
                            /*
                             * SPECIAL processing application/ps
                             */
                            df.add(ipp2java(ALLDOCFLAVORS[i]));
                        } else {
                            df.add(ALLDOCFLAVORS[i]);
                        }
                    }
                }
            } catch (Exception e) {
                // IGNORE exception
                e.printStackTrace();
            }

            supportedFlavors = (df.size() == 0 ? new DocFlavor[] { DocFlavor.INPUT_STREAM.AUTOSENSE }
                    : (DocFlavor[]) df.toArray(new DocFlavor[0]));
        }
        return supportedFlavors;
    }

    /* 
     * @see org.apache.harmony.x.print.PrintClient#getAttributes()
     */
    public PrintServiceAttributeSet getAttributes() {
        synchronized (this) {
            try {
                IppResponse response;
                IppAttributeGroup agroup;
                IppAttribute attr;
                Object[] attrx = new Object[0];

                response = printer.requestPrinterDescriptionAttributes();
                agroup = response
                        .getGroup(IppAttributeGroup.TAG_GET_PRINTER_ATTRIBUTES);
                if (agroup != null) {
                    attributeset.clear();
                    for (int i = 0, ii = agroup.size(); i < ii; i++) {
                        attr = (IppAttribute) agroup.get(i);
                        attrx = Ipp2Java.getJavaByIpp(attr);
                        for (int j = 0, jj = attrx.length; j < jj; j++) {
                            if (attrx[j] instanceof PrintServiceAttribute) {
                                attributeset.add((Attribute) attrx[j]);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // IGNORE exception
                e.printStackTrace();
            }
        }

        return AttributeSetUtilities.unmodifiableView(attributeset);
    }

    /*
     * @see org.apache.harmony.x.PrintClient#getSupportedAttributeCategories()
     */
    public Class[] getSupportedAttributeCategories() {
        ArrayList clazz = new ArrayList();

        try {
            IppResponse response = printer.requestPrinterAttributes();
            IppAttributeGroup agroup = response
                    .getGroup(IppAttributeGroup.TAG_GET_PRINTER_ATTRIBUTES);
            String aname;
            Class claz;
            IppAttribute attr;

            if (agroup != null) {
                for (int i = 0, ii = agroup.size(); i < ii; i++) {
                    attr = (IppAttribute) agroup.get(i);
                    aname = new String(attr.getName());
                    if (aname.indexOf("-supported") > 0) {
                        claz = Ipp2Java.getClassByIppAttributeName(aname
                                .substring(0, aname.indexOf("-supported")));
                        if (claz != null) {
                            clazz.add(claz);
                        }
                    }
                }
            }
            // SPECIAL attributes processing
            getSupportedAttributeCategoriesEx(clazz);
        } catch (Exception e) {
            // IGNORE exception
            // e.printStackTrace();
        }
        return (clazz.size() == 0 ? new Class[0] : (Class[]) clazz
                .toArray(new Class[0]));
    }

    private void getSupportedAttributeCategoriesEx(ArrayList clazz) {
        if (!clazz.contains(Destination.class)) {
            clazz.add(Destination.class);
        }
        if (!clazz.contains(RequestingUserName.class)) {
            clazz.add(RequestingUserName.class);
        }
        if (!clazz.contains(JobName.class)) {
            clazz.add(JobName.class);
        }
        if (!clazz.contains(DocumentName.class)) {
            clazz.add(DocumentName.class);
        }
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#getDefaultAttributeValue(java.lang.Class)
     */
    public Object getDefaultAttributeValue(Class category) {
        if (category == null) {
            throw new NullPointerException("Argument is null");
        }
        if (!(Attribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException(
                    "Argument must implement interface Attribute");
        }

        Object defval[] = null;

        // SPECIAL attributes processing
        defval = getDefaultAttributeValueEx(category);
        if (defval != null) {
            if (defval.length == 0) {
                return null;
            }
            return defval[0];
        }

        if (Media.class.isAssignableFrom(category)) {
            category = Media.class;
        }
        try {
            IppResponse response = printer.requestPrinterAttributes();
            IppAttributeGroup agroup = response
                    .getGroup(IppAttributeGroup.TAG_GET_PRINTER_ATTRIBUTES);
            IppAttribute attr;
            String aname;
            int andex;

            if (agroup != null) {
                aname = Ipp2Java.getIppAttributeNameByClass(category);
                if (aname != null) {
                    if (aname.endsWith("-supported")) {
                        aname = aname.substring(0, aname.indexOf("-supported"));
                    }
                    if (aname.endsWith("-default")) {
                        aname = aname.substring(0, aname.indexOf("-default"));
                    }
                    andex = agroup.findAttribute(aname + "-default");
                    if (andex >= 0) {
                        attr = (IppAttribute) agroup.get(andex);
                        defval = Ipp2Java.getJavaByIpp(attr);
                    }
                }
            }
        } catch (Exception e) {
            // IGNORE exception
            e.printStackTrace();
        }

        return (defval != null && defval.length > 0 ? defval[0] : null);
    }

    /*
     * If attribute was processed - return Object[1]
     * Else - return null
     */
    private Object[] getDefaultAttributeValueEx(Class category) {
        if (Destination.class.isAssignableFrom(category)) {
            return new Object[0];
        } else if (RequestingUserName.class.isAssignableFrom(category)) {
            return new Object[] { new RequestingUserName(
                    (String) AccessController
                            .doPrivileged(new PrivilegedAction() {
                                public Object run() {
                                    return System.getProperty("user.name");
                                }
                            }), Locale.US) };
        } else if (JobName.class.isAssignableFrom(category)) {
            return new Object[] { new JobName("Java print job", Locale.US) };
        } else if (DocumentName.class.isAssignableFrom(category)) {
            return new Object[] { new DocumentName("Java print document",
                    Locale.US) };
        }
        return null;
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#isAttributeValueSupported(javax.print.attribute.Attribute, 
     *          javax.print.DocFlavor, javax.print.attribute.AttributeSet)
     */
    public boolean isAttributeValueSupported(Attribute attribute,
            DocFlavor flavor, AttributeSet attributes) {

        // verify parameters
        if (attribute == null) {
            throw new NullPointerException("Argument is null");
        }
        if (flavor != null && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException("DocFlavor '" + flavor
                    + "' is not supported by the print service");
        }

        // SPECIAL attributes processing
        boolean[] supportedEx = isAttributeValueSupportedEx(attribute, flavor);
        if (supportedEx != null) {
            return supportedEx[0];
        }

        boolean supported = false;
        try {
            IppDocument document;
            IppResponse response;
            IppAttributeGroup agroup;
            IppAttributeGroupSet agroupset;
            Attribute[] attrs;
            String mime = null;
            String aname;

            aname = Ipp2Java.getIppAttributeNameByClass(attribute.getClass(),
                    -1);
            if (aname == null) {
                return false;
            }
            if (flavor == null) {
                mime = "application/octet-stream";
            } else {
                mime = java2ipp(flavor).getMimeType();
            }
            if (attributes == null || attributes.isEmpty()) {
                document = new IppDocument("Qwerty", mime, "");
                agroupset = new IppAttributeGroupSet();
                agroupset.setAttribute(aname, Ipp2Java.getIppByJava(attribute));
                response = printer.requestValidateJob(aname, document,
                        agroupset);
                agroup = response
                        .getGroup(IppAttributeGroup.TAG_UNSUPPORTED_ATTRIBUTES);

                if (agroup == null) {
                    supported = true;
                } else if (agroup != null && agroup.findAttribute(aname) < 0) {
                    supported = true;
                }
            } else {
                document = new IppDocument("Qwerty", mime, "");
                agroupset = new IppAttributeGroupSet();
                agroupset.setAttribute(aname, Ipp2Java.getIppByJava(attribute));
                attrs = attributes.toArray();
                for (int i = 0, ii = attrs.length; i < ii; i++) {
                    agroupset.setAttribute(Ipp2Java.getIppAttributeNameByClass(
                            attrs[i].getClass(), -1), Ipp2Java
                            .getIppByJava(attrs[i]));
                }

                response = printer.requestValidateJob(aname, document,
                        agroupset);
                agroup = response
                        .getGroup(IppAttributeGroup.TAG_UNSUPPORTED_ATTRIBUTES);

                if (agroup == null) {
                    supported = true;
                } else if (agroup != null && agroup.findAttribute(aname) < 0) {
                    supported = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return supported;
    }

    /*
     * If attribute was processed - return boolean[1]
     * Else return null
     */
    private boolean[] isAttributeValueSupportedEx(Attribute avalue,
            DocFlavor flavor) {
        if (Destination.class.isAssignableFrom(avalue.getCategory())) {
            String ms = (flavor != null ? flavor.getMediaSubtype() : "");
            Class cls = (flavor != null ? flavor.getClass() : null);

            if (ms.equalsIgnoreCase("gif") || ms.equalsIgnoreCase("jpeg")
                    || ms.equalsIgnoreCase("png")
                    || ms.equalsIgnoreCase("postscript") || flavor == null
                    || cls == DocFlavor.SERVICE_FORMATTED.class) {
                if (!canPrintToFile()) {
                    return new boolean[] { false };
                }

                URI uri = ((Destination) avalue).getURI();
                try {
                    File file = new File(uri);

                    if (file.isFile()) {
                        if (file.canWrite()) {
                            return new boolean[] { true };
                        }
                        return new boolean[] { false };
                    }

                    String path = file.getParent();
                    File parent = new File(path);
                    if (parent.isDirectory()) {
                        if (parent.canWrite()) {
                            return new boolean[] { true };
                        }
                        return new boolean[] { false };
                    }
                } catch (Exception e) {
                    return new boolean[] { false };
                }
            }
        }
        return null;
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#getSupportedAttributeValues(java.lang.Class, 
     *          javax.print.DocFlavor, javax.print.attribute.AttributeSet)
     */
    public Object getSupportedAttributeValues(Class category, DocFlavor flavor,
            AttributeSet attributes) {
        if (category == null) {
            throw new NullPointerException("Argument is null");
        }
        if (!(Attribute.class.isAssignableFrom(category))) {
            throw new IllegalArgumentException(
                    "Argument must implement interface Attribute");
        }
        if (flavor != null && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException("DocFlavor '" + flavor
                    + "' is not supported by the print service");
        }

        Object vals = null;

        // SPECIAL attributes processing
        vals = getSupportedAttributeValuesEx(category, flavor);
        if (vals != null) {
            if (((Object[]) vals).length == 0) {
                return null;
            }
            return ((Object[]) vals)[0];
        }

        // General attributes
        try {
            String aname = Ipp2Java.getIppAttributeNameByClass(category, 0)
                    + "-supported";
            doVerbose(2,
                    "CUPSClient.java: getSupportedAttributeValues(): ipp attribute: "
                            + aname);
            IppResponse response = printer.requestPrinterAttributes(aname,
                    (flavor == null ? null : java2ipp(flavor).getMimeType()));
            doVerbose(2,
                    "CUPSClient.java: getSupportedAttributeValues(): response: "
                            + response.toString());
            IppAttributeGroup agroup = response
                    .getGroup(IppAttributeGroup.TAG_GET_PRINTER_ATTRIBUTES);
            doVerbose(1,
                    "CUPSClient.java: getSupportedAttributeValues(): agroup: "
                            + agroup.toString());
            if (agroup != null) {
                int aind = agroup.findAttribute(aname);
                if (aind >= 0) {
                    IppAttribute attr = (IppAttribute) agroup.get(aind);
                    vals = Ipp2Java.getJavaByIpp(attr);
                }
            }
            doVerbose(1, "CUPSClient.java: getSupportedAttributeValues(): 1");
            // Make right type/value for return
            if (vals != null && vals.getClass().isArray()) {
                Object[] ara = (Object[]) vals;
                if (ara.length == 1 && ara[0].getClass() != category) {
                    vals = ara[0];
                }
            }
            doVerbose(1, "CUPSClient.java: getSupportedAttributeValues(): 2");
            if (vals != null && vals.getClass().isArray()) {
                int asize = ((Object[]) vals).length;
                if (asize > 0) {
                    Class c = ((Object[]) vals)[0].getClass();
                    /* SPECIAL case for Media* attributes
                     * 
                     * Special case for Media* attributes.
                     * vals[] contains all type of Media classes
                     * So, c must be Media type, not a[0] type 
                     */
                    if (Media.class.isAssignableFrom(c)) {
                        c = Media.class;
                    }
                    doVerbose(1,
                            "CUPSClient.java: getSupportedAttributeValues(): 3");
                    Object[] a = (Object[]) Array.newInstance(c, asize);
                    System.arraycopy(vals, 0, a, 0, a.length);

                    vals = a;
                } else {
                    vals = null;
                }
            }
            doVerbose(1, "CUPSClient.java: getSupportedAttributeValues(): 4");
            if (vals != null && vals.getClass().isArray()) {
                for (int i = 0, ii = ((Attribute[]) vals).length; i < ii; i++) {
                    if (!isAttributeValueSupported(((Attribute[]) vals)[i],
                            flavor, attributes)) {
                        ((Attribute[]) vals)[i] = null;
                    }
                }
                doVerbose(1,
                        "CUPSClient.java: getSupportedAttributeValues(): 5");
                int newvalslength = 0;
                for (int i = 0, ii = ((Attribute[]) vals).length; i < ii; i++) {
                    if (((Attribute[]) vals)[i] != null) {
                        newvalslength++;
                    }
                }
                doVerbose(1,
                        "CUPSClient.java: getSupportedAttributeValues(): 6");
                if (newvalslength != ((Attribute[]) vals).length) {
                    Object[] newvals = new Object[newvalslength];
                    for (int j = 0, i = 0, ii = ((Attribute[]) vals).length; i < ii; i++) {
                        if (((Attribute[]) vals)[i] != null) {
                            newvals[j++] = ((Attribute[]) vals)[i];
                        }
                    }

                    vals = newvals;
                }
            } else if (vals != null) {
                if (!isAttributeValueSupported((Attribute) vals, flavor,
                        attributes)) {
                    vals = null;
                }
            }
            doVerbose(1, "CUPSClient.java: getSupportedAttributeValues(): 7");
            return vals;
        } catch (Exception e) {
            // IGNORE exception
            e.printStackTrace();
        }
        doVerbose(1, "CUPSClient.java: getSupportedAttributeValues(): 8");
        return null;
    } /*  
     * If category processed - return non-null value
     */

    private Object[] getSupportedAttributeValuesEx(Class category,
            DocFlavor flavor) {
        if (Destination.class.isAssignableFrom(category)) {
            String ms = flavor.getMediaSubtype();

            if (ms.equalsIgnoreCase("gif") || ms.equalsIgnoreCase("jpeg")
                    || ms.equalsIgnoreCase("png")
                    || ms.equalsIgnoreCase("postscript")
                    || flavor.getClass() == DocFlavor.SERVICE_FORMATTED.class) {
                try {
                    return new Object[] { new Destination(new URI(
                            "file:///foo/bar")) };
                } catch (URISyntaxException e) {
                    // return empty array - values are not supported
                    return new Object[0];
                }
            }
        } else if (RequestingUserName.class.isAssignableFrom(category)) {
            return new Object[] { new RequestingUserName("I.A.Muser", Locale.US) };
        } else if (JobName.class.isAssignableFrom(category)) {
            return new Object[] { new JobName("Foo print job", Locale.US) };
        } else if (DocumentName.class.isAssignableFrom(category)) {
            return new Object[] { new DocumentName("Foo document", Locale.US) };
        }
        return null;
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#print(javax.print.Doc, 
     *          javax.print.attribute.PrintRequestAttributeSet)
     */
    public void print(Doc doc, PrintRequestAttributeSet attributes)
            throws PrintException {
        synchronized (this) {
            doVerbose(1, "Print " + doc.toString());
            try {
                DocFlavor df = doc.getDocFlavor();
                if (!(df instanceof DocFlavor.INPUT_STREAM
                        || df instanceof DocFlavor.BYTE_ARRAY
                        || df instanceof DocFlavor.CHAR_ARRAY
                        || df instanceof DocFlavor.STRING
                        || df instanceof DocFlavor.READER || df instanceof DocFlavor.URL)) {
                    throw new PrintException("Doc flavor "
                            + df.getRepresentationClassName()
                            + " is not supported yet");
                }

                HashAttributeSet as = new HashAttributeSet();
                DocAttributeSet das;
                das = doc.getAttributes();

                // construct attributes
                if (das != null) {
                    as.addAll(das);
                }
                if (attributes != null) {
                    as.addAll(attributes);
                }
                as.addAll(attributeset);

                // print
                if (as.containsKey(Destination.class)) {
                    print2destination(doc, (Destination) as
                            .get(Destination.class));
                } else {
                    printsimple(doc, as);
                }
            } catch (PrintException e) {
                throw e;
            } catch (Exception e) {
                throw new PrintException(e);
            }
        }
    }

    /*
     * printing to Destination
     */
    private void print2destination(Doc doc, Destination destination)
            throws PrintException {

        try {
            DataOutputStream bw = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(new File(
                            destination.getURI()))));

            if (doc != null) {
                if (doc.getDocFlavor() instanceof DocFlavor.INPUT_STREAM) {
                    InputStream stream = (InputStream) doc.getPrintData();
                    byte[] buf = new byte[1024 * 8];
                    int count = 0;

                    while ((count = stream.read(buf, 0, buf.length)) != -1) {
                        bw.write(buf, 0, count);
                    }
                    stream.close();
                } else if (doc.getDocFlavor() instanceof DocFlavor.URL) {
                    BufferedInputStream stream = new BufferedInputStream(
                            ((URL) doc.getPrintData()).openStream());
                    byte[] buf = new byte[1024 * 8];
                    int count = 0;
                    while ((count = stream.read(buf, 0, buf.length)) != -1) {
                        if (count > 0) {
                            bw.write(buf, 0, count);
                        }
                    }
                    stream.close();
                } else if (doc.getDocFlavor() instanceof DocFlavor.BYTE_ARRAY) {
                    InputStream stream = new ByteArrayInputStream((byte[]) doc
                            .getPrintData());
                    byte[] buf = new byte[1024 * 8];
                    int count = 0;

                    while ((count = stream.read(buf, 0, buf.length)) != -1) {
                        bw.write(buf, 0, count);
                    }
                    stream.close();
                } else if (doc.getDocFlavor() instanceof DocFlavor.SERVICE_FORMATTED) {
                    // TODO - print DocFlavor.SERVICE_FORMATTED
                }
            }

            bw.flush();
            bw.close();
        } catch (Exception e) {
            throw new PrintException(e);
        }
    }

    /*
     * request IppPrinter printer to print document
     */
    private void printsimple(Doc doc, HashAttributeSet as)
            throws PrintException {
        IppDocument document;
        IppResponse response;
        IppAttributeGroupSet agroupset;
        Attribute[] attrs;
        DocFlavor df = doc.getDocFlavor();
        String docname = doc.toString();

        try {
            document = new IppDocument(docname, java2ipp(df).getMimeType(), doc
                    .getPrintData());

            agroupset = new IppAttributeGroupSet();
            attrs = as.toArray();
            for (int i = 0, ii = attrs.length; i < ii; i++) {
                agroupset.setAttribute(Ipp2Java.getIppAttributeNameByClass(
                        attrs[i].getClass(), -1), Ipp2Java
                        .getIppByJava(attrs[i]));
            }
            document.setAgroups(agroupset);

            doVerbose(1, "Validating print job...");
            response = printer.requestValidateJob(docname, document, agroupset);
            doVerbose(1, response.toString());
            checkResponseIsZero(response, "IPP Validate Job: \n");
            doVerbose(1, "Validate OK");

            doVerbose(1, "Printing " + docname + "...");
            response = printer.requestPrintJob(docname, document, agroupset);
            doVerbose(1, response.toString());
            checkResponseIsZero(response, "IPP Print Job: \n");
            doVerbose(1, "Printing OK");
        } catch (PrintException e) {
            throw e;
        } catch (Exception e) {
            if (getVerbose() > 1) {
                e.printStackTrace();
            }
            throw new PrintException(e);
        }
    }

    /*
     * just check that IppResponse is OK
     */
    private void checkResponseIsZero(IppResponse response, String prefix)
            throws PrintException {
        if (response.getStatusCode() != 0) {
            String status = Integer.toHexString(response.getStatusCode());
            String id = Integer.toHexString(response.getRequestId());

            throw new PrintException(prefix
                    + "\n================ IPP response id: 0x" + id
                    + " =====================" + "\nresponse status code: 0x"
                    + status + "\n" + response.toString()
                    + "\n================ end IPP response 0x" + id
                    + " =====================");
        }
    }

    /*
     * convert DocFlavor to DocFlavor ;-)
     * 
     * some printers support application/ps instead of application/postscript
     * So:
     * if mimetype==application/postscript 
     *      && printer does not support mimetype application/postscript
     *      && printer supports mimetype application/ps
     * then 
     *      we change mimetype of docflavor to application/ps
     */
    private DocFlavor java2ipp(DocFlavor pDocFlavor) {
        DocFlavor ippDocFlavor = pDocFlavor;
        String mime = pDocFlavor.getMimeType();

        /*
         * SPECIAL processing application/ps
         */
        if (mime.equals("application/postscript")) {
            try {
                IppDocument document = new IppDocument("Qwerty",
                        "application/postscript", "");
                IppResponse response = printer.requestValidateJob("Qwerty",
                        document, null);
                if (response.getStatusCode() != 0) {
                    document = new IppDocument("Qwerty", "application/ps", "");
                    response = printer.requestValidateJob("Qwerty", document,
                            null);
                    if (response.getStatusCode() == 0) {
                        if (pDocFlavor instanceof DocFlavor.INPUT_STREAM) {
                            ippDocFlavor = new DocFlavor.INPUT_STREAM(
                                    "application/ps");
                        } else if (ippDocFlavor instanceof DocFlavor.BYTE_ARRAY) {
                            ippDocFlavor = new DocFlavor.BYTE_ARRAY(
                                    "application/ps");
                        } else if (ippDocFlavor instanceof DocFlavor.URL) {
                            ippDocFlavor = new DocFlavor.URL("application/ps");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ippDocFlavor;
    }

    /*
     * opposite to java2ipp() method
     */
    private DocFlavor ipp2java(DocFlavor ippDocFlavor) {
        DocFlavor pDocFlavor = ippDocFlavor;
        String mime = ippDocFlavor.getMimeType();

        /*
         * SPECIAL processing application/ps
         */
        if (mime.equals("application/ps")) {
            if (ippDocFlavor instanceof DocFlavor.INPUT_STREAM) {
                pDocFlavor = DocFlavor.INPUT_STREAM.POSTSCRIPT;
            } else if (ippDocFlavor instanceof DocFlavor.BYTE_ARRAY) {
                pDocFlavor = DocFlavor.BYTE_ARRAY.POSTSCRIPT;
            } else if (ippDocFlavor instanceof DocFlavor.URL) {
                pDocFlavor = DocFlavor.URL.POSTSCRIPT;
            }
        }

        return pDocFlavor;
    }

    /*
     * the method's name is saying all
     */
    private boolean isDocFlavorSupported(DocFlavor flavor) {
        if (flavor == null) {
            throw new NullPointerException("DocFlavor flavor is null");
        }

        DocFlavor clientFlavors[] = getSupportedDocFlavors();
        for (int i = 0; i < clientFlavors.length; i++) {
            if (clientFlavors[i].equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    /*
     * check permission to read/write to any file
     */
    private boolean canPrintToFile() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkPermission(new FilePermission("<<ALL FILES>>",
                        "read,write"));
                return true;
            } catch (SecurityException e) {
                return false;
            }
        }
        return true;
    }

    /*
     * just list of all doc flavors from specification
     * it is used in getSupportedDocFlavors() method
     */
    private static DocFlavor[] ALLDOCFLAVORS = { DocFlavor.BYTE_ARRAY.TEXT_PLAIN_HOST,
            DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8,
            DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16,
            DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16BE,
            DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16LE,
            DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII,
            DocFlavor.BYTE_ARRAY.TEXT_HTML_HOST,
            DocFlavor.BYTE_ARRAY.TEXT_HTML_UTF_8,
            DocFlavor.BYTE_ARRAY.TEXT_HTML_UTF_16,
            DocFlavor.BYTE_ARRAY.TEXT_HTML_UTF_16BE,
            DocFlavor.BYTE_ARRAY.TEXT_HTML_UTF_16LE,
            DocFlavor.BYTE_ARRAY.TEXT_HTML_US_ASCII,
            DocFlavor.BYTE_ARRAY.PDF,
            DocFlavor.BYTE_ARRAY.POSTSCRIPT,
            DocFlavor.BYTE_ARRAY.PCL,
            DocFlavor.BYTE_ARRAY.GIF,
            DocFlavor.BYTE_ARRAY.JPEG,
            DocFlavor.BYTE_ARRAY.PNG,
            DocFlavor.BYTE_ARRAY.AUTOSENSE,

            DocFlavor.INPUT_STREAM.TEXT_PLAIN_HOST,
            DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_8,
            DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16,
            DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16BE,
            DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16LE,
            DocFlavor.INPUT_STREAM.TEXT_PLAIN_US_ASCII,
            DocFlavor.INPUT_STREAM.TEXT_HTML_HOST,
            DocFlavor.INPUT_STREAM.TEXT_HTML_UTF_8,
            DocFlavor.INPUT_STREAM.TEXT_HTML_UTF_16,
            DocFlavor.INPUT_STREAM.TEXT_HTML_UTF_16BE,
            DocFlavor.INPUT_STREAM.TEXT_HTML_UTF_16LE,
            DocFlavor.INPUT_STREAM.TEXT_HTML_US_ASCII,
            DocFlavor.INPUT_STREAM.PDF,
            DocFlavor.INPUT_STREAM.POSTSCRIPT,
            DocFlavor.INPUT_STREAM.PCL,
            DocFlavor.INPUT_STREAM.GIF,
            DocFlavor.INPUT_STREAM.JPEG,
            DocFlavor.INPUT_STREAM.PNG,
            DocFlavor.INPUT_STREAM.AUTOSENSE,

            DocFlavor.URL.TEXT_PLAIN_HOST,
            DocFlavor.URL.TEXT_PLAIN_UTF_8,
            DocFlavor.URL.TEXT_PLAIN_UTF_16,
            DocFlavor.URL.TEXT_PLAIN_UTF_16BE,
            DocFlavor.URL.TEXT_PLAIN_UTF_16LE,
            DocFlavor.URL.TEXT_PLAIN_US_ASCII,
            DocFlavor.URL.TEXT_HTML_HOST,
            DocFlavor.URL.TEXT_HTML_UTF_8,
            DocFlavor.URL.TEXT_HTML_UTF_16,
            DocFlavor.URL.TEXT_HTML_UTF_16BE,
            DocFlavor.URL.TEXT_HTML_UTF_16LE,
            DocFlavor.URL.TEXT_HTML_US_ASCII,
            DocFlavor.URL.PDF,
            DocFlavor.URL.POSTSCRIPT,
            DocFlavor.URL.PCL,
            DocFlavor.URL.GIF,
            DocFlavor.URL.JPEG,
            DocFlavor.URL.PNG,
            DocFlavor.URL.AUTOSENSE,

            DocFlavor.CHAR_ARRAY.TEXT_PLAIN,
            DocFlavor.CHAR_ARRAY.TEXT_HTML,

            DocFlavor.STRING.TEXT_PLAIN,
            DocFlavor.STRING.TEXT_HTML,

            DocFlavor.READER.TEXT_PLAIN,
            DocFlavor.READER.TEXT_HTML,

            DocFlavor.SERVICE_FORMATTED.RENDERABLE_IMAGE,
            DocFlavor.SERVICE_FORMATTED.PRINTABLE,
            DocFlavor.SERVICE_FORMATTED.PAGEABLE,

            /*
             * Some printers accept "application/ps" instead of "application/postscript"
             * So, we have special processing for those DocFlavor
             * See comments with phrase:
             * SPECIAL processing application/ps   
             */
            new DocFlavor.INPUT_STREAM("application/ps"),
            new DocFlavor.URL("application/ps"),
            new DocFlavor.BYTE_ARRAY("application/ps") };

    public static int getVerbose() {
        return verbose;
    }

    public static void setVerbose(int newverbose) {
        verbose = newverbose;
        IppPrinter.setVerbose(verbose);
    }

    public static void doVerbose(String v) {
        System.out.println(v);
    }

    public static void doVerbose(int level, String v) {
        if (verbose >= level) {
            System.out.println(v);
        }
    }

}
