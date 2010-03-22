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

package org.apache.harmony.auth.module;

/** 
 * A helper class which queries an information about the current user.
 */
public class UnixSystem {
    
    // Shows whether the hyauth library was loaded already
    private static boolean loadLibDone;
  
    /** 
     * User's main group's ID 
     */
    protected long gid;

    /** 
     * User's main group's name 
     */
    protected String groupname;

    /** 
     * List of group IDs the user belongs to 
     */
    protected long[] groups;

    /** 
     * List of group names the user belongs to 
     */
    protected String[] groupsNames;

    /** 
     * User ID 
     */
    protected long uid;

    /** 
     * User name 
     */
    protected String username;

    /** 
     * Sole constructor. 
     * @throws UnsatisfiedLinkError if hyauth library could not be loaded
     */
    public UnixSystem() {
        if (!loadLibDone) {
            System.loadLibrary("hyauth"); //$NON-NLS-1$
            loadLibDone = true;
        }
        load();
    }

    /** 
     * returns user's main group's id 
     */
    public long getGid() {
        return gid;
    }

    /** 
     * returns user's main group's name
     */
    public String getGroupName() {
        return groupname;
    }
    
    /** 
     * returns list of ids of groups the user belongs to 
     */
    public long[] getGroups() {
        if (groups == null) {
            return new long[0];
        }
        long[] tmp = new long[groups.length];
        System.arraycopy(groups, 0, tmp, 0, tmp.length);
        return tmp;
    }

    /** 
     * returns list of names of groups the user belongs to 
     */
    public String[] getGroupNames() {
        if (groupsNames == null) {
            return new String[0];
        }
        String[] tmp = new String[groupsNames.length];
        System.arraycopy(groupsNames, 0, tmp, 0, tmp.length);
        return tmp;
    }

    /** 
     * returns user's id 
     */
    public long getUid() {
        return uid;
    }

    /** 
     * returns user name 
     */
    public String getUsername() {
        return username;
    }

    /**
     * The function which actually does all the work with loading info
     */
    public native void load();

    /**
     * Returns string representation of this object
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("UnixSystem: \n"); //$NON-NLS-1$
        buf.append("uid:gid=").append(uid).append(":").append(gid); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append("="); //$NON-NLS-1$
        buf.append(username).append(":").append(groupname); //$NON-NLS-1$
        buf.append("\n"); //$NON-NLS-1$

        buf.append("total groups: "); //$NON-NLS-1$
        buf.append(groups == null ? 0 : groups.length);

        if (groups != null) {
            buf.append("\n"); //$NON-NLS-1$
            for (int i = 0; i < groups.length; i++) {
                buf.append(i).append(") ").append(groupsNames[i]).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return buf.toString();
    }
}
