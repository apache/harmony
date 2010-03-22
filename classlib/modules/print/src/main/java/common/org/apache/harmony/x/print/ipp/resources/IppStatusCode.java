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

package org.apache.harmony.x.print.ipp.resources;

import java.util.ListResourceBundle;

/** This class represents status codes 
 * described in RFC 2911 (http://ietf.org/rfc/rfc2911.txt?number=2911)
 */

public class IppStatusCode extends ListResourceBundle {

    public Object[][] getContents() {
        return statuscode;
    }

    /*
     * The status code values range from 0x0000 to 0x7FFF. The value ranges for each status code class are as follows: 
     *
     * "successful" - 0x0000 to 0x00FF 
     * "informational" - 0x0100 to 0x01FF 
     * "redirection" - 0x0200 to 0x02FF 
     * "client-error" - 0x0400 to 0x04FF 
     * "server-error" - 0x0500 to 0x05FF 
     * 
     *    The top half (128 values) of each range (0x0n40 to 0x0nFF, for n = 0
     *    to 5) is reserved for vendor use within each status code class.
     *    Values 0x0600 to 0x7FFF are reserved for future assignment by IETF
     *    standards track documents and MUST NOT be used.
     */
    static final Object[][] statuscode = {
            /*
             * Informational
             * This class of status code indicates a provisional response and is to be used
             * for informational purposes only.
             * There are no status codes defined in IPP/1.1 for this class of status code.
             *
             *
             * Successful Status Codes
             * This class of status code indicates that the client's request was
             * successfully received, understood, and accepted.
             */
            { "successful-ok", new Integer(0x0000) },
            { "successful-ok-ignored-or-substituted-attributes",
                    new Integer(0x0001) },
            { "successful-ok-conflicting-attributes", new Integer(0x0002) },

            /*
             * Redirection Status Codes
             * This class of status code indicates that further action needs to be taken to 
             * fulfill the request. 
             * There are no status codes defined in IPP/1.1 for this class of status code.
             *  
             * Client Error Status Codes
             * This class of status code is intended for cases in which the client seems to
             * have erred. The IPP object SHOULD return a message containing an explanation of
             * the error situation and whether it is a temporary or permanent condition.
             */
            { "client-error-bad-request", new Integer(0x0400) },
            { "client-error-forbidden", new Integer(0x0401) },
            { "client-error-not-authenticated", new Integer(0x0402) },
            { "client-error-not-authorized", new Integer(0x0403) },
            { "client-error-not-possible", new Integer(0x0404) },
            { "client-error-timeout", new Integer(0x0405) },
            { "client-error-not-found", new Integer(0x0406) },
            { "client-error-gone", new Integer(0x0407) },
            { "client-error-request-entity-too-large", new Integer(0x0408) },
            { "client-error-request-value-too-long", new Integer(0x0409) },
            { "client-error-document-format-not-supported", new Integer(0x040A) },
            { "client-error-attributes-or-values-not-supported",
                    new Integer(0x040B) },
            { "client-error-uri-scheme-not-supported", new Integer(0x040C) },
            { "client-error-charset-not-supported", new Integer(0x040D) },
            { "client-error-conflicting-attributes", new Integer(0x040E) },
            { "client-error-compression-not-supported", new Integer(0x040F) },
            { "client-error-compression-error", new Integer(0x0410) },
            { "client-error-document-format-error", new Integer(0x0411) },
            { "client-error-document-access-error", new Integer(0x0412) },

            /*
             * Server Error Status Codes
             * This class of status codes indicates cases in which the IPP object is aware
             * that it has erred or is incapable of performing the request. The IPP object
             * SHOULD include a message containing an explanation of the error situation, and
             * whether it is a temporary or permanent condition.
             */
            { "server-error-internal-error", new Integer(0x0500) },
            { "server-error-operation-not-supported", new Integer(0x0501) },
            { "server-error-service-unavailable", new Integer(0x0502) },
            { "server-error-version-not-supported", new Integer(0x0503) },
            { "server-error-device-error", new Integer(0x0504) },
            { "server-error-temporary-error", new Integer(0x0505) },
            { "server-error-not-accepting-jobs", new Integer(0x0506) },
            { "server-error-busy", new Integer(0x0507) },
            { "server-error-job-canceled", new Integer(0x0508) },
            { "server-error-multiple-document-jobs-not-supported",
                    new Integer(0x0509) } };
}
