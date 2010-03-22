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

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class MockContext implements Context {
	protected Hashtable<String, Object> props;

	static protected DazzleActionController actions;

	public MockContext() {
		this.props = new Hashtable<String, Object>();
	}

	@SuppressWarnings("unchecked")
    public MockContext(Hashtable<?, ?> props) {
		if (null != props) {
			this.props = (Hashtable<String, Object>) props.clone();
		}
	}

	@Override
    public boolean equals(Object obj) {
		if (obj instanceof MockContext) {
			MockContext theOther = (MockContext) obj;
			boolean envmtEqual = (null == props ? null == theOther.props
					: props.equals(theOther.props));
			if (!envmtEqual) {
				return false;
			}

			return true;
		}
        return false;
	}

	public static void setActionController(
			DazzleActionController actionController) {
		MockContext.actions = actionController;
	}

	Object takeActions() throws NamingException {
		if (actions == null) {
			return DazzleActionController.RETURN_NORMAL;
		}
        Object obj = actions.doActions();
        return obj;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#addToEnvironment(java.lang.String,
	 *      java.lang.Object)
	 */
	public Object addToEnvironment(String s, Object o) throws NamingException {
		return this.props.put(s, o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
	 */
	public void bind(Name n, Object o) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n, o);
		takeActions();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
	 */
	public void bind(String s, Object o) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s, o);
		takeActions();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#close()
	 */
	public void close() throws NamingException {
		InvokeRecord.set(null, "close");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#composeName(javax.naming.Name,
	 *      javax.naming.Name)
	 */
	public Name composeName(Name n, Name pfx) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n, pfx);
		takeActions();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
	 */
	public String composeName(String s, String pfx) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s, pfx);
		takeActions();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#createSubcontext(javax.naming.Name)
	 */
	public Context createSubcontext(Name n) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n);
		takeActions();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#createSubcontext(java.lang.String)
	 */
	public Context createSubcontext(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);
		takeActions();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
	 */
	public void destroySubcontext(Name n) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n);
		takeActions();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#destroySubcontext(java.lang.String)
	 */
	public void destroySubcontext(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);
		takeActions();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#getEnvironment()
	 */
	public Hashtable<String, Object> getEnvironment() throws NamingException {
		return this.props;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#getNameInNamespace()
	 */
	public String getNameInNamespace() throws NamingException {
		return (String) takeActions();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#getNameParser(javax.naming.Name)
	 */
	public NameParser getNameParser(Name n) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n);
		takeActions();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#getNameParser(java.lang.String)
	 */
	public NameParser getNameParser(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);
		takeActions();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#list(javax.naming.Name)
	 */
	public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#list(java.lang.String)
	 */
	public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#listBindings(javax.naming.Name)
	 */
	public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#listBindings(java.lang.String)
	 */
	public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#lookup(javax.naming.Name)
	 */
	public Object lookup(Name n) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#lookup(java.lang.String)
	 */
	public Object lookup(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#lookupLink(javax.naming.Name)
	 */
	public Object lookupLink(Name n) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#lookupLink(java.lang.String)
	 */
	public Object lookupLink(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
	 */
	public void rebind(Name n, Object o) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n, o);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
	 */
	public void rebind(String s, Object o) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s, o);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
	 */
	public Object removeFromEnvironment(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);
		return this.props.remove(s);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
	 */
	public void rename(Name nOld, Name nNew) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), nOld, nNew);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
	 */
	public void rename(String sOld, String sNew) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), sOld, sNew);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#unbind(javax.naming.Name)
	 */
	public void unbind(Name n) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), n);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.Context#unbind(java.lang.String)
	 */
	public void unbind(String s) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), s);

	}

}
