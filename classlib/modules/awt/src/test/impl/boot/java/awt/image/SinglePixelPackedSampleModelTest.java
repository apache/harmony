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
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

public class SinglePixelPackedSampleModelTest extends TestCase {
    int w = 10;
    int h = 10;
    int scanlineStride = 20;

    int bitMaskB[] = {0x3, 0xc, 0x30, 0xc0};
    int bitOffsetsB[] = {0, 2, 4, 6};
    int samplesSizeB[] = {2, 2, 2, 2};
    int bitMaskUS[] = {0xf, 0xf0, 0xf00, 0xf000};
    int bitOffsetsUS[] = {0, 4, 8, 12};
    int samplesSizeUS[] = {4, 4, 4, 4};
    int bitMaskI[] = {0xff, 0xff00, 0xff0000, 0xff000000};
    int bitOffsetsI[] = {0, 8, 16, 24};
    int samplesSizeI[] = {8, 8, 8, 8};

    byte byteTestData[];
    short shortTestData[];
    int intTestData[];

    DataBufferByte dbb1, dbb2;
    DataBufferUShort dbu1, dbu2;
    DataBufferInt dbi1, dbi2;

    SinglePixelPackedSampleModel sppsmb1, sppsmb2, sppsmu1, sppsmu2, sppsmi1, sppsmi2;


    public static void main(String[] args) {
        junit.textui.TestRunner.run(SinglePixelPackedSampleModelTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dbb1 = new DataBufferByte(w * h);
        dbb2 = new DataBufferByte(scanlineStride * (h - 1) + w);
        dbu1 = new DataBufferUShort(w * h);
        dbu2 = new DataBufferUShort(scanlineStride * (h - 1) + w);
        dbi1 = new DataBufferInt(w * h);
        dbi2 = new DataBufferInt(scanlineStride * (h - 1) + w);
        sppsmb1 = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h, bitMaskB);
        sppsmb2 = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h, scanlineStride, bitMaskB);
        sppsmu1 = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, w, h, bitMaskUS);
        sppsmu2 = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, w, h, scanlineStride, bitMaskUS);
        sppsmi1 = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, w, h, bitMaskI);
        sppsmi2 = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, w, h, scanlineStride, bitMaskI);
        initTestData();
    }

    /**
     * Constructor for SinglePixelPackedSampleModelTest.
     * @param name
     */
    public SinglePixelPackedSampleModelTest(String name) {
        super(name);
    }

    public final void testGetDataType(){
        assertEquals(DataBuffer.TYPE_BYTE, sppsmb1.getDataType());
        assertEquals(DataBuffer.TYPE_BYTE, sppsmb2.getDataType());
        assertEquals(DataBuffer.TYPE_USHORT, sppsmu1.getDataType());
        assertEquals(DataBuffer.TYPE_USHORT, sppsmu2.getDataType());
        assertEquals(DataBuffer.TYPE_INT, sppsmi1.getDataType());
        assertEquals(DataBuffer.TYPE_INT, sppsmi2.getDataType());
    }

    public final void testGetHeight(){
        assertEquals(h, sppsmb1.getHeight());
        assertEquals(h, sppsmb2.getHeight());
        assertEquals(h, sppsmu1.getHeight());
        assertEquals(h, sppsmu2.getHeight());
        assertEquals(h, sppsmi1.getHeight());
        assertEquals(h, sppsmi2.getHeight());
    }

    public final void testGetWidth(){
        assertEquals(w, sppsmb1.getWidth());
        assertEquals(w, sppsmb2.getWidth());
        assertEquals(w, sppsmu1.getWidth());
        assertEquals(w, sppsmu2.getWidth());
        assertEquals(w, sppsmi1.getWidth());
        assertEquals(w, sppsmi2.getWidth());
    }

    public final void testGetScanlineStride(){
        assertEquals(w, sppsmb1.getScanlineStride());
        assertEquals(scanlineStride, sppsmb2.getScanlineStride());
        assertEquals(w, sppsmu1.getScanlineStride());
        assertEquals(scanlineStride, sppsmu2.getScanlineStride());
        assertEquals(w, sppsmi1.getScanlineStride());
        assertEquals(scanlineStride, sppsmi2.getScanlineStride());
    }

    public final void testGetTransferType(){
        assertEquals(DataBuffer.TYPE_BYTE, sppsmb1.getTransferType());
        assertEquals(DataBuffer.TYPE_BYTE, sppsmb2.getTransferType());
        assertEquals(DataBuffer.TYPE_USHORT, sppsmu1.getTransferType());
        assertEquals(DataBuffer.TYPE_USHORT, sppsmu2.getTransferType());
        assertEquals(DataBuffer.TYPE_INT, sppsmi1.getTransferType());
        assertEquals(DataBuffer.TYPE_INT, sppsmi2.getTransferType());
    }

    public final void testGetNumBands(){
        assertEquals(4, sppsmb1.getNumBands());
        assertEquals(4, sppsmb2.getNumBands());
        assertEquals(4, sppsmu1.getNumBands());
        assertEquals(4, sppsmu2.getNumBands());
        assertEquals(4, sppsmi1.getNumBands());
        assertEquals(4, sppsmi2.getNumBands());
    }

    public final void testGetBitMasks(){
        assertTrue(Arrays.equals(bitMaskB, sppsmb1.getBitMasks()));
        assertTrue(Arrays.equals(bitMaskB, sppsmb2.getBitMasks()));
        assertTrue(Arrays.equals(bitMaskUS, sppsmu1.getBitMasks()));
        assertTrue(Arrays.equals(bitMaskUS, sppsmu2.getBitMasks()));
        assertTrue(Arrays.equals(bitMaskI, sppsmi1.getBitMasks()));
        assertTrue(Arrays.equals(bitMaskI, sppsmi2.getBitMasks()));
    }

    public final void testCreateCompatibleSampleModel(){
        SampleModel sm;
        SinglePixelPackedSampleModel sppsm;

        sm = sppsmb1.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_BYTE, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertTrue(Arrays.equals(bitMaskB, sppsm.getBitMasks()));

        sm = sppsmb2.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_BYTE, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertTrue(Arrays.equals(bitMaskB, sppsm.getBitMasks()));

        sm = sppsmu1.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_USHORT, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertTrue(Arrays.equals(bitMaskUS, sppsm.getBitMasks()));

        sm = sppsmu2.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_USHORT, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertTrue(Arrays.equals(bitMaskUS, sppsm.getBitMasks()));

        sm = sppsmi1.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_INT, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertTrue(Arrays.equals(bitMaskI, sppsm.getBitMasks()));

        sm = sppsmi2.createCompatibleSampleModel(w, h);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_INT, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertTrue(Arrays.equals(bitMaskI, sppsm.getBitMasks()));
    }

    public final void testCreateDataBuffer(){
        DataBuffer db;

        db = sppsmb1.createDataBuffer();
        assertEquals(dbb1.getDataType(), db.getDataType());
        assertEquals(dbb1.getNumBanks(), db.getNumBanks());
        assertEquals(dbb1.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbb1.getOffsets(), db.getOffsets()));

        db = sppsmb2.createDataBuffer();
        assertEquals(dbb2.getDataType(), db.getDataType());
        assertEquals(dbb2.getNumBanks(), db.getNumBanks());
        assertEquals(dbb2.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbb2.getOffsets(), db.getOffsets()));

        db = sppsmu1.createDataBuffer();
        assertEquals(dbu1.getDataType(), db.getDataType());
        assertEquals(dbu1.getNumBanks(), db.getNumBanks());
        assertEquals(dbu1.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbu1.getOffsets(), db.getOffsets()));

        db = sppsmu2.createDataBuffer();
        assertEquals(dbu2.getDataType(), db.getDataType());
        assertEquals(dbu2.getNumBanks(), db.getNumBanks());
        assertEquals(dbu2.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbu2.getOffsets(), db.getOffsets()));

        db = sppsmi1.createDataBuffer();
        assertEquals(dbi1.getDataType(), db.getDataType());
        assertEquals(dbi1.getNumBanks(), db.getNumBanks());
        assertEquals(dbi1.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbi1.getOffsets(), db.getOffsets()));

        db = sppsmi2.createDataBuffer();
        assertEquals(dbi2.getDataType(), db.getDataType());
        assertEquals(dbi2.getNumBanks(), db.getNumBanks());
        assertEquals(dbi2.getSize(), db.getSize());
        assertTrue(Arrays.equals(dbi2.getOffsets(), db.getOffsets()));
    }

    public final void testCreateSubsetSampleModel(){
        int masks[];
        int bands[] = {0,2};
        SampleModel sm;
        SinglePixelPackedSampleModel sppsm;

        sm = sppsmb1.createSubsetSampleModel(bands);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_BYTE, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertEquals(bands.length, sppsm.getNumBands());
        masks = sppsm.getBitMasks();
        for(int i = 0; i < masks.length; i++){
            assertEquals(masks[i], bitMaskB[bands[i]]);
        }

        sm = sppsmb2.createSubsetSampleModel(bands);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_BYTE, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(scanlineStride, sppsm.getScanlineStride());
        assertEquals(bands.length, sppsm.getNumBands());
        masks = sppsm.getBitMasks();
        for(int i = 0; i < masks.length; i++){
            assertEquals(masks[i], bitMaskB[bands[i]]);
        }

        sm = sppsmu1.createSubsetSampleModel(bands);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_USHORT, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertEquals(bands.length, sppsm.getNumBands());
        masks = sppsm.getBitMasks();
        for(int i = 0; i < masks.length; i++){
            assertEquals(masks[i], bitMaskUS[bands[i]]);
        }

        sm = sppsmu2.createSubsetSampleModel(bands);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_USHORT, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(scanlineStride, sppsm.getScanlineStride());
        assertEquals(bands.length, sppsm.getNumBands());
        masks = sppsm.getBitMasks();
        for(int i = 0; i < masks.length; i++){
            assertEquals(masks[i], bitMaskUS[bands[i]]);
        }

        sm = sppsmi1.createSubsetSampleModel(bands);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_INT, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(w, sppsm.getScanlineStride());
        assertEquals(bands.length, sppsm.getNumBands());
        masks = sppsm.getBitMasks();
        for(int i = 0; i < masks.length; i++){
            assertEquals(masks[i], bitMaskI[bands[i]]);
        }

        sm = sppsmi2.createSubsetSampleModel(bands);
        assertTrue(sm instanceof SinglePixelPackedSampleModel);
        sppsm = (SinglePixelPackedSampleModel) sm;
        assertEquals(DataBuffer.TYPE_INT, sppsm.getDataType());
        assertEquals(w, sppsm.getWidth());
        assertEquals(h, sppsm.getHeight());
        assertEquals(scanlineStride, sppsm.getScanlineStride());
        assertEquals(bands.length, sppsm.getNumBands());
        masks = sppsm.getBitMasks();
        for(int i = 0; i < masks.length; i++){
            assertEquals(masks[i], bitMaskI[bands[i]]);
        }
    }

    public final void testEquals(){
        SampleModel sm;

        sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h, bitMaskB);
        assertTrue(sppsmb1.equals(sm));
        assertFalse(sppsmb2.equals(sm));

        sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h, scanlineStride, bitMaskB);
        assertFalse(sppsmb1.equals(sm));
        assertTrue(sppsmb2.equals(sm));

        sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, w, h, bitMaskUS);
        assertTrue(sppsmu1.equals(sm));
        assertFalse(sppsmu2.equals(sm));

        sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, w, h, scanlineStride, bitMaskUS);
        assertFalse(sppsmu1.equals(sm));
        assertTrue(sppsmu2.equals(sm));

        sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, w, h, bitMaskI);
        assertTrue(sppsmi1.equals(sm));
        assertFalse(sppsmi2.equals(sm));

        sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, w, h, scanlineStride, bitMaskI);
        assertFalse(sppsmi1.equals(sm));
        assertTrue(sppsmi2.equals(sm));
    }

    public final void testGetBitOffsets(){
        int bitOffsets[];

        bitOffsets = sppsmb1.getBitOffsets();
        assertEquals(bitOffsetsB.length, bitOffsets.length);
        for(int i = 0; i < bitOffsets.length; i++){
            assertEquals(bitOffsetsB[i], bitOffsets[i]);
        }

        bitOffsets = sppsmb2.getBitOffsets();
        assertEquals(bitOffsetsB.length, bitOffsets.length);
        for(int i = 0; i < bitOffsets.length; i++){
            assertEquals(bitOffsetsB[i], bitOffsets[i]);
        }

        bitOffsets = sppsmu1.getBitOffsets();
        assertEquals(bitOffsetsUS.length, bitOffsets.length);
        for(int i = 0; i < bitOffsets.length; i++){
            assertEquals(bitOffsetsUS[i], bitOffsets[i]);
        }

        bitOffsets = sppsmu2.getBitOffsets();
        assertEquals(bitOffsetsUS.length, bitOffsets.length);
        for(int i = 0; i < bitOffsets.length; i++){
            assertEquals(bitOffsetsUS[i], bitOffsets[i]);
        }

        bitOffsets = sppsmi1.getBitOffsets();
        assertEquals(bitOffsetsI.length, bitOffsets.length);
        for(int i = 0; i < bitOffsets.length; i++){
            assertEquals(bitOffsetsI[i], bitOffsets[i]);
        }

        bitOffsets = sppsmi2.getBitOffsets();
        assertEquals(bitOffsetsI.length, bitOffsets.length);
        for(int i = 0; i < bitOffsets.length; i++){
            assertEquals(bitOffsetsI[i], bitOffsets[i]);
        }
    }

    public final void testNumDataElements(){
        assertEquals(1, sppsmb1.getNumDataElements());
        assertEquals(1, sppsmb2.getNumDataElements());
        assertEquals(1, sppsmu1.getNumDataElements());
        assertEquals(1, sppsmu2.getNumDataElements());
        assertEquals(1, sppsmu1.getNumDataElements());
        assertEquals(1, sppsmi2.getNumDataElements());
    }

    public final void testGetOffset(){
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(y * w + x, sppsmb1.getOffset(x, y));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(y * scanlineStride + x, sppsmb2.getOffset(x, y));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(y * w + x, sppsmu1.getOffset(x, y));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(y * scanlineStride + x, sppsmu2.getOffset(x, y));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(y * w + x, sppsmi1.getOffset(x, y));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(y * scanlineStride + x, sppsmi2.getOffset(x, y));
            }
        }
    }

    public final void testGetSampleSize(){
        int samplesSize[];

        samplesSize = sppsmb1.getSampleSize();
        assertEquals(samplesSizeB.length, samplesSize.length);
        for(int i = 0; i < samplesSize.length; i++){
            assertEquals(samplesSizeB[i], samplesSize[i]);
        }

        samplesSize = sppsmb2.getSampleSize();
        assertEquals(samplesSizeB.length, samplesSize.length);
        for(int i = 0; i < samplesSize.length; i++){
            assertEquals(samplesSizeB[i], samplesSize[i]);
        }

        samplesSize = sppsmu1.getSampleSize();
        assertEquals(samplesSizeUS.length, samplesSize.length);
        for(int i = 0; i < samplesSize.length; i++){
            assertEquals(samplesSizeUS[i], samplesSize[i]);
        }

        samplesSize = sppsmu2.getSampleSize();
        assertEquals(samplesSizeUS.length, samplesSize.length);
        for(int i = 0; i < samplesSize.length; i++){
            assertEquals(samplesSizeUS[i], samplesSize[i]);
        }

        samplesSize = sppsmi1.getSampleSize();
        assertEquals(samplesSizeI.length, samplesSize.length);
        for(int i = 0; i < samplesSize.length; i++){
            assertEquals(samplesSizeI[i], samplesSize[i]);
        }

        samplesSize = sppsmi2.getSampleSize();
        assertEquals(samplesSizeI.length, samplesSize.length);
        for(int i = 0; i < samplesSize.length; i++){
            assertEquals(samplesSizeI[i], samplesSize[i]);
        }
    }

    public final void testGetPixel(){
        initDataBuffers();
        int tpixel[] = null;
        int rpixel[];

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tpixel = sppsmb1.getPixel(x, y, tpixel, dbb1);
                rpixel = getPixelB(x, y);
                assertTrue(Arrays.equals(rpixel, tpixel));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tpixel = sppsmb2.getPixel(x, y, tpixel, dbb2);
                rpixel = getPixelB(x, y);
                assertTrue(Arrays.equals(rpixel, tpixel));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tpixel = sppsmu1.getPixel(x, y, tpixel, dbu1);
                rpixel = getPixelUS(x, y);
                assertTrue(Arrays.equals(rpixel, tpixel));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tpixel = sppsmu2.getPixel(x, y, tpixel, dbu2);
                rpixel = getPixelUS(x, y);
                assertTrue(Arrays.equals(rpixel, tpixel));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tpixel = sppsmi1.getPixel(x, y, tpixel, dbi1);
                rpixel = getPixelI(x, y);
                assertTrue(Arrays.equals(rpixel, tpixel));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tpixel = sppsmi2.getPixel(x, y, tpixel, dbi2);
                rpixel = getPixelI(x, y);
                assertTrue(Arrays.equals(rpixel, tpixel));
            }
        }
    }

    public final void testGetPixels(){
        initDataBuffers();
        int tpixel[] = null;
        int rpixel[];

        tpixel = sppsmb1.getPixels(0, 0, 1, 1, tpixel, dbb1);
        rpixel = getPixelsB(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmb1.getPixels(0, 0, w/2, h/2, tpixel, dbb1);
        rpixel = getPixelsB(0, 0, w/2, h/2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmb1.getPixels(0, 0, w, h, tpixel, dbb1);
        rpixel = getPixelsB(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmb2.getPixels(0, 0, 1, 1, tpixel, dbb2);
        rpixel = getPixelsB(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmb2.getPixels(0, 0, w/2, h/2, tpixel, dbb2);
        rpixel = getPixelsB(0, 0, w/2, h/2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmb2.getPixels(0, 0, w, h, tpixel, dbb2);
        rpixel = getPixelsB(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmu1.getPixels(0, 0, 1, 1, tpixel, dbu1);
        rpixel = getPixelsUS(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmu1.getPixels(0, 0, w/2, h/2, tpixel, dbu1);
        rpixel = getPixelsUS(0, 0, w/2, h/2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmu1.getPixels(0, 0, w, h, tpixel, dbu1);
        rpixel = getPixelsUS(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmu2.getPixels(0, 0, 1, 1, tpixel, dbu2);
        rpixel = getPixelsUS(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmu2.getPixels(0, 0, w/2, h/2, tpixel, dbu2);
        rpixel = getPixelsUS(0, 0, w/2, h/2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmu2.getPixels(0, 0, w, h, tpixel, dbu2);
        rpixel = getPixelsUS(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmi1.getPixels(0, 0, 1, 1, tpixel, dbi1);
        rpixel = getPixelsI(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmi1.getPixels(0, 0, w/2, h/2, tpixel, dbi1);
        rpixel = getPixelsI(0, 0, w/2, h/2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmi1.getPixels(0, 0, w, h, tpixel, dbi1);
        rpixel = getPixelsI(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmi2.getPixels(0, 0, 1, 1, tpixel, dbi2);
        rpixel = getPixelsI(0, 0, 1, 1);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmi2.getPixels(0, 0, w/2, h/2, tpixel, dbi2);
        rpixel = getPixelsI(0, 0, w/2, h/2);
        assertTrue(Arrays.equals(rpixel, tpixel));

        tpixel = null;
        tpixel = sppsmi2.getPixels(0, 0, w, h, tpixel, dbi2);
        rpixel = getPixelsI(0, 0, w, h);
        assertTrue(Arrays.equals(rpixel, tpixel));

    }

    public final void testGetSample(){
        initDataBuffers();
        int tsample;
        int rsample;

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                for(int b = 0; b < sppsmb1.getNumBands(); b++){
                    tsample = sppsmb1.getSample(x, y, b, dbb1);
                    rsample = getSampleB(x, y, b);
                    assertEquals(tsample, rsample);
                }
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                for(int b = 0; b < sppsmb2.getNumBands(); b++){
                    tsample = sppsmb2.getSample(x, y, b, dbb2);
                    rsample = getSampleB(x, y, b);
                    assertEquals(tsample, rsample);
                }
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                for(int b = 0; b < sppsmu1.getNumBands(); b++){
                    tsample = sppsmu1.getSample(x, y, b, dbu1);
                    rsample = getSampleUS(x, y, b);
                    assertEquals(tsample, rsample);
                }
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                for(int b = 0; b < sppsmu2.getNumBands(); b++){
                    tsample = sppsmu2.getSample(x, y, b, dbu2);
                    rsample = getSampleUS(x, y, b);
                    assertEquals(tsample, rsample);
                }
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                for(int b = 0; b < sppsmi1.getNumBands(); b++){
                    tsample = sppsmi1.getSample(x, y, b, dbi1);
                    rsample = getSampleI(x, y, b);
                    assertEquals(tsample, rsample);
                }
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                for(int b = 0; b < sppsmi2.getNumBands(); b++){
                    tsample = sppsmi2.getSample(x, y, b, dbi2);
                    rsample = getSampleI(x, y, b);
                    assertEquals(tsample, rsample);
                }
            }
        }
    }

    public final void testGetSamples(){
        initDataBuffers();
        int rsamples[];
        int tsamples[];

        for(int b = 0; b < sppsmb1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmb1.getSamples(0, 0, 1, 1, b, tsamples, dbb1);
            rsamples = getSamplesB(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmb1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmb1.getSamples(0, 0, w/2, h/2, b, tsamples, dbb1);
            rsamples = getSamplesB(0, 0, w/2, h/2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmb1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmb1.getSamples(0, 0, w, h, b, tsamples, dbb1);
            rsamples = getSamplesB(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmb2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmb2.getSamples(0, 0, 1, 1, b, tsamples, dbb2);
            rsamples = getSamplesB(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmb2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmb2.getSamples(0, 0, w/2, h/2, b, tsamples, dbb2);
            rsamples = getSamplesB(0, 0, w/2, h/2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmb2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmb2.getSamples(0, 0, w, h, b, tsamples, dbb2);
            rsamples = getSamplesB(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmu1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmu1.getSamples(0, 0, 1, 1, b, tsamples, dbu1);
            rsamples = getSamplesUS(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmu1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmu1.getSamples(0, 0, w/2, h/2, b, tsamples, dbu1);
            rsamples = getSamplesUS(0, 0, w/2, h/2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmu1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmu1.getSamples(0, 0, w, h, b, tsamples, dbu1);
            rsamples = getSamplesUS(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmu2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmu2.getSamples(0, 0, 1, 1, b, tsamples, dbu2);
            rsamples = getSamplesUS(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmu2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmu2.getSamples(0, 0, w/2, h/2, b, tsamples, dbu2);
            rsamples = getSamplesUS(0, 0, w/2, h/2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmu2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmu2.getSamples(0, 0, w, h, b, tsamples, dbu2);
            rsamples = getSamplesUS(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmi1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmi1.getSamples(0, 0, 1, 1, b, tsamples, dbi1);
            rsamples = getSamplesI(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmi1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmi1.getSamples(0, 0, w/2, h/2, b, tsamples, dbi1);
            rsamples = getSamplesI(0, 0, w/2, h/2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmi1.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmi1.getSamples(0, 0, w, h, b, tsamples, dbi1);
            rsamples = getSamplesI(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmi2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmi2.getSamples(0, 0, 1, 1, b, tsamples, dbi2);
            rsamples = getSamplesI(0, 0, 1, 1, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmi2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmi2.getSamples(0, 0, w/2, h/2, b, tsamples, dbi2);
            rsamples = getSamplesI(0, 0, w/2, h/2, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        for(int b = 0; b < sppsmi2.getNumBands(); b++){
            tsamples = null;
            tsamples = sppsmi2.getSamples(0, 0, w, h, b, tsamples, dbi2);
            rsamples = getSamplesI(0, 0, w, h, b);
            assertTrue(Arrays.equals(rsamples, tsamples));
        }

        // Regression for HARMONY-2431
        try {
            new SinglePixelPackedSampleModel(3, 216, 1, new int[851])
                    .getSamples(6, 7, 14, Integer.MAX_VALUE, 0, new int[] { 0,
                            0, 0 }, new DataBufferDouble(7, 5));
            fail("ArrayIndexOutOfBoundsException was not thrown"); //$NON-NLS-1$
        } catch (ArrayIndexOutOfBoundsException ex) {
            // expected
        }
    }

    public final void testGetDataElements(){
        initDataBuffers();
        Object tDE, rDE;

        tDE = null;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tDE = sppsmb1.getDataElements(x, y, tDE, dbb1);
                rDE = getDataElementB(x, y);
                assertTrue(tDE instanceof byte[]);
                byte tde[] = (byte[])tDE;
                byte rde[] = (byte[])rDE;
                assertTrue(Arrays.equals(tde, rde));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tDE = sppsmb2.getDataElements(x, y, tDE, dbb2);
                rDE = getDataElementB(x, y);
                assertTrue(tDE instanceof byte[]);
                byte tde[] = (byte[])tDE;
                byte rde[] = (byte[])rDE;
                assertTrue(Arrays.equals(tde, rde));
            }
        }

        tDE = null;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tDE = sppsmu1.getDataElements(x, y, tDE, dbu1);
                rDE = getDataElementUS(x, y);
                assertTrue(tDE instanceof short[]);
                short tde[] = (short[])tDE;
                short rde[] = (short[])rDE;
                assertTrue(Arrays.equals(tde, rde));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tDE = sppsmu2.getDataElements(x, y, tDE, dbu2);
                rDE = getDataElementUS(x, y);
                assertTrue(tDE instanceof short[]);
                short tde[] = (short[])tDE;
                short rde[] = (short[])rDE;
                assertTrue(Arrays.equals(tde, rde));
            }
        }

        tDE = null;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tDE = sppsmi1.getDataElements(x, y, tDE, dbi1);
                rDE = getDataElementI(x, y);
                assertTrue(tDE instanceof int[]);
                int tde[] = (int[])tDE;
                int rde[] = (int[])rDE;
                assertTrue(Arrays.equals(tde, rde));
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                tDE = sppsmi2.getDataElements(x, y, tDE, dbi2);
                rDE = getDataElementI(x, y);
                assertTrue(tDE instanceof int[]);
                int tde[] = (int[])tDE;
                int rde[] = (int[])rDE;
                assertTrue(Arrays.equals(tde, rde));
            }
        }
    }

    public final void testGetDataElementsA(){
        initDataBuffers();
        Object tDE, rDE;
        byte tde[];
        byte rde[];
        short stde[];
        short srde[];
        int itde[];
        int irde[];

        tDE = null;
        tDE = sppsmb1.getDataElements(0, 0, 1, 1, tDE, dbb1);
        rDE = getDataElementsB(0, 0, 1, 1);
        assertTrue(tDE instanceof byte[]);
        tde = (byte[])tDE;
        rde = (byte[])rDE;
        assertTrue(Arrays.equals(tde, rde));

        tDE = null;
        tDE = sppsmb1.getDataElements(0, 0, w/2, h/2, tDE, dbb1);
        rDE = getDataElementsB(0, 0, w/2, h/2);
        assertTrue(tDE instanceof byte[]);
        tde = (byte[])tDE;
        rde = (byte[])rDE;
        assertTrue(Arrays.equals(tde, rde));

        tDE = null;
        tDE = sppsmb1.getDataElements(0, 0, w, h, tDE, dbb1);
        rDE = getDataElementsB(0, 0, w, h);
        assertTrue(tDE instanceof byte[]);
        tde = (byte[])tDE;
        rde = (byte[])rDE;
        assertTrue(Arrays.equals(tde, rde));

        tDE = null;
        tDE = sppsmb2.getDataElements(0, 0, 1, 1, tDE, dbb2);
        rDE = getDataElementsB(0, 0, 1, 1);
        assertTrue(tDE instanceof byte[]);
        tde = (byte[])tDE;
        rde = (byte[])rDE;
        assertTrue(Arrays.equals(tde, rde));

        tDE = null;
        tDE = sppsmb2.getDataElements(0, 0, w/2, h/2, tDE, dbb2);
        rDE = getDataElementsB(0, 0, w/2, h/2);
        assertTrue(tDE instanceof byte[]);
        tde = (byte[])tDE;
        rde = (byte[])rDE;
        assertTrue(Arrays.equals(tde, rde));

        tDE = null;
        tDE = sppsmb2.getDataElements(0, 0, w, h, tDE, dbb2);
        rDE = getDataElementsB(0, 0, w, h);
        assertTrue(tDE instanceof byte[]);
        tde = (byte[])tDE;
        rde = (byte[])rDE;
        assertTrue(Arrays.equals(tde, rde));

        tDE = null;
        tDE = sppsmu1.getDataElements(0, 0, 1, 1, tDE, dbu1);
        rDE = getDataElementsUS(0, 0, 1, 1);
        assertTrue(tDE instanceof short[]);
        stde = (short[])tDE;
        srde = (short[])rDE;
        assertTrue(Arrays.equals(stde, srde));

        tDE = null;
        tDE = sppsmu1.getDataElements(0, 0, w/2, h/2, tDE, dbu1);
        rDE = getDataElementsUS(0, 0, w/2, h/2);
        assertTrue(tDE instanceof short[]);
        stde = (short[])tDE;
        srde = (short[])rDE;
        assertTrue(Arrays.equals(stde, srde));

        tDE = null;
        tDE = sppsmu1.getDataElements(0, 0, w, h, tDE, dbu1);
        rDE = getDataElementsUS(0, 0, w, h);
        assertTrue(tDE instanceof short[]);
        stde = (short[])tDE;
        srde = (short[])rDE;
        assertTrue(Arrays.equals(stde, srde));

        tDE = null;
        tDE = sppsmu2.getDataElements(0, 0, 1, 1, tDE, dbu2);
        rDE = getDataElementsUS(0, 0, 1, 1);
        assertTrue(tDE instanceof short[]);
        stde = (short[])tDE;
        srde = (short[])rDE;
        assertTrue(Arrays.equals(stde, srde));

        tDE = null;
        tDE = sppsmu2.getDataElements(0, 0, w/2, h/2, tDE, dbu2);
        rDE = getDataElementsUS(0, 0, w/2, h/2);
        assertTrue(tDE instanceof short[]);
        stde = (short[])tDE;
        srde = (short[])rDE;
        assertTrue(Arrays.equals(stde, srde));

        tDE = null;
        tDE = sppsmu2.getDataElements(0, 0, w, h, tDE, dbu2);
        rDE = getDataElementsUS(0, 0, w, h);
        assertTrue(tDE instanceof short[]);
        stde = (short[])tDE;
        srde = (short[])rDE;
        assertTrue(Arrays.equals(stde, srde));

        tDE = null;
        tDE = sppsmi1.getDataElements(0, 0, 1, 1, tDE, dbi1);
        rDE = getDataElementsI(0, 0, 1, 1);
        assertTrue(tDE instanceof int[]);
        itde = (int[])tDE;
        irde = (int[])rDE;
        assertTrue(Arrays.equals(itde, irde));

        tDE = null;
        tDE = sppsmi1.getDataElements(0, 0, w/2, h/2, tDE, dbi1);
        rDE = getDataElementsI(0, 0, w/2, h/2);
        assertTrue(tDE instanceof int[]);
        itde = (int[])tDE;
        irde = (int[])rDE;
        assertTrue(Arrays.equals(itde, irde));

        tDE = null;
        tDE = sppsmi1.getDataElements(0, 0, w, h, tDE, dbi1);
        rDE = getDataElementsI(0, 0, w, h);
        assertTrue(tDE instanceof int[]);
        itde = (int[])tDE;
        irde = (int[])rDE;
        assertTrue(Arrays.equals(itde, irde));

        tDE = null;
        tDE = sppsmi2.getDataElements(0, 0, 1, 1, tDE, dbi2);
        rDE = getDataElementsI(0, 0, 1, 1);
        assertTrue(tDE instanceof int[]);
        itde = (int[])tDE;
        irde = (int[])rDE;
        assertTrue(Arrays.equals(tde, rde));

        tDE = null;
        tDE = sppsmi2.getDataElements(0, 0, w/2, h/2, tDE, dbi2);
        rDE = getDataElementsI(0, 0, w/2, h/2);
        assertTrue(tDE instanceof int[]);
        itde = (int[])tDE;
        irde = (int[])rDE;
        assertTrue(Arrays.equals(tde, rde));

        tDE = null;
        tDE = sppsmi2.getDataElements(0, 0, w, h, tDE, dbi2);
        rDE = getDataElementsI(0, 0, w, h);
        assertTrue(tDE instanceof int[]);
        itde = (int[])tDE;
        irde = (int[])rDE;
        assertTrue(Arrays.equals(tde, rde));
    }

    private void initDataBuffers(){
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                dbb1.setElem(y * w + x, byteTestData[y * w + x]);
            }
        }

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                dbb2.setElem(y * scanlineStride + x, byteTestData[y * w + x]);
            }
        }
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                dbu1.setElem(y * w + x, shortTestData[y * w + x]);
            }
        }
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                dbu2.setElem(y * scanlineStride + x, shortTestData[y * w + x]);
            }
        }
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                dbi1.setElem(y * w + x, intTestData[y * w + x]);
            }
        }
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                dbi2.setElem(y * scanlineStride + x, intTestData[y * w + x]);
            }
        }
    }

    public final void testSetDataElements(){
        initTestData();
        byte bdata[];
        short sdata[];
        int idata[];

        int idx = 0;
        byte bde[] = new byte[1];
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                bde[0] = byteTestData[idx++];
                sppsmb1.setDataElements(x, y, bde, dbb1);
            }
        }
        idx = 0;
        bdata = dbb1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb1.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                bde[0] = byteTestData[idx++];
                sppsmb2.setDataElements(x, y, bde, dbb2);
            }
        }
        idx = 0;
        bdata = dbb2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb2.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        idx = 0;
        short sde[] = new short[1];
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                sde[0] = shortTestData[idx++];
                sppsmu1.setDataElements(x, y, sde, dbu1);
            }
        }
        idx = 0;
        sdata = dbu1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu1.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                sde[0] = shortTestData[idx++];
                sppsmu2.setDataElements(x, y, sde, dbu2);
            }
        }
        idx = 0;
        sdata = dbu2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu2.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        idx = 0;
        int ide[] = new int[1];
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                ide[0] = intTestData[idx++];
                sppsmi1.setDataElements(x, y, ide, dbi1);
            }
        }
        idx = 0;
        idata = dbi1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi1.getScanlineStride() + x], intTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                ide[0] = intTestData[idx++];
                sppsmi2.setDataElements(x, y, ide, dbi2);
            }
        }
        idx = 0;
        idata = dbi2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi2.getScanlineStride() + x], intTestData[idx++]);
            }
        }
    }

    public final void testSetDataElementsA(){
        initTestData();
        byte bdata[];
        short sdata[];
        int idata[];

        sppsmb1.setDataElements(0, 0, w, h, byteTestData, dbb1);
        int idx = 0;
        bdata = dbb1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb1.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        sppsmb2.setDataElements(0, 0, w, h, byteTestData, dbb2);
        idx = 0;
        bdata = dbb2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb2.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        sppsmu1.setDataElements(0, 0, w, h, shortTestData, dbu1);
        idx = 0;
        sdata = dbu1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu1.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        sppsmu2.setDataElements(0, 0, w, h, shortTestData, dbu2);
        idx = 0;
        sdata = dbu2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu2.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        sppsmi1.setDataElements(0, 0, w, h, intTestData, dbi1);
        idx = 0;
        idata = dbi1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi1.getScanlineStride() + x], intTestData[idx++]);
            }
        }

        sppsmi2.setDataElements(0, 0, w, h, intTestData, dbi2);
        idx = 0;
        idata = dbi2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi2.getScanlineStride() + x], intTestData[idx++]);
            }
        }
    }

    public final void testSetPixel(){
        initTestData();
        byte bdata[];
        short sdata[];
        int idata[];
        int idx;

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int pixel[] = createPixelFromByteDataElem(byteTestData[idx++]);
                sppsmb1.setPixel(x, y, pixel, dbb1);
            }
        }
        idx = 0;
        bdata = dbb1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb1.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int pixel[] = createPixelFromByteDataElem(byteTestData[idx++]);
                sppsmb2.setPixel(x, y, pixel, dbb2);
            }
        }
        idx = 0;
        bdata = dbb2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb2.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int pixel[] = createPixelFromShortDataElem(shortTestData[idx++]);
                sppsmu1.setPixel(x, y, pixel, dbu1);
            }
        }
        idx = 0;
        sdata = dbu1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu1.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int pixel[] = createPixelFromShortDataElem(shortTestData[idx++]);
                sppsmu2.setPixel(x, y, pixel, dbu2);
            }
        }
        idx = 0;
        sdata = dbu2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu2.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int pixel[] = createPixelFromIntDataElem(intTestData[idx++]);
                sppsmi1.setPixel(x, y, pixel, dbi1);
            }
        }
        idx = 0;
        idata = dbi1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi1.getScanlineStride() + x], intTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int pixel[] = createPixelFromIntDataElem(intTestData[idx++]);
                sppsmi2.setPixel(x, y, pixel, dbi2);
            }
        }
        idx = 0;
        idata = dbi2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi2.getScanlineStride() + x], intTestData[idx++]);
            }
        }
    }

    public final void testSetPixels(){
        initTestData();
        int pixels[];
        byte bdata[];
        short sdata[];
        int idata[];

        pixels = createPixelsFromByteDataElements(byteTestData);
        sppsmb1.setPixels(0, 0, w, h, pixels, dbb1);
        int idx = 0;
        bdata = dbb1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb1.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        sppsmb2.setPixels(0, 0, w, h, pixels, dbb2);
        idx = 0;
        bdata = dbb2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb2.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        pixels = createPixelsFromShortDataElements(shortTestData);
        sppsmu1.setPixels(0, 0, w, h, pixels, dbu1);
        idx = 0;
        sdata = dbu1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu1.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        sppsmu2.setPixels(0, 0, w, h, pixels, dbu2);
        idx = 0;
        sdata = dbu2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu2.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        pixels = createPixelsFromIntDataElements(intTestData);
        sppsmi1.setPixels(0, 0, w, h, pixels, dbi1);
        idx = 0;
        idata = dbi1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi1.getScanlineStride() + x], intTestData[idx++]);
            }
        }

        sppsmi2.setPixels(0, 0, w, h, pixels, dbi2);
        idx = 0;
        idata = dbi2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi2.getScanlineStride() + x], intTestData[idx++]);
            }
        }
        
        // Regression for HARMONY-2431
        try {
            new SinglePixelPackedSampleModel(1, 127, 3, 0, new int[970])
                    .setPixels(Integer.MAX_VALUE, 1, 13, 1, new int[] {},
                            new DataBufferDouble(7, 5));
            fail("ArrayIndexOutOfBoundsException was not thrown"); //$NON-NLS-1$
        } catch (ArrayIndexOutOfBoundsException ex) {
            // expected
        }
    }

    public final void testSetSample(){
        initTestData();
        int sample, idx;
        byte bdata[];
        short sdata[];
        int idata[];

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                byte byteDataElem = byteTestData[idx++];
                for(int b = 0; b < sppsmb1.getNumBands(); b++){
                    sample = createSampleFromByteDataElem(byteDataElem, b);
                    sppsmb1.setSample(x, y, b, sample, dbb1);
                }
            }
        }
        idx = 0;
        bdata = dbb1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb1.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                byte byteDataElem = byteTestData[idx++];
                for(int b = 0; b < sppsmb2.getNumBands(); b++){
                    sample = createSampleFromByteDataElem(byteDataElem, b);
                    sppsmb2.setSample(x, y, b, sample, dbb2);
                }
            }
        }
        idx = 0;
        bdata = dbb2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb2.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                short shortDataElem = shortTestData[idx++];
                for(int b = 0; b < sppsmu1.getNumBands(); b++){
                    sample = createSampleFromShortDataElem(shortDataElem, b);
                    sppsmu1.setSample(x, y, b, sample, dbu1);
                }
            }
        }
        idx = 0;
        sdata = dbu1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu1.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                short shortDataElem = shortTestData[idx++];
                for(int b = 0; b < sppsmu2.getNumBands(); b++){
                    sample = createSampleFromShortDataElem(shortDataElem, b);
                    sppsmu2.setSample(x, y, b, sample, dbu2);
                }
            }
        }
        idx = 0;
        sdata = dbu2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu2.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int intDataElem = intTestData[idx++];
                for(int b = 0; b < sppsmi1.getNumBands(); b++){
                    sample = createSampleFromIntDataElem(intDataElem, b);
                    sppsmi1.setSample(x, y, b, sample, dbi1);
                }
            }
        }
        idx = 0;
        idata = dbi1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi1.getScanlineStride() + x], intTestData[idx++]);
            }
        }

        idx = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int intDataElem = intTestData[idx++];
                for(int b = 0; b < sppsmi2.getNumBands(); b++){
                    sample = createSampleFromIntDataElem(intDataElem, b);
                    sppsmi2.setSample(x, y, b, sample, dbi2);
                }
            }
        }
        idx = 0;
        idata = dbi2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi2.getScanlineStride() + x], intTestData[idx++]);
            }
        }
    }

    public final void testSetSamples(){
        initTestData();
        int idx;
        int samples[];
        byte bdata[];
        short sdata[];
        int idata[];

        for(int b = 0; b < sppsmb1.getNumBands(); b++){
            samples = createSamplesFromByteDataElements(byteTestData, b);
            sppsmb1.setSamples(0, 0, w, h, b, samples, dbb1);
        }
        idx = 0;
        bdata = dbb1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb1.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        for(int b = 0; b < sppsmb2.getNumBands(); b++){
            samples = createSamplesFromByteDataElements(byteTestData, b);
            sppsmb2.setSamples(0, 0, w, h, b, samples, dbb2);
        }
        idx = 0;
        bdata = dbb2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(bdata[y * sppsmb2.getScanlineStride() + x], byteTestData[idx++]);
            }
        }

        for(int b = 0; b < sppsmu1.getNumBands(); b++){
            samples = createSamplesFromShortDataElements(shortTestData, b);
            sppsmu1.setSamples(0, 0, w, h, b, samples, dbu1);
        }
        idx = 0;
        sdata = dbu1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu1.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        for(int b = 0; b < sppsmu2.getNumBands(); b++){
            samples = createSamplesFromShortDataElements(shortTestData, b);
            sppsmu2.setSamples(0, 0, w, h, b, samples, dbu2);
        }
        idx = 0;
        sdata = dbu2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(sdata[y * sppsmu2.getScanlineStride() + x], shortTestData[idx++]);
            }
        }

        for(int b = 0; b < sppsmi1.getNumBands(); b++){
            samples = createSamplesFromIntDataElements(intTestData, b);
            sppsmi1.setSamples(0, 0, w, h, b, samples, dbi1);
        }
        idx = 0;
        idata = dbi1.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi1.getScanlineStride() + x], intTestData[idx++]);
            }
        }

        for(int b = 0; b < sppsmi2.getNumBands(); b++){
            samples = createSamplesFromIntDataElements(intTestData, b);
            sppsmi2.setSamples(0, 0, w, h, b, samples, dbi2);
        }
        idx = 0;
        idata = dbi2.getData();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(idata[y * sppsmi2.getScanlineStride() + x], intTestData[idx++]);
            }
        }
    }

    private void initTestData(){
        int maxByte = 255;
        int maxShort = 65535;
        long maxInt = (1l << 32) -1;
        Random r = new Random();
        int size = w * h;

        byteTestData = new byte[size];
        for(int i = 0; i < size; i++){
            byteTestData[i] = (byte)((int)(r.nextDouble() * maxByte) & 0xff);
        }

        shortTestData = new short[size];
        for(int i = 0; i < size; i++){
            shortTestData[i] = (short)((int)(r.nextDouble() * maxShort) & 0xffff);
        }

        intTestData = new int[size];
        for(int i = 0; i < size; i++){
            intTestData[i] = (int)(r.nextDouble() * maxInt);
        }
    }

    private int[] getPixelB(int x, int y){
        int pixel[] = new int[4];
        int pix = byteTestData[y * w + x] & 0xff;
        for(int i = 0; i < 4; i++){
            pixel[i] = (pix & bitMaskB[i]) >> bitOffsetsB[i];
        }
        return pixel;
    }

    private int[] getPixelUS(int x, int y){
        int pixel[] = new int[4];
        int pix = shortTestData[y * w + x] & 0xffff;
        for(int i = 0; i < 4; i++){
            pixel[i] = (pix & bitMaskUS[i]) >> bitOffsetsUS[i];
        }
        return pixel;
    }

    private int[] getPixelI(int x, int y){
        int pixel[] = new int[4];
        int pix = intTestData[y * w + x];
        for(int i = 0; i < 4; i++){
            pixel[i] = (pix & bitMaskI[i]) >> bitOffsetsI[i];
        }
        return pixel;
    }

    private int[] getPixelsB(int x, int y, int width, int height){
        int pixel[] = new int[4 * width * height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                int pix = byteTestData[y1 * w + x1] & 0xff;
                for(int i = 0; i < 4; i++){
                    pixel[idx++] = (pix & bitMaskB[i]) >> bitOffsetsB[i];
                }
            }
        }
        return pixel;
    }

    private int[] getPixelsUS(int x, int y, int width, int height){
        int pixel[] = new int[4 * width * height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                int pix = shortTestData[y1 * w + x1] & 0xffff;
                for(int i = 0; i < 4; i++){
                    pixel[idx++] = (pix & bitMaskUS[i]) >> bitOffsetsUS[i];
                }
            }
        }
        return pixel;
    }

    private int[] getPixelsI(int x, int y, int width, int height){
        int pixel[] = new int[4 * width * height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                int pix = intTestData[y1 * w + x1];
                for(int i = 0; i < 4; i++){
                    pixel[idx++] = (pix & bitMaskI[i]) >> bitOffsetsI[i];
                }
            }
        }
        return pixel;
    }

    private int getSampleB(int x, int y, int b){
        int pix = byteTestData[y * w + x] & 0xff;
        int sample = (pix & bitMaskB[b]) >> bitOffsetsB[b];
        return sample;
    }

    private int getSampleUS(int x, int y, int b){
        int pix = shortTestData[y * w + x] & 0xffff;
        int sample = (pix & bitMaskUS[b]) >> bitOffsetsUS[b];
        return sample;
    }

    private int getSampleI(int x, int y, int b){
        int pix = intTestData[y * w + x];
        int sample = (pix & bitMaskI[b]) >> bitOffsetsI[b];
        return sample;
    }

    private int[] getSamplesB(int x, int y, int width, int height, int b){
        int samples[] = new int[width * height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                int pix = byteTestData[y1 * w + x1] & 0xff;
                samples[idx++] = (pix & bitMaskB[b]) >> bitOffsetsB[b];
            }
        }
        return samples;
    }

    private int[] getSamplesUS(int x, int y, int width, int height, int b){
        int samples[] = new int[width * height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                int pix = shortTestData[y1 * w + x1] & 0xffff;
                samples[idx++] = (pix & bitMaskUS[b]) >> bitOffsetsUS[b];
            }
        }
        return samples;
    }

    private int[] getSamplesI(int x, int y, int width, int height, int b){
        int samples[] = new int[width * height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                int pix = intTestData[y1 * w + x1];
                samples[idx++] = (pix & bitMaskI[b]) >> bitOffsetsI[b];
            }
        }
        return samples;
    }

    private Object getDataElementB(int x, int y){
        byte pixel[] = new byte[1];
        pixel[0] = byteTestData[y * w + x];
        return pixel;
    }

    private Object getDataElementUS(int x, int y){
        short pixel[] = new short[1];
        pixel[0] = shortTestData[y * w + x];
        return pixel;
    }

    private Object getDataElementI(int x, int y){
        int pixel[] = new int[1];
        pixel[0] = intTestData[y * w + x];
        return pixel;
    }

    private Object getDataElementsB(int x, int y, int width, int height){
        byte pixels[] = new byte[width*height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                pixels[idx++] = byteTestData[y1 * w + x1];
            }
        }
        return pixels;
    }

    private Object getDataElementsUS(int x, int y, int width, int height){
        short pixels[] = new short[width*height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                pixels[idx++] = shortTestData[y1 * w + x1];
            }
        }
        return pixels;
    }

    private Object getDataElementsI(int x, int y, int width, int height){
        int pixels[] = new int[width*height];
        int idx = 0;
        for(int y1 = y; y1 < y + height; y1++){
            for(int x1 = x; x1 < x + width; x1++){
                pixels[idx++] = intTestData[y1 * w + x1];
            }
        }
        return pixels;
    }

    private int[] createPixelFromByteDataElem(byte dataElem){
        int pixel[] = new int[bitMaskB.length];
        for(int i = 0; i < bitMaskB.length; i++){
            pixel[i] = (dataElem & bitMaskB[i]) >>> bitOffsetsB[i];
        }
        return pixel;
    }

    private int[] createPixelFromShortDataElem(short dataElem){
        int pixel[] = new int[bitMaskUS.length];
        for(int i = 0; i < bitMaskUS.length; i++){
            pixel[i] = (dataElem & bitMaskUS[i]) >>> bitOffsetsUS[i];
        }
        return pixel;
    }

    private int[] createPixelFromIntDataElem(int dataElem){
        int pixel[] = new int[bitMaskI.length];
        for(int i = 0; i < bitMaskI.length; i++){
            pixel[i] = (dataElem & bitMaskI[i]) >>> bitOffsetsI[i];
        }
        return pixel;
    }

    private int[] createPixelsFromByteDataElements(byte byteData[]){
        int pixel[] = new int[bitMaskB.length * byteData.length];
        int idx = 0;
        for (byte element : byteData) {
            for(int i = 0; i < bitMaskB.length; i++){
                pixel[idx++] = (element & bitMaskB[i]) >>> bitOffsetsB[i];
            }
        }
        return pixel;
    }

    private int[] createPixelsFromShortDataElements(short shortData[]){
        int pixel[] = new int[bitMaskUS.length * shortData.length];
        int idx = 0;
        for (short element : shortData) {
            for(int i = 0; i < bitMaskUS.length; i++){
                pixel[idx++] = (element & bitMaskUS[i]) >>> bitOffsetsUS[i];
            }
        }
        return pixel;
    }

    private int[] createPixelsFromIntDataElements(int intData[]){
        int pixel[] = new int[bitMaskI.length * intData.length];
        int idx = 0;
        for (int element : intData) {
            for(int i = 0; i < bitMaskI.length; i++){
                pixel[idx++] = (element & bitMaskI[i]) >>> bitOffsetsI[i];
            }
        }
        return pixel;
    }

    private int createSampleFromByteDataElem(byte dataElem, int bank){
        return (dataElem & bitMaskB[bank]) >>> bitOffsetsB[bank];
    }

    private int createSampleFromShortDataElem(short dataElem, int bank){
        return (dataElem & bitMaskUS[bank]) >>> bitOffsetsUS[bank];
    }

    private int createSampleFromIntDataElem(int dataElem, int bank){
        return (dataElem & bitMaskI[bank]) >>> bitOffsetsI[bank];
    }

    private int[] createSamplesFromByteDataElements(byte byteData[], int bank){
        int samples[] = new int[byteData.length];
        for(int i = 0; i < byteData.length; i++){
            samples[i] = (byteData[i] & bitMaskB[bank]) >>> bitOffsetsB[bank];
        }
        return samples;
    }

    private int[] createSamplesFromShortDataElements(short shortData[], int bank){
        int samples[] = new int[shortData.length];
        for(int i = 0; i < shortData.length; i++){
            samples[i] = (shortData[i] & bitMaskUS[bank]) >>> bitOffsetsUS[bank];
        }
        return samples;
    }

    private int[] createSamplesFromIntDataElements(int intData[], int bank){
        int samples[] = new int[intData.length];
        for(int i = 0; i < intData.length; i++){
            samples[i] = (intData[i] & bitMaskI[bank]) >>> bitOffsetsI[bank];
        }
        return  samples;
    }

}
