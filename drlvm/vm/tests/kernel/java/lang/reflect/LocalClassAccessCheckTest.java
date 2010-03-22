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

package java.lang.reflect;

import junit.framework.TestCase;
import java.lang.reflect.InvocationTargetException;

public class LocalClassAccessCheckTest extends TestCase {
    
    private class PrivateInnerClass {
    	public void publicMethod () {
    	}
    }

    public class InnerClass {
    	public void invokeLocalClassMethod() throws NoSuchMethodException, IllegalAccessException, 
                InvocationTargetException {
    		class LocalClass {
    			public void localMethod() {
    			}
    		};
    		LocalClass localClass = new LocalClass();
    		localClass.localMethod();
    		localClass.getClass().getMethod("localMethod", (Class[])null).invoke(localClass, (Object[])null);
    	}

    	public void invokePrivateInnerClassPublicMethod() {
	        class LocalClass {
		        public void localMethod() throws NoSuchMethodException, IllegalAccessException, 
                InvocationTargetException {
		        	PrivateInnerClass pic = new PrivateInnerClass();
		        	pic.publicMethod();
		        	pic.getClass().getMethod("publicMethod", (Class[])null).invoke(pic, (Object[])null);;
		        }
	        };
	        LocalClass localClass = new LocalClass();
	        try {
	        localClass.localMethod();
	        localClass.getClass().getMethod("localMethod", (Class[])null).invoke(localClass, (Object[])null);
	        } catch (Exception e) {
	        	fail(e + " has been thrown");
	        }
        }
    }
    
    public void testAccessLocalClassMethod() {
    	InnerClass ic = new InnerClass();
	    try {
	    	ic.invokeLocalClassMethod();
        } catch (Exception e) {
        	fail(e + " has been thrown");
        }
	}

    public void testAccessInnerClassFromLocalClass() {
    	InnerClass ic = new InnerClass();
	    try {
	    	ic.invokePrivateInnerClassPublicMethod();
        } catch (Exception e) {
        	fail(e + " has been thrown");
        }
	}
}
