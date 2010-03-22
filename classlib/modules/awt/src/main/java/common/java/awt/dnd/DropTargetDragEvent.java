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

package java.awt.dnd;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;

public class DropTargetDragEvent extends DropTargetEvent {

    private static final long serialVersionUID = -8422265619058953682L;

    public DropTargetDragEvent(DropTargetContext dtc, Point cursorLocn, int dropAction,
            int srcActions) {
        super(dtc, cursorLocn, dropAction, srcActions);
    }

    @Override
    public Point getLocation() {
        return super.getLocation();
    }

    public int getSourceActions() {
        return super.getSourceAction();
    }

    public int getDropAction() {
        return super.getUserAction();
    }

    public void acceptDrag(int dragOperation) {
        context.acceptDrag(dragOperation);
    }

    public void rejectDrag() {
        context.rejectDrag();
    }

    public List<DataFlavor> getCurrentDataFlavorsAsList() {
        return context.getCurrentDataFlavorsAsList();
    }

    public boolean isDataFlavorSupported(DataFlavor df) {
        return context.isDataFlavorSupported(df);
    }

    public DataFlavor[] getCurrentDataFlavors() {
        return context.getCurrentDataFlavors();
    }

    public Transferable getTransferable() {
        return context.getTransferable();
    }
}
