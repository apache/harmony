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
 * Model and Semantics, section 4.1.6, 4.4.27 http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public class ReferenceUriSchemesSupported extends EnumSyntax implements Attribute {
    private static final long serialVersionUID = -8989076942813442805L;

    public static final ReferenceUriSchemesSupported FTP = new ReferenceUriSchemesSupported(0);

    public static final ReferenceUriSchemesSupported HTTP = new ReferenceUriSchemesSupported(1);

    public static final ReferenceUriSchemesSupported HTTPS = new ReferenceUriSchemesSupported(2);

    public static final ReferenceUriSchemesSupported GOPHER = new ReferenceUriSchemesSupported(
            3);

    public static final ReferenceUriSchemesSupported NEWS = new ReferenceUriSchemesSupported(4);

    public static final ReferenceUriSchemesSupported NNTP = new ReferenceUriSchemesSupported(5);

    public static final ReferenceUriSchemesSupported WAIS = new ReferenceUriSchemesSupported(6);

    public static final ReferenceUriSchemesSupported FILE = new ReferenceUriSchemesSupported(7);

    private static final ReferenceUriSchemesSupported[] enumValueTable = { FTP, HTTP, HTTPS,
            GOPHER, NEWS, NNTP, WAIS, FILE, };

    private static final String[] stringTable = { "ftp", "http", "https", "gopher", "news",
            "nntp", "wais", "file" };

    protected ReferenceUriSchemesSupported(int value) {
        super(value);
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final Class<? extends Attribute> getCategory() {
        return ReferenceUriSchemesSupported.class;
    }

    public final String getName() {
        return "reference-uri-schemes-supported";
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
