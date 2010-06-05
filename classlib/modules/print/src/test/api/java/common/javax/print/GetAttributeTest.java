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

package javax.print;

import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.QueuedJobCount;
import javax.print.attribute.standard.RequestingUserName;
import junit.framework.TestCase;

public class GetAttributeTest extends TestCase {
    @SuppressWarnings("unchecked")
    public void testGetAttribute() {
        PrintService[] services;
        Object probe;
        Class[] clazz = new Class[] { PrinterIsAcceptingJobs.ACCEPTING_JOBS.getCategory(),
                PrinterState.IDLE.getCategory(), QueuedJobCount.class, Destination.class,
                JobName.class, RequestingUserName.class };
        services = PrintServiceLookup.lookupPrintServices(null, null);
        TestUtil.checkServices(services);
        for (int i = 0, ii = services.length; i < ii; i++) {
            for (int j = 0, jj = clazz.length; j < jj; j++) {
                if (PrintServiceAttribute.class.isAssignableFrom(clazz[j])) {
                    probe = services[i].getAttribute(clazz[j]);
                    assertNotNull(probe);
                }
            }
            try {
                probe = services[i].getAttribute(null);
                fail("NullPointerException must be thrown - the category is null");
            } catch (NullPointerException e) {
                // OK
            }
            try {
                Class<?> invalidClass = Copies.class;
                probe = services[i]
                        .getAttribute((Class<? extends PrintServiceAttribute>) invalidClass);
                fail("IllegalArgumentException must be thrown - category is not a Class that implements interface PrintServiceAttribute.");
            } catch (IllegalArgumentException e) {
                // OK
            }
        }
    }
}