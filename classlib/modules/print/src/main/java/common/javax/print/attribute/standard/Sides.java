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

/*
 * table values are obtained from rfc2911: internet printing protocol/1.1: 
 * model and semantics, section 4.2.8 http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public final class Sides extends EnumSyntax implements DocAttribute, PrintJobAttribute,
        PrintRequestAttribute {
    private static final long serialVersionUID = -6890309414893262822L;

    public static final Sides ONE_SIDED = new Sides(0);

    public static final Sides TWO_SIDED_LONG_EDGE = new Sides(1);

    public static final Sides TWO_SIDED_SHORT_EDGE = new Sides(2);

    public static final Sides DUPLEX = TWO_SIDED_LONG_EDGE;

    public static final Sides TUMBLE = TWO_SIDED_SHORT_EDGE;

    private static final Sides[] enumValueTable = { ONE_SIDED, TWO_SIDED_LONG_EDGE,
            TWO_SIDED_SHORT_EDGE };

    private static final String[] stringTable = { "one-sided", "two-sided-long-edge",
            "two-sided-short-edge" };

    protected Sides(int value) {
        super(value);
    }

    public final Class<? extends Attribute> getCategory() {
        return Sides.class;
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final String getName() {
        return "sides";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
