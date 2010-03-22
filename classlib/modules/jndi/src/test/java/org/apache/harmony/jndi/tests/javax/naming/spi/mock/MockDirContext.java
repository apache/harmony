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
package org.apache.harmony.jndi.tests.javax.naming.spi.mock;

import java.util.Hashtable;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class MockDirContext extends MockContext implements DirContext {

	public MockDirContext(Hashtable<?, ?> h) {
		super(h);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#bind(javax.naming.Name,
	 *      java.lang.Object, javax.naming.directory.Attributes)
	 */
	public void bind(Name name, Object obj, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "bind",
				name, obj, attributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#bind(java.lang.String,
	 *      java.lang.Object, javax.naming.directory.Attributes)
	 */
	public void bind(String s, Object obj, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "bind",
				s, obj, attributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#createSubcontext(javax.naming.Name,
	 *      javax.naming.directory.Attributes)
	 */
	public DirContext createSubcontext(Name name, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"createSubcontext", name, attributes);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#createSubcontext(java.lang.String,
	 *      javax.naming.directory.Attributes)
	 */
	public DirContext createSubcontext(String s, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"createSubcontext", s, attributes);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name)
	 */
	public Attributes getAttributes(Name name) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"getAttributes", name);
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
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"getAttributes", name, as);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#getAttributes(java.lang.String)
	 */
	public Attributes getAttributes(String s) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"getAttributes", s);
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
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"getAttributes", s, as);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#getSchema(javax.naming.Name)
	 */
	public DirContext getSchema(Name name) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"getSchema", name);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#getSchema(java.lang.String)
	 */
	public DirContext getSchema(String s) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"getSchema", s);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#getSchemaClassDefinition(javax.naming.Name)
	 */
	public DirContext getSchemaClassDefinition(Name name)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"getSchemaClassDefinition", name);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#getSchemaClassDefinition(java.lang.String)
	 */
	public DirContext getSchemaClassDefinition(String s) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"getSchemaClassDefinition", s);
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
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"modifyAttributes", name, new Integer(i), attributes);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#modifyAttributes(javax.naming.Name,
	 *      javax.naming.directory.ModificationItem[])
	 */
	public void modifyAttributes(Name name, ModificationItem[] amodificationitem)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"modifyAttributes", name, amodificationitem);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
	 *      int, javax.naming.directory.Attributes)
	 */
	public void modifyAttributes(String s, int i, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"modifyAttributes", s, new Integer(i), attributes);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
	 *      javax.naming.directory.ModificationItem[])
	 */
	public void modifyAttributes(String s, ModificationItem[] amodificationitem)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"),
				"modifyAttributes", s, amodificationitem);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#rebind(javax.naming.Name,
	 *      java.lang.Object, javax.naming.directory.Attributes)
	 */
	public void rebind(Name name, Object obj, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "rebind",
				name, obj, attributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#rebind(java.lang.String,
	 *      java.lang.Object, javax.naming.directory.Attributes)
	 */
	public void rebind(String s, Object obj, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "rebind",
				s, obj, attributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#search(javax.naming.Name,
	 *      javax.naming.directory.Attributes)
	 */
	public NamingEnumeration<SearchResult> search(Name name, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "search",
				name, attributes);
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
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "search",
				name, attributes, as);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#search(javax.naming.Name,
	 *      java.lang.String, java.lang.Object[],
	 *      javax.naming.directory.SearchControls)
	 */
	public NamingEnumeration<SearchResult> search(Name name, String s, Object[] aobj,
			SearchControls searchcontrols) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "search",
				name, s, aobj, searchcontrols);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#search(javax.naming.Name,
	 *      java.lang.String, javax.naming.directory.SearchControls)
	 */
	public NamingEnumeration<SearchResult> search(Name name, String s,
			SearchControls searchcontrols) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "search",
				name, s, searchcontrols);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#search(java.lang.String,
	 *      javax.naming.directory.Attributes)
	 */
	public NamingEnumeration<SearchResult> search(String s, Attributes attributes)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "search",
				s, attributes);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#search(java.lang.String,
	 *      javax.naming.directory.Attributes, java.lang.String[])
	 */
	public NamingEnumeration<SearchResult> search(String s, Attributes attributes, String[] as)
			throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "search",
				s, attributes, as);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#search(java.lang.String,
	 *      java.lang.String, java.lang.Object[],
	 *      javax.naming.directory.SearchControls)
	 */
	public NamingEnumeration<SearchResult> search(String s, String s1, Object[] aobj,
			SearchControls searchcontrols) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "search",
				s, s1, aobj, searchcontrols);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.directory.DirContext#search(java.lang.String,
	 *      java.lang.String, javax.naming.directory.SearchControls)
	 */
	public NamingEnumeration<SearchResult> search(String s, String s1,
			SearchControls searchcontrols) throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "search",
				s, s1, searchcontrols);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#close()
	 */
	@Override
    public void close() throws NamingException {
		InvokeRecord.set((String) getEnvironment().get("url.schema"), "close");
	}

}
