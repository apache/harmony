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
package org.apache.harmony.awt;

/**
 * Single-shot relative timer class.
 * It ticks once and stops.
 */
public class SingleShotTimer extends RelativeTimer {

    /**
     * Constructor of new single-shot timer.
     *
     * @param delay - new timer's delay.
     * @param handler - new timer's handler. It's invoked on new timer's tick.
     */
    public SingleShotTimer(long delay, Runnable handler) {
        super(delay, handler);
    }

    /**
     * Gets this timer's delay.
     *
     * @return delay of this timer.
     */
    public long getDelay() {
        return interval;
    }

    @Override
    void handle() {
        super.handle();
        stop();
    }

}
