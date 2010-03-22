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

package org.apache.harmony.x.swing.text.rtf;

/**
 * This class contains methods for converting RTF code pages to Java encodings and vice versa.
 *
 * @author Aleksey Lagoshin
 */
public class RTFEncodings {

  /**
   * The default encoding for RTF files, it is also used for unsupported code pages.
   */
  public static final String DEFAULT_ENCODING = "Cp1252";

  /**
   * Returns an encoding name for Java corresponding to parsed code page.
   * For all unsupported code pages this method returns the default encoding
   * (see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">this</a> page)
   *
   * @param rtfCodePage code page parsed from RTF file
   * @return an encoding name for Java
   */
  public static String getEncoding(int rtfCodePage) {
    switch (rtfCodePage) {
      case 437: return "Cp437";              // United States IBM
      case 708: return "ISO8859_6";          // Arabic (ASMO 708)
      case 709:                              // Arabic (ASMO 449+, BCON V4)
      case 710:                              // Arabic (transparent Arabic)
      case 711:                              // Arabic (Nafitha Enhanced)
      case 720: return DEFAULT_ENCODING;     // Arabic (transparent ASMO)
      case 819: return "ISO8859_1";          // Windows 3.1 (United States and Western Europe)
      case 850: return "Cp850";              // IBM multilingual
      case 852: return "Cp852";              // Eastern European
      case 860: return "Cp860";              // Portuguese
      case 862: return "Cp862";              // Hebrew
      case 863: return "Cp863";              // French Canadian
      case 864: return "Cp864";              // Arabic
      case 865: return "Cp865";              // Norwegian
      case 866: return "Cp866";              // Soviet Union
      case 874: return "MS874";              // Thai
      case 932: return "MS932";              // Japanese
      case 936: return "MS936";              // Simplified Chinese
      case 949: return "MS949";              // Korean
      case 950: return "MS950";              // Traditional Chinese
      case 1250: return "Cp1250";            // Eastern European
      case 1251: return "Cp1251";            // Cyrillic
      case 1252: return "Cp1252";            // Western European
      case 1253: return "Cp1253";            // Greek
      case 1254: return "Cp1255";            // Turkish
      case 1255: return "Cp1255";            // Hebrew
      case 1256: return "Cp1256";            // Arabic
      case 1257: return "Cp1257";            // Baltic
      case 1258: return "Cp1258";            // Vietnamese
      case 1361: return "x-Johab";           // Johab
      case 10000: return "MacRoman";         // MAC Roman
      case 10001: return "SJIS";             // MAC Japan
      case 10004: return "MacArabic";        // MAC Arabic
      case 10005: return "MacHebrew";        // MAC Hebrew
      case 10006: return "MacGreek";         // MAC Greek
      case 10007: return "MacCyrillic";      // MAC Cyrillic
      case 10029: return "MacCentralEurope"; // MAC Latin2
      case 10081: return "MacTurkish";       // MAC Turkish
      case 57002: return "ISCII91";          // Devanagari
      case 57003:                            // Bengali
      case 57004:                            // Tamil
      case 57005:                            // Telugu
      case 57006:                            // Assamese
      case 57007:                            // Oriya
      case 57008:                            // Kannada
      case 57009:                            // Malayalam
      case 57010:                            // Gujarati
      case 57011:                            // Punjabi
      default: return DEFAULT_ENCODING;
    }
  }

}
