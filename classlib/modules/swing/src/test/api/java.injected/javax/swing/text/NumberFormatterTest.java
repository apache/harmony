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
package javax.swing.text;

import java.text.NumberFormat;
import javax.swing.SwingTestCase;

public class NumberFormatterTest extends SwingTestCase {
    NumberFormatter formatter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        formatter = new NumberFormatter();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void checkBaseProperties(final NumberFormat numberFormat) {
        assertEquals(numberFormat, formatter.getFormat());
        assertNull(formatter.getValueClass());
        assertFalse(formatter.getCommitsOnValidEdit());
        assertTrue(formatter.getAllowsInvalid());
        assertNull(formatter.getMaximum());
        assertNull(formatter.getMinimum());
        assertFalse(formatter.getOverwriteMode());
    }

    public void testSetFormat() {
        NumberFormat numberFormat = NumberFormat.getInstance();
        formatter.setFormat(numberFormat);
        checkBaseProperties(numberFormat);
        formatter.setFormat(null);
        checkBaseProperties(null);
    }

    public void testNumberFormatterNumberFormat() {
        NumberFormat numberFormat = NumberFormat.getInstance();
        formatter = new NumberFormatter(numberFormat);
        checkBaseProperties(numberFormat);
    }

    public void testNumberFormatter() {
        checkBaseProperties(NumberFormat.getNumberInstance());
    }
}
