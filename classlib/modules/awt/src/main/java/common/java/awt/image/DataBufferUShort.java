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
 * @author Igor V. Stolyarov
 */
package java.awt.image;

import org.apache.harmony.awt.internal.nls.Messages;


public final class DataBufferUShort extends DataBuffer {

    short data[][];

    public DataBufferUShort(short dataArrays[][], int size, int offsets[]) {
        super(TYPE_USHORT, size, dataArrays.length, offsets);
        for(int i = 0; i < dataArrays.length; i++){
            if(dataArrays[i].length < offsets[i] + size){
                // awt.28d=Length of dataArray[{0}] is less than size + offset[{1}]
                throw new IllegalArgumentException(Messages.getString("awt.28D", i, i));  //$NON-NLS-1$
            }
        }
        data = dataArrays.clone();
    }

    public DataBufferUShort(short dataArrays[][], int size) {
        super(TYPE_USHORT, size, dataArrays.length);
        data = dataArrays.clone();
    }

    public DataBufferUShort(short dataArray[], int size, int offset) {
        super(TYPE_USHORT, size, 1, offset);
        if(dataArray.length < size + offset){
            // awt.28E=Length of dataArray is less than size + offset
            throw new IllegalArgumentException(Messages.getString("awt.28E")); //$NON-NLS-1$
        }
        data = new short[1][];
        data[0] = dataArray;
    }

    public DataBufferUShort(short dataArray[], int size) {
        super(TYPE_USHORT, size);
        data = new short[1][];
        data[0] = dataArray;
    }

    public DataBufferUShort(int size, int numBanks) {
        super(TYPE_USHORT, size, numBanks);
        data = new short[numBanks][];
        int i= 0;
        while( i < numBanks) {
            data[i++] = new short[size];
        }
    }

    public DataBufferUShort(int size) {
        super(TYPE_USHORT, size);
        data = new short[1][];
        data[0] = new short[size];
    }

    @Override
    public void setElem(int bank, int i, int val) {
        data[bank][offsets[bank] + i] = (short)val;
        notifyChanged();
    }

    @Override
    public void setElem(int i, int val) {
        data[0][offset + i] = (short)val;
        notifyChanged();
    }

    @Override
    public int getElem(int bank, int i) {
        return (data[bank][offsets[bank] + i]) & 0xffff;
    }

    public short[] getData(int bank) {
        notifyTaken();
        return data[bank];
    }

    @Override
    public int getElem(int i) {
        return (data[0][offset + i]) & 0xffff;
    }

    public short[][] getBankData() {
        notifyTaken();
        return data.clone();
    }

    public short[] getData() {
        notifyTaken();
        return data[0];
    }
}

