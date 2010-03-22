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
 * @author Khen G. Kim, Aleksey V. Yantsen
 */

/**
 * Created on 10.01.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

import java.io.UnsupportedEncodingException;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.TypesLengths;

/**
 * This base class represents JDWP packet.
 */
public class Packet {

    public static final int REPLY_PACKET_FLAG = 0x80;

    public static final int FLAGS_INDEX = 8;

    public static final int HEADER_SIZE = 11;

    /**
     * The size in bytes of the BYTE type value.
     */
    protected static final int BYTE_SIZE = 1;

    /**
     * The size in bytes of the SHORT type value.
     */
    protected static final int SHORT_SIZE = 2;

    /**
     * The size in bytes of the INT type value.
     */
    protected static final int INT_SIZE = 4;

    /**
     * The size in bytes of the LONG type value.
     */
    protected static final int LONG_SIZE = 8;

    private static final int LENGTH_INDEX = 0;

    private static final int ID_INDEX = 4;

    private int id;

    private byte flags;

    private int length;

    private byte data[];

    private int reading_data_index;

    /**
     * A constructor that creates an empty CommandPacket with empty header
     * fields and no data.
     */
    public Packet() {
        reading_data_index = 0;
        data = new byte[0];
    }

    /**
     * A constructor that creates Packet from array of bytes including header
     * and data sections.
     * 
     * @param p array of bytes for new packet.
     */
    public Packet(byte p[]) {
        length = (int) readFromByteArray(p, LENGTH_INDEX, INT_SIZE);
        if (length < HEADER_SIZE) {
            throw new TestErrorException(
                    "Packet creation error: size of packet = " + length
                            + "is less than header size = " + HEADER_SIZE);
        }
        id = (int) readFromByteArray(p, ID_INDEX, INT_SIZE);
        flags = p[FLAGS_INDEX];
        data = new byte[p.length - HEADER_SIZE];
        System.arraycopy(p, HEADER_SIZE, data, 0, p.length - HEADER_SIZE);
        reading_data_index = 0;
    }

    /**
     * Gets the length value of the header of the Packet.
     * 
     * @return the length value of the header of the Packet.
     */
    public int getLength() {
        return length;
    }

    /**
     * Sets the id value of the header of the Packet.
     * 
     * @param i
     *            the id value of the header of the Packet.
     */
    public void setId(int i) {
        id = i;
    }

    /**
     * Gets the id value of the header of the Packet.
     * 
     * @return the id value of the header of the Packet.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the flags value of the header of the Packet.
     * 
     * @param f
     *            the flags value of the header of the Packet.
     */
    public void setFlags(byte f) {
        flags = f;
    }

    /**
     * Gets the flags value of the header of the Packet.
     * 
     * @return the flags value of the header of the Packet.
     */
    public byte getFlags() {
        return flags;
    }

    /**
     * Gets the flags value from the header of the Packet.
     * 
     * @param tag
     *            Type tag (see JDWP.tag)
     * @return the flags value of the header of the Packet.
     */
    public boolean isValuePrimitiveType(byte tag) {
        switch (tag) {
        case JDWPConstants.Tag.ARRAY_TAG: {
            return false;
        }
        case JDWPConstants.Tag.BYTE_TAG: {
            return true;
        }
        case JDWPConstants.Tag.CHAR_TAG: {
            return true;
        }
        case JDWPConstants.Tag.OBJECT_TAG: {
            return false;
        }
        case JDWPConstants.Tag.FLOAT_TAG: {
            return true;
        }
        case JDWPConstants.Tag.DOUBLE_TAG: {
            return true;
        }
        case JDWPConstants.Tag.INT_TAG: {
            return true;
        }
        case JDWPConstants.Tag.LONG_TAG: {
            return true;
        }
        case JDWPConstants.Tag.SHORT_TAG: {
            return true;
        }
        case JDWPConstants.Tag.VOID_TAG: {
            return true;
        }
        case JDWPConstants.Tag.BOOLEAN_TAG: {
            return true;
        }
        case JDWPConstants.Tag.STRING_TAG: {
            return false;
        }
        case JDWPConstants.Tag.THREAD_TAG: {
            return false;
        }
        case JDWPConstants.Tag.THREAD_GROUP_TAG: {
            return false;
        }
        case JDWPConstants.Tag.CLASS_LOADER_TAG: {
            return false;
        }
        case JDWPConstants.Tag.CLASS_OBJECT_TAG: {
            return false;
        }
        case JDWPConstants.Tag.NO_TAG: {
            return true;
        }
        default: {
            throw new TestErrorException("Improper JDWP.tag value = " + tag);
        }
        }
    }

    /**
     * Sets the next value of the data of the Packet as byte.
     * 
     * @param val
     *            the byte value.
     */
    public void setNextValueAsByte(byte val) {
        int new_data_size = data.length + BYTE_SIZE;
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size - BYTE_SIZE);
        data[new_data_size - BYTE_SIZE] = val;
    }

    /**
     * Gets the next value of the data of the Packet as byte.
     * 
     * @return the next value of the data of the Packet as byte.
     */
    public byte getNextValueAsByte() {
        reading_data_index = reading_data_index + BYTE_SIZE;
        return data[reading_data_index - BYTE_SIZE];
    }

    /**
     * Sets the next value of the data of the Packet as boolean.
     * 
     * @param val
     *            the boolean value.
     */
    public void setNextValueAsBoolean(boolean val) {
        int old_data_size = data.length;
        int new_data_size = old_data_size
                + TypesLengths.getTypeLength(TypesLengths.BOOLEAN_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, old_data_size);
        if (val) {
            data[old_data_size] = 1;
        } else {
            data[old_data_size] = 0;
        }
    }

    /**
     * Gets the next value of the data of the Packet as boolean.
     * 
     * @return the next value of the data of the Packet as boolean.
     */
    public boolean getNextValueAsBoolean() {
        int res = (int) data[reading_data_index] & 0xFF;
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.BOOLEAN_ID);
        return (res != 0);
    }

    /**
     * Sets the next value of the data of the Packet as short.
     * 
     * @param val
     *            the short value.
     */
    public void setNextValueAsShort(short val) {
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.SHORT_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.SHORT_ID));
        this.writeAtByteArray((long) val, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.SHORT_ID),
                TypesLengths.getTypeLength(TypesLengths.SHORT_ID));
    }

    /**
     * Gets the next value of the data of the Packet as short.
     * 
     * @return the next value of the data of the Packet as short.
     */
    public short getNextValueAsShort() {
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.SHORT_ID);
        return (short) readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.SHORT_ID),
                TypesLengths.getTypeLength(TypesLengths.SHORT_ID));
    }

    /**
     * Sets the next value of the data of the Packet as int.
     * 
     * @param val
     *            the int value.
     */
    public void setNextValueAsInt(int val) {
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.INT_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.INT_ID));
        this.writeAtByteArray((long) val, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.INT_ID), TypesLengths
                .getTypeLength(TypesLengths.INT_ID));
    }

    /**
     * Gets the next value of the data of the Packet as int.
     * 
     * @return the next value of the data of the Packet as int.
     */
    public int getNextValueAsInt() {
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.INT_ID);
        return (int) readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.INT_ID), TypesLengths
                .getTypeLength(TypesLengths.INT_ID));
    }

    /**
     * Sets the next value of the data of the Packet as double.
     * 
     * @param dval
     *            the double value.
     */
    public void setNextValueAsDouble(double dval) {
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.DOUBLE_ID);
        byte data_temp[] = data;
        long val = Double.doubleToLongBits(dval);
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.DOUBLE_ID));
        this.writeAtByteArray((long) val, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.DOUBLE_ID),
                TypesLengths.getTypeLength(TypesLengths.DOUBLE_ID));
    }

    /**
     * Gets the next value of the data of the Packet as double.
     * 
     * @return the next value of the data of the Packet as double.
     */
    public double getNextValueAsDouble() {
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.DOUBLE_ID);
        long res = readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.DOUBLE_ID),
                TypesLengths.getTypeLength(TypesLengths.DOUBLE_ID));

        return Double.longBitsToDouble(res);
    }

    /**
     * Sets the next value of the data of the Packet as float.
     * 
     * @param fval
     *            the float value.
     */
    public void setNextValueAsFloat(float fval) {
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.FLOAT_ID);
        byte data_temp[] = data;
        long val = Float.floatToIntBits(fval);
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.FLOAT_ID));
        this.writeAtByteArray((long) val, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.FLOAT_ID),
                TypesLengths.getTypeLength(TypesLengths.FLOAT_ID));
    }

    /**
     * Gets the next value of the data of the Packet as float.
     * 
     * @return the next value of the data of the Packet as float.
     */
    public float getNextValueAsFloat() {
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.FLOAT_ID);
        long res = readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.FLOAT_ID),
                TypesLengths.getTypeLength(TypesLengths.FLOAT_ID));

        return Float.intBitsToFloat((int) res);
    }

    /**
     * Sets the next value of the data of the Packet as char.
     * 
     * @param val
     *            the char value.
     */
    public void setNextValueAsChar(char val) {
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.CHAR_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.CHAR_ID));
        this.writeAtByteArray((long) val, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.CHAR_ID),
                TypesLengths.getTypeLength(TypesLengths.CHAR_ID));
    }

    /**
     * Gets the next value of the data of the Packet as char.
     * 
     * @return the next value of the data of the Packet as char.
     */
    public char getNextValueAsChar() {
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.CHAR_ID);
        return (char) readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.CHAR_ID),
                TypesLengths.getTypeLength(TypesLengths.CHAR_ID));
    }

    /**
     * Sets the next value of the data of the Packet as long.
     * 
     * @param val
     *            the long value.
     */
    public void setNextValueAsLong(long val) {
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.LONG_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.LONG_ID));
        this.writeAtByteArray(val, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.LONG_ID),
                TypesLengths.getTypeLength(TypesLengths.LONG_ID));
    }

    /**
     * Gets the next value of the data of the Packet as long.
     * 
     * @return the next value of the data of the Packet as long.
     */
    public long getNextValueAsLong() {
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.LONG_ID);
        return readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.LONG_ID),
                TypesLengths.getTypeLength(TypesLengths.LONG_ID));
    }

    /**
     * Sets the next value of the data of the Packet as String in the "UTF-8"
     * Charset.
     * 
     * @param val
     *            the String in the "UTF-8" Charset.
     */
    public void setNextValueAsString(String val) {
        byte data_temp[] = data;
        byte val_as_bytes[];
        try {
            val_as_bytes = val.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new TestErrorException(e);
        }
        int new_data_size = data.length + val_as_bytes.length
                + TypesLengths.getTypeLength(TypesLengths.INT_ID);
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - val_as_bytes.length
                - TypesLengths.getTypeLength(TypesLengths.INT_ID));
        this.writeAtByteArray((long) val_as_bytes.length, data, new_data_size
                - val_as_bytes.length
                - TypesLengths.getTypeLength(TypesLengths.INT_ID), TypesLengths
                .getTypeLength(TypesLengths.INT_ID));
        System.arraycopy(val_as_bytes, 0, data, new_data_size
                - val_as_bytes.length, val_as_bytes.length);
    }

    /**
     * Gets the next value of the data of the Packet as String in the "UTF-8"
     * Charset.
     * 
     * @return the next value of the data of the Packet as String in the "UTF-8"
     *         Charset.
     */
    public String getNextValueAsString() {
        int string_length = this.getNextValueAsInt();
        String res = null;
        try {
            res = new String(data, reading_data_index, string_length, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new TestErrorException(e);
        }
        reading_data_index = reading_data_index + string_length;
        return res;
    }

    /**
     * Sets the next value of the data of the Packet as objectID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param val
     *            the ObjectID value.
     */
    public void setNextValueAsObjectID(long val) {
        if (TypesLengths.getTypeLength(TypesLengths.OBJECT_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.OBJECT_ID) > 8) {
            throw new TestErrorException("Improper ObjectID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.OBJECT_ID));
        }
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.OBJECT_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.OBJECT_ID));
        this.writeAtByteArray(val, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.OBJECT_ID),
                TypesLengths.getTypeLength(TypesLengths.OBJECT_ID));
    }

    /**
     * Gets the next value of the data of the Packet as objectID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsObjectID() {
        if (TypesLengths.getTypeLength(TypesLengths.OBJECT_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.OBJECT_ID) > 8) {
            throw new TestErrorException("Improper ObjectID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.OBJECT_ID) + "!");
        }
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.OBJECT_ID);
        return (int) readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.OBJECT_ID),
                TypesLengths.getTypeLength(TypesLengths.OBJECT_ID));
    }

    /**
     * Sets the next value of the data of the Packet as ThreadID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param val
     *            the ThreadID value.
     */
    public void setNextValueAsThreadID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as ThreadID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsThreadID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as ThreadGroupID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the val value will be ignored.
     * 
     * @param val
     *            the ThreadGroupID value.
     */
    public void setNextValueAsThreadGroupID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as ThreadGroupID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsThreadGroupID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as StringID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param val
     *            the StringID value.
     */
    public void setNextValueAsStringID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as StringID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsStringID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as ClassLoaderID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the val value will be ignored.
     * 
     * @param val
     *            the ClassLoaderID value.
     */
    public void setNextValueAsClassLoaderID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as ClassLoaderID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsClassLoaderID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as ClassObjectID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the val value will be ignored.
     * 
     * @param val
     *            the ClassObjectID value.
     */
    public void setNextValueAsClassObjectID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as ClassObjectID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsClassObjectID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as ArrayID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param val
     *            the ArrayID value.
     */
    public void setNextValueAsArrayID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as ArrayID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsClassArrayID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as ReferenceTypeID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the val value will be ignored.
     * 
     * @param val
     *            the ReferenceTypeID value.
     */
    public void setNextValueAsReferenceTypeID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as ReferenceTypeID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsReferenceTypeID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as ClassID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param val
     *            the ClassID value.
     */
    public void setNextValueAsClassID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as ClassID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsClassID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as InterfaceID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param val
     *            the InterfaceID value.
     */
    public void setNextValueAsInterfaceID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as InterfaceID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsInterfaceID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as ArrayTypeID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param val
     *            the ArrayTypeID value.
     */
    public void setNextValueAsArrayTypeID(long val) {
        this.setNextValueAsObjectID(val);
    }

    /**
     * Gets the next value of the data of the Packet as ArrayTypeID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsArrayTypeID() {
        return this.getNextValueAsObjectID();
    }

    /**
     * Sets the next value of the data of the Packet as tagged-objectID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the val value will be ignored.
     * 
     * @param taggedObject
     *            TaggedObject value.
     */
    public void setNextValueAsTaggedObject(TaggedObject taggedObject) {
        this.setNextValueAsByte(taggedObject.tag);
        this.setNextValueAsObjectID(taggedObject.objectID);
    }

    /**
     * Gets the next value of the data of the Packet as tagged-objectID
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public TaggedObject getNextValueAsTaggedObject() {
        if (TypesLengths.getTypeLength(TypesLengths.OBJECT_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.OBJECT_ID) > 8) {
            throw new TestErrorException("Improper ObjectID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.OBJECT_ID));
        }
        TaggedObject taggedObject = new TaggedObject();
        taggedObject.tag = this.getNextValueAsByte();
        taggedObject.objectID = this.getNextValueAsObjectID();
        return taggedObject;
    }

    /**
     * Sets the next value of the data of the Packet as MethodID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param methodID
     *            MethodID value.
     */
    public void setNextValueAsMethodID(long methodID) {
        if (TypesLengths.getTypeLength(TypesLengths.METHOD_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.METHOD_ID) > 8) {
            throw new TestErrorException("Improper MethodID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.METHOD_ID));
        }
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.METHOD_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.METHOD_ID));
        this.writeAtByteArray(methodID, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.METHOD_ID),
                TypesLengths.getTypeLength(TypesLengths.METHOD_ID));
    }

    /**
     * Gets the next value of the data of the Packet as MethodID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsMethodID() {
        if (TypesLengths.getTypeLength(TypesLengths.METHOD_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.METHOD_ID) > 8) {
            throw new TestErrorException("Improper MethodID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.METHOD_ID));
        }
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.METHOD_ID);
        long result = readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.METHOD_ID),
                TypesLengths.getTypeLength(TypesLengths.METHOD_ID));
        return result;
    }

    /**
     * Sets the next value of the data of the Packet as FieldID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param fieldID
     *            FieldID value.
     */
    public void setNextValueAsFieldID(long fieldID) {
        if (TypesLengths.getTypeLength(TypesLengths.FIELD_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.FIELD_ID) > 8) {
            throw new TestErrorException("Improper FieldID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.FIELD_ID));
        }
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.FIELD_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.FIELD_ID));
        this.writeAtByteArray(fieldID, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.FIELD_ID),
                TypesLengths.getTypeLength(TypesLengths.FIELD_ID));
    }

    /**
     * Gets the next value of the data of the Packet as FieldID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsFieldID() {
        if (TypesLengths.getTypeLength(TypesLengths.FIELD_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.FIELD_ID) > 8) {
            throw new TestErrorException("Improper FieldID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.FIELD_ID));
        }
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.FIELD_ID);
        long result = readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.FIELD_ID),
                TypesLengths.getTypeLength(TypesLengths.FIELD_ID));
        return result;
    }

    /**
     * Sets the next value of the data of the Packet as FrameID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param frameID
     *            FrameID value.
     */
    public void setNextValueAsFrameID(long frameID) {
        if (TypesLengths.getTypeLength(TypesLengths.FRAME_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.FRAME_ID) > 8) {
            throw new TestErrorException("Improper FrameID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.FRAME_ID));
        }
        int new_data_size = data.length
                + TypesLengths.getTypeLength(TypesLengths.FRAME_ID);
        byte data_temp[] = data;
        data = new byte[new_data_size];
        System.arraycopy(data_temp, 0, data, 0, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.FRAME_ID));
        this.writeAtByteArray(frameID, data, new_data_size
                - TypesLengths.getTypeLength(TypesLengths.FRAME_ID),
                TypesLengths.getTypeLength(TypesLengths.FRAME_ID));
    }

    /**
     * Gets the next value of the data of the Packet as FrameID VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public long getNextValueAsFrameID() {
        if (TypesLengths.getTypeLength(TypesLengths.FRAME_ID) < 0
                || TypesLengths.getTypeLength(TypesLengths.FRAME_ID) > 8) {
            throw new TestErrorException("Improper FrameID value length = "
                    + TypesLengths.getTypeLength(TypesLengths.FRAME_ID));
        }
        reading_data_index = reading_data_index
                + TypesLengths.getTypeLength(TypesLengths.FRAME_ID);
        long result = readFromByteArray(data, reading_data_index
                - TypesLengths.getTypeLength(TypesLengths.FRAME_ID),
                TypesLengths.getTypeLength(TypesLengths.FRAME_ID));
        return result;
    }

    /**
     * Sets the next value of the data of the Packet as Location VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param location
     *            Location value.
     */
    public void setNextValueAsLocation(Location location) {
        this.setNextValueAsByte(location.tag);
        this.setNextValueAsClassID(location.classID);
        this.setNextValueAsMethodID(location.methodID);
        this.setNextValueAsLong(location.index);
    }

    /**
     * Gets the next value of the data of the Packet as Location VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public Location getNextValueAsLocation() {
        Location location = new Location();
        location.tag = this.getNextValueAsByte();
        location.classID = this.getNextValueAsClassID();
        location.methodID = this.getNextValueAsMethodID();
        location.index = this.getNextValueAsLong();
        return location;
    }

    /**
     * Sets the next value of the data of the Packet as Value VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param value
     *            Value value.
     * @throws UnsupportedEncodingException
     */
    public void setNextValueAsValue(Value value) {
        this.setNextValueAsByte(value.getTag());
        setNextValueAsUntaggedValue(value);
    }

    /**
     * Gets the next value of the data of the Packet as Value VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public Value getNextValueAsValue() {
        byte tag = this.getNextValueAsByte();
        return getNextValueAsUntaggedValue(tag);
    }

    /**
     * Sets the next value of the data of the Packet as UntaggedValue
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the val value will be ignored.
     * 
     * @param value
     *            UntaggedValue value.
     * @throws UnsupportedEncodingException
     */
    public void setNextValueAsUntaggedValue(Value value) {
        switch (value.getTag()) {
        case JDWPConstants.Tag.BOOLEAN_TAG:
            this.setNextValueAsBoolean(value.getBooleanValue());
            break;
        case JDWPConstants.Tag.BYTE_TAG:
            this.setNextValueAsByte(value.getByteValue());
            break;
        case JDWPConstants.Tag.CHAR_TAG:
            this.setNextValueAsChar(value.getCharValue());
            break;
        case JDWPConstants.Tag.DOUBLE_TAG:
            this.setNextValueAsDouble(value.getDoubleValue());
            break;
        case JDWPConstants.Tag.FLOAT_TAG:
            this.setNextValueAsFloat(value.getFloatValue());
            break;
        case JDWPConstants.Tag.INT_TAG:
            this.setNextValueAsInt(value.getIntValue());
            break;
        case JDWPConstants.Tag.LONG_TAG:
            this.setNextValueAsLong(value.getLongValue());
            break;
        case JDWPConstants.Tag.SHORT_TAG:
            this.setNextValueAsShort(value.getShortValue());
            break;
        case JDWPConstants.Tag.VOID_TAG:
            break;
        case JDWPConstants.Tag.STRING_TAG:
        case JDWPConstants.Tag.ARRAY_TAG:
        case JDWPConstants.Tag.CLASS_LOADER_TAG:
        case JDWPConstants.Tag.CLASS_OBJECT_TAG:
        case JDWPConstants.Tag.OBJECT_TAG:
        case JDWPConstants.Tag.THREAD_GROUP_TAG:
        case JDWPConstants.Tag.THREAD_TAG:
            this.setNextValueAsObjectID(value.getLongValue());
            break;
        default:
            throw new TestErrorException("Illegal tag value = "
                    + value.getTag());
        }
    }

    /**
     * Gets the next value of the data of the Packet as UntaggedValue
     * VM-sensitive value. If length is less than 8 bytes, the appropriate high
     * bits in the returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public Value getNextValueAsUntaggedValue(byte tag) {
        Value value = null;
        switch (tag) {
        case JDWPConstants.Tag.BOOLEAN_TAG:
            value = new Value(this.getNextValueAsBoolean());
            break;
        case JDWPConstants.Tag.BYTE_TAG:
            value = new Value(this.getNextValueAsByte());
            break;
        case JDWPConstants.Tag.CHAR_TAG:
            value = new Value(this.getNextValueAsChar());
            break;
        case JDWPConstants.Tag.DOUBLE_TAG:
            value = new Value(this.getNextValueAsDouble());
            break;
        case JDWPConstants.Tag.FLOAT_TAG:
            value = new Value(this.getNextValueAsFloat());
            break;
        case JDWPConstants.Tag.INT_TAG:
            value = new Value(this.getNextValueAsInt());
            break;
        case JDWPConstants.Tag.LONG_TAG:
            value = new Value(this.getNextValueAsLong());
            break;
        case JDWPConstants.Tag.SHORT_TAG:
            value = new Value(this.getNextValueAsShort());
            break;
        case JDWPConstants.Tag.STRING_TAG:
        case JDWPConstants.Tag.ARRAY_TAG:
        case JDWPConstants.Tag.CLASS_LOADER_TAG:
        case JDWPConstants.Tag.CLASS_OBJECT_TAG:
        case JDWPConstants.Tag.OBJECT_TAG:
        case JDWPConstants.Tag.THREAD_GROUP_TAG:
        case JDWPConstants.Tag.THREAD_TAG:
            value = new Value(tag, this.getNextValueAsObjectID());
            break;
        default:
            throw new TestErrorException("Illegal tag value = " + tag);
        }
        return value;
    }

    /**
     * Sets the next value of the data of the Packet as ArrayRegion VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * val value will be ignored.
     * 
     * @param array
     *            ArrayRegion value.
     * @throws UnsupportedEncodingException
     */
    // public void setNextValueAsArrayRegion(ArrayRegion array) throws
    // UnsupportedEncodingException {
    public void setNextValueAsArrayRegion(ArrayRegion array) {
        this.setNextValueAsByte(array.getTag());
        this.setNextValueAsInt(array.getLength());
        for (int i = 0; i < array.getLength(); i++) {
            if (isValuePrimitiveType(array.getTag())) {
                switch (array.getTag()) {
                case JDWPConstants.Tag.BOOLEAN_TAG:
                    this.setNextValueAsBoolean(array.getValue(i)
                            .getBooleanValue());
                    break;
                case JDWPConstants.Tag.BYTE_TAG:
                    this.setNextValueAsByte(array.getValue(i).getByteValue());
                    break;
                case JDWPConstants.Tag.DOUBLE_TAG:
                    this.setNextValueAsDouble(array.getValue(i)
                            .getDoubleValue());
                    break;
                case JDWPConstants.Tag.FLOAT_TAG:
                    this.setNextValueAsFloat(array.getValue(i).getFloatValue());
                    break;
                case JDWPConstants.Tag.INT_TAG:
                    this.setNextValueAsInt(array.getValue(i).getIntValue());
                    break;
                case JDWPConstants.Tag.LONG_TAG:
                    this.setNextValueAsLong(array.getValue(i).getLongValue());
                    break;
                case JDWPConstants.Tag.SHORT_TAG:
                    this.setNextValueAsShort(array.getValue(i).getShortValue());
                    break;
                default:
                    throw new TestErrorException("Illegal tag value = "
                            + array.getTag());
                }
            } else {
                this.setNextValueAsValue(array.getValue(i));
            }
        }
    }

    /**
     * Gets the next value of the data of the Packet as ArrayRegion VM-sensitive
     * value. If length is less than 8 bytes, the appropriate high bits in the
     * returned value can be ignored.
     * 
     * @return the next value of the data of the Packet as VM-sensitive value.
     */
    public ArrayRegion getNextValueAsArrayRegion() {
        byte array_tag = this.getNextValueAsByte();
        int array_length = this.getNextValueAsInt();

        ArrayRegion array = new ArrayRegion(array_tag, array_length);

        for (int i = 0; i < array_length; i++) {
            if (isValuePrimitiveType(array_tag))
                array.setValue(i, this.getNextValueAsUntaggedValue(array_tag));
            else
                array.setValue(i, this.getNextValueAsValue());
        }
        return array;
    }

    /**
     * Gets the representation of the Packet as array of bytes in the JDWP
     * format including header and data sections.
     * 
     * @return bytes representation of this packet
     */
    public byte[] toBytesArray() {
        byte res[] = new byte[data.length + HEADER_SIZE];
        writeAtByteArray(data.length + HEADER_SIZE, res, LENGTH_INDEX, INT_SIZE);
        writeAtByteArray(id, res, ID_INDEX, INT_SIZE);
        res[FLAGS_INDEX] = flags;
        System.arraycopy(data, 0, res, HEADER_SIZE, data.length);
        return res;
    }

    /**
     * Reads value from array of bytes ar[] starting form index and reading size
     * bytes. If size is less than 8, the appropriate high bits in the resulting
     * long value will be zero.
     * 
     * @param ar
     *            the array of bytes where the value is read from.
     * @param from
     *            index to start reading bytes.
     * @param size
     *            number of bytes to read
     */
    protected static long readFromByteArray(byte ar[], int from, int size) {
        long res = 0;
        byte temp;
        for (int i = 0; i < size; i++) {
            temp = ar[from + i];
            res = (res << 8) | (((long) temp) & 0xFF);
        }
        return res;
    }

    /**
     * Tells whether the packet is reply.
     * 
     * @return true if this packet is reply, false if it is command
     */
    public boolean isReply() {
        return (flags & REPLY_PACKET_FLAG) != 0;
    }

    /**
     * Checks whether all data has been read from the packet.
     * 
     * @return boolean
     */
    public boolean isAllDataRead() {
        return reading_data_index == data.length;
    }

    /**
     * Writes value - val to the array of bytes ar[], beginning from index - to,
     * size of value is - size bytes. If size is less than 8, the appropriate
     * high bits in the val value will be ignored.
     * 
     * @param val
     *            the value, which will be written in the array.
     * @param ar
     *            the array of bytes where the value is read from.
     * @param to
     *            the beginning index in the array of bytes.
     * @param size
     *            size of value in bytes.
     */
    protected void writeAtByteArray(long val, byte ar[], int to, int size) {
        for (int i = 0; i < size; i++) {
            ar[to + i] = (byte) (val >> 8 * (size - 1 - i));
        }
    }

    /**
     * Returns true if this bytes array can be interpreted as reply packet.
     * 
     * @param p
     *            bytes representation of packet
     * @return true or false
     */
    public static boolean isReply(byte[] p) {
        if (p.length < FLAGS_INDEX)
            return false;
        return (p[FLAGS_INDEX] & REPLY_PACKET_FLAG) != 0;
    }

    /**
     * Returns packet length from header of given packet bytes.
     * 
     * @param p
     *            bytes representation of packet
     * @return true or false
     */
    public static int getPacketLength(byte[] p) {
        return (int) readFromByteArray(p, LENGTH_INDEX, INT_SIZE);
    }

    /**
     * Enwraps this bytes array either to ReplyPacket or EventPacket instance,
     * according to result of isReply().
     * 
     * @param p
     *            bytes array to enwrap into packet
     * @return new created ReplyPacket or CommandPacket
     */
    public static Packet interpretPacket(byte[] p) {
        if (p.length < HEADER_SIZE)
            throw new TestErrorException("Wrong packet");
        if (Packet.isReply(p))
            return new ReplyPacket(p);
        return new EventPacket(p);
    }
}
