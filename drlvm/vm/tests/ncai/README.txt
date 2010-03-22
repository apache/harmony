The environmental variables must be set:


   VTSSUITE_ROOT=... <place where suite was installed, ...vm/tests/ncai>
   CLASSPATH=... <...vm/tests/ncai/bin/classes>
   REF_JAVA_HOME=... <reference 1.5.0 java home>
       On Linux, there is ref java in /nfs/ims/proj/drl/mrt/install/java/bea/jdk_1.5.0_06_x86/
   TST_JAVA_HOME=... <tested java>

start.sh [-o b|r|br] [-sin testname1 [testname2]...[testname...N] ]|
         [-grp groupname]|[-all]

   -o r  - run test(s) only (by default)
   -o b  - build test(s) only
   -o br - build and run tests

if option "-o" is absent test(s) will run only.

   -sin - after this option you can point one or several tests for selecting
   -grp - only 1 test group is selected
