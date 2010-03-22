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
package org.apache.harmony.rmi.transport;


/**
 * Constants for RMI Transport Protocol.
 *
 * @author  Mikhail A. Markov
 */
public interface RMIProtocolConstants {

    /** Indicates sequence for beginning of header */
    public static final int RMI_HEADER = 0x4a524d49;

    /** Indicates sequence for beginning of header */
    public static final int HTTP_HEADER = 0x504f5354;

    /** Indicates version of RMI Transport Protocol */
    public static final short PROTOCOL_VER = 0x02;


    /*
     * -------------------------------------------------------------------------
     * Group of RMI protocols
     * -------------------------------------------------------------------------
     */

    /** Indicates StreamProtocol */
    public static final byte STREAM_PROTOCOL = 0x4b;

    /** Indicates SingleOpProtocol */
    public static final byte SINGLEOP_PROTOCOL = 0x4c;

    /** Indicates MultiplexProtocol */
    public static final byte MULTIPLEX_PROTOCOL = 0x4d;


    /*
     * -------------------------------------------------------------------------
     * Group of possible responses to protocol agreement messages
     * -------------------------------------------------------------------------
     */

    /** Indicates protocol accepted response (ProtocolAck) */
    public static final byte PROTOCOL_ACK = 0x4e;

    /** Indicates protocol not supported response (ProtocolNotSupported) */
    public static final byte PROTOCOL_NOT_SUPPORTED = 0x4f;


    /*
     * -------------------------------------------------------------------------
     * Group of possible messages types
     * -------------------------------------------------------------------------
     */

    /** Indicates method invocation (Call message) */
    public static final byte CALL_MSG = 0x50;

    /** Indicates testing liveness of a remote VM (Ping message) */
    public static final byte PING_MSG = 0x52;

    /**
     * Indicates, that remote objects have been received by client in a return
     * value from server (DgcAck message)
     */
    public static final byte DGCACK_MSG = 0x54;


    /*
     * -------------------------------------------------------------------------
     * Group of possible responses to messages
     * -------------------------------------------------------------------------
     */

    /**
     * Indicates the result of a RMI call completion in response to Call message
     */
    public static final byte CALL_OK = 0x51;

    /** Indicates that server is alive in response to Ping message */
    public static final byte PING_ACK = 0x53;


    /*
     * -------------------------------------------------------------------------
     * Group of possible results of remote method invocation (after CALL_OK msg)
     * -------------------------------------------------------------------------
     */

    /** Indicates that value is returned as a result of a RMI call */
    public static final byte RETURN_VAL = 0x01;

    /**
     * Indicates that exception (not communication-related) is thrown
     * as a result of a RMI call
     */
    public static final byte RETURN_EX = 0x02;
}
