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
 * Periodic relative timer class.
 * It ticks with given period in infinite loop unless gets stopped.
 */
public class PeriodicTimer extends RelativeTimer {

    /**
     * Constructor of new periodic timer.
     *
     * @param period - new timer's period.
     * @param handler - new timer's handler. It's invoked every time new timer ticks.
     */
    public PeriodicTimer(long period, Runnable handler) {
        super(period, handler);
    }

    /**
     * Gets this timer's period.
     *
     * @return period of this timer.
     */
    public long getPeriod() {
        return interval;
    }

}
