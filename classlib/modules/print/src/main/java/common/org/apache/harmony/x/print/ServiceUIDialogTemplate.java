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
 * @author Irina A. Arkhipets 
 */ 

/*
 * ServiceUIDialogTemplate.java
 * 
 * This is a superclass for org.apache.harmony.x.print.ServiceUIDialog class.
 * 
 * ServiceUIDialogTemplate was automatically created using Eclipse Visual
 * Editor tool. It is organized to reflect visual dialog representation only.
 * All attributes and print services logic is concentrated in ServiceUIDialog 
 * class.
 *   
 */

package org.apache.harmony.x.print;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.text.NumberFormatter;

public class ServiceUIDialogTemplate {

JPanel namePanel=null;
JLabel nameLabel=null;
JComboBox servicesBox=null;
JButton propertiesBtn=null;
JLabel statusLabel=null;
JLabel typeLabel=null;
JLabel infoLabel=null;
JLabel statusText=null;
JLabel typeText=null;
JLabel infoText=null;
JCheckBox toFileBox=null;
JPanel printRangePanel=null;
JPanel copiesPanel=null;
JRadioButton allRngBtn=null;
JRadioButton pageRngBtn=null;
JLabel toLabel=null;
JFormattedTextField fromTxt=null;
JFormattedTextField toTxt=null;
JLabel copiesLabel=null;
JSpinner cpSpinner=null;
JCheckBox collateBox=null;
JPanel generalPanel=null;
JTabbedPane tabbedPane=null;
JPanel pageSetupMediaPanel=null;
JLabel sizeLabel=null;
JComboBox sizeBox=null;
JLabel sourceLabel=null;
JComboBox sourceBox=null;
JPanel orientationPanel=null;
JRadioButton portraitBtn=null;
JRadioButton landscapeBtn=null;
JRadioButton rvportraitBtn=null;
JRadioButton rvlandscapeBtn=null;
JPanel marginsPanel=null;
JLabel leftLabel=null;
JLabel rightLabel=null;
JFormattedTextField rightTxt=null;
JFormattedTextField leftTxt=null;
JFormattedTextField topTxt=null;
JLabel topLabel=null;
JLabel bottomLabel=null;
JFormattedTextField bottomTxt=null;
JPanel pageSetupPanel=null;
JPanel colorPanel=null;
JRadioButton monoBtn=null;
JRadioButton colorBtn=null;
JPanel qualityPanel=null;
JRadioButton draftBtn=null;
JRadioButton normalBtn=null;
JRadioButton highBtn=null;
JPanel sidesPanel=null;
JRadioButton oneSideBtn=null;
JRadioButton tumbleBtn=null;
JRadioButton duplexBtn=null;
JPanel jobAttributesPanel=null;
JCheckBox bannerBox=null;
JLabel priorityLabel=null;
JSpinner prtSpinner=null;
JTextField jobNameTxt=null;
JLabel jobNameLabel=null;
JLabel userNameLabel=null;
JTextField userNameTxt=null;
JPanel appearancePanel=null;
JButton printBtn=null;
JButton cancelBtn=null;
JDialog printDialog=null; //  @jve:decl-index=0:visual-constraint="86,16"
JPanel dialogPanel=null; //  @jve:decl-index=0:visual-constraint="709,338"
JPanel buttonsPanel=null;

JPanel vendorSuppliedPanel=null;

/*
 * Formatter for the integer text fields
 */
public static NumberFormatter getNumberFormatter() {
    DecimalFormat format = new DecimalFormat("#####");
    NumberFormatter ret;
    format.setParseIntegerOnly(true);
    format.setDecimalSeparatorAlwaysShown(false);
    format.setMinimumIntegerDigits(0);
    format.setMaximumIntegerDigits(5);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(0);
    ret = new NumberFormatter(format);
    ret.setMinimum(new Integer(1));
    ret.setMaximum(new Integer(Integer.MAX_VALUE));
    return ret;
}

/*
 * Formatter for the float text fields
 */
public static NumberFormatter getFloatFormatter() {
    DecimalFormat format = new DecimalFormat("###.##");
    NumberFormatter ret;
    format.setParseIntegerOnly(false);
    format.setDecimalSeparatorAlwaysShown(true);
    format.setMinimumIntegerDigits(1);
    format.setMaximumIntegerDigits(3);
    format.setMinimumFractionDigits(1);
    format.setMaximumFractionDigits(2);
    ret = new NumberFormatter(format);
    ret.setMinimum(new Float(0.0F));
    ret.setMaximum(new Float(999F));
    return ret;
}

private JPanel getNamePanel() {
    if(namePanel==null) {
        namePanel=new JPanel();
        infoText=new JLabel();
        typeText=new JLabel();
        statusText=new JLabel();
        infoLabel=new JLabel("Info:");
        typeLabel=new JLabel("Type");
        statusLabel=new JLabel("Status:");
        GridBagConstraints gridBagConstraints1=new GridBagConstraints();
        GridBagConstraints gridBagConstraints3=new GridBagConstraints();
        GridBagConstraints gridBagConstraints4=new GridBagConstraints();
        GridBagConstraints gridBagConstraints51=new GridBagConstraints();
        GridBagConstraints gridBagConstraints7=new GridBagConstraints();
        GridBagConstraints gridBagConstraints8=new GridBagConstraints();
        GridBagConstraints gridBagConstraints9=new GridBagConstraints();
        GridBagConstraints gridBagConstraints10=new GridBagConstraints();
        GridBagConstraints gridBagConstraints2=new GridBagConstraints();
        GridBagConstraints gridBagConstraints5=new GridBagConstraints();
        nameLabel=new JLabel();
        namePanel.setLayout(new GridBagLayout());
        nameLabel.setText("Name:");
        gridBagConstraints2.gridx=0;
        gridBagConstraints2.gridy=0;
        gridBagConstraints2.fill=java.awt.GridBagConstraints.BOTH;
        gridBagConstraints2.anchor=java.awt.GridBagConstraints.CENTER;
        gridBagConstraints2.gridheight=2;
        gridBagConstraints2.gridwidth=2;
        gridBagConstraints5.gridx=4;
        gridBagConstraints5.gridy=1;
        gridBagConstraints5.gridheight=4;
        gridBagConstraints5.anchor=java.awt.GridBagConstraints.NORTH;
        gridBagConstraints1.gridx=2;
        gridBagConstraints1.gridy=1;
        gridBagConstraints1.weightx=1.0;
        gridBagConstraints1.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints3.gridx=1;
        gridBagConstraints3.gridy=5;
        statusLabel.setText("Status:");
        gridBagConstraints4.gridx=1;
        gridBagConstraints4.gridy=7;
        typeLabel.setText("Type:");
        gridBagConstraints51.gridx=1;
        gridBagConstraints51.gridy=8;
        infoLabel.setText("Info:");
        namePanel.setBorder(BorderFactory.createTitledBorder(null,
                "Print Service", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        gridBagConstraints7.gridx=2;
        gridBagConstraints7.gridy=5;
        gridBagConstraints7.gridwidth=3;
        gridBagConstraints7.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints8.gridx=2;
        gridBagConstraints8.gridy=7;
        gridBagConstraints8.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints9.gridx=2;
        gridBagConstraints9.gridy=8;
        gridBagConstraints9.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints9.gridwidth=1;
        gridBagConstraints9.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints10.gridx=4;
        gridBagConstraints10.gridy=8;
        gridBagConstraints10.gridheight=3;
        gridBagConstraints10.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints10.anchor=java.awt.GridBagConstraints.CENTER;
        gridBagConstraints2.insets=new java.awt.Insets(9, 7, 9, 7);
        namePanel.setPreferredSize(new java.awt.Dimension(515, 180));
        gridBagConstraints5.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints1.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints3.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints3.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints4.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints4.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints51.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints51.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints7.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints8.insets=new java.awt.Insets(9, 7, 9, 7);
        namePanel.add(infoText, gridBagConstraints9);
        namePanel.add(typeLabel, gridBagConstraints4);
        namePanel.add(infoLabel, gridBagConstraints51);
        namePanel.add(gettoFileBox(), gridBagConstraints10);
        namePanel.add(typeText, gridBagConstraints8);
        namePanel.add(statusLabel, gridBagConstraints3);
        namePanel.add(nameLabel, gridBagConstraints2);
        namePanel.add(getPropertiesBtn(), gridBagConstraints5);
        namePanel.add(getservicesBox(), gridBagConstraints1);
        namePanel.add(statusText, gridBagConstraints7);
    }
    return namePanel;
}

/**
 * This method initializes jComboBox
 * 
 * @return javax.swing.JComboBox
 */
private JComboBox getservicesBox() {
    if(servicesBox==null) {
        servicesBox=new JComboBox();
        nameLabel.setDisplayedMnemonic('N');
        nameLabel.setLabelFor(servicesBox);
    }
    return servicesBox;
}

/**
 * This method initializes jButton
 * 
 * @return javax.swing.JButton
 */
private JButton getPropertiesBtn() {
    if(propertiesBtn==null) {
        propertiesBtn=new JButton();
        propertiesBtn.setText("Properties");
    }
    return propertiesBtn;
}

/**
 * This method initializes jCheckBox
 * 
 * @return javax.swing.JCheckBox
 */
private JCheckBox gettoFileBox() {
    if(toFileBox==null) {
        toFileBox=new JCheckBox();
        toFileBox.setText("Print to file");
        toFileBox.setMnemonic('F');
    }
    return toFileBox;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getPrintRangePanel() {
    if(printRangePanel==null) {
        printRangePanel=new JPanel();
        toLabel=new JLabel();
        GridBagConstraints gridBagConstraints11=new GridBagConstraints();
        GridBagConstraints gridBagConstraints12=new GridBagConstraints();
        GridBagConstraints gridBagConstraints13=new GridBagConstraints();
        GridBagConstraints gridBagConstraints14=new GridBagConstraints();
        GridBagConstraints gridBagConstraints17=new GridBagConstraints();
        printRangePanel.setLayout(new GridBagLayout());
        printRangePanel.setBorder(BorderFactory.createTitledBorder(null,
                "Print Range", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        printRangePanel.setPreferredSize(new java.awt.Dimension(256, 110));
        gridBagConstraints11.gridx=0;
        gridBagConstraints11.gridy=0;
        gridBagConstraints11.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints12.gridx=0;
        gridBagConstraints12.gridy=1;
        gridBagConstraints13.gridx=2;
        gridBagConstraints13.gridy=1;
        toLabel.setText("to");
        gridBagConstraints14.gridx=1;
        gridBagConstraints14.gridy=1;
        gridBagConstraints14.weightx=1.0;
        gridBagConstraints14.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints14.insets=new java.awt.Insets(9, 9, 9, 9);
        gridBagConstraints17.gridx=3;
        gridBagConstraints17.gridy=1;
        gridBagConstraints17.weightx=1.0;
        gridBagConstraints17.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints17.insets=new java.awt.Insets(9, 9, 9, 9);
        gridBagConstraints11.insets=new java.awt.Insets(9, 9, 9, 9);
        gridBagConstraints12.insets=new java.awt.Insets(9, 9, 9, 9);
        gridBagConstraints13.insets=new java.awt.Insets(0, 0, 0, 0);
        printRangePanel.add(toLabel, gridBagConstraints13);
        printRangePanel.add(getallRngBtn(), gridBagConstraints11);
        printRangePanel.add(getFromTxt(), gridBagConstraints14);
        printRangePanel.add(getpageRngBtn(), gridBagConstraints12);
        printRangePanel.add(getToTxt(), gridBagConstraints17);
    }
    return printRangePanel;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getCopiesPanel() {
    if(copiesPanel==null) {
        copiesPanel=new JPanel();
        GridBagConstraints gridBagConstraints18=new GridBagConstraints();
        GridBagConstraints gridBagConstraints19=new GridBagConstraints();
        GridBagConstraints gridBagConstraints20=new GridBagConstraints();
        copiesLabel=new JLabel();
        copiesPanel.setLayout(new GridBagLayout());
        copiesPanel.setBorder(BorderFactory.createTitledBorder(null, "Copies",                
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        copiesPanel.setPreferredSize(new java.awt.Dimension(256, 110));
        copiesLabel.setText("Number of copies:");
        gridBagConstraints18.gridx=1;
        gridBagConstraints18.gridy=0;
        gridBagConstraints18.insets=new java.awt.Insets(9, 9, 9, 9);
        gridBagConstraints18.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints19.gridx=4;
        gridBagConstraints19.gridy=0;
        gridBagConstraints19.weightx=1.0;
        gridBagConstraints19.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints19.insets=new java.awt.Insets(9, 9, 9, 9);
        gridBagConstraints19.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints19.gridwidth=2;
        gridBagConstraints20.gridx=1;
        gridBagConstraints20.gridy=1;
        gridBagConstraints20.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints20.insets=new java.awt.Insets(9, 9, 9, 9);
        copiesPanel.add(copiesLabel, gridBagConstraints18);
        copiesPanel.add(getCopiesSpinner(), gridBagConstraints19);
        copiesPanel.add(getCollateBox(), gridBagConstraints20);
    }
    return copiesPanel;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getallRngBtn() {
    if(allRngBtn==null) {
        allRngBtn=new JRadioButton();
        allRngBtn.setText("All");
        allRngBtn.setMnemonic('L');
    }
    return allRngBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getpageRngBtn() {
    if(pageRngBtn==null) {
        pageRngBtn=new JRadioButton();
        pageRngBtn.setText("Pages");
        pageRngBtn.setMnemonic('E');
    }
    return pageRngBtn;
}

/**
 * This method initializes JTextField
 * 
 * @return javax.swing.JTextField
 */
private JFormattedTextField getFromTxt() {
    if(fromTxt==null) {
        fromTxt=new JFormattedTextField(getNumberFormatter());
    }
    return fromTxt;
}

/**
 * This method initializes JTextField
 * 
 * @return javax.swing.JTextField
 */
private JFormattedTextField getToTxt() {
    if(toTxt==null) {
        toTxt=new JFormattedTextField(getNumberFormatter());
    }
    return toTxt;
}

/**
 * This method initializes JTextField
 * 
 * @return javax.swing.JTextField
 */
private JSpinner getCopiesSpinner() {
    if(cpSpinner==null) {
        cpSpinner=new JSpinner();
        copiesLabel.setLabelFor(cpSpinner);
        copiesLabel.setDisplayedMnemonic('O');
    }
    return cpSpinner;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JCheckBox getCollateBox() {
    if(collateBox==null) {
        collateBox=new JCheckBox();
        collateBox.setText("Collate");
        collateBox.setMnemonic('T');
    }
    return collateBox;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getGeneralPanel() {
    if(generalPanel==null) {
        generalPanel=new JPanel();
        generalPanel.setLayout(new FlowLayout());
        generalPanel.setPreferredSize(new java.awt.Dimension(500, 500));
        generalPanel.add(getNamePanel(), null);
        generalPanel.add(getCopiesPanel(), null);
        generalPanel.add(getPrintRangePanel(), null);
    }
    return generalPanel;
}

/**
 * This method initializes jTabbedPane
 * 
 * @return javax.swing.JTabbedPane
 */
private JTabbedPane getTabbedPane() {
    if(tabbedPane==null) {
        tabbedPane=new JTabbedPane();
        tabbedPane.setPreferredSize(new java.awt.Dimension(100, 100));
        tabbedPane.addTab("General", null, getGeneralPanel(), null);
        tabbedPane.addTab("Page Setup", null, getPageSetupPanel(), null);
        tabbedPane.addTab("Appearance", null, getAppearancePanel(), null);
        tabbedPane.setMnemonicAt(0, 'G');
        tabbedPane.setMnemonicAt(1, 'S');
        tabbedPane.setMnemonicAt(2, 'A');
    }
    return tabbedPane;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getPageSetupMediaPanel() {
    if(pageSetupMediaPanel==null) {
        pageSetupMediaPanel=new JPanel();
        sourceLabel=new JLabel();
        sizeLabel=new JLabel();
        GridBagConstraints gridBagConstraints16=new GridBagConstraints();
        GridBagConstraints gridBagConstraints21=new GridBagConstraints();
        GridBagConstraints gridBagConstraints31=new GridBagConstraints();
        GridBagConstraints gridBagConstraints42=new GridBagConstraints();
        pageSetupMediaPanel.setLayout(new GridBagLayout());
        pageSetupMediaPanel.setBorder(BorderFactory.createTitledBorder(null,
                "Media", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        pageSetupMediaPanel.setPreferredSize(new Dimension(515, 120));
        gridBagConstraints16.gridx=0;
        gridBagConstraints16.gridy=0;
        sizeLabel.setText("Size:");
        gridBagConstraints21.gridx=1;
        gridBagConstraints21.gridy=0;
        gridBagConstraints21.weightx=1.0;
        gridBagConstraints21.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints21.insets=new java.awt.Insets(9, 3, 9, 3);
        gridBagConstraints16.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints31.gridx=0;
        gridBagConstraints31.gridy=1;
        sourceLabel.setText("Source:");
        gridBagConstraints42.gridx=1;
        gridBagConstraints42.gridy=1;
        gridBagConstraints42.weightx=1.0;
        gridBagConstraints42.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints42.insets=new java.awt.Insets(9, 3, 9, 3);
        gridBagConstraints31.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints31.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints16.anchor=java.awt.GridBagConstraints.WEST;
        pageSetupMediaPanel.add(sizeLabel, gridBagConstraints16);
        pageSetupMediaPanel.add(getsizeBox(), gridBagConstraints21);
        pageSetupMediaPanel.add(sourceLabel, gridBagConstraints31);
        pageSetupMediaPanel.add(getsourceBox(), gridBagConstraints42);
    }
    return pageSetupMediaPanel;
}

/**
 * This method initializes jComboBox
 * 
 * @return javax.swing.JComboBox
 */
private JComboBox getsizeBox() {
    if(sizeBox==null) {
        sizeBox=new JComboBox();
        sizeBox.setPreferredSize(new java.awt.Dimension(270, 25));
        sizeLabel.setLabelFor(sizeBox);
        sizeLabel.setDisplayedMnemonic('Z');
    }
    return sizeBox;
}

/**
 * This method initializes jComboBox1
 * 
 * @return javax.swing.JComboBox
 */
private JComboBox getsourceBox() {
    if(sourceBox==null) {
        sourceBox=new JComboBox();
        sourceBox.setPreferredSize(new java.awt.Dimension(270, 25));
    }
    return sourceBox;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getOrientationPanel() {
    if(orientationPanel==null) {
        orientationPanel=new JPanel();
        GridBagConstraints gridBagConstraints52=new GridBagConstraints();
        GridBagConstraints gridBagConstraints6=new GridBagConstraints();
        GridBagConstraints gridBagConstraints71=new GridBagConstraints();
        GridBagConstraints gridBagConstraints81=new GridBagConstraints();
        orientationPanel.setLayout(new GridBagLayout());
        orientationPanel.setBorder(BorderFactory.createTitledBorder(null,
                "Orientation", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        orientationPanel.setPreferredSize(new java.awt.Dimension(250, 170));
        gridBagConstraints52.gridx=0;
        gridBagConstraints52.gridy=0;
        gridBagConstraints52.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints52.insets=new java.awt.Insets(5, 9, 5, 9);
        gridBagConstraints6.gridx=0;
        gridBagConstraints6.gridy=1;
        gridBagConstraints6.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints6.insets=new java.awt.Insets(5, 9, 5, 9);
        gridBagConstraints71.gridx=0;
        gridBagConstraints71.gridy=2;
        gridBagConstraints71.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints71.insets=new java.awt.Insets(5, 9, 5, 9);
        gridBagConstraints81.gridx=0;
        gridBagConstraints81.gridy=3;
        gridBagConstraints81.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints81.insets=new java.awt.Insets(5, 9, 5, 9);
        orientationPanel.add(getportraitBtn(), gridBagConstraints52);
        orientationPanel.add(getlandscapeBtn(), gridBagConstraints6);
        orientationPanel.add(getReverseportraitBtn(), gridBagConstraints71);
        orientationPanel.add(getReverselandscapeBtn(), gridBagConstraints81);
    }
    return orientationPanel;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getportraitBtn() {
    if(portraitBtn==null) {
        portraitBtn=new JRadioButton();
        portraitBtn.setText("Portrait");
        portraitBtn.setMnemonic('O');
    }
    return portraitBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getlandscapeBtn() {
    if(landscapeBtn==null) {
        landscapeBtn=new JRadioButton();
        landscapeBtn.setText("Landscape");
        landscapeBtn.setMnemonic('L');
    }
    return landscapeBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getReverseportraitBtn() {
    if(rvportraitBtn==null) {
        rvportraitBtn=new JRadioButton();
        rvportraitBtn.setText("Reverse Portrait");
        rvportraitBtn.setMnemonic('E');
    }
    return rvportraitBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getReverselandscapeBtn() {
    if(rvlandscapeBtn==null) {
        rvlandscapeBtn=new JRadioButton();
        rvlandscapeBtn.setText("Reverse Landscape");
        rvlandscapeBtn.setMnemonic('N');
    }
    return rvlandscapeBtn;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getMarginsPanel() {
    if(marginsPanel==null) {
        marginsPanel=new JPanel();
        bottomLabel=new JLabel();
        topLabel=new JLabel();
        rightLabel=new JLabel();
        GridBagConstraints gridBagConstraints91=new GridBagConstraints();
        GridBagConstraints gridBagConstraints101=new GridBagConstraints();
        GridBagConstraints gridBagConstraints111=new GridBagConstraints();
        GridBagConstraints gridBagConstraints121=new GridBagConstraints();
        GridBagConstraints gridBagConstraints131=new GridBagConstraints();
        GridBagConstraints gridBagConstraints141=new GridBagConstraints();
        GridBagConstraints gridBagConstraints151=new GridBagConstraints();
        GridBagConstraints gridBagConstraints161=new GridBagConstraints();
        leftLabel=new JLabel();
        marginsPanel.setLayout(new GridBagLayout());
        marginsPanel.setPreferredSize(new java.awt.Dimension(250, 170));
        marginsPanel.setBorder(BorderFactory.createTitledBorder(null,
                "Margins", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        leftLabel.setText("Left (mm)");
        rightLabel.setText("Right (mm)");
        gridBagConstraints111.gridx=2;
        gridBagConstraints111.gridy=1;
        gridBagConstraints111.weightx=1.0;
        gridBagConstraints111.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints121.gridx=0;
        gridBagConstraints121.gridy=1;
        gridBagConstraints121.weightx=1.0;
        gridBagConstraints121.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints131.gridx=0;
        gridBagConstraints131.gridy=5;
        gridBagConstraints131.weightx=1.0;
        gridBagConstraints131.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints141.gridx=0;
        gridBagConstraints141.gridy=4;
        topLabel.setText("Top (mm)");
        gridBagConstraints151.gridx=2;
        gridBagConstraints151.gridy=4;
        bottomLabel.setText("Bottom (mm)");
        gridBagConstraints161.gridx=2;
        gridBagConstraints161.gridy=5;
        gridBagConstraints161.weightx=1.0;
        gridBagConstraints161.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints161.insets=new java.awt.Insets(7, 9, 7, 9);
        gridBagConstraints121.insets=new java.awt.Insets(7, 9, 7, 9);
        gridBagConstraints141.insets=new java.awt.Insets(9, 9, 0, 9);
        gridBagConstraints131.insets=new java.awt.Insets(7, 9, 7, 9);
        gridBagConstraints151.insets=new java.awt.Insets(9, 9, 0, 9);
        gridBagConstraints111.insets=new java.awt.Insets(7, 9, 7, 9);
        marginsPanel.add(leftLabel, gridBagConstraints91);
        marginsPanel.add(rightLabel, gridBagConstraints101);
        marginsPanel.add(getRightTxt(), gridBagConstraints111);
        marginsPanel.add(getLeftTxt(), gridBagConstraints121);
        marginsPanel.add(getTopTxt(), gridBagConstraints131);
        marginsPanel.add(bottomLabel, gridBagConstraints151);
        marginsPanel.add(getBottomTxt(), gridBagConstraints161);
        marginsPanel.add(topLabel, gridBagConstraints141);
        gridBagConstraints91.gridx=0;
        gridBagConstraints91.gridy=0;
        gridBagConstraints91.insets=new java.awt.Insets(9, 9, 0, 9);
        gridBagConstraints101.gridx=2;
        gridBagConstraints101.gridy=0;
        gridBagConstraints101.insets=new java.awt.Insets(9, 9, 0, 9);
    }
    return marginsPanel;
}

/**
 * This method initializes JTextField
 * 
 * @return javax.swing.JTextField
 */
private JFormattedTextField getRightTxt() {
    if(rightTxt==null) {
        rightTxt=new JFormattedTextField(getFloatFormatter());
        rightLabel.setLabelFor(rightTxt);
        rightLabel.setDisplayedMnemonic('R');
    }
    return rightTxt;
}

/**
 * This method initializes JTextField1
 * 
 * @return javax.swing.JTextField
 */
private JFormattedTextField getLeftTxt() {
    if(leftTxt==null) {
        leftTxt=new JFormattedTextField(getFloatFormatter());
        leftLabel.setLabelFor(leftTxt);
        leftLabel.setDisplayedMnemonic('F');
    }
    return leftTxt;
}

/**
 * This method initializes JTextField1
 * 
 * @return javax.swing.JTextField
 */
private JFormattedTextField getTopTxt() {
    if(topTxt==null) {
        topTxt=new JFormattedTextField(getFloatFormatter());
        topLabel.setLabelFor(topTxt);
        topLabel.setDisplayedMnemonic('T');        
    }
    return topTxt;
}

/**
 * This method initializes JTextField1
 * 
 * @return javax.swing.JTextField
 */
private JFormattedTextField getBottomTxt() {
    if(bottomTxt==null) {
        bottomTxt=new JFormattedTextField(getFloatFormatter());
        bottomLabel.setLabelFor(bottomTxt);
        bottomLabel.setDisplayedMnemonic('B');
    }
    return bottomTxt;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
JPanel getPageSetupPanel() {
    if(pageSetupPanel==null) {
        GridBagConstraints gridBagConstraints22=new GridBagConstraints();
        pageSetupPanel=new JPanel();
        GridBagConstraints gridBagConstraints191=new GridBagConstraints();
        GridBagConstraints gridBagConstraints23=new GridBagConstraints();
        pageSetupPanel.setLayout(new GridBagLayout());
        pageSetupPanel.setPreferredSize(new java.awt.Dimension(1047, 190));
        gridBagConstraints191.gridx=0;
        gridBagConstraints191.gridy=1;
        gridBagConstraints191.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints191.insets=new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints23.gridx=1;
        gridBagConstraints23.gridy=1;
        gridBagConstraints23.insets=new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints23.anchor=java.awt.GridBagConstraints.EAST;
        gridBagConstraints22.gridx=0;
        gridBagConstraints22.gridy=0;
        gridBagConstraints22.gridwidth=3;
        gridBagConstraints22.anchor=java.awt.GridBagConstraints.CENTER;
        gridBagConstraints22.insets=new java.awt.Insets(5, 0, 5, 0);
        pageSetupPanel.add(getOrientationPanel(), gridBagConstraints191);
        pageSetupPanel.add(getMarginsPanel(), gridBagConstraints23);
        pageSetupPanel.add(getPageSetupMediaPanel(), gridBagConstraints22);
    }
    return pageSetupPanel;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getColorPanel() {
    if(colorPanel==null) {
        colorPanel=new JPanel();
        GridBagConstraints gridBagConstraints32=new GridBagConstraints();
        GridBagConstraints gridBagConstraints43=new GridBagConstraints();
        colorPanel.setLayout(new GridBagLayout());
        colorPanel.setBorder(BorderFactory.createTitledBorder(null,
                "Color Appearance", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        colorPanel.setPreferredSize(new java.awt.Dimension(185, 144));
        gridBagConstraints32.gridx=0;
        gridBagConstraints32.gridy=0;
        gridBagConstraints32.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints32.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints43.gridx=0;
        gridBagConstraints43.gridy=1;
        gridBagConstraints43.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints43.insets=new java.awt.Insets(9, 7, 9, 7);
        colorPanel.add(getmonoBtn(), gridBagConstraints32);
        colorPanel.add(getcolorBtn(), gridBagConstraints43);
    }
    return colorPanel;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getmonoBtn() {
    if(monoBtn==null) {
        monoBtn=new JRadioButton();
        monoBtn.setText("Monochrome");
        monoBtn.setMnemonic('M');
    }
    return monoBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getcolorBtn() {
    if(colorBtn==null) {
        colorBtn=new JRadioButton();
        colorBtn.setText("Color");
        colorBtn.setMnemonic('L');
    }
    return colorBtn;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getQualityPanel() {
    if(qualityPanel==null) {
        qualityPanel=new JPanel();
        GridBagConstraints gridBagConstraints61=new GridBagConstraints();
        GridBagConstraints gridBagConstraints72=new GridBagConstraints();
        GridBagConstraints gridBagConstraints82=new GridBagConstraints();
        qualityPanel.setLayout(new GridBagLayout());
        qualityPanel.setBorder(BorderFactory.createTitledBorder(null,
                "Quality", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        qualityPanel.setPreferredSize(new java.awt.Dimension(310, 144));
        gridBagConstraints61.gridx=1;
        gridBagConstraints61.gridy=0;
        gridBagConstraints61.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints61.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints72.gridx=1;
        gridBagConstraints72.gridy=1;
        gridBagConstraints72.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints72.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints82.gridx=1;
        gridBagConstraints82.gridy=2;
        gridBagConstraints82.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints82.insets=new java.awt.Insets(9, 0, 9, 0);
        qualityPanel.add(getdraftBtn(), gridBagConstraints61);
        qualityPanel.add(getnormalBtn(), gridBagConstraints72);
        qualityPanel.add(gethighBtn(), gridBagConstraints82);
    }
    return qualityPanel;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getdraftBtn() {
    if(draftBtn==null) {
        draftBtn=new JRadioButton();
        draftBtn.setText("Draft");
        draftBtn.setMnemonic('F');
    }
    return draftBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getnormalBtn() {
    if(normalBtn==null) {
        normalBtn=new JRadioButton();
        normalBtn.setText("Normal");
        normalBtn.setMnemonic('N');
    }
    return normalBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton gethighBtn() {
    if(highBtn==null) {
        highBtn=new JRadioButton();
        highBtn.setText("Hight");
        highBtn.setMnemonic('H');
    }
    return highBtn;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getSidesPanel() {
    if(sidesPanel==null) {
        sidesPanel=new JPanel();
        GridBagConstraints gridBagConstraints102=new GridBagConstraints();
        GridBagConstraints gridBagConstraints112=new GridBagConstraints();
        GridBagConstraints gridBagConstraints122=new GridBagConstraints();
        sidesPanel.setLayout(new GridBagLayout());
        sidesPanel.setBorder(BorderFactory.createTitledBorder(null, "Sides",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        sidesPanel.setPreferredSize(new java.awt.Dimension(185, 144));
        gridBagConstraints102.gridx=1;
        gridBagConstraints102.gridy=0;
        gridBagConstraints102.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints102.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints112.gridx=1;
        gridBagConstraints112.gridy=1;
        gridBagConstraints112.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints112.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints122.gridx=1;
        gridBagConstraints122.gridy=2;
        gridBagConstraints122.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints122.insets=new java.awt.Insets(9, 0, 9, 0);
        sidesPanel.add(getoneSideBtn(), gridBagConstraints102);
        sidesPanel.add(gettumbleBtn(), gridBagConstraints112);
        sidesPanel.add(getduplexBtn(), gridBagConstraints122);
    }
    return sidesPanel;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getoneSideBtn() {
    if(oneSideBtn==null) {
        oneSideBtn=new JRadioButton();
        oneSideBtn.setText("One Side");
        oneSideBtn.setMnemonic('O');
    }
    return oneSideBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton gettumbleBtn() {
    if(tumbleBtn==null) {
        tumbleBtn=new JRadioButton();
        tumbleBtn.setText("Tumble");
        tumbleBtn.setMnemonic('T');
    }
    return tumbleBtn;
}

/**
 * This method initializes jRadioButton
 * 
 * @return javax.swing.JRadioButton
 */
private JRadioButton getduplexBtn() {
    if(duplexBtn==null) {
        duplexBtn=new JRadioButton();
        duplexBtn.setText("Duplex");
        duplexBtn.setMnemonic('D');
    }
    return duplexBtn;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getJobAttributesPanel() {
    if(jobAttributesPanel==null) {
        jobAttributesPanel=new JPanel();
        userNameLabel=new JLabel();
        jobNameLabel=new JLabel();
        priorityLabel=new JLabel();
        GridBagConstraints gridBagConstraints132=new GridBagConstraints();
        GridBagConstraints gridBagConstraints142=new GridBagConstraints();
        GridBagConstraints gridBagConstraints152=new GridBagConstraints();
        GridBagConstraints gridBagConstraints162=new GridBagConstraints();
        GridBagConstraints gridBagConstraints171=new GridBagConstraints();
        GridBagConstraints gridBagConstraints181=new GridBagConstraints();
        GridBagConstraints gridBagConstraints192=new GridBagConstraints();
        jobAttributesPanel.setLayout(new GridBagLayout());
        jobAttributesPanel.setBorder(BorderFactory.createTitledBorder(null,
                "Job Attributes", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, null));
        jobAttributesPanel.setPreferredSize(new Dimension(310, 144));
        gridBagConstraints132.gridx=0;
        gridBagConstraints132.gridy=0;
        gridBagConstraints142.gridx=1;
        gridBagConstraints142.gridy=0;
        priorityLabel.setText("Priority");
        gridBagConstraints152.gridx=2;
        gridBagConstraints152.gridy=0;
        gridBagConstraints152.weightx=1.0;
        gridBagConstraints152.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints162.gridx=1;
        gridBagConstraints162.gridy=1;
        gridBagConstraints162.weightx=1.0;
        gridBagConstraints162.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints171.gridx=0;
        gridBagConstraints171.gridy=1;
        jobNameLabel.setText("Job Name:");
        gridBagConstraints181.gridx=0;
        gridBagConstraints181.gridy=2;
        userNameLabel.setText("User Name:");
        gridBagConstraints192.gridx=1;
        gridBagConstraints192.gridy=2;
        gridBagConstraints192.weightx=1.0;
        gridBagConstraints192.fill=java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints192.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints192.gridwidth=2;
        gridBagConstraints132.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints171.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints171.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints181.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints181.anchor=java.awt.GridBagConstraints.WEST;
        gridBagConstraints142.insets=new java.awt.Insets(9, 7, 9, 7);
        gridBagConstraints152.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints162.insets=new java.awt.Insets(9, 0, 9, 0);
        gridBagConstraints162.gridwidth=2;
        jobAttributesPanel.add(getBannerBox(), gridBagConstraints132);
        jobAttributesPanel.add(priorityLabel, gridBagConstraints142);
        jobAttributesPanel.add(getprtSpinner(), gridBagConstraints152);
        jobAttributesPanel.add(getJobNameTxt(), gridBagConstraints162);
        jobAttributesPanel.add(jobNameLabel, gridBagConstraints171);
        jobAttributesPanel.add(userNameLabel, gridBagConstraints181);
        jobAttributesPanel.add(getUserNameTxt(), gridBagConstraints192);
    }
    return jobAttributesPanel;
}

/**
 * This method initializes jCheckBox
 * 
 * @return javax.swing.JCheckBox
 */
private JCheckBox getBannerBox() {
    if(bannerBox==null) {
        bannerBox=new JCheckBox();
        bannerBox.setText("Banner Page");
        bannerBox.setMnemonic('B');
    }
    return bannerBox;
}

/**
 * This method initializes JTextField1
 * 
 * @return javax.swing.JTextField
 */
private JSpinner getprtSpinner() {
    if(prtSpinner==null) {
        prtSpinner=new JSpinner();
        priorityLabel.setLabelFor(prtSpinner);
        priorityLabel.setDisplayedMnemonic('R');        
    }
    return prtSpinner;
}

/**
 * This method initializes JTextField1
 * 
 * @return javax.swing.JTextField
 */
private JTextField getJobNameTxt() {
    if(jobNameTxt==null) {
        jobNameTxt=new JTextField();
        jobNameTxt.setText("");
        jobNameLabel.setLabelFor(jobNameTxt);
        jobNameLabel.setDisplayedMnemonic('J');
    }
    return jobNameTxt;
}

/**
 * This method initializes JTextField1
 * 
 * @return javax.swing.JTextField
 */
private JTextField getUserNameTxt() {
    if(userNameTxt==null) {
        userNameTxt=new JTextField();
        userNameLabel.setLabelFor(userNameTxt);
        userNameLabel.setDisplayedMnemonic('U');
    }
    return userNameTxt;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getAppearancePanel() {
    if(appearancePanel==null) {
        appearancePanel=new JPanel();
        GridBagConstraints gridBagConstraints110=new GridBagConstraints();
        GridBagConstraints gridBagConstraints24=new GridBagConstraints();
        GridBagConstraints gridBagConstraints33=new GridBagConstraints();
        GridBagConstraints gridBagConstraints44=new GridBagConstraints();
        appearancePanel.setLayout(new GridBagLayout());
        gridBagConstraints110.gridx=0;
        gridBagConstraints110.gridy=0;
        gridBagConstraints110.insets=new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints24.gridx=1;
        gridBagConstraints24.gridy=0;
        gridBagConstraints24.insets=new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints33.gridx=0;
        gridBagConstraints33.gridy=1;
        gridBagConstraints33.insets=new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints44.gridx=1;
        gridBagConstraints44.gridy=1;
        gridBagConstraints44.insets=new java.awt.Insets(5, 5, 5, 5);
        appearancePanel.add(getColorPanel(), gridBagConstraints110);
        appearancePanel.add(getQualityPanel(), gridBagConstraints24);
        appearancePanel.add(getSidesPanel(), gridBagConstraints33);
        appearancePanel.add(getJobAttributesPanel(), gridBagConstraints44);
    }
    return appearancePanel;
}

/**
 * This method initializes jButton
 * 
 * @return javax.swing.JButton
 */
private JButton getprintBtn() {
    if(printBtn==null) {
        printBtn=new JButton();
        printBtn.setText("Print");
        printBtn.setMnemonic('P');
    }
    return printBtn;
}

/**
 * This method initializes jButton
 * 
 * @return javax.swing.JButton
 */
private JButton getcancelBtn() {
    if(cancelBtn==null) {
        cancelBtn=new JButton();
        cancelBtn.setText("Cancel");
        cancelBtn.setMnemonic('C');
    }
    return cancelBtn;
}

/**
 * This method initializes jDialog
 * 
 * @return javax.swing.JDialog
 */
private JDialog getPrintDialog() {
    if(printDialog==null) {
        printDialog=new JDialog();
        printDialog.setSize(542, 444);
        printDialog.setName("dialog");
        printDialog.setTitle("Print");
        printDialog.setContentPane(getDialogPanel());
    }
    return printDialog;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
private JPanel getDialogPanel() {
    if(dialogPanel==null) {
        GridBagConstraints gridBagConstraints182=new GridBagConstraints();
        GridBagConstraints gridBagConstraints172=new GridBagConstraints();
        dialogPanel=new JPanel();
        dialogPanel.setLayout(new GridBagLayout());
        dialogPanel.setPreferredSize(new java.awt.Dimension(100, 100));
        dialogPanel.setSize(532, 389);
        gridBagConstraints172.gridx=0;
        gridBagConstraints172.gridy=1;
        gridBagConstraints172.anchor=java.awt.GridBagConstraints.EAST;
        gridBagConstraints172.gridwidth=3;
        gridBagConstraints182.gridx=0;
        gridBagConstraints182.gridy=0;
        gridBagConstraints182.weightx=1.0;
        gridBagConstraints182.weighty=1.0;
        gridBagConstraints182.fill=java.awt.GridBagConstraints.BOTH;
        dialogPanel.add(getButtonsPanel(), gridBagConstraints172);
        dialogPanel.add(getTabbedPane(), gridBagConstraints182);
    }
    return dialogPanel;
}

/**
 * This method initializes jPanel
 * 
 * @return javax.swing.JPanel
 */
JPanel getButtonsPanel() {
    if(buttonsPanel==null) {
        buttonsPanel=new JPanel();
        GridBagConstraints gridBagConstraints143=new GridBagConstraints();
        GridBagConstraints gridBagConstraints153=new GridBagConstraints();
        buttonsPanel.setLayout(new GridBagLayout());
        gridBagConstraints143.gridx=2;
        gridBagConstraints143.gridy=0;
        gridBagConstraints143.anchor=java.awt.GridBagConstraints.EAST;
        gridBagConstraints143.insets=new java.awt.Insets(7, 7, 7, 15);
        gridBagConstraints153.gridx=1;
        gridBagConstraints153.gridy=0;
        gridBagConstraints153.anchor=java.awt.GridBagConstraints.EAST;
        gridBagConstraints153.insets=new java.awt.Insets(7, 7, 7, 0);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        buttonsPanel.setPreferredSize(new java.awt.Dimension(170, 40));
        buttonsPanel.add(getcancelBtn(), gridBagConstraints143);
        buttonsPanel.add(getprintBtn(), gridBagConstraints153);
    }
    return buttonsPanel;
}

private JPanel getVendorSuppliedPanel() {
    if (vendorSuppliedPanel == null) {
        vendorSuppliedPanel = new JPanel();
    }
    return vendorSuppliedPanel;
}

JPanel getPanel() {
    return getDialogPanel();
}

} /* End of ServiceUIDialogTemplate class */
