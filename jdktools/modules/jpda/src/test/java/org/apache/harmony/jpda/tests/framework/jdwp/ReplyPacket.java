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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Khen G. Kim
 */

/**
 * Created on 10.01.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

import org.apache.harmony.jpda.tests.framework.jdwp.Packet;

/**
 * This class represents JDWP reply packet.
 */
public class ReplyPacket extends Packet {

    private final int ERROR_CODE_INDEX = 9;

    private short error_code;

    /**
     * A default constructor that creates an empty ReplyPacket with empty header
     * and no data.
     */
    public ReplyPacket() {
        super();
    }

    /**
     * A constructor that creates ReplyPacket from array of bytes including
     * header and data sections.
     * 
     * @param p
     *            the JDWP packet, given as array of bytes.
     */
    public ReplyPacket(byte p[]) {
        super(p);
        error_code = (short) super.readFromByteArray(p, ERROR_CODE_INDEX,
                Packet.SHORT_SIZE);
    }

    /**
     * Sets the error code value of the header of the ReplyPacket as short.
     * 
     * @param val
     *            the error code.
     */
    public void setErrorCode(short val) {
        error_code = val;
    }

    /**
     * Gets the error code value of the header of the ReplyPacket.
     * 
     * @return the error code value of the header of the ReplyPacket.
     */
    public short getErrorCode() {
        return error_code;
    }

    /**
     * Gets the representation of the ReplyPacket as array of bytes in the JDWP
     * format including header and data sections.
     * 
     * @return the representation of the ReplyPacket as array of bytes in the
     *         JDWP format.
     */
    public byte[] toBytesArray() {
        byte res[] = super.toBytesArray();
        super.writeAtByteArray(error_code, res, ERROR_CODE_INDEX,
                Packet.SHORT_SIZE);
        return res;
    }
}
