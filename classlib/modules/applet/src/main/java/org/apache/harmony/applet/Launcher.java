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
 * @author Pavel Dolgov
 */
package org.apache.harmony.applet;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Standalone applet launcher
 */
public final class Launcher implements Callback {
    
    private final Factory factory;
    
    private final Frame frame;
    private final Panel placeholder;
    private final Label status;
    
    private static final int appletId = 1;
    private static final int documentId = 2;
    
    private URL codeBase;
    private String className;
    
    private Launcher() {
        frame = createFrame();
        placeholder = (Panel)frame.getComponent(1);
        status = (Label)((Container)frame.getComponent(2)).getComponent(0);

        factory = new Factory(this);
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Two parameters required: <code_base_url> <class_name>");
            return;
        }
        
        Launcher launcher = new Launcher();
        try {
            launcher.show(new URL(args[0]), args[1]);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void showDocument(int documentId, URL url, String target) {
        System.err.println("showDocument " + url + " " + target);
    }

    public void showStatus(int documentId, String status) {
        this.status.setText(status);
    }

    public void appletResize(int appletId, int width, int height) {
        if (appletId != this.appletId) {
            return;
        }
        
        int dw = width - placeholder.getWidth();
        int dh = height - placeholder.getHeight();
        
        frame.setSize(frame.getWidth() + dw, frame.getHeight() + dh);
        frame.invalidate();
        frame.validate();
    }
    
    private Frame createFrame() {
        Frame f = new Frame("Applet viewer");
        f.setSize(500, 400);
        f.setLayout(new ThreeTierLayout());
        f.setBackground(SystemColor.control);
        
        Panel panel = new Panel();
        f.add(panel, "north");
        
        Button button = new Button(" Start ");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                factory.start(appletId);
            }});
        panel.add(button);
        
        button = new Button(" Stop ");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                factory.stop(appletId);
            }});
        panel.add(button);
        
        button = new Button(" Restart ");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                factory.stop(appletId);
                factory.destroy(appletId);
                factory.init(appletId);
                factory.start(appletId);
            }});
        panel.add(button);
        
        button = new Button(" Reload ");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Launcher.this.hide();
                Launcher.this.show();
            }});
        panel.add(button);
        
        button = new Button(" Exit ");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Launcher.this.hide();
                frame.dispose();
            }});
        panel.add(button);
        
        Panel place = new Panel();
        place.setName("placeholder");
        place.setLayout(new BorderLayout());
        place.setVisible(false);
        f.add(place, "center");
        
        Label status = new Label("status");
        status.setName("status");
        
        Container statusBar = new SunkenBar(3, 3);
        statusBar.add(status);
        f.add(statusBar, "south");
        
        
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Launcher.this.hide();
                e.getWindow().dispose();
            }
        });

        return f;
    }
    
    
    void show(URL codeBase, String className) {
        this.codeBase = codeBase;
        this.className = className;
        show();
    }
    
    private void show() {
        
        URL documentBase = null;
        
        try {
            documentBase = new URL(codeBase.toString() + className + ".html");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        
        Parameters params = 
            new Parameters(appletId,
                           0,
                           documentBase,
                           documentId,
                           codeBase,
                           className,
                           new HashMap<String, String>(),
                           className,
                           placeholder); 
        
        frame.setVisible(true);
        factory.createAndRun(params);
    }
    
    void hide() {
        factory.stop(appletId);
        factory.destroy(appletId);
        factory.dispose(appletId);
    }
    
    private static class SunkenBar extends Container {
        
        private static final long serialVersionUID = -8850392912011177434L;

        private final int offset;
        private final int margin;

        SunkenBar(int offset, int margin) {
            this.offset = offset;
            this.margin = margin;
        }
        
        @Override
        public void paint(Graphics g) {

            super.paint(g);

            int w = getWidth(), h = getHeight();
            
            g.setColor(SystemColor.controlShadow);
            g.drawLine(offset, offset, w - offset, offset);
            g.drawLine(offset, offset, offset, h - offset);

            g.setColor(SystemColor.controlHighlight);
            g.drawLine(offset, h - offset, w - offset, h - offset);
            g.drawLine(w - offset, offset, w - offset, h - offset);
        }

        @SuppressWarnings("deprecation")
        @Deprecated
        @Override
        public void layout() {
            Component child = getComponent(0);

            child.setBounds(offset + margin, offset+1, getWidth() - 2*(offset + margin), getHeight() - 2*offset-1);
        }
        
        @Override
        public Dimension getPreferredSize() {
            Component child = getComponent(0);
            Dimension size = child.getPreferredSize();
            size.width += 2*(offset+margin);
            return size;
        }
    }

    private static class ThreeTierLayout implements LayoutManager { 
        private final Component items[] = new Component[3];

        public void addLayoutComponent(String name, Component comp) {
            if (name.equals("north")) {
                items[0] = comp;
            } else if (name.equals("center")) {
                items[1] = comp;
            } else if (name.equals("south")) {
                items[2] = comp;
            } 
        }

        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int width = parent.getWidth() - insets.left - insets.right;
            int height = parent.getHeight()  - insets.top - insets.bottom;

            int h0 = items[0].getPreferredSize().height;
            items[0].setBounds(insets.left, insets.top, width, h0);
            
            int h2 = items[2].getPreferredSize().height;
            items[2].setBounds(insets.left, insets.top + height - h2, width, h2);
            
            int h1 = height - h0 - h2; 
            items[1].setBounds(insets.left, insets.top + h0, width, h1);
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension result = new Dimension();
            for (int i=0; i<3; i++) {
                if (items[i] == null) {
                    continue;
                }
                Dimension size = items[i].getMinimumSize();
                if (result.width < size.width) {
                    result.width = size.width;
                }
                result.height += size.height;
            }
            return result;
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension result = new Dimension();
            for (int i=0; i<3; i++) {
                if (items[i] == null) {
                    continue;
                }
                Dimension size = items[i].getPreferredSize();
                if (result.width < size.width) {
                    result.width = size.width;
                }
                result.height += size.height;
            }
            return result;
        }

        public void removeLayoutComponent(Component comp) {
            for (int i=0; i<3; i++) {
                if (items[i] == comp) {
                    items[i] = null;
                }
            }
        }
        
    }
}
