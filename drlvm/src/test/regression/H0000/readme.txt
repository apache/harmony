Test sources were taken and modified from src/test/jni/nio.
Currenlty the test fails with the following message:

HMYEXEL062E Internal VM error: Failed to create Java VM
FAILED to invoke JVM.

But it is impossible to determine wether the test pass or not 
because in both cases VM returns 0 status code.

It demonstrates limitations of using of JUnit test format for VM testing.