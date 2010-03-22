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
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

public final class SheetCollate extends EnumSyntax implements DocAttribute, PrintJobAttribute,
        PrintRequestAttribute {
    private static final long serialVersionUID = 7080587914259873003L;

    public static final SheetCollate UNCOLLATED = new SheetCollate(0);

    public static final SheetCollate COLLATED = new SheetCollate(1);

    private static final String[] stringTable = { "uncollated", "collated" };

    private static final SheetCollate[] enumValueTable = { UNCOLLATED, COLLATED };

    protected SheetCollate(int value) {
        super(value);
    }

    public final Class<? extends Attribute> getCategory() {
        return SheetCollate.class;
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final String getName() {
        return "sheet-collate";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
