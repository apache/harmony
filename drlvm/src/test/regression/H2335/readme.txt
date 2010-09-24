Test are written for http://issues.apache.org/jira/browse/HARMONY-2335.

Please note that test code is dummy - it is not required actually. The bug was
reporduced if you run DRLVM with java.library.path pointed to any directory e.g.:

   <trunk>/drlvm/build/deploy/jdk/jre/bin/java -Djavaa.library.path=tmp NoClass

The failed test output is:
   HMYEXEL062E Internal VM error: Failed to create Java VM
   FAILED to invoke JVM.

