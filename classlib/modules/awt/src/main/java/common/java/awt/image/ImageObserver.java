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
package java.awt.image;

import java.awt.Image;

public interface ImageObserver {

    public static final int WIDTH = 1;

    public static final int HEIGHT = 2;

    public static final int PROPERTIES = 4;

    public static final int SOMEBITS = 8;

    public static final int FRAMEBITS = 16;

    public static final int ALLBITS = 32;

    public static final int ERROR = 64;

    public static final int ABORT = 128;

    public boolean imageUpdate(Image img, int infoflags, int x, int y,
            int width, int height);

}

