This directory contains the style sheet and ant script needed to generate
Doxygen documentation from: 
    - The kernel classes source in ../modules/luni-kernel/src/main/java and 
    	../modules/security-kernel/src/main/java
    - The native source in ../native-src/shared and
    	../native-src/${target.platform}

where ${target.platform} is either win.IA32 or linux.IA32.

The generated documentation will be placed into kernel_doc and vm_doc
directories respectively.
