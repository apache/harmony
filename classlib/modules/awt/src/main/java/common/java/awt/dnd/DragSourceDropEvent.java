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

public class DragSourceDropEvent extends DragSourceEvent {

    private static final long serialVersionUID = -5571321229470821891L;
    private int action;
    private boolean success;

    public DragSourceDropEvent(DragSourceContext dsc, int action, boolean success, int x, int y) {
        super(dsc, x, y);

        initFields(action, success);
    }

    public DragSourceDropEvent(DragSourceContext dsc, int action, boolean success) {
        super(dsc);

        initFields(action, success);
    }

    public DragSourceDropEvent(DragSourceContext dsc) {
        this(dsc, DnDConstants.ACTION_NONE, false);
    }

    private void initFields(int action, boolean success) {
        this.success = success;
        this.action = action;
    }

    public boolean getDropSuccess() {
        return success;
    }

    public int getDropAction() {
        int ret = DnDConstants.ACTION_NONE;

        if (success) {
            ret = (action & getDragSourceContext().getSourceActions());
        }

        return ret;
    }

}
