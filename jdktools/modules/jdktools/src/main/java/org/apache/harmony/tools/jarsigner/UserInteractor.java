/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.tools.jarsigner;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class to interact with user - ask for confirmations, and necessary parameters
 * which haven't been set in the command line.
 */
class UserInteractor {
    // used to get additional data prompted
    private static InputStreamReader in = new InputStreamReader(System.in);

    // buffer for the data read
    private static char[] readData = new char[256];

    // number of symbols read
    private static int charsRead;

    // length of the "\r\n" which is added to the end of the line,
    // when ENTER is pressed.
    private static int newLineLength = 2;

    // Prints prompt and waits the user to enter the needed data,
    // the data is returned.
    static char[] getDataFromUser(String prompt) throws IOException {
        System.out.println(prompt);
        charsRead = in.read(readData);
        char[] password = new char[charsRead - newLineLength];
        System.arraycopy(readData, 0, password, 0, charsRead - newLineLength);
        return password;
    }
}

