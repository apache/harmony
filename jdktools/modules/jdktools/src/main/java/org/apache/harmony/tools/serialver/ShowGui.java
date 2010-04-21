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
package org.apache.harmony.tools.serialver;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.harmony.tools.ClassProvider;

public class ShowGui extends JFrame {

    private static final long serialVersionUID = 679244276103498687L;

    ClassProvider classProvider;
    JButton showButton;
    JLabel classNameLabel;
    JLabel serialVersionLabel;
    JLabel statusBarLabel;
    JTextField classNameField;
    JTextField serialVersionField;

    public ShowGui(ClassProvider cp) {
        super("Serial Version Inspector");
        this.classProvider = cp;
        initComponents();
    }

    public void initComponents() {
        // Layout
        setLayout(new FlowLayout());

        // Initializaton
        classNameLabel = new JLabel("Full Class Name:");
        add(classNameLabel);

        classNameField = new JTextField(32);
        add(classNameField);

        showButton = new JButton("Show");
        add(showButton);

        serialVersionLabel = new JLabel("Serial Version:");
        add(serialVersionLabel);

        serialVersionField = new JTextField(38);
        serialVersionField.setEditable(false);
        add(serialVersionField);

        statusBarLabel = new JLabel("");
        add(statusBarLabel);

        // Handlers
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                actionPerfomed(evt);
            }
        };

        classNameField.addActionListener(al);
        showButton.addActionListener(al);
    }

    public void actionPerfomed(ActionEvent evt) {
        String targetClass = classNameField.getText();

        if (targetClass.length() == 0) {
            // No string was entered
            statusBarLabel.setText("");
            serialVersionField.setText("");
            return;
        }

        // Load the class.
        try {
            Clazz clazz = new Clazz(classProvider, targetClass);

            // Let's check if the class has any 'annoiance'
            if (!Main.isSerializable(clazz)) {
                statusBarLabel.setText("Class " + clazz.getName()
                                       + " is not Serializable.");
            } else {
                // Calculating suid with static class from the tool
                long hash = Main.calculeSUID(clazz);
                serialVersionField.setText("    static final long serialVersionUID = "
                                           + hash + "L;");
                statusBarLabel.setText("");
            }
        } catch (ClassNotFoundException e) { // Class Not Found
            serialVersionField.setText("");
            statusBarLabel.setText("Class " + targetClass + " not found.");
        }
    }
}
