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

package org.apache.harmony.awt;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Panel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;

import junit.framework.TestCase;

public class HeadlessTest extends TestCase {    

    public void testJButton(){
        JButton jb = new JButton();
        jb.addNotify();
    }

    public void testJPanel(){
        JPanel jp = new JPanel();
        jp.addNotify();
    }
    
    public void testPanel(){
        Panel p = new Panel();
        p.addNotify();
    }

    public void testCanvas(){
        Canvas cnv = new Canvas();
        cnv.addNotify();
    }

    public void testBufferedImage(){
        BufferedImage bi = new BufferedImage(800,600,BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2 = (Graphics2D)(bi.getGraphics()); 
        g2.setColor(Color.RED);
        g2.fillRect(200,100,600,500);
        g2.setColor(Color.GREEN);
        g2.drawOval(500,300,100,100);
        Image i = null;
        try{
            File f = new File("test.jpg");
            f.createNewFile();
            ImageIO.write(bi, "jpg", f);
            i = ImageIO.read(f);
            f.delete();
        }catch(IOException e){
            throw new AssertionError(e); 
        }
    }
}


