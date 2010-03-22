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
 * Model and Semantics, Appendix C, http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public class MediaName extends Media implements Attribute {
    private static final long serialVersionUID = 4653117714524155448L;

    public static final MediaName NA_LETTER_WHITE = new MediaName(0);

    public static final MediaName NA_LETTER_TRANSPARENT = new MediaName(1);

    public static final MediaName ISO_A4_WHITE = new MediaName(2);

    public static final MediaName ISO_A4_TRANSPARENT = new MediaName(3);

    private static final MediaName[] enumValueTable = { NA_LETTER_WHITE, NA_LETTER_TRANSPARENT,
            ISO_A4_WHITE, ISO_A4_TRANSPARENT };

    private static final String[] stringTable = { "na-letter-white", "na-letter-transparent",
            "iso-a4-white", "iso-a4-transparent" };

    protected MediaName(int value) {
        super(value);
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
