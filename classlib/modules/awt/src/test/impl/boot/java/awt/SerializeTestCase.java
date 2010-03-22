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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;

import junit.framework.TestCase;

public class SerializeTestCase extends TestCase {

    public static boolean SERIALIZATION_TEST = false;
    
    public String serializePath;
    
    public SerializeTestCase(String name) {
        super(name);
        if (SERIALIZATION_TEST) {
            String classPath = "../resources/serialization/" + Tools.getClasstPath(this.getClass());
            URL url = ClassLoader.getSystemClassLoader().getResource(classPath);
            assertNotNull("Path not found " + classPath, url);
            serializePath = url.getPath();
        }
    }

    public String objToStr(Object obj) {
        return obj.toString();
    }

    public void checkRead(Object obj) {
        String file = serializePath + objToStr(obj) + ".ser";
        Object golden = loadSerialized(file);
        assertTrue("Non equals objects " + file, objToStr(golden).equals(objToStr(obj)));
    }

    public void checkWrite(Object obj) {
        String name = objToStr(obj);
        String expected = serializePath + name + ".ser";
        File actual = null;
        try {
            actual = File.createTempFile(name, ".actual", new File(serializePath));
            actual.deleteOnExit();
        } catch (IOException e) {
            fail("Can't create temp file");
        }
        saveSerialized(obj, actual.getPath());
        assertTrue("Non equals files " + expected, compare(expected, actual.getPath()));
    }

    public Object loadSerialized(String file) {
        try {
            //System.out.println("load " + file);   
            FileInputStream fs = new FileInputStream(file);
            ObjectInputStream os = new ObjectInputStream(fs);
            Object obj = os.readObject();
            os.close();
            fs.close();
            return obj;
        } catch (Exception e) {
            fail("Can''t read object from file " + file);
        }
        return null;
    }

    public void saveSerialized(Object obj) {
        saveSerialized(obj, serializePath + objToStr(obj));
    }

    public void saveSerialized(Object obj, String file) {
        try {
//            System.out.println("save " + file);
            FileOutputStream fs = new FileOutputStream(file);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(obj);
            os.close();
            fs.close();
        } catch (Exception e) {
            fail("Can''t write object to file " + file);
        }
    }

    boolean compare(String file1, String file2) {
        boolean cmp = false;
        try {
            FileInputStream fs1 = new FileInputStream(file1);
            FileInputStream fs2 = new FileInputStream(file2);

            byte[] buf1 = new byte[256];
            byte[] buf2 = new byte[256];
            int count1, count2;

        OUTER:
            while(true) {
                count1 = fs1.read(buf1);
                count2 = fs2.read(buf2);

                if (count1 != count2) {
                    break OUTER;
                }
                if (count1 == -1) {
                    cmp = true;
                    break;
                }
                for(int i = 0; i < count1; i++) {
                    if (buf1[i] != buf2[i]) {
                        break OUTER;
                    }
                }
            }

            fs1.close();
            fs2.close();
        } catch (Exception e) {
            fail("Can''t compare files " + file1 + " and " + file2);
        }
        return cmp;
    }

}
