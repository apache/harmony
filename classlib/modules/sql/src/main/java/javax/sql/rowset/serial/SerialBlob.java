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

package javax.sql.rowset.serial;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

import org.apache.harmony.sql.internal.nls.Messages;

public class SerialBlob implements Blob, Serializable, Cloneable {

    private static final long serialVersionUID = -8144641928112860441L;

    // required by serialized form
    private byte[] buf;

    // required by serialized form
    @SuppressWarnings("unused")
    private Blob blob;

    // required by serialized form
    private long len;

    // required by serialized form
    @SuppressWarnings("unused")
    private long origLen;

    /**
     * Constructs an instance by the given <code>blob</code>
     * 
     * @param blob
     *            the given blob
     * @throws SerialException
     *             if an error is encountered during serialization
     * @throws SQLException
     *             if <code>blob</code> is null
     */
    public SerialBlob(Blob blob) throws SerialException, SQLException {
        if (blob == null) {
            throw new SQLException(Messages.getString("sql.13")); //$NON-NLS-1$
        }
        this.blob = blob;
        buf = blob.getBytes(1, (int) blob.length());
        len = buf.length;
        origLen = len;
    }

    /**
     * Constructs an instance by the given <code>buf</code>
     * 
     * @param buf
     *            the given buffer
     * @throws SerialException
     *             if an error is encountered during serialization
     * @throws SQLException
     *             if a SQL error is encountered
     */
    public SerialBlob(byte[] buf) throws SerialException, SQLException {
        this.buf = new byte[buf.length];
        len = buf.length;
        origLen = len;
        System.arraycopy(buf, 0, this.buf, 0, (int) len);
    }

    /**
     * Returns an input stream of this SerialObject.
     * 
     * @throws SerialException
     *             if an error is encountered
     */
    public InputStream getBinaryStream() throws SerialException {
        return new ByteArrayInputStream(buf);
    }

    /**
     * Returns a copied array of this SerialObject, starting at the
     * <code> pos </code> with the given <code> length</code> number. If
     * <code> pos </code> + <code> length </code> - 1 is larger than the length
     * of this SerialObject array, the <code> length </code> will be shortened
     * to the length of array - <code>pos</code> + 1.
     * 
     * @param pos
     *            the starting position of the array to be copied.
     * @param length
     *            the total length of bytes to be copied
     * @throws SerialException
     *             if an error is encountered
     */
    public byte[] getBytes(long pos, int length) throws SerialException {
        if (pos < 1 || pos > len || length < 0) {
            throw new SerialException(Messages.getString("sql.14")); //$NON-NLS-1$
        }
        if (length > len - pos + 1) {
            length = (int) (len - pos + 1);
        }
        byte[] copiedArray = new byte[length];
        System.arraycopy(buf, (int) pos - 1, copiedArray, 0, length);
        return copiedArray;
    }

    /**
     * Gets the number of bytes in this SerialBlob object.
     * 
     * @return an long value with the length of the SerialBlob in bytes
     * @throws SerialException
     *             if an error is encoutnered
     */
    public long length() throws SerialException {
        return len;
    }

    /**
     * Search for the position in this Blob at which a specified pattern begins,
     * starting at a specified position within the Blob.
     * 
     * @param pattern
     *            a Blob containing the pattern of data to search for in this
     *            Blob
     * @param start
     *            the position within this Blob to start the search, where the
     *            first position in the Blob is 1
     * @return a long value with the position at which the pattern begins. -1 if
     *         the pattern is not found in this Blob.
     * @throws SQLException
     *             if an error occurs accessing the Blob
     * @throws SerialException
     *             if an error is encountered
     */
    public long position(Blob pattern, long start) throws SerialException,
            SQLException {
        byte[] patternBytes = pattern.getBytes(1, (int) pattern.length());
        return position(patternBytes, start);
    }

    /**
     * Search for the position in this Blob at which the specified pattern
     * begins, starting at a specified position within the Blob.
     * 
     * @param pattern
     *            a byte array containing the pattern of data to search for in
     *            this Blob
     * @param start
     *            the position within this Blob to start the search, where the
     *            first position in the Blob is 1
     * @return a long value with the position at which the pattern begins. -1 if
     *         the pattern is not found in this Blob.
     * @throws SerialException
     *             if an error is encountered
     * @throws SQLException
     *             if an error occurs accessing the Blob
     */
    public long position(byte[] pattern, long start) throws SerialException,
            SQLException {
        if (start < 1 || len - (start - 1) < pattern.length) {
            return -1;
        }

        for (int i = (int) (start - 1); i <= (len - pattern.length); ++i) {
            if (match(buf, i, pattern)) {
                return i + 1;
            }
        }
        return -1;
    }

    /*
     * Returns true if the bytes array contains exactly the same elements from
     * start position to start + subBytes.length as subBytes. Otherwise returns
     * false.
     */
    private boolean match(byte[] bytes, int start, byte[] subBytes) {
        for (int i = 0; i < subBytes.length;) {
            if (bytes[start++] != subBytes[i++]) {
                return false;
            }
        }
        return true;
    }

    public OutputStream setBinaryStream(long pos) throws SerialException,
            SQLException {
        if (blob == null) {
            throw new SerialException(Messages.getString("sql.18")); //$NON-NLS-1$
        }
        OutputStream os = blob.setBinaryStream(pos);
        if (os == null) {
            throw new SerialException(Messages.getString("sql.18")); //$NON-NLS-1$
        }
        return os;
    }

    public int setBytes(long pos, byte[] theBytes) throws SerialException,
            SQLException {
        return setBytes(pos, theBytes, 0, theBytes.length);
    }

    public int setBytes(long pos, byte[] theBytes, int offset, int length)
            throws SQLException {
        if (pos < 1 || length < 0 || pos > (len - length + 1)) {
            throw new SerialException(Messages.getString("sql.15")); // $NON-NLS-1$
        }
        if (offset < 0 || length < 0 || offset > (theBytes.length - length)) {
            throw new SerialException(Messages.getString("sql.16")); // $NON-NLS-1$
        }
        System.arraycopy(theBytes, offset, buf, (int) pos - 1, length);
        return length;
    }

    public void truncate(long length) throws SerialException {
        if (length > this.len) {
            throw new SerialException(Messages.getString("sql.17")); //$NON-NLS-1$
        }
        buf = getBytes(1, (int) length);
        len = length;
    }

}
