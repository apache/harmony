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
 * @author Salikh Zakirov
 */  

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/**
 * @keyword XXX_test_harness
 */
public class Check implements Runnable {

    final static String FS = File.separator;
    static PrintStream stdout = System.out;
    static boolean all_passed, all_completed, status_printed;

    public static void main (String[] args) {

        // construct the list of tests
        Collection tests;
        if (args.length > 0) {
            // from command-line arguments
            tests = new ArrayList();
            for (int i = 0; i < args.length; i++) tests.add(args[i]);
        } else {
            // find recursively
            tests = findTests(new File(baseDir(), "tests" + FS + "smoke"));
        }

        // register a shutdown hook to print test result
        Runtime.getRuntime().addShutdownHook(new ExitShutdownHook());

        // compute overall status
        status_printed = false;
        all_passed = true;
        all_completed = false;
        Iterator i = tests.iterator();
        while (i.hasNext()) {
            String test = (String) i.next();
            all_passed = all_passed && runTest(test);
        }
        all_completed = true;
    }

    static class ExitShutdownHook extends Thread {
        public void run() {
            if (Check.all_passed && Check.all_completed) {
                Check.stdout.println("OK");
            } else {
                Check.stdout.println("CHECK FAILED");
            }
        }
    }

    // instance variables, to track a single test
    Thread checkerThread;
    ByteArrayOutputStream dump;
    InputStream in;
    boolean passed, failed;

    public Check (InputStream in) {
        this.in = in;
        dump = new ByteArrayOutputStream();
        passed = false;
        failed = false;
    }

    /// used to check test output.
    public void run() {
        checkerThread = Thread.currentThread();
        Pattern pass_pattern = Pattern.compile("pass", Pattern.CASE_INSENSITIVE);
        Pattern fail_pattern = Pattern.compile("fail", Pattern.CASE_INSENSITIVE);
        try {
            BufferedReader inr = new BufferedReader(new InputStreamReader(in));
            for (String s = inr.readLine(); null != s; s = inr.readLine()) {
                passed = passed || pass_pattern.matcher(s).find();
                failed = failed || fail_pattern.matcher(s).find();
                dump.write(s.getBytes());
                dump.write(10);
            }
        } catch (Throwable e) {
            stdout.println("> checker caught exception, " + e);
            failed = true;
        } finally {
            try { if (null != in) in.close(); } catch (IOException e) {}
        }
    }

    public boolean passed() {
        if (null != checkerThread)
            try { checkerThread.join(); } catch (InterruptedException e) {}
        return passed && !failed;
    }

    public void dump(PrintStream out) {
        out.println(dump);
    }

    public static boolean runTest (String test) {
        trace("  ... " + test);
        try {
            Class test_class = Class.forName(test);
            Method main = test_class.getMethod("main", 
                new Class[] { String[].class });
            PipedInputStream in = new PipedInputStream();
            PrintStream out = new PrintStream(new PipedOutputStream(in));
            System.setOut(out);
            Check checker = new Check(in);
            Thread checker_thread = new Thread(checker);
            checker_thread.setDaemon(true);
            checker_thread.start();
            main.invoke(null, new Object[] { new String[] {} });
            out.close();
            if (checker.passed()) {
                stdout.println(test + " passed");
            } else {
                stdout.println("---v---8< " + test + " output >8---v---");
                checker.dump(stdout);
                stdout.println("---^---8< " + test + " output >8---^---");
                stdout.println(test + " failed");
                return false;
            }
        } catch (Throwable e) {
            stdout.println(test + " failed, " + e);
            return false;
        }
        return true;
    }

    public static String classname (File file) {
        String absname = file.getPath();
        final String key = "tests" + FS + "smoke" + FS;
        int i = absname.indexOf(key) + key.length();
        int j = absname.lastIndexOf(".java");
        StringBuffer name = new StringBuffer(absname.substring(i,j));
        for (i = name.indexOf(FS); i>0; i = name.indexOf(FS,i+1)) {
            name.setCharAt(i, '.');
        }
        return name.toString();
    }

    public static String excludeKeywords () {
        String s;
        s = System.getProperty("EXCLUDE_KEYWORDS");
        final String predefined_excludes = "XXX|X_ia32|X_drl|X_Windows|X_group|golden|ignore";
        if (null == s) {
            final String default_excludes = "slow";
            return "@keyword.*((?:" + default_excludes + "|" + predefined_excludes + ")\\w*)";
        }
        Pattern p = Pattern.compile("[\\s,+-./]+");
        Matcher m = p.matcher(s);
        return "@keyword.*((?:" + m.replaceAll("|") + "|" + predefined_excludes + ")\\w*)";
    }

    public static Collection findTests (File dir) {
        ArrayList result = new ArrayList();
        Collection files = findJavaFiles(dir);
        Pattern pattern = Pattern.compile(excludeKeywords());
        FILES: for (Iterator i = files.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            String name = classname(file);
            // skip myself
            if (Check.class.getName().equals(name)) {
                i.remove();
                continue;
            }
            FileInputStream fin = null;
            BufferedReader in = null;
            try {
                fin = new FileInputStream(file);
                in = new BufferedReader(new InputStreamReader(fin));
                for (String s = in.readLine(); null != s; s = in.readLine()) {
                    Matcher m = pattern.matcher(s);
                    if (m.find()) {
                        stdout.println(name + " skipped due to " + m.group(1));
                        i.remove();
                        continue FILES;
                    }
                }
            } catch (Throwable e) {
                stdout.println("WARNING, " + name + ": " + e);
                i.remove();
            } finally {
                try { if (null != fin) fin.close(); } catch (IOException e) {}
                try { if (null != in) in.close(); } catch (IOException e) {}
            }
            result.add(name);
        }
        return result;
    }

    public static Collection findJavaFiles (File dir) {
        ArrayList result = new ArrayList();

        // recurse to subdirectories
        File[] dirs = dir.listFiles(new FileFilter() {
            public boolean accept(File f) { return f.isDirectory(); }
        });
        for (int i = 0; i < dirs.length; i++) {
            result.addAll(findJavaFiles(dirs[i]));
        }

        // find .java files in this directory
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File f) { return f.getName().endsWith(".java"); }
        });
        for (int i = 0; i < files.length; i++) {
            result.add(files[i]);
        }

        return result;
    }

    public static File baseDir() {
        // 1. try PWD and its parents
        File pwd = new File(".");
        if (isBaseDir(pwd)) return pwd;
        File parent = new File(pwd, ".."); if (isBaseDir(parent)) return parent; 
        parent = new File(parent, ".."); if (isBaseDir(parent)) return parent; 
        parent = new File(parent, ".."); if (isBaseDir(parent)) return parent; 
        parent = new File(parent, ".."); if (isBaseDir(parent)) return parent; 
        parent = new File(parent, ".."); if (isBaseDir(parent)) return parent; 

        // 2. try CLASSPATH elements and their parents
        String classpath = System.getProperty("java.class.path");
        StringTokenizer tok = new StringTokenizer(classpath, File.pathSeparator);
        while (tok.hasMoreTokens()) {
            File cpdir = new File(tok.nextToken());
            if (isBaseDir(cpdir)) return cpdir;
            cpdir = new File(cpdir, ".."); if (isBaseDir(cpdir)) return cpdir;
            cpdir = new File(cpdir, ".."); if (isBaseDir(cpdir)) return cpdir;
            cpdir = new File(cpdir, ".."); if (isBaseDir(cpdir)) return cpdir;
            cpdir = new File(cpdir, ".."); if (isBaseDir(cpdir)) return cpdir;
        }
        throw new RuntimeException(
            "Cannot determine base vm workspace directory");
    }

    public static boolean isBaseDir(File dir) {
        // if dir is the root directory of vm workspace
        return dir.isDirectory()
            && (new File(dir,"src").isDirectory())
            && (new File(dir,"tests").isDirectory())
            && (new File(dir,"tests" + FS + "smoke").isDirectory());
    }

    public static void trace (Object o) {
        stdout.println(o);
        stdout.flush();
    }
}
