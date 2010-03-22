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

import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;

/**
 * Wrapper for the DEVMODE native structure.
 */
public class DevmodeStructWrapper {

    // DmField flags
    public static final int   DM_ORIENTATION     = 0x00000001;
    public static final int   DM_PAPERSIZE       = 0x00000002;
    public static final int   DM_PAPERLENGTH     = 0x00000004;
    public static final int   DM_PAPERWIDTH      = 0x00000008;
    public static final int   DM_SCALE           = 0x00000010;
    public static final int   DM_COPIES          = 0x00000100;
    public static final int   DM_DEFAULTSOURCE   = 0x00000200;
    public static final int   DM_PRINTQUALITY    = 0x00000400;
    public static final int   DM_COLOR           = 0x00000800;
    public static final int   DM_DUPLEX          = 0x00001000;
    public static final int   DM_YRESOLUTION     = 0x00002000;
    public static final int   DM_TTOPTION        = 0x00004000;
    public static final int   DM_COLLATE         = 0x00008000;

    // Orientation fields
    public static final short DMORIENT_PORTRAIT  = 1;
    public static final short DMORIENT_LANDSCAPE = 2;

    // Print quality predefined values
    public static final short DMRES_HIGH         = -4;
    public static final short DMRES_MEDIUM       = -3;
    public static final short DMRES_DRAFT        = -1;

    // Sides
    public static final short DMDUP_SIMPLEX      = 1;
    public static final short DMDUP_VERTICAL     = 2;
    public static final short DMDUP_HORIZONTAL   = 3;

    // Collate
    public static final short DMCOLLATE_FALSE    = 0;
    public static final short DMCOLLATE_TRUE     = 1;

    // Chromaticity
    public static final short DMCOLOR_MONOCHROME = 1;
    public static final short DMCOLOR_COLOR      = 2;

    public long               structPtr;

    public DevmodeStructWrapper(final long structPtr) {
        this.structPtr = structPtr;
    }

    public long getStructPtr() {
        return structPtr;
    }

    public String getDmDeviceName() {
        return getDmDeviceName(structPtr);
    }

    public long getDmFields() {
        return getDmFields(structPtr);
    }

    public OrientationRequested getOrientation() {
        return getDmOrientation(structPtr) == DMORIENT_LANDSCAPE
                        ? OrientationRequested.LANDSCAPE
                        : OrientationRequested.PORTRAIT;
    }

    public void setOrientation(final OrientationRequested orientation) {
        if (OrientationRequested.PORTRAIT.equals(orientation)) {
            setDmOrientation(structPtr, DMORIENT_PORTRAIT);
        } else if (OrientationRequested.LANDSCAPE.equals(orientation)
                        || OrientationRequested.REVERSE_LANDSCAPE
                                        .equals(orientation)) {
            setDmOrientation(structPtr, DMORIENT_LANDSCAPE);
        }
    }

    public Paper getPaper() {
        final short size = getDmPaperSize(structPtr);
        Paper p = StdPaper.getPaper(size);

        if (p == null) {
            final long fields = getDmFields();

            if (((fields & DM_PAPERLENGTH) != 0)
                            && ((fields & DM_PAPERWIDTH) != 0)) {
                p = new CustomPaper(size, new MediaSize(
                                getDmPaperWidth(structPtr) / 10,
                                getDmPaperLength(structPtr) / 10,
                                Size2DSyntax.MM));
            }
        }

        return p;
    }

    public void setPaper(final Paper paper) {
        if (paper != null) {
            if (paper.getDmPaperSize() > 0) {
                setDmPaperSize(structPtr, paper.getDmPaperSize());
            } else {
                setDmPaperWidth(structPtr, (short) (paper.getSize().getX(
                                Size2DSyntax.MM) * 10));
                setDmPaperLength(structPtr, (short) (paper.getSize().getY(
                                Size2DSyntax.MM) * 10));
            }
        }
    }

    public void setPaper(final MediaSize size) {
        final Paper p = StdPaper.getPaper(size);
        setPaper((p != null) ? p : new CustomPaper(0, size));
    }

    public void setPaper(final MediaSizeName name) {
        final Paper p = StdPaper.getPaper(name);
        setPaper((p != null) ? p : new CustomPaper(0, MediaSize
                        .getMediaSizeForName(name)));
    }

    public Copies getCopies() {
        final short copies = getDmCopies(structPtr);
        return copies > 0 ? new Copies(copies) : new Copies(1);
    }

    public void setCopies(final Copies c) {
        setDmCopies(structPtr, (short) c.getValue());
    }

    public PrintQuality getPrintQuality() {
        switch (getDmPrintQuality(structPtr)) {
        case DMRES_HIGH:
            return PrintQuality.HIGH;
        case DMRES_DRAFT:
            return PrintQuality.DRAFT;
        default:
            return PrintQuality.NORMAL;
        }
    }

    public void setPrintQuality(final PrintQuality quality) {
        if (PrintQuality.NORMAL.equals(quality)) {
            setDmPrintQuality(structPtr, DMRES_MEDIUM);
        } else if (PrintQuality.HIGH.equals(quality)) {
            setDmPrintQuality(structPtr, DMRES_HIGH);
        } else if (PrintQuality.DRAFT.equals(quality)) {
            setDmPrintQuality(structPtr, DMRES_DRAFT);
        }
    }

    public Sides getSides() {
        switch (getDmDuplex(structPtr)) {
        case DMDUP_VERTICAL:
            return Sides.TWO_SIDED_LONG_EDGE;
        case DMDUP_HORIZONTAL:
            return Sides.TWO_SIDED_SHORT_EDGE;
        default:
            return Sides.ONE_SIDED;
        }
    }

    public void setSides(final Sides sides) {
        if (Sides.ONE_SIDED.equals(sides)) {
            setDmDuplex(structPtr, DMDUP_SIMPLEX);
        } else if (Sides.TWO_SIDED_LONG_EDGE.equals(sides)) {
            setDmDuplex(structPtr, DMDUP_VERTICAL);
        } else if (Sides.TWO_SIDED_SHORT_EDGE.equals(sides)) {
            setDmDuplex(structPtr, DMDUP_HORIZONTAL);
        }
    }

    public SheetCollate getCollate() {
        return getDmCollate(structPtr) == DMCOLLATE_TRUE
                        ? SheetCollate.COLLATED : SheetCollate.UNCOLLATED;
    }

    public void setCollate(final SheetCollate collate) {
        if (SheetCollate.UNCOLLATED.equals(collate)) {
            setDmCollate(structPtr, DMCOLLATE_FALSE);
        } else if (SheetCollate.COLLATED.equals(collate)) {
            setDmCollate(structPtr, DMCOLLATE_TRUE);
        }
    }

    public PrinterResolution getPrinterResolution() {
        final int x = getDmPrintQuality(structPtr);
        final int y = getDmYResolution(structPtr);

        if (y > 0) {
            return new PrinterResolution(x > 0 ? x : y, y, ResolutionSyntax.DPI);
        }

        return null;
    }

    public void setPrinterResolution(final PrinterResolution res) {
        setDmPrintQuality(structPtr, (short) res
                        .getCrossFeedResolution(ResolutionSyntax.DPI));
        setDmYResolution(structPtr, (short) res
                        .getFeedResolution(ResolutionSyntax.DPI));
    }

    public Chromaticity getChromaticity() {
        return getDmColor(structPtr) == DMCOLOR_COLOR ? Chromaticity.COLOR
                        : Chromaticity.MONOCHROME;
    }

    public void setChromaticity(final Chromaticity chromaticity) {
        if (Chromaticity.COLOR.equals(chromaticity)) {
            setDmColor(structPtr, DMCOLOR_COLOR);
        } else if (Chromaticity.MONOCHROME.equals(chromaticity)) {
            setDmColor(structPtr, DMCOLOR_MONOCHROME);
        }
    }

    public void setAttribute(final Attribute attr) {
        final Class<? extends Attribute> category = attr.getCategory();

        if (OrientationRequested.class.equals(category)) {
            setOrientation((OrientationRequested) attr);
        } else if (MediaSize.class.equals(category)) {
            setPaper((MediaSize) attr);
        } else if (Media.class.equals(category)) {
            setPaper((MediaSizeName) attr);
        } else if (Paper.class.equals(category)) {
            setPaper((Paper) attr);
        } else if (Copies.class.equals(category)) {
            setCopies((Copies) attr);
        } else if (PrintQuality.class.equals(category)) {
            setPrintQuality((PrintQuality) attr);
        } else if (Sides.class.equals(category)) {
            setSides((Sides) attr);
        } else if (SheetCollate.class.equals(category)) {
            setCollate((SheetCollate) attr);
        } else if (PrinterResolution.class.equals(category)) {
            setPrinterResolution((PrinterResolution) attr);
        } else if (Chromaticity.class.equals(category)) {
            setChromaticity((Chromaticity) attr);
        }
    }

    public void setAttributes(final AttributeSet attrs) {
        if (attrs != null) {
            for (Attribute attr : attrs.toArray()) {
                setAttribute(attr);
            }
        }
    }

    public <T extends AttributeSet> T getAttributes(final T attrs) {
        final long flags = getDmFields();
        final Paper p = getPaper();
        final PrinterResolution res = getPrinterResolution();

        if (p != null) {
            attrs.add(p.getSize());
            attrs.add(p.getSize().getMediaSizeName());
        }
        if (res != null) {
            attrs.add(res);
        }
        if ((flags & DM_ORIENTATION) != 0) {
            attrs.add(getOrientation());
        }
        if ((flags & DM_COPIES) != 0) {
            attrs.add(getCopies());
        }
        if ((flags & DM_PRINTQUALITY) != 0) {
            attrs.add(getPrintQuality());
        }
        if ((flags & DM_DUPLEX) != 0) {
            attrs.add(getSides());
        }
        if ((flags & DM_COLLATE) != 0) {
            attrs.add(getCollate());
        }
        if ((flags & DM_COLOR) != 0) {
            attrs.add(getChromaticity());
        }

        return attrs;
    }

    // --------------------- Native functions --------------------------- //
    public static native String getDmDeviceName(final long structPtr);

    public static native long getDmFields(final long structPtr);

    public static native short getDmOrientation(final long structPtr);

    public static native void setDmOrientation(final long structPtr,
                    final short orientation);

    public static native short getDmPaperSize(final long structPtr);

    public static native void setDmPaperSize(final long structPtr,
                    final short paperSize);

    public static native short getDmPaperLength(final long structPtr);

    public static native void setDmPaperLength(final long structPtr,
                    final short paperLength);

    public static native short getDmPaperWidth(final long structPtr);

    public static native void setDmPaperWidth(final long structPtr,
                    final short paperWidth);

    public static native short getDmScale(final long structPtr);

    public static native void setDmScale(final long structPtr, final short scale);

    public static native short getDmCopies(final long structPtr);

    public static native void setDmCopies(final long structPtr,
                    final short copies);

    public static native short getDmDefaultSource(final long structPtr);

    public static native void setDmDefaultSource(final long structPtr,
                    final short defaultSource);

    public static native short getDmPrintQuality(final long structPtr);

    public static native void setDmPrintQuality(final long structPtr,
                    final short printQuality);

    public static native short getDmColor(final long structPtr);

    public static native void setDmColor(final long structPtr, final short color);

    public static native short getDmDuplex(final long structPtr);

    public static native void setDmDuplex(final long structPtr,
                    final short duplex);

    public static native short getDmYResolution(final long structPtr);

    public static native void setDmYResolution(final long structPtr,
                    final short yResolution);

    public static native short getDmTTOption(final long structPtr);

    public static native void setDmTTOption(final long structPtr,
                    final short option);

    public static native short getDmCollate(final long structPtr);

    public static native void setDmCollate(final long structPtr,
                    final short collate);

    public static native void releaseStruct(final long structPtr);

    @Override
    protected synchronized void finalize() throws Throwable {
        if (structPtr > 0) {
            releaseStruct(structPtr);
            structPtr = 0;
        }
    }

    public static interface Paper extends Attribute {
        public short getDmPaperSize();

        public MediaSize getSize();
    }

    public static class CustomPaper implements Paper {
        private static final long serialVersionUID = 3265772990664792005L;
        final short               dmPaperSize;
        final MediaSize           size;

        public CustomPaper(final int dmPaperSize, final MediaSize size) {
            this.dmPaperSize = (short) dmPaperSize;
            this.size = size;
        }

        public short getDmPaperSize() {
            return dmPaperSize;
        }

        public MediaSize getSize() {
            return size;
        }

        public Class<? extends Attribute> getCategory() {
            return Paper.class;
        }

        public String getName() {
            return size.getName();
        }
    }

    public static enum StdPaper implements Paper {
            ISO_A2(66, MediaSize.ISO.A2), // DMPAPER_A2
            ISO_A3(8, MediaSize.ISO.A3), // DMPAPER_A3
            ISO_A4(9, MediaSize.ISO.A4), // DMPAPER_A4
            ISO_A5(11, MediaSize.ISO.A5), // DMPAPER_A5
            ISO_A6(70, MediaSize.ISO.A6), // DMPAPER_A6
            NA_LETTER(1, MediaSize.NA.LETTER), // DMPAPER_LETTER
            NA_LEGAL(5, MediaSize.NA.LEGAL), // DMPAPER_LEGAL
            TABLOID(3, MediaSize.Other.TABLOID), // DMPAPER_TABLOID
            NA_10x14(16, MediaSize.NA.NA_10x14_ENVELOPE), // DMPAPER_10X14
            ISO_B4(12, MediaSize.ISO.B4), // DMPAPER_B4
            JIS_B5(13, MediaSize.JIS.B5), // DMPAPER_B5
            JIS_B6(88, MediaSize.JIS.B5), // DMPAPER_B6_JIS
            JPC(43, MediaSize.Other.JAPANESE_POSTCARD), // DMPAPER_JAPANESE_POSTCARD
            JPC_D(69, MediaSize.Other.JAPANESE_DOUBLE_POSTCARD); // DMPAPER_DBL_JAPANESE_POSTCARD

        final short     dmPaperSize;
        final MediaSize size;

        StdPaper(final int dmPaperSize, final MediaSize size) {
            this.dmPaperSize = (short) dmPaperSize;
            this.size = size;
        }

        public static Paper getPaper(final short dmPaperSize) {
            for (StdPaper p : values()) {
                if (p.dmPaperSize == dmPaperSize) {
                    return p;
                }
            }

            return null;
        }

        public static Paper getPaper(final MediaSize size) {
            for (StdPaper p : values()) {
                if (p.size.equals(size)) {
                    return p;
                }
            }

            return null;
        }

        public static Paper getPaper(final MediaSizeName name) {
            for (StdPaper p : values()) {
                if (p.size.getMediaSizeName().equals(name)) {
                    return p;
                }
            }

            return null;
        }

        public static MediaSize[] getSizes() {
            final StdPaper[] paper = values();
            final MediaSize[] names = new MediaSize[paper.length];

            for (int i = 0; i < paper.length; i++) {
                names[i] = paper[i].size;
            }

            return names;
        }

        public static MediaSizeName[] getNames() {
            final StdPaper[] paper = values();
            final MediaSizeName[] names = new MediaSizeName[paper.length];

            for (int i = 0; i < paper.length; i++) {
                names[i] = paper[i].size.getMediaSizeName();
            }

            return names;
        }

        public short getDmPaperSize() {
            return dmPaperSize;
        }

        public MediaSize getSize() {
            return size;
        }

        public Class<? extends Attribute> getCategory() {
            return Paper.class;
        }

        public String getName() {
            return toString() + ": " + size; //$NON-NLS-1$
        }
    }
}
