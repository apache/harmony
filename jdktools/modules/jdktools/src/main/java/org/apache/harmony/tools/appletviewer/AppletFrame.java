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

package org.apache.harmony.tools.appletviewer;

import java.applet.Applet;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

import java.util.Enumeration;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

class AppletFrame extends JFrame {
    private final Applet applet;
    private final JLabel statusLabel;
    JPanel appletPanel;
    
    private static ShutdownHandler shutdownHandler = new ShutdownHandler();
    
    public AppletFrame(AppletInfo appletInfo) throws Exception {
        // Load applet class
        applet = ViewerAppletContext.loadApplet(appletInfo);

        applet.setPreferredSize(new Dimension(appletInfo.getWidth(), appletInfo.getHeight()));
       

        shutdownHandler.addFrame(this);
        
        // Create menu bar
        setJMenuBar(createMenu());
        
        // Create applet pane
        setLayout(new BorderLayout());
        appletPanel = new JPanel();
        appletPanel.setLayout(new BorderLayout());
        appletPanel.add(applet, BorderLayout.CENTER);
        add(appletPanel, BorderLayout.CENTER);
        
        // Create status pane
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setMinimumSize(new Dimension(100, 15));
        panel.setPreferredSize(new Dimension(100, 15));
        statusLabel = new JLabel();
        statusLabel.setMinimumSize(new Dimension(100, 15));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(statusLabel, BorderLayout.WEST);
        add(panel, BorderLayout.SOUTH);
        appletInfo.setStatusLabel(statusLabel);

        addComponentListener(new ComponentAdapter(){
            public void componentResized(ComponentEvent e){
                if(e.getComponent() == AppletFrame.this){
                    applet.setPreferredSize(new Dimension(appletPanel.getWidth(), appletPanel.getHeight()));
                }                
            }
        });

        // Start applet and make frame visible
        // Init should be called after pack to make components displayable
        pack();
        applet.init();
        setVisible(true);       
        applet.start();
    }
    
    private JMenuBar createMenu() {
    	JMenuBar menuBar = new JMenuBar();
    	
    	// Create Control menu
    	JMenu controlMenu = new JMenu("Control");
    	controlMenu.add(new JMenuItem(new StartAction()));
    	controlMenu.add(new JMenuItem(new StopAction()));
    	controlMenu.add(new JSeparator());
    	controlMenu.add(new JMenuItem(new PropertiesAction()));
    	controlMenu.add(new JSeparator());
    	controlMenu.add(new JMenuItem(new CloseAction()));
    	controlMenu.add(new JMenuItem(new ExitAction()));
    	
    	menuBar.add(controlMenu);
    	
    	return menuBar;
    }

    Applet getApplet(){
        return applet;
    }
    
    private class StartAction extends  AbstractAction {
    	public StartAction() {
            super("Start");
    	}
    	
        public void actionPerformed(final ActionEvent e) {
            applet.start();
            applet.setEnabled(true);
        }
    }
    
    private class StopAction extends  AbstractAction {
        public StopAction() {
            super("Stop");
        }
    	
        public void actionPerformed(ActionEvent e) {
            applet.stop();
            applet.setEnabled(false);
        }
    }
    
    private class PropertiesAction extends  AbstractAction {
    	public PropertiesAction() {
            super("Properties");
    	}
    	
        public void actionPerformed(final ActionEvent e) {
            showSetPropDialog(AppletFrame.this);
        }

        private void showSetPropDialog(final JFrame frame){
            final JDialog dialog = new JDialog(frame, "Harmony AppletViewer Properties");

            // Sheet part of Dialog
            JLabel httpHost =  new JLabel(Main.httpProxyHost);
            httpHost.setFont(httpHost.getFont().deriveFont(Font.PLAIN));

            JLabel httpPort =  new JLabel(Main.httpProxyPort);
            httpPort.setFont(httpPort.getFont().deriveFont(Font.PLAIN));

            JLabel httpsHost = new JLabel(Main.httpsProxyHost);
            httpsHost.setFont(httpsHost.getFont().deriveFont(Font.PLAIN));

            JLabel httpsPort = new JLabel(Main.httpsProxyPort);
            httpsPort.setFont(httpsPort.getFont().deriveFont(Font.PLAIN));

            JLabel ftpHost =   new JLabel(Main.ftpProxyHost);
            ftpHost.setFont(ftpHost.getFont().deriveFont(Font.PLAIN));

            JLabel ftpPort =   new JLabel(Main.ftpProxyPort);
            ftpPort.setFont(ftpPort.getFont().deriveFont(Font.PLAIN));
            
            final JTextField tfHttpHost =  new JTextField(Main.properties.getProperty(Main.httpProxyHost));
            Dimension d = tfHttpHost.getPreferredSize();
            tfHttpHost.setPreferredSize(new Dimension(50, d.height));

            final JTextField tfHttpPort =  new JTextField(Main.properties.getProperty(Main.httpProxyPort));
            tfHttpPort.setPreferredSize(new Dimension(50, d.height));

            final JTextField tfHttpsHost = new JTextField(Main.properties.getProperty(Main.httpsProxyHost));
            tfHttpsHost.setPreferredSize(new Dimension(50, d.height));

            final JTextField tfHttpsPort = new JTextField(Main.properties.getProperty(Main.httpsProxyPort));
            tfHttpsPort.setPreferredSize(new Dimension(50, d.height));

            final JTextField tfFtpHost =   new JTextField(Main.properties.getProperty(Main.ftpProxyHost));
            tfFtpHost.setPreferredSize(new Dimension(50, d.height));

            final JTextField tfFtpPort =   new JTextField(Main.properties.getProperty(Main.ftpProxyPort));
            tfFtpPort.setPreferredSize(new Dimension(50, d.height));

            JPanel sheetPanel = new JPanel();

            sheetPanel.setLayout(new GridLayout(6,2));

            sheetPanel.add(httpHost);
            sheetPanel.add(tfHttpHost);

            sheetPanel.add(httpPort);
            sheetPanel.add(tfHttpPort);

            sheetPanel.add(httpsHost);
            sheetPanel.add(tfHttpsHost);

            sheetPanel.add(httpsPort);
            sheetPanel.add(tfHttpsPort);

            sheetPanel.add(ftpHost);
            sheetPanel.add(tfFtpHost);

            sheetPanel.add(ftpPort);
            sheetPanel.add(tfFtpPort);

            sheetPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            final boolean useSameServer;

            final JCheckBox sameServer = new JCheckBox("Use same proxy server for all protocols");
            if(Main.properties.getProperty(Main.httpProxyHost).equals(
                Main.properties.getProperty(Main.httpsProxyHost)) && 
                Main.properties.getProperty(Main.httpProxyHost).equals(
                Main.properties.getProperty(Main.ftpProxyHost)) &&
                Main.properties.getProperty(Main.httpProxyPort).equals(
                Main.properties.getProperty(Main.httpsProxyPort)) && 
                Main.properties.getProperty(Main.httpProxyPort).equals(
                Main.properties.getProperty(Main.ftpProxyPort))) {

                sameServer.setSelected(true);

                tfHttpsHost.setText("");
                tfHttpsHost.setEditable(false);

                tfHttpsPort.setText("");
                tfHttpsPort.setEditable(false);

                tfFtpHost.setText("");
                tfFtpHost.setEditable(false);

                tfFtpPort.setText("");
                tfFtpPort.setEditable(false);
                useSameServer = true;
            } else {
                sameServer.setSelected(false);
                useSameServer = false;
            }

            sameServer.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED){

                        tfHttpsHost.setText("");
                        tfHttpsHost.setEditable(false);

                        tfHttpsPort.setText("");
                        tfHttpsPort.setEditable(false);

                        tfFtpHost.setText("");
                        tfFtpHost.setEditable(false);

                        tfFtpPort.setText("");
                        tfFtpPort.setEditable(false);

                    } else {

                        tfHttpsHost.setEditable(true);

                        tfHttpsPort.setEditable(true);

                        tfFtpHost.setEditable(true);

                        tfFtpPort.setEditable(true);

                    }
                    tfHttpHost.setCaretPosition(0);
                }
            });

            JPanel checkBoxPanel = new JPanel();
            checkBoxPanel.add(sameServer);
            checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            

            // Button part of Dialog
            JButton apply = new JButton("Apply");            
            apply.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(!checkPort(Main.httpProxyPort, tfHttpPort.getText().trim())){
                        return;
                    }                    
                    if(sameServer.isSelected()){
                        Main.properties.setProperty(Main.httpProxyHost, tfHttpHost.getText().trim());
                        Main.properties.setProperty(Main.httpProxyPort, tfHttpPort.getText().trim());
                        Main.properties.setProperty(Main.httpsProxyHost, tfHttpHost.getText().trim());
                        Main.properties.setProperty(Main.httpsProxyPort, tfHttpPort.getText().trim());
                        Main.properties.setProperty(Main.ftpProxyHost, tfHttpHost.getText().trim());
                        Main.properties.setProperty(Main.ftpProxyPort, tfHttpPort.getText().trim());
                    } else {
                        if(!checkPort(Main.httpsProxyPort, tfHttpsPort.getText().trim())){
                            return;
                        }                    
                        if(!checkPort(Main.ftpProxyPort, tfFtpPort.getText().trim())){
                            return;
                        }                    
                        Main.properties.setProperty(Main.httpProxyHost, tfHttpHost.getText().trim());
                        Main.properties.setProperty(Main.httpProxyPort, tfHttpPort.getText().trim());
                        Main.properties.setProperty(Main.httpsProxyHost, tfHttpsHost.getText().trim());
                        Main.properties.setProperty(Main.httpsProxyPort, tfHttpsPort.getText().trim());
                        Main.properties.setProperty(Main.ftpProxyHost, tfFtpHost.getText().trim());
                        Main.properties.setProperty(Main.ftpProxyPort, tfFtpPort.getText().trim());
                    }

                    Enumeration<?> en = Main.properties.propertyNames();

                    while(en.hasMoreElements()){
                        String key = (String)en.nextElement();
                        String val = Main.properties.getProperty(key);
                        if(val != null && val != ""){
                            System.setProperty(key, val);
                        }
                    }
                        
                    Main.storeProxyProperties();

                    dialog.setVisible(false);
                    dialog.dispose();
                }

                private boolean checkPort(String portName, String value){
                    boolean passed = true;
                    try{
                        if(Integer.parseInt(value) < 0){
                            passed = false;
                            showErrorMessage(portName);
                        }
                    } catch(NumberFormatException e){
                        passed = false;
                        showErrorMessage(portName);
                    }
                    return passed;
                }

                private void showErrorMessage(String portName){
                    JOptionPane.showMessageDialog(frame, 
                        portName + " must be a positive integer value", "Invalid entry", 
                        JOptionPane.ERROR_MESSAGE);
                }
            });

            JButton reset = new JButton("Reset");            
            reset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    tfHttpHost.setText(Main.properties.getProperty(Main.httpProxyHost));
                    tfHttpPort.setText(Main.properties.getProperty(Main.httpProxyPort));
 
                    if(useSameServer){
                        sameServer.setSelected(true);

                        tfHttpsHost.setText("");
                        tfHttpsHost.setEditable(false);

                        tfHttpsPort.setText("");
                        tfHttpsPort.setEditable(false);

                        tfFtpHost.setText("");
                        tfFtpHost.setEditable(false);

                        tfFtpPort.setText("");
                        tfFtpPort.setEditable(false);
                    } else {
                        tfHttpsHost.setText(Main.properties.getProperty(Main.httpsProxyHost));
                        tfHttpsPort.setText(Main.properties.getProperty(Main.httpsProxyPort));
                        tfFtpHost.setText(Main.properties.getProperty(Main.ftpProxyHost));
                        tfFtpPort.setText(Main.properties.getProperty(Main.ftpProxyPort));
                        sameServer.setSelected(false);
                    }

                }
            });

            JButton cancel = new JButton("Cancel"); 
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

            buttonPanel.add(apply);
            buttonPanel.add(reset);
            buttonPanel.add(cancel);

            JPanel contentPane = new JPanel();
            contentPane.setLayout(new BorderLayout());
            contentPane.add(sheetPanel, BorderLayout.NORTH);
            contentPane.add(checkBoxPanel, BorderLayout.CENTER);
            contentPane.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setContentPane(contentPane);
            dialog.setLocationRelativeTo(frame);
            dialog.pack();
            
            dialog.setVisible(true);

            tfHttpHost.setCaretPosition(0);
        }
    }
    
    private class CloseAction extends  AbstractAction {
        public CloseAction() {
            super("Close");
        }
    	
        public void actionPerformed(ActionEvent e) {
            AppletFrame.this.processWindowEvent(
                new WindowEvent(AppletFrame.this, WindowEvent.WINDOW_CLOSING));
        }
    }
    
    private class ExitAction extends  AbstractAction {
        public ExitAction() {
            super("Exit");
        }
    	
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }
    
    private static class ShutdownHandler implements WindowListener {
        HashSet<JFrame> frameList = new HashSet<JFrame>();

        public void windowActivated(WindowEvent e) {
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            JFrame frame = (JFrame)e.getWindow();
            frameList.remove(frame);

            Applet applet = ((AppletFrame)frame).getApplet();
            if(applet != null){
                ViewerAppletContext ac = 
                    (ViewerAppletContext)applet.getAppletContext();
                ac.remove(applet);
            }

            if (frameList.isEmpty())
                System.exit(0);
        }

        public void windowDeactivated(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowOpened(WindowEvent e) {
        }
        
        public void addFrame(JFrame frame) {
            frameList.add(frame);
            frame.addWindowListener(this);
        }

    }

}
