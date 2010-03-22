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

package org.apache.harmony.sound.tests.javax.sound.sampled;

import java.io.File;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

import junit.framework.TestCase;

/**
 * 
 * Dummy sound provider located in soundProvider.jar is used for testing.
 * Provider sources are provided at the comments at the end of this file.
 * 
 */
public class AudioSystemTest extends TestCase {

    public void testAudioFile() throws Exception {
        boolean ok;

        assertTrue(AudioSystem.getAudioFileFormat(new URL("file:./myFile.txt")) != null);

        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();

        ok = false;
        for (int i = 0; i < types.length; i++) {
            if (types[i].getExtension().equals("txt")) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);
    }

    public void testMixer() throws Exception {
        boolean ok;

        Mixer.Info[] minfos = AudioSystem.getMixerInfo();
        assertTrue(minfos.length > 0);
        assertEquals("NAME", minfos[0].getName());
        assertEquals("VERSION", minfos[0].getVersion());

        assertTrue(AudioSystem.getMixer(null) != null);

        Mixer mix = AudioSystem.getMixer(minfos[0]);
        assertEquals("org.apache.harmony.sound.testProvider.MyMixer",
                mix.getClass().getName());
        Line.Info[] mli = mix.getSourceLineInfo();
        assertEquals(4, mli.length);

        Line.Info[] infos = AudioSystem.getSourceLineInfo(mli[0]);
        ok = false;
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getLineClass().getName().equals(
                    "org.apache.harmony.sound.testProvider.myClip")) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);

        infos = AudioSystem.getTargetLineInfo(mli[0]);
        ok = false;
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getLineClass().getName().equals(
                    "org.apache.harmony.sound.testProvider.myClip")) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);
    }

    public void testAudioInputStream() throws Exception {

        AudioInputStream stream = AudioSystem.getAudioInputStream(new File(
                "myFile.txt"));
        assertTrue(stream != null);

        // no exception expected
        AudioSystem.write(stream, new AudioFileFormat.Type("TXT", "txt"),
                System.out);

        assertEquals(AudioSystem.getAudioInputStream(
                AudioFormat.Encoding.PCM_UNSIGNED, stream), stream);
    }

    // see TestFormatConversionProvider
    public void testFormatConversion() throws Exception {

        boolean ok;

        AudioFormat af_source = new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED, 1f, 2, 3, 4, 5f, true);

        AudioFormat.Encoding[] aafe = AudioSystem
                .getTargetEncodings(AudioFormat.Encoding.PCM_UNSIGNED);
        ok = false;
        for (int i = 0; i < aafe.length; i++) {
            // contains PCM_SIGNED (see TestFormatConversionProvider)
            if (aafe[i].equals(AudioFormat.Encoding.PCM_SIGNED)) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);

        assertTrue(AudioSystem.isConversionSupported(
                AudioFormat.Encoding.PCM_SIGNED, af_source));

        AudioFormat[] aaf = AudioSystem.getTargetFormats(
                AudioFormat.Encoding.PCM_UNSIGNED, af_source);

        ok = false;
        for (int i = 0; i < aaf.length; i++) {
            if (aaf[i].getSampleRate() == 10f
                    && aaf[i].getSampleSizeInBits() == 2
                    && aaf[i].getChannels() == 30
                    && aaf[i].getFrameSize() == 40
                    && aaf[i].getFrameRate() == 50f) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);
    }

    public void testGetLine() throws Exception {

        assertEquals("org.apache.harmony.sound.testProvider.myClip",
                AudioSystem.getLine(
                new Line.Info(javax.sound.sampled.Clip.class)).getClass()
                .getName());
        assertEquals("org.apache.harmony.sound.testProvider.mySourceDataLine",
                AudioSystem.getLine(
                new Line.Info(javax.sound.sampled.SourceDataLine.class))
                .getClass().getName());
        assertEquals("org.apache.harmony.sound.testProvider.myTargetDataLine",
                AudioSystem.getLine(
                new Line.Info(javax.sound.sampled.TargetDataLine.class))
                .getClass().getName());
        assertEquals("org.apache.harmony.sound.testProvider.myPort",
                AudioSystem.getLine(
                new Line.Info(javax.sound.sampled.Port.class)).getClass()
                .getName());

        assertEquals("org.apache.harmony.sound.testProvider.myClip",
                     AudioSystem.getClip().getClass().getName());

    }
}


// SOUND PROVIDER SOURCES:
//
// META-INF/services/ files:
//    file META-INF/services/javax.sound.sampled.spi:
//org.apache.harmony.sound.testProvider.TestAudioFileWriter
//
//    file META-INF/services/javax.sound.sampled.spi:
//org.apache.harmony.sound.testProvider.TestFormatConversionProvider
//
//    file META-INF/services/javax.sound.sampled.spi:
//org.apache.harmony.sound.testProvider.TestMixerProvider 
//
//    file META-INF/services/javax.sound.sampled.spi.AudioFileReader:
//org.apache.harmony.sound.testProvider.TestAudioFileReader 
//
//Source files:
//
//TestAudioFileReader.java
//
//package org.apache.harmony.sound.testProvider;
//
//import javax.sound.sampled.*;
//import javax.sound.sampled.spi.*;
//import java.util.*;
//import java.io.*;
//import java.net.*;
//
//public class TestAudioFileReader extends AudioFileReader {
//    static AudioFileFormat aff;
//    static AudioFormat af;
//
//    static {
//       AudioFormat.Encoding enc = AudioFormat.Encoding.PCM_UNSIGNED;
//       AudioFileFormat.Type type = new AudioFileFormat.Type("TXT", "txt");
//       af = new AudioFormat(enc , 1f, 2, 3, 4, 5f, true);
//       aff = new AudioFileFormat(type , af, 10);
//    }
//
//    public TestAudioFileReader() {
//        super();
//    };
//
//    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
//        return aff;
//    }
//
//    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
//        return aff;
//    }
//    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException  {
//          return aff;
//    }
//    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
//        InputStream is = new ByteArrayInputStream(new byte[1001]);
//        return new AudioInputStream(is, af, 10);
//    }
//    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
//        InputStream is = new ByteArrayInputStream(new byte[1001]);
//        return new AudioInputStream(is, af, 10);
//    }
//    public AudioInputStream getAudioInputStream(File file)
//            throws UnsupportedAudioFileException,IOException {
//        InputStream is = new ByteArrayInputStream(new byte[1001]);
//        return new AudioInputStream(is, af, 10);
//    }
//}
//
//TestAudioFileWriter.java
//
//package org.apache.harmony.sound.testProvider;
//
//import javax.sound.sampled.*;
//import javax.sound.sampled.spi.*;
//import java.util.*;
//import java.io.*;
//import java.net.*;
//
//public class TestAudioFileWriter extends AudioFileWriter { 
//
//    static AudioFileFormat aff;
//    static AudioFormat af;
//    static AudioFileFormat.Type type;
//
//    static {
//        AudioFormat.Encoding enc = AudioFormat.Encoding.PCM_UNSIGNED;
//        type = new AudioFileFormat.Type("TXT", "txt");
//        vaf = new AudioFormat(enc , 1f, 2, 3, 4, 5f, true);
//        aff = new AudioFileFormat(type , af, 10);
//        }
//
//    public TestAudioFileWriter () {
//        super();
//    };
//
//    public AudioFileFormat.Type[] getAudioFileTypes() {
//        return new AudioFileFormat.Type[] {type};
//    }
//    public AudioFileFormat.Type[] getAudioFileTypes(AudioInputStream stream) {
//        return new AudioFileFormat.Type[] {type};
//    }
//    public boolean isFileTypeSupported(AudioFileFormat.Type fileType) {
//        return type.equals(fileType);
//    }
//    public boolean isFileTypeSupported(AudioFileFormat.Type fileType, AudioInputStream stream) {
//        return type.equals(fileType);
//    }
//    public int write(AudioInputStream stream, AudioFileFormat.Type fileType, OutputStream out) throws IOException {
//        return 10;
//    }
//    public int write(AudioInputStream stream, AudioFileFormat.Type fileType, File out) throws IOException { 
//        return 10;
//    }
//}
//
//TestFormatConversionProvider.java
//
//package org.apache.harmony.sound.testProvider;
//
//import javax.sound.sampled.*;
//import javax.sound.sampled.spi.*;
//import java.util.*;
//import java.io.*;
//import java.net.*;
//
//public class TestFormatConversionProvider extends FormatConversionProvider{ 
//
//    static AudioFormat.Encoding[] enc_source;
//    static AudioFormat.Encoding[] enc_target;
//    static AudioFormat af_source;
//    static AudioFormat af_target;
//
//    static {
//        enc_source = new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_UNSIGNED};
//        af_source = new AudioFormat(enc_source [0] , 1f, 2, 3, 4, 5f, true);
//        enc_target= new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
//        af_target= new AudioFormat(enc_target[0] , 10f, 2, 30, 40, 50f, false);
//    }
//    public TestFormatConversionProvider() {
//        super();
//    };
//    public AudioInputStream getAudioInputStream(
//            AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
//        if (!enc_target[0].equals(targetEncoding) ||
//                !af_source.equals(sourceStream.getFormat())) {
//            throw new IllegalArgumentException("conversion not supported");
//        }
//        return sourceStream;
//    }
//    public AudioInputStream getAudioInputStream(
//            AudioFormat targetFormat, AudioInputStream sourceStream) {
//        if (!af_target.equals(targetFormat) ||
//                !af_source.equals(sourceStream.getFormat())) {
//            throw new IllegalArgumentException("conversion not supported");
//        }
//        return sourceStream;  
//    }
//    public AudioFormat.Encoding[] getTargetEncodings(
//            AudioFormat sourceFormat) {
//        if (af_source.matches(sourceFormat)) {
//        return enc_target;
//        } else {
//            return new AudioFormat.Encoding[0];
//        }
//    }
//    public AudioFormat[] getTargetFormats(
//            AudioFormat.Encoding targetFormat, AudioFormat sourceFormat) {
//        if (af_source.matches(sourceFormat)) {
//        return new AudioFormat[] {af_target};
//        } else {
//            return new AudioFormat[0];
//        }    
//    }
//    public AudioFormat.Encoding[] getSourceEncodings() {
//        return enc_source;
//    }
//    public AudioFormat.Encoding[] getTargetEncodings() {
//        return enc_target;    
//    }
//}
//
//TestMixerProvider.java
//
//package org.apache.harmony.sound.testProvider;
//
//import javax.sound.sampled.*;
//import javax.sound.sampled.spi.*;
//import java.util.*;
//import java.io.*;
//import java.net.*;
//
//public class TestMixerProvider extends MixerProvider { 
//    static Mixer.Info info;
//    static Mixer mixer; 
//    static {
//        info = new MyMixerInfo("NAME", "VENDOR", "DESCRIPTION", "VERSION");
//        mixer = new MyMixer(info);
//    }
//    public TestMixerProvider () {super();}
//    public boolean isMixerSupported(Mixer.Info info) {
//        return this.info.equals(info);
//    }
//    public Mixer.Info[] getMixerInfo() {
//        return new Mixer.Info[] {info};
//    }
//    public Mixer getMixer(Mixer.Info info) {
//        if (this.info.equals(info)) {
//            return mixer;
//        }
//        throw new IllegalArgumentException("TestMixerProvider ");
//    }
//}
//
//class MyMixerInfo extends Mixer.Info {
//    public MyMixerInfo(String name, String vendor, String description,
//            String version) {
//        super(name, vendor, description, version);
//    }
//}
//
//class MyMixer implements Mixer {
//    private Mixer.Info minfo;
//    private Line[] sourceLines = new Line[] {new myClip(),
//                                             new mySourceDataLine(),
//                                             new myTargetDataLine(),
//                                             new myPort()};
//    private Line[] targetLines = new Line[] {new myClip(),
//                                             new mySourceDataLine(),
//                                             new myTargetDataLine(),
//                                             new myPort()};
//    private Line.Info[] lineInfos = new Line.Info[] {
//        new Line.Info(javax.sound.sampled.Clip.class),
//        new Line.Info(javax.sound.sampled.SourceDataLine.class),
//        new Line.Info(javax.sound.sampled.TargetDataLine.class),                                      
//        new Line.Info(javax.sound.sampled.Port.class)
//    };
//    public MyMixer(Mixer.Info info) {
//        minfo = info;
//    }
//    public Line getLine(Line.Info info) throws LineUnavailableException {
//        for (int i = 0; i < lineInfos.length; i++) {
//            if (lineInfos[i].matches(info)) {
//                return sourceLines[i];
//            }
//        }
//        throw new IllegalArgumentException("not supported " + info);
//    }
//    public int getMaxLines(Line.Info info) {
//        return AudioSystem.NOT_SPECIFIED;
//    }
//    public Mixer.Info getMixerInfo() {
//        return minfo;
//    }
//    public Line.Info[] getSourceLineInfo() { 
//        return lineInfos;
//    }
//    public Line.Info[] getSourceLineInfo(Line.Info info) {
//        for (int i = 0; i < lineInfos.length; i++) {
//            if (lineInfos[i].matches(info)) {
//                return new Line.Info[] {sourceLines[i].getLineInfo()};
//            }
//        }
//        throw new IllegalArgumentException("not supported " + info);
//    }
//    public Line[] getSourceLines() {
//        return sourceLines;
//    }
//    public Line.Info[] getTargetLineInfo() { 
//        return lineInfos;
//    }
//    public Line.Info[] getTargetLineInfo(Line.Info info) { 
//        for (int i = 0; i < lineInfos.length; i++) {
//            if (lineInfos[i].matches(info)) {
//                return new Line.Info[] {targetLines[i].getLineInfo()};
//            }
//        }
//        throw new IllegalArgumentException("not supported " + info);
//    }
//    public Line[] getTargetLines() {
//        return targetLines;
//    }
//    public boolean isLineSupported(Line.Info info) {
//        for (int i = 0; i < lineInfos.length; i++) {
//            if (lineInfos[i].matches(info)) {
//              return true;
//            }
//        }
//        return false;
//    }
//    public boolean isSynchronizationSupported(Line[] lines, boolean maintainSync) {
//        return false;
//    }
//    public void synchronize(Line[] lines, boolean maintainSync) {}
//
//    public void unsynchronize(Line[] lines) {}
//
//    // methods of Line interface
//    public void close() {}
//    public Control getControl(Control.Type control) {
//        throw new IllegalArgumentException("not supported "+ control);
//    }
//    public Control[] getControls() {
//        return new Control[0];
//    }
//    public Line.Info getLineInfo() {
//        return new Line.Info(this.getClass());
//    }
//    public boolean isControlSupported(Control.Type control) {
//        return false;
//    }
//    public boolean isOpen() {
//        return false;
//    }
//    public void open() throws LineUnavailableException {}
//    public void removeLineListener(LineListener listener) {}
//    public void addLineListener(LineListener listener) {}
//}
//
//class myClip implements Clip {
//    public int getFrameLength() { return 10;}
//    public long getMicrosecondLength() {return 100;}
//    public void loop(int count) {}
//    public void open(AudioFormat format, byte[] data, int offset, int bufferSize)
//            throws LineUnavailableException {}
//    public void open(AudioInputStream stream) throws LineUnavailableException,
//            IOException {}
//    public void setFramePosition(int frames) {}
//    public void setLoopPoints(int start, int end) {}
//    public void setMicrosecondPosition(long microseconds) {}
//    public int available() {return 1;}
//    public void drain() {}
//    public void flush() {}
//    public int getBufferSize() {return 1;}
//    public AudioFormat getFormat() {return null;}
//    public int getFramePosition() {return 1;}
//    public float getLevel() {return 1f;}
//    public long getLongFramePosition() {return 1;}
//    public long getMicrosecondPosition() {return 10;}
//    public boolean isActive() {return false;}
//    public boolean isRunning(){return false;}
//    public void start() {}
//    public void stop(){}
//    public void close() {}
//    public Control getControl(Control.Type control) {
//        throw new IllegalArgumentException("not supported "+ control);
//    }
//    public Control[] getControls() {
//        return new Control[0];
//    }
//    public Line.Info getLineInfo() {
//        return new Line.Info(this.getClass());
//    }
//    public boolean isControlSupported(Control.Type control) {
//        return false;
//    }
//    public boolean isOpen() {
//        return false;
//    }
//    public void open() throws LineUnavailableException {}
//    public void removeLineListener(LineListener listener) {}
//    public void addLineListener(LineListener listener) {}
//}
//
//class mySourceDataLine implements SourceDataLine{
//    public void open(AudioFormat format) throws LineUnavailableException {}
//    public void open(AudioFormat format, int bufferSize)
//            throws LineUnavailableException {}
//    public int write(byte[] b, int off, int len) {return 1;}
//    public int available() {return 1;}
//    public void drain() {}
//    public void flush() {}
//    public int getBufferSize() {return 1;}
//    public AudioFormat getFormat() {return null;}
//    public int getFramePosition() {return 1;}
//    public float getLevel() {return 1f;}
//    public long getLongFramePosition() {return 1;}
//    public long getMicrosecondPosition() {return 10;}
//    public boolean isActive() {return false;}
//    public boolean isRunning(){return false;}
//    public void start() {}
//    public void stop(){}
//    public void close() {}
//    public Control getControl(Control.Type control) {
//        throw new IllegalArgumentException("not supported "+ control);
//    }
//    public Control[] getControls() {
//        return new Control[0];
//    }
//    public Line.Info getLineInfo() {
//        return new Line.Info(this.getClass());
//    }
//    public boolean isControlSupported(Control.Type control) {
//        return false;
//    }
//    public boolean isOpen() {
//        return false;
//    }
//    public void open() throws LineUnavailableException {}
//    public void removeLineListener(LineListener listener) {}
//    public void addLineListener(LineListener listener) {}
//}
//
//class myTargetDataLine implements TargetDataLine {
//    public void open(AudioFormat format) throws LineUnavailableException{}
//    public void open(AudioFormat format, int bufferSize)
//            throws LineUnavailableException{}
//    public int read(byte[] b, int off, int len) {return 1;}
//    public int available() {return 1;}
//    public void drain() {}
//    public void flush() {}
//    public int getBufferSize() {return 1;}
//    public AudioFormat getFormat() {return null;}
//    public int getFramePosition() {return 1;}
//    public float getLevel() {return 1f;}
//    public long getLongFramePosition() {return 1;}
//    public long getMicrosecondPosition() {return 10;}
//    public boolean isActive() {return false;}
//    public boolean isRunning(){return false;}
//    public void start() {}
//    public void stop(){}
//    public void close() {}
//    public Control getControl(Control.Type control) {
//        throw new IllegalArgumentException("not supported "+ control);
//    }
//    public Control[] getControls() {
//        return new Control[0];
//    }
//    public Line.Info getLineInfo() {
//        return new Line.Info(this.getClass());
//    }
//    public boolean isControlSupported(Control.Type control) {
//        return false;
//    }
//    public boolean isOpen() {
//        return false;
//    }
//    public void open() throws LineUnavailableException {}
//    public void removeLineListener(LineListener listener) {}
//    public void addLineListener(LineListener listener) {}
//}
//
//class myPort implements Port {
//    public void close() {}
//    public Control getControl(Control.Type control) {
//        throw new IllegalArgumentException("not supported "+ control);
//    }
//    public Control[] getControls() {
//        return new Control[0];
//    }
//    public Line.Info getLineInfo() {
//        return new Line.Info(this.getClass());
//    }
//    public boolean isControlSupported(Control.Type control) {
//        return false;
//    }
//    public boolean isOpen() {
//        return false;
//    }
//    public void open() throws LineUnavailableException {}
//    public void removeLineListener(LineListener listener) {}
//    public void addLineListener(LineListener listener) {}
//}
