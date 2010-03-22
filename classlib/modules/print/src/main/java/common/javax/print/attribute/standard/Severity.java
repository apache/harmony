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
 * table values are obtained from rfc2911: internet printing protocol/1.1: 
 * model and semantics, section 4.4.12 http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public final class Severity extends EnumSyntax implements Attribute {
    private static final long serialVersionUID = 8781881462717925380L;

    public static final Severity REPORT = new Severity(0);

    public static final Severity WARNING = new Severity(1);

    public static final Severity ERROR = new Severity(2);

    private static final Severity[] enumValueTable = { REPORT, WARNING, ERROR };

    private static final String[] stringTable = { "report", "warning", "error" };

    protected Severity(int value) {
        super(value);
    }

    public final Class<? extends Attribute> getCategory() {
        return Severity.class;
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final String getName() {
        return "severity";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
