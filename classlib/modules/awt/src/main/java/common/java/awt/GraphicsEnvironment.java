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
 * @author Oleg V. Khaschansky
 */

package java.awt;

import java.awt.image.BufferedImage;
import java.util.Locale;

import org.apache.harmony.awt.ContextStorage;
import org.apache.harmony.awt.gl.CommonGraphics2DFactory;


public abstract class GraphicsEnvironment {

    static Boolean isHeadless;

    protected GraphicsEnvironment() {}

    public static GraphicsEnvironment getLocalGraphicsEnvironment() {
        synchronized(ContextStorage.getContextLock()) {
            if (ContextStorage.getGraphicsEnvironment() == null) {
                if (isHeadless()) {
                    ContextStorage.setGraphicsEnvironment(new HeadlessGraphicsEnvironment());
                } else {
                    final CommonGraphics2DFactory g2df =
                        (CommonGraphics2DFactory) Toolkit.getDefaultToolkit().getGraphicsFactory();

                    ContextStorage.setGraphicsEnvironment(
                            g2df.createGraphicsEnvironment(ContextStorage.getWindowFactory())
                    );
                }
            }

            return ContextStorage.getGraphicsEnvironment();
        }
    }

    public boolean isHeadlessInstance() {
        return false;
    }

    public static boolean isHeadless() {
        if (isHeadless == null) {
            isHeadless = Boolean.valueOf(org.apache.harmony.awt.Utils.getSystemProperty("java.awt.headless")); //$NON-NLS-1$ 
        }

        return isHeadless.booleanValue();
    }

    public Rectangle getMaximumWindowBounds() throws HeadlessException {
        return getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }

    public Point getCenterPoint() throws HeadlessException {
        final Rectangle mwb = getMaximumWindowBounds();
        return new Point(mwb.width >> 1, mwb.height >> 1);
    }

    public void preferLocaleFonts() {
        // Note: API specification says following:
        // "The actual change in font rendering behavior resulting
        // from a call to this method is implementation dependent;
        // it may have no effect at all." So, doing nothing is an
        // acceptable behavior for this method.

        // For now FontManager uses 1.4 font.properties scheme for font mapping, so
        // this method doesn't make any sense. The implementation of this method
        // which will influence font mapping is postponed until
        // 1.5 mapping scheme not implemented.

        // todo - Implement non-default behavior with 1.5 font mapping scheme
    }

    public void preferProportionalFonts() {
        // Note: API specification says following:
        // "The actual change in font rendering behavior resulting
        // from a call to this method is implementation dependent;
        // it may have no effect at all." So, doing nothing is an
        // acceptable behavior for this method.

        // For now FontManager uses 1.4 font.properties scheme for font mapping, so
        // this method doesn't make any sense. The implementation of this method
        // which will influence font mapping is postponed until
        // 1.5 mapping scheme not implemented.

        // todo - Implement non-default behavior with 1.5 font mapping scheme
    }

    public abstract Graphics2D createGraphics(BufferedImage bufferedImage);

    public abstract Font[] getAllFonts();

    public abstract String[] getAvailableFontFamilyNames();

    public abstract String[] getAvailableFontFamilyNames(Locale locale);

    public abstract GraphicsDevice getDefaultScreenDevice() throws HeadlessException;

    public abstract GraphicsDevice[] getScreenDevices() throws HeadlessException;
}
