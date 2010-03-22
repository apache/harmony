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


package org.apache.harmony.security.tests.provider.crypto.serialization;


import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


import java.security.Security;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.Serializable;


/**
 * Tests against SecureRandom with SHA1PRNG serialization
 */


public class SHA1PRNG_SecureRandomTest extends TestCase {


    private static final int LIMIT1 = 100;   // constant value limiting loop
    private static final int LIMIT2 = 50;    //

    private static final int CASES = 4;
 
    static String algorithm = "SHA1PRNG";    // name of algorithm
    static String provider  = "Crypto";     // name of provider

    private int testcase = 0;

    private byte zero[] = new byte[0];

    private int flag;
    private static final int SELF   = 0;
    private static final int GOLDEN = 1;


    /**
     * @return array of SecureRandom objects to be deserialized in tests.
     */
    protected Object[] getData() {

        SecureRandom sr;
        Object[] data = new Object[5];
      

        for ( int i = 0; i < data.length ; i++ ) {
            try {
                sr = SecureRandom.getInstance(algorithm, provider);

                switch (i) {

                  case 0 : break;

                  case 1 : sr.setSeed( zero );
                           break;

                  case 2 : sr.setSeed( new byte[] { (byte)1 } );
                           break;

                  case 3 : sr.nextBytes( zero );
                           break;

                  case 4 : sr.nextBytes( new byte[1] );
                           break;
                }
                data[i] = sr;
 
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("ATTENTION: " + e);
            } catch (NoSuchProviderException e) {
                throw new RuntimeException("ATTENTION: " + e);
            }
        }
        return data;
    };


    /**
     * Comparing sequencies of bytes 
     * returned by "nextBytes(..)" of referenced and tested objects
     */
    private void testingSame(SecureRandom ref, SecureRandom test) {

        byte refBytes[]  = null;
        byte testBytes[] = null;

        for ( int k = 0; k < LIMIT2; k++ ) {

            for ( int i = 0; i < LIMIT1; i++ ) {

                refBytes  = new byte[i];
                testBytes = new byte[i];

                ref.nextBytes(refBytes);
                test.nextBytes(testBytes);

                for (int j = 0; j < refBytes.length; j++ ) {
                    assertTrue("NOT same:  testcase =" + testcase +
                               " k=" +k + " i=" +i + " j=" +j +
                               " refBytes[j]=" + refBytes[j] + " testBytes[j]=" + testBytes[j],
                                refBytes[j] == testBytes[j] );
                }
            }
            ref.setSeed(refBytes);
            test.setSeed(testBytes);
        }
    }


    /**
     * Comparing sequencies of bytes 
     * returned by "nextBytes(..)" of referenced and tested objects
     */
    private void testingNotSame(SecureRandom ref, SecureRandom test) {

        byte refTotalBytes[]  = new byte[(LIMIT1*LIMIT1)/2];
        byte testTotalBytes[] = new byte[(LIMIT1*LIMIT1)/2];

        for ( int k = 0; k < LIMIT2; k++ ) {

            byte refBytes[]  = null;
            byte testBytes[] = null;

            int n = 0;

            for ( int i = 0; i < LIMIT1; i++ ) {

                refBytes  = new byte[i];
                testBytes = new byte[i];

                ref.nextBytes(refBytes);
                test.nextBytes(testBytes);

                System.arraycopy(refBytes,  0, refTotalBytes,  n, refBytes.length);
                System.arraycopy(testBytes, 0, testTotalBytes, n, testBytes.length);

                n += i;
            }

            boolean b = true;
            int j = 0;
            for ( int n1 = 0 ; n1 <= n ; n1++) {

                b &= refTotalBytes[n1] == testTotalBytes[n1];

                if ( j >= 20 || n1 == n ) {
                    assertFalse("the same sequencies :: testcase=" + testcase + 
                                " k=" +k + "n1 =" + n1, b);
                    b = true;
                    j = 0;
                }
                j++;
            }

            ref.setSeed(refBytes);
            test.setSeed(testBytes);
        }
    }


    private SerializableAssert comparator = new SerializableAssert(){

        /**
         * Tests that data objects can be serialized and deserialized without exceptions 
         * and the deserialization produces object of the same class.
         */
        public void assertDeserialized(Serializable reference, Serializable test) {

            SecureRandom ref = (SecureRandom) reference;
            SecureRandom tst = (SecureRandom) test;

            boolean b;
            byte seed[] = new byte[]{ 0 };

            switch ( testcase ) {

              // non-initialized object

              case 0 : // testing setSeed(..)
                       ref.setSeed(zero);
                       tst.setSeed(zero);
                       testingSame(ref, tst);
                       break;

              case 5 : // testing setSeed(..)
                       ref.setSeed(seed);
                       tst.setSeed(seed);
                       testingSame(ref, tst);
                       break;

              case 10 : // testing nextBytes(..)
                        ref.nextBytes(zero);
                        tst.nextBytes(zero);
                        testingNotSame(ref, tst);
                        break;

              case 15 : // testing nextBytes(..)
                        ref.nextBytes(seed);
                        tst.nextBytes(seed);
                        testingNotSame(ref, tst);
                        break;

              // object initialized with setSeed(zero)

              case 1 : // testing setSeed(..)
                       ref.setSeed(zero);
                       tst.setSeed(zero);
                       testingSame(ref, tst);
                       break;

              case 6 : // testing setSeed(..)
                       ref.setSeed(seed);
                       tst.setSeed(seed);
                       testingSame(ref, tst);
                       break;

              case 11 : // testing nextBytes(..)
                        ref.nextBytes(zero);
                        tst.nextBytes(zero);
                        testingSame(ref, tst);
                        break;

              case 16 : // testing nextBytes(..)
                        ref.nextBytes(seed);
                        tst.nextBytes(seed);
                        testingSame(ref, tst);
                        break;

              // object initialized with setSeed(seed)

              case 2 : // testing setSeed(..)
                       ref.setSeed(zero);
                       tst.setSeed(zero);
                       testingSame(ref, tst);
                       break;

              case 7 : // testing setSeed(..)
                       ref.setSeed(seed);
                       tst.setSeed(seed);
                       testingSame(ref, tst);
                       break;

              case 12 : // testing nextBytes(..)
                        ref.nextBytes(zero);
                        tst.nextBytes(zero);
                        testingSame(ref, tst);
                        break;

              case 17 : // testing nextBytes(..)
                        ref.nextBytes(seed);
                        tst.nextBytes(seed);
                        testingSame(ref, tst);
                        break;

              // object initialized with nextBytes(zero)

              case 3 : // testing setSeed(..)
                       ref.setSeed(zero);
                       tst.setSeed(zero);
                       if ( flag == SELF ) {
                           testingSame(ref, tst);
                       } else {
                           testingNotSame(ref, tst);
                       }
                       break;

              case 8 : // testing setSeed(..)
                       ref.setSeed(seed);
                       tst.setSeed(seed);
                       if ( flag == SELF ) {
                           testingSame(ref, tst);
                       } else {
                           testingNotSame(ref, tst);
                       }
                       break;

              case 13 : // testing nextBytes(..)
                        ref.nextBytes(zero);
                        tst.nextBytes(zero);
                        if ( flag == SELF ) {
                            testingSame(ref, tst);
                        } else {
                            testingNotSame(ref, tst);
                        }
                        break;

              case 18 : // testing nextBytes(..)
                        ref.nextBytes(seed);
                        tst.nextBytes(seed);
                        if ( flag == SELF ) {
                            testingSame(ref, tst);
                        } else {
                            testingNotSame(ref, tst);
                        }
                        break;

              // object initialized with nextBytes(seed)

              case 4 : // testing setSeed(..)
                       ref.setSeed(zero);
                       tst.setSeed(zero);
                       if ( flag == SELF ) {
                           testingSame(ref, tst);
                       } else {
                           testingNotSame(ref, tst);
                       }
                       break;

              case 9 : // testing setSeed(..)
                       ref.setSeed(seed);
                       tst.setSeed(seed);
                       if ( flag == SELF ) {
                       testingSame(ref, tst);
                       } else {
                           testingNotSame(ref, tst);
                       }
                       break;

              case 14 : // testing nextBytes(..)
                        ref.nextBytes(zero);
                        tst.nextBytes(zero);
                        if ( flag == SELF ) {
                            testingSame(ref, tst);
                        } else {
                            testingNotSame(ref, tst);
                        }
                        break;

               case 19 : // testing nextBytes(..)
                         ref.nextBytes(seed);
                         tst.nextBytes(seed);
                         if ( flag == SELF ) {
                             testingSame(ref, tst);
                         } else {
                             testingNotSame(ref, tst);
                         }
                         break;

               default: fail("ATTENTION: default case is not expected to happen");
            }
            testcase++;
        }
    };


    /**
     * Testing deserialized object.
     */
    public void testSerializationSelf() throws Exception {

        Object[] data;

        flag = SELF;
        for ( int i = 0; i < CASES; i++ ) {

            data = getData();
            SerializationTest.verifySelf(data, comparator);
        }
    }


    /**
     * Testing that SecureRandom with SHA1PRNG objects can be deserialized from golden files.
     */
    public void testSerializationCompartibility() throws Exception {

        Object[] data;

        flag = GOLDEN;
        for ( int i = 0; i < CASES; i++ ) {

            data = getData();
            SerializationTest.verifyGolden(this, data, comparator);
        }
    }


    public static Test suite() {
        return new TestSuite(SHA1PRNG_SecureRandomTest.class);
    }

    public static void main(String[] args)  {
        junit.textui.TestRunner.run(suite());
    }

}
