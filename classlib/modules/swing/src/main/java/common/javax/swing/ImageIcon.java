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

package javax.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.IllegalComponentStateException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.net.URL;
import java.util.Locale;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleIcon;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;

/**
 * <p>
 * <i>ImageIcon</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class ImageIcon implements Icon, Serializable, Accessible {
    private static final long serialVersionUID = -6101950798829449111L;

    protected class AccessibleImageIcon extends AccessibleContext implements AccessibleIcon,
            Serializable {
        private static final long serialVersionUID = -860693743697825660L;
        
        protected AccessibleImageIcon() {
        }

        @Override
        public Accessible getAccessibleChild(int i) {
            return null;
        }

        @Override
        public int getAccessibleChildrenCount() {
            return 0;
        }
        
        @Override
        public Accessible getAccessibleParent() {
            return null;
        }

        @Override
        public int getAccessibleIndexInParent() {
            return -1;
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.ICON;
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            return null;
        }

        @Override
        public Locale getLocale() throws IllegalComponentStateException {
            return null;
        }

        public void setAccessibleIconDescription(String description) {
            setDescription(description);
        }

        public String getAccessibleIconDescription() {
            return getDescription();
        }

        public int getAccessibleIconWidth() {
            return getIconWidth();
        }

        public int getAccessibleIconHeight() {
            return getIconHeight();
        }
    }

    protected static final Component component;

    protected static final MediaTracker tracker;

    private ImageObserver observer;

    private AccessibleContext accessibleContext;

    private Image image;

    private String description;

    /**
     * Unique id is to enable image loading tracking via MediaTracker
     */
    private final int id = getUniqueID();

    /**
     * The last unique ID assigned.
     */
    private static int lastAssignedID;
    static {
        component = new Panel();
        tracker = new MediaTracker(component);
        lastAssignedID = 0;
    }

    /**
     * 'generates' unique ids for icons
     */
    private final static int getUniqueID() {
        return lastAssignedID++;
    }

    public ImageIcon(URL url, String description) {
        this(url);
        this.description = description;
    }

    public ImageIcon(String fileName, String description) {
        this(fileName);
        this.description = description;
    }

    public ImageIcon(Image image, String description) {
        this(image);
        this.description = description;
    }

    public ImageIcon(byte[] imageData, String description) {
        this(imageData);
        this.description = description;
    }

    public ImageIcon(URL url) {
        image = Toolkit.getDefaultToolkit().createImage(url);
        loadImage(image);
        description = url.toString();
    }

    public ImageIcon(String fileName) {
        image = Toolkit.getDefaultToolkit().createImage(fileName);
        loadImage(image);
        description = fileName;
    }

    public ImageIcon(Image image) {
        this.image = image;
        loadImage(image);
        Object comment = image.getProperty("comment", observer);
        if (comment instanceof String) {
            description = (String) comment;
        }
    }

    public ImageIcon(byte[] imageData) {
        image = Toolkit.getDefaultToolkit().createImage(imageData);
        loadImage(image);
        Object comment = image.getProperty("comment", observer);
        if (comment instanceof String) {
            description = (String) comment;
        }
    }

    public ImageIcon() {
    }

    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(image, x, y, (observer != null) ? observer : c);
    }

    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleImageIcon())
                : accessibleContext;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description != null ? description : super.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setImageObserver(ImageObserver observer) {
        this.observer = observer;
    }

    public ImageObserver getImageObserver() {
        return observer;
    }

    public void setImage(Image newImage) {
        tracker.removeImage(image);
        image = newImage;
        tracker.addImage(image, id);
    }

    protected void loadImage(Image image) {
        tracker.addImage(image, id);
        try {
            tracker.waitForID(id);
        } catch (InterruptedException e) {
            // reset the interrupt state for the current thread
            Thread.currentThread().interrupt();
        }
    }

    public Image getImage() {
        return image;
    }

    public int getImageLoadStatus() {
        return tracker.statusID(id, false);
    }

    public int getIconWidth() {
        return (image != null) ? image.getWidth(observer) : -1;
    }

    public int getIconHeight() {
        return (image != null) ? image.getHeight(observer) : -1;
    }
}
