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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.spi.DirectoryManager;
import javax.naming.spi.ResolveResult;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.internal.parser.AttributeTypeAndValuePair;
import org.apache.harmony.jndi.provider.GenericURLContext;
import org.apache.harmony.jndi.provider.ldap.parser.LdapUrlParser;

public class ldapURLContext extends GenericURLContext implements DirContext {

    /**
     * Creates instance of this context with empty environment.
     */
    public ldapURLContext() {
        super(null);
    }

    /**
     * Creates instance of this context with specified environment.
     * 
     * @param environment
     *            Environment to copy.
     */
    public ldapURLContext(Hashtable<?, ?> environment) {
        super(environment);
    }

    @Override
    protected ResolveResult getRootURLContext(String url, Hashtable<?, ?> env)
            throws NamingException {
    	Hashtable<?, ?> myEnv = null;
    	if (env == null) {    		
            myEnv = environment;
    	} else {
    		myEnv = (Hashtable<?, ?>) env.clone();
        }
        
        LdapUrlParser parser = LdapUtils.parserURL(url, false);

        String host = parser.getHost();
        int port = parser.getPort();
        String dn = parser.getBaseObject();

        LdapClient client = LdapClient.newInstance(host, port, myEnv, LdapUtils
                .isLdapsURL(url));

        LdapContextImpl context = new LdapContextImpl(client,
                (Hashtable<Object, Object>) myEnv, dn);

        // not support ldap url + other namespace name
        return new ResolveResult(context, "");
    }

    @Override
    protected DirContext getContinuationContext(Name name)
            throws NamingException {
        return DirectoryManager
                .getContinuationDirContext(createCannotProceedException(name));
    }

    public void bind(Name name, Object obj, Attributes attributes)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            bind(name.get(0), obj, attributes);
            return;
        }

        DirContext context = getContinuationContext(name);

        try {
            context.bind(name.getSuffix(1), obj, attributes);

        } finally {
            context.close();
        }

    }

    public void bind(String url, Object obj, Attributes attributes)
            throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            context.bind(result.getRemainingName(), obj, attributes);
            return;
        } finally {
            context.close();
        }
    }

    public DirContext createSubcontext(Name name, Attributes attributes)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return createSubcontext(name.get(0), attributes);
        }
        DirContext context = getContinuationContext(name);

        try {
            return context.createSubcontext(name.getSuffix(1), attributes);
        } finally {
            context.close();
        }
    }

    public DirContext createSubcontext(String url, Attributes attributes)
            throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            return context.createSubcontext(result.getRemainingName(),
                    attributes);
        } finally {
            context.close();
        }
    }

    public Attributes getAttributes(Name name) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return getAttributes(name.get(0));
        }
        DirContext context = getContinuationContext(name);

        try {
            return context.getAttributes(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    public Attributes getAttributes(Name name, String[] as)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return getAttributes(name.get(0), as);
        }
        DirContext context = getContinuationContext(name);

        try {
            return context.getAttributes(name.getSuffix(1), as);
        } finally {
            context.close();
        }
    }

    public Attributes getAttributes(String url) throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            return context.getAttributes(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    public Attributes getAttributes(String url, String[] as)
            throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            return context.getAttributes(result.getRemainingName(), as);
        } finally {
            context.close();
        }
    }

    public DirContext getSchema(Name name) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return getSchema(name.get(0));
        }
        DirContext context = getContinuationContext(name);

        try {
            return context.getSchema(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    public DirContext getSchema(String url) throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            return context.getSchema(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return getSchemaClassDefinition(name.get(0));
        }
        DirContext context = getContinuationContext(name);

        try {
            return context.getSchemaClassDefinition(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    public DirContext getSchemaClassDefinition(String url)
            throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            return context.getSchema(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    public void modifyAttributes(Name name, int i, Attributes attributes)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            modifyAttributes(name.get(0), i, attributes);
            return;
        }
        DirContext context = getContinuationContext(name);

        try {
            context.modifyAttributes(name.getSuffix(1), i, attributes);
            return;
        } finally {
            context.close();
        }

    }

    public void modifyAttributes(Name name, ModificationItem[] modificationItems)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            modifyAttributes(name.get(0), modificationItems);
            return;
        }
        DirContext context = getContinuationContext(name);

        try {
            context.modifyAttributes(name.getSuffix(1), modificationItems);
            return;
        } finally {
            context.close();
        }

    }

    public void modifyAttributes(String url, int i, Attributes attributes)
            throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            context.modifyAttributes(result.getRemainingName(), i, attributes);
        } finally {
            context.close();
        }

    }

    public void modifyAttributes(String url,
            ModificationItem[] modificationItems) throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            context.modifyAttributes(result.getRemainingName(),
                    modificationItems);
        } finally {
            context.close();
        }
    }

    public void rebind(Name name, Object obj, Attributes attributes)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            rebind(name.get(0), obj, attributes);
            return;
        }

        DirContext context = getContinuationContext(name);

        try {
            context.rebind(name.getSuffix(1), obj, attributes);

        } finally {
            context.close();
        }

    }

    public void rebind(String url, Object obj, Attributes attributes)
            throws NamingException {
        ResolveResult result = getRootURLContext(url, environment);
        DirContext context = (DirContext) result.getResolvedObj();

        try {
            context.rebind(result.getRemainingName(), obj, attributes);
        } finally {
            context.close();
        }

    }

    public NamingEnumeration<SearchResult> search(Name name,
            Attributes attributes) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return search(name.get(0), attributes);
        }

        DirContext context = getContinuationContext(name);

        try {
            return context.search(name.getSuffix(1), attributes);

        } finally {
            context.close();
        }
    }

    public NamingEnumeration<SearchResult> search(Name name,
            Attributes attributes, String[] as) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return search(name.get(0), attributes, as);
        }

        DirContext context = getContinuationContext(name);

        try {
            return context.search(name.getSuffix(1), attributes, as);

        } finally {
            context.close();
        }
    }

    public NamingEnumeration<SearchResult> search(Name name, String filter,
            Object[] objs, SearchControls searchControls)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return search(name.get(0), filter, objs, searchControls);
        }

        DirContext context = getContinuationContext(name);

        try {
            return context.search(name.getSuffix(1), filter, objs,
                    searchControls);

        } finally {
            context.close();
        }
    }

    public NamingEnumeration<SearchResult> search(Name name, String filter,
            SearchControls searchControls) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return search(name.get(0), filter, searchControls);
        }

        DirContext context = getContinuationContext(name);

        try {
            return context.search(name.getSuffix(1), filter, searchControls);

        } finally {
            context.close();
        }
    }

    public NamingEnumeration<SearchResult> search(String url,
            Attributes attributes) throws NamingException {
        return search(url, attributes, null);
    }

    /**
     * <code>url</code> is an LDAP URL, which may contains query ('?') parts,
     * and any parts in URL should override corresponding arguments. For
     * example, if <code>url</code> contains filter parts, the
     * <code>attributes</code> arguments would be useless, LDAP search will
     * use filter from url.
     * 
     */
    public NamingEnumeration<SearchResult> search(String url,
            Attributes attributes, String[] as) throws NamingException {
        LdapUrlParser parser = LdapUtils.parserURL(url, true);
        String dn = parser.getBaseObject();
        String host = parser.getHost();
        int port = parser.getPort();

        LdapClient client = LdapClient.newInstance(host, port, environment,
                LdapUtils.isLdapsURL(url));
        LdapContextImpl context = null;
        try {
            context = new LdapContextImpl(client,
                    (Hashtable<Object, Object>) environment, dn);

            SearchControls controls = parser.getControls();
            if (controls == null) {
                controls = new SearchControls();
                controls.setReturningAttributes(as);
            } else if (!parser.hasAttributes()) {
                controls.setReturningAttributes(as);
            }

            // construct filter
            Filter filter = null;
            if (parser.hasFilter()) {
                // use filter in url
                filter = parser.getFilter();
            } else {
                // construct filter from attributes
                if (attributes == null || attributes.size() == 0) {
                    // no attributes, use default filter "(objectClass=*)"
                    filter = new Filter(Filter.PRESENT_FILTER);
                    filter.setValue("objectClass");
                } else {
                    if (attributes.size() == 1) {
                        filter = new Filter(Filter.EQUALITY_MATCH_FILTER);
                        Attribute att = attributes.getAll().next();
                        filter.setValue(new AttributeTypeAndValuePair(att
                                .getID(), att.get()));
                    } else {
                        NamingEnumeration<? extends Attribute> attrs = attributes
                                .getAll();
                        filter = new Filter(Filter.AND_FILTER);
                        while (attrs.hasMore()) {
                            Attribute attr = attrs.next();
                            String type = attr.getID();
                            NamingEnumeration<?> enuValues = attr.getAll();
                            while (enuValues.hasMore()) {
                                Object value = enuValues.next();
                                Filter child = new Filter(
                                        Filter.EQUALITY_MATCH_FILTER);
                                child.setValue(new AttributeTypeAndValuePair(
                                        type, value));
                                filter.addChild(child);
                            }
                        }
                    }
                }
            }

            LdapSearchResult result = context.doSearch(dn, filter, controls);
            
            if (result.isEmpty() && result.getException() != null) {
                throw result.getException();
            }
            
            return result.toSearchResultEnumeration(dn);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    public NamingEnumeration<SearchResult> search(String url, String filter,
            Object[] objs, SearchControls searchControls)
            throws NamingException {
        LdapUrlParser parser = LdapUtils.parserURL(url, true);
        String dn = parser.getBaseObject();
        String host = parser.getHost();
        int port = parser.getPort();

        LdapClient client = LdapClient.newInstance(host, port, environment,
                LdapUtils.isLdapsURL(url));
        LdapContextImpl context = null;

        try {
            context = new LdapContextImpl(client,
                    (Hashtable<Object, Object>) environment, dn);

            Filter f = parser.getFilter();
            if (f == null) {
                f = LdapUtils.parseFilter(filter, objs);
            }

            if (searchControls == null) {
                searchControls = new SearchControls();
            }
            if (parser.getControls() != null) {
                SearchControls controls = parser.getControls();
                if (parser.hasAttributes()) {
                    searchControls.setReturningAttributes(controls
                            .getReturningAttributes());
                }
                if (parser.hasScope()) {
                    searchControls.setSearchScope(controls.getSearchScope());
                }
            }

            LdapSearchResult result = context.doSearch(dn, f,
                    searchControls);
            
            if (result.isEmpty() && result.getException() != null) {
                throw result.getException();
            }
            
            return result.toSearchResultEnumeration(dn);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }
    
    private String convertToRelativeName(String targetContextDN, String dn) {
        if (targetContextDN.equals("")) {
            return dn;
        }

        int index = dn.lastIndexOf(targetContextDN);

        if (index == 0) {
            return "";
        }
        
        return dn.substring(0, index - 1);
    }
    
    public NamingEnumeration<SearchResult> search(String url, String filter,
            SearchControls searchControls) throws NamingException {
        return search(url, filter, new Object[0], searchControls);
    }

}
