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
import javax.print.attribute.PrintServiceAttribute;

/*
 * Table values are obtained from RFC2911: Internet Printing Protocol/1.1: 
 * Model and Semantics, section 4.4.11, http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public final class PrinterState extends EnumSyntax implements PrintServiceAttribute {
    private static final long serialVersionUID = -649578618346507718L;

    public static final PrinterState UNKNOWN = new PrinterState(0);

    public static final PrinterState IDLE = new PrinterState(3);

    public static final PrinterState PROCESSING = new PrinterState(4);

    public static final PrinterState STOPPED = new PrinterState(5);

    private static final PrinterState[] enumValueTable = { UNKNOWN, null, null, IDLE,
            PROCESSING, STOPPED };

    private static final String[] stringTable = { "unknown", null, null, "idle", "processing",
            "stopped" };

    protected PrinterState(int value) {
        super(value);
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterState.class;
    }

    public final String getName() {
        return "printer-state";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
