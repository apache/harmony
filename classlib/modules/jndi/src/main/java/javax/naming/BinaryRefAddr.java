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

package javax.naming;

import java.util.Arrays;

/**
 * A <code>BinaryRefAddr</code> refers to an address which is represented by a
 * binary address.
 */
public class BinaryRefAddr extends RefAddr {

    private static final long serialVersionUID = -3415254970957330361L;

    /**
     * The buffer for the binary address itself.
     * 
     * @serial
     */
    private byte[] buf;

    /**
     * Constructs a <code>BinaryRefAddr</code> using the specified address
     * type and the full details of the supplied byte array.
     * 
     * @param type
     *            the address type which cannot be null
     * @param address
     *            the address itself which cannot be null
     */
    public BinaryRefAddr(String type, byte[] address) {
        this(type, address, 0, address.length);
    }

    /**
     * Constructs a <code>BinaryRefAddr</code> using the specified address
     * type and part of the supplied byte array. The number of bytes to be taken
     * is specified by <code>size</code>. Additionally these bytes are taken
     * from a starting point specified by <code>index</code>.
     * 
     * @param type
     *            the address type. It cannot be null.
     * @param address
     *            the address itself. It cannot be null.
     * @param index
     *            the starting point to copy bytes from. It must be greater than
     *            or equal to zero and must be less than or equal to the size of
     *            the byte array.
     * @param size
     *            the number of bytes to copy. It must be greater than or equal
     *            to zero and must be less than or equal to the size of the byte
     *            array less the starting position.
     * @throws ArrayIndexOutOfBoundsException
     *             If <code>size</code> or <code>index</code> does not meet
     *             the constraints.
     */
    public BinaryRefAddr(String type, byte[] address, int index, int size) {
        super(type);
        this.buf = new byte[size];
        System.arraycopy(address, index, this.buf, 0, size);
    }

    /**
     * Gets the content of this address.
     * 
     * @return an array of bytes containing the address. It cannot be null.
     */
    @Override
    public Object getContent() {
        return buf;
    }

    /**
     * Returns true if this address is equal to the supplied object
     * <code>o</code>. They are considered equal if the address types are
     * equal and the data in the buffers is of the same length and contains the
     * same bytes.
     * 
     * @param o
     *            the object to compare with
     * @return true if this address is equal to <code>o</code>, otherwise
     *         false
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof BinaryRefAddr) {
            BinaryRefAddr a = (BinaryRefAddr) o;
            return this.addrType.equals(a.addrType)
                    && Arrays.equals(this.buf, a.buf);
        }
        return false;
    }

    /**
     * Returns the hashcode of this address. The result is the hashcode of the
     * address type added to each byte from the data buffer.
     * 
     * @return the hashcode of this address
     */
    @Override
    public int hashCode() {
        int i = this.addrType.hashCode();

        for (byte element : this.buf) {
            i += element;
        }
        return i;
    }

    /**
     * Returns the string representation of this address. The string includes
     * the address type and a maximum of 128 bytes address content expressed in
     * hexadecimal form.
     * 
     * @return the string representation of this address
     */
    @Override
    public String toString() {
        String s = "The type of the address is: " //$NON-NLS-1$
                + this.addrType + "\nThe content of the address is: "; //$NON-NLS-1$
        int max = this.buf.length > 128 ? 128 : this.buf.length;

        for (int i = 0; i < max; i++) {
            s += Integer.toHexString(this.buf[i]) + " "; //$NON-NLS-1$
        }
        s = s.substring(0, s.length() - 1) + "\n"; //$NON-NLS-1$

        return s;
    }

}
