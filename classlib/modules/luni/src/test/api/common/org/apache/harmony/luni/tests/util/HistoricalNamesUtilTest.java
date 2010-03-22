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
package org.apache.harmony.luni.tests.util;

import org.apache.harmony.luni.util.HistoricalNamesUtil;

import junit.framework.TestCase;

public class HistoricalNamesUtilTest extends TestCase {

    public void test_getHistoricalName_Exist() {
        assertEquals("Big5_HKSCS", HistoricalNamesUtil
                .getHistoricalName("Big5_HKSCS"));
        assertEquals("EUC_JP", HistoricalNamesUtil.getHistoricalName("EUC-JP"));
        assertEquals("EUC_KR", HistoricalNamesUtil.getHistoricalName("EUC-KR"));
        assertEquals("EUC_CN", HistoricalNamesUtil.getHistoricalName("GB2312"));
        assertEquals("Cp838", HistoricalNamesUtil.getHistoricalName("IBM-Thai"));
        assertEquals("SJIS", HistoricalNamesUtil.getHistoricalName("Shift_JIS"));
        assertEquals("UnicodeBigUnmarked", HistoricalNamesUtil
                .getHistoricalName("UTF-16BE"));
        assertEquals("MS932", HistoricalNamesUtil
                .getHistoricalName("windows-31j"));
        assertEquals("Big5_Solaris", HistoricalNamesUtil
                .getHistoricalName("x-Big5-Solaris"));
        assertEquals("MacCroatian", HistoricalNamesUtil
                .getHistoricalName("x-MacCroatian"));
    }

    public void test_getHistoricalName_NotExist() {
        assertEquals("notexist", HistoricalNamesUtil
                .getHistoricalName("notexist"));
    }

    public void test_getHistoricalName_Null() {
        assertNull(HistoricalNamesUtil.getHistoricalName(null));
    }
}
