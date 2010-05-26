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

package tests.support;

import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;



public class Support_Excludes {

    static boolean isInitialized = false;
    static boolean verbose = false;
    static boolean ignoreExcludes = false;
    static final HashMap<String,String> excludes =
        new HashMap<String,String>();
    static final HashMap<String,String> archMap = new HashMap<String,String>();

    static {
        archMap.put("amd64", "x86_64");
        archMap.put("i386", "x86");
        archMap.put("i686", "x86");
        archMap.put("ppc", "ppc32");
        ignoreExcludes = System.getProperty("hy.excludes.ignore") != null;
        verbose = System.getProperty("hy.excludes.verbose") != null;
    }


    public static boolean isExcluded() {
        return isExcluded(null, 3);
    }

    public static boolean isExcluded(String tag) {
        return isExcluded(tag, 3);
    }

    public static boolean isRI() {
        return arch().equals("ri");
    }

    static String arch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (archMap.containsKey(arch)) {
            arch = archMap.get(arch);
        }
        return arch;
    }

    static String os() {
        String os = System.getProperty("os.name").toLowerCase();
        return os;
    }

    static String vm() {
        // allow the vm value to be overriden using:
        //   ant -Dhy.test.vmargs=-Dhy.excludes.vm=myvm test
        String vm = System.getProperty("hy.excludes.vm");
        if (vm != null) {
            return vm.toLowerCase();
        }
        String vendor = System.getProperty("java.vm.vendor").toLowerCase();
        if (vendor.startsWith("apache")) {
            return "drlvm";
        }
        if (vendor.startsWith("sun")) {
            return "ri";
        }
        if (vendor.startsWith("ibm")) {
            return "ibm";
            // String version = System.getProperty("java.vm.version");
            // return "ibm" + version;
        }
        return "unknown";
    }

    static StackTraceElement caller(int depth) {
        Throwable t = new Throwable();
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        return stack[depth];
    }

    static void load(String suffix) {
        String excludeFile = "exclude."+suffix;
        if (verbose) {
            System.err.println("loading: excludes/"+excludeFile);
        }
        try {
            BufferedReader reader =
                new BufferedReader(new FileReader("excludes/"+excludeFile));
            String line;
            while ((line = reader.readLine()) != null
                   && !line.startsWith('#')) {
                if (line.endsWith(".java")) {
                    line = line.substring(0,line.length()-5);
                } else if (line.endsWith(".class")) {
                    line = line.substring(0,line.length()-6);
                }
                line = line.replace('/', '.');
                excludes.put(line,excludeFile);
                if (verbose) {
                    System.err.println("excluding: "+line);
                }
            }
            reader.close();
        } catch (IOException e) {
            // ignore
        }
    }

    static void load() {
        isInitialized = true;
        load("common");
        load("interm");
        String platform = os()+"."+arch()+"."+vm();
        load(platform);
        load(platform + ".interm");
    }

    static boolean existsExclude(String key) {
        if (excludes.containsKey(key)) {
            if (verbose) {
                System.err.println("excluding, reason: "+key
                                   +"("+excludes.get(key)+")");
            }
            return true;
        }
        return false;
    }

    static boolean isExcluded(String tag, int depth) {
        if (ignoreExcludes) {
            return false;
        }
        if (!isInitialized) {
            load();
        }
        String suffix = "";
        if (tag != null) {
            if (existsExclude(tag)) {
                return true;
            }
            suffix = "+" + tag;
        }
        StackTraceElement context = caller(depth);
        return existsExclude(context.getClassName()+suffix)
            || existsExclude(context.getClassName()+"#"
                             +context.getMethodName()+suffix);
    }
}
