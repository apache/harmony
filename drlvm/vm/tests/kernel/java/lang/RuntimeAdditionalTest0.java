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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

import junit.framework.TestCase;

/*
 * Created on March 29, 2006
 *
 * This RuntimeAdditionalTest class is used to test the Core API Runtime class
 * 
 */

/**
 * ###############################################################################
 * ###############################################################################
 * TODO: 1.
 * ###############################################################################
 * ###############################################################################
 */

public class RuntimeAdditionalTest0 extends TestCase {
    public static String os;

    public static String cm = null;
    public static String javaStarter = "java";
    public static String catStarter = null;
    public static String treeStarter = null;
    public static String psStarter = null;
    public static String killStarter = null;
    static {
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1) {
            os = "Win";
            String pathList = System.getProperty("java.library.path");
            String[] paths = pathList.split(File.pathSeparator);
            int ind1;
            for (ind1 = 0; ind1 < paths.length; ind1++) {
                if (paths[ind1] == null) {
                    continue;
                }
                File asf = new java.io.File(paths[ind1] + File.separator
                        + "cmd.exe");
                if (asf.exists()) {
                    cm = paths[ind1] + File.separator + "cmd.exe";
                    break;
                }
                asf = new java.io.File(paths[ind1] + File.separator
                        + "cat.exe");
                if (asf.exists()) {
                    catStarter = paths[ind1] + File.separator + "cat.exe";
                    break;
                }
                asf = new java.io.File(paths[ind1] + File.separator
                        + "tree.com");
                if (asf.exists()) {
                    treeStarter = paths[ind1] + File.separator + "tree.com";
                    break;
                }
                asf = new java.io.File(paths[ind1] + File.separator
                        + "ps.exe");
                if (asf.exists()) {
                    psStarter = paths[ind1] + File.separator + "ps.exe";
                    break;
                }
                asf = new java.io.File(paths[ind1] + File.separator
                        + "kill.exe");
                if (asf.exists()) {
                    killStarter = paths[ind1] + File.separator + "kill.exe";
                    break;
                }
            }

            if (cm == null) {
                if (new java.io.File((cm = "C:\\WINNT\\system32\\cmd.exe"))
                        .exists()) {
                } else if (new java.io.File(
                        (cm = "C:\\WINDOWS\\system32\\cmd.exe")).exists()) {
                } else {
                    cm = "cmd.exe";
                    System.out
                            .println("### WARNING: cmd.exe hasn't been found! Please, set the path to cmd.exe via java.library.path property.");
                }
            }

            if (catStarter == null) {
                if (new java.io.File((catStarter = "C:\\WINNT\\system32\\cat.exe"))
                        .exists()) {
                } else if (new java.io.File(
                        (catStarter = "C:\\WINDOWS\\system32\\cat.exe")).exists()) {
                } else if (new java.io.File(
                        (catStarter = "C:\\CygWin\\bin\\cat.exe")).exists()) {
                } else {
                    cm = "cat.exe";
                    System.out
                            .println("### WARNING: cat.exe hasn't been found! Please, set the path to cat.exe via java.library.path property.");
                }
            }

            if (treeStarter == null) {
                if (new java.io.File((treeStarter = "C:\\WINNT\\system32\\tree.com"))
                        .exists()) {
                } else if (new java.io.File(
                        (treeStarter = "C:\\WINDOWS\\system32\\tree.com")).exists()) {
                } else {
                    treeStarter = "tree.com";
                    System.out
                            .println("### WARNING: tree.com hasn't been found! Please, set the path to tree.com via java.library.path property.");
                }
            }

            if (psStarter == null) {
                if (new java.io.File((psStarter = "C:\\WINNT\\system32\\ps.exe"))
                        .exists()) {
                } else if (new java.io.File(
                        (psStarter = "C:\\WINDOWS\\system32\\ps.exe")).exists()) {
                } else if (new java.io.File(
                        (psStarter = "C:\\CygWin\\bin\\ps.exe")).exists()) {
                } else {
                    psStarter = "ps.exe";
                    System.out
                            .println("### WARNING: ps.exe hasn't been found! Please, set the path to ps.exe via java.library.path property.");
                }
            }

            if (killStarter == null) {
                if (new java.io.File((killStarter = "C:\\WINNT\\system32\\kill.exe"))
                        .exists()) {
                } else if (new java.io.File(
                        (killStarter = "C:\\WINDOWS\\system32\\kill.exe")).exists()) {
                } else if (new java.io.File(
                        (killStarter = "C:\\CygWin\\bin\\kill.exe")).exists()) {
                } else {
                    killStarter = "kill.exe";
                    System.out
                            .println("### WARNING: kill.exe hasn't been found! Please, set the path to kill.exe via java.library.path property.");
                }
            }
			
			javaStarter = "java";
        } else if (System.getProperty("os.name").toLowerCase().indexOf("linux") != -1) {
            os = "Lin";
            cm = "/bin/sh";

            try {
                Process proc = null;
				try {
					proc = Runtime.getRuntime().exec(new String[]{"find", "/usr", "-name", "java"});
				} catch (java.io.IOException e) {
					proc = Runtime.getRuntime().exec(new String[]{"/usr/bin/find", "/usr", "-name", "java"});
				}
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
                    proc.getInputStream()));
		String rV;
				while((rV = br.readLine())!=null && !rV.endsWith("/jre/bin/java")) {
				}
				if(rV!=null) {
					javaStarter = "/usr/jrockit-j2sdk1.4.2_04/jre/bin/java";
					javaStarter = "/usr/lib/java/jre/bin/java";
					javaStarter = rV;
				} else {
					/*
					String pathList = System.getProperty("java.library.path");
					String[] paths = pathList.split(File.pathSeparator);
					int ind1;
					for (ind1 = 0; ind1 < paths.length; ind1++) {
						if (paths[ind1] == null) {
							continue;
						}
						File asf = new java.io.File(paths[ind1] + File.separator
								+ "java");
						if (asf.exists()) {
							javaStarter = paths[ind1] + File.separator + "java";
							break;
						}
					}
					*/
					javaStarter = "/usr/lib/java/jre/bin/java"; // last hope :)
				}
				System.out.println(javaStarter);
} catch (java.io.IOException e) {
                    System.out
                            .println("### WARNING: Some needed external java hasn't been found! A lot of RuntimeAdditionalTest* can fail due to this reason!");
                    //System.out
                    //        .println("### WARNING: java hasn't been found! You can set the path to java, for example, via java.library.path property.");
           }

try {
                Process proc = null;
				try {
					proc = Runtime.getRuntime().exec(new String[]{"find", "/usr", "-name", "tree"});
				} catch (java.io.IOException e) {
					proc = Runtime.getRuntime().exec(new String[]{"/usr/bin/find", "/usr", "-name", "tree"});
				}
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
                    proc.getInputStream()));
				String rV;
				while((rV = br.readLine())!=null && !rV.endsWith("/bin/tree")) {
				}
				if(rV!=null) {
					treeStarter = rV;
				} else {
					treeStarter = "/usr/bin/tree"; // last hope
				}
 				System.out.println(treeStarter);
           } catch (java.io.IOException e) {
                    System.out
                            .println("### WARNING: tree command hasn't been found! A lot of RuntimeAdditionalTest* can fail due to this reason!");
           }

            try {
                Process proc = null;
				try {
					proc = Runtime.getRuntime().exec(new String[]{"find", "/usr", "-name", "cat"});
				} catch (java.io.IOException e) {
					proc = Runtime.getRuntime().exec(new String[]{"/usr/bin/find", "/usr", "-name", "cat"});
				}
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
                    proc.getInputStream()));
				String rV;
				while((rV = br.readLine())!=null && !rV.endsWith("/bin/cat")) {
				}
				if(rV!=null) {
					catStarter = rV;
				} else {
					catStarter = "/bin/cat"; // last hope
				}
 				System.out.println(catStarter);
           } catch (java.io.IOException e) {
                    System.out
                            .println("### WARNING: tree command hasn't been found! A lot of RuntimeAdditionalTest* can fail due to this reason!");
           }

            try {
                Process proc = null;
				try {
					proc = Runtime.getRuntime().exec(new String[]{"find", "/usr", "-name", "ps"});
				} catch (java.io.IOException e) {
					proc = Runtime.getRuntime().exec(new String[]{"/usr/bin/find", "/usr", "-name", "ps"});
				}
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
                    proc.getInputStream()));
				String rV;
				while((rV = br.readLine())!=null && !rV.endsWith("/bin/ps")) {
				}
				if(rV!=null) {
					psStarter = rV;
				} else {
					psStarter = "/bin/ps"; // last hope
				}
					//psStarter = "/bin/ps"; // last hope
 				System.out.println(psStarter);
           } catch (java.io.IOException e) {
                    System.out
                            .println("### WARNING: tree command hasn't been found! A lot of RuntimeAdditionalTest* can fail due to this reason!");
           }

            try {
                Process proc = null;
				try {
					proc = Runtime.getRuntime().exec(new String[]{"find", "/usr", "-name", "kill"});
				} catch (java.io.IOException e) {
					proc = Runtime.getRuntime().exec(new String[]{"/usr/bin/find", "/usr", "-name", "kill"});
				}
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
                    proc.getInputStream()));
				String rV;
				while((rV = br.readLine())!=null && !rV.endsWith("/bin/kill")) {
				}
				if(rV!=null) {
					killStarter = rV;
				} else {
					killStarter = "/bin/kill"; // last hope
				}
 				System.out.println(killStarter);
           } catch (java.io.IOException e) {
                    System.out
                            .println("### WARNING: tree command hasn't been found! A lot of RuntimeAdditionalTest* can fail due to this reason!");
           }
		
        } else {
            os = "Unk";
        }

    }

    private static File seekFileInDirsHierarchic(File dirToFind,
            String extension, int size) {
        File af[] = dirToFind.listFiles();
        if (af == null)
            return null;
        File dirs[] = new File[af.length];
        File res = null;
        int j = 0;
        int i = 0;
        try {
            for (i = 0; i < af.length; i++) {
                if (af[i].isFile() && af[i].getName().endsWith(extension) && af[i].getName().indexOf(" ") == -1
                        && af[i].length() > size && (1.5 * size) > af[i].length()) {
                    return af[i];
                } else if (af[i].isDirectory()) {
                    dirs[j++] = af[i];
                }
            }
            for (i = 0; i < j; i++) {
                if ((res = seekFileInDirsHierarchic(dirs[i], extension, size)) != null) {
                    return res;
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String textFile;
    static int fileLength = 7000/(os.equals("Lin")?2:1); //10000; //50000;
    static {
        File f = null;
        if (os.equals("Win")) {
            if ((f = seekFileInDirsHierarchic(new File("C:\\Program Files"),
                    ".txt", fileLength)) != null) {
                textFile = f.getAbsolutePath();
            }
        } else if (os.equals("Lin")) {
            if ((f = seekFileInDirsHierarchic(new File("/opt"), ".txt", fileLength)) != null) {
                textFile = f.getAbsolutePath();
            }
            if (textFile == null) {
                if ((f = seekFileInDirsHierarchic(new File("/usr"), ".txt",
                        fileLength)) != null) {
                    textFile = f.getAbsolutePath();
                }
            }
        } else {
            textFile = null;
        }
        if (textFile == null)
            System.out
                    .println("### WARNING: *.txt file hasn't been found! Tests aren't able for correct run.");
        System.out.println(textFile);
    }

    public static String libFile;
    static {
        File f = null;
        if (os.equals("Win")) {
            if ((f = seekFileInDirsHierarchic(new File("C:\\Program Files"),
                    ".dll", fileLength)) != null) {
                libFile = f.getAbsolutePath();
            }
        } else if (os.equals("Lin")) {
            if ((f = seekFileInDirsHierarchic(new File("/opt"), ".so", fileLength)) != null) {
                libFile = f.getAbsolutePath();
            }
            if (libFile == null) {
                if ((f = seekFileInDirsHierarchic(new File("/usr"), ".so",
                        fileLength)) != null) {
                    libFile = f.getAbsolutePath();
                }
            }
            if (libFile == null) {
                if ((f = seekFileInDirsHierarchic(new File("/lib"), ".so",
                        fileLength)) != null) {
                    libFile = f.getAbsolutePath();
                }
            }
		} else {
            libFile = null;
        }
        if (libFile == null)
            System.out
                    .println("### WARNING: *.dll|so file hasn't been found! Tests aren't able for correct run.");
        System.out.println(libFile);
    }

    /*
     * static String exeFile; static { File f = null; if (os.equals("Win")) { if
     * ((f = seekFileInDirsHierarchic(new File("C:\\Program Files"), ".exe",
     * fileLength)) != null ) { exeFile = f.getAbsolutePath(); } } else if
     * (os.equals("Lin")){ if ((f = seekFileInDirsHierarchic(new File("/opt"),
     * ".exe", fileLength)) != null ) { exeFile = f.getAbsolutePath(); } if (exeFile ==
     * null) { if ((f = seekFileInDirsHierarchic(new File("/usr"), ".exe",
     * fileLength)) != null ) { exeFile = f.getAbsolutePath(); } } } else { exeFile =
     * null; } if (exeFile == null) System.out.println("### WARNING: *.exe file
     * hasn't been found! Tests aren't able for correct run.");
     * System.out.println(exeFile); }
     */

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * using the InputStream.read, OutputStream.write; chain of Process.waitFor,
     * Process.destroy, Process.waitFor, Process.exitValue
     */
    public void test_1() {
		System.out.println("==test_1===");
        String cmnd = null;
        if (os.equals("Win")) {
            cmnd = cm + " /C date";
        } else if (os.equals("Lin")) {
            cmnd = "sh -c \"sh -version\"";
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }

        if (os.equals("Win")) {
            try {
                Process pi3 = Runtime.getRuntime().exec(cmnd);
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
                java.io.OutputStream os = pi3.getOutputStream();
                pi3.getErrorStream();
                java.io.InputStream is = pi3.getInputStream();
                if (is.available() < 1) {
                    fail("ERROR (test_1): input stream is empty(1).");
                }
                int ia = is.available();
                byte[] bb = new byte[ia];
                is.read(bb);
                String r1 = new String(bb);
                if (r1.indexOf("The current date is") == -1
                        || r1.indexOf("Enter the new date") == -1) {
                    fail("ERROR (test_1): unexpected message(1).");
                }
                for (int ii = 0; ii < ia; ii++) {
                    bb[ii] = (byte) 0;
                }

                os.write('x');
                os.write('x');
                os.write('-');
                os.write('x');
                os.write('x');
                os.write('-');
                os.write('x');
                os.write('x');
                os.write('\n');
                os.flush();

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
                if (is.available() < 1) {
                    fail("ERROR (test_1): input stream is empty(2).");
                }
                ia = is.available();
                byte[] bbb = new byte[ia];
                is.read(bbb);
                r1 = new String(bbb);
                if (r1.indexOf("The system cannot accept the date entered") == -1
                        && r1.indexOf("Enter the new date") == -1) {
                    fail("ERROR (test_1): unexpected message(2).");
                }
                os.write('\n');
                try {
                    pi3.exitValue();
                } catch (IllegalThreadStateException e) {
                    os.flush();
                    try {
                        pi3.waitFor();
                    } catch (InterruptedException ee) {
                    }
                }
                /*System.out.println(*/pi3.waitFor()/*)*/;
                pi3.destroy();
                /*System.out.println(*/pi3.waitFor()/*)*/;
                /*System.out.println(*/pi3.exitValue()/*)*/;
            } catch (Exception eeee) {
                eeee.printStackTrace();
                fail("ERROR (test_1): unexpected exception.");
            }
        } else if (os.equals("Lin")) {
            try {
                //Process pi3 = Runtime.getRuntime().exec("sh -c \"sh
                // -version\"");
                Process pi3 = Runtime.getRuntime().exec("sh -c date");
                pi3.getOutputStream();
                pi3.getErrorStream();
                java.io.InputStream is = pi3.getInputStream();
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                if (is.available() < 1) {
                    fail("ERROR (test_1): input stream is empty(1).");
                }
                int ia = is.available();
                byte[] bb = new byte[ia];
                is.read(bb);
                String r1 = new String(bb);
                if (r1.indexOf("S") == -1 && r1.indexOf("M") == -1
                        && r1.indexOf("T") == -1 && r1.indexOf("W") == -1
                        && r1.indexOf("F") == -1) {
                    fail("ERROR (test_1): unexpected message(1)." + r1);
                }
                for (int ii = 0; ii < ia; ii++) {
                    bb[ii] = (byte) 0;
                }

                Process pi4 = Runtime.getRuntime().exec("sh -c sh");
                java.io.OutputStream os4 = pi4.getOutputStream();
                pi4.getErrorStream();
                pi4.getInputStream();

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                try {
                    pi4.exitValue();
                    fail("ERROR (test_1): process should exist.");
                } catch (IllegalThreadStateException e) {
                }
                os4.write('e');
                os4.write('x');
                os4.write('i');
                os4.write('t');
                os4.write('\n');
                os4.flush();
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                try {
                    pi4.exitValue();
                } catch (IllegalThreadStateException e) {
                    fail("ERROR (test_1): process should be destroyed.");
                }
                /*System.out.println(*/pi4.waitFor()/*)*/;
                pi4.destroy();
                /*System.out.println(*/pi4.waitFor()/*)*/;
                /*System.out.println(*/pi4.exitValue()/*)*/;
            } catch (Exception eeee) {
                eeee.printStackTrace();
                fail("ERROR (test_1): unexpected exception.");
            }
        }
    }
	////////////// Common additional service: ///////////////
	static java.io.DataOutputStream ps = null;
	static {
		try{
			//String str = ":>";
			File fff = null;
	        //fff = new java.io.File("C:\\IJE\\orp\\ZSS\\___ttttt"+System.getProperty("java.vm.name")+".txt");
	        //fff = new java.io.File("/nfs/ins/proj/drl/coreapi/ZSS/DRL_VM/___ttttt"+System.getProperty("java.vm.name")+".txt");
	        fff = new File(System.getProperty("java.io.tmpdir")+File.separator+"_messageOf_"+((System.getProperty("java.vm.name").indexOf("DRL")!=-1)?System.getProperty("java.vm.name"):"XXX")+"_"+System.getProperty("user.name")+".txt");
	        fff.createNewFile();
			ps = new java.io.DataOutputStream(new java.io.FileOutputStream(fff));
			//ps.writeBytes(System.getProperties().toString());
		    //new Throwable().printStackTrace(ps);
		}catch(Throwable e){
			//if(System.getProperty("java.vm.name").indexOf("DRL")!=-1){int i=0,j=0; i=i/j;}
		}
	}
		
	static void doMessage(String mess){
		//ps.println(mess);
		try {
			ps.writeBytes(mess);
		} catch (java.io.IOException  ee) {}
	}

	static void killCat() {
		try{
			//String cmnd1 = RuntimeAdditionalTest0.os.equals("Win")?RuntimeAdditionalTest0.cm + " /C ps -Ws":RuntimeAdditionalTest0.cm + " -c \"ps -ef\"";
			//String cmnd1 = RuntimeAdditionalTest0.os.equals("Win")?"C:\\CygWin\\bin\\ps -Ws":RuntimeAdditionalTest0.cm + " -c \"ps -ef\"";
			//String cmnd1 = RuntimeAdditionalTest0.psStarter+(RuntimeAdditionalTest0.os.equals("Win")?" -Ws":" -ef");
			/**/String cmnd1 = RuntimeAdditionalTest0.psStarter+(RuntimeAdditionalTest0.os.equals("Win")?" -Ws":" -U "+System.getProperty("user.name"));
			Process pi5 = Runtime.getRuntime().exec(cmnd1);
            BufferedReader br = new BufferedReader(new InputStreamReader(pi5
                    .getInputStream()));
            boolean flg = true;
			String procValue = null;
			while (!br.ready()) {}
            while ((procValue = br.readLine()) != null) {
RuntimeAdditionalTest0.doMessage("9:"+procValue+"\n");
				if (procValue.indexOf("cat") != -1 /*&& (RuntimeAdditionalTest0.os.equals("Win")?true:procValue.indexOf(System.getProperty("user.name")) != -1)*/) {
                    flg = false;
                    break;
                }
            }
            if (!flg) {
				String as[] = procValue.split(" ");
RuntimeAdditionalTest0.doMessage("XX:"+procValue+"\n");
				for (int i = 0; i < as.length; i++) {
					if(as[i].matches("\\d+")){
						//cmnd1 = RuntimeAdditionalTest0.os.equals("Win")?RuntimeAdditionalTest0.cm + " /C kill "+as[i]:RuntimeAdditionalTest0.cm + " -c \"kill "+as[i]+"\"";
						//cmnd1 = RuntimeAdditionalTest0.os.equals("Win")?"C:\\WINNT\\system32\\kill "+as[i]:RuntimeAdditionalTest0.cm + " -c \"kill "+as[i]+"\"";
						cmnd1 = RuntimeAdditionalTest0.killStarter+(RuntimeAdditionalTest0.os.equals("Win")?"":" -9")+" "+as[i];
RuntimeAdditionalTest0.doMessage("XXX:"+cmnd1+"\n");
						Runtime.getRuntime().exec(cmnd1);
						Thread.sleep(3000);
					}
				}
            }
		}catch(Exception e){
			fail("ERROR killCat: unexpected exception: "+e.toString());
		}
	}

	static void killTree() {
		try{
			//String cmnd1 = RuntimeAdditionalTest0.os.equals("Win")?RuntimeAdditionalTest0.cm + " /C ps -Ws":RuntimeAdditionalTest0.cm + " -c \"ps -ef\"";
			//String cmnd1 = RuntimeAdditionalTest0.psStarter+(RuntimeAdditionalTest0.os.equals("Win")?" -Ws":" -ef");
			/**/String cmnd1 = RuntimeAdditionalTest0.psStarter+(RuntimeAdditionalTest0.os.equals("Win")?" -Ws":" -U "+System.getProperty("user.name"));
			Process pi5 = Runtime.getRuntime().exec(cmnd1);
            BufferedReader br = new BufferedReader(new InputStreamReader(pi5
                    .getInputStream()));
            boolean flg = true;
			String procValue = null;
			while (!br.ready()) {}
            while ((procValue = br.readLine()) != null) {
                if (procValue.indexOf("tree") != -1 /*&& (RuntimeAdditionalTest0.os.equals("Win")?true:procValue.indexOf(System.getProperty("user.name")) != -1)*/) {
                    flg = false;
                    break;
                }
            }
            if (!flg) {
				String as[] = procValue.split(" ");
				for (int i = 0; i < as.length; i++) {
					if(as[i].matches("\\d+")){
						//cmnd1 = RuntimeAdditionalTest0.os.equals("Win")?RuntimeAdditionalTest0.cm + " /C kill "+as[i]:RuntimeAdditionalTest0.cm + " -c \"kill "+as[i]+"\"";
						cmnd1 = RuntimeAdditionalTest0.killStarter+(RuntimeAdditionalTest0.os.equals("Win")?"":" -9")+" "+as[i];
						Runtime.getRuntime().exec(cmnd1);
						Thread.sleep(3000);
					}
				}
            }
		}catch(Exception e){
			fail("ERROR killTree: unexpected exception: "+e.toString());
		}
	}
}
