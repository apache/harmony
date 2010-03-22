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

import javax.print.attribute.EnumSyntax;

/*
 * Table values are obtained from RFC2911: Internet Printing Protocol/1.1: 
 * Model and Semantics, Appendix C, http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public class MediaSizeName extends Media {
    private static final long serialVersionUID = 2778798329756942747L;

    public static final MediaSizeName ISO_A0 = new MediaSizeName(0);

    public static final MediaSizeName ISO_A1 = new MediaSizeName(1);

    public static final MediaSizeName ISO_A2 = new MediaSizeName(2);

    public static final MediaSizeName ISO_A3 = new MediaSizeName(3);

    public static final MediaSizeName ISO_A4 = new MediaSizeName(4);

    public static final MediaSizeName ISO_A5 = new MediaSizeName(5);

    public static final MediaSizeName ISO_A6 = new MediaSizeName(6);

    public static final MediaSizeName ISO_A7 = new MediaSizeName(7);

    public static final MediaSizeName ISO_A8 = new MediaSizeName(8);

    public static final MediaSizeName ISO_A9 = new MediaSizeName(9);

    public static final MediaSizeName ISO_A10 = new MediaSizeName(10);

    public static final MediaSizeName ISO_B0 = new MediaSizeName(11);

    public static final MediaSizeName ISO_B1 = new MediaSizeName(12);

    public static final MediaSizeName ISO_B2 = new MediaSizeName(13);

    public static final MediaSizeName ISO_B3 = new MediaSizeName(14);

    public static final MediaSizeName ISO_B4 = new MediaSizeName(15);

    public static final MediaSizeName ISO_B5 = new MediaSizeName(16);

    public static final MediaSizeName ISO_B6 = new MediaSizeName(17);

    public static final MediaSizeName ISO_B7 = new MediaSizeName(18);

    public static final MediaSizeName ISO_B8 = new MediaSizeName(19);

    public static final MediaSizeName ISO_B9 = new MediaSizeName(20);

    public static final MediaSizeName ISO_B10 = new MediaSizeName(21);

    public static final MediaSizeName JIS_B0 = new MediaSizeName(22);

    public static final MediaSizeName JIS_B1 = new MediaSizeName(23);

    public static final MediaSizeName JIS_B2 = new MediaSizeName(24);

    public static final MediaSizeName JIS_B3 = new MediaSizeName(25);

    public static final MediaSizeName JIS_B4 = new MediaSizeName(26);

    public static final MediaSizeName JIS_B5 = new MediaSizeName(27);

    public static final MediaSizeName JIS_B6 = new MediaSizeName(28);

    public static final MediaSizeName JIS_B7 = new MediaSizeName(29);

    public static final MediaSizeName JIS_B8 = new MediaSizeName(30);

    public static final MediaSizeName JIS_B9 = new MediaSizeName(31);

    public static final MediaSizeName JIS_B10 = new MediaSizeName(32);

    public static final MediaSizeName ISO_C0 = new MediaSizeName(33);

    public static final MediaSizeName ISO_C1 = new MediaSizeName(34);

    public static final MediaSizeName ISO_C2 = new MediaSizeName(35);

    public static final MediaSizeName ISO_C3 = new MediaSizeName(36);

    public static final MediaSizeName ISO_C4 = new MediaSizeName(37);

    public static final MediaSizeName ISO_C5 = new MediaSizeName(38);

    public static final MediaSizeName ISO_C6 = new MediaSizeName(39);

    public static final MediaSizeName NA_LETTER = new MediaSizeName(40);

    public static final MediaSizeName NA_LEGAL = new MediaSizeName(41);

    public static final MediaSizeName EXECUTIVE = new MediaSizeName(42);

    public static final MediaSizeName LEDGER = new MediaSizeName(43);

    public static final MediaSizeName TABLOID = new MediaSizeName(44);

    public static final MediaSizeName INVOICE = new MediaSizeName(45);

    public static final MediaSizeName FOLIO = new MediaSizeName(46);

    public static final MediaSizeName QUARTO = new MediaSizeName(47);

    public static final MediaSizeName JAPANESE_POSTCARD = new MediaSizeName(48);

    public static final MediaSizeName JAPANESE_DOUBLE_POSTCARD = new MediaSizeName(49);

    public static final MediaSizeName A = new MediaSizeName(50);

    public static final MediaSizeName B = new MediaSizeName(51);

    public static final MediaSizeName C = new MediaSizeName(52);

    public static final MediaSizeName D = new MediaSizeName(53);

    public static final MediaSizeName E = new MediaSizeName(54);

    public static final MediaSizeName ISO_DESIGNATED_LONG = new MediaSizeName(55);

    public static final MediaSizeName ITALY_ENVELOPE = new MediaSizeName(56);

    public static final MediaSizeName MONARCH_ENVELOPE = new MediaSizeName(57);

    public static final MediaSizeName PERSONAL_ENVELOPE = new MediaSizeName(58);

    public static final MediaSizeName NA_NUMBER_9_ENVELOPE = new MediaSizeName(59);

    public static final MediaSizeName NA_NUMBER_10_ENVELOPE = new MediaSizeName(60);

    public static final MediaSizeName NA_NUMBER_11_ENVELOPE = new MediaSizeName(61);

    public static final MediaSizeName NA_NUMBER_12_ENVELOPE = new MediaSizeName(62);

    public static final MediaSizeName NA_NUMBER_14_ENVELOPE = new MediaSizeName(63);

    public static final MediaSizeName NA_6X9_ENVELOPE = new MediaSizeName(64);

    public static final MediaSizeName NA_7X9_ENVELOPE = new MediaSizeName(65);

    public static final MediaSizeName NA_9X11_ENVELOPE = new MediaSizeName(66);

    public static final MediaSizeName NA_9X12_ENVELOPE = new MediaSizeName(67);

    public static final MediaSizeName NA_10X13_ENVELOPE = new MediaSizeName(68);

    public static final MediaSizeName NA_10X14_ENVELOPE = new MediaSizeName(69);

    public static final MediaSizeName NA_10X15_ENVELOPE = new MediaSizeName(70);

    public static final MediaSizeName NA_5X7 = new MediaSizeName(71);

    public static final MediaSizeName NA_8X10 = new MediaSizeName(72);

    private static final String[] stringTable = { "iso-a0", "iso-a1", "iso-a2", "iso-a3",
            "iso-a4", "iso-a5", "iso-a6", "iso-a7", "iso-a8", "iso-a9", "iso-a10", "iso-b0",
            "iso-b1", "iso-b2", "iso-b3", "iso-b4", "iso-b5", "iso-b6", "iso-b7", "iso-b8",
            "iso-b9", "iso-b10", "jis-b0", "jis-b1", "jis-b2", "jis-b3", "jis-b4", "jis-b5",
            "jis-b6", "jis-b7", "jis-b8", "jis-b9", "jis-b10", "iso-c0", "iso-c1", "iso-c2",
            "iso-c3", "iso-c4", "iso-c5", "iso-c6", "na-letter", "na-legal", "executive",
            "ledger", "tabloid", "invoice", "folio", "quarto", "japanese-postcard",
            "japanese-double-postcard", "a", "b", "c", "d", "e", "iso-designated-long",
            "italy-envelope", "monarch-envelope", "personal-envelope", "na-number-9-envelope",
            "na-number-10-envelope", "na-number-11-envelope", "na-number-12-envelope",
            "na-number-14-envelope", "na-6x9-envelope", "na-7x9-envelope", "na-9x11-envelope",
            "na-9x12-envelope", "na-10x13-envelope", "na-10x14-envelope", "na-10x15-envelope",
            "na-5x7", "na-8x10" };

    private static final MediaSizeName[] enumValueTable = { ISO_A0, ISO_A1, ISO_A2, ISO_A3,
            ISO_A4, ISO_A5, ISO_A6, ISO_A7, ISO_A8, ISO_A9, ISO_A10, ISO_B0, ISO_B1, ISO_B2,
            ISO_B3, ISO_B4, ISO_B5, ISO_B6, ISO_B7, ISO_B8, ISO_B9, ISO_B10, JIS_B0, JIS_B1,
            JIS_B2, JIS_B3, JIS_B4, JIS_B5, JIS_B6, JIS_B7, JIS_B8, JIS_B9, JIS_B10, ISO_C0,
            ISO_C1, ISO_C2, ISO_C3, ISO_C4, ISO_C5, ISO_C6, NA_LETTER, NA_LEGAL, EXECUTIVE,
            LEDGER, TABLOID, INVOICE, FOLIO, QUARTO, JAPANESE_POSTCARD,
            JAPANESE_DOUBLE_POSTCARD, A, B, C, D, E, ISO_DESIGNATED_LONG, ITALY_ENVELOPE,
            MONARCH_ENVELOPE, PERSONAL_ENVELOPE, NA_NUMBER_9_ENVELOPE, NA_NUMBER_10_ENVELOPE,
            NA_NUMBER_11_ENVELOPE, NA_NUMBER_12_ENVELOPE, NA_NUMBER_14_ENVELOPE,
            NA_6X9_ENVELOPE, NA_7X9_ENVELOPE, NA_9X11_ENVELOPE, NA_9X12_ENVELOPE,
            NA_10X13_ENVELOPE, NA_10X14_ENVELOPE, NA_10X15_ENVELOPE, NA_5X7, NA_8X10 };

    protected MediaSizeName(int value) {
        super(value);
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
