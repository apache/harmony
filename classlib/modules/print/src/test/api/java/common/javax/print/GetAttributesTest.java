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

import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttributeSet;

import junit.framework.TestCase;

public class GetAttributesTest extends TestCase {

    public void testGetAttributes() {
        System.out
                .println("============= START testGetAttributes ================");

        PrintService[] services;
        PrintServiceAttributeSet as;
        Attribute[] aa;
        services = PrintServiceLookup.lookupPrintServices(null, null);
        TestUtil.checkServices(services);
        
        for (int i = 0, ii = services.length; i < ii; i++) {
            System.out.println("----" + services[i].getName() + "----");
            as = services[i].getAttributes();
            aa = as.toArray();
            for (int j = 0; j < aa.length; j++) {
                System.out.println(aa[j].getName() + ": " + aa[j].toString());
            }
        }

        System.out
                .println("============= END testGetAttributes ================");
    }
    
}