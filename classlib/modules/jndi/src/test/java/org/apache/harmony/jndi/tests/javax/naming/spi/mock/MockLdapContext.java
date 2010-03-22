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

import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

public class MockLdapContext extends MockDirContext implements LdapContext {
	Hashtable<String, Object> ldapProps;

	@SuppressWarnings("unchecked")
    public MockLdapContext(Hashtable<?, ?> h) {
		super(h);
		this.ldapProps = (Hashtable<String, Object>) h.clone();
	}

	public Hashtable<String, Object> getProps() {
		return this.ldapProps;
	}

	public ExtendedResponse extendedOperation(ExtendedRequest e)
			throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), e);
		takeActions();
		return null;
	}

	public LdapContext newInstance(Control[] ac) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), ac);
		takeActions();
		return null;
	}

	public void reconnect(Control[] ac) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), ac);
		takeActions();
	}

	public Control[] getConnectControls() throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"),
				"getConnectControls");
		takeActions();
		return null;
	}

	public void setRequestControls(Control[] ac) throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"), ac);
		takeActions();
	}

	public Control[] getRequestControls() throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"),
				"getRequestControls");
		takeActions();
		return null;
	}

	public Control[] getResponseControls() throws NamingException {
		InvokeRecord.set((String) this.props.get("url.schema"),
				"getResponseControls");
		takeActions();
		return null;
	}

}
