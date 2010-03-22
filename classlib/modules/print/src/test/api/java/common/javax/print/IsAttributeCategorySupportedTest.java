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
 * @author Igor A. Pyankov 
 */ 

package javax.print;

import javax.print.attribute.standard.JobStateReason;

import junit.framework.TestCase;

public class IsAttributeCategorySupportedTest extends TestCase {

    public void testIsAttributeCategorySupported() {
        System.out
                .println("============= START testIsAttributeCategorySupported ================");

        PrintService[] services;
        Class[] claz;
        boolean supported;

        services = PrintServiceLookup.lookupPrintServices(null, null);
        TestUtil.checkServices(services);
        for (int i = 0, ii = services.length; i < ii; i++) {
            System.out.println("------------------" + services[i].getName()
                    + "-------------------");
            claz = services[i].getSupportedAttributeCategories();
            for (int j = 0, jj = claz.length; j < jj; j++) {
                supported = services[i].isAttributeCategorySupported(claz[j]);
                if (!supported) {
                    fail("Category " + claz[j] + " must be supported.");
                }
                System.out.println(claz[j] + ": " + supported);
            }

            supported = services[i]
                    .isAttributeCategorySupported(JobStateReason.ABORTED_BY_SYSTEM
                            .getCategory());
            if (supported) {
                fail("Category "
                        + JobStateReason.ABORTED_BY_SYSTEM.getCategory()
                        + " must not be supported.");
            }
            System.out.println(JobStateReason.ABORTED_BY_SYSTEM.getCategory()
                    + ": " + supported);
        }

        System.out
                .println("============= END testIsAttributeCategorySupported ================");
    }

}