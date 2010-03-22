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
 * This class represents JDWP command packet.
 */
public class CommandPacket extends Packet {
    private final int COMMAND_SET_INDEX = 9;
    private final int COMMAND_INDEX     = 10;

    private byte      command_set;
    private byte      command;

    /**
     * Creates an empty CommandPacket with empty header and no data.
     */
    public CommandPacket() {
        super();
    }

    /**
     * Creates an empty CommandPacket for specific JDWP command with no data.
     */
    public CommandPacket(byte commandSet, byte command) {
        super();
        this.command_set = commandSet;
        this.command = command;
    }

    /**
     * Creates CommandPacket from given array of bytes including header and data sections.
     * 
     * @param bytes_array the JDWP packet, given as array of bytes.
     */
    public CommandPacket(byte bytes_array[]) {
        super(bytes_array);
        command_set = bytes_array[COMMAND_SET_INDEX];
        command = bytes_array[COMMAND_INDEX];
    }

    /**
     * Sets command set value of the header of the CommandPacket as byte.
     * 
     * @param val the command set.
     */
    public void setCommandSet(byte val) {
        command_set = val;
    }

    /**
     * Gets command set value of the header of the CommandPacket as byte.
     * 
     * @return the command set value of the header of the CommandPacket as byte.
     */
    public byte getCommandSet() {
        return command_set;
    }

    /**
     * Sets command value of the header of the CommandPacket as byte.
     * 
     * @param val the command.
     */
    public void setCommand(byte val) {
        command = val;
    }

    /**
     * Sets command value of the header of the CommandPacket as byte.
     * 
     * @param commandSet number of the command set.
     * @param command number of the command.
     */
    public void setCommand(byte commandSet, byte command) {
        this.command_set = commandSet;
        this.command = command;
    }

    /**
     * Gets command value of the header of the CommandPacket as byte.
     * 
     * @return the command value of the header of the CommandPacket as byte.
     */
    public byte getCommand() {
        return command;
    }

    /**
     * Gets the representation of the CommandPacket as array of bytes in the
     * JDWP format including header and data sections.
     * 
     * @return the representation of the CommandPacket as array of bytes in the
     *         JDWP format.
     */
    public byte[] toBytesArray() {
        byte res[] = super.toBytesArray();
        res[COMMAND_SET_INDEX] = command_set;
        res[COMMAND_INDEX] = command;
        return res;
    }

}