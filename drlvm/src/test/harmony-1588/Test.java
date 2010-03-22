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

import java.util.Vector;

class Fin {
    public void finalize() {}
}

public class Test {
    public static void main(String[] args) {
        try {
            Vector v = new Vector();
            while (true) {
                int fin = 0, obj = 0;
                Object[] array = new Object[4096];
                for(int i = 0; i < 512; i++) {
                    array[i * 8] = new Object();
                    for(int j = 1; j < 8; j++) {
                        array[i * 8 + j] = new Fin();
                    }
                }
                for(int i = 0; i < 4096; i++) {
                    if (array[i] instanceof Fin) {
                        fin++;
                    } else {
                        obj++;
                    }
                }
                System.err.print(".");
                v.add(array);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("PASSED");
        }
    }
}
