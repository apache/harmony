======================================
         Apache Harmony DRLVM
======================================

DRLVM is one of the virtual machines of the Apache Harmony project. 
It contains: 
 
    - VM (VM Core)
    - GC
    - JIT
    - Bytecode Verifier 
    - Class Libraries (Kernel Classes only)
    - OS Layer

See http://wiki.apache.org/harmony for a definition of these components.

The currently supported configurations are Windows* x86, x86_64 and Linux* x86, x86_64, ia64.

  
1. DRLVM SOURCE TREE CONTENTS
-----------------------------
 
This source tree consists of the source files, the building scripts,
and the tests source files for integrity testing of the DRLVM.

The structure is as follows: 

 <EXTRACT_DIR>
        |
        +---make           - Files required to build the contribution
        |      
        \---vm             - VM source files
            |
            +- doc         - DRLVM Developer's Guide and Getting Started guide
            |
            +- em          - Execution Manager component responsible for dynamic optimization
            |
            +- gc_cc       - Stop-the-world adaptive copying/slide compacting garbage
            |                collector with dynamic algorithm switching
            |
            +- gc_gen      - Generational garbage collector
            |
            +- include     - Set of header files containing external specification
            |                and inter-component interfaces
            |
            +- interpreter - Interpreter component
            |
            +- jitrino     - Just-in-time Compiler component
            |
            +- port        - OS and platform porting layer component which, together with
            |                APR, provides an unified interface to low-level system routines 
            |                across different platforms
            |
            +- tests       - Tests source files
            |
            +- thread      - Thread manager (TM) library aimed to provide threading capabilities 
            |                for Java virtual machines
            |
            +- vmcore      - Core component responsible for class loading and resolution,
            |                kernel classes, JNI and JVMTI support, stack support, threading
            |                support, exception handling, verifier, and a set of other services
            |                and utilities
            |
            +- vmi         - Component responsible for compatibility with the Harmony class
            |                libraries 
            |
            \- vmstart     - Partial implementation of the component manager for handling 
                             VM components as pluggable modules 
 
 
2. TOOLS AND ENVIRONMENT VARIABLES REQUIRED FOR THE BUILD
---------------------------------------------------------
In order to build the source code, it is necessary to configure the following tools 
in the user environment. That is, the working environment should be such that the PATH 
environment variable contains all of the directories where the executables of
the tools listed below are located and that all of those executables can be 
successfully invoked from the command line. On Windows, this typically implies
that the build is started from Visual Studio Command Prompt console window.


* C++ compiler - on Windows, the Microsoft(R) 32-bit C/C++ Compiler and on
               Linux, the GNU project C/C++ Compiler.

* Java compiler - By default, the build scripts are setup to use the Eclipse
                  compiler (ECJ). The ECJ needs to be in the Ant class path
                  to execute correctly.
                  
* Apache Ant - A Java based build tool. See http://ant.apache.org/.
               It's suggested that the ANT_OPTS environment variable be set
               to a value of "-Xms256M -Xmx512M" while running the build scripts
               for Harmony.

* Doxygen - the open source documentation system for a variety of programming 
            languages including C, C++ and Java.
            See http://www.doxygen.org
            

The top-level Ant script <EXTRACT_DIR>/build.xml has a default target 
which builds both the Java source and C++ source files. It is expected
therefore that, at a minimum, a C++ compiler and a Java compiler be available.
Doxygen is only necessary if generation of HTML documentation from the
source code is to be carried out by invoking Ant with the "doc" target on
the <EXTRACT_DIR>/build.xml script. As a convenience, pre-generated
HTML files are already stored in subversion.

2.1 DRLVM DEPENDENCIES
----------------------
The DRLVM depends on common_resources module of Apache Harmony for managing 
external dependencies. Further, it needs either HDK or Harmony class library 
to build against and comprise a complete workable JRE.
You can separately obtain those modules, refer to Getting Started page on 
Apache Harmony site [http://harmony.apache.org/quickhelp_contributors.html].
 
Also, there are external resources required for building DRLVM: 
zlib, apr-1.3.8, log4cxx, cpptasks-1.b04, etc. 
This list can change as DRLVM is being developed, so the best way to resolve 
external dependencies is to let the build download them:
$ ant fetch-depends

By default DRLVM assumes federated build layout, as described on the 
aforementioned Getting Started page. That is, common_resources and classlib 
modules reside in the same parent directory as DRLVM's <EXTRACT_DIR>, 
w/o extra "trunk" levels inside. 
You may override default locations via the following build arguments:
-Dcommon.resources.loc=<common_resources-location>
-Dhy.hdk=<unpacked-HDK-location>
-Dexternal.dep.CLASSLIB.loc=<classlib-location>
For example, run this command to check if all build prerequisites are met:
$ ant -Dcommon.resources.loc=<path> -Dhy.hdk=<path> check-depends

3. BUILDING VM
--------------

The simplest way to get started is to change directory into <EXTRACT_DIR> and
then type "ant" to run Apache Ant against the default target of the build.xml
file. Provided that the required compilers are available and configured properly, 
Ant will proceed to compile all the DRLVM source code.
Build mode (release/debug) is controlled with "hy.cfg" property, default is debug.
E.g. type to build in release mode:
$ ant -Dhy.cfg=release

The build produces a set of .jar files, native libraries, and
support files that constitute the executable JRE. Also, it imports required
supplementary binaries from the pre-built classlib. The complete workable JRE 
is placed in the following directory tree structure:

./build/${OS}_${CPU_arch}_${CXX}_${BUILD_CFG}
       \---deploy
             |
             \---jdk
                  |
                  \---jre
                  |     |
                  |     +---bin           <- classlibrary native code & launcher
                  |     |   |
                  |     |   +---default   <- DRLVM binaries
                  |     |
                  |     \---lib
                  |         |
                  |         +---boot      <- common JARs for bootclasspath
                  |         | 
                  |         +---ext       <- extensions directory
                  |         |
                  |         \---security
                  |
                  +---include             <- JNI & JVMTI headers

              
You can now run DRLVM on the command line or under Eclipse*. For details on 
how to run the VM under Eclipse*, see Getting Started with DRLVM, 
[http://harmony.apache.org/subcomponents/drlvm/getting_started.html].  

 3.1 Build the selected components. 
     
If you're developing only particular component of DRLVM, it is possible to 
optimize recompilation time and re-build just that component invoking 
particular ant script. For example, do clean rebuild of GCv5:
$ ant -f make/vm/gc_gen.xml clean build
Another example, perform incremental build of Jitrino:
$ ant -f make/vm/jitrino.xml

However this way works only if you did not modify any of component's dependencies. 
E.g. if you modified encoder or vmcore while developing Jitrino, you'd want to rebuild 
Jitrino and everything it depends upon:
$ ant jitrino

4. RUNNING DRLVM WITH EXTERNAL CLASS LIBRARIES
----------------------------------------------
 
To run DRLVM with third-party external class libraries, do the following:

1. Check that these class libraries comply with the kernel classes interface 
   and the VMI interfaces, see the description in the directory
   <EXTRACT_DIR>/Harmony/vm/vmcore/src/kernel_classes. 

2. Add external native dynamic libraries (*.dll or *.so files) to the system path or copy 
   the files into the <EXTRACT_DIR>/deploy/jre/bin directory. 

3. Add external library class directories or .jar files into the -Xbootclasspath option. 

Example:
$ java -Xbootclasspath/p:c:\external_library\lib\classes.jar MyApp
 

5. BUILDING AND RUNNING TESTS
-----------------------------

DRLVM provides a fair amount of functional and unit tests, you can build and run 
them all with the following command:
$ ant smoke.test kernel.test cunit.test jvmti.test reg.test hut.test ehwa.test 
The test binaries and run results are placed in "tests" directory tree
nearby "deploy" one:
./build/${OS}_${CPU_arch}_${CXX}_${BUILD_CFG}/tests

For convenience, build also provides aggregative test targets, which run most of the 
tests with a single command:
$ ant test
$ ant test2

Common flags supported by all test suites:
* test.mode - list of predefined VM modes to test (see make/test.properties). 
              "jit,opt,int" by default. Does not affect HUT, cunit and reg tests 
* test.case - name of a single specific test to run
* test.jvm.exe - location of external JVM to be tested 
 

6. TROUBLESHOOTING
-------------------
 
For build troubleshooting information, refer to the Wiki page: 
http://wiki.apache.org/harmony/DrlvmBuildTroubleshooting


7. TODO
--------

For information on TODO issues, refer to the Wiki page:
http://wiki.apache.org/harmony/TODO_List_for_DRLVM  
