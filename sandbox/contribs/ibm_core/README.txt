README
======



Archive Contents
----------------
After extracting the archive onto disk at <EXTRACT_DIR> you should find the
following directories under <EXTRACT_DIR>/Harmony :


*  depends  - support files (both text and binary) that are required to build
              the class libraries and any native libraries required. Includes
              several third party source files as detailed in the file
              Harmony/depends/oss/README.txt.
               
*  doc      - configuration files and images that are required to generate
              HTML documentation of the Java and C source using the Doxygen
              documentation system. Pre-genenerated HTML is included. 
               
*  java-src - Java source files that can be compiled into the class library
              component files
              
*  native-src -  C language source files plus the required makefiles that
                 together can be used to build the natives libraries required
                 by the Java class library components to function correctly.
                   


Pre-requisites for Building
---------------------------
In order to build the Java and C source contained in the java-src and native-src
directories, it is necessary to configure the following tools in the user 
environment. That is, the working environment should be such that the PATH 
environment variable contains all of the directories where the executables of
the tools listed below are located and that all of those executables can be 
successfully invoked from the command line.


* C compiler - on Windows, the Microsoft(R) 32-bit C/C++ Compiler and on
               Linux, the GNU project C/C++ Compiler.

* Java compiler - e.g. as delivered in a Java SDK. The compiler must be 
                  capable of handling Java 1.4 source.
                  See http://www.ibm.com/developerworks/java/jdk/index.html
                  
                  Although it cannot be invoked through an executable, it is 
                  also possible to use the JDT compiler as delivered in a
                  standard Eclipse download to accomplish the compiling of the 
                  source under the java-src directory. See the section on 
                  "Modifying the Java Build Compiler" for more details. 
                  
* Apache Ant - the Java based build tool. See http://ant.apache.org

* Doxygen - the open source documentation system for a variety of programming 
            languages including C, C++ and Java.
            See http://www.doxygen.org
            

The top-level Ant script <EXTRACT_DIR>/Harmony/build.xml has a default target 
which builds both the Java source and C source files. It is expected
therefore that, at a minimum, a C compiler and a Java compiler be available.
Doxygen is only necessary if generation of HTML documentation from the
source code is to be carried out by invoking Ant with the "doc" target on
the <EXTRACT_DIR>/Harmony/build.xml script. As a convenience, pre-generated
HTML files are already included.



Building
--------
The simplest way to get started is to change directory into
<EXTRACT_DIR>/Harmony and then type "ant" to run Apache Ant against the
default target of the build.xml file. Provided that both a C compiler and a Java
compiler are available, Ant will proceed to compile all Java source beneath
the <EXTRACT_DIR>/Harmony/java-src folders and all C source beneath the
<EXTRACT_DIR>/Harmony/native-src folders.


Building Java
-------------
The class files output from the Java compilation will be assembled into a set
of JAR files which reflect the proposed components of the class libraries (see
http://wiki.apache.org/harmony/ClassLibrary for more information). Together with
the required support files, these JAR files will then be laid out in the boot
directory of the following directory tree structure ...

<EXTRACT_DIR>/Harmony
       |
       \---deploy
             |
             \---jre
                  |
                  +---bin           <- classlibrary native code & launcher
                  |   |
                  |   +---default   <- VM-specific files for launcher's default VM
                  |   +---vm1       <- VM-specific files for 'vm1'
                  |   \---vm2       <- VM-specific files for 'vm2', and so on
                  |
                  \---lib
                      |
                      +---boot      <- common JARs for bootclasspath
                      | 
                      +---ext       <- extensions directory
                      |
                      \---security
    


Building Natives
----------------
Next, the source files contained under <EXTRACT_DIR>/Harmony/native-src will be
built using the available C compiler. The output from this stage will be the 
creation of a launcher executable (java.exe on Windows, java on Linux) in the 
bin directory of the above layout together with a number of shared libraries
(with the .dll extension on Windows, and .so on Linux). 

A record of the progress of the native build step is kept in the text file 
<EXTRACT_DIR>/Harmony/native-src/<target platform>/build.log where
<target platform> will have the value "win.IA32" or "linux.IA32". The contents
of this file are especially useful in tracking down problems in your build 
environment. For example, on Windows if the native build exits on error and 
the resulting build.log file contains the message :

fatal error U1052: file 'ntwin32.mak' not found

this is an indication that the required build environment has not been properly
configured in the working console window. 



Build Output
------------
The supplied source is built into a set of Java class libraries and the native 
shared libraries necessary to support their correct functioning. In order to 
be used to run a compiled Java application the built class libraries still
require a virtual machine component. 



Building Documentation
----------------------
Two sets of HTML files can be found under the <EXTRACT_DIR>/Harmony/doc 
directory. 

* kernel_doc - contains HTML documentation on the set of Java classes called the
               "kernel classes". These are the set of classes which - under the
               architecture proposed in this contribution - are closely tied to
               the structure of the VM. 
               Browse <EXTRACT_DIR>/Harmony/doc/kernel_doc/html/index.html

* vm_doc - contains HTML documentation concerned with how the Java class
           libraries may be used on different VM implementations. This includes
           an overview of the external dependencies of the class libraries 
           and introduces a VM interface through which the class library natives
           and VM can interoperate with one another.        
           Browse <EXTRACT_DIR>/Harmony/doc/vm_doc/html/index.html

Running the "doc" target of the top-level Ant script
<EXTRACT_DIR>/Harmony/build.xml will refresh the contents of these directories.
The target makes use of the doxygen tool and so expects the doxygen executable
to be available in the current user environment (i.e the user PATH environment
variable should contain an entry for <DOXYGEN_INSTALL_DIR>/bin ). 



Modifying the Java Build Compiler
---------------------------------
When compiling the Java source files the top-level Ant script calls another
Ant script, <EXTRACT_DIR>/Harmony/java-src/build.xml. By opening up this Ant 
file for editing and making a small change to the "build.compiler" property
declared near the top of the file it is possible to specify any Java compiler of
choice - provided it is capable of handling Java 1.4 source code. For example,
to use the JDT compiler built into Eclipse this property should be changed to
use the Eclipse compiler adapter ...

<property name="build.compiler" value="org.eclipse.jdt.core.JDTCompilerAdapter" />

So that Ant can actually locate this Eclipse compiler adapter during a build, 
the location of the supporting JAR files in the Eclipse distribution need to be
appended to the CLASSPATH environment variable. For an Eclipse 3.1 distribution
this amounts to adding <ECLIPSE_INSTALL>/plugins/org.eclipse.jdt.core_3.1.0.jar
together with the jdtCompilerAdapter.jar it contains. 

Remember that the Java source files located under
<EXTRACT_DIR>/Harmony/java-src are intended for consumption by a compiler that
understands Java 1.4 source code.

For further information on this topic please refer to the Eclipse batch compiler
website, the Ant documentation for the "javac" task and to the Eclipse help system
contents. 

http://dev.eclipse.org/viewcvs/index.cgi/%7Echeckout%7E/jdt-core-home/howto/batch%20compile/batchCompile.html



What's Next ?
-------------

The class libraries included in this contribution do not provide the full J2SE
API. Instead the included source is only a subset of the Java 1.4.2 API.

In addition there is minimal support for the Java security framework. Users 
wishing to experiment with introducing JCE support to these class libraries are 
recommended to download the latest release of the Bouncy Castle Cryptography API
and install the provider jar into the <EXTRACT_DIR>/Harmony/deploy/jre/lib/ext
location. 
See http://www.bouncycastle.org

To test out the functionality of the built class library components a compatible
VM needs to be obtained.  A compatible VM implements the Virtual Machine Interface
as described in the documentation above.



Troubleshooting & Known Problems
--------------------------------

The java launcher has a bug which makes it sensitive to the JAVA_HOME
environment setting. Until this bug is fixed users should be aware that When
running Java applications with the built class library components on a
compatible VM the JAVA_HOME variable should either be unset or else explicitly
set to EXTRACT_DIR>/Harmony/deploy/jre. Any other value risks crashing
the launcher. 

                                  ----------

Linux users may need to install an appropriate libc compatibility patch to their
operating system if they see the following error message when attempting to run
a Java application with the built class library components on a compatible VM :

<error: unable to load ICUInterface34 (libstdc++.so.5: cannot open shared object
file: No such file or directory)>

Where to obtain the required patch and the precise means of applying it will 
obviously differ according to Linux distribution. On Debian the advanced package
tool apt-get could be used as follows :

user@server:~$> apt-get install libstdc++5

On a Red Hat Enterprise Linux machine the rpm tool may be used to install the 
equivalent package thus : 

user@server:~$> rpm -Uvh compat-libstdc++-33-3.2.3-47.3.i386.rpm

Consult the system administration documentation of your particular Linux
distribution for more information.

                                  ----------

Linux users may need to update the value of their LD_LIBRARY_PATH environment 
variable if they see the following error message when attempting to run a Java
application with the built class library components on a compatible VM : 

error while loading shared libraries: libhysig.so: cannot open shared 
object file: No such file or directory

On some systems this error can occur even when the shared library
(e.g. <EXTRACT_DIR>/Harmony/deploy/jre/bin/libhysig.so) has been built correctly
and is present in the correct location. This is not a problem with the built
shared library but instead is dependent on how the operating system locates and 
loads dynamically linked libraries at runtime. Updating the LD_LIBRARY_PATH 
environment variable to include the directory
<EXTRACT_DIR>/Harmony/deploy/jre/bin should solve this. An alternative remedy 
would be to add new entries for each of the shared libraries built from the
contributed native source code to the local /etc/ld.so.conf file and then 
running the ldconfig program to rebuild the /etc/ld.so.cache. 

