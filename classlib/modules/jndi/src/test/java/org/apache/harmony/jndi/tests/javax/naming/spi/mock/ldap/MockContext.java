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
/**
 * @author Hugo Beilis
 * @author Leonardo Soler
 * @author Gabriel Miretti
 * @version 1.0
 */
package org.apache.harmony.jndi.tests.javax.naming.spi.mock.ldap;

import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ldap.Control;

/**
 * <p>Implementation of the interfaces Context. This class has the intention of
 * give us a context to test another classes.</p> 
 *
 */
public class MockContext implements Context {
	
	/**
	 * <p>This field is the properties of init context.</p>.
	 */
	protected Hashtable props;
	/**
	 * <p>This field is the control of init context.</p>.
	 */
	protected Control[] concs;
	/**
	 * <p>Constructor method for the context.</p>.
	 */
	public MockContext(Hashtable envmt) {
		this.props = new Hashtable();
		            
	}
	/**
	 * <p>Method to get the propeties of this context.</p>
	 */
	public Hashtable getProps() {
		return this.props;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Object lookup(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Object lookup(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void bind(Name arg0, Object arg1) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void bind(String arg0, Object arg1) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void rebind(Name arg0, Object arg1) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void rebind(String arg0, Object arg1) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void unbind(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void unbind(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void rename(Name arg0, Name arg1) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void rename(String arg0, String arg1) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<NameClassPair> list(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<NameClassPair> list(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<Binding> listBindings(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<Binding> listBindings(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void destroySubcontext(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void destroySubcontext(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Context createSubcontext(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Context createSubcontext(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Object lookupLink(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Object lookupLink(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NameParser getNameParser(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NameParser getNameParser(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Name composeName(Name arg0, Name arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public String composeName(String arg0, String arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Object addToEnvironment(String arg0, Object arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Object removeFromEnvironment(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Hashtable<?, ?> getEnvironment() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void close() throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public String getNameInNamespace() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

}
