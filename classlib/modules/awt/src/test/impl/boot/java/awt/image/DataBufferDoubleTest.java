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

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import junit.framework.TestCase;

public class DataBufferDoubleTest extends TestCase {

    DataBufferDouble db1, db2, db3, db4, db5, db6;
    int numBanks = 4;
    int arraySize = 20;
    int size = 10;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        double dataArrays[][] = new double[numBanks][];
        for(int i = 0; i < numBanks; i++){
            dataArrays[i] = new double[arraySize];
        }

        double dataArray[] = new double[arraySize];
        int offsets[] = new int[numBanks];
        for(int i = 0; i < numBanks; i++){
            offsets[i] = i;
        }

        db1 = new DataBufferDouble(dataArrays, size);
        db2 = new DataBufferDouble(dataArrays, size, offsets);
        db3 = new DataBufferDouble(dataArray, size);
        db4 = new DataBufferDouble(dataArray, size, numBanks);
        db5 = new DataBufferDouble(size);
        db6 = new DataBufferDouble(size, numBanks);
    }

    public DataBufferDoubleTest(String name) {
        super(name);
    }

    public final void testGetDataType(){
        assertEquals(DataBuffer.TYPE_DOUBLE, db1.getDataType());
        assertEquals(DataBuffer.TYPE_DOUBLE, db2.getDataType());
        assertEquals(DataBuffer.TYPE_DOUBLE, db3.getDataType());
        assertEquals(DataBuffer.TYPE_DOUBLE, db4.getDataType());
        assertEquals(DataBuffer.TYPE_DOUBLE, db5.getDataType());
        assertEquals(DataBuffer.TYPE_DOUBLE, db6.getDataType());
    }

    public final void testGetDataTypeSize(){
        assertEquals(64, DataBuffer.getDataTypeSize(db1.getDataType()));
        assertEquals(64, DataBuffer.getDataTypeSize(db2.getDataType()));
        assertEquals(64, DataBuffer.getDataTypeSize(db3.getDataType()));
        assertEquals(64, DataBuffer.getDataTypeSize(db4.getDataType()));
        assertEquals(64, DataBuffer.getDataTypeSize(db5.getDataType()));
        assertEquals(64, DataBuffer.getDataTypeSize(db6.getDataType()));
    }

    public final void testGetNumBanks(){
        assertEquals(numBanks, db1.getNumBanks());
        assertEquals(numBanks, db2.getNumBanks());
        assertEquals(1, db3.getNumBanks());
        assertEquals(1, db4.getNumBanks());
        assertEquals(1, db5.getNumBanks());
        assertEquals(numBanks, db6.getNumBanks());
    }

    public final void testGetSize(){
        assertEquals(size, db1.getSize());
        assertEquals(size, db2.getSize());
        assertEquals(size, db3.getSize());
        assertEquals(size, db4.getSize());
        assertEquals(size, db5.getSize());
        assertEquals(size, db6.getSize());
    }

    public final void testGetOffset(){
        assertEquals(0, db1.getOffset());
        assertEquals(0, db2.getOffset());
        assertEquals(0, db3.getOffset());
        assertEquals(numBanks, db4.getOffset());
        assertEquals(0, db5.getOffset());
        assertEquals(0, db6.getOffset());
    }

    public final void testGetOffsets(){
        int offsets[];

        offsets = db1.getOffsets();
        for(int i = 0; i < db1.getNumBanks(); i++){
            assertEquals(0, offsets[i]);
        }

        offsets = db2.getOffsets();
        for(int i = 0; i < db2.getNumBanks(); i++){
            assertEquals(i, offsets[i]);
        }

        offsets = db3.getOffsets();
        for(int i = 0; i < db3.getNumBanks(); i++){
            assertEquals(0, offsets[i]);
        }

        offsets = db4.getOffsets();
        for(int i = 0; i < db4.getNumBanks(); i++){
            assertEquals(numBanks, offsets[i]);
        }

        offsets = db5.getOffsets();
        for(int i = 0; i < db5.getNumBanks(); i++){
            assertEquals(0, offsets[i]);
        }

        offsets = db6.getOffsets();
        for(int i = 0; i < db6.getNumBanks(); i++){
            assertEquals(0, offsets[i]);
        }
    }

    public final void testGetBankData(){
        double bankData[][];

        bankData = db1.getBankData();
        assertEquals(numBanks, bankData.length);

        bankData = db2.getBankData();
        assertEquals(numBanks, bankData.length);

        bankData = db3.getBankData();
        assertEquals(1, bankData.length);

        bankData = db4.getBankData();
        assertEquals(1, bankData.length);

        bankData = db5.getBankData();
        assertEquals(1, bankData.length);

        bankData = db6.getBankData();
        assertEquals(numBanks, bankData.length);

    }

    public final void testGetData(){
        double data[];

        data = db1.getData();
        assertEquals(arraySize, data.length);

        data = db2.getData();
        assertEquals(arraySize, data.length);

        data = db3.getData();
        assertEquals(arraySize, data.length);

        data = db4.getData();
        assertEquals(arraySize, data.length);

        data = db5.getData();
        assertEquals(size, data.length);

        data = db6.getData();
        assertEquals(size, data.length);
    }

    public final void testGetDataBank(){
        double data[];

        for(int i = 0; i < db1.getNumBanks(); i++){
            data = db1.getData(i);
            assertEquals(arraySize, data.length);
        }

        for(int i = 0; i < db2.getNumBanks(); i++){
            data = db2.getData(i);
            assertEquals(arraySize, data.length);
        }

        for(int i = 0; i < db3.getNumBanks(); i++){
            data = db3.getData(i);
            assertEquals(arraySize, data.length);
        }

        for(int i = 0; i < db4.getNumBanks(); i++){
            data = db4.getData(i);
            assertEquals(arraySize, data.length);
        }

        for(int i = 0; i < db5.getNumBanks(); i++){
            data = db5.getData(i);
            assertEquals(size, data.length);
        }

        for(int i = 0; i < db6.getNumBanks(); i++){
            data = db6.getData(i);
            assertEquals(size, data.length);
        }
    }

    public final void testSetElemBank(){
        double data[];
        int offsets[];

        for(int i = 0; i < db1.getNumBanks(); i++){
            for(int j = 0; j < db1.getSize(); j++){
                db1.setElem(i, j, j);
            }
        }
        offsets = db1.getOffsets();
        for(int i = 0; i < db1.getNumBanks(); i++){
            data = db1.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db2.getNumBanks(); i++){
            for(int j = 0; j < db2.getSize(); j++){
                db2.setElem(i, j, j);
            }
        }
        offsets = db2.getOffsets();
        for(int i = 0; i < db2.getNumBanks(); i++){
            data = db2.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db3.getNumBanks(); i++){
            for(int j = 0; j < db3.getSize(); j++){
                db3.setElem(i, j, j);
            }
        }
        offsets = db3.getOffsets();
        for(int i = 0; i < db3.getNumBanks(); i++){
            data = db3.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db4.getNumBanks(); i++){
            for(int j = 0; j < db4.getSize(); j++){
                db4.setElem(i, j, j);
            }
        }
        offsets = db4.getOffsets();
        for(int i = 0; i < db4.getNumBanks(); i++){
            data = db4.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db5.getNumBanks(); i++){
            for(int j = 0; j < db5.getSize(); j++){
                db5.setElem(i, j, j);
            }
        }
        offsets = db5.getOffsets();
        for(int i = 0; i < db5.getNumBanks(); i++){
            data = db5.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db6.getNumBanks(); i++){
            for(int j = 0; j < db6.getSize(); j++){
                db6.setElem(i, j, j);
            }
        }
        offsets = db6.getOffsets();
        for(int i = 0; i < db6.getNumBanks(); i++){
            data = db6.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

    }

    public final void testSetElem(){
        double data[];
        int offset;

        for(int i = 0; i < size; i++){
            db1.setElem(i, i);
        }
        data = db1.getData();
        offset = db1.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db2.setElem(i, i);
        }
        data = db2.getData();
        offset = db2.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db3.setElem(i, i);
        }
        data = db3.getData();
        offset = db3.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db4.setElem(i, i);
        }
        data = db4.getData();
        offset = db4.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db5.setElem(i, i);
        }
        data = db5.getData();
        offset = db5.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db6.setElem(i, i);
        }
        data = db6.getData();
        offset = db6.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }
    }

    public final void testGetElemBank(){
        double data[];
        int offsets[];

        offsets = db1.getOffsets();
        for(int i = 0; i < db1.getNumBanks(); i++){
            data = db1.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db1.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db1.getElem(i, j));
            }
        }

        offsets = db2.getOffsets();
        for(int i = 0; i < db2.getNumBanks(); i++){
            data = db2.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db2.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db2.getElem(i, j));
            }
        }

        offsets = db3.getOffsets();
        for(int i = 0; i < db3.getNumBanks(); i++){
            data = db3.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db3.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db3.getElem(i, j));
            }
        }

        offsets = db4.getOffsets();
        for(int i = 0; i < db4.getNumBanks(); i++){
            data = db4.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db4.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db4.getElem(i, j));
            }
        }

        offsets = db5.getOffsets();
        for(int i = 0; i < db5.getNumBanks(); i++){
            data = db5.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db5.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db5.getElem(i, j));
            }
        }

        offsets = db6.getOffsets();
        for(int i = 0; i < db6.getNumBanks(); i++){
            data = db6.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db6.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db6.getElem(i, j));
            }
        }
    }

    public final void testGetElem(){
        double data[];
        int offset;

        data = db1.getData();
        offset = db1.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db1.getElem(i));
        }

        data = db2.getData();
        offset = db2.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db2.getElem(i));
        }

        data = db3.getData();
        offset = db3.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db3.getElem(i));
        }

        data = db4.getData();
        offset = db4.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db4.getElem(i));
        }

        data = db5.getData();
        offset = db5.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db5.getElem(i));
        }

        data = db6.getData();
        offset = db6.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db6.getElem(i));
        }
    }

    public final void testSetElemFloatBank(){
        double data[];
        int offsets[];

        for(int i = 0; i < db1.getNumBanks(); i++){
            for(int j = 0; j < db1.getSize(); j++){
                db1.setElemFloat(i, j, j);
            }
        }
        offsets = db1.getOffsets();
        for(int i = 0; i < db1.getNumBanks(); i++){
            data = db1.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db2.getNumBanks(); i++){
            for(int j = 0; j < db2.getSize(); j++){
                db2.setElemFloat(i, j, j);
            }
        }
        offsets = db2.getOffsets();
        for(int i = 0; i < db2.getNumBanks(); i++){
            data = db2.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db3.getNumBanks(); i++){
            for(int j = 0; j < db3.getSize(); j++){
                db3.setElemFloat(i, j, j);
            }
        }
        offsets = db3.getOffsets();
        for(int i = 0; i < db3.getNumBanks(); i++){
            data = db3.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db4.getNumBanks(); i++){
            for(int j = 0; j < db4.getSize(); j++){
                db4.setElemFloat(i, j, j);
            }
        }
        offsets = db4.getOffsets();
        for(int i = 0; i < db4.getNumBanks(); i++){
            data = db4.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db5.getNumBanks(); i++){
            for(int j = 0; j < db5.getSize(); j++){
                db5.setElemFloat(i, j, j);
            }
        }
        offsets = db5.getOffsets();
        for(int i = 0; i < db5.getNumBanks(); i++){
            data = db5.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db6.getNumBanks(); i++){
            for(int j = 0; j < db6.getSize(); j++){
                db6.setElemFloat(i, j, j);
            }
        }
        offsets = db6.getOffsets();
        for(int i = 0; i < db6.getNumBanks(); i++){
            data = db6.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

    }

    public final void testSetElemFloat(){
        double data[];
        int offset;

        for(int i = 0; i < size; i++){
            db1.setElemFloat(i, i);
        }
        data = db1.getData();
        offset = db1.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db2.setElemFloat(i, i);
        }
        data = db2.getData();
        offset = db2.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db3.setElemFloat(i, i);
        }
        data = db3.getData();
        offset = db3.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db4.setElemFloat(i, i);
        }
        data = db4.getData();
        offset = db4.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db5.setElemFloat(i, i);
        }
        data = db5.getData();
        offset = db5.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db6.setElemFloat(i, i);
        }
        data = db6.getData();
        offset = db6.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }
    }

    public final void testGetElemFloatBank(){
        double data[];
        int offsets[];

        offsets = db1.getOffsets();
        for(int i = 0; i < db1.getNumBanks(); i++){
            data = db1.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db1.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db1.getElemFloat(i, j), 0f);
            }
        }

        offsets = db2.getOffsets();
        for(int i = 0; i < db2.getNumBanks(); i++){
            data = db2.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db2.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db2.getElem(i, j), 0f);
            }
        }

        offsets = db3.getOffsets();
        for(int i = 0; i < db3.getNumBanks(); i++){
            data = db3.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db3.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db3.getElemFloat(i, j), 0f);
            }
        }

        offsets = db4.getOffsets();
        for(int i = 0; i < db4.getNumBanks(); i++){
            data = db4.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db4.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db4.getElemFloat(i, j), 0f);
            }
        }

        offsets = db5.getOffsets();
        for(int i = 0; i < db5.getNumBanks(); i++){
            data = db5.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db5.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db5.getElemFloat(i, j), 0f);
            }
        }

        offsets = db6.getOffsets();
        for(int i = 0; i < db6.getNumBanks(); i++){
            data = db6.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db6.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db6.getElemFloat(i, j), 0f);
            }
        }
    }

    public final void testGetElemFloat(){
        double data[];
        int offset;

        data = db1.getData();
        offset = db1.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db1.getElemFloat(i), 0f);
        }

        data = db2.getData();
        offset = db2.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db2.getElemFloat(i), 0f);
        }

        data = db3.getData();
        offset = db3.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db3.getElemFloat(i), 0f);
        }

        data = db4.getData();
        offset = db4.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db4.getElemFloat(i), 0f);
        }

        data = db5.getData();
        offset = db5.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db5.getElemFloat(i), 0f);
        }

        data = db6.getData();
        offset = db6.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db6.getElemFloat(i), 0f);
        }
    }

    public final void testSetElemDoubleBank(){
        double data[];
        int offsets[];

        for(int i = 0; i < db1.getNumBanks(); i++){
            for(int j = 0; j < db1.getSize(); j++){
                db1.setElemDouble(i, j, j);
            }
        }
        offsets = db1.getOffsets();
        for(int i = 0; i < db1.getNumBanks(); i++){
            data = db1.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db2.getNumBanks(); i++){
            for(int j = 0; j < db2.getSize(); j++){
                db2.setElemDouble(i, j, j);
            }
        }
        offsets = db2.getOffsets();
        for(int i = 0; i < db2.getNumBanks(); i++){
            data = db2.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db3.getNumBanks(); i++){
            for(int j = 0; j < db3.getSize(); j++){
                db3.setElemDouble(i, j, j);
            }
        }
        offsets = db3.getOffsets();
        for(int i = 0; i < db3.getNumBanks(); i++){
            data = db3.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db4.getNumBanks(); i++){
            for(int j = 0; j < db4.getSize(); j++){
                db4.setElemDouble(i, j, j);
            }
        }
        offsets = db4.getOffsets();
        for(int i = 0; i < db4.getNumBanks(); i++){
            data = db4.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db5.getNumBanks(); i++){
            for(int j = 0; j < db5.getSize(); j++){
                db5.setElemDouble(i, j, j);
            }
        }
        offsets = db5.getOffsets();
        for(int i = 0; i < db5.getNumBanks(); i++){
            data = db5.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

        for(int i = 0; i < db6.getNumBanks(); i++){
            for(int j = 0; j < db6.getSize(); j++){
                db6.setElemDouble(i, j, j);
            }
        }
        offsets = db6.getOffsets();
        for(int i = 0; i < db6.getNumBanks(); i++){
            data = db6.getData(i);
            for(int j = 0; j < size; j++){
                assertEquals(j, data[offsets[i] + j], 0);
            }
        }

    }


    public final void testSetElemDouble(){
        double data[];
        int offset;

        for(int i = 0; i < size; i++){
            db1.setElemDouble(i, i);
        }
        data = db1.getData();
        offset = db1.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db2.setElemDouble(i, i);
        }
        data = db2.getData();
        offset = db2.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db3.setElemDouble(i, i);
        }
        data = db3.getData();
        offset = db3.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db4.setElemDouble(i, i);
        }
        data = db4.getData();
        offset = db4.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db5.setElemDouble(i, i);
        }
        data = db5.getData();
        offset = db5.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }

        for(int i = 0; i < size; i++){
            db6.setElemDouble(i, i);
        }
        data = db6.getData();
        offset = db6.getOffset();
        for(int i = 0; i < size; i++){
            assertEquals(i, data[offset + i], 0);
        }
    }

    public final void testGetElemDoubleBank(){
        double data[];
        int offsets[];

        offsets = db1.getOffsets();
        for(int i = 0; i < db1.getNumBanks(); i++){
            data = db1.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db1.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db1.getElemDouble(i, j), 0);
            }
        }

        offsets = db2.getOffsets();
        for(int i = 0; i < db2.getNumBanks(); i++){
            data = db2.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db2.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db2.getElemDouble(i, j), 0);
            }
        }

        offsets = db3.getOffsets();
        for(int i = 0; i < db3.getNumBanks(); i++){
            data = db3.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db3.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db3.getElemDouble(i, j), 0);
            }
        }

        offsets = db4.getOffsets();
        for(int i = 0; i < db4.getNumBanks(); i++){
            data = db4.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db4.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db4.getElemDouble(i, j), 0);
            }
        }

        offsets = db5.getOffsets();
        for(int i = 0; i < db5.getNumBanks(); i++){
            data = db5.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db5.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db5.getElemDouble(i, j), 0);
            }
        }

        offsets = db6.getOffsets();
        for(int i = 0; i < db6.getNumBanks(); i++){
            data = db6.getData(i);
            for(int j = 0; j < size; j++){
                data[offsets[i] + j] = j;
            }
        }
        for(int i = 0; i < db6.getNumBanks(); i++){
            for(int j = 0; j < size; j++){
                assertEquals(j, db6.getElemDouble(i, j), 0);
            }
        }
    }

    public final void testGetElemDouble(){
        double data[];
        int offset;

        data = db1.getData();
        offset = db1.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db1.getElemDouble(i), 0);
        }

        data = db2.getData();
        offset = db2.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db2.getElemDouble(i), 0);
        }

        data = db3.getData();
        offset = db3.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db3.getElemDouble(i), 0);
        }

        data = db4.getData();
        offset = db4.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db4.getElemDouble(i), 0);
        }

        data = db5.getData();
        offset = db5.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db5.getElemDouble(i), 0);
        }

        data = db6.getData();
        offset = db6.getOffset();
        for(int i = 0; i < size; i++){
            data[offset + i] = i;
        }
        for(int i = 0; i < size; i++){
            assertEquals(i, db6.getElemDouble(i), 0);
        }
    }

}
