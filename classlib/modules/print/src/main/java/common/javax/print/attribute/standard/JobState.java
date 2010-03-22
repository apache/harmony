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
import javax.print.attribute.PrintJobAttribute;

/*
 * Table values are obtained from RFC2911: Internet Printing Protocol/1.1: 
 * Model and Semantics, section 4.3.7, http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public class JobState extends EnumSyntax implements PrintJobAttribute {
    private static final long serialVersionUID = 400465010094018920L;

    public static final JobState UNKNOWN = new JobState(0);

    public static final JobState PENDING = new JobState(3);

    public static final JobState PENDING_HELD = new JobState(4);

    public static final JobState PROCESSING = new JobState(5);

    public static final JobState PROCESSING_STOPPED = new JobState(6);

    public static final JobState CANCELED = new JobState(7);

    public static final JobState ABORTED = new JobState(8);

    public static final JobState COMPLETED = new JobState(9);

    private static final JobState[] enumValueTable = { UNKNOWN, null, null, PENDING,
            PENDING_HELD, PROCESSING, PROCESSING_STOPPED, CANCELED, ABORTED, COMPLETED };

    private static final String[] stringTable = { "unknown", null, null, "pending",
            "pending-held", "processing", "processing-stopped", "canceled", "aborted",
            "completed" };

    protected JobState(int value) {
        super(value);
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final Class<? extends Attribute> getCategory() {
        return JobState.class;
    }

    public final String getName() {
        return "job-state";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
