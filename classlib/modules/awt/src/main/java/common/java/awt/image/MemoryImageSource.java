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

import java.util.Hashtable;
import java.util.Vector;

import org.apache.harmony.awt.internal.nls.Messages;

public class MemoryImageSource implements ImageProducer {

    int width;
    int height;
    ColorModel cm;
    byte bData[];
    int iData[];
    int offset;
    int scanline;
    Hashtable<?, ?> properties;
    Vector<ImageConsumer> consumers;
    boolean animated;
    boolean fullbuffers;
    int dataType;

    static final int DATA_TYPE_BYTE = 0;
    static final int DATA_TYPE_INT = 1;

    public MemoryImageSource(int w, int h, ColorModel cm, int pix[],
            int off, int scan, Hashtable<?, ?> props) {
        init(w, h, cm, pix, off, scan, props);
    }

    public MemoryImageSource(int w, int h, ColorModel cm, byte pix[],
            int off, int scan, Hashtable<?, ?> props) {
        init(w, h, cm, pix, off, scan, props);
    }

    public MemoryImageSource(int w, int h, int pix[], int off, int scan,
            Hashtable<?, ?> props) {
        init(w, h, ColorModel.getRGBdefault(), pix, off, scan, props);
    }

    public MemoryImageSource(int w, int h, ColorModel cm, int pix[],
            int off, int scan) {
        init(w, h, cm, pix, off, scan, null);
    }

    public MemoryImageSource(int w, int h, ColorModel cm, byte pix[],
            int off, int scan) {
        init(w, h, cm, pix, off, scan, null);
    }

    public MemoryImageSource(int w, int h, int pix[], int off, int scan) {
        init(w, h, ColorModel.getRGBdefault(), pix, off, scan, null);
    }

    public synchronized boolean isConsumer(ImageConsumer ic) {
        return consumers.contains(ic);
    }

    public void startProduction(ImageConsumer ic) {
        if(!isConsumer(ic) && ic != null) {
            consumers.addElement(ic);
        }
        try{
            setHeader(ic);
            setPixels(ic, 0, 0, width, height);
            if(animated){
                ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
            }else{
                ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
                if(isConsumer(ic)) {
                    removeConsumer(ic);
                }
            }
        }catch(Exception e){
            if(isConsumer(ic)) {
                ic.imageComplete(ImageConsumer.IMAGEERROR);
            }
            if(isConsumer(ic)) {
                removeConsumer(ic);
            }
        }
    }

    public void requestTopDownLeftRightResend(ImageConsumer ic) {
    }

    public synchronized void removeConsumer(ImageConsumer ic) {
        consumers.removeElement(ic);
    }

    public synchronized void addConsumer(ImageConsumer ic) {
        if(ic == null || consumers.contains(ic)) {
            return;
        }
        consumers.addElement(ic);
    }

    public synchronized void newPixels(int newpix[], ColorModel newmodel,
            int offset, int scansize) {
        this.dataType = DATA_TYPE_INT;
        this.iData = newpix;
        this.cm = newmodel;
        this.offset = offset;
        this.scanline = scansize;
        newPixels();
    }

    public synchronized void newPixels(byte newpix[], ColorModel newmodel,
            int offset, int scansize) {
        this.dataType = DATA_TYPE_BYTE;
        this.bData = newpix;
        this.cm = newmodel;
        this.offset = offset;
        this.scanline = scansize;
        newPixels();
    }

    public synchronized void setFullBufferUpdates(boolean fullbuffers) {
        if(this.fullbuffers == fullbuffers) {
            return;
        }
        this.fullbuffers = fullbuffers;
        if(animated){
            Object consAr[] = consumers.toArray();
            for (Object element : consAr) {
                ImageConsumer con = (ImageConsumer)element;
                try{
                    if(fullbuffers){
                        con.setHints(ImageConsumer.TOPDOWNLEFTRIGHT |
                                ImageConsumer.COMPLETESCANLINES);
                    }else{
                        con.setHints(ImageConsumer.RANDOMPIXELORDER);
                    }
                }catch(Exception e){
                    if(isConsumer(con)) {
                        con.imageComplete(ImageConsumer.IMAGEERROR);
                    }
                    if(isConsumer(con)) {
                        removeConsumer(con);
                    }
                }
            }
        }
    }

    public synchronized void setAnimated(boolean animated) {
        if(this.animated == animated) {
            return;
        }
        Object consAr[] = consumers.toArray();
        for (Object element : consAr) {
            ImageConsumer con = (ImageConsumer)element;
            try{
                con.imageComplete(ImageConsumer.STATICIMAGEDONE);
            }catch(Exception e){
                if(isConsumer(con)) {
                    con.imageComplete(ImageConsumer.IMAGEERROR);
                }
            }
            if(isConsumer(con)){
                removeConsumer(con);
            }
        }
        this.animated = animated;
    }

    public synchronized void newPixels(int x, int y, int w, int h,
            boolean framenotify) {
        if(animated){
            if(fullbuffers){
                x = 0;
                y = 0;
                w = width;
                h = height;
            }else{
                if(x < 0){
                    w += x;
                    x = 0;
                }
                if(w > width) {
                    w = width - x;
                }
                if(y < 0){
                    h += y;
                    y = 0;
                }
            }
            if(h > height) {
                h = height - y;
            }
            Object consAr[] = consumers.toArray();
            for (Object element : consAr) {
                ImageConsumer con = (ImageConsumer)element;
                try{
                    if(w > 0 && h > 0) {
                        setPixels(con, x, y, w, h);
                    }
                    if(framenotify) {
                        con.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
                    }
                }catch(Exception ex){
                    if(isConsumer(con)) {
                        con.imageComplete(ImageConsumer.IMAGEERROR);
                    }
                    if(isConsumer(con)) {
                        removeConsumer(con);
                    }
                }
            }
        }
    }

    public synchronized void newPixels(int x, int y, int w, int h) {
        newPixels(x, y, w, h, true);
    }

    public void newPixels() {
        newPixels(0, 0, width, height, true);
    }

    private void init(int width, int height, ColorModel model, byte pixels[],
            int off, int scan, Hashtable<?, ?> prop){

        this.width = width;
        this.height = height;
        this.cm = model;
        this.bData = pixels;
        this.offset = off;
        this.scanline = scan;
        this.properties = prop;
        this.dataType = DATA_TYPE_BYTE;
        this.consumers = new Vector<ImageConsumer>();

    }

    private void init(int width, int height, ColorModel model, int pixels[],
            int off, int scan, Hashtable<?, ?> prop){

        this.width = width;
        this.height = height;
        this.cm = model;
        this.iData = pixels;
        this.offset = off;
        this.scanline = scan;
        this.properties = prop;
        this.dataType = DATA_TYPE_INT;
        this.consumers = new Vector<ImageConsumer>();
    }

    private void setPixels(ImageConsumer con, int x, int y, int w, int h){
        int pixelOff = scanline * y + offset + x;

        switch(dataType){
        case DATA_TYPE_BYTE:
            con.setPixels(x, y, w, h, cm, bData, pixelOff, scanline);
            break;
        case DATA_TYPE_INT:
            con.setPixels(x, y, w, h, cm, iData, pixelOff, scanline);
            break;
        default:
            // awt.22A=Wrong type of pixels array
            throw new IllegalArgumentException(Messages.getString("awt.22A")); //$NON-NLS-1$
        }
    }

    private synchronized void setHeader(ImageConsumer con){
        con.setDimensions(width, height);
        con.setProperties(properties);
        con.setColorModel(cm);
        con.setHints(animated ? (fullbuffers ? (ImageConsumer.TOPDOWNLEFTRIGHT |
                ImageConsumer.COMPLETESCANLINES) : ImageConsumer.RANDOMPIXELORDER) :
                (ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES |
                 ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME));
    }

}

