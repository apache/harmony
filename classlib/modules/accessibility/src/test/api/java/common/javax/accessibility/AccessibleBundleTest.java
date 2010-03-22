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
 * @author Dennis Ushakov
 */

package javax.accessibility;

import java.util.Locale;

import junit.framework.TestCase;

public class AccessibleBundleTest extends TestCase {
    private MockAccessibleBundle bundle;

    private Object[][] resources;

    private static final String ACCESSIBLE_RESOURSE_BUNDLE = "javax.accessibility.AccessibleResourceBundle"; //$NON-NLS-1$

    class MockAccessibleBundle extends AccessibleBundle {
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String toDisplayString(String resourceBundleName, Locale locale) {
            return super.toDisplayString(resourceBundleName, locale);
        }
    }

    @SuppressWarnings("deprecation")
    public void setUp() {
        bundle = new MockAccessibleBundle();
        resources = new AccessibleResourceBundle().getContents();
    }

    public void tearDown() {
        bundle = null;
        resources = null;
    }

    /**
     * @test {@link javax.accessibility.AccessibleBundle#toDisplayString()}
     * @add test
     *      {@link javax.accessibility.AccessibleBundle#toDisplayString(java.util.Locale)}
     *@add test
     *      {@link javax.accessibility.AccessibleBundle#toDisplayString(String, java.util.Locale)}
     */
    public void testToDisplayString_withoutArgAndWithArg() throws Exception {
        for (int i = 0; i < resources.length; i++) {
            bundle.setKey((String) resources[i][0]);
            Locale defaultLocale = Locale.getDefault();
            String expected = bundle.toDisplayString();
            String actualWithLocale = bundle.toDisplayString(defaultLocale);
            String actualWithResourceBundleName = bundle.toDisplayString(
                    ACCESSIBLE_RESOURSE_BUNDLE, defaultLocale);
            assertEquals("DisplayString don't match resource: "
                    + bundle.getKey(), bundle.toDisplayString(),
                    resources[i][1]);
            assertEquals("toDisplayString(locale) error ", expected,
                    actualWithLocale);
            assertEquals(expected, actualWithResourceBundleName);
            assertEquals("toDisplayString don't match toString: "
                    + bundle.getKey(), bundle.toString(), bundle
                    .toDisplayString());
        }
        bundle.setKey("ShouldNotFindSuchAString");
        assertEquals("Not bundled DisplayString should match itself",
                "ShouldNotFindSuchAString", bundle.toDisplayString());
    }

}
