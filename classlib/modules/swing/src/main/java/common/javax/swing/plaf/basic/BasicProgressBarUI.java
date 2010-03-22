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
 * @author Dennis Ushakov
 */

package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ProgressBarUI;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class BasicProgressBarUI extends ProgressBarUI {

    public class ChangeHandler implements ChangeListener {
        public void stateChanged(final ChangeEvent event) {
            progressBar.repaint();
        }
    }

    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            String changedProperty = event.getPropertyName();

            if (StringConstants.PROGRESS_STRING_PAINTED_PROPERTY.equals(changedProperty)) {
                progressBar.revalidate();
            }
            if (StringConstants.ORIENTATION.equals(changedProperty)) {
                updateRotatedFont(progressBar.getFont());
                progressBar.revalidate();
            }
            if (StringConstants.FONT_PROPERTY_CHANGED.equals(changedProperty)) {
                updateRotatedFont((Font)event.getNewValue());
            }
            if (StringConstants.INDETERMINATE_PROPERTY.equals(changedProperty)) {
                if (progressBar.isIndeterminate()) {
                    setAnimationIndex(0);
                    startAnimationTimer();
                } else {
                    stopAnimationTimer();
                }
            }
            progressBar.repaint();
        }
    }

    protected JProgressBar progressBar;
    protected ChangeListener changeListener;
    protected Rectangle boxRect;

    private static final int NOTEXT_HEIGHT_ADDITION = 5;
    private static final int MIN_WIDTH = 10;
    private static final int STRING_PAINTED_CELL_LENGTH = 1;
    private static final int STRING_PAINTED_CELL_SPACING = 0;
    
    private int cellLength;
    private int cellSpacing;
    private PropertyChangeListener propertyChangeListener;
    private Color selectionBackground;
    private Color selectionForeground;

    private int cycleTime;
    private int repaintInterval;
    private int animationIndex;
    private int maxAnimationIndex;
    private Timer animationTimer;

    private Dimension verticalSize;
    private Dimension horizontalSize;
    private AffineTransform rotateFont;
    private Font rotatedFont;

    public static ComponentUI createUI(final JComponent c) {
        return new BasicProgressBarUI();
    }

    public void installUI(final JComponent c) {
        progressBar = (JProgressBar)c;
        installDefaults();
        installListeners();
    }

    public void uninstallUI(final JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        progressBar = null;
    }

    public void paint(final Graphics g, final JComponent c) {
        Color oldColor = g.getColor();

        if (progressBar.isIndeterminate()) {
            paintIndeterminate(g, progressBar);
        } else {
            paintDeterminate(g, progressBar);
        }

        if (progressBar.isStringPainted()) {
            String paintString = getPaintedString();
            Insets border = progressBar.getInsets();
            Rectangle innerArea = SwingUtilities.calculateInnerArea(progressBar, null);
            int actualWidth = innerArea.width;
            int actualHeight = innerArea.height;

            Point stringPosition = getStringPlacement(g, paintString, border.left, border.top,
                                                      actualWidth, actualHeight);
            paintString(g, stringPosition.x, stringPosition.y,  actualWidth, actualHeight,
                        getAmountFull(border, actualWidth, actualHeight), border);
        }
        g.setColor(oldColor);
    }

    public Dimension getPreferredSize(final JComponent c) {
        return new Dimension(calculateWidth(true), calculateHeight(true));
    }

    public Dimension getMinimumSize(final JComponent c) {
        return new Dimension(MIN_WIDTH, calculateHeight(true));
    }

    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Short.MAX_VALUE, calculateHeight(true));
    }

    protected void installDefaults() {
        rotateFont = new AffineTransform();
        rotateFont.rotate(Math.PI / 2);
        LookAndFeel.installColorsAndFont(progressBar, "ProgressBar.background", "ProgressBar.foreground", "ProgressBar.font");
        LookAndFeel.installBorder(progressBar, "ProgressBar.border");
        LookAndFeel.installProperty(progressBar, "opaque", Boolean.TRUE);
        selectionBackground = UIManager.getColor("ProgressBar.selectionBackground");
        selectionForeground = UIManager.getColor("ProgressBar.selectionForeground");

        cellLength = UIManager.getInt("ProgressBar.cellLength");
        cellSpacing = UIManager.getInt("ProgressBar.cellSpacing");
        
        cycleTime = UIManager.getInt("ProgressBar.cycleTime");
        repaintInterval = UIManager.getInt("ProgressBar.repaintInterval");
        maxAnimationIndex = cycleTime / repaintInterval;
        animationTimer = new Timer(repaintInterval, new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                incrementAnimationIndex();
                progressBar.repaint();
            }
        });

        verticalSize = (Dimension)UIManager.get("ProgressBar.verticalSize");
        horizontalSize = (Dimension)UIManager.get("ProgressBar.horizontalSize");
        updateRotatedFont(progressBar.getFont());

    }

    protected void uninstallDefaults() {
        Utilities.uninstallColorsAndFont(progressBar);
        LookAndFeel.uninstallBorder(progressBar);
    }

    protected void installListeners() {
        changeListener = new ChangeHandler();
        progressBar.addChangeListener(changeListener);
        propertyChangeListener = new PropertyChangeHandler();
        progressBar.addPropertyChangeListener(propertyChangeListener);
    }

    protected void uninstallListeners() {
        progressBar.removeChangeListener(changeListener);
        progressBar.removePropertyChangeListener(propertyChangeListener);
    }

    protected void startAnimationTimer() {
        if (animationTimer != null) {
            animationTimer.start();
        }
    }

    protected void stopAnimationTimer() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    protected Dimension getPreferredInnerHorizontal() {
        if (progressBar == null) {
            throw new NullPointerException();
        }
        return horizontalSize;
    }

    protected Dimension getPreferredInnerVertical() {
        if (progressBar == null) {
            throw new NullPointerException();
        }
        return verticalSize;
    }

    protected Color getSelectionForeground() {
        return selectionForeground;
    }

    protected Color getSelectionBackground() {
        return selectionBackground;
    }

    protected int getCellLength() {
        return !progressBar.isStringPainted() ? cellLength : STRING_PAINTED_CELL_LENGTH;        
    }

    protected void setCellLength(final int cellLen) {
        cellLength = cellLen;
    }

    protected int getCellSpacing() {
        return !progressBar.isStringPainted() ? cellSpacing : STRING_PAINTED_CELL_SPACING;
    }

    protected void setCellSpacing(final int cellSpace) {
        cellSpacing = cellSpace;
    }

    protected int getAmountFull(final Insets b, final int width, final int height) {
        return isVertical() ? (int)(height * progressBar.getPercentComplete()) :
                              (int)(width * progressBar.getPercentComplete());
    }

    protected Rectangle getBox(final Rectangle r) {
        boxRect = getFilledArea(r, 0);
        return boxRect;
    }

    protected int getBoxLength(final int length, final int otherDimension) {
        return length / 6;
    }

    protected void paintIndeterminate(final Graphics g, final JComponent c) {
        Rectangle innerArea = SwingUtilities.calculateInnerArea(progressBar, null);
        Rectangle filledArea = getFilledArea(innerArea, 0);

        g.setColor(c.getForeground());
        g.fillRect(filledArea.x, filledArea.y, filledArea.width, filledArea.height);
    }

    protected void paintDeterminate(final Graphics g, final JComponent c) {
        Insets insets = progressBar.getInsets();
        Rectangle innerArea = SwingUtilities.calculateInnerArea(progressBar, null);
        int actualWidth = innerArea.width;
        int actualHeight = innerArea.height;

        
        g.setColor(progressBar.getForeground());
        int amountFull = getAmountFull(insets, actualWidth, actualHeight);
        if (!isVertical()) {
            if (getCellSpacing() == 0) {
                g.fillRect(insets.left, insets.top, amountFull, actualHeight);    
            } else {
                g.clipRect(insets.left, insets.top, amountFull, actualHeight);
                for (int i = 0; i < amountFull; i += cellLength + cellSpacing) {
                    g.fillRect(i, insets.top, cellLength, actualHeight);
                }
            }
        } else {
            if (getCellSpacing() == 0) {
                g.fillRect(insets.left, insets.top - amountFull + actualHeight, actualWidth, amountFull);
            } else {
                g.clipRect(insets.left, insets.top - amountFull + actualHeight, actualWidth, amountFull);
                for (int i = 0; i < amountFull; i += cellLength + cellSpacing) {
                    g.fillRect(insets.left, insets.top - i + actualHeight, actualWidth, cellLength);
                }
            }
        }
    }

    protected void paintString(final Graphics g, final int x, final int y,
                               final int width, final int height, final int amountFull,  final Insets insets) {

        Rectangle innerArea = SwingUtilities.calculateInnerArea(progressBar, null);

        Rectangle filledArea;
        if (progressBar.isIndeterminate()) {
            filledArea = getBox(innerArea);
        } else  {
            filledArea = getFilledArea(innerArea, amountFull);
        }

        if (isVertical()) {
            paintString(g, filledArea, x, y, width, height, amountFull, insets, rotatedFont);
        } else {
            paintString(g, filledArea, x, y, width, height, amountFull, insets, progressBar.getFont());
        }
    }

    protected Point getStringPlacement(final Graphics g, final String progressString,
                                       final int x, final int y, final int width, final int height) {
        FontMetrics fm = Utilities.getFontMetrics(progressBar);
        Dimension size = Utilities.getStringSize(progressString, fm);
        size.height -= (fm.getAscent() - fm.getDescent());

        return isVertical() ? new Point(x + (width - size.height) / 2, y + (height - size.width) / 2) :
                              new Point(x + (width - size.width) / 2, y + (height + size.height) / 2);
    }

    protected int getAnimationIndex() {
        return animationIndex;
    }

    protected void setAnimationIndex(final int newValue) {
        if (progressBar == null) {
            throw new NullPointerException(Messages.getString("swing.03","progressBar")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        animationIndex = newValue % maxAnimationIndex;
    }

    protected void incrementAnimationIndex() {
        setAnimationIndex(getAnimationIndex() + 1);
    }

    private int calculateHeight(final boolean checkVertical) {
        if (checkVertical && isVertical()) {
            return calculateWidth(false);
        }
        Insets insets = progressBar.getInsets();
        int height = insets.top + insets.bottom + horizontalSize.height;
        if (progressBar.isStringPainted()) {
            height -= NOTEXT_HEIGHT_ADDITION;
            height += progressBar.getFontMetrics(progressBar.getFont()).getHeight();
        }
        return height;
    }

    private int calculateWidth(final boolean checkVertical) {
        if (checkVertical && isVertical()) {
            return calculateHeight(false);
        }
        Insets insets = progressBar.getInsets();
        int width = insets.left + insets.right + Utilities.getStringSize(getPaintedString(),
                                            progressBar.getFontMetrics(progressBar.getFont())).width;
        return Math.max(horizontalSize.width + insets.left + insets.right, width);
    }

    private String getPaintedString() {
        String result = progressBar.getString();
        return result != null ? result :
               progressBar.isIndeterminate() ? null :
                   Integer.toString((int)(progressBar.getPercentComplete() * 100)) + "%";
    }

    private boolean isVertical() {
        return progressBar.getOrientation() == JProgressBar.VERTICAL;
    }

    private void updateRotatedFont(final Font font) {
        if (font != null) {
            rotatedFont = font.deriveFont(rotateFont);
        }
    }

    private void paintString(final Graphics g, final Rectangle filledArea, final int x, final int y,
                                     final int width, final int height, final int amountFull,  final Insets insets,
                                     final Font font) {


        final Rectangle oldClip = g.getClipBounds();
        final String paintedString = getPaintedString();
        final FontMetrics fm = progressBar.getFontMetrics(font);

        Utilities.drawString(g, paintedString, x, y, fm, getSelectionBackground(), -1);

        g.clipRect(filledArea.x, filledArea.y, filledArea.width, filledArea.height);
        Utilities.drawString(g, paintedString, x, y, fm, getSelectionForeground(), -1);
        g.setClip(oldClip);
    }

    private Rectangle getFilledArea(final Rectangle innerArea, final int amountFull) {
        Rectangle result = new Rectangle();
        if (!progressBar.isIndeterminate()) {
            result.x = innerArea.x;
            if (isVertical()) {
                result.y = innerArea.y + innerArea.height - amountFull;
                result.width = innerArea.width;
                result.height = amountFull;
            } else {
                result.y = innerArea.y;
                result.width = amountFull;
                result.height = innerArea.height;
            }
        } else {
            if (isVertical()) {
                result.x = innerArea.x;
                result.y = innerArea.y + calculatePosition(innerArea.height);
                result.width = innerArea.width;
                result.height = getBoxLength(innerArea.height, innerArea.width);
            } else {
                result.x = innerArea.x + calculatePosition(innerArea.width);
                result.y = innerArea.y;
                result.width = getBoxLength(innerArea.width, innerArea.height);
                result.height = innerArea.height;
            }
        }
        return result;
    }

    private int calculatePosition(final int length) {
        int animationIndex = getAnimationIndex();
        return (length - getBoxLength(length, 0)) * 2 * Math.min(animationIndex, (maxAnimationIndex - animationIndex)) / maxAnimationIndex;
    }
}


