/* Licensed to the Apache Software Foundation (ASF) under one or more
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

package java.util.prefs;

import org.apache.harmony.prefs.internal.nls.Messages;

/**
 * Default implementation of {@code AbstractPreferences} for windows
 * platform, using windows registry as back end.
 * 
 * @since 1.4
 */
class RegistryPreferencesImpl extends AbstractPreferences {

    static {
        System.loadLibrary("hyprefs"); //$NON-NLS-1$
    }

    // Registry path for root preferences.
    private static final String ROOT_PATH = "SOFTWARE\\JavaSoft\\Prefs"; //$NON-NLS-1$

    // Index for returned error code.
    private static final int ERROR_CODE = 0;

    // Error code for registry access.
    private static final int RETURN_SUCCESS = 0;

    @SuppressWarnings("unused")
    private static final int RETURN_FILE_NOT_FOUND = 1;

    private static final int RETURN_ACCESS_DENIED = 2;

    @SuppressWarnings("unused")
    private static final int RETURN_UNKNOWN_ERROR = 3;

    // Registry path for this preferences, default value is the root path
    private byte[] path = ROOT_PATH.getBytes();

    /**
     * Construct {@code RegistryPreferencesImpl} instance using given parent and
     * given name
     */
    public RegistryPreferencesImpl(AbstractPreferences parent, String name) {
        super(parent, name);
        this.userNode = parent.isUserNode();
        path = (ROOT_PATH + encodeWindowsStr(absolutePath())).getBytes();
    }

    /**
     * Construct root {@code RegistryPreferencesImpl} instance, construct user
     * root if userNode is true, system root otherwise
     */
    public RegistryPreferencesImpl(boolean userNode) {
        super(null, ""); //$NON-NLS-1$
        this.userNode = userNode;
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        int[] error = new int[1];
        byte[][] names = getChildNames(path, userNode, error);
        if (error[ERROR_CODE] != RETURN_SUCCESS) {
            // prefs.B=Enumerate child nodes error
            throw new BackingStoreException(Messages.getString("prefs.B")); //$NON-NLS-1$
        }
        String[] result = new String[names.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = decodeWindowsStr(new String(names[i]));
        }
        return result;
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        int[] error = new int[1];
        RegistryPreferencesImpl result = new RegistryPreferencesImpl(this, name);
        // FIXME: is it right thing to set newNode here?
        result.newNode = getNode(path, encodeWindowsStr(name).getBytes(),
                result.userNode, error);
        if (error[ERROR_CODE] == RETURN_ACCESS_DENIED) {
            // prefs.E=Access denied
            throw new SecurityException(Messages.getString("prefs.E")); //$NON-NLS-1$
        }
        return result;
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        int[] error = new int[1];
        flushPrefs(path, userNode, error);
        if (error[ERROR_CODE] != RETURN_SUCCESS) {
            // prefs.C=Flush error
            throw new BackingStoreException(Messages.getString("prefs.C")); //$NON-NLS-1$
        }
    }

    @Override
    protected String getSpi(String key) {
        int[] error = new int[1];
        byte[] result = getValue(path, encodeWindowsStr(key).getBytes(),
                userNode, error);
        if (error[ERROR_CODE] != RETURN_SUCCESS) {
            return null;
        }
        return decodeWindowsStr(new String(result));
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        int[] errorCode = new int[1];
        byte[][] keys = keys(path, userNode, errorCode);
        if (errorCode[ERROR_CODE] != RETURN_SUCCESS) {
            // prefs.D=Enumerate keys error
            throw new BackingStoreException(Messages.getString("prefs.D")); //$NON-NLS-1$
        }
        String[] result = new String[keys.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = decodeWindowsStr(new String(keys[i]));
        }
        return result;
    }

    @Override
    protected void putSpi(String name, String value) {
        int[] errorCode = new int[1];
        putValue(path, encodeWindowsStr(name).getBytes(), encodeWindowsStr(
                value).getBytes(), userNode, errorCode);
        if (errorCode[ERROR_CODE] == RETURN_ACCESS_DENIED) {
            // prefs.E=Access denied
            throw new SecurityException(Messages.getString("prefs.E")); //$NON-NLS-1$
        }
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        int[] error = new int[1];
        removeNode(((RegistryPreferencesImpl) parent()).path, encodeWindowsStr(
                name()).getBytes(), userNode, error);
        if (error[ERROR_CODE] != RETURN_SUCCESS) {
            // prefs.F=Remove node error
            throw new BackingStoreException(Messages.getString("prefs.F")); //$NON-NLS-1$
        }
    }

    @Override
    protected void removeSpi(String key) {
        int[] errorCode = new int[1];
        removeKey(path, encodeWindowsStr(key).getBytes(), userNode, errorCode);
        if (errorCode[ERROR_CODE] == RETURN_ACCESS_DENIED) {
            // prefs.E=Access denied
            throw new SecurityException(Messages.getString("prefs.E")); //$NON-NLS-1$
        }
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        flushSpi();
    }

    // Handle the lower/upper case pitfall.
    private static String encodeWindowsStr(String str) {
        char[] chars = str.toCharArray();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '/') {
                buffer.append("\\"); //$NON-NLS-1$
            } else if (c == '\\') {
                buffer.append("//"); //$NON-NLS-1$
            } else if ((c >= 'A') && (c <= 'Z')) {
                buffer.append('/').append(c);
            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    private static String decodeWindowsStr(String str) {
        StringBuilder buffer = new StringBuilder(str.length());
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\\') {
                buffer.append('/');
            } else if (c == '/') {
                if ((c = chars[++i]) == '/') {
                    buffer.append('\\');
                } else {
                    buffer.append(c);
                }
            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    private native byte[] getValue(byte[] registryPath, byte[] key,
            boolean isUserNode, int[] errorCode);

    private native void putValue(byte[] registryPath, byte[] key, byte[] value,
            boolean isUserNode, int[] errorCode);

    private native void removeKey(byte[] registryPath, byte[] key,
            boolean isUserNode, int[] errorCode);

    private native byte[][] keys(byte[] registryPath, boolean isUserNode,
            int[] errorCode);

    private native void removeNode(byte[] registryPath, byte[] name,
            boolean isUserNode, int[] errorCode);

    private native boolean getNode(byte[] registryPath, byte[] name,
            boolean isUserNode, int[] errorCode);

    private native byte[][] getChildNames(byte[] registryPath,
            boolean isUserNode, int[] errorCode);

    private native void flushPrefs(byte[] registryPath, boolean isUserNode,
            int[] errorCode) throws SecurityException;
}
