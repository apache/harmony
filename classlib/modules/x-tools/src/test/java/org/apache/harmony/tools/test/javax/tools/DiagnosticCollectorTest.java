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

import java.util.Iterator;
import java.util.List;

import javax.tools.DiagnosticCollector;

import junit.framework.TestCase;

public class DiagnosticCollectorTest extends TestCase {
    public void testReport() throws Exception {
        DiagnosticCollector dc = new DiagnosticCollector();
        dc.report(null);
        dc.report(new MockDiagnostic());
    }
    
    public void testGetDiagnostic() throws Exception {
        DiagnosticCollector dc = new DiagnosticCollector();
        dc.getDiagnostics();
        List list = dc.getDiagnostics();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            System.out.println(iter.next());            
        }
    }
}
