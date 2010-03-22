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

package javax.naming.directory;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.NotContextException;
import javax.naming.directory.SearchResult;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This is the root context for directory service operations.
 * 
 * <p>
 * The <code>InitialDirContext</code> behavior is defined by the specification
 * for <code>javax.naming.InitialContext</code>.
 * </p>
 */
public class InitialDirContext extends InitialContext implements DirContext {

    /**
     * Constructs a new <code>InitialDirContext</code> with no environment
     * properties.
     * 
     * @throws NamingException
     *             If failed to a construct new instance.
     */
    public InitialDirContext() throws NamingException {
        super();
    }

    /**
     * Constructs a new <code>InitialDirContext</code> instance with no
     * environment properties. A mechanism for subclass constructors to
     * construct a new <code>InitialDirContext</code> instance before all
     * environment parameters are known.
     * 
     * @param flag
     *            If flag is true, the new instance is created but not
     *            initialized. In this case the subclass constructor is expected
     *            to call <code>init</code> after the environment parameters
     *            are known. If flag is false, a new instance is created and
     *            initialized with no environment parameters.
     * @throws NamingException
     *             If failed to construct new instance.
     */
    protected InitialDirContext(boolean flag) throws NamingException {
        super(flag);
    }

    /**
     * Constructs a new <code>InitialDirContext</code> instance with
     * environment properties.
     * 
     * @param hashtable
     *            Contains the environment parameters. This constructor will not
     *            change the hashtable or keep a reference to it. The hashtable
     *            parameter may be null.
     * @throws NamingException
     *             If failed to construct a new instance.
     * @see InitialContext
     */
    public InitialDirContext(Hashtable<?, ?> hashtable) throws NamingException {
        super(hashtable);
    }

    private DirContext getURLOrDefaultInitDirCtx(Name name)
            throws NamingException {
        return castToDirContext(super.getURLOrDefaultInitCtx(name));
    }

    /*
     * Try to cast the default context to DirContext.
     */
    private DirContext castToDirContext(Context ctx)
            throws NoInitialContextException, NotContextException {
        if (ctx instanceof DirContext) {
            return (DirContext) ctx;
        } else if (null == ctx) {
            // jndi.1A=Cannot create initial context.
            throw new NoInitialContextException(Messages.getString("jndi.1A")); //$NON-NLS-1$
        } else {
            // jndi.1B=DirContext object is required.
            throw new NotContextException(Messages.getString("jndi.1B")); //$NON-NLS-1$
        }
    }

    private DirContext getURLOrDefaultInitDirCtx(String name)
            throws NamingException {
        return castToDirContext(super.getURLOrDefaultInitCtx(name));
    }

    public void bind(Name name, Object obj, Attributes attributes)
            throws NamingException {
        getURLOrDefaultInitDirCtx(name).bind(name, obj, attributes);
    }

    public void bind(String s, Object obj, Attributes attributes)
            throws NamingException {
        getURLOrDefaultInitDirCtx(s).bind(s, obj, attributes);
    }

    public DirContext createSubcontext(Name name, Attributes attributes)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).createSubcontext(name,
                attributes);
    }

    public DirContext createSubcontext(String s, Attributes attributes)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(s).createSubcontext(s, attributes);
    }

    public Attributes getAttributes(Name name) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getAttributes(name);
    }

    public Attributes getAttributes(Name name, String[] as)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getAttributes(name, as);
    }

    public Attributes getAttributes(String s) throws NamingException {
        return getURLOrDefaultInitDirCtx(s).getAttributes(s);
    }

    public Attributes getAttributes(String s, String[] as)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(s).getAttributes(s, as);
    }

    public DirContext getSchema(Name name) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getSchema(name);
    }

    public DirContext getSchema(String s) throws NamingException {
        return getURLOrDefaultInitDirCtx(s).getSchema(s);
    }

    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getSchemaClassDefinition(name);
    }

    public DirContext getSchemaClassDefinition(String s) throws NamingException {
        return getURLOrDefaultInitDirCtx(s).getSchemaClassDefinition(s);
    }

    public void modifyAttributes(Name name, int i, Attributes attributes)
            throws NamingException {
        getURLOrDefaultInitDirCtx(name).modifyAttributes(name, i, attributes);
    }

    public void modifyAttributes(Name name, ModificationItem[] modificationItems)
            throws NamingException {
        getURLOrDefaultInitDirCtx(name).modifyAttributes(name,
                modificationItems);
    }

    public void modifyAttributes(String s, int i, Attributes attributes)
            throws NamingException {
        getURLOrDefaultInitDirCtx(s).modifyAttributes(s, i, attributes);
    }

    public void modifyAttributes(String s, ModificationItem[] modificationItems)
            throws NamingException {
        getURLOrDefaultInitDirCtx(s).modifyAttributes(s, modificationItems);

    }

    public void rebind(Name name, Object obj, Attributes attributes)
            throws NamingException {
        getURLOrDefaultInitDirCtx(name).rebind(name, obj, attributes);
    }

    public void rebind(String s, Object obj, Attributes attributes)
            throws NamingException {
        getURLOrDefaultInitDirCtx(s).rebind(s, obj, attributes);
    }

    public NamingEnumeration<SearchResult> search(Name name,
            Attributes attributes) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).search(name, attributes);
    }

    public NamingEnumeration<SearchResult> search(Name name,
            Attributes attributes, String[] as) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).search(name, attributes, as);
    }

    public NamingEnumeration<SearchResult> search(Name name, String filter,
            Object[] objs, SearchControls searchControls)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).search(name, filter, objs,
                searchControls);
    }

    public NamingEnumeration<SearchResult> search(Name name, String filter,
            SearchControls searchcontrols) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).search(name, filter,
                searchcontrols);
    }

    public NamingEnumeration<SearchResult> search(String name,
            Attributes attributes) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).search(name, attributes);
    }

    public NamingEnumeration<SearchResult> search(String name,
            Attributes attributes, String[] as) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).search(name, attributes, as);
    }

    public NamingEnumeration<SearchResult> search(String name, String filter,
            Object[] objs, SearchControls searchControls)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).search(name, filter, objs,
                searchControls);
    }

    public NamingEnumeration<SearchResult> search(String name, String filter,
            SearchControls searchControls) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).search(name, filter,
                searchControls);
    }

}
