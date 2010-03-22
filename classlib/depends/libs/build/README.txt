Dependencies for AWT
--------------------

For Debian/Ubuntu, it should be possible to run:

  ant fetch-depends

to construct the dependency tree.

On other platforms the dependencies must be build using the following
instructions.


1. Building the external libraries
----------------------------------

To enable image decoding (JPEG and PNG images) and color management
with awt, build the IJG JPEG, Libpng and the Little CMS libraries.

After performing the instructions below, you create the following
directory tree structure:

<EXTRACT_DIR>/depends/libs/build
       |
       \---jpeg
       |    |
       |    +--- cderror.h
       |    +--- jinclude.h
       |    +--- jpeglib.h
       |    +--- cdjpeg.h
       |    +--- jdct.h
       |    +--- jmemsys.h
       |    +--- jversion.h
       |    +--- jchuff.h
       |    +--- jdhuff.h
       |    +--- jmorecfg.h
       |    +--- jerror.h
       |    +--- jpegint.h
       |    +--- jconfig.lnx and/or jconfig.vc
       |    +--- libjpeg.lib and/or libjpeg.linux.x86 and/or
       |         libjpeg.linux.x86_64
       \---png
       |    |
       |    +--- png.h
       |    +--- pngconf.h
       |    +--- libpng.lib and/or libpng.linux.x86 and/or libpng.linux.x86_64
       \---lcms
       |    |
       |    +--- icc34.h
       |    +--- lcms.h
       |    +--- lcms114.lib and/or liblcms.linux.x86 and/or
       |         liblcms.linux.x86_64
       ...

NOTE: The tree above indicates the files required for this contribution,
      not all the files distributed with each library.

Further in the document, <EXTERNAL_LIBS_DIR> denotes the directory
<EXTRACT_DIR>/depends/libs/build.

1.1 Building the IJG IPEG library


    0. Source can be found here :
   
          http://www.ijg.org/files/

    System: Windows* IA-32

    1. Change the directory to the IJG JPEG library source directory;
       normally, jpeg-6b.
    2. Copy the file jconfig.vc to jconfig.h.
    3. Copy the jconfig.vc file to the <EXTERNAL_LIBS_DIR>/jpeg/ directory.
    4. Start the Microsoft* Windows* SDK build environment or
       the Visual Studio .NET* 2003 Command Prompt.
    5. Build the library by running:

	    For the release version: nmake nodebug=1 /f makefile.vc
	    For the debug version: nmake /f makefile.vc

    6. Copy the file libjpeg.lib to the <EXTERNAL_LIBS_DIR>/jpeg directory.
    7. Copy the required header files to the <EXTERNAL_LIBS_DIR>/jpeg
       directory.  For a list of required files, see the tree view above.

    System: Linux* IA-32

    1. Change the directory to the IJG JPEG library source directory;
       normally, jpeg-6b.
    2. Configure the build by running:

	    For the release version: ./configure CFLAGS="-O2 -fpic"
	    For the debug version: ./configure CFLAGS="-g -fpic"

    3. Copy the file jconfig.h created during the previous step to
       the <EXTERNAL_LIBS_DIR>/jpeg/jconfig.lnx directory.
    4. To build the library, run:

	    make

    5. Copy the resulting libjpeg.a file to the file
       <EXTERNAL_LIBS_DIR>/jpeg/libjpeg.linux.x86.

    6. Copy the required header files to the <EXTERNAL_LIBS_DIR>/jpeg
       directory.

1.2 Building the Libpng library

    0. Source can be found here : 

        http://www.libpng.org/pub/png/libpng.html
        http://www.zlib.net/

    System: Windows* IA-32

    1. Place the libpng source directory and zlib source directory
       in a convenient location and rename the zlib source directory to zlib.
    2. Change the working directory to the libpng source directory.
    3. Edit the file ./scripts/makefile.vcwin32 by changing the following line:

	    CFLAGS  = -nologo -MD -O2 -W3 -I..\zlib

	    For the release version, type: CFLAGS = -nologo -MT -O2 -W3 -I..\zlib .
	    For the debug version, type: CFLAGS = -nologo -MTd -W3 -I..\zlib .

    4. Start the Microsoft* Windows* SDK build environment
       or the Visual Studio .NET* 2003 Command Prompt.
    5. Subsequently run the following commands:

	    nmake /f scripts\makefile.vcwin32 clean
	    nmake /f scripts\makefile.vcwin32

    6. Copy the files libpng.lib,  png.h and pngconf.h to
       the directory <EXTERNAL_LIBS_DIR>/png.

   System: Linux* IA-32

   1. Change the working directory to the libpng source directory.
   2. Configure the build by running:

	    For the release version: ./configure CFLAGS="-O2 -fpic"
	    For the debug version: ./configure CFLAGS="-g -fpic"

   3. To build the library, run:

   	   make

   4. Copy the resulting ./.libs/libpng.a file the following file
      <EXTERNAL_LIBS_DIR>/png/libpng.linux.x86

   5. Copy the files png.h and pngconf.h to the
      <EXTERNAL_LIBS_DIR>/png directory.

1.3 Building the Little CMS library


   0. Source can be found here : 

    http://www.littlecms.com/downloads.htm

   System: Windows* IA-32

   1. Change the working directory to the LCMS source directory.
   2. Open the Visual Studio* solution from the ./Projects/VC7/ directory.
   3. Set the LCMS static library as a startup project.
   4. Specify the configuration: debug or release.
   5. Open the project properties. Go to C/C++/Code generation
      and change the run-time library to multi-threaded: /MT or /MTd.
   6. Go to C/C++/Preprocessor and add __CONSOLE__ to the preprocessor
      definitions.
   7. Build the LCMS static library.
   8. Copy the file lcms114.lib to the <EXTERNAL_LIBS_DIR>/lcms directory.
	    NOTE: Use the file lcms114d.lib for the debug configuration.

      NOTE : v1.15 of lcms seems to only create lcms.lib

   9. Copy ./include/icc34.h and ./include/lcms.h to the
      <EXTERNAL_LIBS_DIR>/lcms directory.

   System: Linux* IA-32

   1. Change the working directory to the LCMS source directory.
   2. Configure the build by running:

   	For the release version: ./configure CFLAGS="-O2 -fpic"
   	For the debug version: ./configure CFLAGS="-g -fpic"

   3. To build the library, run:

   	make

   4. Copy the file ./src/.libs/liblcms.a to the following file
      <EXTERNAL_LIBS_DIR>/lcms/liblcms.linux.x86

   5. Copy the files ./include/icc34.h and ./include/lcms.h
      to the <EXTERNAL_LIBS_DIR>/lcms directory.
