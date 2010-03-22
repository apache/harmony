/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Allows for execution of external applications as subprocesses.
 *
 * @author  Vasily Zakharov
 *
 * @todo    Check with <code>ProcessBuilder</code> for Java 5.0.
 */
public final class SubProcess {

    /**
     * Default argument to {@link #tell(String)}
     * and expect {@link #expect(String)} methods.
     */
    public static final String READY_STRING = "PROCESS_READY"; //$NON-NLS-1$

    /**
     * Process.
     */
    private Process process;

    /**
     * Data input stream.
     */
    private DataInputStream dataInput;

    /**
     * Data output stream.
     */
    private DataOutputStream dataOutput;

    /**
     * Data error stream.
     */
    private DataInputStream dataError;

    /**
     * Process input stream.
     */
    private InputStream processInput;

    /**
     * Process output stream.
     */
    private OutputStream processOutput;

    /**
     * Process error stream.
     */
    private InputStream processError;

    /**
     * Pipe target input stream.
     */
    private InputStream targetInput;

    /**
     * Pipe target output stream.
     */
    private OutputStream targetOutput;

    /**
     * Pipe target error stream.
     */
    private OutputStream targetError;

    /**
     * Creates subprocess with full control of its streams.

     * Equivalent to
     * {@link #SubProcess(String[], boolean, OutputStream, boolean, InputStream, boolean, OutputStream)
     * SubProcess(args, true, System.out, true, System.in, true, System.err)}.
     *
     * @param   args
     *          Program name and command line arguments
     *          (as for {@link Runtime#exec(String[])}).
     *
     * @throws  IOException
     */
    public SubProcess(String[] args) throws IOException {
        this(args, true, System.out, true, System.in, true, System.err);
    }

    /**
     * Creates instance of this class with no control of its streams.
     * If <code>pipe</code> is <code>true</code>, the streams are piped
     * to the respective system streams of the current process.
     * This is equivalent to
     * {@link #SubProcess(String[], boolean, OutputStream, boolean, InputStream, boolean, OutputStream)
     * SubProcess(args, false, System.out, false, System.in, false, System.err)}.
     *
     * If <code>pipe</code> is <code>false</code>, the streams are discarded.
     * This is equivalent to
     * {@link #SubProcess(String[], boolean, OutputStream, boolean, InputStream, boolean, OutputStream)
     * SubProcess(args, false, null, false, null, false, null)}.
     *
     * @param   args
     *          Program name and command line arguments
     *          (as for {@link Runtime#exec(String[])}).
     *
     * @param   pipe
     *          If <code>true</code>, the streams are piped
     *          to the respective system streams of the current process,
     *          if <code>false</code>, the streams are discarded.
     *
     * @throws  IOException
     */
    public SubProcess(String[] args, boolean pipe) throws IOException {
        this(args, false, (pipe ? System.out : null),
                   false, (pipe ? System.in : null),
                   false, (pipe ? System.err : null));
    }

    /**
     * Creates instance of this class with no control of its streams.
     * This is equivalent to
     * {@link #SubProcess(String[], boolean, OutputStream, boolean, InputStream, boolean, OutputStream)
     * SubProcess(args, false, outputStream, false, inputStream, false, errorStream)}.
     *
     * @param   args
     *          Program name and command line arguments
     *          (as for {@link Runtime#exec(String[])}).
     *
     * @param   outputStream
     *          Output stream to pipe program input to
     *          if <code>inputControl</code> is <code>false</code>.
     *          May be <code>null</code>,
     *          in this case input from the program is discarded.
     *
     * @param   inputStream
     *          Input stream to pipe to the program output stream
     *          if <code>outputControl</code> is <code>false</code>.
     *          May be <code>null</code>,
     *          in this case the program output stream is closed.
     *
     * @param   errorStream
     *          Error stream to pipe program error input to
     *          if <code>errorControl</code> is <code>false</code>.
     *          May be <code>null</code>,
     *          in this case error input from the program is discarded.
     *
     * @throws  IOException
     */
    public SubProcess(String[] args,
            OutputStream outputStream, InputStream inputStream,
            OutputStream errorStream) throws IOException {
        this(args, false, outputStream, false, inputStream, false, errorStream);
    }

    /**
     * Creates instance of this class.
     *
     * @param   args
     *          Program name and command line arguments
     *          (as for {@link Runtime#exec(String[])}).
     *
     * @param   inputControl
     *          If <code>true</code>, input from the program is available
     *          to {@link #expect()} methods and <code>outputStream</code>
     *          parameter is ignored, otherwise it is piped to the specified
     *          <code>outputStream</code>.
     *
     * @param   outputStream
     *          Output stream to pipe program input to
     *          if <code>inputControl</code> is <code>false</code>.
     *          May be <code>null</code>,
     *          in this case input from the program is discarded.
     *
     * @param   outputControl
     *          If <code>true</code>, output stream to the program is available
     *          to {@link #tell()} methods and <code>inputStream</code>
     *          parameter is ignored, otherwise the specified
     *          <code>inputStream</code> is piped to program output stream.
     *
     * @param   inputStream
     *          Input stream to pipe to the program output stream
     *          if <code>outputControl</code> is <code>false</code>.
     *          May be <code>null</code>,
     *          in this case the program output stream is closed.
     *
     * @param   errorControl
     *          If <code>true</code>, error input from the program is available
     *          to {@link #expectError()} methods and <code>errorStream</code>
     *          parameter is ignored, otherwise it is piped to the specified
     *          <code>errorStream</code>.
     *
     * @param   errorStream
     *          Error stream to pipe program error input to
     *          if <code>errorControl</code> is <code>false</code>.
     *          May be <code>null</code>,
     *          in this case error input from the program is discarded.
     *
     * @throws  IOException
     */
    public SubProcess(String[] args,
            boolean inputControl, OutputStream outputStream,
            boolean outputControl, InputStream inputStream,
            boolean errorControl, OutputStream errorStream) throws IOException {
        process = Runtime.getRuntime().exec(args);

        processInput = process.getInputStream();
        processOutput = process.getOutputStream();
        processError = process.getErrorStream();

        targetInput = inputStream;
        targetOutput = outputStream;
        targetError = errorStream;

        if (inputControl) {
            dataInput = new DataInputStream(processInput);
        } else {
            dataInput = null;
            doPipeInput();
        }

        if (outputControl) {
            dataOutput = new DataOutputStream(processOutput);
        } else {
            dataOutput = null;
            doPipeOutput();
        }

        if (errorControl) {
            dataError = new DataInputStream(processError);
        } else {
            dataError = null;
            doPipeError();
        }
    }

    /**
     * Discards the remaining input.
     * Usable when <code>inputControl</code> is enabled
     * but there's nothing else to {@linkplain #expect() expect}.
     */
    public void discardInput() {
        pipeInput(null);
    }

    /**
     * Pipes the remaining input to the target output stream
     * specified in <a href="#constructor_detail">constructor</a>.
     * Usable when <code>inputControl</code> is enabled
     * but there's nothing else to {@linkplain #expect() expect}.
     */
    public void pipeInput() {
        pipeInput(targetOutput);
    }

    /**
     * Pipes the remaining input to the specified output stream.
     * Usable when <code>inputControl</code> is enabled
     * but there's nothing else to {@linkplain #expect() expect}.
     *
     * @param   outputStream
     *          Output stream to pipe program input to.
     *          May be <code>null</code>,
     *          in this case input from the program is discarded.
     */
    public void pipeInput(OutputStream outputStream) {
        if (dataInput != null) {
            dataInput = null;
            targetOutput = outputStream;
            doPipeInput();
        }
    }

    /**
     * Creates pipe from process input to target output.
     */
    private void doPipeInput() {
        new StreamPipe(processInput, targetOutput).start();
    }

    /**
     * Closes the program output.
     * Usable when <code>outputControl</code> is enabled
     * but there's nothing else to {@linkplain #tell() tell}.
     */
    public void closeOutput() {
        pipeOutput(null);
    }

    /**
     * Pipes the target input stream specified in
     * <a href="#constructor_detail">constructor</a>
     * to the program output stream.
     * Usable when <code>outputControl</code> is enabled
     * but there's nothing else to {@linkplain #tell() tell}.
     */
    public void pipeOutput() {
        pipeOutput(targetInput);
    }

    /**
     * Pipes the specified input stream to the program output stream.
     * Usable when <code>outputControl</code> is enabled
     * but there's nothing else to {@linkplain #tell() tell}.
     *
     * @param   inputStream
     *          Input stream to pipe to the program output.
     *          May be <code>null</code>,
     *          in this case the program output stream is closed.
     */
    public void pipeOutput(InputStream inputStream) {
        if (dataOutput != null) {
            dataOutput = null;
            targetInput = inputStream;
            doPipeOutput();
        }
    }

    /**
     * Creates pipe from target input to process output
     * or closes process output if target input is <code>null</code>.
     */
    private void doPipeOutput() {
        if (targetInput != null) {
            new StreamPipe(targetInput, processOutput).start();
        } else {
            try {
                processOutput.close();
            } catch (IOException e) {}
        }
    }

    /**
     * Discards the remaining error input.
     * Usable when <code>errorControl</code> is enabled
     * but there's nothing else to {@linkplain #expectError() expect}.
     */
    public void discardError() {
        pipeError(null);
    }

    /**
     * Pipes the remaining error input to the target error stream
     * specified in <a href="#constructor_detail">constructor</a>.
     * Usable when <code>errorControl</code> is enabled
     * but there's nothing else to {@linkplain #expectError() expect}.
     */
    public void pipeError() {
        pipeError(targetError);
    }

    /**
     * Pipes the remaining error input to the specified error stream.
     * Usable when <code>errorControl</code> is enabled
     * but there's nothing else to {@linkplain #expectError() expect}.
     *
     * @param   errorStream
     *          Error stream to pipe program error input to.
     *          May be <code>null</code>,
     *          in this case error input from the program is discarded.
     */
    public void pipeError(OutputStream errorStream) {
        if (dataError != null) {
            dataError = null;
            targetError = errorStream;
            doPipeError();
        }
    }

    /**
     * Creates pipe from process error input to target error output.
     */
    private void doPipeError() {
        new StreamPipe(processError, targetError).start();
    }

    /**
     * Waits for subprocess to terminate and returns its exit code.
     *
     * @return  The subprocess exit code.
     */
    public int waitFor() {
        while (true) {
            try {
                return process.waitFor();
            } catch (InterruptedException e) {}
        }
    }

    /**
     * Destroys this subprocess.
     */
    public void destroy() {
        process.destroy();
        dataInput = null;
        dataOutput = null;
        dataError = null;
    }

    /**
     * Writes the {@linkplain #READY_STRING default "ready" string}
     * to the process output stream.
     *
     * @throws  IllegalStateException
     *          If subprocess output stream control is disabled.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public void tell() throws IllegalStateException, IOException {
        tell(READY_STRING);
    }

    /**
     * Writes the specified string to the process output stream.
     *
     * @param   str
     *          String to write.
     *
     * @throws  IllegalStateException
     *          If subprocess output stream control is disabled.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public void tell(String str) throws IllegalStateException, IOException {
        if (dataOutput == null) {
            // rmi.48=Subprocess output stream control disabled
            throw new IllegalStateException(Messages.getString("rmi.48")); //$NON-NLS-1$
        }
        tell(dataOutput, str);
    }

    /**
     * Writes the {@linkplain #READY_STRING default "ready" string}
     * to the {@linkplain System#out system output stream}.
     *
     * This static method is usable by child subprocesses
     * for communication with the parent process.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public static void tellOut() throws IOException {
        tellOut(READY_STRING);
    }

    /**
     * Writes the specified string to the
     * {@linkplain System#out system output stream}.
     *
     * This static method is usable by child subprocesses
     * for communication with the parent process.
     *
     * @param   str
     *          String to write.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public static void tellOut(String str) throws IOException {
        tell(new DataOutputStream(System.out), str);
    }

    /**
     * Writes the {@linkplain #READY_STRING default "ready" string}
     * to the {@linkplain System#err system error stream}.
     *
     * This static method is usable by child subprocesses
     * for communication with the parent process.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public static void tellError() throws IOException {
        tellError(READY_STRING);
    }

    /**
     * Writes the specified string to the
     * {@linkplain System#err system error stream}.
     *
     * This static method is usable by child subprocesses
     * for communication with the parent process.
     *
     * @param   str
     *          String to write.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public static void tellError(String str) throws IOException {
        tell(new DataOutputStream(System.err), str);
    }

    /**
     * Writes the specified string to the process output stream.
     *
     * @param   stream
     *          Stream to write the string to.
     *
     * @param   str
     *          String to write.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    private static void tell(DataOutputStream stream, String str)
            throws IOException {
        stream.writeBytes('\n' + str + '\n');
        stream.flush();
    }

    /**
     * Waits until the {@linkplain #READY_STRING default "ready" string}
     * appears in the program input. Equivalent to
     * {@link #expect(String, boolean, boolean) expect(READY_STRING, false, false)}.
     *
     * @throws  IllegalStateException
     *          If subprocess input stream control is disabled.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public void expect() throws IllegalStateException, IOException {
        expect(READY_STRING);
    }

    /**
     * Waits until the specified string appears in the program input.
     * Equivalent to
     * {@link #expect(String, boolean, boolean) expect(str, false, false)}.
     *
     * @param   str
     *          String to wait for.
     *
     * @throws  IllegalStateException
     *          If subprocess input stream control is disabled.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public void expect(String str) throws IllegalStateException, IOException {
        expect(str, false, false);
    }

    /**
     * Waits until the specified string appears in the program input.
     *
     * @param   str
     *          String to wait for.
     *
     * @param   whole
     *          If <code>true</code>, the whole input lines are compared
     *          to the specified string, otherwise the string is considered
     *          to be found if it appears as a substring in any input line.
     *
     * @param   ignoreCase
     *          If <code>true</code>, case-insensitive comparison is performed.
     *
     * @throws  IllegalStateException
     *          If subprocess input stream control is disabled.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public void expect(String str, boolean whole, boolean ignoreCase)
            throws IllegalStateException, IOException {
        if (dataInput == null) {
            // rmi.49=Subprocess input stream control disabled
            throw new IllegalStateException(Messages.getString("rmi.49")); //$NON-NLS-1$
        }
        expect(dataInput, str, whole, ignoreCase);
    }

    /**
     * Waits until the {@linkplain #READY_STRING default "ready" string}
     * appears in the program error input. Equivalent to
     * {@link #expectError(String, boolean, boolean) expectError(READY_STRING, false, false)}.
     *
     * @throws  IllegalStateException
     *          If subprocess error stream control is disabled.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public void expectError() throws IllegalStateException, IOException {
        expectError(READY_STRING);
    }

    /**
     * Waits until the specified string appears in the program error input.
     * Equivalent to
     * {@link #expectError(String, boolean, boolean) expectError(str, false, false)}.
     *
     * @param   str
     *          String to wait for.
     *
     * @throws  IllegalStateException
     *          If subprocess error stream control is disabled.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public void expectError(String str)
            throws IllegalStateException, IOException {
        expectError(str, false, false);
    }

    /**
     * Waits until the specified string appears in the program error input.
     *
     * @param   str
     *          String to wait for.
     *
     * @param   whole
     *          If <code>true</code>, the whole input lines are compared
     *          to the specified string, otherwise the string is considered
     *          to be found if it appears as a substring in any input line.
     *
     * @param   ignoreCase
     *          If <code>true</code>, case-insensitive comparison is performed.
     *
     * @throws  IllegalStateException
     *          If subprocess error stream control is disabled.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public void expectError(String str, boolean whole, boolean ignoreCase)
            throws IllegalStateException, IOException {
        if (dataError == null) {
            // rmi.4A=Subprocess error stream control disabled
            throw new IllegalStateException(Messages.getString("rmi.4A")); //$NON-NLS-1$
        }
        expect(dataError, str, whole, ignoreCase);
    }

    /**
     * Waits until the {@linkplain #READY_STRING default "ready" string}
     * appears in {@linkplain System#in system input stream}. Equivalent to
     * {@link #expectIn(String, boolean, boolean) expectIn(READY_STRING, false, false)}.
     *
     * This static method is usable by child subprocesses
     * for communication with the parent process.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public static void expectIn() throws IOException {
        expectIn(READY_STRING);
    }

    /**
     * Waits until the specified string appears in
     * {@linkplain System#in system input stream}.
     * Equivalent to {@link #expectIn(String, boolean, boolean) expectIn(str, false, false)}.
     *
     * This static method is usable by child subprocesses
     * for communication with the parent process.
     *
     * @param   str
     *          String to wait for.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public static void expectIn(String str) throws IOException {
        expectIn(str, false, false);
    }

    /**
     * Waits until the specified string appears in
     * {@linkplain System#in system input stream}.
     *
     * This static method is usable by child subprocesses
     * for communication with the parent process.
     *
     * @param   str
     *          String to wait for.
     *
     * @param   whole
     *          If <code>true</code>, the whole input lines are compared
     *          to the specified string, otherwise the string is considered
     *          to be found if it appears as a substring in any input line.
     *
     * @param   ignoreCase
     *          If <code>true</code>, case-insensitive comparison is performed.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    public static void expectIn(String str, boolean whole, boolean ignoreCase)
            throws IOException {
        expect(new DataInputStream(System.in), str, whole, ignoreCase);
    }

    /**
     * Waits until the specified string appears in the specified stream.
     *
     * @param   stream
     *          Stream to wait for the string in.
     *
     * @param   str
     *          String to wait for.
     *
     * @param   whole
     *          If <code>true</code>, the whole input lines are compared
     *          to the specified string, otherwise the string is considered
     *          to be found if it appears as a substring in any input line.
     *
     * @param   ignoreCase
     *          If <code>true</code>, case-insensitive comparison is performed.
     *
     * @throws  IOException
     *          If I/O error occurs.
     */
    private static void expect(DataInputStream stream, String str,
            boolean whole, boolean ignoreCase) throws IOException {
        if (ignoreCase && !whole) {
            str = str.toLowerCase();
        }

        while (true) {
            String line = stream.readLine();

            if (line == null) {
                // End of stream
                throw new EOFException();
            }

            if (whole ? (ignoreCase ? line.equalsIgnoreCase(str)
                                    : line.equals(str))
                      : ((ignoreCase ? line.toLowerCase()
                                     : line).indexOf(str) >= 0)) {
                // expectString is found
                return;
            }
        }
    }

    /**
     * Automatically transfers data from input to output stream
     * using new thread.
     *
     * Use {@link #start()} method to start the transferring thread.
     * The thread terminates itself when end of input stream is encountered.
     */
    private final class StreamPipe extends Thread {

        /**
         * Input stream.
         */
        private InputStream input;

        /**
         * Output stream.
         */
        private OutputStream output;

        /**
         * Constructs this object.
         *
         * @param   input
         *          Input stream to read data from.
         *
         * @param   output
         *          Output stream to write data to.
         *          Can be <code>null</code>, in this case data are just read
         *          from <code>input</code> stream and discarded.
         */
        StreamPipe(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
            setDaemon(true);
        }

        /**
         * Runs this thread, called by VM.
         */
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                int len;

                /*
                 * Checking the streams' state seems to be unnecessary,
                 * as soon as the thread is a daemon thread,
                 * and will also exit at first error in either stream.
                 */
                while ((len = input.read(buffer)) > 0) {
                    if (output != null) {
                        output.write(buffer, 0, len);
                        output.flush();
                    }
                }
                // rmi.4B=read(byte[]) returned unexpected value: {0}
                assert (len == -1)
                        : (Messages.getString("rmi.4B", len)); //$NON-NLS-1$
            } catch (IOException e) {
                // rmi.console.07=StreamPipe error:
                System.err.print(Messages.getString("rmi.console.07")); //$NON-NLS-1$
                e.printStackTrace();
            }
        }
    }
}
