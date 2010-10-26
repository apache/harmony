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

package org.apache.harmony.tests.tools.javac;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;

import com.sun.tools.javac.Main;

public class MainTest extends TestCase {	
	
	private static final String RESOURCES = "resources/";
	
	/**
	 * Method that takes in a non-existent file and checks the output for the appropriate Error message
	 * 
	 */
    public void test_nonExists()  {
        final StringWriter out = new StringWriter();
        final String testStr = "no_this_test.java";
        final int rc = Main.compile(new String[]{testStr}, new PrintWriter(out));        
        assertTrue("The output should have " + testStr, out.toString().contains("missing") && rc == 1);
    }
    
    /**
     * Method that takes a valid (A pgm without any errors) file and tests for the proper return code
     */
    public void test_exists()
    {
    	final StringWriter out = new StringWriter();
    	final StringWriter err = new StringWriter();
        	
    	final String srcFile =  RESOURCES + "Simple.java";
    	final File f = new File(srcFile);
    	final String testStr =  f.getAbsolutePath();
        	
        final int rc = Main.compile(new String[]{testStr}, new PrintWriter(out), new PrintWriter(err));        
        assertTrue("The program " + testStr + " should cleanly compile", err.toString().trim().equals("") && rc == 0 );
    }
	
    /**
     * Method that takes a valid (A program without any errors) file but with unresolved dependencies and tests for the proper return code     
     */
    public void test_existsWithUnresolvedDep()
    {
    	final StringWriter out = new StringWriter();
    	final StringWriter err = new StringWriter();
        
    	final String srcFile =  RESOURCES + "Sample.java";
    	final File f = new File(srcFile);
    	final String testStr =  f.getAbsolutePath();
    	
    	final int rc = Main.compile(new String[]{testStr}, new PrintWriter(out), new PrintWriter(err));       
        assertTrue("The program " + testStr + " shouldn't compile due to unresolved dependencies", err.toString().contains("ERROR") && (rc == 1) );
    }
    
    /**
     * Method that takes a valid (A program without any errors) file  with Resolved dependencies and tests for the proper return code    
     */
    public void test_existsWithResolvedDep()
    {
    	final StringWriter out = new StringWriter();
    	final StringWriter err = new StringWriter();
    	
    	final String srcFile =  RESOURCES + "Sample.java";
    	final File f = new File(srcFile);
    	final String testStr =  f.getAbsolutePath();
        
        final String option1 =  "-classpath" ;
        
        final String jarFile =  RESOURCES + "Dependency.jar";
    	final File f1 = new File(jarFile);
    	final String option2 =  f1.getAbsolutePath();
    	
        final int rc = Main.compile(new String[]{testStr, option1, option2}, new PrintWriter(out), new PrintWriter(err));        
        assertTrue("The program " + testStr + " should compile as dependency " +  option2 + " is resolved", ! err.toString().contains("ERROR") && (rc == 0) );
    }  
	
	
}
