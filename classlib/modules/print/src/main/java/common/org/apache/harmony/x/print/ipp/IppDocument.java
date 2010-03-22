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

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.harmony.x.print.ipp.util.IppMimeType;


public class IppDocument {
    static int doccount;

    protected String docname;
    protected String format; /* MIME type */
    protected Object document; /* String or DataInputStream */
    protected IppAttributeGroupSet agroups; /* all attributes as in IppRequest */

    public IppDocument(String name, String mime, Object data)
            throws IppException {
        if (data != null
                && mime != null
                && (data instanceof InputStream || data instanceof byte[]
                        || data instanceof char[] || data instanceof String
                        || data instanceof Reader || data instanceof URL)) {
            this.document = data;
            this.format = new IppMimeType(mime).getIppSpecificForm();

            if (name == null || name.equals("")) {
                this.docname = new String(
                        (String) AccessController.doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                return System.getProperty("user.name");
                            }
                        })
                                + "-" + doccount);
            } else {
                this.docname = name;
            }

            this.agroups = new IppAttributeGroupSet();
        } else {
            throw new IppException("Wrong argument(s) for IPP document");
        }
    }

    public IppAttributeGroupSet getAgroups() {
        return agroups;
    }

    public void setAgroups(IppAttributeGroupSet attrgroups) {
        this.agroups = attrgroups;
    }

    public String getName() {
        return docname;
    }

    public void setName(String name) {
        this.docname = name;
    }

    public Object getDocument() {
        return document;
    }

    public void setDocument(Object data) throws IppException {
        if (data instanceof InputStream || data instanceof byte[]
                || data instanceof char[] || data instanceof String
                || data instanceof Reader || data instanceof URL) {
            this.document = data;
        } else {
            throw new IppException("Wrong type for IPP document");
        }
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String mime) {
        this.format = new IppMimeType(mime).getIppSpecificForm();
    }

    public boolean setAttribute(String aname, int avalue) {
        return setAttribute(aname, new Integer(avalue));
    }

    public boolean setAttribute(String aname, String avalue) {
        return setAttribute(aname, (Object) avalue);
    }

    public boolean setAttribute(String aname, Object avalue) {
        return agroups.setAttribute(aname, avalue);
    }

}