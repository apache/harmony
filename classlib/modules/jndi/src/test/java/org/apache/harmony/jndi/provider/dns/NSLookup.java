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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Alexei Y. Zakharov
 */

package org.apache.harmony.jndi.provider.dns;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * Simple NS lookup utility.
 * @author Alexei Zakharov
 */
public class NSLookup {
    
    // add real IP of your DNS server here
    public static final String DNS_SERVER = "127.0.0.1"; 

    /**
     * Empty constructor
     */
    public NSLookup() {}

    /**
     * Main method.
     * @param args the hostname we want to get information for should be given
     *  as an argument 
     */
    public static void main(String[] args) throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        DirContext ctx = null;
        Attributes attrs = null;
        String[] attrNames = new String[1];
        NamingEnumeration<?> attrEnum;

        if (args.length == 0) {
            System.out.println("USAGE: java " + 
                    "org.apache.harmony.jndi.provider.dns.NSLookup <hostname>");
            return;
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.harmony.jndi.provider.dns.DNSContextFactory");
        env.put(Context.PROVIDER_URL, "dns://" + DNS_SERVER + "/.");
        env.put(Context.AUTHORITATIVE, "false");
        env.put(DNSContext.RECURSION, "true");
        ctx = new InitialDirContext(env);
        attrNames[0] = "A";
        attrs = ctx.getAttributes(args[0], attrNames);
        attrEnum = attrs.getAll();
        while (attrEnum.hasMoreElements()) {
            Attribute attr = (Attribute) attrEnum.nextElement();
            NamingEnumeration<?> vals = attr.getAll();

            while (vals.hasMoreElements()) {
                System.out.println(attr.getID() + " " + vals.nextElement());
            }
        }
    }
}
