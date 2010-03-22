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

public abstract class SampleModel {

    protected int width;

    protected int height;

    protected int numBands;

    protected int dataType;

    public SampleModel(int dataType, int w, int h, int numBands) {
        if (w <= 0 || h <= 0) {
            // awt.22E=w or h is less than or equal to zero
            throw new IllegalArgumentException(Messages.getString("awt.22E")); //$NON-NLS-1$
        }

        double squre = ((double) w) * ((double) h);
        if (squre >= Integer.MAX_VALUE) {
            // awt.22F=The product of w and h is greater than Integer.MAX_VALUE
            throw new IllegalArgumentException(Messages.getString("awt.22F")); //$NON-NLS-1$
        }

        if (dataType < DataBuffer.TYPE_BYTE ||
                dataType > DataBuffer.TYPE_DOUBLE &&
                dataType != DataBuffer.TYPE_UNDEFINED) {
            // awt.230=dataType is not one of the supported data types
            throw new IllegalArgumentException(Messages.getString("awt.230")); //$NON-NLS-1$
        }

        if (numBands < 1) {
            // awt.231=Number of bands must be more then 0
            throw new IllegalArgumentException(Messages.getString("awt.231")); //$NON-NLS-1$
        }

        this.dataType = dataType;
        this.width = w;
        this.height = h;
        this.numBands = numBands;

    }

    public abstract Object getDataElements(int x, int y, Object obj,
            DataBuffer data);

    public Object getDataElements(int x, int y, int w, int h, Object obj,
            DataBuffer data) {
        int numDataElements = getNumDataElements();
        int idx = 0;

        switch (getTransferType()) {
        case DataBuffer.TYPE_BYTE:
            byte bdata[];
            byte bbuf[] = null;

            if (obj == null) {
                bdata = new byte[numDataElements * w * h];
            } else {
                bdata = (byte[]) obj;
            }

            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    bbuf = (byte[]) getDataElements(j, i, bbuf, data);
                    for (int n = 0; n < numDataElements; n++) {
                        bdata[idx++] = bbuf[n];
                    }
                }
            }
            obj = bdata;
            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            short sdata[];
            short sbuf[] = null;

            if (obj == null) {
                sdata = new short[numDataElements * w * h];
            } else {
                sdata = (short[]) obj;
            }

            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    sbuf = (short[]) getDataElements(j, i, sbuf, data);
                    for (int n = 0; n < numDataElements; n++) {
                        sdata[idx++] = sbuf[n];
                    }
                }
            }
            obj = sdata;
            break;

        case DataBuffer.TYPE_INT:
            int idata[];
            int ibuf[] = null;

            if (obj == null) {
                idata = new int[numDataElements * w * h];
            } else {
                idata = (int[]) obj;
            }

            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    ibuf = (int[]) getDataElements(j, i, ibuf, data);
                    for (int n = 0; n < numDataElements; n++) {
                        idata[idx++] = ibuf[n];
                    }
                }
            }
            obj = idata;
            break;

        case DataBuffer.TYPE_FLOAT:
            float fdata[];
            float fbuf[] = null;

            if (obj == null) {
                fdata = new float[numDataElements * w * h];
            } else {
                fdata = (float[]) obj;
            }

            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    fbuf = (float[]) getDataElements(j, i, fbuf, data);
                    for (int n = 0; n < numDataElements; n++) {
                        fdata[idx++] = fbuf[n];
                    }
                }
            }
            obj = fdata;
            break;

        case DataBuffer.TYPE_DOUBLE:
            double ddata[];
            double dbuf[] = null;

            if (obj == null) {
                ddata = new double[numDataElements * w * h];
            } else {
                ddata = (double[]) obj;
            }

            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    dbuf = (double[]) getDataElements(j, i, dbuf, data);
                    for (int n = 0; n < numDataElements; n++) {
                        ddata[idx++] = dbuf[n];
                    }
                }
            }
            obj = ddata;
            break;

        }

        return obj;
    }

    public abstract void setDataElements(int x, int y, Object obj,
            DataBuffer data);

    public void setDataElements(int x, int y, int w, int h, Object obj,
            DataBuffer data) {
        int numDataElements = getNumDataElements();
        int idx = 0;

        switch (getTransferType()) {
        case DataBuffer.TYPE_BYTE:
            byte bbuf[] = new byte[numDataElements];
            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    for (int n = 0; n < numDataElements; n++) {
                        bbuf[n] = ((byte[]) obj)[idx++];
                    }
                    setDataElements(j, i, bbuf, data);
                }
            }

            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            short sbuf[] = new short[numDataElements];
            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    for (int n = 0; n < numDataElements; n++) {
                        sbuf[n] = ((short[]) obj)[idx++];
                    }
                    setDataElements(j, i, sbuf, data);
                }
            }
            break;

        case DataBuffer.TYPE_INT:
            int ibuf[] = new int[numDataElements];
            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    for (int n = 0; n < numDataElements; n++) {
                        ibuf[n] = ((int[]) obj)[idx++];
                    }
                    setDataElements(j, i, ibuf, data);
                }
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float fbuf[] = new float[numDataElements];
            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    for (int n = 0; n < numDataElements; n++) {
                        fbuf[n] = ((float[]) obj)[idx++];
                    }
                    setDataElements(j, i, fbuf, data);
                }
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double dbuf[] = new double[numDataElements];
            for (int i = y; i < y + h; i++) {
                for (int j = x; j < x + w; j++) {
                    for (int n = 0; n < numDataElements; n++) {
                        dbuf[n] = ((double[]) obj)[idx++];
                    }
                    setDataElements(j, i, dbuf, data);
                }
            }
            break;

        }
    }

    public abstract SampleModel createSubsetSampleModel(int bands[]);

    public abstract SampleModel createCompatibleSampleModel(int a0, int a1);

    public int[] getPixel(int x, int y, int iArray[], DataBuffer data) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        int pixel[];

        if (iArray == null) {
            pixel = new int[numBands];
        } else {
            pixel = iArray;
        }

        for (int i = 0; i < numBands; i++) {
            pixel[i] = getSample(x, y, i, data);
        }

        return pixel;
    }

    public void setPixel(int x, int y, int iArray[], DataBuffer data) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        for (int i = 0; i < numBands; i++) {
            setSample(x, y, i, iArray[i], data);
        }
    }

    public float[] getPixel(int x, int y, float fArray[], DataBuffer data) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        float pixel[];

        if (fArray == null) {
            pixel = new float[numBands];
        } else {
            pixel = fArray;
        }

        for (int i = 0; i < numBands; i++) {
            pixel[i] = getSampleFloat(x, y, i, data);
        }

        return pixel;
    }

    public void setPixel(int x, int y, float fArray[], DataBuffer data) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        for (int i = 0; i < numBands; i++) {
            setSample(x, y, i, fArray[i], data);
        }
    }

    public double[] getPixel(int x, int y, double dArray[], DataBuffer data) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        double pixel[];

        if (dArray == null) {
            pixel = new double[numBands];
        } else {
            pixel = dArray;
        }

        for (int i = 0; i < numBands; i++) {
            pixel[i] = getSampleDouble(x, y, i, data);
        }

        return pixel;
    }

    public void setPixel(int x, int y, double dArray[], DataBuffer data) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        for (int i = 0; i < numBands; i++) {
            setSample(x, y, i, dArray[i], data);
        }
    }

    public abstract int getSample(int x, int y, int b, DataBuffer data);

    public float getSampleFloat(int x, int y, int b, DataBuffer data) {
        return getSample(x, y, b, data);
    }

    public double getSampleDouble(int x, int y, int b, DataBuffer data) {
        return getSample(x, y, b, data);
    }

    public int[] getPixels(int x, int y, int w, int h, int iArray[],
            DataBuffer data) {
        if (x < 0 || y < 0 || x + w > this.width || y + h > this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        int pixels[];
        int idx = 0;

        if (iArray == null) {
            pixels = new int[w * h * numBands];
        } else {
            pixels = iArray;
        }

        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                for (int n = 0; n < numBands; n++) {
                    pixels[idx++] = getSample(j, i, n, data);
                }
            }
        }
        return pixels;
    }

    public void setPixels(int x, int y, int w, int h, int iArray[],
            DataBuffer data) {
        if (x < 0 || y < 0 || x + w > this.width || y + h > this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        int idx = 0;
        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                for (int n = 0; n < numBands; n++) {
                    setSample(j, i, n, iArray[idx++], data);
                }
            }
        }
    }

    public float[] getPixels(int x, int y, int w, int h, float fArray[],
            DataBuffer data) {
        if (x < 0 || y < 0 || x + w > this.width || y + h > this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        float pixels[];
        int idx = 0;

        if (fArray == null) {
            pixels = new float[w * h * numBands];
        } else {
            pixels = fArray;
        }

        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                for (int n = 0; n < numBands; n++) {
                    pixels[idx++] = getSampleFloat(j, i, n, data);
                }
            }
        }
        return pixels;
    }

    public void setPixels(int x, int y, int w, int h, float fArray[],
            DataBuffer data) {
        if (x < 0 || y < 0 || x + w > this.width || y + h > this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        int idx = 0;
        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                for (int n = 0; n < numBands; n++) {
                    setSample(j, i, n, fArray[idx++], data);
                }
            }
        }
    }

    public double[] getPixels(int x, int y, int w, int h, double dArray[],
            DataBuffer data) {
        if (x < 0 || y < 0 || x + w > this.width || y + h > this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        double pixels[];
        int idx = 0;

        if (dArray == null) {
            pixels = new double[w * h * numBands];
        } else {
            pixels = dArray;
        }

        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                for (int n = 0; n < numBands; n++) {
                    pixels[idx++] = getSampleDouble(j, i, n, data);
                }
            }
        }
        return pixels;
    }

    public void setPixels(int x, int y, int w, int h, double dArray[],
            DataBuffer data) {
        if (x < 0 || y < 0 || x + w > this.width || y + h > this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }
        int idx = 0;
        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                for (int n = 0; n < numBands; n++) {
                    setSample(j, i, n, dArray[idx++], data);
                }
            }
        }
    }

    public abstract void setSample(int x, int y, int b, int s, DataBuffer data);

    public int[] getSamples(int x, int y, int w, int h, int b, int iArray[],
            DataBuffer data) {
        int samples[];
        int idx = 0;

        if (iArray == null) {
            samples = new int[w * h];
        } else {
            samples = iArray;
        }

        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                samples[idx++] = getSample(j, i, b, data);
            }
        }

        return samples;
    }

    public void setSamples(int x, int y, int w, int h, int b, int iArray[],
            DataBuffer data) {
        int idx = 0;
        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                setSample(j, i, b, iArray[idx++], data);
            }
        }
    }

    public float[] getSamples(int x, int y, int w, int h, int b,
            float fArray[], DataBuffer data) {
        float samples[];
        int idx = 0;

        if (fArray == null) {
            samples = new float[w * h];
        } else {
            samples = fArray;
        }

        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                samples[idx++] = getSampleFloat(j, i, b, data);
            }
        }

        return samples;
    }

    public void setSamples(int x, int y, int w, int h, int b, float fArray[],
            DataBuffer data) {
        int idx = 0;
        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                setSample(j, i, b, fArray[idx++], data);
            }
        }
    }

    public double[] getSamples(int x, int y, int w, int h, int b,
            double dArray[], DataBuffer data) {
        double samples[];
        int idx = 0;

        if (dArray == null) {
            samples = new double[w * h];
        } else {
            samples = dArray;
        }

        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                samples[idx++] = getSampleDouble(j, i, b, data);
            }
        }

        return samples;
    }

    public void setSamples(int x, int y, int w, int h, int b, double dArray[],
            DataBuffer data) {
        int idx = 0;
        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                setSample(j, i, b, dArray[idx++], data);
            }
        }
    }

    public void setSample(int x, int y, int b, float s, DataBuffer data) {
        setSample(x, y, b, (int) s, data);
    }

    public void setSample(int x, int y, int b, double s, DataBuffer data) {
        setSample(x, y, b, (int) s, data);
    }

    public abstract DataBuffer createDataBuffer();

    public abstract int getSampleSize(int band);

    public abstract int[] getSampleSize();

    public final int getWidth() {
        return width;
    }

    public int getTransferType() {
        return dataType;
    }

    public abstract int getNumDataElements();

    public final int getNumBands() {
        return numBands;
    }

    public final int getHeight() {
        return height;
    }

    public final int getDataType() {
        return dataType;
    }

}

