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

/**
 * A <code>Control</code> corresponds to a control used in LDAPv3. Controls
 * are specified in RFC2251. A control provides extra information related to an
 * operation on the server. It may be a request control which is sent when a
 * request is made to the LDAPv3 server or it may be a response control which is
 * received from the LDAPv3 server.
 */
public interface Control extends Serializable {

    /**
     * The constant indicating that a <code>Control</code> is critical.
     */
    public static final boolean CRITICAL = true;

    /**
     * The constant indicating that a <code>Control</code> is not critical.
     */
    public static final boolean NONCRITICAL = false;

    /**
     * Returns the object ID assigned to this <code>Control</code> instance.
     * (see RFC2251).
     * 
     * @return the object ID assigned to the control
     */
    String getID();

    /**
     * Indicates whether this <code>Control</code> instance is critical.
     * 
     * @return true if critical, otherwise false
     */
    boolean isCritical();

    /**
     * Returns the value of this <code>Control</code> instance encoded using
     * ASN.1 Basic Encoding Rules (BER).
     * 
     * @return the encoded value of this <code>Control</code> instance
     */
    byte[] getEncodedValue();

}
