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

/**
 * TODO: JavaDoc
 */
public final class ManageReferralControl extends BasicControl {

    private static final long serialVersionUID = 3017756160149982566L;

    public static final String OID = "2.16.840.1.113730.3.4.2"; //$NON-NLS-1$

    public ManageReferralControl() {
        super(OID, Control.CRITICAL, null);
    }

    public ManageReferralControl(boolean criticality) {
        super(OID, criticality, null);
    }

}
