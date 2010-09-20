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

package javax.annotation.processing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public abstract class AbstractProcessor implements Processor {

    private boolean isInitialized;

    protected ProcessingEnvironment processingEnv;

    protected AbstractProcessor() {

    }

    public Iterable<? extends Completion> getCompletions(Element element,
            AnnotationMirror annotation, ExecutableElement member,
            String userText) {
        // return a empty iterable
        return new Iterable<Completion>() {
            public Iterator<Completion> iterator() {
                return new Iterator<Completion>() {

                    public boolean hasNext() {
                        return false;
                    }

                    public Completion next() {
                        return null;
                    }

                    public void remove() {
                        // do nothing
                    }
                };
            }
        };
    }

    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>();
    }

    public Set<String> getSupportedOptions() {
        return new HashSet<String>();
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_6;
    }

    public void init(ProcessingEnvironment processingEnv) {
        if (this.processingEnv != null && this.processingEnv == processingEnv) {
            throw new IllegalStateException();
        }
        this.processingEnv = processingEnv;
        isInitialized = true;
    }

    protected boolean isInitialized() {
        return isInitialized;
    }

    public abstract boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv);

}
