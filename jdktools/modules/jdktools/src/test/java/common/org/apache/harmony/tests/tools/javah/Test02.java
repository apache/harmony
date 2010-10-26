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

package org.apache.harmony.tests.tools.javah;
import org.apache.harmony.tests.tools.javah.Test.*;

public class Test02 extends Test01 {

    private static final byte ZZZ_зима = 22;

    private static final byte zzz11 = 2;
    private static final short zzz12 = 2;
    private static final int zzz13 = 2;
    private static final long zzz14 = 2;
    private static final float zzz15 = 2;
    private static final double zzz16 = 2.0;
    private static final boolean zzz17 = true;
    private static final char zzz18 = '2';
    private static final int zzz19[] = null;
    private static final int zzz20[] = {20};

    protected native int zzz2(String v);

    private native long zzz2(long v);

    public native String zzz2(boolean v);

    native void zzz2_zzz2(Thread v, String s[], Class c[], Object o[]) throws Exception;

    native void zzz2_zzz2(Thread v, char c, Class z, String s, Object o) throws Exception;

    native void zzz2_zzz2(Thread v, char[] c) throws Exception;

    native void zzz2_zzz2(int[] i, Thread v, char[] c) throws Exception;

    native Throwable zzz2_zzz2_и_вася(Throwable[] i, Throwable v, char[] c)
            throws Exception, IndexOutOfBoundsException;

    class nested {
        static final long nested_yes = -2;
        static final long nested = 2;

        native void nested2_nested2(boolean v);

        private native long nested2(long v);

        public native int nested2(boolean v);
    }
}
