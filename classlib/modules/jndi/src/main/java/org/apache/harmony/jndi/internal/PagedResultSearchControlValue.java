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

package org.apache.harmony.jndi.internal;

/**
 * Represents the following ASN1 Syntax on java Types:
 * 
 * <pre>
 *  
 * realSearchControlValue ::= SEQUENCE {
 *   size            INTEGER (0..maxInt),
 *                           -- requested page size from client
 *                           -- result set size estimate from server
 *   cookie          OCTET STRING
 * } 
 * </pre>
 */
public class PagedResultSearchControlValue {

    private byte[] cookie;

    private int size;

    /**
     * Constructor
     * 
     * @param size
     *            an int with the size
     * @param cookie
     *            the cookie from the server
     */
    public PagedResultSearchControlValue(int size, byte[] cookie) {
        this.cookie = cookie;
        this.size = size;
    }

    /**
     * Getter method for cookie
     * 
     * @return the cookie
     */
    public byte[] getCookie() {
        return cookie;
    }

    /**
     * Getter method for size
     * 
     * @return the size
     */
    public int getSize() {
        return size;
    }

}
