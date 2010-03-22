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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.beans;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation shows the correspondence between parameters of constructor
 * and getter methods. For example: 
 * <code>public class Bean {
 * 
 *     private final int x;
 * 
 *     @ConstructorProperties({"x"}) 
 *     public Bean(int x) { 
 *         this.x = x;
 *     }
 * 
 *     public int getX() { 
 *         return x; 
 *     } 
 * }</code>
 * 
 * Using annotation ConstructorProperties on Bean's contructor means parameter x
 * should be retrieved by getX.
 * 
 * @since 1.6
 */
@Documented
@Target(value = CONSTRUCTOR)
@Retention(value = RUNTIME)
public @interface ConstructorProperties {

    public String[] value();

}
