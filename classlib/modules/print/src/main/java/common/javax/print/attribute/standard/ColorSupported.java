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

public final class ColorSupported extends EnumSyntax implements PrintServiceAttribute {
    private static final long serialVersionUID = -2700555589688535545L;

    public static final ColorSupported NOT_SUPPORTED = new ColorSupported(0);

    public static final ColorSupported SUPPORTED = new ColorSupported(1);

    private static final String[] stringTable = { "false", "true" };

    private static final ColorSupported[] enumValueTable = { NOT_SUPPORTED, SUPPORTED };

    protected ColorSupported(int value) {
        super(value);
    }

    public final Class<? extends Attribute> getCategory() {
        return ColorSupported.class;
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable;
    }

    public final String getName() {
        return "color-supported";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
