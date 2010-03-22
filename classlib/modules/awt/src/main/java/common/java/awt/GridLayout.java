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
 * @author Michael Danilov
 */
package java.awt;

import java.io.Serializable;

import org.apache.harmony.awt.internal.nls.Messages;

public class GridLayout implements LayoutManager, Serializable {
    private static final long serialVersionUID = -7411804673224730901L;   

    private static final int DEFAULT_GAP = 0;
    private static final int DEFAULT_COLS_NUMBER = 0;
    private static final int DEFAULT_ROWS_NUMBER = 1;

    private final Toolkit toolkit = Toolkit.getDefaultToolkit();

    private int rows;
    private int columns;
    private int vGap;
    private int hGap;

    private transient Component[] components;

    public GridLayout() {
        this(DEFAULT_ROWS_NUMBER, DEFAULT_COLS_NUMBER, DEFAULT_GAP, DEFAULT_GAP);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public GridLayout(int rows, int cols) {
        this(rows, cols, DEFAULT_GAP, DEFAULT_GAP);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public GridLayout(int rows, int cols, int hgap, int vgap) {
        toolkit.lockAWT();
        try {
            if ((rows == 0) && (cols == 0)) {
                // awt.75=rows and cols cannot both be zero
                throw new IllegalArgumentException(Messages.getString("awt.75")); //$NON-NLS-1$
            }
            // awt.76=rows and cols cannot be negative
            assert (cols >= 0) && (rows >= 0) : Messages.getString("awt.76"); //$NON-NLS-1$

            this.rows = rows;
            columns = cols;
            vGap = vgap;
            hGap = hgap;

        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public String toString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new GridLayout());
         */

        toolkit.lockAWT();
        try {
            return (getClass().getName() + "[hgap=" + hGap + ",vgap=" + vGap //$NON-NLS-1$ //$NON-NLS-2$
                    + ",rows=" + rows + ",cols=" + columns + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getHgap() {
        toolkit.lockAWT();
        try {
            return hGap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getVgap() {
        toolkit.lockAWT();
        try {
            return vGap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setHgap(int hgap) {
        toolkit.lockAWT();
        try {
            hGap = hgap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setVgap(int vgap) {
        toolkit.lockAWT();
        try {
            vGap = vgap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getColumns() {
        toolkit.lockAWT();
        try {
            return columns;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getRows() {
        toolkit.lockAWT();
        try {
            return rows;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setColumns(int cols) {
        toolkit.lockAWT();
        try {
            if ((rows == 0) && (cols == 0)) {
                // awt.75=rows and cols cannot both be zero
                throw new IllegalArgumentException(Messages.getString("awt.75")); //$NON-NLS-1$
            }            

            columns = cols;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setRows(int rows) {
        toolkit.lockAWT();
        try {
            if ((rows == 0) && (columns == 0)) {
                // awt.75=rows and cols cannot both be zero
                throw new IllegalArgumentException(Messages.getString("awt.75")); //$NON-NLS-1$
            }            

            this.rows = rows;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void addLayoutComponent(String name, Component comp) {
        // take no action
    }

    public void removeLayoutComponent(Component comp) {
        // take no action
    }

    public void layoutContainer(Container parent) {
        toolkit.lockAWT();
        try {
            components = parent.getComponents();
            if (components.length == 0) {
                return;
            }
            Rectangle clientRect = parent.getClient();
            if (clientRect.isEmpty()) {
                return;
            }

            Dimension gridSize = calculateGrid();

            fillGrid(gridSize.width, gridSize.height, clientRect,
                    parent.getComponentOrientation().isLeftToRight());
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension minimumLayoutSize(Container parent) {
        toolkit.lockAWT();
        try {
            return parent.addInsets(layoutSize(parent, false));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension preferredLayoutSize(Container parent) {
        toolkit.lockAWT();
        try {
            return parent.addInsets(layoutSize(parent, true));
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Calculates real grid size from size given in constructor
     * @return true number of columns, rows in grid
     */
    private Dimension calculateGrid() {
        int trueCols = Math.max(0, columns);
        int trueRows = Math.max(0, rows);

        if (trueRows == 0) {
            trueCols = columns;
//            trueRows = (int) Math.ceil((double) components.length/(double) trueCols);
            trueRows = components.length / trueCols;
            if (components.length % trueCols > 0) {
                trueRows++;
            }
        } else {
            trueRows = rows;
//            trueCols = (int) Math.ceil((double) components.length/(double) trueRows);
            trueCols = components.length / trueRows;
            if (components.length % trueRows > 0) {
                trueCols++;
            }
        }

        return new Dimension(trueCols, trueRows);
    }

    /**
     * Calculates & sets components bounds
     * @param trueCols number of columns in grid
     * @param trueRows number of rows in grid
     * @param clientRect rectangle to fit components into
     * @param l2r true if component orientation is left to right,
     * false otherwise
     */
    private void fillGrid(int trueCols, int trueRows, Rectangle clientRect, boolean l2r) {
        int colsWidths[] = new int[trueCols];
        int colsOffsets[] = new int[trueCols];
        int rowsHeights[] = new int[trueRows];
        int rowsOffsets[] = new int[trueRows];

        spreadLength(clientRect.width, clientRect.x, 
                     hGap, colsWidths, colsOffsets);
        spreadLength(clientRect.height, clientRect.y, 
                     vGap, rowsHeights, rowsOffsets);

        exit: for (int i = 0, n = 0; i < trueRows; i++) {
            for (int j = 0; j < trueCols; j++) {
                int trueCol = (l2r ? j : trueCols - j - 1);

                components[n].setBounds(colsOffsets[trueCol], rowsOffsets[i],
                                        colsWidths[trueCol], rowsHeights[i]);


                if (++n == components.length) {
                    break exit;
                }
            }
        }
    }

    /**
     * Computes & sets lengths & offsets of grid rows/columns
     * @param length size of space to be distributed between rows/columns
     * @param offset starting coordinate
     * @param gap gap between each of the rows/columns
     * @param lengths array of lengths of rows/columns to be set(out parameter)
     * @param offsets array of offsets of rows/columns to be set(out parameter)
     */
    private void spreadLength(int length, int offset, int gap, 
                              int lengths[], int offsets[]) {
        int n = lengths.length;
        int clearLength = length - (n - 1) * gap;

        for (int i = 0, sharedLength = 0, accumGap = offset; i < n; i++, accumGap += gap) {
            int curLength;

            curLength = (int) (clearLength * ((double) (i + 1) / (double) n)) - sharedLength;
            lengths[i] = curLength;
            offsets[i] = sharedLength + accumGap;
            sharedLength += curLength;
        }
    }

    /**
     * Computes size necessary to layout all
     * components in container. Insets are not taken into account.
     * @param container in which to do the layout
     * @param preferred layout size is determined with components
     * having preffered sizes if true, minimum sizes otherwise
     * @return layout dimensions
     */
    private Dimension layoutSize(Container parent, boolean preferred) {
        components = parent.getComponents();
        if (components.length == 0) {
            return new Dimension();
        }
        Dimension gridSize = calculateGrid();
        int maxWidth = 0;
        int maxHeight = 0;

        for (Component element : components) {
            Dimension compSize = (preferred ?
                    element.getPreferredSize() :
                    element.getMinimumSize());

            maxWidth = Math.max(maxWidth, compSize.width);
            maxHeight = Math.max(maxHeight, compSize.height);
        }

        int width = maxWidth * gridSize.width + hGap * (gridSize.width - 1);
        int height = maxHeight * gridSize.height + vGap * (gridSize.height - 1);

        return new Dimension(width, height);
    }

}
