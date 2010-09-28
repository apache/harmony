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
package java.awt;

import java.util.Locale;

import org.apache.harmony.awt.internal.nls.Messages;

public final class PageAttributes implements Cloneable {
    private MediaType media;
    private ColorType color;
    private OrientationRequestedType orientationRequested;
    private OriginType origin;
    private PrintQualityType printQuality;
    private int[] printerResolution;

/*----------------- section of the nested classes --------------------*/
    public static final class ColorType {
        private final String name;
        public static final ColorType COLOR
                                        = new ColorType(0, "COLOR"); //$NON-NLS-1$
        public static final ColorType MONOCHROME
                                        = new ColorType(1, "MONOCHROME"); //$NON-NLS-1$

        private ColorType(int i, String n){
            super();
            name = n;
        }

        private ColorType(){
            this(1, "MONOCHROME"); //$NON-NLS-1$
        }
    }

    public static final class MediaType {
        private final String name;

        public static final MediaType ISO_4A0 = new MediaType(71, "ISO_4A0"); //$NON-NLS-1$
        public static final MediaType ISO_2A0 = new MediaType(70, "ISO_2A0"); //$NON-NLS-1$

        public static final MediaType ISO_A0 = new MediaType(0, "ISO_A0"); //$NON-NLS-1$
        public static final MediaType ISO_A1 = new MediaType(1, "ISO_A1"); //$NON-NLS-1$
        public static final MediaType ISO_A2 = new MediaType(2, "ISO_A2"); //$NON-NLS-1$
        public static final MediaType ISO_A3 = new MediaType(3, "ISO_A3"); //$NON-NLS-1$
        public static final MediaType ISO_A4 = new MediaType(4, "ISO_A4"); //$NON-NLS-1$
        public static final MediaType ISO_A5 = new MediaType(5, "ISO_A5"); //$NON-NLS-1$
        public static final MediaType ISO_A6 = new MediaType(6, "ISO_A6"); //$NON-NLS-1$
        public static final MediaType ISO_A7 = new MediaType(7, "ISO_A7"); //$NON-NLS-1$
        public static final MediaType ISO_A8 = new MediaType(8, "ISO_A8"); //$NON-NLS-1$
        public static final MediaType ISO_A9 = new MediaType(9, "ISO_A9"); //$NON-NLS-1$
        public static final MediaType ISO_A10 = new MediaType(10, "ISO_A10"); //$NON-NLS-1$

        public static final MediaType ISO_B0 = new MediaType(10, "ISO_B0"); //$NON-NLS-1$
        public static final MediaType ISO_B1 = new MediaType(11, "ISO_B1"); //$NON-NLS-1$
        public static final MediaType ISO_B2 = new MediaType(12, "ISO_B2"); //$NON-NLS-1$
        public static final MediaType ISO_B3 = new MediaType(13, "ISO_B3"); //$NON-NLS-1$
        public static final MediaType ISO_B4 = new MediaType(14, "ISO_B4"); //$NON-NLS-1$
        public static final MediaType ISO_B5 = new MediaType(15, "ISO_B5"); //$NON-NLS-1$
        public static final MediaType ISO_B6 = new MediaType(16, "ISO_B6"); //$NON-NLS-1$
        public static final MediaType ISO_B7 = new MediaType(17, "ISO_B7"); //$NON-NLS-1$
        public static final MediaType ISO_B8 = new MediaType(18, "ISO_B8"); //$NON-NLS-1$
        public static final MediaType ISO_B9 = new MediaType(19, "ISO_B9"); //$NON-NLS-1$
        public static final MediaType ISO_B10 = new MediaType(20, "ISO_B10"); //$NON-NLS-1$

        public static final MediaType JIS_B0 = new MediaType(30, "JIS_B0"); //$NON-NLS-1$
        public static final MediaType JIS_B1 = new MediaType(31, "JIS_B1"); //$NON-NLS-1$
        public static final MediaType JIS_B2 = new MediaType(32, "JIS_B2"); //$NON-NLS-1$
        public static final MediaType JIS_B3 = new MediaType(33, "JIS_B3"); //$NON-NLS-1$
        public static final MediaType JIS_B4 = new MediaType(34, "JIS_B4"); //$NON-NLS-1$
        public static final MediaType JIS_B5 = new MediaType(35, "JIS_B5"); //$NON-NLS-1$
        public static final MediaType JIS_B6 = new MediaType(36, "JIS_B6"); //$NON-NLS-1$
        public static final MediaType JIS_B7 = new MediaType(37, "JIS_B7"); //$NON-NLS-1$
        public static final MediaType JIS_B8 = new MediaType(38, "JIS_B8"); //$NON-NLS-1$
        public static final MediaType JIS_B9 = new MediaType(39, "JIS_B9"); //$NON-NLS-1$
        public static final MediaType JIS_B10 = new MediaType(40, "JIS_B10"); //$NON-NLS-1$

        public static final MediaType ISO_C0 = new MediaType(50, "ISO_C0"); //$NON-NLS-1$
        public static final MediaType ISO_C1 = new MediaType(51, "ISO_C1"); //$NON-NLS-1$
        public static final MediaType ISO_C2 = new MediaType(52, "ISO_C2"); //$NON-NLS-1$
        public static final MediaType ISO_C3 = new MediaType(53, "ISO_C3"); //$NON-NLS-1$
        public static final MediaType ISO_C4 = new MediaType(54, "ISO_C4"); //$NON-NLS-1$
        public static final MediaType ISO_C5 = new MediaType(55, "ISO_C5"); //$NON-NLS-1$
        public static final MediaType ISO_C6 = new MediaType(56, "ISO_C6"); //$NON-NLS-1$
        public static final MediaType ISO_C7 = new MediaType(57, "ISO_C7"); //$NON-NLS-1$
        public static final MediaType ISO_C8 = new MediaType(58, "ISO_C8"); //$NON-NLS-1$
        public static final MediaType ISO_C9 = new MediaType(59, "ISO_C9"); //$NON-NLS-1$
        public static final MediaType ISO_C10 = new MediaType(60, "ISO_C10"); //$NON-NLS-1$

        public static final MediaType ISO_DESIGNATED_LONG = new MediaType(100,
                "ISO_DESIGNATED_LONG"); //$NON-NLS-1$

        public static final MediaType EXECUTIVE = new MediaType(101,
                "EXECUTIVE"); //$NON-NLS-1$

        public static final MediaType FOLIO = new MediaType(102, "FOLIO"); //$NON-NLS-1$

        public static final MediaType INVOICE = new MediaType(103, "INVOICE"); //$NON-NLS-1$

        public static final MediaType LEDGER = new MediaType(104, "LEDGER"); //$NON-NLS-1$

        public static final MediaType NA_LETTER = new MediaType(105,
                "NA_LETTER"); //$NON-NLS-1$

        public static final MediaType NA_LEGAL = new MediaType(106, "NA_LEGAL"); //$NON-NLS-1$

        public static final MediaType QUARTO = new MediaType(107, "QUARTO"); //$NON-NLS-1$

        public static final MediaType A = new MediaType(200, "A"); //$NON-NLS-1$
        public static final MediaType B = new MediaType(201, "B"); //$NON-NLS-1$
        public static final MediaType C = new MediaType(202, "C"); //$NON-NLS-1$
        public static final MediaType D = new MediaType(203, "D"); //$NON-NLS-1$
        public static final MediaType E = new MediaType(204, "E"); //$NON-NLS-1$

        public static final MediaType NA_10X15_ENVELOPE = new MediaType(311,
                "NA_10X15_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_10X14_ENVELOPE = new MediaType(310,
                "NA_10X14_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_10X13_ENVELOPE = new MediaType(309,
                "NA_10X13_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_9X12_ENVELOPE = new MediaType(308,
                "NA_9X12_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_9X11_ENVELOPE = new MediaType(307,
                "NA_9X11_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_7X9_ENVELOPE = new MediaType(306,
                "NA_7X9_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_6X9_ENVELOPE = new MediaType(305,
                "NA_6X9_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_NUMBER_9_ENVELOPE = new MediaType(
                312, "NA_NUMBER_9_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_NUMBER_10_ENVELOPE = new MediaType(
                313, "NA_NUMBER_10_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_NUMBER_11_ENVELOPE = new MediaType(
                314, "NA_NUMBER_11_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_NUMBER_12_ENVELOPE = new MediaType(
                315, "NA_NUMBER_12_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType NA_NUMBER_14_ENVELOPE = new MediaType(
                316, "NA_NUMBER_14_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType INVITE_ENVELOPE = new MediaType(300,
                "INVITE_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType ITALY_ENVELOPE = new MediaType(301,
                "ITALY_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType MONARCH_ENVELOPE = new MediaType(302,
                "MONARCH_ENVELOPE"); //$NON-NLS-1$
        public static final MediaType PERSONAL_ENVELOPE = new MediaType(304,
                "PERSONAL_ENVELOPE"); //$NON-NLS-1$

        /*aliases*/
        public static final MediaType A0 = ISO_A0;
        public static final MediaType A1 = ISO_A1;
        public static final MediaType A2 = ISO_A2;
        public static final MediaType A3 = ISO_A3;
        public static final MediaType A4 = ISO_A4;
        public static final MediaType A5 = ISO_A5;
        public static final MediaType A6 = ISO_A6;
        public static final MediaType A7 = ISO_A7;
        public static final MediaType A8 = ISO_A8;
        public static final MediaType A9 = ISO_A9;
        public static final MediaType A10 = ISO_A10;

        public static final MediaType B0 = ISO_B0;
        public static final MediaType B1 = ISO_B1;
        public static final MediaType B2 = ISO_B2;
        public static final MediaType B3 = ISO_B3;
        public static final MediaType B4 = ISO_B4;
        public static final MediaType B5 = ISO_B5;
        public static final MediaType B6 = ISO_B6;
        public static final MediaType B7 = ISO_B7;
        public static final MediaType B8 = ISO_B8;
        public static final MediaType B9 = ISO_B9;
        public static final MediaType B10 = ISO_B10;

        public static final MediaType ISO_B4_ENVELOPE = ISO_B4;
        public static final MediaType ISO_B5_ENVELOPE = ISO_B5;

        public static final MediaType ISO_C0_ENVELOPE = ISO_C0;
        public static final MediaType ISO_C1_ENVELOPE = ISO_C1;
        public static final MediaType ISO_C2_ENVELOPE = ISO_C2;
        public static final MediaType ISO_C3_ENVELOPE = ISO_C3;
        public static final MediaType ISO_C4_ENVELOPE = ISO_C4;
        public static final MediaType ISO_C5_ENVELOPE = ISO_C5;
        public static final MediaType ISO_C6_ENVELOPE = ISO_C6;
        public static final MediaType ISO_C7_ENVELOPE = ISO_C7;
        public static final MediaType ISO_C8_ENVELOPE = ISO_C8;
        public static final MediaType ISO_C9_ENVELOPE = ISO_C9;
        public static final MediaType ISO_C10_ENVELOPE = ISO_C10;


        public static final MediaType C0 = ISO_C0;
        public static final MediaType C1 = ISO_C1;
        public static final MediaType C2 = ISO_C2;
        public static final MediaType C3 = ISO_C3;
        public static final MediaType C4 = ISO_C4;
        public static final MediaType C5 = ISO_C5;
        public static final MediaType C6 = ISO_C6;
        public static final MediaType C7 = ISO_C7;
        public static final MediaType C8 = ISO_C8;
        public static final MediaType C9 = ISO_C9;
        public static final MediaType C10 = ISO_C10;

        public static final MediaType ISO_DESIGNATED_LONG_ENVELOPE
                                            = ISO_DESIGNATED_LONG;

        public static final MediaType STATEMENT = INVOICE;
        public static final MediaType TABLOID = LEDGER;
        public static final MediaType LETTER = NA_LETTER;
        public static final MediaType NOTE = NA_LETTER;
        public static final MediaType LEGAL = NA_LEGAL;

        public static final MediaType ENV_10X15 = NA_10X15_ENVELOPE;
        public static final MediaType ENV_10X14 = NA_10X14_ENVELOPE;
        public static final MediaType ENV_10X13 = NA_10X13_ENVELOPE;
        public static final MediaType ENV_9X12 = NA_9X12_ENVELOPE;
        public static final MediaType ENV_9X11 = NA_9X11_ENVELOPE;
        public static final MediaType ENV_7X9 = NA_7X9_ENVELOPE;
        public static final MediaType ENV_6X9 = NA_6X9_ENVELOPE;

        public static final MediaType ENV_9 = NA_NUMBER_9_ENVELOPE;
        public static final MediaType ENV_10 = NA_NUMBER_10_ENVELOPE;
        public static final MediaType ENV_11 = NA_NUMBER_11_ENVELOPE;
        public static final MediaType ENV_12 = NA_NUMBER_12_ENVELOPE;
        public static final MediaType ENV_14 = NA_NUMBER_14_ENVELOPE;

        public static final MediaType ENV_INVITE = INVITE_ENVELOPE;
        public static final MediaType ENV_ITALY = ITALY_ENVELOPE;
        public static final MediaType ENV_MONARCH = MONARCH_ENVELOPE;
        public static final MediaType ENV_PERSONAL = PERSONAL_ENVELOPE;
        public static final MediaType INVITE = INVITE_ENVELOPE;
        public static final MediaType ITALY = ITALY_ENVELOPE;
        public static final MediaType MONARCH = MONARCH_ENVELOPE;
        public static final MediaType PERSONAL = PERSONAL_ENVELOPE;

        private MediaType(int i, String n){
            super();
            name = n;
        }
        private MediaType(){
            this(4, "ISO_A4"); //$NON-NLS-1$
        }
    }

    public static final class OrientationRequestedType {
        private final String name;

        public static final OrientationRequestedType PORTRAIT
                            = new OrientationRequestedType(0, "PORTRAIT"); //$NON-NLS-1$
        public static final OrientationRequestedType LANDSCAPE
                            = new OrientationRequestedType(1, "LANDSCAPE"); //$NON-NLS-1$

        private OrientationRequestedType(int i, String n){
            super();
            name = n;
        }
        private OrientationRequestedType(){
            this(0, "PORTRAIT"); //$NON-NLS-1$
        }
    }

    public static final class OriginType {
        private final String name;

        public static final OriginType PHYSICAL
                            = new OriginType(0, "PHYSICAL"); //$NON-NLS-1$
        public static final OriginType PRINTABLE
                            = new OriginType(1, "PRINTABLE"); //$NON-NLS-1$

        private OriginType(int i, String n){
            super();
            name = n;
        }
        private OriginType(){
            this(0, "PHYSICAL"); //$NON-NLS-1$
        }
    }

    public static final class PrintQualityType {
        private final String name;

        public static final PrintQualityType HIGH = new PrintQualityType(5,
                "HIGH"); //$NON-NLS-1$
        public static final PrintQualityType NORMAL = new PrintQualityType(4,
                "NORMAL"); //$NON-NLS-1$
        public static final PrintQualityType DRAFT = new PrintQualityType(2,
                "DRAFT"); //$NON-NLS-1$

        private PrintQualityType(int i, String n){
            name = n;
        }

        private PrintQualityType(){
            this(4, "NORMAL"); //$NON-NLS-1$
        }
    }
 /*--------------- end of section of the nested classes ------------------*/

    public PageAttributes() {
        setColor(ColorType.MONOCHROME);
        setMediaToDefault();
        setOrientationRequestedToDefault();
        setOrigin(OriginType.PHYSICAL);
        setPrintQualityToDefault();
        setPrinterResolutionToDefault();
    }

    public PageAttributes(PageAttributes.ColorType color,
            PageAttributes.MediaType media,
            PageAttributes.OrientationRequestedType orientationRequested,
            PageAttributes.OriginType origin,
            PageAttributes.PrintQualityType printQuality,
            int[] printerResolution) {

        setColor(color);
        setMedia(media);
        setOrientationRequested(orientationRequested);
        setOrigin(origin);
        setPrintQuality(printQuality);
        setPrinterResolution(printerResolution);
    }

    public PageAttributes(PageAttributes pageAttributes) {
        set(pageAttributes);
    }

    @Override
    public Object clone() {
        PageAttributes pa = new PageAttributes(this);
        return pa;
    }

    @Override
    public boolean equals(Object obj) {
        PageAttributes pa;

        if (!(obj instanceof PageAttributes)) {
            return false;
        }
        pa = (PageAttributes) obj;
        if (color != pa.color) {
            return false;
        }
        if (media != pa.media) {
            return false;
        }
        if (orientationRequested != pa.orientationRequested) {
            return false;
        }
        if (origin != pa.origin) {
            return false;
        }
        if (printQuality != pa.printQuality) {
            return false;
        }
        if (origin != pa.origin) {
            return false;
        }
        if (!(printerResolution[0] == pa.printerResolution[0]
                && printerResolution[1] == pa.printerResolution[1]
                && printerResolution[2] == pa.printerResolution[2])) {
            return false;
        }
        return true;
    }

    @Override
    public String toString(){
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new PageAttributes());
         */

        String s;
        s = "color=" + getColor().name  + ",media=" + getMedia().name //$NON-NLS-1$ //$NON-NLS-2$
        + ",orientation-requested=" + getOrientationRequested().name //$NON-NLS-1$
        + ",origin=" + getOrigin().name //$NON-NLS-1$
        + ",print-quality=" + getPrintQuality().name //$NON-NLS-1$
        + ",printer-resolution=" + printerResolution[0] + "x" //$NON-NLS-1$ //$NON-NLS-2$
        + printerResolution[1] + (printerResolution[2]==3?"dpi":"dpcm"); //$NON-NLS-1$ //$NON-NLS-2$
        return s;
    }

    @Override
    public int hashCode() {
        int hash = this.toString().hashCode();
        return hash;
    }

    public void set(PageAttributes obj) {
        color = obj.color;
        media = obj.media;
        orientationRequested = obj.orientationRequested;
        origin = obj.origin;
        printQuality = obj.printQuality;
        printerResolution = obj.printerResolution;
    }

    public void setColor(PageAttributes.ColorType color) {
        if (color == null) {
            // awt.11C=Invalid value for color
            throw new IllegalArgumentException(Messages.getString("awt.11C")); //$NON-NLS-1$
        }
        this.color = color;
    }

    public PageAttributes.ColorType getColor() {
        return color;
    }

    public void setMedia(PageAttributes.MediaType media) {
        if(media == null) {
            // awt.116=Invalid value for media
            throw new IllegalArgumentException(Messages.getString("awt.116")); //$NON-NLS-1$
        }
        this.media = media;
    }

    public PageAttributes.MediaType getMedia() {
        return media;
    }

    public void setOrientationRequested(
            PageAttributes.OrientationRequestedType orientationRequested) {

        if (orientationRequested == null) {
            // awt.117=Invalid value for orientationRequested
            throw new IllegalArgumentException(Messages.getString("awt.117")); //$NON-NLS-1$
        }
        this.orientationRequested = orientationRequested;
    }

    public void setOrientationRequested(int i_orientationRequested) {
        if(i_orientationRequested == 3) {
            setOrientationRequested(OrientationRequestedType.PORTRAIT);
            return;
        }
        if(i_orientationRequested == 4) {
            setOrientationRequested(OrientationRequestedType.LANDSCAPE);
            return;
        }
        // awt.117=Invalid value for orientationRequested
        throw new IllegalArgumentException(Messages.getString("awt.117")); //$NON-NLS-1$
    }

    public PageAttributes.OrientationRequestedType getOrientationRequested() {
        return orientationRequested;
    }

    public void setOrigin(PageAttributes.OriginType origin) {
        if (origin == null) {
            // awt.119=Invalid value for origin
            throw new IllegalArgumentException(Messages.getString("awt.119")); //$NON-NLS-1$
        }
        this.origin = origin;
    }

    public PageAttributes.OriginType getOrigin() {
        return origin;
    }

    public void setPrintQuality(PageAttributes.PrintQualityType printQuality) {
        if (printQuality == null) {
            // awt.11A=Invalid value for printQuality
            throw new IllegalArgumentException(Messages.getString("awt.11A")); //$NON-NLS-1$
        }
        this.printQuality = printQuality;
    }

    public void setPrintQuality(int iprintQuality) {
        if (iprintQuality == 3) {
            setPrintQuality(PrintQualityType.DRAFT);
            return;
        }
        if (iprintQuality == 4) {
            setPrintQuality(PrintQualityType.NORMAL);
            return;
        }
        if (iprintQuality == 5) {
            setPrintQuality(PrintQualityType.HIGH);
            return;
        }
        // awt.11A=Invalid value for printQuality
        throw new IllegalArgumentException(Messages.getString("awt.11A")); //$NON-NLS-1$
    }

    public PageAttributes.PrintQualityType getPrintQuality() {
        return printQuality;
    }

    public void setPrinterResolution(int[] aprinterResolution) {
        if (aprinterResolution == null
                || aprinterResolution.length != 3
                || aprinterResolution[0] <= 0
                || aprinterResolution[1] <= 0
                || (aprinterResolution[2] != 3 && aprinterResolution[2] != 4)) {
            // awt.11B=Invalid value for printerResolution[]
            throw new IllegalArgumentException(Messages.getString("awt.11B")); //$NON-NLS-1$
        }
        printerResolution = new int[3];
        printerResolution[0] = aprinterResolution[0];
        printerResolution[1] = aprinterResolution[1];
        printerResolution[2] = aprinterResolution[2];
    }

    public void setPrinterResolution(int iprinterResolution) {
        if (iprinterResolution <= 0) {
            // awt.118=Invalid value for printerResolution
            throw new IllegalArgumentException(Messages.getString("awt.118")); //$NON-NLS-1$
        }
        printerResolution = new int[3];
        printerResolution[0] = iprinterResolution;
        printerResolution[1] = iprinterResolution;
        printerResolution[2] = 3;
    }

    public int[] getPrinterResolution() {
        return printerResolution;
    }

    public void setMediaToDefault() {
        Locale loc = Locale.getDefault();

        if (loc.equals(Locale.CANADA) || loc.equals(Locale.US)) {
            setMedia(MediaType.NA_LETTER);
            return;
        }
        setMedia(MediaType.ISO_A4);
        return;
    }

    public void setOrientationRequestedToDefault() {
        setOrientationRequested(OrientationRequestedType.PORTRAIT);
        return;
    }

   public void setPrintQualityToDefault() {
        setPrintQuality(PrintQualityType.NORMAL);
        return;
    }

    public void setPrinterResolutionToDefault() {
        setPrinterResolution(72);
        return;
    }
}
