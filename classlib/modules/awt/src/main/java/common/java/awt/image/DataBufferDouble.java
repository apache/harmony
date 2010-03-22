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

public final class DataBufferDouble extends DataBuffer {

    double data[][];

    public DataBufferDouble(double dataArrays[][], int size, int offsets[]) {
        super(TYPE_DOUBLE, size, dataArrays.length, offsets);
        data = dataArrays.clone();
    }

    public DataBufferDouble(double dataArrays[][], int size) {
        super(TYPE_DOUBLE, size, dataArrays.length);
        data = dataArrays.clone();
    }

    public DataBufferDouble(double dataArray[], int size, int offset) {
        super(TYPE_DOUBLE, size, 1, offset);
        data = new double[1][];
        data[0] = dataArray;
    }

    public DataBufferDouble(double dataArray[], int size) {
        super(TYPE_DOUBLE, size);
        data = new double[1][];
        data[0] = dataArray;
    }

    public DataBufferDouble(int size, int numBanks) {
        super(TYPE_DOUBLE, size, numBanks);
        data = new double[numBanks][];
        int i = 0;
        while (i < numBanks) {
            data[i++] = new double[size];
        }
    }

    public DataBufferDouble(int size) {
        super(TYPE_DOUBLE, size);
        data = new double[1][];
        data[0] = new double[size];
    }

    @Override
    public void setElem(int bank, int i, int val) {
        data[bank][offsets[bank] + i] = val;
        notifyChanged();
    }

    @Override
    public void setElemFloat(int bank, int i, float val) {
        data[bank][offsets[bank] + i] = val;
        notifyChanged();
    }

    @Override
    public void setElemDouble(int bank, int i, double val) {
        data[bank][offsets[bank] + i] = val;
        notifyChanged();
    }

    @Override
    public void setElem(int i, int val) {
        data[0][offset + i] = val;
        notifyChanged();
    }

    @Override
    public int getElem(int bank, int i) {
        return (int) (data[bank][offsets[bank] + i]);
    }

    @Override
    public float getElemFloat(int bank, int i) {
        return (float) (data[bank][offsets[bank] + i]);
    }

    @Override
    public double getElemDouble(int bank, int i) {
        return data[bank][offsets[bank] + i];
    }

    @Override
    public void setElemFloat(int i, float val) {
        data[0][offset + i] = val;
        notifyChanged();
    }

    @Override
    public void setElemDouble(int i, double val) {
        data[0][offset + i] = val;
        notifyChanged();
    }

    public double[] getData(int bank) {
        notifyTaken();
        return data[bank];
    }

    @Override
    public int getElem(int i) {
        return (int) (data[0][offset + i]);
    }

    @Override
    public float getElemFloat(int i) {
        return (float) (data[0][offset + i]);
    }

    @Override
    public double getElemDouble(int i) {
        return data[0][offset + i];
    }

    public double[][] getBankData() {
        notifyTaken();
        return data.clone();
    }

    public double[] getData() {
        notifyTaken();
        return data[0];
    }
}

