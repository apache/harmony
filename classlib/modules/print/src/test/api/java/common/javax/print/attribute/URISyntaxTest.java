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
 * @author Elena V. Sayapina 
 */ 

package javax.print.attribute;

import java.net.URI;

import junit.framework.TestCase;


public class URISyntaxTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(URISyntaxTest.class);
    }

    static {
        System.out.println("URISyntax testing...");
    }

    uriSyntax us1, us2;
    URI uri1, uri2;


    /*
     * URISyntax() constructor testing. 
     */
    public final void testURISyntax() throws Exception {
        uri1 = new URI("http://news.ngs.ru/more/14161.shtml");
        us1 = new uriSyntax(uri1);

        try {
            uri1 = null;
            us1 = new uriSyntax(uri1);
            fail("NullPointerException wasn't thrown when expected");
        } catch (NullPointerException e) {
        }

    }

    /*
     * hashCode() method testing. 
     */
    public final void testHashCode() throws Exception {
        uri1 = new URI("http://www.ietf.org/rfc/rfc2396.txt");
        us1 = new uriSyntax(uri1);
        assertTrue(us1.hashCode() == us1.hashCode());

        uri1 = new URI("http://www.ietf.org/rfc/rfc2396.txt");
        us1 = new uriSyntax(uri1);
        uri2 = new URI("http://www.ietf.org/rfc/rfc2395.txt");
        us2 = new uriSyntax(uri2);
        assertFalse(us1.hashCode() == us2.hashCode());
    }

    /*
     * equals(Object object) method testing. 
     */
    public final void testEqualsObject() throws Exception {
        uri1 = new URI("http://www.melodi.ru/main/index.php#");
        us1 = new uriSyntax(uri1);
        assertTrue(us1.equals(us1));

        uri1 = new URI("http://www.ietf.org/rfc/rfc2396.txt");
        us1 = new uriSyntax(uri1);
        uri2 = new URI("http://www.ietf.org/rfc/rfc2395.txt");
        us2 = new uriSyntax(uri2);
        assertFalse(us1.equals(us2));
    }

    /*
     * getURI() method testing. 
     */
    public final void testGetURI() throws Exception {
        uri1 = new URI("http://www.melodi.ru/main/index.php#");
        us1 = new uriSyntax(uri1);
        assertEquals(uri1, us1.getURI());

        uri2 = new URI("http://www.ietf.org/rfc/rfc2395.txt");
        us2 = new uriSyntax(uri2);
        assertEquals(uri2, us2.getURI());
    }


    /*
     * Auxiliary class
     */
    public class uriSyntax extends URISyntax {

        public uriSyntax(URI uri){
            super(uri);
        }
    }

}
