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
 * @author Aleksey V. Yantsen
 */

/**
 * Created on 10.25.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

public class ArrayRegion {
    private byte    tag;
    private int     length;
    private Value[] values;

    /**
     * Constructor
     */
    public ArrayRegion(byte tag, int length) {
        this.tag = tag;
        this.length = length;
        values = new Value[length];
    }
    
    /**
     * @param index Index of value to return
     * @return Returns the value.
     */
    public Value getValue(int index) {
        return values[index];
    }

    /**
     * @param index Index of value
     * @param value Value to set
     */
    public void setValue(int index, Value value) {
        values[index] = value;
    }

    /**
     * @return Returns the length.
     */
    public int getLength() {
        return length;
    }
    /**
     * @param length The length to set.
     */
    public void setLength(int length) {
        this.length = length;
    }
    /**
     * @return Returns the tag.
     */
    public byte getTag() {
        return tag;
    }
    /**
     * @param tag The tag to set.
     */
    public void setTag(byte tag) {
        this.tag = tag;
    }
}