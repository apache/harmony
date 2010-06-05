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

import junit.framework.TestCase;

public class GetSupportedDocFlavorsTest extends TestCase {

    public void testGetSupportedDocFlavors() {
        System.out
                .println("============= START testGetSupportedDocFlavors ================");

        PrintService[] services;
        DocFlavor[] df;
        services = PrintServiceLookup.lookupPrintServices(null, null);
        TestUtil.checkServices(services);
        for (int i = 0, ii = services.length; i < ii; i++) {
            System.out.println("------------------" + services[i].getName()
                    + "-------------------");
            df = services[i].getSupportedDocFlavors();
            if (df.length == 0) {
                fail("Array of supported doc flavors should have at least one element");
            }
            for (int j = 0; j < df.length; j++) {
                System.out.println(df[j]);
            }
        }

        System.out
                .println("============= END testGetSupportedDocFlavors ================");
    }

}