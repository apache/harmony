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
 * @author Igor V. Stolyarov
 */
package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

public class RenderContext implements Cloneable {

    AffineTransform transform;
    Shape aoi;
    RenderingHints hints;

    public RenderContext(AffineTransform usr2dev, Shape aoi, RenderingHints hints) {
        this.transform = (AffineTransform)usr2dev.clone();
        this.aoi = aoi;
        this.hints = hints;
    }

    public RenderContext(AffineTransform usr2dev, Shape aoi) {
        this(usr2dev, aoi, null);
    }

    public RenderContext(AffineTransform usr2dev, RenderingHints hints) {
        this(usr2dev, null, hints);
    }

    public RenderContext(AffineTransform usr2dev) {
        this(usr2dev, null, null);
    }

    @Override
    public Object clone() {
        return new RenderContext(transform, aoi, hints);
    }

    public void setTransform(AffineTransform newTransform) {
        transform = (AffineTransform)newTransform.clone();
    }

    /**
     * @deprecated
     */
    @Deprecated    
    public void preConcetenateTransform(AffineTransform modTransform) {
        preConcatenateTransform(modTransform);
    }

    public void preConcatenateTransform(AffineTransform modTransform) {
        transform.preConcatenate(modTransform);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void concetenateTransform(AffineTransform modTransform) {
        concatenateTransform(modTransform);
    }

    public void concatenateTransform(AffineTransform modTransform) {
        transform.concatenate(modTransform);
    }

    public AffineTransform getTransform() {
        return (AffineTransform)transform.clone();
    }

    public void setAreaOfInterest(Shape newAoi) {
        aoi = newAoi;
    }

    public Shape getAreaOfInterest() {
        return aoi;
    }

    public void setRenderingHints(RenderingHints hints) {
        this.hints = hints;
    }

    public RenderingHints getRenderingHints() {
        return hints;
    }
}
