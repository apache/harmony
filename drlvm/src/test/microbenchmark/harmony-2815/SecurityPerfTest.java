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

import java.security.Security;

/**
recursive call of run() checks for security permissions

to run it, issue the recursion depth list via command-line parameters like this:
java SecurityPerfTest 50 100 150 300 350 400 450 500 

on Jrockit/ia32 the time spent for each step depends linearly on the number of
iterations:
50 107 // this line should be ignored
100 1
150 1
300 3
350 5
400 6
450 7
500 9
*/
public class SecurityPerfTest {
    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());
        for (int i = 0; i < args.length; i++ ) {
            long start_time = System.currentTimeMillis();
            int amount = Integer.parseInt(args[i]);
            run(amount);
            long total_time = System.currentTimeMillis() - start_time;
            System.out.println(amount + " " + total_time);
        }
   }

public static void run(int count) {
       if ( count < 0 ) {
           return;
       }

//System.out.println("Runner: " + count);
       System.getProperty("os.name");
       run(count - 1);
   }
}
