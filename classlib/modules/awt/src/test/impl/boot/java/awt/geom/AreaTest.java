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

public class AreaTest extends PathIteratorTestCase {

    public AreaTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testConstructor() {
        // Regression test HARMONY-1404
        try {
            new Area(null);
            fail("Expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    public void testContainsPoint() {
        try {
             // Regression test HARMONY-1404
             Area emptyArea = new Area();
             emptyArea.contains((Point2D)null);
             fail("Expected NPE");
         } catch (NullPointerException e) {
             // expected
         }
         
         Area area = new Area(new Ellipse2D.Double(200, 300, 400, 200));
         assertTrue(area.contains(250, 350));
         assertFalse(area.contains(200, 300));
         assertFalse(area.contains(50, 50));
         
         assertTrue(area.contains(new Point2D.Double(500, 400)));
         assertFalse(area.contains(new Point2D.Double(700, 400)));
         
         // Regression test HARMONY-4612
         GeneralPath path = new GeneralPath();
         path.moveTo(50, 100);
         path.lineTo(100, 50);
         path.lineTo(150, 100);
         path.lineTo(100, 150);
         path.closePath();
         
         Area areaPath = new Area(path);
         assertFalse(areaPath.contains(100, 50));
     }

     public void testContainsRect() {
         // Regression test HARMONY-1476
         GeneralPath path = new GeneralPath();
         path.moveTo(100, 500);
         path.lineTo(400, 100);
         path.lineTo(700, 500);
         path.closePath();
         
         Area area = new Area(path);
         assertTrue(area.contains(new Rectangle2D.Double(300, 400, 100, 50)));
         assertFalse(area.contains(new Rectangle2D.Double(50, 400, 700, 50)));
         
         GeneralPath path1 = new GeneralPath();
         path1.moveTo(400, 500);
         path1.quadTo(200, 200, 400, 100);
         path1.quadTo(600, 200, 400, 500);
         path1.closePath();
         
         Area area1 = new Area(path1);
         assertTrue(area1.contains(350, 200, 50, 50));
         assertFalse(area1.contains(100, 50, 600, 500));
         
     	// Regression test HARMONY-1404
         try {
             Area emptyArea = new Area();
             emptyArea.contains((Rectangle2D)null);
             fail("Expected NPE");
         } catch (NullPointerException e) {
             // expected
         }
     }

     public void testIntersectsRect() {
         // Regression test HARMONY-1476
         GeneralPath path = new GeneralPath();
         path.moveTo(100, 500);
         path.lineTo(400, 100);
         path.lineTo(700, 500);
         path.closePath();
         
         Area area = new Area(path);
         assertTrue(area.intersects(new Rectangle2D.Double(300, 400, 100, 50)));
         assertFalse(area.intersects(new Rectangle2D.Double(50, 50, 50, 50)));
         
         GeneralPath path1 = new GeneralPath();
         path1.moveTo(400, 500);
         path1.quadTo(200, 200, 400, 100);
         path1.quadTo(600, 200, 400, 500);
         path1.closePath();
         
         Area area1 = new Area(path1);
         assertTrue(area1.intersects(350, 200, 50, 50));
         assertFalse(area1.intersects(500, 50, 100, 50));
         
         // Regression test HARMONY-1404
         try {
             Area emptyArea = new Area();
             emptyArea.intersects((Rectangle2D)null);
             fail("Expected NPE");
         } catch (NullPointerException e) {
             // expected
         }
     }
     
     public void testIsRectangle() {
     	 // Regression test HARMONY-1476
     	Area area = new Area(new Rectangle2D.Double(200, 300, 400, 150));
     	assertTrue(area.isRectangular());
         
     	GeneralPath path = new GeneralPath();
         path.moveTo(200, 300);
         path.lineTo(600, 300);
         path.lineTo(600, 450);
         path.lineTo(200, 450);
         path.closePath();
         
         Area area1 = new Area(path);
         assertTrue(area1.isRectangular());
         
         Area area2 = new Area(new Ellipse2D.Double(200, 300, 400, 150));
         assertFalse(area2.isRectangular());     
     }
     
     public void testGetPathIterator() {
         // Regression test HARMONY-1860
         Area a = new Area();
         PathIterator path = a.getPathIterator(null);
         checkPathRule(path, PathIterator.WIND_EVEN_ODD);
         checkPathDone(path, true);
     }
     
     public void testCreateTransformedArea() {
         // Regression test HARMONY-1880
         AffineTransform t = AffineTransform.getScaleInstance(2, 3);
         Area a1 = new Area();        
         Area a2 = a1.createTransformedArea(t);
         PathIterator path = a2.getPathIterator(null);
         checkPathRule(path, PathIterator.WIND_EVEN_ODD);
         checkPathDone(path, true);
     }
     
     public void testSubtract() {
         // Regression test HARMONY-4410
 		Rectangle2D rect1 = new Rectangle2D.Double(300, 300, 200, 150);
		Rectangle2D rect2 = new Rectangle2D.Double(350, 200, 300, 150);

		Area area1 = new Area(rect1);
		Area area2 = new Area(rect2);

		Area a = (Area) area1.clone();
		a.intersect(area2);
		area1.add(area2);
		area1.subtract(a);
		
		assertFalse(area1.contains(375, 325));
		assertTrue(area1.contains(600, 300));
		assertTrue(area1.contains(325, 325));
    }
     
    public void testTransformPathIterator() {
        // Regression test HARMONY-4680
        AffineTransform transform = new AffineTransform(2.0, 0.0, 0.0, 200.0 / 140.0, 0.0, 0.0);
        Area ar = new Area(new Rectangle2D.Double(100, 100, 50.0, 100.0));
         
        PathIterator path = ar.getPathIterator(null);
        PathIterator transformedPath = ar.getPathIterator(transform);
        double[] coords = new double[6];
        double[] transformedCoords = new double[6];

        while (!path.isDone() && !transformedPath.isDone()) {
            int rule1 = path.currentSegment(coords);
            int rule2 = transformedPath.currentSegment(transformedCoords);
            assertTrue(rule1 == rule2);
            switch (rule1) {
                case PathIterator.SEG_MOVETO: {
                    transform.transform(coords, 0, coords, 0, 1);
                    assertTrue(coords[0] == transformedCoords[0] && coords[1] == transformedCoords[1]);
                    break;
                }
                case PathIterator.SEG_LINETO: {
                    transform.transform(coords, 0, coords, 0, 1);
                    assertTrue(coords[0] == transformedCoords[0] && coords[1] == transformedCoords[1]);
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    break;
                }
            }
            path.next();
            transformedPath.next();
        }
        assertTrue(path.isDone() && transformedPath.isDone());
    }   

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AreaTest.class);
    }
}
