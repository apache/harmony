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


package org.apache.harmony.security.provider.crypto;

import java.security.ProviderException;
import java.security.AccessController;

import org.apache.harmony.security.internal.nls.Messages;


/**
 * The static class providing access on Windows platform
 * to system means for generating true random bits. <BR>
 *
 * It uses a native method to get the random bits from CryptGenRandom.
 * If the required library is not installed 
 * the provider shouldn't register the algorithm.
 */

public class RandomBitsSupplier implements SHA1_Data {

    /**
     * specification for native library
     */
    private static native boolean getWindowsRandom(byte[] bytes, int numBytes);

    /**
     * static field is "true" only if native library is linked
     */
    private static boolean serviceAvailable;


    static {
        try {
            AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public Object run() throws UnsatisfiedLinkError {
                        System.loadLibrary(LIBRARY_NAME); 
                        return null;
                    }
                }
            );
        } catch (UnsatisfiedLinkError e) {
            serviceAvailable = false;
        }
        serviceAvailable = true;
    }


    /**
     * The method is called by provider to determine if a device is available.
     */
    static boolean isServiceAvailable() {
        return serviceAvailable;
    }


    /**
     * The method returns byte array containing random bits.
     *
     * @param
     *       numBytes - length of bytes requested
     * @return
     *       byte array
     * @throws
     *       InvalidArgumentException - if numBytes <= 0         <BR>
     *       ProviderException - if some problem related to native library is discovered <BR>
     */
    public static synchronized byte[] getRandomBits(int numBytes) {

        if ( numBytes <= 0 ) {
            throw new IllegalArgumentException(Messages.getString("security.195", numBytes)); //$NON-NLS-1$
        }

        if ( !serviceAvailable ) {
            throw new ProviderException(
                Messages.getString("security.197") ); //$NON-NLS-1$
        }

        byte[] myBytes = new byte[numBytes];

        if ( !getWindowsRandom(myBytes, numBytes) ) {

            // it is unexpected result
            throw new ProviderException(
                Messages.getString("security.198") ); //$NON-NLS-1$
        }

        return myBytes;
    }
}
