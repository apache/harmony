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

package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;

/*
 * Table values are obtained from RFC2911: Internet Printing Protocol/1.1: 
 * Model and Semantics, section 4.3.8, http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public class JobStateReason extends EnumSyntax implements Attribute {
    private static final long serialVersionUID = -8765894420449009168L;

    public static final JobStateReason JOB_INCOMING = new JobStateReason(0);

    public static final JobStateReason JOB_DATA_INSUFFICIENT = new JobStateReason(1);

    public static final JobStateReason DOCUMENT_ACCESS_ERROR = new JobStateReason(2);

    public static final JobStateReason SUBMISSION_INTERRUPTED = new JobStateReason(3);

    public static final JobStateReason JOB_OUTGOING = new JobStateReason(4);

    public static final JobStateReason JOB_HOLD_UNTIL_SPECIFIED = new JobStateReason(5);

    public static final JobStateReason RESOURCES_ARE_NOT_READY = new JobStateReason(6);

    public static final JobStateReason PRINTER_STOPPED_PARTLY = new JobStateReason(7);

    public static final JobStateReason PRINTER_STOPPED = new JobStateReason(8);

    public static final JobStateReason JOB_INTERPRETING = new JobStateReason(9);

    public static final JobStateReason JOB_QUEUED = new JobStateReason(10);

    public static final JobStateReason JOB_TRANSFORMING = new JobStateReason(11);

    public static final JobStateReason JOB_QUEUED_FOR_MARKER = new JobStateReason(12);

    public static final JobStateReason JOB_PRINTING = new JobStateReason(13);

    public static final JobStateReason JOB_CANCELED_BY_USER = new JobStateReason(14);

    public static final JobStateReason JOB_CANCELED_BY_OPERATOR = new JobStateReason(15);

    public static final JobStateReason JOB_CANCELED_AT_DEVICE = new JobStateReason(16);

    public static final JobStateReason ABORTED_BY_SYSTEM = new JobStateReason(17);

    public static final JobStateReason UNSUPPORTED_COMPRESSION = new JobStateReason(18);

    public static final JobStateReason COMPRESSION_ERROR = new JobStateReason(19);

    public static final JobStateReason UNSUPPORTED_DOCUMENT_FORMAT = new JobStateReason(20);

    public static final JobStateReason DOCUMENT_FORMAT_ERROR = new JobStateReason(21);

    public static final JobStateReason PROCESSING_TO_STOP_POINT = new JobStateReason(22);

    public static final JobStateReason SERVICE_OFF_LINE = new JobStateReason(23);

    public static final JobStateReason JOB_COMPLETED_SUCCESSFULLY = new JobStateReason(24);

    public static final JobStateReason JOB_COMPLETED_WITH_WARNINGS = new JobStateReason(25);

    public static final JobStateReason JOB_COMPLETED_WITH_ERRORS = new JobStateReason(26);

    public static final JobStateReason JOB_RESTARTABLE = new JobStateReason(27);

    public static final JobStateReason QUEUED_IN_DEVICE = new JobStateReason(28);

    private static final JobStateReason[] enumValueTable = { JOB_INCOMING,
            JOB_DATA_INSUFFICIENT, DOCUMENT_ACCESS_ERROR, SUBMISSION_INTERRUPTED, JOB_OUTGOING,
            JOB_HOLD_UNTIL_SPECIFIED, RESOURCES_ARE_NOT_READY, PRINTER_STOPPED_PARTLY,
            PRINTER_STOPPED, JOB_INTERPRETING, JOB_QUEUED, JOB_TRANSFORMING,
            JOB_QUEUED_FOR_MARKER, JOB_PRINTING, JOB_CANCELED_BY_USER,
            JOB_CANCELED_BY_OPERATOR, JOB_CANCELED_AT_DEVICE, ABORTED_BY_SYSTEM,
            UNSUPPORTED_COMPRESSION, COMPRESSION_ERROR, UNSUPPORTED_DOCUMENT_FORMAT,
            DOCUMENT_FORMAT_ERROR, PROCESSING_TO_STOP_POINT, SERVICE_OFF_LINE,
            JOB_COMPLETED_SUCCESSFULLY, JOB_COMPLETED_WITH_WARNINGS, JOB_COMPLETED_WITH_ERRORS,
            JOB_RESTARTABLE, QUEUED_IN_DEVICE };

    private static final String[] stringTable = { "job-incoming", "job-data-insufficient",
            "document-access-error", "submission-interrupted", "job-outgoing",
            "job-hold-until-specified", "resources-are-not-ready", "printer-stopped-partly",
            "printer-stopped", "job-interpreting", "job-queued", "job-transforming",
            "job-queued-for-marker", "job-printing", "job-canceled-by-user",
            "job-canceled-by-operator", "job-canceled-at-device", "aborted-by-system",
            "unsupported-compression", "compression-error", "unsupported-document-format",
            "document-format-error", "processing-to-stop-point", "service-off-line",
            "job-completed-successfully", "job-completed-with-warnings",
            "job-completed-with-errors", "job-restartable", "queued-in-device" };

    protected JobStateReason(int value) {
        super(value);
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final Class<? extends Attribute> getCategory() {
        return JobStateReason.class;
    }

    public final String getName() {
        return "job-state-reason";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
