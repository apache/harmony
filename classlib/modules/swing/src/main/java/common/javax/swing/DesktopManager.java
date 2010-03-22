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
 * @author Vadim L. Bogdanov
 */

package javax.swing;

/**
 * <code>DesktopManager</code> objects are owned by a <code>JDesktopPane</code>
 * object. They implement L&F specific behavior for <code>JDesktopPane</code>.
 *
 */
public interface DesktopManager {

    /**
     * Displays the internal frame, if it's possible. This method normally
     * is not called.
     *
     * @param f the internal frame to open
     */
    void openFrame(final JInternalFrame f);

    /**
     * Restore size and location of the internal frame to its size
     * and location before maximizing.
     *
     * @param f the internal frame to minimize
     */
    void minimizeFrame(final JInternalFrame f);

    /**
     * Maximizes the internal frame (resize it to match its parent's bounds).
     *
     * @param f the internal frame to maximize
     */
    void maximizeFrame(final JInternalFrame f);

    /**
     * Removes the internal frame from its parent and adds its desktop icon
     * instead.
     *
     * @param f the internal frame to iconify
     */
    void iconifyFrame(final JInternalFrame f);

    /**
     * Removes the internal frame's desktop icon from its parent and add
     * the internal frame. Size and location of the internal frame are
     * restored to those before iconification.
     *
     * @param f the internal frame to deiconify
     */
    void deiconifyFrame(final JInternalFrame f);

    /**
     * Closes the internal frame, i.e. removes it from its parent.
     *
     * @param f the internal frame to close
     */
    void closeFrame(final JInternalFrame f);

    /**
     * Sets focus to the internal frame. This method is usually called
     * after setting JInternalFrame's <code>IS_SELECTED_PROPERTY</code>
     * property to <code>true</code>.
     *
     * @param f the internal frame to activate
     */
    void activateFrame(final JInternalFrame f);

    /**
     * Removes focus from the internal frame. This method is usually called
     * after setting JInternalFrame's <code>IS_SELECTED_PROPERTY</code>
     * property to <code>false</code>.
     *
     * @param f the internal frame to deactivate
     */
    void deactivateFrame(final JInternalFrame f);

    /**
     * Primitive method to reshape.
     *
     * @param f the component to set bounds to
     * @param x the new horizontal position of the component measured from
     *          the left corner of its container
     * @param y the new vertical position of the component measured from
     *          the upper corner of its container
     * @param width the new width of the component
     * @param height the new height of the component
     */
    void setBoundsForFrame(final JComponent f, final int x, final int y,
                           final int width, final int height);

    /**
     * This method is called when the user begins to resize the frame.
     * <code>f</code> is normally <code>JInternalFrame</code>.
     *
     * @param f the component to resize
     * @param direction direction of resizing
     */
    void beginResizingFrame(final JComponent f, final int direction);

    /**
     * Resizes the component. Call of this method is preceded by call of
     * <code>beginResizingFrame()</code>.
     * <code>f</code> is normally <code>JInternalFrame</code>.
     *
     * @param f the component to resize
     * @param x the new horizontal position of the component measured from
     *          the left corner of its container
     * @param y the new vertical position of the component measured from
     *          the upper corner of its container
     * @param width the new width of the component
     * @param height the new height of the component
     */
    void resizeFrame(final JComponent f, final int x, final int y,
                     final int width, final int height);

    /**
     * This method is called when the users has finished resizing the frame.
     * <code>f</code> is normally <code>JInternalFrame</code>.
     *
     * @param f the component to resize
     */
    void endResizingFrame(final JComponent f);

    /**
     * This method is called when the user begins to move the frame.
     * <code>f</code> is normally <code>JInternalFrame</code>.
     *
     * @param f the moved component
     */

    void beginDraggingFrame(final JComponent f);

    /**
     * The frame has been moved. Call of this method is preceded by call of
     * <code>beginDraggingFrame()</code>.
     * <code>f</code> is normally <code>JInternalFrame</code>.
     *
     * @param f the moved component
     * @param x the new x position
     * @param y the new y position
     */
    void dragFrame(final JComponent f, final int x, final int y);

    /**
     * This method is called when the users has finished moving the frame.
     * <code>f</code> is normally <code>JInternalFrame</code>.
     *
     * @param f the moved component
     */
    void endDraggingFrame(final JComponent f);
}
