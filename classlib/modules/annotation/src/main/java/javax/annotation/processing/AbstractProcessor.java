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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public abstract class AbstractProcessor implements Processor {

    private boolean isInitialized;

    private static final Set<String> emptySet;

    protected ProcessingEnvironment processingEnv;

    static {
        Set<String> tempSet = Collections.emptySet();
        emptySet = Collections.unmodifiableSet(tempSet);
    }
    
    protected AbstractProcessor() {

    }

    public Iterable<? extends Completion> getCompletions(Element element,
            AnnotationMirror annotation, ExecutableElement member,
            String userText) {
        // return an empty iterable
        List<Completion> emptyList = Collections.emptyList();
        return emptyList;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getSupportedAnnotationTypes() {
        // check if the processing class has the types set
        SupportedAnnotationTypes types = this.getClass().getAnnotation(SupportedAnnotationTypes.class);
        if (types != null) {
            return Collections.unmodifiableSet(new HashSet(Arrays.asList(types.value())));
        }
        // otherwise return an empty set
        return emptySet;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getSupportedOptions() {
        // check if the processing class has the options set
        SupportedOptions options = this.getClass().getAnnotation(SupportedOptions.class);
        if (options != null) {
            return Collections.unmodifiableSet(new HashSet(Arrays.asList(options.value())));
        }
        // otherwise return an empty set
        return emptySet;
    }

    public SourceVersion getSupportedSourceVersion() {
        // check if the processing class has the source version set
        SupportedSourceVersion sourceVersion = this.getClass().getAnnotation(SupportedSourceVersion.class);
        if (sourceVersion != null) {
            return sourceVersion.value();
        }
        return SourceVersion.RELEASE_6;
    }

    public void init(ProcessingEnvironment processingEnv) {
        if (processingEnv == null) {
            throw new NullPointerException();
        }
        if (this.processingEnv != null) {
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
