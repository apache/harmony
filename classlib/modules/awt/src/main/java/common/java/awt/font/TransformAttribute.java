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
import java.io.Serializable;

import org.apache.harmony.awt.internal.nls.Messages;

public final class TransformAttribute implements Serializable {
    private static final long serialVersionUID = 3356247357827709530L;

    // affine transform of this TransformAttribute instance
    private AffineTransform fTransform;

    public TransformAttribute(AffineTransform transform) {
        if (transform == null) {
            // awt.94=transform can not be null
            throw new IllegalArgumentException(Messages.getString("awt.94")); //$NON-NLS-1$
        }
        if (!transform.isIdentity()){
            this.fTransform = new AffineTransform(transform);
        }
    }

    public AffineTransform getTransform() {
        if (fTransform != null){
            return new AffineTransform(fTransform);
        }
        return new AffineTransform();
    }

    public boolean isIdentity() {
        return (fTransform == null);
    }

}

