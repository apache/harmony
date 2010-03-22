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

package org.apache.harmony.tools.javah.anttask;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.launch.Locator;
import org.apache.tools.ant.taskdefs.ExecuteJava;
import org.apache.tools.ant.taskdefs.optional.Javah;
import org.apache.tools.ant.taskdefs.optional.javah.JavahAdapter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * Adapter to org.apache.harmony.tools.javah.Main.
 *
 * This class depends on Apache Ant tool 1.6.5 or later.
 * Please see http://ant.apache.org for more information about this tool.
 */
public class Adapter implements JavahAdapter {

    /**
     * Runs our implementation of the <code>javah</code>.
     * 
     * @param javah is the Javah task.
     * @return <code>true</code> if there is no error; Otherwise
     *         <code>false</code>.
     */
    public boolean compile(Javah javah) throws BuildException {
        Class clss = null;

        // Try to load the main class of the tool.
        try {
            clss = Class.forName("org.apache.harmony.tools.javah.Main");
        } catch (ClassNotFoundException e) {
            throw new BuildException("Can't load a class", e, 
                    javah.getLocation());
        }

        // Prepare a command to launch the tool.
        Commandline cmd = setupCommand(javah);
        cmd.setExecutable(clss.getName());

        // Prepare a java command.
        ExecuteJava java = new ExecuteJava();
        java.setJavaCommand(cmd);

        // Find a file or a jar which represents the required class.
        File file = Locator.getClassSource(clss);
        if (file != null) {
            // The found file should be included into the class path.
            Path classpath = new Path(javah.getProject(), file.getPath());

            // Try to load BCEL's ClassPath utility class.
            try {
                clss = Class.forName("org.apache.bcel.util.ClassPath");
            } catch (ClassNotFoundException e) {
                throw new BuildException("Can't load BCEL", e, 
                        javah.getLocation());
            }

            // Find a file or a jar which represents the required class.
            file = Locator.getClassSource(clss);
            if (file != null) {
                // Add the found file to the class path.
                classpath.append(new Path(javah.getProject(), file.getPath()));
            }

            // Set the class path.
            java.setClasspath(classpath);
        }

        // Run the java command.
        return java.fork(javah) == 0;
    }

    /**
     * Prepare a command line that includes all parameters.
     * 
     * @param javah is the Javah task.
     * @return The prepared command line.
     */
    private Commandline setupCommand(Javah javah) {
        Commandline cmd = new Commandline();

        // Add a destination directory if any.
        if (javah.getDestdir() != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(javah.getDestdir());
        }

        // Add an output file if any.
        if (javah.getOutputfile() != null) {
            cmd.createArgument().setValue("-o");
            cmd.createArgument().setFile(javah.getOutputfile());
        }

        // Add a class path if any.
        if (javah.getClasspath() != null) {
            cmd.createArgument().setValue("-classpath");
            cmd.createArgument().setPath(javah.getClasspath());
        }

        // Add a verbose flag if any.
        if (javah.getVerbose()) {
            cmd.createArgument().setValue("-verbose");
        }

        // Add a boot class path if any.
        Path bootClasspath = new Path(javah.getProject());
        if (javah.getBootclasspath() != null) {
            bootClasspath.append(javah.getBootclasspath());
        }
        if (bootClasspath.size() > 0) {
            cmd.createArgument().setValue("-bootclasspath");
            cmd.createArgument().setPath(bootClasspath);
        }

        // Add settings given as nested arg elements.
        cmd.addArguments(javah.getCurrentArgs());

        // Add and log the parameters and source files.
        javah.logAndAddFiles(cmd);
        return cmd;
    }
}
