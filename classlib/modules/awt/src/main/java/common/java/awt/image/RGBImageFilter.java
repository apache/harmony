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


public abstract class RGBImageFilter extends ImageFilter {

    protected ColorModel origmodel;

    protected ColorModel newmodel;

    protected boolean canFilterIndexColorModel;

    public RGBImageFilter() {}

    public IndexColorModel filterIndexColorModel(IndexColorModel icm) {
        int transferType = icm.getTransferType();
        int bits = icm.getPixelSize();
        int mapSize = icm.getMapSize();
        int colorMap[] = new int[mapSize];
        int filteredColorMap[] = new int[mapSize];
        icm.getRGBs(colorMap);
        int trans = -1;
        boolean hasAlpha = false;
        for(int i = 0; i < mapSize; i++){
            filteredColorMap[i] = filterRGB(-1, -1, colorMap[i]);
            int alpha = filteredColorMap[i] >>> 24;
            if(alpha != 0xff){
                if(!hasAlpha) {
                    hasAlpha = true;
                }
                if(alpha == 0 && trans < 0) {
                    trans = i;
                }
            }
        }

        return new IndexColorModel(bits, mapSize, filteredColorMap, 0,
                hasAlpha, trans, transferType);
    }

    public void substituteColorModel(ColorModel oldcm, ColorModel newcm) {
        origmodel = oldcm;
        newmodel = newcm;
    }

    @Override
    public void setColorModel(ColorModel model) {
        if(model instanceof IndexColorModel &&
                canFilterIndexColorModel){
            IndexColorModel icm = (IndexColorModel) model;
            ColorModel filteredModel = filterIndexColorModel(icm);
            substituteColorModel(model, filteredModel);
            consumer.setColorModel(filteredModel);
        }else{
            consumer.setColorModel(ColorModel.getRGBdefault());
        }
    }

    @Override
    public void setPixels(int x, int y, int w, int h, ColorModel model, 
            int[] pixels, int off, int scansize) {
        
        if(model == null || model == origmodel){
            consumer.setPixels(x, y, w, h, newmodel, pixels, off, scansize);
        }else{
            int rgbPixels[] = new int[w];
            for(int sy = y, pixelsOff = off; sy < y + h; 
                sy++, pixelsOff += scansize){
                
                for(int sx = x, idx = 0; sx < x + w; sx++, idx++){
                    rgbPixels[idx] = model.getRGB(pixels[pixelsOff + idx]);
                }
                filterRGBPixels(x, sy, w, 1, rgbPixels, 0, w);
            }
        }
    }

    @Override
    public void setPixels(int x, int y, int w, int h, ColorModel model, 
            byte[] pixels, int off, int scansize) {
        
        if(model == null || model == origmodel){
            consumer.setPixels(x, y, w, h, newmodel, pixels, off, scansize);
        }else{
            int rgbPixels[] = new int[w];
            for(int sy = y, pixelsOff = off; sy < y + h; 
                sy++, pixelsOff += scansize){
                
                for(int sx = x, idx = 0; sx < x + w; sx++, idx++){
                    rgbPixels[idx] = 
                        model.getRGB(pixels[pixelsOff + idx] & 0xff);
                }
                filterRGBPixels(x, sy, w, 1, rgbPixels, 0, w);
            }
        }
    }

    public void filterRGBPixels(int x, int y, int w, int h, 
            int[] pixels, int off, int scansize) {
        
        for(int sy = y, lineOff = off; sy < y + h; sy++, lineOff += scansize){
            for(int sx = x, idx = 0; sx < x + w; sx++, idx++){
                pixels[lineOff + idx] = 
                    filterRGB(sx, sy, pixels[lineOff + idx]);
            }
        }
        consumer.setPixels(x, y, w, h, ColorModel.getRGBdefault(), 
                pixels, off, scansize);
    }

    public abstract int filterRGB(int x, int y, int rgb);

}

