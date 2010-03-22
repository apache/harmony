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

package org.apache.harmony.jndi.provider.ldap;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.CommunicationException;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.LimitExceededException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.OperationNotSupportedException;
import javax.naming.PartialResultException;
import javax.naming.ServiceUnavailableException;
import javax.naming.SizeLimitExceededException;
import javax.naming.TimeLimitExceededException;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.ldap.LdapName;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.provider.ldap.parser.FilterParser;
import org.apache.harmony.jndi.provider.ldap.parser.LdapUrlParser;
import org.apache.harmony.jndi.provider.ldap.parser.ParseException;

@SuppressWarnings("boxing")
public class LdapUtils {
    private static HashMap<Integer, Class<?>> errorCodesMap = new HashMap<Integer, Class<?>>();

    static {
        errorCodesMap.put(1, NamingException.class);
        errorCodesMap.put(2, CommunicationException.class);
        errorCodesMap.put(3, TimeLimitExceededException.class);
        errorCodesMap.put(4, SizeLimitExceededException.class);
        errorCodesMap.put(7, AuthenticationNotSupportedException.class);
        errorCodesMap.put(8, AuthenticationNotSupportedException.class);
        errorCodesMap.put(9, PartialResultException.class);
        errorCodesMap.put(11, LimitExceededException.class);
        errorCodesMap.put(12, OperationNotSupportedException.class);
        errorCodesMap.put(13, AuthenticationNotSupportedException.class);
        errorCodesMap.put(16, NoSuchAttributeException.class);
        errorCodesMap.put(17, InvalidAttributeIdentifierException.class);
        errorCodesMap.put(18, InvalidSearchFilterException.class);
        errorCodesMap.put(19, InvalidAttributeValueException.class);
        errorCodesMap.put(20, AttributeInUseException.class);
        errorCodesMap.put(21, InvalidAttributeValueException.class);
        errorCodesMap.put(32, NameNotFoundException.class);
        errorCodesMap.put(33, NamingException.class);
        errorCodesMap.put(34, InvalidNameException.class);
        errorCodesMap.put(36, NamingException.class);
        errorCodesMap.put(48, AuthenticationNotSupportedException.class);
        errorCodesMap.put(49, AuthenticationException.class);
        errorCodesMap.put(50, NoPermissionException.class);
        errorCodesMap.put(51, ServiceUnavailableException.class);
        errorCodesMap.put(52, ServiceUnavailableException.class);
        errorCodesMap.put(53, OperationNotSupportedException.class);
        errorCodesMap.put(54, NamingException.class);
        errorCodesMap.put(64, InvalidNameException.class);
        errorCodesMap.put(65, SchemaViolationException.class);
        errorCodesMap.put(66, ContextNotEmptyException.class);
        errorCodesMap.put(67, SchemaViolationException.class);
        errorCodesMap.put(68, NameAlreadyBoundException.class);
        errorCodesMap.put(69, SchemaViolationException.class);
        errorCodesMap.put(71, NamingException.class);
        errorCodesMap.put(80, NamingException.class);
    }

    public static Filter parseFilter(String filter, Object[] args)
            throws InvalidSearchFilterException {
        if (filter == null) {
            // ldap.28=Parameter of filter should not be null
            throw new NullPointerException(Messages.getString("ldap.28")); //$NON-NLS-1$
        }

        FilterParser parser = new FilterParser(filter);
        
        if (args == null) {
            args = new Object[0];
        }
        
        parser.setArgs(args);
        
        try {
            return parser.parse();
        } catch (ParseException e) {
            // ldap.29=Invalid search filter
            InvalidSearchFilterException ex = new InvalidSearchFilterException(
                    Messages.getString("ldap.29")); //$NON-NLS-1$
            ex.setRootCause(e);
            throw ex;
        }
    }

    public static LdapUrlParser parserURL(String url, boolean isAllowedQuery)
            throws InvalidNameException {
        if (url == null) {
            // ldap.2B=LDAP URL should not be null
            throw new NullPointerException(Messages.getString("ldap.2B")); //$NON-NLS-1$
        }

        LdapUrlParser parser = new LdapUrlParser(url);
        try {
            parser.parseURL();
        } catch (ParseException e) {
            // ldap.2C=Invalid LDAP URL
            IllegalArgumentException ex = new IllegalArgumentException(Messages
                    .getString("ldap.2C")); //$NON-NLS-1$
            ex.initCause(e);
            throw ex;
        }

        if (!isAllowedQuery
                && (parser.getFilter() != null || parser.getControls() != null)) {
            // ldap.2D=LDAP URL may only contain host, port and dn components
            throw new InvalidNameException(Messages.getString("ldap.2D")); //$NON-NLS-1$
        }

        return parser;
    }

    public static NamingException getExceptionFromResult(LdapResult result) {
        int errorCode = result.getResultCode();
        // 0 means successful
        if (errorCode == 0) {
            return null;
        }

        Class<?> exceptionClass = errorCodesMap.get(errorCode);
        // not in map, using NamingException
        if (exceptionClass == null) {
            exceptionClass = NamingException.class;
        }

        try {
            Constructor<?> constructor = exceptionClass
                    .getConstructor(new Class[] { String.class });
            String message = null;

            if (result.getErrorMessage() != null
                    && !result.getErrorMessage().equals("")) { //$NON-NLS-1$
                // ldap.34=[LDAP: error code {0} - {1}]
                message = Messages.getString("ldap.34", new Object[] { //$NON-NLS-1$
                        errorCode, result.getErrorMessage() });
            } else {
                // ldap.35=[LDAP: error code {0}]
                message = Messages.getString("ldap.35", //$NON-NLS-1$
                        new Object[] { errorCode });
            }

            return (NamingException) constructor
                    .newInstance(new Object[] { message });
        } catch (Exception e) {
            // ldap.35=[LDAP: error code {0}]
            return new NamingException(Messages.getString("ldap.35", //$NON-NLS-1$
                    new Object[] { errorCode }));
        }
    }

    /**
     * convert absolute dn to the dn relatived to the dn of
     * <code>targetContextDN</code>.
     * 
     * @param dn
     *            absolute dn
     * @param base
     *            base dn of the relative name
     * @return dn relatived to the <code>dn</code> of <code>base</code>
     * @throws NamingException
     * @throws InvalidNameException
     */
    public static String convertToRelativeName(String dn, String base)
            throws InvalidNameException, NamingException {
        return convertToRelativeName(new LdapName(dn), new LdapName(base))
                .toString();
    }

    public static LdapName convertToRelativeName(LdapName dn, LdapName base)
            throws NamingException {
        if (base.size() == 0) {
            return dn;
        }

        if (dn.size() < base.size()) {
            // TODO add error message
            throw new NamingException("");
        }

        Name prefix = dn.getPrefix(base.size());
        if (!prefix.equals(base)) {
            // TODO add error message
            throw new NamingException("");
        }

        return (LdapName) dn.getSuffix(base.size());

    }
    /**
     * Get SocketFactory according three properties:
     * "java.naming.ldap.factory.socket", "java.naming.security.protocol" and
     * protocol defined in URL. If "java.naming.ldap.factory.socket" set, then
     * use it. otherwise check protocol defined in URL: "ldaps" use
     * <code>SSLSocketFactory.getDefault()</code> to retrieve factory; If is
     * "ldap", check whether "java.naming.security.protocol" is set to "ssl", if
     * set, use <code>SSLSocketFactory.getDefault()</code> get factory.
     * 
     * @param envmt
     * @param isLdaps
     * @return
     * @throws ConfigurationException
     */
    public static SocketFactory getSocketFactory(Hashtable<?, ?> envmt,
            boolean isLdaps) throws ConfigurationException {
        String factoryName = (String) envmt
                .get("java.naming.ldap.factory.socket");

        SocketFactory factory = null;

        // if "java.naming.ldap.factory.socket" set, use it
        if (factoryName != null && !("".equals(factoryName))) {
            try {
                factory = (SocketFactory) classForName(factoryName)
                        .newInstance();
            } catch (Exception e) {
                ConfigurationException ex = new ConfigurationException();
                ex.setRootCause(e);
                throw ex;
            }
        }

        // factory name not set
        if (factory == null) {
            if (isLdaps) {
                factory = SSLSocketFactory.getDefault();
            }
            // It's case sensitive in RI
            else if ("ssl".equalsIgnoreCase((String) envmt
                    .get(Context.SECURITY_PROTOCOL))) {
                factory = SSLSocketFactory.getDefault();
            } else {
                factory = SocketFactory.getDefault();
            }
        }

        return factory;
    }

    public static boolean isLdapsURL(String url) {
        return url.toLowerCase().startsWith("ldaps://");
    }

    private static Class<?> classForName(final String className)
            throws ClassNotFoundException {
        Class<?> cls = null;
        // try thread context class loader first
        try {
            cls = Class.forName(className, true, Thread.currentThread()
                    .getContextClassLoader());
        } catch (ClassNotFoundException e) {
            // try system class loader second
            cls = Class.forName(className, true, ClassLoader
                    .getSystemClassLoader());
        }

        return cls;
    }
}
