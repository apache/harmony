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

package javax.naming.ldap;

import java.io.IOException;

import org.apache.harmony.jndi.internal.PagedResultSearchControlValue;

/**
 * TODO: JavaDoc
 */
public final class PagedResultsResponseControl extends BasicControl {

    private static final long serialVersionUID = -8819778744844514666L;

    public static final String OID = "1.2.840.113556.1.4.319"; //$NON-NLS-1$

    private int resultSize;

    private byte[] cookie;

    public PagedResultsResponseControl(String id, boolean criticality,
            byte[] value) throws IOException {
        super(id, criticality, value);
        PagedResultSearchControlValue pgscv = (PagedResultSearchControlValue) PagedResultsControl.ASN1_ENCODER
                .decode(value);
        resultSize = pgscv.getSize();
        cookie = pgscv.getCookie();
    }

    public byte[] getCookie() {
        if (cookie.length == 0) {
            return null;
        }
        return cookie;
    }

    public int getResultSize() {
        return resultSize;
    }

}
