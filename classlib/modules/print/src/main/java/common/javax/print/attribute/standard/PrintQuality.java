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
 * Table values are obtained from RFC2911: Internet Printing Protocol/1.1: 
 * Model and Semantics, section 4.2.13, http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public class PrintQuality extends EnumSyntax implements DocAttribute, PrintJobAttribute,
        PrintRequestAttribute {
    private static final long serialVersionUID = -3072341285225858365L;

    public static final PrintQuality DRAFT = new PrintQuality(3);

    public static final PrintQuality NORMAL = new PrintQuality(4);

    public static final PrintQuality HIGH = new PrintQuality(5);

    private static final PrintQuality[] enumValueTable = { DRAFT, NORMAL, HIGH };

    private static final String[] stringTable = { "draft", "normal", "high" };

    protected PrintQuality(int value) {
        super(value);
    }

    public final Class<? extends Attribute> getCategory() {
        return PrintQuality.class;
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final String getName() {
        return "print-quality";
    }

    @Override
    protected int getOffset() {
        return 3;
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
