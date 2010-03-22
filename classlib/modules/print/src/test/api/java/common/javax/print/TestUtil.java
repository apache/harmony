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

public class TestUtil extends TestCase {
    public void testFakeTest() {
        // fake test case
    }

    public static void checkServices(PrintService[] srvs) {
        assertNotNull(
                "\nPrintServiceLookup.lookupPrintServices must return non-null value",
                srvs);
        if (srvs.length == 0) {
            String szerr = "\nNo printers found.";
            String os = System.getProperty("os.name");

            if (os == null) {
                szerr += "\nPlease add printer on localhost.";
            } else if (os.toLowerCase().indexOf("windows") >= 0) {
                szerr += "\nPlease add printer on localhost.";
            } else if (os.toLowerCase().indexOf("linux") >= 0) {
                szerr += "\nPlease install CUPS and create printers on localhost";
            }
            szerr += "\nYou can use "
                    + "'print.cups.servers' and/or 'print.ipp.printers' \n"
                    + "properties to specify CUPS servers and/or IPP printers.\n"
                    + "Usage: \n"
                    + "  print.cups.servers=http://<cups-server1>:<port1>,...\n"
                    + "  print.ipp.printers=http://<ipp-server1>:<port1>/path/to/printer1,...\n";
            fail(szerr);
        }
    }

}