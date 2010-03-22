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
 * @author  Mikhail A. Markov
 */
package java.rmi.dgc;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.rmi.server.UID;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public final class VMID implements Serializable {
    private static final long serialVersionUID = -538642295484486218L;
    private byte[] addr;
    private UID uid;

    /*
     * If true then we successfully obtained host address and this VMID could
     * be treated as unique.
     */
    private static boolean unique = true;

    /*
     * This VM local address. If it was successfully obtained then isUnique()
     * method for VMIDs created in this VM will return true, otherwise addr
     * fields will be set to address 127.0.0.1 and isUnique() method will
     * return false.
     */
    private static byte[] localAddr = (byte[]) AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    try {
                        return InetAddress.getLocalHost().getAddress();
                    } catch (Exception ex) {
                        unique = false;
                        return new byte[] { 127, 0, 0, 1 };
                    }
                }
            });

    /**
     * @com.intel.drl.spec_ref
     */
    public VMID() {
        addr = localAddr;
        uid = new UID();
    }

    /**
     * @com.intel.drl.spec_ref
     * It returns hexadecimal representation of address with 2 positions for
     * each byte.
     */
    public String toString() {
        String str = "VMID["; //$NON-NLS-1$

        for (int i = 0; i < addr.length; ++i) {
            int ibyte = addr[i] & 255; // quick way to have a module of the byte

            str += ((ibyte < 16) ? "0" : "") + Integer.toString(ibyte, 16); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return str + ", " + uid + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof VMID)) {
            return false;
        }
        VMID vmid = (VMID) obj;
        return uid.equals(vmid.uid) && Arrays.equals(addr, vmid.addr);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public int hashCode() {
        return uid.hashCode();
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated VMID's are unique in overwhelming majority of cases 
     */
    @Deprecated
    public static boolean isUnique() {
        return unique;
    }

    private void readObject(ObjectInput in)
            throws IOException, ClassNotFoundException {
        addr = (byte []) in.readObject();
        uid = UID.read(in);
    }

    private void writeObject(ObjectOutput out) throws IOException {
        out.writeObject(addr);
        uid.write(out);
    }
}
