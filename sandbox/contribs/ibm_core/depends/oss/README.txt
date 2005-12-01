README
======

This directory contains third party open source software which has been placed
into three separate source archives. During the build of the contributed class 
library component native source under <EXTRACT_DIR>/Harmony/native-src, the
contents of these three archives will be copied to the appropriate part of 
the native source tree for inclusion in the build process. 

Keeping these OSS source files distinct from the rest of the native source tree
helps distinguish third party content from the source delivered in the
contribution archive and so should make it easier to analyze and reason about
the new source. Some small modifications to the included OSS source files were
required so as not to break this contribution's native build process. These
changes are discussed below.

For information about the licenses of the three OSS libraries discussed in this
document please refer to the file <EXTRACT_DIR>/Harmony/THIRD_PARTY_NOTICES.txt.



ICU4C version 3.4
-----------------
ICU4C is a freely available set of libraries that provide Unicode support to
C and C++ applications. The file icu4c-3.4-harmony.zip contains a set of C 
header files (with .h extension) that come straight from the ICU download
available on the ICU home page. These have been grouped into a directory 
called "unicode" which is expanded under the
<EXTRACT_DIR>/Harmony/native-src/<target platform>/include directory as part
of this contribution's native build. The icu4c-3.4-harmony.zip file also
contains an ICUUC library which is expanded to the 
<EXTRACT_DIR>/Harmony/native-src/<target platform>/text directory during the
build where it used as part of the hytext shared library.

The official ICU page is located at
http://www.ibm.com/software/globalization/icu



ICU4JNI version 3.4 with Patch 01
---------------------------------
ICU4JNI is a freely available set of JNI wrappers to the ICU4C functionality
described above.  The ICU4JNI version 3.4 has a bug that causes a memory leak.
This bug has been fixed and released as an official patch.  The version of
ICU4JNI included in this set of files has already had the patch applied.

For further information see
http://www.ibm.com/software/globalization/icu/downloads.jsp#icu4j



FDLIBM version 5.2
------------------
FDLIBM (Freely Distributable LIBM) is a C math library for machines that
can support IEEE 754 floating-point arithmetic. File fdlibm_5.2.zip mostly 
contains the original source as downloaded from the FDLIBM home page with the
exception of a few files whose names clashed with others in the original
native-src folder under <EXTRACT_DIR>/Harmony. In the zip file these files 
have all been given the ".orig" suffix. The modified versions of the files
are to be found (with no such extra suffix) under
<EXTRACT_DIR>/Harmony/native-src/<target platform>/fdlibm. As part of the 
native build process the contents of this zip file are expanded to the
<EXTRACT_DIR>/Harmony/native-src/<target platform>/fdlibm directory. 

The official FDLIBM page is located at http://www.netlib.org/fdlibm/



ZLIB version 1.2.3
------------------
zlib is a freely available compression library. The file zlib_1.2.3.zip
contains almost the original source for version 1.2.3 as downloaded from the
zlib home page. So as not to cause confusion with the makefile developed for
this original code contribution the original Makefile from zlib 1.2.3 has been
renamed Makefile.orig. During the native build of the shared libraries which 
support the class library component jars the contents of this zip get expanded
to the directory <EXTRACT_DIR>/Harmony/native-src/<target platform>/zlib.

The official zlib home page is located at http://www.gzip.org/zlib

