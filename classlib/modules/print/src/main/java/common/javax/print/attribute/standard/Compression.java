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

public class Compression extends EnumSyntax implements DocAttribute {
    private static final long serialVersionUID = -5716748913324997674L;

    public static final Compression NONE = new Compression(0);

    public static final Compression DEFLATE = new Compression(1);

    public static final Compression GZIP = new Compression(2);

    public static final Compression COMPRESS = new Compression(3);

    private static final Compression[] enumValueTable = { NONE, DEFLATE, GZIP, COMPRESS };

    private static final String[] stringTable = { "none", "deflate", "gzip", "compress" };

    protected Compression(int value) {
        super(value);
    }

    public final Class<? extends Attribute> getCategory() {
        return Compression.class;
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final String getName() {
        return "compression";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
