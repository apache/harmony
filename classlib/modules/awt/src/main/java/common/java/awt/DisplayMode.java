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
 * @author Alexey A. Petrenko
 */
package java.awt;

public final class DisplayMode {
    private final int width;

    private final int height;

    private final int bitDepth;

    private final int refreshRate;

   /***************************************************************************
    *
    *  Constants
    *
    ***************************************************************************/

    public static final int BIT_DEPTH_MULTI = -1;

    public static final int REFRESH_RATE_UNKNOWN = 0;

   /***************************************************************************
    *
    *  Constructors
    *
    ***************************************************************************/

    public DisplayMode(int width, int height, int bitDepth, int refreshRate) {
        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.refreshRate = refreshRate;
    }


   /***************************************************************************
    *
    *  Public methods
    *
    ***************************************************************************/

    @Override
    public boolean equals(Object dm) {
        if (dm instanceof DisplayMode) {
            return equals((DisplayMode)dm);
        }
        return false;
    }

    public boolean equals(DisplayMode dm) {
        if (dm == null) {
            return false;
        }
        if (dm.bitDepth != bitDepth) {
            return false;
        }
        if (dm.refreshRate != refreshRate) {
            return false;
        }
        if (dm.width != width) {
            return false;
        }
        if (dm.height != height) {
            return false;
        }
        return true;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public int getHeight() {
        return height;
    }

    public int getRefreshRate() {
        return refreshRate;
    }

    public int getWidth() {
        return width;
    }
}
