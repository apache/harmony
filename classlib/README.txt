README
======


This directory contains the Apache Harmony class library code.


Contents
--------
After checking out the code onto disk at <EXTRACT_DIR> you should find the
following directories under <EXTRACT_DIR> :

*  make     - support for building the source code and packaging the results.

*  depends  - support files (both text and binary) that are required to build
              the class libraries and any native libraries required. Includes
              several third party source files as detailed in the file
              Harmony/depends/oss/README.txt.
               
*  doc      - configuration files and images that are required to generate
              HTML documentation of the Java and C source using the Doxygen
              documentation system. Pre-genenerated HTML is included. 
               
*  modules  - Java, C/C++, assembly source files that can be compiled into the
              class library component files.

*  support  - Java source files used to support test cases across various modules.


Export Notice
-------------

This distribution includes cryptographic software.  The country in 
which you currently reside may have restrictions on the import, 
possession, use, and/or re-export to another country, of 
encryption software.  BEFORE using any encryption software, please 
check your country's laws, regulations and policies concerning the
import, possession, or use, and re-export of encryption software, to 
see if this is permitted.  See http://www.wassenaar.org/ for more
information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity 
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms.  The form and manner of this Apache Software Foundation
distribution makes it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception (see the BIS 
Export Administration Regulations, Section 740.13) for both object 
code and source code.

The following provides more details on the included cryptographic
software:

Apache Harmony contains code that is specifically designed to enable
cryptography.  In particular Apache Harmony contains an implementation
the Java cryptographic extensions.  In addition, binary distributions
of Apache Harmony may contain cryptographic functionality provided by
The Legion of the Bouncy Castle (http://www.bouncycastle.org).


Pre-requisites for Building
---------------------------
In order to build the source code contained in the modules and support
directories, it is necessary to configure the following tools in the user 
environment. That is, the working environment should be such that the PATH 
environment variable contains all of the directories where the executables of
the tools listed below are located and that all of those executables can be 
successfully invoked from the command line.


* C compiler - on Windows, the Microsoft(R) 32-bit C/C++ Compiler and on
               Linux, the GNU project C/C++ Compiler.

* Java compiler - By default, the build scripts are setup to use the Eclipse
                  compiler (ECJ). The ECJ needs to be in the Ant class path
                  to execute correctly. The ECJ JAR is downloaded when the 
                  'fetch-depends' target is run on the top-level build script.
                  Once downloaded copy the JAR from HARMONY_TRUNK/depends/ecj_x.x
                  folder to the ANT_HOME/lib folder.
                  
* Apache Ant - A Java based build tool. See http://ant.apache.org/.
               It's suggested that the ANT_OPTS environment variable be set
               to a value of "-Xms256M -Xmx512M" while running the build scripts
               for Harmony.

* Doxygen - the open source documentation system for a variety of programming 
            languages including C, C++ and Java.
            See http://www.doxygen.org
            

The top-level Ant script <EXTRACT_DIR>/build.xml has a default target 
which builds both the Java source and C source files. It is expected
therefore that, at a minimum, a C compiler and a Java compiler be available.
Doxygen is only necessary if generation of HTML documentation from the
source code is to be carried out by invoking Ant with the "doc" target on
the <EXTRACT_DIR>/build.xml script. As a convenience, pre-generated
HTML files are already stored in subversion.



Building
--------
The simplest way to get started is to change directory into <EXTRACT_DIR> and
then type "ant" to run Apache Ant against the default target of the build.xml
file. Provided that the required compilers are available, Ant will proceed to
compile all the class library source code.


Building Java
-------------
The class files output from the Java compilation will be assembled into a set
of JAR files which reflect the components of the class libraries (see
http://wiki.apache.org/harmony/ClassLibrary for more information). Together with
the required support files, these JAR files will then be laid out in the boot
directory of the following directory tree structure ...

<EXTRACT_DIR>
       |
       \---deploy
             |
             \---jdk
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
Next, the source files contained under <EXTRACT_DIR>/modules will be
built using the available C/C++/ASM compiler. The output from this stage will be the 
creation of a launcher executable (java.exe on Windows, java on Linux) in the 
bin directory of the above layout together with a number of shared libraries
(with the .dll extension on Windows, and .so on Linux). 

On Windows if the native build exits with the message :

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
Two sets of HTML files can be found under the <EXTRACT_DIR>/doc directory. 

* kernel_doc - contains HTML documentation on the set of Java classes called the
               "kernel classes". These are the set of classes which - under the
               Harmony class library architecture - are closely tied to
               the structure of the VM. 
               Browse <EXTRACT_DIR>/doc/kernel_doc/html/index.html

* vm_doc - contains HTML documentation concerned with how the Java class
           libraries may be used on different VM implementations. This includes
           an overview of the external dependencies of the class libraries 
           and introduces a VM interface through which the class library natives
           and VM can interoperate with one another.        
           Browse <EXTRACT_DIR>/doc/vm_doc/html/index.html

Running the "doc" target of the top-level Ant script
<EXTRACT_DIR>/build.xml will refresh the contents of these directories.
The target makes use of the doxygen tool and so expects the doxygen executable
to be available in the current user environment (i.e the user PATH environment
variable should contain an entry for <DOXYGEN_INSTALL_DIR>/bin ). 



Modifying the Java Build Compiler
---------------------------------
By default, the Java compiler is set to use the ECJ compiler. This value is
set in the HARMONY_TRUNK/make/properties.xml and looks like the following XML
element.

<property name="hy.javac.compiler" value="org.eclipse.jdt.core.JDTCompilerAdapter" />

The compiler can be set to "modern", as per the Ant manual, which will cause Ant
to use the JDK's 'javac' tool. You may also need to change the 'build.compilerarg'
to '-nowarn' instead of the JDT '-warn:none'.


Options for ECJ compiler
------------------------

For more information on configuring the ECJ, check out this document on the batch
compiler - http://dev.eclipse.org/viewcvs/index.cgi/*checkout*/jdt-core-home/howto/batch%20compile/batchCompile.html?rev=HEAD&content-type=text/html.


What's Next ?
-------------

The class libraries do not provide the full JSE API. Instead the included source
is only a subset of the Java 1.5 API.


To test out the functionality of the built class library components a compatible
VM needs to be obtained.  A compatible VM implements the Virtual Machine Interface
as described in the documentation above.


Troubleshooting & Known Problems
--------------------------------

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

error while loading shared libraries: libhyprt.so: cannot open shared 
object file: No such file or directory

On some systems this error can occur even when the shared library
(e.g. <EXTRACT_DIR>/deploy/jdk/jre/bin/libhyprt.so) has been built correctly
and is present in the correct location. This is not a problem with the built
shared library but instead is dependent on how the operating system locates and 
loads dynamically linked libraries at runtime. Updating the LD_LIBRARY_PATH 
environment variable to include the directory
<EXTRACT_DIR>/deploy/jdk/jre/bin should solve this. An alternative remedy 
would be to add new entries for each of the shared libraries built from the
contributed native source code to the local /etc/ld.so.conf file and then 
running the ldconfig program to rebuild the /etc/ld.so.cache. 

                                  ----------

Linux users may need to update their GNU Make (http://www.gnu.org/software/make/)
to version 3.80 or later if they see the following error message when attempting
to build native source:

: No such file or directory
make: *** [../libvmi.so] Error 1

(The library name may be different)

                                  ----------

If the build fails with an odd error that similar to the following output snippet,
try setting the ANT_OPTS environment variable to "-Xms256M -Xmx512M".

<snippet>
    [javac] ----------
    [javac] 1. ERROR in C:\dev\harmony\enhanced\classlib\trunk\modules\accessibi
lity\src\main\java\javax\accessibility\Accessible.java (at line 0)
    [javac]     /*
    [javac]     ^
    [javac] Internal compiler error
    [javac] java.lang.OutOfMemoryError: Java heap space

    [javac] ----------

BUILD FAILED
C:\dev\harmony\enhanced\classlib\trunk\build.xml:108: The following error occurr
ed while executing this line:
C:\dev\harmony\enhanced\classlib\trunk\make\build-java.xml:143: java.lang.reflec
t.InvocationTargetException
</snippet>
