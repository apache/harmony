INTEL CONTRIBUTION TO APACHE HARMONY
          September, 2006
====================================


This archive contains the contribution to the Apache Harmony project 
from Intel. The contribution consists of the following components:

  JPDA module:
    - JDWP agent
    - JDWP transport
    - JDWP tests

See http://java.sun.com/products/jpda/ for a definition 
of JPDA components.


1. ARCHIVE CONTENTS
-------------------

The archive contains the source files, the building environment,
and the unit tests for testing the JPDA implementation.

After extracting this archive, the following directories appear under
<EXTRACT_DIR>/Harmony/modules/, where EXTRACT_DIR is the location 
into which the archive was extracted:

  <EXTRACT_DIR>/Harmony/modules
       |
       \---jpda
           |
           +---doc  - The JPDA module documentation
           +---make - The JPDA module and tests build scripts
           +---src  - The JPDA module source files
           \---test - Unit tests for the JPDA module

Extracting the archive into your Harmony classlib source tree
enables you to build it with the default options.
Extracting the archive to a separate directory requires that
you specify the paths to the build scripts using ant properties.
See section 3 for details.


2. TOOLS AND LIBRARIES REQUIRED FOR THE BUILD
---------------------------------------------

To build the Java* and C++ sources contained in the src/ directory,
install and configure the following tools and support libraries:

+ Apache Ant
    - Apache Ant version 1.6.2 or higher
        http://ant.apache.org

+ Java* SDK and compiler
    - You can use either of the following:
        + A J2SE* 1.5.0 compatible SDK
        + Apache Harmony Execution Environment
            http://incubator.apache.org/harmony/
        with the Eclipse* Java* compiler version 3.1.1
            http://download.eclipse.org/eclipse/downloads/

+ C/C++ compiler
    - on Windows*, you can use either of the following:
        + Microsoft* 32-bit C/C++ compiler version 7 or higher
            http://www.microsoft.com/downloads/
        and Windows* platform SDK
            http://www.microsoft.com/downloads/
        + Microsoft* Visual Studio .NET* 2003 or higher.

    - on Linux*, use the GNU project C/C++ compiler 
        version 3.3.3 or higher
            http://gcc.gnu.org

+ cpptasks
    - The cpptasks bundle version 1.0 beta 3 or higher
        http://ant-contrib.sourceforge.net/
        http://sourceforge.net/project/showfiles.php?group_id=3617

To build the unit test sources contained in the test/ directory and
run unit tests, additionally install:

+ JUnit
    - JUnit testing framework version 3.8.2 or higher
        http://junit.org/

To build Java* code with the Eclipse* Java* compiler (ECJ), optionally install:

+ Eclipse
    - Eclipse* SDK version 3.1.1 or later
       http://www.eclipse.org/
  or
    - Standalone Eclipse* Java* compiler version 3.1.2 or later
        http://www.eclipsezone.com/eclipse/forums/m92017878
        http://www.eclipse.org/jdt/core/patches/ecj_3.1.2.jar
        
To create HTML documentation from the agent sources, optionally install:

+ Doxygen
    - Doxygen version 1.3.9 or higher
        http://www.stack.nl/~dimitri/doxygen/


3. BUILDING JPDA/JDWP NATIVE LIBRARIES, HTML DOCUMENTATION AND TESTS
--------------------------------------------------------------------

1. Place the file cpptasks.jar from the cpptasks bundle to the directory
   <EXTRACT_DIR>/Harmony/depends/jars/cpptasks-1.0b3/.

2. On Windows*, start the Microsoft* Windows* SDK build environment
   or the Visual Studio .NET* 2003 Command Prompt.

3. On both platforms, verify the values for the following environment
   variables:
    - PATH must contain the path to Ant and the C++ compiler.
    - JAVA_HOME must point to J2SE 1.5.0 compatible SDK or the Harmony build.

4. Ensure that you have jni.h and jvmti.h required for native components 
   of the JPDA project.

5. Optionally, install Doxygen or another tool to generate documentation 
   for agent components and include Doxygen binaries directory to the
   system path. 

NOTE: All paths must be absolute.

6. Build the JPDA module by doing the following:

  6.1 Change the working directory to jpda/make/.

  6.2 Start the build with Apache Ant by typing 
        ant
      Ant runs against the default target and compiles all Java* and C++ 
      sources from the source directory. You can also make Ant run against
      specific targets, namely:
        - help                  Prints help.
        - build (default)       Builds all binaries: the agent, 
                                transport and tests.
        - build.jdwp.agent      Builds only the agent subcomponent.
        - build.jdwp.transport  Builds only the transport subcomponent.
        - build.jdwp.tests      Builds only the tests and places them into
                                jdwp_tests.jar.
        - build.doc             Builds all documentation (agent and tests).
        - build.jdwp.agent.doc  Builds docs from comments in source code
                                of the agent component.
        - build.jdwp.tests.doc  Builds docs from comments in tests' source code.
        - run.jdwp.tests        Runs JDWP unit tests.
        - clean                 Cleans all generated files.
      See the ant script file build.xml for a full list of targets. 
      You can adjust your build process even further by setting properties, 
      see section 4 below.

  NOTE: 
  You may get errors in compiling JDWP agent sources because of incorrect
  jvmti.h file included in the current Harmony sources, see KNOWN ISSUES. 

The build produces native libraries of the JDWP agent and transport and
the .jar file with tests. By default, these files are placed in
the following directory tree:

  <EXTRACT_DIR>/Harmony
       |
       \---deploy
           |
           +---jdk
           |   |
           |   \---jre
           |       |
           |       \---bin
           |           |
           |           +--- jdwp.dll or libjdwp.so
           |           \--- dt_socket.dll or libdt_socket.so
           |
           +---test
           |   |
           |   \---jdwp_tests.jar
           |
           \---doc
               |
               \---... // generated HTML documents

Where <EXTRACT_DIR>/Harmony path corresponds to the Ant property harmony.path.


4. CONFIGURING THE BUILD PROCESS
--------------------------------

The following ant properties affect JPDA build:
    Specify ant command-line options in the format -Dproperty=value 

 - harmony.path          Path to Harmony sources directory.
                           Default: "../../.." 
                           Example: <EXTRACT_DIR>/Harmony
 - build.path            Path to directory where to build JDWP agent
                         and unit tests.
                            Default: ${harmony.path}
 - deploy.path           Path to directory where to deploy JDWP agent
                         and unit tests.
                            Default: ${harmony.path}/deploy
 - jni_h.path            Path to the include/ directory with JNI and JVMTI
                         header files
                            Default: ${harmony.path}/modules/luni/src/main/native/include/shared
 - cpptask.jar           Path to cpptask jar file.
                            Default: ${harmony.path}/depends/jars/cpptasks-1.0b3/cpptasks.jar
 - junit.jar             Path to the junit.jar file.
                            Default: ${harmony.path}/depends/jars/junit_3.8.2/junit.jar
 - build.compiler        Java* compiler name
                            Default: javac
 - native.compiler       C/C++ compiler name
                            Default:  gcc on Linux and msvc on Windows*
 - java.debug.option     Debug configuration for Java* sources
                            Default: on
 - native.debug.option   Debug configurartion for C/C++ sources
                            Default: on
 - doxygen               Full path to run doxygen
                            Default: doxygen

See the following list for useful tips on configuring the build process.
 + Changing the output location for binaries
   Set the build.path property, for example:
      -Dbuild.path=C:/workspace/jpda_jdwp_build

 + Changing the location of the required libraries CPPTASK and JUNIT
   By default, these libraries are taken from
     ${harmony.path}/depends/jars/build
   To change the location, reset the cpptask.jar and junit.jar properties,
   for example:
     -Djunit.jar=C:/tools/junit/junit.jar
     -Dcpptask.jar=C:/tools/cpptask/cpptasks.jar

 + Changing the location of the JNI and JVMTI headers
   By default, these headers are taken from
     ${harmony.path}/modules/luni/src/main/native/include/shared
   To change the location, reset the jni_h.path ant property value,
   for example:
     -Djni_h.path=C:/some-jdk/include;C:/some-jdk/include/win32

   NOTE
   The current JNI and JVMTI header files in the Harmony classlib tree are
   incorrect and cause build errors. You are recommended to use JNI and JVMTI
   header files from DRLVM sources or JDK builds.

 + Changing the Java* compiler to compile Java* sources
   By default, Ant uses the default Java* compiler in javac task.
   To use the Eclipse* compiler (ECJ), add the compiler's .jar files to CLASSPATH
   and specify the Eclipse* Java* compiler for the build.compiler property. 
   Example:
     using standalone ECJ:
       set CLASSPATH=<ECJ_HOME>/ecj_3.1.2.jar
       ant -Dbuild.compiler=org.eclipse.jdt.core.JDTCompilerAdapter
     using Eclipse* SDK:
       set CLASSPATH=<ECLIPSE_HOME>/plugins/org.eclipse.jdt.core_3.1.1/jdtCompilerAdapter.jar:\
       <ECLIPSE_HOME>/plugins/org.eclipse.jdt.core_3.1.1.jar
       ant -Dbuild.compiler=org.eclipse.jdt.core.JDTCompilerAdapter

 + Changing the native compiler 
   By default, the native.compiler property defines the following 
   default native compilers:
    - msvc under Windows
    - gcc under Linux
   You can reset the property to use another compiler, for example,
   Intel C++ compiler:
    -Dnative.compiler=icl under Windows
    -Dnative.compiler=icc under Linux

 + Changing the build configuration for Java* and native code
   You can use the debug or release configuration. By default, the debug
   mode is used. To produce release libraries, reset the following:
     -Djava.debug.option=off
     -Dnative.debug.option=off

   NOTE
   All JDWP unit tests are written in Java* and must be compiled in debug 
   configuration. Otherwise, they may fail because of lack of debug information.


5. RUNNING JDWP AGENT WITH AN EXTERNAL VM
-----------------------------------------

This JDWP agent can be used for debugging Java* applications with Harmony/DRLVM
JRE or another JRE supporting the JVMTI interface. To enable this, do:

 - Copy jdwp and dt_socket libraries to the JRE binary directory,
   for example, to <JRE>/bin.
 - Debug the application with this JRE under any Java* debugger,
   for example, the Eclipse* JDT debugger.

The JDWP agent supports all standard options described in JPDA specification,
except 'onthrow' and 'onuncought' used for starting the debugger on demand.

To start JVM with the JDWP agent on the command line, use the options shown
in the usage message:

Usage: java -agentlib:agent=[help] |
        [suspend=y|n][,transport=name][,address=addr]
        [,server=y|n][,timeout=n]
        [,trace=none|all|log_kinds][,src=all|sources][,log=filepath]

Where:
        help            Getting this message
        suspend=y|n     Suspending on start (default: y)
        transport=name  Name of transport to use for connection
        address=addr    Transport address for connection
        server=y|n      Listening for or attaching to debugger (default: n)
        timeout=n       Time in ms to wait for connection (0-forever)
        trace=log_kinds Filtering to the log message kind (default: none)
        src=sources     Filtering to __FILE__ (default: all)
        log=filepath    Dumping output into filepath

Example:
       java -agentlib:agent=transport=dt_socket,address=localhost:7777,server=y

NOTE
    The trace, src and log subarguments are available only in the agent built
    in the debug configuration.


6. RUNNING JDWP TESTS
---------------------

To run JDWP tests, set JAVA_HOME to point to the appropriate JRE with
the JDWP agent, include the JUnit framework into CLASSPATH, change the working
directory to make/ and run the Ant script for target run.jdwp.tests.
For example:

    set JAVA_HOME=C:/some-jre
    set CLASSPATH=C:/JUnit/junit.jar
    ant run.jdwp.tests

This target runs all JDWP unit tests using the junit task for specified JRE.
Test results are saved in the <tst.reports.path> location, which is 
<EXTRACT_DIR>/Harmony/build/test_reports by default.

Alternatively, you can specify the path to the same or a different JVM for
the debugger and debuggee components of the tests. For that, reset properties
test.debugger.jvm and test.debuggee.jvm, for example:
  -Dtest.debugger.jvm=C:/some-jre/bin/java.exe
  -Dtest.debuggee.jvm=C:/some-jre/bin/java.exe

NOTE
    If you run unit tests against Harmony JRE with DRLVM and use the JUnit
    framework version 4.0 or higher, you may get errors, because DRLVM 
    does not support the new classfile version of the JUnit classes. 
    Use an older version of JUnit framework instead, such as version 3.8.2.


7. KNOWN ISSUES
---------------

The contributed JPDA module has the following known problems:

- This JDWP agent has only been tested on Linux/IA-32 and Windows/IA-32
  platforms with Harmony/DRLVM JRE and BEA* JRockit* JDK 1.5.

- Several JDWP tests fail because of known problems with JVMTI implementation
  or unimplemented JVMTI features in DRLVM and other tested JVMs.

- The current jvmti.h file in the Harmony classlib sources cannot be used
  in C++ sources, which causes build errors. You are recommended to use jvmti.h
  from DRLVM sources or from an existing JDK build.

- The latest cpptask release version 1.0b4 hangs running Intel* C++ compiler
  on Windows*, you are recommended to use cpptask version 1.0b3 instead.
  
- The latest JUnit framework version 4.0 or higher is compiled for the 1.5
  target, which DRLVM does not currently support. You might get errors when 
  running unit tests against Harmony JRE with DRLVM. Use JUnit framework 
  version 3.8.2 in this case.


8. TODO
-------

The following functionality is missing in the current JDWP implementation:

  - Support for agent options onthrow and onuncought to start the debugger
    when an exception is thrown in the application.

  - Grouping different events for the same location, for example, breakpoint 
    and step events. With the current implementation you need to resume the 
    thread twice when stepping over a line with a breakpoint.

  - Support for the optional ClassUnload event that requires JVMTI support 
    for extended events.

  - Support for changing the default strata, which is not clearly described in 
    the JPDA spec and may require full support for source strata 
    according to JSR-45.


9. DISCLAIMER
--------------
*) Other brands and names are the property of their respective owners.
