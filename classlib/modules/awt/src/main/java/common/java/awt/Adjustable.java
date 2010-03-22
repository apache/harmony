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
 * @author Pavel Dolgov
 */
package java.awt;

import java.awt.event.AdjustmentListener;

public interface Adjustable {

    public static final int HORIZONTAL = 0;

    public static final int VERTICAL = 1;

    public static final int NO_ORIENTATION = 2;

    public int getValue();

    public void setValue(int a0);

    public void addAdjustmentListener(AdjustmentListener a0);

    public int getBlockIncrement();

    public int getMaximum();

    public int getMinimum();

    public int getOrientation();

    public int getUnitIncrement();

    public int getVisibleAmount();

    public void removeAdjustmentListener(AdjustmentListener a0);

    public void setBlockIncrement(int a0);

    public void setMaximum(int a0);

    public void setMinimum(int a0);

    public void setUnitIncrement(int a0);

    public void setVisibleAmount(int a0);

}

