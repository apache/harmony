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
/**
 * @author Pavel Dolgov
 */
package java.awt.datatransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import junit.framework.TestCase;

public class DataFlavorRTest extends TestCase {

    public void testSerializeDefaultDataFlavor() throws IOException,
            ClassNotFoundException {
        DataFlavor flavor = new DataFlavor();
        DataFlavor restored = (DataFlavor) writeAndRead(flavor);
        assertEquals(restored, flavor);
    }

    private Serializable writeAndRead(Serializable original)
            throws IOException, ClassNotFoundException {
        File tempFile = File.createTempFile("save", ".object");
        tempFile.deleteOnExit();

        FileOutputStream fos = new FileOutputStream(tempFile);

        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(original);
        oos.close();

        FileInputStream fis = new FileInputStream(tempFile);
        ObjectInputStream ois = new ObjectInputStream(fis);

        Serializable restored = (Serializable) ois.readObject();
        ois.close();
        return restored;
    }
}