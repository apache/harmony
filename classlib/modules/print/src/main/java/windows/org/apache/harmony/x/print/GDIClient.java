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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PagesPerMinute;
import javax.print.attribute.standard.PagesPerMinuteColor;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterLocation;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.QueuedJobCount;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;

import org.apache.harmony.x.print.PrintClient;

/*
 * GDIClient is a print client based on windows GDI inteface.
 * See description of PrintClient interface for more information.
 */
class GDIClient implements PrintClient {

    private String serviceName = null;
    private boolean isPrinting = false;

    private static final int MAX_BUFFER_SIZE = 10240;

    private static final int ATTRIBUTES_ARRAY_SIZE = 8;
    private static final int COPIES_INDEX = 0;
    private static final int SIDES_INDEX = 1;
    private static final int PAPER_ID_INDEX = 2;
    private static final int COLLATE_INDEX = 3;
    private static final int CHROMATICITY_INDEX = 4;
    private static final int ORIENTATION_INDEX = 5;
    private static final int XRESOLUTION_INDEX = 6;
    private static final int YRESOLUTION_INDEX = 7;

    private static final int NOT_STANDARD_MEDIA = 1000;
    private HashMap medias = new HashMap();
    
    GDIClient(String serviceName) {
        this.serviceName = serviceName;
    }
    
    /*
     * @see org.apache.harmony.x.print.PrintClient#getSupportedDocFlavors()
     */
    public DocFlavor[] getSupportedDocFlavors() {
        if (checkPostScript(serviceName)) {
            return new DocFlavor[] {
                new DocFlavor.INPUT_STREAM("INTERNAL/postscript"),
                DocFlavor.INPUT_STREAM.POSTSCRIPT,
                DocFlavor.BYTE_ARRAY.POSTSCRIPT,
                DocFlavor.URL.POSTSCRIPT,
                DocFlavor.INPUT_STREAM.AUTOSENSE,
                DocFlavor.BYTE_ARRAY.AUTOSENSE,
                DocFlavor.URL.AUTOSENSE
            };
        } else {
            return new DocFlavor[] {
                new DocFlavor.INPUT_STREAM("INTERNAL/postscript"),
                DocFlavor.INPUT_STREAM.AUTOSENSE,
                DocFlavor.BYTE_ARRAY.AUTOSENSE,
                DocFlavor.URL.AUTOSENSE
            };
        }
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#getAttributes()
     */
    public PrintServiceAttributeSet getAttributes() {
        PrintServiceAttributeSet attributes =
            new HashPrintServiceAttributeSet();
        attributes.add(new PrinterName(serviceName, Locale.getDefault()));
        if (getColorSupported(serviceName)) {
            attributes.add(ColorSupported.SUPPORTED);
            int colorPPM = getPagesPerMinuteColor(serviceName);
            if (colorPPM > 0) {
                attributes.add(new PagesPerMinuteColor(colorPPM));
            }
        } else {
            attributes.add(ColorSupported.NOT_SUPPORTED);
        }
        
        int pagesPerMinute = getPagesPerMinute(serviceName);
        if (pagesPerMinute > 0) {
            attributes.add(new PagesPerMinute(pagesPerMinute));
        }
        
        String printerLocation = getPrinterLocation(serviceName);
        if (printerLocation != null) {
            attributes.add(new PrinterLocation(printerLocation,
                    Locale.getDefault()));
        }
        
        int acceptingJobs = getPrinterIsAcceptingJobs(serviceName);
        if (acceptingJobs == 0) {
            attributes.add(PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS);
        } else if (acceptingJobs == 1) {
            attributes.add(PrinterIsAcceptingJobs.ACCEPTING_JOBS);
        }
        
        int jobCount = getQueuedJobCount(serviceName);
        if (jobCount >= 0) {
            attributes.add(new QueuedJobCount(jobCount));
        }
        return attributes;
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#getSupportedAttributeCategories()
     */
    public Class[] getSupportedAttributeCategories() {
        ArrayList supportedCategories = new ArrayList();
        if (getCopiesSupported(serviceName) >= 1) {
            supportedCategories.add(Copies.class);
        }
        if (getSidesSupported(serviceName)) {
            supportedCategories.add(Sides.class);
        }
        if (getSupportedMediaSizeNames() != null) {
            supportedCategories.add(Media.class);
        }
        if (getResolutionsSupported(serviceName) != null) {
            supportedCategories.add(PrinterResolution.class);
        }
        if (getOrientationSupported(serviceName)) {
            supportedCategories.add(OrientationRequested.class);
        }
        if (getCollateSupported(serviceName)) {
            supportedCategories.add(SheetCollate.class);
        }
        supportedCategories.add(Chromaticity.class);
        supportedCategories.add(JobName.class);
        supportedCategories.add(RequestingUserName.class);
        supportedCategories.add(Destination.class);
        
        Class[] categories = new Class[supportedCategories.size()];
        supportedCategories.toArray(categories);
        return categories;
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#getDefaultAttributeValue(
     * java.lang.Class)
     */
    public Object getDefaultAttributeValue(Class category) {
        if (category.equals(JobName.class)) {
            return new JobName("Java GDI client print job", Locale.US);
        } else if (category.equals(RequestingUserName.class)) {
            return new RequestingUserName(System.getProperty("user.name"),
                    Locale.US);
        } else if (category.equals(Destination.class)) {
            File file = new File(System.getProperty("user.dir") +
                    File.separator + "output.prn");
            return new Destination(file.toURI());
        } else if (category.equals(SheetCollate.class)) {
            return SheetCollate.COLLATED;
        } else if (category.equals(Copies.class)) {
            return new Copies(1);
        }
        return null;
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#isAttributeValueSupported(
     * javax.print.attribute.Attribute, javax.print.DocFlavor,
     * javax.print.attribute.AttributeSet)
     */
    public boolean isAttributeValueSupported(Attribute attribute,
            DocFlavor flavor, AttributeSet attributes) {
        Class category = attribute.getCategory();
        if (category.equals(JobName.class) ||
            category.equals(RequestingUserName.class)) {
            return true;
        } else  if (category.equals(Destination.class)) {
            Destination destination = (Destination)attribute;
            if (destination.getURI().getScheme().equals("file")) {
                return true;
            }
        }
        if (flavor != null) {
            if (flavor.equals(DocFlavor.INPUT_STREAM.AUTOSENSE) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.AUTOSENSE) ||
                flavor.equals(DocFlavor.URL.AUTOSENSE) ||
                flavor.equals(DocFlavor.INPUT_STREAM.POSTSCRIPT) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.POSTSCRIPT) ||
                flavor.equals(DocFlavor.URL.POSTSCRIPT)) {
                return false;
            }
        }
        if (category.equals(Copies.class)) {
            CopiesSupported copies =
                (CopiesSupported)getSupportedAttributeValues(category, flavor,
                                                             attributes);
            int value = ((Copies)attribute).getValue();
            if (copies != null && copies.contains(value)) {
                return true;
            }
        } else if (category.equals(Sides.class)) {
            Sides[] sides = (Sides[])getSupportedAttributeValues(category,
                    flavor, attributes);
            if (sides != null) {
                for (int i = 0; i < sides.length; i++) {
                    if (sides[i].equals(attribute)) {
                        return true;
                    }
                }
            }
        } else if (category.equals(Media.class)) {
            Media[] medias = (Media[])getSupportedAttributeValues(category,
                    flavor, attributes);
            if (medias != null) {
                for (int i = 0; i < medias.length; i++) {
                    if (medias[i].equals(attribute)) {
                        return true;
                    }
                }
            }
        } else if (category.equals(Chromaticity.class)) {
            if (attribute.equals(Chromaticity.COLOR)) {
                if (!getColorSupported(serviceName)) {
                    return false;
                }
            }
            return true;
        } else if (category.equals(OrientationRequested.class)) {
            if (getOrientationSupported(serviceName)) {
                if (attribute.equals(OrientationRequested.PORTRAIT) ||
                    attribute.equals(OrientationRequested.LANDSCAPE))
                return true;
            }
        } else if (category.equals(PrinterResolution.class)) {
            int[] resolutions = getResolutionsSupported(serviceName);
            if (resolutions != null && resolutions.length > 1) {
                PrinterResolution resolution = (PrinterResolution)attribute;
                int xres = resolution.getCrossFeedResolution(
                        PrinterResolution.DPI);
                int yres = resolution.getFeedResolution(PrinterResolution.DPI);
                for (int i = 0; i < resolutions.length / 2; i++) {
                    if (xres == resolutions[i * 2] &&
                        yres == resolutions[i * 2 + 1]) {
                        return true;
                    }
                }
            }
        } else if (category.equals(SheetCollate.class)) {
            if (getCollateSupported(serviceName)) {
                if (attribute == SheetCollate.COLLATED ||
                    attribute == SheetCollate.UNCOLLATED) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * @see org.apache.harmony.x.print.PrintClient#getSupportedAttributeValues(
     * java.lang.Class, javax.print.DocFlavor,
     * javax.print.attribute.AttributeSet)
     */
    public Object getSupportedAttributeValues(Class category, DocFlavor flavor,
            AttributeSet attributes) {
        if (flavor != null &&
            (flavor.equals(DocFlavor.INPUT_STREAM.AUTOSENSE) ||
             flavor.equals(DocFlavor.BYTE_ARRAY.AUTOSENSE) ||
             flavor.equals(DocFlavor.URL.AUTOSENSE) ||
             flavor.equals(DocFlavor.INPUT_STREAM.POSTSCRIPT) ||
             flavor.equals(DocFlavor.BYTE_ARRAY.POSTSCRIPT) ||
             flavor.equals(DocFlavor.URL.POSTSCRIPT))) {
            return null;
        }
        if (category.equals(Copies.class)) {
            int copies = getCopiesSupported(serviceName);
            if (copies == 1) {
                return new CopiesSupported(1);
            } else if (copies > 1) {
                return new CopiesSupported(1, copies);
            }
        } else if (category.equals(Sides.class)) {
            if (getSidesSupported(serviceName)) {
                return new Sides[] {Sides.ONE_SIDED,
                                    Sides.TWO_SIDED_SHORT_EDGE,
                                    Sides.TWO_SIDED_LONG_EDGE,
                                    Sides.DUPLEX,
                                    Sides.TUMBLE};
            }
        } else if (category.equals(Media.class)) {
            return getSupportedMediaSizeNames();
        } else if (category.equals(MediaSizeName.class)) {
            return getSupportedMediaSizeNames();
        } else if (category.equals(Chromaticity.class)) {
            if (getColorSupported(serviceName)) {
                return new Chromaticity[] { Chromaticity.MONOCHROME,
                                            Chromaticity.COLOR };
            } else {
                return new Chromaticity[] { Chromaticity.MONOCHROME };
            }
        } else if (category.equals(OrientationRequested.class)) {
            if (getOrientationSupported(serviceName)) {
                return new OrientationRequested[]
                    { OrientationRequested.PORTRAIT,
                      OrientationRequested.LANDSCAPE };
            }
        } else if (category.equals(PrinterResolution.class)) {
            int[] resolutions = getResolutionsSupported(serviceName);
            if (resolutions != null && resolutions.length > 1) {
                PrinterResolution[] res =
                    new PrinterResolution[resolutions.length / 2];
                for (int i = 0; i < resolutions.length / 2; i++) {
                    res[i] = new PrinterResolution(resolutions[i * 2],
                            resolutions[i * 2 + 1], PrinterResolution.DPI);
                }
                return res;
            }
        } else if (category.equals(SheetCollate.class)) {
            if (getCollateSupported(serviceName)) {
                return new SheetCollate[] { SheetCollate.COLLATED,
                                            SheetCollate.UNCOLLATED };
            }
        }
        return null;
    }

    private MediaSizeName[] getSupportedMediaSizeNames() {
        int[][] mediaSizes = getMediaSizesSupported(serviceName);
        if (mediaSizes == null || mediaSizes.length == 0) {
            return null;
        }
        int[] ids = getMediaIDs(serviceName);
        String[] names = getMediaNames(serviceName);
        if (ids == null || names == null || ids.length != mediaSizes.length ||
                names.length != mediaSizes.length) {
            /*
             * Something wrong. Don't know exactly what happend,
             * but printer driver returned different ammount of
             * sizes, IDs and names of media.
             */
            return null;
        }
        MediaSizeName[] mediaNames =  GDIMediaName.getStandardMediaSizeNames();
        if (mediaNames == null || mediaNames.length == 0) {
            return null;
        }
        ArrayList result = new ArrayList();
        medias.clear();
        for (int i = 0; i < mediaSizes.length; i++) {
            float sizeX = mediaSizes[i][0] / 10.0f;
            float sizeY = mediaSizes[i][1] / 10.0f;
            if (sizeX > 0 && sizeY > 0) {
                boolean standardFound = false;
                for (int j = 0; j < mediaNames.length; j++) {
                    MediaSize mediaSize =
                        MediaSize.getMediaSizeForName(mediaNames[j]);
                    if (mediaSize != null &&
                        Math.abs(sizeX - mediaSize.getX(MediaSize.MM)) < 1 &&
                        Math.abs(sizeY - mediaSize.getY(MediaSize.MM)) < 1) {
                        standardFound = true;
                        if (!result.contains(mediaNames[j])) {
                            result.add(mediaNames[j]);
                            medias.put(mediaNames[j], new Integer(ids[i]));
                            /* TODO:
                             * Do we have to do this break? If not,
                             * all names of one size returned.
                             */
                            break;
                        }
                    }
                }
                if (!standardFound) {
                    GDIMediaName name = new GDIMediaName(names[i],
                            NOT_STANDARD_MEDIA + i);
                    MediaSizeName sname = MediaSize.findMedia(sizeX, sizeY,
                            MediaSize.MM);
                    if (sname == null) {
                        MediaSize size = new MediaSize(sizeX, sizeY,
                                MediaSize.MM, name);
                    } else {
                        MediaSize size = MediaSize.getMediaSizeForName(sname);
                        if (size.getX(MediaSize.MM) != sizeX ||
                            size.getY(MediaSize.MM) != sizeY) {
                            MediaSize newSize = new MediaSize(sizeX, sizeY,
                                    MediaSize.MM, name);
                        }
                    }
                    if (!result.contains(name)) {
                        result.add(name);
                        medias.put(name, new Integer(ids[i]));
                    }
                }
            }
        }
        if (result.size() > 0) {
            return (MediaSizeName[])result.toArray(new MediaSizeName[0]);
        }
        return null;
    }

    private MediaSize[] getSupportedMediaSizes() {
        MediaSizeName[] mediaSizeNames = getSupportedMediaSizeNames();
        if (mediaSizeNames != null) {
            MediaSize[] mediaSizes = new MediaSize[mediaSizeNames.length];
            for (int i = 0; i < mediaSizeNames.length; i++) {
                mediaSizes[i] =
                    MediaSize.getMediaSizeForName(mediaSizeNames[i]);
            }
            return mediaSizes;
        }
        return null;
    }

//----------------------- Printing ---------------------------------------------    
    
    /*
     * @see org.apache.harmony.x.print.PrintClient#print(javax.print.Doc,
     * javax.print.attribute.PrintRequestAttributeSet)
     */
    public void print(Doc doc, PrintRequestAttributeSet attributes)
            throws PrintException {
        synchronized (this) {
            DocFlavor flavor = doc.getDocFlavor();
            if (flavor.equals(DocFlavor.INPUT_STREAM.POSTSCRIPT) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.POSTSCRIPT) ||
                flavor.equals(DocFlavor.URL.POSTSCRIPT) ||
                flavor.equals(DocFlavor.INPUT_STREAM.AUTOSENSE) ||
                flavor.equals(DocFlavor.BYTE_ARRAY.AUTOSENSE) ||
                flavor.equals(DocFlavor.URL.AUTOSENSE)) {
                InputStream data = null;
                try {
                    if (flavor.equals(DocFlavor.URL.POSTSCRIPT)) {
                        data = ((URL)doc.getPrintData()).openStream();
                    } else {
                        data = doc.getStreamForBytes();
                    }
                } catch (IOException ioe) {
                    throw new PrintException(
                            "Can't read data from document souce");
                }
                int printerID = startDocPrinter(serviceName,
                        convertAttributes(attributes, flavor),
                        getJobName(attributes), getDestination(attributes));
                if (printerID != 0) {
                    byte[] buffer = new byte[10240];
                    try {
                        int bytesRead = data.read(buffer);
                        while (bytesRead >= 0) {
                            if (!writePrinter(buffer, bytesRead, printerID)) {
                                endDocPrinter(printerID);
                                throw new PrintException(
                                        "Can't send data to printer");
                            }
                            bytesRead = data.read(buffer);
                        }
                    } catch (IOException ioe) {
                        throw new PrintException(
                                "Can't read print data from Doc");
                    }
                    if (!endDocPrinter(printerID)) {
                        throw new PrintException("Can't finish job normally");
                    }
                } else {
                    throw new PrintException("Can't start printing");
                }
            } else if (flavor.getMimeType().toLowerCase().equals(
                           "internal/postscript") &&
                       flavor.getRepresentationClassName().equals(
                           "java.io.InputStream")) {
                InputStream data = null;
                try {
                    data = (InputStream)doc.getPrintData();
                } catch (IOException ioe) {
                    throw new PrintException(
                            "Can't read data from document souce");
                }
                PSInterpreter interpreter = new PSInterpreter(data, serviceName,
                        this, attributes);
                interpreter.interpret();
            } else {
                throw new PrintException("DocFlavor is not supported");
            }
        }
    }

    int[] convertAttributes(PrintRequestAttributeSet attrs, DocFlavor flavor)
            throws PrintException {
        PrintRequestAttributeSet attributes = null;
        if (attrs == null ||
            flavor.equals(DocFlavor.INPUT_STREAM.AUTOSENSE) ||
            flavor.equals(DocFlavor.BYTE_ARRAY.AUTOSENSE) ||
            flavor.equals(DocFlavor.URL.AUTOSENSE)) {
            int[] defaultValues = {-1, -1, -1, -1, -1, -1, -1, -1}; 
            return defaultValues;
        } else {
            attributes = attrs;
        }
        Attribute[] requestAttrs = attributes.toArray();
        int[] printAttributes = new int[ATTRIBUTES_ARRAY_SIZE];
        Arrays.fill(printAttributes, -1);
        for (int i = 0; i < requestAttrs.length; i++) {
            Class category = requestAttrs[i].getCategory();
            if (!isAttributeValueSupported(requestAttrs[i], flavor, attrs)) {
                // Do nothing or throw PrintException if Fidelity supported.
            } else if (category.equals(Copies.class)) {
                Copies copies = (Copies)requestAttrs[i];
                printAttributes[COPIES_INDEX] = copies.getValue();
            } else if (category.equals(Sides.class)) {
                Sides sides = (Sides)requestAttrs[i];
                printAttributes[SIDES_INDEX] = 1;
                if (sides.equals(Sides.DUPLEX) ||
                    sides.equals(Sides.TWO_SIDED_LONG_EDGE)) {
                    printAttributes[SIDES_INDEX] = 2;
                } else if (sides.equals(Sides.TUMBLE) ||
                           sides.equals(Sides.TWO_SIDED_SHORT_EDGE)) {
                    printAttributes[SIDES_INDEX] = 3;
                }
            } else if (category.equals(Media.class)) {
                if (medias.containsKey(requestAttrs[i])) {
                    Integer id = (Integer)medias.get(requestAttrs[i]);
                    printAttributes[PAPER_ID_INDEX] = id.intValue();
                }
            } else if (category.equals(Chromaticity.class)) {
                if (requestAttrs[i].equals(Chromaticity.MONOCHROME)) {
                    printAttributes[CHROMATICITY_INDEX] = 1;
                } else if (requestAttrs[i].equals(Chromaticity.COLOR)) {
                    printAttributes[CHROMATICITY_INDEX] = 2;
                }
            } else if (category.equals(OrientationRequested.class)) {
                if (requestAttrs[i].equals(OrientationRequested.PORTRAIT)) {
                    printAttributes[ORIENTATION_INDEX] = 1;
                } else
                  if (requestAttrs[i].equals(OrientationRequested.LANDSCAPE)) {
                    printAttributes[ORIENTATION_INDEX] = 2;
                }
            } else if (category.equals(PrinterResolution.class)) {
                PrinterResolution res = (PrinterResolution)requestAttrs[i];
                int xres = res.getCrossFeedResolution(PrinterResolution.DPI);
                int yres = res.getFeedResolution(PrinterResolution.DPI);
                printAttributes[XRESOLUTION_INDEX] = xres;
                printAttributes[YRESOLUTION_INDEX] = yres;
            } else if (category.equals(SheetCollate.class)) {
                SheetCollate collate = (SheetCollate)requestAttrs[i];
                if (collate == SheetCollate.COLLATED) {
                    printAttributes[COLLATE_INDEX] = 1;
                } else if (collate == SheetCollate.UNCOLLATED) {
                    printAttributes[COLLATE_INDEX] = 0;
                }
            }
        }
        return printAttributes;
    }
    
    String getDestination(PrintRequestAttributeSet attrs)
            throws PrintException {
        if (attrs != null) {
            if (attrs.containsKey(Destination.class)) {
                Destination destination =
                    (Destination)attrs.get(Destination.class);
                if (!destination.getURI().getScheme().equals("file")) {
                    throw new PrintException(
                            "Only files supported as destinations.");
                }
                String file = destination.getURI().getPath();
                if (file.startsWith("/")) {
                    file = file.substring(1);
                }
                return file;
            }
        }
        return null;
    }

    String getJobName(PrintRequestAttributeSet attrs) {
        if (attrs != null) {
            if (attrs.containsKey(JobName.class)) {
                JobName name = (JobName)attrs.get(JobName.class);
                return name.getValue();
            }
        }
        return "Java GDI client print job";
    }
    
//----------------------- DocFlavors -------------------------------------------

    private static native boolean checkPostScript(String serviceName);
    
//----------------------- PrintService attributes ------------------------------    
    
    private static native boolean getColorSupported(String printerName);
    private static native int getPagesPerMinute(String printerNmae);
    private static native int getPagesPerMinuteColor(String printerNmae);
    private static native int getPrinterIsAcceptingJobs(String printerNmae);
    private static native String getPrinterLocation(String printerNmae);
    private static native int getQueuedJobCount(String printerName);

//  --------------------- Print request attributes -----------------------------    

    private static native int getCopiesSupported(String printerName);
    private static native boolean getSidesSupported(String printerName);
    private static native int[][] getMediaSizesSupported(String printerName);
    private static native int[] getMediaIDs(String printerName);
    private static native String[] getMediaNames(String printerName);
    private static native int[] getMediaTraysSupported(String printerName);
    private static native int[] getResolutionsSupported(String printerName);
    private static native boolean getOrientationSupported(String printerName);
    private static native boolean getCollateSupported(String printerName);

//----------------------- Printing methods -------------------------------------

    private static native int startDocPrinter(String serviceName,
            int[] attributes, String jobName, String destination);
    private static native boolean writePrinter(byte[] data, int bytes,
            int printerID);
    private static native boolean endDocPrinter(int printerID);

//----------------------- Internal classes -------------------------------------
    
    private static class GDIMediaName extends MediaSizeName {
        
        private static final long serialVersionUID = 8176250163720875699L;

        private static GDIMediaName staticMediaName = new GDIMediaName(-1);
        private String mediaName = null;
        
        private GDIMediaName(int value) {
            super(value);
        }

        private GDIMediaName(String mediaName, int value) {
            super(value);
            this.mediaName = mediaName;
        }
        
        private static MediaSizeName[] getStandardMediaSizeNames() {
            return (MediaSizeName[])staticMediaName.getEnumValueTable();
        }
        
        public String toString() {
            return mediaName;
        }

        public boolean equals(MediaSizeName name) {
            if (name.hashCode() == hashCode()) {
                return true;
            }
            return false;
        }
        
        public int hashCode() {
            return mediaName.hashCode();
        }
    }
}
