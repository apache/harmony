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
import java.util.Locale;

import javax.print.attribute.Attribute;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.Finishings;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.Sides;

import junit.framework.TestCase;

public class IsAttributeValueSupportedTest extends TestCase {
    public void testIsAttributeValueSupported() throws Exception {
        System.out
                .println("============= START testIsAttributeValueSupported ================");

        PrintService[] services;
        URI uri1, uri2, uri3;
        boolean supported = false;

        services = PrintServiceLookup.lookupPrintServices(null, null);
        TestUtil.checkServices(services);
        for (int i = 0, ii = services.length; i < ii; i++) {
            System.out.println("----------- " + services[i].getName()
                    + "----------");

            uri1 = new URI("file:///foo/bar");
            uri2 = new URI("file:///F:/printing/tmp/print.out");
            uri3 = new URI("file:///F:/printing/tmp/xxx/print.out");

            Attribute[] attrs = { MediaSizeName.ISO_A0,
                    Finishings.NONE,
                    Finishings.EDGE_STITCH,
                    MediaSizeName.ISO_A2,
                    MediaSizeName.ISO_A3,
                    new Destination(uri1),
                    new Destination(uri2),
                    new Destination(uri3),
                    new DocumentName("xyz", Locale.US),
                    new JobName("xyz", Locale.US),
                    new RequestingUserName("xyz", Locale.US),
                    Sides.DUPLEX,
                    Sides.ONE_SIDED,
                    Sides.TUMBLE,
                    Sides.TWO_SIDED_LONG_EDGE,
                    Sides.TWO_SIDED_SHORT_EDGE,
                    null };
            for (int a = 0, ac = attrs.length; a < ac; a++) {
                try {
                    supported = services[i].isAttributeValueSupported(
                            attrs[a], DocFlavor.INPUT_STREAM.GIF, null);
                } catch (NullPointerException e1) {
                    if (attrs[a] != null) {
                        fail(e1.toString());
                    }
                } catch (IllegalArgumentException e) {
                    if (services[i]
                            .isDocFlavorSupported(DocFlavor.INPUT_STREAM.GIF)) {
                        fail(e.toString());
                    }
                } catch (Exception e) {
                    fail(e.toString());
                }
                System.out.println(attrs[a]

                        + (attrs[a] == null ? "" : "("
                                + attrs[a].getCategory().toString() + ")")
                        + " : " + supported);
            }
        }

        System.out.println("============= END testIsAttributeValueSupported ================");
    }

}