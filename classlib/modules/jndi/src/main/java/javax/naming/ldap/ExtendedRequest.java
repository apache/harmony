/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.naming.ldap;

import java.io.Serializable;
import javax.naming.NamingException;

/**
 * See RFC2251 for the definition of an <code>ExtendedRequest</code>.
 * 
 * @see ExtendedResponse
 */
public interface ExtendedRequest extends Serializable {

    /**
     * Gets the object ID assigned to this request. (see RFC2251)
     * 
     * @return the object ID assigned to this request
     */
    String getID();

    /**
     * Gets the request encoded using ASN.1 Basic Encoding Rules (BER).
     * 
     * @return the request encoded using ASN.1 BER
     */
    byte[] getEncodedValue();

    /**
     * Returns a suitable <code>ExtendedResponse</code> object for this
     * request. The method parameters provide the data obtained by the service
     * provider from the LDAP server for this request.
     * 
     * @param s
     *            the object identifier of the response control. May be null.
     * @param value
     *            holds the value of the response control as raw ASN.1 BER
     *            encoded bytes, including the tag and length of the response
     *            but excluding its OID.
     * @param i
     *            specifies the start index of useable data within array
     *            <code>value</code>.
     * @param i2
     *            specifies the number of data bytes to use within array
     *            <code>value</code>.
     * @return a suitable <code>ExtendedResponse</code> object for this
     *         request.
     * 
     * @throws NamingException
     *             If an error is encountered.
     */
    ExtendedResponse createExtendedResponse(String s, byte[] value, int i,
            int i2) throws NamingException;

}
