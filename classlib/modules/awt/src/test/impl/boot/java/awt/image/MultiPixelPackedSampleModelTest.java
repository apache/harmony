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
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JSplitPane;

import junit.framework.TestCase;

public class MultiPixelPackedSampleModelTest extends TestCase {
    final int w = 20;

    final int h = 20;

    final int bPixelBits = 2;

    final int sPixelBits = 4;

    final int iPixelBits = 8;

    final int bDataBitOffset = bPixelBits;

    final int sDataBitOffset = sPixelBits;

    final int iDataBitOffset = iPixelBits;

    int scanlineStride = 6;

    byte byteTestData[];

    short shortTestData[];

    int intTestData[];

    DataBufferByte dbb1, dbb2;

    DataBufferUShort dbu1, dbu2;

    DataBufferInt dbi1, dbi2;

    MultiPixelPackedSampleModel mppsmb1, mppsmb2, mppsmu1, mppsmu2, mppsmi1,
            mppsmi2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        dbb1 = new DataBufferByte(
                w
                        / (DataBuffer.getDataTypeSize(DataBuffer.TYPE_BYTE) / bPixelBits)
                        * h);
        dbb2 = new DataBufferByte(scanlineStride * h + (bDataBitOffset + 7) / 8);
        dbu1 = new DataBufferUShort(
                w
                        / (DataBuffer.getDataTypeSize(DataBuffer.TYPE_USHORT) / sPixelBits)
                        * h);
        dbu2 = new DataBufferUShort(scanlineStride * h + (sDataBitOffset + 15)
                / 16);
        dbi1 = new DataBufferInt(
                w
                        / (DataBuffer.getDataTypeSize(DataBuffer.TYPE_INT) / iPixelBits)
                        * h);
        dbi2 = new DataBufferInt(scanlineStride * h + (iDataBitOffset + 31)
                / 32);

        mppsmb1 = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h,
                bPixelBits);
        mppsmb2 = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h,
                bPixelBits, scanlineStride, bDataBitOffset);
        mppsmu1 = new MultiPixelPackedSampleModel(DataBuffer.TYPE_USHORT, w, h,
                sPixelBits);
        mppsmu2 = new MultiPixelPackedSampleModel(DataBuffer.TYPE_USHORT, w, h,
                sPixelBits, scanlineStride, sDataBitOffset);
        mppsmi1 = new MultiPixelPackedSampleModel(DataBuffer.TYPE_INT, w, h,
                iPixelBits);
        mppsmi2 = new MultiPixelPackedSampleModel(DataBuffer.TYPE_INT, w, h,
                iPixelBits, scanlineStride, iDataBitOffset);

        //      initTestData();
    }

    public MultiPixelPackedSampleModelTest(String name) {
        super(name);
    }

    public final void testGetDataType() {
        assertEquals(DataBuffer.TYPE_BYTE, mppsmb1.getDataType());
        assertEquals(DataBuffer.TYPE_BYTE, mppsmb2.getDataType());
        assertEquals(DataBuffer.TYPE_USHORT, mppsmu1.getDataType());
        assertEquals(DataBuffer.TYPE_USHORT, mppsmu2.getDataType());
        assertEquals(DataBuffer.TYPE_INT, mppsmi1.getDataType());
        assertEquals(DataBuffer.TYPE_INT, mppsmi2.getDataType());
    }

    public final void testGetNumBands() {
        assertEquals(1, mppsmb1.getNumBands());
        assertEquals(1, mppsmb2.getNumBands());
        assertEquals(1, mppsmu1.getNumBands());
        assertEquals(1, mppsmu2.getNumBands());
        assertEquals(1, mppsmi1.getNumBands());
        assertEquals(1, mppsmi2.getNumBands());
    }

    public final void testGetWidth() {
        assertEquals(w, mppsmb1.getWidth());
        assertEquals(w, mppsmb2.getWidth());
        assertEquals(w, mppsmu1.getWidth());
        assertEquals(w, mppsmu2.getWidth());
        assertEquals(w, mppsmi1.getWidth());
        assertEquals(w, mppsmi2.getWidth());
    }

    public final void testGetHeight() {
        assertEquals(h, mppsmb1.getHeight());
        assertEquals(h, mppsmb2.getHeight());
        assertEquals(h, mppsmu1.getHeight());
        assertEquals(h, mppsmu2.getHeight());
        assertEquals(h, mppsmi1.getHeight());
        assertEquals(h, mppsmi2.getHeight());
    }

    public final void testGetDataBitOffset() {
        assertEquals(0, mppsmb1.getDataBitOffset());
        assertEquals(bDataBitOffset, mppsmb2.getDataBitOffset());
        assertEquals(0, mppsmu1.getDataBitOffset());
        assertEquals(sDataBitOffset, mppsmu2.getDataBitOffset());
        assertEquals(0, mppsmi1.getDataBitOffset());
        assertEquals(iDataBitOffset, mppsmi2.getDataBitOffset());
    }

    public final void testGetNumDataElements() {
        assertEquals(1, mppsmb1.getNumDataElements());
        assertEquals(1, mppsmb2.getNumDataElements());
        assertEquals(1, mppsmu1.getNumDataElements());
        assertEquals(1, mppsmu2.getNumDataElements());
        assertEquals(1, mppsmi1.getNumDataElements());
        assertEquals(1, mppsmi2.getNumDataElements());
    }

    public final void testGetScanlineStride() {
        assertEquals(
                w
                        / (DataBuffer.getDataTypeSize(DataBuffer.TYPE_BYTE) / bPixelBits),
                mppsmb1.getScanlineStride());
        assertEquals(scanlineStride, mppsmb2.getScanlineStride());
        assertEquals(
                w
                        / (DataBuffer.getDataTypeSize(DataBuffer.TYPE_USHORT) / sPixelBits),
                mppsmu1.getScanlineStride());
        assertEquals(scanlineStride, mppsmu2.getScanlineStride());
        assertEquals(
                w
                        / (DataBuffer.getDataTypeSize(DataBuffer.TYPE_INT) / iPixelBits),
                mppsmi1.getScanlineStride());
        assertEquals(scanlineStride, mppsmi2.getScanlineStride());
    }

    public final void testGetTransferType() {
        assertEquals(DataBuffer.TYPE_BYTE, mppsmb1.getTransferType());
        assertEquals(DataBuffer.TYPE_BYTE, mppsmb2.getTransferType());
        assertEquals(DataBuffer.TYPE_BYTE, mppsmu1.getTransferType());
        assertEquals(DataBuffer.TYPE_BYTE, mppsmu2.getTransferType());
        assertEquals(DataBuffer.TYPE_BYTE, mppsmi1.getTransferType());
        assertEquals(DataBuffer.TYPE_BYTE, mppsmi2.getTransferType());
    }

    public final void testGetPixelBitStride() {
        assertEquals(bPixelBits, mppsmb1.getPixelBitStride());
        assertEquals(bPixelBits, mppsmb2.getPixelBitStride());
        assertEquals(sPixelBits, mppsmu1.getPixelBitStride());
        assertEquals(sPixelBits, mppsmu2.getPixelBitStride());
        assertEquals(iPixelBits, mppsmi1.getPixelBitStride());
        assertEquals(iPixelBits, mppsmi2.getPixelBitStride());
    }

    public final void testGetSampleSize() {
        int samplesSize[];
        int standard[] = new int[1];

        standard[0] = bPixelBits;
        samplesSize = mppsmb1.getSampleSize();
        assertTrue(Arrays.equals(samplesSize, standard));
        samplesSize = mppsmb2.getSampleSize();
        assertTrue(Arrays.equals(samplesSize, standard));

        standard[0] = sPixelBits;
        samplesSize = mppsmu1.getSampleSize();
        assertTrue(Arrays.equals(samplesSize, standard));
        samplesSize = mppsmu2.getSampleSize();
        assertTrue(Arrays.equals(samplesSize, standard));

        standard[0] = iPixelBits;
        samplesSize = mppsmi1.getSampleSize();
        assertTrue(Arrays.equals(samplesSize, standard));
        samplesSize = mppsmi2.getSampleSize();
        assertTrue(Arrays.equals(samplesSize, standard));
    }

    public final void testGetSampleSizeBand() {
        for (int b = 0; b < mppsmb1.getNumBands(); b++) {
            assertEquals(bPixelBits, mppsmb1.getSampleSize(b));
        }

        for (int b = 0; b < mppsmb2.getNumBands(); b++) {
            assertEquals(bPixelBits, mppsmb2.getSampleSize(b));
        }

        for (int b = 0; b < mppsmu1.getNumBands(); b++) {
            assertEquals(sPixelBits, mppsmu1.getSampleSize(b));
        }

        for (int b = 0; b < mppsmu2.getNumBands(); b++) {
            assertEquals(sPixelBits, mppsmu2.getSampleSize(b));
        }

        for (int b = 0; b < mppsmi1.getNumBands(); b++) {
            assertEquals(iPixelBits, mppsmi1.getSampleSize(b));
        }

        for (int b = 0; b < mppsmi2.getNumBands(); b++) {
            assertEquals(iPixelBits, mppsmi2.getSampleSize(b));
        }
    }

    public final void testEquals() {
        SampleModel sm;

        sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h,
                bPixelBits);
        assertTrue(mppsmb1.equals(sm));
        assertFalse(mppsmb2.equals(sm));

        sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h,
                bPixelBits, scanlineStride, bDataBitOffset);
        assertTrue(mppsmb2.equals(sm));
        assertFalse(mppsmb1.equals(sm));

        sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_USHORT, w, h,
                sPixelBits);
        assertTrue(mppsmu1.equals(sm));
        assertFalse(mppsmu2.equals(sm));

        sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_USHORT, w, h,
                sPixelBits, scanlineStride, sDataBitOffset);
        assertTrue(mppsmu2.equals(sm));
        assertFalse(mppsmu1.equals(sm));

        sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_INT, w, h,
                iPixelBits);
        assertTrue(mppsmi1.equals(sm));
        assertFalse(mppsmi2.equals(sm));

        sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_INT, w, h,
                iPixelBits, scanlineStride, iDataBitOffset);
        assertTrue(mppsmi2.equals(sm));
        assertFalse(mppsmi1.equals(sm));

    }

    public final void testCreateCompatibleSampleModel() {
        SampleModel sm;
        MultiPixelPackedSampleModel mppsm;

        sm = mppsmb1.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof MultiPixelPackedSampleModel);
        mppsm = (MultiPixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_BYTE, mppsm.getDataType());
        assertEquals(w, mppsm.getWidth());
        assertEquals(h, mppsm.getHeight());
        assertEquals(bPixelBits, mppsm.getPixelBitStride());

        sm = mppsmb2.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof MultiPixelPackedSampleModel);
        mppsm = (MultiPixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_BYTE, mppsm.getDataType());
        assertEquals(w, mppsm.getWidth());
        assertEquals(h, mppsm.getHeight());
        assertEquals(bPixelBits, mppsm.getPixelBitStride());

        sm = mppsmu1.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof MultiPixelPackedSampleModel);
        mppsm = (MultiPixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_USHORT, mppsm.getDataType());
        assertEquals(w, mppsm.getWidth());
        assertEquals(h, mppsm.getHeight());
        assertEquals(sPixelBits, mppsm.getPixelBitStride());

        sm = mppsmu2.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof MultiPixelPackedSampleModel);
        mppsm = (MultiPixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_USHORT, mppsm.getDataType());
        assertEquals(w, mppsm.getWidth());
        assertEquals(h, mppsm.getHeight());
        assertEquals(sPixelBits, mppsm.getPixelBitStride());

        sm = mppsmi1.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof MultiPixelPackedSampleModel);
        mppsm = (MultiPixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_INT, mppsm.getDataType());
        assertEquals(w, mppsm.getWidth());
        assertEquals(h, mppsm.getHeight());
        assertEquals(iPixelBits, mppsm.getPixelBitStride());

        sm = mppsmi2.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof MultiPixelPackedSampleModel);
        mppsm = (MultiPixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_INT, mppsm.getDataType());
        assertEquals(w, mppsm.getWidth());
        assertEquals(h, mppsm.getHeight());
        assertEquals(iPixelBits, mppsm.getPixelBitStride());

    }

    public final void testCreateSubsetSampleModel() {
        int bands[] = { 0 };
        int wrongBands[] = { 0, 1 };

        try {
            mppsmb1.createSubsetSampleModel(null);
        } catch (RasterFormatException e) {
            fail();
        }

        try {
            mppsmb1.createSubsetSampleModel(bands);
        } catch (RasterFormatException e) {
            fail();
        }

        try {
            mppsmb1.createSubsetSampleModel(wrongBands);
        } catch (RasterFormatException e) {
            assertTrue(true);
        }
    }

    public final void testCreateDataBuffer() {
        DataBuffer db;

        db = mppsmb1.createDataBuffer();
        assertEquals(dbb1.getDataType(), db.getDataType());
        assertEquals(dbb1.getNumBanks(), db.getNumBanks());
        assertEquals(dbb1.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbb1.getOffsets(), db.getOffsets()));

        db = mppsmb2.createDataBuffer();
        assertEquals(dbb2.getDataType(), db.getDataType());
        assertEquals(dbb2.getNumBanks(), db.getNumBanks());
        assertEquals(dbb2.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbb2.getOffsets(), db.getOffsets()));

        db = mppsmu1.createDataBuffer();
        assertEquals(dbu1.getDataType(), db.getDataType());
        assertEquals(dbu1.getNumBanks(), db.getNumBanks());
        assertEquals(dbu1.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbu1.getOffsets(), db.getOffsets()));

        db = mppsmu2.createDataBuffer();
        assertEquals(dbu2.getDataType(), db.getDataType());
        assertEquals(dbu2.getNumBanks(), db.getNumBanks());
        assertEquals(dbu2.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbu2.getOffsets(), db.getOffsets()));

        db = mppsmi1.createDataBuffer();
        assertEquals(dbi1.getDataType(), db.getDataType());
        assertEquals(dbi1.getNumBanks(), db.getNumBanks());
        assertEquals(dbi1.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbi1.getOffsets(), db.getOffsets()));

        db = mppsmi2.createDataBuffer();
        assertEquals(dbi2.getDataType(), db.getDataType());
        assertEquals(dbi2.getNumBanks(), db.getNumBanks());
        assertEquals(dbi2.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbi2.getOffsets(), db.getOffsets()));
    }

    public final void testGetBitOffset() {
        int pixelStride;
        int bitOffset;
        int offset;
        int type;
        int dataElemSize;

        pixelStride = mppsmb1.getPixelBitStride();
        bitOffset = mppsmb1.getDataBitOffset();
        type = mppsmb1.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int x = 0; x < w; x++) {
            offset = (x * pixelStride + bitOffset) & (dataElemSize - 1);
            assertEquals(offset, mppsmb1.getBitOffset(x));
        }

        pixelStride = mppsmb2.getPixelBitStride();
        bitOffset = mppsmb2.getDataBitOffset();
        type = mppsmb2.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int x = 0; x < w; x++) {
            offset = (x * pixelStride + bitOffset) & (dataElemSize - 1);
            assertEquals(offset, mppsmb2.getBitOffset(x));
        }

        pixelStride = mppsmu1.getPixelBitStride();
        bitOffset = mppsmu1.getDataBitOffset();
        type = mppsmu1.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int x = 0; x < w; x++) {
            offset = (x * pixelStride + bitOffset) & (dataElemSize - 1);
            assertEquals(offset, mppsmu1.getBitOffset(x));
        }

        pixelStride = mppsmu2.getPixelBitStride();
        bitOffset = mppsmu2.getDataBitOffset();
        type = mppsmu2.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int x = 0; x < w; x++) {
            offset = (x * pixelStride + bitOffset) & (dataElemSize - 1);
            assertEquals(offset, mppsmu2.getBitOffset(x));
        }

        pixelStride = mppsmi1.getPixelBitStride();
        bitOffset = mppsmi1.getDataBitOffset();
        type = mppsmi1.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int x = 0; x < w; x++) {
            offset = (x * pixelStride + bitOffset) & (dataElemSize - 1);
            assertEquals(offset, mppsmi1.getBitOffset(x));
        }

        pixelStride = mppsmi2.getPixelBitStride();
        bitOffset = mppsmi2.getDataBitOffset();
        type = mppsmi2.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int x = 0; x < w; x++) {
            offset = (x * pixelStride + bitOffset) & (dataElemSize - 1);
            assertEquals(offset, mppsmi2.getBitOffset(x));
        }
    }

    public final void testGetOffset() {
        int stride;
        int pixelStride;
        int bitOffset;
        int offset;
        int type;
        int dataElemSize;

        stride = mppsmb1.getScanlineStride();
        pixelStride = mppsmb1.getPixelBitStride();
        bitOffset = mppsmb1.getDataBitOffset();
        type = mppsmb1.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                offset = y * stride + (x * pixelStride + bitOffset)
                        / dataElemSize;
                assertEquals(offset, mppsmb1.getOffset(x, y));
            }
        }

        stride = mppsmb2.getScanlineStride();
        pixelStride = mppsmb2.getPixelBitStride();
        bitOffset = mppsmb2.getDataBitOffset();
        type = mppsmb2.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                offset = y * stride + (x * pixelStride + bitOffset)
                        / dataElemSize;
                assertEquals(offset, mppsmb2.getOffset(x, y));
            }
        }

        stride = mppsmu1.getScanlineStride();
        pixelStride = mppsmu1.getPixelBitStride();
        bitOffset = mppsmu1.getDataBitOffset();
        type = mppsmu1.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                offset = y * stride + (x * pixelStride + bitOffset)
                        / dataElemSize;
                assertEquals(offset, mppsmu1.getOffset(x, y));
            }
        }

        stride = mppsmu2.getScanlineStride();
        pixelStride = mppsmu2.getPixelBitStride();
        bitOffset = mppsmu2.getDataBitOffset();
        type = mppsmu2.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                offset = y * stride + (x * pixelStride + bitOffset)
                        / dataElemSize;
                assertEquals(offset, mppsmu2.getOffset(x, y));
            }
        }

        stride = mppsmi1.getScanlineStride();
        pixelStride = mppsmi1.getPixelBitStride();
        bitOffset = mppsmi1.getDataBitOffset();
        type = mppsmi1.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                offset = y * stride + (x * pixelStride + bitOffset)
                        / dataElemSize;
                assertEquals(offset, mppsmi1.getOffset(x, y));
            }
        }

        stride = mppsmi2.getScanlineStride();
        pixelStride = mppsmi2.getPixelBitStride();
        bitOffset = mppsmi2.getDataBitOffset();
        type = mppsmi2.getDataType();
        dataElemSize = DataBuffer.getDataTypeSize(type);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                offset = y * stride + (x * pixelStride + bitOffset)
                        / dataElemSize;
                assertEquals(offset, mppsmi2.getOffset(x, y));
            }
        }
    }

    public final void testGetDataElements() {
        initDataBuffers();
        byte bde[] = new byte[1];
        short sde[] = new short[1];
        int ide[] = new int[1];
        Object de;
        int trType;
        int idx;

        idx = 0;
        trType = mppsmb1.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                de = null;
                de = mppsmb1.getDataElements(x, y, de, dbb1);
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    bde = (byte[]) de;
                    assertEquals(1, bde.length);
                    assertEquals(byteTestData[idx++] & 0xff, bde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_USHORT:
                    sde = (short[]) de;
                    assertEquals(1, sde.length);
                    assertEquals(byteTestData[idx++] & 0xff, sde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_INT:
                    ide = (int[]) de;
                    assertEquals(1, ide.length);
                    assertEquals(byteTestData[idx++] & 0xff, ide[0]);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong TransferType");
                }
            }
        }

        idx = 0;
        trType = mppsmb2.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                de = null;
                de = mppsmb2.getDataElements(x, y, de, dbb2);
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    bde = (byte[]) de;
                    assertEquals(1, bde.length);
                    assertEquals(byteTestData[idx++] & 0xff, bde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_USHORT:
                    sde = (short[]) de;
                    assertEquals(1, sde.length);
                    assertEquals(byteTestData[idx++] & 0xff, sde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_INT:
                    ide = (int[]) de;
                    assertEquals(1, ide.length);
                    assertEquals(byteTestData[idx++] & 0xff, ide[0]);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong TransferType");
                }
            }
        }

        idx = 0;
        trType = mppsmu1.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                de = null;
                de = mppsmu1.getDataElements(x, y, de, dbu1);
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    bde = (byte[]) de;
                    assertEquals(1, bde.length);
                    assertEquals(shortTestData[idx++] & 0xffff, bde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_USHORT:
                    sde = (short[]) de;
                    assertEquals(1, sde.length);
                    assertEquals(shortTestData[idx++] & 0xffff, sde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_INT:
                    ide = (int[]) de;
                    assertEquals(1, ide.length);
                    assertEquals(shortTestData[idx++] & 0xffff, ide[0]);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong TransferType");
                }
            }
        }

        idx = 0;
        trType = mppsmu2.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                de = null;
                de = mppsmu2.getDataElements(x, y, de, dbu2);
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    bde = (byte[]) de;
                    assertEquals(1, bde.length);
                    assertEquals(shortTestData[idx++] & 0xffff, bde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_USHORT:
                    sde = (short[]) de;
                    assertEquals(1, sde.length);
                    assertEquals(shortTestData[idx++] & 0xffff, sde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_INT:
                    ide = (int[]) de;
                    assertEquals(1, ide.length);
                    assertEquals(shortTestData[idx++], ide[0]);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong TransferType");
                }
            }
        }

        idx = 0;
        trType = mppsmi1.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                de = null;
                de = mppsmi1.getDataElements(x, y, de, dbi1);
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    bde = (byte[]) de;
                    assertEquals(1, bde.length);
                    assertEquals(intTestData[idx++], bde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_USHORT:
                    sde = (short[]) de;
                    assertEquals(1, sde.length);
                    assertEquals(intTestData[idx++], sde[0] & 0xffff);
                    break;
                case DataBuffer.TYPE_INT:
                    ide = (int[]) de;
                    assertEquals(1, ide.length);
                    assertEquals(intTestData[idx++], ide[0]);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong TransferType");
                }
            }
        }

        idx = 0;
        trType = mppsmi2.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                de = null;
                de = mppsmi2.getDataElements(x, y, de, dbi2);
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    bde = (byte[]) de;
                    assertEquals(1, bde.length);
                    assertEquals(intTestData[idx++], bde[0] & 0xff);
                    break;
                case DataBuffer.TYPE_USHORT:
                    sde = (short[]) de;
                    assertEquals(1, sde.length);
                    assertEquals(intTestData[idx++], sde[0] & 0xffff);
                    break;
                case DataBuffer.TYPE_INT:
                    ide = (int[]) de;
                    assertEquals(1, ide.length);
                    assertEquals(intTestData[idx++], ide[0]);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong TransferType");
                }
            }
        }
    }

    public final void testGetDataElementsA() {
        initDataBuffers();
        Object tDE, rDE;
        byte tde[];
        byte rde[];
        short stde[];
        short srde[];
        int itde[];
        int irde[];
        int trType;

        trType = mppsmb1.getTransferType();
        tDE = null;
        tDE = mppsmb1.getDataElements(0, 0, 1, 1, tDE, dbb1);
        rDE = getDataElementsB(0, 0, 1, 1, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmb1.getDataElements(0, 0, w / 2, h / 2, tDE, dbb1);
        rDE = getDataElementsB(0, 0, w / 2, w / 2, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmb1.getDataElements(0, 0, w, h, tDE, dbb1);
        rDE = getDataElementsB(0, 0, w, h, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        trType = mppsmb2.getTransferType();
        tDE = null;
        tDE = mppsmb2.getDataElements(0, 0, 1, 1, tDE, dbb2);
        rDE = getDataElementsB(0, 0, 1, 1, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmb2.getDataElements(0, 0, w / 2, h / 2, tDE, dbb2);
        rDE = getDataElementsB(0, 0, w / 2, w / 2, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmb2.getDataElements(0, 0, w, h, tDE, dbb2);
        rDE = getDataElementsB(0, 0, w, h, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        trType = mppsmu1.getTransferType();
        tDE = null;
        tDE = mppsmu1.getDataElements(0, 0, 1, 1, tDE, dbu1);
        rDE = getDataElementsUS(0, 0, 1, 1, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmu1.getDataElements(0, 0, w / 2, h / 2, tDE, dbu1);
        rDE = getDataElementsUS(0, 0, w / 2, w / 2, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmu1.getDataElements(0, 0, w, h, tDE, dbu1);
        rDE = getDataElementsUS(0, 0, w, h, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        trType = mppsmu2.getTransferType();
        tDE = null;
        tDE = mppsmu2.getDataElements(0, 0, 1, 1, tDE, dbu2);
        rDE = getDataElementsUS(0, 0, 1, 1, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmu2.getDataElements(0, 0, w / 2, h / 2, tDE, dbu2);
        rDE = getDataElementsUS(0, 0, w / 2, w / 2, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmu2.getDataElements(0, 0, w, h, tDE, dbu2);
        rDE = getDataElementsUS(0, 0, w, h, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        trType = mppsmi1.getTransferType();
        tDE = null;
        tDE = mppsmi1.getDataElements(0, 0, 1, 1, tDE, dbi1);
        rDE = getDataElementsI(0, 0, 1, 1, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmi1.getDataElements(0, 0, w / 2, h / 2, tDE, dbi1);
        rDE = getDataElementsI(0, 0, w / 2, w / 2, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmi1.getDataElements(0, 0, w, h, tDE, dbi1);
        rDE = getDataElementsI(0, 0, w, h, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        trType = mppsmi2.getTransferType();
        tDE = null;
        tDE = mppsmi2.getDataElements(0, 0, 1, 1, tDE, dbi2);
        rDE = getDataElementsI(0, 0, 1, 1, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmi2.getDataElements(0, 0, w / 2, h / 2, tDE, dbi2);
        rDE = getDataElementsI(0, 0, w / 2, w / 2, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }

        tDE = null;
        tDE = mppsmi2.getDataElements(0, 0, w, h, tDE, dbi2);
        rDE = getDataElementsI(0, 0, w, h, trType);
        switch (trType) {
        case DataBuffer.TYPE_BYTE:
            tde = (byte[]) tDE;
            rde = (byte[]) rDE;
            assertTrue(Arrays.equals(rde, tde));
            break;
        case DataBuffer.TYPE_USHORT:
            stde = (short[]) tDE;
            srde = (short[]) rDE;
            assertTrue(Arrays.equals(srde, stde));
            break;
        case DataBuffer.TYPE_INT:
            itde = (int[]) tDE;
            irde = (int[]) rDE;
            assertTrue(Arrays.equals(irde, itde));
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }
    }

    public final void testGetPixel() {
        initDataBuffers();

        int pixel[];
        int idx;

        pixel = null;
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel = mppsmb1.getPixel(x, y, pixel, dbb1);
                assertEquals(byteTestData[idx++] & 0xff, pixel[0]);
            }
        }

        pixel = null;
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel = mppsmb2.getPixel(x, y, pixel, dbb2);
                assertEquals(byteTestData[idx++] & 0xff, pixel[0]);
            }
        }

        pixel = null;
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel = mppsmu1.getPixel(x, y, pixel, dbu1);
                assertEquals(shortTestData[idx++] & 0xffff, pixel[0]);
            }
        }

        pixel = null;
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel = mppsmu2.getPixel(x, y, pixel, dbu2);
                assertEquals(shortTestData[idx++] & 0xffff, pixel[0]);
            }
        }

        pixel = null;
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel = mppsmi1.getPixel(x, y, pixel, dbi1);
                assertEquals(intTestData[idx++], pixel[0]);
            }
        }

        pixel = null;
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel = mppsmi2.getPixel(x, y, pixel, dbi2);
                assertEquals(intTestData[idx++], pixel[0]);
            }
        }
    }

    public final void testGetPixels() {
        initDataBuffers();
        int tpixel[] = null;
        int rpixel[];

        tpixel = mppsmb1.getPixels(0, 0, 1, 1, tpixel, dbb1);
        rpixel = getPixelsB(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmb1.getPixels(0, 0, w / 2, h / 2, tpixel, dbb1);
        rpixel = getPixelsB(0, 0, w / 2, h / 2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmb1.getPixels(0, 0, w, h, tpixel, dbb1);
        rpixel = getPixelsB(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmb2.getPixels(0, 0, 1, 1, tpixel, dbb2);
        rpixel = getPixelsB(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmb2.getPixels(0, 0, w / 2, h / 2, tpixel, dbb2);
        rpixel = getPixelsB(0, 0, w / 2, h / 2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmb2.getPixels(0, 0, w, h, tpixel, dbb2);
        rpixel = getPixelsB(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmu1.getPixels(0, 0, 1, 1, tpixel, dbu1);
        rpixel = getPixelsUS(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmu1.getPixels(0, 0, w / 2, h / 2, tpixel, dbu1);
        rpixel = getPixelsUS(0, 0, w / 2, h / 2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmu1.getPixels(0, 0, w, h, tpixel, dbu1);
        rpixel = getPixelsUS(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmu2.getPixels(0, 0, 1, 1, tpixel, dbu2);
        rpixel = getPixelsUS(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmu2.getPixels(0, 0, w / 2, h / 2, tpixel, dbu2);
        rpixel = getPixelsUS(0, 0, w / 2, h / 2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmu2.getPixels(0, 0, w, h, tpixel, dbu2);
        rpixel = getPixelsUS(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmi1.getPixels(0, 0, 1, 1, tpixel, dbi1);
        rpixel = getPixelsI(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmi1.getPixels(0, 0, w / 2, h / 2, tpixel, dbi1);
        rpixel = getPixelsI(0, 0, w / 2, h / 2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmi1.getPixels(0, 0, w, h, tpixel, dbi1);
        rpixel = getPixelsI(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmi2.getPixels(0, 0, 1, 1, tpixel, dbi2);
        rpixel = getPixelsI(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmi2.getPixels(0, 0, w / 2, h / 2, tpixel, dbi2);
        rpixel = getPixelsI(0, 0, w / 2, h / 2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = mppsmi2.getPixels(0, 0, w, h, tpixel, dbi2);
        rpixel = getPixelsI(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));
    }

    public final void testGetSample() {
        initDataBuffers();
        int sample;
        int idx;

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = mppsmb1.getSample(x, y, 0, dbb1);
                assertEquals(byteTestData[idx++] & 0xff, sample);
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = mppsmb2.getSample(x, y, 0, dbb2);
                assertEquals(byteTestData[idx++] & 0xff, sample);
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = mppsmu1.getSample(x, y, 0, dbu1);
                assertEquals(shortTestData[idx++] & 0xffff, sample);
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = mppsmu2.getSample(x, y, 0, dbu2);
                assertEquals(shortTestData[idx++] & 0xffff, sample);
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = mppsmi1.getSample(x, y, 0, dbi1);
                assertEquals(intTestData[idx++], sample);
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = mppsmi2.getSample(x, y, 0, dbi2);
                assertEquals(intTestData[idx++], sample);
            }
        }
    }

    public final void testGetSamples() {
        initDataBuffers();
        int rsamples[];
        int tsamples[];

        for (int b = 0; b < mppsmb1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmb1.getSamples(0, 0, 1, 1, b, tsamples, dbb1);
            rsamples = getSamplesB(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmb1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmb1
                    .getSamples(0, 0, w / 2, h / 2, b, tsamples, dbb1);
            rsamples = getSamplesB(0, 0, w / 2, h / 2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmb1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmb1.getSamples(0, 0, w, h, b, tsamples, dbb1);
            rsamples = getSamplesB(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmb2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmb2.getSamples(0, 0, 1, 1, b, tsamples, dbb2);
            rsamples = getSamplesB(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmb2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmb2
                    .getSamples(0, 0, w / 2, h / 2, b, tsamples, dbb2);
            rsamples = getSamplesB(0, 0, w / 2, h / 2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmb2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmb2.getSamples(0, 0, w, h, b, tsamples, dbb2);
            rsamples = getSamplesB(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmu1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmu1.getSamples(0, 0, 1, 1, b, tsamples, dbu1);
            rsamples = getSamplesUS(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmu1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmu1
                    .getSamples(0, 0, w / 2, h / 2, b, tsamples, dbu1);
            rsamples = getSamplesUS(0, 0, w / 2, h / 2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmu1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmu1.getSamples(0, 0, w, h, b, tsamples, dbu1);
            rsamples = getSamplesUS(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmu2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmu2.getSamples(0, 0, 1, 1, b, tsamples, dbu2);
            rsamples = getSamplesUS(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmu2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmu2
                    .getSamples(0, 0, w / 2, h / 2, b, tsamples, dbu2);
            rsamples = getSamplesUS(0, 0, w / 2, h / 2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmu2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmu2.getSamples(0, 0, w, h, b, tsamples, dbu2);
            rsamples = getSamplesUS(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmi1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmi1.getSamples(0, 0, 1, 1, b, tsamples, dbi1);
            rsamples = getSamplesI(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmi1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmi1
                    .getSamples(0, 0, w / 2, h / 2, b, tsamples, dbi1);
            rsamples = getSamplesI(0, 0, w / 2, h / 2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmi1.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmi1.getSamples(0, 0, w, h, b, tsamples, dbi1);
            rsamples = getSamplesI(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmi2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmi2.getSamples(0, 0, 1, 1, b, tsamples, dbi2);
            rsamples = getSamplesI(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmi2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmi2
                    .getSamples(0, 0, w / 2, h / 2, b, tsamples, dbi2);
            rsamples = getSamplesI(0, 0, w / 2, h / 2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for (int b = 0; b < mppsmi2.getNumBands(); b++) {
            tsamples = null;
            tsamples = mppsmi2.getSamples(0, 0, w, h, b, tsamples, dbi2);
            rsamples = getSamplesI(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

    }

    public final void testSetDataElements() {
        initTestData();
        int trType;
        int idx;
        int pixelStride;
        int stride;
        int bitOffset;

        idx = 0;
        trType = mppsmb1.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Object de;
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    byte bde[] = new byte[1];
                    bde[0] = byteTestData[idx++];
                    de = bde;
                    break;
                case DataBuffer.TYPE_USHORT:
                    short sde[] = new short[1];
                    sde[0] = (short) (byteTestData[idx++] & 0xff);
                    de = sde;
                    break;
                case DataBuffer.TYPE_INT:
                    int ide[] = new int[1];
                    ide[0] = byteTestData[idx++] & 0xff;
                    de = ide;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong Transfer Type: "
                            + trType);
                }
                mppsmb1.setDataElements(x, y, de, dbb1);
            }
        }
        idx = 0;
        pixelStride = mppsmb1.getPixelBitStride();
        stride = mppsmb1.getScanlineStride();
        bitOffset = mppsmb1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[idx++] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb1));

            }
        }

        idx = 0;
        trType = mppsmb2.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Object de;
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    byte bde[] = new byte[1];
                    bde[0] = byteTestData[idx++];
                    de = bde;
                    break;
                case DataBuffer.TYPE_USHORT:
                    short sde[] = new short[1];
                    sde[0] = (short) (byteTestData[idx++] & 0xff);
                    de = sde;
                    break;
                case DataBuffer.TYPE_INT:
                    int ide[] = new int[1];
                    ide[0] = byteTestData[idx++] & 0xff;
                    de = ide;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong Transfer Type: "
                            + trType);
                }
                mppsmb2.setDataElements(x, y, de, dbb2);
            }
        }
        idx = 0;
        pixelStride = mppsmb2.getPixelBitStride();
        stride = mppsmb2.getScanlineStride();
        bitOffset = mppsmb2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[idx++] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb2));

            }
        }

        idx = 0;
        trType = mppsmu1.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Object de;
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    byte bde[] = new byte[1];
                    bde[0] = (byte) shortTestData[idx++];
                    de = bde;
                    break;
                case DataBuffer.TYPE_USHORT:
                    short sde[] = new short[1];
                    sde[0] = shortTestData[idx++];
                    de = sde;
                    break;
                case DataBuffer.TYPE_INT:
                    int ide[] = new int[1];
                    ide[0] = shortTestData[idx++] & 0xffff;
                    de = ide;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong Transfer Type: "
                            + trType);
                }
                mppsmu1.setDataElements(x, y, de, dbu1);
            }
        }
        idx = 0;
        pixelStride = mppsmu1.getPixelBitStride();
        stride = mppsmu1.getScanlineStride();
        bitOffset = mppsmu1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[idx++] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu1));

            }
        }

        idx = 0;
        trType = mppsmu2.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Object de;
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    byte bde[] = new byte[1];
                    bde[0] = (byte) shortTestData[idx++];
                    de = bde;
                    break;
                case DataBuffer.TYPE_USHORT:
                    short sde[] = new short[1];
                    sde[0] = shortTestData[idx++];
                    de = sde;
                    break;
                case DataBuffer.TYPE_INT:
                    int ide[] = new int[1];
                    ide[0] = shortTestData[idx++] & 0xffff;
                    de = ide;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong Transfer Type: "
                            + trType);
                }
                mppsmu2.setDataElements(x, y, de, dbu2);
            }
        }
        idx = 0;
        pixelStride = mppsmu2.getPixelBitStride();
        stride = mppsmu2.getScanlineStride();
        bitOffset = mppsmu2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[idx++] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu2));

            }
        }

        idx = 0;
        trType = mppsmi1.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Object de;
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    byte bde[] = new byte[1];
                    bde[0] = (byte) intTestData[idx++];
                    de = bde;
                    break;
                case DataBuffer.TYPE_USHORT:
                    short sde[] = new short[1];
                    sde[0] = (short) intTestData[idx++];
                    de = sde;
                    break;
                case DataBuffer.TYPE_INT:
                    int ide[] = new int[1];
                    ide[0] = intTestData[idx++];
                    de = ide;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong Transfer Type: "
                            + trType);
                }
                mppsmi1.setDataElements(x, y, de, dbi1);
            }
        }
        idx = 0;
        pixelStride = mppsmi1.getPixelBitStride();
        stride = mppsmi1.getScanlineStride();
        bitOffset = mppsmi1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbi1));

            }
        }

        idx = 0;
        trType = mppsmi2.getTransferType();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Object de;
                switch (trType) {
                case DataBuffer.TYPE_BYTE:
                    byte bde[] = new byte[1];
                    bde[0] = (byte) intTestData[idx++];
                    de = bde;
                    break;
                case DataBuffer.TYPE_USHORT:
                    short sde[] = new short[1];
                    sde[0] = (short) intTestData[idx++];
                    de = sde;
                    break;
                case DataBuffer.TYPE_INT:
                    int ide[] = new int[1];
                    ide[0] = intTestData[idx++];
                    de = ide;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong Transfer Type: "
                            + trType);
                }
                mppsmi2.setDataElements(x, y, de, dbi2);
            }
        }
        idx = 0;
        pixelStride = mppsmi2.getPixelBitStride();
        stride = mppsmi2.getScanlineStride();
        bitOffset = mppsmi2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbi2));

            }
        }
        
        // Regression for HARMONY-2779
        try {
            new MultiPixelPackedSampleModel(0, 52, 4, 8).setDataElements(6, 2,
                    (Object) null, new DataBufferFloat(4, 1));
            fail("ArrayIndexOutOfBoundsException was not thrown"); //$NON-NLS-1$
        } catch (ArrayIndexOutOfBoundsException ex) {
            // expected
        }
        
        // Regression for HARMONY-2779
        try {
            new MultiPixelPackedSampleModel(0, 14907, 18936, 2)
                    .setDataElements(14, 14, new JSplitPane(),
                            (DataBuffer) null);
            fail("NullPointerException was not thrown"); //$NON-NLS-1$
        } catch (NullPointerException  ex) {
            // expected
        }
    }

    public final void testSetPixel() {
        initTestData();
        int pixel[] = new int[1];
        int idx;
        int pixelStride;
        int stride;
        int bitOffset;

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel[0] = byteTestData[idx++];
                mppsmb1.setPixel(x, y, pixel, dbb1);
            }
        }

        idx = 0;
        pixelStride = mppsmb1.getPixelBitStride();
        stride = mppsmb1.getScanlineStride();
        bitOffset = mppsmb1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbb1));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel[0] = byteTestData[idx++];
                mppsmb2.setPixel(x, y, pixel, dbb2);
            }
        }

        idx = 0;
        pixelStride = mppsmb2.getPixelBitStride();
        stride = mppsmb2.getScanlineStride();
        bitOffset = mppsmb2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbb2));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel[0] = shortTestData[idx++];
                mppsmu1.setPixel(x, y, pixel, dbu1);
            }
        }

        idx = 0;
        pixelStride = mppsmu1.getPixelBitStride();
        stride = mppsmu1.getScanlineStride();
        bitOffset = mppsmu1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbu1));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel[0] = shortTestData[idx++];
                mppsmu2.setPixel(x, y, pixel, dbu2);
            }
        }

        idx = 0;
        pixelStride = mppsmu2.getPixelBitStride();
        stride = mppsmu2.getScanlineStride();
        bitOffset = mppsmu2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbu2));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel[0] = intTestData[idx++];
                mppsmi1.setPixel(x, y, pixel, dbi1);
            }
        }

        idx = 0;
        pixelStride = mppsmi1.getPixelBitStride();
        stride = mppsmi1.getScanlineStride();
        bitOffset = mppsmi1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbi1));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel[0] = intTestData[idx++];
                mppsmi2.setPixel(x, y, pixel, dbi2);
            }
        }

        idx = 0;
        pixelStride = mppsmi2.getPixelBitStride();
        stride = mppsmi2.getScanlineStride();
        bitOffset = mppsmi2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbi2));
            }
        }
    }

    public final void testSetPixels() {
        initTestData();
        int pixels[];
        int pixelStride;
        int stride;
        int bitOffset;

        pixelStride = mppsmb1.getPixelBitStride();
        stride = mppsmb1.getScanlineStride();
        bitOffset = mppsmb1.getDataBitOffset();

        pixels = createPixelsFromByteData(0, 0, 1, 1);
        mppsmb1.setPixels(0, 0, 1, 1, pixels, dbb1);
        assertEquals(byteTestData[0] & 0xff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbb1));

        pixels = createPixelsFromByteData(0, 0, w / 2, h / 2);
        mppsmb1.setPixels(0, 0, w / 2, h / 2, pixels, dbb1);
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(byteTestData[y * w + x] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb1));
            }
        }

        pixels = createPixelsFromByteData(0, 0, w, h);
        mppsmb1.setPixels(0, 0, w, h, pixels, dbb1);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[y * w + x] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb1));
            }
        }

        pixelStride = mppsmb2.getPixelBitStride();
        stride = mppsmb2.getScanlineStride();
        bitOffset = mppsmb2.getDataBitOffset();

        pixels = createPixelsFromByteData(0, 0, 1, 1);
        mppsmb2.setPixels(0, 0, 1, 1, pixels, dbb2);
        assertEquals(byteTestData[0] & 0xff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbb2));

        pixels = createPixelsFromByteData(0, 0, w / 2, h / 2);
        mppsmb2.setPixels(0, 0, w / 2, h / 2, pixels, dbb2);
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(byteTestData[y * w + x] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb2));
            }
        }

        pixels = createPixelsFromByteData(0, 0, w, h);
        mppsmb2.setPixels(0, 0, w, h, pixels, dbb2);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[y * w + x] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb2));
            }
        }

        pixelStride = mppsmu1.getPixelBitStride();
        stride = mppsmu1.getScanlineStride();
        bitOffset = mppsmu1.getDataBitOffset();

        pixels = createPixelsFromShortData(0, 0, 1, 1);
        mppsmu1.setPixels(0, 0, 1, 1, pixels, dbu1);
        assertEquals(shortTestData[0] & 0xffff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbu1));

        pixels = createPixelsFromShortData(0, 0, w / 2, h / 2);
        mppsmu1.setPixels(0, 0, w / 2, h / 2, pixels, dbu1);
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(shortTestData[y * w + x] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu1));
            }
        }

        pixels = createPixelsFromShortData(0, 0, w, h);
        mppsmu1.setPixels(0, 0, w, h, pixels, dbu1);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[y * w + x] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu1));
            }
        }

        pixelStride = mppsmu2.getPixelBitStride();
        stride = mppsmu2.getScanlineStride();
        bitOffset = mppsmu2.getDataBitOffset();

        pixels = createPixelsFromShortData(0, 0, 1, 1);
        mppsmu2.setPixels(0, 0, 1, 1, pixels, dbu2);
        assertEquals(shortTestData[0] & 0xffff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbu2));

        pixels = createPixelsFromShortData(0, 0, w / 2, h / 2);
        mppsmu2.setPixels(0, 0, w / 2, h / 2, pixels, dbu2);
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(shortTestData[y * w + x] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu2));
            }
        }

        pixels = createPixelsFromShortData(0, 0, w, h);
        mppsmu2.setPixels(0, 0, w, h, pixels, dbu2);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[y * w + x] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu2));
            }
        }

        pixelStride = mppsmi1.getPixelBitStride();
        stride = mppsmi1.getScanlineStride();
        bitOffset = mppsmi1.getDataBitOffset();

        pixels = createPixelsFromIntData(0, 0, 1, 1);
        mppsmi1.setPixels(0, 0, 1, 1, pixels, dbi1);
        assertEquals(intTestData[0], getPixel(0, 0, pixelStride, bitOffset,
                stride, dbi1));

        pixels = createPixelsFromIntData(0, 0, w / 2, h / 2);
        mppsmi1.setPixels(0, 0, w / 2, h / 2, pixels, dbi1);
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(intTestData[y * w + x], getPixel(x, y,
                        pixelStride, bitOffset, stride, dbi1));
            }
        }

        pixels = createPixelsFromIntData(0, 0, w, h);
        mppsmi1.setPixels(0, 0, w, h, pixels, dbi1);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[y * w + x], getPixel(x, y,
                        pixelStride, bitOffset, stride, dbi1));
            }
        }

        pixelStride = mppsmi2.getPixelBitStride();
        stride = mppsmi2.getScanlineStride();
        bitOffset = mppsmi2.getDataBitOffset();

        pixels = createPixelsFromIntData(0, 0, 1, 1);
        mppsmi2.setPixels(0, 0, 1, 1, pixels, dbi2);
        assertEquals(intTestData[0] & 0xff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbi2));

        pixels = createPixelsFromIntData(0, 0, w / 2, h / 2);
        mppsmi2.setPixels(0, 0, w / 2, h / 2, pixels, dbi2);
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(intTestData[y * w + x], getPixel(x, y,
                        pixelStride, bitOffset, stride, dbi2));
            }
        }

        pixels = createPixelsFromIntData(0, 0, w, h);
        mppsmi2.setPixels(0, 0, w, h, pixels, dbi2);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[y * w + x], getPixel(x, y,
                        pixelStride, bitOffset, stride, dbi2));
            }
        }
    }

    public final void testSetSample() {
        initTestData();
        int sample;
        int idx;
        int pixelStride;
        int stride;
        int bitOffset;

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = byteTestData[idx++];
                mppsmb1.setSample(x, y, 0, sample, dbb1);
            }
        }
        idx = 0;
        pixelStride = mppsmb1.getPixelBitStride();
        stride = mppsmb1.getScanlineStride();
        bitOffset = mppsmb1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbb1));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = byteTestData[idx++];
                mppsmb2.setSample(x, y, 0, sample, dbb2);
            }
        }
        idx = 0;
        pixelStride = mppsmb2.getPixelBitStride();
        stride = mppsmb2.getScanlineStride();
        bitOffset = mppsmb2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbb2));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = shortTestData[idx++];
                mppsmu1.setSample(x, y, 0, sample, dbu1);
            }
        }
        idx = 0;
        pixelStride = mppsmu1.getPixelBitStride();
        stride = mppsmu1.getScanlineStride();
        bitOffset = mppsmu1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbu1));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = shortTestData[idx++];
                mppsmu2.setSample(x, y, 0, sample, dbu2);
            }
        }
        idx = 0;
        pixelStride = mppsmu2.getPixelBitStride();
        stride = mppsmu2.getScanlineStride();
        bitOffset = mppsmu2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbu2));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = intTestData[idx++];
                mppsmi1.setSample(x, y, 0, sample, dbi1);
            }
        }
        idx = 0;
        pixelStride = mppsmi1.getPixelBitStride();
        stride = mppsmi1.getScanlineStride();
        bitOffset = mppsmi1.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbi1));
            }
        }

        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sample = intTestData[idx++];
                mppsmi2.setSample(x, y, 0, sample, dbi2);
            }
        }
        idx = 0;
        pixelStride = mppsmi2.getPixelBitStride();
        stride = mppsmi2.getScanlineStride();
        bitOffset = mppsmi2.getDataBitOffset();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[idx++], getPixel(x, y, pixelStride,
                        bitOffset, stride, dbi2));
            }
        }
    }

    public final void testSetSamples() {
        initTestData();
        int samples[];
        int pixelStride;
        int stride;
        int bitOffset;
        int numBands;

        pixelStride = mppsmb1.getPixelBitStride();
        stride = mppsmb1.getScanlineStride();
        bitOffset = mppsmb1.getDataBitOffset();
        numBands = mppsmb1.getNumBands();

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromByteData(0, 0, 1, 1, b);
            mppsmb1.setSamples(0, 0, 1, 1, b, samples, dbb1);
        }
        assertEquals(byteTestData[0] & 0xff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbb1));

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromByteData(0, 0, w / 2, h / 2, b);
            mppsmb1.setSamples(0, 0, w / 2, h / 2, b, samples, dbb1);
        }
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(byteTestData[y * w + x] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb1));
            }
        }

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromByteData(0, 0, w, h, b);
            mppsmb1.setSamples(0, 0, w, h, b, samples, dbb1);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[y * w + x] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb1));
            }
        }

        pixelStride = mppsmb2.getPixelBitStride();
        stride = mppsmb2.getScanlineStride();
        bitOffset = mppsmb2.getDataBitOffset();
        numBands = mppsmb2.getNumBands();

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromByteData(0, 0, 1, 1, b);
            mppsmb2.setSamples(0, 0, 1, 1, b, samples, dbb2);
        }
        assertEquals(byteTestData[0] & 0xff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbb2));

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromByteData(0, 0, w / 2, h / 2, b);
            mppsmb2.setSamples(0, 0, w / 2, h / 2, b, samples, dbb2);
        }
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(byteTestData[y * w + x] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb2));
            }
        }

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromByteData(0, 0, w, h, b);
            mppsmb2.setSamples(0, 0, w, h, b, samples, dbb2);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(byteTestData[y * w + x] & 0xff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbb2));
            }
        }

        pixelStride = mppsmu1.getPixelBitStride();
        stride = mppsmu1.getScanlineStride();
        bitOffset = mppsmu1.getDataBitOffset();
        numBands = mppsmu1.getNumBands();

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromShortData(0, 0, 1, 1, b);
            mppsmu1.setSamples(0, 0, 1, 1, b, samples, dbu1);
        }
        assertEquals(shortTestData[0] & 0xffff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbu1));

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromShortData(0, 0, w / 2, h / 2, b);
            mppsmu1.setSamples(0, 0, w / 2, h / 2, b, samples, dbu1);
        }
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(shortTestData[y * w + x] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu1));
            }
        }

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromShortData(0, 0, w, h, b);
            mppsmu1.setSamples(0, 0, w, h, b, samples, dbu1);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[y * w + x] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu1));
            }
        }

        pixelStride = mppsmu2.getPixelBitStride();
        stride = mppsmu2.getScanlineStride();
        bitOffset = mppsmu2.getDataBitOffset();
        numBands = mppsmu2.getNumBands();

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromShortData(0, 0, 1, 1, b);
            mppsmu2.setSamples(0, 0, 1, 1, b, samples, dbu2);
        }
        assertEquals(shortTestData[0] & 0xffff, getPixel(0, 0, pixelStride,
                bitOffset, stride, dbu2));

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromShortData(0, 0, w / 2, h / 2, b);
            mppsmu2.setSamples(0, 0, w / 2, h / 2, b, samples, dbu2);
        }
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(shortTestData[y * w + x] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu2));
            }
        }

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromShortData(0, 0, w, h, b);
            mppsmu2.setSamples(0, 0, w, h, b, samples, dbu2);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(shortTestData[y * w + x] & 0xffff, getPixel(x, y,
                        pixelStride, bitOffset, stride, dbu2));
            }
        }

        pixelStride = mppsmi1.getPixelBitStride();
        stride = mppsmi1.getScanlineStride();
        bitOffset = mppsmi1.getDataBitOffset();
        numBands = mppsmi1.getNumBands();

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromIntData(0, 0, 1, 1, b);
            mppsmi1.setSamples(0, 0, 1, 1, b, samples, dbi1);
        }
        assertEquals(intTestData[0], getPixel(0, 0, pixelStride, bitOffset,
                stride, dbi1));

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromIntData(0, 0, w / 2, h / 2, b);
            mppsmi1.setSamples(0, 0, w / 2, h / 2, b, samples, dbi1);
        }
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(intTestData[y * w + x], getPixel(x, y,
                        pixelStride, bitOffset, stride, dbi1));
            }
        }

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromIntData(0, 0, w, h, b);
            mppsmi1.setSamples(0, 0, w, h, b, samples, dbi1);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[y * w + x], getPixel(x, y,
                        pixelStride, bitOffset, stride, dbi1));
            }
        }

        pixelStride = mppsmi2.getPixelBitStride();
        stride = mppsmi2.getScanlineStride();
        bitOffset = mppsmi2.getDataBitOffset();
        numBands = mppsmi2.getNumBands();

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromIntData(0, 0, 1, 1, b);
            mppsmi2.setSamples(0, 0, 1, 1, b, samples, dbi2);
        }
        assertEquals(intTestData[0], getPixel(0, 0, pixelStride, bitOffset,
                stride, dbi2));

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromIntData(0, 0, w / 2, h / 2, b);
            mppsmi2.setSamples(0, 0, w / 2, h / 2, b, samples, dbi2);
        }
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w / 2; x++) {
                assertEquals(intTestData[y * w + x], getPixel(x, y,
                        pixelStride, bitOffset, stride, dbi2));
            }
        }

        for (int b = 0; b < numBands; b++) {
            samples = createSamplesFromIntData(0, 0, w, h, b);
            mppsmi2.setSamples(0, 0, w, h, b, samples, dbi2);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(intTestData[y * w + x], getPixel(x, y,
                        pixelStride, bitOffset, stride, dbi2));
            }
        }
    }

    private void initTestData() {
        int maxByte = (1 << bPixelBits) - 1;
        int maxShort = (1 << sPixelBits) - 1;
        int maxInt = (1 << iPixelBits) - 1;
        Random r = new Random();
        int size = w * h;

        byteTestData = new byte[size];
        for (int i = 0; i < size; i++) {
            byteTestData[i] = (byte) ((int) (r.nextDouble() * maxByte) & 0xff);
        }

        shortTestData = new short[size];
        for (int i = 0; i < size; i++) {
            shortTestData[i] = (short) ((int) (r.nextDouble() * maxShort) & 0xffff);
        }

        intTestData = new int[size];
        for (int i = 0; i < size; i++) {
            intTestData[i] = (int) (r.nextDouble() * maxInt);
        }
    }

    private void initDataBuffers() {
        initTestData();
        int pixelStride = mppsmb1.getPixelBitStride();
        int stride = mppsmb1.getScanlineStride();
        int offset = mppsmb1.getDataBitOffset();
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                setPixel(x, y, pixelStride, offset, stride,
                        byteTestData[idx++], dbb1);
            }
        }

        pixelStride = mppsmb2.getPixelBitStride();
        stride = mppsmb2.getScanlineStride();
        offset = mppsmb2.getDataBitOffset();
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                setPixel(x, y, pixelStride, offset, stride,
                        byteTestData[idx++], dbb2);
            }
        }

        pixelStride = mppsmu1.getPixelBitStride();
        stride = mppsmu1.getScanlineStride();
        offset = mppsmu1.getDataBitOffset();
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                setPixel(x, y, pixelStride, offset, stride,
                        shortTestData[idx++], dbu1);
            }
        }

        pixelStride = mppsmu2.getPixelBitStride();
        stride = mppsmu2.getScanlineStride();
        offset = mppsmu2.getDataBitOffset();
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                setPixel(x, y, pixelStride, offset, stride,
                        shortTestData[idx++], dbu2);
            }
        }

        pixelStride = mppsmi1.getPixelBitStride();
        stride = mppsmi1.getScanlineStride();
        offset = mppsmi1.getDataBitOffset();
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                setPixel(x, y, pixelStride, offset, stride, intTestData[idx++],
                        dbi1);
            }
        }

        pixelStride = mppsmi2.getPixelBitStride();
        stride = mppsmi2.getScanlineStride();
        offset = mppsmi2.getDataBitOffset();
        idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                setPixel(x, y, pixelStride, offset, stride, intTestData[idx++],
                        dbi2);
            }
        }
    }

    private void setPixel(int x, int y, int pixelStride, int bitOffset,
            int scanline, int pixel, DataBuffer db) {

        int type = db.getDataType();
        int dataElemSize = DataBuffer.getDataTypeSize(type);
        int bitMask = (1 << pixelStride) - 1;
        int bitnum = bitOffset + x * pixelStride;
        int idx = (y * scanline + bitnum / dataElemSize);
        int dataElem = db.getElem(idx);
        int shift = dataElemSize - (bitnum & (dataElemSize - 1)) - pixelStride;
        int mask = ~(bitMask << shift);
        dataElem &= mask;
        dataElem |= (pixel & bitMask) << shift;
        db.setElem(idx, dataElem);
    }

    private int getPixel(int x, int y, int pixelStride, int bitOffset,
            int scanline, DataBuffer db) {

        int type = db.getDataType();
        int dataElemSize = DataBuffer.getDataTypeSize(type);
        int bitMask = (1 << pixelStride) - 1;
        int bitnum = bitOffset + x * pixelStride;
        int idx = (y * scanline + bitnum / dataElemSize);
        int dataElem = db.getElem(idx);
        int shift = dataElemSize - (bitnum & (dataElemSize - 1)) - pixelStride;
        return (dataElem >> shift) & bitMask;
    }

    private Object getDataElementsB(int x, int y, int width, int height,
            int transferType) {
        Object de;
        int size = width * height;
        int idx = 0;
        switch (transferType) {
        case DataBuffer.TYPE_BYTE:
            byte bde[] = new byte[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    bde[idx++] = byteTestData[yi * w + xi];
                }
            }
            de = bde;
            break;
        case DataBuffer.TYPE_USHORT:
            short sde[] = new short[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    sde[idx++] = (short) (byteTestData[yi * w + xi] & 0xff);
                }
            }
            de = sde;
            break;
        case DataBuffer.TYPE_INT:
            int ide[] = new int[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    ide[idx++] = byteTestData[yi * w + xi] & 0xff;
                }
            }
            de = ide;
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }
        return de;
    }

    private Object getDataElementsUS(int x, int y, int width, int height,
            int transferType) {
        Object de;
        int size = width * height;
        int idx = 0;
        switch (transferType) {
        case DataBuffer.TYPE_BYTE:
            byte bde[] = new byte[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    bde[idx++] = (byte) shortTestData[yi * w + xi];
                }
            }
            de = bde;
            break;
        case DataBuffer.TYPE_USHORT:
            short sde[] = new short[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    sde[idx++] = shortTestData[yi * w + xi];
                }
            }
            de = sde;
            break;
        case DataBuffer.TYPE_INT:
            int ide[] = new int[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    ide[idx++] = shortTestData[yi * w + xi] & 0xffff;
                }
            }
            de = ide;
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }
        return de;
    }

    private Object getDataElementsI(int x, int y, int width, int height,
            int transferType) {
        Object de;
        int size = width * height;
        int idx = 0;
        switch (transferType) {
        case DataBuffer.TYPE_BYTE:
            byte bde[] = new byte[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    bde[idx++] = (byte) intTestData[yi * w + xi];
                }
            }
            de = bde;
            break;
        case DataBuffer.TYPE_USHORT:
            short sde[] = new short[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    sde[idx++] = (short) intTestData[yi * w + xi];
                }
            }
            de = sde;
            break;
        case DataBuffer.TYPE_INT:
            int ide[] = new int[size];
            for (int yi = y; yi < y + height; yi++) {
                for (int xi = x; xi < x + width; xi++) {
                    ide[idx++] = byteTestData[yi * w + xi] & 0xff;
                }
            }
            de = ide;
            break;
        default:
            throw new IllegalArgumentException("Wrong Transfer Type");
        }
        return de;
    }

    private int[] getPixelsB(int x, int y, int width, int height) {
        int pixels[] = new int[width * height];
        int idx = 0;
        for (int yi = y; yi < y + height; yi++) {
            for (int xi = x; xi < x + width; xi++) {
                pixels[idx++] = byteTestData[yi * w + xi] & 0xff;
            }
        }
        return pixels;
    }

    private int[] getPixelsUS(int x, int y, int width, int height) {
        int pixels[] = new int[width * height];
        int idx = 0;
        for (int yi = y; yi < y + height; yi++) {
            for (int xi = x; xi < x + width; xi++) {
                pixels[idx++] = shortTestData[yi * w + xi] & 0xffff;
            }
        }
        return pixels;
    }

    private int[] getPixelsI(int x, int y, int width, int height) {
        int pixels[] = new int[width * height];
        int idx = 0;
        for (int yi = y; yi < y + height; yi++) {
            for (int xi = x; xi < x + width; xi++) {
                pixels[idx++] = intTestData[yi * w + xi];
            }
        }
        return pixels;
    }

    private int[] getSamplesB(int x, int y, int width, int height, int b) {
        int samples[] = new int[width * height];
        int idx = 0;
        for (int y1 = y; y1 < y + height; y1++) {
            for (int x1 = x; x1 < x + width; x1++) {
                samples[idx++] = byteTestData[y1 * w + x1] & 0xff;
            }
        }
        return samples;
    }

    private int[] getSamplesUS(int x, int y, int width, int height, int b) {
        int samples[] = new int[width * height];
        int idx = 0;
        for (int y1 = y; y1 < y + height; y1++) {
            for (int x1 = x; x1 < x + width; x1++) {
                samples[idx++] = shortTestData[y1 * w + x1] & 0xffff;
            }
        }
        return samples;
    }

    private int[] getSamplesI(int x, int y, int width, int height, int b) {
        int samples[] = new int[width * height];
        int idx = 0;
        for (int y1 = y; y1 < y + height; y1++) {
            for (int x1 = x; x1 < x + width; x1++) {
                samples[idx++] = intTestData[y1 * w + x1];
            }
        }
        return samples;
    }

    private int[] createPixelsFromByteData(int x, int y, int width, int height) {
        int pixels[] = new int[width * height];
        int idx = 0;
        for (int yi = y; yi < y + height; yi++) {
            for (int xi = x; xi < x + width; xi++) {
                pixels[idx++] = byteTestData[yi * w + xi] & 0xff;
            }
        }
        return pixels;
    }

    private int[] createPixelsFromShortData(int x, int y, int width, int height) {
        int pixels[] = new int[width * height];
        int idx = 0;
        for (int yi = y; yi < y + height; yi++) {
            for (int xi = x; xi < x + width; xi++) {
                pixels[idx++] = shortTestData[yi * w + xi] & 0xffff;
            }
        }
        return pixels;
    }

    private int[] createPixelsFromIntData(int x, int y, int width, int height) {
        int pixels[] = new int[width * height];
        int idx = 0;
        for (int yi = y; yi < y + height; yi++) {
            for (int xi = x; xi < x + width; xi++) {
                pixels[idx++] = intTestData[yi * w + xi];
            }
        }
        return pixels;
    }

    private int[] createSamplesFromByteData(int x, int y, int width,
            int height, int bank) {
        return createPixelsFromByteData(x, y, width, height);
    }

    private int[] createSamplesFromShortData(int x, int y, int width,
            int height, int bank) {
        return createPixelsFromShortData(x, y, width, height);
    }

    private int[] createSamplesFromIntData(int x, int y, int width, int height,
            int bank) {
        return createPixelsFromIntData(x, y, width, height);
    }
    
    
}
