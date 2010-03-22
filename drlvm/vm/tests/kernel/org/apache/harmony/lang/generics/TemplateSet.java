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

/**
 * @author Serguei S.Zapreyev
 **/

package org.apache.harmony.lang.generics;

import java.lang.reflect.*;

//import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/*
 * Created on May 05, 2006
 *
 * This TemplateSet class keeps the template set for the java.lang.ClassGenericsTest4 unit test
 * 
 */

public class TemplateSet<X> {

	@Retention(value=RetentionPolicy.RUNTIME)
	@interface igt{
	    abstract String author() default "Zapreyev";
	};
    @igt class Mc001 {};


	@igt interface MI001<T0 extends java.io.Serializable> {};
    @igt interface MI002<T1 extends MI001> {};
    @igt interface MI003<T2> extends MI001 {};
    @igt interface MI004<T2> {
        @igt interface MI005<T21, T22> {
        };
    };
    @igt(author="Serguei Stepanovich Zapreyev") public class Mc002<T3 extends TemplateSet/*Class*/> {
        @igt public class Mc004<T5 extends TemplateSet/*Integer*/> {
        };
    };
    @igt class Mc003<T4 extends Thread &java.io.Serializable &Cloneable> extends TemplateSet<? super Class>.Mc002<TemplateSet> implements MI002<MI003<java.io.Serializable>>, MI003<MI003<Cloneable>>, MI004/*<Cloneable>*/.MI005<Type, Type> {};
    @igt class Mc007\u0391<T7 extends Thread &java.io.Serializable &Cloneable> {};
    @igt class Mc008\u0576\u06C0\u06F10<T8 extends Mc007\u0391 &java.io.Serializable &Cloneable> extends TemplateSet<? super Mc007\u0391>.Mc002<TemplateSet> implements MI002<MI003<java.io.Serializable>>, MI003<MI003<Cloneable>>, MI004.MI005<Mc007\u0391, Type> {};
    @igt class Mc010\u0576\u06C0\u06F10 {};
    @igt interface MI010\u0576\u06C0\u06F10 {};
    @igt class MC011\u0576\u06C0\u06F10 extends Mc010\u0576\u06C0\u06F10 implements MI010\u0576\u06C0\u06F10 {};

    class Mc005 extends Thread implements java.io.Serializable, Cloneable {private static final long serialVersionUID = 0L;};
    public Mc003<Mc005> fld0;
    public TemplateSet<? super Class>.Mc002<TemplateSet>.Mc004<TemplateSet> f0;
    public X f111;
    public TemplateSet f112;
    static class Mc009\u0576\u06C0\u06F1 extends Throwable implements java.io.Serializable, Cloneable {private static final long serialVersionUID = 0L;};
    @igt(author="*****") public <UuUuU extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> void foo1For_5(UuUuU a1) throws UuUuU, java.io.IOException {}
    public <\u0391 extends Throwable, TM1, TM2, TM3, TM4, TM5, TM6, TM7> X foo2For_5()  throws \u0391, java.io.IOException {X f = null; return f;}
    public <\u0576\u06C0\u06F1 extends Throwable, \u0576\u06C0\u06F11 extends Throwable, \u0576\u06C0\u06F12 extends Throwable, \u0576\u06C0\u06F13 extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> TM2 foo3For_5(\u0576\u06C0\u06F1[] BAAB, TM1 a1, TM2 a2, TemplateSet<? super Class>.Mc002<TemplateSet>.Mc004<TemplateSet> a3) throws \u0576\u06C0\u06F1, Throwable, \u0576\u06C0\u06F13, \u0576\u06C0\u06F12, \u0576\u06C0\u06F11, TemplateSet.Mc009\u0576\u06C0\u06F1 {TM2 f = null; return f;}
    public Mc003<Mc005> foo4For_5(TemplateSet<? super Class>.Mc002<TemplateSet>.Mc004<?> a1, @igt(author="Czar") Mc003<Mc005> a2, @igt(author="President") Mc003<Mc005> ... a3) {return a2;}
    public TemplateSet<? super Class>.Mc002<TemplateSet>.Mc004<TemplateSet> foo5For_5(X a1, Class<Type> a2,  TemplateSet<? super Class>.Mc002<TemplateSet>.Mc004<TemplateSet> a3) {return a3;}
	class MC006{
		@igt(author="*****") public <UuUuU extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> MC006(UuUuU a1) throws UuUuU, java.io.IOException {}
		public <\u0391 extends Throwable, TM1, TM2, TM3, TM4, TM5, TM6, TM7> MC006()  throws \u0391, java.io.IOException {}
		public <\u0576\u06C0\u06F1 extends Throwable, \u0576\u06C0\u06F11 extends Throwable, \u0576\u06C0\u06F12 extends Throwable, \u0576\u06C0\u06F13 extends Throwable, TM1, TM2 extends Thread &java.io.Serializable &Cloneable> MC006(\u0576\u06C0\u06F1[] BAAB, TM1 a1, TM2 a2, TemplateSet<? super Class>.Mc002<TemplateSet>.Mc004<TemplateSet> a3) throws \u0576\u06C0\u06F1, Throwable, \u0576\u06C0\u06F13, \u0576\u06C0\u06F12, \u0576\u06C0\u06F11 {}
		public MC006(TemplateSet<? super Class>.Mc002<TemplateSet>.Mc004<?> a1, @igt(author="Czar") Mc003<Mc005> a2, @igt(author="President") Mc003<Mc005> ... a3) {}
		public MC006(X a1, Class<Type> a2,  TemplateSet<? super Class>.Mc002<TemplateSet>.Mc004<TemplateSet> a3) {}
	}
}