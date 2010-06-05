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
 * @author Dmitry A. Durnev
 */
/*
 * Created on 29.09.2004
 *
 */
package java.awt;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.peer.ComponentPeer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

@SuppressWarnings("serial")
public class ComponentTest extends TestCase {
    Robot robot;

    public class SimpleComponent extends Component {
    }

    public class SimpleContainer extends Container {
    }
    /* more complex component for [deprecated] event testing */
    @SuppressWarnings("deprecation")
    class MyComponent extends Canvas {

        @Override
        public boolean gotFocus(Event evt, Object what) {
            synchronized(robot) {
                return callback(evt);
            }
        }
        @Override
        public boolean lostFocus(Event evt, Object what) {
            synchronized(robot) {
                return callback(evt);
            }
        }
        @Override
        public boolean keyDown(Event evt, int key) {
            synchronized(robot) {
                return callback(evt);                
            }
        }
        @Override
        public boolean keyUp(Event evt, int key) {
            synchronized(robot) {                
                return callback(evt);
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, getWidth(), getHeight());
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    paintFlag = true;
                }
            });
        }
    }
    @SuppressWarnings("deprecation")
    class MoveResizeComponent extends SimpleComponent {
        @Override
        public void reshape(int x, int y, int w, int h) {
            assertTrue(methodCalled);
            assertTrue(setBoundsCalled);
            assertFalse(reshapeCalled);
            reshapeCalled = true;
            super.reshape(x, y, w, h);
        }
        @Override
        public void setBounds(int x, int y, int w, int h) {
            assertTrue(methodCalled);
            assertFalse(setBoundsCalled);
            setBoundsCalled = true;
            super.setBounds(x, y, w, h);
        }
    }
    @SuppressWarnings("deprecation")
    class MyButton extends Button {
        @Override
        public boolean  action(Event evt, Object arg1) {
            synchronized(robot) {
                return callback(evt);
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    paintFlag = true;
                }
            });
        }
    }
    @SuppressWarnings("deprecation")
    class MyFrame extends Frame {

        @Override
        public boolean handleEvent(Event evt) {
            synchronized (robot) {
                return callback(evt);
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, getWidth(), getHeight());
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    paintFlag = true;
                }
            });
        }
    }
    SimpleComponent tc;
    Component myTestComp;
    Frame frame;
    boolean paintFlag;
    boolean listenerCalled, parentListenerCalled;

    private static final Map<Integer, Event> oldEventsMap = new HashMap<Integer, Event>(); //<int, boolean> key: Event id, value: if listener was called

    boolean methodCalled, setBoundsCalled, reshapeCalled;
    String propName;
    PropertyChangeListener propListener = new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent pce) {
            listenerCalled = true;
            propName = pce.getPropertyName();
            oldValue = pce.getOldValue();
            newValue = pce.getNewValue();
            src = pce.getSource();
        }
    };
    Object oldValue, newValue, src;
    int waitTime = 3000; //time to wait for events in ms
    private Event event; //saved last deprecated Event object
    private final int nRetries = 3; //number of times to repeat [robot] actions

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ComponentTest.class);

    }

    static void putEvent(int id, Event evt) {
        oldEventsMap.put(new Integer(id), evt);
    }

    static Event getEvent(int id) {
        Object val = oldEventsMap.get(new Integer(id));
        if (val instanceof Event) {
            return (Event) val;
        }
        return null;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tc = new SimpleComponent();
        myTestComp = new MyComponent();
        cleanPropertyFields();
        event = null;
        methodCalled = setBoundsCalled = reshapeCalled = false;
    }
    public void testCreate(){
        assertNotNull(tc);
        assertNotNull(myTestComp);
    }
    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {

        if (frame != null) {
            frame.dispose();
            Thread.sleep(500);//wait for the frame to be disposed
            frame = null;
            if (oldEventsMap != null) {
                oldEventsMap.clear();
            }
        }
        super.tearDown();

    }

    public final void testSetVisible() {
        tc.setVisible(true);
        assertTrue(tc.isVisible());
        tc.setVisible(false);
        assertFalse(tc.isVisible());
    }

    public final void testSetSize() {
        Dimension s = new Dimension(20, 10);
        tc.setSize(s);
        assertEquals(s, tc.getSize());
    }

    public final void testSetLocation() {
        Point p = new Point(20, 10);
        tc.setLocation(p);
        assertEquals(p, tc.getLocation());
    }

    public final void testContains() {
        Dimension s = new Dimension(20, 10);
        tc.setSize(s);
        Point p = new Point(30, 40), p1 = new Point(-10, 0);
        tc.setLocation(p);
        assertFalse(tc.contains(p1));
        assertFalse(tc.contains(p));
        assertTrue(tc.contains(new Point(15,5)));
        assertFalse(tc.contains(new Point(20, 5)));
        assertFalse(tc.contains(new Point(5, 10)));
    }

    public final void testSetPreferredSize() {
        Dimension s0 = new Dimension();
        assertEquals(tc.getPreferredSize(), s0);
        Dimension s = new Dimension(100, 50);
        tc.setPreferredSize(s);
        assertEquals(tc.getPreferredSize(), s);
        tc.setPreferredSize(null);
        assertEquals(tc.getPreferredSize(), s0);
    }

    public final void testGetPreferredSize() {

        Dimension s0 = new Dimension();
        assertEquals(s0, tc.getPreferredSize());
        Dimension s = new Dimension(100, 50);
        tc.setMinimumSize(s);
        assertEquals(s, tc.getPreferredSize());
        tc.setMinimumSize(null);
        tc.setSize(s = new Dimension(10, 50));
        assertEquals(s, tc.getPreferredSize());
    }

    //focus:
    public final void testSetFocusable() {
        assertTrue(tc.isFocusable());
        tc.setFocusable(false);
        assertFalse(tc.isFocusable());
        tc.setFocusable(true);
        assertTrue(tc.isFocusable());
    }

    public final void testGetFocusCycleRootAncestor() {
        assertNull(tc.getFocusCycleRootAncestor());
        SimpleContainer tC = new SimpleContainer();
        tC.add(tc);
        assertNull(tc.getFocusCycleRootAncestor());
        tC.setFocusCycleRoot(true);
        assertSame(tC, tc.getFocusCycleRootAncestor());
        tC.setFocusCycleRoot(false);
        assertNull(tc.getFocusCycleRootAncestor());
    }

    public final void testIsFocusCycleRoot() {
        assertTrue(tc.isFocusCycleRoot(null));
        SimpleContainer tC = new SimpleContainer();
        tC.add(tc);
        assertTrue(tc.isFocusCycleRoot(null));
        tC.setFocusCycleRoot(true);
        assertTrue(tc.isFocusCycleRoot(tC));
        tC.setFocusCycleRoot(false);
        assertTrue(tc.isFocusCycleRoot(null));
    }

    public final void testSetFocusTraversalKeysEnabled() {
        assertTrue(tc.getFocusTraversalKeysEnabled());
        tc.setFocusTraversalKeysEnabled(false);
        assertFalse(tc.getFocusTraversalKeysEnabled());
    }

    public final void testGetFocusTraversalKeys() {
        Set<AWTKeyStroke> testSet = Collections.emptySet();
        assertEquals(testSet, tc.getFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS));
        Set<AWTKeyStroke> forSet = tc.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        testSet = new HashSet<AWTKeyStroke>();
        testSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
        testSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK));
        assertEquals(testSet, forSet);
    }

    public final void testSetFocusTraversalKeys() {
        int keysID = KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS;
        assertFalse(tc.areFocusTraversalKeysSet(keysID));
        Set<AWTKeyStroke> upSet = new HashSet<AWTKeyStroke>();
        upSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_SPACE, 0));
        tc.setFocusTraversalKeys(keysID, upSet);
        assertTrue(tc.areFocusTraversalKeysSet(keysID));
        assertEquals(upSet, tc.getFocusTraversalKeys(keysID));
    }

    public final void testAreFocusTraversalKeysSet() {
        int id = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
        assertFalse(tc.areFocusTraversalKeysSet(id));
        Set<AWTKeyStroke> testSet = new HashSet<AWTKeyStroke>();
        testSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
        testSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK));
        tc.setFocusTraversalKeys(id, testSet);
        assertTrue(tc.areFocusTraversalKeysSet(id));

    }
    
    public void testGetFont() {
        // Regression for HARMONY-1605
        final Font defaultFont = new Font("Dialog", Font.PLAIN, 12); //$NON-NLS-1$
        final Window w = new Window(new Frame());
        final Component c = tc;
        final Button b = new Button();

        assertNull(w.getFont());
        assertNull(c.getFont());
        w.add(c);
        assertNull(c.getFont());
        w.setVisible(true);
        assertEquals(defaultFont, w.getFont());
        assertEquals(defaultFont, c.getFont());

        assertNull(b.getFont());
        b.setVisible(true);
        assertNull(b.getFont());
        b.setVisible(false);
        b.setVisible(true);
        assertNull(b.getFont());
        new Frame().add(b);
        assertNull(b.getFont());
        w.add(b);
        assertEquals(defaultFont, b.getFont());

        w.setVisible(false);
        w.dispose();
        // End of regression for HARMONY-1605
    }

    private void createRobot() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void createFrameAndWait() {
        createRobot();
        assertNull(frame);
        frame = new Frame();
        frame.setBounds(0, 0, 100, 100);
        myTestComp.setBounds(0, 0, 30, 50);
        frame.add(myTestComp);
        waitShowWindow(robot, frame);
    }

    boolean callback(Event evt) {
        synchronized (robot) {
            putEvent(evt.id, evt);
            robot.notify();
        }
        return true;
    }

   public final void testAction() {
       int eventId = Event.ACTION_EVENT;
        myTestComp = new MyButton();
        createFrameAndWait();
        robot.setAutoDelay(700);
        Point screenLoc = myTestComp.getLocationOnScreen();
        int centerX = myTestComp.getWidth() / 2;
        int centerY = myTestComp.getHeight() / 2;
        screenLoc.translate(centerX, centerY);
        final Point absPos = screenLoc;

        waitForEvent(new Runnable() {
            public void run() {
                robot.mouseMove(absPos.x, absPos.y);
                robot.mousePress(InputEvent.BUTTON1_MASK | InputEvent.BUTTON2_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK | InputEvent.BUTTON2_MASK);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        Event actionEvt = new Event(myTestComp, eventId,
                ((Button) myTestComp).getLabel());
        actionEvt.modifiers = Event.ALT_MASK;
        checkOldEvent(actionEvt);
    }
   @SuppressWarnings("deprecation")
    public final void testMouseDownUp() {

        myTestComp = new MyComponent() {
            @Override
            public boolean mouseDown(Event evt, int arg1, int arg2) {
                synchronized(robot) {
                    return callback(evt);
                }
          }

          @Override
        public boolean mouseUp(Event evt, int arg1, int arg2) {
              synchronized(robot) {
                  return callback(evt);
              }
          }
        };
        int eventId = Event.MOUSE_DOWN;
        createFrameAndWait();
        robot.setAutoDelay(500);
        
        Point screenLoc = myTestComp.getLocationOnScreen();
        int centerX = myTestComp.getWidth() / 2, centerY = myTestComp.getHeight() / 2;
        screenLoc.translate(centerX, centerY);
        robot.mouseMove(screenLoc.x, screenLoc.y);
        waitForEvent(new Runnable() {
            public void run() {
                robot.mousePress(InputEvent.BUTTON1_MASK);
            }
        }, eventId, waitTime);
        
        assertNotNull(event);
        Event mouseEvt = new Event(myTestComp, 0l, eventId, centerX, centerY, 0, 0);
        mouseEvt.clickCount = 1;
        checkOldEvent(mouseEvt);

        eventId = Event.MOUSE_UP;
        waitForEvent(new Runnable() {
            public void run() {
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        mouseEvt.clickCount = 0;
        mouseEvt.id = Event.MOUSE_UP;
        checkOldEvent(mouseEvt);

    }
   @SuppressWarnings("deprecation")
    public final void testMouseEnterExit() {

        myTestComp = new MyComponent () {
            @Override
            public boolean mouseEnter(Event evt, int arg1, int arg2) {
                synchronized(robot) {
                    return callback(evt);
                }
            }
            @Override
            public boolean mouseExit(Event evt, int arg1, int arg2) {
                synchronized(robot) {
                    return callback(evt);
                }
            }
        };
        int eventId = Event.MOUSE_ENTER;
        createFrameAndWait();
        robot.setAutoDelay(20);
        
        Point screenLoc = myTestComp.getLocationOnScreen();
        Dimension size = myTestComp.getSize();
        final int outX = screenLoc.x + size.width + 1;

        screenLoc.translate(size.width / 2, size.height / 2);
        final int centerY = screenLoc.y;
        final int centerX = screenLoc.x;
        final float k = centerY / centerX;
        
        //mouse enter:
        waitForEvent(new Runnable() {
            public void run() {
                robot.mouseMove(0, 0);
                for (int x = 0; x <= centerX; x++) {
                    robot.mouseMove(x, (int) (k * x));
                }
            }
        }, eventId, waitTime);

        assertNotNull(event);
        assertEquals(eventId, event.id);
        assertEquals(0, event.modifiers);

        eventId = Event.MOUSE_EXIT;
        waitForEvent(new Runnable() {
            public void run() {
                robot.mousePress(InputEvent.BUTTON3_MASK); //for modifiers test
                for (int x = centerX; x <= outX; x++) {
                    robot.mouseMove(x, centerY);
                }
                robot.mouseRelease(InputEvent.BUTTON3_MASK); //for modifiers test
            }
        }, eventId, waitTime);

        assertNotNull(event);
        assertEquals(eventId, event.id);
        assertEquals(Event.META_MASK, event.modifiers);

    }
    @SuppressWarnings("deprecation")
    public final void testMouseDragMove() {
        int eventId = Event.MOUSE_MOVE;
        myTestComp = new MyComponent() {
            @Override
            public boolean mouseMove(Event evt, int arg1, int arg2) {
                synchronized(robot) {
                    return callback(evt);
                }
            }
            @Override
            public boolean mouseDrag(Event evt, int arg1, int arg2) {
                synchronized(robot) {
                    return callback(evt);
                }
            }

        };
        createFrameAndWait();
        robot.setAutoDelay(300);
        
        Point screenLoc = myTestComp.getLocationOnScreen(), absLoc = new Point();
        absLoc.setLocation(screenLoc);
        Dimension size = myTestComp.getSize();
        int dx = size.width / 3, dy = size.height / 3;
        absLoc.translate(dx , dy);
        final Point pos = absLoc;
        waitForEvent(new Runnable() {
            public void run() {
                    robot.mouseMove(pos.x, pos.y);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        Event mouseEvt = new Event(myTestComp, 0l, eventId, dx, dy, 0, 0);
        checkOldEvent(mouseEvt);
        absLoc.translate(3, 3);
        final Point dragPos = absLoc;
        eventId = Event.MOUSE_DRAG;
        waitForEvent(new Runnable() {
            public void run() {
                    robot.mousePress(InputEvent.BUTTON1_MASK |
                                     InputEvent.BUTTON3_MASK); //for drag test
                    robot.mouseMove(dragPos.x, dragPos.y);
                    robot.mouseRelease(InputEvent.BUTTON1_MASK |
                                       InputEvent.BUTTON3_MASK); //for drag test
            }
        }, eventId, waitTime);
        assertNotNull(event);
        mouseEvt.id = eventId;
        mouseEvt.x = absLoc.x - screenLoc.x;
        mouseEvt.y = absLoc.y - screenLoc.y;
        mouseEvt.modifiers = InputEvent.BUTTON3_MASK;
        checkOldEvent(mouseEvt);

    }

    public final void testGotLostFocus() {
       waitFocus();
        //LOST_FOCUS
        int eventId = Event.LOST_FOCUS;
        waitForEvent(new Runnable() {
            public void run() {
                frame.requestFocus();
            }
        }, eventId, waitTime);
        assertNotNull(event);
        checkOldEvent(new Event(myTestComp, eventId, null));

    }

    public final void testKeyDownUp() {
        waitFocus();
        final int keyCode = KeyEvent.VK_A;
        int oldKey = 'a';
        // KEY_PRESS:
        int eventId = Event.KEY_PRESS;
        waitForEvent(new Runnable() {
            public void run() {
                robot.keyPress(keyCode);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        checkOldEvent(new Event(myTestComp, 0l, eventId,
                0, 0, oldKey, 0));

        // KEY_RELEASE:
        eventId = Event.KEY_RELEASE;
        waitForEvent(new Runnable() {
            public void run() {
                robot.keyRelease(keyCode);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        checkOldEvent(new Event(myTestComp, 0l, eventId,
                0, 0, oldKey, 0));

    }

    private void waitFocus() {
        myTestComp = new MyComponent();
        int eventId = Event.GOT_FOCUS;
        myTestComp.setFocusable(true);
        frame = new Frame();

        myTestComp.setBounds(20, 20, 40, 30);
        frame.setBounds(0, 0, 150, 100);        

        frame.add(myTestComp);
        createRobot();
        
        robot.setAutoDelay(300);
        // GOT_FOCUS:
        waitForEvent(new Runnable() {
            public void run() {
                frame.setVisible(true);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        checkOldEvent(new Event(myTestComp, eventId, null));
    }

    public final void testKeyActionRelease() {
        waitFocus();
        final int keyCode = KeyEvent.VK_PAGE_UP;
        int oldKey = Event.PGUP;
        // KEY_ACTION:
        int eventId = Event.KEY_ACTION;
        waitForEvent(new Runnable() {
            public void run() {
                robot.keyPress(keyCode);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        checkOldEvent(new Event(myTestComp, 0l, eventId,
                0, 0, oldKey, 0));
        // KEY_ACTION_RELEASE:
        eventId = Event.KEY_ACTION_RELEASE;
        waitForEvent(new Runnable() {
            public void run() {
                robot.keyRelease(keyCode);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        checkOldEvent(new Event(myTestComp, 0l, eventId,
                0, 0, oldKey, 0));
    }

    public final void testWindowDestroy() {
        int eventId = Event.WINDOW_DESTROY;
        createRobot();
        robot.setAutoDelay(250);
        assertNull(frame);
        frame = new MyFrame();

        frame.setBounds(0, 0, 100, 100);

        //wait for GOT_FOCUS:
        event = waitForEvent(new Runnable() {
            public void run() {
                frame.setVisible(true);
                frame.toFront();
            }
        },  Event.GOT_FOCUS, waitTime);
        assertNotNull(event);
        assertEquals(Event.GOT_FOCUS, event.id);


        event = waitForEvent(new Runnable() {
            public void run() {
                
//              close window by pressing <ALT> + <F4>
                robot.keyPress(KeyEvent.VK_ALT);
                robot.keyPress(KeyEvent.VK_F4);
                robot.keyRelease(KeyEvent.VK_F4);
                robot.keyRelease(KeyEvent.VK_ALT);

            }
        }, eventId, waitTime);
        assertNotNull(event);
        assertEquals(eventId, event.id);
        checkOldEvent(new Event(frame, eventId, null));

    }

    public final void testWindowIconify() {
        int eventId = Event.WINDOW_ICONIFY;
        createRobot();
        robot.setAutoDelay(250);
        assertNull(frame);
        frame = new MyFrame();

        frame.setBounds(0, 0, 200, 200);
        waitShowWindow(robot, frame);

        event = waitForEvent(new Runnable() {
            public void run() {
                frame.setState(Frame.ICONIFIED);
            }
        }, eventId, waitTime);

        assertNotNull(event);
        checkOldEvent(new Event(frame, eventId, null));
        //must deiconify to become on top of other windows
        eventId = Event.WINDOW_DEICONIFY;

        waitForEvent(new Runnable() {
            public void run() {
                frame.setState(Frame.NORMAL);
            }
        }, eventId, waitTime);
        assertNotNull(event);
        checkOldEvent(new Event(frame, eventId, null));

    }

    public final void testWindowMove() {
        createRobot();
        int eventId =  Event.WINDOW_MOVED;
        robot.setAutoDelay(250);
        assertNull(frame);
        frame = new MyFrame();

        frame.setBounds(0, 0, 200, 200);
        waitShowWindow(robot, frame);
        final int dx = frame.getWidth() / 2;
        final int dy = frame.getInsets().top / 2;
        final int x = frame.getX() + dx;
        final int y = frame.getY() + dy;
        waitForEvent(new Runnable() {
            public void run() {
                robot.mouseMove(x, y);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseMove(x + 15, y + 15);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
            }
        }, eventId, waitTime);

        assertNotNull(event);
        checkOldEvent(new Event(frame, 0l, eventId,
                frame.getX(), frame.getY(), 0, 0));
    }

    /**
     * Make some action(passed in runnable) on the current thread and then wait
     * on a lock until listener is called or a specified amount of time has
     * elapsed
     *
     * @param runnable
     *            method run() of this object will be called first
     * @param eventId
     *            id(type) of event to wait for
     * @param waitTime
     *            the maximum time to wait
     */
    private Event waitForEvent(Runnable runnable, int eventId, int waitTime) {
        int timeout = 100;
        for (int n = 0; n < nRetries; n++) {
            int time = 0;
            event = null;

            putEvent(eventId, null); //listener wasn't called
            runnable.run();
            synchronized (robot) {

                event = null;
                //runnable.run();
                while (time < waitTime) {
                    try {
                        event = getEvent(eventId);
                        if (event != null) {
                            return event;
                        }
                        robot.wait(timeout);
                        time += timeout;

                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        return event;
    }
    
    private void waitShowWindow(Robot robot, Component comp) {
        assertNotNull(robot);
        assertNotNull(comp);
        paintFlag = false;
        comp.setVisible(true);
        while (!paintFlag) {
            Thread.yield();
        }
//        Point center = comp.getLocation();
//        Component parent = comp;
//        //get absolute location before the component is shown on screen
//        while (parent != null && !(parent instanceof Window)) {
//            parent = parent.getParent();
//            Point parentLoc = parent.getLocation();
//            center.translate(parentLoc.x, parentLoc.y);
//        }
//        Dimension size = comp.getSize();
//        if (comp instanceof Window) {
//            Window w = (Window) comp;
//          Insets insets = w.getInsets();
//          center.translate(insets.left, insets.top);
//          size.width -= insets.left + insets.right;
//          size.height -= insets.bottom + insets.top;
//        }
//        center.translate(size.width / 2, size.height / 2);
//        //check pixel color before the window is shown:
//        Color c = robot.getPixelColor(center.x, center.y);
//      Color newColor = new Color((c.getRed() + 16) & 0xff,
//              (c.getGreen() + 16) & 0xff, (c.getBlue() + 16) & 0xff);
//      comp.setBackground(newColor);
//      if (parent instanceof Window) {
//          parent.setVisible(true);
//      }
//      //wait for frame's background to be painted:
//      while (! robot.getPixelColor(center.x, center.y).equals(newColor)) {
//          robot.delay(25);
//      }

    }
    public final void testAddPropertyChangeListener() {
        cleanPropertyFields();

        tc.addPropertyChangeListener(propListener);
        PropertyChangeListener[] propListeners = tc.getPropertyChangeListeners();
        assertEquals(1, propListeners.length);
        assertSame(propListener, propListeners[0]);

        String name = "Test Component";
        tc.setName(name);
        checkPropertyFields("name", tc, name);

        cleanPropertyFields();
        ComponentOrientation orientation = ComponentOrientation.LEFT_TO_RIGHT;
        tc.setComponentOrientation(orientation);
        checkPropertyFields("componentOrientation", tc, orientation);

        Color color = Color.BLUE;
        cleanPropertyFields();
        tc.setBackground(color);
        checkPropertyFields("background", tc, color);

        cleanPropertyFields();
        tc.setFocusable(false);
        checkPropertyFields("focusable", tc, new Boolean(false));

        cleanPropertyFields();
        tc.setFocusTraversalKeysEnabled(false);
        checkPropertyFields("focusTraversalKeysEnabled", tc, new Boolean(false));

        cleanPropertyFields();
        Set<AWTKeyStroke> keys = Collections.emptySet();
        tc.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
                                 keys);
        checkPropertyFields("upCycleFocusTraversalKeys", tc, keys);
        assertNull(oldValue);
        keys = new HashSet<AWTKeyStroke>();
        keys.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.CTRL_DOWN_MASK));
        cleanPropertyFields();
        tc.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
        checkPropertyFields("forwardFocusTraversalKeys", tc, keys);


        cleanPropertyFields();
        keys.clear();
        keys.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_ENTER, 0));
        tc.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);
        checkPropertyFields("backwardFocusTraversalKeys", tc, keys);

        cleanPropertyFields();
        keys.clear();
        keys.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_F12, InputEvent.ALT_DOWN_MASK));
        tc.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, keys);
        checkPropertyFields("upCycleFocusTraversalKeys", tc, keys);

        cleanPropertyFields();
        tc.setFont(null);
        checkPropertyFields("font", tc, null);

        cleanPropertyFields();
        color = Color.RED;
        tc.setForeground(color);
        checkPropertyFields("foreground", tc, color);

        cleanPropertyFields();
        tc.setLocale(null);
        checkPropertyFields("locale", tc, null);

        cleanPropertyFields();
        Dimension size = new Dimension(100, 200);
        tc.setPreferredSize(size);
        checkPropertyFields("preferredSize", tc, size);

        cleanPropertyFields();
        tc.setMinimumSize(size);
        checkPropertyFields("minimumSize", tc, size);

        cleanPropertyFields();
        tc.setMaximumSize(size);
        checkPropertyFields("maximumSize", tc, size);
    }

    private void checkPropertyFields(String propName, Object source, Object newVal) {
        assertTrue(listenerCalled);
        assertEquals(propName, this.propName);
        assertSame(source, src);
        assertEquals(newVal, newValue);
        if (newVal != null) {
            assertFalse(newVal.equals(oldValue));
        }
    }

    private void cleanPropertyFields() {
        listenerCalled = false;
        propName = null;
        oldValue = newValue = src = null;
    }

    private void checkOldEvent(Event evt) {
        assertNotNull(event);
        int id = evt.id;
        assertEquals(id, event.id);
        assertSame(evt.target, event.target);
        //for mouse events fields x, y, modifiers and clickCount(only MousePress) are valid:
        if (id >= Event.MOUSE_DOWN && id <= Event.MOUSE_DRAG) {
                checkOldEventModXY(evt);
                assertEquals(evt.clickCount, id == Event.MOUSE_DOWN ? event.clickCount : 0);

        }
        //only arg and modifiers(for action only) are valid for these events:
        if (id == Event.ACTION_EVENT ||
            id == Event.LIST_SELECT ||
            id == Event.LIST_DESELECT ||
           (id >= Event.SCROLL_LINE_UP && id <= Event.SCROLL_END)) {
            assertSame(evt.arg, event.arg);
            if (id == Event.ACTION_EVENT) {
                assertEquals(evt.modifiers, event.modifiers);
            }
        }
        //x, y, key, modifiers are valid for key events
        if (id >= Event.KEY_PRESS && id <= Event.KEY_ACTION_RELEASE) {
            checkOldEventModXY(evt);
            assertEquals(evt.key, event.key);
        }
        //x, y are valid for WINDOW_MOVED events
        if (id == Event.WINDOW_MOVED) {
            checkOldEventXY(evt);
        }
    }

    private void checkOldEventModXY(Event evt) {
        checkOldEventXY(evt);
        assertEquals(evt.modifiers, event.modifiers);
    }

    private void checkOldEventXY(Event evt) {
        assertEquals(evt.x, event.x);
        assertEquals(evt.y, event.y);
    }

    public final void testFirePropertyChangeStringBooleanBoolean(){
        String propName = "booleanProp";
        tc.addPropertyChangeListener(propName, propListener);
        cleanPropertyFields();
        tc.firePropertyChange(propName, false, true);
        assertTrue(listenerCalled);
        assertEquals(propName, this.propName);
        assertSame(tc, src);
        assertSame(Boolean.TRUE, newValue);
        assertSame(Boolean.FALSE, oldValue);
    }
    @SuppressWarnings("deprecation")
    public final void testBounds() {
        Component comp = new SimpleComponent () {
            @Override
            public Rectangle bounds() {
                methodCalled = true;
                return super.bounds();
            }
        };
        Rectangle r = new Rectangle(10, 20, 30, 40);
        comp.setBounds(r);
        assertFalse(methodCalled);
        Rectangle r1 = comp.getBounds();
        assertTrue(methodCalled);
        assertNotSame(r, r1);
        assertEquals(r, r1);
    }
    @SuppressWarnings("deprecation")
    public final void testDisableEnable() {
        Component comp = new SimpleComponent () {
            @Override
            public void disable() {
                assertTrue(methodCalled);//called from enable(boolean)
                methodCalled = true;
                super.disable();
            }
            @Override
            public void enable(boolean b) {
                assertFalse(methodCalled);
                methodCalled = true;
                super.enable(b);
            }
            @Override
            public void enable() {
                assertTrue(methodCalled);//called from enable(boolean)
                methodCalled = true;
                super.enable();
            }
        };

        assertFalse(methodCalled);
        assertTrue(comp.isEnabled());
        comp.setEnabled(false);
        assertTrue(methodCalled);
        assertFalse(comp.isEnabled());
        methodCalled = false;
        comp.setEnabled(true);
        assertTrue(methodCalled);
        assertTrue(comp.isEnabled());
    }
    @SuppressWarnings("deprecation")
    public final void testHideShow() {
        Component comp = new SimpleComponent () {
            @Override
            public void hide() {
                assertTrue(methodCalled);//called from enable(boolean)
                methodCalled = true;
                super.hide();
            }
            @Override
            public void show(boolean b) {
                assertFalse(methodCalled);
                methodCalled = true;
                super.show(b);
            }
            @Override
            public void show() {
                assertTrue(methodCalled);//called from enable(boolean)
                methodCalled = true;
                super.show();
            }
        };

        assertFalse(methodCalled);
        assertTrue(comp.isVisible());
        comp.setVisible(false);
        assertTrue(methodCalled);
        assertFalse(comp.isVisible());
        methodCalled = false;
        comp.setVisible(true);
        assertTrue(methodCalled);
        assertTrue(comp.isVisible());
    }
    @SuppressWarnings("deprecation")
    public final void testInside() {
        Component comp = new SimpleComponent () {
            @Override
            public boolean inside(int x, int y) {
                methodCalled = true;
                return super.inside(x, y);
            }
        };
        Point p = new Point(-1, -1);
        comp.setBounds(0, 5, 10, 15);
        assertFalse(methodCalled);
        assertFalse(comp.contains(p));
        assertTrue(methodCalled);
        p.setLocation(8, 14);
        methodCalled = false;
        assertTrue(comp.contains(p));
        assertTrue(methodCalled);
    }
    @SuppressWarnings("deprecation")
    public final void testIsFocusTraversable() {
        Component comp = new SimpleComponent () {
            @Override
            public boolean isFocusTraversable() {
                methodCalled = true;
                return super.isFocusTraversable();
            }
        };
        assertFalse(methodCalled);
        assertTrue(comp.isFocusable());
        assertTrue(methodCalled);
    }
    @SuppressWarnings("deprecation")
    public final void testLayout() {
        Component comp = new SimpleComponent () {
            @Override
            public void layout() {
                methodCalled = true;
                super.layout();
            }
        };
        assertFalse(methodCalled);
        comp.doLayout();
        assertTrue(methodCalled);

    }
    @SuppressWarnings("deprecation")
    public final void testLocate() {
        Component comp = new SimpleComponent () {
            @Override
            public Component locate(int x, int y) {
                methodCalled = true;
                return super.locate(x, y);
            }
        };
        comp.setBounds(0, 0, 10, 20);
        Point p = new Point(5, 15);
        assertFalse(methodCalled);
        assertSame(comp, comp.getComponentAt(p));
        assertTrue(methodCalled);
        p.setLocation(100, 100);
        assertNull(comp.getComponentAt(p));

    }
    @SuppressWarnings("deprecation")
    public final void testLocation() {
        Component comp = new SimpleComponent () {
            @Override
            public Point location() {
                methodCalled = true;
                return super.location();
            }
        };

        Point p = new Point(10, 20);
        comp.setLocation(p);
        assertFalse(methodCalled);
        assertEquals(p, comp.getLocation());
        assertTrue(methodCalled);
    }
    @SuppressWarnings("deprecation")
    public final void testMinimumSize() {
        Component comp = new SimpleComponent () {
            @Override
            public Dimension minimumSize() {
                methodCalled = true;
                return super.minimumSize();
            }
        };
        Dimension size = new Dimension();
        assertFalse(methodCalled);
        Dimension minSize = comp.getMinimumSize();
        assertTrue(methodCalled);
        assertEquals(size, minSize);
        size.setSize(10, 15);
        comp.setSize(size);
        minSize = comp.getMinimumSize();
        assertNotSame(size, minSize);
        assertEquals(size, minSize);
    }
    @SuppressWarnings("deprecation")
    public final void testPreferredSize() {
        assertEquals(new Dimension(), tc.getPreferredSize());
        final Dimension minSize = new Dimension(5, 15);
        Component comp = new SimpleComponent () {
            @Override
            public Dimension preferredSize() {
                methodCalled = true;
                return super.preferredSize();
            }
            @Override
            public Dimension minimumSize() {
                return minSize;
            }
        };

        assertFalse(methodCalled);
        Dimension prefSize = comp.getPreferredSize();
        assertTrue(methodCalled);
        assertNotSame(minSize, prefSize);
        assertEquals(minSize, prefSize);

    }
    @SuppressWarnings("deprecation")
    public final void testSize() {
        Component comp = new SimpleComponent () {
            @Override
            public Dimension size() {
                methodCalled = true;
                return super.size();
            }
        };
        Dimension size = new Dimension();
        assertFalse(methodCalled);
        assertEquals(size, comp.getSize());
        assertTrue(methodCalled);
        size.setSize(10, 15);
        comp.setSize(size);
        assertEquals(size, comp.getSize());
    }
    @SuppressWarnings("deprecation")
    public final void testReshape() {
        Component comp = new SimpleComponent () {
            @Override
            public void reshape(int x, int y, int w, int h) {
                methodCalled = true;
                super.reshape(x, y, w, h);
            }
        };
        Rectangle rect = new Rectangle(5, 6, 7, 8);
        assertFalse(methodCalled);
        comp.setBounds(rect);
        assertTrue(methodCalled);
        assertEquals(rect, comp.getBounds());
    }
    @SuppressWarnings("deprecation")
    public final void testMove() {
        Component comp = new MoveResizeComponent () {
            @Override
            public void move(int x, int y) {
                assertFalse(methodCalled);
                assertFalse(setBoundsCalled);
                assertFalse(reshapeCalled);
                methodCalled = true;
                super.move(x, y);
            }
        };
        Point p = new Point(5, 6);
        comp.setLocation(p);
        assertTrue(methodCalled);
        assertTrue(setBoundsCalled);
        assertTrue(reshapeCalled);
        assertEquals(p, comp.getLocation());
    }
    @SuppressWarnings("deprecation")
    public final void testResizeIntInt() {
        Component comp = new MoveResizeComponent () {
            @Override
            public void resize(int x, int y) {
                assertFalse(methodCalled);
                assertFalse(setBoundsCalled);
                assertFalse(reshapeCalled);
                methodCalled = true;
                super.resize(x, y);
            }
        };
        comp.setSize(10, 20);
        assertTrue(methodCalled);
        assertTrue(setBoundsCalled);
        assertTrue(reshapeCalled);
        assertEquals(new Dimension(10, 20), comp.getSize());
    }
    @SuppressWarnings("deprecation")
    public final void testResizeDimension() {
        Component comp = new SimpleComponent () {
            @Override
            public void resize(Dimension size) {
                assertFalse(methodCalled);
                assertFalse(setBoundsCalled);
                assertFalse(reshapeCalled);
                methodCalled = true;
                super.resize(size);
            }
            @Override
            public void resize(int w, int h) {
                assertTrue(methodCalled);
                assertTrue(setBoundsCalled);
                assertFalse(reshapeCalled);
                reshapeCalled = true;
                super.resize(w, h);
            }
            @Override
            public void setSize(int w, int h) {
                assertTrue(methodCalled);
                assertFalse(setBoundsCalled);
                assertFalse(reshapeCalled);
                setBoundsCalled = true;
                super.setSize(w, h);
            }
        };
        Dimension size = new Dimension(10, 20);
        comp.setSize(size);
        assertTrue(methodCalled);
        assertTrue(setBoundsCalled);
        assertTrue(reshapeCalled);
        assertEquals(size, comp.getSize());
    }
    @SuppressWarnings("deprecation")
    public final void testNextFocus() {
        Component comp = new SimpleComponent () {
            @Override
            public void nextFocus() {
                methodCalled = true;
                super.nextFocus();
            }
        };

        assertFalse(methodCalled);
        comp.transferFocus();
        assertTrue(methodCalled);
    }
    @SuppressWarnings("deprecation")
    public final void testGetPeer() {
        frame = new Frame();
        Object peer = frame.getPeer();
        assertNull(peer);
        frame.addNotify();
        peer = frame.getPeer();
        assertNotNull(peer);
        assertTrue(peer instanceof ComponentPeer);
        frame.removeNotify();
        assertNull(peer = frame.getPeer());
    }

    public final void testGetMousePosition() {
        assertNotNull(myTestComp);
        assertNull(myTestComp.getMousePosition());
        createFrameAndWait();
        robot.setAutoDelay(100);
        Component testComp = new SimpleComponent();
        testComp.setBounds(5, 10, 60, 70);
        frame.add(testComp);
        myTestComp.setBounds(10, 20, 30, 40);
        robot.mouseMove(0, 0);
        assertNull(myTestComp.getMousePosition());
        robot.mouseMove(20, 30);
        Point pos = new Point(10, 10);
        assertEquals(pos, myTestComp.getMousePosition());
        assertNull(testComp.getMousePosition());
        robot.mouseMove(45, 65);
        pos = new Point(40, 55);
        assertEquals(pos, testComp.getMousePosition());
        assertNull(myTestComp.getMousePosition());
    }
}


