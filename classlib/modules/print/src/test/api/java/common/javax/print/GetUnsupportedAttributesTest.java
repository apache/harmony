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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.Finishings;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.RequestingUserName;

import junit.framework.TestCase;

public class GetUnsupportedAttributesTest extends TestCase {
    public void testIsAttributeValueSupported() throws Exception {
        System.out
                .println("============= START GetUnsupportedAttributesTest ================");

        PrintService[] services;
        DocFlavor[] flavors = new DocFlavor[] { DocFlavor.INPUT_STREAM.GIF,
                DocFlavor.INPUT_STREAM.POSTSCRIPT,
                DocFlavor.INPUT_STREAM.TEXT_PLAIN_US_ASCII };

        services = PrintServiceLookup.lookupPrintServices(null, null);
        TestUtil.checkServices(services);
        if (services.length > 0) {
            for (int i = 0, ii = services.length; i < ii; i++) {
                System.out.println("----------- " + services[i].getName()
                        + "----------");

                URI uri = null;
                try {
                    uri = new URI("file:///c:/no/such/dir/print.out");
                    //uri = File.createTempFile("xxx", null).toURI();
                } catch (URISyntaxException e) {
                    fail();
                }

                AttributeSet attrs = new HashAttributeSet();
                attrs.add(Finishings.EDGE_STITCH);
                attrs.add(MediaSizeName.JAPANESE_DOUBLE_POSTCARD);
                attrs.add(new Destination(uri));
                attrs.add(new DocumentName("Doc X", Locale.US));
                attrs.add(new JobName("Job Y", Locale.US));
                attrs.add(new RequestingUserName("User Z", Locale.US));

                for (int j = 0; j < flavors.length; j++) {
                    if (services[i].isDocFlavorSupported(flavors[j])) {
                        AttributeSet aset = services[i]
                                .getUnsupportedAttributes(flavors[j], attrs);
                        if (aset == null) {
                            fail("At least one attribute is unsupported");
                        }
                        if (aset != null) {
                            Attribute[] aarr = aset.toArray();
                            System.out
                                    .println("Usupported attributes fo DocFlavor "
                                            + flavors[j]);
                            for (int k = 0, kk = aarr.length; k < kk; k++) {
                                System.out.println("\t" + aarr[k]);
                            }
                        }
                    }
                }
            }
        }

        System.out
                .println("============= END GetUnsupportedAttributesTest ================");
    }

}