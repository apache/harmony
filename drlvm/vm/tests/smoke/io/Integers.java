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
 * @author Alexei Fedotov
 */

package io;

import java.io.*;
import java.util.*;

class Spaghetti implements Serializable {
    public static final long serialVersionUID = 0L;
    Integer s = new Integer(1);
    Spaghetti s1, s2;
    {
        s1 = s2 = this;
    }
}

public class Integers {
    private static final int SPAGHETTI_NUM = 500;
    private static final Random rnd = new Random();

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        Spaghetti[] sa = new Spaghetti[SPAGHETTI_NUM];

        //fill array with empty objects
        for (int i = 0; i < SPAGHETTI_NUM; i++) {
            sa[i] = new Spaghetti();
        }

        //mess it up
        for (int i = 0; i < SPAGHETTI_NUM; i++) {
            sa[i].s1 = sa[rnd.nextInt(SPAGHETTI_NUM)];
            sa[i].s2 = sa[rnd.nextInt(SPAGHETTI_NUM)];
        }

        oos.writeObject(sa);
        oos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        ois.readObject();
        ois.close();
        System.out.println("PASSED");
    }
}

