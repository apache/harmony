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
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.Sides;

import junit.framework.TestCase;

public class LookupDefaultPrintServiceTest extends TestCase {
    public void testLookupDefaultPrintService() throws Exception {
        System.out
                .println("======= START LookupDefaultPrintServiceTest ======");

        DocFlavor psFlavor = DocFlavor.INPUT_STREAM.GIF;
        PrintService service;
        InputStream fis;
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        HashDocAttributeSet daset = new HashDocAttributeSet();
        DocPrintJob pj;
        Doc doc;

        aset.add(new Copies(2));
        aset.add(MediaSizeName.ISO_A4);
        daset.add(MediaName.ISO_A4_WHITE);
        daset.add(Sides.TWO_SIDED_LONG_EDGE);

        service = PrintServiceLookup.lookupDefaultPrintService();
        if (service != null) {
            if (service.isDocFlavorSupported(psFlavor)) {
                if (service.getUnsupportedAttributes(psFlavor, aset)==null) {
                    fis = this.getClass().getResourceAsStream(
                            "/Resources/GIF.gif");
                    doc = new SimpleDoc(fis, psFlavor, daset);

                    pj = service.createPrintJob();
                    pj.print(doc, aset);
                    System.out.println(fis.toString() + " printed on "
                            + service.getName());
                }
            } else {
                System.out.println("flavor is not supported");
            }
        } else {
            System.out.println("service not found");
        }

        System.out.println("======= END LookupDefaultPrintServiceTest =======");
    }

}