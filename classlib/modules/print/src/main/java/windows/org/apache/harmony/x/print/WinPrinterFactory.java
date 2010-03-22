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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Vector;

import javax.print.PrintException;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.QueuedJobCount;

public class WinPrinterFactory {

    public static final int PRINTER_STATUS_PAUSED            = 1;
    public static final int PRINTER_STATUS_ERROR             = 2;
    public static final int PRINTER_STATUS_PENDING_DELETION  = 4;
    public static final int PRINTER_STATUS_PAPER_JAM         = 8;
    public static final int PRINTER_STATUS_PAPER_OUT         = 0x10;
    public static final int PRINTER_STATUS_MANUAL_FEED       = 0x20;
    public static final int PRINTER_STATUS_PAPER_PROBLEM     = 0x40;
    public static final int PRINTER_STATUS_OFFLINE           = 0x80;
    public static final int PRINTER_STATUS_IO_ACTIVE         = 0x100;
    public static final int PRINTER_STATUS_BUSY              = 0x200;
    public static final int PRINTER_STATUS_PRINTING          = 0x400;
    public static final int PRINTER_STATUS_OUTPUT_BIN_FULL   = 0x800;
    public static final int PRINTER_STATUS_NOT_AVAILABLE     = 0x1000;
    public static final int PRINTER_STATUS_WAITING           = 0x2000;
    public static final int PRINTER_STATUS_PROCESSING        = 0x4000;
    public static final int PRINTER_STATUS_INITIALIZING      = 0x8000;
    public static final int PRINTER_STATUS_WARMING_UP        = 0x10000;
    public static final int PRINTER_STATUS_TONER_LOW         = 0x20000;
    public static final int PRINTER_STATUS_NO_TONER          = 0x40000;
    public static final int PRINTER_STATUS_PAGE_PUNT         = 0x80000;
    public static final int PRINTER_STATUS_USER_INTERVENTION = 0x100000;
    public static final int PRINTER_STATUS_OUT_OF_MEMORY     = 0x200000;
    public static final int PRINTER_STATUS_DOOR_OPEN         = 0x400000;
    public static final int PRINTER_STATUS_SERVER_UNKNOWN    = 0x800000;
    public static final int PRINTER_STATUS_POWER_SAVE        = 0x1000000;

    static {
        checkPrintJobAccess();
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                System.loadLibrary("print"); //$NON-NLS-1$
                return null;
            }
        });
    }

    public static PrinterState getPrinterState(final long handle)
                    throws PrintException {
        final long status = getPrinterStatus(handle);

        if ((status & (PRINTER_STATUS_PRINTING | PRINTER_STATUS_PROCESSING)) != 0) {
            return PrinterState.PROCESSING;
        } else if ((status & (PRINTER_STATUS_DOOR_OPEN | PRINTER_STATUS_ERROR
                        | PRINTER_STATUS_NO_TONER
                        | PRINTER_STATUS_NOT_AVAILABLE | PRINTER_STATUS_OFFLINE
                        | PRINTER_STATUS_OUT_OF_MEMORY
                        | PRINTER_STATUS_OUTPUT_BIN_FULL
                        | PRINTER_STATUS_PAPER_JAM | PRINTER_STATUS_PAPER_OUT
                        | PRINTER_STATUS_PAPER_PROBLEM | PRINTER_STATUS_USER_INTERVENTION)) != 0) {
            return PrinterState.STOPPED;
        } else if ((status & PRINTER_STATUS_SERVER_UNKNOWN) != 0) {
            return PrinterState.UNKNOWN;
        } else {
            return PrinterState.IDLE;
        }
    }

    public static QueuedJobCount getQueuedJobCount(final long handle)
                    throws PrintException {
        return new QueuedJobCount(getQueuedJobs(handle));
    }

    public static MediaSizeName[] getSupportedMediaSizeNames(final long handle)
                    throws PrintException {
        final MediaSizeName[] names;
        final int[] sizes = getSupportedPaperSizes(handle);
        final Vector<MediaSizeName> v = new Vector<MediaSizeName>(
                        sizes.length / 2);

        for (int i = 0; i < sizes.length; i += 2) {
            if ((sizes[i] > 0) && (sizes[i + 1] > 0)) {
                final MediaSizeName name = MediaSize.findMedia(sizes[i] / 10,
                                sizes[i + 1] / 10, Size2DSyntax.MM);

                if ((name != null) && !v.contains(name)) {
                    v.add(name);
                }
            }
        }

        names = new MediaSizeName[v.size()];
        return v.toArray(names);
    }

    public static MediaSize[] getSupportedMediaSizes(final long handle)
                    throws PrintException {
        final MediaSizeName[] names = getSupportedMediaSizeNames(handle);
        final MediaSize[] sizes = new MediaSize[names.length];

        for (int i = 0; i < names.length; i++) {
            sizes[i] = MediaSize.getMediaSizeForName(names[i]);
        }

        return sizes;
    }

    public static OrientationRequested[] getSupportedOrientations(
                    final long handle) throws PrintException {
        if (getLandscapeOrientationDegree(handle) == 270) {
            return new OrientationRequested[] { OrientationRequested.PORTRAIT,
                            OrientationRequested.REVERSE_LANDSCAPE };
        }
        return new OrientationRequested[] { OrientationRequested.PORTRAIT,
                        OrientationRequested.LANDSCAPE };
    }

    public static PrinterResolution[] getSupportedPrinterResolutions(
                    final long handle) throws PrintException {
        final int[] res = getSupportedResolutions(handle);
        final PrinterResolution[] resolutions = new PrinterResolution[res.length / 2];

        for (int i = 0; i < res.length; i += 2) {
            resolutions[i / 2] = new PrinterResolution(res[i], res[i + 1],
                            ResolutionSyntax.DPI);
        }
        return resolutions;
    }

    public static void checkPrintJobAccess() {
        final SecurityManager mgr = System.getSecurityManager();

        if (mgr != null) {
            mgr.checkPrintJobAccess();
        }
    }

    public static native String getDefaultPrinterName() throws PrintException;

    public static native String[] getConnectedPrinterNames()
                    throws PrintException;

    public static native long getPrinterHandle(final String printerName)
                    throws PrintException;

    public static native void releasePrinterHandle(final long handle)
                    throws PrintException;

    /**
     * Returns pointer to DEVMODEW structure
     */
    public static native long getPrinterProps(final String printerName,
                    final long handle) throws PrintException;

    public static native long getPrinterDC(final String printerName,
                    final long pDevMode) throws PrintException;

    public static native void releasePrinterDC(final long pdc)
                    throws PrintException;

    public static native int startDoc(final long pdc, final String docName,
                    final String filePath) throws PrintException;

    public static native void endDoc(final long pdc) throws PrintException;

    public static native void startPage(final long pdc) throws PrintException;

    public static native void endPage(final long pdc) throws PrintException;

    public static native int getQueuedJobs(final long handle)
                    throws PrintException;

    public static native int getPixelsPerInchX(final long pdc)
                    throws PrintException;

    public static native int getPixelsPerInchY(final long pdc)
                    throws PrintException;

    public static native int getPaperPhysicalWidth(final long pdc)
                    throws PrintException;

    public static native int getPaperPhysicalHeight(final long pdc)
                    throws PrintException;

    public static native long getPrinterStatus(final long handle)
                    throws PrintException;

    public static native boolean isColorPrintingSupported(final long handle)
                    throws PrintException;

    public static native boolean isCollatingSupported(final long handle)
                    throws PrintException;

    public static native boolean isDuplexSupported(final long handle)
                    throws PrintException;

    public static native int[] getSupportedPaperSizes(final long handle)
                    throws PrintException;

    public static native int[] getSupportedResolutions(final long handle)
                    throws PrintException;

    public static native int getLandscapeOrientationDegree(final long handle)
                    throws PrintException;

    public static native int getMaxNumberOfCopies(final long handle)
                    throws PrintException;

    public static native void cancelPrinterJob(final long handle,
                    final int jobId) throws PrintException;
}
