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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Alexei Y. Zakharov
 */
package org.apache.harmony.jndi.provider.dns;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.spi.DirectoryManager;
import javax.naming.spi.NamingManager;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This class represents DNS context. This is the main class and the main entry
 * point to DNS service provider for JNDI.
 * 
 * @see dnsURLContext
 * @see DNSName
 * @see DNSNameParser
 */
public class DNSContext implements DirContext, Cloneable {

    // some environment property names
    public static final String LOOKUP_ATTR = "org.apache.harmony.jndi.provider.dns.lookup.attr"; //$NON-NLS-1$

    public static final String RECURSION = "org.apache.harmony.jndi.provider.dns.recursion"; //$NON-NLS-1$

    public static final String TIMEOUT_INITIAL = "org.apache.harmony.jndi.provider.dns.timeout.initial"; //$NON-NLS-1$

    public static final String TIMEOUT_RETRIES = "org.apache.harmony.jndi.provider.dns.timeout.retries"; //$NON-NLS-1$

    public static final String THREADS_MAX = "org.apache.harmony.jndi.provider.dns.threads.max"; //$NON-NLS-1$

    // used in internal methods
    private static final int NAME_CLASS_SWT = 1;

    private static final int BINDING_SWT = 2;

    private DNSNameParser nameParser;

    private Hashtable<Object, Object> environment;

    private Resolver resolver;

    private DNSName contextName;

    // default values for properties that has been read from the environment
    private boolean authoritative = ProviderConstants.DEFAULT_AUTHORITATIVE;

    private int lookupAttrType = ProviderConstants.DEFAULT_LOOKUP_ATTR_TYPE;

    private int lookupAttrClass = ProviderConstants.DEFAULT_LOOKUP_ATTR_CLASS;

    private boolean recursion = ProviderConstants.DEFAULT_RECURSION;

    private int timeoutInitial = ProviderConstants.DEFAULT_INITIAL_TIMEOUT;

    private int timeoutRetries = ProviderConstants.DEFAULT_TIMEOUT_RETRIES;

    private int maxThreads = ProviderConstants.DEFAULT_MAX_THREADS;

    // <--- start of constructor section

    /**
     * A DNS context constructor. Should not be accessed directly.
     * 
     * @param env
     *            the hash table with environment variables. The context will
     *            make a clone of given hash table.
     * @throws InvalidNameException
     *             something wrong with domain names given in
     *             <code>java.naming.provider.url</code> property
     * @throws ConfigurationException
     *             if some error occurred during parsing of configuration
     *             parameters
     * @throws NamingException
     *             if some parse error occurred
     * @throws NullPointerException
     *             if the environment is null
     * 
     */
    @SuppressWarnings("unchecked")
    DNSContext(Hashtable<?, ?> env) throws NamingException {
        nameParser = new DNSNameParser();
        if (env == null) {
            // jndi.45=environment is null
            throw new NullPointerException(Messages.getString("jndi.45")); //$NON-NLS-1$
        }
        this.environment = (Hashtable<Object, Object>) env.clone();
        parseBoolProp(Context.AUTHORITATIVE);
        parseLookupProp();
        if (environment.containsKey(RECURSION)) {
            parseBoolProp(RECURSION);
        }
        if (environment.containsKey(TIMEOUT_INITIAL)) {
            parseIntProp(TIMEOUT_INITIAL);
        }
        if (environment.containsKey(TIMEOUT_RETRIES)) {
            parseIntProp(TIMEOUT_RETRIES);
        }
        parseIntProp(THREADS_MAX);
        resolver = new Resolver(timeoutInitial, timeoutRetries, maxThreads,
                authoritative, recursion);
        parseProviderUrlProp();
    }

    /**
     * Parses integer environment property and fills appropriate internal
     * variable if necessary.
     * 
     * @param paramName
     *            name of parameter
     * @throws NumberFormatException
     *             if error encountered while parsing
     */
    private void parseIntProp(String paramName) throws NumberFormatException {
        Object tmp = environment.get(paramName);

        if (tmp != null && tmp instanceof String) {
            try {
                int n = Integer.parseInt((String) tmp);

                if (paramName.equals(TIMEOUT_RETRIES)) {
                    timeoutRetries = n;
                } else if (paramName.equals(TIMEOUT_INITIAL)) {
                    timeoutInitial = n;
                } else if (paramName.equals(THREADS_MAX)) {
                    maxThreads = n;
                }
            } catch (NumberFormatException e) {
                throw e;
            }
        }
    }

    /**
     * Parses boolean environment property and fills appropriate internal
     * variable if necessary.
     * 
     * @param paramName
     *            name of parameter
     */
    private void parseBoolProp(String paramName) {
        Object tmp = environment.get(paramName);
        boolean val = false;

        if (tmp != null) {
            if (tmp instanceof String && tmp.equals("true")) { //$NON-NLS-1$
                val = true;
            }
            if (paramName.equals(Context.AUTHORITATIVE)) {
                authoritative = val;
            } else if (paramName.equals(DNSContext.RECURSION)) {
                recursion = val;
            }
        }
    }

    /**
     * Parses "lookup attribute" environment property and fills appropriate
     * internal variable.
     * 
     * @throws ConfigurationException
     *             if some DNS type or DNS class is unknown
     */
    private void parseLookupProp() throws ConfigurationException {
        Object tmp;

        if (environment.containsKey(LOOKUP_ATTR)) {
            int k;
            String recClassName;
            String recTypeName;
            String lookupAttr;

            tmp = environment.get(LOOKUP_ATTR);
            if (tmp instanceof String) {
                lookupAttr = (String) tmp;
                k = lookupAttr.indexOf(" "); //$NON-NLS-1$
                if (k > -1) {
                    recClassName = lookupAttr.substring(0, k);

                    lookupAttrClass = ProviderMgr
                            .getRecordClassNumber(recClassName);
                    if (lookupAttrClass == -1) {
                        // jndi.46=DNS class {0} is not supported
                        throw new ConfigurationException(Messages.getString(
                                "jndi.46", recClassName));//$NON-NLS-1$
                    }
                    recTypeName = lookupAttr.substring(k).trim();
                } else {
                    lookupAttrClass = ProviderConstants.DEFAULT_LOOKUP_ATTR_CLASS;
                    recTypeName = lookupAttr.trim();
                }
                lookupAttrType = ProviderMgr.getRecordTypeNumber(recTypeName);
                if (lookupAttrType == -1) {
                    // jndi.47=DNS type {0} is not supported
                    throw new ConfigurationException(Messages.getString(
                            "jndi.47", recTypeName)); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Parses "provider URL" environment property and fills appropriate internal
     * variable.
     * 
     * @throws NamingException
     *             if such exception encountered while parsing
     */
    private void parseProviderUrlProp() throws NamingException {
        Object tmp;

        if (environment.containsKey(Context.PROVIDER_URL)) {
            tmp = environment.get(Context.PROVIDER_URL);
            if (tmp instanceof String) {
                StringTokenizer st = new StringTokenizer((String) tmp, " "); //$NON-NLS-1$

                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    DNSPseudoURL dnsURL;

                    try {
                        dnsURL = new DNSPseudoURL(token);
                        if (dnsURL.isHostIpGiven()) {
                            resolver.addInitialServer(null, dnsURL.getHost(),
                                    dnsURL.getPort(), dnsURL.getDomain());
                        } else {
                            resolver.addInitialServer(dnsURL.getHost(), null,
                                    dnsURL.getPort(), dnsURL.getDomain());
                        }
                        if (contextName == null) {
                            contextName = (DNSName) nameParser.parse(dnsURL
                                    .getDomain());
                        } else {
                            DNSName name2 = (DNSName) nameParser.parse(dnsURL
                                    .getDomain());

                            if (name2.compareTo(contextName) != 0) {
                                // jndi.48=conflicting domains: {0} and {1}
                                throw new ConfigurationException(Messages
                                        .getString(
                                                "jndi.48", contextName, name2)); //$NON-NLS-1$
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // jndi.49=Unable to parse DNS URL {0}. {1}
                        throw new ConfigurationException(Messages.getString(
                                "jndi.49", token, e.getMessage())); //$NON-NLS-1$
                    }
                }
            }
        } else {
            contextName = ProviderConstants.ROOT_ZONE_NAME_OBJ;
        }
    }

    /**
     * Constructs new DNS context with given name. This constructor will read
     * all private properties from its ancestor context. The environment will
     * not be parsed.
     * 
     * @param ancestorCtx
     *            an ancestor context to read all internal properties from
     * @param name
     *            name of newly created context in the ancestor context
     */
    DNSContext(DNSContext ancestorCtx, DNSName name) {
        super();
        initialize(ancestorCtx, name);
    }

    /**
     * Initialize all private properties from the given context.
     * 
     * @param ancestorCtx
     *            an ancestor context to read all internal properties from
     * @param name
     *            name of newly created context in the ancestor context
     */
    @SuppressWarnings("unchecked")
    private void initialize(DNSContext ancestorCtx, DNSName name) {
        contextName = (DNSName) name.clone();
        nameParser = ancestorCtx.nameParser;
        environment = (Hashtable<Object, Object>) ancestorCtx.environment.clone();
        resolver = ancestorCtx.resolver;
        authoritative = ancestorCtx.authoritative;
        lookupAttrType = ancestorCtx.lookupAttrType;
        lookupAttrClass = ancestorCtx.lookupAttrClass;
        recursion = ancestorCtx.recursion;
        timeoutInitial = ancestorCtx.timeoutInitial;
        timeoutRetries = ancestorCtx.timeoutRetries;
        maxThreads = ancestorCtx.maxThreads;
    }

    /**
     * Obtains all attributes associated with given name.
     * 
     * @param name
     *            domain name or composite name in string form
     * @return collection of found attributes
     * @throws NamingException
     *             or its subtype if such has been encountered
     * @see DNSContext#getAttributes(Name, String[]) for more details
     * @see javax.naming.directory.DirContext#getAttributes(java.lang.String)
     */
    public Attributes getAttributes(String name) throws NamingException {
        return getAttributes(convertNameFromStringForm(name), null);
    }

    /**
     * This method is not supported by the DNS provider.
     * 
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      int, javax.naming.directory.Attributes)
     */
    public void modifyAttributes(String arg0, int arg1, Attributes arg2)
            throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            ((DirContext) obj).modifyAttributes("", arg1, arg2); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * Determines all attributes associated with given name.
     * 
     * @param name
     *            the name to look for; should be an instance of either
     *            <code>DNSName</code> class or <code>CompositeName</code>
     *            class
     * @return collection of found attributes
     * @throws NamingException
     *             or its subtype if such has been encountered
     * @see DNSContext#getAttributes(Name, String[]) for more details
     * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name)
     */
    public Attributes getAttributes(Name name) throws NamingException {
        return getAttributes(name, null);
    }

    /**
     * This method is not supported by the DNS provider.
     * 
     * @see javax.naming.directory.DirContext#modifyAttributes(javax.naming.Name,
     *      int, javax.naming.directory.Attributes)
     */
    public void modifyAttributes(Name arg0, int arg1, Attributes arg2)
            throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            ((DirContext) obj).modifyAttributes("", arg1, arg2); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported by the DNS provider.
     * 
     * @see javax.naming.directory.DirContext#getSchema(java.lang.String)
     */
    public DirContext getSchema(String arg0) throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).getSchema(""); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#getSchemaClassDefinition(java.lang.String)
     */
    public DirContext getSchemaClassDefinition(String arg0)
            throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).getSchemaClassDefinition(""); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#getSchema(javax.naming.Name)
     */
    public DirContext getSchema(Name arg0) throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).getSchema(""); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#getSchemaClassDefinition(javax.naming.Name)
     */
    public DirContext getSchemaClassDefinition(Name arg0)
            throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).getSchemaClassDefinition(""); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes(String arg0, ModificationItem[] arg1)
            throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            ((DirContext) obj).modifyAttributes("", arg1); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#modifyAttributes(javax.naming.Name,
     *      javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes(Name arg0, ModificationItem[] arg1)
            throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            ((DirContext) obj).modifyAttributes("", arg1); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      javax.naming.directory.Attributes)
     */
    public NamingEnumeration<SearchResult> search(String arg0, Attributes arg1)
            throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).search("", arg1); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      javax.naming.directory.Attributes)
     */
    public NamingEnumeration<SearchResult> search(Name arg0, Attributes arg1)
            throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).search("", arg1); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#bind(java.lang.String,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void bind(String arg0, Object arg1, Attributes arg2)
            throws NamingException {
        bind(convertNameFromStringForm(arg0), arg1, arg2);
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#rebind(java.lang.String,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void rebind(String arg0, Object arg1, Attributes arg2)
            throws NamingException {
        rebind(convertNameFromStringForm(arg0), arg1, arg2);
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#bind(javax.naming.Name,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void bind(Name arg0, Object arg1, Attributes arg2)
            throws NamingException {
        ContextNamePair pair;

        try {
            pair = getTargetNamespaceContextNamePair(arg0);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair.context instanceof DirContext) {
            ((DirContext) pair.context).bind(pair.name, arg1, arg2);
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#rebind(javax.naming.Name,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void rebind(Name arg0, Object arg1, Attributes arg2)
            throws NamingException {
        ContextNamePair pair;

        try {
            pair = getTargetNamespaceContextNamePair(arg0);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair.context instanceof DirContext) {
            ((DirContext) pair.context).rebind(pair.name, arg1, arg2);
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * Retrieves attributes associated with given name.
     * 
     * @param name
     *            name to look for; should be either domain name or composite
     *            name in string form
     * @param attrNames
     *            array of attribute names that should be retrieved
     * @return collection of found attributes
     * @throws NamingException
     *             or its subtypes if such have been encountered
     * @see #getAttributes(Name) for details
     * @see javax.naming.directory.DirContext#getAttributes(java.lang.String,
     *      java.lang.String[])
     */
    public Attributes getAttributes(String name, String[] attrNames)
            throws NamingException {
        return getAttributes(convertNameFromStringForm(name), attrNames);
    }

    /**
     * Obtains attributes associated with the given name. Each members of
     * <code>attrNames</code> array should be the correct name of DNS resource
     * record type (possibly prefixed with correct DNS resource record class) as
     * specified in RFC 1035. If class name is given then it should be separated
     * with space from the type name. If class name is missed then IN class is
     * used by default.
     * 
     * @param name
     *            the name to look for; should have either <code>DNSName</code>
     *            type or <code>CompositeName</code> type
     * @param attrNames
     *            the array with names of attributes to look for; if null then
     *            all attributes will be retrieved; if empty then none of
     *            attributes will be retrieved
     * @throws InvalidNameException
     *             if <code>name</code> is neither the instance of
     *             <code>DNSName</code> nor <code>CompositeName</code> class
     *             or the first component of composite name is not a domain name
     * @throws InvalidAttributeIdentifierException
     *             if the name of DNS type or DNS class given in
     *             <code>attrNames</code> array is invalid
     * @throws NameNotFoundException
     *             if authoritative server for desired zone was contacted but
     *             given name has not been found in that zone
     * @throws ServiceUnavailableException
     *             if no authoritative server for desired name was found or all
     *             servers are dead or malfunction
     * @throws NoPermissionException
     *             if no appropriate permissions on using network resources were
     *             granted
     * @throws NullPointerException
     *             if <code>name</code> is null
     * @throws NamingException
     *             if some other type of problem has been encountered
     * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name,
     *      java.lang.String[])
     * @see RFC 1035
     */
    public Attributes getAttributes(Name name, String[] attrNames)
            throws NamingException {
        int[] types;
        int[] classes;
        DNSName nameToLookFor = null;
        DNSName altName = null;
        CompositeName remainingName = null;
        Attributes attrs = null;

        // analyze given name object
        if (name == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        } else if (name.size() == 0) {
            // attributes of the current context are requested
            nameToLookFor = (DNSName) contextName.clone();
        } else if (name instanceof CompositeName) {
            // treat the first component of the given composite name as
            // a domain name and the rest as a Next Naming System name
            String tmp = name.get(0);
            // check if it is really a domain name
            altName = (DNSName) nameParser.parse(tmp);
            // if (!altName.isAbsolute()) {
            nameToLookFor = (DNSName) composeName(altName, contextName);
            // } else {
            // nameToLookFor = (DNSName) altName.clone();
            // }
            if (name.size() > 1) {
                remainingName = (CompositeName) name.getSuffix(1);
            }
        } else if (name instanceof DNSName) {
            // if (!((DNSName) name).isAbsolute()) {
            nameToLookFor = (DNSName) composeName(name, contextName);
            // } else {
            // nameToLookFor = (DNSName) name.clone();
            // }
        } else {
            // jndi.4B=Only instances of CompositeName class or DNSName class
            // are acceptable
            throw new InvalidNameException(Messages.getString("jndi.4B")); //$NON-NLS-1$
        }

        // we should have correct nameToLookFor at this point
        if (remainingName != null) {
            CannotProceedException cpe = constructCannotProceedException(
                    altName, remainingName);
            DirContext nnsContext = DirectoryManager
                    .getContinuationDirContext(cpe);

            attrs = nnsContext.getAttributes(remainingName, attrNames);
        } else {
            // analyze given attrNames object
            if (attrNames == null) {
                // this means that all attributes should be obtained
                types = new int[1];
                classes = new int[1];
                types[0] = ProviderConstants.ANY_QTYPE;
                classes[0] = ProviderConstants.ANY_QCLASS;
            } else {
                HashSet<Integer> classesSet = new HashSet<Integer>();
                HashSet<Integer> typesSet = new HashSet<Integer>();
                Iterator<Integer> iter;
                int j;

                for (String element : attrNames) {
                    int k = element.indexOf(' ');
                    String typeStr = null;
                    int classInt;
                    int typesInt;

                    if (k > 0) {
                        String classStr = element.substring(0, k);

                        classInt = ProviderMgr.getRecordClassNumber(classStr);
                        if (classInt == -1) {
                            // jndi.4C=Unknown record class: {0}
                            throw new InvalidAttributeIdentifierException(
                                    Messages.getString("jndi.4C", classStr)); //$NON-NLS-1$
                        }
                        classesSet.add(new Integer(classInt));
                        typeStr = element.substring(k, element.length()).trim();
                    } else {
                        classesSet.add(new Integer(ProviderConstants.IN_CLASS));
                        typeStr = element.trim();
                    }
                    typesInt = ProviderMgr.getRecordTypeNumber(typeStr);
                    if (typesInt == -1) {
                        // jndi.4D=Unknown record type: {0}
                        throw new InvalidAttributeIdentifierException(Messages
                                .getString("jndi.4D", typeStr)); //$NON-NLS-1$
                    }
                    typesSet.add(new Integer(typesInt));
                }
                // filling classes array
                classes = new int[classesSet.size()];
                iter = classesSet.iterator();
                j = 0;
                while (iter.hasNext()) {
                    Integer n = iter.next();

                    classes[j++] = n.intValue();
                }
                // filling types array
                types = new int[typesSet.size()];
                iter = typesSet.iterator();
                j = 0;
                while (iter.hasNext()) {
                    Integer n = iter.next();

                    types[j++] = n.intValue();
                }
            }

            // we should have correct nameToLookFor, classes and types at this
            // point
            // let's look for attributes
            try {
                Enumeration<ResourceRecord> records = resolver.lookup(
                        nameToLookFor.toString(), types, classes);

                attrs = createAttributesFromRecords(records);
            } catch (SecurityException e) {
                NoPermissionException e2 = new NoPermissionException();

                e2.setRootCause(e);
                throw e2;
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                NamingException ne = new NamingException();

                ne.setRootCause(e);
                throw ne;
            }
        }
        return attrs;
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#createSubcontext(java.lang.String,
     *      javax.naming.directory.Attributes)
     */
    public DirContext createSubcontext(String arg0, Attributes arg1)
            throws NamingException {
        return createSubcontext(convertNameFromStringForm(arg0), arg1);
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#createSubcontext(javax.naming.Name,
     *      javax.naming.directory.Attributes)
     */
    public DirContext createSubcontext(Name arg0, Attributes arg1)
            throws NamingException {
        ContextNamePair pair;

        try {
            pair = getTargetNamespaceContextNamePair(arg0);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair.context instanceof DirContext) {
            return ((DirContext) pair.context)
                    .createSubcontext(pair.name, arg1);
        }
        // jndi.4A=found object is not a DirContext
        throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      javax.naming.directory.Attributes, java.lang.String[])
     */
    public NamingEnumeration<SearchResult> search(String arg0, Attributes arg1,
            String[] arg2) throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).search("", arg1, arg2); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      javax.naming.directory.Attributes, java.lang.String[])
     */
    public NamingEnumeration<SearchResult> search(Name arg0, Attributes arg1,
            String[] arg2) throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).search("", arg1, arg2); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      java.lang.String, javax.naming.directory.SearchControls)
     */
    public NamingEnumeration<SearchResult> search(String arg0, String arg1,
            SearchControls arg2) throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).search("", arg1, arg2); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      java.lang.String, javax.naming.directory.SearchControls)
     */
    public NamingEnumeration<SearchResult> search(Name arg0, String arg1,
            SearchControls arg2) throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).search("", arg1, arg2); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      java.lang.String, java.lang.Object[],
     *      javax.naming.directory.SearchControls)
     */
    public NamingEnumeration<SearchResult> search(String arg0, String arg1,
            Object[] arg2, SearchControls arg3) throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).search("", arg1, arg2, arg3); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      java.lang.String, java.lang.Object[],
     *      javax.naming.directory.SearchControls)
     */
    public NamingEnumeration<SearchResult> search(Name arg0, String arg1,
            Object[] arg2, SearchControls arg3) throws NamingException {
        Object obj = lookup(arg0);

        if (obj instanceof DNSContext) {
            throw new OperationNotSupportedException();
        } else if (obj instanceof DirContext) {
            return ((DirContext) obj).search("", arg1, arg2, arg3); //$NON-NLS-1$
        } else {
            // jndi.4A=found object is not a DirContext
            throw new NotContextException(Messages.getString("jndi.4A")); //$NON-NLS-1$
        }
    }

    /**
     * Frees all resources obtained by the current context.
     * 
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException {
        // do nothing right now
    }

    /**
     * @return fully qualified DNS name of the current context
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() {
        return contextName.toString();
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext(String arg0) throws NamingException {
        destroySubcontext(convertNameFromStringForm(arg0));
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind(String arg0) throws NamingException {
        unbind(convertNameFromStringForm(arg0));
    }

    /**
     * Returns a clone of the environment associated with this context.
     * 
     * @return a hash table with the context's environment
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return (Hashtable<?, ?>) environment.clone();
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext(Name arg0) throws NamingException {
        ContextNamePair pair;

        try {
            pair = getTargetNamespaceContextNamePair(arg0);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair.context instanceof Context) {
            ((Context) pair.context).destroySubcontext(pair.name);
        } else {
            // jndi.4E=found object is not a Context
            throw new NotContextException(Messages.getString("jndi.4E")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind(Name arg0) throws NamingException {
        ContextNamePair pair;

        try {
            pair = getTargetNamespaceContextNamePair(arg0);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair.context instanceof Context) {
            ((Context) pair.context).unbind(pair.name);
        } else {
            // jndi.4E=found object is not a Context
            throw new NotContextException(Messages.getString("jndi.4E")); //$NON-NLS-1$
        }
    }

    /**
     * Performs the lookup operation for given name. The method will try to
     * construct a composite name from given argument value. If it is succeed
     * and resulting composite name has the size more than one then the
     * <code>lookup(Name)</code> version of <code>lookup</code> method will
     * be called with constructed composite name as an argument. If the size of
     * constructed composite name equals to one then the value of given argument
     * will be treated as a string form of a domain name and an attempt to
     * create an instance of <code>DNSName</code> class will be made. The
     * <code>lookup(Name)</code> will be called after this.
     * 
     * @param name
     *            the name to look for
     * @return an object associated with given name
     * @throws InvalidNameException
     *             if given argument is a string representation of neither a
     *             composite name nor a domain name; or the first component of
     *             the composite name is not a domain name.
     * @throws NamingException
     *             if some other type of <code>NamingException</code> was
     *             encountered
     * @throws NullPointerException
     *             if the name is null
     * @see #lookup(Name)
     * @see javax.naming.Context#lookup(java.lang.String)
     */
    public Object lookup(String name) throws NamingException {
        return lookup(convertNameFromStringForm(name));
    }

    /**
     * @param nameStr
     *            string representation of a name
     * @return an instance of <code>CompositeName</code> or
     *         <code>DNSName</code> class
     * @throws InvalidNameException
     *             if <code>nameStr</code> is neither a string representation
     *             of <code>CompositeName</code> class nor an instance of
     *             <code>DNSName</code> class.
     */
    private Name convertNameFromStringForm(String nameStr)
            throws InvalidNameException {
        Name nameObj = null;

        if (nameStr == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        }
        nameObj = new CompositeName(nameStr);
        if (nameObj.size() == 1) {
            nameObj = nameParser.parse(nameStr);
        } else if (nameObj.size() == 0) {
            nameObj = new DNSName();
        }
        return nameObj;
    }

    /**
     * Looks for a object associated with the given name. This methods just
     * forwards the request to <code>#lookup(String)</code> method and do
     * nothing more.
     * 
     * @param name
     *            name to look for
     * @return found object
     * @throws NamingException
     *             if encountered
     * @see #lookup(String) for details
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */

    public Object lookupLink(String name) throws NamingException {
        return lookup(name);
    }

    /**
     * Removes the property with given name from the context's environment.
     * 
     * @param name
     *            the name of the property to remove
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment(String name) {
        return environment.remove(name);
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     */
    public void bind(String arg0, Object arg1) throws NamingException {
        bind(convertNameFromStringForm(arg0), arg1);
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind(String arg0, Object arg1) throws NamingException {
        rebind(convertNameFromStringForm(arg0), arg1);
    }

    /**
     * Performs the lookup operation for the given name in the current context.
     * 
     * @param name
     *            this method looks for object associated with given name
     * @return found object
     * @throws InvalidNameException
     *             if the argument is not a valid type, e.g.
     *             <code>CompositeName</code> or <code>DNSName</code>; or
     *             the first component of given composite name is not a domain
     *             name
     * @throws NameNotFoundException
     *             if authoritative server for desired zone was contacted but
     *             given name has not been found in that zone
     * @throws ServiceUnavailableException
     *             if no authoritative server for desired name was found or all
     *             servers are dead or malfunction
     * @throws NoPermissionException
     *             if no appropriate permissions on using network resources were
     *             granted
     * @throws NullPointerException
     *             if <code>name</code> is null
     * @throws NamingException
     *             if some other type of <code>NamingException</code> was
     *             encountered
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup(Name name) throws NamingException {
        int[] types = new int[1];
        int[] classes = new int[1];
        DNSName nameToLookFor = null;
        DNSName altName = null;
        CompositeName remainingName = null;
        Object result = null;

        // analyze given name object
        if (name == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        } else if (name.size() == 0) {
            // attributes of the current context are requested
            nameToLookFor = (DNSName) contextName.clone();
        } else if (name instanceof CompositeName) {
            // treat the first component of the given composite name as
            // a domain name and the rest as a Next Naming System name
            String tmp = name.get(0);
            // check if it is really a domain name
            altName = (DNSName) nameParser.parse(tmp);
            // if (!altName.isAbsolute()) {
            nameToLookFor = (DNSName) composeName(altName, contextName);
            // } else {
            // nameToLookFor = (DNSName) altName.clone();
            // }
            if (name.size() > 1) {
                remainingName = (CompositeName) name.getSuffix(1);
            }
        } else if (name instanceof DNSName) {
            // if (!((DNSName) name).isAbsolute()) {
            nameToLookFor = (DNSName) composeName(name, contextName);
            // } else {
            // nameToLookFor = (DNSName) name.clone();
            // }
        } else {
            // jndi.4B=Only instances of CompositeName class or DNSName class
            // are acceptable
            throw new InvalidNameException(Messages.getString("jndi.4B")); //$NON-NLS-1$
        }
        // we should have correct nameToLookFor at this point
        types[0] = lookupAttrType;
        classes[0] = lookupAttrClass;
        if (remainingName != null) {
            CannotProceedException cpe = constructCannotProceedException(
                    altName, remainingName);
            Context nnsContext = DirectoryManager.getContinuationContext(cpe);

            result = nnsContext.lookup(remainingName);
        } else {
            try {
                DNSContext resolvedCtx = new DNSContext(this, nameToLookFor);
                Enumeration<ResourceRecord> records = resolver.lookup(
                        nameToLookFor.toString(), types, classes);
                Attributes attrs = createAttributesFromRecords(records);

                result = DirectoryManager.getObjectInstance(resolvedCtx, name,
                        this, environment, attrs);
            } catch (SecurityException e) {
                NoPermissionException e2 = new NoPermissionException(e
                        .getMessage());

                e2.setRootCause(e);
                throw e2;
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                NamingException ne = new NamingException(e.getMessage());

                ne.setRootCause(e);
                throw ne;
            }
        }
        return result;
    }

    /**
     * Constructs <code>CannotProcessException</code> object and fills it with
     * necessary information. Values of some instance variables of the current
     * context are used.
     * 
     * @param name
     *            the portion of name that belongs to DNS namespace
     * @param remainingName
     *            the remainder of the name that belongs to NNS namespace
     * @return newly constructed exception object
     * @throws NamingException
     *             if <code>NamingException</code> was encountered somewhere
     */
    private CannotProceedException constructCannotProceedException(
            DNSName name, CompositeName remainingName) throws NamingException {
        DNSName nameToLookFor = (DNSName) composeName(name, contextName);
        final DNSContext resolvedCtx = new DNSContext(this, nameToLookFor);
        // namespace border violation, need to ask NNS
        RefAddr refAddr = new RefAddr("nns") { //$NON-NLS-1$
            private static final long serialVersionUID = 8654740210501193418L;

            DNSContext context = (DNSContext) resolvedCtx.clone();

            @Override
            public Object getContent() {
                return context;
            }
        };
        Reference ref = new Reference(this.getClass().getName(), refAddr);
        CannotProceedException cpe = null;
        CompositeName resolvedName = null;

        if (environment.containsKey(NamingManager.CPE)) {
            cpe = (CannotProceedException) environment.get(NamingManager.CPE);
            resolvedName = (CompositeName) cpe.getResolvedName();
            // remove the last component if it is ""
            // (the sign of the next naming system)
            if (resolvedName != null
                    && resolvedName.get(resolvedName.size() - 1).equals("")) //$NON-NLS-1$
            {
                resolvedName.remove(resolvedName.size() - 1);
            }
        } else {
            cpe = new CannotProceedException();
        }
        cpe.setEnvironment((Hashtable) environment.clone());
        cpe.setAltName(name);
        cpe.setAltNameCtx((DNSContext) this.clone());
        cpe.setRemainingName(remainingName);
        if (resolvedName == null) {
            resolvedName = new CompositeName();
        }
        resolvedName.add(nameToLookFor.toString());
        // the sign of the next naming system
        resolvedName.add(""); //$NON-NLS-1$
        cpe.setResolvedName(resolvedName);
        cpe.setResolvedObj(ref);
        return cpe;
    }

    /**
     * Creates an <code>Attributes</code> object from the given enumeration of
     * resource records.
     * 
     * @param recs
     *            enumeration of resource records received from the resolver
     * @return corresponding instance of <code>Attributes</code>
     */
    private static Attributes createAttributesFromRecords(
            Enumeration<ResourceRecord> recs) {
        Attributes attrs = new BasicAttributes(true);

        while (recs.hasMoreElements()) {
            ResourceRecord curRec = recs.nextElement();
            String clssTypeStr = null;
            Attribute oldAttr = null;

            if (curRec.getRRClass() == ProviderConstants.IN_CLASS) {
                clssTypeStr = ProviderConstants.rrTypeNames[curRec.getRRType()];
            } else {
                clssTypeStr = ProviderConstants.rrClassNames[curRec
                        .getRRClass()]
                        + " " + //$NON-NLS-1$
                        ProviderConstants.rrTypeNames[curRec.getRRType()];
            }
            oldAttr = attrs.get(clssTypeStr);
            if (oldAttr != null) {
                oldAttr.add(oldAttr.size(), curRec.getRData());
            } else {
                BasicAttribute attr = new BasicAttribute(clssTypeStr, curRec
                        .getRData(), false);

                attrs.put(attr);
            }
        }
        return attrs;
    }

    /**
     * Looks for a object associated with the given name. This methods just
     * forwards the request to <code>#lookup(Name)</code> method and do
     * nothing more.
     * 
     * @param name
     *            name to look for
     * @return found object
     * @throws NamingException
     *             if encountered
     * @see #lookup(Name) for details
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink(Name name) throws NamingException {
        return lookup(name);
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind(Name arg0, Object arg1) throws NamingException {
        ContextNamePair pair;

        try {
            pair = getTargetNamespaceContextNamePair(arg0);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair.context instanceof Context) {
            ((Context) pair.context).bind(pair.name, arg1);
        } else {
            // jndi.4E=found object is not a Context
            throw new NotContextException(Messages.getString("jndi.4E")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind(Name arg0, Object arg1) throws NamingException {
        ContextNamePair pair;

        try {
            pair = getTargetNamespaceContextNamePair(arg0);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair.context instanceof Context) {
            ((Context) pair.context).rebind(pair.name, arg1);
        } else {
            // jndi.4E=found object is not a Context
            throw new NotContextException(Messages.getString("jndi.4E")); //$NON-NLS-1$
        }
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename(String arg0, String arg1) throws NamingException {
        rename(convertNameFromStringForm(arg0), convertNameFromStringForm(arg1));
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext(String arg0) throws NamingException {
        return createSubcontext(convertNameFromStringForm(arg0));
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext(Name arg0) throws NamingException {
        ContextNamePair pair;

        try {
            pair = getTargetNamespaceContextNamePair(arg0);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair.context instanceof Context) {
            return ((Context) pair.context).createSubcontext(pair.name);
        }
        // jndi.4E=found object is not a Context
        throw new NotContextException(Messages.getString("jndi.4E")); //$NON-NLS-1$
    }

    /**
     * This method is not supported.
     * 
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename(Name arg0, Name arg1) throws NamingException {
        ContextNamePair pair1;
        ContextNamePair pair2;

        try {
            pair1 = getTargetNamespaceContextNamePair(arg0);
            pair2 = getTargetNamespaceContextNamePair(arg1);
        } catch (IllegalArgumentException e) {
            throw new OperationNotSupportedException();
        }
        if (pair1.context instanceof Context
                && pair1.context.getClass().getName().equals(
                        pair2.context.getClass().getName())
                && ((Context) pair1.context).getNameInNamespace().equals(
                        ((Context) pair2.context).getNameInNamespace())) {
            ((Context) pair1.context).rename(pair1.name, pair2.name);
        } else {
            // jndi.4F=found object is not a Context or target contexts are not
            // equal
            throw new NotContextException(Messages.getString("jndi.4F")); //$NON-NLS-1$
        }
    }

    /**
     * Returns the name parser for given name.
     * 
     * @param name
     *            a name in the string form to return a name parser for
     * @return the name parser found
     * @throws NotContextException
     *             if found object is not a context
     * @throws NamingException
     *             if such exception was encountered during lookup
     * @see DNSContext#getNameParser(Name) for details
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser(String name) throws NamingException {
        Object obj;

        if (name == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        }
        obj = lookup(name);
        if (obj instanceof DNSContext) {
            return nameParser;
        } else if (obj instanceof Context) {
            return ((Context) obj).getNameParser(""); //$NON-NLS-1$
        }
        // jndi.4E=found object is not a Context
        throw new NotContextException(Messages.getString("jndi.4E")); //$NON-NLS-1$
    }

    /**
     * Tries to look for the context associated with the given name and returns
     * the appropriate name parser. For <code>DNSContext</code> this method
     * will return an instance of <code>DNSNameParser</code> class.
     * 
     * @param a
     *            name to return a name parser for
     * @return a name parser for the naming system the found context is
     *         associated with
     * @throws NotContextException
     *             if found object is not a context so we cannot obtain a name
     *             parser from it
     * @throws NamingException
     *             if such exception was encountered during lookup
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser(Name name) throws NamingException {
        Object obj;

        if (name == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        }
        obj = lookup(name);
        if (obj instanceof DNSContext) {
            return nameParser;
        } else if (obj instanceof Context) {
            return ((Context) obj).getNameParser(""); //$NON-NLS-1$
        }
        // jndi.4E=found object is not a Context
        throw new NotContextException(Messages.getString("jndi.4E")); //$NON-NLS-1$
    }

    /**
     * Lists all names along with corresponding class names contained by given
     * context.
     * 
     * @param name
     *            context name to list
     * @return enumeration of <code>NameClassPair</code> objects
     * @throws NamingException
     *             if an error was encountered
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    public NamingEnumeration<NameClassPair> list(String name)
            throws NamingException {
        return list_common(convertNameFromStringForm(name), NAME_CLASS_SWT);
    }

    /**
     * Lists all names along with corresponding objects contained by given
     * context.
     * 
     * @param name
     *            context name to list
     * @return enumeration of <code>Binding</code> objects
     * @throws NamingException
     *             if an error was encountered
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    public NamingEnumeration<Binding> listBindings(String name)
            throws NamingException {
        return list_common(convertNameFromStringForm(name), BINDING_SWT);
    }

    /**
     * Lists all names along with corresponding class names contained by given
     * context.
     * 
     * @param name
     *            context name to list
     * @return enumeration of <code>NameClassPair</code> objects
     * @throws NoPermissionException
     *             if the resolver is not allowed to use a network subsystem
     * @throws NameNotFoundException
     *             if authoritative server(s) was not found
     * @throws ServiceUnavailableException
     *             if none of found servers permits zone transfers
     * @throws DomainProtocolException
     *             if some DNS specific error has occurred
     * @throws NamingException
     *             if other type of error has occurred
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    public NamingEnumeration<NameClassPair> list(Name name)
            throws NamingException {
        return list_common(name, NAME_CLASS_SWT);
    }

    /**
     * Lists all names along with corresponding class names contained by given
     * context.
     * 
     * @param name
     *            context name to list
     * @param contentSwt
     *            method will return enumeration of <code>NameClassPair</code>
     *            objects if this switch is set to
     *            <code>1<code>; enumeration of <code>Binding</code> if the switch is set
     * to <code>2</code>
     * @return enumeration of <code>NameClassPair</code> or
     *  <code>Binding</code> objects 
     * @throws NamingException
     * 
     * TODO better resolve situation then the zone just has been transferred and
     * then ANY_QTYPE request is performed; we could take the result from
     * resolver's cache since we are sure the cache is up to date and contains
     * absolutely all records from target zone
     */
    @SuppressWarnings("unchecked")
    private <T> NamingEnumeration<T> list_common(Name name, int contentSwt)
            throws NamingException {
        DNSName nameToList = null;
        DNSName altName = null;
        CompositeName remainingName = null;
        NamingEnumeration<T> result = null;

        if (contentSwt != 1 && contentSwt != 2) {
            // jndi.50=contentSwt should be equal to 1 or 2
            throw new IllegalArgumentException(Messages.getString("jndi.50")); //$NON-NLS-1$
        }
        if (name == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        }
        // analyze given name object
        else if (name.size() == 0) {
            // attributes of the current context are requested
            nameToList = (DNSName) contextName.clone();
        } else if (name instanceof CompositeName) {
            // treat the first component of the given composite name as
            // a domain name and the rest as a Next Naming System name
            String tmp = name.get(0);
            // check if it is really a domain name
            altName = (DNSName) nameParser.parse(tmp);
            // if (!altName.isAbsolute()) {
            nameToList = (DNSName) composeName(altName, contextName);
            // } else {
            // nameToList = (DNSName) altName.clone();
            // }
            if (name.size() > 1) {
                remainingName = (CompositeName) name.getSuffix(1);
            }
        } else if (name instanceof DNSName) {
            // if (!((DNSName) name).isAbsolute()) {
            nameToList = (DNSName) composeName(name, contextName);
            // } else {
            // nameToList = (DNSName) name.clone();
            // }
        } else {
            throw new InvalidNameException(Messages.getString("jndi.4B")); //$NON-NLS-1$
        }
        // we should have correct nameToLookFor at this point
        if (remainingName != null) {
            CannotProceedException cpe = constructCannotProceedException(
                    altName, remainingName);
            Context nnsContext = DirectoryManager.getContinuationContext(cpe);

            result = (NamingEnumeration<T>) nnsContext.list(remainingName);
        } else {
            // do the job
            try {
                Enumeration<ResourceRecord> resEnum = resolver.list(nameToList
                        .toString());
                Hashtable<String, T> entries = new Hashtable<String, T>();
                DNSContext targetCtx = new DNSContext(this, nameToList);

                // collecting direct children
                while (resEnum.hasMoreElements()) {
                    ResourceRecord rr = resEnum.nextElement();
                    // fullName is an full name of current record
                    Name curName = nameParser.parse(rr.getName());

                    // if contains direct child of current context
                    if (curName.startsWith(nameToList)
                            && curName.size() > nameToList.size()) {
                        // extract relative name of direct child
                        String elNameStr = curName.get(nameToList.size());

                        // if we don't have such child yet
                        if (!entries.containsKey(elNameStr)) {
                            Object elObj;
                            T objToPut = null;
                            // absolute name of direct child
                            DNSName elNameAbs = null;
                            // relative name of direct child
                            DNSName elNameRel = null;
                            // context that represents direct child
                            DNSContext elCtx;

                            elNameRel = new DNSName();
                            elNameRel.add(elNameStr);
                            elNameAbs = (DNSName) nameToList.clone();
                            elNameAbs.add(elNameStr);
                            elCtx = new DNSContext(this, elNameAbs);
                            elObj = DirectoryManager.getObjectInstance(elCtx,
                                    elNameRel, targetCtx, environment, null);
                            switch (contentSwt) {
                                case 1:
                                    // NameClassPair
                                    objToPut = (T) new NameClassPair(elNameStr,
                                            elObj.getClass().getName(), true);
                                    break;
                                case 2:
                                    // Binding
                                    objToPut = (T) new Binding(elNameStr,
                                            elObj, true);
                                    break;
                            }
                            entries.put(elNameStr, objToPut);
                        }
                    }
                }
                result = new BasicNamingEnumerator<T>(entries.elements());
            } catch (SecurityException e) {
                throw e;
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                NamingException e2 = new NamingException(e.getMessage());

                e2.setRootCause(e);
                throw e2;
            }

        }
        return result;
    }

    /**
     * Lists all names along with corresponding objects contained by given
     * context.
     * 
     * @param name
     *            context name to list
     * @return enumeration of <code>Binding</code> objects
     * @throws NamingException
     *             if an error was encountered
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration<Binding> listBindings(Name name)
            throws NamingException {
        return list_common(name, BINDING_SWT);
    }

    /**
     * Add a new property to the environment.
     * 
     * @param propName
     *            a name for the new property
     * @param propValue
     *            a value of the new property
     * @return an old value of this property; <code>null</code> if not found
     * @throws NullPointerException
     *             if <code>propName</code> or <code>propValue</code> is
     *             null.
     * @throws NamingException
     *             if such was encountered
     * @see javax.naming.Context#addToEnvironment(java.lang.String,
     *      java.lang.Object)
     */
    public Object addToEnvironment(String propName, Object propValue)
            throws NamingException {
        Object oldVal = environment.put(propName, propValue);

        if (propName.equals(Context.AUTHORITATIVE)) {
            parseBoolProp(Context.AUTHORITATIVE);
            resolver.setAuthoritativeAnswerDesired(authoritative);
        } else if (propName.equals(RECURSION)) {
            parseBoolProp(RECURSION);
            resolver.setRecursionDesired(recursion);
        } else if (propName.equals(TIMEOUT_INITIAL)) {
            parseIntProp(TIMEOUT_INITIAL);
            resolver.setInitialTimeout(timeoutInitial);
        } else if (propName.equals(TIMEOUT_RETRIES)) {
            parseIntProp(TIMEOUT_RETRIES);
            resolver.setTimeoutRetries(timeoutRetries);
        } else if (propName.equals(THREADS_MAX)) {
            parseIntProp(THREADS_MAX);
            resolver.setThreadNumberLimit(maxThreads);
        } else if (propName.equals(LOOKUP_ATTR)) {
            parseLookupProp();
        } else if (propName.equals(Context.PROVIDER_URL)) {
            parseProviderUrlProp();
        }
        return oldVal;
    }

    /**
     * Appends one name to another. The method initially tries to construct
     * composite names from given strings. If it succeeds and resulting
     * composite names have length more than 1 then the method
     * <code>composeName(Name, Name)</code> will be called with constructed
     * composite names as arguments. If one of constructed composite names (or
     * both) have the length less or equal to 1 then it will be treated as a
     * string representation of DNS name. The <code>DNSNameParser.parse</code>
     * method will be called with given string as an argument. If no exception
     * is thrown then the <code>composeName(Name, Name)</code> will be called
     * with the parsed <code>DNSName</code> as an argument.
     * 
     * @param name
     *            a name relative to the current context
     * @param prefix
     *            the name of the current context in one of its ancestors
     * @return a composition of <code>prefix</code> and <code>name</code>
     * @throws NamingException
     * @throws NullPointerException
     *             if the value of any argument is null
     * @see DNSContext#composeName(Name, Name)
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName(String name, String prefix)
            throws NamingException {
        Name name1 = null;
        Name name2 = null;

        if (name == null || prefix == null) {
            // jndi.51=Given name of prefix is null
            throw new NullPointerException(Messages.getString("jndi.51")); //$NON-NLS-1$
        }
        if (name.length() == 0) {
            return prefix;
        }
        if (prefix.length() == 0) {
            return name;
        }
        try {
            name1 = new CompositeName(name);
            name2 = new CompositeName(prefix);
        } catch (InvalidNameException e) {
        }
        if (name1 == null || name1.size() <= 1) {
            name1 = nameParser.parse(name);
        }
        if (name2 == null || name2.size() <= 1) {
            name2 = nameParser.parse(prefix);
        }
        return composeName(name1, name2).toString();
    }

    /**
     * Adds <code>name</code> to the end of <code>prefix</code>. Following
     * cases are checked:
     * <ol>
     * <li><code>suffix</code> is a composite name, <code>name</code> is
     * compound one</li>
     * <li><code>suffix</code> is a compound name, <code>name</code> is
     * composite one</li>
     * <li>Both <code>suffix</code> and <code>name</code> are compound
     * names</li>
     * <li>Both <code>suffix</code> and <code>name</code> are composite
     * names</li>
     * </ol>
     * If one of names is a composite name then the result will be also a
     * composite name. If we are composing compound name and composite name then
     * the compound name will be converted to the string form and appended as a
     * member to the resulting composite name.
     * 
     * @param name
     *            a name relative to the current context
     * @param prefix
     *            the name of the current context in one of its ancestors
     * @return a composition of <code>prefix</code> and <code>name</code>
     * @throws NamingException
     *             if the compound name is not an instance of
     *             <code>DNSName</code> class or we are trying to append an
     *             absolute DNS name to the non-root prefix.
     * @throws NullPointerException
     *             if <code>name</code> or <code>prefix</code> is null
     * @see javax.naming.Context#composeName(javax.naming.Name,
     *      javax.naming.Name)
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        Name result = null;

        if (name == null || prefix == null) {
            // jndi.51=Given name of prefix is null
            throw new NullPointerException(Messages.getString("jndi.51")); //$NON-NLS-1$
        }
        if (name.size() == 0) {
            return prefix;
        }
        if (prefix.size() == 0) {
            return name;
        }
        if (name instanceof CompositeName && prefix instanceof CompositeName) {
            // probably we need to glue together the last element of the prefix
            // and the first element of the name
            String comp1 = name.get(0);
            String comp2 = prefix.get(prefix.size() - 1);

            result = new CompositeName();
            if (prefix.size() > 1) {
                result.addAll(prefix.getPrefix(prefix.size() - 1));
            }
            try {
                result.add(concatenateDNSNames(comp1, comp2));
            } catch (InvalidNameException e) {
                // comp1 or comp2 is not a valid DNS name
                // components should be strongly separated
                result.add(comp2);
                result.add(comp1);
            }
            if (name.size() > 1) {
                result.addAll(name.getSuffix(1));
            }
        } else if (prefix instanceof CompositeName && name instanceof DNSName) {
            // probably we need to glue together the last element of the prefix
            // and the name
            String comp1 = name.toString();
            String comp2 = prefix.get(prefix.size() - 1);

            result = new CompositeName();
            if (prefix.size() > 1) {
                result.addAll(prefix.getPrefix(prefix.size() - 1));
            }
            try {
                result.add(concatenateDNSNames(comp1, comp2));
            } catch (InvalidNameException e) {
                // comp2 is not a valid DNS name
                // components should be strongly separated
                result.add(comp2);
                result.add(comp1);
            }
        } else if (prefix instanceof DNSName && name instanceof CompositeName) {
            // probably we need to glue together the prefix and
            // the first element of the name
            String comp1 = name.get(0);
            String comp2 = prefix.toString();

            result = new CompositeName();
            try {
                result.add(concatenateDNSNames(comp1, comp2));
            } catch (InvalidNameException e) {
                // comp2 is not a valid DNS name
                // components should be strongly separated
                result.add(comp2);
                result.add(comp1);
            }
            if (name.size() > 1) {
                result.addAll(name.getSuffix(1));
            }
        } else if (prefix instanceof DNSName && name instanceof DNSName) {
            DNSName rootZone = ProviderConstants.ROOT_ZONE_NAME_OBJ;
            boolean prefixIsRoot = (prefix.compareTo(rootZone) == 0);
            boolean nameIsRoot = (name.compareTo(rootZone) == 0);
            boolean nameStartsFromRoot = name.get(0).equals(""); //$NON-NLS-1$

            if (nameStartsFromRoot) {
                // jndi.52=Can't append an absolute DNS name
                throw new NamingException(Messages.getString("jndi.52")); //$NON-NLS-1$
            }
            if (prefixIsRoot && nameIsRoot) {
                result = (DNSName) rootZone.clone();
            } else if (!prefixIsRoot && nameIsRoot) {
                // jndi.53=Root domain should be the rightmost one
                throw new NamingException(Messages.getString("jndi.53")); //$NON-NLS-1$
            } else {
                result = new DNSName();
                result.addAll(prefix);
                result.addAll(name);
            }
        } else {
            // jndi.4B=Only instances of CompositeName class or DNSName class
            // are acceptable
            throw new NamingException(Messages.getString("jndi.4B")); //$NON-NLS-1$
        }
        return result;
    }

    /**
     * Concatenate two DNS name components into one DNS name
     * 
     * @param comp1
     * @param comp2
     * @return concatenation of <code>comp1</code> and <code>comp2</code>
     * @throws InvalidNameException
     *             if either <code>comp1</code> or <code>comp2</code> cannot
     *             be parsed
     * @throws NamingException
     *             if DNS name syntax is violated during composition of a new
     *             name from given components
     */
    private String concatenateDNSNames(String comp1, String comp2)
            throws NamingException {
        boolean comp1IsRoot = comp1.equals("."); //$NON-NLS-1$
        boolean comp2IsRoot = comp2.equals("."); //$NON-NLS-1$
        String composition = null;

        nameParser.parse(comp1);
        nameParser.parse(comp2);
        if (comp1.endsWith(".")) { //$NON-NLS-1$
            // jndi.52=Can't append an absolute DNS name
            throw new NamingException(Messages.getString("jndi.52")); //$NON-NLS-1$
        }
        if (comp1IsRoot && comp2IsRoot) {
            composition = "."; //$NON-NLS-1$
        } else if (!comp1IsRoot && comp2IsRoot) {
            composition = comp1 + "."; //$NON-NLS-1$
        } else if (comp1IsRoot && !comp2IsRoot) {
            // jndi.53=Root domain should be the rightmost one
            throw new NamingException(Messages.getString("jndi.53")); //$NON-NLS-1$
        } else {
            composition = comp1 + "." + comp2; //$NON-NLS-1$
        }
        return composition;
    }

    /**
     * Constructs the clone of this DNS context.
     * 
     * @see Object#clone()
     */
    @Override
    public Object clone() {
        DNSContext clone;
        try {
            clone = (DNSContext)super.clone();
        } catch (CloneNotSupportedException e) {
            // impossible
            return null;
        }
        clone.initialize(this, contextName);
        return clone;
    }

    /**
     * Checks if the given object is equal to the current context. It will
     * return <code>true</code> if and only if the specified object is an
     * instance of <code>DNSContext</code> class and has the same domain name.
     * 
     * @param obj
     *            an object to compare with
     * @return <code>true</code> if given object is equal to this one;
     *         <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof DNSContext
                && contextName.equals(((DNSContext) obj).contextName)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the hash of the context.
     * 
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return contextName.hashCode();
    }

    /**
     * @param name
     *            composite name to process
     * @return root context of the namespace to that the target object belongs
     *         and the name relative to this root context
     */
    private ContextNamePair getTargetNamespaceContextNamePair(Name cmpName)
            throws NamingException {
        CompositeName nameToLookFor;
        String remainingName;
        Object obj;

        if (cmpName == null || !(cmpName instanceof CompositeName)
                || cmpName.size() < 2) {
            throw new IllegalArgumentException();
        }
        remainingName = cmpName.get(cmpName.size() - 1);
        nameToLookFor = (CompositeName) cmpName.getPrefix(cmpName.size() - 1);
        nameToLookFor.add(""); //$NON-NLS-1$
        obj = lookup(nameToLookFor);
        return new ContextNamePair(obj, remainingName);
    }

    static class ContextNamePair {
        ContextNamePair(Object context, String name) {
            this.context = context;
            this.name = name;
        }

        Object context;

        String name;
    }

}
