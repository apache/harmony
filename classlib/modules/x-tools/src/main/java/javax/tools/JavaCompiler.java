/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.tools;

import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.annotation.processing.Processor;

public interface JavaCompiler extends OptionChecker, Tool {

    public static interface CompilationTask extends Callable<Boolean> {
        Boolean call();

        void setLocale(Locale locale);

        void setProcessors(Iterable<? extends Processor> processors);
    }

    StandardJavaFileManager getStandardFileManager(
            DiagnosticListener<? super JavaFileObject> diagnosticListener,
            Locale locale, Charset charset);

    JavaCompiler.CompilationTask getTask(Writer out,
            JavaFileManager fileManager,
            DiagnosticListener<? super JavaFileObject> diagnosticListener,
            Iterable<String> options, Iterable<String> classes,
            Iterable<? extends JavaFileObject> compilationUnits);
}
