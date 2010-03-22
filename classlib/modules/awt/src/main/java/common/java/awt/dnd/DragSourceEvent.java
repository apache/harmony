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
package java.awt.dnd;

import java.awt.Point;
import java.util.EventObject;

import org.apache.harmony.awt.internal.nls.Messages;

public class DragSourceEvent extends EventObject {

    private static final long serialVersionUID = -763287114604032641L;
    private final DragSourceContext context;
    private final Point location;

    public DragSourceEvent(DragSourceContext dsc) {
        super(dsc);

        context = dsc;
        location = null;
    }

    public DragSourceEvent(DragSourceContext dsc, int x, int y) {
        super(dsc);

        if (dsc == null) {
            // awt.18A=Context is null.
            throw new IllegalArgumentException(Messages.getString("awt.18A")); //$NON-NLS-1$
        }

        context = dsc;
        location = new Point(x, y);
    }

    public DragSourceContext getDragSourceContext() {
        return context;
    }

    public Point getLocation() {
        if (location != null) {
            return new Point(location);
        }

        return null;
    }

    public int getY() {
        if (location != null) {
            return location.y;
        }

        return 0;
    }

    public int getX() {
        if (location != null) {
            return location.x;
        }

        return 0;
    }

}
