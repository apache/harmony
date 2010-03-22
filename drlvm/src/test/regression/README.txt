               Directions for Regression Test Developers
               =========================================

    This file describes conventions accepted for VM regression test suite
    and steps needed to integrate new tests into the suite.
    ---------------------------------------------------------------------


Test Format and Test Naming conventions
---------------------------------------

 1. The tests are supposed to be in JUnit test format

 2. The source files of each regression test should be placed in a directory
    named H{NUMBER}, where {NUMBER} (here and below) is an ID
    of JIRA issue the test is related to.
        For example: H1234

 3. The name of the main class should end with Test suffix
        For example: SomethingTest.java

 4. The package of the test should be
    org.apache.harmony.drlvm.tests.regression.h{NUMBER}
       For example: org.apache.harmony.drlvm.tests.regression.h1234

 5. The source files for the tests can be in Java, Java Assembler, and
    C/C++ programming languages.


New Test Integration
--------------------

To integrate new test in the regression test suite:

 1. Make sure it complies with conventions described above.

 2. Put the test sources into H{NUMBER} directory.

 3. By default test execution entry point is a class ending with Test
    suffix. It will be automatically launched by regression test
    infrastructure as a JUnit test. If there are several classes ending
    with Test suffix all of them will be launched.

After that regression test infrastructure will be able to compile and
execute your test.


Custom Test Launchers
---------------------

If there is a need in some more sophisticated test launching mechanism
(JVM parameters or environment variables need to be specified in order to
reproduce the problem, etc.) it can be done by using CUSTOM LAUNCHER.
It is an Ant build file named run.test.xml and placed near the test
sources. The target named "run-test" should specify the way in which the
test is launched. This Ant's file is executed in context of DRLVM build
system and inherits all the properties, references, and macrodefinitions
defined there. The custom launcher is started from
            build/make/targets/reg.test.run.xml
file which contains some useful macrodefinitions to simplify custom
launchers.

 Note:  If there are tests with Test suffix along with the custom launcher
 -----  they won't be automatically launched by regression testing
        infrastructure.


Some useful properties for custom launchers:
-------------------------------------------

    ${reg.test.class.path} - the path to where java and
        java assembler sources were compiled

    ${reg.test.2launch.native.path} - the path to where native
        sources were compiled


Examples
--------
The best way to learn something is to use examples. Please, use existing 
regression tests for this purpose. The following tests can act as a starting 
points:

    H1694 - Simple JUnit test automatically compiled and 
            launched by regression testing framework.

    H2151 - The test consisting of java and native code.
            It uses custom launcher for execution.

    H788  - The test consisting of java and java assembler
            sources. Source files are automatically compiled by
            regression testing framework. The entry point for the test
            is JUnit test implemented by Test.java source file.


Test Exclusion
--------------

If there is a serious reason to exclude some test from regression testing
it can be done by means of exclude lists placed under 'excludes' directory.
To exclude the test, say H1234, from regression testing on operating
system OS running on top of platform PLATFORM just put line 'H1234'
(without quotes) in file exclude.OS.PLATFORM . That's all. The possible
values for OS are linux and windows, for PLATFORM are x86 and x86_64 .


Test Execution
--------------
To execute the Regression Test Suite type

    > ant reg.test

in top level directory of DRLVM workspace. This will execute all non-excluded
regression tests for your platform configuration.

Also by providing the value for 'test.case' property you can choose 
which particular regression tests to execute. 
For example, to run only H1234 regression test type the following 
on your command line:

    > ant -Dtest.case=H1234 reg.test

To check for several regressions use coma or space separated list as a value for
the property. So the following command:

    > ant -Dtest.case="H1234,H4321" reg.test

will execute two regression tests - H1234 and H4321.
 
 Note:  If some of the tests selected by test.case property are in
 -----  exclude list, they will be executed anyway. So you can use this property
        to go around exclude lists.

