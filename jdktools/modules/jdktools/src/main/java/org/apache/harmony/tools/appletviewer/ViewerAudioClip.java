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

package org.apache.harmony.tools.appletviewer;

import java.applet.AudioClip;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

class ViewerAudioClip implements AudioClip {

    private ClipImpl clip;

    public ViewerAudioClip(URL url) {
        AudioFormat af = null;
        AudioInputStream ais = null;
        SourceDataLine line = null;
        try {
            ais = AudioSystem.getAudioInputStream(url);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        af = ais.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);

        boolean isSupported = AudioSystem.isLineSupported(info);
        if (!isSupported){
            AudioFormat tf = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                af.getSampleRate(),
                16,
                af.getChannels(),
                af.getChannels() << 1,
                af.getSampleRate(),
                false);
                ais = AudioSystem.getAudioInputStream(tf, ais);
                af = ais.getFormat();
                info = new DataLine.Info(SourceDataLine.class, af);
        }
        try{
            line = (SourceDataLine) AudioSystem.getLine(info);
        }catch (Exception e){
            e.printStackTrace();
        }

        clip = new ClipImpl(af, ais, line);

    }

    public void loop() {
        if(clip != null)
            clip.loop();
    }

    public void play() {
        if(clip != null)
            clip.play();
    }

    public void stop() {
        if(clip != null)
            clip.stop();
    }

    private static class ClipImpl implements AudioClip, Runnable {

        static final int BufferSize = 1024;
        static final int UNLIMITED = -1;

        AudioFormat af;
        AudioInputStream ais;
        SourceDataLine line;
        Thread clip;
        boolean started;
        int streamLength;
        int count;

        ClipImpl(AudioFormat af, AudioInputStream ais, SourceDataLine line){
            this.af = af;
            this.ais = ais;
            this.line = line;

            if(ais.getFrameLength() == AudioSystem.NOT_SPECIFIED ||
                af.getFrameSize() == AudioSystem.NOT_SPECIFIED){

                streamLength = -1;
            }

            long length = ais.getFrameLength() * af.getFrameSize();

            if(length > Integer.MAX_VALUE){
                streamLength = -1;
            }

            streamLength = (int)length;
            clip = new Thread(this);
        }

        public void run(){
            if(streamLength < 0) return;

            started = true;

            int bytesRead = 0;
            byte[] data = new byte[BufferSize];

            try{
                line.open(af);
            }catch (Exception e){
                e.printStackTrace();
            }

            // Main cycle

            while(true){
 
                line.start();

                do{

                    ais.mark(streamLength);
                    bytesRead = 0;
                    while (bytesRead != -1){
                        try{
                            bytesRead = ais.read(data, 0, data.length);
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        if (bytesRead >= 0){
                            line.write(data, 0, bytesRead);
                        }
                    }

                    try{
                        ais.reset();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }while(count < 0 || --count > 0);

                synchronized(clip){
                    try{
                        clip.wait();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
        }

        public void play(){
            if(!started) clip.start();
            synchronized(this){
                count = 1;
                synchronized(clip){
                    clip.notify();
                }
            }
        }

        public void loop(){
            if(!started) clip.start();
            synchronized(this){
                count = UNLIMITED;
                synchronized(clip){
                    clip.notify();
                }
            }
        }

        public void stop(){
            synchronized(this){
                line.stop();
                count = 1;
            }
        }

        protected void finalize(){
            if(line != null && line.isOpen()){
                line.drain();
                line.close();
            }
        }

    }
}
