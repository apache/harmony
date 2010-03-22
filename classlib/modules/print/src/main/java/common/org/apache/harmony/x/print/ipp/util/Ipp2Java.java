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

package org.apache.harmony.x.print.ipp.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import javax.print.attribute.Attribute;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Compression;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.Finishings;
import javax.print.attribute.standard.JobHoldUntil;
import javax.print.attribute.standard.JobImpressions;
import javax.print.attribute.standard.JobImpressionsCompleted;
import javax.print.attribute.standard.JobImpressionsSupported;
import javax.print.attribute.standard.JobKOctets;
import javax.print.attribute.standard.JobKOctetsProcessed;
import javax.print.attribute.standard.JobKOctetsSupported;
import javax.print.attribute.standard.JobMediaSheets;
import javax.print.attribute.standard.JobMediaSheetsCompleted;
import javax.print.attribute.standard.JobMediaSheetsSupported;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobPriority;
import javax.print.attribute.standard.JobPrioritySupported;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.MultipleDocumentHandling;
import javax.print.attribute.standard.NumberUp;
import javax.print.attribute.standard.NumberUpSupported;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PDLOverrideSupported;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PagesPerMinute;
import javax.print.attribute.standard.PagesPerMinuteColor;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterInfo;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterLocation;
import javax.print.attribute.standard.PrinterMakeAndModel;
import javax.print.attribute.standard.PrinterMessageFromOperator;
import javax.print.attribute.standard.PrinterMoreInfo;
import javax.print.attribute.standard.PrinterMoreInfoManufacturer;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.PrinterStateReasons;
import javax.print.attribute.standard.PrinterURI;
import javax.print.attribute.standard.QueuedJobCount;
import javax.print.attribute.standard.ReferenceUriSchemesSupported;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.Severity;
import javax.print.attribute.standard.Sides;

import org.apache.harmony.x.print.attributes.PPDMediaSizeName;
import org.apache.harmony.x.print.ipp.IppAttribute;
import org.apache.harmony.x.print.ipp.IppDefs;


/** 
 * This class make dependencies between Ipp attributes (RFC 2910) and corresponding classes
 */

public class Ipp2Java {
    static Object[][] ipp2attr = { { "job-priority",
            new JobPriority(1).getCategory() },
            { "job-priority-default", new JobPriority(1).getCategory() },
            { "job-priority-supported",
                    new JobPrioritySupported(1).getCategory() },
            { "job-hold-until", new JobHoldUntil(new Date()).getCategory() },
            { "job-hold-until-default",
                    new JobHoldUntil(new Date()).getCategory() },
            { "job-hold-until-supported",
                    new JobHoldUntil(new Date()).getCategory() },
            { "job-sheets", JobSheets.NONE.getCategory() },
            { "job-sheets-default", JobSheets.NONE.getCategory() },
            { "job-sheets-supported", JobSheets.NONE.getCategory() },
            { "multiple-document-handling",
                    MultipleDocumentHandling.SINGLE_DOCUMENT.getCategory() },
            { "multiple-document-handling-default",
                    MultipleDocumentHandling.SINGLE_DOCUMENT.getCategory() },
            { "multiple-document-handling-supported",
                    MultipleDocumentHandling.SINGLE_DOCUMENT.getCategory() },
            { "copies", new Copies(1).getCategory() },
            { "copies-default", new Copies(1).getCategory() },
            { "copies-supported", new CopiesSupported(1).getCategory() },
            { "finishings", Finishings.NONE.getCategory() },
            { "finishings-default", Finishings.NONE.getCategory() },
            { "finishings-supported", Finishings.NONE.getCategory() },
            { "page-ranges", new PageRanges(1).getCategory() },
            // NO in RFC3380
            // {"page-ranges-default", new PageRanges(1) },
            { "page-ranges-supported", new PageRanges(1).getCategory() },
            { "sides", Sides.ONE_SIDED.getCategory() },
            { "sides-default", Sides.ONE_SIDED.getCategory() },
            { "sides-supported", Sides.ONE_SIDED.getCategory() },
            { "number-up", new NumberUp(1).getCategory() },
            { "number-up-default", new NumberUp(1).getCategory() },
            { "number-up-supported", new NumberUpSupported(1).getCategory() },
            { "orientation-requested",
                    OrientationRequested.PORTRAIT.getCategory() },
            { "orientation-requested-default",
                    OrientationRequested.PORTRAIT.getCategory() },
            { "orientation-requested-supported",
                    OrientationRequested.PORTRAIT.getCategory() },

            { "media", Media.class },
            { "media-default", Media.class },
            { "media-supported", Media.class },

            { "printer-resolution",
                    new PrinterResolution(1, 1, 1).getCategory() },
            { "printer-resolution-default",
                    new PrinterResolution(1, 1, 1).getCategory() },
            { "printer-resolution-supported",
                    new PrinterResolution(1, 1, 1).getCategory() },
            { "printer-uri-supported",
                    new PrinterURI(URI.create("http://localhost:631")).getCategory() },
            { "printer-name",
                    new PrinterName("ipp-printer", Locale.US).getCategory() },
            { "printer-location",
                    new PrinterLocation("Earth", Locale.US).getCategory() },
            { "printer-make-and-model",
                    new PrinterMakeAndModel("ipp-printer", Locale.US).getCategory() },
            { "printer-message-from-operator",
                    new PrinterMessageFromOperator("ipp-printer", Locale.US).getCategory() },
            { "printer-more-info",
                    new PrinterMoreInfo(URI.create("http://localhost")).getCategory() },
            { "printer-more-info-manufacturer",
                    new PrinterMoreInfoManufacturer(
                            URI.create("http://localhost")).getCategory() },
            { "printer-info", new PrinterInfo("info", Locale.US).getCategory() },
            { "print-quality", PrintQuality.NORMAL.getCategory() },
            { "print-quality-default", PrintQuality.NORMAL.getCategory() },
            { "print-quality-supported", PrintQuality.NORMAL.getCategory() },
            { "color-supported", ColorSupported.SUPPORTED.getCategory() },
            { "compression", Compression.NONE.getCategory() },
            { "compression-supported", Compression.NONE.getCategory() },
            { "job-k-octets", new JobKOctets(1).getCategory() },
            { "job-k-octets-supported",
                    new JobKOctetsSupported(1, 2).getCategory() },
            { "job-k-octets-processed ",
                    new JobKOctetsProcessed(1).getCategory() },
            { "job-impressions", new JobImpressions(1).getCategory() },
            { "job-impressions-supported",
                    new JobImpressionsSupported(1, 2).getCategory() },
            { "job-impressions-completed",
                    new JobImpressionsCompleted(1).getCategory() },
            { "job-media-sheets", new JobMediaSheets(1).getCategory() },
            { "job-media-sheets-supported",
                    new JobMediaSheetsSupported(1, 2).getCategory() },
            { "job-media-completed",
                    new JobMediaSheetsCompleted(1).getCategory() },
            { "pdl-override-supported",
                    PDLOverrideSupported.ATTEMPTED.getCategory() },
            { "reference-uri-schemes-supported",
                    ReferenceUriSchemesSupported.HTTP.getCategory() },
            { "job-name", new JobName("jobname", Locale.US).getCategory() },
            { "color-supported", ColorSupported.SUPPORTED.getCategory() },
            { "printer-state", PrinterState.UNKNOWN.getCategory() },
            { "printer-state-reasons", new PrinterStateReasons().getCategory() },
            { "printer-is-accepting-jobs",
                    PrinterIsAcceptingJobs.ACCEPTING_JOBS.getCategory() },
            { "queued-job-count", new QueuedJobCount(1).getCategory() },
            { "requesting-user-name",
                    new RequestingUserName("username", Locale.US).getCategory() },
            { "job-name", new JobName("jobname", Locale.US).getCategory() },
            { "document-name",
                    new DocumentName("docname", Locale.US).getCategory() },
            { "color-supported", ColorSupported.NOT_SUPPORTED.getCategory() },
            { "pdl-override-supported",
                    PDLOverrideSupported.ATTEMPTED.getCategory() },
            { "pages-per-minute", new PagesPerMinute(1).getCategory() },
            { "pages-per-minute-color",
                    new PagesPerMinuteColor(1).getCategory() } };

    public static Class getClassByIppAttributeName(String attribute) {
        for (int i = 0, ii = ipp2attr.length; i < ii; i++) {
            if (attribute.equals(ipp2attr[i][0])) {
                return (Class) ipp2attr[i][1];
            }
        }
        return null;
    }

    public static String getIppAttributeNameByClass(Class claz) {
        return getIppAttributeNameByClass(claz, -1);
    }

    /**
     * if ippsfx==-1 -> first found <br>
     * if ippsfx==0 -> simple attr <br>
     * if ippsfx==1 -> default attr <br>
     * if ippsfx==2 -> supported attr <br>
     */
    public static String getIppAttributeNameByClass(Class claz, int ippsfx) {
        String aname = null;

        for (int i = 0, ii = ipp2attr.length; i < ii; i++) {
            if (((Class) ipp2attr[i][1]).isAssignableFrom(claz)) {
                aname = (String) ipp2attr[i][0];
                if (ippsfx == -1) {
                    return aname;
                } else if (ippsfx == 0 && !aname.endsWith("-default")
                        && !aname.endsWith("-supported")) {
                    return aname;
                } else if (ippsfx == 1 && aname.endsWith("-default")) {
                    return aname;
                } else if (ippsfx == 2 && aname.endsWith("-supported")) {
                    return aname;
                }
            }
        }
        return null;
    }

    public static Object[] getJavaByIpp(IppAttribute attr) {
        Vector attrx = new Vector();
        Class claz;
        String aname;
        byte atag;
        Vector avalue;
        Attribute a = null;

        aname = new String(attr.getName());
        claz = getClassByIppAttributeName(aname);
        atag = attr.getTag();
        avalue = attr.getValue();

        if (aname.equals("printer-state")
                || aname.equals("printer-is-accepting-jobs")
                || aname.equals("finishings")
                || aname.equals("finishings-default")
                || aname.equals("finishings-supported")
                || aname.equals("orientation-requested")
                || aname.equals("orientation-requested-default")
                || aname.equals("orientation-requested-supported")
                || aname.equals("color-supported")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = (Attribute) IppAttributeUtils.getObject(claz,
                        ((Integer) avalue.get(i)).intValue());
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("multiple-document-handling")
                || aname.equals("multiple-document-handling-default")
                || aname.equals("multiple-document-handling-supported")
                || aname.equals("compression")
                || aname.equals("compression-default")
                || aname.equals("compression-supported")
                || aname.equals("job-sheets")
                || aname.equals("job-sheets-default")
                || aname.equals("job-sheets-supported")
                || aname.equals("pdl-override-supported")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                Object o = avalue.get(i);
                a = (Attribute) IppAttributeUtils.getObject(claz, new String(
                        (byte[]) o));
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("printer-info")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                // TODO need to set locale corresponded to 
                // attributes-charset/attributes-natural-language
                a = new PrinterInfo(new String((byte[]) avalue.get(i)),
                        Locale.US);
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("sides") || aname.equals("sides-default")
                || aname.equals("sides-supported")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = (Attribute) IppAttributeUtils.getObject(claz, new String(
                        (byte[]) avalue.get(i)));
                if (a != null) {
                    attrx.add(a);
                }
            }
            /* SPECIAL case for Sides* attributes
             * 
             * CUPS returns "one", "two-long-edge", "two-short-edge"
             * instead of "one-sided", "two-sided-long-edge", "two-sided-short-edge"
             */
            if (attrx.isEmpty() && avalue.size() > 0) {
                for (int i = 0, ii = avalue.size(); i < ii; i++) {
                    String sz = new String((byte[]) avalue.get(i));
                    if (sz.indexOf("sided") == -1) {
                        int ind = sz.indexOf("-");
                        if (ind == -1) {
                            ind = sz.length();
                        }
                        sz = sz.substring(0, ind) + "-sided"
                                + sz.substring(ind);
                    }
                    a = (Attribute) IppAttributeUtils.getObject(claz, sz);
                    if (a != null) {
                        attrx.add(a);
                    }
                }
            }
        } else if (aname.equals("pages-per-minute")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = new PagesPerMinute(((Integer) avalue.get(i)).intValue());
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("pages-per-minute-color")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = new PagesPerMinuteColor(
                        ((Integer) avalue.get(i)).intValue());
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("job-priority")
                || aname.equals("job-priority-default")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = new JobPriority(((Integer) avalue.get(i)).intValue());
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("job-priority-supported")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = new JobPrioritySupported(
                        ((Integer) avalue.get(i)).intValue());
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("queued-job-count")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = new QueuedJobCount(((Integer) avalue.get(i)).intValue());
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("printer-state-reason")
                || aname.equals("printer-state-reasons")) {
            PrinterStateReasons rs = new PrinterStateReasons();
            String r;
            Severity s;
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                r = new String((byte[]) avalue.get(i));
                if (r.endsWith("-error")) {
                    r = r.substring(0, r.indexOf("-error"));
                    s = Severity.ERROR;
                } else if (r.endsWith("-warning")) {
                    r = r.substring(0, r.indexOf("-warning"));
                    s = Severity.WARNING;
                } else if (r.endsWith("-report")) {
                    r = r.substring(0, r.indexOf("-report"));
                    s = Severity.WARNING;
                } else {
                    s = Severity.ERROR;
                }
                a = (Attribute) IppAttributeUtils.getObject(
                        PrinterStateReason.class, r);
                if (a != null) {
                    rs.put((PrinterStateReason)a, s);
                }
            }
            if (rs.size() > 0) {
                attrx.add(rs);
            }
        } else if (aname.equals("copies") || aname.equals("copies-default")
                || aname.equals("copies-supported")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                if (atag == IppAttribute.TAG_RANGEOFINTEGER) {
                    DataInputStream di = new DataInputStream(
                            new ByteArrayInputStream((byte[]) avalue.get(i)));
                    try {
                        a = new CopiesSupported(di.readInt(), di.readInt());
                    } catch (IOException e) {
                        // IGNORE exception
                        a = null;
                        e.printStackTrace();
                    }
                } else if (atag == IppAttribute.TAG_INTEGER) {
                    a = new Copies(((Integer) avalue.get(i)).intValue());
                }
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("page-ranges-supported")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = new PageRanges(((Integer) avalue.get(i)).intValue());
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("number-up")
                || aname.equals("number-up-default")
                || aname.equals("number-up-supported")) {
            Vector v = new Vector();
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                if (atag == IppAttribute.TAG_INTEGER) {
                    v.add(new int[] { ((Integer) avalue.get(i)).intValue() });
                } else if (atag == IppAttribute.TAG_RANGEOFINTEGER) {
                    DataInputStream di = new DataInputStream(
                            new ByteArrayInputStream((byte[]) avalue.get(i)));
                    try {
                        v.add(new int[] { di.readInt(), di.readInt() });
                    } catch (IOException e) {
                        // IGNORE exception
                        e.printStackTrace();
                    }
                }
            }
            int[][] x = new int[v.size()][];
            for (int j = 0, jj = v.size(); j < jj; j++) {
                x[j] = (int[]) v.get(j);
            }
            a = new NumberUpSupported(x);
            if (a != null) {
                attrx.add(a);
            }
        } else if (aname.equals("job-hold-until")
                || aname.equals("job-hold-until-default")
                || aname.equals("job-hold-until-supported")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = new JobHoldUntil(new Date(0L));
                if (a != null) {
                    attrx.add(a);
                    break;
                }
            }
        } else if (aname.equals("media-supported")
                || aname.equals("media-default") || aname.equals("media")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                a = (Attribute) IppAttributeUtils.getObject(
                        MediaSizeName.class, new String((byte[]) avalue.get(i)));
                if (a != null) {
                    attrx.add(a);
                }
                a = (Attribute) IppAttributeUtils.getObject(MediaTray.class,
                        new String((byte[]) avalue.get(i)));
                if (a != null) {
                    attrx.add(a);
                }
                a = (Attribute) IppAttributeUtils.getObject(MediaName.class,
                        new String((byte[]) avalue.get(i)));
                if (a != null) {
                    attrx.add(a);
                }
                a = (Attribute) IppAttributeUtils.getObject(
                        PPDMediaSizeName.class, new String(
                                (byte[]) avalue.get(i)));
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("printer-uri-supported")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                try {
                    a = new PrinterURI(new URI(new String(
                            (byte[]) avalue.get(i))));
                    if (a != null) {
                        attrx.add(a);
                    }
                } catch (URISyntaxException e) {
                    // IGNORE exception for bad URI
                    e.printStackTrace();
                }
            }
        } else if (aname.equals("printer-name")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                // TODO need to set locale corresponded to 
                // attributes-charset/attributes-natural-language
                a = new PrinterName(new String((byte[]) avalue.get(i)),
                        Locale.US);
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("printer-make-and-model")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                // TODO need to set locale corresponded to 
                // attributes-charset/attributes-natural-language
                a = new PrinterMakeAndModel(new String((byte[]) avalue.get(i)),
                        Locale.US);
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("printer-location")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                // TODO need to set locale corresponded to 
                // attributes-charset/attributes-natural-language
                a = new PrinterLocation(new String((byte[]) avalue.get(i)),
                        Locale.US);
                if (a != null) {
                    attrx.add(a);
                }
            }
        } else if (aname.equals("printer-more-info")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                try {
                    a = new PrinterMoreInfo(new URI(new String(
                            (byte[]) avalue.get(i))));
                    if (a != null) {
                        attrx.add(a);
                    }
                } catch (URISyntaxException e) {
                    // IGNORE exception for bad URI
                    e.printStackTrace();
                }
            }
        } else if (aname.equals("printer-more-info-manufacturer")) {
            for (int i = 0, ii = avalue.size(); i < ii; i++) {
                try {
                    a = new PrinterMoreInfoManufacturer(new URI(new String(
                            (byte[]) avalue.get(i))));
                    if (a != null) {
                        attrx.add(a);
                    }
                } catch (URISyntaxException e) {
                    // IGNORE exception for bad URI
                    e.printStackTrace();
                }
            }
        } else if (aname.equals("printer-up-time")
                || aname.equals("printer-state-time")
                || aname.equals("printer-current-time")
                || aname.equals("uri-security-supported")
                || aname.equals("operations-supported")
                || aname.equals("charset-configured")
                || aname.equals("charset-supported")
                || aname.equals("natural-language-configured")
                || aname.equals("generated-natural-language-supported")
                || aname.equals("document-format-default")
                || aname.equals("document-format-supported")
                || aname.equals("printer-state-message")
                || aname.equals("printer-state-message-default")
                || aname.equals("printer-state-message-supported")
                || aname.equals("ipp-versions-supported")
                || aname.equals("uri-authentication-supported")
                || aname.equals("job-quota-period")
                || aname.equals("job-k-limit")
                || aname.equals("job-page-limit") || aname.equals("device-uri")
                || aname.equals("printer-type")
                || aname.equals("multiple-document-jobs-supported")
                || aname.equals("multiple-operation-time-out")
                || aname.equals("printer-state-history")
                || aname.equals("media-ready")
                || aname.equals("output-bin-supported")) {
            // IGNORE - no such java-attributes
        } else {
            // TODO need to implement other attributes
            System.err.println("Not yet implemented: " + aname);
        }

        return attrx.toArray();
    }

    public static IppAttribute getIppByJava(Attribute attr) {
        IppAttribute a = null;
        String aname = getIppAttributeNameByClass(attr.getClass());
        byte vtag;

        if (aname != null) {
            vtag = IppDefs.getAttributeVtag(aname);

            if (vtag != -1) {
                Object o = IppAttributeUtils.getIppValue(attr, vtag);
                if (o != null) {
                    if (o instanceof Integer) {
                        a = new IppAttribute(vtag, aname,
                                ((Integer) o).intValue());
                    } else if (o instanceof String) {
                        a = new IppAttribute(vtag, aname, (String) o);
                    } else if (o instanceof byte[]) {
                        a = new IppAttribute(vtag, aname, (byte[]) o);
                    } else if (o instanceof Vector) {
                        a = new IppAttribute(vtag, aname, (Vector) o);
                    }
                }
            }
        }

        return a;
    }

}
