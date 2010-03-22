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
package java.awt.geom;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import java.awt.Tools;

public abstract class ShapeTestCase extends PathIteratorTestCase {

    final static boolean OUTPUT = System.getProperty("OUTPUT") != null; 
    
    final static double SHAPE_DELTA = 0.01;
    final static int RECT_WIDTH = 46;
    final static int RECT_HEIGHT = 34;
    final static int POINT_MIN_COUNT = 10;
    final static int ERROR_MAX_COUNT = 5;
    final static int CHECK_STEP = 8;

    final static Color colorInside = Color.lightGray;
    final static Color colorOutside = Color.white;
    final static Color colorShape = Color.gray;
    final static Color colorGrid = new Color(0xA0, 0xA0, 0xA0);

    final static int cInside = colorInside.getRGB();
    final static int cOutside = colorOutside.getRGB();
    final static int cShape = colorShape.getRGB();
    final static int cGrid = colorGrid.getRGB();

    final static Color errorColor = Color.red;
    final static Color successColor = Color.green;

    final static HashMap<String, Integer> arcTypes = new HashMap<String, Integer>();
    static {        
        arcTypes.put("PIE", Arc2D.PIE);
        arcTypes.put("CHORD", Arc2D.CHORD);
        arcTypes.put("OPEN", Arc2D.OPEN);
    }
    
    static String shapePath = null;
    static String outputPath = null;
    
    protected FilenameFilter filterImage, filterShape;

    abstract static class Runner {

        class TextTokenizer {

            StreamTokenizer t;
            String buf;

            TextTokenizer(String text) {
                t = new StreamTokenizer(new StringReader(text));
                buf = text;
            }

            double getDouble() throws IOException {
                while (t.nextToken() != StreamTokenizer.TT_EOF) {
                    if (t.ttype == StreamTokenizer.TT_NUMBER) {
                        return t.nval;
                    }
                }
                throw new IOException("Double not found");
            }

            String getString() throws IOException {
                while (t.nextToken() != StreamTokenizer.TT_EOF) {
                    if (t.ttype == StreamTokenizer.TT_WORD) {
                        return t.sval;
                    }
                }
                throw new IOException("String not found");
            }

            boolean findString(String substr) {
                int pos = buf.indexOf(substr);
                if (pos != -1) {
                    t = new StreamTokenizer(new StringReader(buf.substring(pos + substr.length())));
                }
                return pos != -1;
            }

        }

        abstract static class Rectangle extends Runner {
            
            static String outputPrefix = "undefined";
            
            static class Contains extends Rectangle {
                
                static {
                    outputPrefix = "cr_";
                }
                
                static boolean[] result = new boolean[] {false, false, true, false};
                
                boolean execute(Shape shape, int x, int y, int width, int height, int expected) {
                    return result[expected] == shape.contains(x, y, width, height);
                }
                
            }
            
            static class Intersects extends Rectangle {
                
                static {
                    outputPrefix = "ir_";
                }
                
                static boolean[] result = new boolean[] {false, false, true, true};
                
                boolean execute(Shape shape, int x, int y, int width, int height, int expected) {
                    return result[expected] == shape.intersects(x, y, width, height);
                }
                
            }
            
            int[] prevRect;
            int[] count;
            
            abstract boolean execute(Shape shape, int x, int y, int width, int height, int expected);
            
            public boolean run(String fileName) {
                boolean error = false;
                try {
                    Shape shape = createShape(fileName);
                    BufferedImage img = Tools.BufferedImage.loadIcon(fileName);
                    int buf[][] = createImageBuffer(img);
                    Graphics g = img.getGraphics();
                    g.setColor(errorColor);

                    count = new int[]{0, 0, 0};
                    prevRect = null;

                    for(int x = 0; x < img.getWidth() - RECT_WIDTH; x++)
                        for(int y = 0; y < img.getHeight() - RECT_HEIGHT; y++) {
                            int rectType = getRectType(null, buf, x, y, RECT_WIDTH, RECT_HEIGHT, true);
                            if (rectType == 0) {
                                // Invalid rectangle
                                continue;
                            }
                            if (!execute(shape, x, y, RECT_WIDTH, RECT_HEIGHT, rectType)) {
                                g.drawRect(x, y, RECT_WIDTH, RECT_HEIGHT);
                                error = true;
                            }
                        }

                    int errCount = 0;
                    Random rnd = new Random();
                    for(int i = 0; i < 1000; i ++) {
                        int rx = (int)(rnd.nextDouble() * (img.getWidth() - RECT_WIDTH));
                        int ry = (int)(rnd.nextDouble() * (img.getHeight() - RECT_HEIGHT));
                        int rw = (int)(rnd.nextDouble() * (img.getWidth() - rx - 1)) + 1;
                        int rh = (int)(rnd.nextDouble() * (img.getHeight() - ry - 1)) + 1;

                        int rectType = getRectType(img, buf, rx, ry, rw, rh, false);
                        if (rectType == 0) {
                            // Invalid rectangle
                            continue;
                        }
                        if (!execute(shape, rx, ry, rw, rh, rectType)) {
                            g.drawRect(rx, ry, rw, rh);
                            error = true;
                            errCount++;
                        }
                        if (errCount > ERROR_MAX_COUNT) {
                            break;
                        }
                    }

                    if (OUTPUT) {
                        Tools.BufferedImage.saveIcon(img, outputPath + outputPrefix + Tools.File.extractFileName(fileName));
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    fail(e.toString());
                }
                return !error;
            }
            
            int getRectType(BufferedImage img, int buf[][], int rx, int ry, int rw, int rh, boolean usePrev) {

                if ((rx + ry) % 2 == 0) {
                    return 0;
                }

                if (!usePrev) {
                    prevRect = null;
                    count = new int[3];
                }

                int[] newRect = new int[]{rx, ry, rx + rw, ry + rh};
                countRect(buf, prevRect, newRect, count);
                prevRect = newRect;

                if (count[0] > POINT_MIN_COUNT && count[1] == 0 && count[2] == 0) {
                    return 1; // Outside
                }
                if (count[1] > POINT_MIN_COUNT && count[0] == 0 && count[2] == 0) {
                    return 2; // Inside
                }
                if (count[0] > POINT_MIN_COUNT && count[1] > POINT_MIN_COUNT) {
                    return 3; // Both
                }
                return 0; // Invalid rectangle
            }
            
            void countRect(int[][] buf, int[] r1, int[] r2, int[] count) {
                if (r1 != null && (r1[0] > r2[2] || r1[2] < r2[0] || r1[1] > r2[3] || r1[3] < r2[1])) {
                    count[0] = count[1] = count[2] = 0;
                    countRect(buf, null, r2, count);
                    return;
                }

                int x1, y1, x2, y2;
                if (r1 == null) {
                    x1 = r2[0];
                    y1 = r2[1];
                    x2 = r2[2];
                    y2 = r2[3];
                } else {
                    x1 = Math.min(r1[0], r2[0]);
                    y1 = Math.min(r1[1], r2[1]);
                    x2 = Math.max(r1[2], r2[2]);
                    y2 = Math.max(r1[3], r2[3]);
                }
                for(int x = x1; x <= x2; x++)
                    for(int y = y1; y <= y2; y++) {
                        boolean inside1 = r1 != null && r1[0] <= x && x <= r1[2] && r1[1] <= y && y <= r1[3];
                        boolean inside2 = r2 != null && r2[0] <= x && x <= r2[2] && r2[1] <= y && y <= r2[3];

                        if (inside1 ^ inside2) {
                            int index = 3;
                            int color = getColor(buf[x][y]);
                            if (color == colorOutside.getRGB()) {
                                index = 0;
                            } else
                            if (color == colorInside.getRGB()) {
                                index = 1;
                            } else
                            if (color == colorShape.getRGB()) {
                                index = 2;
                            }
                            if (inside1) {
                                count[index]--;
                            } else {
                                count[index]++;
                            }
                        }
                    }
            }
            
        }
        
        static class Point extends Runner {

            public boolean run(String fileName) {
                boolean error = false;
                Shape shape = createShape(fileName);
                BufferedImage img = Tools.BufferedImage.loadIcon(fileName);
                for(int x = 0; x < img.getWidth(); x++)
                    for(int y = 0; y < img.getHeight(); y++) {
                        int color = getColor(img.getRGB(x, y));
                        boolean res = shape.contains(x, y);
                        if ((color == colorInside.getRGB() && !res) ||
                            (color == colorOutside.getRGB() && res))
                        {
                            img.setRGB(x, y, errorColor.getRGB());
                            error = true;
                        }
                    }
                if (OUTPUT) {
                    Tools.BufferedImage.saveIcon(img, outputPath + "cp_" + Tools.File.extractFileName(fileName));
                }
                return !error;
            }
            
        }
        
        static class PathIterator extends Runner {

            public boolean run(String fileName) {
                double flatness = getFlatness(fileName);
                AffineTransform at = createTransform(fileName);
                Shape shape1 = createShape(fileName);
                Shape shape2 = Tools.Shape.load(fileName);
                GeneralPath path = new GeneralPath();
                path.append(flatness < 0.0 ? shape1.getPathIterator(at) : shape1.getPathIterator(at, flatness), false);
                if (OUTPUT) {
                    Tools.Shape.save(path, outputPath + (at == null ? "pi_" : "pia_") + Tools.File.extractFileName(fileName));
                }
                return Tools.PathIterator.equals(
                        path.getPathIterator(null),
                        shape2.getPathIterator(null),
                        SHAPE_DELTA);
            }
            
            double getFlatness(String fileName) {
                try {
                    TextTokenizer t = new TextTokenizer(Tools.File.extractFileName(fileName));
                    if (t.findString("flat(")) {
                        return t.getDouble();
                    }
                } catch(IOException e) {
                    fail("Can't read flatness " + fileName);
                }
                return -1.0;
            }
            
            AffineTransform createTransform(String fileName) {
                AffineTransform at = null;
                try {
                    String fname = Tools.File.extractFileName(fileName);
                    TextTokenizer t = new TextTokenizer(fname);


                    if (t.findString("affine(")) {
        /*
                        String ttype = t.getString();
                        if (ttype.equals("M")) {
                            at.setTransform(AffineTransform.getTranslateInstance(t.getDouble(), t.getDouble()));
                        } else
                        if (ttype.equals("R")) {
                            at.setTransform(AffineTransform.getRotateInstance(t.getDouble()));
                        } else
                        if (ttype.equals("SC")) {
                            at.setTransform(AffineTransform.getScaleInstance(t.getDouble(), t.getDouble()));
                        } else
                        if (ttype.equals("SH")) {
                            at.setTransform(AffineTransform.getShearInstance(t.getDouble(), t.getDouble()));
                        } else
                        if (ttype.equals("F")) {
                            at.setTransform(AffineTransform.getShearInstance(t.getDouble(), t.getDouble()));
                        }
        */
                        at = new AffineTransform(
                                t.getDouble(),
                                t.getDouble(),
                                t.getDouble(),
                                t.getDouble(),
                                t.getDouble(),
                                t.getDouble());
                    }

                } catch (IOException e) {
                    fail("Can't read transform " + fileName);
                }
                return at;
            }

        }
        
        abstract boolean run(String fileName);
        
        Shape createShape(String fileName) {
            Shape shape = null;
            try {
                String fname = Tools.File.extractFileName(fileName);
                TextTokenizer t = new TextTokenizer(fname);
                if (t.findString("rect(")) {
                    shape = new Rectangle2D.Double(
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble());
                } else
                if (t.findString("ellipse(")) {
                    shape = new Ellipse2D.Double(
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble());
                } else
                if (t.findString("round(")) {
                    shape = new RoundRectangle2D.Double(
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble());
                } else
                if (t.findString("arc(")) {
                    shape = new Arc2D.Double(
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            arcTypes.get(t.getString()));
                } else
                if (t.findString("line(")) {
                    shape = new Line2D.Double(
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble());
                } else
                if (t.findString("quad(")) {
                    shape = new QuadCurve2D.Double(
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble());
                } else
                if (t.findString("cubic(")) {
                    shape = new CubicCurve2D.Double(
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble(),
                            t.getDouble());
                } else
                if (t.findString("polygon(")) {
                    shape = new Polygon();
                    try {
                        while(true) {
                            ((Polygon)shape).addPoint((int)t.getDouble(), (int)t.getDouble());
                        }
                    } catch(IOException e) {
                    }
                } else {
                    // GeneralPath
                    shape = Tools.Shape.load(Tools.File.changeExt(fileName, ".shape"));
                }

            } catch (IOException e) {
                fail("Can't read shape " + fileName);
            }
            return shape;
        }

        int[][] createImageBuffer(BufferedImage img) {
            int buf[][] = new int[img.getWidth()][img.getHeight()];
            for(int x = 0; x < img.getWidth(); x++)
                for(int y = 0; y < img.getHeight(); y++) {
                    buf[x][y] = img.getRGB(x, y);
                }
            return buf;
        }

        static int getColor(int color) {
            if (color == cInside || color == cOutside || color == cShape) {
                return color;
            }
            int xored = (color ^ cGrid ^ 0xFF808080);
            if (xored == cInside || xored == cOutside || xored == cShape) {
                return xored;
            }
            return color;
        }

    }
    
    public ShapeTestCase(String name) {
        super(name);
        String classPath = "../resources/shapes/" + Tools.getClasstPath(this.getClass());
        URL url = ClassLoader.getSystemClassLoader().getResource(classPath);
        assertNotNull("Path not found " + classPath, url);
        shapePath = url.getPath();
        outputPath = shapePath + "output/";
    }

    void print(String text) {
        if (OUTPUT) {
            System.out.println(text);        
        }
    }
    
    public FilenameFilter createFilter(final String include, final String exclude) {
        return new FilenameFilter() {
              public boolean accept(File dir, String name) {
                return
                    (include == null || Pattern.matches(include, name)) &&
                    (exclude == null || !Pattern.matches(exclude, name));
              }
        };
    }

    String [] getTestList(String path, FilenameFilter filter) {
          File folder = new File(path);
          String list[] = folder.list(filter);
          if (list != null) {
              for(int i = 0; i < list.length; i++) {
                list[i] = folder.getAbsolutePath() + File.separator + list[i];
              }
          }
          return list;
    }

    void iterator(String name, FilenameFilter filter, Runner runner) {
        if (filter == null) {
            return; // skip test
        }
        if (OUTPUT) {
            new File(outputPath).mkdirs();
        }
        String tests[] = getTestList(shapePath, filter);
        assertTrue("Shapes not found " + shapePath, tests != null && tests.length > 0);
        for(int i = 0; i < tests.length; i++) {
            boolean result = runner.run(tests[i]);
            assertTrue(tests[i] + " " + Tools.PathIterator.equalsError, result);
        }
    }

    public void testGetPathIterator() {
        iterator(
                "getPathIterator()",
                filterShape,
                new Runner.PathIterator());
    }

    public void testContainsPoint() {
        iterator(
                "contains(double,double)",
                filterImage,
                new Runner.Point());
    }

    public void testContainsRect() {
        iterator(
                "contains(double,double,double,double)",
                filterImage,
                new Runner.Rectangle.Contains());
    }

    public void testIntersectsRect() {
        iterator(
                "intersects(double,double,double,double)",
                filterImage,
                new Runner.Rectangle.Intersects());
    }

}

