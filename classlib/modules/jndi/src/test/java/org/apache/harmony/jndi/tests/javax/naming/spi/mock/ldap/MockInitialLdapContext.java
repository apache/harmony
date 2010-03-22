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

import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * <p>Implementation of the interfaces LdapContext. This class has the intention of
 * give us a context to test another classes.</p> 
 *
 */
public class MockInitialLdapContext extends MockDirContext implements LdapContext {

	/**
	 * <p>This field is the properties of init context.</p>.
	 */
	private Hashtable env=null;
	/**
	 * <p>This field is a temporary control of init context.</p>.
	 */
	Control[] tempcs=null;
	/**
	 * <p>This field is a falg to know if a reconection was made.</p>.
	 */
	boolean flag=false;
	/**
	 * <p>This field is the connection control of init context.</p>.
	 */
	Control[] concs=null;
	/**
	 * <p>This field is the request control of init context.</p>.
	 */
	private Control[] reqcs;
	/**
	 * <p>This field is the last response control of init context.</p>.
	 */
	Control[] last=null;
	
	/**
	 * <p>This method retrieves the properties.</p>
	 */
	public Hashtable getProps(){
		return env;
		
	}
	
	/**
	 * <p>Constructor method for this mock.</p>
	 * @param envmt The properties.
	 * @throws NamingException If an error is encounter.
	 */
	public MockInitialLdapContext(Hashtable envmt) throws NamingException {
		super(envmt);
		this.env=(Hashtable) envmt.clone();
		
	}
	/**
	 * <p>This method is performs an extended operation.</p>
	 * @param arg0 The extended request for the operation.
	 * @return ExtendedResponse The extended response of this operation.
	 */
	public ExtendedResponse extendedOperation(ExtendedRequest arg0)	throws NamingException , NullPointerException{

		if(arg0==null) throw new NullPointerException();
		if(arg0.getID()=="1.3.6.1.4.1.1466.20037") return new MockStartTlsResponse();
		return null;
	}
	
	/**
	 * <p>This method creates a new instance of this context initialized using request controls.</p>
	 * @param arg0 The controls to be use for the new instance.
	 * @return LdapContext The new context.
	 */
	public LdapContext newInstance(Control[] arg0) throws NamingException {
		if(arg0==null||arg0!=null){
			return new InitialLdapContext(env,arg0);
		}
		throw new NamingException();
	}
	
	/**
	 * <p>This method reconnects to the LDAP server using the supplied controls and this context's environment.</p>
	 * @param arg0 The controls to be use for the reconnection.
	 */
	public void reconnect(Control[] arg0) throws NamingException {
		
		tempcs=arg0;
		flag=true;

	}
	
	/**
	 * <p>This method close the connection to the LDAP server.</p>
	 */
	public void close() throws NamingException {

		super.close();

	}
	
	/**
	 * <p>This method retrieves the connection controls.</p>
	 * @return The connection controls.
	 */
	public Control[] getConnectControls() throws NamingException {
		if(flag){
			this.last=tempcs;
			return tempcs;
		}
		this.last=concs;
		return this.concs;
		
	}

	/**
	 * <p>This method set new controls to a request.</p>
	 * @param arg0 The new set of controls.
	 */
	public void setRequestControls(Control[] arg0) throws NamingException {
		
		if (null != arg0) {
            this.reqcs = new Control[arg0.length];
            System.arraycopy(arg0, 0, reqcs, 0, arg0.length);
            
        }else {
        	this.reqcs=arg0;
        }

	}

	/**
	 * <p>This method get the set of controls of a request.</p>
	 * @return The set of controls.
	 */
	public Control[] getRequestControls() throws NamingException {
		this.last=reqcs;
		return this.reqcs;
		
		
	}
	/**
	 * <p>This method get the set of controls of a request.</p>
	 * @return The set of controls.
	 */
	public Control[] getResponseControls() throws NamingException {
		
		return this.last;
				
	}
		
}
