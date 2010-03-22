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

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.QueuedJobCount;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import javax.print.event.PrintServiceAttributeEvent;
import javax.print.event.PrintServiceAttributeListener;

import org.apache.harmony.x.print.DevmodeStructWrapper.Paper;
import org.apache.harmony.x.print.DevmodeStructWrapper.StdPaper;

class WinPrintService implements PrintService {
    
    static final JobName                                     DEFAULT_JOB_NAME    = new JobName("Java printing", null); //$NON-NLS-1$

    private static final DocFlavor[]                         SUPPORTED_FLAVORS   = {
                    DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                    DocFlavor.SERVICE_FORMATTED.PAGEABLE, DocFlavor.URL.JPEG,
                    DocFlavor.INPUT_STREAM.JPEG, DocFlavor.BYTE_ARRAY.JPEG,
                    DocFlavor.URL.GIF, DocFlavor.INPUT_STREAM.GIF,
                    DocFlavor.BYTE_ARRAY.GIF, DocFlavor.URL.PNG,
                    DocFlavor.INPUT_STREAM.PNG, DocFlavor.BYTE_ARRAY.PNG        };

    private static final Class<?>[]                          SUPPORTED_ATTR_CATS = new Class<?>[] {
                    JobName.class, RequestingUserName.class, Destination.class,
                    OrientationRequested.class, Media.class, MediaSize.class,
                    Copies.class, PrintQuality.class, PrinterResolution.class,
                    Sides.class, SheetCollate.class, Chromaticity.class,
                    MediaPrintableArea.class, PrinterResolution.class           };

    final String                                             printerName;
    private final Map<PrintServiceAttributeListener, Object> attrListeners;
    private long                                             pHandle;
    private DevmodeStructWrapper                             dmStruct;
    private DevmodeStructWrapper                             defaultDmStruct;

    WinPrintService(final String printerName) {
        this.printerName = printerName;
        attrListeners = Collections
                        .synchronizedMap(new WeakHashMap<PrintServiceAttributeListener, Object>());
    }

    public void addPrintServiceAttributeListener(
                    final PrintServiceAttributeListener listener) {
        attrListeners.put(listener, null);
    }

    public DocPrintJob createPrintJob() {
        WinPrinterFactory.checkPrintJobAccess();
        try {
            notifyListeners(
                            WinPrinterFactory
                                            .getPrinterState(getPrinterHandle()),
                            WinPrinterFactory
                                            .getQueuedJobCount(getPrinterHandle()));
        } catch (final PrintException ex) {
            throw new RuntimeException(ex);
        }
        return new WinPrintJob(this);
    }

    public <T extends PrintServiceAttribute> T getAttribute(
                    final Class<T> category) {
        if (category == null) {
            throw new NullPointerException("category should not be null"); //$NON-NLS-1$
        }

        if (!PrintServiceAttribute.class.isAssignableFrom(category)) {
            throw new IllegalArgumentException(category
                            + " is not assignable from PrintServiceAttribute"); //$NON-NLS-1$
        }

        try {
            if (PrinterName.class.equals(category)) {
                return category.cast(new PrinterName(printerName, null));
            } else if (PrinterState.class.equals(category)) {
                return category.cast(WinPrinterFactory
                                .getPrinterState(getPrinterHandle()));
            } else if (QueuedJobCount.class.equals(category)) {
                return category.cast(WinPrinterFactory
                                .getQueuedJobCount(getPrinterHandle()));
            }

            return null;
        } catch (final PrintException ex) {
            throw new RuntimeException(ex);
        }
    }

    public PrintServiceAttributeSet getAttributes() {
        final PrintServiceAttributeSet attrs = new HashPrintServiceAttributeSet();

        attrs.add(new PrinterName(printerName, null));
        try {
            attrs.add(WinPrinterFactory.getPrinterState(getPrinterHandle()));
            attrs.add(WinPrinterFactory.getQueuedJobCount(getPrinterHandle()));
        } catch (final PrintException ex) {
            throw new RuntimeException(ex);
        }

        return AttributeSetUtilities.unmodifiableView(attrs);
    }

    public Object getDefaultAttributeValue(
                    final Class<? extends Attribute> category) {
        checkArgs(category, null);

        final DevmodeStructWrapper dm = getDefaultPrinterProps();

        if (JobName.class.equals(category)) {
            return DEFAULT_JOB_NAME;
        } else if (RequestingUserName.class.equals(category)) {
            return new RequestingUserName(getSystemProperty("user.name"), //$NON-NLS-1$
                            null);
        } else if (Destination.class.equals(category)) {
            File file = new File(getSystemProperty("user.dir") //$NON-NLS-1$
                            + File.separator + "output.prn"); //$NON-NLS-1$
            return new Destination(file.toURI());
        } else if (OrientationRequested.class.equals(category)) {
            return dm.getOrientation();
        } else if (Paper.class.equals(category)) {
            return getDefaultPaper();
        } else if (Media.class.equals(category)) {
            return getDefaultPaper().getSize().getMediaSizeName();
        } else if (MediaSize.class.equals(category)) {
            return getDefaultPaper().getSize();
        } else if (PrintQuality.class.equals(category)) {
            return dm.getPrintQuality();
        } else if (Sides.class.equals(category)) {
            return dm.getSides();
        } else if (Copies.class.equals(category)) {
            return dm.getCopies();
        } else if (SheetCollate.class.equals(category)) {
            return dm.getCollate();
        } else if (PrinterResolution.class.equals(category)) {
            return dm.getPrinterResolution();
        } else if (Chromaticity.class.equals(category)) {
            return dm.getChromaticity();
        }

        return null;
    }

    public String getName() {
        return printerName;
    }

    public ServiceUIFactory getServiceUIFactory() {
        return null;
    }

    public Class<?>[] getSupportedAttributeCategories() {
        return SUPPORTED_ATTR_CATS;
    }

    public Object getSupportedAttributeValues(
                    final Class<? extends Attribute> category,
                    final DocFlavor flavor, final AttributeSet attributes) {
        checkArgs(category, flavor);

        try {
            if (OrientationRequested.class.equals(category)) {
                return WinPrinterFactory
                                .getSupportedOrientations(getPrinterHandle());
            } else if (Media.class.equals(category)
                            || MediaSizeName.class.equals(category)) {
                return WinPrinterFactory
                                .getSupportedMediaSizeNames(getPrinterHandle());
            } else if (MediaSize.class.equals(category)) {
                return WinPrinterFactory
                                .getSupportedMediaSizes(getPrinterHandle());
            } else if (CopiesSupported.class.equals(category)) {
                final int max = WinPrinterFactory
                                .getMaxNumberOfCopies(getPrinterHandle());
                return max > 1 ? new CopiesSupported(1, max)
                                : new CopiesSupported(1);
            } else if (PrintQuality.class.equals(category)) {
                return new PrintQuality[] { PrintQuality.HIGH,
                                PrintQuality.NORMAL, PrintQuality.DRAFT };
            } else if (Sides.class.equals(category)) {
                return WinPrinterFactory.isDuplexSupported(getPrinterHandle())
                                ? new Sides[] { Sides.ONE_SIDED,
                                                Sides.TWO_SIDED_SHORT_EDGE,
                                                Sides.TWO_SIDED_LONG_EDGE }
                                : new Sides[] { Sides.ONE_SIDED };
            } else if (SheetCollate.class.equals(category)) {
                return WinPrinterFactory.isDuplexSupported(getPrinterHandle())
                                ? new SheetCollate[] { SheetCollate.COLLATED,
                                                SheetCollate.UNCOLLATED }
                                : new SheetCollate[] { SheetCollate.UNCOLLATED };
            } else if (Chromaticity.class.equals(category)) {
                return WinPrinterFactory
                                .isColorPrintingSupported(getPrinterHandle())
                                ? new Chromaticity[] { Chromaticity.MONOCHROME,
                                                Chromaticity.COLOR }
                                : new Chromaticity[] { Chromaticity.MONOCHROME };
            } else if (PrinterResolution.class.equals(category)) {
                return WinPrinterFactory
                                .getSupportedPrinterResolutions(getPrinterHandle());
            } else if (PrintQuality.class.equals(category)) {
                return new PrintQuality[] { PrintQuality.HIGH,
                                PrintQuality.NORMAL, PrintQuality.DRAFT };
            }
        } catch (final PrintException ex) {
            throw new RuntimeException(ex);
        }

        return null;
    }

    public DocFlavor[] getSupportedDocFlavors() {
        return SUPPORTED_FLAVORS;
    }

    public AttributeSet getUnsupportedAttributes(final DocFlavor flavor,
                    final AttributeSet attributes) {
        checkFlavor(flavor);

        if (attributes == null) {
            return null;
        }

        final AttributeSet result = new HashAttributeSet();

        for (Attribute attr : attributes.toArray()) {
            if (!isAttributeValueSupported(attr, flavor, attributes)) {
                result.add(attr);
            }
        }

        return result.size() > 0 ? result : null;
    }

    public boolean isAttributeCategorySupported(
                    final Class<? extends Attribute> category) {
        checkArgs(category, null);
        return arrayContains(SUPPORTED_ATTR_CATS, category);
    }

    public boolean isAttributeValueSupported(final Attribute attrval,
                    final DocFlavor flavor, final AttributeSet attributes) {
        checkFlavor(flavor);

        final Class<? extends Attribute> category = attrval.getCategory();

        try {
            if (Copies.class.equals(category)) {
                int max = WinPrinterFactory
                                .getMaxNumberOfCopies(getPrinterHandle());
                return max <= 0 ? ((Copies) attrval).getValue() == 1
                                : ((Copies) attrval).getValue() <= max;
            }
        } catch (final PrintException ex) {
            throw new RuntimeException(ex);
        }

        final Object obj = getSupportedAttributeValues(category, flavor,
                        attributes);

        if (obj != null) {
            if (obj.getClass().isArray()) {
                return arrayContains((Object[]) obj, attrval);
            } else {
                return obj.equals(attrval);
            }
        }

        return false;
    }

    public boolean isDocFlavorSupported(final DocFlavor flavor) {
        return arrayContains(SUPPORTED_FLAVORS, flavor);
    }

    public void removePrintServiceAttributeListener(
                    final PrintServiceAttributeListener listener) {
        attrListeners.remove(listener);
    }

    @Override
    public boolean equals(final Object object) {
        return (object instanceof WinPrintService)
                        && ((WinPrintService) object).printerName
                                        .equals(printerName);
    }

    @Override
    public String toString() {
        return getName();
    }

    synchronized long getPrinterHandle() {
        if (pHandle == 0) {
            try {
                pHandle = WinPrinterFactory.getPrinterHandle(printerName);
            } catch (final PrintException ex) {
                throw new RuntimeException(ex);
            }
        }

        return pHandle;
    }

    synchronized DevmodeStructWrapper getPrinterProps() {
        if (dmStruct == null) {
            try {
                dmStruct = new DevmodeStructWrapper(WinPrinterFactory
                                .getPrinterProps(printerName,
                                                getPrinterHandle()));
            } catch (final PrintException ex) {
                throw new RuntimeException(ex);
            }
        }

        return dmStruct;
    }

    synchronized DevmodeStructWrapper getDefaultPrinterProps() {
        if (defaultDmStruct == null) {
            try {
                defaultDmStruct = new DevmodeStructWrapper(WinPrinterFactory
                                .getPrinterProps(printerName,
                                                getPrinterHandle()));
            } catch (final PrintException ex) {
                throw new RuntimeException(ex);
            }
        }

        return defaultDmStruct;
    }

    @Override
    protected synchronized void finalize() {
        if (pHandle > 0) {
            try {
                WinPrinterFactory.releasePrinterHandle(pHandle);
                pHandle = 0;
                dmStruct = null;
                defaultDmStruct = null;
            } catch (final PrintException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static <T> boolean arrayContains(final T[] array, final T val) {
        for (T a : array) {
            if (a.equals(val)) {
                return true;
            }
        }
        return false;
    }

    private static String getSystemProperty(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(name);
            }
        });
    }

    private void notifyListeners(final PrintServiceAttribute... attrs) {
        final PrintServiceAttributeEvent event = new PrintServiceAttributeEvent(
                        this, new HashPrintServiceAttributeSet(attrs));
        for (PrintServiceAttributeListener listener : attrListeners.keySet()) {
            listener.attributeUpdate(event);
        }
    }

    private Paper getDefaultPaper() {
        final Paper p = getDefaultPrinterProps().getPaper();
        return p != null ? p : StdPaper.ISO_A4;
    }

    private void checkFlavor(final DocFlavor flavor) {
        if ((flavor != null) && !isDocFlavorSupported(flavor)) {
            throw new IllegalArgumentException("unsupported flavor"); //$NON-NLS-1$
        }
    }

    private void checkArgs(final Class<? extends Attribute> category,
                    final DocFlavor flavor) {
        if (category == null) {
            throw new NullPointerException("category should not be null"); //$NON-NLS-1$
        }

        if (!Attribute.class.isAssignableFrom(category)) {
            throw new IllegalArgumentException(category
                            + " is not assignable from Attribute"); //$NON-NLS-1$
        }

        checkFlavor(flavor);
    }
}
