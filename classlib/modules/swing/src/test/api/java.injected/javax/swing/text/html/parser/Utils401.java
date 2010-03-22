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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text.html.parser;

import java.util.Hashtable;

import junit.framework.TestCase;

public class Utils401 {
    private static void handleEntity(final String name, final int ch,
                                    final Hashtable table) {
        handleEntity(name, name, ch, table);
        handleEntity(new Integer(ch), name, ch, table);
    }

    private static void handleEntity(final Object key,
                                    final String name,
                                    final int ch,
                                    final Hashtable table) {
        handleEntity(key, name, ch, DTDConstants.CDATA, table);
    }

    private static void handleEntity(final Object key,
                                    final String name,
                                    final int ch,
                                    final int type,
                                    final Hashtable table) {
        TestCase.assertTrue(table.containsKey(key));
        Entity entity = (Entity)table.get(key);
        Utils.checkEntity(entity, name, type, (char)ch,
                          true, false);
        table.remove(key);
    }

    public static void check32Entities(final Hashtable table) {
        //203 is becasue there are 100 * 2 + 3 values.
        //factor 2 = just another key in hashtable;
        //3 = (SPACE + RE + RS).
        TestCase.assertEquals(203, table.size());
        handleLat1(table);
        handleEntity("amp", 38, table);
        handleEntity("gt", 62, table);
        handleEntity("lt", 60, table);
        handleEntity("quot", 34, table);
        handleEntity("#SPACE", "#SPACE", ' ', 0, table);
        handleEntity("#RS", "#RS", '\n', 0, table);
        handleEntity("#RE", "#RE", '\r', 0, table);
        TestCase.assertEquals(0, table.size());
    }

    public static void check401Entities(final Hashtable table) {
        handleEntity("#SPACE", "#SPACE", ' ', 0, table);
        handleEntity("#RS", "#RS", '\n', 0, table);
        handleEntity("#RE", "#RE", '\r', 0, table);
        handleLat1(table);
        handleSpecial(table);
        handleSymbol(table);
        handleEntity("squot", 39, table);
        TestCase.assertEquals(0, table.size());
    }

    private static void handleLat1(final Hashtable table) {
        //--Don't edit
        handleEntity("nbsp", 160, table);
        handleEntity("iexcl", 161, table);
        handleEntity("cent", 162, table);
        handleEntity("pound", 163, table);
        handleEntity("curren", 164, table);
        handleEntity("yen", 165, table);
        handleEntity("brvbar", 166, table);
        handleEntity("sect", 167, table);
        handleEntity("uml", 168, table);
        handleEntity("copy", 169, table);
        handleEntity("ordf", 170, table);
        handleEntity("laquo", 171, table);
        handleEntity("not", 172, table);
        handleEntity("shy", 173, table);
        handleEntity("reg", 174, table);
        handleEntity("macr", 175, table);
        handleEntity("deg", 176, table);
        handleEntity("plusmn", 177, table);
        handleEntity("sup2", 178, table);
        handleEntity("sup3", 179, table);
        handleEntity("acute", 180, table);
        handleEntity("micro", 181, table);
        handleEntity("para", 182, table);
        handleEntity("middot", 183, table);
        handleEntity("cedil", 184, table);
        handleEntity("sup1", 185, table);
        handleEntity("ordm", 186, table);
        handleEntity("raquo", 187, table);
        handleEntity("frac14", 188, table);
        handleEntity("frac12", 189, table);
        handleEntity("frac34", 190, table);
        handleEntity("iquest", 191, table);
        handleEntity("Agrave", 192, table);
        handleEntity("Aacute", 193, table);
        handleEntity("Acirc", 194, table);
        handleEntity("Atilde", 195, table);
        handleEntity("Auml", 196, table);
        handleEntity("Aring", 197, table);
        handleEntity("AElig", 198, table);
        handleEntity("Ccedil", 199, table);
        handleEntity("Egrave", 200, table);
        handleEntity("Eacute", 201, table);
        handleEntity("Ecirc", 202, table);
        handleEntity("Euml", 203, table);
        handleEntity("Igrave", 204, table);
        handleEntity("Iacute", 205, table);
        handleEntity("Icirc", 206, table);
        handleEntity("Iuml", 207, table);
        handleEntity("ETH", 208, table);
        handleEntity("Ntilde", 209, table);
        handleEntity("Ograve", 210, table);
        handleEntity("Oacute", 211, table);
        handleEntity("Ocirc", 212, table);
        handleEntity("Otilde", 213, table);
        handleEntity("Ouml", 214, table);
        handleEntity("times", 215, table);
        handleEntity("Oslash", 216, table);
        handleEntity("Ugrave", 217, table);
        handleEntity("Uacute", 218, table);
        handleEntity("Ucirc", 219, table);
        handleEntity("Uuml", 220, table);
        handleEntity("Yacute", 221, table);
        handleEntity("THORN", 222, table);
        handleEntity("szlig", 223, table);
        handleEntity("agrave", 224, table);
        handleEntity("aacute", 225, table);
        handleEntity("acirc", 226, table);
        handleEntity("atilde", 227, table);
        handleEntity("auml", 228, table);
        handleEntity("aring", 229, table);
        handleEntity("aelig", 230, table);
        handleEntity("ccedil", 231, table);
        handleEntity("egrave", 232, table);
        handleEntity("eacute", 233, table);
        handleEntity("ecirc", 234, table);
        handleEntity("euml", 235, table);
        handleEntity("igrave", 236, table);
        handleEntity("iacute", 237, table);
        handleEntity("icirc", 238, table);
        handleEntity("iuml", 239, table);
        handleEntity("eth", 240, table);
        handleEntity("ntilde", 241, table);
        handleEntity("ograve", 242, table);
        handleEntity("oacute", 243, table);
        handleEntity("ocirc", 244, table);
        handleEntity("otilde", 245, table);
        handleEntity("ouml", 246, table);
        handleEntity("divide", 247, table);
        handleEntity("oslash", 248, table);
        handleEntity("ugrave", 249, table);
        handleEntity("uacute", 250, table);
        handleEntity("ucirc", 251, table);
        handleEntity("uuml", 252, table);
        handleEntity("yacute", 253, table);
        handleEntity("thorn", 254, table);
        handleEntity("yuml", 255, table);
    }

    private static void handleSymbol(final Hashtable table) {
        //! Don't edit
        handleEntity("fnof", 402, table);
        handleEntity("Alpha", 913, table);
        handleEntity("Beta", 914, table);
        handleEntity("Gamma", 915, table);
        handleEntity("Delta", 916, table);
        handleEntity("Epsilon", 917, table);
        handleEntity("Zeta", 918, table);
        handleEntity("Eta", 919, table);
        handleEntity("Theta", 920, table);
        handleEntity("Iota", 921, table);
        handleEntity("Kappa", 922, table);
        handleEntity("Lambda", 923, table);
        handleEntity("Mu", 924, table);
        handleEntity("Nu", 925, table);
        handleEntity("Xi", 926, table);
        handleEntity("Omicron", 927, table);
        handleEntity("Pi", 928, table);
        handleEntity("Rho", 929, table);
        handleEntity("Sigma", 931, table);
        handleEntity("Tau", 932, table);
        handleEntity("Upsilon", 933, table);
        handleEntity("Phi", 934, table);
        handleEntity("Chi", 935, table);
        handleEntity("Psi", 936, table);
        handleEntity("Omega", 937, table);
        handleEntity("alpha", 945, table);
        handleEntity("beta", 946, table);
        handleEntity("gamma", 947, table);
        handleEntity("delta", 948, table);
        handleEntity("epsilon", 949, table);
        handleEntity("zeta", 950, table);
        handleEntity("eta", 951, table);
        handleEntity("theta", 952, table);
        handleEntity("iota", 953, table);
        handleEntity("kappa", 954, table);
        handleEntity("lambda", 955, table);
        handleEntity("mu", 956, table);
        handleEntity("nu", 957, table);
        handleEntity("xi", 958, table);
        handleEntity("omicron", 959, table);
        handleEntity("pi", 960, table);
        handleEntity("rho", 961, table);
        handleEntity("sigmaf", 962, table);
        handleEntity("sigma", 963, table);
        handleEntity("tau", 964, table);
        handleEntity("upsilon", 965, table);
        handleEntity("phi", 966, table);
        handleEntity("chi", 967, table);
        handleEntity("psi", 968, table);
        handleEntity("omega", 969, table);
        handleEntity("thetasym", 977, table);
        handleEntity("upsih", 978, table);
        handleEntity("piv", 982, table);
        handleEntity("bull", 8226, table);
        handleEntity("hellip", 8230, table);
        handleEntity("prime", 8242, table);
        handleEntity("Prime", 8243, table);
        handleEntity("oline", 8254, table);
        handleEntity("frasl", 8260, table);
        handleEntity("weierp", 8472, table);
        handleEntity("image", 8465, table);
        handleEntity("real", 8476, table);
        handleEntity("trade", 8482, table);
        handleEntity("alefsym", 8501, table);
        handleEntity("larr", 8592, table);
        handleEntity("uarr", 8593, table);
        handleEntity("rarr", 8594, table);
        handleEntity("darr", 8595, table);
        handleEntity("harr", 8596, table);
        handleEntity("crarr", 8629, table);
        handleEntity("lArr", 8656, table);
        handleEntity("uArr", 8657, table);
        handleEntity("rArr", 8658, table);
        handleEntity("dArr", 8659, table);
        handleEntity("hArr", 8660, table);
        handleEntity("forall", 8704, table);
        handleEntity("part", 8706, table);
        handleEntity("exist", 8707, table);
        handleEntity("empty", 8709, table);
        handleEntity("nabla", 8711, table);
        handleEntity("isin", 8712, table);
        handleEntity("notin", 8713, table);
        handleEntity("ni", 8715, table);
        handleEntity("prod", 8719, table);
        handleEntity("sum", 8721, table);
        handleEntity("minus", 8722, table);
        handleEntity("lowast", 8727, table);
        handleEntity("radic", 8730, table);
        handleEntity("prop", 8733, table);
        handleEntity("infin", 8734, table);
        handleEntity("ang", 8736, table);
        handleEntity("and", 8743, table);
        handleEntity("or", 8744, table);
        handleEntity("cap", 8745, table);
        handleEntity("cup", 8746, table);
        handleEntity("int", 8747, table);
        handleEntity("there4", 8756, table);
        handleEntity("sim", 8764, table);
        handleEntity("cong", 8773, table);
        handleEntity("asymp", 8776, table);
        handleEntity("ne", 8800, table);
        handleEntity("equiv", 8801, table);
        handleEntity("le", 8804, table);
        handleEntity("ge", 8805, table);
        handleEntity("sub", 8834, table);
        handleEntity("sup", 8835, table);
        handleEntity("nsub", 8836, table);
        handleEntity("sube", 8838, table);
        handleEntity("supe", 8839, table);
        handleEntity("oplus", 8853, table);
        handleEntity("otimes", 8855, table);
        handleEntity("perp", 8869, table);
        handleEntity("sdot", 8901, table);
        handleEntity("lceil", 8968, table);
        handleEntity("rceil", 8969, table);
        handleEntity("lfloor", 8970, table);
        handleEntity("rfloor", 8971, table);
        handleEntity("lang", 9001, table);
        handleEntity("rang", 9002, table);
        handleEntity("loz", 9674, table);
        handleEntity("spades", 9824, table);
        handleEntity("clubs", 9827, table);
        handleEntity("hearts", 9829, table);
        handleEntity("diams", 9830, table);
    }

    private static void handleSpecial(final Hashtable table) {
        //!Don't edit
        handleEntity("quot", 34, table);
        handleEntity("amp", 38, table);
        handleEntity("lt", 60, table);
        handleEntity("gt", 62, table);
        handleEntity("OElig", 338, table);
        handleEntity("oelig", 339, table);
        handleEntity("Scaron", 352, table);
        handleEntity("scaron", 353, table);
        handleEntity("Yuml", 376, table);
        handleEntity("circ", 710, table);
        handleEntity("tilde", 732, table);
        handleEntity("ensp", 8194, table);
        handleEntity("emsp", 8195, table);
        handleEntity("thinsp", 8201, table);
        handleEntity("zwnj", 8204, table);
        handleEntity("zwj", 8205, table);
        handleEntity("lrm", 8206, table);
        handleEntity("rlm", 8207, table);
        handleEntity("ndash", 8211, table);
        handleEntity("mdash", 8212, table);
        handleEntity("lsquo", 8216, table);
        handleEntity("rsquo", 8217, table);
        handleEntity("sbquo", 8218, table);
        handleEntity("ldquo", 8220, table);
        handleEntity("rdquo", 8221, table);
        handleEntity("bdquo", 8222, table);
        handleEntity("dagger", 8224, table);
        handleEntity("Dagger", 8225, table);
        handleEntity("permil", 8240, table);
        handleEntity("lsaquo", 8249, table);
        handleEntity("rsaquo", 8250, table);
        handleEntity("euro", 8364, table);
    }
}
