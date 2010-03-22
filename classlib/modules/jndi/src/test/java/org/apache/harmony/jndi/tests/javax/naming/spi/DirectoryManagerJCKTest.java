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

package org.apache.harmony.jndi.tests.javax.naming.spi;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
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
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.DirectoryManager;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import junit.framework.TestCase;

public class DirectoryManagerJCKTest extends TestCase {

	public void testGetContinuationDirContext() throws NamingException {
		// Step 1: Write a simple class which implements
		// LdapContext and InitialContextFactory.
		// Please refer to the following class MyLdapContext

		// Step 2: Create an instance of MyLdapContext
		DirContext context = new MyLdapContext();

		// Step 3: Create an instance of CannotProceedException,
		// and set the resolve object as "context"
		CannotProceedException exception = new CannotProceedException(
				"TestGetContinuationDirContext");
		exception.setResolvedObj(context);
		// Step 4: Call DirectoryManager.getContinuationDirContext and pass
		// the "exception";

		DirContext newContext = DirectoryManager
				.getContinuationDirContext(exception);
		// Step 5: check result
		assertNotNull(newContext);
		// System.out.println(context);
		// System.out.println(newContext);
	}

	public void testGetContinuationDirContext2() throws NamingException {
		// 1.Write a simple class which implements Context, NameParser,
		// InitialContextFactory,ObjectFactory

		// 2.Create an instance of the class defined in step 1 as a Context.
		Context context = new MyContext161();

		// 3.Set the Resolved Object of the CannotProceedException to be the
		// Context
		// created in step 2 using the setResolvedObj method.
		CannotProceedException ex = new CannotProceedException(
				"TestGetContinuationDirContext2");
		ex.setResolvedObj(context);

		// 4. Call the getcontinuationDirContext method of the DirectoryManager
		// class
		// passing the CannotProceedException object and modified at step 3.
		DirContext newContext = null;
		try {
			newContext = DirectoryManager.getContinuationDirContext(ex);
			// fail("Should throw CannotProceedException.");
		} catch (CannotProceedException cpe) {
			// System.out.println(cpe);
		}

		try {
			newContext.bind("bindName", "Object to be binded");
		} catch (Exception e) {
			// System.out.println(e);
		}

		try {
			newContext.getAttributes("test");
		} catch (Exception e) {
			// System.out.println(e);
		}

		// 5. Check the DirContext returned in step 4: this will throw
		// javax.naming.CannotProceedException
	}

	public void testGetContinuationDirContext3() throws NamingException {
		CannotProceedException cpe = new CannotProceedException(
				"TestGetContinuationDirContext3");
		DirContext ctx = DirectoryManager.getContinuationDirContext(cpe);
		assertNotNull(ctx);
	}

	/*
	 * Mock class for internal use which implements LdapContext and
	 * InitialContextFactory
	 * 
	 */
	class MyLdapContext implements LdapContext, InitialContextFactory {

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.ldap.LdapContext#extendedOperation(javax.naming.ldap.ExtendedRequest)
		 */
		public ExtendedResponse extendedOperation(ExtendedRequest e)
				throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.ldap.LdapContext#getConnectControls()
		 */
		public Control[] getConnectControls() throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.ldap.LdapContext#getRequestControls()
		 */
		public Control[] getRequestControls() throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.ldap.LdapContext#getResponseControls()
		 */
		public Control[] getResponseControls() throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.ldap.LdapContext#newInstance(javax.naming.ldap.Control[])
		 */
		public LdapContext newInstance(Control[] ac) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.ldap.LdapContext#reconnect(javax.naming.ldap.Control[])
		 */
		public void reconnect(Control[] ac) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.ldap.LdapContext#setRequestControls(javax.naming.ldap.Control[])
		 */
		public void setRequestControls(Control[] ac) throws NamingException {

		}

		public Context getInitialContext(Hashtable<?, ?> envmt)
				throws NamingException {
			return null;
		}

		public void bind(Name name, Object obj, Attributes attributes)
				throws NamingException {

		}

		public void bind(String s, Object obj, Attributes attributes)
				throws NamingException {

		}


		public DirContext createSubcontext(Name name, Attributes attributes)
				throws NamingException {
			return null;
		}


		public DirContext createSubcontext(String s, Attributes attributes)
				throws NamingException {

			return null;
		}

		public Attributes getAttributes(Name name) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name,
		 *      java.lang.String[])
		 */
		public Attributes getAttributes(Name name, String[] as)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#getAttributes(java.lang.String)
		 */
		public Attributes getAttributes(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#getAttributes(java.lang.String,
		 *      java.lang.String[])
		 */
		public Attributes getAttributes(String s, String[] as)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#getSchema(javax.naming.Name)
		 */
		public DirContext getSchema(Name name) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#getSchema(java.lang.String)
		 */
		public DirContext getSchema(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#getSchemaClassDefinition(javax.naming.Name)
		 */
		public DirContext getSchemaClassDefinition(Name name)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#getSchemaClassDefinition(java.lang.String)
		 */
		public DirContext getSchemaClassDefinition(String s)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#modifyAttributes(javax.naming.Name,
		 *      int, javax.naming.directory.Attributes)
		 */
		public void modifyAttributes(Name name, int i, Attributes attributes)
				throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#modifyAttributes(javax.naming.Name,
		 *      javax.naming.directory.ModificationItem[])
		 */
		public void modifyAttributes(Name name,
				ModificationItem[] modificationItems) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
		 *      int, javax.naming.directory.Attributes)
		 */
		public void modifyAttributes(String s, int i, Attributes attributes)
				throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
		 *      javax.naming.directory.ModificationItem[])
		 */
		public void modifyAttributes(String s,
				ModificationItem[] modificationItems) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#rebind(javax.naming.Name,
		 *      java.lang.Object, javax.naming.directory.Attributes)
		 */
		public void rebind(Name name, Object obj, Attributes attributes)
				throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#rebind(java.lang.String,
		 *      java.lang.Object, javax.naming.directory.Attributes)
		 */
		public void rebind(String s, Object obj, Attributes attributes)
				throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#search(javax.naming.Name,
		 *      javax.naming.directory.Attributes)
		 */
		public NamingEnumeration<SearchResult> search(Name name, Attributes attributes)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#search(javax.naming.Name,
		 *      javax.naming.directory.Attributes, java.lang.String[])
		 */
		public NamingEnumeration<SearchResult> search(Name name, Attributes attributes,
				String[] as) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#search(javax.naming.Name,
		 *      java.lang.String, java.lang.Object[],
		 *      javax.naming.directory.SearchControls)
		 */
		public NamingEnumeration<SearchResult> search(Name name, String filter,
				Object[] objs, SearchControls searchControls)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#search(javax.naming.Name,
		 *      java.lang.String, javax.naming.directory.SearchControls)
		 */
		public NamingEnumeration<SearchResult> search(Name name, String filter,
				SearchControls searchControls) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#search(java.lang.String,
		 *      javax.naming.directory.Attributes)
		 */
		public NamingEnumeration<SearchResult> search(String name, Attributes attributes)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#search(java.lang.String,
		 *      javax.naming.directory.Attributes, java.lang.String[])
		 */
		public NamingEnumeration<SearchResult> search(String name, Attributes attributes,
				String[] as) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#search(java.lang.String,
		 *      java.lang.String, java.lang.Object[],
		 *      javax.naming.directory.SearchControls)
		 */
		public NamingEnumeration<SearchResult> search(String name, String filter,
				Object[] objs, SearchControls searchControls)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.directory.DirContext#search(java.lang.String,
		 *      java.lang.String, javax.naming.directory.SearchControls)
		 */
		public NamingEnumeration<SearchResult> search(String name, String filter,
				SearchControls searchControls) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#addToEnvironment(java.lang.String,
		 *      java.lang.Object)
		 */
		public Object addToEnvironment(String s, Object o)
				throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
		 */
		public void bind(Name n, Object o) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
		 */
		public void bind(String s, Object o) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#close()
		 */
		public void close() throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#composeName(javax.naming.Name,
		 *      javax.naming.Name)
		 */
		public Name composeName(Name n, Name pfx) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#composeName(java.lang.String,
		 *      java.lang.String)
		 */
		public String composeName(String s, String pfx) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#createSubcontext(javax.naming.Name)
		 */
		public Context createSubcontext(Name n) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#createSubcontext(java.lang.String)
		 */
		public Context createSubcontext(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
		 */
		public void destroySubcontext(Name n) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#destroySubcontext(java.lang.String)
		 */
		public void destroySubcontext(String s) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#getEnvironment()
		 */
		public Hashtable<?, ?> getEnvironment() throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#getNameInNamespace()
		 */
		public String getNameInNamespace() throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#getNameParser(javax.naming.Name)
		 */
		public NameParser getNameParser(Name n) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#getNameParser(java.lang.String)
		 */
		public NameParser getNameParser(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#list(javax.naming.Name)
		 */
		public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#list(java.lang.String)
		 */
		public NamingEnumeration<NameClassPair> list(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#listBindings(javax.naming.Name)
		 */
		public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#listBindings(java.lang.String)
		 */
		public NamingEnumeration<Binding> listBindings(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#lookup(javax.naming.Name)
		 */
		public Object lookup(Name n) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#lookup(java.lang.String)
		 */
		public Object lookup(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#lookupLink(javax.naming.Name)
		 */
		public Object lookupLink(Name n) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#lookupLink(java.lang.String)
		 */
		public Object lookupLink(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
		 */
		public void rebind(Name n, Object o) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
		 */
		public void rebind(String s, Object o) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
		 */
		public Object removeFromEnvironment(String s) throws NamingException {

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#rename(javax.naming.Name,
		 *      javax.naming.Name)
		 */
		public void rename(Name nOld, Name nNew) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
		 */
		public void rename(String sOld, String sNew) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#unbind(javax.naming.Name)
		 */
		public void unbind(Name n) throws NamingException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#unbind(java.lang.String)
		 */
		public void unbind(String s) throws NamingException {

		}
	}

	class MyContext161 implements Context, NameParser, InitialContextFactory,
			ObjectFactory {

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#addToEnvironment(java.lang.String,
		 *      java.lang.Object)
		 */
		public Object addToEnvironment(String s, Object o)
				throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
		 */
		public void bind(Name n, Object o) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
		 */
		public void bind(String s, Object o) throws NamingException {
			// System.out.println("The mock method bind is called!");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#close()
		 */
		public void close() throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#composeName(javax.naming.Name,
		 *      javax.naming.Name)
		 */
		public Name composeName(Name n, Name pfx) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#composeName(java.lang.String,
		 *      java.lang.String)
		 */
		public String composeName(String s, String pfx) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#createSubcontext(javax.naming.Name)
		 */
		public Context createSubcontext(Name n) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#createSubcontext(java.lang.String)
		 */
		public Context createSubcontext(String s) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
		 */
		public void destroySubcontext(Name n) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#destroySubcontext(java.lang.String)
		 */
		public void destroySubcontext(String s) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#getEnvironment()
		 */
		public Hashtable<?, ?> getEnvironment() throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#getNameInNamespace()
		 */
		public String getNameInNamespace() throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#getNameParser(javax.naming.Name)
		 */
		public NameParser getNameParser(Name n) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#getNameParser(java.lang.String)
		 */
		public NameParser getNameParser(String s) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#list(javax.naming.Name)
		 */
		public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#list(java.lang.String)
		 */
		public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#listBindings(javax.naming.Name)
		 */
		public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#listBindings(java.lang.String)
		 */
		public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#lookup(javax.naming.Name)
		 */
		public Object lookup(Name n) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#lookup(java.lang.String)
		 */
		public Object lookup(String s) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#lookupLink(javax.naming.Name)
		 */
		public Object lookupLink(Name n) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#lookupLink(java.lang.String)
		 */
		public Object lookupLink(String s) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
		 */
		public void rebind(Name n, Object o) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
		 */
		public void rebind(String s, Object o) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
		 */
		public Object removeFromEnvironment(String s) throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#rename(javax.naming.Name,
		 *      javax.naming.Name)
		 */
		public void rename(Name nOld, Name nNew) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
		 */
		public void rename(String sOld, String sNew) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#unbind(javax.naming.Name)
		 */
		public void unbind(Name n) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.Context#unbind(java.lang.String)
		 */
		public void unbind(String s) throws NamingException {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.NameParser#parse(java.lang.String)
		 */
		public Name parse(String s) throws InvalidNameException,
				NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.spi.InitialContextFactory#getInitialContext(java.util.Hashtable)
		 */
		public Context getInitialContext(Hashtable<?, ?> envmt)
				throws NamingException {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object,
		 *      javax.naming.Name, javax.naming.Context, java.util.Hashtable)
		 */
		public Object getObjectInstance(Object o, Name n, Context c,
				Hashtable<?, ?> envmt) throws Exception {
			return null;
		}

	}
}
