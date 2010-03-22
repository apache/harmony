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

package org.apache.harmony.x.print.ipp;

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Vector;

public class IppPrinter {
    protected static int verbose = 0;
    protected URI printeruri;
    protected IppAttributeGroupSet agroups; /* all attributes as in IppRequest */

    public static int getVerbose() {
        return verbose;
    }

    public static void setVerbose(int newverbose) {
        verbose = newverbose;
        IppClient.setVerbose(verbose);
    }

    public static void doVerbose(String v) {
        System.out.println(v);
    }

    public static void doVerbose(int level, String v) {
        if (verbose >= level) {
            System.out.println(v);
        }
    }

    public IppPrinter(URI uri) throws Exception {
        this.printeruri = uri;

        IppClient c = new IppClient(this.printeruri);
        IppRequest request;
        IppResponse response;
        IppAttributeGroup agroup;
        Vector va = new Vector();

        request = new IppRequest();
        request.setVersion(1, 1);
        request.setOperationId(IppOperation.GET_PRINTER_ATTRIBUTES);
        agroup = request.newGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
        agroup.add(new IppAttribute(IppAttribute.TAG_CHARSET,
                "attributes-charset", "utf-8"));
        agroup.add(new IppAttribute(IppAttribute.TAG_NATURAL_LANGUAGE,
                "attributes-natural-language", "en-us"));
        agroup.add(new IppAttribute(IppAttribute.TAG_URI, "printer-uri", uri
                .toString()));
        va.add("printer-uri-supported".getBytes());
        va.add("ipp-versions-supported".getBytes());
        agroup.add(new IppAttribute(IppAttribute.TAG_KEYWORD,
                "requested-attributes", va));

        response = c.request(request.getBytes());

        if (response.operationid >= 0x0000 && response.operationid <= 0x00FF) {
            if (response.ippversion[0] != 1 || response.ippversion[1] != 1) {
                throw new IppException("Can't use ipp protocol version "
                        + response.ippversion[0] + "." + response.ippversion[1]
                        + " of printer <" + uri.toString() + ">");
            }
        } else {
            throw new IppException("Can't use ipp printer <" + uri.toString()
                    + ">" + "\n" + response.toString());
        }
        this.agroups = new IppAttributeGroupSet();
    }

    public URI getURI() {
        return printeruri;
    }

    public IppResponse requestPrinterAttributes() throws Exception {
        return requestPrinterAttributes("all", null);
    }

    public IppResponse requestJobTemplateAttributes() throws Exception {
        return requestPrinterAttributes("job-template", null);
    }

    public IppResponse requestPrinterDescriptionAttributes() throws Exception {
        //return requestPrinterAttributes("printer-description", null);
        return requestPrinterAttributes("all", null);
    }

    public IppResponse requestPrinterAttributes(String what, String mimetype)
            throws Exception {
        IppClient c = new IppClient(printeruri);
        IppRequest request;
        IppResponse response;
        IppAttributeGroup agroup;
        Vector va = new Vector();

        request = new IppRequest();
        request.setVersion(1, 1);
        request.setOperationId(IppOperation.GET_PRINTER_ATTRIBUTES);
        agroup = request.newGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
        agroup.add(new IppAttribute(IppAttribute.TAG_CHARSET,
                "attributes-charset", "utf-8"));
        agroup.add(new IppAttribute(IppAttribute.TAG_NATURAL_LANGUAGE,
                "attributes-natural-language", "en-us"));
        agroup.add(new IppAttribute(IppAttribute.TAG_URI, "printer-uri",
                printeruri.toString()));
        va = new Vector();
        va.add(what.getBytes());
        agroup.add(new IppAttribute(IppAttribute.TAG_KEYWORD,
                "requested-attributes", va));
        agroup.add(new IppAttribute(IppAttribute.TAG_BOOLEAN,
                "ipp-attribute-fidelity", 0x01));
        if (mimetype != null && mimetype.length() > 0) {
            agroup.add(new IppAttribute(IppAttribute.TAG_MIMEMEDIATYPE,
                    "document-format", mimetype));
        }

        response = c.request(request.getBytes());

        if (response.operationid >= 0x0000 && response.operationid <= 0x00FF) {
            return response;
        }
        return null;
    }

    public boolean setAttribute(String aname, Object avalue) {
        return agroups.setAttribute(aname, avalue);
    }

    public IppResponse requestValidateJob(String jobname, IppDocument document,
            IppAttributeGroupSet agset) throws Exception {
        IppClient c = new IppClient(printeruri);
        IppRequest request;
        IppResponse response;
        IppAttributeGroup opa, joba;

        request = new IppRequest();
        request.setVersion(1, 1);
        request.setOperationId(IppOperation.VALIDATE_JOB);

        opa = request.newGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
        opa.add(new IppAttribute(IppAttribute.TAG_CHARSET,
                "attributes-charset", "utf-8"));
        opa.add(new IppAttribute(IppAttribute.TAG_NATURAL_LANGUAGE,
                "attributes-natural-language", "en-us"));
        opa.add(new IppAttribute(IppAttribute.TAG_URI, "printer-uri",
                printeruri.toString()));
        opa.add(new IppAttribute(IppAttribute.TAG_MIMEMEDIATYPE,
                "document-format", document.getFormat()));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "requesting-user-name", (String) AccessController
                        .doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                return System.getProperty("user.name");
                            }
                        })));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "job-name", jobname));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "document-name", document.getName()));
        opa.add(new IppAttribute(IppAttribute.TAG_BOOLEAN,
                "ipp-attribute-fidelity", 0x01));
        opa.set((IppAttributeGroup) document.getAgroups().get(
                new Integer(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES)));
        if (agset != null) {
            opa.set((IppAttributeGroup) agset.get(new Integer(
                    IppAttributeGroup.TAG_OPERATION_ATTRIBUTES)));
        }

        joba = request.newGroup(IppAttributeGroup.TAG_JOB_TEMPLATE_ATTRIBUTES);
        joba.set((IppAttributeGroup) document.getAgroups().get(
                new Integer(IppAttributeGroup.TAG_JOB_TEMPLATE_ATTRIBUTES)));
        if (agset != null) {
            joba.set((IppAttributeGroup) agset.get(new Integer(
                    IppAttributeGroup.TAG_JOB_TEMPLATE_ATTRIBUTES)));
        }

        response = c.request(request);

        return response;
    }

    /*
     * The method validate mimetypes.
     * Returned array is the same as parameter,
     * just with null in position of unsupported mimetypes.
     * This method created for performance issue
     */
    public String[] requestGetSupportedMimeTypes(String[] mimetypes)
            throws Exception {
        IppClient c = new IppClient(printeruri);
        IppRequest request;
        IppResponse response;
        IppAttributeGroup opa;

        request = new IppRequest();
        request.setVersion(1, 1);
        request.setOperationId(IppOperation.VALIDATE_JOB);

        opa = request.newGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
        opa.add(new IppAttribute(IppAttribute.TAG_CHARSET,
                "attributes-charset", "utf-8"));
        opa.add(new IppAttribute(IppAttribute.TAG_NATURAL_LANGUAGE,
                "attributes-natural-language", "en-us"));
        opa.add(new IppAttribute(IppAttribute.TAG_URI, "printer-uri",
                printeruri.toString()));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "requesting-user-name", (String) AccessController
                        .doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                return System.getProperty("user.name");
                            }
                        })));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "job-name", "validatemimetypes"));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "document-name", "validatemimetypes"));
        opa.add(new IppAttribute(IppAttribute.TAG_BOOLEAN,
                "ipp-attribute-fidelity", 0x01));
        opa.add(new IppAttribute(IppAttribute.TAG_MIMEMEDIATYPE,
                "document-format", ""));

        String[] validmimes = new String[mimetypes.length];
        int mimeattrindex = opa.findAttribute("document-format");
        for (int i = 0, ii = mimetypes.length; i < ii; i++) {
            opa.set(mimeattrindex, new IppAttribute(
                    IppAttribute.TAG_MIMEMEDIATYPE, "document-format",
                    mimetypes[i]));

            response = c.request(request);

            if (response.getStatusCode() == 0) {
                validmimes[i] = mimetypes[i];
            } else {
                validmimes[i] = null;
            }
        }

        return validmimes;
    }

    public IppResponse requestPrintJob(String jobname, IppDocument document,
            IppAttributeGroupSet agset) throws Exception {
        IppClient c = new IppClient(printeruri);
        IppRequest request;
        IppResponse response;
        IppAttributeGroup opa, joba;

        request = new IppRequest();
        request.setVersion(1, 1);
        request.setOperationId(IppOperation.PRINT_JOB);

        opa = request.newGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
        opa.add(new IppAttribute(IppAttribute.TAG_CHARSET,
                "attributes-charset", "utf-8"));
        opa.add(new IppAttribute(IppAttribute.TAG_NATURAL_LANGUAGE,
                "attributes-natural-language", "en-us"));
        opa.add(new IppAttribute(IppAttribute.TAG_URI, "printer-uri",
                printeruri.toString()));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "requesting-user-name", (String) AccessController
                        .doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                return System.getProperty("user.name");
                            }
                        })));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "job-name", jobname));
        opa.add(new IppAttribute(IppAttribute.TAG_NAMEWITHOUTLANGUAGE,
                "document-name", document.getName()));
        opa.add(new IppAttribute(IppAttribute.TAG_MIMEMEDIATYPE,
                "document-format", document.getFormat()));
        opa.add(new IppAttribute(IppAttribute.TAG_BOOLEAN,
                "ipp-attribute-fidelity", 0x01));
        opa.set((IppAttributeGroup) document.getAgroups().get(
                new Integer(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES)));

        joba = request.newGroup(IppAttributeGroup.TAG_JOB_TEMPLATE_ATTRIBUTES);
        joba.set((IppAttributeGroup) document.getAgroups().get(
                new Integer(IppAttributeGroup.TAG_JOB_TEMPLATE_ATTRIBUTES)));
        if (agset != null) {
            joba.set((IppAttributeGroup) agset.get(new Integer(
                    IppAttributeGroup.TAG_JOB_TEMPLATE_ATTRIBUTES)));
        }

        request.setDocument(document.getDocument());

        DoIppRequest job = new DoIppRequest(c, request);
        response = job.getResponse();
        if (job.exceptionOccured()) {
            throw new IppException(job.getException().getMessage());
        }

        return response;
    }

    private class DoIppRequest extends Thread {
        Object lock;
        IppClient client;
        IppRequest request;
        IppResponse response;
        Exception exception;
        boolean exceptionisnotnull;
        boolean requestisdone = false;

        public DoIppRequest(IppClient ippclient, IppRequest ipprequest) {
            this.client = ippclient;
            this.request = ipprequest;
            this.response = null;
            this.exception = null;
            this.exceptionisnotnull = false;
            this.requestisdone = false;
            lock = new Object();

            start();
        }

        public void run() {
            synchronized (lock) {
                try {
                    response = client.request(request);
                } catch (Exception e) {
                    if (verbose > 1) {
                        doVerbose(2,
                                "IppPrinter.java: run(): Exception thrown: "
                                        + e.toString());
                        e.printStackTrace();
                    }
                    response = null;
                    exception = e;
                    exceptionisnotnull = true;
                }

                requestisdone = true;
                lock.notifyAll();
            }
        }

        /**
         * Returns the deserialized object. This method will block until the
         * object is actually available.
         */
        IppResponse getResponse() {
            try {
                synchronized (lock) {
                    if (requestisdone) {
                        return response;
                    }
                    lock.wait();
                }
            } catch (InterruptedException ie) {
                if (verbose > 1) {
                    doVerbose(2,
                            "IppPrinter.java: getResponse(): Exception thrown: "
                                    + ie.toString());
                    ie.printStackTrace();
                }
                return null;
            }
            return response;
        }

        boolean exceptionOccured() {
            return exceptionisnotnull;
        }

        Exception getException() {
            return exception;
        }
    }
}
