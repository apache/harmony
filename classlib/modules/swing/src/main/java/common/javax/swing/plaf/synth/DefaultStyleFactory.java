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

package javax.swing.plaf.synth;

import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JComponent;

/**
 * The defaultStyleFactory returns styles fills up by putStyle(String, String,
 * SynthStyle) method
 */
class DefaultStyleFactory extends SynthStyleFactory {

    /**
     * List that containing styles
     */
    private final ArrayList<StyleInfo> styles = new ArrayList<StyleInfo>();

    @Override
    public SynthStyle getStyle(JComponent comp, Region reg) {

        SynthStyle result = findStyle(comp, reg);

        if (result == null) {
            return SynthStyle.NULL_STYLE;
        }

        return result;
    }

    public void putStyle(String bindType, String bindKey, SynthStyle style) {

        styles.add(new StyleInfo(bindType, bindKey, style));
    }

    private SynthStyle findStyle(JComponent comp, Region reg) {

        String name = comp.getName();
        SynthStyle foundByName = null;
        SynthStyle foundByRegion = null;

        for (StyleInfo candidate : styles) {

            if ((name != null) && ("name".equals(candidate.getBindType())) //$NON-NLS-1$
                    && Pattern.matches(name, candidate.getBindKey())) {
                foundByName = candidate.getStyle();
            }

            if (("region".equals(candidate.getBindType())) //$NON-NLS-1$
                    && reg.getName().equals(candidate.getBindKey())) {
                foundByRegion = candidate.getStyle();
            }
        }

        // foundByName has a priority
        if (foundByName == null) {

            return foundByRegion;
        }

        return foundByName;
    }

    /**
     * StyleInfo can be used for representing the style and for finding a style
     * from list
     */
    private static class StyleInfo {

        private final String bindType;

        private final String bindKey;

        private final SynthStyle currentStyle;

        /**
         * Note that the constructor lowercases type and key for the StyleKey
         */
        public StyleInfo(String bindType, String bindKey,
                SynthStyle currentStyle) {
            this.bindKey = bindKey.toLowerCase();
            this.bindType = bindType.toLowerCase();
            this.currentStyle = currentStyle;
        }

        public String getBindType() {
            return bindType;
        }

        public String getBindKey() {
            return bindKey;
        }

        public SynthStyle getStyle() {
            return currentStyle;
        }

        @Override
        public String toString() {
            return "KEY[" + bindType + " " + bindKey + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

    }

}
