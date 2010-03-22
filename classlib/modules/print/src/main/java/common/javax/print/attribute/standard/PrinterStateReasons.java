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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;

/*
 * Table values are obtained from RFC2911: Internet Printing Protocol/1.1: 
 * Model and Semantics, section 4.4.11, http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public final class PrinterStateReasons extends HashMap<PrinterStateReason, Severity> implements
        PrintServiceAttribute {
    private static final long serialVersionUID = -3731791085163619457L;

    public PrinterStateReasons() {
        super();
    }

    public PrinterStateReasons(int initialCapacity) {
        super(initialCapacity);
    }

    public PrinterStateReasons(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PrinterStateReasons(Map<PrinterStateReason, Severity> map) {
        this();
        for (Map.Entry<PrinterStateReason, Severity> mapEntry : map.entrySet()) {
            put(mapEntry.getKey(), mapEntry.getValue());
        }
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterStateReasons.class;
    }

    public final String getName() {
        return "printer-state-reasons";
    }

    @Override
    public Severity put(PrinterStateReason reason, Severity severity) {
        if (reason == null) {
            throw new NullPointerException("Reason is null");
        }
        if (severity == null) {
            throw new NullPointerException("Severity is null");
        }
        return super.put(reason, severity);
    }

    public Set<PrinterStateReason> printerStateReasonSet(Severity severity) {
        if (severity == null) {
            throw new NullPointerException("Severity is null");
        }
        Set<PrinterStateReason> set = new HashSet<PrinterStateReason>();
        for (Map.Entry<PrinterStateReason, Severity> mapEntry : entrySet()) {
            if (mapEntry.getValue() == severity) {
                set.add(mapEntry.getKey());
            }
        }
        return Collections.unmodifiableSet(set);
    }
}
