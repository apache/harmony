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
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import org.apache.harmony.luni.util.NotImplementedException;

/**
 * <p>
 * <i>ProgressMonitor</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class ProgressMonitor implements Accessible {
    protected class AccessibleProgressMonitor extends AccessibleContext implements
            AccessibleText, ChangeListener, PropertyChangeListener {
        protected AccessibleProgressMonitor() {
        }

        public void stateChanged(ChangeEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void propertyChange(PropertyChangeEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public String getAccessibleName() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public String getAccessibleDescription() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleRole getAccessibleRole() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleParent() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public int getAccessibleIndexInParent() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public int getAccessibleChildrenCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleChild(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Locale getLocale() throws IllegalComponentStateException,
                NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleComponent getAccessibleComponent() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleValue getAccessibleValue() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleText getAccessibleText() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getIndexAtPoint(Point p) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Rectangle getCharacterBounds(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getCharCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getCaretPosition() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getAtIndex(int part, int index) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getAfterIndex(int part, int index) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getBeforeIndex(int part, int index) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public AttributeSet getCharacterAttribute(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getSelectionStart() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getSelectionEnd() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getSelectedText() throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    private Action cancelAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
            close();
            isCancelled = true;
        }
    };

    protected AccessibleContext accessibleContext;

    private static final int DEFAULT_MILLIS_TO_DECIDE = 500;

    private static final int DEFAULT_MILLIS_TO_POPUP = 2000;

    private int millisToDecideToPopup = DEFAULT_MILLIS_TO_DECIDE;

    private int millisToPopup = DEFAULT_MILLIS_TO_POPUP;

    private Timer shouldShowTimer;

    private int max;

    private int min;

    private int progress;

    private JProgressBar progressBar;

    private JDialog progressDialog;

    private Component parentComponent;

    private JLabel noteLabel;

    private Object message;

    private boolean shouldShow;

    private boolean isCancelled;

    public ProgressMonitor(Component parentComponent, Object message, String note, int min,
            int max) {
        this.parentComponent = parentComponent;
        this.message = message;
        noteLabel = new JLabel(note);
        this.min = min;
        progress = min;
        this.max = max;
        startShouldShowTimer();
    }

    public void setProgress(int progress) {
        int oldProgress = this.progress;
        if (progress >= max) {
            close();
        } else if (progress > this.progress) {
            this.progress = progress;
            if (shouldShow && !isCancelled && progressVisible(oldProgress, progress)) {
                if (progressDialog == null) {
                    showDialog();
                }
                progressBar.setValue(progress);
            }
        }
    }

    public void close() {
        if (progressDialog != null) {
            progressDialog.dispose();
            progressDialog = null;
        }
    }

    public int getMinimum() {
        return min;
    }

    public void setMinimum(int min) {
        this.min = min;
        if (progressBar != null) {
            progressBar.setMinimum(min);
        }
    }

    public int getMaximum() {
        return max;
    }

    public void setMaximum(int max) {
        this.max = max;
        if (progressBar != null) {
            progressBar.setMaximum(max);
        }
    }

    public boolean isCanceled() {
        return isCancelled;
    }

    public void setMillisToDecideToPopup(int millisToDecideToPopup) {
        this.millisToDecideToPopup = millisToDecideToPopup;
        if (shouldShowTimer != null) {
            shouldShowTimer.setDelay(millisToDecideToPopup);
        }
    }

    public int getMillisToDecideToPopup() {
        return millisToDecideToPopup;
    }

    public void setMillisToPopup(int millisToPopup) {
        this.millisToPopup = millisToPopup;
    }

    public int getMillisToPopup() {
        return millisToPopup;
    }

    public void setNote(String note) {
        noteLabel.setText(note);
    }

    public String getNote() {
        return noteLabel.getText();
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleProgressMonitor();
        }
        return accessibleContext;
    }

    private void startShouldShowTimer() {
        shouldShow = false;
        if (shouldShowTimer == null) {
            shouldShowTimer = new Timer(millisToDecideToPopup, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                    shouldShow = (max - min) * millisToDecideToPopup > millisToPopup
                            * (progress - min);
                }
            });
        }
        shouldShowTimer.setRepeats(false);
        shouldShowTimer.restart();
    }

    private void showDialog() {
        progressBar = new JProgressBar(min, max);
        JButton cancelButton = new JButton();
        cancelButton.setAction(cancelAction);
        cancelButton.setText(UIManager.getString("OptionPane.cancelButtonText"));
        cancelButton.setMnemonic(UIManager.getInt("OptionPane.cancelButtonMnemonic"));
        Object[] topPanel = { message, noteLabel, progressBar };
        Object[] bottomPanel = { cancelButton };
        JOptionPane dialogCreator = new JOptionPane(topPanel, JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, bottomPanel);
        progressDialog = dialogCreator.createDialog(parentComponent, UIManager
                .getString("ProgressMonitor.progressText"));
        progressDialog.setModal(false);
        progressDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAction.actionPerformed(null);
            }
        });
        progressDialog.setVisible(true);
    }

    private boolean progressVisible(int oldProgress, int newProgress) {
        return (newProgress - oldProgress) * 100 >= (max - min);
    }
}
