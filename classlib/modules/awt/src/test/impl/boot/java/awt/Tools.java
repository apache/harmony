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
package java.awt;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.util.Arrays;

//import javax.imageio.ImageIO;

import org.apache.harmony.awt.gl.MultiRectAreaOp;

import junit.framework.Assert;

public abstract class Tools {

    static String typeName[] = {"move", "line", "quad", "cubic", "close"};

    static int findString(String buf[], String value) {
        for(int i = 0; i < buf.length; i++) {
            if (buf[i].equals(value)) {
                return i;
            }
        }
        Assert.fail("Unknown value " + value);
        return -1;
    }
    
    public static String getClasstPath(Class clazz) {
        String name = clazz.getName();
        name = name.substring(0, name.lastIndexOf('.'));

        return name.replace('.', '/') + '/';
    }

    public static void checkDeadLoop(Component c, int[] count) {
        final int DEAD_LOOP_TIMEOUT = 1000;
        final int VALID_NUMBER_OF_PAINT_CALLS = 15;
        
        Frame f = new Frame();
        
        f.add(c);
        f.setSize(300,200);
        f.setVisible(true);

        try {
            Thread.sleep(DEAD_LOOP_TIMEOUT);
        } catch (Exception e) {}
        
        f.dispose();
        
        Assert.assertTrue("paint() called " + count[0] +
                " times, a dead loop occurred",
                count[0] <= VALID_NUMBER_OF_PAINT_CALLS);        
    }

    public static class Shape {

        static int pointCount[] = {
                2, // MOVE
                2, // LINE
                4, // QUAD
                6, // CUBIC
                0  // CLOSE
            };

        static final double IMAGE_REL_BORDER = 0.1;
        static final double IMAGE_MIN_BORDER = 10.0;
        static final double IMAGE_MAX_BORDER = 100.0;
        static final Color backColor = Color.white;

        public static Frame show(final java.awt.Shape shape) {
            Frame f = new Frame("Shape") {
                
                public void paint(Graphics g) {
                    // Background
                    g.setColor(Color.white);
                    g.fillRect(0, 0, getWidth(), getHeight());

                    // Fill shape
                    g.setColor(Color.lightGray);
                    ((Graphics2D)g).fill(shape);

                    // Draw shape
                    g.setColor(Color.black);
                    ((Graphics2D)g).draw(shape);

//                    java.awt.image.BufferedImage img = Shape.createImage(shape, null, Color.gray, Color.lightGray);
//                    g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
                }
                
            };

            f.addWindowListener(
                    new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                            System.exit(0);
                        }
                    }
                );          

            f.setSize(600, 400);
            f.show();
            return f;            
        }
        
        public static void save(java.awt.Shape s, String fileName) {
            try {
                FileWriter f = new FileWriter(fileName);
                java.awt.geom.PathIterator p = s.getPathIterator(null);
                double coords[] = new double[6];
                while(!p.isDone()) {
                    int type = p.currentSegment(coords);
                    f.write(typeName[type] + getCoords(coords, pointCount[type]) + "\n");
                    p.next();
                }
                f.close();
            } catch (IOException e) {
                Assert.fail("Can''t write to file " + fileName);
            }
        }

        public static java.awt.Shape load(String fileName) {
            GeneralPath s = null;
            try {
                FileReader f = new FileReader(fileName);
                s = new GeneralPath();
                StreamTokenizer t = new StreamTokenizer(f);
                int count = 0;
                int type = 0;
                float coords[] = new float[6];
                while(t.nextToken() != StreamTokenizer.TT_EOF) {
                    switch(t.ttype) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_WORD:
                        type = findString(typeName, t.sval);
                        if (type == java.awt.geom.PathIterator.SEG_CLOSE) {
                            s.closePath();
                        }
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        coords[count++] = (float)t.nval;
                        if (count == pointCount[type]) {
                            count = 0;
                            switch(type) {
                            case java.awt.geom.PathIterator.SEG_MOVETO:
                                s.moveTo(coords[0], coords[1]);
                                break;
                            case java.awt.geom.PathIterator.SEG_LINETO:
                                s.lineTo(coords[0], coords[1]);
                                break;
                            case java.awt.geom.PathIterator.SEG_QUADTO:
                                s.quadTo(coords[0], coords[1], coords[2], coords[3]);
                                break;
                            case java.awt.geom.PathIterator.SEG_CUBICTO:
                                s.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                                break;
                            }
                        }
                        break;
                    }
                }
                f.close();
            } catch (IOException e) {
                Assert.fail("Can''t read file " + fileName);
            }
            return s;
        }

        static String getCoords(double coords[], int count) {
            String s = "";
            for(int i = 0; i < count; i++) {
                s = s + " " + coords[i];
            }
            return s;
        }

        public static java.awt.image.BufferedImage createImage(java.awt.Shape shape, AffineTransform t, Color draw, Color fill) {

            // Calculate image border
            Rectangle r = shape.getBounds();
            double border = r.getWidth() * IMAGE_REL_BORDER;
            border = Math.min(IMAGE_MAX_BORDER, border);
            border = Math.max(IMAGE_MIN_BORDER, border);

            // Shift shape in the center of the image
            if (t == null) {
                t = AffineTransform.getTranslateInstance(- r.getX() + border, - r.getY() + border);
            } else {
                t.setToTranslation(- r.getX() + border, - r.getY() + border);
            }
            java.awt.geom.GeneralPath dst = new java.awt.geom.GeneralPath();
            dst.append(shape.getPathIterator(t), false);

            java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(
                    (int)(r.getWidth() + border * 2.0),
                    (int)(r.getHeight() + border * 2.0),
                    java.awt.image.BufferedImage.TYPE_INT_RGB);

            Graphics g = img.getGraphics();

            // Background
            g.setColor(backColor);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());

            // Fill shape
            g.setColor(fill);
            ((Graphics2D)g).fill(dst);

            // Draw shape
            g.setColor(draw);
            ((Graphics2D)g).draw(dst);

            return img;
        }

        public static boolean equals(java.awt.Shape s1, java.awt.Shape s2, double delta) {
            return PathIterator.equals(s1.getPathIterator(null), s2.getPathIterator(null), delta);
        }

        public static boolean equals(java.awt.Shape s1, java.awt.Shape s2, float delta) {
            return PathIterator.equals(s1.getPathIterator(null), s2.getPathIterator(null), delta);
        }

        public static java.awt.Shape scale(java.awt.Shape shape, double k) {
            java.awt.geom.PathIterator path = shape.getPathIterator(AffineTransform.getScaleInstance(k, k));
            java.awt.geom.GeneralPath dst = new java.awt.geom.GeneralPath(path.getWindingRule());
            dst.append(path, false);
            return dst;
        }

        public static java.awt.Shape flip(java.awt.Shape shape) {
            java.awt.geom.PathIterator path = shape.getPathIterator(new AffineTransform(0, 1, 1, 0, 0, 0));
            java.awt.geom.GeneralPath dst = new java.awt.geom.GeneralPath(path.getWindingRule());
            dst.append(path, false);
            return dst;
        }

        public static String toString(java.awt.Shape shape) {
            return shape.getClass().getName() + "\n" + PathIterator.toString(shape.getPathIterator(null));
        }

        public static void drawColored(java.awt.Shape shape, Graphics2D g) {
            java.awt.geom.PathIterator path = shape.getPathIterator(null);
            float cx = 0;
            float cy = 0;
            float coords[] = new float[6];
            while(!path.isDone()) {
                switch(path.currentSegment(coords)) {
                case java.awt.geom.PathIterator.SEG_MOVETO:
                    cx = coords[0];
                    cy = coords[1];
                    break;
                case java.awt.geom.PathIterator.SEG_LINETO:
                    g.setColor(Color.blue);
                    g.draw(new Line2D.Float(cx, cy, cx = coords[0], cy = coords[1]));
                    break;
                case java.awt.geom.PathIterator.SEG_QUADTO:
                    g.setColor(Color.green);
                    g.draw(new QuadCurve2D.Float(cx, cy, coords[0], coords[1], cx = coords[2], cy = coords[3]));
                    break;
                case java.awt.geom.PathIterator.SEG_CUBICTO:
                    g.setColor(Color.red);
                    g.draw(new CubicCurve2D.Float(cx, cy, coords[0], coords[1], coords[2], coords[3], cx = coords[4], cy = coords[5]));
                    break;
                case java.awt.geom.PathIterator.SEG_CLOSE:
                    break;
                }
                path.next();
            }
        }

    }

    public static class BasicStroke {

        static String propName[] = new String[] {"width", "cap", "join", "miter", "dash", "phase"};
        static String capName[] = new String[] {"BUTT", "ROUND", "SQUARE"};
        static String joinName[] = new String[] {"MITER", "ROUND", "BEVEL"};

        public static void save(java.awt.BasicStroke bs, String fileName) {
            try {
                FileWriter f = new FileWriter(fileName);
                save(bs, f);
                f.close();
            } catch (IOException e) {
                Assert.fail("Can''t write to file " + fileName);
            }
        }

        public static void save(java.awt.BasicStroke bs, Writer f) throws IOException {
            f.write("width " + bs.getLineWidth() + "\n");
            f.write("cap " + capName[bs.getEndCap()] + "\n");
            f.write("join "+ joinName[bs.getLineJoin()] + "\n");
            f.write("miter " + bs.getMiterLimit() + "\n");
            float dash[] = bs.getDashArray();
            if (dash != null) {
                String str = "";
                for (float element : dash) {
                    str = str + element + " ";
                }
                f.write("dash " + str + "\n");
                f.write("phase " + bs.getDashPhase());
            }
        }

        public static java.awt.BasicStroke load(String fileName) {
            java.awt.BasicStroke bs = null;
            try {
                FileReader f = new FileReader(fileName);
                bs = load(f);
                f.close();
            } catch (IOException e) {
                Assert.fail("Can''t read file " + fileName);
            }
            return bs;
        }

        public static java.awt.BasicStroke load(Reader f) throws IOException {
            // Set default values
            java.awt.BasicStroke bs = new java.awt.BasicStroke();
            float width = bs.getLineWidth();
            int cap = bs.getEndCap();
            int join = bs.getLineJoin();
            float miterLimit = bs.getMiterLimit();
            float dash[] = bs.getDashArray();
            float dashPhase = bs.getDashPhase();

            int prop = -1;
            int dashCount = 0;
            StreamTokenizer t = new StreamTokenizer(f);

            while(t.nextToken() != StreamTokenizer.TT_EOF) {
                switch(t.ttype) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_WORD:
                    switch(prop) {
                    case 4: // dash
                        float tmp[] = new float[dashCount];
                        System.arraycopy(dash, 0, tmp, 0, dashCount);
                        dash = tmp;
                    case -1:
                        prop = findString(propName, t.sval);
                        break;
                    case 1: // cap
                        cap = findString(capName, t.sval);
                        prop = -1;
                        break;
                    case 2: // join
                        join = findString(joinName, t.sval);
                        prop = -1;
                        break;
                    }
                    break;
                case StreamTokenizer.TT_NUMBER:
                    switch(prop) {
                    case 0: // width
                        width = (float)t.nval;
                        prop = -1;
                        break;
                    case 3: // miterLimit
                        miterLimit = (float)t.nval;
                        prop = -1;
                        break;
                    case 4: // dash
                        if (dash == null) {
                            dash = new float[10];
                            dashCount = 0;
                        }
                        dash[dashCount++] = (float)t.nval;
                        break;
                    case 5: // dashPhase
                        dashPhase = (float)t.nval;
                        prop = -1;
                        break;
                    }
                    break;
                }
            }
            return new java.awt.BasicStroke(width, cap, join,miterLimit, dash, dashPhase);
        }

    }

    public static class BufferedImage {

        public static java.awt.image.BufferedImage load(String filename) {
            java.awt.image.BufferedImage img = null;
            try {
//                Code should be enabled when imageio is supported
//                img = ImageIO.read(new java.io.File(filename));
                return null;
            } catch (Exception e) {
                Assert.fail("Can't open file: " + filename);
            }
            return img;
        }

        public static void save(java.awt.image.BufferedImage img, String filename) {
            try {
//              Code should be enabled when imageio is supported
//                ImageIO.write(img, "jpg", new java.io.File(filename));
            } catch (Exception e) {
                Assert.fail("Can't save file: " + filename);
            }
        }

        public static java.awt.image.BufferedImage loadIcon(String filename) {
            try {
//                Code should be enabled when imageio is supported
//                return ImageIO.read(new java.io.File(filename));
            } catch(Exception e) {
                Assert.fail("Can't open file: " + filename);
            }
            return null;
        }

        public static void saveIcon(java.awt.image.BufferedImage img, String filename) {
            try {
//                Code should be enabled when imageio is supported
//                ImageIO.write(img, "png", new java.io.File(filename));
            } catch(Exception e) {
                Assert.fail("Can't save file: " + filename);
            }
        }

    }

    public static class File {

        public static String changeExt(String file, String newExt) {
            int k = file.lastIndexOf('.');
            return file.substring(0, k) + newExt;
        }

        public static String extractFileName(String file) {
            int k;
            if ((k = file.lastIndexOf('/')) == -1) {
                if ((k = file.lastIndexOf('\\')) == -1) {
                    k = 1;
                }
            }
            return file.substring(k + 1);
        }

        public static String extractFileExt(String file) {
            int k = file.lastIndexOf('.');
            return file.substring(k + 1);
        }

    }

    public static class PathIterator {

        public static String equalsError = "";
        
        static boolean coordsEquals(double coords1[], double coords2[], int count, double delta) {
            for(int i = 0; i < count; i++) {
                if (Math.abs(coords1[i] - coords2[i]) > delta) {
                    return false;
                }
            }
            return true;
        }

        static boolean coordsEquals(float coords1[], float coords2[], int count, float delta) {
            for(int i = 0; i < count; i++) {
                if (Math.abs(coords1[i] - coords2[i]) > delta) {
                    return false;
                }
            }
            return true;
        }

        public static boolean equals(java.awt.geom.PathIterator p1, java.awt.geom.PathIterator p2, double delta) {
            equalsError = "";
            if (p1.getWindingRule() != p2.getWindingRule()) {
                equalsError = "WindingRule expected " + p1.getWindingRule() + " but was " + p2.getWindingRule();
                return false;
            }
            int count = 0;
            double coords1[] = new double[6];
            double coords2[] = new double[6];
            while(!p1.isDone() && !p2.isDone()) {
                int type1 = p1.currentSegment(coords1);
                int type2 = p2.currentSegment(coords2);
                if (type1 != type2 || !coordsEquals(coords1, coords2, Shape.pointCount[type1], delta)) {
                    equalsError = "Expected #" + count + " segment "+ typeName[type1] + Arrays.toString(coords1) + " but was " + typeName[type2] + Arrays.toString(coords2);
                    return false;
                }
                p1.next();
                p2.next();
                count++;
            }
            if (p1.isDone() != p2.isDone()) {
                equalsError = "Expected #" + count + " isDone " + p1.isDone() + " but was " + p2.isDone();
                return false;
            }
            return true;
        }

        public static boolean equals(java.awt.geom.PathIterator p1, java.awt.geom.PathIterator p2, float delta) {
            if (p1.getWindingRule() != p2.getWindingRule()) {
                return false;
            }
            float coords1[] = new float[6];
            float coords2[] = new float[6];
            while(!p1.isDone() && !p2.isDone()) {
                int type1 = p1.currentSegment(coords1);
                int type2 = p2.currentSegment(coords2);
                if (type1 != type2 || !coordsEquals(coords1, coords2, Shape.pointCount[type1], delta)) {
                    return false;
                }
                p1.next();
                p2.next();
            }
            if (p1.isDone() != p2.isDone()) {
                return false;
            }
            return true;
        }

        public static String toString(java.awt.geom.PathIterator path) {
            String out = "";
            float coords[] = new float[6];
            while(!path.isDone()) {
                switch(path.currentSegment(coords)) {
                case java.awt.geom.PathIterator.SEG_MOVETO:
                    out += "move(" + coords[0] + "," + coords[1] + ")\n";
                    break;
                case java.awt.geom.PathIterator.SEG_LINETO:
                    out += "line(" + coords[0] + "," + coords[1] + ")\n";
                    break;
                case java.awt.geom.PathIterator.SEG_QUADTO:
                    out += "quad(" + coords[0] + "," + coords[1] + "," + coords[2] + "," + coords[3] + ")\n";
                    break;
                case java.awt.geom.PathIterator.SEG_CUBICTO:
                    out += "cubic(" + coords[0] + "," + coords[1] + "," + coords[2] + "," + coords[3] + "," + coords[4] + "," + coords[5] + ")\n";
                    break;
                case java.awt.geom.PathIterator.SEG_CLOSE:
                    out += "close\n";
                    break;
                }
                path.next();
            }
            out += "done\n";
            return out;
        }
    }

    public static class MultiRectArea {

        public final static Color[] color = new Color[] {
                Color.blue,
                Color.green,
                Color.red,
                Color.yellow,
                Color.MAGENTA,
                Color.orange,
                Color.lightGray,
                Color.cyan,
                Color.pink
        };

        public final static Color[] colorBlue = new Color[] {
                new Color(0x3F),
                new Color(0x5F),
                new Color(0x7F),
                new Color(0x9F),
                new Color(0xBF),
                new Color(0xDF),
                new Color(0xFF)
        };

        public final static Color[] colorGreen = new Color[] {
                new Color(0x3F00),
                new Color(0x5F00),
                new Color(0x7F00),
                new Color(0x9F00),
                new Color(0xBF00),
                new Color(0xDF00),
                new Color(0xFF00)
        };

        static final int BORDER = 30;
        static final Color colorBack = Color.white;

        public static void save(org.apache.harmony.awt.gl.MultiRectArea area, String fileName) {
            try {
                FileWriter f = new FileWriter(fileName);
                Rectangle[] rect = area.getRectangles();
                for (Rectangle element : rect) {
                    f.write(
                            element.x + "," +
                            element.y + "," +
                            (element.width + element.x - 1) + "," +
                            (element.height + element.y - 1) + "\n");
                }
                f.close();
            } catch (IOException e) {
                Assert.fail("Can''t write to file " + fileName);
            }
        }

        public static org.apache.harmony.awt.gl.MultiRectArea load(String fileName) {
            org.apache.harmony.awt.gl.MultiRectArea area = null;
            try {
                int[] buf = MultiRectAreaOp.createBuf(0);
                int count = 1;

                FileReader f = new FileReader(fileName);
                StreamTokenizer t = new StreamTokenizer(f);
                while(t.nextToken() != StreamTokenizer.TT_EOF) {
                    if (t.ttype == StreamTokenizer.TT_NUMBER) {
                        buf = MultiRectAreaOp.checkBufSize(buf, 1);
                        buf[count++] = (int)t.nval;
                    }
                }
                f.close();

                int j = 0;
                Rectangle[] rect = new Rectangle[(count - 1) / 4];
                for(int i = 1; i < count; i += 4) {
                    rect[j++] = new Rectangle(
                            buf[i],
                            buf[i + 1],
                            buf[i + 2] - buf[i] + 1,
                            buf[i + 3] - buf[i + 1] + 1);
                }
                area = new org.apache.harmony.awt.gl.MultiRectArea(rect);
            } catch (IOException e) {
                Assert.fail("Can''t read file " + fileName);
            }
            return area;
        }

        public static java.awt.image.BufferedImage createImage(org.apache.harmony.awt.gl.MultiRectArea area) {
            return createImage(area, color);
        }

        public static java.awt.image.BufferedImage createImage(org.apache.harmony.awt.gl.MultiRectArea area, Color[] palette) {

            // Calculate image border
            Rectangle bounds = area.getBounds();
            int width = bounds.x + bounds.width + BORDER;
            int height = bounds.y + bounds.height + BORDER;

            java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(
                        width, height,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);

            Graphics g = img.getGraphics();

            // Background
            g.setColor(colorBack);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());

            Rectangle[] rect = area.getRectangles();
            for(int i = 0; i < rect.length; i++) {
                g.setColor(palette[i % palette.length]);
                g.fillRect(rect[i].x, rect[i].y, rect[i].width, rect[i].height);
            }
            return img;
        }

    }

}
