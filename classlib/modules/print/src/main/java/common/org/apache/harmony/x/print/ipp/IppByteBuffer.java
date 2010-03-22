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
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print.ipp;

import java.nio.ByteBuffer;

public class IppByteBuffer {
    byte[] buf;
    ByteBuffer bbuf;
    int count = 0;

    public IppByteBuffer() {
        buf = new byte[512];
        bbuf = ByteBuffer.wrap(buf);
    }

    public IppByteBuffer(byte[] rb) {
        buf = new byte[rb.length];
        bbuf = ByteBuffer.wrap(buf);

        System.arraycopy(rb, 0, buf, 0, rb.length);
        count = rb.length;
    }

    protected int addcapacity(int add) {
        byte[] newbuf = new byte[buf.length + ((add / 512) + 1) * 512];
        ByteBuffer newbbuf = ByteBuffer.wrap(newbuf);

        System.arraycopy(buf, 0, newbuf, 0, buf.length);
        buf = newbuf;
        bbuf = newbbuf;

        return buf.length;
    }

    public byte[] put(int index, byte value) {
        if (index >= buf.length) {
            addcapacity(index + 1 - buf.length);
        }
        //bbuf.put(index, value);
        buf[index] = value;
        count = (index + 1) > count ? index + 1 : count;

        return buf;
    }

    public byte get(int index) {
        return bbuf.get(index);
    }

    public byte[] put(int index, short value) {
        if ((index + 2) > buf.length) {
            addcapacity(index + 2 - buf.length);
        }
        bbuf.putShort(index, value);
        count = (index + 2) > count ? index + 2 : count;

        return buf;
    }

    public short getShort(int index) {
        return bbuf.getShort(index);
    }

    public byte[] put(int index, int value) {
        if ((index + 4) > buf.length) {
            addcapacity(index + 4 - buf.length);
        }
        bbuf.putInt(index, value);
        count = (index + 4) > count ? index + 4 : count;

        return buf;
    }

    public int getInt(int index) {
        return bbuf.getInt(index);
    }

    public byte[] put(int index, byte[] value) {
        if ((index + value.length) > buf.length) {
            addcapacity(index + value.length - buf.length);
        }
        System.arraycopy(value, 0, buf, index, value.length);
        count = (index + value.length) > count ? index + value.length : count;

        return buf;
    }

    public byte[] get(int index, int length) {
        byte[] bb = new byte[length];

        System.arraycopy(buf, index, bb, 0, length);
        return bb;
    }

    public byte[] getBytes() {
        return get(0, count);
    }

    public byte[] getBuf() {
        return buf;
    }

}