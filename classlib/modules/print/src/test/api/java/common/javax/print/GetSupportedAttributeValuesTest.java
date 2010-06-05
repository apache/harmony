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

import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.standard.Finishings;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

import junit.framework.TestCase;

public class GetSupportedAttributeValuesTest extends TestCase {

    public void testGetSupportedAttributeValues() {
        System.out
                .println("============= START testGetSupportedAttributeValues ================");

        PrintService[] services;
        DocFlavor[] flavors = new DocFlavor[] { DocFlavor.INPUT_STREAM.GIF,
                DocFlavor.INPUT_STREAM.POSTSCRIPT,
                DocFlavor.INPUT_STREAM.TEXT_PLAIN_US_ASCII };

        services = PrintServiceLookup.lookupPrintServices(null, null);
        TestUtil.checkServices(services);
        for (int i = 0, ii = services.length; i < ii; i++) {
            System.out.println("\n----------- " + services[i].getName()
                    + "----------");

            Class[] cats = services[i].getSupportedAttributeCategories();
            HashAttributeSet aset = new HashAttributeSet();
            aset.add(MediaSizeName.ISO_A0);
            aset.add(OrientationRequested.LANDSCAPE);
            aset.add(Finishings.SADDLE_STITCH);

            for (int l = 0, ll = flavors.length; l < ll; l++) {
                if (services[i].isDocFlavorSupported(flavors[l])) {
                    System.out.println("    " + flavors[l]);

                    for (int j = 0, jj = cats.length; j < jj; j++) {
                        System.out.println("        " + cats[j]);

                        Object obj = services[i].getSupportedAttributeValues(
                                cats[j], flavors[l], aset);
                        if (obj != null && obj.getClass().isArray()) {
                            Object[] a = (Object[]) obj;
                            for (int k = 0, kk = a.length; k < kk; k++) {
                                System.out.println("            " + a[k]);
                            }
                        } else {
                            System.out.println("            " + obj);
                        }
                    }
                }
            }
        }

        System.out
                .println("============= END testGetSupportedAttributeValues ================");
    }

}