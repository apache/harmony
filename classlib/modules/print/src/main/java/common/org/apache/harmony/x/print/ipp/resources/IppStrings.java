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

/** This class contains messages that are used in IppOperation and related classes. 
 * Most of messages can be found in RFC 2911/2910/3380
 * http://ietf.org/rfc/rfc2911.txt?number=2911
 * http://ietf.org/rfc/rfc2910.txt?number=2910
 * http://ietf.org/rfc/rfc3380.txt?number=3380
 */

public class IppStrings extends ListResourceBundle {

    public Object[][] getContents() {
        return strings;
    }

    static final Object[][] strings = { { "IppOperation.PRINT_JOB", "Print-Job" },
            { "IppOperation.PRINT_URI", "Print-URI" },
            { "IppOperation.VALIDATE_JOB", "Validate-Job" },
            { "IppOperation.CREATE_JOB", "Create-Job" },
            { "IppOperation.SEND_DOCUMENT", "Send-Document" },
            { "IppOperation.SEND_URI", "Send-URI" },
            { "IppOperation.CANCEL_JOB", "Cancel-Job" },
            { "IppOperation.GET_JOB_ATTRIBUTES", "Get-Job-Attributes" },
            { "IppOperation.GET_JOBS", "Get-Jobs" },
            { "IppOperation.GET_PRINTER_ATTRIBUTES", "Get-Printer-Attributes" },
            { "IppOperation.HOLD_JOB", "Hold-Job" },
            { "IppOperation.RELEASE_JOB", "Release-Job" },
            { "IppOperation.RESTART_JOB", "Restart-Job" },
            { "IppOperation.RESERVED_FOR_A_FUTURE_OPERATION",
                    "reserved for a future operation" },
            { "IppOperation.PAUSE_PRINTER", "Pause-Printer" },
            { "IppOperation.RESUME_PRINTER", "Resume-Printer" },
            { "IppOperation.PURGE_JOBS", "Purge-Jobs" },
            { "IppOperation.TAG_CUPS_GET_DEFAULT", "CUPS Get default printer" },
            { "IppOperation.TAG_CUPS_GET_PRINTERS", "CUPS Get all printers" },

            { "IppAttributesGroup.OPERATION_ATTRIBUTES", "Operation Attributes" },
            { "IppAttributesGroup.JOB_TEMPLATE_ATTRIBUTES",
                    "Job Template Attributes" },
            { "IppAttributesGroup.JOB_OBJECT_ATTRIBUTES",
                    "Job Object Attributes" },
            { "IppAttributesGroup.GET_JOB_ATTRIBUTES", "Get Job Attributes" },
            { "IppAttributesGroup.GET_PRINTER_ATTRIBUTES",
                    "Get Printer Attributes" },
            { "IppAttributesGroup.RESERVED", "Reserved For Future" },
            { "IppAttributesGroup.END_OF_ATTRIBUTES", "end-of-attributes-tag" },
            { "IppAttributesGroup.UNSUPPORTED_ATTRIBUTES",
                    "unsupported-attributes-tag" } };
}
