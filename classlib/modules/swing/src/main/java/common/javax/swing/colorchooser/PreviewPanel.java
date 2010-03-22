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
package javax.swing.colorchooser;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;

import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.harmony.x.swing.Utilities;

class PreviewPanel extends JPanel {
    
    private static final int BIG_SQUARE_SIZE = 13;
    private static final int SMALL_SQUARE_SIZE = 7;
    
    private static final int TOP_OFFSET = 10;
    private static final int TEXT_OFFSET = 10;
    
    private int sampleWidth;
    private int sampleHeight;
            
    private int swatchSize;
    private int swatchOffset;
    
    private Polygon oldColorPoly = new Polygon(new int[]{swatchOffset, swatchOffset + swatchSize, swatchOffset + swatchSize}, 
                                                              new int[]{TOP_OFFSET, TOP_OFFSET, TOP_OFFSET + swatchSize},
                                                              3);
    private Polygon newColorPoly = new Polygon(new int[]{swatchOffset, swatchOffset, swatchOffset + swatchSize}, 
                                                              new int[]{TOP_OFFSET, TOP_OFFSET + swatchSize, TOP_OFFSET + swatchSize},
                                                              3);       
    
    private Color previousColor;    
    
    public Dimension getPreferredSize() {
        initPanelSize();
        return new Dimension(sampleWidth * 2 + TEXT_OFFSET * 3 + swatchSize , 64);
    }
    
    public void paint(final Graphics graphics) {
        Color oldColor = graphics.getColor();
                
        if (previousColor == null) {
            previousColor = getForeground();
        }                
        
        super.paint(graphics);
        initPanelSize();
        paintTextSamples(graphics);
        paintSwatch(graphics);
        paintRectSamples(graphics);
        
        graphics.setColor(oldColor);
    }
    
    private void paintTextSamples(final Graphics graphics) {            
        Color color = getForeground();
        String textSample = UIManager.getString("ColorChooser.sampleText");
        
        graphics.setColor(color);
        graphics.fillRect(TEXT_OFFSET, TOP_OFFSET, sampleWidth, sampleHeight);
        graphics.fillRect(TEXT_OFFSET + sampleWidth + 1, TOP_OFFSET, sampleWidth, sampleHeight);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(TEXT_OFFSET, TOP_OFFSET + sampleHeight + 1, sampleWidth, sampleHeight);
        

        Point stringPosition = getStringPlacement(textSample, TEXT_OFFSET, TOP_OFFSET,
                                                  sampleWidth - 25, sampleHeight);
        graphics.drawString(textSample, stringPosition.x, stringPosition.y);
        stringPosition = getStringPlacement(textSample, TEXT_OFFSET + sampleWidth + 25, TOP_OFFSET,
                                            sampleWidth - 25, sampleHeight);
        graphics.setColor(Color.BLACK);
        graphics.drawString(textSample, stringPosition.x, stringPosition.y);
        graphics.setColor(color);
        stringPosition = getStringPlacement(textSample, TEXT_OFFSET, TOP_OFFSET + sampleHeight,
                                            sampleWidth - 25, sampleHeight);
        graphics.drawString(textSample, stringPosition.x, stringPosition.y);
        stringPosition = getStringPlacement(textSample, TEXT_OFFSET + sampleWidth + 25, 
                                            TOP_OFFSET + sampleHeight, sampleWidth - 25, sampleHeight);
        graphics.drawString(textSample, stringPosition.x, stringPosition.y);                                    
        
        graphics.setColor(Color.BLACK);
        graphics.drawRect(TEXT_OFFSET, TOP_OFFSET, sampleWidth * 2, sampleHeight * 2);
        graphics.drawLine(TEXT_OFFSET + sampleWidth, TOP_OFFSET, TEXT_OFFSET + sampleWidth, TOP_OFFSET + sampleHeight * 2);
        graphics.drawLine(TEXT_OFFSET, TOP_OFFSET + sampleHeight, TEXT_OFFSET + sampleWidth * 2, TOP_OFFSET + sampleHeight);
    }
    
    private void paintRectSamples(final Graphics graphics) {
        Color color = getForeground();
        
        graphics.setColor(Color.WHITE);
        int bigSquareRelativeY = (sampleHeight - BIG_SQUARE_SIZE + 1) / 2;
        graphics.fillRect(TEXT_OFFSET + sampleWidth - 19, TOP_OFFSET + bigSquareRelativeY,
                          BIG_SQUARE_SIZE,  BIG_SQUARE_SIZE);            
        
        graphics.setColor(Color.BLACK);
        graphics.fillRect(TEXT_OFFSET + sampleWidth + 6, TOP_OFFSET + bigSquareRelativeY,
                          BIG_SQUARE_SIZE,  BIG_SQUARE_SIZE);            
        graphics.fillRect(TEXT_OFFSET + sampleWidth - 16, TOP_OFFSET + sampleHeight +
                          bigSquareRelativeY + SMALL_SQUARE_SIZE / 2,
                          SMALL_SQUARE_SIZE,  SMALL_SQUARE_SIZE);
        
        graphics.setColor(color);
        graphics.fillRect(TEXT_OFFSET + sampleWidth - 19, TOP_OFFSET + sampleHeight 
                          + bigSquareRelativeY, BIG_SQUARE_SIZE,  BIG_SQUARE_SIZE);
        graphics.fillRect(TEXT_OFFSET + sampleWidth + 6, TOP_OFFSET + sampleHeight +
                          bigSquareRelativeY, BIG_SQUARE_SIZE,  BIG_SQUARE_SIZE);
        
        graphics.setColor(Color.WHITE);
        graphics.fillRect(TEXT_OFFSET + sampleWidth  + 9, TOP_OFFSET + sampleHeight + 
                          bigSquareRelativeY + SMALL_SQUARE_SIZE / 2,
                          SMALL_SQUARE_SIZE,  SMALL_SQUARE_SIZE);
        
        graphics.setColor(Color.BLACK);
        graphics.fillRect(TEXT_OFFSET + sampleWidth - 16, TOP_OFFSET + sampleHeight +
                          bigSquareRelativeY + SMALL_SQUARE_SIZE / 2,
                          SMALL_SQUARE_SIZE,  SMALL_SQUARE_SIZE);
        
        graphics.setColor(color);
        graphics.fillRect(TEXT_OFFSET + sampleWidth  + 9, TOP_OFFSET + 
                          bigSquareRelativeY + SMALL_SQUARE_SIZE / 2,
                          SMALL_SQUARE_SIZE,  SMALL_SQUARE_SIZE);
        graphics.fillRect(TEXT_OFFSET + sampleWidth - 16, TOP_OFFSET +
                          bigSquareRelativeY + SMALL_SQUARE_SIZE / 2,
                          SMALL_SQUARE_SIZE,  SMALL_SQUARE_SIZE);
                                
    }
    
    private void paintSwatch(final Graphics graphics) {
        Color color = getForeground();
        
        graphics.setColor(previousColor);
        graphics.fillPolygon(oldColorPoly);
        graphics.setColor(color);
        graphics.fillPolygon(newColorPoly);
        graphics.setColor(Color.BLACK);
        graphics.drawRect(swatchOffset, TOP_OFFSET, swatchSize, swatchSize);
    }
    
    private Point getStringPlacement(final String string, final int x, final int y, final int width, final int height) {
        FontMetrics fm = Utilities.getFontMetrics(this);
        Dimension size = Utilities.getStringSize(string, fm);

        return new Point(x + (width - size.width) / 2, y + fm.getHeight() + (height - fm.getHeight()) / 2);
    }
    
    private void initPanelSize() {
        String textSample = UIManager.getString("ColorChooser.sampleText");
        Dimension stringSize = Utilities.getStringSize(textSample, this.getFontMetrics(this.getFont()));
        sampleWidth = stringSize.width + BIG_SQUARE_SIZE + 20;
        sampleHeight = stringSize.height + 4;
                
        swatchSize = sampleHeight * 2;
        swatchOffset = sampleWidth * 2 + TEXT_OFFSET * 2;
        
        oldColorPoly = new Polygon(new int[]{swatchOffset,swatchOffset + swatchSize, swatchOffset + swatchSize},
                                   new int[]{TOP_OFFSET, TOP_OFFSET, TOP_OFFSET + swatchSize},
                                   3);
        newColorPoly = new Polygon(new int[]{swatchOffset, swatchOffset, swatchOffset + swatchSize}, 
                                   new int[]{TOP_OFFSET, TOP_OFFSET + swatchSize, TOP_OFFSET + swatchSize},
                                   3);       
    }
}