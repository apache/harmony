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

package org.apache.harmony.tools.test.javax.tools;

import java.util.Locale;

import javax.tools.Diagnostic;

public class MockDiagnostic implements Diagnostic {

    public String getCode() {
        return "Mock Error code 0";
    }

    public long getColumnNumber() {
        return 1000;
    }

    public long getEndPosition() {
        return -1000;
    }

    public Kind getKind() {
        return Kind.ERROR;
    }

    public long getLineNumber() {
        return 2000;
    }

    public String getMessage(Locale locale) {
        return "localed message";
    }

    public long getPosition() {
        return 3000;
    }

    public Object getSource() {
        return null;
    }

    public long getStartPosition() {
        return -2000;
    }

}
