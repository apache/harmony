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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


/**
 * <p>Implementation of the interfaces DirContext. This class has the intention of
 * give us a dircontext to test another classes.</p> 
 *
 */
public class MockDirContext extends MockContext implements DirContext {

	protected Context defaultInitCtx;//FIXME
	/**
	 * <p>Constructor method for the dircontext.</p>.
	 */
	public MockDirContext(Hashtable h) {
		super(h);
		this.defaultInitCtx=this;//FIXME
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Attributes getAttributes(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Attributes getAttributes(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Attributes getAttributes(Name arg0, String[] arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public Attributes getAttributes(String arg0, String[] arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void modifyAttributes(Name arg0, int arg1, Attributes arg2) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void modifyAttributes(String arg0, int arg1, Attributes arg2) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void modifyAttributes(Name arg0, ModificationItem[] arg1) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void modifyAttributes(String arg0, ModificationItem[] arg1) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void bind(Name arg0, Object arg1, Attributes arg2) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void bind(String arg0, Object arg1, Attributes arg2) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void rebind(Name arg0, Object arg1, Attributes arg2) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public void rebind(String arg0, Object arg1, Attributes arg2) throws NamingException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public DirContext createSubcontext(Name arg0, Attributes arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public DirContext createSubcontext(String arg0, Attributes arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public DirContext getSchema(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public DirContext getSchema(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public DirContext getSchemaClassDefinition(Name arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public DirContext getSchemaClassDefinition(String arg0) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<SearchResult> search(Name arg0, Attributes arg1, String[] arg2) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<SearchResult> search(String arg0, Attributes arg1, String[] arg2) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<SearchResult> search(Name arg0, Attributes arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<SearchResult> search(String arg0, Attributes arg1) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<SearchResult> search(Name arg0, String arg1, SearchControls arg2) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<SearchResult> search(String arg0, String arg1, SearchControls arg2) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<SearchResult> search(Name arg0, String arg1, Object[] arg2, SearchControls arg3) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * <p>Method not implemented yet.</p>
	 */
	public NamingEnumeration<SearchResult> search(String arg0, String arg1, Object[] arg2, SearchControls arg3) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
