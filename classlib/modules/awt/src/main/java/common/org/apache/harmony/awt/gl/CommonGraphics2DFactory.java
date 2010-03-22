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
 * @author Alexey A. Petrenko, Ilya S. Okomin
 */
package org.apache.harmony.awt.gl;

import java.awt.Font;
import java.awt.peer.FontPeer;
import java.io.IOException;

import org.apache.harmony.awt.wtk.GraphicsFactory;

/**
 * Common GraphicsFactory implementation
 *
 */
public abstract class CommonGraphics2DFactory implements GraphicsFactory {
    
    // static instance of CommonGraphics2DFactory
    public static CommonGraphics2DFactory inst;

    // Font methods
    public FontPeer getFontPeer(Font font) {
        return getFontManager().getFontPeer(font.getName(), font.getStyle(), font.getSize());
    }
    
    /**
     * Embeds font from gile with specified path into the system. 
     * 
     * @param fontFilePath path to the font file 
     * @return Font object that was created from the file.
     */
    public abstract Font embedFont(String fontFilePath) throws IOException;

}