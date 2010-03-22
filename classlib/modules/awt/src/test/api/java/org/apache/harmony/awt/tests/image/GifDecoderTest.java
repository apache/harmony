/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.awt.tests.image;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.net.URL;
import java.util.Hashtable;

import junit.framework.TestCase;

public class GifDecoderTest extends TestCase implements ImageConsumer{

    ImageProducer ip;
    Object lock = new Object();
    ColorModel cm = ColorModel.getRGBdefault();
    int state = 0;
    boolean started = false;

    private final ClassLoader c = ClassLoader.getSystemClassLoader();

    private Image createImage(String name) {
        final URL path = c.getResource("../resources/images/" + name);
        assertNotNull("Resource not found: " + name, path); //$NON-NLS-1$
        return Toolkit.getDefaultToolkit().createImage(path);
    }

    public void setColorModel(ColorModel model){}
    public void setHints(int hintflags){}
    public void setDimensions(int width, int height){}
    public void setProperties(Hashtable<?,?> props){}

    public void imageComplete(int status){

        state = status;
        if(state == STATICIMAGEDONE) stopProduction(); 
        synchronized(lock){
            lock.notify();
        }

    }

    public void setPixels(int x, int y, int w, int h, ColorModel model,
        byte[] pixels, int off, int scansize) {

        cm = model;
        synchronized(lock){
            lock.notify();
        }

    }

    public void setPixels(int x, int y, int w, int h, ColorModel model,
        int[] pixels, int off, int scansize) {

        cm = model;
        synchronized(lock){
            lock.notify();
        }

    }

    private void startProduction(ImageProducer ip){
        if(!started){
            this.ip = ip;
            started = true;
            ip.startProduction(this);
        }

    }

    private synchronized void stopProduction(){
        ip.removeConsumer(this);
    }

    public void test_H5491() throws Exception{

        Image img = createImage("test.gif");
        while(state != STATICIMAGEDONE){
            startProduction(img.getSource());
            synchronized(lock){
                try{
                    lock.wait(2000);
                } catch(InterruptedException e){}
            }

            if(cm == null)  fail("ColorModel in setPixels is null!");
        }

    }
}
