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

package org.apache.harmony.x.print.attributes;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

/*
 * The PPDMediaSizeName attribute is intended for supporting papers
 * returned by CUPS print servers.
 * 
 * Usually, when CUPS servers return list of supported papers, the names
 * of media are from PostScript Printer Description File Format Specification,
 * see http://partners.adobe.com/public/developer/en/ps/5003.PPD_Spec_v4.3.pdf
 * 
 * These media names are not part of Java 2 Platform, Standard Edition, API
 * Specification. Therefore, we are decided to add this attribute type to printing
 * API.
 * 
 * The class PPDMediaSizeName extends MediaSizeName class and has same
 * functionality.
 * 
 */
public class PPDMediaSizeName extends MediaSizeName {
    
    private static final long serialVersionUID = -2117680157822011363L;
    
    public static final PPDMediaSizeName s10x11 = new PPDMediaSizeName(0, 720, 792);
    public static final PPDMediaSizeName s10x13 = new PPDMediaSizeName(1, 720, 936);
    public static final PPDMediaSizeName s10x14 = new PPDMediaSizeName(2, 720, 1008);
    public static final PPDMediaSizeName s12x11 = new PPDMediaSizeName(3, 864, 792);
    public static final PPDMediaSizeName s15x11 = new PPDMediaSizeName(4, 1080, 792);
    public static final PPDMediaSizeName s7x9 = new PPDMediaSizeName(5, 504, 648);
    public static final PPDMediaSizeName s8x10 = new PPDMediaSizeName(6, 576, 720);
    public static final PPDMediaSizeName s9x11 = new PPDMediaSizeName(7, 648, 792);
    public static final PPDMediaSizeName s9x12 = new PPDMediaSizeName(8, 648, 864);
    public static final PPDMediaSizeName A0 = new PPDMediaSizeName(9, 2384, 3370);
    public static final PPDMediaSizeName A1 = new PPDMediaSizeName(10, 1684, 2384);
    public static final PPDMediaSizeName A2 = new PPDMediaSizeName(11, 1191, 1684);
    public static final PPDMediaSizeName A3 = new PPDMediaSizeName(12, 842, 1191);
    public static final PPDMediaSizeName A3_Transverse = new PPDMediaSizeName(13, 842, 1191);
    public static final PPDMediaSizeName A3Extra = new PPDMediaSizeName(14, 913, 1262);
    public static final PPDMediaSizeName A3Extra_Transverse = new PPDMediaSizeName(15, 913, 1262);
    public static final PPDMediaSizeName A3Rotated = new PPDMediaSizeName(16, 1191, 842);
    public static final PPDMediaSizeName A4 = new PPDMediaSizeName(17, 595, 842);
    public static final PPDMediaSizeName A4_Transverse = new PPDMediaSizeName(18, 595, 842);
    public static final PPDMediaSizeName A4Extra = new PPDMediaSizeName(19, 667, 914);
    public static final PPDMediaSizeName A4Plus = new PPDMediaSizeName(20, 595, 936);
    public static final PPDMediaSizeName A4Rotated = new PPDMediaSizeName(21, 842, 595);
    public static final PPDMediaSizeName A4Small = new PPDMediaSizeName(22, 595, 842);
    public static final PPDMediaSizeName A5 = new PPDMediaSizeName(23, 420, 595);
    public static final PPDMediaSizeName A5_Transverse = new PPDMediaSizeName(24, 420, 595);
    public static final PPDMediaSizeName A5Extra = new PPDMediaSizeName(25, 492, 668);
    public static final PPDMediaSizeName A5Rotated = new PPDMediaSizeName(26, 595, 420);
    public static final PPDMediaSizeName A6 = new PPDMediaSizeName(27, 297, 420);
    public static final PPDMediaSizeName A6Rotated = new PPDMediaSizeName(28, 420, 297);
    public static final PPDMediaSizeName A7 = new PPDMediaSizeName(29, 210, 297);
    public static final PPDMediaSizeName A8 = new PPDMediaSizeName(30, 148, 210);
    public static final PPDMediaSizeName A9 = new PPDMediaSizeName(31, 105, 148);
    public static final PPDMediaSizeName A10 = new PPDMediaSizeName(32, 73, 105);
    public static final PPDMediaSizeName AnsiC = new PPDMediaSizeName(33, 1224, 1584);
    public static final PPDMediaSizeName AnsiD = new PPDMediaSizeName(34, 1584, 2448);
    public static final PPDMediaSizeName AnsiE = new PPDMediaSizeName(35, 2448, 3168);
    public static final PPDMediaSizeName ARCHA = new PPDMediaSizeName(36, 648, 864);
    public static final PPDMediaSizeName ARCHB = new PPDMediaSizeName(37, 864, 1296);
    public static final PPDMediaSizeName ARCHC = new PPDMediaSizeName(38, 1296, 1728);
    public static final PPDMediaSizeName ARCHD = new PPDMediaSizeName(39, 1728, 2592);
    public static final PPDMediaSizeName ARCHE = new PPDMediaSizeName(40, 2592, 3456);
    public static final PPDMediaSizeName B0 = new PPDMediaSizeName(41, 2920, 4127);
    public static final PPDMediaSizeName B1 = new PPDMediaSizeName(42, 2064, 2920);
    public static final PPDMediaSizeName B2 = new PPDMediaSizeName(43, 1460, 2064);
    public static final PPDMediaSizeName B3 = new PPDMediaSizeName(44, 1032, 1460);
    public static final PPDMediaSizeName B4 = new PPDMediaSizeName(45, 729, 1032);
    public static final PPDMediaSizeName B4Rotated = new PPDMediaSizeName(46, 1032, 729);
    public static final PPDMediaSizeName B5 = new PPDMediaSizeName(47, 516, 729);
    public static final PPDMediaSizeName B5_Transverse = new PPDMediaSizeName(48, 516, 729);
    public static final PPDMediaSizeName B5Rotated = new PPDMediaSizeName(49, 729, 516);
    public static final PPDMediaSizeName B6 = new PPDMediaSizeName(50, 363, 516);
    public static final PPDMediaSizeName B6Rotated = new PPDMediaSizeName(51, 516, 363);
    public static final PPDMediaSizeName B7 = new PPDMediaSizeName(52, 258, 363);
    public static final PPDMediaSizeName B8 = new PPDMediaSizeName(53, 181, 258);
    public static final PPDMediaSizeName B9 = new PPDMediaSizeName(54, 127, 181);
    public static final PPDMediaSizeName B10 = new PPDMediaSizeName(55, 91, 127);
    public static final PPDMediaSizeName C4 = new PPDMediaSizeName(56, 649, 918);
    public static final PPDMediaSizeName C5 = new PPDMediaSizeName(57, 459, 649);
    public static final PPDMediaSizeName C6 = new PPDMediaSizeName(58, 323, 459);
    public static final PPDMediaSizeName Comm10 = new PPDMediaSizeName(59, 297, 684);
    public static final PPDMediaSizeName DL = new PPDMediaSizeName(60, 312, 624);
    public static final PPDMediaSizeName DoublePostcard = new PPDMediaSizeName(61, 567, (float) 419.5);
    public static final PPDMediaSizeName DoublePostcardRotated = new PPDMediaSizeName(62, (float) 419.5, 567);
    public static final PPDMediaSizeName Env9 = new PPDMediaSizeName(63, 279, 639);
    public static final PPDMediaSizeName Env10 = new PPDMediaSizeName(64, 297, 684);
    public static final PPDMediaSizeName Env11 = new PPDMediaSizeName(65, 324, 747);
    public static final PPDMediaSizeName Env12 = new PPDMediaSizeName(66, 342, 792);
    public static final PPDMediaSizeName Env14 = new PPDMediaSizeName(67, 360, 828);
    public static final PPDMediaSizeName EnvC0 = new PPDMediaSizeName(68, 2599, 3676);
    public static final PPDMediaSizeName EnvC1 = new PPDMediaSizeName(69, 1837, 2599);
    public static final PPDMediaSizeName EnvC2 = new PPDMediaSizeName(70, 1298, 1837);
    public static final PPDMediaSizeName EnvC3 = new PPDMediaSizeName(71, 918, 1296);
    public static final PPDMediaSizeName EnvC4 = new PPDMediaSizeName(72, 649, 918);
    public static final PPDMediaSizeName EnvC5 = new PPDMediaSizeName(73, 459, 649);
    public static final PPDMediaSizeName EnvC6 = new PPDMediaSizeName(74, 323, 459);
    public static final PPDMediaSizeName EnvC65 = new PPDMediaSizeName(75, 324, 648);
    public static final PPDMediaSizeName EnvC7 = new PPDMediaSizeName(76, 230, 323);
    public static final PPDMediaSizeName EnvChou3 = new PPDMediaSizeName(77, 340, 666);
    public static final PPDMediaSizeName EnvChou3Rotated = new PPDMediaSizeName(78, 666, 340);
    public static final PPDMediaSizeName EnvChou4 = new PPDMediaSizeName(79, 255, 581);
    public static final PPDMediaSizeName EnvChou4Rotated = new PPDMediaSizeName(80, 581, 255);
    public static final PPDMediaSizeName EnvDL = new PPDMediaSizeName(81, 312, 624);
    public static final PPDMediaSizeName EnvInvite = new PPDMediaSizeName(82, 624, 624);
    public static final PPDMediaSizeName EnvISOB4 = new PPDMediaSizeName(83, 708, 1001);
    public static final PPDMediaSizeName EnvISOB5 = new PPDMediaSizeName(84, 499, 709);
    public static final PPDMediaSizeName EnvISOB6 = new PPDMediaSizeName(85, 499, 354);
    public static final PPDMediaSizeName EnvItalian = new PPDMediaSizeName(86, 312, 652);
    public static final PPDMediaSizeName EnvKaku2 = new PPDMediaSizeName(87, 680, 941);
    public static final PPDMediaSizeName EnvKaku2Rotated = new PPDMediaSizeName(88, 941, 680);
    public static final PPDMediaSizeName EnvKaku3 = new PPDMediaSizeName(89, 612, 785);
    public static final PPDMediaSizeName EnvKaku3Rotated = new PPDMediaSizeName(90, 785, 612);
    public static final PPDMediaSizeName EnvMonarch = new PPDMediaSizeName(91, 279, 540);
    public static final PPDMediaSizeName EnvPersonal = new PPDMediaSizeName(92, 261, 468);
    public static final PPDMediaSizeName EnvPRC1 = new PPDMediaSizeName(93, 289, 468);
    public static final PPDMediaSizeName EnvPRC1Rotated = new PPDMediaSizeName(94, 468, 289);
    public static final PPDMediaSizeName EnvPRC2 = new PPDMediaSizeName(95, 289, 499);
    public static final PPDMediaSizeName EnvPRC2Rotated = new PPDMediaSizeName(96, 499, 289);
    public static final PPDMediaSizeName EnvPRC3 = new PPDMediaSizeName(97, 354, 499);
    public static final PPDMediaSizeName EnvPRC3Rotated = new PPDMediaSizeName(98, 499, 354);
    public static final PPDMediaSizeName EnvPRC4 = new PPDMediaSizeName(99, 312, 590);
    public static final PPDMediaSizeName EnvPRC4Rotated = new PPDMediaSizeName(100, 590, 312);
    public static final PPDMediaSizeName EnvPRC5 = new PPDMediaSizeName(101, 312, 624);
    public static final PPDMediaSizeName EnvPRC5Rotated = new PPDMediaSizeName(102, 624, 312);
    public static final PPDMediaSizeName EnvPRC6 = new PPDMediaSizeName(103, 340, 652);
    public static final PPDMediaSizeName EnvPRC6Rotated = new PPDMediaSizeName(104, 652, 340);
    public static final PPDMediaSizeName EnvPRC7 = new PPDMediaSizeName(105, 454, 652);
    public static final PPDMediaSizeName EnvPRC7Rotated = new PPDMediaSizeName(106, 652, 454);
    public static final PPDMediaSizeName EnvPRC8 = new PPDMediaSizeName(107, 340, 876);
    public static final PPDMediaSizeName EnvPRC8Rotated = new PPDMediaSizeName(108, 876, 340);
    public static final PPDMediaSizeName EnvPRC9 = new PPDMediaSizeName(109, 649, 918);
    public static final PPDMediaSizeName EnvPRC9Rotated = new PPDMediaSizeName(110, 918, 649);
    public static final PPDMediaSizeName EnvPRC10 = new PPDMediaSizeName(111, 918, 1298);
    public static final PPDMediaSizeName EnvPRC10Rotated = new PPDMediaSizeName(112, 1298, 918);
    public static final PPDMediaSizeName EnvYou4 = new PPDMediaSizeName(113, 298, 666);
    public static final PPDMediaSizeName EnvYou4Rotated = new PPDMediaSizeName(114, 666, 298);
    public static final PPDMediaSizeName Executive = new PPDMediaSizeName(115, 522, 756);
    public static final PPDMediaSizeName FanFoldUS = new PPDMediaSizeName(116, 1071, 792);
    public static final PPDMediaSizeName FanFoldGerman = new PPDMediaSizeName(117, 612, 864);
    public static final PPDMediaSizeName FanFoldGermanLegal = new PPDMediaSizeName(118, 612, 936);
    public static final PPDMediaSizeName Folio = new PPDMediaSizeName(119, 595, 935);
    public static final PPDMediaSizeName ISOB0 = new PPDMediaSizeName(120, 2835, 4008);
    public static final PPDMediaSizeName ISOB1 = new PPDMediaSizeName(121, 2004, 2835);
    public static final PPDMediaSizeName ISOB2 = new PPDMediaSizeName(122, 1417, 2004);
    public static final PPDMediaSizeName ISOB3 = new PPDMediaSizeName(123, 1001, 1417);
    public static final PPDMediaSizeName ISOB4 = new PPDMediaSizeName(124, 709, 1001);
    public static final PPDMediaSizeName ISOB5 = new PPDMediaSizeName(125, 499, 709);
    public static final PPDMediaSizeName ISOB5Extra = new PPDMediaSizeName(126, (float) 569.7, 782);
    public static final PPDMediaSizeName ISOB6 = new PPDMediaSizeName(127, 354, 499);
    public static final PPDMediaSizeName ISOB7 = new PPDMediaSizeName(128, 249, 354);
    public static final PPDMediaSizeName ISOB8 = new PPDMediaSizeName(129, 176, 249);
    public static final PPDMediaSizeName ISOB9 = new PPDMediaSizeName(130, 125, 176);
    public static final PPDMediaSizeName ISOB10 = new PPDMediaSizeName(131, 88, 125);
    public static final PPDMediaSizeName Ledger = new PPDMediaSizeName(132, 1224, 792);
    public static final PPDMediaSizeName Legal = new PPDMediaSizeName(133, 612, 1008);
    public static final PPDMediaSizeName LegalExtra = new PPDMediaSizeName(134, 684, 1080);
    public static final PPDMediaSizeName Letter = new PPDMediaSizeName(135, 612, 792);
    public static final PPDMediaSizeName Letter_Transverse = new PPDMediaSizeName(136, 612, 792);
    public static final PPDMediaSizeName LetterExtra = new PPDMediaSizeName(137, 684, 864);
    public static final PPDMediaSizeName LetterExtra_Transverse = new PPDMediaSizeName(138, 684, 864);
    public static final PPDMediaSizeName LetterPlus = new PPDMediaSizeName(139, 612, (float) 913.7);
    public static final PPDMediaSizeName LetterRotated = new PPDMediaSizeName(140, 792, 612);
    public static final PPDMediaSizeName LetterSmall = new PPDMediaSizeName(141, 612, 792);
    public static final PPDMediaSizeName Monarch = new PPDMediaSizeName(142, 279, 540);
    public static final PPDMediaSizeName Note = new PPDMediaSizeName(143, 612, 792);
    public static final PPDMediaSizeName Postcard = new PPDMediaSizeName(144, 284, 419);
    public static final PPDMediaSizeName PostcardRotated = new PPDMediaSizeName(145, 419, 284);
    public static final PPDMediaSizeName PRC16K = new PPDMediaSizeName(146, 414, 610);
    public static final PPDMediaSizeName PRC16KRotated = new PPDMediaSizeName(147, 610, 414);
    public static final PPDMediaSizeName PRC32K = new PPDMediaSizeName(148, 275, 428);
    public static final PPDMediaSizeName PRC32KBig = new PPDMediaSizeName(149, 275, 428);
    public static final PPDMediaSizeName PRC32KBigRotated = new PPDMediaSizeName(150, 428, 275);
    public static final PPDMediaSizeName PRC32KRotated = new PPDMediaSizeName(151, 428, 275);
    public static final PPDMediaSizeName Quarto = new PPDMediaSizeName(152, 610, 780);
    public static final PPDMediaSizeName Statement = new PPDMediaSizeName(153, 396, 612);
    public static final PPDMediaSizeName SuperA = new PPDMediaSizeName(154, 643, 1009);
    public static final PPDMediaSizeName SuperB = new PPDMediaSizeName(155, 864, 1380);
    public static final PPDMediaSizeName Tabloid = new PPDMediaSizeName(156, 792, 1224);
    public static final PPDMediaSizeName TabloidExtra = new PPDMediaSizeName(157, 864, 1296);

    private static final String[] stringTable = { "10x11",
            "10x13",
            "10x14",
            "12x11",
            "15x11",
            "7x9",
            "8x10",
            "9x11",
            "9x12",
            "A0",
            "A1",
            "A2",
            "A3",
            "A3.Transverse",
            "A3Extra",
            "A3Extra.Transverse",
            "A3Rotated",
            "A4",
            "A4.Transverse",
            "A4Extra",
            "A4Plus",
            "A4Rotated",
            "A4Small",
            "A5",
            "A5.Transverse",
            "A5Extra",
            "A5Rotated",
            "A6",
            "A6Rotated",
            "A7",
            "A8",
            "A9",
            "A10",
            "AnsiC",
            "AnsiD",
            "AnsiE",
            "ARCHA",
            "ARCHB",
            "ARCHC",
            "ARCHD",
            "ARCHE",
            "B0",
            "B1",
            "B2",
            "B3",
            "B4",
            "B4Rotated",
            "B5",
            "B5.Transverse",
            "B5Rotated",
            "B6",
            "B6Rotated",
            "B7",
            "B8",
            "B9",
            "B10",
            "C4",
            "C5",
            "C6",
            "Comm10",
            "DL",
            "DoublePostcard",
            "DoublePostcardRotated",
            "Env9",
            "Env10",
            "Env11",
            "Env12",
            "Env14",
            "EnvC0",
            "EnvC1",
            "EnvC2",
            "EnvC3",
            "EnvC4",
            "EnvC5",
            "EnvC6",
            "EnvC65",
            "EnvC7",
            "EnvChou3",
            "EnvChou3Rotated",
            "EnvChou4",
            "EnvChou4Rotated",
            "EnvDL",
            "EnvInvite",
            "EnvISOB4",
            "EnvISOB5",
            "EnvISOB6",
            "EnvItalian",
            "EnvKaku2",
            "EnvKaku2Rotated",
            "EnvKaku3",
            "EnvKaku3Rotated",
            "EnvMonarch",
            "EnvPersonal",
            "EnvPRC1",
            "EnvPRC1Rotated",
            "EnvPRC2",
            "EnvPRC2Rotated",
            "EnvPRC3",
            "EnvPRC3Rotated",
            "EnvPRC4",
            "EnvPRC4Rotated",
            "EnvPRC5",
            "EnvPRC5Rotated",
            "EnvPRC6",
            "EnvPRC6Rotated",
            "EnvPRC7",
            "EnvPRC7Rotated",
            "EnvPRC8",
            "EnvPRC8Rotated",
            "EnvPRC9",
            "EnvPRC9Rotated",
            "EnvPRC10",
            "EnvPRC10Rotated",
            "EnvYou4",
            "EnvYou4Rotated",
            "Executive",
            "FanFoldUS",
            "FanFoldGerman",
            "FanFoldGermanLegal",
            "Folio",
            "ISOB0",
            "ISOB1",
            "ISOB2",
            "ISOB3",
            "ISOB4",
            "ISOB5",
            "ISOB5Extra",
            "ISOB6",
            "ISOB7",
            "ISOB8",
            "ISOB9",
            "ISOB10",
            "Ledger",
            "Legal",
            "LegalExtra",
            "Letter",
            "Letter.Transverse",
            "LetterExtra",
            "LetterExtra.Transverse",
            "LetterPlus",
            "LetterRotated",
            "LetterSmall",
            "Monarch",
            "Note",
            "Postcard",
            "PostcardRotated",
            "PRC16K",
            "PRC16KRotated",
            "PRC32K",
            "PRC32KBig",
            "PRC32KBigRotated",
            "PRC32KRotated",
            "Quarto",
            "Statement",
            "SuperA",
            "SuperB",
            "Tabloid",
            "TabloidExtra" };

    private static final PPDMediaSizeName[] enumValueTable = { s10x11,
            s10x13,
            s10x14,
            s12x11,
            s15x11,
            s7x9,
            s8x10,
            s9x11,
            s9x12,
            A0,
            A1,
            A2,
            A3,
            A3_Transverse,
            A3Extra,
            A3Extra_Transverse,
            A3Rotated,
            A4,
            A4_Transverse,
            A4Extra,
            A4Plus,
            A4Rotated,
            A4Small,
            A5,
            A5_Transverse,
            A5Extra,
            A5Rotated,
            A6,
            A6Rotated,
            A7,
            A8,
            A9,
            A10,
            AnsiC,
            AnsiD,
            AnsiE,
            ARCHA,
            ARCHB,
            ARCHC,
            ARCHD,
            ARCHE,
            B0,
            B1,
            B2,
            B3,
            B4,
            B4Rotated,
            B5,
            B5_Transverse,
            B5Rotated,
            B6,
            B6Rotated,
            B7,
            B8,
            B9,
            B10,
            C4,
            C5,
            C6,
            Comm10,
            DL,
            DoublePostcard,
            DoublePostcardRotated,
            Env9,
            Env10,
            Env11,
            Env12,
            Env14,
            EnvC0,
            EnvC1,
            EnvC2,
            EnvC3,
            EnvC4,
            EnvC5,
            EnvC6,
            EnvC65,
            EnvC7,
            EnvChou3,
            EnvChou3Rotated,
            EnvChou4,
            EnvChou4Rotated,
            EnvDL,
            EnvInvite,
            EnvISOB4,
            EnvISOB5,
            EnvISOB6,
            EnvItalian,
            EnvKaku2,
            EnvKaku2Rotated,
            EnvKaku3,
            EnvKaku3Rotated,
            EnvMonarch,
            EnvPersonal,
            EnvPRC1,
            EnvPRC1Rotated,
            EnvPRC2,
            EnvPRC2Rotated,
            EnvPRC3,
            EnvPRC3Rotated,
            EnvPRC4,
            EnvPRC4Rotated,
            EnvPRC5,
            EnvPRC5Rotated,
            EnvPRC6,
            EnvPRC6Rotated,
            EnvPRC7,
            EnvPRC7Rotated,
            EnvPRC8,
            EnvPRC8Rotated,
            EnvPRC9,
            EnvPRC9Rotated,
            EnvPRC10,
            EnvPRC10Rotated,
            EnvYou4,
            EnvYou4Rotated,
            Executive,
            FanFoldUS,
            FanFoldGerman,
            FanFoldGermanLegal,
            Folio,
            ISOB0,
            ISOB1,
            ISOB2,
            ISOB3,
            ISOB4,
            ISOB5,
            ISOB5Extra,
            ISOB6,
            ISOB7,
            ISOB8,
            ISOB9,
            ISOB10,
            Ledger,
            Legal,
            LegalExtra,
            Letter,
            Letter_Transverse,
            LetterExtra,
            LetterExtra_Transverse,
            LetterPlus,
            LetterRotated,
            LetterSmall,
            Monarch,
            Note,
            Postcard,
            PostcardRotated,
            PRC16K,
            PRC16KRotated,
            PRC32K,
            PRC32KBig,
            PRC32KBigRotated,
            PRC32KRotated,
            Quarto,
            Statement,
            SuperA,
            SuperB,
            Tabloid,
            TabloidExtra };

    protected PPDMediaSizeName(int value) {
        super(value);
    }

    // unit == 1/72 inch
    protected PPDMediaSizeName(int value, float x, float y) {
        super(value);
        if (x > y) {
            float z = x;
            y = x;
            x = z;
        }
        new MediaSize(x / 72, y / 72, Size2DSyntax.INCH, this);
    }

    public EnumSyntax[] getEnumValueTable() {
        return (EnumSyntax[]) enumValueTable.clone();
    }

    public String[] getStringTable() {
        return (String[]) stringTable.clone();
    }

    public PPDMediaSizeName getPPDMediaSizeNameByName(String name) {
        for (int i = 0, ii = stringTable.length; i < ii; i++) {
            if (name.equals(stringTable[i])) {
                return enumValueTable[i];
            }
        }
        return null;
    }
}
