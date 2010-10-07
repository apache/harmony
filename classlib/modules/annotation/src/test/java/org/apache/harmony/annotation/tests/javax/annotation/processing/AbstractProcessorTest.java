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
package org.apache.harmony.annotation.tests.javax.annotation.processing;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import junit.framework.TestCase;

public class AbstractProcessorTest extends TestCase {

    AbstractProcessor processor;
    AbstractProcessor annotatedProcessor;

    public void setUp() {
        processor = new MockAbstractProcessor();
        annotatedProcessor = new MockAbstractProcessorAnnotated();
    }

    /**
     * @tests javax.annotation.processing.AbstractProcessor.getSupportedOptions()
     */
    public void test_getSupportedOptions() {
        // check default options
        Set<String> supportedOptions = processor.getSupportedOptions();
        assertEquals(0, supportedOptions.size());
        // set should be unmodifiable
        try {
            supportedOptions.add("test");
            fail("Returned supported options set is not unmodifiable");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // check annotated options
        supportedOptions = annotatedProcessor.getSupportedOptions();
        assertEquals(2, supportedOptions.size());
        assertTrue(supportedOptions.contains("option.one"));
        assertTrue(supportedOptions.contains("option.two"));
        // set should be unmodifiable
        try {
            supportedOptions.add("test");
            fail("Returned supported options set is not unmodifiable");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /**
     * @tests javax.annotation.processing.AbstractProcessor.getSupportedAnnotationTypes()
     */
    public void test_getSupportedAnnotationTypes() {
        // check default types
        Set<String> supportedTypes = processor.getSupportedAnnotationTypes();
        assertEquals(0, supportedTypes.size());
        // set should be unmodifiable
        try {
            supportedTypes.add("test");
            fail("Returned supported annotation types set is not unmodifiable");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // check annotated types
        supportedTypes = annotatedProcessor.getSupportedAnnotationTypes();
        assertEquals(3, supportedTypes.size());
        assertTrue(supportedTypes.contains("type.one"));
        assertTrue(supportedTypes.contains("type.two"));
        assertTrue(supportedTypes.contains("type2.*"));
        // set should be unmodifiable
        try {
            supportedTypes.add("test");
            fail("Returned supported annotation types set is not unmodifiable");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /**
     * @tests javax.annotation.processing.AbstractProcessor.getSupportedSourceVersion()
     */
    public void test_getSupportedSourceVersion() {
        // check default source version
        assertEquals(SourceVersion.RELEASE_6, processor
                .getSupportedSourceVersion());

        // check annotated version
        assertEquals(SourceVersion.RELEASE_5, annotatedProcessor
                .getSupportedSourceVersion());
    }

    /**
     * @tests javax.annotation.processing.AbstractProcessor.init(
     *        javax.annotation.processing.ProcessingEnvironment)
     */
    public void test_init() {
        try {
            processor.init(null);
            fail("Calling init(null) should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }
        ProcessingEnvironment processingEnv = new MockProcessingEnvironment();
        processor.init(processingEnv);

        assertEquals(processingEnv, ((MockAbstractProcessor) processor)
                .getEnvironment());

        try {
            processor.init(processingEnv);
            fail("Calling init twice should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }

        try {
            processor.init(new MockProcessingEnvironment());
            fail("Calling init twice should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests javax.annotation.processing.AbstractProcessor.isInitialized()
     */
    public void test_isInitialized() {
        assertFalse(((MockAbstractProcessor) processor).isInitialized());
        processor.init(new MockProcessingEnvironment());
        assertTrue(((MockAbstractProcessor) processor).isInitialized());
    }

    /**
     * @tests javax.annotation.processing.AbstractProcessor.getCompletions()
     */
    public void test_getCompletions() {
        // check that the default implementation returns an empty iterator
        Iterable<? extends Completion> completions = processor.getCompletions(
                null, null, null, null);
        assertFalse(completions.iterator().hasNext());
    }

    class MockAbstractProcessor extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations,
                RoundEnvironment roundEnv) {
            return false;
        }

        public ProcessingEnvironment getEnvironment() {
            return processingEnv;
        }

        public boolean isInitialized() {
            return super.isInitialized();
        }
    }

    @SupportedSourceVersion(SourceVersion.RELEASE_5)
    @SupportedAnnotationTypes( { "type.one", "type.two", "type2.*" })
    @SupportedOptions( { "option.one", "option.two", "option.one" })
    class MockAbstractProcessorAnnotated extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations,
                RoundEnvironment roundEnv) {
            return false;
        }

        public ProcessingEnvironment getEnvironment() {
            return processingEnv;
        }

        public boolean isInitialized() {
            return super.isInitialized();
        }
    }

    class MockProcessingEnvironment implements ProcessingEnvironment {

        public Elements getElementUtils() {
            return null;
        }

        public Filer getFiler() {
            return null;
        }

        public Locale getLocale() {
            return null;
        }

        public Messager getMessager() {
            return null;
        }

        public Map<String, String> getOptions() {
            return null;
        }

        public SourceVersion getSourceVersion() {
            return null;
        }

        public Types getTypeUtils() {
            return null;
        }
    }
}
