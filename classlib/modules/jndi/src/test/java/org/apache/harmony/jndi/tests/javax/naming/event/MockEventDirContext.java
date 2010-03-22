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

package org.apache.harmony.jndi.tests.javax.naming.event;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamingListener;

public class MockEventDirContext implements EventDirContext {

    public void addNamingListener(Name name, String s, Object[] aobj,
            SearchControls searchcontrols, NamingListener naminglistener)
            throws NamingException {

    }

    public void addNamingListener(Name name, String s, SearchControls searchcontrols,
            NamingListener naminglistener) throws NamingException {

    }

    public void addNamingListener(String s, String s1, Object[] aobj,
            SearchControls searchcontrols, NamingListener naminglistener)
            throws NamingException {

    }

    public void addNamingListener(String s, String s1, SearchControls searchcontrols,
            NamingListener naminglistener) throws NamingException {

    }

    public void addNamingListener(Name name, int i, NamingListener naminglistener)
            throws NamingException {

    }

    public void addNamingListener(String s, int i, NamingListener naminglistener)
            throws NamingException {

    }

    public void removeNamingListener(NamingListener naminglistener) throws NamingException {

    }

    public boolean targetMustExist() throws NamingException {

        return false;
    }

    public Object addToEnvironment(String s, Object o) throws NamingException {

        return null;
    }

    public void bind(Name n, Object o) throws NamingException {

    }

    public void bind(String s, Object o) throws NamingException {

    }

    public void close() throws NamingException {

    }

    public Name composeName(Name n, Name pfx) throws NamingException {

        return null;
    }

    public String composeName(String s, String pfx) throws NamingException {

        return null;
    }

    public Context createSubcontext(Name n) throws NamingException {

        return null;
    }

    public Context createSubcontext(String s) throws NamingException {

        return null;
    }

    public void destroySubcontext(Name n) throws NamingException {

    }

    public void destroySubcontext(String s) throws NamingException {

    }

    public Hashtable<?, ?> getEnvironment() throws NamingException {

        return null;
    }

    public String getNameInNamespace() throws NamingException {

        return null;
    }

    public NameParser getNameParser(Name n) throws NamingException {

        return null;
    }

    public NameParser getNameParser(String s) throws NamingException {

        return null;
    }

    public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {

        return null;
    }

    public NamingEnumeration<NameClassPair> list(String s) throws NamingException {

        return null;
    }

    public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {

        return null;
    }

    public NamingEnumeration<Binding> listBindings(String s) throws NamingException {

        return null;
    }

    public Object lookup(Name n) throws NamingException {

        return null;
    }

    public Object lookup(String s) throws NamingException {

        return null;
    }

    public Object lookupLink(Name n) throws NamingException {

        return null;
    }

    public Object lookupLink(String s) throws NamingException {

        return null;
    }

    public void rebind(Name n, Object o) throws NamingException {

    }

    public void rebind(String s, Object o) throws NamingException {

    }

    public Object removeFromEnvironment(String s) throws NamingException {

        return null;
    }

    public void rename(Name nOld, Name nNew) throws NamingException {

    }

    public void rename(String sOld, String sNew) throws NamingException {

    }

    public void unbind(Name n) throws NamingException {

    }

    public void unbind(String s) throws NamingException {

    }

    public void bind(Name name, Object obj, Attributes attributes) throws NamingException {

    }

    public void bind(String s, Object obj, Attributes attributes) throws NamingException {

    }

    public DirContext createSubcontext(Name name, Attributes attributes) throws NamingException {

        return null;
    }

    public DirContext createSubcontext(String s, Attributes attributes) throws NamingException {

        return null;
    }

    public Attributes getAttributes(Name name) throws NamingException {

        return null;
    }

    public Attributes getAttributes(Name name, String[] as) throws NamingException {

        return null;
    }

    public Attributes getAttributes(String s) throws NamingException {

        return null;
    }

    public Attributes getAttributes(String s, String[] as) throws NamingException {

        return null;
    }

    public DirContext getSchema(Name name) throws NamingException {

        return null;
    }

    public DirContext getSchema(String s) throws NamingException {

        return null;
    }

    public DirContext getSchemaClassDefinition(Name name) throws NamingException {

        return null;
    }

    public DirContext getSchemaClassDefinition(String s) throws NamingException {

        return null;
    }

    public void modifyAttributes(Name name, int i, Attributes attributes)
            throws NamingException {

    }

    public void modifyAttributes(Name name, ModificationItem[] amodificationitem)
            throws NamingException {

    }

    public void modifyAttributes(String s, int i, Attributes attributes) throws NamingException {

    }

    public void modifyAttributes(String s, ModificationItem[] amodificationitem)
            throws NamingException {

    }

    public void rebind(Name name, Object obj, Attributes attributes) throws NamingException {

    }

    public void rebind(String s, Object obj, Attributes attributes) throws NamingException {

    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes attributes)
            throws NamingException {

        return null;
    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes attributes, String[] as)
            throws NamingException {

        return null;
    }

    public NamingEnumeration<SearchResult> search(Name name, String s, Object[] aobj,
            SearchControls searchcontrols) throws NamingException {

        return null;
    }

    public NamingEnumeration<SearchResult> search(Name name, String s,
            SearchControls searchcontrols) throws NamingException {

        return null;
    }

    public NamingEnumeration<SearchResult> search(String s, Attributes attributes)
            throws NamingException {

        return null;
    }

    public NamingEnumeration<SearchResult> search(String s, Attributes attributes, String[] as)
            throws NamingException {

        return null;
    }

    public NamingEnumeration<SearchResult> search(String s, String s1, Object[] aobj,
            SearchControls searchcontrols) throws NamingException {

        return null;
    }

    public NamingEnumeration<SearchResult> search(String s, String s1,
            SearchControls searchcontrols) throws NamingException {

        return null;
    }

}
