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
package javax.swing.text.html.parser;

/**
 * Class is generated basing on <a href="http://www.w3.org/TR/html401/sgml/entities.html">Character Entities</a>
 */
class EntitiesHandler {
    static void handleEntities() {
        //don't edit!
        handleEntity("squot", 39);
        //lat1
        handleEntity("nbsp", 160);
        handleEntity("iexcl", 161);
        handleEntity("cent", 162);
        handleEntity("pound", 163);
        handleEntity("curren", 164);
        handleEntity("yen", 165);
        handleEntity("brvbar", 166);
        handleEntity("sect", 167);
        handleEntity("uml", 168);
        handleEntity("copy", 169);
        handleEntity("ordf", 170);
        handleEntity("laquo", 171);
        handleEntity("not", 172);
        handleEntity("shy", 173);
        handleEntity("reg", 174);
        handleEntity("macr", 175);
        handleEntity("deg", 176);
        handleEntity("plusmn", 177);
        handleEntity("sup2", 178);
        handleEntity("sup3", 179);
        handleEntity("acute", 180);
        handleEntity("micro", 181);
        handleEntity("para", 182);
        handleEntity("middot", 183);
        handleEntity("cedil", 184);
        handleEntity("sup1", 185);
        handleEntity("ordm", 186);
        handleEntity("raquo", 187);
        handleEntity("frac14", 188);
        handleEntity("frac12", 189);
        handleEntity("frac34", 190);
        handleEntity("iquest", 191);
        handleEntity("Agrave", 192);
        handleEntity("Aacute", 193);
        handleEntity("Acirc", 194);
        handleEntity("Atilde", 195);
        handleEntity("Auml", 196);
        handleEntity("Aring", 197);
        handleEntity("AElig", 198);
        handleEntity("Ccedil", 199);
        handleEntity("Egrave", 200);
        handleEntity("Eacute", 201);
        handleEntity("Ecirc", 202);
        handleEntity("Euml", 203);
        handleEntity("Igrave", 204);
        handleEntity("Iacute", 205);
        handleEntity("Icirc", 206);
        handleEntity("Iuml", 207);
        handleEntity("ETH", 208);
        handleEntity("Ntilde", 209);
        handleEntity("Ograve", 210);
        handleEntity("Oacute", 211);
        handleEntity("Ocirc", 212);
        handleEntity("Otilde", 213);
        handleEntity("Ouml", 214);
        handleEntity("times", 215);
        handleEntity("Oslash", 216);
        handleEntity("Ugrave", 217);
        handleEntity("Uacute", 218);
        handleEntity("Ucirc", 219);
        handleEntity("Uuml", 220);
        handleEntity("Yacute", 221);
        handleEntity("THORN", 222);
        handleEntity("szlig", 223);
        handleEntity("agrave", 224);
        handleEntity("aacute", 225);
        handleEntity("acirc", 226);
        handleEntity("atilde", 227);
        handleEntity("auml", 228);
        handleEntity("aring", 229);
        handleEntity("aelig", 230);
        handleEntity("ccedil", 231);
        handleEntity("egrave", 232);
        handleEntity("eacute", 233);
        handleEntity("ecirc", 234);
        handleEntity("euml", 235);
        handleEntity("igrave", 236);
        handleEntity("iacute", 237);
        handleEntity("icirc", 238);
        handleEntity("iuml", 239);
        handleEntity("eth", 240);
        handleEntity("ntilde", 241);
        handleEntity("ograve", 242);
        handleEntity("oacute", 243);
        handleEntity("ocirc", 244);
        handleEntity("otilde", 245);
        handleEntity("ouml", 246);
        handleEntity("divide", 247);
        handleEntity("oslash", 248);
        handleEntity("ugrave", 249);
        handleEntity("uacute", 250);
        handleEntity("ucirc", 251);
        handleEntity("uuml", 252);
        handleEntity("yacute", 253);
        handleEntity("thorn", 254);
        handleEntity("yuml", 255);
        //special
        handleEntity("quot", 34);
        handleEntity("amp", 38);
        handleEntity("lt", 60);
        handleEntity("gt", 62);
        handleEntity("OElig", 338);
        handleEntity("oelig", 339);
        handleEntity("Scaron", 352);
        handleEntity("scaron", 353);
        handleEntity("Yuml", 376);
        handleEntity("circ", 710);
        handleEntity("tilde", 732);
        handleEntity("ensp", 8194);
        handleEntity("emsp", 8195);
        handleEntity("thinsp", 8201);
        handleEntity("zwnj", 8204);
        handleEntity("zwj", 8205);
        handleEntity("lrm", 8206);
        handleEntity("rlm", 8207);
        handleEntity("ndash", 8211);
        handleEntity("mdash", 8212);
        handleEntity("lsquo", 8216);
        handleEntity("rsquo", 8217);
        handleEntity("sbquo", 8218);
        handleEntity("ldquo", 8220);
        handleEntity("rdquo", 8221);
        handleEntity("bdquo", 8222);
        handleEntity("dagger", 8224);
        handleEntity("Dagger", 8225);
        handleEntity("permil", 8240);
        handleEntity("lsaquo", 8249);
        handleEntity("rsaquo", 8250);
        handleEntity("euro", 8364);
        //symbol
        handleEntity("fnof", 402);
        handleEntity("Alpha", 913);
        handleEntity("Beta", 914);
        handleEntity("Gamma", 915);
        handleEntity("Delta", 916);
        handleEntity("Epsilon", 917);
        handleEntity("Zeta", 918);
        handleEntity("Eta", 919);
        handleEntity("Theta", 920);
        handleEntity("Iota", 921);
        handleEntity("Kappa", 922);
        handleEntity("Lambda", 923);
        handleEntity("Mu", 924);
        handleEntity("Nu", 925);
        handleEntity("Xi", 926);
        handleEntity("Omicron", 927);
        handleEntity("Pi", 928);
        handleEntity("Rho", 929);
        handleEntity("Sigma", 931);
        handleEntity("Tau", 932);
        handleEntity("Upsilon", 933);
        handleEntity("Phi", 934);
        handleEntity("Chi", 935);
        handleEntity("Psi", 936);
        handleEntity("Omega", 937);
        handleEntity("alpha", 945);
        handleEntity("beta", 946);
        handleEntity("gamma", 947);
        handleEntity("delta", 948);
        handleEntity("epsilon", 949);
        handleEntity("zeta", 950);
        handleEntity("eta", 951);
        handleEntity("theta", 952);
        handleEntity("iota", 953);
        handleEntity("kappa", 954);
        handleEntity("lambda", 955);
        handleEntity("mu", 956);
        handleEntity("nu", 957);
        handleEntity("xi", 958);
        handleEntity("omicron", 959);
        handleEntity("pi", 960);
        handleEntity("rho", 961);
        handleEntity("sigmaf", 962);
        handleEntity("sigma", 963);
        handleEntity("tau", 964);
        handleEntity("upsilon", 965);
        handleEntity("phi", 966);
        handleEntity("chi", 967);
        handleEntity("psi", 968);
        handleEntity("omega", 969);
        handleEntity("thetasym", 977);
        handleEntity("upsih", 978);
        handleEntity("piv", 982);
        handleEntity("bull", 8226);
        handleEntity("hellip", 8230);
        handleEntity("prime", 8242);
        handleEntity("Prime", 8243);
        handleEntity("oline", 8254);
        handleEntity("frasl", 8260);
        handleEntity("weierp", 8472);
        handleEntity("image", 8465);
        handleEntity("real", 8476);
        handleEntity("trade", 8482);
        handleEntity("alefsym", 8501);
        handleEntity("larr", 8592);
        handleEntity("uarr", 8593);
        handleEntity("rarr", 8594);
        handleEntity("darr", 8595);
        handleEntity("harr", 8596);
        handleEntity("crarr", 8629);
        handleEntity("lArr", 8656);
        handleEntity("uArr", 8657);
        handleEntity("rArr", 8658);
        handleEntity("dArr", 8659);
        handleEntity("hArr", 8660);
        handleEntity("forall", 8704);
        handleEntity("part", 8706);
        handleEntity("exist", 8707);
        handleEntity("empty", 8709);
        handleEntity("nabla", 8711);
        handleEntity("isin", 8712);
        handleEntity("notin", 8713);
        handleEntity("ni", 8715);
        handleEntity("prod", 8719);
        handleEntity("sum", 8721);
        handleEntity("minus", 8722);
        handleEntity("lowast", 8727);
        handleEntity("radic", 8730);
        handleEntity("prop", 8733);
        handleEntity("infin", 8734);
        handleEntity("ang", 8736);
        handleEntity("and", 8743);
        handleEntity("or", 8744);
        handleEntity("cap", 8745);
        handleEntity("cup", 8746);
        handleEntity("int", 8747);
        handleEntity("there4", 8756);
        handleEntity("sim", 8764);
        handleEntity("cong", 8773);
        handleEntity("asymp", 8776);
        handleEntity("ne", 8800);
        handleEntity("equiv", 8801);
        handleEntity("le", 8804);
        handleEntity("ge", 8805);
        handleEntity("sub", 8834);
        handleEntity("sup", 8835);
        handleEntity("nsub", 8836);
        handleEntity("sube", 8838);
        handleEntity("supe", 8839);
        handleEntity("oplus", 8853);
        handleEntity("otimes", 8855);
        handleEntity("perp", 8869);
        handleEntity("sdot", 8901);
        handleEntity("lceil", 8968);
        handleEntity("rceil", 8969);
        handleEntity("lfloor", 8970);
        handleEntity("rfloor", 8971);
        handleEntity("lang", 9001);
        handleEntity("rang", 9002);
        handleEntity("loz", 9674);
        handleEntity("spades", 9824);
        handleEntity("clubs", 9827);
        handleEntity("hearts", 9829);
        handleEntity("diams", 9830);
    }

    static DTD dtd;
    static void initEntitiesCreation(final DTD dtd) {
        EntitiesHandler.dtd = dtd;
        handleEntities();
    }
    static void handleEntity(final String name, final int data) {
        DTDUtilities.handleEntity(dtd, name, data);
    }
}
