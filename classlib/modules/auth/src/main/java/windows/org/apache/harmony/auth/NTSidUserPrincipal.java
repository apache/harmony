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

/**
 * @author Alexander V. Astapchuk
 */
package org.apache.harmony.auth;

/** 
 * A principal which holds information about NT user basing on its sid.
 */
public class NTSidUserPrincipal extends NTSid {

    private static final long serialVersionUID = -76980455882379611L;

    /**
     * A constructor which takes user SID as its only argument. 
     * @param sid user SID
     */
    public NTSidUserPrincipal(String sid) {
        super(sid);
    }

    /**
     * A constructor which takes an extended set of information - user SID, 
     * its name and its domain name 
     * @param sid user SID
     * @param objName user name
     * @param objDomain name of user's domain
     */
    public NTSidUserPrincipal(String sid, String objName, String objDomain) {
        super(sid, objName, objDomain);
    }
}