/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.lang.management;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LoggingMXBean;

/**
 * Runtime type for {@link java.util.logging.LoggingMXBean}.
 * 
 * @since 1.5
 */
public class LoggingMXBeanImpl extends DynamicMXBeanImpl implements
        LoggingMXBean {

    private static LoggingMXBeanImpl instance = new LoggingMXBeanImpl();

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    private LoggingMXBeanImpl() {
        setMBeanInfo(ManagementUtils
                .getMBeanInfo(LoggingMXBean.class.getName()));
    }

    /**
     * Singleton accessor method.
     * 
     * @return the <code>LoggingMXBeanImpl</code> singleton.
     */
    static LoggingMXBeanImpl getInstance() {
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.LoggingMXBean#getLoggerLevel(java.lang.String)
     */
    public String getLoggerLevel(String loggerName) {
        String result = null;

        Logger logger = LogManager.getLogManager().getLogger(loggerName);
        if (logger != null) {
            // The named Logger exists. Now attempt to obtain its log level.
            Level level = logger.getLevel();
            if (level != null) {
                result = level.getName();
            } else {
                // A null return from getLevel() means that the Logger
                // is inheriting its log level from an ancestor. Return an
                // empty string to the caller.
                result = "";
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.LoggingMXBean#getLoggerNames()
     */
    public List<String> getLoggerNames() {
        // By default, return an empty list to caller
        List<String> result = new ArrayList<String>();

        Enumeration<String> enumeration = LogManager.getLogManager()
                .getLoggerNames();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                result.add(enumeration.nextElement());
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.LoggingMXBean#getParentLoggerName(java.lang.String)
     */
    public String getParentLoggerName(String loggerName) {
        String result = null;

        Logger logger = LogManager.getLogManager().getLogger(loggerName);
        if (logger != null) {
            // The named Logger exists. Now attempt to obtain its parent.
            Logger parent = logger.getParent();
            if (parent != null) {
                // There is a parent
                result = parent.getName();
            } else {
                // logger must be the root Logger
                result = "";
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.LoggingMXBean#setLoggerLevel(java.lang.String,
     *      java.lang.String)
     */
    public void setLoggerLevel(String loggerName, String levelName) {
        final Logger logger = LogManager.getLogManager().getLogger(loggerName);
        if (logger != null) {
            // The named Logger exists. Now attempt to set its level. The
            // below attempt to parse a Level from the supplied levelName
            // will throw an IllegalArgumentException if levelName is not
            // a valid level name.
            if (levelName != null) {
                Level newLevel = Level.parse(levelName);
                logger.setLevel(newLevel);
            } else {
                logger.setLevel(null);
            }
        } else {
            // Named Logger does not exist.
            throw new IllegalArgumentException(
                    "Unable to find Logger with name " + loggerName);
        }
    }
}
