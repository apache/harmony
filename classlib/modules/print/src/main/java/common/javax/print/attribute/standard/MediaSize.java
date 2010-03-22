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

package javax.print.attribute.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.print.attribute.Attribute;
import javax.print.attribute.Size2DSyntax;

@SuppressWarnings("unused")
public class MediaSize extends Size2DSyntax implements Attribute {
    private static final long serialVersionUID = -1967958664615414771L;

    private static final List<MediaSize> sizesList = new ArrayList<MediaSize>(80);

    private static final Map<MediaSizeName, MediaSize> mediaSizeMap = new HashMap<MediaSizeName, MediaSize>(
            80, 1);

    public static final class Engineering {
        private Engineering() {
            super();
        }

        public static final MediaSize A = new MediaSize(8.5f, 11.0f, Size2DSyntax.INCH,
                MediaSizeName.A);

        public static final MediaSize B = new MediaSize(11, 17, Size2DSyntax.INCH,
                MediaSizeName.B);

        public static final MediaSize C = new MediaSize(17, 22, Size2DSyntax.INCH,
                MediaSizeName.C);

        public static final MediaSize D = new MediaSize(22, 34, Size2DSyntax.INCH,
                MediaSizeName.D);

        public static final MediaSize E = new MediaSize(34, 44, Size2DSyntax.INCH,
                MediaSizeName.E);
    }

    public static final class ISO {
        private ISO() {
            super();
        }

        public static final MediaSize A0 = new MediaSize(841, 1189, Size2DSyntax.MM,
                MediaSizeName.ISO_A0);

        public static final MediaSize A1 = new MediaSize(594, 841, Size2DSyntax.MM,
                MediaSizeName.ISO_A1);

        public static final MediaSize A2 = new MediaSize(420, 594, Size2DSyntax.MM,
                MediaSizeName.ISO_A2);

        public static final MediaSize A3 = new MediaSize(297, 420, Size2DSyntax.MM,
                MediaSizeName.ISO_A3);

        public static final MediaSize A4 = new MediaSize(210, 297, Size2DSyntax.MM,
                MediaSizeName.ISO_A4);

        public static final MediaSize A5 = new MediaSize(148, 210, Size2DSyntax.MM,
                MediaSizeName.ISO_A5);

        public static final MediaSize A6 = new MediaSize(105, 148, Size2DSyntax.MM,
                MediaSizeName.ISO_A6);

        public static final MediaSize A7 = new MediaSize(74, 105, Size2DSyntax.MM,
                MediaSizeName.ISO_A7);

        public static final MediaSize A8 = new MediaSize(52, 74, Size2DSyntax.MM,
                MediaSizeName.ISO_A8);

        public static final MediaSize A9 = new MediaSize(37, 52, Size2DSyntax.MM,
                MediaSizeName.ISO_A9);

        public static final MediaSize A10 = new MediaSize(26, 37, Size2DSyntax.MM,
                MediaSizeName.ISO_A10);

        public static final MediaSize B0 = new MediaSize(1000, 1414, Size2DSyntax.MM,
                MediaSizeName.ISO_B0);

        public static final MediaSize B1 = new MediaSize(707, 1000, Size2DSyntax.MM,
                MediaSizeName.ISO_B1);

        public static final MediaSize B2 = new MediaSize(500, 707, Size2DSyntax.MM,
                MediaSizeName.ISO_B2);

        public static final MediaSize B3 = new MediaSize(353, 500, Size2DSyntax.MM,
                MediaSizeName.ISO_B3);

        public static final MediaSize B4 = new MediaSize(250, 353, Size2DSyntax.MM,
                MediaSizeName.ISO_B4);

        public static final MediaSize B5 = new MediaSize(176, 250, Size2DSyntax.MM,
                MediaSizeName.ISO_B5);

        public static final MediaSize B6 = new MediaSize(125, 176, Size2DSyntax.MM,
                MediaSizeName.ISO_B6);

        public static final MediaSize B7 = new MediaSize(88, 125, Size2DSyntax.MM,
                MediaSizeName.ISO_B7);

        public static final MediaSize B8 = new MediaSize(62, 88, Size2DSyntax.MM,
                MediaSizeName.ISO_B8);

        public static final MediaSize B9 = new MediaSize(44, 62, Size2DSyntax.MM,
                MediaSizeName.ISO_B9);

        public static final MediaSize B10 = new MediaSize(31, 44, Size2DSyntax.MM,
                MediaSizeName.ISO_B10);

        public static final MediaSize C3 = new MediaSize(324, 458, Size2DSyntax.MM,
                MediaSizeName.ISO_C3);

        public static final MediaSize C4 = new MediaSize(229, 324, Size2DSyntax.MM,
                MediaSizeName.ISO_C4);

        public static final MediaSize C5 = new MediaSize(162, 229, Size2DSyntax.MM,
                MediaSizeName.ISO_C5);

        public static final MediaSize C6 = new MediaSize(114, 162, Size2DSyntax.MM,
                MediaSizeName.ISO_C6);

        public static final MediaSize DESIGNATED_LONG = new MediaSize(110, 220,
                Size2DSyntax.MM, MediaSizeName.ISO_DESIGNATED_LONG);
    }

    public static final class JIS {
        private JIS() {
            super();
        }

        public static final MediaSize B0 = new MediaSize(1030, 1456, Size2DSyntax.MM,
                MediaSizeName.JIS_B0);

        public static final MediaSize B1 = new MediaSize(728, 1030, Size2DSyntax.MM,
                MediaSizeName.JIS_B1);

        public static final MediaSize B2 = new MediaSize(515, 728, Size2DSyntax.MM,
                MediaSizeName.JIS_B2);

        public static final MediaSize B3 = new MediaSize(364, 515, Size2DSyntax.MM,
                MediaSizeName.JIS_B3);

        public static final MediaSize B4 = new MediaSize(257, 364, Size2DSyntax.MM,
                MediaSizeName.JIS_B4);

        public static final MediaSize B5 = new MediaSize(182, 257, Size2DSyntax.MM,
                MediaSizeName.JIS_B5);

        public static final MediaSize B6 = new MediaSize(128, 182, Size2DSyntax.MM,
                MediaSizeName.JIS_B6);

        public static final MediaSize B7 = new MediaSize(91, 128, Size2DSyntax.MM,
                MediaSizeName.JIS_B7);

        public static final MediaSize B8 = new MediaSize(64, 91, Size2DSyntax.MM,
                MediaSizeName.JIS_B8);

        public static final MediaSize B9 = new MediaSize(45, 64, Size2DSyntax.MM,
                MediaSizeName.JIS_B9);

        public static final MediaSize B10 = new MediaSize(32, 45, Size2DSyntax.MM,
                MediaSizeName.JIS_B10);

        public static final MediaSize CHOU_1 = new MediaSize(142, 332, Size2DSyntax.MM);

        public static final MediaSize CHOU_2 = new MediaSize(119, 277, Size2DSyntax.MM);

        public static final MediaSize CHOU_3 = new MediaSize(120, 235, Size2DSyntax.MM);

        public static final MediaSize CHOU_4 = new MediaSize(90, 205, Size2DSyntax.MM);

        public static final MediaSize CHOU_30 = new MediaSize(92, 235, Size2DSyntax.MM);

        public static final MediaSize CHOU_40 = new MediaSize(90, 225, Size2DSyntax.MM);

        public static final MediaSize KAKU_0 = new MediaSize(287, 382, Size2DSyntax.MM);

        public static final MediaSize KAKU_1 = new MediaSize(270, 382, Size2DSyntax.MM);

        public static final MediaSize KAKU_2 = new MediaSize(240, 332, Size2DSyntax.MM);

        public static final MediaSize KAKU_3 = new MediaSize(216, 277, Size2DSyntax.MM);

        public static final MediaSize KAKU_4 = new MediaSize(197, 267, Size2DSyntax.MM);

        public static final MediaSize KAKU_5 = new MediaSize(190, 240, Size2DSyntax.MM);

        public static final MediaSize KAKU_6 = new MediaSize(162, 229, Size2DSyntax.MM);

        public static final MediaSize KAKU_7 = new MediaSize(142, 205, Size2DSyntax.MM);

        public static final MediaSize KAKU_8 = new MediaSize(119, 197, Size2DSyntax.MM);

        public static final MediaSize KAKU_20 = new MediaSize(229, 324, Size2DSyntax.MM);

        public static final MediaSize KAKU_A4 = new MediaSize(228, 312, Size2DSyntax.MM);

        public static final MediaSize YOU_1 = new MediaSize(120, 176, Size2DSyntax.MM);

        public static final MediaSize YOU_2 = new MediaSize(114, 162, Size2DSyntax.MM);

        public static final MediaSize YOU_3 = new MediaSize(98, 148, Size2DSyntax.MM);

        public static final MediaSize YOU_4 = new MediaSize(105, 235, Size2DSyntax.MM);

        public static final MediaSize YOU_5 = new MediaSize(95, 217, Size2DSyntax.MM);

        public static final MediaSize YOU_6 = new MediaSize(98, 190, Size2DSyntax.MM);

        public static final MediaSize YOU_7 = new MediaSize(92, 165, Size2DSyntax.MM);
    }

    public static final class NA {
        private NA() {
            super();
        }

        public static final MediaSize LETTER = new MediaSize(8.5f, 11.0f, Size2DSyntax.INCH,
                MediaSizeName.NA_LETTER);

        public static final MediaSize LEGAL = new MediaSize(8.5f, 14.0f, Size2DSyntax.INCH,
                MediaSizeName.NA_LEGAL);

        public static final MediaSize NA_5X7 = new MediaSize(5, 7, Size2DSyntax.INCH,
                MediaSizeName.NA_5X7);

        public static final MediaSize NA_8X10 = new MediaSize(8, 10, Size2DSyntax.INCH,
                MediaSizeName.NA_8X10);

        public static final MediaSize NA_NUMBER_9_ENVELOPE = new MediaSize(3.875f, 8.875f,
                Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_9_ENVELOPE);

        public static final MediaSize NA_NUMBER_10_ENVELOPE = new MediaSize(4.125f, 9.5f,
                Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_10_ENVELOPE);

        public static final MediaSize NA_NUMBER_11_ENVELOPE = new MediaSize(4.5f, 10.375f,
                Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_11_ENVELOPE);

        public static final MediaSize NA_NUMBER_12_ENVELOPE = new MediaSize(4.75f, 11.0f,
                Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_12_ENVELOPE);

        public static final MediaSize NA_NUMBER_14_ENVELOPE = new MediaSize(5.0f, 11.5f,
                Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_14_ENVELOPE);

        public static final MediaSize NA_6X9_ENVELOPE = new MediaSize(6, 9, Size2DSyntax.INCH,
                MediaSizeName.NA_6X9_ENVELOPE);

        public static final MediaSize NA_7X9_ENVELOPE = new MediaSize(7, 9, Size2DSyntax.INCH,
                MediaSizeName.NA_7X9_ENVELOPE);

        public static final MediaSize NA_9x11_ENVELOPE = new MediaSize(9, 11,
                Size2DSyntax.INCH, MediaSizeName.NA_9X11_ENVELOPE);

        public static final MediaSize NA_9x12_ENVELOPE = new MediaSize(9, 12,
                Size2DSyntax.INCH, MediaSizeName.NA_9X12_ENVELOPE);

        public static final MediaSize NA_10x13_ENVELOPE = new MediaSize(10, 13,
                Size2DSyntax.INCH, MediaSizeName.NA_10X13_ENVELOPE);

        public static final MediaSize NA_10x14_ENVELOPE = new MediaSize(10, 14,
                Size2DSyntax.INCH, MediaSizeName.NA_10X14_ENVELOPE);

        public static final MediaSize NA_10X15_ENVELOPE = new MediaSize(10, 15,
                Size2DSyntax.INCH, MediaSizeName.NA_10X15_ENVELOPE);
    }

    public static final class Other {
        private Other() {
            super();
        }

        public static final MediaSize EXECUTIVE = new MediaSize(7.25f, 10.5f,
                Size2DSyntax.INCH, MediaSizeName.EXECUTIVE);

        public static final MediaSize LEDGER = new MediaSize(11, 17, Size2DSyntax.INCH,
                MediaSizeName.LEDGER);

        public static final MediaSize TABLOID = new MediaSize(11, 17, Size2DSyntax.INCH,
                MediaSizeName.TABLOID);

        public static final MediaSize INVOICE = new MediaSize(5.5f, 8.5f, Size2DSyntax.INCH,
                MediaSizeName.INVOICE);

        public static final MediaSize FOLIO = new MediaSize(8.5f, 13.0f, Size2DSyntax.INCH,
                MediaSizeName.FOLIO);

        public static final MediaSize QUARTO = new MediaSize(8.5f, 10.83f, Size2DSyntax.INCH,
                MediaSizeName.QUARTO);

        public static final MediaSize ITALY_ENVELOPE = new MediaSize(110, 230, Size2DSyntax.MM,
                MediaSizeName.ITALY_ENVELOPE);

        public static final MediaSize MONARCH_ENVELOPE = new MediaSize(3.87f, 7.5f,
                Size2DSyntax.INCH, MediaSizeName.MONARCH_ENVELOPE);

        public static final MediaSize PERSONAL_ENVELOPE = new MediaSize(3.625f, 6.5f,
                Size2DSyntax.INCH, MediaSizeName.PERSONAL_ENVELOPE);

        public static final MediaSize JAPANESE_POSTCARD = new MediaSize(100, 148,
                Size2DSyntax.MM, MediaSizeName.JAPANESE_POSTCARD);

        public static final MediaSize JAPANESE_DOUBLE_POSTCARD = new MediaSize(148, 200,
                Size2DSyntax.MM, MediaSizeName.JAPANESE_DOUBLE_POSTCARD);
    }

    /*
     * Force the load and initialization of inner classes.
     */
    static {
        
        MediaSize initClass = MediaSize.Engineering.A;
        initClass = MediaSize.ISO.A0;
        initClass = MediaSize.JIS.B0;
        initClass = MediaSize.NA.LEGAL;
        initClass = MediaSize.Other.EXECUTIVE;
    }

    private MediaSizeName mediaSizeName;

    public MediaSize(int x, int y, int units) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("x > y");
        }
        synchronized (MediaSize.class) {
            sizesList.add(this);
        }
    }

    public MediaSize(float x, float y, int units) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("x > y");
        }
        synchronized (MediaSize.class) {
            sizesList.add(this);
        }
    }

    public MediaSize(int x, int y, int units, MediaSizeName mediaSizeName) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("x > y");
        }
        this.mediaSizeName = mediaSizeName;
        synchronized (MediaSize.class) {
            sizesList.add(this);
            mediaSizeMap.put(mediaSizeName, this);
        }
    }

    public MediaSize(float x, float y, int units, MediaSizeName mediaSizeName) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("x > y");
        }
        this.mediaSizeName = mediaSizeName;
        synchronized (MediaSize.class) {
            sizesList.add(this);
            mediaSizeMap.put(mediaSizeName, this);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MediaSize)) {
            return false;
        }
        return super.equals(object);
    }

    public static MediaSizeName findMedia(float x, float y, int units) {
        if ((x <= 0.0) || (y <= 0.0) || (units < 1)) {
            throw new IllegalArgumentException("Valid values are: "
                    + "x > 0, y > 0, units >= 1");
        }
        synchronized (MediaSize.class) {
            MediaSize hit = null;
            double curX, curY, curDif;
            double difference = Double.MAX_VALUE;
            Iterator<MediaSize> i = sizesList.iterator();
            while (i.hasNext()) {
                MediaSize mediaSize = i.next();
                curX = mediaSize.getX(units);
                curY = mediaSize.getY(units);
                if ((x == curX) && (y == curY)) {
                    hit = mediaSize;
                    break;
                }
                curDif = curX * curX - 2 * x * curX - 2 * y * curY + curY * curY;
                if (curDif <= difference) {
                    difference = curDif;
                    hit = mediaSize;
                }
            }
            if (hit != null) {
                return hit.getMediaSizeName();
            }
            return null;
        }
    }

    public final Class<? extends Attribute> getCategory() {
        return MediaSize.class;
    }

    public static MediaSize getMediaSizeForName(MediaSizeName mediaSizeName) {
        synchronized (MediaSize.class) {
            return mediaSizeMap.get(mediaSizeName);
        }
    }

    public MediaSizeName getMediaSizeName() {
        return mediaSizeName;
    }

    public final String getName() {
        return "media-size";
    }
}
