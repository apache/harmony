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
 * @author  Vasily Zakharov
 */

package org.apache.harmony.jndi.provider;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ResolveResult;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * Base class for URL naming context implementations.
 * 
 * In many cases, subclasses should only override
 * {@link #getRootURLContext(String, Hashtable)} method and provide a public
 * constructor calling
 * {@link GenericURLContext#GenericURLContext(Hashtable) super(environment)}.
 */
public abstract class GenericURLContext implements Context {

    /**
     * Local environment.
     */
    protected Hashtable<Object, Object> environment;

    /**
     * Creates instance of this context with empty environment.
     */
    protected GenericURLContext() {
        this(null);
    }

    /**
     * Creates instance of this context with specified environment.
     * 
     * @param environment
     *            Environment to copy.
     */
    @SuppressWarnings("unchecked")
    protected GenericURLContext(Hashtable<?, ?> environment) {
        super();
        if (environment == null) {
            this.environment = new Hashtable<Object, Object>();
        } else {
            this.environment = (Hashtable<Object, Object>) environment.clone();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object lookup(Name name) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return lookup(name.get(0));
        }
        Context context = getContinuationContext(name);
        try {
            return context.lookup(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object lookup(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            return context.lookup(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object lookupLink(Name name) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return lookupLink(name.get(0));
        }
        Context context = getContinuationContext(name);
        try {
            return context.lookupLink(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object lookupLink(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            return context.lookupLink(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void bind(Name name, Object obj) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            bind(name.get(0), obj);
        } else {
            Context context = getContinuationContext(name);

            try {
                context.bind(name.getSuffix(1), obj);
            } finally {
                context.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void bind(String name, Object obj) throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            context.bind(result.getRemainingName(), obj);
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rebind(Name name, Object obj) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            rebind(name.get(0), obj);
        } else {
            Context context = getContinuationContext(name);

            try {
                context.rebind(name.getSuffix(1), obj);
            } finally {
                context.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rebind(String name, Object obj) throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            context.rebind(result.getRemainingName(), obj);
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unbind(Name name) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            unbind(name.get(0));
        } else {
            Context context = getContinuationContext(name);

            try {
                context.unbind(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unbind(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            context.unbind(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Context createSubcontext(Name name) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return createSubcontext(name.get(0));
        }
        Context context = getContinuationContext(name);
        try {
            return context.createSubcontext(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Context createSubcontext(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            return context.createSubcontext(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroySubcontext(Name name) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            destroySubcontext(name.get(0));
        } else {
            Context context = getContinuationContext(name);

            try {
                context.destroySubcontext(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroySubcontext(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            context.destroySubcontext(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This method uses {@link #urlEquals(String, String)} to compare URL
     * prefixes of the names.
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        if (!(oldName instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString(
                    "jndi.26", oldName)); //$NON-NLS-1$
        }

        if (!(newName instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString(
                    "jndi.26", newName)); //$NON-NLS-1$
        }

        if ((oldName.size() == 1) ^ (newName.size() != 1)) {
            // jndi.27=Renaming of names of which one has only one component, +
            // and another has more than one component is not supported: {0} ->
            // {1}
            throw new OperationNotSupportedException(Messages.getString(
                    "jndi.27", oldName, newName)); //$NON-NLS-1$
        }

        if (oldName.size() == 1) {
            rename(oldName.get(0), newName.get(0));
        } else {
            if (!urlEquals(oldName.get(0), oldName.get(0))) {
                // jndi.28=Renaming of names using different URLs as their first
                // components is not supported: {0} -> {1}
                throw new OperationNotSupportedException(Messages.getString(
                        "jndi.28", oldName, newName)); //$NON-NLS-1$
            }
            Context context = getContinuationContext(oldName);

            try {
                context.rename(oldName.getSuffix(1), newName.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This method uses {@link #getURLPrefix(String)} and
     * {@link #getURLSuffix(String, String)} methods to parse string names, and
     * also uses {@link #urlEquals(String, String)} to compare URL prefixes of
     * the names.
     */
    public void rename(String oldName, String newName) throws NamingException {
        String oldPrefix = getURLPrefix(oldName);
        String newPrefix = getURLPrefix(newName);

        if (!urlEquals(oldPrefix, newPrefix)) {
            // jndi.29=Renaming of names using different URL prefixes is not
            // supported: {0} -> {1}
            throw new OperationNotSupportedException(Messages.getString(
                    "jndi.29", oldName, newName)); //$NON-NLS-1$
        }
        ResolveResult result = getRootURLContext(oldName, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            context.rename(result.getRemainingName(), getURLSuffix(newPrefix,
                    newName));
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<NameClassPair> list(Name name)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return list(name.get(0));
        }
        Context context = getContinuationContext(name);

        try {
            return context.list(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<NameClassPair> list(String name)
            throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            return context.list(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<Binding> listBindings(Name name)
            throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return listBindings(name.get(0));
        }
        Context context = getContinuationContext(name);

        try {
            return context.listBindings(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<Binding> listBindings(String name)
            throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            return context.listBindings(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NameParser getNameParser(Name name) throws NamingException {
        if (!(name instanceof CompositeName)) {
            // jndi.26=URL context can't accept non-composite name: {0}
            throw new InvalidNameException(Messages.getString("jndi.26", name)); //$NON-NLS-1$
        }

        if (name.size() == 1) {
            return getNameParser(name.get(0));
        }
        Context context = getContinuationContext(name);
        try {
            return context.getNameParser(name.getSuffix(1));
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NameParser getNameParser(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name, environment);
        Context context = (Context) result.getResolvedObj();

        try {
            return context.getNameParser(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        return ((Name) prefix.clone()).addAll(name);
    }

    /**
     * {@inheritDoc}
     */
    public String composeName(String name, String prefix) {
        return ((prefix.length() < 1) ? name : (name.length() < 1) ? prefix
                : (prefix + '/' + name));
    }

    /**
     * {@inheritDoc}
     */
    public String getNameInNamespace() {
        return ""; //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public Hashtable<?, ?> getEnvironment() {
        return (Hashtable<?, ?>) environment.clone();
    }

    /**
     * {@inheritDoc}
     */
    public Object addToEnvironment(String propName, Object propVal) {
        return environment.put(propName, propVal);
    }

    /**
     * {@inheritDoc}
     */
    public Object removeFromEnvironment(String propName) {
        return environment.remove(propName);
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        environment = null;
    }

    /**
     * Lookups the first component (considered a URL) of the specified name
     * using {@link #lookup(String)}, wraps it into
     * {@link CannotProceedException}, passes it to
     * {@link NamingManager#getContinuationContext(CannotProceedException)}
     * method and returns the result.
     * 
     * This method is used by {@link #lookup(Name)} and other public methods
     * taking {@link Name} as a parameter.
     * 
     * This method uses {@link #createCannotProceedException(Name)} method.
     * 
     * @param name
     *            Name to parse.
     * 
     * @return Continuation context.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     */
    protected Context getContinuationContext(Name name) throws NamingException {
        return NamingManager
                .getContinuationContext(createCannotProceedException(name));
    }

    /**
     * Lookups the first component (considered a URL) of the specified name
     * using {@link #lookup(String)} and wraps it into
     * {@link CannotProceedException}.
     * 
     * @param name
     *            Name to parse.
     * 
     * @return Created {@link CannotProceedException}.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     */
    protected final CannotProceedException createCannotProceedException(
            Name name) throws NamingException {
        CannotProceedException cpe = new CannotProceedException();
        cpe.setResolvedObj(lookup(name.get(0)));
        cpe.setEnvironment(environment);
        return cpe;
    }

    /**
     * Determines the proper context from the specified URL and returns the
     * {@link ResolveResult} object with that context as resolved object and the
     * rest of the URL as remaining name.
     * 
     * This method is used by {@link #lookup(String)} and other public methods
     * taking {@link String} name as a parameter.
     * 
     * This method must be overridden by particular URL context implementations.
     * 
     * When overriding make sure that {@link #getURLPrefix(String)},
     * {@link #getURLSuffix(String, String)} and
     * {@link #getRootURLContext(String, Hashtable)} methods are in sync in how
     * they parse URLs.
     * 
     * @param url
     *            URL.
     * 
     * @param environment
     *            Environment.
     * 
     * @return {@link ResolveResult} object with resolved context as resolved
     *         object the rest of the URL as remaining name.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     */
    protected abstract ResolveResult getRootURLContext(String url,
            Hashtable<?, ?> environment) throws NamingException;

    /**
     * Compares two URLs for equality.
     * 
     * Implemented here as <code>url1.equals(url2)</code>. Subclasses may
     * provide different implementation.
     * 
     * This method is only used by {@link #rename(Name, Name)} and
     * {@link #rename(String, String)} methods.
     * 
     * @param url1
     *            First URL to compare.
     * 
     * @param url2
     *            Second URL to compare.
     * 
     * @return <code>true</code> if specified URLs are equal,
     *         <code>false</code> otherwise.
     */
    protected boolean urlEquals(String url1, String url2) {
        return url1.equals(url2);
    }

    /**
     * Returns URL prefix, containing scheme name, hostname and port.
     * 
     * This method is only used by {@link #rename(String, String)} method and
     * may be overridden by subclasses.
     * 
     * When overriding make sure that {@link #getURLPrefix(String)},
     * {@link #getURLSuffix(String, String)} and
     * {@link #getRootURLContext(String, Hashtable)} methods are in sync in how
     * they parse URLs.
     * 
     * @param url
     *            URL to parse.
     * 
     * @return URL prefix.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     */
    protected String getURLPrefix(String url) throws NamingException {
        int index = url.indexOf(':');
        if (index < 0) {
            // jndi.2A=Invalid URL: {0}
            throw new OperationNotSupportedException(Messages.getString(
                    "jndi.2A", url)); //$NON-NLS-1$
        }
        index++;

        if (url.startsWith("//", index)) { //$NON-NLS-1$
            int slashPos = url.indexOf('/', index + 2);
            index = ((slashPos >= 0) ? slashPos : url.length());
        }
        return url.substring(0, index);
    }

    /**
     * Returns URL suffix, containing everything but the
     * {@linkplain #getURLPrefix(String) prefix} and separating slash, as a
     * single-component {@link CompositeName}.
     * 
     * This method is only used by {@link #rename(String, String)} method and
     * may be overridden by subclasses.
     * 
     * This method uses {@link #decodeURLString(String)} to decode the suffix
     * string.
     * 
     * When overriding make sure that {@link #getURLPrefix(String)},
     * {@link #getURLSuffix(String, String)} and
     * {@link #getRootURLContext(String, Hashtable)} methods are in sync in how
     * they parse URLs.
     * 
     * @param prefix
     *            URL prefix, returned by {@link #getURLPrefix(String)}
     *            previously called on the same URL.
     * 
     * @param url
     *            URL to parse.
     * 
     * @return URL suffix as a single-component {@link CompositeName}.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     */
    protected Name getURLSuffix(String prefix, String url)
            throws NamingException {
        int length = prefix.length();

        if (length >= (url.length() - 1)) {
            // If prefix is only 1 character shorter than URL,
            // that character is slash and can be ignored.
            return new CompositeName();
        }

        String suffix = url
                .substring((url.charAt(length) == '/') ? (length + 1) : length);

        try {
            return new CompositeName().add(decodeURLString(suffix));
        } catch (IllegalArgumentException e) {
            throw (InvalidNameException) new InvalidNameException()
                    .initCause(e);
        }
    }

    /**
     * Decodes URL string by transforming URL-encoded characters into their
     * Unicode character representations.
     * 
     * This method is used by {@link #getURLSuffix(String, String)}.
     * 
     * @param str
     *            URL or part of URL string.
     * 
     * @return Decoded string.
     * 
     * @throws IllegalArgumentException
     *             If URL format is incorrect.
     */
    protected static final String decodeURLString(String str)
            throws IllegalArgumentException {
        int length = str.length();
        byte bytes[] = new byte[length];
        int index = 0;

        for (int i = 0; i < length;) {
            char c = str.charAt(i++);

            if (c == '%') {
                int next = i + 2;

                if (next > length) {
                    // jndi.2B=Invalid URL format: {0}
                    throw new IllegalArgumentException(Messages.getString(
                            "jndi.2B", str)); //$NON-NLS-1$
               }

                try {
                    bytes[index++] = (byte) Integer.parseInt(str.substring(i,
                            next), 16);
                } catch (NumberFormatException e) {
                    // jndi.2B=Invalid URL format: {0}
                    throw (IllegalArgumentException) new IllegalArgumentException(
                            Messages.getString("jndi.2B", str)).initCause(e); //$NON-NLS-1$
                }

                i = next;
            } else {
                bytes[index++] = (byte) c;
            }
        }
        return new String(bytes, 0, index);
    }

}
