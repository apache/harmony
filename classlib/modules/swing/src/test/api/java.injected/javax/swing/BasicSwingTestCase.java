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

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;

import junit.framework.TestCase;

public abstract class BasicSwingTestCase extends TestCase {
    public static final long DEFAULT_TIMEOUT_DELAY = 10000;

    private boolean ignoreNotImplemented;
    private Robot robot;
    private LookAndFeel previousLF;

    private static long defaultTimeoutDelay;
    protected static long timeoutDelay;

    static {
        String timeoutProp = System.getProperty("test.timeout");
        if (timeoutProp == null || timeoutProp.length() == 0) {
            defaultTimeoutDelay = DEFAULT_TIMEOUT_DELAY;
        } else {
            defaultTimeoutDelay = Integer.parseInt(timeoutProp);
        }
    }

    @Override
    protected void setUp() throws Exception {
        timeoutDelay = defaultTimeoutDelay;
        previousLF = UIManager.getLookAndFeel();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        UIManager.getDefaults().clear();
        if (previousLF != null) {
            UIManager.setLookAndFeel(previousLF);
        } else {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }
        JFrame.setDefaultLookAndFeelDecorated(false);
        JDialog.setDefaultLookAndFeelDecorated(false);
        closeAllFrames();
    }

    public static void assertEquals(final Object[] expected, final Object[] actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            fail("Arrays are not same: one of them is null");
        }
        assertEquals("Arrays of different types", expected.getClass(), actual.getClass());
        assertEquals("Arrays of different lengths", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Arrays content at index " + i + " is not same", expected[i], actual[i]);
        }
    }



    public BasicSwingTestCase() {
    }

    public BasicSwingTestCase(final String name) {
        super(name);
    }

    protected void runBareSuper() throws Throwable {
        super.runBare();
    }

    protected Throwable runBareImpl() throws Throwable {
        try {
            runBareSuper();
        } catch (Throwable e) {
            return e;
        }

        return null;
    }

    @Override
    public void runBare() throws Throwable {
        final Throwable[] exception = new Throwable[1];
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    exception[0] = runBareImpl();
                } catch (Throwable e) {
                }
            }
        });
        thread.start();
        thread.join(timeoutDelay);
        int interruptAttempts = 0;
        while (thread.isAlive()){
            thread.interrupt();
            if (interruptAttempts++ > 5) {
                fail("Test interrupted due timeout");
            }
        }
        if (exception[0] != null) {
            rethrow(exception[0]);
        }
    }

    public void setIgnoreNotImplemented(final boolean b) {
        ignoreNotImplemented = b;
    }

    protected static class Marker {
        private final boolean autoreset;

        private boolean occured;
        private Object auxiliary;

        public Marker() {
            this(false);
        }

        /**
         * If autoreset is turned on, then the call to <code>isOccurred()</code>
         * resets the state just before returning the value.
         *
         * @param autoreset enables the autoreset feature
         */
        public Marker(final boolean autoreset) {
            this.autoreset = autoreset;
        }

        public void setOccurred() {
            setOccurred(true);
        }

        public void setOccurred(final boolean occured) {
            this.occured = occured;
        }

        /**
         * Returns the state of this marker. If autoreset feature is enabled,
         * <code>reset()</code> method will be called before return. Beware that
         * any auxiliary data will also be reset, if the feature is enabled.
         *
         * @return true if an event occurred
         */
        public boolean isOccurred() {
            boolean result = occured;
            if (autoreset) {
                reset();
            }
            return result;
        }

        public void setAuxiliary(final Object aux) {
            auxiliary = aux;
        }

        public Object getAuxiliary() {
            return auxiliary;
        }

        public void reset() {
            occured = false;
            auxiliary = null;
        }

        public boolean isAutoreset() {
            return autoreset;
        }
    }

    private class FixedFontMetrics extends FontMetrics {
        private static final long serialVersionUID = 1L;
        private final int charWidth;
        private final int charHeight;

        public FixedFontMetrics(final Font fnt, final int charWidth) {
            super(fnt);
            this.charWidth = charWidth;
            charHeight = super.getHeight();
        }

        public FixedFontMetrics(final Font fnt, final int charWidth, final int charHeight) {
            super(fnt);
            this.charWidth = charWidth;
            this.charHeight = charHeight;
        }

        @Override
        public int stringWidth(final String str) {
            return ((str == null || str.equals("")) ? 0 : str.length() * charWidth);
        }

        @Override
        public int charWidth(final int ch) {
            return charWidth;
        }

        @Override
        public int charWidth(final char ch) {
            return charWidth;
        }

        @Override
        public int getAscent() {
            return charWidth;
        }

        @Override
        public int getDescent() {
            return charWidth*2;
        }

        @Override
        public int getLeading() {
            return 2;
        }

        @Override
        public int getHeight() {
            return charHeight;
        }
    };

    protected abstract class ExceptionalCase {
        private Class<?> clazz;
        private String msg;

        public abstract void exceptionalAction() throws Exception;

        public ExceptionalCase() {
        }

        public ExceptionalCase(String msg, Class<?> clazz) {
            this.msg = msg;
            this.clazz = clazz;
        }

        public Class<?> expectedExceptionClass() {
            return clazz;
        }

        public String expectedExceptionMessage() {
            return msg;
        }
    }

    protected abstract class BadLocationCase extends ExceptionalCase {
        @Override
        public Class<?> expectedExceptionClass() {
            return BadLocationException.class;
        }
    }

    protected abstract class ClassCastCase extends ExceptionalCase {
        @Override
        public Class<?> expectedExceptionClass() {
            return ClassCastException.class;
        }
    }

    protected abstract class IllegalArgumentCase extends ExceptionalCase {
        @Override
        public Class<?> expectedExceptionClass() {
            return IllegalArgumentException.class;
        }
    }

    protected abstract class NullPointerCase extends ExceptionalCase {
        @Override
        public Class<?> expectedExceptionClass() {
            return NullPointerException.class;
        }
    }

    protected abstract class StringIndexOutOfBoundsCase extends ExceptionalCase {
        @Override
        public Class<?> expectedExceptionClass() {
            return StringIndexOutOfBoundsException.class;
        }
    }

    protected static class EventsController implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean isVerbose;
        private Object lastEventHappened;
        private final Map<Object, Object> events = new HashMap<Object, Object>();

        protected EventsController() {
            this(false);
        }

        protected EventsController(final boolean verbose) {
            isVerbose = verbose;
        }

        public void setVerbose(final boolean verbose) {
            isVerbose = verbose;
        }

        protected boolean isVerbose() {
            return isVerbose;
        }

        public void reset() {
            lastEventHappened = null;
            events.clear();
        }

        public boolean isChanged() {
            return events.size() != 0;
        }

        public boolean isChanged(final Object key) {
            if (!isChanged() || key == null) {
                return false;
            }
            for (Iterator<Object> it = events.keySet().iterator(); it.hasNext(); ) {
                String property = (String)it.next();
                if (key.equals(property)) {
                    return true;
                }
            }

            return false;
        }

        protected void addEvent(final Object key, final Object event) {
            events.put(key, event);
            lastEventHappened = event;
        }

        public Object getEvent(final Object key) {
            return events.get(key);
        }

        public Object getLastEvent() {
            return lastEventHappened;
        }

        public int getNumEvents() {
            return events.size();
        }

        public int findMe(final Object[] listenersArray) {
            int found = 0;
            for (Object element : listenersArray) {
                if (element == this) {
                    found++;
                }
            }

            return found;
        }
    }

    protected class PropertyChangeController extends EventsController implements PropertyChangeListener {
        private static final long serialVersionUID = 1L;
        public PropertyChangeController() {
            super(false);
        }

        public PropertyChangeController(final boolean verbose) {
            super(verbose);
        }

        public void propertyChange(final PropertyChangeEvent e) {
            addEvent(e.getPropertyName(), e);
            if (isVerbose()) {
                System.out.println("Changed property " + e.getPropertyName() + ", old value = '" + e.getOldValue() + "', new value = '" + e.getNewValue() + "'");
            }
        }

        public void checkLastPropertyFired(final Object source, final String propertyName,
                                        final Object oldValue, final Object newValue) {
            PropertyChangeEvent lastEvent = (PropertyChangeEvent)getLastEvent();
            checkEvent(lastEvent, source, propertyName, oldValue, newValue);
        }

        public void checkPropertyFired(final Object source, final String propertyName,
                                       final Object oldValue, final Object newValue) {
            PropertyChangeEvent e = (PropertyChangeEvent)getEvent(propertyName);
            checkEvent(e, source, propertyName, oldValue, newValue);
        }

        private void checkEvent(PropertyChangeEvent event, final Object source,
                               final String propertyName,
                               final Object oldValue, final Object newValue) {
               assertNotNull("event's been fired ", event);
               assertEquals("event's property name ", propertyName, event.getPropertyName());
               assertEquals("event's oldValue ", oldValue, event.getOldValue());
               assertEquals("event's newValue ", newValue, event.getNewValue());
               assertSame("event's source ", source, event.getSource());
           }
    }

    protected class ChangeController extends EventsController implements ChangeListener {
        private static final long serialVersionUID = 1L;

        public ChangeController() {
            super(false);
        }

        public ChangeController(final boolean verbose) {
            super(verbose);
        }

        public void stateChanged(ChangeEvent e) {
            addEvent(new Integer(getNumEvents()), e);
            if (isVerbose()) {
                System.out.println("Changed");
            }
        }

        public ChangeEvent getEvent() {
            return (ChangeEvent)getLastEvent();
        }
    }

    protected interface TraverseAction {
        void componentTraversed(Component c);
    }


    /**
     * PropertyChangeListener for test purposes to easily check that some property was changed
     */
    protected PropertyChangeController propertyChangeController;

    public static boolean isHarmony() {
        return Toolkit.getDefaultToolkit().getClass().getName().equals("java.awt.ToolkitImpl");
    }

    /**
     * Serializes an object and returns the deserialized version of this object.
     *
     * @param objectToWrite object to serialize
     * @return the deserialized object if successful
     * @throws IOException if write or read operation throws this exception.
     * @throws ClassNotFoundException if object cannot be read
     *                                from an input stream
     */
    public static Object serializeObject(final Object objectToWrite)
        throws IOException, ClassNotFoundException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(output);
        oos.writeObject(objectToWrite);
        oos.close();
        output.close();

        InputStream input = new ByteArrayInputStream(output.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(input);
        Object result = ois.readObject();
        ois.close();
        input.close();

        return result;
    }

    protected Graphics createTestGraphics() {
        return new BufferedImage(300, 300, BufferedImage.TYPE_3BYTE_BGR).createGraphics();
    }

    protected FontMetrics getFontMetrics(final Font fnt) {
        return new FixedFontMetrics(fnt, fnt.getSize());
    }

    protected FontMetrics getFontMetrics(final Font fnt, final int charWidth) {
        return new FixedFontMetrics(fnt, charWidth);
    }

    protected FontMetrics getFontMetrics(final Font fnt, final int charWidth, final int charHeight) {
        return new FixedFontMetrics(fnt, charWidth, charHeight);
    }

    protected void testExceptionalCase(final ExceptionalCase ec) {
        try {
            ec.exceptionalAction();
            fail("Exceptional case was not detected!");
        } catch (final Exception e) {
            if (ec.expectedExceptionClass() != null) {
                if (!ec.expectedExceptionClass().isAssignableFrom(e.getClass())) {
                    fail("Exception of wrong type " + e.getClass() + " is produced!");
                }
            }
            if (ec.expectedExceptionMessage() != null) {
                assertEquals("Wrong exception message", ec.expectedExceptionMessage(),
                             e.getMessage());
            }
        }
    }

    protected void traverseComponentTree(final Component root, final TraverseAction action) {
        action.componentTraversed(root);
        if (root instanceof Container) {
            for (int i = 0; i < ((Container)root).getComponentCount(); i++) {
                traverseComponentTree(((Container)root).getComponent(i), action);
            }
        }
    }

    protected void rethrow(final Throwable exception) throws Throwable {
        String msg = exception.getMessage();
        if (!ignoreNotImplemented || msg == null
                || exception.getMessage().indexOf("implemented") == -1) {
            throw exception;
        }
    }

    protected void waitForIdle() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            getRobot().waitForIdle();
        } else {
            SwingUtilities.invokeAndWait(new Thread());
        }
    }

    protected boolean hasListener(final EventListener[] listeners, final Class<?> listenerClass) {
        return getListener(listeners, listenerClass) != null;
    }

    protected EventListener getListener(final EventListener[] listeners, final Class<?> listenerClass) {
        for (EventListener element : listeners) {
            if (element.getClass().isAssignableFrom(listenerClass)) {
                return element;
            }
        }

        return null;
    }

    protected boolean waitForFocus(final Component c) throws Exception {
        int counter = 0;
        while(true) {
            if (isFocusOwner(c)) {
                return true;
            }
            Thread.sleep(100);
            counter++;
            if (counter > 10) {
                return false;
            }
        }
    }

    protected boolean isSystemWindow(final Frame f) {
        return "JUnit".equals(f.getTitle());
    }

    protected Component findComponent(final Container root, final Class<?> findClass, final boolean exactClassMatch) {
        if (exactClassMatch && findClass == root.getClass()
            || !exactClassMatch && findClass.isAssignableFrom(root.getClass())) {

            return root;
        }

        for (int i = 0; i < root.getComponentCount(); i++) {
            Component child = root.getComponent(i);
            if (child instanceof Container) {
                child = findComponent((Container)child, findClass, exactClassMatch);
                if (child != null) {
                    return child;
                }
            } else if (exactClassMatch && findClass == child.getClass()
                       || !exactClassMatch && findClass.isAssignableFrom(child.getClass())) {

                return child;
            }
        }

        return null;
    }

    private boolean isFocusOwner(final Component c) throws Exception {
        final Marker result = new Marker();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                result.setOccurred(c.isFocusOwner());
            }
        });

        return result.isOccurred();
    }

    private Robot getRobot() throws Exception {
        if (robot == null) {
            robot = new Robot();
        }

        return robot;
    }

    private void closeAllFrames() {
        Frame[] frames = Frame.getFrames();
        for (Frame f : frames) {
            if (f.isDisplayable()) {
                if (!isSystemWindow(f)) {
                    f.dispose();
                }
            }
        }
    }
}
