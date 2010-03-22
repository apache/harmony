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
 * @author Rustem V. Rafikov
 */
package javax.imageio;

import java.awt.*;

public abstract class IIOParam {
    protected Rectangle sourceRegion;
    protected int sourceXSubsampling = 1;
    protected int sourceYSubsampling = 1;
    protected int subsamplingXOffset;
    protected int subsamplingYOffset;
    protected int[] sourceBands;
    protected ImageTypeSpecifier destinationType;
    protected Point destinationOffset = new Point(0, 0);
    protected IIOParamController defaultController;
    protected IIOParamController controller;

    protected IIOParam() {}

    public void setSourceRegion(Rectangle sourceRegion) {
        if (sourceRegion != null) {
            if (sourceRegion.x < 0) {
                throw new IllegalArgumentException("x < 0");
            }
            if (sourceRegion.y < 0) {
                throw new IllegalArgumentException("y < 0");
            }
            if (sourceRegion.width <= 0) {
                throw new IllegalArgumentException("width <= 0");
            }
            if (sourceRegion.height <= 0) {
                throw new IllegalArgumentException("height <= 0");
            }

            if (sourceRegion.width <= subsamplingXOffset) {
                throw new IllegalArgumentException("width <= subsamplingXOffset");
            }

            if (sourceRegion.height <= subsamplingYOffset) {
                throw new IllegalArgumentException("height <= subsamplingXOffset");
            }
            //-- clone it to avoid unexpected modifications
            this.sourceRegion = (Rectangle) sourceRegion.clone();
        } else {
            this.sourceRegion = null;
        }
    }

    public Rectangle getSourceRegion() {
        if (sourceRegion == null) {
            return null;
        }
        //-- clone it to avoid unexpected modifications
        return (Rectangle) sourceRegion.clone();
    }

    public void setSourceSubsampling(int sourceXSubsampling,
                                 int sourceYSubsampling,
                                 int subsamplingXOffset,
                                 int subsamplingYOffset) {

        if (sourceXSubsampling <= 0) {
            throw new IllegalArgumentException("sourceXSubsampling <= 0");
        }
        if (sourceYSubsampling <= 0) {
            throw new IllegalArgumentException("sourceYSubsampling <= 0");
        }

        if ((subsamplingXOffset < 0) || (subsamplingXOffset >= sourceXSubsampling)) {
            throw new IllegalArgumentException("subsamplingXOffset is wrong");
        }

        if ((subsamplingYOffset < 0) || (subsamplingYOffset >= sourceYSubsampling)) {
            throw new IllegalArgumentException("subsamplingYOffset is wrong");
        }

        //-- does region contain pixels
        if (sourceRegion != null) {
            if (sourceRegion.width <= subsamplingXOffset ||
                    sourceRegion.height <= subsamplingYOffset) {
                throw new IllegalArgumentException("there are no pixels in region");
            }
        }

        this.sourceXSubsampling = sourceXSubsampling;
        this.sourceYSubsampling = sourceYSubsampling;
        this.subsamplingXOffset = subsamplingXOffset;
        this.subsamplingYOffset = subsamplingYOffset;
    }

    public int getSourceXSubsampling() {
        return sourceXSubsampling;
    }

    public int getSourceYSubsampling() {
        return sourceYSubsampling;
    }

    public int getSubsamplingXOffset() {
        return subsamplingXOffset;
    }

    public int getSubsamplingYOffset() {
        return subsamplingYOffset;
    }

    public void setSourceBands(final int[] sourceBands) {
        if (sourceBands == null) {
            this.sourceBands = null;
        } else {
            for (int i = 0; i < sourceBands.length; i++) {
                if (sourceBands[i] < 0) {
                    throw new IllegalArgumentException("negative value");
                }
                
                for (int j = i + 1; j < sourceBands.length; j++) {
                    if (sourceBands[i] == sourceBands[j]) {
                        throw new IllegalArgumentException("duplicate value");
                    }
                }
            }
            
            this.sourceBands = sourceBands.clone();
        }
    }

    public int[] getSourceBands() {
        return (sourceBands != null) ? sourceBands.clone() : null;
    }

    public void setDestinationType(final ImageTypeSpecifier destinationType) {
        this.destinationType = destinationType;
    }

    public ImageTypeSpecifier getDestinationType() {
        return destinationType;
    }

    public void setDestinationOffset(Point destinationOffset) {
        if (destinationOffset == null) {
            throw new IllegalArgumentException("destinationOffset == null!");
        }
        
        this.destinationOffset = (Point) destinationOffset.clone();
    }

    public Point getDestinationOffset() {
        return (Point) destinationOffset.clone();        
    }

    public void setController(final IIOParamController controller) {
        this.controller = controller;
    }

    public IIOParamController getController(){
        return controller;
    }

    public IIOParamController getDefaultController() {
        return defaultController;
    }

    public boolean hasController() {
        return (controller != null);
    }

    public boolean activateController() {
        final IIOParamController controller = getController();
        
        if (controller == null) {
            throw new IllegalStateException("controller wasn't set");
        }
        
        return controller.activate(this);
    }
}
