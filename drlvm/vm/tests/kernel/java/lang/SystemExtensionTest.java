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
 * @author Serguei S.Zapreyev
 */

package java.lang;

import java.io.File;

import junit.framework.TestCase;

/*
 * Created on 01.31.2006
 * 
 * This SystemExtensionTest class is used to test the Core API
 * java.lang.System class
 *  
 */

public class SystemExtensionTest extends TestCase {

    /**
     *  
     */
    public void test_arraycopy_Obj_I_Obj_I_I() {
        class X {
            public int fld;
            public X(int i) { fld = i; }
        }
        X ax1[] = new X[]{new X(0), new X(1), new X(2), new X(3), new X(4), new X(5), new X(6), new X(7), new X(8), new X(9)};
        X ax2[] = new X[20];
        try {
            System.arraycopy((Object)ax1, 5, (Object)ax2, 12, 3);
            assertTrue("Error1", ax2[12].fld == 5 && ax2[13].fld == 6 && ax2[14].fld == 7 );
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
        System.out.println("test_arraycopy_Obj_I_Obj_I_I");
    }

    /**
     *  
     */
    public void test_currentTimeMillis_V() {
            assertTrue("Error1", (System.currentTimeMillis() - System.currentTimeMillis()) <= 0);
            System.out.println("test_currentTimeMillis_V");
    }

    /**
     *  
     */
    public void test_exit_I() {
            //exit(777);
        System.out.println("test_exit_I");
    }


	/**
	 *  
	 */
	public void test_gc() {
		long r1=Runtime.getRuntime().freeMemory();
		long r2, r4;
		String[] sa = new String[(int)r1/50000];
		int ind1=0;
			try {
				String stmp = "";
				for(int ind2=0; ind2<50/*1000*/; ind2++){
					stmp += "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
					 "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
				}
				for(ind1=0; ind1 <(int)r1/50000; ind1++){
					sa[ind1]=""+stmp;
				}
		
				r2=Runtime.getRuntime().freeMemory();
				for(ind1=0; ind1 <(int)r1/50000; /*ind1++*/ind1+=100){
					sa[ind1]=null;
					System.gc();
					try{Thread.sleep(10);}catch(Exception e){}
				}
				sa=null;
				try{Thread.sleep(500);}catch(Exception e){}
				System.gc();
				try{Thread.sleep(500);}catch(Exception e){}
				r4=Runtime.getRuntime().freeMemory();
				
				if( !(r4>r2) ){
					System.out.println( "WARNNING: It would be better if System.gc method could initiate garbage collecting! (Here we have "+r4+" !> "+r2+" .)");
				}
			} catch (OutOfMemoryError e) {
			    System.out.println( "WARNNING: test did not check System.gc method due to the technical reason !");
			} catch (Exception e) {
	            fail("Error1: " + e.toString());
			}
	        System.out.println("test_gc");
	}

    /**
     *  See SystemTest.testGetenvString()
     */
    //public void test_getenv_Str() {
    //}

    /**
     *  
     */
    public void test_getProperties_V() {
        try {
            java.util.Properties pl = System.getProperties();
            for (java.util.Enumeration e = pl.propertyNames() ; e.hasMoreElements() ;) {
                if (((String)e.nextElement()).equals("os.name")) {
                    System.out.println("test_getProperties_V");
                    return;
                }
            }
            fail("Error1");
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getProperty_Str() {
        try {
            if (System.getProperty("java.version")!=null || System.getProperty("java.vendor")!=null || System.getProperty("java.vendor.url")!=null || System.getProperty("java.home")!=null || System.getProperty("java.vm.specification.version")!=null || System.getProperty("java.vm.specification.vendor")!=null || System.getProperty("java.vm.specification.name")!=null || System.getProperty("java.vm.version")!=null || System.getProperty("java.vm.vendor")!=null || System.getProperty("java.vm.name")!=null || System.getProperty("java.specification.version")!=null || System.getProperty("java.specification.vendor")!=null || System.getProperty("java.specification.name")!=null || System.getProperty("java.class.version")!=null || System.getProperty("java.class.path")!=null || System.getProperty("java.library.path")!=null || System.getProperty("java.io.tmpdir")!=null || System.getProperty("java.compiler")!=null || System.getProperty("java.ext.dirs")!=null || System.getProperty("os.name")!=null || System.getProperty("os.arch")!=null || System.getProperty("os.version")!=null || System.getProperty("file.separator")!=null || System.getProperty("path.separator")!=null || System.getProperty("line.separator")!=null || System.getProperty("user.name")!=null || System.getProperty("user.home")!=null || System.getProperty("user.dir")!=null || System.getProperty("os.name")!=null) {
                System.out.println("test_getProperty_Str");
                return;
            }
            fail("Error1");
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getProperty_Str_Str() {
        try {
            if (System.getProperty(System.getProperty("os.name")+"_UND_"+System.getProperty("user.name"), "ZSS").equals("ZSS")) {
                System.out.println("test_getProperty_Str_Str");
                return;
            }
            fail("Error1");
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getSecurityManager_V() {
            assertTrue("Error1", System.getSecurityManager() == null);
            System.out.println("test_getSecurityManager_V");
    }

    /**
     *  
     */
    public void test_identityHashCode_Obj() {
        assertTrue("Error1", System.identityHashCode(null) == 0);
        Boolean o1 = new Boolean(true);
        assertTrue("Error2", System.identityHashCode(o1) != o1.hashCode());
        Byte o2 = new Byte(Byte.MAX_VALUE);
        assertTrue("Error3", System.identityHashCode(o2) != o2.hashCode());
        Character o3 = new Character(Character.MAX_VALUE);
        assertTrue("Error4", System.identityHashCode(o3) != o3.hashCode());
        Double o4 = new Double(Double.MAX_VALUE);
        assertTrue("Error5", System.identityHashCode(o4) != o4.hashCode());
        Float o5 = new Float(Float.MAX_VALUE);
        assertTrue("Error6", System.identityHashCode(o5) != o5.hashCode());
        Integer o6 = new Integer(Integer.MAX_VALUE);
        assertTrue("Error7", System.identityHashCode(o6) != o6.hashCode());
        Long o7 = new Long(Long.MAX_VALUE);
        assertTrue("Error8", System.identityHashCode(o7) != o7.hashCode());
        Short o8 = new Short(Short.MAX_VALUE);
        assertTrue("Error9", System.identityHashCode(o8) != o8.hashCode());
        System.out.println("test_identityHashCode_Obj");
    }

    /**
     *  
     */
    public void test_load_Str() {
		String jLP = null;

		String jlp = System.getProperty("java.library.path");
		String vblp = System.getProperty("vm.boot.library.path");
		jLP = ( jlp!=null && jlp.length()!=0 ?jlp:"")+( vblp!=null && vblp.length()!=0 ?File.pathSeparator+vblp:"");

		if (jLP.length()==0) {
			System.out.println( "WARNNING: test didn't check the loading process.");
			return;
		}
		String[] paths = jLP.split(File.pathSeparator);
		String ext = (System.getProperty("os.name").indexOf("indows")!=-1 ? ".dll":".so");
		int ind1;
		int ind2;
		File[] asf = null;
		for (ind1 = 0; ind1< paths.length; ind1++){
			asf = new java.io.File(paths[ind1]).listFiles();
			if (asf!=null) {
				for (ind2 = 0; ind2< asf.length; ind2++){
					if (asf[ind2].getName().indexOf(ext)!=-1){
						try{
							System.load(asf[ind2].getCanonicalPath());
					        System.out.println("test_load_Str");
							return;
						} catch (UnsatisfiedLinkError e) {
							continue;
						} catch (Throwable e) {
							continue;
						}					
					}
				}
			}
		}
		fail("System.load method should provide loading a dynamic library!");
    }

    /**
     *  
     */
    public void test_loadLibrary_Str() {
		String jLP = null; 

		String jlp = System.getProperty("java.library.path");
		String vblp = System.getProperty("vm.boot.library.path");
		jLP = ( jlp!=null && jlp.length()!=0 ?jlp:"")+( vblp!=null && vblp.length()!=0 ?File.pathSeparator+vblp:"");
		if (jLP.length()==0) {
			System.out.println( "WARNNING: test didn't check the loading process.");
			return;
		}
		String[] paths = jLP.split(File.pathSeparator);
		String ext = (System.getProperty("os.name").indexOf("indows")!=-1 ? ".dll":".so");
		int ind1;
		int ind2;
		File[] asf = null;
		for (ind1 = 0; ind1< paths.length; ind1++){
			if (paths[ind1]==null) {
				continue;
			}
			asf = new java.io.File(paths[ind1]).listFiles();
			if (asf!=null) {
				for (ind2 = 0; ind2< asf.length; ind2++){
					if (asf[ind2].getName().indexOf(ext)!=-1){
						String libName = asf[ind2].getName();
						if(ext.equals(".dll")){
							libName = libName.substring(0,libName.length()-4);
						} else {
							libName = libName.substring(3,libName.length()-3);
						}
						try{
							System.loadLibrary(libName);
					        System.out.println("test_loadLibrary_Str");
							return;
						} catch (UnsatisfiedLinkError e) {
							continue;
						} catch (Throwable e) {
							continue;
						}					
					
					}
				}
			}
		}
		fail("System.loadLibrary method should provide loading a dynamic library!");
    }

    /**
     *  
     */
    public void test_mapLibraryName_Str() {
        assertTrue("Error1", System.mapLibraryName("lib").indexOf((System.getProperty("os.name").toLowerCase().indexOf("windows")!=-1 ? ".dll":".so")) != -1);
        System.out.println("test_mapLibraryName_Str");
    }

    /**
     *  
     */
	static class forInternalUseOnly {
		String stmp;

		forInternalUseOnly () {
			this.stmp = "";
			for(int ind2=0; ind2<100; ind2++){
				this.stmp += "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"+
				"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
			}
		}
		protected void finalize() throws Throwable {
			runFinalizationFlag = true;
			super.finalize();
		}
	}

	static boolean runFinalizationFlag = false;
    public void test_runFinalization_V() {
		runFinalizationFlag = false;
		for(int ind2=0; ind2<10; ind2++){
			forInternalUseOnly ins = new forInternalUseOnly();
			ins.stmp += "";
			ins = null;
			try{Thread.sleep(10);}catch(Exception e){}
			System.gc();
			try{Thread.sleep(10);}catch(Exception e){}
			System.runFinalization();
		}
		assertTrue( "ERROR1: someting's wrong", runFinalizationFlag);
        System.out.println("test_runFinalization_V");
    }

    /**
     *  
     */
    public void test_runFinalizersOnExit_Z() {
		runFinalizationFlag = false;
		for(int ind2=0; ind2<5; ind2++){
			System.runFinalizersOnExit(false);
			forInternalUseOnly ins = new forInternalUseOnly();
			ins.stmp += "";
			ins = null;
			try{Thread.sleep(10);}catch(Exception e){}
			System.gc();
			try{Thread.sleep(10);}catch(Exception e){}
		}
		assertTrue( "ERROR1: someting's wrong", runFinalizationFlag);
		
		runFinalizationFlag = false;
		for(int ind2=0; ind2<5; ind2++){
		    System.runFinalizersOnExit(true);
			forInternalUseOnly ins = new forInternalUseOnly();
			ins.stmp += "";
			ins = null;
			try{Thread.sleep(10);}catch(Exception e){}
			System.gc();
			try{Thread.sleep(10);}catch(Exception e){}
		}
		assertTrue( "ERROR2: someting's wrong", runFinalizationFlag);
        System.out.println("test_runFinalizersOnExit_Z");
    }

    /**
     *  
     */
    public void test_setErr_Pri() {
        java.io.PrintStream cp = System.err;
        java.io.ByteArrayOutputStream es = new java.io.ByteArrayOutputStream(1000);
		System.setErr(new java.io.PrintStream(es));
		System.err.print("Serguei S.Zapreyev");
		System.err.flush();
		System.setErr(cp);
		assertTrue("Error1", es.toString().indexOf("Serguei S.Zapreyev") != -1);
        System.out.println("test_setErr_Pri");
    }

    /**
     *  
     */
    public void test_setIn_Inp() {
        java.io.InputStream cp = System.in;
        byte ab[] = new byte[300];
		System.setIn(new java.io.StringBufferInputStream("Serguei S.Zapreyev"));
		try {
		    System.in.read(ab, 0, 18);
		} catch (java.io.IOException _) {
			System.setIn(cp);
		    fail("Error1");
		}
		System.setIn(cp);
		assertTrue("Error2", new String(ab).toString().indexOf("Serguei S.Zapreyev") != -1);
        System.out.println("test_setIn_Inp");
    }

    /**
     *  
     */
    public void test_setOut_Pri() {
        java.io.PrintStream cp = System.out;
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream(1000);
		System.setOut(new java.io.PrintStream(os));
		System.out.print("Serguei S.Zapreyev");
		System.out.flush();
		System.setOut(cp);
		assertTrue("Error1", os.toString().indexOf("Serguei S.Zapreyev") != -1);
        System.out.println("test_setOut_Pri");
    }

    /**
     *  
     */
    public void test_setProperties_Pro() {
        try {
            java.util.Properties pl = System.getProperties();
            pl.setProperty("SeStZa", "ZAPREYEV SERGUEI");
            System.setProperties(pl);
            if (System.getProperty("SeStZa").equals("ZAPREYEV SERGUEI")) {
                System.out.println("test_setProperties_Pro");
                return;
            }
            fail("Error1");
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_setProperty_Str_Str() {
        try {
            System.setProperty("ZSS", "Serguei S.Zapreyev");
            if (System.getProperty("ZSS").equals("Serguei S.Zapreyev")) {
                System.out.println("test_setProperty_Str_Str");
                return;
            }
            fail("Error1");
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
	static String policyFile;
	static {
		if (System.getProperty("java.vm.vendor").equals("Intel DRL")) {
			//policyFile = "drl.policy";
			policyFile = "java.policy";
		} else {
			policyFile = "java.policy";			
		}
	}
// Commented because of the drlvm issue (env.SYSTEMDRIVE)
    public void te_st_setSecurityManager_Sec() {
        setAllPermissions();
        assertTrue("Error1", System.getSecurityManager() != null);
        setNoPermissions();
        try {
            new SystemExtensionTest().test_load_Str();
            fail("Error2");
        } catch (SecurityException  e) {
            // correct behaviour
        } catch (Exception  e) {
            fail("Error3: " + e.toString());
        }
        System.setSecurityManager(null);
        assertTrue("Error4", System.getSecurityManager() == null);
        
		File fff = null;
		try {
			fff = new File(System.getProperty("user.home") + java.io.File.separator + "."+policyFile+".copiedBySMT");
			if (fff.exists()) {
				File fff3 = new File(System.getProperty("user.home") + java.io.File.separator + "."+policyFile+"");
				if (fff3.exists()) {
					fff3.delete();
				}
				fff.renameTo(fff3);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
        System.out.println("test_setSecurityManager_Sec");
    }

	/**
	 *  
	 */
	private void setTestPermission0(String perm, String arg1, String arg2, boolean changeSM) {
		SecurityManager sm = System.getSecurityManager();
		try {
				File fff2 = null;
				java.io.DataOutputStream dos2 = null;
				try {
					fff2 = new File(System.getProperty("user.home") + java.io.File.separator + "."+policyFile+"");
					if (fff2.exists()) {
						File fff3 = new File(System.getProperty("user.home") + java.io.File.separator + "."+policyFile+".copiedBySMT");
						if (fff3.exists()) {
							fff2.delete();
							fff2 = new File(System.getProperty("user.home") + java.io.File.separator + "."+policyFile+"");
						} else {
							fff2.renameTo(fff3);
						}
					}
					;
					fff2.createNewFile();
					dos2 = new java.io.DataOutputStream(new java.io.FileOutputStream(fff2));
				} catch (Throwable e) {
					e.printStackTrace();
				}

				try {
					dos2.writeBytes("grant {\n");
					String tmpstr = "";
					if (arg1.length() != 0 || arg2.length() != 0) {
						if (arg1.length() != 0 && arg2.length() != 0) {
							tmpstr = " \"" + arg1 + "\", \"" + arg2 + "\"";
						} else {
							tmpstr = " \"" + arg1 + arg2 + "\"";
						}
					}
					if (perm != null && perm.length() != 0){
						dos2.writeBytes("permission " + perm + tmpstr + ";\n");
					}
					if (changeSM) {
						dos2.writeBytes("permission " + "java.lang.RuntimePermission \"createSecurityManager\";\n");
						dos2.writeBytes("permission " + "java.lang.RuntimePermission \"setSecurityManager\";\n");
						dos2.writeBytes("permission " + "java.util.PropertyPermission \"user.home\", \"read\";\n");
						String tmp = System.getProperty("user.home") + java.io.File.separator + "."+policyFile+"";
						tmp = tmp.replace(java.io.File.separatorChar,'=');
						tmp = tmp.replaceAll("=", "==");
						tmp = tmp.replace('=',java.io.File.separatorChar);
						dos2.writeBytes("permission " + "java.io.FilePermission \""+tmp+"\", \"read,delete,write\";\n");
						tmp = System.getProperty("user.home") + java.io.File.separator + "."+policyFile+".copiedBySMT";
						tmp = tmp.replace(java.io.File.separatorChar,'=');
						tmp = tmp.replaceAll("=", "==");
						tmp = tmp.replace('=',java.io.File.separatorChar);
						dos2.writeBytes("permission " + "java.io.FilePermission \""+tmp+"\", \"read,delete\";\n");
						dos2.writeBytes("permission " + "java.util.PropertyPermission \"java.security.policy\", \"write\";\n");
						dos2.writeBytes("permission " + "java.security.SecurityPermission \"getPolicy\";\n");
						dos2.writeBytes("permission " + "java.security.SecurityPermission \"setProperty.policy.url.1\";\n");
					}
					dos2.writeBytes("};\n");
					dos2.flush();
					dos2.close();
				} catch (Throwable e) {
					e.printStackTrace();
				}
				//System.setProperty("java.security.policy", "=" + System.getProperty("user.home") + java.io.File.separator + "."+policyFile+"");
				java.security.Policy.getPolicy().refresh();
				//java.security.Security.setProperty("policy.url.1", "file:${java.home}/lib/security/"+policyFile+"");
				java.security.Security.setProperty("policy.url.1", "file:${user.home}/."+policyFile+"");

				sm = new SecurityManager();
				System.setSecurityManager(sm);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 *  
	 */
	private void setAllPermissions() {
		setTestPermission0("java.security.AllPermission", "", "", false);
	}
			
	/**
	 *  
	 */
	private void setNoPermissions() {
		//setTestPermission0("NothingPermissions", "", "", false);
		setTestPermission0("", "", "", true);
	}

}