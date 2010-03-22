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
 * @author Ilya S. Okomin
 */
package java.awt.font;

public interface OpenType {

    public static final int TAG_ACNT = 1633906292;

    public static final int TAG_AVAR = 1635148146;

    public static final int TAG_BASE = 1111577413;

    public static final int TAG_BDAT = 1650745716;

    public static final int TAG_BLOC = 1651273571;

    public static final int TAG_BSLN = 1651731566;

    public static final int TAG_CFF = 1128678944;

    public static final int TAG_CMAP = 1668112752;

    public static final int TAG_CVAR = 1668702578;

    public static final int TAG_CVT = 1668707360;

    public static final int TAG_DSIG = 1146308935;

    public static final int TAG_EBDT = 1161970772;

    public static final int TAG_EBLC = 1161972803;

    public static final int TAG_EBSC = 1161974595;

    public static final int TAG_FDSC = 1717859171;

    public static final int TAG_FEAT = 1717920116;

    public static final int TAG_FMTX = 1718449272;

    public static final int TAG_FPGM = 1718642541;

    public static final int TAG_FVAR = 1719034226;

    public static final int TAG_GASP = 1734439792;

    public static final int TAG_GDEF = 1195656518;

    public static final int TAG_GLYF = 1735162214;

    public static final int TAG_GPOS = 1196445523;

    public static final int TAG_GSUB = 1196643650;

    public static final int TAG_GVAR = 1735811442;

    public static final int TAG_HDMX = 1751412088;

    public static final int TAG_HEAD = 1751474532;

    public static final int TAG_HHEA = 1751672161;

    public static final int TAG_HMTX = 1752003704;

    public static final int TAG_JSTF = 1246975046;

    public static final int TAG_JUST = 1786082164;

    public static final int TAG_KERN = 1801810542;

    public static final int TAG_LCAR = 1818452338;

    public static final int TAG_LOCA = 1819239265;

    public static final int TAG_LTSH = 1280594760;

    public static final int TAG_MAXP = 1835104368;

    public static final int TAG_MMFX = 1296909912;

    public static final int TAG_MMSD = 1296913220;

    public static final int TAG_MORT = 1836020340;

    public static final int TAG_NAME = 1851878757;

    public static final int TAG_OPBD = 1836020340;

    public static final int TAG_OS2 = 1330851634;

    public static final int TAG_PCLT = 1346587732;

    public static final int TAG_POST = 1886352244;

    public static final int TAG_PREP = 1886545264;

    public static final int TAG_PROP = 1886547824;

    public static final int TAG_TRAK = 1953653099;

    public static final int TAG_TYP1 = 1954115633;

    public static final int TAG_VDMX = 1447316824;

    public static final int TAG_VHEA = 1986553185;

    public static final int TAG_VMTX = 1986884728;

    public int getVersion();

    public byte[] getFontTable(int sfntTag);

    public byte[] getFontTable(int sfntTag, int offset, int count);

    public byte[] getFontTable(String strSfntTag);

    public byte[] getFontTable(String strSfntTag, int offset, int count);

    public int getFontTableSize(String strSfntTag);

    public int getFontTableSize(int sfntTag);

}

