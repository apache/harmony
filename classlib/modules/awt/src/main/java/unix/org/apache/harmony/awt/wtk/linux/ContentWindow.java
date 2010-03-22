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
 * @author Dmitry Durnev
 */
package org.apache.harmony.awt.wtk.linux;

import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;

import org.apache.harmony.awt.wtk.CreationParams;


/**
 * This is a child "content" window of a
 * decorated window(Frame or Dialog).Position
 * of such a window is the same as position of
 * WM frame: when insets change it is translated
 * on insets.
 */
public class ContentWindow extends LinuxWindow {

    private final LinuxWindow frame;
    /**
     * @param factory
     * @param p
     */
    public ContentWindow(LinuxWindowFactory factory,
            CreationParams p) {
        super(factory, p);
        frame = (LinuxWindow) factory.getWindowById(p.parentId);
        super.setTitle("Content Window"); //$NON-NLS-1$
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setTitle(java.lang.String)
     */
    public void setTitle(String title) {
        frame.setTitle(title);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setBounds(int, int, int, int, int)
     */
    public void setBounds(int x, int y, int w, int h, int boundsMask) {
        Insets ins = frame.getInsets();
        frame.setBounds(x/* + ins.left*/, y /*+ ins.top*/,
                w - (ins.left + ins.right),
                h - (ins.top + ins.bottom), boundsMask);
    }
    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setIconImage(java.awt.Image)
     */
    public void setIconImage(Image image) {
        frame.setIconImage(image);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setState(int)
     */
    public void setState(int state) {
        frame.setState(state);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setVisible(boolean)
     */
    public void setVisible(boolean v) {
        super.setVisible(v);
        frame.setVisible(v);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setEnabled(boolean)
     */
    public void setEnabled(boolean value) {
        frame.setEnabled(value);
    }

    /**
     * @see org.apache.harmony.awt.wtk.linux.LinuxWindow#setInputAllowed(boolean)
     */
    void setInputAllowed(boolean value) {
        frame.setInputAllowed(value);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#getInsets()
     */
    public Insets getInsets() {
        return frame.getInsets();
    }

    /**
     * @see org.apache.harmony.awt.wtk.linux.LinuxWindow#getCurrentState()
     */
    int getCurrentState() {
        return frame.getCurrentState();
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#dispose()
     */
    public void dispose() {
        super.dispose();
        frame.dispose();
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#toBack()
     */
    public void toBack() {
        frame.toBack();
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#toFront()
     */
    public void toFront() {
        frame.toFront();
    }

    public void setAlwaysOnTop(boolean value) {
        frame.setAlwaysOnTop(value);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setMaximizedBounds(java.awt.Rectangle)
     */
    public void setMaximizedBounds(Rectangle bounds) {
        frame.setMaximizedBounds(bounds);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setResizable(boolean)
     */
    public void setResizable(boolean value) {
        frame.setResizable(value);
    }

    /**
     * @return Returns the frame.
     */
    LinuxWindow getFrame() {
        return frame;
    }
}
