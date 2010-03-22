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

package org.apache.harmony.jndi.tests.javax.naming;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.LinkLoopException;
import javax.naming.Name;

import junit.framework.TestCase;

public class LinkLoopExceptionTest extends TestCase {

	public void testSetLinkResolvedName() throws InvalidNameException {
		LinkLoopException ex = new LinkLoopException("Test");
		Properties env = new Properties();
		env.put("jndi.syntax.direction", "flat");
		Name name = new CompoundName("Test", env);
		ex.setLinkResolvedName(name);
		ex.setLinkResolvedName(null);
		assertNull(ex.getLinkResolvedName());
	}

	public void testSetLinkRemainingName() throws InvalidNameException {
		LinkLoopException ex = new LinkLoopException("Test");
		Properties env = new Properties();
		env.put("jndi.syntax.direction", "flat");
		Name name = new CompoundName("Test", env);
		ex.setLinkRemainingName(name);
		ex.setLinkRemainingName(null);
		assertNull(ex.getLinkRemainingName());
	}
}
