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
 * @author Sergey I. Salishev
 */
package javax.imageio;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * @author Sergey I. Salishev
 */
public class ImageReadParam extends IIOParam {

    protected boolean canSetSourceRenderSize;
    protected BufferedImage destination;
    protected int[] destinationBands;
    protected int minProgressivePass;
    protected int numProgressivePasses;
    protected Dimension sourceRenderSize;

    public boolean canSetSourceRenderSize() {
        return canSetSourceRenderSize;
    }

    public BufferedImage getDestination() {
        return destination;
    }

    public int[] getDestinationBands() {
        return destinationBands;
    }

    public int getSourceMaxProgressivePass() {
        if (getSourceNumProgressivePasses() == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return getSourceMinProgressivePass() + getSourceNumProgressivePasses() - 1;
    }

    public int getSourceMinProgressivePass() {
        return minProgressivePass;
    }

    public int getSourceNumProgressivePasses() {
        return numProgressivePasses;
    }

    public Dimension getSourceRenderSize() {
        return sourceRenderSize;
    }

    public void setDestination(BufferedImage destination) {
        this.destination = destination;
    }

    public void setDestinationBands(int[] destinationBands) {
        this.destinationBands = destinationBands;
    }

    @Override
    public void setDestinationType(ImageTypeSpecifier destinationType) {
        this.destinationType = destinationType;
    }

    public void setSourceProgressivePasses(int minPass, int numPasses) {
        minProgressivePass = minPass;
        numProgressivePasses = numPasses;
    }

    public void setSourceRenderSize(Dimension size) throws UnsupportedOperationException {
        if (!canSetSourceRenderSize) {
            throw new UnsupportedOperationException("can't set source renderer size");
        }
        sourceRenderSize = size;        
    }
}

