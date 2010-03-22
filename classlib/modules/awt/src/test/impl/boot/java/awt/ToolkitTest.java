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

package java.awt;


import junit.framework.TestCase;

public class ToolkitTest extends TestCase {

    public ToolkitTest() {
        super();
    }

    public ToolkitTest(String name) {
        super(name);
    }

    public void testGetImage() {
        try {    
            Toolkit tk = Toolkit.getDefaultToolkit();            

            assertNotNull(tk.getImage((String) null));
        } catch (NullPointerException npe) {             
            fail("Unexpected NPE");            
        }
    }

    public void testCreateCustomCursor() {
        try {
            // Regression for HARMONY-4491
            Toolkit tk = Toolkit.getDefaultToolkit();            
            Image img = tk.createImage(new byte[] { 0 } );
            Cursor cursor = tk.createCustomCursor(img, new Point(0, 0), "");
        } catch (IndexOutOfBoundsException e) {
            fail("Unexpected IndexOutOfBoundsException");            
        }
    }
}
