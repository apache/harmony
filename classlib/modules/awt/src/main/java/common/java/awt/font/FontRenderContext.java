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
 * @author Ilya S. Okomin
 */
package java.awt.font;

import java.awt.geom.AffineTransform;

public class FontRenderContext {

    // Affine transform of this mode
    private AffineTransform transform;

    // Is the anti-aliased mode used
    private boolean fAntiAliased;

    // Is the fractional metrics used
    private boolean fFractionalMetrics;


    public FontRenderContext(AffineTransform trans, boolean antiAliased, 
            boolean usesFractionalMetrics) {
        if (trans != null){
            transform = new AffineTransform(trans);
        }
        fAntiAliased = antiAliased;
        fFractionalMetrics = usesFractionalMetrics;
    }

    protected FontRenderContext() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj != null) {
            try {
                return equals((FontRenderContext) obj);
            } catch (ClassCastException e) {
                return false;
            }
        }
        return false;

    }

    public AffineTransform getTransform() {
        if (transform != null){
            return new AffineTransform(transform);
        }
        return new AffineTransform();
    }

    public boolean equals(FontRenderContext frc) {
        if (this == frc){
            return true;
        }

        if (frc == null){
            return false;
        }

        if (!frc.getTransform().equals(this.getTransform()) &&
            !frc.isAntiAliased() == this.fAntiAliased &&
            !frc.usesFractionalMetrics() == this.fFractionalMetrics){
            return false;
        }
        return true;
    }

    public boolean usesFractionalMetrics() {
        return this.fFractionalMetrics;
    }

    public boolean isAntiAliased() {
        return this.fAntiAliased;
    }

    @Override
    public int hashCode() {
        return this.getTransform().hashCode() ^
                new Boolean(this.fFractionalMetrics).hashCode() ^
                new Boolean(this.fAntiAliased).hashCode();
    }

}

