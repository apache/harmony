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
 * @author Oleg V. Khaschansky
 *
 * @date: Oct 14, 2005
 */

package java.awt.image;

public class ByteLookupTable extends LookupTable {
    private byte data[][];
    public ByteLookupTable(int offset, byte[] data) {
        super(offset, 1);
        if (data.length < 1)
            throw new IllegalArgumentException("Length of data should not be less then one");
        this.data = new byte[1][data.length];
        // The data array stored as a reference
        this.data[0] = data;
    }

    public ByteLookupTable(int offset, byte[][] data) {
        super(offset, data.length);
        this.data = new byte[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            // The data array for each band stored as a reference
            this.data[i] = data[i];
        }
    }

    public final byte[][] getTable() {
        // Returns data by reference
        return data;
    }

    @Override
    public int[] lookupPixel(int[] src, int[] dst) {
        if (dst == null) {
            dst = new int[src.length];
        }

        int offset = getOffset();
        if (getNumComponents() == 1) {
            for (int i = 0; i < src.length; i++) {
                dst[i] = data[0][src[i]-offset];
            }
        } else {
            for (int i = 0; i < getNumComponents(); i++) {
                dst[i] = data[i][src[i]-offset];
            }
        }

        return dst;
    }

    public byte[] lookupPixel(byte[] src, byte[] dst) {
        if (dst == null) {
            dst = new byte[src.length];
        }

        int offset = getOffset();
        if (getNumComponents() == 1) {
            for (int i = 0; i < src.length; i++) {
                dst[i] = data[0][src[i]-offset];
            }
        } else {
            for (int i = 0; i < getNumComponents(); i++) {
                dst[i] = data[i][src[i]-offset];
            }
        }

        return dst;
    }
}
