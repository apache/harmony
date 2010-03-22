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

import java.io.InputStream;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

import junit.framework.TestCase;

public class LookupPrintServicesTest extends TestCase {
    public void testLookupPrintServices() throws Exception {
        System.out.println("======== START LookupPrintServicesTest ========");

        PrintService[] services;
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        HashDocAttributeSet daset = new HashDocAttributeSet();
        DocPrintJob pj;
        Doc doc;

        Object[][] filetoprint = { { "/Resources/JPEG.jpg",
                DocFlavor.INPUT_STREAM.JPEG },
                { "/Resources/GIF.gif", DocFlavor.INPUT_STREAM.GIF } };

        DocFlavor df;
        InputStream fis;

        for (int i = 0; i < filetoprint.length; i++) {
            df = (DocFlavor) filetoprint[i][1];

            services = PrintServiceLookup.lookupPrintServices(df, aset);
            TestUtil.checkServices(services);

            for (int j = 0; j < services.length; j++) {
                fis = this.getClass().getResourceAsStream(
                        (String) filetoprint[i][0]);
                doc = new SimpleDoc(fis, df, daset);
                PrintService printer = services[j];

                pj = printer.createPrintJob();
                pj.print(doc, aset);
                System.out.println(fis.toString() + " printed on "
                        + printer.getName());
            }
        }

        System.out.println("====== END LookupPrintServicesTest ========");
    }

}