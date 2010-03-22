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

public class DropTargetEvent extends EventObject {

    private static final long serialVersionUID = 2821229066521922993L;

    protected DropTargetContext context;

    private int userAction;
    private int sourceAction;
    private Point location;

    public DropTargetEvent(DropTargetContext dtc) {
        super(dtc.getDropTarget());

        context = dtc;
    }

    public DropTargetContext getDropTargetContext() {
        return context;
    }

    DropTargetEvent(DropTargetContext dtc, Point location,
            int userAction, int sourceAction)
    {
        this(dtc);

        if (!DnDConstants.isValidAction(userAction)) {
            // awt.177=Invalid user action.
            throw new IllegalArgumentException(Messages.getString("awt.177")); //$NON-NLS-1$
        }
        if (!DnDConstants.isValidAction(sourceAction)) {
            // awt.178=Invalid source action.
            throw new IllegalArgumentException(Messages.getString("awt.178")); //$NON-NLS-1$
        }

        this.location = (Point) location.clone();
        this.userAction = userAction;
        this.sourceAction = sourceAction;
    }

    Point getLocation() {
        return new Point(location);
    }

    int getSourceAction() {
        return sourceAction;
    }

    int getUserAction() {
        return userAction;
    }

}
