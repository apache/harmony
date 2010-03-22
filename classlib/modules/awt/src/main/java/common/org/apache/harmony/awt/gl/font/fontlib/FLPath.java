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
package org.apache.harmony.awt.gl.font.fontlib;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.NoSuchElementException;

import org.apache.harmony.awt.internal.nls.Messages;

final public class FLPath implements PathIterator {
    
    /**
     * The space amount in points buffer for different segmenet's types
     */
    /*private static int pointShift[] = {
            0,  // CLOSE
            2,  // LINETO            
            2,  // MOVETO
            6,  // CUBICTO
            4}; // QUADTO*/
            
    //General path
    static int pointShift[] = {
        2,  // MOVETO
        2,  // LINETO
        4,  // QUADTO
        6,  // CUBICTO
        0}; // CLOSE

            

    /**
     * The current cursor position in types buffer
     */
    int commandsIndex;
    
    /**
     * The current cursor position in points buffer
     */
    int pointIndex = 0;
    
    private int size;
    
    private final FLOutline outline = new FLOutline(); 
    
    FLPath(long glyphPointer) {        
        getShape(outline, glyphPointer);
        
        size = outline.commands.length;
    }

    public int getWindingRule() {
        return PathIterator.WIND_EVEN_ODD;
    }

    public boolean isDone() {
        return commandsIndex >= size;
    }

    public void next() {
        commandsIndex++;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            // awt.4B=Iterator out of bounds
            throw new NoSuchElementException(Messages.getString("awt.4B")); //$NON-NLS-1$
        }
        int type = outline.commands[commandsIndex];
        int count = pointShift[type];
        for (int i = 0; i < count; i++) {
            coords[i] = outline.points[pointIndex + i];
        }
        pointIndex += count;
        return type;
    }

    public int currentSegment(float[] coords) {
        if (isDone()) {
            // awt.4B=Iterator out of bounds
            throw new NoSuchElementException(Messages.getString("awt.4B")); //$NON-NLS-1$
        }        
        
        int type = outline.commands[commandsIndex];
        int count = pointShift[type];
        
        System.arraycopy(outline.points, pointIndex, coords, 0, count);
        pointIndex += count;
        return type;
    }
    
    Shape getShape() {
        GeneralPath gp = new GeneralPath();
        
        gp.append(this, false);
        
        return gp;        
    }
    
    private native void getShape(FLOutline outline, long glyphPointer);
}
