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
 * @author Denis M. Kishenko
 */
package java.awt.geom;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Tools;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class ShapeProducer {

    Color colorBackground = Color.white;
    Color colorShape = Color.gray;
    Color colorFill = Color.lightGray;
    Color colorGrid = new Color(0xA0, 0xA0, 0xA0);

    double[][][] rect = new double[][][] {
            {{50, 50, 100, 200}, null, null},
            {{50, 50, 200, 100}, {1, 0, 0, 1, 20, 30}, null},
    };

    double[][][] round = new double[][][] {
            {{50, 50, 400, 200, 140, 100}, null, null},
            {{50, 50, 400, 200, 140, 100}, null, {10}},
            {{50, 50, 400, 200, 140, 100}, null, {5}},
            {{50, 50, 200, 400, 100, 140}, {1, 0, 0, 1, 30, 20}, null},
    };

    double[][][] ellipse = new double[][][] {
            {{50, 50, 100, 200}, null, null},
            {{50, 50, 200, 100}, null, {5}},
            {{50, 50, 200, 100}, null, {10}},
            {{50, 50, 200, 100}, {1, 0, 0, 1, 20, 30}, null},
    };

    double[][][] arc = new double[][][] {
            {{50, 50, 200, 200,    0,   60}, null, null, {0}},
            {{50, 50, 400, 400,  -50,  170}, null, null, {1}},
            {{50, 50, 200, 200,  100,  200}, null, null, {2}},
            {{50, 50, 400, 400, -150,  270}, null, null, {0}},
            {{50, 50, 200, 200,  200,  359}, null, null, {1}},
            {{50, 50, 200, 400, -250,  360}, null, null, {2}},
            {{50, 50, 400, 200,  300,  361}, null, null, {0}},
            {{50, 50, 400, 400, -350,  390}, null, null, {1}},
            {{50, 50, 200, 200,  400,  710}, null, null, {2}},
            {{50, 50, 200, 400, -450,  730}, null, null, {0}},

            {{50, 50, 400, 200,    0,  -60}, null, null, {1}},
            {{50, 50, 400, 400,   50, -170}, null, null, {2}},
            {{50, 50, 200, 200, -100, -200}, null, null, {0}},
            {{50, 50, 200, 400,  150, -270}, null, null, {1}},
            {{50, 50, 400, 200, -200, -359}, null, null, {2}},
            {{50, 50, 400, 400,  250, -360}, null, null, {0}},
            {{50, 50, 200, 200, -300, -361}, null, null, {1}},
            {{50, 50, 200, 400,  350, -390}, null, null, {2}},
            {{50, 50, 400, 200, -400, -710}, null, null, {0}},
            {{50, 50, 400, 400,  450, -730}, null, null, {1}},

            {{50, 50, 400, 400,  20,  330}, null, null, {2}},
            {{50, 50, 400, 400,  20, -330}, null, null, {0}},

            {{50, 50, 200, 400,  40, -80}, null, null, {1}},
            {{50, 50, 200, 400,  40, -80}, {1, 2, 2, 1, 0, 0}, null, {2}},
            {{50, 50, 200, 400,  40, -80}, {2, 0, 0, 2, 0, 0}, null, {0}},

            {{50, 50, 200, 200,    0,  60}, null, {10}, {1}},
            {{50, 50, 200, 200,  100, 200}, null, {20}, {2}},
            {{50, 50, 400, 400, -150, 270}, null, {30}, {0}},
            {{50, 50, 400, 400,  20,  330}, null, {40}, {1}},
            {{50, 50, 400, 400,  20,  330}, null, {10}, {2}},
    };

    double[][][] quad = new double[][][] {
            {{50, 50, 250, 100, 150, 250}, null, null},
            {{250, 50, 50, 250, 200, 200}, null, null}
    };

    double[][][] cubic = new double[][][] {
            {{50, 50, 150, 150, 250, 150, 350, 60}, null, null},
            {{50, 50, 150, 200, 300, 320, 200, 100}, null, null},
            {{300, 50, 50, 200, 300, 50, 350, 150}, null, null},
            {{150, 250, 50, 250, 250, 50, 250, 200}, null, null},
            {{450, 100, 50, 250, 150, 50, 300, 250}, null, null},
            {{50, 300, 150, 250, 150, 100, 300, 50}, null, null},
            {{50, 50, 100, 150, 250, 100, 50, 300}, null, null},
    };


    double[][][] polygon = new double[][][] {
            {{50, 50, 150, 250, 250, 150}, null, null},
            {{50, 50, 150, 50, 150, 150, 50, 150}, null, null},
            {{50, 300, 250, 150, 100, 100, 200, 250, 200, 50}, null, null},
            {{50, 50, 200, 50, 200, 150, 100, 150, 100, 100, 150, 100, 150, 200, 50, 200}, null, null},
    };

    String path;

    public ShapeProducer(String path) {
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        this.path = path;
/*
        createRect();
        createRound();
        createEllipse();
        createArc();
        createQuad();
        createCubic();
*/
        createPolygon();
    }

    AffineTransform createTransform(double[] matrix) {
        return matrix == null ? null : new AffineTransform(matrix);
    }

    Rectangle2D createRect(double[] matrix) {
        return new Rectangle2D.Double(
                matrix[0],
                matrix[1],
                matrix[2],
                matrix[3]);
    }

    RoundRectangle2D createRound(double[] matrix) {
        return new RoundRectangle2D.Double(
                matrix[0],
                matrix[1],
                matrix[2],
                matrix[3],
                matrix[4],
                matrix[5]);
    }

    Ellipse2D createEllipse(double[] matrix) {
        return new Ellipse2D.Double(
                matrix[0],
                matrix[1],
                matrix[2],
                matrix[3]);
    }

    Arc2D createArc(double[] matrix, int type) {
        return new Arc2D.Double(
                matrix[0],
                matrix[1],
                matrix[2],
                matrix[3],
                matrix[4],
                matrix[5],
                type);
    }

    QuadCurve2D createQuad(double[] matrix) {
        return new QuadCurve2D.Double(
                matrix[0],
                matrix[1],
                matrix[2],
                matrix[3],
                matrix[4],
                matrix[5]);
    }

    CubicCurve2D createCubic(double[] matrix) {
        return new CubicCurve2D.Double(
                matrix[0],
                matrix[1],
                matrix[2],
                matrix[3],
                matrix[4],
                matrix[5],
                matrix[6],
                matrix[7]);
    }

    Polygon createPolygon(double[] matrix) {
        Polygon p = new Polygon();
        for(int i = 0; i < matrix.length;) {
            p.addPoint((int)matrix[i++], (int)matrix[i++]);
        }
        return p;
    }

    void createRect() {
        for (double[][] element : rect) {
            saveRectShape(
                    path,
                    createRect(element[0]),
                    createTransform(element[1]),
                    createFlatness(element[2]));
        }
    }

    void createRound() {
        for (double[][] element : round) {
            saveRectShape(
                    path,
                    createRound(element[0]),
                    createTransform(element[1]),
                    createFlatness(element[2]));
        }
    }

    void createEllipse() {
        for (double[][] element : ellipse) {
            saveRectShape(
                    path,
                    createEllipse(element[0]),
                    createTransform(element[1]),
                    createFlatness(element[2]));
        }
    }

    void createArc() {
        for (double[][] element : arc) {
            for(int j = 0; j < element[3].length; j++) {
                saveRectShape(
                        path,
                        createArc(element[0], (int)element[3][j]),
                        createTransform(element[1]),
                        createFlatness(element[2]));
            }
        }
    }

    void createQuad() {
        for (double[][] element : quad) {
            saveRectShape(
                    path,
                    createQuad(element[0]),
                    createTransform(element[1]),
                    createFlatness(element[2]));
        }
    }

    void createCubic() {
        for (double[][] element : cubic) {
            saveRectShape(
                    path,
                    createCubic(element[0]),
                    createTransform(element[1]),
                    createFlatness(element[2]));
        }
    }

    void createPolygon() {
        for (double[][] element : polygon) {
            saveRectShape(
                    path,
                    createPolygon(element[0]),
                    createTransform(element[1]),
                    createFlatness(element[2]));
        }
    }

    String doubleToStr(double a) {
        if (a == (int)a) {
            return "" + (int)a;
        }
        return "" + a;
    }


    Point2D transform(AffineTransform at, Point2D p) {
        return at == null ? p : at.transform(p, null);
    }

    void saveRectShape(String outputPath, Shape shape, AffineTransform at, double flatness) {
        PathIterator path = flatness < 0.0 ? shape.getPathIterator(at) : shape.getPathIterator(at, flatness);
        GeneralPath src = new GeneralPath(path.getWindingRule());
        src.append(path, false);

        Rectangle r = src.getBounds();
        int width = (int)(r.getX() + r.getWidth()) + 50;
        int height = (int)(r.getY() + r.getHeight()) + 50;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics g = img.getGraphics();

        // Background
        g.setColor(colorBackground);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        // Fill shape
/*
        g.setColor(colorFill);
        ((Graphics2D)g).fill(src);
*/
        for(int x = 0; x < img.getWidth(); x++) {
            for(int y = 0; y < img.getHeight(); y++) {
                if (x == 120 && y == 120) {
                    System.out.println("test");
                }
                if (src.contains(x, y)) {
                    img.setRGB(x, y, colorFill.getRGB());
                }
            }
        }

        // Draw shape
        Stroke oldStroke = ((Graphics2D)g).getStroke();
        ((Graphics2D)g).setStroke(new BasicStroke(4.0f));
        g.setColor(colorShape);
        ((Graphics2D)g).draw(src);

        if (shape instanceof Arc2D) {
            Arc2D arc = (Arc2D)shape;
            switch(arc.getArcType()) {
            case Arc2D.PIE:
                // Exclude center of pie
                Point2D p = transform(
                        at,
                        new Point2D.Double(
                                ((Arc2D)shape).getCenterX(),
                                ((Arc2D)shape).getCenterY()));
                g.fillOval((int)(p.getX() - 3), (int)(p.getY() - 3), 6, 6);
                break;
            case Arc2D.OPEN:
                Point2D p1 = transform(at, arc.getStartPoint());
                Point2D p2 = transform(at, arc.getEndPoint());
                // Exclude span
                ((Graphics2D)g).draw(new Line2D.Double(p1, p2));
                break;
            }
        }
        if (shape instanceof QuadCurve2D) {
            QuadCurve2D c = (QuadCurve2D)shape;
            ((Graphics2D)g).draw(new Line2D.Double(c.getP1(), c.getP2()));
        }
        if (shape instanceof CubicCurve2D) {
            CubicCurve2D c = (CubicCurve2D)shape;
            ((Graphics2D)g).draw(new Line2D.Double(c.getP1(), c.getP2()));
        }

        ((Graphics2D)g).setStroke(oldStroke);
        g.setXORMode(colorGrid);
        for(int x = 0; x < img.getWidth(); x += 50) {
            g.drawLine(x, 0, x, img.getHeight());
        }
        for(int y = 0; y < img.getHeight(); y += 50) {
            g.drawLine(0, y, img.getWidth(), y);
        }
        g.setPaintMode();

        String shapeDesc = getShapeDesc(shape);

        System.out.println(outputPath + shapeDesc);

        if (at != null) {
            shapeDesc += "_" + getAffineDesc(at);
        }

        if (flatness >= 0.0) {
            shapeDesc += "_flat(" + doubleToStr(flatness) + ")";
        }

        Tools.Shape.save(src, outputPath + shapeDesc + ".shape");
        Tools.BufferedImage.saveIcon(img, outputPath + shapeDesc + ".ico");

    }

    String getAffineDesc(AffineTransform at) {
        return
            "affine(" +
            doubleToStr(at.getScaleX()) + "," +
            doubleToStr(at.getShearY()) + "," +
            doubleToStr(at.getShearX()) + "," +
            doubleToStr(at.getScaleY()) + "," +
            doubleToStr(at.getTranslateX()) + "," +
            doubleToStr(at.getTranslateY()) + ")";
    }

    String getShapeDesc(Shape shape) {
        String shapeDesc = null;

        if (shape instanceof RectangularShape) {
            RectangularShape rshape = (RectangularShape)shape;
            shapeDesc =
                doubleToStr(rshape.getX()) + "," +
                doubleToStr(rshape.getY()) + "," +
                doubleToStr(rshape.getWidth()) + "," +
                doubleToStr(rshape.getHeight());

            if (shape instanceof Ellipse2D) {
                shapeDesc = "ellipse(" + shapeDesc + ")";
            } else
            if (shape instanceof Rectangle2D) {
                shapeDesc = "rect(" + shapeDesc + ")";
            } else
            if (shape instanceof RoundRectangle2D) {
                RoundRectangle2D rr = (RoundRectangle2D)shape;
                shapeDesc = "round(" + shapeDesc + "," +
                    doubleToStr(rr.getArcWidth()) + "," +
                    doubleToStr(rr.getArcHeight()) + ")";
            } else
            if (shape instanceof Arc2D) {
                Arc2D a = (Arc2D)shape;
                shapeDesc = "arc(" + shapeDesc + "," +
                    doubleToStr(a.getAngleStart()) + "," +
                    doubleToStr(a.getAngleExtent());

                switch(a.getArcType()) {
                case Arc2D.CHORD:
                    shapeDesc += ",CHORD)";
                    break;
                case Arc2D.OPEN:
                    shapeDesc += ",OPEN)";
                    break;
                case Arc2D.PIE:
                    shapeDesc += ",PIE)";
                    break;
                }
            }
        } else
        if (shape instanceof QuadCurve2D) {
            QuadCurve2D c = (QuadCurve2D)shape;
            shapeDesc = "quad(" +
                doubleToStr(c.getX1()) + "," +
                doubleToStr(c.getY1()) + "," +
                doubleToStr(c.getCtrlX()) + "," +
                doubleToStr(c.getCtrlY()) + "," +
                doubleToStr(c.getX2()) + "," +
                doubleToStr(c.getY2()) + ")";
        } else
        if (shape instanceof CubicCurve2D) {
            CubicCurve2D c = (CubicCurve2D)shape;
            shapeDesc = "cubic(" +
                doubleToStr(c.getX1()) + "," +
                doubleToStr(c.getY1()) + "," +
                doubleToStr(c.getCtrlX1()) + "," +
                doubleToStr(c.getCtrlY1()) + "," +
                doubleToStr(c.getCtrlX2()) + "," +
                doubleToStr(c.getCtrlY2()) + "," +
                doubleToStr(c.getX2()) + "," +
                doubleToStr(c.getY2()) + ")";
        } else
        if (shape instanceof Polygon) {
            Polygon p = (Polygon)shape;
            shapeDesc = "polygon(";
            for(int i = 0; i < p.npoints; i++) {
                shapeDesc += p.xpoints[i] + "," + p.ypoints[i];
                if (i < p.npoints - 1) {
                    shapeDesc += ",";
                }
            }
            shapeDesc += ")";
        }

        return shapeDesc;
    }

    double createFlatness(double[] buf) {
        return buf == null ? -1.0 : buf[0];
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("ShapeProduccer [outputpath]");
            return;
        }
        new ShapeProducer(args[0]);
    }
}
