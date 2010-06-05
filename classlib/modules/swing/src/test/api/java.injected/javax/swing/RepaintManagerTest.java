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
 * @author Anton Avtamonov
 */
package javax.swing;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InvocationEvent;
import org.apache.harmony.awt.ComponentInternals;

public class RepaintManagerTest extends BasicSwingTestCase {
    private Dimension dbMaxSize;

    public RepaintManagerTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        timeoutDelay = 10 * DEFAULT_TIMEOUT_DELAY;
        dbMaxSize = RepaintManager.currentManager(null).getDoubleBufferMaximumSize();
    }

    @Override
    protected void tearDown() throws Exception {
        RepaintManager.currentManager(null).setDoubleBufferMaximumSize(dbMaxSize);
        super.tearDown();
    }

    public void testCurrentManager() throws Exception {
        RepaintManager inst1 = RepaintManager.currentManager(new JButton());
        assertNotNull(inst1);
        RepaintManager inst2 = RepaintManager.currentManager(new Button());
        assertNotNull(inst2);
        RepaintManager inst3 = RepaintManager.currentManager(null);
        assertNotNull(inst3);
        assertTrue(inst1 == inst2);
        assertTrue(inst2 == inst3);
    }

    public void testSetCurrentManager() throws Exception {
        RepaintManager newInst = new RepaintManager();
        RepaintManager.setCurrentManager(newInst);
        assertTrue(RepaintManager.currentManager(null) == newInst);
        RepaintManager.setCurrentManager(null);
        assertFalse(RepaintManager.currentManager(null) == newInst);
        assertNotNull(RepaintManager.currentManager(null));
    }

    public void testAddRemoveInvalidComponent() throws Exception {
        Frame f = new Frame();
        final JPanel rootPanel = new JPanel(new BorderLayout()) {
                private static final long serialVersionUID = 1L;
                
                @Override
                    public boolean isValidateRoot() {
                    return true;
                }
            };
        final JPanel controlled = new JPanel();
        f.add(rootPanel);
        rootPanel.add(controlled);
        
        assertFalse(controlled.isValid());
        assertFalse(rootPanel.isValid());
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    RepaintManager.currentManager(null).addInvalidComponent(controlled);
                }
            });
        
        final Marker isValid = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    isValid.setOccurred(controlled.isValid());
                }
            });
        assertFalse(isValid.isOccurred());
        
        f.setVisible(true);
        waitForIdle();
        assertTrue(controlled.isValid());
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    RepaintManager.currentManager(null).addInvalidComponent(controlled);
                }
            });
        assertTrue(controlled.isValid());
        assertTrue(rootPanel.isValid());
        
        isValid.reset();
        controlled.invalidate();
        waitForIdle();
        assertFalse(controlled.isValid());
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    RepaintManager.currentManager(null).addInvalidComponent(controlled);
                }
            });
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    isValid.setOccurred(controlled.isValid());
                }
            });
        assertTrue(isValid.isOccurred());
        
        isValid.reset();
        controlled.invalidate();
        assertFalse(controlled.isValid());
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    RepaintManager.currentManager(null).addInvalidComponent(controlled);
                    RepaintManager.currentManager(null).removeInvalidComponent(controlled);
                }
            });
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    isValid.setOccurred(controlled.isValid());
                }
            });
        assertTrue(isValid.isOccurred());
        
        isValid.reset();
        controlled.invalidate();
        assertFalse(controlled.isValid());
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    RepaintManager.currentManager(null).addInvalidComponent(controlled);
                    RepaintManager.currentManager(null).removeInvalidComponent(rootPanel);
                }
            });
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    isValid.setOccurred(controlled.isValid());
                }
            });
        assertFalse(isValid.isOccurred());
        
        try { // Regression test for HARMONY-1725
            RepaintManager.currentManager(null).addInvalidComponent(null);
        } catch (NullPointerException e) {
            fail("Unexpected NullPointerException is thrown");
        }
    }

    public void testValidateInvalidComponents() throws Exception {
        Frame f = new Frame();
        final JPanel rootPanel = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isValidateRoot() {
                return true;
            }
        };
        final JPanel controlled = new JPanel();
        f.add(rootPanel);
        rootPanel.add(controlled);

        f.setVisible(true);
        waitForIdle();
        assertTrue(controlled.isValid());
        controlled.invalidate();
        assertFalse(controlled.isValid());
        final Marker isValid = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addInvalidComponent(controlled);
                RepaintManager.currentManager(null).validateInvalidComponents();
                isValid.setOccurred(controlled.isValid());
            }
        });
        assertTrue(isValid.isOccurred());

        f.dispose();
        isValid.reset();
        controlled.invalidate();
        assertFalse(controlled.isValid());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addInvalidComponent(controlled);
                RepaintManager.currentManager(null).removeInvalidComponent(rootPanel);
                RepaintManager.currentManager(null).validateInvalidComponents();
                isValid.setOccurred(controlled.isValid());
            }
        });
        assertFalse(isValid.isOccurred());
    }

    public void testAddDirtyRegion() throws Exception {
        final JPanel root = new JPanel(new BorderLayout());
        JFrame f = new JFrame();
        f.getContentPane().add(root);
        f.setSize(100, 100);
        f.setVisible(true);
        waitForNativePaint(f);
        final Marker marker = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                marker.setAuxiliary(RepaintManager.currentManager(null).getDirtyRegion(root));
            }
        });
        assertEquals(new Rectangle(10, 10, 40, 40), marker.getAuxiliary());
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 200, 200);
                marker.setAuxiliary(RepaintManager.currentManager(null).getDirtyRegion(root));
            }
        });
        assertEquals(new Rectangle(10, 10, 200, 200), marker.getAuxiliary());
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                RepaintManager.currentManager(null).addDirtyRegion(root, 20, 5, 40, 30);
                marker.setAuxiliary(RepaintManager.currentManager(null).getDirtyRegion(root));
            }
        });
        assertEquals(new Rectangle(10, 5, 50, 45), marker.getAuxiliary());
        f.dispose();
    }

    public void testMarkCompletelyDirtyClean() throws Exception {
        final JPanel root = new JPanel(new BorderLayout());
        JFrame f = new JFrame();
        f.getContentPane().add(root);
        f.setSize(100, 100);
        f.setVisible(true);
        waitForNativePaint(f);
        final Marker marker = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                RepaintManager.currentManager(null).markCompletelyDirty(root);
                marker.setAuxiliary(RepaintManager.currentManager(null).getDirtyRegion(root));
            }
        });
        Rectangle dirtyRect = (Rectangle) marker.getAuxiliary();
        assertEquals(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE), dirtyRect);
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).markCompletelyClean(root);
                RepaintManager.currentManager(null).markCompletelyDirty(root);
                marker.setAuxiliary(RepaintManager.currentManager(null).getDirtyRegion(root));
            }
        });
        Rectangle dirtyRect2 = (Rectangle) marker.getAuxiliary();
        assertEquals(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE), dirtyRect2);
        assertNotSame(dirtyRect, dirtyRect2);
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 20,
                        Integer.MAX_VALUE, Integer.MAX_VALUE);
                marker.setOccurred(RepaintManager.currentManager(null).isCompletelyDirty(root));
            }
        });
        assertTrue(marker.isOccurred());
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                RepaintManager.currentManager(null).markCompletelyClean(root);
                marker.setAuxiliary(RepaintManager.currentManager(null).getDirtyRegion(root));
            }
        });
        assertEquals(new Rectangle(), marker.getAuxiliary());
    }

    public void testIsCompletelyDirty() throws Exception {
        final JPanel root = new JPanel(new BorderLayout());
        JFrame f = new JFrame();
        f.getContentPane().add(root);
        f.setSize(100, 100);
        f.setVisible(true);
        waitForNativePaint(f);
        final Marker marker = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                marker.setOccurred(RepaintManager.currentManager(null).isCompletelyDirty(root));
            }
        });
        assertFalse(marker.isOccurred());
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 0, 0, 200, 200);
                marker.setOccurred(RepaintManager.currentManager(null).isCompletelyDirty(root));
            }
        });
        assertFalse(marker.isOccurred());
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 0, 0,
                        Integer.MAX_VALUE, Integer.MAX_VALUE);
                marker.setOccurred(RepaintManager.currentManager(null).isCompletelyDirty(root));
            }
        });
        assertTrue(marker.isOccurred());
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).markCompletelyDirty(root);
                marker.setOccurred(RepaintManager.currentManager(null).isCompletelyDirty(root));
            }
        });
        assertTrue(marker.isOccurred());
    }

    public void testGetDirtyRegion() throws Exception {
        final JPanel root = new JPanel(new BorderLayout());
        JFrame f = new JFrame();
        f.getContentPane().add(root);
        f.setSize(100, 100);
        f.setVisible(true);
        waitForNativePaint(f);
        final Marker marker = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Rectangle r1 = RepaintManager.currentManager(null).getDirtyRegion(root);
                Rectangle r2 = RepaintManager.currentManager(null).getDirtyRegion(root);
                marker.setAuxiliary(new Rectangle[] { r1, r2 });
            }
        });
        assertEquals(new Rectangle(), ((Rectangle[]) marker.getAuxiliary())[0]);
        assertEquals(new Rectangle(), ((Rectangle[]) marker.getAuxiliary())[1]);
        assertNotSame(((Rectangle[]) marker.getAuxiliary())[0], ((Rectangle[]) marker
                .getAuxiliary())[1]);
        marker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                marker.setAuxiliary(RepaintManager.currentManager(null).getDirtyRegion(root));
            }
        });
        assertEquals(new Rectangle(10, 10, 40, 40), marker.getAuxiliary());
    }

    public void testPaintDirtyRegions() throws Exception {
        final Marker rootPaintMarker = new Marker();
        final Marker rootPaintImmediatelyMarker = new Marker();
        final JPanel root = new JPanel(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(final Graphics g) {
                if (checkRepaintEvent()) {
                    rootPaintMarker.setAuxiliary(g.getClipBounds());
                }
                super.paint(g);
            }

            @Override
            public void paintImmediately(final int x, final int y, final int w, final int h) {
                if (checkRepaintEvent()) {
                    rootPaintImmediatelyMarker.setAuxiliary(new Rectangle(x, y, w, h));
                }
                super.paintImmediately(x, y, w, h);
            }

            @Override
            public void paintImmediately(final Rectangle r) {
                if (checkRepaintEvent()) {
                    rootPaintImmediatelyMarker.setAuxiliary(r);
                }
                super.paintImmediately(r);
            }
        };
        final Marker inner1PaintMarker = new Marker();
        final Marker inner1PaintImmediatelyMarker = new Marker();
        final JPanel inner1 = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(final Graphics g) {
                if (checkRepaintEvent()) {
                    inner1PaintMarker.setAuxiliary(g.getClipBounds());
                }
                super.paint(g);
            }

            @Override
            public void paintImmediately(final int x, final int y, final int w, final int h) {
                if (checkRepaintEvent()) {
                    inner1PaintImmediatelyMarker.setAuxiliary(new Rectangle(x, y, w, h));
                }
                super.paintImmediately(x, y, w, h);
            }

            @Override
            public void paintImmediately(final Rectangle r) {
                if (checkRepaintEvent()) {
                    inner1PaintImmediatelyMarker.setAuxiliary(r);
                }
                super.paintImmediately(r);
            }
        };
        inner1.setBounds(20, 20, 40, 40);
        final Marker inner2PaintMarker = new Marker();
        final Marker inner2PaintImmediatelyMarker = new Marker();
        final JPanel inner2 = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(final Graphics g) {
                if (checkRepaintEvent()) {
                    inner2PaintMarker.setAuxiliary(g.getClipBounds());
                }
                super.paint(g);
            }

            @Override
            public void paintImmediately(final int x, final int y, final int w, final int h) {
                if (checkRepaintEvent()) {
                    inner2PaintImmediatelyMarker.setAuxiliary(new Rectangle(x, y, w, h));
                }
                super.paintImmediately(x, y, w, h);
            }

            @Override
            public void paintImmediately(final Rectangle r) {
                if (checkRepaintEvent()) {
                    inner2PaintImmediatelyMarker.setAuxiliary(r);
                }
                super.paintImmediately(r);
            }
        };
        inner2.setBounds(10, 70, 20, 20);
        final JFrame f = new JFrame();
        f.getContentPane().add(root);
        root.add(inner1);
        root.add(inner2);
        f.setSize(150, 150);
        f.setVisible(true);
        waitForNativePaint(f);
        final Marker marker = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                RepaintManager.currentManager(null).paintDirtyRegions();
                marker.setAuxiliary(RepaintManager.currentManager(null).getDirtyRegion(root));
            }
        });
        assertEquals(new Rectangle(), marker.getAuxiliary());
        f.setVisible(false);
        f.setVisible(true);
        waitForNativePaint(f);
        rootPaintMarker.setAuxiliary(null);
        inner1PaintMarker.setAuxiliary(null);
        inner2PaintMarker.setAuxiliary(null);
        rootPaintImmediatelyMarker.setAuxiliary(null);
        inner1PaintImmediatelyMarker.setAuxiliary(null);
        inner2PaintImmediatelyMarker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                RepaintManager.currentManager(null).paintDirtyRegions();
            }
        });
        assertEquals(new Rectangle(10, 10, 40, 40), rootPaintMarker.getAuxiliary());
        assertEquals(new Rectangle(0, 0, 30, 30), inner1PaintMarker.getAuxiliary());
        assertNull(inner2PaintMarker.getAuxiliary());
        assertEquals(new Rectangle(10, 10, 40, 40), rootPaintImmediatelyMarker.getAuxiliary());
        assertNull(inner1PaintImmediatelyMarker.getAuxiliary());
        assertNull(inner2PaintImmediatelyMarker.getAuxiliary());
        f.setVisible(false);
        f.setVisible(true);
        waitForNativePaint(f);
        rootPaintMarker.setAuxiliary(null);
        inner1PaintMarker.setAuxiliary(null);
        inner2PaintMarker.setAuxiliary(null);
        rootPaintImmediatelyMarker.setAuxiliary(null);
        inner1PaintImmediatelyMarker.setAuxiliary(null);
        inner2PaintImmediatelyMarker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 5, 10, 20, 70);
                RepaintManager.currentManager(null).paintDirtyRegions();
            }
        });
        assertEquals(new Rectangle(5, 10, 20, 70), rootPaintMarker.getAuxiliary());
        assertEquals(new Rectangle(0, 0, 5, 40), inner1PaintMarker.getAuxiliary());
        assertEquals(new Rectangle(0, 0, 15, 10), inner2PaintMarker.getAuxiliary());
        assertEquals(new Rectangle(5, 10, 20, 70), rootPaintImmediatelyMarker.getAuxiliary());
        assertNull(inner1PaintImmediatelyMarker.getAuxiliary());
        assertNull(inner2PaintImmediatelyMarker.getAuxiliary());
        f.setVisible(false);
        f.setVisible(true);
        waitForNativePaint(f);
        rootPaintMarker.setAuxiliary(null);
        inner1PaintMarker.setAuxiliary(null);
        inner2PaintMarker.setAuxiliary(null);
        rootPaintImmediatelyMarker.setAuxiliary(null);
        inner1PaintImmediatelyMarker.setAuxiliary(null);
        inner2PaintImmediatelyMarker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(root, 10, 10, 40, 40);
                RepaintManager.currentManager(null).addDirtyRegion(inner1, 20, 25, 15, 10);
                RepaintManager.currentManager(null).paintDirtyRegions();
            }
        });
        assertEquals(new Rectangle(10, 10, 45, 45), rootPaintMarker.getAuxiliary());
        assertEquals(new Rectangle(0, 0, 35, 35), inner1PaintMarker.getAuxiliary());
        assertNull(inner2PaintMarker.getAuxiliary());
        assertEquals(new Rectangle(10, 10, 45, 45), rootPaintImmediatelyMarker.getAuxiliary());
        assertNull(inner1PaintImmediatelyMarker.getAuxiliary());
        assertNull(inner2PaintImmediatelyMarker.getAuxiliary());
        f.setVisible(false);
        f.setVisible(true);
        waitForNativePaint(f);
        rootPaintMarker.setAuxiliary(null);
        inner1PaintMarker.setAuxiliary(null);
        inner2PaintMarker.setAuxiliary(null);
        rootPaintImmediatelyMarker.setAuxiliary(null);
        inner1PaintImmediatelyMarker.setAuxiliary(null);
        inner2PaintImmediatelyMarker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(inner1, 10, 10, 20, 20);
                RepaintManager.currentManager(null).paintDirtyRegions();
            }
        });
        assertNull(rootPaintMarker.getAuxiliary());
        assertEquals(new Rectangle(10, 10, 20, 20), inner1PaintMarker.getAuxiliary());
        assertNull(inner2PaintMarker.getAuxiliary());
        assertNull(rootPaintImmediatelyMarker.getAuxiliary());
        assertEquals(new Rectangle(10, 10, 20, 20), inner1PaintImmediatelyMarker.getAuxiliary());
        assertNull(inner2PaintImmediatelyMarker.getAuxiliary());
        f.setVisible(false);
        f.setVisible(true);
        waitForNativePaint(f);
        rootPaintMarker.setAuxiliary(null);
        inner1PaintMarker.setAuxiliary(null);
        inner2PaintMarker.setAuxiliary(null);
        rootPaintImmediatelyMarker.setAuxiliary(null);
        inner1PaintImmediatelyMarker.setAuxiliary(null);
        inner2PaintImmediatelyMarker.setAuxiliary(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                RepaintManager.currentManager(null).addDirtyRegion(inner1, 10, 10, 20, 20);
                RepaintManager.currentManager(null).addDirtyRegion(inner2, 5, 5, 10, 10);
                RepaintManager.currentManager(null).paintDirtyRegions();
            }
        });
        assertNull(rootPaintMarker.getAuxiliary());
        assertEquals(new Rectangle(10, 10, 20, 20), inner1PaintMarker.getAuxiliary());
        assertEquals(new Rectangle(5, 5, 10, 10), inner2PaintMarker.getAuxiliary());
        assertNull(rootPaintImmediatelyMarker.getAuxiliary());
        assertEquals(new Rectangle(10, 10, 20, 20), inner1PaintImmediatelyMarker.getAuxiliary());
        assertEquals(new Rectangle(5, 5, 10, 10), inner2PaintImmediatelyMarker.getAuxiliary());
        f.dispose();
    }

    public void testIsDoubleBufferingEnabled() throws Exception {
        assertTrue(RepaintManager.currentManager(null).isDoubleBufferingEnabled());
        RepaintManager.currentManager(null).setDoubleBufferingEnabled(false);
        assertFalse(RepaintManager.currentManager(null).isDoubleBufferingEnabled());
    }

    public void testGetDoubleBufferMaximumSize() throws Exception {
        assertEquals(Toolkit.getDefaultToolkit().getScreenSize(), RepaintManager
                .currentManager(null).getDoubleBufferMaximumSize());
        Dimension bufferSize = new Dimension(100, 100);
        RepaintManager.currentManager(null).setDoubleBufferMaximumSize(bufferSize);
        assertEquals(bufferSize, RepaintManager.currentManager(null)
                .getDoubleBufferMaximumSize());
    }

    public void testGetOffscreenBuffer() throws Exception {
        JPanel root = new JPanel();
        JFrame f = new JFrame();
        f.getContentPane().add(root);
        assertNull(RepaintManager.currentManager(null).getOffscreenBuffer(root, 10, 10));
        f.pack();
        Image offscreenImage = RepaintManager.currentManager(null).getOffscreenBuffer(root, 10,
                10);
        assertNotNull(offscreenImage);
        assertEquals(10, offscreenImage.getWidth(f));
        assertEquals(10, offscreenImage.getHeight(f));
        assertEquals(RepaintManager.currentManager(null).getOffscreenBuffer(root, 10, 10),
                RepaintManager.currentManager(null).getOffscreenBuffer(root, 10, 10));
        assertEquals(RepaintManager.currentManager(null).getOffscreenBuffer(f.getRootPane(),
                10, 10), RepaintManager.currentManager(null).getOffscreenBuffer(root, 10, 10));
        assertEquals(RepaintManager.currentManager(null).getOffscreenBuffer(f, 10, 10),
                RepaintManager.currentManager(null).getOffscreenBuffer(root, 10, 10));
        Image im10x10 = RepaintManager.currentManager(null).getOffscreenBuffer(root, 10, 10);
        Image im10x20 = RepaintManager.currentManager(null).getOffscreenBuffer(root, 10, 20);
        Image im20x10 = RepaintManager.currentManager(null).getOffscreenBuffer(root, 20, 10);
        Image im20x20 = RepaintManager.currentManager(null).getOffscreenBuffer(root, 20, 20);
        assertNotSame(im10x10, im10x20);
        assertNotSame(im10x20, im20x10);
        assertNotSame(im10x10, im20x10);
        assertNotSame(im10x20, im20x20);
        assertNotSame(im10x10, RepaintManager.currentManager(null).getOffscreenBuffer(root, 10,
                10));
        assertSame(im20x20, RepaintManager.currentManager(null)
                .getOffscreenBuffer(root, 10, 10));
        assertSame(im20x20, RepaintManager.currentManager(null)
                .getOffscreenBuffer(root, 20, 20));
        assertSame(im20x20, RepaintManager.currentManager(null).getOffscreenBuffer(f, 20, 20));
        assertSame(im20x20, RepaintManager.currentManager(null).getOffscreenBuffer(
                new JButton(), 20, 20));
        Image im30x20 = RepaintManager.currentManager(null).getOffscreenBuffer(root, 30, 20);
        assertNotSame(im20x20, im30x20);
        assertSame(im30x20, RepaintManager.currentManager(null)
                .getOffscreenBuffer(root, 20, 20));
        assertNull(RepaintManager.currentManager(null)
                .getOffscreenBuffer(new JButton(), 50, 20));
        assertNotSame(im30x20, RepaintManager.currentManager(null).getOffscreenBuffer(root, 20,
                20));
        offscreenImage = RepaintManager.currentManager(null).getOffscreenBuffer(root, 10000,
                10000);
        assertNotNull(offscreenImage);
        assertEquals(RepaintManager.currentManager(null).getDoubleBufferMaximumSize().width,
                offscreenImage.getWidth(f));
        assertEquals(RepaintManager.currentManager(null).getDoubleBufferMaximumSize().height,
                offscreenImage.getHeight(f));
        offscreenImage = RepaintManager.currentManager(null).getOffscreenBuffer(root, 10000,
                10000);
        assertNotNull(offscreenImage);
        assertEquals(RepaintManager.currentManager(null).getDoubleBufferMaximumSize().width,
                offscreenImage.getWidth(f));
        assertEquals(RepaintManager.currentManager(null).getDoubleBufferMaximumSize().height,
                offscreenImage.getHeight(f));
        f.dispose();
    }

    public void testGetVolatileOffscreenBuffer() throws Exception {
        JPanel root = new JPanel();
        JFrame f = new JFrame();
        f.getContentPane().add(root);
        f.pack();
        Image offscreenImage = RepaintManager.currentManager(null).getVolatileOffscreenBuffer(
                root, 400, 400);
        assertNotNull(offscreenImage);
        assertEquals(400, offscreenImage.getWidth(f));
        assertEquals(400, offscreenImage.getHeight(f));
        assertEquals(RepaintManager.currentManager(null).getVolatileOffscreenBuffer(root, 400,
                400), RepaintManager.currentManager(null).getVolatileOffscreenBuffer(root, 400,
                400));
        assertEquals(RepaintManager.currentManager(null).getVolatileOffscreenBuffer(
                f.getRootPane(), 400, 400), RepaintManager.currentManager(null)
                .getVolatileOffscreenBuffer(root, 400, 400));
        assertEquals(RepaintManager.currentManager(null)
                .getVolatileOffscreenBuffer(f, 400, 400), RepaintManager.currentManager(null)
                .getVolatileOffscreenBuffer(root, 400, 400));
        Image im400x400 = RepaintManager.currentManager(null).getVolatileOffscreenBuffer(root,
                400, 400);
        Image im400x420 = RepaintManager.currentManager(null).getVolatileOffscreenBuffer(root,
                400, 420);
        Image im420x400 = RepaintManager.currentManager(null).getVolatileOffscreenBuffer(root,
                420, 400);
        Image im420x420 = RepaintManager.currentManager(null).getVolatileOffscreenBuffer(root,
                420, 420);
        assertNotSame(im400x400, im400x420);
        assertNotSame(im400x420, im420x400);
        assertNotSame(im400x420, im420x400);
        assertNotSame(im400x420, im420x420);
        assertNotSame(im400x400, RepaintManager.currentManager(null)
                .getVolatileOffscreenBuffer(root, 400, 400));
        assertSame(im420x420, RepaintManager.currentManager(null).getVolatileOffscreenBuffer(
                root, 400, 400));
        assertSame(im420x420, RepaintManager.currentManager(null).getVolatileOffscreenBuffer(
                root, 420, 420));
        assertSame(im420x420, RepaintManager.currentManager(null).getVolatileOffscreenBuffer(f,
                420, 420));
        assertSame(im420x420, RepaintManager.currentManager(null).getVolatileOffscreenBuffer(
                new JButton(), 420, 420));
        Image im430x420 = RepaintManager.currentManager(null).getVolatileOffscreenBuffer(root,
                430, 420);
        assertNotSame(im420x420, im430x420);
        assertSame(im430x420, RepaintManager.currentManager(null).getVolatileOffscreenBuffer(
                root, 420, 420));
        assertSame(im430x420, RepaintManager.currentManager(null).getVolatileOffscreenBuffer(
                root, 420, 420));
        offscreenImage = RepaintManager.currentManager(null).getVolatileOffscreenBuffer(root,
                10000, 10000);
        assertNotNull(offscreenImage);
        assertEquals(RepaintManager.currentManager(null).getDoubleBufferMaximumSize().width,
                offscreenImage.getWidth(f));
        assertEquals(RepaintManager.currentManager(null).getDoubleBufferMaximumSize().height,
                offscreenImage.getHeight(f));
        f.dispose();
    }

    private boolean checkRepaintEvent() {
        return EventQueue.getCurrentEvent() instanceof InvocationEvent;
    }

    private void waitForNativePaint(final Window w) throws Exception {
        if (!isHarmony()) {
            return;
        }
        int counter = 0;
        while (!wasPainted(w) && counter++ < 50) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        Thread.sleep(1000);
    }

    private boolean wasPainted(final Window w) throws Exception {
        final Marker result = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                result.setOccurred(ComponentInternals.getComponentInternals().wasPainted(w));
            }
        });
        return result.isOccurred();
    }
}
