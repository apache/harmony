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

import java.util.HashMap;

import javax.naming.AuthenticationNotSupportedException;
import javax.naming.CommunicationException;
import javax.naming.LimitExceededException;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.OperationNotSupportedException;
import javax.naming.PartialResultException;
import javax.naming.ServiceUnavailableException;
import javax.naming.SizeLimitExceededException;
import javax.naming.TimeLimitExceededException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.NoSuchAttributeException;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * Some useful utilities
 */
public class Util {

    private static HashMap errorCodes = new HashMap();
    
    static { 
        // TODO Add every needed LDAP errror code description and exception
        errorCodes.put(1,new NamingException(Messages.getString("ldap.0A")));
        errorCodes.put(2,new CommunicationException(Messages.getString("ldap.0B")));
        errorCodes.put(3,new TimeLimitExceededException(Messages.getString("ldap.0C")));
        errorCodes.put(4,new SizeLimitExceededException(Messages.getString("ldap.0D")));
        errorCodes.put(7,new AuthenticationNotSupportedException(Messages.getString("ldap.0E")));
        errorCodes.put(8,new AuthenticationNotSupportedException(Messages.getString("ldap.0F")));
        errorCodes.put(9,new PartialResultException(Messages.getString("ldap.10")));
        errorCodes.put(11,new LimitExceededException(Messages.getString("ldap.11")));
        errorCodes.put(16,new NoSuchAttributeException(Messages.getString("ldap.12")));
        errorCodes.put(18,new InvalidSearchFilterException(Messages.getString("ldap.13")));
        errorCodes.put(50,new NoPermissionException(Messages.getString("ldap.14")));
        errorCodes.put(51,new ServiceUnavailableException(Messages.getString("ldap.15")));
        errorCodes.put(53,new OperationNotSupportedException(Messages.getString("ldap.16")));
        errorCodes.put(80,new NamingException(Messages.getString("ldap.17")));
    }

    /**
     * Return the correct exception for a given error code
     * 
     * @param code
     *            error code
     * @return the correct NamingException
     */
    public static NamingException getExceptionFromErrorCode(int code) {
        if (code == 0) {
            return null;
        }
        if (errorCodes.get(code) == null) {
            return new NamingException(Messages.getString("ldap.18") + " "
                    + code + "]");
        }
        return (NamingException) errorCodes.get(code);
    }

}
