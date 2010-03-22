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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class InitialContextAppTest extends TestCase {
    private static final Log log = new Log(InitialContextAppTest.class);

    public void testConstructor_App() throws NamingException, IOException {
        // Comment this test case out because this test case
        // needs complex configuration about jndi properties.

        // log.setMethod("testConstructor_App");
        // InitialContext context = new InitialContext();
        // Hashtable props = context.getEnvironment();
        // // printHashtable(props);
        // Hashtable expected = TestInitialContextLib.readAllProps(null);
        // assertEquals(expected, props);
    }

    /**
     * regression: Harmony-4942
     * 
     */
    public void testConstructor() throws Exception {
        final File file1 = new File("src/test/resources/test1");
        if (!file1.exists()) {
            file1.mkdir();
        }

        URL url = file1.toURL();
        URLClassLoader cltest1 = new URLClassLoader(new URL[] { url }, Thread
                .currentThread().getContextClassLoader());
        Thread test1 = new Thread(new Runnable() {

            public void run() {
                try {
                    File propsFile = new File(file1.toString()
                            + "/jndi.properties");

                    FileOutputStream fos = new FileOutputStream(propsFile);

                    PrintStream ps = new PrintStream(fos);
                    ps
                            .println("java.naming.factory.initial=org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory");
                    ps.println("java.naming.provider.url=http://test1");
                    ps.close();

                    InitialContext context = new InitialContext();
                    Hashtable<?, ?> env = context.getEnvironment();
                    assertEquals(
                            "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory",
                            env.get("java.naming.factory.initial"));
                    assertEquals("http://test1", env
                            .get("java.naming.provider.url"));

                    propsFile.delete();

                    // create new properties file with different values
                    fos = new FileOutputStream(propsFile);
                    ps = new PrintStream(fos);
                    ps
                            .println("java.naming.factory.initial=not.exist.ContextFactory");
                    ps.println("java.naming.provider.url=http://test1.new");
                    ps.close();

                    context = new InitialContext();
                    env = context.getEnvironment();
                    assertEquals(
                            "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory",
                            env.get("java.naming.factory.initial"));
                    assertEquals("http://test1", env
                            .get("java.naming.provider.url"));

                    propsFile.delete();
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }

        });
        // use different classloader
        test1.setContextClassLoader(cltest1);

        test1.start();

        final File file2 = new File("src/test/resources/test2");
        if (!file2.exists()) {
            file2.mkdir();
        }
        url = file2.toURL();
        URLClassLoader cltest2 = new URLClassLoader(new URL[] { url }, Thread
                .currentThread().getContextClassLoader());

        Thread test2 = new Thread(new Runnable() {

            public void run() {
                try {
                    File propsFile = new File(file2.toString()
                            + "/jndi.properties");
                    FileOutputStream fos = new FileOutputStream(propsFile);
                    PrintStream ps = new PrintStream(fos);
                    ps
                            .println("java.naming.factory.initial=org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory");
                    ps.println("java.naming.provider.url=http://test2");
                    ps.close();

                    InitialContext context = new InitialContext();
                    Hashtable<?, ?> env = context.getEnvironment();
                    assertEquals(
                            "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory",
                            env.get("java.naming.factory.initial"));
                    assertEquals("http://test2", env
                            .get("java.naming.provider.url"));

                    propsFile.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        // use different classloader
        test2.setContextClassLoader(cltest2);
        test2.start();

        Thread.sleep(1000);
        file1.deleteOnExit();
        file2.deleteOnExit();
    }

    void printHashtable(Hashtable<?, ?> env) {
        // TO DO: Need to remove
        Enumeration<?> keys = env.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            log.log(key + "=" + env.get(key));
        }
    }
}
