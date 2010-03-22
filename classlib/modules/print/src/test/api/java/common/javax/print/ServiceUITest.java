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
 * @author Irina A. Arkhipets 
 */ 

/*
 * ServiceUITests.java
 * 
 * JUnit tests for javax.print.ServiceUI class
 * 
 */

package javax.print;

import javax.print.attribute.HashPrintRequestAttributeSet;

import junit.framework.TestCase;

public class ServiceUITest extends TestCase {

private PrintService [] services = null;
private HashPrintRequestAttributeSet attrs = null;

protected void setUp() throws Exception {
    services = PrintServiceLookup.lookupPrintServices(
        DocFlavor.INPUT_STREAM.GIF, null);
    attrs = new HashPrintRequestAttributeSet();
}

/*
 * Throws IllegalArgumentException if services array is null
*/
public void testPrintDialog1() {
    try {
        ServiceUI.printDialog(null, 
                              5,
                              5,
                              null,
                              null,
                              null,
                              attrs);
        fail();
    } catch (IllegalArgumentException e) {
        /* IllegalArgimentException is expected here, so the test passes */
    }
}

/*
 * Throws IllegalArgumentException if services array is or empty 
*/
public void testPrintDialog2() {
    try {
        ServiceUI.printDialog(null, 
                              5,
                              5,
                              new PrintService [] {},
                              null,
                              null,
                              attrs);
        fail();
    } catch (IllegalArgumentException e) {
        /* IllegalArgimentException is expected here, so the test passes */
    }
}

/*
 * Throws IllegalArgumentException if attributes is null  
*/
public void testPrintDialog3() {
    try {
        ServiceUI.printDialog(null, 
                              5,
                              5,
                              services,
                              null,
                              null,
                              null);
        fail();
    } catch (IllegalArgumentException e) {
        /* IllegalArgimentException is expected here, so the test passes */
    }
}

/*
 * Throws IllegalArgumentException if initial PrintService
 * is not in the list of browsable services
*/ 
public void testPrintDialog4() {
    if ((services != null) && (services.length > 1)) {
        PrintService [] s1 = new PrintService [services.length-1];
        for (int i = 0; i < services.length - 1; i++) {
            s1[i] = services[i];
        }        
        try {
            ServiceUI.printDialog(null, 
                                  5,
                                  5,
                                  s1,
                                  services[services.length - 1],
                                  null,
                                  null);
            fail();
        } catch (IllegalArgumentException e) {
            /* IllegalArgimentException is expected here, so the test passes */
        }
    }
}

} /* End of ServiceUITests */
