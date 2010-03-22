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

package javax.accessibility;


@SuppressWarnings("nls")
public class AccessibleRelation extends AccessibleBundle {
    // Calls to new String(String) here are required - see HARMONY-6008
    public static final String LABEL_FOR = new String("labelFor");
    public static final String LABELED_BY = new String("labeledBy");
    public static final String MEMBER_OF = new String("memberOf");
    public static final String CONTROLLER_FOR = new String("controllerFor");
    public static final String CONTROLLED_BY = new String("controlledBy");
    public static final String FLOWS_TO = "flowsTo";
    public static final String FLOWS_FROM = "flowsFrom";
    public static final String SUBWINDOW_OF = "subwindowOf";
    public static final String PARENT_WINDOW_OF = "parentWindowOf";
    public static final String EMBEDS = "embeds";
    public static final String EMBEDDED_BY = "embeddedBy";
    public static final String CHILD_NODE_OF = "childNodeOf";
    public static final String LABEL_FOR_PROPERTY = "labelForProperty";
    public static final String LABELED_BY_PROPERTY = "labeledByProperty";
    public static final String MEMBER_OF_PROPERTY = "memberOfProperty";
    public static final String CONTROLLER_FOR_PROPERTY = "controllerForProperty";
    public static final String CONTROLLED_BY_PROPERTY = "controlledByProperty";
    public static final String FLOWS_TO_PROPERTY = "flowsToProperty";
    public static final String FLOWS_FROM_PROPERTY = "flowsFromProperty";
    public static final String SUBWINDOW_OF_PROPERTY = "subwindowOfProperty";
    public static final String PARENT_WINDOW_OF_PROPERTY = "parentWindowOfProperty";
    public static final String EMBEDS_PROPERTY = "embedsProperty";
    public static final String EMBEDDED_BY_PROPERTY = "embeddedByProperty";
    public static final String CHILD_NODE_OF_PROPERTY = "childNodeOfProperty";

    private Object[] targets;

    public AccessibleRelation(final String key) {
        this.key = key;
        targets = new Object[0];
    }

    public AccessibleRelation(final String key, final Object target) {
        this.key = key;
        setTarget(target);
    }

    public AccessibleRelation(final String key, final Object[] target) {
        this.key = key;
        setTarget(target);
    }

    public String getKey() {
        return key;
    }

    public Object[] getTarget() {
        return targets.clone();
    }

    public void setTarget(final Object target) {
        targets = new Object[]{target};
    }

    public void setTarget(final Object[] target) {
        targets = target != null ? (Object[])target.clone() : new Object[0];
    }
}

