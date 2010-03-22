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
 * @author Anton Avtamonov
 */
package javax.swing.plaf.basic;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

final class DnDMouseHelper {
    private final JComponent dndComponent;

    private boolean dragStarted;
    private boolean readyForDrag;
    private boolean processedOnPress;
    private boolean shouldProcessOnRelease;

    public DnDMouseHelper(final JComponent c) {
        dndComponent = c;
    }

    public void mousePressed(final MouseEvent e,
                             final boolean dragEnabled,
                             final boolean clickedToItem,
                             final boolean itemSelected) {
        processedOnPress = dragEnabled && clickedToItem && !itemSelected;
        readyForDrag = dragEnabled && clickedToItem && itemSelected;
        shouldProcessOnRelease = dragEnabled && !processedOnPress && !dragStarted;
    }

    public boolean shouldProcessOnRelease() {
        return shouldProcessOnRelease;
    }

    public void mouseReleased(final MouseEvent e) {
        dragStarted = false;
    }

    public void mouseDragged(final MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)
            && readyForDrag
            && !dragStarted
            && dndComponent.getTransferHandler() != null) {

            dndComponent.getTransferHandler().exportAsDrag(dndComponent, e, TransferHandler.COPY);
            dragStarted = true;
            shouldProcessOnRelease = false;
        }
    }

    public boolean isDndStarted() {
        return dragStarted;
    }
}
