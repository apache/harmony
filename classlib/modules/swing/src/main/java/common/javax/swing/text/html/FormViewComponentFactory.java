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
 * @author Roman I. Chernyatchik
 */
package javax.swing.text.html;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;

import org.apache.harmony.x.swing.Utilities;
import org.apache.harmony.x.swing.text.html.HTMLIconFactory;
import org.apache.harmony.x.swing.text.html.form.Form;
import org.apache.harmony.x.swing.text.html.form.FormButtonModel;
import org.apache.harmony.x.swing.text.html.form.FormSelectComboBoxModel;
import org.apache.harmony.x.swing.text.html.form.FormSelectListModel;
import org.apache.harmony.x.swing.text.html.form.FormTextModel;
import org.apache.harmony.x.swing.text.html.form.FormToggleButtonModel;

final class FormViewComponentFactory {
    static class InputImageIcon implements Icon {
        private BackgroundImageLoader loader;
        private Icon icon;

        public InputImageIcon(final String src, final URL baseURL,
                                   final FormView view) {
            if (src == null) {
                icon = HTMLIconFactory.getNoImageIcon();
            } else {
                URL url = HTML.resolveURL(src, baseURL);
                if (url == null) {
                    icon = HTMLIconFactory.getLoadingFailedIcon();
                } else {
                    loader = new BackgroundImageLoader(url, true, -1, -1) {
                        protected void onReady() {
                            super.onReady();
                            view.preferenceChanged(view, true, true);
                        }

                        protected void onError() {
                            super.onError();
                            icon = HTMLIconFactory.getNoImageIcon();
                            view.preferenceChanged(view, true, true);
                        }
                   };
                }
            }
        }

        public boolean imageWasLoaded() {
            return loader != null && loader.isReady();
        }

        public void paintIcon(final Component c, final Graphics g,
                              final int x, final int y) {
            if (icon != null) {
                icon.paintIcon(c, g, x, y);
                return;
            }

            if (!loader.isReady()) {
                HTMLIconFactory.getLoadingImageIcon().paintIcon(c, g, x, y);
                return;
            }

            g.drawImage(loader.image,
                        x, y,
                        getIconWidth(), getIconHeight(),
                        loader);
        }

        public int getIconWidth() {
            if (icon != null) {
                return icon.getIconWidth();
            }

            if (!loader.isReady()) {
                return HTMLIconFactory.getLoadingImageIcon().getIconWidth();
            }

            return loader.getWidth();
        }

        public int getIconHeight() {
            if (icon != null) {
                return icon.getIconHeight();
            }

            if (!loader.isReady()) {
                return HTMLIconFactory.getLoadingImageIcon().getIconHeight();
            }

            return loader.getHeight();
        }
    };

        private static final int DEFAULT_TEXTFIELD_SIZE = 20;
        private static final int DEFAULT_STRUT = 5;
        private static final int DEFAULT_COLS_COUNT = 20;
        private static final int DEFAULT_ROWS_COUNT = 3;

        private static final char MEAN_CHAR = 'z';

        private static final Color IMG_BORDER_HIGHLIGHT = new Color(136, 136,
                                                                    136);
        private static final Color IMG_BORDER_SHADOW = new Color(204, 204, 204);

        private static final String DIR_RTL = "rtl";
        private static final String BROWSE_BUTTON_DEFAULT_TEXT = "Browse...";
        private static final String SUBMIT_DEFAULT_TEXT = "Submit Query";
        private static final String RESET_DEFAULT_TEXT = "Reset";

        private FormViewComponentFactory() {
        }

        public static Component createButtonComponent(final Object model,
                                                       final AttributeSet attrs,
                                                       final FormView view) {

            // TODO Implement support of BUTTON content
            return createImageComponent(model, attrs, view);
        }

        public static Component createInputButtonComponent(final Object model,
                final AttributeSet attrs) {

            ButtonModel buttonModel = (ButtonModel) model;
            final JButton button = new JButton("");

            // Model
            if (buttonModel == null) {
                buttonModel = new FormButtonModel(new Form(SimpleAttributeSet.EMPTY),
                                                  SimpleAttributeSet.EMPTY);
            }
            button.setModel(buttonModel);

            // VALUE
            String attribute = (String)attrs.getAttribute(HTML.Attribute.VALUE);
            if (!Utilities.isEmptyString(attribute)) {
                button.setText(attribute);
            } else {
                final int width, height;
                final FontMetrics fontMetrics
                    = button.getFontMetrics(button.getFont());
                final Insets insets = button.getInsets();
                width =  DEFAULT_STRUT + insets.top
                         + insets.bottom;
                height =  fontMetrics.getHeight() + insets.top + insets.bottom;

                Dimension size = button.getPreferredSize();
                size.width = width;
                size.height = height;
                button.setPreferredSize(size);
                button.setMaximumSize(size);
                button.setMinimumSize(size);
            }

            // SIZE
            setButtonSize(button, attrs);

            // TITLE
            setTitle(button, attrs);

            // ACCESSKEY
            setButtonAccessKey(button, attrs);

            // ALIGN
            setButtonAlign(button);

            // DISABLED
            setDisabled(button, attrs);

            return button;
        }

        public static Component createInputCheckBoxComponent(final Object model,
                                                     final AttributeSet attrs) {
            ToggleButtonModel checkBoxModel = (ToggleButtonModel) model;
            final JCheckBox checkBox = new JCheckBox();

            // Model
            if (checkBoxModel == null) {
                checkBoxModel = new FormToggleButtonModel(new Form(SimpleAttributeSet.EMPTY),
                                                          SimpleAttributeSet.EMPTY);
            }
            checkBox.setModel(checkBoxModel);

            // SIZE
            setButtonSize(checkBox, attrs);

            // TITLE
            setTitle(checkBox, attrs);

            // CHECKED
            setChecked(checkBox, attrs);

            // ACCESSKEY
            setButtonAccessKey(checkBox, attrs);

            // ALIGN
            setButtonAlign(checkBox);

            // DISABLED
            setDisabled(checkBox, attrs);

            return checkBox;
        }

        public static Component createInputImageComponent(final Object model,
                                                          final AttributeSet attrs,
                                                          final FormView view) {
            final Component image = createImageComponent(model, attrs, view);

            // ActionPerformed
            image.addMouseListener(view.new MouseEventListener());

            return image;
        }

        public static Component createInputPasswordComponent(final Object model,
                                                 final AttributeSet attrs,
                                                 final FormView view) {
            PlainDocument document = (PlainDocument) model;
            final JPasswordField passwordField = new JPasswordField();

            // Model
            if (document == null) {
                document = new FormTextModel(new Form(SimpleAttributeSet.EMPTY),
                                             SimpleAttributeSet.EMPTY);
            }
            passwordField.setDocument(document);

            // ActionPerformed
            passwordField.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent event) {
                    view.actionPerformed(event);
                }

            });

            // VALUE
            String attribute = (String)attrs.getAttribute(HTML.Attribute.VALUE);

            if (!Utilities.isEmptyString(attribute)) {
                passwordField.setText(attribute);
            }

            // SIZE
            setTextSize(passwordField, attrs, passwordField.getEchoChar());

            // TITLE
            setTitle(passwordField, attrs);

            // ACCESSKEY
            setTextAccessKey(passwordField, attrs);

            // DIR
            setTextDir(passwordField, attrs);

            // READONLY
            setTextReadonly(passwordField, attrs);

            // ALIGN
            setTextAlign(passwordField);

            // DISABLED
            setDisabled(passwordField, attrs);

            return passwordField;
        }

        public static Component createInputRadioComponent(final Object model,
                                                    final AttributeSet attrs) {
            ToggleButtonModel radioButtonModel;
            final JRadioButton radioButton = new JRadioButton();

            // NAME
            String attribute = (String) attrs.getAttribute(HTML.Attribute.NAME);
            if (!Utilities.isEmptyString(attribute)) {
                radioButtonModel = (ToggleButtonModel) model;
            } else {
                radioButtonModel = new ToggleButtonModel() {
                    public void setGroup(final ButtonGroup group) {
                        //Do nothing
                    };
                };
            }

            // Model
            if (radioButtonModel == null) {
                radioButtonModel = new FormToggleButtonModel(new Form(SimpleAttributeSet.EMPTY),
                                                             SimpleAttributeSet.EMPTY);
            }
            radioButton.setModel(radioButtonModel);

            // SIZE
            setButtonSize(radioButton, attrs);

            // TITLE
            setTitle(radioButton, attrs);

            // CHECKED
            setChecked(radioButton, attrs);

            // ACCESSKEY
            setButtonAccessKey(radioButton, attrs);

            // ALIGN
            setButtonAlign(radioButton);

            // DISABLED
            setDisabled(radioButton, attrs);

            return radioButton;
        }

        public static Component createInputResetComponent(final Object model,
                                                     final AttributeSet attrs,
                                                     final FormView view) {
            ButtonModel resetButtonModel = (ButtonModel) model;
            final JButton resetButton = new JButton();

            // Model
            if (resetButtonModel == null) {
                resetButtonModel = new FormButtonModel(new Form(SimpleAttributeSet.EMPTY),
                                                       SimpleAttributeSet.EMPTY);
            }
            resetButton.setModel(resetButtonModel);

            // ActionPerformed
            resetButton.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent event) {
                    view.actionPerformed(event);
                }

            });

            // VALUE
            String attribute = (String)attrs.getAttribute(HTML.Attribute.VALUE);

            if (!Utilities.isEmptyString(attribute)) {
                resetButton.setText(attribute);
            } else {
                resetButton.setText(RESET_DEFAULT_TEXT);
            }

            // SIZE
            setButtonSize(resetButton, attrs);

            // TITLE
            setTitle(resetButton, attrs);

            // ACCESSKEY
            setButtonAccessKey(resetButton, attrs);

            // ALIGN
            setButtonAlign(resetButton);

            // DISABLED
            setDisabled(resetButton, attrs);

            return resetButton;
        }

        public static Component createInputSubmitComponent(final Object model,
                                                       final AttributeSet attrs,
                                                       final FormView view) {
            ButtonModel submitButtonModel = (ButtonModel) model;
            final JButton submitButton = new JButton();

            // Model
            if (submitButtonModel == null) {
                submitButtonModel = new FormButtonModel(new Form(SimpleAttributeSet.EMPTY),
                                                        SimpleAttributeSet.EMPTY);
            }
            submitButton.setModel(submitButtonModel);

            // ActionPerformed
            submitButton.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent event) {
                    view.actionPerformed(event);
                }

            });

            // VALUE
            String attribute = (String)attrs.getAttribute(HTML.Attribute.VALUE);

            if (!Utilities.isEmptyString(attribute)) {
                submitButton.setText(attribute);
            } else {
                submitButton.setText(SUBMIT_DEFAULT_TEXT);
            }

            // SIZE
            setButtonSize(submitButton, attrs);

            // TITLE
            setTitle(submitButton, attrs);

            // ACCESSKEY
            setButtonAccessKey(submitButton, attrs);

            // ALIGN
            setButtonAlign(submitButton);

            // DISABLED
            setDisabled(submitButton, attrs);

            return submitButton;
        }

        public static Component createInputTextComponent(final Object model,
                                                     final AttributeSet attrs,
                                                     final FormView view) {
            PlainDocument document = (PlainDocument) model;
            final JTextField textField = new JTextField();

            // Model
            if (document == null) {
                document = new FormTextModel(new Form(SimpleAttributeSet.EMPTY),
                                             SimpleAttributeSet.EMPTY);
            }
            textField.setDocument(document);

            // ActionPerformed
            textField.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent event) {
                    view.actionPerformed(event);
                }

            });

            // VALUE
            final String attribute = (String)attrs.getAttribute(HTML.Attribute
                                                                .VALUE);
            if (!Utilities.isEmptyString(attribute)) {
                textField.setText(attribute);
            }

            // SIZE
            setTextSize(textField, attrs, MEAN_CHAR);

            // TITLE
            setTitle(textField, attrs);

            // ACCESSKEY
            setTextAccessKey(textField, attrs);

            // DIR
            setTextDir(textField, attrs);

            // READONLY
            setTextReadonly(textField, attrs);

            // ALIGN
            setTextAlign(textField);

            // DISABLED
            setDisabled(textField, attrs);

            return textField;
        }

        public static Component createInputFileComponent(final Object model,
                                                     final AttributeSet attrs) {
            /*
             * FilePath attributes
             */
            PlainDocument document = (PlainDocument) model;
            final JTextField filePath = new JTextField();

            // Model
            if (document == null) {
                document = new FormTextModel(new Form(SimpleAttributeSet.EMPTY),
                                             SimpleAttributeSet.EMPTY);
            }
            filePath.setDocument(document);

            // SIZE
            setTextSize(filePath, attrs, MEAN_CHAR);

            // ACCESSKEY
            setTextAccessKey(filePath, attrs);

            // DIR
            boolean isRTL = setTextDir(filePath, attrs);

            /*
             * Browse button attributes
             */
            final JButton browseButton
                = new JButton(BROWSE_BUTTON_DEFAULT_TEXT);

            // READONLY
            String attribute = (String) attrs.getAttribute(HTML.Attribute
                                                           .READONLY);
            if (attribute != null) {
               filePath.setEditable(false);
            } else {
                browseButton.addActionListener(new ActionListener() {
                    private JFileChooser chooser;
                    public void actionPerformed(final ActionEvent e) {
                        if (chooser == null) {
                            chooser = new JFileChooser();
                        }
                        if (chooser.showOpenDialog(browseButton)
                            == JFileChooser.APPROVE_OPTION) {

                            filePath.setText(chooser.getSelectedFile().getPath());
                        }
                    }
                });
            }

            /*
             * Box attributes
             */
            final Box box = Box.createHorizontalBox();

            // TITLE
            attribute = (String) attrs.getAttribute(HTML.Attribute.TITLE);
            if (!Utilities.isEmptyString(attribute)) {
                filePath.setToolTipText(attribute);
                browseButton.setToolTipText(attribute);
            }

            // ALIGN
            box.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            box.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
            browseButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            browseButton.setAlignmentY(JComponent.CENTER_ALIGNMENT);

            // DISABLED
            if (attrs.getAttribute(HTML.Attribute.DISABLED) != null) {
                filePath.setEnabled(false);
                browseButton.setEnabled(false);
            }

            box.add(filePath);
            box.add(Box.createHorizontalStrut(5));
            box.add(browseButton);

            if (isRTL) {
                box.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            }

            return box;
        }

        public static JComponent createSelectMultipleComponent(
                                                       final Object model,
                                                       final AttributeSet attrs) {
            // MULTIPLE
            final boolean isMultiple
                = (attrs.getAttribute(HTML.Attribute.MULTIPLE) != null);

            // SIZE
            int linesCount = 0;
            String attribute = (String)attrs.getAttribute(HTML.Attribute.SIZE);
            if (!Utilities.isEmptyString(attribute)) {
                try {
                    linesCount = Integer.parseInt(attribute);
                } catch (NumberFormatException e) {
                    //DO nothing
                }
            }

            /*
             * JList attributes
             */
            JList selectionList = new JList();
            FormSelectListModel optionModel;

            // Model
            if (model != null) {
                optionModel = (FormSelectListModel)model;
            } else {
                optionModel = new FormSelectListModel(new Form(SimpleAttributeSet.EMPTY),
                                                      SimpleAttributeSet.EMPTY,
                                                      selectionList.getSelectionModel());
            }

            selectionList.setModel(optionModel);
            selectionList.setSelectionModel(optionModel.getSelectionModel());

            if (isMultiple) {
                selectionList.setSelectionMode(ListSelectionModel
                                               .MULTIPLE_INTERVAL_SELECTION);

            }

            // TITLE
            if (!Utilities.isEmptyString(optionModel.getTitle())) {
                selectionList.setToolTipText(optionModel.getTitle());
            }

            // DIR
            setTextDir(selectionList, attrs);

            // OPTION attributes
            if (linesCount <= 1) {
                linesCount = Math.max(1, selectionList.getModel().getSize());
            }

            // Selection
            FormViewUtils.resetMultipleSelection(optionModel);

            /*
             * JScrollPane attributes
             */
            final FontMetrics fontMetrics
                = selectionList.getFontMetrics(selectionList.getFont());
            Dimension size;
            if (optionModel.getSize() == 0) {
                size = selectionList.getPreferredSize();
                Insets insets = selectionList.getInsets();
                size.width = fontMetrics.charWidth(MEAN_CHAR)
                             + insets.left + insets.right;
                selectionList.setPreferredSize(size);
            }

            JScrollPane pane = new JScrollPane(selectionList,
                                               JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                               JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


            size = pane.getPreferredSize();
            size.height = linesCount * fontMetrics.getHeight();
            pane.setMinimumSize(size);
            pane.setMaximumSize(size);
            pane.setPreferredSize(size);
            pane.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);

            // DISABLED
            if (optionModel.isEnabled()) {
                pane.setEnabled(false);
            }

            return pane;
        }

        public static JComponent createSelectSimpleComponent(
                                                     final Object model,
                                                     final AttributeSet attrs) {
            JComboBox selectElement = new JComboBox();
            FormSelectComboBoxModel comboBoxModel = (FormSelectComboBoxModel)model;
            // Model
            if (comboBoxModel == null) {
                comboBoxModel = new FormSelectComboBoxModel(new Form(SimpleAttributeSet.EMPTY),
                                                            SimpleAttributeSet.EMPTY);

            }
            selectElement.setModel(comboBoxModel);

            selectElement.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);

            // TITLE
            if (!Utilities.isEmptyString(comboBoxModel.getTitle())) {
                selectElement.setToolTipText(comboBoxModel.getTitle());
            }

            // DIR
            setTextDir(selectElement, attrs);

            // Selection
            FormViewUtils.resetSimpleSelection(selectElement.getModel());

            // Size
            final Dimension size = selectElement.getPreferredSize();
            selectElement.setMinimumSize(size);
            selectElement.setMaximumSize(size);

            // DISABLED
            if (!comboBoxModel.isEnabled()) {
               selectElement.setEnabled(false);
            }

            return selectElement;
        }


        public static Component createTextAreaComponent(final Object model,
                                                        final AttributeSet attrs,
                                                        final FormView view) {
            /*
             * JTextArea attributes
             */
            Dimension size;
            PlainDocument document = (PlainDocument)model;

            //ROWS
            int rowsCount = DEFAULT_ROWS_COUNT;
            String attribute = (String)attrs.getAttribute(HTML.Attribute.ROWS);
            if (!Utilities.isEmptyString(attribute)) {
                try {
                    rowsCount = Integer.parseInt(attribute);
                } catch (NumberFormatException  e) {
                    //Do nothing
                }
            }

            //COLS
            int columnsCount = DEFAULT_COLS_COUNT;
            attribute = (String)attrs.getAttribute(HTML.Attribute.COLS);
            if (!Utilities.isEmptyString(attribute)) {
                try {
                    columnsCount = Integer.parseInt(attribute);
                } catch (NumberFormatException  e) {
                    //Do nothing
                }
            }

            //Model
            if (document == null) {
                document = new FormTextModel(new Form(SimpleAttributeSet.EMPTY),
                                             SimpleAttributeSet.EMPTY);
            }

            final JTextArea textArea = new JTextArea(document,
                                                     null,
                                                     rowsCount,
                                                     columnsCount);
            //DIR
            setTextDir(textArea, attrs);

            //ACCESSKEY
            setTextAccessKey(textArea, attrs);

            //READONLY
            setTextReadonly(textArea, attrs);

            /*
             * JScrollPane attributes
             */
            final JScrollPane pane = new JScrollPane(textArea);
            size = pane.getPreferredSize();
            pane.setMinimumSize(size);
            pane.setPreferredSize(size);
            pane.setMaximumSize(size);

            pane.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);

            //TITLE
            attribute = (String) attrs.getAttribute(HTML.Attribute.TITLE);
            if (!Utilities.isEmptyString(attribute)) {
                textArea.setToolTipText(attribute);
                pane.setToolTipText(attribute);
                pane.getVerticalScrollBar().setToolTipText(attribute);
                pane.getHorizontalScrollBar().setToolTipText(attribute);
            }


            //DISABLED
            if (attrs.getAttribute(HTML.Attribute.DISABLED) != null) {
                textArea.setEnabled(false);
                pane.setEnabled(false);
            }

            return pane;
        }

        private static Component createImageComponent(final Object model,
                                                      final AttributeSet attrs,
                                                      final FormView view) {

            ButtonModel imageModel = (ButtonModel) model;
            final JButton image = new JButton("");

            // Model
            if (imageModel == null) {
                imageModel = new FormButtonModel(new Form(SimpleAttributeSet.EMPTY),
                                                 SimpleAttributeSet.EMPTY);
            }
            image.setModel(imageModel);

            image.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // SRC, ALT
            String attribute = (String)attrs.getAttribute(HTML.Attribute.SRC);

            InputImageIcon icon
                = new InputImageIcon(attribute,
                                          ((HTMLDocument)view.getDocument())
                                          .getBase(),
                                          view);
            image.setIcon(icon);
            image.setBackground(Color.WHITE);
            Dimension size;
            if (icon.imageWasLoaded()) {
                image.setBorderPainted(false);
                size = new Dimension(icon.getIconWidth(),
                                     icon.getIconHeight());
            } else {
                Border outside = new BevelBorder(BevelBorder.LOWERED,
                                                 IMG_BORDER_SHADOW,
                                                 IMG_BORDER_HIGHLIGHT);
                image.setBorder(new CompoundBorder(outside,
                                                  new EmptyBorder(5, 5, 5, 5)));
                image.setContentAreaFilled(false);
                image.setFocusPainted(false);
                attribute = (String)attrs.getAttribute(HTML.Attribute.ALT);
                if (!Utilities.isEmptyString(attribute)) {
                    image.setFont(new Font("Button.font", 0 , 12));
                    image.setText(attribute);
                    image.setToolTipText(attribute);
                }
                size = image.getPreferredSize();
            }
            image.setMinimumSize(size);
            image.setPreferredSize(size);
            image.setMaximumSize(size);

            //SIZE
            setButtonSize(image, attrs);

            //TITLE
            setTitle(image, attrs);

            //ACCESSKEY
            setButtonAccessKey(image, attrs);

            //ALIGN
            setButtonAlign(image);

            //DISABLED
            setDisabled(image, attrs);

            return image;
        }

        private static void setTextSize(final JTextComponent textComponent,
                final AttributeSet attrs, final char widestChar) {
            final String attribute
                = (String) attrs.getAttribute(HTML.Attribute.SIZE);
            int width = DEFAULT_TEXTFIELD_SIZE;
            if (attribute != null) {
                try {
                    final int newWidth = Integer.parseInt(attribute);
                    if (newWidth > width) {
                        width = newWidth;
                    }
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
            final FontMetrics fontMetrics
                = textComponent.getFontMetrics(textComponent.getFont());
            final int charWidth = fontMetrics.charWidth(widestChar);
            Dimension size = textComponent.getPreferredSize();

            size.width = width * charWidth;
            textComponent.setPreferredSize(size);
            textComponent.setMaximumSize(size);

            size = new Dimension(DEFAULT_TEXTFIELD_SIZE * charWidth,
                                 size.height);
            textComponent.setMinimumSize(size);
        }

        private static String setTitle(final JComponent component,
                                       final AttributeSet attrs) {
            final String attribute = (String) attrs.getAttribute(HTML.Attribute
                                                                 .TITLE);
            if (!Utilities.isEmptyString(attribute)) {
                component.setToolTipText(attribute);
            }
            return attribute;
        }

        private static void setTextReadonly(final JTextComponent textComponent,
                                            final AttributeSet attrs) {
            if (attrs.getAttribute(HTML.Attribute.READONLY) != null) {
                textComponent.setEditable(false);
            }
        }

        private static void setButtonAccessKey(final AbstractButton button,
                                               final AttributeSet attrs) {
            final String attribute = (String) attrs.getAttribute(HTML.Attribute
                                                                 .ACCESSKEY);
            if (!Utilities.isEmptyString(attribute)) {
                button.setMnemonic(attribute.charAt(0));
            }
        }

        private static void setButtonAlign(final AbstractButton button) {
            button.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            button.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
        }

        private static void setButtonSize(final AbstractButton button,
                                          final AttributeSet attrs) {
            final String attribute;
            attribute = (String)attrs.getAttribute(HTML.Attribute.SIZE);
            if (attribute != null) {
                Dimension size = button.getPreferredSize();
                try {
                    size.width = Integer.parseInt(attribute);
                } catch (NumberFormatException  e) {
                    //Do nothing
                }
                button.setPreferredSize(size);
                button.setMaximumSize(size);
                button.setMinimumSize(size);
            }
        }

        private static void setChecked(final JToggleButton button,
                                       final AttributeSet attrs) {
            if (attrs.getAttribute(HTML.Attribute.CHECKED) != null) {
                button.setSelected(true);
            }
        }

        private static void setDisabled(final Component component,
                                        final AttributeSet attrs) {
            if (attrs.getAttribute(HTML.Attribute.DISABLED) != null) {
                component.setEnabled(false);
            }
        }

        private static void setTextAccessKey(final JTextComponent textComponent,
                                             final AttributeSet attrs) {
            final String attribute = (String) attrs.getAttribute(HTML.Attribute
                                                                 .ACCESSKEY);
            if (!Utilities.isEmptyString(attribute)) {
                ActionListener listener = new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        textComponent.requestFocusInWindow();
                    }

                };
                final char key = attribute.charAt(0);
                final KeyStroke keystroke1
                    = KeyStroke.getKeyStroke(Character.toLowerCase(key),
                                             InputEvent.ALT_MASK);
                final KeyStroke keystroke2
                    = KeyStroke.getKeyStroke(Character.toUpperCase(key),
                                             InputEvent.ALT_MASK);
                textComponent.registerKeyboardAction(listener,
                                                     keystroke1,
                                                     JComponent
                                                     .WHEN_IN_FOCUSED_WINDOW);
                textComponent.registerKeyboardAction(listener,
                                                     keystroke2,
                                                     JComponent
                                                     .WHEN_IN_FOCUSED_WINDOW);
            }
        }

        private static void setTextAlign(final JTextComponent component) {
            component.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            component.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
        }

        private static boolean setTextDir(final Component component,
                                          final AttributeSet attrs) {
            final String attribute = (String)attrs.getAttribute(HTML.Attribute
                                                                .DIR);
            if (!Utilities.isEmptyString(attribute)) {
                if (DIR_RTL.equals(attribute.toLowerCase())) {
                    component.setComponentOrientation(ComponentOrientation
                                                          .RIGHT_TO_LEFT);
                    return true;
                }
            }
            return false;
        }

}