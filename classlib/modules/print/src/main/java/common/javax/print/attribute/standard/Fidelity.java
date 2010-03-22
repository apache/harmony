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
import javax.print.attribute.PrintRequestAttribute;

public final class Fidelity extends EnumSyntax implements PrintJobAttribute,
        PrintRequestAttribute {
    private static final long serialVersionUID = 6320827847329172308L;

    public static final Fidelity FIDELITY_TRUE = new Fidelity(0);

    public static final Fidelity FIDELITY_FALSE = new Fidelity(1);

    private static final Fidelity[] enumValueTable = { FIDELITY_TRUE, FIDELITY_FALSE };

    private static final String[] stringTable = { "true", "false" };

    protected Fidelity(int value) {
        super(value);
    }

    public final Class<? extends Attribute> getCategory() {
        return Fidelity.class;
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable;
    }

    public final String getName() {
        return "ipp-attribute-fidelity";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable;
    }
}
